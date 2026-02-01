package ai.rever.boss.plugin.dynamic.runconfigurations

import ai.rever.boss.plugin.api.LanguageData
import ai.rever.boss.plugin.api.RunConfigurationData
import ai.rever.boss.plugin.api.RunConfigurationDataProvider
import ai.rever.boss.plugin.api.RunConfigurationTypeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Run Configurations panel.
 *
 * Manages run configuration detection and execution.
 */
class RunConfigurationsViewModel(
    private val runConfigurationDataProvider: RunConfigurationDataProvider?,
    private val scope: CoroutineScope,
    private val getWindowId: () -> String?,
    private val getProjectPath: () -> String?
) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedConfigId = MutableStateFlow<String?>(null)
    val selectedConfigId: StateFlow<String?> = _selectedConfigId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _filterType = MutableStateFlow<RunConfigurationTypeData?>(null)
    val filterType: StateFlow<RunConfigurationTypeData?> = _filterType.asStateFlow()

    private val _filterLanguage = MutableStateFlow<LanguageData?>(null)
    val filterLanguage: StateFlow<LanguageData?> = _filterLanguage.asStateFlow()

    /**
     * Get the detected configurations flow.
     */
    val configurations: StateFlow<List<RunConfigurationData>>?
        get() = runConfigurationDataProvider?.detectedConfigurations

    /**
     * Get the scanning state flow.
     */
    val isScanning: StateFlow<Boolean>?
        get() = runConfigurationDataProvider?.isScanning

    /**
     * Get the last error flow.
     */
    val lastError: StateFlow<String?>?
        get() = runConfigurationDataProvider?.lastError

    /**
     * Initialize by scanning the project.
     */
    fun initialize() {
        scanProject()
    }

    /**
     * Scan the project for run configurations.
     */
    fun scanProject() {
        val projectPath = getProjectPath()
        val windowId = getWindowId()
        if (projectPath != null && windowId != null) {
            scope.launch {
                try {
                    runConfigurationDataProvider?.scanProject(projectPath, windowId)
                    _statusMessage.value = "Scan complete"
                } catch (e: Exception) {
                    _statusMessage.value = "Scan failed: ${e.message}"
                }
            }
        }
    }

    /**
     * Execute a run configuration.
     */
    fun execute(config: RunConfigurationData) {
        val windowId = getWindowId()
        if (windowId != null) {
            scope.launch {
                try {
                    _statusMessage.value = "Running ${config.name}..."
                    runConfigurationDataProvider?.execute(config, windowId)
                    _statusMessage.value = "Started: ${config.name}"
                } catch (e: Exception) {
                    _statusMessage.value = "Failed to run: ${e.message}"
                }
            }
        }
    }

    /**
     * Select a configuration.
     */
    fun select(configId: String) {
        _selectedConfigId.value = configId
    }

    /**
     * Update search query.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Set type filter.
     */
    fun setTypeFilter(type: RunConfigurationTypeData?) {
        _filterType.value = type
    }

    /**
     * Set language filter.
     */
    fun setLanguageFilter(language: LanguageData?) {
        _filterLanguage.value = language
    }

    /**
     * Clear status message.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Clear error.
     */
    fun clearError() {
        scope.launch {
            runConfigurationDataProvider?.clearError()
        }
    }

    /**
     * Check if the provider is available.
     */
    fun isAvailable(): Boolean {
        return runConfigurationDataProvider != null
    }

    /**
     * Check if there's a project loaded.
     */
    fun hasProject(): Boolean {
        val projectPath = getProjectPath()
        return projectPath != null && projectPath.isNotEmpty()
    }
}
