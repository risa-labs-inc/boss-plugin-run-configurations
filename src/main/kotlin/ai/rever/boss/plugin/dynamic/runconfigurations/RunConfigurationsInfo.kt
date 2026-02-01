package ai.rever.boss.plugin.dynamic.runconfigurations

import ai.rever.boss.plugin.api.Panel.Companion.left
import ai.rever.boss.plugin.api.Panel.Companion.top
import ai.rever.boss.plugin.api.Panel.Companion.bottom
import ai.rever.boss.plugin.api.PanelId
import ai.rever.boss.plugin.api.PanelInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow

object RunConfigurationsInfo : PanelInfo {
    override val id = PanelId("run-configurations", 6)
    override val displayName = "Run Configurations"
    override val icon = Icons.Outlined.PlayArrow
    override val defaultSlotPosition = left.top.bottom
}
