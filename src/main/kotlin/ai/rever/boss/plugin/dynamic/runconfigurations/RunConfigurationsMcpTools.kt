package ai.rever.boss.plugin.dynamic.runconfigurations

import ai.rever.boss.plugin.api.McpToolDefinition
import ai.rever.boss.plugin.api.McpToolHandler
import ai.rever.boss.plugin.api.McpToolProvider
import ai.rever.boss.plugin.api.McpToolResult
import ai.rever.boss.plugin.api.RunConfigurationDataProvider

/**
 * MCP tools contributed by the Run Configurations plugin: list auto-detected run
 * configurations for the current project and execute one. Registered in
 * [RunConfigurationsDynamicPlugin.register]; removed automatically on disable/unload.
 */
internal class RunConfigurationsMcpToolProvider(
    override val providerId: String,
    private val provider: RunConfigurationDataProvider?,
    private val getWindowId: () -> String?,
    private val getProjectPath: () -> String?,
) : McpToolProvider {

    override fun tools(): List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "run_config_list",
            description = "List auto-detected run configurations for the current project (id, name, type, file).",
            handler = McpToolHandler {
                val p = provider ?: return@McpToolHandler unavailable()
                val projectPath = getProjectPath()
                if (projectPath != null) {
                    p.scanProject(projectPath, getWindowId() ?: "unknown")
                }
                val configs = p.detectedConfigurations.value
                if (configs.isEmpty()) McpToolResult("No run configurations detected.")
                else McpToolResult(configs.joinToString("\n") { c ->
                    "${c.id}  [${c.type}/${c.language.displayName}]  ${c.name}  (${c.filePath}:${c.lineNumber})"
                })
            },
        ),
        McpToolDefinition(
            name = "run_config_run",
            description = "Execute a detected run configuration by its id (from run_config_list).",
            inputSchema = ID_SCHEMA,
            readOnly = false,
            handler = McpToolHandler { args ->
                val p = provider ?: return@McpToolHandler unavailable()
                val id = args.string("id")
                    ?: return@McpToolHandler McpToolResult("Missing required argument: id", isError = true)
                val config = p.detectedConfigurations.value.firstOrNull { it.id == id }
                    ?: return@McpToolHandler McpToolResult("No run configuration with id $id", isError = true)
                p.execute(config, getWindowId() ?: "unknown")
                McpToolResult("Started run configuration ${config.name}.")
            },
        ),
    )

    private fun unavailable(): McpToolResult =
        McpToolResult("Run configuration provider unavailable in this context.", isError = true)

    private companion object {
        const val ID_SCHEMA =
            """{"type":"object","properties":{"id":{"type":"string","description":"Run configuration id."}},"required":["id"]}"""
    }
}
