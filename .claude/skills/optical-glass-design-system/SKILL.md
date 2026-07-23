---
name: optical-glass-design-system
description: The project's glass design language — optical rules, motion philosophy, design tokens, architecture constraints, and an implementation checklist. Read BEFORE designing or implementing ANY glass, translucent, frosted, dock, overlay, dialog, palette, or premium-surface UI in this repository.
---

# Optical Glass Design System

The design language distilled from the GlassSurface and FluidGlass specifications
and refined through Phases 8–14. This document is the constitution for every
translucent surface in the AI Learning Platform. It describes a *material*, not
a component library. No framework code belongs here.

---

## 1. Purpose

This skill exists so that every future surface in the product feels cut from
the same slab of glass.

The product's visual identity is built on **heavy optical glass** — not the web's
default "frosted rectangle" idiom. Ordinary glassmorphism is a white blur with a
border; it hides content and signals nothing. Our glass is a *physical material*:
it has thickness, mass, density, an entrance face and an exit face. Light enters
it, bends, disperses slightly into color at the edges, reflects off internal
surfaces, and leaves. Content behind the glass is *revealed through* it —
refracted, clarified, never buried.

The philosophy is realism over spectacle. A real slab of smoked glass on a desk
is beautiful because of how it behaves under light, not because it moves. So the
system spends its budget on optics — depth, refraction, edge energy, reflections —
and is deliberately miserly with motion. When something does move, it is the
*light* that moves, or the *reveal* of content; the glass itself stays heavy and
still.

Every future phase (Knowledge Graph, AI Workspace, Command Palette, premium
surfaces) should be able to read this document and produce UI that is
indistinguishable in material from the login card built in Phase 9 and the dock
built in Phase 12.

## 2. Core Principles

1. **Heavy optical glass, not film.** Surfaces read as thick slabs with mass.
   Thickness is expressed through a double edge (bright entrance lip + darker
   inner contour), a back-face reflection, and internal scattering — never
   through bigger drop shadows.

2. **Transmission before frost.** The primary job of glass is to *transmit* the
   scene behind it. Legibility is earned with smoked neutral-density tint
   (darkening), not with opaque white frost. Frost opacity defaults to zero.

3. **Refraction is the signature.** The background visibly bends at the surface,
   strongest at edges and corners where a real lens distorts most. Refraction is
   established once per surface, not recomputed per frame.

4. **Chromatic dispersion at the rim.** Edges split light subtly: a cool cast on
   the light-facing edge, a warm cast opposite (or per-channel offsets in the
   refraction itself). It is a whisper — visible when you look for it, invisible
   when you don't.

5. **Internal depth before blur.** The impression of depth comes from layered
   internal cues — front rim, inner back rim, back-face reflection, faint
   caustics — not from cranking blur. Blur alone is flat; depth is layered.

6. **Fresnel reflection.** Glass reflects more at grazing angles. Edge highlights
   are directional and follow the scene's light source; the rim facing the light
   glows, the far rim falls dark. Uniform borders on all four edges are wrong.

7. **Smoked ND glass.** The house material is dark — a neutral-density smoke
   tint, theme-agnostic (never white in light mode). On-glass text is a fixed
   dusk palette; theme-relative text on a smoked slab breaks.

8. **Layered reflections.** A surface carries several independent light layers —
   permanent inner glow, edge glow, a travelling sheen, proximity lift — each
   gated by its own variable, each subtle, composing into one living material.

9. **One scene, one light.** Every surface in a view answers to the same implied
   light source. Highlights, Fresnel arcs, and sheens must agree on direction.
   Competing light sources shatter the illusion instantly.

10. **Physical light behavior over decoration.** Every optical effect must be
    explainable as physics: "the light source is up-left, so this rim glows."
    If an effect exists only because it looks cool, it doesn't ship.

11. **Glass reveals content; it never hides it.** If a glass treatment reduces
    the legibility or discoverability of what's under or on it, the treatment
    loses, not the content.

12. **Subtle motion, meaningful motion.** Idle surfaces are almost still. Motion
    exists to communicate material (light drifting across a slab), state
    (proximity to a light), or causality (a press settling under mass) — never
    to attract attention.

13. **Glass has mass.** Interactions are damped: a pressed control settles by a
    fraction of a pixel; nothing springs, bounces, or overshoots. Weight is part
    of the material's honesty.

14. **Realism over flashy animation.** When a choice arises between a more
    physically plausible static rendering and a more animated one, choose
    plausibility. Our surfaces impress by holding up to a second look.

## 3. Motion Principles

- **Reflections move; objects don't.** The cursor, scroll position, and nearby
  "light sources" steer highlights, sheens, and Fresnel arcs *across* surfaces.
  The surfaces themselves stay planted. Cursor influences light, not geometry.
- **Idle animation must be near-imperceptible.** The optional surface-flow layer
  (travelling highlight + faint caustics) runs on 20–40 second loops at low
  opacity. If a user consciously notices the idle loop, it's too strong.
