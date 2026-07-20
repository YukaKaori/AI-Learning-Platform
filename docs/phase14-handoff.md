# Phase 14 Handoff — Product Realism & Premium Experience

**Status: Phase 14 is COMPLETE** as of 2026-07-20 (implemented and verified in
one session, NOT committed — awaiting approval). Phase 14 is **additive UI work**
on the Product presentation plus a small locale addition: no backend, no auth,
no routing, no new dependencies, no WebGL, no changes to GlassSurface / GlassDock
/ LoginView / glass.css / tokens.css / motion.css. **Do not start Phase 15
without approval.**

## The reframe

Phase 13 turned the Product page into a premium SaaS *landing page*. Phase 14's
objective is no longer visual redesign — it is **product realism**: every scene
should feel like a real working application a user could open today, not concept
art. The guiding principle was *less decoration, more authenticity* — so this
phase is a **refinement pass on the single existing component**
(`ProductPresentation.vue`), not a rewrite. Phase 13 had already done much of the
realism groundwork (real conversations, named graph nodes, SRS chips, a reasoning
pipeline); Phase 14 closes the remaining gaps and adds narrative connective
tissue.

## Design decisions (and the direction confirmed up front)

Two points in the brief were in genuine tension with the existing architecture,
so they were resolved with the user before any code was written:

1. **Motion philosophy.** Part 2 ("nothing should constantly move") directly
   reverses Phase 13, which deliberately kept nearly every mockup floating and
   the engine streaming particles forever. **Decision: keep Phase 13's ambient
   life untouched and only *add* realism** — no motion was removed. The one new
   motion is a genuine one-shot (below), which fits Part 2's spirit ("streaming
   once") without walking back the established feel.
2. **"Streaming once" over localized text.** A CSS-only typewriter is not
   reliable across variable-width fonts, wrapping, and two languages. **Decision:
   a small amount of component JS** drives a true one-shot type-out, guarded for
   reduced motion. "No new dependencies" is honoured — this is plain Vue + an
   `IntersectionObserver`, no libraries.
