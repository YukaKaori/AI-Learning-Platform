<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppIcon from '../AppIcon.vue'
import { useScrollReveal } from '@/composables/useScrollReveal'

/**
 * Product page — a premium commercial product website (Phase 12).
 *
 * Phase 11's dark keynote becomes a bright product site: the login stage
 * stays the black gallery, and opening Product steps into a completely
 * different world — warm whites, soft pastel atmospheres, real landing-page
 * compositions. The contrast is the point: dark → bright, art → product.
 *
 * The paged keynote engine is retired. This layer is a NATIVE scroll
 * container with gentle CSS scroll-snap (`y proximity`): eight full-viewport
 * sections that scroll like apple.com, not like a slide deck. Sections
 * alternate composition — text left / visual right, flipped, centered,
 * a diagonal — and every visual is a CSS-only UI mockup (a conversation
 * window, a note editor with a code panel, a card stack in depth, a glowing
 * constellation, a streaming response, a milestone timeline). No WebGL, no
 * new dependencies, no GlassSurface here: the page keeps exactly two
 * displacement filters (sign-in slab + dock), and the dock floating above
 * finally refracts bright colored fields instead of darkness.
 *
 * Motion stays subtle: slow scroll-reveals via the house `[data-reveal]`
 * vocabulary, gentle `app-float` loops, drifting atmosphere blobs. No
 * springs, no bounce. Reduced motion reveals everything immediately
 * (useScrollReveal) and the global override freezes the loops.
 *
 * Escape (or the dock's Login facet) returns to the sign-in gallery via
 * the `close` event, exactly as before.
 */

const { t } = useI18n()

const emit = defineEmits<{ close: [] }>()

const SECTIONS = [
  'hero',
  'tutor',
  'notes',
  'flashcards',
  'graph',
  'engine',
  'roadmap',
  'coming',
] as const

const navItems = computed(() =>
  SECTIONS.map((key) =>
    key === 'hero' ? t('app.name') : t(`landing.product.slides.${key}.title`),
  ),
)

/** The three feature points shown beside a split section's headline. */
function points(key: 'tutor' | 'notes' | 'graph' | 'engine') {
  return (['p1', 'p2', 'p3'] as const).map((p) => t(`landing.product.slides.${key}.${p}`))
}

const roadmapStops = computed(() => [
  { label: t('landing.product.slides.roadmap.m1'), soon: false },
  { label: t('landing.product.slides.roadmap.m2'), soon: false },
  { label: t('landing.product.slides.roadmap.m3'), soon: false },
  { label: t('landing.product.slides.roadmap.m4'), soon: true },
])

const rootRef = ref<HTMLElement | null>(null)
const activeIndex = ref(0)

useScrollReveal(rootRef)

let sections: HTMLElement[] = []
let sectionObserver: IntersectionObserver | null = null

function goTo(i: number) {
  // behavior stays unset: the container's CSS scroll-behavior decides, so
  // reduced-motion users get an instant jump for free.
  sections[i]?.scrollIntoView({ block: 'start' })
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
  }
}

/* The dock floats above this layer as a sibling, so wheel input over the
   glass would otherwise fall into a dead zone (the stage beneath cannot
   scroll). Forward it into the page. */
function onWindowWheel(event: WheelEvent) {
  const root = rootRef.value
  if (!root || !(event.target instanceof Node) || root.contains(event.target)) return
  root.scrollTop += event.deltaY
}

onMounted(() => {
  const root = rootRef.value
  if (!root) return
  // The page takes the keyboard on arrival: arrows scroll, Escape leaves.
  // preventScroll matters: the login stage behind has hidden scrollable
  // overflow (the underlight bleed), and a plain focus() makes Chromium
  // scroll that overflow-hidden stage — visibly teleporting the dock.
  root.focus({ preventScroll: true })
  sections = Array.from(root.querySelectorAll<HTMLElement>('.pp-section'))
  sectionObserver = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          const i = sections.indexOf(entry.target as HTMLElement)
          if (i >= 0) activeIndex.value = i
        }
      }
    },
    { root, threshold: 0.55 },
  )
  sections.forEach((section) => sectionObserver?.observe(section))
  window.addEventListener('wheel', onWindowWheel, { passive: true })
})

onBeforeUnmount(() => {
  sectionObserver?.disconnect()
  sectionObserver = null
  window.removeEventListener('wheel', onWindowWheel)
})
</script>

