# Phase 11 Handoff — Product Experience Gallery (Apple Keynote Style)

**Status: Phase 11 is COMPLETE** as of 2026-07-18 (implemented in one session,
verified end-to-end, NOT committed — awaiting approval; Phases 9 and 10 are
also still uncommitted in the same working tree). Phase 11 is PURE UI work
confined to the Product page: no backend, no auth changes, no routing
redesign, no new dependencies. This document is the resume point for
whatever follows. **Do not start Phase 12 without approval.**

## The reframe

The Product keynote is no longer a black deck of captions — it is the one
**colorful space** in the project. The login gallery stays the black room
with the pink lotus; the Product gallery becomes eight rooms of colored
light, each slide owning a full-bleed atmosphere and a CSS-only visual
composition. The floating glass dock is now the optical centerpiece: the
atmospheres run edge to edge underneath it, so its displacement refraction
finally has moving colored light to bend. The dock never moves; the world
dissolves behind it.

## Brief-vs-implementation conflicts resolved up front

1. **AI Engine slide** — the Phase 10 brief dropped it (strings deleted);
   the Phase 11 brief lists it as Slide 6. Phase 11 supersedes: the slide is
   restored (icon `cpu`, new in the registry) with fresh
   `landing.product.slides.engine.*` strings in both locales. Deck order:
   hero, tutor, notes, flashcards, graph, engine, roadmap, coming (8).
2. **"The dock remains fixed"** — the dock is still in-flow (Phase 10 watch
   item), but while Product is open the keynote layer owns every wheel/touch
   gesture (`preventDefault` + `touch-action: none`), so the page beneath
   cannot scroll and the dock is genuinely stationary (bounding-box-verified
   across the whole deck). No layout re-decision was needed.
3. **"Warm white" atmospheres** — the whole on-stage type system is a fixed
   light dusk palette; a truly white slide would need a parallel dark-text
   system. Roadmap is rendered as warm amber dawn instead: unmistakably
   warm, still deep enough for the shared typography.
4. **Scroll model** — the brief's "one viewport per section" + "exactly like
   today" keeps Phase 10's paged keynote engine verbatim (wheel
   accumulation ≥42 with 240ms idle reset, transition lock — now 1050ms —
   keyboard Arrow/Page/Home/End, vertical touch swipe ≥40px, finite deck,
   no autoplay, Escape closes).

## ProductPresentation.vue — what changed

- **Atmosphere layer** (new, `aria-hidden`): a full-bleed `.atmo` per slide,
  keyed by slide, cross-fading via `<Transition name="atmo">` over **1600ms
  opacity-only** — deliberately slower than the content's 1000ms refocus
  ("light moves slower than glass"). Each `.atmo--<key>` modifier sets four
  custom properties: `--atmo-base` (an opaque deep two-stop gradient — no
  slide is ever pure black) and `--atmo-a/b/c` feeding three 78vmax
  radial-gradient blobs that drift on the existing `app-underlight-a/b/c`
  keyframes (38–56s, transform-only). Palettes: hero violet/rose/blue mesh,
  tutor deep blue/cyan, notes warm amber, flashcards plum/magenta, graph
  deep teal/emerald, engine electric indigo, roadmap warm dawn, coming a
  single distant violet bloom. The hero also gets `.atmo-sheen`, a slow
  highlight band on `app-glass-flow` (42s) — a glass reflection at room
  scale.
- **Typography**: titles now clamp to 4.4rem (features), 6.4rem (hero — a
  white→violet→rose→blue gradient via `background-clip: text`), 5.6rem
  (coming, with a soft violet text-shadow glow). Still headline + one line,
  nothing more.
- **Visual compositions** (all `aria-hidden`, CSS gradients/transforms
  only, no text): tutor = three floating chat panes with skeleton bars and
  a gradient badge; notes = three tinted note cards (amber/rose/violet)
  joined by faint threads; flashcards = a three-card stack in depth;
  graph = a 480×260 constellation (six glowing nodes, eight gradient
  links radiating from a cored `network` glyph, staggered pulse); engine =
  a breathing `cpu` core + halo with 14 rising token-particles; roadmap = a
  timeline whose stop labels **reuse the deck's own feature titles** (no
  extra strings) with the future stop as a pulsing hollow ring. Shared
  `.pp-card` pane carries the dock's optical family (double edge, chromatic
  rims, deep drop shadow) with **no backdrop-filter**.
- **Motion discipline**: slide refocus slowed to 1000ms (drift 64px, blur
  12px), lock 1050ms; float loops 9–14s alternate; no springs, no bounce.
  Reduced motion collapses everything via the global override (verified).