- **Interaction first, decoration second.** Motion budget goes to feedback the
  user caused — hover pooling light, focus lifting an edge, press settling —
  before any ambient effect.
- **One-shot over infinite.** Entrances, typewriter reveals, and emphasis
  animations play once and rest. Infinite loops are reserved for the single
  ambient light layer, if used at all.
- **Scrolling reveals; it doesn't transition.** Scroll exposes content already
  present in the scene (parallax of light, sections snapping into view). It
  never triggers theatrical wipes, flips, or fades that replace the scene.
- **Motion carries state, cheaply.** All reactive lighting flows through custom
  properties driving gradients, transforms, and opacity — properties that
  composite without layout or filter recomputation. The refraction chain is
  never animated per frame.
- **Reduced motion is first-class.** With `prefers-reduced-motion` (and on
  touch, where there is no pointer to be a light), light-tracking strength
  stays at zero and ambient loops freeze — by construction (the variables
  default to 0), not by patch.

## 4. Architecture Rules

1. **One glass primitive.** `GlassSurface`
   (`ai-learning-web/src/components/experience/GlassSurface.vue`) is the sole
   refracting primitive. New surfaces compose it; they never re-implement
   displacement filters, and never fork a second glass component. (FluidGlass
   was evaluated and rejected — WebGL demo scene, not portable. Its *ideas* —
   bar mode, transmission, chromatic aberration — were translated into
   GlassSurface + CSS instead. Keep it that way.)
2. **The material system is CSS.** On-glass control skins live in
   `ai-learning-web/src/styles/glass.css` under `.glass-material`. New on-glass
   controls extend that file; they don't carry private glass styles.
3. **CSS variables drive appearance.** All optical state (`--glass-depth`,
   `--glass-density`, `--glass-fresnel`, `--glass-light-*`, `--glass-proximity`,
   `--glass-flow-opacity`, …) is custom-property-gated with inert defaults.
   A surface that opts into nothing renders as the calm baseline. Stage logic
   (e.g. `useGlassSpotlight`) writes variables; components never reach into
   each other.
4. **No WebGL unless absolutely necessary.** SVG filters + CSS deliver the
   material. A WebGL dependency requires a written justification that CSS/SVG
   cannot achieve the effect, plus a full non-WebGL fallback.
5. **Performance budget.** Reactive lighting uses compositor-friendly channels
   only (opacity, transform, gradient positions via variables). Expensive work
   (displacement-map generation) happens on mount/resize, never per frame.
   No new per-frame JS loops for decoration.
6. **Progressive enhancement with real fallbacks.** Browsers that can't apply
   SVG backdrop filters (Safari, Firefox) get the frosted fallback and must
   remain fully legible and functional. Feature-detect; never UA-gate features.
7. **Composition over inheritance.** Build a dock, dialog, or palette by
   placing content on a GlassSurface and applying `.glass-material` — not by
   subclassing or copying the surface.
8. **Scoped, not global.** Glass token remaps apply only inside the material
   container. The rest of the app keeps its solid-surface skins; base
   components (AppInput, AppButton) are never modified for glass.
9. **Accessibility is not traded for optics.** Focus rings, contrast on
   on-glass text, keyboard reachability, and touch behavior survive every
   glass treatment.

## 5. Design Tokens

Conceptual dials of the material. Each maps to existing custom properties or
props; future work adjusts these dials rather than inventing new mechanisms.

- **Glass Density** (`--glass-density`, `--glass-tint`) — how much smoke is in
  the slab; the legibility dial. High density = dark, ND-filter glass (login
  card); low density = clear water glass (dock).
- **Glass Temperature** — the color bias of tint and edge casts: cool
  (blue-white, default up-light side) vs. warm (amber, down-shadow side).
  Both temperatures appear on one surface only as the two sides of dispersion.
- **Edge Energy** (borderWidth, distortion scale, channel offsets) — how
  strongly the rim bends and splits light. High at hero surfaces, low at quiet
  utility panels.
- **Reflection Strength** (`--glass-inner-glow`, `--glass-edge-glow`,
  `--glass-light-strength`) — how much of the scene's light the surface throws
  back. Rises with proximity to a light source, falls at rest.
- **Optical Depth** (`--glass-depth`) — presence of the thickness cues: front
  rim, back rim, back-face reflection, internal scatter. The "is this a slab
  or a film" dial.
- **Transmission** (backgroundOpacity ≈ 0, saturation) — how much of the
  background passes through. High transmission is the default; frost is a
  fallback state, not a style.
- **Surface Flow** (`--glass-flow-opacity`, surfaceFlow) — presence of the
  slow ambient light traversal. The "is this material alive" dial; near zero
  at idle.
- **Material Weight** — interaction damping: transition durations slightly
  heavier than app defaults, sub-pixel press settle, no springs. Heavier glass
  = slower, calmer responses.

## 6. Allowed Components

Surfaces that should be built from this system (existing and future):

