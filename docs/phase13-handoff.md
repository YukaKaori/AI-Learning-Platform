# Phase 13 Handoff — Premium SaaS Landing Transformation

**Status: Phase 13 is COMPLETE** as of 2026-07-18 (implemented and verified
in one session, NOT committed — awaiting approval). Phase 13 is PURE UI work
on the Product presentation plus locale additions: no backend, no auth
changes, no routing changes, no new dependencies, no WebGL. **Do not start
Phase 14 without approval.**

## The reframe

Phase 12 turned the Product page from a dark keynote into a **bright product
website**. Phase 13 turns that website into a **real premium AI SaaS landing
page ready for launch** (Apple / Linear / Raycast / Stripe / OpenAI
register). The difference is content and story, not chrome: Phase 12's mockups
were elegant skeleton bars; Phase 13's mockups carry real words — questions
with answers, notes with sentences, cards with prompts, graph nodes with
names, an engine that shows its reasoning steps.

The page now tells a product story instead of listing features:

```
Vision      → hero      (identity, one strong claim, a real workspace window)
Product     → tutor / notes / flashcards  (real UI a user would recognise)
Experience  → graph / engine  (labelled knowledge, visible reasoning)
Trust       → trust     (product principles + honest metrics)
Future      → roadmap → cta   (a strong closing door, not "coming soon")
```

Eight sections became **nine**: the "Coming Soon" ending is replaced by a
**Trust** section (principles + metrics) and a **CTA** section (headline +
action button). The user leaves the page understanding what it is, why it
exists, how it works, why it is better, and why they should use it.

## Architecture — unchanged from Phase 12

Everything the brief told me to preserve is preserved:

- **Login page** — untouched (the only LoginView change is the existing
  `is-on-light` dock-ink binding from Phase 12, which already keys off
  `gallery === 'product'` and needs no change for the 9-section layout).
- **GlassSurface** — not rewritten, not touched.
- **GlassDock** — geometry, material, motion untouched. The Phase 12
  `--dock-halo` token seam still carries the dark-ink flip over the bright
  page. No new dock states, no bounce, no scale (verified byte-stable).
- **No new dependencies, no WebGL / Three.js.** Every mockup is CSS-only DOM.
- **Filter budget: exactly 2** displacement filters page-wide (sign-in slab +
  dock), verified in both desktop and mobile product mode.
- **Motion system** — still the house `[data-reveal]` + `useScrollReveal`
  vocabulary with `--reveal-delay` staggering and `app-float` loops. No
  springs, no bounce. Reduced motion reveals everything immediately and
  freezes loops (global override), smooth scroll drops to instant jumps.
- **Scroll model** — still a fixed full-screen NATIVE scroll container (z 40,
  under the dock at 50) with `scroll-snap-type: y proximity`. The
  wheel-over-dock forwarding listener and `preventScroll` focus are carried
  over verbatim.

## ProductPresentation.vue — rewritten (the only component changed)

- **Nine sections**, `SECTIONS` array now
  `hero · tutor · notes · flashcards · graph · engine · trust · roadmap · cta`.
  Each keeps `min-height: 100dvh`, `scroll-snap-align: start`, its own pastel
  atmosphere (three drifting underlight blobs) and its own accent pair
  (`--sec-accent` / `--sec-accent-2` — every section now defines both, so
  gradients read consistently).
- **Hero** is the strongest moment: a product identity pill, a gradient
  headline on a real emotional claim (`hero.title` + `hero.line`), and a
  realistic **workspace window** in normal flow — a three-pane app mockup
  (sidebar of modules · a live tutor conversation with suggestion chips · a
  "this week" progress panel with done/now rows). This is the hero's subject,
  not decoration.
