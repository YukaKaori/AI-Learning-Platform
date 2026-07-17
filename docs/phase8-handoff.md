# Phase 8 Handoff — Premium Glass Experience

**Status: Steps 1 and 2 of Phase 8 are COMPLETE** as of 2026-07-17. This
document is the resume point for **Step 3** in a new session. Phase 8's scope
beyond step 2 has not been planned yet — wait for explicit confirmation
before starting anything new.

Phase 8 is PURE UI / Design System work. Hard constraints carried through
every step: no Phase 7 business-logic changes, no backend changes, no
CRUD / API / AI module changes.

## What Step 1 delivered — Login Experience (GlassSurface first)

### 1. GlassSurface — the official glass primitive

`src/components/experience/GlassSurface.vue` (exported from
`src/components/index.ts`) is the Design System's **official glass
primitive**. It was ported from React Bits (JavaScript + CSS variant) to a
Vue 3.5 SFC in the session before step 1 and is **complete — do not rewrite,
replace, or duplicate it**. Every future glass component builds on top of it.

How it works: a per-instance SVG filter (`feImage` displacement map generated
as a data URI from the element's measured size, split into per-RGB-channel
`feDisplacementMap`s and recombined) is applied via
`backdrop-filter: url(#id)`, so the element genuinely *refracts* what is
behind it. Chromium-only; Safari/Firefox are UA-sniffed into
`.glass-surface--fallback` (plain frosted `backdrop-filter`), and a
`@supports not (backdrop-filter)` layer degrades further. The dark variant of
the fallback keys off `html.dark` (the app's theme mechanism — NOT
`prefers-color-scheme`); the SVG variant's frost uses CSS `light-dark()`,
which works because `tokens.css` sets `color-scheme` per theme.

Porting notes (vs the React original): props/attrs are reactive bindings
instead of imperative `setAttribute` effects; the duplicated ResizeObservers
were merged into one that feeds a `measured` ref; `className`/`style` props
were dropped in favor of Vue fallthrough attrs.

### 2. Pink lotus asset — permanent location

`src/image/pinklotus.png` (ad-hoc) → **`src/assets/login/pinklotus.png`**.
Rationale: the project's asset architecture is per-experience folders under
`src/assets/` (precedent: `src/assets/welcome/` holds the GlassScene flower
wallpaper). The lotus is the login stage's centerpiece, so it lives in
`assets/login/`. `src/image/` was removed. The source PNG is 1536×1024 RGB
(~1.4 MB) with a pure-black surround — that black field is load-bearing (see
below).

### 3. LoginView redesign

`src/views/LoginView.vue` — script logic **unchanged** (auth store login,
remember-me localStorage, redirect query handling, error-code → i18n mapping,
theme/locale footer controls). Presentation rebuilt:

- **Stage**: `#000` in BOTH themes — the lotus artwork carries its own black
  field, so the page extends it edge-to-edge and crops are invisible
  (`object-fit: cover` at `object-position: 55% 42%`, where the bloom sits in
  the source frame — this keeps it optically centered, behind the card).
  Theme choice is expressed by the glass, not the backdrop.
- **Vignette layer**: one gradient stack — a rose aura around the bloom
  (`var(--scene-aura)`, theme-aware) + a corner vignette pulling focus
  inward.
- **Lotus breathing**: `app-breathe` (new motion.css keyframe, see § 4) at
  14 s alternate, transform/opacity only so it stays on the compositor.
- **Card**: a real `GlassSurface` (max-width 520 px, radius 28) floating in
  front of the bloom so the filter has something luminous to refract. The
  card itself does NOT float perpetually — a single soft `app-slide-up`
  entrance, then stillness. Two reasons: calm > motion (the breathing lotus
  owns the movement), and animating an element whose backdrop-filter is an
  SVG displacement chain forces continuous repaints of the filtered region.
- **Hint/error messages** now enter via `<Transition name="app-slide-down">`.
- On-glass legibility fixes (scoped, token-based): unchecked
  `el-checkbox` border lifted to `--color-muted` (the token border tuned for
  solid surfaces vanishes on glass); the "forgot password" plain button uses
  `--color-primary-active` in light mode / `--color-primary` in dark.

### GlassSurface parameters chosen for the login card — and why