<template>
  <section
    ref="rootRef"
    class="presentation"
    :aria-label="t('landing.product.label')"
    tabindex="-1"
    @keydown="onKeydown"
  >
    <!-- 1 · Hero — centered, the product name in full light. -->
    <section class="pp-section pp-section--hero">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
        <i class="sec-blob sec-blob--b"></i>
        <i class="sec-blob sec-blob--c"></i>
      </div>
      <div class="hero-inner">
        <div class="hero-cards" aria-hidden="true" data-reveal style="--reveal-delay: 260ms">
          <div class="ui-window hero-card hero-card--chat">
            <div class="ui-titlebar"><i class="ui-dot"></i><i class="ui-dot"></i><i class="ui-dot"></i></div>
            <i class="skel" style="width: 78%"></i>
            <i class="skel" style="width: 52%"></i>
            <i class="skel skel--accent" style="width: 64%"></i>
          </div>
          <div class="ui-window hero-card hero-card--graph">
            <i class="hero-node" style="left: 22%; top: 30%"></i>
            <i class="hero-node" style="left: 66%; top: 22%"></i>
            <i class="hero-node" style="left: 48%; top: 62%"></i>
            <i class="hero-edge" style="left: 26%; top: 32%; width: 42%; rotate: -8deg"></i>
            <i class="hero-edge" style="left: 50%; top: 34%; width: 26%; rotate: 58deg"></i>
          </div>
        </div>
        <h2 class="pp-title pp-title--hero" data-reveal>{{ t('app.name') }}</h2>
        <p class="pp-sub pp-sub--hero" data-reveal style="--reveal-delay: 140ms">
          {{ t('landing.product.hero.line') }}
        </p>
        <p class="pp-hint" data-reveal style="--reveal-delay: 320ms">
          <AppIcon name="chevron-down" size="sm" />
          {{ t('landing.product.hint') }}
        </p>
      </div>
    </section>

    <!-- 2 · AI Tutor — text left, conversation window right. -->
    <section class="pp-section pp-section--tutor">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
        <i class="sec-blob sec-blob--b"></i>
        <i class="sec-blob sec-blob--c"></i>
      </div>
      <div class="pp-grid">
        <div class="pp-copy" data-reveal>
          <span class="pp-badge" aria-hidden="true"><AppIcon name="graduation-cap" :size="22" /></span>
          <h2 class="pp-title">{{ t('landing.product.slides.tutor.title') }}</h2>
          <p class="pp-sub">{{ t('landing.product.slides.tutor.line') }}</p>
          <ul class="pp-points">
            <li v-for="p in points('tutor')" :key="p" class="pp-point">
              <span class="point-icon" aria-hidden="true"><AppIcon name="check" :size="13" /></span>
              {{ p }}
            </li>
          </ul>
        </div>
        <div class="pp-stage" aria-hidden="true" data-reveal style="--reveal-delay: 160ms">
          <div class="ui-window chat-window">
            <div class="ui-titlebar"><i class="ui-dot"></i><i class="ui-dot"></i><i class="ui-dot"></i></div>
            <div class="chat-body">
              <div class="chat-bubble chat-bubble--user">
                <i class="skel skel--onprimary" style="width: 72%"></i>
                <i class="skel skel--onprimary" style="width: 44%"></i>
              </div>
              <div class="chat-bubble chat-bubble--ai">
                <span class="chat-badge"><AppIcon name="graduation-cap" :size="14" /></span>
                <div class="chat-lines">
                  <i class="skel" style="width: 92%"></i>
                  <i class="skel" style="width: 78%"></i>
                  <i class="skel" style="width: 56%"></i>
                </div>
              </div>
            </div>
          </div>
          <div class="float-chip float-chip--a">
            <AppIcon name="sparkles" :size="14" />
            <i class="skel skel--chip" style="width: 62px"></i>
          </div>
          <div class="float-chip float-chip--b">
            <AppIcon name="message-square" :size="14" />
            <i class="skel skel--chip" style="width: 46px"></i>
          </div>
          <div class="ui-window float-panel timeline-panel">
            <div class="tl-row"><i class="tl-dot"></i><i class="skel" style="width: 68%"></i></div>
            <div class="tl-row"><i class="tl-dot"></i><i class="skel" style="width: 52%"></i></div>
            <div class="tl-row"><i class="tl-dot tl-dot--now"></i><i class="skel" style="width: 60%"></i></div>
          </div>
        </div>
      </div>
    </section>

    <!-- 3 · AI Notes — editor left, text right. -->
    <section class="pp-section pp-section--notes">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
        <i class="sec-blob sec-blob--b"></i>
        <i class="sec-blob sec-blob--c"></i>
      </div>
      <div class="pp-grid pp-grid--flip">
        <div class="pp-copy" data-reveal>
          <span class="pp-badge" aria-hidden="true"><AppIcon name="notebook-pen" :size="22" /></span>
          <h2 class="pp-title">{{ t('landing.product.slides.notes.title') }}</h2>
          <p class="pp-sub">{{ t('landing.product.slides.notes.line') }}</p>
          <ul class="pp-points">
            <li v-for="p in points('notes')" :key="p" class="pp-point">
              <span class="point-icon" aria-hidden="true"><AppIcon name="check" :size="13" /></span>
              {{ p }}
            </li>
          </ul>
        </div>
        <div class="pp-stage" aria-hidden="true" data-reveal style="--reveal-delay: 160ms">
          <div class="ui-window note-window">
            <div class="ui-titlebar"><i class="ui-dot"></i><i class="ui-dot"></i><i class="ui-dot"></i></div>
            <i class="skel skel--title" style="width: 56%"></i>
            <i class="skel" style="width: 92%"></i>
            <i class="skel" style="width: 84%"></i>
            <i class="note-image"></i>
            <i class="skel" style="width: 64%"></i>
          </div>
          <div class="float-panel code-panel">
            <div class="code-line">
              <i class="code-tok code-tok--violet" style="width: 34px"></i>
              <i class="code-tok" style="width: 58px"></i>
            </div>
            <div class="code-line">
              <i class="code-tok code-tok--rose" style="width: 46px"></i>
              <i class="code-tok" style="width: 30px"></i>
              <i class="code-tok code-tok--cyan" style="width: 40px"></i>
            </div>
            <div class="code-line">
              <i class="code-tok" style="width: 24px"></i>
              <i class="code-tok code-tok--amber" style="width: 52px"></i>
            </div>
          </div>
          <div class="float-chip float-chip--note">
            <AppIcon name="sparkles" :size="14" />
            <i class="skel skel--chip" style="width: 52px"></i>
          </div>
        </div>
      </div>
    </section>

    <!-- 4 · Flashcards — centered, a deck resting in depth. -->
    <section class="pp-section pp-section--flashcards">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
        <i class="sec-blob sec-blob--b"></i>
        <i class="sec-blob sec-blob--c"></i>
      </div>
      <div class="pp-center">
        <div class="pp-copy pp-copy--center" data-reveal>
          <span class="pp-badge" aria-hidden="true"><AppIcon name="layers" :size="22" /></span>
          <h2 class="pp-title">{{ t('landing.product.slides.flashcards.title') }}</h2>
          <p class="pp-sub">{{ t('landing.product.slides.flashcards.line') }}</p>
        </div>
        <div class="pp-stage stack-stage" aria-hidden="true" data-reveal style="--reveal-delay: 160ms">
          <div class="ui-window stack-card stack-card--far"></div>
          <div class="ui-window stack-card stack-card--mid"></div>
          <div class="ui-window stack-card stack-card--front">
            <AppIcon name="layers" :size="36" :stroke-width="1.25" />
            <i class="skel" style="width: 52%"></i>
          </div>
          <div class="stack-progress">
            <i class="progress-dot is-done"></i>
            <i class="progress-dot is-done"></i>
            <i class="progress-dot"></i>
          </div>
        </div>
      </div>
    </section>

    <!-- 5 · Knowledge Graph — text left, constellation right. -->
    <section class="pp-section pp-section--graph">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
        <i class="sec-blob sec-blob--b"></i>
        <i class="sec-blob sec-blob--c"></i>
      </div>
      <div class="pp-grid">
        <div class="pp-copy" data-reveal>
          <span class="pp-badge" aria-hidden="true"><AppIcon name="network" :size="22" /></span>
          <h2 class="pp-title">{{ t('landing.product.slides.graph.title') }}</h2>
          <p class="pp-sub">{{ t('landing.product.slides.graph.line') }}</p>
          <ul class="pp-points">
            <li v-for="p in points('graph')" :key="p" class="pp-point">
              <span class="point-icon" aria-hidden="true"><AppIcon name="check" :size="13" /></span>
              {{ p }}
            </li>
          </ul>
        </div>
        <div class="pp-stage" aria-hidden="true" data-reveal style="--reveal-delay: 160ms">
          <div class="graph-field">
            <i class="graph-link" style="width: 173px; transform: rotate(-157.6deg)"></i>
            <i class="graph-link" style="width: 169px; transform: rotate(-27.5deg)"></i>
            <i class="graph-link" style="width: 184px; transform: rotate(12.5deg)"></i>
            <i class="graph-link" style="width: 158px; transform: rotate(155.4deg)"></i>
            <i class="graph-link" style="width: 112px; transform: rotate(57.7deg)"></i>
            <i
              class="graph-link graph-link--outer"
              style="left: 80px; top: 64px; width: 106px; transform: rotate(-18.8deg)"
            ></i>
            <i
              class="graph-link graph-link--outer"
              style="left: 390px; top: 52px; width: 122px; transform: rotate(75.7deg)"
            ></i>
            <i
              class="graph-link graph-link--outer"
              style="left: 96px; top: 196px; width: 206px; transform: rotate(8.1deg)"
            ></i>
            <i class="graph-node" style="left: 80px; top: 64px; --d: 0s"></i>
            <i class="graph-node graph-node--sm" style="left: 390px; top: 52px; --d: -1.3s"></i>
            <i class="graph-node" style="left: 420px; top: 170px; --d: -2.1s"></i>
            <i class="graph-node graph-node--sm" style="left: 96px; top: 196px; --d: -3.4s"></i>
            <i class="graph-node" style="left: 300px; top: 225px; --d: -4.2s"></i>
            <i class="graph-node graph-node--sm" style="left: 180px; top: 30px; --d: -5s"></i>
            <span class="graph-core">
              <AppIcon name="network" :size="24" :stroke-width="1.4" />
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- 6 · AI Engine — diagonal: streaming panel low-left, copy high-right. -->
    <section class="pp-section pp-section--engine">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
        <i class="sec-blob sec-blob--b"></i>
        <i class="sec-blob sec-blob--c"></i>
      </div>
      <div class="pp-grid pp-grid--flip pp-grid--engine">
        <div class="pp-copy" data-reveal>
          <span class="pp-badge" aria-hidden="true"><AppIcon name="cpu" :size="22" /></span>
          <h2 class="pp-title">{{ t('landing.product.slides.engine.title') }}</h2>
          <p class="pp-sub">{{ t('landing.product.slides.engine.line') }}</p>
          <ul class="pp-points">
            <li v-for="p in points('engine')" :key="p" class="pp-point">
              <span class="point-icon" aria-hidden="true"><AppIcon name="check" :size="13" /></span>
              {{ p }}
            </li>
          </ul>
        </div>
        <div class="pp-stage engine-stage" aria-hidden="true" data-reveal style="--reveal-delay: 160ms">
          <i v-for="n in 12" :key="n" class="engine-particle" :style="{ '--i': n }"></i>
          <span class="engine-halo"></span>
          <span class="engine-core"><AppIcon name="cpu" :size="36" :stroke-width="1.1" /></span>
          <div class="ui-window stream-window">
            <div class="ui-titlebar"><i class="ui-dot"></i><i class="ui-dot"></i><i class="ui-dot"></i></div>
            <i class="skel" style="width: 88%"></i>
            <i class="skel" style="width: 74%"></i>
            <i class="skel" style="width: 80%"></i>
            <div class="stream-tail">
              <i class="skel" style="width: 38%"></i>
              <i class="stream-caret"></i>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 7 · Roadmap — centered timeline of milestones. -->
    <section class="pp-section pp-section--roadmap">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
        <i class="sec-blob sec-blob--b"></i>
        <i class="sec-blob sec-blob--c"></i>
      </div>
      <div class="pp-center">
        <div class="pp-copy pp-copy--center" data-reveal>
          <span class="pp-badge" aria-hidden="true"><AppIcon name="route" :size="22" /></span>
          <h2 class="pp-title">{{ t('landing.product.slides.roadmap.title') }}</h2>
          <p class="pp-sub">{{ t('landing.product.slides.roadmap.line') }}</p>
        </div>
        <div class="pp-stage roadmap-stage" aria-hidden="true" data-reveal style="--reveal-delay: 160ms">
          <i class="roadmap-track"></i>
          <div
            v-for="stop in roadmapStops"
            :key="stop.label"
            class="roadmap-stop"
            :class="{ 'is-soon': stop.soon }"
          >
            <i class="roadmap-dot"></i>
            <span class="roadmap-label">{{ stop.label }}</span>
            <span v-if="stop.soon" class="roadmap-soon">{{
              t('landing.product.slides.roadmap.soon')
            }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 8 · Coming Soon — minimal ending, large type over one soft bloom. -->
    <section class="pp-section pp-section--coming">
      <div class="sec-atmo" aria-hidden="true">
        <i class="sec-blob sec-blob--a"></i>
      </div>
      <div class="pp-center">
        <span class="coming-orb" aria-hidden="true"><AppIcon name="sparkles" :size="26" /></span>
        <h2 class="pp-title pp-title--coming" data-reveal>
          {{ t('landing.product.slides.coming.title') }}
        </h2>
        <p class="pp-sub" data-reveal style="--reveal-delay: 140ms">
          {{ t('landing.product.slides.coming.line') }}
        </p>
      </div>
    </section>

    <nav class="pp-nav" :aria-label="t('landing.product.nav')">
      <button
        v-for="(item, i) in navItems"
        :key="SECTIONS[i]"
        type="button"
        class="pp-nav-dot"
        :class="{ active: i === activeIndex }"
        :aria-label="item"
        :aria-current="i === activeIndex || undefined"
        @click="goTo(i)"
      ></button>
    </nav>
  </section>
</template>

<style scoped>
/*
 * The bright room. Fixed full-screen layer under the dock (z 40 vs 50),
 * but unlike the keynote it is a real scroll container: native wheel,
 * touch and keyboard scrolling with gentle snap. The dark stage never
 * shows through — the first paint is already warm white.
 */
.presentation {
  position: fixed;
  inset: 0;
  z-index: 40;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scroll-snap-type: y proximity;
  background: #fbfaff;
  outline: none;

  /* Dark ink on light air — the inverse of the stage's dusk system. */
  --pp-ink: #211c44;
  --pp-ink-2: rgba(33, 28, 68, 0.64);
  --pp-ink-3: rgba(33, 28, 68, 0.42);
  --pp-line: rgba(33, 28, 68, 0.08);
}

@media (prefers-reduced-motion: no-preference) {
  .presentation {
    scroll-behavior: smooth;
  }
}

/* ------------------------------------------------------------------ */
/* Sections — one atmosphere each, never dark, never empty             */
/* ------------------------------------------------------------------ */

.pp-section {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 96px clamp(24px, 7vw, 96px) 150px;
  overflow: hidden;
  scroll-snap-align: start;
  background: var(--sec-base);
}

.sec-atmo {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

/* Large blurred color fields drifting on the Phase 9 underlight paths —
   the same gesture as the dark atmospheres, re-lit in pastel. */
.sec-blob {
  position: absolute;
  width: 70vmax;
  aspect-ratio: 1;
  border-radius: 50%;
}

.sec-blob--a {
  top: -30%;
  left: -20%;
  background: radial-gradient(circle, var(--blob-a), transparent 64%);
  animation: app-underlight-a 46s var(--ease-in-out) infinite alternate;
}

.sec-blob--b {
  top: -16%;
  right: -24%;
  background: radial-gradient(circle, var(--blob-b), transparent 64%);
  animation: app-underlight-b 58s var(--ease-in-out) infinite alternate;
}

.sec-blob--c {
  bottom: -32%;
  left: 12%;
  background: radial-gradient(circle, var(--blob-c), transparent 64%);
  animation: app-underlight-c 40s var(--ease-in-out) infinite alternate;
}

/* The eight atmospheres — every section owns its own light. */
.pp-section--hero {
  --sec-base: linear-gradient(180deg, #fdfcff, #f4f0ff);
  --blob-a: rgba(150, 115, 255, 0.18);
  --blob-b: rgba(255, 130, 195, 0.14);
  --blob-c: rgba(95, 155, 255, 0.13);
  --sec-accent: #6d4aff;
}

.pp-section--tutor {
  --sec-base: linear-gradient(180deg, #f4f8ff, #e9f2ff);
  --blob-a: rgba(90, 150, 255, 0.2);
  --blob-b: rgba(80, 210, 255, 0.14);
  --blob-c: rgba(140, 120, 255, 0.1);
  --sec-accent: #2563eb;
  --sec-accent-2: #4f8cff;
}

.pp-section--notes {
  --sec-base: linear-gradient(180deg, #fffaf2, #fdf1e0);
  --blob-a: rgba(255, 180, 90, 0.2);
  --blob-b: rgba(255, 130, 110, 0.13);
  --blob-c: rgba(255, 220, 150, 0.16);
  --sec-accent: #d97706;
  --sec-accent-2: #f59e0b;
}

.pp-section--flashcards {
  --sec-base: linear-gradient(180deg, #fbf6ff, #f2eaff);
  --blob-a: rgba(200, 120, 255, 0.16);
  --blob-b: rgba(255, 120, 190, 0.13);
  --blob-c: rgba(150, 130, 255, 0.13);
  --sec-accent: #9333ea;
  --sec-accent-2: #c04ae2;
}

.pp-section--graph {
  --sec-base: linear-gradient(180deg, #f2fbf8, #e6f6f0);
  --blob-a: rgba(45, 212, 175, 0.18);
  --blob-b: rgba(70, 200, 255, 0.12);
  --blob-c: rgba(130, 255, 205, 0.14);
  --sec-accent: #0d9488;
  --sec-accent-2: #14b8a6;
}

.pp-section--engine {
  --sec-base: linear-gradient(180deg, #f6f5ff, #ecebff);
  --blob-a: rgba(120, 95, 255, 0.18);
  --blob-b: rgba(70, 170, 255, 0.13);
  --blob-c: rgba(200, 120, 255, 0.12);
  --sec-accent: #6d28d9;
  --sec-accent-2: #7c5cff;
}

.pp-section--roadmap {
  --sec-base: linear-gradient(180deg, #fffdf6, #fbf4e4);
  --blob-a: rgba(255, 195, 110, 0.2);
  --blob-b: rgba(255, 150, 105, 0.12);
  --blob-c: rgba(255, 235, 180, 0.2);
  --sec-accent: #d97706;
}

.pp-section--coming {
  --sec-base: linear-gradient(180deg, #f9f8ff, #efecfc);
  --blob-a: rgba(150, 120, 255, 0.2);
  --sec-accent: #6d4aff;
}

/* ------------------------------------------------------------------ */
/* Type — premium scale, dark ink, generous air                        */
/* ------------------------------------------------------------------ */

.pp-title {
  margin: 0;
  font-family: var(--font-headline-family);
  font-size: clamp(2.3rem, 4.2vw, 3.8rem);
  font-weight: 680;
  line-height: 1.06;
  letter-spacing: var(--font-headline-tracking);
  color: var(--pp-ink);
}

/* The hero headline — the product name poured in saturated light. */
.pp-title--hero {
  font-size: clamp(3rem, 7.5vw, 6.2rem);
  background: linear-gradient(96deg, #5b3df0 4%, #b833b8 40%, #e8467c 66%, #2563eb 96%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
}

.pp-title--coming {
  font-size: clamp(2.7rem, 6.4vw, 5.2rem);
}

.pp-sub {
  margin: var(--space-4) 0 0;
  max-width: 42ch;
  font-size: clamp(1.05rem, 1.6vw, 1.3rem);
  line-height: 1.6;
  color: var(--pp-ink-2);
}

.pp-sub--hero {
  margin-inline: auto;
  font-size: clamp(1.15rem, 2vw, 1.5rem);
}

.pp-hint {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-8) 0 0;
  font-size: var(--text-xs);
  color: var(--pp-ink-3);
}

.pp-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  margin-bottom: var(--space-5);
  border-radius: 15px;
  background: linear-gradient(135deg, var(--sec-accent), var(--sec-accent-2, var(--sec-accent)));
  color: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 30px -12px color-mix(in srgb, var(--sec-accent) 55%, transparent);
}

.pp-points {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin: var(--space-6) 0 0;
  padding: 0;
  list-style: none;
}

.pp-point {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: 1rem;
  color: var(--pp-ink-2);
}

.point-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--sec-accent) 14%, transparent);
  color: var(--sec-accent);
}

/* ------------------------------------------------------------------ */
/* Layout — alternating compositions                                   */
/* ------------------------------------------------------------------ */

.pp-grid {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 6fr);
  align-items: center;
  gap: clamp(40px, 6vw, 96px);
  width: min(1160px, 100%);
  margin-inline: auto;
}

.pp-grid--flip .pp-copy {
  order: 2;
}

.pp-grid--flip .pp-stage {
  order: 1;
}

/* Engine's diagonal: the visual sits low, the copy rides high. */
.pp-grid--engine .pp-stage {
  translate: 0 34px;
}

.pp-grid--engine .pp-copy {
  translate: 0 -26px;
}

.pp-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(880px, 100%);
  margin-inline: auto;
  text-align: center;
}

.pp-copy--center {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-inner {
  position: relative;
  width: min(980px, 100%);
  text-align: center;
}

.pp-stage {
  position: relative;
  width: 100%;
  min-height: 320px;
}

/* ------------------------------------------------------------------ */
/* Shared UI-mockup vocabulary — white windows, soft depth             */
/* ------------------------------------------------------------------ */

.ui-window {
  position: absolute;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: var(--space-4);
  border: 1px solid var(--pp-line);
  border-radius: 20px;
  background: #ffffff;
  box-shadow:
    0 32px 70px -28px rgba(33, 28, 68, 0.28),
    0 4px 14px rgba(33, 28, 68, 0.05);
}

.ui-titlebar {
  display: flex;
  gap: 6px;
}

.ui-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(33, 28, 68, 0.14);
}

.skel {
  display: block;
  height: 8px;
  border-radius: 4px;
  background: rgba(33, 28, 68, 0.12);
}

.skel--title {
  height: 12px;
  border-radius: 6px;
  background: rgba(33, 28, 68, 0.22);
}

.skel--accent {
  background: color-mix(in srgb, var(--sec-accent) 35%, transparent);
}

.skel--onprimary {
  background: rgba(255, 255, 255, 0.55);
}

.skel--chip {
  height: 6px;
}

/* Floating accessories share the window skin at chip scale. */
.float-chip {
  position: absolute;
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 10px 14px;
  border: 1px solid var(--pp-line);
  border-radius: var(--radius-full);
  background: #ffffff;
  box-shadow: 0 18px 40px -18px rgba(33, 28, 68, 0.3);
  color: var(--sec-accent);
}

.float-panel {
  position: absolute;
}

/* ------------------------------------------------------------------ */
/* Hero composition — two soft app previews behind the headline        */
/* ------------------------------------------------------------------ */

.hero-cards {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-card {
  width: 190px;
  opacity: 0.85;
}

.hero-card--chat {
  left: 0;
  top: -34px;
  rotate: -6deg;
  animation: app-float 11s var(--ease-in-out) infinite alternate;
}

.hero-card--graph {
  right: 0;
  bottom: -50px;
  height: 120px;
  rotate: 4deg;
  animation: app-float 13s var(--ease-in-out) -5s infinite alternate;
}

.hero-node {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--sec-accent);
  opacity: 0.75;
}

.hero-edge {
  position: absolute;
  height: 1.5px;
  background: color-mix(in srgb, var(--sec-accent) 30%, transparent);
  transform-origin: 0 50%;
}

/* ------------------------------------------------------------------ */
/* Tutor — a real conversation window with floating suggestions        */
/* ------------------------------------------------------------------ */

.chat-window {
  left: 6%;
  top: 0;
  width: min(420px, 88%);
  animation: app-float 12s var(--ease-in-out) infinite alternate;
}

.chat-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-bubble {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 16px;
}

.chat-bubble--user {
  flex-direction: column;
  align-self: flex-end;
  width: 58%;
  background: linear-gradient(135deg, var(--sec-accent), var(--sec-accent-2));
  box-shadow: 0 12px 26px -12px color-mix(in srgb, var(--sec-accent) 60%, transparent);
}

.chat-bubble--ai {
  align-items: flex-start;
  width: 86%;
  background: rgba(33, 28, 68, 0.045);
}

.chat-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--sec-accent), var(--sec-accent-2));
  color: rgba(255, 255, 255, 0.95);
}

.chat-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  padding-top: 4px;
}

