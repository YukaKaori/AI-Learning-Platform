# Phase 10 Handoff — Fluid Glass Dock + Product Showcase

**Status: Phase 10 is COMPLETE** as of 2026-07-18 (implemented in one session,
verified end-to-end, NOT committed — awaiting approval; note Phase 9's work
is also still uncommitted in the same working tree). Phase 10 is PURE UI /
Design System work: no backend, routing, store or auth-logic changes. This
document is the resume point for whatever follows.

## The reframe

The bottom of the login stage is no longer a website section (the Phase 9
carousel slab) — it is a **persistent fluid glass dock**, and the login page
is now a three-room exhibition navigated by that dock. The dock is the
product: one living GlassSurface holding three facet buttons — **Login**
(the current workspace; never navigates away), **Product** (a full-screen
keynote presentation) and **Sponsor** (a minimal sponsor page). The rooms
are *galleries in the same building*: opening one never routes anywhere —
the dark stage, the darkness shroud and the dock all remain; a full-screen
layer simply fades in through defocus **behind** the dock, exactly like a
persistent VisionOS dock. React Bits' FluidGlass was studied for its optical
language only (per the Phase 9 evaluation its R3F/WebGL implementation stays
rejected); everything here is built from the existing GlassSurface system.

## 1. GlassDock.vue (new, components/experience/, exported)

- One GlassSurface (`surfaceFlow` on) with the showcase slab's exact optical
  tuning: `--glass-depth: 1`, `--glass-fresnel: 1`, `--glass-density: 0.3`,
  smoked `--glass-tint`, distortion -70, offsets 0/4/8. The page's filter
  budget stays at exactly **2** displacement passes (probe-verified with
  every gallery open).
- Exports `type GalleryName = 'login' | 'product' | 'sponsor'` (re-exported
  from components/index.ts).
- Three `.dock-item` facet buttons inside `.glass-material`: smoked panes
  with double edge, chromatic rims, and the moving reflection at the
  facet-local light position. Icons: login `home`, product `gem`, sponsor
  `heart`.
- Interaction is light only: hover pools background light and brightens the
  border (verified: computed `transform` stays `none` on hover); press is
  the damped `translateY(0.5px)`; the active facet holds more light
  (`is-active` + `aria-current="page"` + title = "Current workspace"). No
  bounce, no spring, no floating, no scaling, no elevation.
- `defineExpose({ focusItem })` — closing a gallery returns keyboard focus
  to the facet that opened it.
- ≤420px the labels hide (`display: none`); names stay via `aria-label`.

## 2. Spotlight now lights the dock (zero composable changes)

`useGlassSpotlight`'s facet mechanism was re-rooted, not extended: LoginView
now passes `facets: { root: stageRef, selector: '.app-input, .app-button,
.glass-check__box, .glass-dock, .dock-item' }`. The dock's GlassSurface root
is itself a facet (so its sheen layer gets dock-local `--glass-light-x/y`),
and each button is a facet. Strength/radius inherit from the stage through
the CSS cascade (custom properties inherit); dock proximity is 0 (only the
card gets proximity), which keeps the dock's response deliberately quieter
than the card's. Touch / reduced-motion users still get strength 0.

## 3. ProductPresentation.vue (new) — the keynote

Full-viewport gallery layer (`position: fixed`, z-index 40, background
`rgb(2 3 7 / 0.93)` — a whisper of the artwork survives behind the deck).
**No GlassSurface here**; slides are typography + one large glass-facet
glyph on the dark stage.

