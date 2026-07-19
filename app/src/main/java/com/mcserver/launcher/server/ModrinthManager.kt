package com.mcserver.launcher.server

import com.mcserver.launcher.McApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Modrinth API 集成管理器 — 搜索和下载 Modrinth 上的模组/插件/数据包/资源包/着色器。
 * 借鉴 Prism Launcher / ATLauncher 的设计，使用 HttpURLConnection + org.json。
 *
 * Modrinth API 文档: https://docs.modrinth.com/
 */
object ModrinthManager {

    private const val BASE_URL = "https://api.modrinth.com/v2"
    private const val USER_AGENT = "MCServerLauncher/0.10.0 (com.mcserver.launcher)"
    private const val REQUEST_TIMEOUT = 30_000

    // ── 数据模型 ──────────────────────────────────────────────

    data class ModrinthProject(
        val id: String,
        val title: String,
        val description: String,
        val iconUrl: String?,
        val downloads: Int,
        val followers: Int,
        val categories: List<String>,
        val projectType: String,
        val slug: String = "",
        val clientSide: String = "",
        val serverSide: String = ""
    )

    data class ModrinthVersion(
        val id: String,
        val name: String,
        val versionNumber: String,
        val gameVersions: List<String>,
        val loaders: List<String>,
        val files: List<ModrinthFile>,
        val datePublished: String = "",
        val changelog: String = ""
    )

    data class ModrinthFile(
        val url: String,
        val filename: String,
        val size: Long,
        val primary: Boolean = false
    )

    data class SearchResult(
        val projects: List<ModrinthProject>,
        val totalHits: Int,
        val offset: Int,
        val limit: Int
    )

    // ── 搜索 ──────────────────────────────────────────────────

    /**
     * 搜索 Modrinth 上的项目。
     *
     * @param query 搜索关键词
     * @param projectTypes 项目类型过滤（mod, plugin, datapack, resourcepack, shader）
     * @param limit 每页数量
     * @param offset 偏移量
     * @param gameVersion Minecraft 版本过滤（可选）
     */
    suspend fun search(
        query: String,
        projectTypes: List<String> = listOf("mod", "plugin"),
        limit: Int = 20,
        offset: Int = 0,
        gameVersion: String? = null
    ): SearchResult = withContext(Dispatchers.IO) {
        val facets = buildFacets(projectTypes, gameVersion)
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val encodedFacets = URLEncoder.encode(facets, "UTF-8")

        val url = URL("$BASE_URL/search?query=$encodedQuery&limit=$limit&offset=$offset&facets=$encodedFacets")
        val json = fetchJson(url)

        val hits = mutableListOf<ModrinthProject>()
        val hitsArray = json.optJSONArray("hits") ?: JSONArray()
        for (i in 0 until hitsArray.length()) {
            val item = hitsArray.getJSONObject(i)
            hits.add(parseProject(item))
        }

        SearchResult(
            projects = hits,
            totalHits = json.optInt("total_hits", 0),
            offset = json.optInt("offset", 0),
            limit = json.optInt("limit", limit)
        )
    }

    // ── 项目详情 ──────────────────────────────────────────────