.float-chip--a {
  right: 2%;
  top: 8%;
  rotate: 2deg;
  animation: app-float 10s var(--ease-in-out) -3s infinite alternate;
}

.float-chip--b {
  right: 10%;
  top: 32%;
  rotate: -2deg;
  animation: app-float 13s var(--ease-in-out) -7s infinite alternate;
}

.timeline-panel {
  left: 0;
  bottom: -6%;
  width: 200px;
  gap: 10px;
  rotate: -2deg;
  animation: app-float 14s var(--ease-in-out) -9s infinite alternate;
}

.tl-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tl-row .skel {
  flex: 1;
  max-width: 68%;
}

.tl-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--sec-accent) 45%, transparent);
}

.tl-dot--now {
  background: var(--sec-accent);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--sec-accent) 18%, transparent);
}

/* ------------------------------------------------------------------ */
/* Notes — an editor window, a code panel, an AI chip                  */
/* ------------------------------------------------------------------ */

.note-window {
  left: 4%;
  top: 0;
  width: min(400px, 86%);
  animation: app-float 12s var(--ease-in-out) infinite alternate;
}

.note-image {
  height: 74px;
  border-radius: 12px;
  background: linear-gradient(
    120deg,
    color-mix(in srgb, var(--sec-accent) 30%, #fff),
    color-mix(in srgb, #e8467c 22%, #fff)
  );
}

/* One small dark pane is allowed in the bright room: code reads as code. */
.code-panel {
  right: 0;
  top: 16%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 210px;
  padding: var(--space-4);
  border-radius: 16px;
  background: #241f3f;
  box-shadow: 0 28px 60px -24px rgba(33, 28, 68, 0.5);
  rotate: 3deg;
  animation: app-float 13s var(--ease-in-out) -4s infinite alternate;
}

.code-line {
  display: flex;
  gap: 8px;
}

.code-tok {
  height: 7px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.28);
}

.code-tok--violet {
  background: rgba(170, 140, 255, 0.85);
}

.code-tok--rose {
  background: rgba(255, 130, 175, 0.8);
}

.code-tok--cyan {
  background: rgba(110, 220, 255, 0.8);
}

.code-tok--amber {
  background: rgba(255, 200, 110, 0.85);
}

.float-chip--note {
  right: 14%;
  bottom: 2%;
  rotate: -3deg;
  animation: app-float 11s var(--ease-in-out) -6s infinite alternate;
}

/* ------------------------------------------------------------------ */
/* Flashcards — the deck in depth, now resting in daylight             */
/* ------------------------------------------------------------------ */

.stack-stage {
  min-height: 280px;
  margin-top: var(--space-8);
  max-width: 560px;
}

/* Centred by geometry, never by transform — the front card's float
   animation owns `transform` outright (the Phase 11 clobbering rule). */
.stack-card {
  left: calc(50% - 131px);
  top: calc(50% - 90px);
  width: 262px;
  height: 156px;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  border-radius: 22px;
  color: var(--pp-ink);
}

.stack-card--far {
  translate: 0 -44px;
  scale: 0.85;
  opacity: 0.45;
}

.stack-card--mid {
  translate: 0 -22px;
  scale: 0.93;
  opacity: 0.7;
}

.stack-card--front {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--sec-accent) 14%, #fff),
    color-mix(in srgb, var(--sec-accent-2) 22%, #fff)
  );
  color: var(--sec-accent);
  animation: app-float 10s var(--ease-in-out) infinite alternate;
}