- 7 slides: hero (brand mark at exhibition scale + `app.name` +
  `landing.product.hero.line` + scroll hint), then tutor / notes /
  flashcards / graph / roadmap / coming (icon glyph 128px, hero-scale title,
  one dim line — the Phase 9 `engine` slide was dropped per the Phase 10
  brief's slide list; its strings are gone from both locales).
- Paging engine evolved verbatim from Phase 9's GlassShowcase: wheel with
  delta accumulation (≥42, 240ms idle reset), 950ms transition lock (rapid
  wheel = exactly one slide, verified), keyboard (Arrow/Page/Home/End),
  vertical touch swipe ≥40px (mouse drags ignored). Differences: the deck is
  **finite** (clamps at both ends, no wrap — verified) and there is **no
  autoplay** (a keynote is audience-paced).
- Slide changes reuse the optical-refocus vocabulary at stage scale (blur
  10px, scaleY compression, ±~50px directional drift, 900ms `--ease-out`).
- Escape emits `close`. Root takes focus on mount. A11y mirrors the old
  showcase: `role="group"` + `aria-roledescription="carousel"` + polite live
  region; bead rail (right edge, luminance-only) with `aria-current`.

## 4. SponsorPanel.vue (new) — the quiet room

Same fixed gallery layer. One centred statement (heart glyph, title, one
line) and three future channels resting as unlit facets: Buy Me a Coffee
(`coffee`), GitHub Sponsors (`heart`), WeChat (`qr-code`) — each tagged
"Coming soon", deliberately non-interactive until a later phase wires real
links. Escape closes. Icons `coffee` and `qr-code` were added to the
registry (lucide, already a dependency).

## 5. LoginView.vue — gallery state machine

Auth/script logic untouched (login round-trip re-verified). Composition:

- `gallery: Ref<GalleryName>` — the dock's `navigate` sets it; overlays'
  `close` (Escape) resets to `'login'` and restores focus to the opening
  dock facet via `nextTick`.
- The sign-in slab **stays mounted** in every gallery (filter budget, and
  the card aperture keeps breathing behind the layer) but recedes:
  `.is-recessed` = opacity 0 + blur(10px) + `pointer-events: none`, plus the
  `inert` attribute (verified). **Gotcha:** the entrance animation's
  `fill: both` would pin opacity at 1 over any class, so `.is-recessed` sets
  `animation: none`; returning to login therefore replays the entrance —
  read as the camera stepping back into the first room, intended. The same
  pattern applies to the colophon (`.is-dimmed`).
- Gallery transition (`.gallery-*`): opacity + blur(14px) only — 900ms in,
  500ms out. No transforms, nothing router-like.
- `.showcase-anchor` → `.dock-anchor` (z-index 50 > overlay 40: the dock and
  its underlight float above every gallery — hit-test verified). The three
  underlight blobs are unchanged and now read as the dock's caustics over
  the presentation.
- GlassShowcase.vue was **deleted** (superseded; note it was never committed
  — it existed only in the Phase 9 working tree, so it will not appear in
  git history unless resurrected from this handoff's description).

## 6. Locales

`landing.showcase.*` replaced by `landing.dock.*` (label, login, product,
sponsor, current), `landing.product.*` (label, hint, hero.line, slides ×6)
and `landing.sponsor.*` (title, line, soon, items ×3) in BOTH locales
(parity enforced by the existing spec, 8/8 unit tests pass).

## Verification record (2026-07-18)

- `vue-tsc --build` ✓, `npm run lint` (oxlint + eslint) ✓, `npm run build`
  ✓, `npm run test:unit` (8/8) ✓.
- Playwright/Chromium sweep — **38/38 checks** across desktop dark
  (1440×900), light scheme, mobile touch (390×844), reduced motion: layout
  (no scroll either axis at 1440×900; mobile no h-scroll; labels condense);
  filter budget exactly 2 in every gallery; dock (3 items, active
  aria-current, hover = background change with `transform: none`, hit-test
  above open presentation); spotlight (stage strength ignites, dock slab +
  dock item receive facet-local px vars; reduced motion stays 0); keynote
  (7 beads, focus on open, rapid wheel = one slide, ArrowUp back, End+wheel
  clamps, Escape closes, card recesses to opacity 0 + inert + returns,
  colophon dims, focus restored to the Product facet); sponsor (3 channels,
  Escape closes); real login round-trip `demo`/`Demo123456` off `/login` ✓;
  zero page errors in all four contexts.
- Screenshots reviewed (login+dock, product hero, feature slide, sponsor):
  keynote whitespace reads premium, dock floats over every room, active
  facet holds light.
- Standing caveat: real Safari/Firefox still not verified locally (same as
  Phases 8–9); the GlassSurface fallback path is unchanged this phase.

## Watch items

- The finite deck means wheel input at the last slide does nothing — if a
  future brief wants an "end of deck" affordance, add a bead pulse, not a
  bounce.
- Overlays are `position: fixed`; on very short viewports the page can still
  scroll while in the login gallery (unchanged Phase 9 behavior), and the
  dock scrolls with the flow. If a brief demands an always-pinned dock,
  that's a layout re-decision (sticky anchor), not a tweak.
- The facet selector now roots at the stage; any future `.app-button` added
  anywhere on the login stage automatically becomes a light facet — usually
  desirable, but remember it.
- `.is-recessed` / `.is-dimmed` must keep `animation: none` (entrance
  fill-mode override) — removing it silently breaks the recede.
- Sponsor channels are placeholders; wiring real links (BMAC / GitHub /
  WeChat QR) is a one-file change in SponsorPanel.vue plus locale strings.

## Phase 11 candidates

Unchanged from the Phase 7/8/9 lists (registration/email/password-reset,
payments, OSS upload, external auth, spaced repetition, WebSockets,
Docker/CI) plus: real sponsor links + WeChat QR asset; adopt the Phase 9
material on Welcome/app chrome; compressed lotus asset for login LCP
(1.4MB PNG still ships).
