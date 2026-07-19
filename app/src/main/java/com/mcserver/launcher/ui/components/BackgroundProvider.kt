package com.mcserver.launcher.ui.components

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mcserver.launcher.data.PreferencesManager
import java.io.File

/**
 * 背景渲染层 — 在内容下方叠加图片/视频背景 + 模糊 + 暗化遮罩。
 *
 * 设计：基底保持纯黑白灰，个性化叠加在底层，不破坏文字可读性。
 */
@Composable
fun BackgroundProvider(
    prefsManager: PreferencesManager,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgType by prefsManager.backgroundType.collectAsState(initial = "none")
    val bgPath by prefsManager.backgroundPath.collectAsState(initial = "")
    val blurStrength by prefsManager.backgroundBlur.collectAsState(initial = 0f)
    val darkMask by prefsManager.backgroundDarkMask.collectAsState(initial = 0.3f)

    Box(modifier = modifier) {
        // 底层：背景
        if (bgType != "none" && bgPath.isNotEmpty()) {
            when (bgType) {
                "image" -> ImageBackground(
                    path = bgPath,
                    blurStrength = blurStrength,
                    darkMask = darkMask
                )
                "video" -> VideoBackground(
                    path = bgPath,
                    blurStrength = blurStrength,
                    darkMask = darkMask
                )
            }
        }

        // 上层：内容
        content()
    }
}

@Composable
private fun ImageBackground(
    path: String,
    blurStrength: Float,
    darkMask: Float
) {
    val context = LocalContext.current
    val isContentUri = path.startsWith("content://")

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(if (isContentUri) Uri.parse(path) else File(path))
                .crossfade(1000)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurStrength > 0) Modifier.blur(blurStrength.dp)
                    else Modifier
                ),
            contentScale = ContentScale.Crop
        )

        // 暗化遮罩
        if (darkMask > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = darkMask))
            )
        }
    }
}

@Composable
private fun VideoBackground(
    path: String,
    blurStrength: Float,
    darkMask: Float
) {
    // Android 视频背景使用 VideoView + TextureView 桥接，
    // 在 Compose 中通过 AndroidView 嵌入。
    // 此处为简化实现，视频背景仅在非 Compose 层实现。
    // 实际项目中可用 ExoPlayer + AndroidView 实现。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (blurStrength > 0) Modifier.blur(blurStrength.dp)
                else Modifier
            )
            .background(Color.Black.copy(alpha = 0.5f))
    )
}

/**
 * 验证文件是否为图片类型
 */
fun isImageFile(path: String): Boolean {
    val lower = path.lowercase()
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
        lower.endsWith(".png") || lower.endsWith(".webp") ||
        lower.endsWith(".gif") || lower.endsWith(".bmp")
}

/**
 * 验证文件是否为视频类型
 */
fun isVideoFile(path: String): Boolean {
    val lower = path.lowercase()
    return lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
        lower.endsWith(".webm") || lower.endsWith(".avi") ||
        lower.endsWith(".mov") || lower.endsWith(".flv")
}