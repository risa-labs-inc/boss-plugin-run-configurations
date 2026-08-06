# BOSS Run Configurations

Auto-detected ways to run the open project, in the left sidebar.

Scans the project through the host's `RunConfigurationDataProvider` and lists what it finds -
main functions, scripts, test entry points - grouped by language and runnable in one click.

This complements the run dropdown in the top bar rather than duplicating it: the dropdown shows
run **history**, this panel shows **auto-detected** configurations.

## What it does

- **Scans on open** and detects runnable entry points from the project source.
- **Groups by language**, each with its own icon and brand colour: Kotlin, Java, Python,
  JavaScript, TypeScript, Go, Rust, and Unknown.
- **Search** by name, and filter by configuration type or by language.
- **Run in one click**, with a status message that moves from "Running X" to "Started: X".
- **Distinct empty states** for no provider, no project open, and nothing detected, plus a
  dismissable error banner.

## MCP tools

| Tool | Purpose |
|---|---|
| `run_config_list` | Rescan the project and list id, type, language, name and `file:line` |
| `run_config_run` | Execute a configuration by id |

Unlike most panel-backed tools, these hold the provider directly and work whether or not the
panel is open.

**`run_config_run` executes code from the project and is not permission-gated.** Anything an
agent can reach through this tool runs with your user's privileges.

## Requirements

- BOSS >= 9.2.20, boss-plugin-api >= 1.0.20
- `runConfigurationDataProvider` is load-bearing: without it the panel shows a no-provider
  message and both tools return an error.
- No external binaries directly, though the host runner needs whatever toolchain a given
  configuration calls for.

Detection and execution both live in the host provider. This plugin is the UI over it.

## Build

```bash
./gradlew buildPluginJar
cp build/libs/boss-plugin-run-configurations-*.jar ~/.boss/plugins/
```

See [AGENTS.md](AGENTS.md) for architecture and conventions.

## License

Proprietary - Risa Labs Inc.
