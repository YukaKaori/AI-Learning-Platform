# Phase 8 Handoff — Premium Glass Experience

**Status: Step 1 of Phase 8 is COMPLETE** as of 2026-07-17. This document is
the resume point for **Step 2** in a new session. Phase 8's scope beyond
step 1 has not been planned yet — wait for explicit confirmation before
starting anything new.

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
  continuity login → welcome is now broken on purpose (step 1 scope was
  login only); Step 2 should decide whether the welcome experience adopts
  the lotus language.

## Phase 8 candidates (unchanged from Phase 7 handoff)

Registration/email/password-reset, payments, OSS/file upload, external auth,
spaced-repetition engine, WebSockets, Docker/CI — plus, within the Premium
Glass Experience theme, re-skinning the welcome scene and app chrome on top
of GlassSurface.