.stack-card--front .skel {
  width: 52%;
  background: color-mix(in srgb, var(--sec-accent) 30%, transparent);
}

.stack-progress {
  position: absolute;
  left: calc(50% - 24px);
  bottom: 0;
  display: flex;
  gap: 8px;
}

.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(33, 28, 68, 0.15);
}

.progress-dot.is-done {
  background: var(--sec-accent);
}

/* ------------------------------------------------------------------ */
/* Knowledge Graph — the constellation, re-lit for daylight            */
/* ------------------------------------------------------------------ */

.graph-field {
  position: relative;
  width: 480px;
  max-width: 100%;
  height: 260px;
  margin-inline: auto;
}

.graph-link {
  position: absolute;
  left: 240px;
  top: 130px;
  height: 1.5px;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--sec-accent) 45%, transparent),
    color-mix(in srgb, var(--sec-accent) 8%, transparent)
  );
  transform-origin: 0 50%;
}

.graph-link--outer {
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--sec-accent-2) 30%, transparent),
    color-mix(in srgb, var(--sec-accent-2) 6%, transparent)
  );
}

.graph-node {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--sec-accent-2);
  box-shadow:
    0 0 0 5px color-mix(in srgb, var(--sec-accent-2) 16%, transparent),
    0 8px 20px -6px color-mix(in srgb, var(--sec-accent) 55%, transparent);
  transform: translate(-50%, -50%);
  animation: pp-node-pulse 5s var(--ease-in-out) var(--d, 0s) infinite alternate;
}

