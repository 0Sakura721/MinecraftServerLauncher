import java.net.HttpURLConnection
import java.net.URL
import java.io.FileOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ═══════════════════════════════════════════════════════════════
//  内置资源辅助任务（手动运行，非阻塞）
//
//  运行方式：./gradlew :app:downloadBundledAssets
//
//  下载 proot + Ubuntu rootfs 到 assets/bundled/，
//  之后构建的 APK 将内置这些文件，用户首次启动零下载。
//
//  注意：CI 构建不会自动运行此任务。
//        如需发布内置版 APK，请先在本地运行此任务再构建。
// ═══════════════════════════════════════════════════════════════
val bundledAssetsDir = layout.projectDirectory.dir("src/main/assets/bundled")

val downloadBundledAssets by tasks.registering {
    group = "bundled"
    description = "下载 proot + Ubuntu 24.04 rootfs 到 assets/bundled/"

    // Ubuntu 24.04 base rootfs（官方 CDN，CI 可访问）
    val ubuntuVersion = "24.04"
    val files = linkedMapOf(
        "ubuntu-base-$ubuntuVersion-arm64.tar.gz" to
            "https://cdimage.ubuntu.com/ubuntu-base/releases/$ubuntuVersion/release/ubuntu-base-$ubuntuVersion-base-arm64.tar.gz",
        "ubuntu-base-$ubuntuVersion-armhf.tar.gz" to
            "https://cdimage.ubuntu.com/ubuntu-base/releases/$ubuntuVersion/release/ubuntu-base-$ubuntuVersion-base-armhf.tar.gz",
    )
    // proot 二进制暂时不自动下载（上游仅提供 x86_64 构建），
    // 请手动放置到 assets/bundled/proot-aarch64 和 proot-armhf。
    // 来源参考：Termux proot 包 或 proot-me/proot-static-build 的 community ARM builds

    doLast {
        val destDir = bundledAssetsDir.asFile
        destDir.mkdirs()

        fun download(urlStr: String, dest: java.io.File): Boolean {
            if (dest.exists() && dest.length() > 0) {
                println("  ⏭ 跳过（已存在）: ${dest.name} (${dest.length() / 1024} KB)")
                return true
            }
            try {
                println("  ⬇ 下载: ${dest.name} ...")
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 60000
                conn.readTimeout = 300000
                conn.instanceFollowRedirects = true
                var currentConn = conn
                for (i in 1..5) {
                    val code = currentConn.responseCode
                    if (code in listOf(301, 302, 307, 308)) {
                        val loc = currentConn.getHeaderField("Location") ?: break
                        currentConn.disconnect()
                        currentConn = URL(loc).openConnection() as HttpURLConnection
                        currentConn.connectTimeout = 60000
                        currentConn.readTimeout = 300000
                    } else break
                }
                check(currentConn.responseCode == 200) { "HTTP ${currentConn.responseCode}" }
                val total = currentConn.contentLengthLong
                FileOutputStream(dest).use { out ->
                    currentConn.inputStream.use { input ->
                        val buf = ByteArray(8192)
                        var read: Int
                        var downloaded = 0L
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            downloaded += read
                            if (total > 0 && downloaded % (5 * 1024 * 1024) == 0L) {
                                print("\r    ${downloaded * 100 / total}% (${downloaded / 1024 / 1024} / ${total / 1024 / 1024} MB)")
                            }
                        }
                    }
                }
                currentConn.disconnect()
                println("\r  ✓ 完成: ${dest.name} (${dest.length() / 1024 / 1024} MB)")
                return true
            } catch (e: Exception) {
                System.err.println("  ✗ 失败: ${dest.name} - ${e.message}")
                dest.delete()
                return false
            }
        }

        println("═══ 下载预置资源 ═══")
        var allOk = true
        files.forEach { (name, url) ->
            if (!download(url, File(destDir, name))) allOk = false
        }
        println("═══ 预置资源完成 ${if (allOk) "✓" else "（有失败项，不影响构建）"} ═══")
    }
}

android {
    namespace = "com.mcserver.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mcserver.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.10.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // BuildConfig fields accessible at runtime
        buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
        buildConfigField("String", "GIT_COMMIT", "\"${try {
            ProcessBuilder("git", "rev-parse", "--short", "HEAD").start().inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) { "unknown" }}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "kaze123"
            keyAlias = "kaze_debug"
            keyPassword = "kaze123"
        }
        create("kazeDebug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "kaze123"
            keyAlias = "kaze_debug"
            keyPassword = "kaze123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("kazeDebug")
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Image loading (Coil)
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-core:3.0.4")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
