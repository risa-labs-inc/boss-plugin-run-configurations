package ai.rever.boss.plugin.dynamic.runconfigurations

import ai.rever.boss.plugin.api.DynamicPlugin
import ai.rever.boss.plugin.api.PluginContext

/**
 * Run Configurations dynamic plugin - Loaded from external JAR.
 *
 * Auto-detect and run project configurations
 */
class RunConfigurationsDynamicPlugin : DynamicPlugin {
    override val pluginId: String = "ai.rever.boss.plugin.dynamic.runconfigurations"
    override val displayName: String = "Run Configurations (Dynamic)"
    override val version: String = "1.0.0"
    override val description: String = "Auto-detect and run project configurations"
    override val author: String = "Risa Labs"
    override val url: String = "https://github.com/risa-labs-inc/boss-plugin-run-configurations"

    override fun register(context: PluginContext) {
        context.panelRegistry.registerPanel(RunConfigurationsInfo) { ctx, panelInfo ->
            RunConfigurationsComponent(ctx, panelInfo)
        }
    }
}