.graph-node--sm {
  width: 9px;
  height: 9px;
}

.graph-core {
  position: absolute;
  left: 240px;
  top: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--sec-accent) 30%, transparent);
  background: #ffffff;
  box-shadow: 0 22px 50px -18px color-mix(in srgb, var(--sec-accent) 60%, transparent);
  transform: translate(-50%, -50%);
  color: var(--sec-accent);
}

@keyframes pp-node-pulse {
  from {
    opacity: 0.6;
    transform: translate(-50%, -50%) scale(0.85);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.12);
  }
}

/* ------------------------------------------------------------------ */
/* AI Engine — a streaming response beside a breathing core            */
/* ------------------------------------------------------------------ */

.engine-stage {
  min-height: 360px;
}

.stream-window {
  right: 4%;
  bottom: 6%;
  width: min(360px, 80%);
  rotate: -2deg;
  animation: app-float 12s var(--ease-in-out) infinite alternate;
}

.stream-tail {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stream-caret {
  width: 8px;
  height: 14px;
  border-radius: 2px;
  background: var(--sec-accent);
  animation: pp-caret 1.1s steps(2, start) infinite;
}

@keyframes pp-caret {
  to {
    opacity: 0;
  }
}

.engine-core {
  position: absolute;
  left: 10%;
  top: 8%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border-radius: 28px;
  border: 1px solid color-mix(in srgb, var(--sec-accent) 25%, transparent);
  background: #ffffff;
  box-shadow: 0 26px 60px -22px color-mix(in srgb, var(--sec-accent) 65%, transparent);
  color: var(--sec-accent);
}

.engine-halo {
  position: absolute;
  left: calc(10% + 48px - 105px);
  top: calc(8% + 48px - 105px);
  width: 210px;
  height: 210px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    color-mix(in srgb, var(--sec-accent) 18%, transparent),
    transparent 70%
  );
  animation: pp-halo 6s var(--ease-in-out) infinite alternate;
}