| Prop | Value | Why |
|------|-------|-----|
| `width` / `height` | `"100%"` / `"auto"` | Sized by content + a `max-width: 520px` class; the internal ResizeObserver regenerates the displacement map on any size change. |
| `borderRadius` | `28` | Large, calm corner — between `--radius-glass` (16) and the hero scale; also drawn into the displacement map so refraction follows the corner. |
| `borderWidth` | `0.08` | Slightly wider edge-refraction band than default (0.07) — reads as a physical bevel on a card this large. |
| `brightness` / `opacity` | `50` (default) / `0.97` | Brightness 50 keeps the map core displacement-neutral (only edges refract, like real glass). Opacity raised from 0.93 → 0.97 to cut gradient leak-through that painted faint rainbow streaks across the card face. |
| `blur` | `10` | Edge-band softness in the map; near default. |
| `displace` | `0.5` | A whisper of output blur that melts pixel-level shimmer in the refracted edges. |
| `backgroundOpacity` | `0.44` dark / `0.66` light (computed from `appStore.isDark`) | The stage is black in both themes, so the frost carries the theme: dark stays smoky so the bloom glows through; light needs a denser white frost for text contrast (0.3/0.52 first attempts failed readability on the bright bloom — verified by screenshot). |
| `saturation` | `1.15` | Slightly enriches the pink glow behind the glass without candy-coating it. |
| `distortionScale` | `-88` | Realism over exaggeration — the demo's −180 warped the petals cartoonishly; −88 bends them like polished glass. |
| `redOffset`/`greenOffset`/`blueOffset` | `0 / 4 / 8` | Halved from defaults: a subtle chromatic fringe on high-contrast petal edges (the "expensive lens" cue) without visible RGB ghosting. |
| `xChannel`/`yChannel`/`mixBlendMode` | defaults | The stock map encoding is correct for this use. |

### 4. Design System additions

- **`app-breathe`** keyframe in `src/styles/motion.css` — slow scale-and-light
  pulse for cinematic backdrop imagery. Reusable by any future stage
  (documented inline: run 10 s+, alternate, transform/opacity only). Global
  `prefers-reduced-motion` override freezes it like everything else.
- Everything else stayed **local** to LoginView deliberately: the black
  stage, vignette, and on-glass control fixes are login-specific until a
  second surface needs them. If Step 2 builds more black-stage scenes,
  extract a `LotusStage`/`CinematicStage` experience component then — don't
  pre-abstract.

## Verification record (all on 2026-07-17)

- `vue-tsc --build` ✓, `npm run lint` (oxlint + eslint) ✓, `npm run build` ✓.
- Playwright/Chromium sweep: dark + light × desktop (1440×900) + mobile
  (390×844), dark tablet (834×1112) — composition, contrast, and refraction
  visually reviewed via screenshots; zero page errors.
- Keyboard: tab order username → password → show-password → remember-me →
  forgot-password → submit → footer controls; focus ring clearly visible on
  glass.
- `reducedMotion: 'reduce'` emulation: lotus animation duration collapses to
  0.01 ms (global override) ✓.
- Real login round-trip against the live backend: `demo` / `Demo123456` →
  lands on `/welcome` ✓.
- Not verified: Safari/Firefox fallback rendering (no local install) — the
  fallback CSS is upstream React Bits code, low risk, but eyeball it when a
  Mac is handy.

## Known trade-offs / watch items

- The SVG-filter backdrop is Chromium-only by design; non-Chromium users get
  clean frosted glass, not refraction.
- The lotus PNG is 1.4 MB and upscales past 1536 px viewports. If Step 2
  cares about login LCP, generate a compressed 2× set (like
  `welcome/flower-1280/2560.jpg`) — do NOT re-crop; the composition depends
  on the black surround.
- Light mode's small secondary text on glass (version stamp, checkbox label)
  is at DS-hierarchy contrast, not AAA. Raising frost past ~0.7 would fix it
  at the cost of the glass reading as paint — revisit only if flagged.
- WelcomeView still uses GlassScene + the old flower wallpaper. Visual
  continuity login → welcome is now broken on purpose (steps 1–2 scoped to
  login/the primitive only); a later step should decide whether the welcome
  experience adopts the lotus language.

## What Step 2 delivered — GlassSurface Pro (Interactive Optical Lighting)

Step 2 turned GlassSurface from a static optical surface into a **living**
one, still pure Vue + CSS + SVG (no Three.js / WebGL / FluidGlass — that was
evaluated and rejected, see the memory note). Three pieces:

### 1. `src/composables/useGlassSpotlight.ts` — the lighting engine

Same engineering contract as `useSpotlight` (its GlassScene sibling): pointer
events only move goalposts; a requestAnimationFrame loop eases everything and
writes **CSS custom properties only** — no Vue re-renders, no layout reads in
the hot path (rects are cached, re-measured at most once/frame and only when
a resize/scroll/RO callback marked them dirty; one ResizeObserver total).
The loop self-stops when all values settle. Exposes `active`, `cursor`,
`smoothedCursor`, `intensity`, `radius`, `proximity`, `brightness` as refs
for logic/tests — **do not bind them in templates** (they update per frame).

Written variables: on the **stage** — `--glass-light-x/y` (stage-local px),
`--glass-light-radius`, `--glass-light-strength` (0..1, intensity shaped by
proximity). On the **card** (options.card, a GlassSurface root via its new
`defineExpose({ element })`) — the same four in card-local coordinates plus
`--glass-proximity` (0..1, distance of the light to the card edge over an
influence range). Gated off (strength pinned to 0) for coarse pointers and
`prefers-reduced-motion`; the media queries are watched live.

