package ai.rever.boss.plugin.dynamic.runconfigurations

import ai.rever.boss.plugin.api.DynamicPlugin
import ai.rever.boss.plugin.api.PluginContext
import ai.rever.boss.plugin.api.RunConfigurationDataProvider
import kotlinx.coroutines.CoroutineScope

/**
 * Run Configurations dynamic plugin - Loaded from external JAR.
 *
 * Auto-detect and run project configurations for multiple languages.
 */
class RunConfigurationsDynamicPlugin : DynamicPlugin {
    override val pluginId: String = "ai.rever.boss.plugin.dynamic.runconfigurations"
    override val displayName: String = "Run Configurations (Dynamic)"
    override val version: String = "1.0.2"
    override val description: String = "Auto-detect and run project configurations"
    override val author: String = "Risa Labs"
    override val url: String = "https://github.com/risa-labs-inc/boss-plugin-run-configurations"

    private var runConfigurationDataProvider: RunConfigurationDataProvider? = null
    private var pluginScope: CoroutineScope? = null
    private var getWindowId: () -> String? = { null }
    private var getProjectPath: () -> String? = { null }

    override fun register(context: PluginContext) {
        // Capture providers from context
        runConfigurationDataProvider = context.runConfigurationDataProvider
        pluginScope = context.pluginScope
        getWindowId = { context.windowId }
        getProjectPath = { context.projectPath }

        context.panelRegistry.registerPanel(RunConfigurationsInfo) { ctx, panelInfo ->
            RunConfigurationsComponent(
                ctx = ctx,
                panelInfo = panelInfo,
                runConfigurationDataProvider = runConfigurationDataProvider,
                scope = pluginScope ?: error("Plugin scope not available"),
                getWindowId = getWindowId,
                getProjectPath = getProjectPath
            )
        }
    }
}