@keyframes pp-halo {
  from {
    opacity: 0.55;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1.15);
  }
}

.engine-particle {
  position: absolute;
  left: calc(6% + var(--i) * 7.4%);
  bottom: 4%;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--sec-accent) 55%, transparent);
  opacity: 0;
  animation: pp-rise calc(5.5s + var(--i) * 0.35s) linear calc(var(--i) * -1.35s) infinite;
}

.engine-particle:nth-child(even) {
  width: 3px;
  height: 3px;
  background: color-mix(in srgb, var(--sec-accent-2) 55%, transparent);
}

@keyframes pp-rise {
  0% {
    transform: translateY(0);
    opacity: 0;
  }
  12% {
    opacity: 0.7;
  }
  80% {
    opacity: 0.2;
  }
  100% {
    transform: translateY(-240px);
    opacity: 0;
  }
}

/* ------------------------------------------------------------------ */
/* Roadmap — milestones on one warm line                               */
/* ------------------------------------------------------------------ */

.roadmap-stage {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: min(720px, 92vw);
  min-height: 0;
  padding-top: 64px;
  margin-top: var(--space-8);
  /* The line and its stops breathe as ONE object — dots never leave the track. */
  animation: app-float 14s var(--ease-in-out) infinite alternate;
}

.roadmap-track {
  position: absolute;
  left: 2%;
  right: 2%;
  top: 71px;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(
    90deg,
    var(--sec-accent),
    color-mix(in srgb, var(--sec-accent) 55%, transparent) 62%,
    color-mix(in srgb, var(--sec-accent) 12%, transparent)
  );
}

