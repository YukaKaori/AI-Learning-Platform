# Phase 9 Handoff — Optical Glass Installation

**Status: Phase 9 is COMPLETE** as of 2026-07-17 (implemented in one session,
verified end-to-end). Phase 9 is PURE UI / Design System work: no backend,
routing, store or auth-logic changes. This document is the resume point for
whatever follows.

## The reframe

The login page is no longer "a login page with glass effects" — it is **an
optical glass installation that happens to contain a login form**: a black
gallery, one hero artwork (the lotus at native 3:2, `min(60vw, 900px)`), and
two slabs of smoked optical glass floating in front of it. Legibility comes
from **optical density (smoked ND tint), never white frost**. Both themes get
the smoked slab; theme is expressed as glass *temperature* (`--glass-tint`
via `light-dark()`), and on-glass text is a fixed dusk palette (see
glass.css). The Phase 8 reveal story is fully preserved: shroud, card-shaped
permanent aperture, travelling reveal, asymmetric attack/release — only the
aperture feather widened (`CARD_HOLE_REACH` 16→24, `CARD_HOLE_FEATHER`
14→20) so a whisper of petal bleeds past the rim: the flower continues
beyond the slab, but stays in darkness until the reveal finds it.

## 1. GlassSurface.vue — additive evolution (NOT rewritten)

The SVG displacement chain, all props and defaults are untouched and still
never regenerate per frame. Three new layer divs, each fully gated by custom
properties whose defaults render nothing — **every non-opted-in GlassSurface
is pixel-identical to Phase 8**:

- `__depth` — smoked ND body (`--glass-tint` color × `--glass-density` 0..1,
  vertical gradient so the slab reads thicker toward its base), back-face
  reflection, sharp front rim, plus `::before` inner back rim (1px contour
  inset 3px, biased 1px down — the "thick slab" double edge) and `::after`
  internal scattering (opacity = depth × light-strength × proximity). All
  alphas are produced by `color-mix(... calc(var(--glass-depth,0) ...))`, so
  `--glass-depth: 0` paints literally nothing.
- `__fresnel` — conic arc masked to a 1.5px ring (`mask-composite: exclude`,
  with an `@supports not` guard that hides the ring where unsupported).
  Aimed by `--glass-light-angle` (0deg = light above); undriven it rests as
  a top highlight. Gated by `--glass-fresnel: 0` default.
- `__flow` — rendered only with the new `surfaceFlow` prop (default false):
  a travelling diagonal highlight (34s) + caustic pools (26s), keyframes
  `app-glass-flow` / `app-glass-caustics` in motion.css, transform/opacity
  only. Presence overridable via `--glass-flow-opacity` (the showcase lifts
  it to 1 while a slide shifts — the glass "works" during refocus).

## 2. useGlassSpotlight.ts — angle + facets (same contract)

- Writes `--glass-light-angle` on the card each frame (atan2 from card
  centre, +90 so 0deg = above; **unwrapped** against the previous frame so
  the Fresnel arc glides through the ±180° seam instead of snapping).
- New `facets: { root, selector }` option. Facet elements are collected by
  `querySelectorAll` inside `measureRects()` (same dirty flag — no extra
  observers, no hot-path layout reads). Each facet receives only local
  `--glass-light-x/y` (2 writes, skipped while strength < 0.01);
  strength/radius/proximity inherit through the CSS cascade from the card.
  Selector-targeting reaches component-internal elements (AppInput's inner
  `.app-input` box) without modifying the components — a data-attribute via
  fallthrough would land on the wrapper and mis-measure by the label height.

## 3. styles/glass.css (new, imported by index.css)

The shared material system, scoped to `.glass-material`. Two mechanisms:

1. **Token remap** — `--color-text*`, borders, and the brand steps are
   remapped on the container (brand fixed to the dark-theme values: the slab
   is theme-agnostic smoked and light's primary loses contrast on dark
   glass). Text/placeholder/icon/label colors need zero selector overrides.
2. **Optical skins** for control chrome. Specificity note: these fight the
   components' scoped styles, so rules use a doubled context class
   (`.glass-material.glass-material`) to win regardless of CSS order.

Vocabulary shared by every control (inputs, primary button, ghost buttons,
native checkbox): smoked transparency (no white fills), double edge,
recessed depth (inputs read carved INTO the slab), 1px cool/warm chromatic
rims, and the **moving reflection** — a radial background positioned at the
facet-local `--glass-light-x/y` whose alpha is
`color-mix(... calc(strength ...))`, i.e. transparent for touch/reduced
motion users. The press state is a damped `translateY(0.5px)` overriding the
app-wide scale press; nothing on glass may use `--ease-spring`.

`el-checkbox` is **gone** from the login card — replaced by a native
`<input type="checkbox">` + `.glass-check__box` styled here (the box is also
a spotlight facet). LoginView no longer uses any Element Plus control.

## 4. LoginView.vue

Auth/script logic untouched. Presentation:

- Lotus hero-scaled (`min(60vw, 900px)`, mobile `min(96vw, 520px)`), stage
  still literal `#000`, artwork never stretched/cropped. The card intersects
  the bloom (verified: artwork rect strictly contains the card horizontally).
- Card material opt-in on `.login-card`: `--glass-depth: 1`,
  `--glass-fresnel: 1`, `--glass-density: 0.34`,
  `--glass-tint: light-dark(rgb(17 18 24), rgb(7 9 15))`,
  `--glass-edge-glow: 0.68`; props retuned `borderWidth 0.12`,
  `backgroundOpacity 0.1` (whisper — density carries legibility),
  `distortionScale -110`, offsets 0/5/10.
- Card body carries `.glass-material`; spotlight gains
  `facets: { root: cardEl, selector: '.app-input, .app-button, .glass-check__box' }`.
