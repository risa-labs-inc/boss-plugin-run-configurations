package ai.rever.boss.plugin.dynamic.runconfigurations

import ai.rever.boss.plugin.api.LanguageData
import ai.rever.boss.plugin.api.RunConfigurationData
import ai.rever.boss.plugin.api.RunConfigurationDataProvider
import ai.rever.boss.plugin.api.RunConfigurationTypeData
import ai.rever.boss.plugin.scrollbar.getPanelScrollbarConfig
import ai.rever.boss.plugin.scrollbar.lazyListScrollbar
import ai.rever.boss.plugin.ui.BossTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.SimpleIcons
import compose.icons.feathericons.Terminal
import compose.icons.feathericons.Zap
import compose.icons.simpleicons.Go
import compose.icons.simpleicons.Java
import compose.icons.simpleicons.Javascript
import compose.icons.simpleicons.Kotlin
import compose.icons.simpleicons.Python
import compose.icons.simpleicons.Rust
import compose.icons.simpleicons.Typescript
import kotlinx.coroutines.CoroutineScope

/**
 * Run Configurations panel content (Dynamic Plugin).
 *
 * Displays auto-detected run configurations for the project,
 * grouped by language with official brand icons.
 */
@Composable
fun RunConfigurationsContent(
    runConfigurationDataProvider: RunConfigurationDataProvider?,
    scope: CoroutineScope,
    getWindowId: () -> String?,
    getProjectPath: () -> String?
) {
    val viewModel = remember(runConfigurationDataProvider, scope) {
        RunConfigurationsViewModel(runConfigurationDataProvider, scope, getWindowId, getProjectPath)
    }

    BossTheme {
        if (!viewModel.isAvailable()) {
            NoProviderMessage()
        } else if (!viewModel.hasProject()) {
            NoProjectMessage()
        } else {
            ConfigurationsList(viewModel)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
}

@Composable
private fun NoProviderMessage() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Run Configurations",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Run configuration provider not available",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Please ensure the host provides run configuration access",
                fontSize = 11.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun NoProjectMessage() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Code,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Gray.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No project selected",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open a project to detect run configurations",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ConfigurationsList(viewModel: RunConfigurationsViewModel) {
    val configurations by viewModel.configurations?.collectAsState() ?: return
    val isScanning by viewModel.isScanning?.collectAsState() ?: return
    val lastError by viewModel.lastError?.collectAsState() ?: return
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val listState = rememberLazyListState()

    // Filter configurations by search
    val filteredConfigurations = remember(configurations, searchQuery) {
        if (searchQuery.isBlank()) {
            configurations
        } else {
            configurations.filter { config ->
                config.name.contains(searchQuery, ignoreCase = true) ||
                        config.filePath.contains(searchQuery, ignoreCase = true) ||
                        config.language.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Group configurations by language
    val groupedConfigs = remember(filteredConfigurations) {
        filteredConfigurations.groupBy { it.language }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF2B2D30)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Error banner
            if (lastError != null) {
                ErrorBanner(
                    message = lastError!!,
                    onDismiss = viewModel::clearError
                )
            }

            // Header with scan button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Configurations",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )

                // Scan/Refresh button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = !isScanning) { viewModel.scanProject() }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.dp,
                            color = MaterialTheme.colors.primary
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Rescan project",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colors.primary.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isScanning) "Scanning..." else "Rescan",
                        fontSize = 9.sp,
                        color = MaterialTheme.colors.primary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status message
            if (statusMessage != null) {
                StatusMessage(
                    message = statusMessage!!,
                    onDismiss = viewModel::clearStatusMessage
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Content
            when {
                isScanning && configurations.isEmpty() -> {
                    // Scanning in progress
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scanning for run configurations...",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                filteredConfigurations.isEmpty() -> {
                    // No configurations found
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                if (searchQuery.isNotBlank()) Icons.Outlined.Search else Icons.Outlined.Science,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "No configurations matching \"$searchQuery\""
                                else
                                    "No run configurations found",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            if (searchQuery.isBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add main functions or scripts to your project",
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Show grouped configurations
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .lazyListScrollbar(
                                listState = listState,
                                direction = Orientation.Vertical,
                                config = getPanelScrollbarConfig()
                            ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        groupedConfigs.forEach { (language, configs) ->
                            // Language group header
                            item(key = "header-${language.name}") {
                                LanguageGroupHeader(language = language, count = configs.size)
                            }

                            // Configuration items
                            items(
                                items = configs,
                                key = { it.id }
                            ) { config ->
                                RunConfigurationItem(
                                    config = config,
                                    onRun = { viewModel.execute(config) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E1F22))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.Gray.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 11.sp,
                color = Color.White
            ),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search configurations...",
                            fontSize = 11.sp,
                            color = Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear",
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onQueryChange("") },
                tint = Color.Gray.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = Color(0xFF5C2020),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                fontSize = 10.sp,
                color = Color(0xFFFF8080),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "X",
                fontSize = 12.sp,
                color = Color(0xFFFF8080),
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun StatusMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            fontSize = 10.sp,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss",
            modifier = Modifier
                .size(12.dp)
                .clickable { onDismiss() },
            tint = MaterialTheme.colors.primary
        )
    }
}

@Composable
private fun LanguageGroupHeader(
    language: LanguageData,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            getLanguageIcon(language),
            contentDescription = language.displayName,
            modifier = Modifier.size(14.dp),
            tint = getLanguageColor(language)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = language.displayName,
            fontSize = 11.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "($count)",
            fontSize = 10.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Divider line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF4B5563))
        )
    }
}

@Composable
private fun RunConfigurationItem(
    config: RunConfigurationData,
    onRun: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onRun() },
        color = Color(0xFF3C3F43),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Config type icon
            Icon(
                getConfigTypeIcon(config.type),
                contentDescription = config.type.name,
                modifier = Modifier.size(14.dp),
                tint = getLanguageColor(config.language).copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Config info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // File path (relative)
                val relativePath = config.filePath
                    .removePrefix(config.workingDirectory)
                    .removePrefix("/")

                Text(
                    text = relativePath,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Run button
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = "Run",
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onRun() }
                    .padding(2.dp),
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LANGUAGE ICONS AND COLORS (using SimpleIcons)
// ═══════════════════════════════════════════════════════════════════════════

private fun getLanguageIcon(language: LanguageData): ImageVector {
    return when (language) {
        LanguageData.KOTLIN -> SimpleIcons.Kotlin
        LanguageData.JAVA -> SimpleIcons.Java
        LanguageData.PYTHON -> SimpleIcons.Python
        LanguageData.JAVASCRIPT -> SimpleIcons.Javascript
        LanguageData.TYPESCRIPT -> SimpleIcons.Typescript
        LanguageData.GO -> SimpleIcons.Go
        LanguageData.RUST -> SimpleIcons.Rust
        LanguageData.UNKNOWN -> FeatherIcons.Terminal
    }
}

private fun getLanguageColor(language: LanguageData): Color {
    return when (language) {
        LanguageData.KOTLIN -> Color(0xFF7F52FF)
        LanguageData.JAVA -> Color(0xFFE76F00)
        LanguageData.PYTHON -> Color(0xFF3776AB)
        LanguageData.JAVASCRIPT -> Color(0xFFF7DF1E)
        LanguageData.TYPESCRIPT -> Color(0xFF3178C6)
        LanguageData.GO -> Color(0xFF00ADD8)
        LanguageData.RUST -> Color(0xFFDEA584)
        LanguageData.UNKNOWN -> Color.Gray
    }
}

private fun getConfigTypeIcon(type: RunConfigurationTypeData): ImageVector {
    return when (type) {
        RunConfigurationTypeData.MAIN_FUNCTION -> FeatherIcons.Zap
        RunConfigurationTypeData.SCRIPT -> FeatherIcons.Terminal
        RunConfigurationTypeData.TEST -> Icons.Outlined.Science
        RunConfigurationTypeData.CUSTOM -> Icons.Outlined.Code
    }
}
