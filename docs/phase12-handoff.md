# Phase 12 Handoff — Product Page as a Premium Product Website

**Status: Phase 12 is COMPLETE** as of 2026-07-18 (implemented and verified
in one session, NOT committed — awaiting approval). Phase 12 is PURE UI work
on the Product presentation plus two token-level seams (dock text, stage
overflow): no backend, no auth changes, no routing changes, no new
dependencies. **Do not start Phase 13 without approval.**

Naming note: "Phase 12" was earlier used informally for the GlassDock
bar-mode redo (committed inside f244d7c with Phases 9–11). This document is
the canonical Phase 12: the Product page rewrite. The dock redo predates it
and is untouched here.

## The reframe

Phase 11's Product keynote was the one colorful room in a dark museum —
but still a dark room. Phase 12 makes it a different world entirely: a
**bright premium product website** (Apple / Linear / Stripe register). The
login stage stays the black gallery; opening Product steps from dark to
light, art to product. The persistent glass dock is the one object that
travels between both worlds, refracting black in one and pastel color
fields in the other.

## Brief-vs-reality conflicts, resolved with the user up front

1. **Scroll model** — the brief says "not a presentation slide / scrolling
   smooth and effortless / do NOT imitate PowerPoint", but Phase 10/11's
   engine was a hard-paged keynote. Decision (user-approved): **retire the
   paging engine**; the layer is now a native scroll container with CSS
   `scroll-snap-type: y proximity`. Wheel/touch/keyboard scroll natively;
   snap gently aligns sections. The 50/50-verified keynote engine (wheel
   accumulation, transition lock, live region, refocus transitions) is gone
   from this page.
2. **Dock legibility** — fixed near-white labels on clear glass are
   invisible over white sections; the brief forbids redesigning the dock.
   Decision (user-approved): **color-token flip only**. GlassDock's two
   hardcoded text-shadow halos became `--dock-halo` / `--dock-halo-active`
   variables (defaults unchanged), and LoginView sets
   `.dock-anchor.is-on-light :deep(.glass-material)` (only while
   `gallery === 'product'`) to dark-ink `--on-glass-text*` plus a light
   halo. The override MUST target `.glass-material` itself — glass.css
   declares those variables on that element, so an ancestor-level override
   loses. Geometry, material, motion of the dock: untouched.
3. Phase 11's "warm white rendered as warm dawn" resolution is deliberately
   reversed: the Product page now has its own **dark-ink-on-light** type
   system (`--pp-ink: #211c44` + alpha steps), fully separate from the
   stage's fixed dusk palette.

## ProductPresentation.vue — rewritten

- **Container**: `position: fixed; inset: 0; z-index: 40` (still under the
  dock at 50), `overflow-y: auto`, `overscroll-behavior: contain`,
  `scroll-snap-type: y proximity`, `scroll-behavior: smooth` gated behind
  `prefers-reduced-motion: no-preference`. Background `#fbfaff` — the dark
  stage never flashes through.
- **Eight sections**, each `min-height: 100dvh`, `scroll-snap-align:
  start`, its own pastel atmosphere (`--sec-base` gradient + up to three
  78→70vmax radial blobs drifting on the shared `app-underlight-*`
  keyframes) and its own accent (`--sec-accent`, `--sec-accent-2`) that
  colors badges, points, and the visual via `color-mix`.
- **Alternating compositions**: hero (centered, gradient headline, two
  floating preview cards) → tutor (text left / chat window + suggestion
  chips + learning-timeline panel right) → notes (editor window + dark code
  panel + AI chip left / text right) → flashcards (centered 3-card stack in
  depth + progress dots) → graph (text left / the Phase 11 constellation
  re-lit in teal on white) → engine (diagonal: copy high-right, streaming
  window with blinking caret + core + halo + 12 particles low-left) →
  roadmap (centered milestone timeline, 4 stops, "Next" pill on the hollow
  future stop) → coming (minimal: orb + large type on one violet bloom).
- **Content hierarchy**: headline + one-line subtitle everywhere; split
  sections add three check-chip feature points (new `p1..p3` strings);
  roadmap has real milestone strings (`m1..m4`, `soon`) instead of reusing
  feature titles. `landing.product.nav` added for the section nav label.
  All new keys exist in both locales (parity spec still passes).
