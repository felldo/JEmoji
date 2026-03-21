# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build

```bash
# Build all subprojects
./gradlew build

# Build only the main jemoji library
./gradlew :jemoji:build

# Build only the languages module
./gradlew :jemoji-languages:build
```

### Generate emoji data

The `Emojis.java` constants class and `serializedEmojis.ser` resource are auto-generated — they are not hand-written.

```bash
# Generate emoji data (fetches from Unicode, Discord, Slack, GitHub)
# Also regenerates public/ JSON files and copies jemoji.jar to libs/
./gradlew generate

# Full regeneration including all translation/description files from CLDR
./gradlew generateAll
```

The `generate` task must be run before first compile if `jemoji/build/generated/jemoji/net/fellbaum/jemoji/Emojis.java` does not exist. The task is in the `jemoji` Gradle group.

After `build`, the jar is automatically copied to `libs/jemoji.jar` (used as a buildscript classpath dependency in `build.gradle.kts`).

### Tests

```bash
# Run all tests
./gradlew test

# Run tests for a specific subproject
./gradlew :jemoji:test
./gradlew :jemoji-languages:test

# Run a single test class
./gradlew :jemoji:test --tests "net.fellbaum.jemoji.EmojiManagerTest"

# Run a single test method
./gradlew :jemoji:test --tests "net.fellbaum.jemoji.EmojiManagerTest.extractEmojisInOrder"
```

### Benchmarks

Benchmark sources live in `jemoji/src/jmh/java/`.

```bash
./gradlew :jemoji:jmh
```

### Publishing

```bash
# Publish all subprojects to staging
./gradlew publishAll

# Publish a single subproject
./gradlew :jemoji:publish
```

Publishing requires `JEMOJI_SINGING_SECRET_KEY_RING_FILE` to be set in Gradle properties.

## Architecture

### Module structure

This is a Gradle multi-project build with two publishable subprojects:

- **`jemoji`** — Core library (~600 KB). Contains `EmojiManager`, `Emoji`, `EmojiLoader`, and all emoji utility logic. Emoji data is stored as a serialized Java object at `jemoji/src/main/resources/jemoji/serializedEmojis.ser`.
- **`jemoji-languages`** — Optional language module (~13 MB). Provides descriptions and keywords in 160+ languages via `EmojiLanguage` enum. Loaded via Java `ServiceLoader` (`ResourceFilesProvider` SPI).
- **`buildSrc`** — Shared Gradle convention plugins (`myproject.java-conventions.gradle.kts`, `myproject.library-conventions.gradle.kts`).

### Code generation pipeline (root `build.gradle.kts`)

The root `build.gradle.kts` is the code generation engine — it is a large Kotlin file (not a library source file) that:

1. Fetches emoji data from the Unicode GitHub repo (`unicode-org/unicodetools`), Discord (via HtmlUnit browser automation), and Slack.
2. Fetches alias data from GitHub's emoji API and iamcal emoji data.
3. Fetches CLDR translation files (descriptions, keywords) for 160+ languages from GitHub.
4. Uses **JavaPoet** to generate `Emojis.java` (type-safe emoji constants).
5. Serializes all emoji data with **Kryo** into `serializedEmojis.ser`.
6. Outputs public-facing JSON files (descriptions, keywords) into the `public/` directory.
7. Outputs translation files into `emoji_source_files/description/`.

Source files from external services are cached in `emoji_source_files/` (e.g., `discord-emoji-definition.json`, `github-emoji-definition.json`).

### Runtime emoji loading (`EmojiManager`)

`EmojiManager` uses lazy double-checked locking to initialize emoji data on first use:
- Unicode emoji maps are initialized on first call to any lookup/extraction method.
- Alias maps are initialized separately on first alias-related call.
- Language descriptions/keywords are lazily loaded per language on first access.

The `jemoji-languages` module integrates via `ServiceLoader<ResourceFilesProvider>` — the main `jemoji` module detects whether the language module is on the classpath at class-loading time.

### Multi-Release JAR

Both `jemoji` and `jemoji-languages` are built as Multi-Release JARs targeting Java 8 with a Java 9 overlay (`META-INF/versions/9/`) that provides `module-info.java` for JPMS support.

### Key classes

- `EmojiManager` — Public API for all emoji operations (extract, replace, remove, lookup by unicode/alias/HTML/URL).
- `Emoji` — Immutable value class representing a single emoji with all its metadata. Implements `Serializable`.
- `EmojiLoader` — Handles deserialization of emoji data and language file loading.
- `InternalEmojiUtils` — Internal string/codepoint utilities shared across the package.
- `InternalCodepointSequence` — Internal key type for alias lookup maps (handles colon normalization).
- `Emojis` — Auto-generated class with type-safe constants (e.g., `Emojis.THUMBS_UP`).

### Data flow summary

```
Unicode/Discord/Slack/GitHub → build.gradle.kts (generate task)
  → Emojis.java (generated source, compiled into jemoji)
  → serializedEmojis.ser (resource, loaded at runtime by EmojiManager)
  → public/*.json (public JSON files)
  → jemoji-languages/src/main/resources/**/*.ser (language data files)
```
