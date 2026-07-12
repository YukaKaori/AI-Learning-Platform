# Design System

Phase 3 deliverable. Full component/token reference for `ai-learning-web`. Binding
conventions live in `docs/architecture.md` — this document explains them.

## Philosophy

An AI-native learning workspace, not an admin dashboard. The visual language takes
cues from Apple, Linear, Notion, Raycast, Cursor, Vercel and Arc: calm, minimal,
content-forward, quietly premium. Concretely:

- **Restraint over decoration.** Soft shadows, not material elevation. Motion that
  clarifies state changes, never motion for its own sake.
- **Tokens, always.** Every color, spacing, radius, shadow, font size, and duration in
  a component traces back to a token. If a value isn't in `tokens.css`, it doesn't
  belong in a component's `<style>` block.
- **Semantic naming.** Tokens describe role (`--color-danger`, `--radius-card`), never
  the raw value (no `--red-500`, no `--8px`). This is what lets the whole product
  re-skin from one file.

## Token architecture

All tokens live in `src/styles/tokens.css` (base scales + light/dark values) and
`src/styles/motion.css` (transition/keyframe tokens). `src/styles/element-theme.css`
maps Element Plus's internal variables onto these tokens so EP components inherit the
same language instead of their default "admin" look.

### Typography

Two layers: primitives (`--text-xs` … `--text-3xl`, `--leading-*`, `--tracking-*`) and
a semantic scale built on top of them:

| Token prefix | Use |
| --- | --- |
| `--font-display-*` | Hero/marketing-weight headings. Responsive via `clamp()`. |
| `--font-headline-*` | Page titles (`AppPageHeader`). |
| `--font-title-*` | Section/card titles (`AppSection`, `AppCard`). |
| `--font-body-*` | Default reading text. |
| `--font-caption-*` | Secondary/supporting text. |
| `--font-label-*` | Uppercase-style micro labels (form labels, eyebrow text). |

Each has `-family`, `-size`, `-weight`, `-leading`, `-tracking` sub-tokens.
`--font-mono` is the monospace family for code/data.

### Color

Semantic roles, not raw hues — every role has a light and dark value in `tokens.css`:

`primary`, `secondary`, `success`, `warning`, `danger`, `info` (each with a `-soft`
tint for backgrounds and `-hover`/`-active` where interactive), plus `bg`, `surface`,
`surface-hover`, `border`, `border-strong`, `muted`, `muted-soft`, `overlay` (backdrop
scrim), `text`/`text-secondary`/`text-tertiary`, and `focus-ring`.

### Spacing

4px-rooted scale: `--space-0-5` (2px) through `--space-24` (96px), matching the
requested 8px rhythm (`--space-2` = 8px, `--space-4` = 16px, …) while keeping a 4px
half-step for tight UI (icon gaps, tag padding).

### Radius

Base scale `--radius-sm/md/lg/xl/full`, plus semantic aliases components actually
consume: `--radius-button`, `--radius-input`, `--radius-card`, `--radius-dialog`,
`--radius-panel`, `--radius-glass`. Change the alias, not the component, to retune a
surface's roundedness.

### Shadow

`--shadow-sm/md/lg` for resting elevation, `--shadow-float` for popovers/dropdowns,
`--shadow-glass` for translucent floating surfaces (dialog, drawer). Every shadow has
a distinct dark-mode value — dark shadows use higher opacity black, not the same
rgba as light mode.

### Glass tokens (Phase 4+ prep)