- Old white-frost control skins deleted (superseded by glass.css); old
  bottom navigation deleted (links, REPO_URL, styles, `landing.nav.*`).
- **Tagline removed entirely** (user request mid-phase): element, styles and
  `landing.tagline` in BOTH locales are gone. Colophon stays.
- `.showcase-anchor` (margin-top: auto) holds `.stage-underlight` — three
  blobs (rose 0.09 / violet 0.08 / warm white 0.05) drifting on
  `app-underlight-a/b/c` (44s/58s/36s, transform-only), above the shroud,
  below the glass, so the showcase's displacement filter refracts genuinely
  moving light — living caustics with zero filter work.
- Vertical fit at 1440×900 re-tuned after the showcase (taller than the old
  nav) overflowed by 51px: card margin-top `clamp(72px, 12vh, 160px)`,
  card-body top padding space-8, header margin space-6. Playwright-measured:
  no scroll at 1440×900 in either axis.

## 5. GlassShowcase.vue (new, components/experience/, exported)

One persistent GlassSurface (`surfaceFlow` on, `--glass-density: 0.3`, one
step thinner than the card) — the slab **never moves/scales/unmounts**; only
the world inside changes. Page filter budget stays at exactly **2**
displacement passes (probe-verified).

- 7 slides (icon / title / line from `landing.showcase.slides.*`): tutor
  `graduation-cap`, notes `notebook-pen`, flashcards `layers`, graph
  `network`, engine `brain`, roadmap `route`, coming `sparkles`.
- **Optical refocus transition** (~900ms, `--ease-out`, no overshoot):
  outgoing defocuses `blur(7px)`, compresses `scaleY(.94)`, drifts opposite
  the travel direction; incoming refocuses/decompresses from the travel
  side. While shifting, `is-shifting` lifts `--glass-flow-opacity` to 1.
- Input: wheel (preventDefault scoped to the slab; delta accumulation ≥42,
  240ms idle reset, 950ms lockout — rapid wheel moves exactly one step),
  keyboard (Arrow/Page/Home/End on the focusable group), touch swipe
  (pointer events, ≥40px vertical, `touch-action: pan-x`; mouse drags
  ignored), bead indicators (aria-current, luminance-only change).
- Autoplay 7s (timeout chain): pauses on hover (mouse only), focus-within,
  touch, `document.hidden`; **disabled under reduced motion** (live-watched
  matchMedia). A11y: `role="group"` + `aria-roledescription="carousel"`,
  polite live region announcing only user-initiated changes.

## 6. Locales

`landing.nav.*` and `landing.tagline` removed; `landing.showcase.*` (label +
7 × title/line) added to both zh-CN and en-US (parity enforced by the
existing locales spec).

## Verification record (2026-07-17)

- `vue-tsc --build` ✓, `npm run lint` ✓, `npm run build` ✓,
  `npm run test:unit` (8/8) ✓.
- Playwright/Chromium sweep — **28/28 checks**: dark+light desktop
  (1440×900), dark mobile (390×844, touch). Layout (no h-scroll anywhere, no
  v-scroll at 1440×900, showcase below card, card intersects artwork);
  driven variables sane (strength 0.84, proximity 0.66, angle `-87.9deg`
  with pointer left of card, facet-local px on `.app-input`); glass checkbox
  toggles; wheel advances / lockout absorbs rapid wheel / off-slab wheel
  ignored; ArrowUp pages back; bead jump; autoplay advances when idle and
  pauses while focus is inside (bead focus counts — by design); touch swipe
  advances; reduced-motion (strength stays 0, autoplay never fires, manual
  nav still works); zero page errors in every context.
- Real login round-trip `demo`/`Demo123456` → `/welcome` ✓ (card DOM changed
  this phase — native checkbox).
- Fallback probe (Firefox UA in Chromium): `.glass-surface--fallback` skin
  applied, depth/fresnel layers render on top, zero errors. Real
  Safari/Firefox still not verified locally (no install) — same standing
  caveat as Phase 8.
- Screenshots reviewed (idle/light-near × themes, mobile, showcase slides):
  smoked slab reads as thick glass, bloom emerges through the sheet with the
  crown above, petals continue past both edges into darkness.

## Watch items

- At the card's right edge a thin sliver of un-refracted petal sits next to
  its displaced copy (the widened aperture showing the flower beyond the
  glass while the filter shifts it inside). Reads as the intended emergence
  seam; if it ever reads as a glitch, shrink `CARD_HOLE_REACH` toward 16.
- Light theme is now dark glass too (approved): only `--glass-tint`
  temperature and rim brightness differ. If a future brief wants a truly
  bright light-mode slab, that is a material re-decision, not a tweak.
- The showcase blocks page scroll while hovered/touched (by design — the
  wheel/swipe belong to the pager). On short viewports users must scroll
  from outside the slab.
- `.glass-material` remaps brand tokens; don't nest solid-surface components
  inside it without checking their palette.
- Facet rects re-measure on the shared dirty flag (resize/scroll/RO). If a
  future glass page adds facets that move *without* those signals (e.g. an
  animated accordion), call the composable's markRectsDirty path or resize
  something.
- Perf philosophy unchanged: 2 displacement filters, CSS-variable writes
  only in the rAF loop (~25/frame while lit), all new animation
  compositor-friendly. If a third always-mounted surface appears, profile
  first (Phase 8 warning stands).

## Phase 10 candidates

Unchanged from Phase 7/8 lists (registration/email/password-reset, payments,
OSS upload, external auth, spaced repetition, WebSockets, Docker/CI) plus:
adopt the Phase 9 material on Welcome/app chrome; a compressed lotus asset
for login LCP (1.4MB PNG still upscales past 1536px).
