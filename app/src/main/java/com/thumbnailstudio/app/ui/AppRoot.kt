package com.thumbnailstudio.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.SettingsStore
import com.thumbnailstudio.app.data.Templates
import com.thumbnailstudio.app.model.Background
import com.thumbnailstudio.app.model.BgType
import com.thumbnailstudio.app.model.Project
import com.thumbnailstudio.app.model.TextLayer
import com.thumbnailstudio.app.model.newId

sealed interface Route {
    data object Home : Route
    data object Templates : Route
    data object Projects : Route
    data object Settings : Route
    data class Editor(val projectId: String) : Route
}

/** Quick-start canvas presets offered on the home dashboard. */
data class Starter(val name: String, val emoji: String, val build: () -> Project)

val starters: List<Starter> = listOf(
    Starter("Dark studio", "\uD83C\uDF11") {
        Project(
            id = newId(), name = "Dark studio thumbnail",
            background = Background(BgType.GRADIENT, 0xFF0F172A.toInt(), 0xFF312E81.toInt(), 135f, vignette = 0.45f),
            layers = listOf(
                TextLayer(
                    id = newId(), text = "YOUR TITLE HERE", fontSize = 104f,
                    x = 640f, y = 360f, width = 980f, height = 220f,
                    strokeEnabled = true, strokeWidth = 8f, strokeColor = 0xFF000000.toInt()
                )
            )
        )
    },
    Starter("Neon pop", "\uD83D\uDD25") {
        Project(
            id = newId(), name = "Neon thumbnail",
            background = Background(BgType.GRADIENT, 0xFF3B0764.toInt(), 0xFFBE185D.toInt(), 45f, vignette = 0.5f),
            layers = listOf(
                TextLayer(
                    id = newId(), text = "BIG NEWS", fontSize = 128f,
                    x = 640f, y = 360f, width = 1000f, height = 240f,
                    color = 0xFFFFE066.toInt(), strokeEnabled = true, strokeWidth = 10f,
                    strokeColor = 0xFF1F0A2E.toInt(), shadowBlur = 24f
                )
            )
        )
    },
    Starter("Clean light", "\u2600\uFE0F") {
        Project(
            id = newId(), name = "Light thumbnail",
            background = Background(BgType.SOLID, 0xFFF8FAFC.toInt(), vignette = 0.08f),
            layers = listOf(
                TextLayer(
                    id = newId(), text = "SIMPLE & CLEAN", fontSize = 96f,
                    x = 640f, y = 360f, width = 1000f, height = 220f,
                    color = 0xFF0F172A.toInt(), shadowEnabled = false,
                    fontKey = "sans_bold"
                )
            )
        )
    },
    Starter("Photo ready", "\uD83D\uDCF7") {
        Project(
            id = newId(), name = "Photo thumbnail",
            background = Background(BgType.GRADIENT, 0xFF111827.toInt(), 0xFF374151.toInt(), 160f, vignette = 0.55f),
            layers = listOf(
                TextLayer(
                    id = newId(), text = "ADD YOUR PHOTO", fontSize = 92f,
                    x = 640f, y = 560f, width = 1040f, height = 180f,
                    strokeEnabled = true, strokeWidth = 8f, strokeColor = 0xFF000000.toInt(),
                    boxEnabled = true, boxColor = 0x99000000.toInt()
                )
            )
        )
    }
)

@Composable
fun ThumbnailStudioRoot(store: ProjectStore, settings: SettingsStore) {
    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    val current = stack.last()

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun openProject(project: Project) {
        store.save(project)
        store.saveThumbnail(project)
        push(Route.Editor(project.id))
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Crossfade(targetState = current, animationSpec = tween(180), label = "route") { route ->
                when (route) {
                    is Route.Home -> HomeScreen(
                        store = store,
                        onNewThumbnail = {
                            val project = starters.first().build().copy(name = "Untitled thumbnail")
                            openProject(project)
                        },
                        onStarter = { starter ->
                            openProject(starter.build())
                        },
                        onTemplates = { push(Route.Templates) },
                        onProjects = { push(Route.Projects) },
                        onSettings = { push(Route.Settings) },
                        onOpenProject = { id -> push(Route.Editor(id)) },
                        onOpenTemplate = { def -> openProject(Templates.instantiate(def)) }
                    )

                    is Route.Templates -> TemplatesScreen(
                        store = store,
                        onBack = { pop() },
                        onUseTemplate = { def -> openProject(Templates.instantiate(def)) },
                        onOpenProject = { id -> push(Route.Editor(id)) }
                    )

                    is Route.Projects -> ProjectsScreen(
                        store = store,
                        onBack = { pop() },
                        onOpenProject = { id -> push(Route.Editor(id)) },
                        onDuplicate = { project -> store.duplicate(project, project.name + " copy") },
                        onNew = {
                            openProject(starters.first().build().copy(name = "Untitled thumbnail"))
                        }
                    )

                    is Route.Settings -> SettingsScreen(
                        settings = settings,
                        store = store,
                        onBack = { pop() }
                    )

                    is Route.Editor -> {
                        val project = remember(route.projectId) { store.load(route.projectId) }
                        if (project == null) {
                            Box(Modifier.fillMaxSize().padding(24.dp)) {
                                EmptyState(
                                    title = "Project not found",
                                    message = "This project may have been deleted.",
                                    emoji = "\uD83D\uDD0D"
                                )
                            }
                        } else {
                            EditorScreen(
                                store = store,
                                settings = settings,
                                initial = project,
                                onClose = {
                                    pop()
                                },
                                onExported = { }
                            )
                        }
                    }
                }
            }
        }
    }
}