- Dock / navigation bar (exists: GlassDock — clear water-glass bar)
- Auth and smoked-glass cards (exists: login card)
- Sidebar (future collapsible navigation)
- Command Palette (the flagship candidate: a floating slab over the workspace)
- Dialogs and modal sheets (confirmations, premium upsells)
- Cards elevated above content (stats, achievements, sponsor cards)
- Context menus and dropdown panels
- Floating toolbars (editor/selection toolbars)
- Widgets (timers, streaks, quick actions)
- Timeline overlays and scrubbers
- Knowledge Graph overlays (node inspectors, legends, filters floating over the graph)
- Search overlays
- Premium subscription dialogs and plan cards
- Toast/notification stack (light-touch, low density)

Not everything is glass: dense reading surfaces (lesson bodies, tables, code
editors, long forms) stay solid. Glass marks *elevated, transient, or premium*
layers — the things floating above the work, never the work itself.

## 7. Forbidden Patterns

- ❌ **Fake blur** — `background: rgba(255,255,255,.2)` posing as glass with no
  backdrop interaction.
- ❌ **White frosted rectangles** — the generic glassmorphism card; our glass is
  smoked or water-clear, never milk.
- ❌ **Constant floating / hover-bobbing** — surfaces have mass; they don't levitate.
- ❌ **Bounce and spring animations** — overshoot contradicts weight.
- ❌ **Decorative glass without function** — if the layer isn't elevated,
  transient, or premium, it isn't glass.
- ❌ **Random gradients** — every gradient is a light with a direction and a
  reason; no brand-colored washes for flavor.
- ❌ **Heavy drop shadows** — depth comes from internal optics, not from
  40px-blur black halos.
- ❌ **Duplicate glass implementations** — no second surface primitive, no
  copy-pasted filter chains, no one-off `backdrop-filter` blobs in page CSS.
- ❌ **Multiple competing light sources** — one implied light per scene.
- ❌ **Per-frame filter regeneration or layout-thrashing JS** for optical effects.
- ❌ **Theme-relative text on smoked glass** — on-glass palette is fixed dusk.
- ❌ **Glass that darkens/blurs its own content** — the material sits behind
  content, never on top of it.

## 8. Roadmap Integration

For every Phase 15+ feature (see `docs/roadmap-v1.md`), this skill is read
before UI design. How the language shows up naturally:

- **Knowledge Graph** — the graph canvas is the *scene* (a light source in
  itself); inspectors, legends, and filters float as smoked slabs whose
  reflections respond to the glow of nearby nodes.
- **AI Workspace / future AI features** — AI presence is expressed as light
  inside glass: a thinking state is a slow internal sheen, a completed answer
  a one-shot brightening. AI chrome is glass; AI *content* is solid and legible.
- **Command Palette** — the canonical heavy slab: high Optical Depth, high Edge
  Energy, appears with a one-shot settle (no bounce), results on solid rows
  within the material.
- **Search** — same slab family as the palette, lower density.
- **Sidebar** — low density, low motion; a quiet pane of the same material, not
  a hero surface.
- **Marketplace & Sponsor page** — sponsor and product cards use card-grade
  glass with proximity lighting; restraint keeps "premium" from becoming "busy."
- **Premium pages** — the highest expression of the material (max depth,
  fresnel, dispersion) since premium *is* the product's optical brand — while
  pricing/terms stay maximally legible.
- **Settings / Admin dashboard** — mostly solid; glass only for transient
  layers (confirm dialogs, pickers). Data tables are never glass.
- **Community** — user content on solid surfaces; glass reserved for
  composers, reaction popovers, and profile hover cards.
- **Focus Mode** — the strongest reveal-not-hide statement: surrounding chrome
  recedes into dim, low-density glass while the work stays bright and solid.

## 9. Implementation Checklist

Before implementing any future UI surface, verify:

- [ ] Read this skill; the surface is on the Allowed list (or justified as elevated/transient/premium)
- [ ] Uses the existing `GlassSurface` primitive — no new/duplicate glass implementation
- [ ] On-glass controls use `.glass-material` (glass.css), extended there if needed
- [ ] Optical hierarchy preserved — density/depth/edge energy match the surface's rank in the scene
- [ ] One light source: highlights, Fresnel, and sheens agree in direction with the rest of the view
- [ ] Legibility: content revealed, not hidden; on-glass text uses the fixed dusk palette; contrast verified
- [ ] Motion is meaningful: interaction-driven or near-imperceptible ambient; one-shot preferred; no bounce
- [ ] Performance budget respected: variables/opacity/transform only per frame; no filter regeneration; no new RAF loops
- [ ] Reduced-motion and touch degrade to zero light-strength by default
- [ ] Browser fallback verified (Safari/Firefox frosted path is legible and functional)
- [ ] Accessibility preserved: focus-visible rings, keyboard paths, semantics untouched by the material
- [ ] Uses the design-token dials (Density, Depth, Edge Energy, …) — no ad-hoc magic values outside glass.css
- [ ] Consistent with this Optical Glass Design System end to end
