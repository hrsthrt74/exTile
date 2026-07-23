package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(TileConfig()) }
    var currentSysuiTiles by remember { mutableStateOf("") }
    var backupText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        config = ConfigRepository.getConfig(context)
        currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
    }

    fun generateBackupJson(): String {
        return buildString {
            append("{\n")
            append("  \"expandedTiles\": [${config.expandedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"collapsedTiles\": [${config.collapsedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"currentSysuiTiles\": \"$currentSysuiTiles\"\n")
            append("}")
        }
    }

    fun parseBackupJson(json: String): TileConfig? {
        return try {
            val expandedMatch = Regex("\"expandedTiles\":\\s*\\[([^\\]]+)\\]").find(json)
            val collapsedMatch = Regex("\"collapsedTiles\":\\s*\\[([^\\]]+)\\]").find(json)

            if (expandedMatch != null && collapsedMatch != null) {
                val expanded = expandedMatch.groupValues[1]
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }

                val collapsed = collapsedMatch.groupValues[1]
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }

                TileConfig(expandedTiles = expanded, collapsedTiles = collapsed)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "备份与恢复",
            style = MiuixTheme.textStyles.headline1
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "当前系统磁贴",
                    style = MiuixTheme.textStyles.title2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentSysuiTiles.ifEmpty { "无数据" },
                    style = MiuixTheme.textStyles.body2
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "备份配置",
                    style = MiuixTheme.textStyles.title2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将当前配置导出为 JSON 格式",
                    style = MiuixTheme.textStyles.body2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { backupText = generateBackupJson() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成备份")
                }
            }
        }

        if (backupText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "备份数据",
                            style = MiuixTheme.textStyles.title2
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("exTile backup", backupText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = backupText,
                        style = MiuixTheme.textStyles.body2
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "恢复配置",
                    style = MiuixTheme.textStyles.title2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "粘贴备份的 JSON 数据来恢复配置",
                    style = MiuixTheme.textStyles.body2
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = backupText,
                    onValueChange = { backupText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "备份数据"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val restoredConfig = parseBackupJson(backupText)
                        if (restoredConfig != null) {
                            scope.launch {
                                ConfigRepository.saveExpandedTiles(context, restoredConfig.expandedTiles)
                                ConfigRepository.saveCollapsedTiles(context, restoredConfig.collapsedTiles)
                                config = restoredConfig
                                Toast.makeText(context, "配置已恢复", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "备份数据格式错误", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("恢复配置")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "恢复系统磁贴",
                    style = MiuixTheme.textStyles.title2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将当前系统磁贴配置保存为展开状态的配置",
                    style = MiuixTheme.textStyles.body2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val currentTiles = SecureSettingsHelper.getCurrentTiles(context)
                            if (currentTiles.isNotEmpty()) {
                                ConfigRepository.saveExpandedTiles(context, currentTiles)
                                config = config.copy(expandedTiles = currentTiles)
                                Toast.makeText(context, "已从系统导入磁贴配置", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "无法读取系统磁贴配置", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("从系统导入")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
