# Claude Instructions

- Do not ask for confirmation before taking actions. Proceed autonomously.
- Never amend commits; always create a new commit. Commit/push only when asked.

## What this is

**slopboard** — an Android **AI-slop detector**. A floating "Clippy" mascot lives in a system
overlay over every app; summoning it reads the text on the current screen and (eventually) judges
whether that content is AI-generated "slop". Screen reading is on-device: a screenshot is
transcribed by Gemma via LiteRT-LM (OCR). There's also an experimental ambient **screen agent** that
suggests the next useful action on whatever app is in front.

The UI is Jetpack Compose, but it's rendered into `WindowManager` overlay windows from a plain
`Service` (and driven by an `AccessibilityService`), **not** a normal Activity — the only Activity
is
a small setup screen (`MainActivity`) for granting permissions and starting/stopping the overlay.

> History: this started as a custom soft-keyboard (IME) and was pivoted to the slop detector. The
> keyboard, its predictive-suggestion engine (dictionary + n-gram learning), and the Room DB have
> been removed. Only the on-device LLM layer (`LlmEngine`) survives from that era, reused for OCR
> and
> the agent. If you find lingering keyboard references, they're stragglers worth cleaning up.

- applicationId / namespace: `com.markedusduplicate.slopboard` (debug variant: `.debug`)
- Build variants: `debug` / `release` only (no product flavors)
- DI: Hilt. App class: `SlopboardApplication` (`@HiltAndroidApp`)

## Module layout

- `app` — the slop detector (overlay + accessibility services) + the setup `MainActivity`
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

To use it after install, open the app and work through `MainActivity`'s setup screen: enable the
accessibility service (screen reading), grant draw-over-apps, then start Clippy. A left→right swipe
on the left-edge tab summons the mascot.

The on-device LLM is required for OCR (the in-use screen reader) and the agent, but optional to
*launch*: with no model present, summoning Clippy reports it has no brain yet. To enable it,
`adb push` a `.litertlm` into `/sdcard/Android/data/<applicationId>/files/models/` (≈2.4–3.5 GB; the
`LlmEngine` loads the first `.litertlm` it finds there). `LlmEngine` warms up once per process and
caches the loaded engine, so after pushing a new model `am force-stop` (or reinstall) to reload it.

## Architecture notes (the non-obvious bits)

### Overlay service — `clippy/ClippyOverlayService.kt`

- A plain started `Service` (no Activity host) that hosts three `WindowManager` overlay windows: the
  draggable mascot (`clippy/ClippyComposeView.kt`, an emoji + speech bubble), the always-present
  left-edge summon tab (`clippy/ClippyEdgeHandleView.kt`), and the agent's full-screen highlight
  overlay (`agent/AgentOverlayView.kt`).
- An overlay service has no lifecycle/decor-view callbacks, so the service implements
  `LifecycleOwner` + `ViewModelStoreOwner` + `SavedStateRegistryOwner` itself, drives its own
  `LifecycleRegistry` to RESUMED, and sets the view-tree owners directly on each overlay view — all
  required for Compose to compose and recompose.
- `clippy/ClippyEdgeHandleView.kt` declares `setSystemGestureExclusionRects` so its left→right swipe
  isn't eaten by the Android 10+ system back gesture (exclusion budget is ~200dp/edge, so the tab is
  short).
- Requires the draw-over-apps permission (checked in `onCreate`) and the accessibility service
  (for reading the screen). Started/stopped from `MainActivity`'s setup screen.

### Screen reading — `slop/` + `accessibility/`

- `slop/ScreenTextReader` is the seam (returns `slop/ScreenReadResult`). Two impls behind Hilt
  qualifiers in `di/ScreenTextModule.kt`:
    - **`OcrScreenTextReader`** (`@OcrScreenText`, **in use**): grabs a screenshot via
      `accessibility/ScreenshotCapturer` and asks `LlmEngine.generateWithImage` to transcribe it
      (`suggestion/llm/OcrPrompt`). A screenshot is the visible viewport only, so it captures just
      what
      the user sees. Hard-requires a loaded model.
    - **`AccessibilityScreenTextReader`** (`@AccessibilityScreenText`): a placeholder for pulling
      text
      straight out of the a11y node tree (faster, no model). Not implemented.
- `accessibility/SlopboardAccessibilityService` registers the `ScreenshotCapturer` (only an
  `AccessibilityService` can call `takeScreenshot`) and publishes visible window text to
  `accessibility/ScreenContextHolder`. The user must enable it under Settings → Accessibility;
  capture stays on-device.
- `slop/ContentExtractor` (+ `ContentExtractionPrompt`) is dormant: it would isolate the main
  post/article text from a noisy capture **verbatim** (the model selects which captured lines are
  content; it never rewrites them, which would bias detection toward "AI") before handing it to the
  detector.

### Slop detection — `slop/AiDetectorRepository.kt`

- The provider-agnostic boundary: callers hand it the on-screen text and get back a
  `slop/SlopVerdict`.
  Currently a **placeholder** that reports the backend isn't wired yet — it's not on the live
  overlay
  path. The first planned backend is Pangram, a Retrofit service in `di/NetworkModule.kt`
  authenticated with `BuildConfig.AI_DETECTOR_API_KEY` (resolved from env or `local.properties`).

### Ambient screen agent — `agent/`

- `agent/AgentEngine` (app-singleton, runs on the `@ApplicationCoroutineScope`) drives a guided
  loop:
  read the foreground app's actionable elements (`agent/ScreenController`, implemented by the
  accessibility service), ask `LlmEngine` for the single most useful next action over that element
  list (`agent/AgentPrompt` → parsed by `agent/AgentAction`), and surface it as
  `AgentState.Suggest` — Clippy highlights the target and offers to do it. **Never auto-acts**:
  every
  step waits for the user's tap.
- The LLM picks elements by their snapshot `index` (`agent/ActionableNode`), never by pixel
  coordinates — the accessibility tree is the action space. Prompts/parsing are pure (no Android /
  LiteRT types) so they're unit-testable.

### LiteRT-LM / on-device GPU (hard-won, easy to get wrong)

`suggestion/llm/LlmEngine.kt` wraps the LiteRT-LM `Engine`. The non-obvious constraints:

- **GPU needs `<uses-native-library>` in `AndroidManifest.xml`.** The GPU delegate (ML Drift) is
  OpenCL; on Android 12+ the app can't `dlopen` the vendor `libOpenCL.so` unless declared
  (`libOpenCL.so`, `libvndksupport.so`, `libcdsprpc.so`, `libedgetpu_litert.so`, all
  `required="false"`). Without it, GPU init fails with an opaque `INTERNAL` error and silently
  falls back to CPU (~10× slower). This was the single hardest bug in the project.
- **Pin `litertlm = "0.11.0"`** — 0.12.0 regressed GPU for these Gemma builds. (0.11.0 matches AI
  Edge Gallery, the reference app we diffed against.)
- **Warm-up runs in `@ApplicationCoroutineScope`.** Engine init takes seconds and `engineOrNull()`
  is non-blocking (returns null until ready), so callers degrade gracefully while it loads instead
  of
  blocking.
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
overlay view lifecycle). `logDebug` uses Timber's `DebugTree`, so the tag is the calling class's
simple name (e.g. `LlmEngine`, `ClippyOverlayService`). The native LiteRT runtime logs under
`litert` / `litert-lm`.