3. **Story framing.** Part 3/4 wanted the isolated feature blocks to read as one
   story. **Decision: add a chapter eyebrow** above each section title
   (Product · 01 → Experience · 05 → Principles → What's next), which needed four
   new localized `chapters` strings.

## What changed

### `components/experience/ProductPresentation.vue` (only component touched)

- **Chapter eyebrows (`.pp-kicker`)** on the seven titled scenes (tutor, notes,
  flashcards, graph, engine, trust, roadmap). A small uppercase tracked label
  with an accent tick, grouping the nine scenes into one arc. The five feature
  scenes carry a running number (`· 01`…`· 05`); the two closing chapters
  (Principles, What's next) carry only the chapter name. Driven by a `KICKERS`
  config + a `kicker()` helper that resolves the localized chapter label. The
  hero keeps its existing identity pill (its own eyebrow) and the CTA stays
  eyebrow-free (it is the closer). Badge margin trimmed `space-5 → space-4` to
  seat the eyebrow.
- **Engine one-shot typewriter.** The AI-Engine stream now types its localized
  output a character at a time the first time the engine scene scrolls into the
  container, then rests with the existing blinking caret. Implementation:
  `engineText` computed slices `engineFull` by a reactive `engineTypedCount`; a
  dedicated `IntersectionObserver` (root = the scroll container, threshold 0.4)
  fires `startEngineTyping()` **once** (`setInterval`, 26 ms/char) and then
  disconnects. Once complete, `engineDone` pins the full string so a later locale
  switch simply re-renders it whole. Reduced-motion users skip the observer
  entirely and get the full text immediately on mount. Timer + observer are
  cleaned up in `onBeforeUnmount`.
- **Notes: real code, not skeleton bars.** The floating dark code panel — the
  last purely-abstract mockup element — now shows a real, syntax-lit attention
  snippet (`scores = q @ k.T / sqrt(d)` / `weights = softmax(scores)` /
  `out = weights @ v`). Language-neutral (code), so no locale keys; reinforces
  the "rich text, images **and code**" point. The old `.code-line` / `.code-tok*`
  skeleton markup and CSS were removed.
- **Tutor: natural timestamps.** Each conversation bubble is stamped (`09:41`,
  numeric so it is locale-neutral), the sender's own time pulled to its side —
  a quiet realism cue the brief named explicitly.

Deliberately **not** done: flashcard back-card text. The back cards peek only a
~20 px strip above the front card, so any content would be cramped or hidden, and
adding it risks the Phase 11 "centred by geometry, not transform" rule. The front
card + SRS interval chips + progress row already deliver realistic review
content, so the back cards stay as a clean visual deck.

### `locales/en-US.ts` + `locales/zh-CN.ts`

- Added `landing.product.chapters` = `{ product, experience, trust, future }`
  (4 leaf keys per locale). en: Product / Experience / Principles / What's next.
  zh: 产品 / 体验 / 原则 / 未来规划. Nothing else changed; locale parity holds.

## Architecture preserved (Parts 6 & 8)

- **Filter budget: exactly 2** displacement filters (login card + dock), verified
  in both themes — ProductPresentation still mounts **no** GlassSurface; every
  new element is plain CSS/DOM.
- GlassSurface, GlassDock, LoginView, glass.css, tokens.css, motion.css,
  useScrollReveal, the icon registry, router and `components/index.ts` are all
  untouched. No new dependencies, no WebGL/Three.js.
- All existing Phase 13 motion (the ambient `app-float`, `pp-node-pulse`,
  `pp-halo`, `pp-rise` particles, `pp-caret`, `pp-soon-pulse`) is unchanged. The
  only new motion is the engine typewriter, a one-shot.

## Performance observations

- **Filter count unchanged (2).** The typewriter is transform-free — it mutates a
  reactive integer and re-renders a text node; no layout thrash, no new
  compositor layers. The notes code panel replaced three skeleton `<i>` bars with
  a handful of `<span>`s (net DOM roughly flat). Chapter eyebrows add seven small
  static text rows. Build output and bundle size are unchanged in character
  (no new modules). Equal-to-or-better than Phase 13.
- Reduced motion: the global override still freezes every loop; the typewriter
  short-circuits to full text on mount, so content is never hidden or animated.

## Verification results (2026-07-20)

- `npm run type-check` (vue-tsc --build) ✓
- `npm run lint` (oxlint + eslint, both `--fix`) ✓ — no churn beyond the three
  edited files.
- `npm run test:unit` — **8/8** ✓ (incl. locale-parity + no-empty-strings, which
  cover the new `chapters` keys).
- `npm run build` ✓ (built in ~2.5 s).
- **Playwright/Chromium sweep — 23/23 green** across three contexts (dark, light,
  reduced-motion), all at 1440×900:
  - 9 sections; **7 chapter kickers** present with a `chapter · 0N` label;
  - **filter budget = 2** (`svg filter[id^="glass-filter-"]`), both themes;
  - notes real code present (`softmax`) and **zero** `.code-tok` skeleton nodes;
  - tutor **2** `.chat-time` timestamps;
  - engine typed output > 20 chars after arrival with the caret present;
  - reduced-motion context shows the **full** engine text without scrolling;
  - **no horizontal overflow** (container + document), both themes;
  - **zero console errors**, both themes.
- Screenshots of all nine dark sections reviewed: tutor (eyebrow "产品 · 01" +
  `09:41` stamps), notes (real syntax-lit snippet), **engine caught mid-typewriter**
  ("结合你最近的测验与今天的笔记，下…"), trust/flashcards/roadmap centered eyebrows —
  all read as a real, premium, launch-ready product.
- No incorrect Playwright assertions were encountered (the Phase 13 section-6
  arithmetic assumption was not reused; navigation is driven by real nav-dot
  clicks + section content, not hardcoded pixel offsets).

## Watch items

- **Engine typewriter is one-shot per mount.** Leaving and re-opening the Product
  gallery remounts the component, so it types again on the next arrival — correct
  and intended. If a future phase caches the mounted layer, revisit `engineDone`.
- The typewriter observer uses the container as `root`; it relies on the engine
  stage being a real child of `.presentation` (it is). Don't give the stage its
  own transform/`overflow` that would break intersection.
- All Phase 12/13 watch items still stand (center-band scrollspy needs sections
  ≥ ~10% viewport; wheel-forwarding writes `scrollTop` directly; useScrollReveal
  observes the viewport, which is fine because the container is full-screen fixed;
  the CTA button emits `close`, it does not authenticate).
- Standing cross-browser caveat unchanged: real Safari/Firefox unverified locally;
  `overflow: clip` / `color-mix` / `light-dark` need Chrome 90+ / Safari 16+ /
  Firefox 81+.

## Phase 15 candidates

Unchanged from the Phase 10/12/13 list (registration/email/password-reset,
payments, OSS upload, external auth, real spaced repetition, WebSockets/streaming
backend, Docker/CI, real sponsor links + WeChat QR asset, Phase 9 material on
Welcome/app chrome, compressed lotus asset for login LCP).
