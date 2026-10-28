package com.thumbnailstudio.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thumbnailstudio.app.BuildConfig
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.SettingsStore
import com.thumbnailstudio.app.render.ImagePipeline
import com.thumbnailstudio.app.update.UpdateChecker
import com.thumbnailstudio.app.update.UpdateStatus
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    store: ProjectStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateStatus?>(null) }
    var checking by remember { mutableStateOf(false) }
    var cacheKb by remember { mutableStateOf(ImagePipeline.cacheSizeKb()) }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        Column(Modifier.padding(horizontal = 18.dp)) {

            SectionLabel("Appearance")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    ChipRow(
                        options = listOf("system", "dark", "light"),
                        selected = settings.themeMode,
                        onSelect = { settings.updateThemeMode(it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Canvas guides")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    ToggleRow("Show grid", settings.showGrid) { settings.updateShowGrid(it) }
                    ToggleRow("Show safe area", settings.showSafeArea) { settings.updateShowSafeArea(it) }
                    ToggleRow("Snap to centre guides", settings.snapToCenter) { settings.updateSnapToCenter(it) }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Export defaults")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Format", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    ChipRow(
                        options = listOf("PNG", "JPEG"),
                        selected = settings.exportFormat,
                        onSelect = { settings.updateExportFormat(it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    SliderRow(
                        label = "JPEG quality",
                        value = settings.jpegQuality.toFloat(),
                        valueRange = 60f..100f,
                        onChange = { settings.updateJpegQuality(it.toInt()) },
                        display = { "${it.toInt()}%" }
                    )
                    Text(
                        "Exports are always 1280 × 720 pixels.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Updates (optional)")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Installed version", style = MaterialTheme.typography.titleMedium)
                            Text(
                                BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                checking = true
                                scope.launch {
                                    updateState = UpdateChecker.check(BuildConfig.VERSION_NAME)
                                    settings.markUpdateChecked()
                                    checking = false
                                }
                            },
                            enabled = !checking
                        ) { Text(if (checking) "Checking…" else "Check") }
                    }
                    Spacer(Modifier.height(10.dp))
                    when (val state = updateState) {
                        null -> Text(
                            "Checking updates is optional and needs internet. Editing never does.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        is UpdateStatus.Failed -> Text(
                            "Could not check: ${state.message}. You can still use the app normally.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        is UpdateStatus.UpToDate -> Text(
                            "You are on the latest version (${state.current}).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        is UpdateStatus.UpdateAvailable -> Column {
                            Text(
                                "Version ${state.release.tagName} is available",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (state.release.notes.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    state.release.notes.take(400),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                openUrl(state.release.apkUrl ?: state.release.htmlUrl)
                            }) { Text("Open download") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Storage")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Image cache", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$cacheKb KB in memory · exports folder is temporary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = {
                            ImagePipeline.clearCache()
                            store.clearExportCache()
                            cacheKb = ImagePipeline.cacheSizeKb()
                        }) { Text("Clear") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("About")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Thumbnail Studio ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "An offline YouTube thumbnail editor. All rendering happens on your device with Android graphics APIs. " +
                            "Design assistance (contrast checks, palettes, layout suggestions) is rule-based — " +
                            "no generative AI models are bundled or downloaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { openUrl(UpdateChecker.REPO_URL) }) { Text("GitHub") }
                        OutlinedButton(onClick = { openUrl(UpdateChecker.RELEASES_URL) }) { Text("Releases") }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Made with Jetpack Compose · No trackers · No accounts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { }.padding(bottom = 24.dp)
            )
        }
    }
}