- **Motion**: house `[data-reveal]` + `useScrollReveal(rootRef)` (its
  IntersectionObserver roots on the viewport, which is exactly the fixed
  container's box — works unchanged) with `--reveal-delay` staggering;
  gentle `app-float` loops on the visuals; no springs. Reduced motion:
  useScrollReveal reveals everything immediately, global override freezes
  loops, smooth scrolling drops to instant jumps.
- **Nav**: the bead rail became `.pp-nav` fixed dots (dark ink); active
  section tracked by an IntersectionObserver (`root: container`,
  threshold 0.55), `aria-current` follows, click = `scrollIntoView` (CSS
  decides smooth vs instant). Carousel role/roledescription and the live
  region are gone — this is a normal scrolling region now
  (`aria-label` kept). Escape still emits `close`; focus returns to the
  Product facet (unchanged LoginView logic).
- **Wheel-over-dock dead zone**: the dock is a z-50 sibling, so wheel over
  the glass reaches nothing scrollable. A passive window wheel listener
  forwards deltas into the container when the event target is outside it.
- **Filter budget unchanged**: no GlassSurface here, still exactly 2
  `svg filter` elements page-wide. The Phase 11 transform-clobbering rules
  (individual `rotate:`/`translate:`/`scale:` for posed floaters; geometric
  centring; in-flow pulse keyframes) are carried over throughout.

## The stage-scroll bug (important, was latent before Phase 12)

`.login-stage` had `overflow: hidden` and ~341px of hidden scrollable
overflow (the dock's underlight bleed). `overflow: hidden` still creates a
**programmatically scrollable** box, and Chromium would sometimes scroll it
to its max while the Product layer entered (flaky, landing near the end of
the 900ms gallery-enter transition; not caused by our `focus()` — that now
passes `preventScroll: true` anyway, kept as belt-and-braces). Result: the
whole stage silently shifted up 341px and the "fixed" dock sat mid-screen
(y≈428 instead of ≈769 at 1440×900) — almost certainly reproducible in
Phase 11 too, where dock stability was only measured *within* the deck.
Fix: `.login-stage { overflow: clip }` — clip is not a scroll container,
so no browser heuristic (focus reveal, snap, anchoring) can ever move it.
Probe-verified: stage scrollTop stays 0 and the dock box is byte-stable
across open → hover → wheel in every scenario that previously flaked.

## Files touched

- `components/experience/ProductPresentation.vue` — full rewrite.
- `components/experience/GlassDock.vue` — two text-shadows now read
  `var(--dock-halo…)` with the old values as defaults; nothing else.
- `views/LoginView.vue` — `is-on-light` class binding on `.dock-anchor`,
  the `:deep(.glass-material)` token flip block, `overflow: clip`.
- `locales/en-US.ts` + `zh-CN.ts` — `landing.product.nav`, `p1..p3` on
  tutor/notes/graph/engine, `m1..m4` + `soon` on roadmap (17 keys each).
- Nothing else: GlassSurface, glass.css, motion.css, SponsorPanel, icon
  registry, router, components/index.ts untouched.

## Verification record (2026-07-18)

- `vue-tsc --build` ✓, `npm run lint` ✓, `npm run test:unit` (8/8) ✓,
  `npm run build` ✓ — re-run on final code after the scroll fixes.
- Playwright/Chromium sweep — **39/39 checks** across desktop dark
  (1440×900), mobile touch (390×844), reduced motion, light scheme:
  8 sections + 8 nav dots; container scrolls ~8 viewports; root and
  atmospheres light with dark-ink titles; filter budget exactly 2 in
  product mode (desktop and mobile); dock ink flip on and off (Escape
  restores dusk tokens and focus); every composition present (chat window
  + 3 accessories, editor + code panel, 3-card stack, 6 nodes/8 links/core,
  core + 12 particles + stream window, 4 stops + 1 soon pill, orb); reveals
  fire on arrival and not before; native wheel, dot-click jump, and
  wheel-over-dock forwarding all scroll; dock bounding box byte-identical
  before/after interaction; no horizontal overflow desktop or mobile;
  reduced motion reveals everything immediately and dot jumps still work;
  light scheme identical; zero page errors in all four contexts.
- Screenshots reviewed for hero/tutor/notes/flashcards/graph/engine/
  roadmap/coming + mobile: bright, premium, alternating layouts read as a
  real product site; dock labels clearly legible in dark ink over every
  section.
- Standing caveats: real Safari/Firefox still unverified locally (as
  Phases 8–11). `overflow: clip` needs Chrome 90+/Safari 16+/Firefox 81+ —
  inside the existing `color-mix`/`light-dark` baseline.

## Watch items

- The window wheel-forwarding listener writes `scrollTop` directly
  (instant, no smooth) — if a future layer floats above the product page,
  wheels over it will scroll the product; scope the check if that's wrong.
- `useScrollReveal` observes against the viewport, not the container. Fine
  while the container IS viewport-sized; if the page ever becomes a
  windowed panel, pass a scroll root.
- Section-tracking IO threshold is 0.55: sections shorter than ~55% of the
  viewport would never activate their dot. All sections are ≥100dvh today.
- The nav dots are `position: fixed` inside the layer; during the 900ms
  gallery enter/leave the root's `filter` makes them position against the
  layer instead of the viewport — same geometry (inset 0), no visible
  effect, but don't give the root a transform.
- Keyboard scrolling relies on focus living inside the container (focused
  on mount). If some future control steals focus to outside the layer,
  arrows stop paging until the user clicks back in.

## Phase 13 candidates

Unchanged from the Phase 10 list (registration/email/password-reset,
payments, OSS upload, external auth, spaced repetition, WebSockets,
Docker/CI, real sponsor links + WeChat QR asset, Phase 9 material on
Welcome/app chrome, compressed lotus asset for login LCP).