- **Real mockups** everywhere, all copy localized:
  - *Tutor* — a conversation with a real Q ("Why does quicksort degrade to
    O(n²)?") and a real answer, two suggestion chips, a learning-context panel.
  - *Notes* — a document window with a title, a body sentence, an **AI summary**
    highlight block, three **linked-concept** pills, plus the Phase 12 dark
    code panel and an "AI polish" chip.
  - *Flashcards* — a card mid-review with a real question, "tap to reveal", the
    three **spaced-repetition** interval chips (Again / Good / Easy) and a
    "Card 12 of 30" progress row.
  - *Graph* — the Phase 11 constellation re-lit, now with **named** nodes
    (Machine Learning core + 6 concepts) and one highlighted **learning path**
    (brighter links + path-coloured nodes) with a "Your learning path" pill.
  - *Engine* — a **reasoning pipeline** panel (4 checkpoints: done / done /
    active-pulsing / pending) beside the breathing core + halo + 12 particles,
    plus a streaming-output window with the blinking caret.
- **Trust** — a centered principle grid (4 cards: Learners first · AI-native ·
  Private by default · Always evolving) and an honest metric row
  (5 modules · 2 languages · 100% streaming · 0 ads/trackers). No invented
  companies, customers, or testimonials — philosophy and truthful numbers only,
  per the brief.
- **CTA** — a soft orb, a large headline ("The future of learning starts
  here"), a one-line description, and a real gradient **action button**
  ("Start learning" / "开始学习") that exits to sign-in via the existing
  `close` event. Sign-in IS the product's front door, so the CTA reuses it —
  no new routing.
- **Navigation enhancement** — the bead rail is now a **labelled** rail: each
  item is `dot + name`; the active item (and hover/focus) reveals its section
  name with a quiet opacity+slide, everything else stays a bare dot. Active
  section is tracked by an IntersectionObserver on the container's **center
  band** (`rootMargin: -45% 0px -45% 0px`) rather than a fixed visible-fraction
  threshold — this stays correct for sections taller than the viewport (the
  mobile trust section). `aria-current` follows; click = `scrollIntoView`
  (CSS decides smooth vs instant). Labels hide on mobile (dots only).

## Localization

- **en-US.ts** and **zh-CN.ts** both gained the same keys under
  `landing.product`: `hero.title` + 9 workspace strings; `tutor` gained q/a +
  2 chips + ctx label + 3 ctx rows; `notes` gained doc/body/summary/chip + 3
  links; `flashcards` gained q/reveal/count + 3 SRS labels; `graph` gained
  core + 6 node names + path; `engine` gained steps label + 4 step names +
  output; new `trust` block (title/line + 4×title/desc + 4×metric value/label);
  `roadmap` unchanged; **`coming` removed, `cta` added** (title/line/action).
- Locale parity test still passes (en-US and zh-CN cover identical key sets,
  no empty strings).

## Visual depth

Every section keeps three layers per the brief: a **background** atmosphere
(gradient base + up to three drifting blurred color fields), a **middle**
product-UI layer (the mockup), and the **foreground** glass dock floating
above (z 50). Depth comes from the layered compositions and soft shadows, not
from more animation.

## Files touched

- `components/experience/ProductPresentation.vue` — full rewrite (9 sections,
  real mockups, labelled nav, center-band scrollspy, CTA button).
- `locales/en-US.ts` + `locales/zh-CN.ts` — Phase 13 product strings
  (see Localization). `coming` → `cta`; new `trust` block; mockup copy.
- **Nothing else**: GlassSurface, GlassDock, glass.css, motion.css, tokens.css,
  LoginView, SponsorPanel, useScrollReveal, icon registry, router,
  components/index.ts — all untouched. All mockup icons are pre-registered
  names (graduation-cap, brain, shield, trending-up, network, cpu, layers,
  notebook-pen, sparkles, code, link, route, check, check-circle, circle-dot,
  message-square, arrow-right, chevron-down).

## Performance notes

- Filter budget unchanged: **exactly 2** displacement filters (verified
  desktop + mobile). No GlassSurface added to this page.
- All motion is transform/opacity only (compositor-friendly): `app-float`,
  `app-underlight-*`, the reveal transition, and the small in-mockup pulses
  (`pp-node-pulse`, `pp-halo`, `pp-rise`, `pp-caret`, `pp-soon-pulse`). No
  new keyframes beyond the Phase 12 set except reuse. Equal-or-better than
  Phase 12 (same number of animated layers class-wise; slightly more DOM text
  nodes, no new filters or blurs).

## Verification record (2026-07-18)

- `vue-tsc --build` ✓, `npm run lint` ✓ (oxlint + eslint), `npm run test:unit`
  (8/8, incl. locale parity) ✓, `npm run build` ✓.
- Playwright/Chromium sweep — **67/68 checks** across four contexts. The one
  non-pass is a **test-arithmetic assumption**, not a product defect: the
  dot-click test hardcoded that section 6 begins at exactly `6×900px`, but
  sections can exceed `100dvh` when content is tall, so trust actually starts
  at 5572px. The authoritative signals for that same click — the real
  center-band scrollspy marking the trust nav item `active` and
  `aria-current="true"` — both passed, confirming the click landed on and
  centered the trust section.
- **Desktop dark (1440×900)**: 9 sections + 9 nav items; Escape round-trip
  (dock ink flip on then reverted); filter budget exactly 2; scrolls ~9
  viewports; no horizontal overflow (container + document); every mockup's
  real content asserted (hero workspace 4 sidebar items / 3 ctx rows / 2 chips
  / real bubble text; tutor real Q&A + 2 chips + 3 ctx rows; notes title +
  AI-summary block + 3 links; flashcard real question + 3 SRS chips + "30"
  count; graph 7 tags + 2 path links + path pill; engine 4 steps + real stream
  text + 12 particles; trust 4 cards + 4 metrics; roadmap 4 stops + 1 soon
  pill; CTA labelled button); reveals fire on arrival and not before (trust
  cards hidden until scrolled to); labelled nav active label visible at bottom;
  dot-click navigation + aria-current; wheel-over-dock forwarding scrolls;
  **dock bounding box byte-identical** before/after interaction; stage
  scrollTop stays 0; CTA button exits to sign-in and unmounts the product
  layer; zero console errors.
- **Desktop light (1440×900)**: 9 sections; bright root (`rgb(251,250,255)`);
  zero console errors. Screenshots identical premium read to dark.
- **Reduced motion**: all `[data-reveal]` revealed immediately; dot jump works
  (instant); zero console errors.
- **Mobile (390×844, touch)**: 9 sections; filter budget 2; no horizontal
  overflow (container + document); workspace side rail + engine steps + nav
  labels hidden; real swipe (CDP synthesizeScrollGesture) scrolls the page;
  trust grid collapses to single column; zero console errors.
- Screenshots reviewed for all 9 desktop sections + light + 3 mobile: bright,
  premium, alternating layouts read as a real launch-ready product site; dock
  labels legible in dark ink over every section.
- Standing caveats (unchanged from Phases 8–12): real Safari/Firefox still
  unverified locally; `overflow: clip` / `color-mix` / `light-dark` baseline
  needs Chrome 90+ / Safari 16+ / Firefox 81+.

## Watch items

- **Center-band scrollspy** (`rootMargin: -45% 0px -45% 0px`, threshold 0):
  a section shorter than ~10% of the viewport would never overlap the band and
  never activate its dot. All sections are ≥100dvh today.
- The nav `.pp-nav-label` is `position`-less inline flex text with
  `white-space: nowrap` capped at `14em` + ellipsis; a very long localized
  section title would truncate rather than wrap (intentional).
- The CTA button emits `close` (same as Escape / the dock Login facet) — it
  returns to the sign-in gallery, it does NOT authenticate. If a future phase
  wants "Start learning" to deep-link post-auth, wire it through the router
  instead of `close`.
- All Phase 12 watch items still stand (wheel-forwarding writes scrollTop
  directly; useScrollReveal observes the viewport not the container; don't give
  the presentation root a transform; keyboard scroll needs focus inside the
  container).

## Phase 14 candidates

Unchanged from the Phase 10/12 list (registration/email/password-reset,
payments, OSS upload, external auth, spaced repetition, WebSockets, Docker/CI,
real sponsor links + WeChat QR asset, Phase 9 material on Welcome/app chrome,
compressed lotus asset for login LCP).
