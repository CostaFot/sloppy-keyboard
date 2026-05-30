# Claude Instructions

- Do not ask for confirmation before taking actions. Proceed autonomously.
- Never amend commits; always create a new commit. Commit/push only when asked.

## What this is

**slopboard** — a custom Android soft keyboard (IME) built with Jetpack Compose and
Navigation 3. The keyboard UI is rendered by Compose inside an `InputMethodService`, not a
normal Activity. It learns the user's n-grams locally (Room) and refines suggestions with an
on-device LLM (Gemma via LiteRT-LM) — see "Predictive suggestions" below.

- applicationId / namespace: `com.markedusduplicate.slopboard` (debug variant: `.debug`)
- Build variants: `debug` / `release` only (no product flavors)
- DI: Hilt. App class: `SlopboardApplication` (`@HiltAndroidApp`)

## Module layout

- `app` — the keyboard + a demo `MainActivity`
- `design` — theme/UI (`AppTheme`)
- `common`, `common-test`, `logging`, `work`, `auth`, `testing` — shared libs
  (namespaces stay `com.markedusduplicate.*`; only the app/template packages were renamed to
  slopboard)
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

The LLM layer is optional at runtime: with no model present the keyboard works on n-gram
suggestions alone. To enable it, `adb push` a `.litertlm` into
`/sdcard/Android/data/<applicationId>/files/models/` (≈2.4–3.5 GB; the `LlmEngine` loads the
first `.litertlm` it finds there). `LlmEngine` warms up once per process and caches the loaded
engine, so after pushing a new model `am force-stop` (or reinstall) to reload it.

## Architecture notes (the non-obvious bits)

### IME service — `keyboard/SlopboardKeyboardService.kt`

- The service extends `keyboard/LifecycleInputMethodService.kt` (a `ServiceLifecycleDispatcher`-
  backed `InputMethodService`, so the service itself is the `LifecycleOwner`) and also implements
  `ViewModelStoreOwner` + `SavedStateRegistryOwner`. There is no Activity host.
- `onCreateInputView()` returns a `keyboard/KeyboardComposeView.kt` (`AbstractComposeView`, so
  Compose owns its own `Recomposer`/composition) and sets the service as the view-tree lifecycle /
  view-model-store / saved-state / navigation-event-dispatcher owner on `window?.window?.decorView`.
- `onEvaluateInputViewShown()` is overridden to return `true`. Without it the keyboard is
  hidden whenever a hardware keyboard is present (e.g. an emulator with "Enable keyboard
  input"), and `onCreateInputView()` is never called.
- Text I/O goes through `KeyboardHandler` (a `@Singleton` holding a `MutableSharedFlow`
  queue). The service collects the queue and commits text/deletes via `currentInputConnection`.

### Navigation — Navigation 3

- The keyboard back stack lives in `KeyboardStateHolder.backStack` (`mutableStateListOf<Any>`),
  rendered by `NavDisplay`. A single route `KeyboardRoute` → `keyboard/main/KeyboardScreen` (the
  `NavDisplay` is kept so more screens can be added later).

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

## Predictive suggestions (DB + on-device LLM)

Two suggestion sources behind a `SuggestionSource` seam, blended in
`suggestion/SuggestionCoordinator.kt`:
the DB source shows instantly on each keystroke; the LLM source replaces it when inference returns
(`flatMapLatest`, so typing cancels stale inference). Bound via Hilt qualifiers `@DbSuggestions`
(`NgramSuggestionSource`) and `@LlmSuggestions` (`LlmSuggestionSource`).

- **Learning (no per-key hooks):** `keyboard/observe/ObservationManager.kt` diffs successive
  "text before cursor" snapshots to detect finalized words → writes n-grams / corrections to Room
  (`data/db/`: `NgramEntry`, `UserCorrection`, `AcceptedSuggestion`; insert-or-increment DAO).
  `InputContextTracker` holds the current context plus an `allowed` flag that disables learning &
  suggestions for password / `NO_SUGGESTIONS` fields. The service feeds it from `onStartInput` /
  `onUpdateSelection` / the input queue (`refreshInputContext()`).
- **`PersonalizationRepository` is the DB boundary** — every suspend fn opens with
  `withContext(dispatcherProvider.io)`, so it's main-safe regardless of caller (the suggestion VM
  runs on `Dispatchers.Main.immediate`).
- Singletons take the injected `@ApplicationCoroutineScope`, not hand-rolled scopes.

### LiteRT-LM / on-device GPU (hard-won, easy to get wrong)

`suggestion/llm/LlmEngine.kt` wraps the LiteRT-LM `Engine`. The non-obvious constraints:

- **GPU needs `<uses-native-library>` in `AndroidManifest.xml`.** The GPU delegate (ML Drift) is
  OpenCL; on Android 12+ the app can't `dlopen` the vendor `libOpenCL.so` unless declared
  (`libOpenCL.so`, `libvndksupport.so`, `libcdsprpc.so`, `libedgetpu_litert.so`, all
  `required="false"`). Without it, GPU init fails with an opaque `INTERNAL` error and silently
  falls back to CPU (~10× slower). This was the single hardest bug in the project.
- **Pin `litertlm = "0.11.0"`** — 0.12.0 regressed GPU for these Gemma builds. (0.11.0 matches AI
  Edge Gallery, the reference app we diffed against.)
- **Warm-up runs in `@ApplicationCoroutineScope`, not the suggestion flow.** Engine init takes
  seconds and `engineOrNull()` is non-blocking (returns null until ready); if it ran inside the
  coordinator's `flatMapLatest`, every keystroke would cancel and restart the load.
- `EngineConfig`: set `maxNumTokens` (KV-cache size), `cacheDir = null` (the GPU weight cache lands
  next to the model in external storage), and for the multimodal Gemma on GPU set
  `visionBackend = GPU`, `audioBackend = CPU`. Backend order tried GPU → CPU → NPU (NPU isn't
  supported by these Gemma `.litertlm` builds).

## Testing

`CoroutinesTestRule(eager = …)`: `eager = true` (default) → `UnconfinedTestDispatcher` (runs
immediately; for hot flows / fire-and-forget launches); `eager = false` → `StandardTestDispatcher`
(manual virtual-clock; for asserting debounce timing).

Code comments: KDoc on classes and public functions only, no inline narration inside function
bodies.

## Logging

`logDebug { … }` (`com.markedusduplicate.logging`) and `Timber` (tag `"KeyboardComposeView"` for the
keyboard view lifecycle). `logDebug` uses Timber's `DebugTree`, so the tag is the calling class's
simple name (e.g. `LlmEngine`, `SlopboardKeyboardService`). The native LiteRT runtime logs under
`litert` / `litert-lm`.