.roadmap-stop {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.roadmap-dot {
  width: 17px;
  height: 17px;
  border-radius: 50%;
  border: 3px solid #fffdf6;
  background: var(--sec-accent);
  box-shadow: 0 6px 16px -4px color-mix(in srgb, var(--sec-accent) 60%, transparent);
}

.roadmap-stop.is-soon .roadmap-dot {
  background: #fffdf6;
  border: 2.5px solid var(--sec-accent);
  /* In-flow pulse — pp-node-pulse carries a centring translate and would
     lift this dot off the track (the Phase 11 lesson). */
  animation: pp-soon-pulse 3.5s var(--ease-in-out) infinite alternate;
}

@keyframes pp-soon-pulse {
  from {
    opacity: 0.6;
    transform: scale(0.88);
  }
  to {
    opacity: 1;
    transform: scale(1.1);
  }
}

.roadmap-label {
  max-width: 9em;
  font-size: var(--text-sm);
  font-weight: 550;
  color: var(--pp-ink-2);
}

.roadmap-soon {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--sec-accent) 14%, transparent);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--sec-accent);
}

/* ------------------------------------------------------------------ */
/* Coming Soon — the quiet ending                                      */
/* ------------------------------------------------------------------ */

.coming-orb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  margin-bottom: var(--space-6);
  border-radius: 50%;
  background: radial-gradient(
    circle at 32% 28%,
    #ffffff,
    color-mix(in srgb, var(--sec-accent) 16%, #fff)
  );
  box-shadow:
    0 30px 70px -24px color-mix(in srgb, var(--sec-accent) 55%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  color: var(--sec-accent);
  animation: app-float 12s var(--ease-in-out) infinite alternate;
}

/* ------------------------------------------------------------------ */
/* Section nav — quiet dots at the right edge                          */
/* ------------------------------------------------------------------ */

.pp-nav {
  position: fixed;
  right: clamp(14px, 2.5vw, 36px);
  top: 50%;
  translate: 0 -50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.pp-nav-dot {
  width: 8px;
  height: 8px;
  padding: 0;
  border: none;
  border-radius: var(--radius-full);
  background: rgba(33, 28, 68, 0.2);
  cursor: pointer;
  transition: background-color 500ms var(--ease-out);
}

.pp-nav-dot.active {
  background: rgba(33, 28, 68, 0.85);
}

.pp-nav-dot:focus-visible {
  outline: var(--border-width-md) solid var(--color-focus-ring);
  outline-offset: 2px;
}

/* ------------------------------------------------------------------ */
/* Responsive — the compositions stack, the air stays                  */
/* ------------------------------------------------------------------ */

@media (max-width: 980px) {
  .pp-grid {
    grid-template-columns: 1fr;
    gap: var(--space-8);
    justify-items: center;
    text-align: center;
  }

  /* Stacked flow always reads copy first, visual second. */
  .pp-grid--flip .pp-copy {
    order: 1;
  }

  .pp-grid--flip .pp-stage {
    order: 2;
  }

  .pp-grid--engine .pp-copy,
  .pp-grid--engine .pp-stage {
    translate: 0 0;
  }

  .pp-copy {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .pp-stage {
    max-width: 520px;
  }
}

@media (max-width: 640px) {
  .pp-section {
    padding: 80px var(--space-5) 132px;
  }

  .hero-cards {
    display: none;
  }

  .float-chip--a,
  .float-chip--b,
  .float-chip--note,
  .timeline-panel {
    display: none;
  }

  .code-panel {
    right: 2%;
    top: auto;
    bottom: 0;
    width: 170px;
  }

  .graph-field {
    transform: scale(0.72);
    margin-block: -36px;
  }

  .engine-stage {
    min-height: 300px;
  }

  .engine-core {
    left: 4%;
    top: 2%;
  }

  .roadmap-label {
    font-size: var(--text-xs);
  }

  .pp-nav {
    right: 8px;
  }
}
</style>
