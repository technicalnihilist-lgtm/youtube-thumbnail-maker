package com.thumbnailstudio.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.ProjectSummary
import com.thumbnailstudio.app.model.Project

@Composable
fun ProjectsScreen(
    store: ProjectStore,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onDuplicate: (Project) -> Unit,
    onNew: () -> Unit
) {
    var projects by remember { mutableStateOf<List<ProjectSummary>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<ProjectSummary?>(null) }

    LaunchedEffect(refreshKey) { projects = store.list() }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("My projects", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${projects.size} saved · autosaved locally",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (projects.isEmpty()) {
                EmptyState(
                    title = "No projects yet",
                    message = "Start from a blank canvas or pick a template. Everything you make is saved on this device.",
                    emoji = "\uD83D\uDDBC\uFE0F"
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(projects, key = { it.id }) { summary ->
                        var menuOpen by remember { mutableStateOf(false) }
                        Column {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clickable { onOpenProject(summary.id) }
                            ) {
                                val project = remember(summary.id, summary.updatedAt) { store.load(summary.id) }
                                if (project != null) {
                                    ProjectThumbnail(project, Modifier.fillMaxSize(), widthPx = 360)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).clickable { onOpenProject(summary.id) }) {
                                    Text(
                                        summary.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        relativeTime(summary.updatedAt) + " · ${summary.layerCount} layers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Duplicate") },
                                        onClick = {
                                            menuOpen = false
                                            val project = store.load(summary.id)
                                            if (project != null) {
                                                onDuplicate(project)
                                                refreshKey++
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as template") },
                                        onClick = {
                                            menuOpen = false
                                            val project = store.load(summary.id)
                                            if (project != null) {
                                                store.saveAsTemplate(project, summary.name)
                                                refreshKey++
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuOpen = false
                                            pendingDelete = summary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete project?") },
            text = { Text("\"${toDelete.name}\" will be removed from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    store.delete(toDelete.id)
                    pendingDelete = null
                    refreshKey++
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
