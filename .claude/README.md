# Agent configuration

`settings.json` is the shared, committed Claude Code configuration for this
repository. Personal overrides belong in `settings.local.json`, which
`.gitignore` keeps out of commits.

## What the deny rules do

Claude Code has no `.claudeignore` file. Files are hidden from the agent with
`permissions.deny` rules using [gitignore pattern
syntax](https://code.claude.com/docs/en/permissions#read-and-edit); a `Read`
deny rule applies to Read, Grep, Glob, `@file` mentions, and to file commands
Claude Code recognises in Bash (`cat`, `head`, `sed`, shell redirections).

The rules here cover three categories:

1. **Build output and intermediates** — `build/intermediates`, `build/tmp`,
   `build/kotlin`, `build/classes`, packaged APK/AAB/mapping output, plus
   `.gradle/`, `.kotlin/` and the IDE caches. On a warm machine these are the
   largest directories in the tree by far and contain nothing that isn't
   derived from source, so reading them only burns context.
2. **Binary artifacts** — `*.apk`, `*.dex`, `*.jar`, `*.class`, `*.hprof`.
   Never useful as text; a heap dump in particular can be gigabytes.
3. **Secrets** — `local.properties`, keystores, signing properties,
   `google-services.json`, `.env` files.

## What is deliberately *not* denied

* `**/build/reports/**` and `**/build/test-results/**` — Detekt, Android Lint
  and JUnit write their output there, and the agent needs to read it to
  diagnose a failing `./gradlew lint`, `./gradlew detekt` or
  `./run_focused_tests.sh` run.
* `**/build/generated/**` — KSP/Room generated sources are occasionally worth
  inspecting when a schema migration misbehaves.
* `app/schemas/**` — the exported Room schema JSON is a tracked source of
  truth (see AGENTS.md, "Database Integrity Protocol").

## Relationship to `.gitignore`

`.gitignore` already keeps build output out of the repository, and Grep honours
it when searching. The deny rules add the cases `.gitignore` cannot cover: a
direct `Read` of an ignored path, and secrets that must stay unreadable whether
or not they are tracked.