### 2. GlassSurface — consumes CSS variables, never regenerates the filter

Additive only (the SVG displacement chain from step 1 is untouched and still
never recalculates per frame). New always-on lighting layers, all driven by
custom properties with calm defaults so every existing/future usage gets
permanent illumination with zero JS:

- `::before` **inner glow** — top light + faint answering bloom from below;
  opacity `calc(var(--glass-inner-glow, .55) + var(--glass-proximity, 0) * .3)`.
- `::after` **edge glow** — 1px rim + top bevel + inner halo box-shadows;
  opacity `calc(var(--glass-edge-glow, .5) + var(--glass-proximity, 0) * .4)`.
- `.glass-surface__light` **light-tracking sheen** — a radial highlight at
  `--glass-light-x/y` (card-local), dormant until strength × proximity > 0.
- **Frost thins near light** (background clarity rises): the `--glass-frost`
  alpha is multiplied by `(1 - proximity * 0.1)`. The factor was 0.16 first;
  dark-theme subtitle legibility under a bright petal pulled it back to 0.1
  (screenshot-verified). Deliberately no CSS transitions on these layers —
  the composable already interpolates per frame and a transition would fight
  it.
- No lighting layer touches `backdrop-filter`; refraction cost is unchanged.

New token: `--glass-light-radius: 360px` in tokens.css (the composable reads
it as its base radius fallback; stages may override).

### 3. LoginView — the sleeping stage

Script logic unchanged; the view only provides the stage and the card
(`useGlassSpotlight(stageRef, { card })` — interaction logic lives entirely
in the composable). Two new decorative layers:

- `.stage-shroud` — `rgba(0,0,0,.84)` over the (still breathing) lotus, so
  the stage sleeps almost black. Two feathered holes are cut with two
  mask-image layers + `mask-composite: intersect` (alphas multiply, so
  transparency from either layer opens the shroud): a **permanent pool**
  behind the centered card (the glass always reveals the bloom and always
  reads brighter than the stage — mouse enhances, never replaces), and the
  **travelling light**, whose aperture is `radius × strength` so it blooms
  from zero and stays shut for touch/reduced-motion users. A
  `@supports not (mask-composite: intersect)` guard keeps just the permanent
  pool where compositing is unavailable.
- `.stage-glow` — the light itself: three overlapping screen-blended radial
  falloffs (tight rose core ×0.5, wide body ×1.1, long violet tail ×2.2)
  with whisper-low alphas — accumulation, not a painted disc.

### Step 2 verification record (2026-07-17)

- `vue-tsc --build` ✓, `npm run lint` ✓, `npm run build` ✓,
  `npm run test:unit` (8/8) ✓.
- Playwright/Chromium sweep, dark + light × desktop (1440×900) / tablet
  (834×1112) / mobile (390×844): idle (permanent illumination), light-far,
  light-near states screenshot-reviewed; zero page errors. Driven variables
  dumped and sane (proximity ≈1 over the card, eases with distance).
- `reducedMotion: 'reduce'`: strength stays `0` after pointer movement;
  permanent pool + GlassSurface default glows remain ✓.
- Real touch emulation (`isMobile + hasTouch`): the fine-hover gate is false,
  light never ignites ✓. (Plain small-viewport emulation still reports a fine
  pointer — that 0.98 strength reading is an emulator artifact, not a bug.)
- Not verified: Safari/Firefox (no local install) — they take the fallback
  skin + the new CSS layers; `mask-composite: intersect` is supported by
  both current engines, and the @supports guard covers the rest.

### Step 2 watch items

- The travelling light repaints the shroud/glow gradient layers each frame
  (compositor-promoted via `translateZ(0)`); fine on desktop Chromium. If a
  future stage stacks more masked layers, profile before adding a fourth.
- The permanent pool is sized in stage-relative percentages
  (`54% × 62% at 50% 50%`) and assumes a centered card — true for login. A
  future page with an off-center GlassSurface should either re-position the
  pool or drive it from the card rect.
- GlassSurface's lighting defaults (`--glass-inner-glow: .55`,
  `--glass-edge-glow: .5`) now apply to *every* GlassSurface. On the login
  card they read correctly; when Workspace/Dashboard/AI-Tutor/dialogs adopt
  the primitive, tune per-surface via the two variables rather than editing
  the component.

## Phase 8 candidates (unchanged from Phase 7 handoff)

Registration/email/password-reset, payments, OSS/file upload, external auth,
spaced-repetition engine, WebSockets, Docker/CI — plus, within the Premium
Glass Experience theme, re-skinning the welcome scene and app chrome on top
of GlassSurface.
