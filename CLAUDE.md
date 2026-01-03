# Claude Code Project Notes

## Building

**Do NOT attempt local builds** - JAVA_HOME is not configured in this environment.

**Use GitHub Actions for all builds:**
1. Commit and push changes to GitHub
2. GitHub Actions will automatically build the APK
3. Check build status at: https://github.com/valentinyeo/android-notepad/actions

## Project Info
- Package: `com.simplenotepad`
- Min SDK: 26 (Android 8.0)
- Target SDK: 34
- Language: Kotlin
- UI: Jetpack Compose + Material 3

## Key Features
- Tabbed interface for multiple files
- Markdown formatting toolbar (Title/Subtitle/Heading dropdowns, Bold, Italic, etc.)
- Find/Replace
- Auto-save
- Theme support (Light/Dark/System)