- **Filter budget unchanged**: still no GlassSurface in the presentation —
  exactly **2** displacement filters page-wide (sign-in slab + dock),
  probe-verified as 2 `svg filter` elements (each GlassSurface's filter
  holds 3 `feDisplacementMap`, one per RGB channel — count filters, not
  displacement maps, or you'll read 6).
- Paging engine, a11y (role group / carousel, polite live region, bead
  rail with `aria-current`, focus-on-mount) untouched from Phase 10; beads
  are 8 now.

## The transform-clobbering gotcha (important for future compositions)

`app-float` (and any shared keyframe animating `transform`) **replaces** an
element's static `transform` — a card posed with `transform: rotate(...)`
or centred with `translate(-50%, -50%)` silently loses that pose the moment
the animation starts. Two bugs were caught and fixed this way in review:
the flashcard front card sat off-centre, and the "coming soon" roadmap dot
(borrowing `pp-node-pulse`, whose keyframes carry the graph nodes' centring
translate) floated above its track line. The rules now used throughout:

- Static poses use the **individual properties** `rotate:` / `translate:` /
  `scale:` (they compose with an animated `transform`).
- Geometric centring uses `left/top: calc(50% - halfsize)`, never a
  transform, on anything that floats.
- In-flow pulses get their own keyframes (`pp-soon-pulse`) without the
  absolute-positioning translate.

## Other files touched

- `locales/en-US.ts` + `zh-CN.ts`: `landing.product.slides.engine.*` added
  (parity spec still 8/8).
- `components/icons/registry.ts`: `cpu` (lucide `Cpu`) added.
- No changes to GlassDock, GlassSurface, LoginView, SponsorPanel, glass.css,
  motion.css, components/index.ts — the gallery state machine and dock are
  exactly as Phase 10 left them.

## Verification record (2026-07-18)

- `vue-tsc --build` ✓, `npm run lint` (oxlint + eslint) ✓,
  `npm run test:unit` (8/8) ✓, `npm run build` ✓ — all re-run on final code.
- Playwright/Chromium sweep — **50/50 checks** across desktop dark
  (1440×900), mobile touch (390×844), reduced motion, light scheme: filter
  budget exactly 2 in both galleries; 8 beads; hero gradient headline +
  sheen + non-black atmosphere; every composition present (3 tutor cards,
  3 note cards + 2 threads, 3-card stack, 6 nodes + 8 links + core, 14
  particles + core + halo, 4 roadmap stops with 1 pulsing); rapid wheel =
  exactly one slide; finite-deck clamp at coming; ArrowUp/Home paging;
  dock bounding box unchanged across the whole deck, hover transform
  `none`, z 50 over overlay 40; live region announces; Escape closes with
  card return + focus restored to the Product facet; mobile swipe pages,
  compositions scale 0.72, no horizontal scroll; reduced motion static +
  functional; light scheme identical (fixed palette); zero page errors.
- Roadmap dot alignment probe: all four dot centres within 0.00px of the
  track after the pulse fix.
- Screenshots reviewed for every slide plus dock-over-color and mobile:
  atmospheres read premium, the dock visibly refracts the colored fields,
  compositions are balanced after the transform fixes.
- Standing caveats: real Safari/Firefox still not verified locally (same as
  Phases 8–10). Individual `rotate`/`translate`/`scale` properties require
  Chrome 104+/Safari 14.1+/Firefox 72+ — well inside the project's existing
  baseline (`color-mix`, `light-dark`).

## Watch items

- Playwright note: real CDP wheel events can be delayed past the transition
  lock under heavy render load — test paging with synthetic
  `WheelEvent` dispatch (see scratchpad script pattern) or you'll get
  flaky double-advances that are probe artifacts, not bugs.
- The atmosphere cross-fade (1600ms) outlives the transition lock (1050ms):
  paging quickly can briefly stack two leaving `.atmo` layers — harmless
  opacity dissolve, but don't add per-atmo filters or the compositor cost
  doubles during that window.
- Trackpad inertia can still push a second advance if momentum events
  arrive after lock + idle reset — unchanged Phase 10 behavior, accepted.
- The roadmap timeline reuses feature titles as labels; if a locale ever
  makes those long, `.roadmap-label { max-width: 9em }` wraps them.
- If a future slide composition needs a floating element with a static
  pose, follow the individual-property rules above.

## Phase 12 candidates

Unchanged from the Phase 10 list (registration/email/password-reset,
payments, OSS upload, external auth, spaced repetition, WebSockets,
Docker/CI, real sponsor links + WeChat QR asset, Phase 9 material on
Welcome/app chrome, compressed lotus asset for login LCP).
