# Claude Instructions

- Do not ask for confirmation before taking actions. Proceed autonomously.
- Never amend commits; always create a new commit. Commit/push only when asked.

## What this is

**slopboard** — a custom Android soft keyboard (IME) built with Jetpack Compose and
Navigation 3. The keyboard UI is rendered by Compose inside an `InputMethodService`, not a
normal Activity.

- applicationId / namespace: `com.feelsokman.slopboard` (debug variant: `.debug`)
- Build variants: `debug` / `release` only (no product flavors)
- DI: Hilt. App class: `SlopboardApplication` (`@HiltAndroidApp`)

## Module layout

- `app` — the keyboard + a demo `MainActivity`
- `design` — theme/UI (`AppTheme`)
- `common`, `common-test`, `logging`, `work`, `auth`, `testing` — shared libs
  (namespaces stay `com.feelsokman.*`; only the app/template packages were renamed to slopboard)
- `build-logic/convention` — Gradle convention plugins (`application.common`,
  `application.compose.common`, `hilt.common`, `library.common`, `library.compose.common`)

## Build / run

Gradle uses the Android Studio JBR — **`JAVA_HOME` must be set** or `./gradlew` fails with
"JAVA_HOME is not set". The user sets it themselves.

- Compile: `./gradlew :app:compileDebugKotlin`
- Build APK: `./gradlew :app:assembleDebug`
- Install: `./gradlew :app:installDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`

To actually use the keyboard after install: enable it in Settings → System → Languages &
input → On-screen keyboards, then select it as the active IME.

## Architecture notes (the non-obvious bits)

### IME service — `keyboard/SlopboardKeyboardService.kt`

- `onCreateInputView()` builds the Compose view by hand in `keyboard/KeyboardCompose.kt`
  (`createKeyboardComposeView`): a `ComposeView` wired to a `CustomLifecycleOwner` + a manual
  `Recomposer`, with `DisposeOnDetachedFromWindow`. There is no Activity host.
- `onEvaluateInputViewShown()` is overridden to return `true`. Without it the keyboard is
  hidden whenever a hardware keyboard is present (e.g. an emulator with "Enable keyboard
  input"), and `onCreateInputView()` is never called.
- Text I/O goes through `KeyboardHandler` (a `@Singleton` holding a `MutableSharedFlow`
  queue). The service collects the queue and commits text/deletes via `currentInputConnection`.

### Navigation — Navigation 3

- The keyboard back stack lives in `KeyboardStateHolder.backStack` (`mutableStateListOf<Any>`),
  rendered by `NavDisplay`. Screens: `keyboard/first` and `keyboard/second`.

### Retained view models — `retain/`

- `RetainedViewModel` (base, implements `RetainObserver`, owns a `viewModelScope`) +
  `rememberRetainedViewModel { context -> … }` (factory lambda, backed by Compose's
  `retain {}`) + `RetainDecorator` (per-nav-entry `RetainedValuesStore`, clears on pop).
- Resolve VMs from Hilt inside the factory lambda. **Match the accessor to the entry point's
  component:**
  - Keyboard VMs use `@InstallIn(SingletonComponent::class)` entry points → resolve with
    `EntryPointAccessors.fromApplication(context, …)` (the service/view context isn't an
    Activity, so `EntryPoints.get(context, …)` would hit the ServiceComponent and miss them).
  - The Activity sample uses an `ActivityComponent` entry point → `EntryPoints.get(context, …)`.
- Don't give multiple `@EntryPoint` interfaces a shared generic supertype — Hilt funnels them
  into one component and can't implement the same generic interface twice. Use a plain
  interface with its own method per VM.

## Logging

`logDebug { … }` (`com.feelsokman.logging`) and `Timber` (tag `"KeyboardComposeView"` for the
keyboard view lifecycle).
