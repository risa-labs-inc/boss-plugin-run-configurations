package ai.rever.boss.plugin.dynamic.runconfigurations

import ai.rever.boss.plugin.api.PanelComponentWithUI
import ai.rever.boss.plugin.api.PanelInfo
import ai.rever.boss.plugin.api.RunConfigurationDataProvider
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope

/**
 * Run Configurations panel component (Dynamic Plugin)
 *
 * Provides run configuration detection and execution functionality.
 */
class RunConfigurationsComponent(
    ctx: ComponentContext,
    override val panelInfo: PanelInfo,
    private val runConfigurationDataProvider: RunConfigurationDataProvider?,
    private val scope: CoroutineScope,
    private val getWindowId: () -> String?,
    private val getProjectPath: () -> String?
) : PanelComponentWithUI, ComponentContext by ctx {

    @Composable
    override fun Content() {
        RunConfigurationsContent(
            runConfigurationDataProvider = runConfigurationDataProvider,
            scope = scope,
            getWindowId = getWindowId,
            getProjectPath = getProjectPath
        )
    }
}