    /**
     * 获取项目详细信息。
     */
    suspend fun getProject(projectId: String): ModrinthProject = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/project/$projectId")
        val json = fetchJson(url)
        parseProject(json)
    }

    /**
     * 通过 slug 获取项目详细信息。
     */
    suspend fun getProjectBySlug(slug: String): ModrinthProject = withContext(Dispatchers.IO) {
        // 先搜索 slug 获取 id
        val result = search(slug, limit = 1)
        val matched = result.projects.firstOrNull { it.slug.equals(slug, ignoreCase = true) }
        if (matched != null) {
            getProject(matched.id)
        } else {
            throw NoSuchElementException("未找到 slug 为 '$slug' 的项目")
        }
    }

    // ── 版本列表 ──────────────────────────────────────────────

    /**
     * 获取项目的版本列表。
     *
     * @param projectId 项目 ID
     * @param loaders 加载器过滤（paper, spigot, fabric, forge, quilt, neoforge 等）
     * @param gameVersions Minecraft 版本过滤
     */
    suspend fun getVersions(
        projectId: String,
        loaders: List<String> = listOf("paper", "spigot", "fabric", "forge"),
        gameVersions: List<String> = emptyList()
    ): List<ModrinthVersion> = withContext(Dispatchers.IO) {
        val params = mutableListOf<String>()

        if (loaders.isNotEmpty()) {
            val encodedLoaders = URLEncoder.encode(JSONArray(loaders).toString(), "UTF-8")
            params.add("loaders=$encodedLoaders")
        }
        if (gameVersions.isNotEmpty()) {
            val encodedVersions = URLEncoder.encode(JSONArray(gameVersions).toString(), "UTF-8")
            params.add("game_versions=$encodedVersions")
        }

        val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val url = URL("$BASE_URL/project/$projectId/version$queryString")
        val jsonArray = fetchJsonArray(url)

        val versions = mutableListOf<ModrinthVersion>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            versions.add(parseVersion(item))
        }
        versions
    }

    /**
     * 获取单个版本详情。
     */
    suspend fun getVersion(versionId: String): ModrinthVersion = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/version/$versionId")
        val json = fetchJson(url)
        parseVersion(json)
    }

    // ── 下载 ──────────────────────────────────────────────────

    /**
     * 下载模组/插件文件到目标目录。
     *
     * @param fileInfo 要下载的文件信息
     * @param targetDir 目标目录（plugins/ 或 mods/）
     * @param onProgress 进度回调（0.0 - 1.0）
     * @return 下载后的本地文件
     */
    suspend fun downloadFile(
        fileInfo: ModrinthFile,
        targetDir: File,
        onProgress: ((Float) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) targetDir.mkdirs()

        val targetFile = File(targetDir, fileInfo.filename)

        val connection = URL(fileInfo.url).openConnection() as HttpURLConnection
        connection.connectTimeout = REQUEST_TIMEOUT
        connection.readTimeout = 300_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("下载失败: HTTP ${connection.responseCode} - ${connection.responseMessage}")
        }

        val totalSize = connection.contentLength.toLong()
        val input = BufferedInputStream(connection.inputStream)
        val output = FileOutputStream(targetFile)
        val buffer = ByteArray(16384)
        var bytesRead: Int
        var downloaded = 0L

        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            downloaded += bytesRead

            if (totalSize > 0 && onProgress != null) {
                onProgress((downloaded.toFloat() / totalSize).coerceIn(0f, 1f))
            }
        }

        output.flush()
        output.close()
        input.close()
        connection.disconnect()

        targetFile
    }

    /**
     * 下载项目的特定版本到目标目录。
     * 自动选择与加载器和游戏版本匹配的第一个文件。
     *
     * @param projectId 项目 ID
     * @param targetDir 目标目录
     * @param loader 加载器类型（如 "paper"）
     * @param gameVersion Minecraft 版本（如 "1.20.1"）
     * @param onProgress 进度回调
     * @return 下载后的本地文件，如果没有匹配版本则返回 null
     */
    suspend fun downloadProjectVersion(
        projectId: String,
        targetDir: File,
        loader: String = "paper",
        gameVersion: String = "",
        onProgress: ((Float) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        val versions = if (gameVersion.isNotEmpty()) {
            getVersions(projectId, listOf(loader), listOf(gameVersion))
        } else {
            getVersions(projectId, listOf(loader))
        }

        // 选择最新的匹配版本
        val latestVersion = versions.firstOrNull { version ->
            version.files.isNotEmpty()
        } ?: return@withContext null

        // 选择主文件
        val file = latestVersion.files.firstOrNull { it.primary } ?: latestVersion.files.first()

        downloadFile(file, targetDir, onProgress)
    }

    /**
     * 获取项目最新版本的下载 URL（不下载）。
     */
    suspend fun getLatestDownloadUrl(
        projectId: String,
        loader: String = "paper",
        gameVersion: String = ""
    ): String? = withContext(Dispatchers.IO) {
        val versions = if (gameVersion.isNotEmpty()) {
            getVersions(projectId, listOf(loader), listOf(gameVersion))
        } else {
            getVersions(projectId, listOf(loader))
        }

        val latestVersion = versions.firstOrNull { it.files.isNotEmpty() } ?: return@withContext null
        val file = latestVersion.files.firstOrNull { it.primary } ?: latestVersion.files.first()
        file.url
    }

    // ── 辅助工具 ──────────────────────────────────────────────

    /**
     * 获取 Modrinth 服务器的统计信息（项目总数、作者总数等）。
     */
    suspend fun getStatistics(): JSONObject = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/statistics")
        fetchJson(url)
    }

    /**
     * 获取项目的图标 Bitmap（返回 URL，由调用方自行加载）。
     */
    fun getIconUrl(project: ModrinthProject): String? {
        return project.iconUrl
    }

    // ── 内部解析方法 ──────────────────────────────────────────

    private fun parseProject(json: JSONObject): ModrinthProject {
        val categories = mutableListOf<String>()
        val categoriesArray = json.optJSONArray("categories")
        if (categoriesArray != null) {
            for (i in 0 until categoriesArray.length()) {
                categories.add(categoriesArray.getString(i))
            }
        }

        return ModrinthProject(
            id = json.optString("project_id", json.optString("id", "")),
            title = json.optString("title", ""),
            description = json.optString("description", ""),
            iconUrl = json.optString("icon_url", null),
            downloads = json.optInt("downloads", 0),
            followers = json.optInt("followers", 0),
            categories = categories,
            projectType = json.optString("project_type", ""),
            slug = json.optString("slug", ""),
            clientSide = json.optString("client_side", ""),
            serverSide = json.optString("server_side", "")
        )
    }

    private fun parseVersion(json: JSONObject): ModrinthVersion {
        val gameVersions = mutableListOf<String>()
        val gvArray = json.optJSONArray("game_versions")
        if (gvArray != null) {
            for (i in 0 until gvArray.length()) {
                gameVersions.add(gvArray.getString(i))
            }
        }

        val loaders = mutableListOf<String>()
        val loadersArray = json.optJSONArray("loaders")
        if (loadersArray != null) {
            for (i in 0 until loadersArray.length()) {
                loaders.add(loadersArray.getString(i))
            }
        }

        val files = mutableListOf<ModrinthFile>()
        val filesArray = json.optJSONArray("files")
        if (filesArray != null) {
            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                files.add(
                    ModrinthFile(
                        url = fileObj.optString("url", ""),
                        filename = fileObj.optString("filename", ""),
                        size = fileObj.optLong("size", 0),
                        primary = fileObj.optBoolean("primary", false)
                    )
                )
            }
        }

        return ModrinthVersion(
            id = json.optString("id", ""),
            name = json.optString("name", ""),
            versionNumber = json.optString("version_number", ""),
            gameVersions = gameVersions,
            loaders = loaders,
            files = files,
            datePublished = json.optString("date_published", ""),
            changelog = json.optString("changelog", "")
        )
    }

    private fun buildFacets(projectTypes: List<String>, gameVersion: String?): String {
        val facets = mutableListOf<List<String>>()

        if (projectTypes.isNotEmpty()) {
            facets.add(projectTypes.map { "project_type:$it" })
        }

        if (!gameVersion.isNullOrEmpty()) {
            facets.add(listOf("versions:$gameVersion"))
        }

        return JSONArray(facets.map { JSONArray(it) }).toString()
    }

    // ── 网络请求 ──────────────────────────────────────────────

    private fun fetchJson(url: URL): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = REQUEST_TIMEOUT
            connection.readTimeout = REQUEST_TIMEOUT
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("API 请求失败: HTTP ${connection.responseCode} - ${connection.responseMessage}")
            }

            val text = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(text)
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchJsonArray(url: URL): JSONArray {
        var connection: HttpURLConnection? = null
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = REQUEST_TIMEOUT
            connection.readTimeout = REQUEST_TIMEOUT
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("API 请求失败: HTTP ${connection.responseCode} - ${connection.responseMessage}")
            }

            val text = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONArray(text)
        } finally {
            connection?.disconnect()
        }
    }
}
