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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.Templates

@Composable
fun TemplatesScreen(
    store: ProjectStore,
    onBack: () -> Unit,
    onUseTemplate: (Templates.TemplateDef) -> Unit,
    onOpenProject: (String) -> Unit
) {
    var category by remember { mutableStateOf("All") }
    val list = remember(category) { Templates.byCategory(category) }
    val userTemplates = remember { store.list(includeTemplates = true) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("Templates", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Generated on device · fully editable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ChipRow(
            options = Templates.categories,
            selected = category,
            onSelect = { category = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (userTemplates.isNotEmpty() && category == "All") {
                items(userTemplates) { summary ->
                    val project = remember(summary.id, summary.updatedAt) { store.load(summary.id) }
                    Column(Modifier.clickable { onOpenProject(summary.id) }) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        ) {
                            if (project != null) {
                                ProjectThumbnail(project, Modifier.fillMaxSize(), widthPx = 360)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            summary.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            "Saved template",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            items(list) { def ->
                Column(Modifier.clickable { onUseTemplate(def) }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    ) {
                        val preview = remember(def.id) { Templates.instantiate(def) }
                        ProjectThumbnail(preview, Modifier.fillMaxSize(), widthPx = 380)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        def.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        def.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(def.accent)
                    )
                }
            }
        }
    }
}
