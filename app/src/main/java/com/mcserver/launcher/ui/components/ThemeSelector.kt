package com.mcserver.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mcserver.launcher.ui.theme.ThemeMode

@Composable
fun ThemeSelectorCard(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "主题",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            ThemeMode.entries.forEach { mode ->
                ThemeOption(
                    mode = mode,
                    isSelected = currentTheme == mode,
                    onClick = { onThemeSelected(mode) }
                )
                if (mode != ThemeMode.entries.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val previewColors = when (mode) {
        ThemeMode.LIGHT -> listOf(Color(0xFFF5F5F5), Color(0xFFFFFFFF), Color(0xFF4CAF50))
        ThemeMode.DARK -> listOf(Color(0xFF1C1B1F), Color(0xFF2B2930), Color(0xFF81C784))
        ThemeMode.AMOLED -> listOf(Color(0xFF000000), Color(0xFF0D0D0D), Color(0xFF81C784))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主题预览色块
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                previewColors.forEach { color ->
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = color,
                        border = if (color == Color(0xFFFFFFFF) || color == Color(0xFFF5F5F5))
                            ButtonDefaults.outlinedButtonBorder
                        else null
                    ) {}
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
