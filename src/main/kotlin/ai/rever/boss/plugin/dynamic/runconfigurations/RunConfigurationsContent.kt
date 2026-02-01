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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.Orientation
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
import kotlinx.coroutines.CoroutineScope

/**
 * Run Configurations panel content (Dynamic Plugin).
 *
 * Displays auto-detected run configurations for the project.
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
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Project Open",
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
    val selectedConfigId by viewModel.selectedConfigId.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterLanguage by viewModel.filterLanguage.collectAsState()
    val listState = rememberLazyListState()

    // Filter configurations
    val filteredConfigurations = remember(configurations, searchQuery, filterType, filterLanguage) {
        configurations.filter { config ->
            val matchesSearch = searchQuery.isEmpty() ||
                config.name.contains(searchQuery, ignoreCase = true) ||
                config.filePath.contains(searchQuery, ignoreCase = true)
            val matchesType = filterType == null || config.type == filterType
            val matchesLanguage = filterLanguage == null || config.language == filterLanguage
            matchesSearch && matchesType && matchesLanguage
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar
            ConfigurationsToolbar(
                searchQuery = searchQuery,
                onSearchChange = viewModel::updateSearchQuery,
                onRefresh = viewModel::scanProject,
                isScanning = isScanning
            )

            Divider(color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f))

            // Error message
            if (lastError != null) {
                ErrorMessage(
                    message = lastError!!,
                    onDismiss = viewModel::clearError
                )
            }

            // Status message
            if (statusMessage != null) {
                StatusMessage(
                    message = statusMessage!!,
                    onDismiss = viewModel::clearStatusMessage
                )
            }

            // Loading indicator
            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colors.primary
                )
            }

            // Configurations list
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredConfigurations.isEmpty()) {
                    EmptyMessage(
                        isSearching = searchQuery.isNotEmpty() || filterType != null || filterLanguage != null,
                        isScanning = isScanning
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .lazyListScrollbar(listState, Orientation.Vertical, getPanelScrollbarConfig())
                    ) {
                        items(
                            items = filteredConfigurations,
                            key = { it.id }
                        ) { config ->
                            ConfigurationItem(
                                config = config,
                                isSelected = config.id == selectedConfigId,
                                onSelect = { viewModel.select(config.id) },
                                onRun = { viewModel.execute(config) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigurationsToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    isScanning: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search field
        Row(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colors.background.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search configurations...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { onSearchChange("") },
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Refresh button
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(28.dp),
            enabled = !isScanning
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Scan Project",
                modifier = Modifier.size(16.dp),
                tint = if (isScanning) MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                       else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.error.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colors.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            fontSize = 11.sp,
            color = MaterialTheme.colors.error,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colors.error
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
            .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            fontSize = 11.sp,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
private fun EmptyMessage(isSearching: Boolean, isScanning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Outlined.Search else Icons.Outlined.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isScanning -> "Scanning project..."
                isSearching -> "No matching configurations"
                else -> "No configurations found"
            },
            fontSize = 12.sp,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
        )
        if (!isSearching && !isScanning) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Click refresh to scan the project",
                fontSize = 10.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ConfigurationItem(
    config: RunConfigurationData,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRun: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else MaterialTheme.colors.background
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type icon
        Icon(
            imageVector = getTypeIcon(config.type),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = getLanguageColor(config.language)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Configuration info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = config.filePath.substringAfterLast('/'),
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Language badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(getLanguageColor(config.language).copy(alpha = 0.1f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = config.language.displayName,
                fontSize = 9.sp,
                color = getLanguageColor(config.language),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Run button
        IconButton(
            onClick = onRun,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Run",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/**
 * Get the icon for a configuration type.
 */
private fun getTypeIcon(type: RunConfigurationTypeData): ImageVector {
    return when (type) {
        RunConfigurationTypeData.MAIN_FUNCTION -> Icons.Outlined.PlayArrow
        RunConfigurationTypeData.SCRIPT -> Icons.Outlined.Description
        RunConfigurationTypeData.TEST -> Icons.Outlined.Science
        RunConfigurationTypeData.CUSTOM -> Icons.Outlined.Settings
    }
}

/**
 * Get the color for a language.
 */
@Composable
private fun getLanguageColor(language: LanguageData): Color {
    return when (language) {
        LanguageData.KOTLIN -> Color(0xFF7F52FF)
        LanguageData.JAVA -> Color(0xFFE76F00)
        LanguageData.PYTHON -> Color(0xFF3776AB)
        LanguageData.JAVASCRIPT -> Color(0xFFF7DF1E)
        LanguageData.TYPESCRIPT -> Color(0xFF3178C6)
        LanguageData.GO -> Color(0xFF00ADD8)
        LanguageData.RUST -> Color(0xFFDEA584)
        LanguageData.UNKNOWN -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
    }
}
