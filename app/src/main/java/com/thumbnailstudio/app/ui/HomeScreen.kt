package com.thumbnailstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.ProjectSummary
import com.thumbnailstudio.app.data.Templates
import com.thumbnailstudio.app.ui.theme.Cyan
import com.thumbnailstudio.app.ui.theme.Rose
import com.thumbnailstudio.app.ui.theme.Violet

@Composable
fun HomeScreen(
    store: ProjectStore,
    onNewThumbnail: () -> Unit,
    onStarter: (Starter) -> Unit,
    onTemplates: () -> Unit,
    onProjects: () -> Unit,
    onSettings: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenTemplate: (Templates.TemplateDef) -> Unit
) {
    var recent by remember { mutableStateOf<List<ProjectSummary>>(emptyList()) }
    LaunchedEffect(Unit) { recent = store.list().take(10) }
    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Thumbnail Studio",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Pro thumbnails, 100% offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hero call to action
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Violet, Color(0xFF9C27B0), Rose)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                TagPill("1280 × 720 · 16:9", color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Make a thumbnail\nthat gets clicked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Text effects, cutouts, gradients and templates — all on-device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onNewThumbnail,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2A1B60)
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New thumbnail", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Quick start")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(starters) { starter ->
                GlassCard(modifier = Modifier.width(150.dp), onClick = { onStarter(starter) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(starter.emoji, fontSize = 26.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(starter.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Blank canvas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Recent projects", Modifier.weight(1f))
            Text(
                "See all",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onProjects() }.padding(bottom = 6.dp)
            )
        }
        if (recent.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Nothing saved yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your projects autosave here, so you can close the app and pick up later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recent) { summary ->
                    Column(Modifier.width(200.dp).clickable { onOpenProject(summary.id) }) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        ) {
                            val project = remember(summary.id, summary.updatedAt) { store.load(summary.id) }
                            if (project != null) {
                                ProjectThumbnail(project, Modifier.fillMaxSize(), widthPx = 360)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            summary.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            relativeTime(summary.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Popular templates")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Templates.all.take(6)) { def ->
                Column(Modifier.width(210.dp).clickable { onOpenTemplate(def) }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    ) {
                        val preview = remember(def.id) { Templates.instantiate(def) }
                        ProjectThumbnail(preview, Modifier.fillMaxSize(), widthPx = 380)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(def.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(
                        def.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(def.accent)
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.weight(1f), onClick = onTemplates) {
                Column(Modifier.padding(16.dp)) {
                    Text("\uD83C\uDFA8", fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Templates", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "15 ready designs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            GlassCard(Modifier.weight(1f), onClick = onProjects) {
                Column(Modifier.padding(16.dp)) {
                    Text("\uD83D\uDDC2\uFE0F", fontSize = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("My projects", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${recent.size} saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Cyan)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Works in airplane mode. No account, no cloud, no tracking.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 7 -> "$days d ago"
        else -> android.text.format.DateFormat.format("d MMM", timestamp).toString()
    }
}
