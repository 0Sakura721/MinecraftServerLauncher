import java.net.HttpURLConnection
import java.net.URL
import java.io.FileOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ═══════════════════════════════════════════════════════════════
//  构建时预下载：proot 二进制 + Alpine minirootfs 内置到 APK assets
//  用户首次启动无需等待下载，直接从 APK 提取即可
// ═══════════════════════════════════════════════════════════════
val bundledAssetsDir = layout.projectDirectory.dir("src/main/assets/bundled")

val downloadBundledAssets by tasks.registering {
    group = "bundled"
    description = "下载 proot + Alpine rootfs 到 assets/bundled/，实现内置 Linux 环境"

    val prootVersion = "v5.4.0"
    val alpineVersion = "3.21.0"
    val alpineMinor = "v3.21"

    // proot 二进制
    val prootFiles = mapOf(
        "proot-aarch64" to "https://github.com/proot-me/proot-static-build/releases/download/$prootVersion/proot_${prootVersion.drop(1)}_aarch64",
        "proot-armhf"   to "https://github.com/proot-me/proot-static-build/releases/download/$prootVersion/proot_${prootVersion.drop(1)}_armhf",
    )

    // Alpine minirootfs
    val alpineFiles = mapOf(
        "alpine-minirootfs-$alpineVersion-aarch64.tar.gz" to "https://dl-cdn.alpinelinux.org/alpine/$alpineMinor/releases/aarch64/alpine-minirootfs-$alpineVersion-aarch64.tar.gz",
        "alpine-minirootfs-$alpineVersion-armhf.tar.gz"   to "https://dl-cdn.alpinelinux.org/alpine/$alpineMinor/releases/armhf/alpine-minirootfs-$alpineVersion-armhf.tar.gz",
    )

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
                conn.connectTimeout = 30000
                conn.readTimeout = 120000
                conn.instanceFollowRedirects = true
                // 跟随重定向（GitHub release 会 302）
                var currentConn = conn
                for (i in 1..5) {
                    val code = currentConn.responseCode
                    if (code == 302 || code == 301 || code == 307 || code == 308) {
                        val loc = currentConn.getHeaderField("Location") ?: break
                        currentConn.disconnect()
                        currentConn = URL(loc).openConnection() as HttpURLConnection
                        currentConn.connectTimeout = 30000
                        currentConn.readTimeout = 120000
                    } else break
                }
                if (currentConn.responseCode != 200) {
                    System.err.println("  ✗ HTTP ${currentConn.responseCode}: ${dest.name}")
                    return false
                }
                val total = currentConn.contentLengthLong
                FileOutputStream(dest).use { out ->
                    currentConn.inputStream.use { input ->
                        val buf = ByteArray(8192)
                        var read: Int
                        var downloaded = 0L
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            downloaded += read
                            if (total > 0 && downloaded % (1024 * 1024) == 0L) {
                                print("\r    ${downloaded * 100 / total}% (${downloaded / 1024} / ${total / 1024} KB)")
                            }
                        }
                    }
                }
                currentConn.disconnect()
                println("\r  ✓ 完成: ${dest.name} (${dest.length() / 1024} KB)")
                return true
            } catch (e: Exception) {
                System.err.println("  ✗ 失败: ${dest.name} - ${e.message}")
                dest.delete()
                return false
            }
        }

        println("═══ 下载预置资源 ═══")
        prootFiles.forEach { (name, url) ->
            download(url, File(destDir, name))
        }
        alpineFiles.forEach { (name, url) ->
            download(url, File(destDir, name))
        }
        println("═══ 预置资源完成 ═══")
    }
}

tasks.named("preBuild") {
    dependsOn(downloadBundledAssets)
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