`--glass-bg`, `--glass-border`, `--glass-blur` back the glass `AppCard` variant and
the `.el-dialog`/`.el-drawer` surfaces today. They are **not** a full theme yet — see
[Theme engine](#theme-engine).

### Motion

`--duration-fast/base/slow` + `--ease-out/in-out/spring` in `tokens.css`;
`--motion-scale-hover/press` and the `app-fade` / `app-scale` / `app-slide-up` /
`app-slide-down` keyframes + Vue `<Transition>` name classes in `motion.css`. All
motion is disabled under `prefers-reduced-motion: reduce`.

### Breakpoints

Fixed reference values, documented (not tokenized — CSS media queries can't consume
custom properties): `640 / 768 / 1024 / 1280`. Components hard-code these literals in
`@media` rules; `AppLayout`'s sidebar collapse uses `768px`.

## Component library

`src/components/` — flat file layout, one component per file, explicit named exports
from `src/components/index.ts`. No auto-import: this mirrors `vite.config.ts`'s
`unplugin-vue-components`, which is deliberately scoped to Element Plus only.

### Custom, token-built (signature surfaces)

`AppButton`, `AppInput`, `AppCard`, `AppAvatar`, `AppTag`, `AppBadge`, `AppEmpty`,
`AppLoading`, `AppSkeleton`, `AppSection`, `AppPageHeader`, `AppSearch`.

These own their full visual implementation — no Element Plus underneath — because
they're either simple enough to hand-roll (avatar, tag, badge) or are the product's
signature look-and-feel surfaces where full control matters (button, input, card).

### Themed wrappers over Element Plus (complex primitives)

`AppDialog`, `AppDrawer`, `AppTooltip`, `AppPagination`.

Positioning, focus-trap, Esc-to-close, and popper placement are hard problems EP has
already solved correctly and accessibly. These wrappers restyle the EP output via
`element-theme.css` (glass surfaces for dialog/drawer, dark popper for tooltip,
token-driven radius for pagination) rather than reimplementing that logic — matching
the constitution's "EP for complex primitives" rule.

### Shared vocabulary

`src/components/types.ts`: `Size` (`sm|md|lg`), `Tone` (`primary|secondary|success|
warning|danger|info`), `ButtonVariant`, `CardVariant`. Reuse these unions in new
components instead of inventing parallel ones.

### Conventions

- TypeScript props via `defineProps<T>()` + `withDefaults`; emits via `defineEmits<T>`.
- Two-way state via `defineModel` (not manual `modelValue`/`update:modelValue` wiring).
- Every color/spacing/radius/shadow/duration in `<style scoped>` is a `var(--token)` —
  no literals except pixel values that are themselves the scale's raw output (e.g. an
  icon-tile's fixed layout width).
- `:focus-visible` is never suppressed; the global ring (`base.css`) covers most
  interactive elements automatically because native `button`/`input`/`a` elements are
  used, not `div`s with click handlers.
- Icon-only interactive elements always carry `aria-label`.

## Icon strategy

`AppIcon` is the **only** file (besides `src/components/icons/registry.ts`) allowed to
import from the underlying icon library (`lucide-vue-next`). Application code requests
icons by string name:

```vue
<AppIcon name="search" size="sm" />
```

`registry.ts` maps `IconName` (a string-literal union) to the actual Lucide component.
Swapping icon providers later — a different library, a self-hosted sprite sheet — means
editing only `registry.ts` and `AppIcon.vue`; no call site changes.

## Theme engine

`src/stores/app.ts` owns theme state: `light | dark | system`, persisted to
`localStorage`, reactive to OS `prefers-color-scheme` changes when in `system` mode,
applied via the `html.dark` class (`stores/app.ts` → `applyTheme()`). Every token in
`tokens.css` has a value under `:root` (light) and an override under `html.dark`.

**Extension point**: a `glass` mode is not implemented, but the tokens it would need
(`--glass-*`) already exist and are already used by floating surfaces. Adding it later
means extending the `ThemeMode` union and adding a second class toggle
(`html.theme-glass` or similar) — no restructuring.

## Layout system

`src/layouts/`:

- `AppHeader.vue` — thin top bar, visible only below the `768px` breakpoint. Carries
  the mobile nav toggle.
- `AppSidebar.vue` — brand, nav, and the theme/locale/user controls. Rendered twice:
  once as the always-visible desktop rail, once inside `AppDrawer` for mobile (same
  component, no duplicated markup).
- `AppLayout.vue` — composes header + static sidebar + mobile drawer + `<RouterView>`
  content area. Responsive collapse is pure CSS (`display: none` past the breakpoint)
  plus one `ref` for the drawer's open state — no resize listeners.

## Accessibility baseline

- Every custom interactive component is a native `button`/`input`/`a` — keyboard
  operability and correct semantics come for free.
- Global `:focus-visible` ring driven by `--color-focus-ring` (`base.css`).
- Icon-only controls require `aria-label`; decorative icons are `aria-hidden`.
- `AppLoading`/`AppEmpty` expose `role="status"`; form errors use `role="alert"` and
  `aria-describedby`.
- Complex overlay a11y (focus trap, `aria-modal`, `Esc` handling) is inherited from
  Element Plus in the wrapped components — not reimplemented.
- `prefers-reduced-motion: reduce` disables all token-driven animation globally.

## Verification / living reference

`/design-system` (route `design-system`, inside the authenticated `AppLayout`, not
linked from nav) renders every component in its variants — use it to eyeball tokens
and components together, in both themes and both locales, rather than trusting this
document alone.

## Explicitly out of scope (Phase 3)

No business modules, no AI features, no dashboards, no login redesign. The premium
login experience and the full glass theme are Phase 4+.
