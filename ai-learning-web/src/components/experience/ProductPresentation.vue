<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppIcon from '../AppIcon.vue'
import type { IconName } from '../icons/registry'

/**
 * Product presentation — an Apple-keynote product gallery (Phase 11).
 *
 * A full-viewport gallery layer that appears BEHIND the persistent glass
 * dock: one slide per viewport, one wheel gesture per slide, generous
 * whitespace, almost no text. The paging engine is Phase 10's verbatim —
 * wheel delta accumulation with an idle reset, a heavy transition lock so
 * rapid input moves exactly one slide, keyboard paging, vertical touch
 * swipe, a finite deck with no autoplay.
 *
 * What changed in Phase 11: the deck is no longer black. This page is the
 * one colorful space in the project. Each slide owns a full-bleed color
 * atmosphere (mesh-gradient fields on slow transform drifts) and a CSS-only
 * visual composition — floating cards, a glowing constellation, rising
 * particles, a timeline. The atmospheres run edge to edge underneath the
 * dock, so the dock's displacement refraction finally has moving colored
 * light to bend: the world scrolls, the glass never moves.
 *
 * Motion is layered by speed — content refocuses in ~1s, the atmosphere
 * cross-fades over ~1.6s ("light moves slower than glass"). Still no
 * GlassSurface here: the page keeps exactly two displacement filters
 * (sign-in slab + dock). Everything below is gradients and transforms.
 *
 * Escape (or the dock's Login facet) returns the camera to the sign-in
 * gallery via the `close` event.
 */

const { t } = useI18n()

const emit = defineEmits<{ close: [] }>()

const SLIDES: ReadonlyArray<{ key: string; icon: IconName | null }> = [
  { key: 'hero', icon: null },
  { key: 'tutor', icon: 'graduation-cap' },
  { key: 'notes', icon: 'notebook-pen' },
  { key: 'flashcards', icon: 'layers' },
  { key: 'graph', icon: 'network' },
  { key: 'engine', icon: 'cpu' },
  { key: 'roadmap', icon: 'route' },
  { key: 'coming', icon: 'sparkles' },
]

const slides = computed(() =>
  SLIDES.map(({ key, icon }) => ({
    key,
    icon,
    title: key === 'hero' ? t('app.name') : t(`landing.product.slides.${key}.title`),
    line: key === 'hero' ? t('landing.product.hero.line') : t(`landing.product.slides.${key}.line`),
  })),
)

/* The roadmap timeline reuses the deck's own feature titles — the story the
   audience just watched, laid out in order. No extra strings. */
const roadmapStops = computed(() => [
  { label: t('landing.product.slides.tutor.title'), soon: false },
  { label: t('landing.product.slides.notes.title'), soon: false },
  { label: t('landing.product.slides.graph.title'), soon: false },
  { label: t('landing.product.slides.coming.title'), soon: true },
])

const rootRef = ref<HTMLElement | null>(null)
const index = ref(0)
const direction = ref<1 | -1>(1)
const locked = ref(false)
const announcement = ref('')

const current = computed(() => slides.value[index.value]!)
const transitionName = computed(() => (direction.value > 0 ? 'refocus-next' : 'refocus-prev'))

const LOCK_MS = 1050
const WHEEL_THRESHOLD = 42
const SWIPE_THRESHOLD = 40

let unlockTimer = 0
let wheelIdleTimer = 0
let wheelAcc = 0
let touchStartY: number | null = null

function go(delta: number) {
  if (locked.value) return
  const target = index.value + (delta > 0 ? 1 : -1)
  if (target < 0 || target >= SLIDES.length) return
  direction.value = delta > 0 ? 1 : -1
  index.value = target
  afterMove()
}

function goTo(target: number) {
  if (locked.value || target === index.value) return
  direction.value = target > index.value ? 1 : -1
  index.value = target
  afterMove()
}

function afterMove() {
  locked.value = true
  window.clearTimeout(unlockTimer)
  unlockTimer = window.setTimeout(() => {
    locked.value = false
  }, LOCK_MS)
  const slide = slides.value[index.value]
  announcement.value = slide ? `${slide.title} — ${slide.line}` : ''
}

/* Wheel owns the whole gallery layer: the page underneath must never
   scroll while the keynote is on stage. */
function onWheel(event: WheelEvent) {
  event.preventDefault()
  wheelAcc += event.deltaY
  window.clearTimeout(wheelIdleTimer)
  wheelIdleTimer = window.setTimeout(() => {
    wheelAcc = 0
  }, 240)
  if (locked.value) return
  if (Math.abs(wheelAcc) >= WHEEL_THRESHOLD) {
    const delta = Math.sign(wheelAcc)
    wheelAcc = 0
    go(delta)
  }
}

function onKeydown(event: KeyboardEvent) {
  switch (event.key) {
    case 'ArrowDown':
    case 'PageDown':
      event.preventDefault()
      go(1)
      break
    case 'ArrowUp':
    case 'PageUp':
      event.preventDefault()
      go(-1)
      break
    case 'Home':
      event.preventDefault()
      goTo(0)
      break
    case 'End':
      event.preventDefault()
      goTo(SLIDES.length - 1)
      break
    case 'Escape':
      event.preventDefault()
      emit('close')
      break
  }
}

/* Vertical swipe pages the deck; mouse drags are ignored (mouse users have
   the wheel and the beads). */
function onPointerDown(event: PointerEvent) {
  if (event.pointerType === 'mouse') return
  touchStartY = event.clientY
}

function onPointerUp(event: PointerEvent) {
  if (touchStartY != null) {
    const travel = touchStartY - event.clientY
    if (Math.abs(travel) >= SWIPE_THRESHOLD) go(travel > 0 ? 1 : -1)
  }
  touchStartY = null
}

function onPointerCancel() {
  touchStartY = null
}

onMounted(() => {
  // The keynote takes the keyboard as soon as the camera arrives.
  rootRef.value?.focus()
})

onBeforeUnmount(() => {
  window.clearTimeout(unlockTimer)
  window.clearTimeout(wheelIdleTimer)
})
</script>

<template>
  <section
    ref="rootRef"
    class="presentation"
    role="group"
    aria-roledescription="carousel"
    :aria-label="t('landing.product.label')"
    tabindex="-1"
    @wheel="onWheel"
    @keydown="onKeydown"
    @pointerdown="onPointerDown"
    @pointerup="onPointerUp"
    @pointercancel="onPointerCancel"
  >
    <!-- Atmosphere — the slide's full-bleed color field. It runs under the
         dock and cross-fades slower than the content above it. -->
    <div class="atmosphere" aria-hidden="true">
      <Transition name="atmo">
        <div :key="current.key" class="atmo" :class="`atmo--${current.key}`">
          <i class="atmo-blob atmo-blob--a"></i>
          <i class="atmo-blob atmo-blob--b"></i>
          <i class="atmo-blob atmo-blob--c"></i>
          <i v-if="current.key === 'hero'" class="atmo-sheen"></i>
        </div>
      </Transition>
    </div>

    <div class="presentation-viewport">
      <Transition :name="transitionName">
        <article :key="index" class="pp-slide" :class="`pp-slide--${current.key}`">
          <!-- AI Tutor — a conversation floating in space. -->
          <div v-if="current.key === 'tutor'" class="pp-visual visual-tutor" aria-hidden="true">
            <div class="pp-card tutor-card tutor-card--question">
              <i class="skel" style="width: 64%"></i>
              <i class="skel" style="width: 38%"></i>
            </div>
            <div class="pp-card tutor-card tutor-card--answer">
              <span class="tutor-badge"><AppIcon name="graduation-cap" size="sm" /></span>
              <i class="skel" style="width: 82%"></i>
              <i class="skel" style="width: 66%"></i>
              <i class="skel" style="width: 44%"></i>
            </div>
            <div class="pp-card tutor-card tutor-card--echo">
              <i class="skel" style="width: 56%"></i>
            </div>
          </div>

          <!-- AI Notes — colorful snippets, softly connected. -->
          <div
            v-else-if="current.key === 'notes'"
            class="pp-visual visual-notes"
            aria-hidden="true"
          >
            <i class="notes-thread notes-thread--a"></i>
            <i class="notes-thread notes-thread--b"></i>
            <div class="pp-card note-card note-card--amber">
              <i class="note-dot"></i>
              <i class="skel" style="width: 74%"></i>
              <i class="skel" style="width: 48%"></i>
            </div>
            <div class="pp-card note-card note-card--rose">
              <i class="note-dot"></i>
              <i class="skel" style="width: 66%"></i>
              <i class="skel" style="width: 52%"></i>
            </div>
            <div class="pp-card note-card note-card--violet">
              <i class="note-dot"></i>
              <i class="skel" style="width: 58%"></i>
            </div>
          </div>

          <!-- Flashcards — a deck resting in depth. -->
          <div
            v-else-if="current.key === 'flashcards'"
            class="pp-visual visual-stack"
            aria-hidden="true"
          >
            <div class="pp-card stack-card stack-card--far"></div>
            <div class="pp-card stack-card stack-card--mid"></div>
            <div class="pp-card stack-card stack-card--front">
              <AppIcon name="layers" :size="40" :stroke-width="1.25" />
              <i class="skel" style="width: 54%"></i>
            </div>
          </div>

          <!-- Knowledge Graph — a glowing constellation. -->
          <div
            v-else-if="current.key === 'graph'"
            class="pp-visual visual-graph"
            aria-hidden="true"
          >
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
              <AppIcon name="network" :size="26" :stroke-width="1.4" />
            </span>
          </div>

          <!-- AI Engine — a core breathing light, tokens rising. -->
          <div
            v-else-if="current.key === 'engine'"
            class="pp-visual visual-engine"
            aria-hidden="true"
          >
            <i v-for="n in 14" :key="n" class="engine-particle" :style="{ '--i': n }"></i>
            <span class="engine-halo"></span>
            <span class="engine-core">
              <AppIcon name="cpu" :size="44" :stroke-width="1.1" />
            </span>
          </div>

          <!-- Roadmap — the story so far, laid on one line of light. -->
          <div
            v-else-if="current.key === 'roadmap'"
            class="pp-visual visual-roadmap"
            aria-hidden="true"
          >
            <i class="roadmap-track"></i>
            <div
              v-for="stop in roadmapStops"
              :key="stop.label"
              class="roadmap-stop"
              :class="{ 'is-soon': stop.soon }"
            >
              <i class="roadmap-dot"></i>
              <span class="roadmap-label">{{ stop.label }}</span>
            </div>
          </div>

          <h2 class="pp-title">{{ current.title }}</h2>
          <p class="pp-line">{{ current.line }}</p>
          <p v-if="index === 0" class="pp-hint">
            <AppIcon name="chevron-down" size="sm" />
            {{ t('landing.product.hint') }}
          </p>
        </article>
      </Transition>
    </div>

    <div class="presentation-beads">
      <button
        v-for="(slide, i) in slides"
        :key="slide.key"
        type="button"
        class="bead"
        :class="{ active: i === index }"
        :aria-label="slide.title"
        :aria-current="i === index || undefined"
        @click="goTo(i)"
      ></button>
    </div>

    <span class="visually-hidden" aria-live="polite">{{ announcement }}</span>
  </section>
</template>

<style scoped>
/*
 * The gallery layer. Phase 11 makes this the one colorful room in the
 * project, so the base is opaque — each slide's atmosphere paints the whole
 * frame. Sits below the dock (which keeps floating above, refracting the
 * atmospheres) and above everything else on the stage.
 */
.presentation {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  background: #030208;
  outline: none;
  /* All vertical gestures belong to the deck. */
  touch-action: none;
}

/* ------------------------------------------------------------------ */
/* Atmospheres — full-bleed mesh-gradient color fields, one per slide  */
/* ------------------------------------------------------------------ */

.atmosphere {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.atmo {
  position: absolute;
  inset: 0;
  background: var(--atmo-base);
}

/* Three soft light masses per field, drifting on the Phase 9 underlight
   paths (30–60s, transform only) so no two slides ever feel synchronized. */
.atmo-blob {
  position: absolute;
  width: 78vmax;
  aspect-ratio: 1;
  border-radius: 50%;
}

.atmo-blob--a {
  top: -32%;
  left: -22%;
  background: radial-gradient(circle, var(--atmo-a), transparent 66%);
  animation: app-underlight-a 44s var(--ease-in-out) infinite alternate;
}

.atmo-blob--b {
  top: -18%;
  right: -26%;
  background: radial-gradient(circle, var(--atmo-b), transparent 66%);
  animation: app-underlight-b 56s var(--ease-in-out) infinite alternate;
}

.atmo-blob--c {
  bottom: -36%;
  left: 14%;
  background: radial-gradient(circle, var(--atmo-c), transparent 66%);
  animation: app-underlight-c 38s var(--ease-in-out) infinite alternate;
}

/* Hero only: a glass reflection — one wide highlight band slowly crossing
   the field, the same optical gesture as GlassSurface's surfaceFlow. */
.atmo-sheen {
  position: absolute;
  top: -25%;
  bottom: -25%;
  left: 0;
  width: 30%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
  animation: app-glass-flow 42s linear infinite;
}

/* The eight rooms. Every base stays deep enough for the fixed dusk type. */
.atmo--hero {
  --atmo-base: linear-gradient(180deg, #150a2e, #07040f);
  --atmo-a: rgba(147, 90, 255, 0.4);
  --atmo-b: rgba(255, 110, 180, 0.3);
  --atmo-c: rgba(80, 140, 255, 0.26);
}

.atmo--tutor {
  --atmo-base: linear-gradient(180deg, #061a3e, #040914);
  --atmo-a: rgba(64, 140, 255, 0.36);
  --atmo-b: rgba(70, 215, 255, 0.22);
  --atmo-c: rgba(120, 100, 255, 0.2);
}

.atmo--notes {
  --atmo-base: linear-gradient(180deg, #2a1606, #140a03);
  --atmo-a: rgba(255, 170, 70, 0.3);
  --atmo-b: rgba(255, 110, 90, 0.22);
  --atmo-c: rgba(255, 220, 150, 0.14);
}

.atmo--flashcards {
  --atmo-base: linear-gradient(180deg, #260a24, #11040f);
  --atmo-a: rgba(255, 90, 180, 0.3);
  --atmo-b: rgba(180, 90, 255, 0.26);
  --atmo-c: rgba(255, 150, 120, 0.16);
}

.atmo--graph {
  --atmo-base: linear-gradient(180deg, #04231e, #02100e);
  --atmo-a: rgba(40, 220, 180, 0.28);
  --atmo-b: rgba(60, 200, 255, 0.22);
  --atmo-c: rgba(120, 255, 200, 0.13);
}

.atmo--engine {
  --atmo-base: linear-gradient(180deg, #120d33, #060414);
  --atmo-a: rgba(110, 90, 255, 0.36);
  --atmo-b: rgba(60, 170, 255, 0.26);
  --atmo-c: rgba(200, 110, 255, 0.2);
}

/* "Warm white" from the brief, rendered as warm dawn: unmistakably warm,
   still deep enough for the fixed light typography. */
.atmo--roadmap {
  --atmo-base: linear-gradient(180deg, #241206, #100704);
  --atmo-a: rgba(255, 190, 120, 0.3);
  --atmo-b: rgba(255, 140, 100, 0.2);
  --atmo-c: rgba(255, 235, 190, 0.15);
}

/* The ending: minimal, one distant bloom — never pure black. */
.atmo--coming {
  --atmo-base: linear-gradient(180deg, #0c0719, #04030a);
  --atmo-a: rgba(140, 110, 255, 0.22);
  --atmo-b: rgba(255, 130, 200, 0.08);
  --atmo-c: rgba(90, 120, 255, 0.06);
}

/* Light moves slower than glass: the color field cross-fades over 1.6s
   while the content above refocuses in 1s. Both layers are absolutely
   positioned, so enter and leave overlap into a true dissolve. */
.atmo-enter-active,
.atmo-leave-active {
  transition: opacity 1600ms var(--ease-out);
}

.atmo-enter-from,
.atmo-leave-to {
  opacity: 0;
}

/* ------------------------------------------------------------------ */
/* Slides                                                              */
/* ------------------------------------------------------------------ */

.presentation-viewport {
  position: relative;
  flex: 1;
  overflow: hidden;
}

/*
 * One slide = one viewport. Content is biased upward so it breathes above
 * the persistent dock instead of colliding with it; the atmosphere still
 * runs the full frame underneath the glass.
 */
.pp-slide {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  padding: var(--space-8);
  padding-bottom: 148px;
  text-align: center;
}

/* Keynote typography — hero scale, confident weight, almost no words. The
   palette stays the fixed dusk of the stage (every atmosphere is deep). */
.pp-title {
  margin: 0;
  font-family: var(--font-headline-family);
  font-size: clamp(2.5rem, 5.5vw, 4.4rem);
  font-weight: 680;
  line-height: 1.08;
  letter-spacing: var(--font-headline-tracking);
  color: rgba(248, 246, 255, 0.97);
}

/* Hero headline — the product name poured in light. */
.pp-slide--hero .pp-title {
  font-size: clamp(3rem, 8vw, 6.4rem);
  background: linear-gradient(100deg, #ffffff 8%, #cabcff 38%, #ff9ed8 66%, #93b8ff 95%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
}

.pp-slide--coming .pp-title {
  font-size: clamp(2.8rem, 7vw, 5.6rem);
  text-shadow: 0 0 60px rgba(150, 120, 255, 0.35);
}

.pp-line {
  margin: 0;
  max-width: 44ch;
  font-size: clamp(1.05rem, 1.8vw, 1.35rem);
  line-height: 1.6;
  color: rgba(240, 237, 252, 0.72);
}

.pp-hint {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-6) 0 0;
  font-size: var(--text-xs);
  color: rgba(240, 237, 252, 0.45);
}

/* ------------------------------------------------------------------ */
/* Visual compositions — CSS only, one per feature slide               */
/* ------------------------------------------------------------------ */

.pp-visual {
  position: relative;
  width: min(640px, 90vw);
  height: clamp(200px, 30vh, 290px);
  margin-bottom: var(--space-4);
}

/* Shared floating pane: the dock's optical family without any filter —
   smoked translucency, bright entrance lip, chromatic rims, deep shadow. */
.pp-card {
  position: absolute;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: var(--space-4);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.11), rgba(255, 255, 255, 0.04));
  box-shadow:
    0 34px 60px -30px rgba(0, 0, 0, 0.6),
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    inset 1px 1px 0 rgba(150, 216, 255, 0.06),
    inset -1px -1px 0 rgba(255, 188, 150, 0.06);
}

.skel {
  display: block;
  height: 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.28);
}

/* Tutor — question, answer, and an older exchange sinking into depth.
   NOTE: static pose uses the individual `rotate`/`translate`/`scale`
   properties throughout these compositions — app-float animates `transform`
   and would silently replace a static transform, un-rotating the card. */
.tutor-card--question {
  top: 6%;
  left: 4%;
  width: 200px;
  rotate: -2deg;
  animation: app-float 9s var(--ease-in-out) infinite alternate;
}

.tutor-card--answer {
  top: 20%;
  right: 6%;
  width: 264px;
  rotate: 1.5deg;
  animation: app-float 11s var(--ease-in-out) -3s infinite alternate;
}

.tutor-card--echo {
  bottom: 4%;
  left: 16%;
  width: 170px;
  opacity: 0.5;
  filter: blur(1px);
  animation: app-float 13s var(--ease-in-out) -6s infinite alternate;
}

.tutor-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(90, 160, 255, 0.9), rgba(120, 100, 255, 0.9));
  box-shadow: 0 0 18px rgba(90, 150, 255, 0.5);
  color: rgba(255, 255, 255, 0.95);
}

/* Notes — three tinted snippets, connected by faint threads. */
.note-card {
  width: 190px;
}

.note-card--amber {
  top: 8%;
  left: 6%;
  rotate: -5deg;
  background: linear-gradient(to bottom, rgba(255, 176, 80, 0.24), rgba(255, 176, 80, 0.06));
  animation: app-float 10s var(--ease-in-out) infinite alternate;
}

.note-card--rose {
  top: 2%;
  right: 8%;
  rotate: 3deg;
  background: linear-gradient(to bottom, rgba(255, 110, 150, 0.24), rgba(255, 110, 150, 0.06));
  animation: app-float 12s var(--ease-in-out) -4s infinite alternate;
}

.note-card--violet {
  bottom: 4%;
  left: 34%;
  width: 170px;
  rotate: -1deg;
  background: linear-gradient(to bottom, rgba(160, 120, 255, 0.24), rgba(160, 120, 255, 0.06));
  animation: app-float 11s var(--ease-in-out) -7s infinite alternate;
}

.note-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.45);
}

.notes-thread {
  position: absolute;
  height: 1.5px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transform-origin: 0 50%;
}

.notes-thread--a {
  left: 26%;
  top: 34%;
  width: 42%;
  transform: rotate(8deg);
}

.notes-thread--b {
  left: 44%;
  top: 46%;
  width: 34%;
  transform: rotate(-32deg);
}

/* Flashcards — a deck at rest: two echoes behind, one card in the light. */
.visual-stack {
  display: flex;
}

/* Centred by geometry (262×158), not by transform — the front card's float
   animation owns `transform` outright. */
.stack-card {
  left: calc(50% - 131px);
  top: calc(50% - 79px);
  width: 262px;
  height: 158px;
  align-items: center;
  justify-content: center;
  border-radius: 22px;
  color: rgba(248, 246, 255, 0.92);
}

.stack-card--far {
  translate: 0 -46px;
  scale: 0.85;
  opacity: 0.32;
}

.stack-card--mid {
  translate: 0 -23px;
  scale: 0.93;
  opacity: 0.58;
}

.stack-card--front {
  gap: var(--space-3);
  background: linear-gradient(to bottom, rgba(255, 130, 190, 0.2), rgba(160, 100, 255, 0.1));
  animation: app-float 10s var(--ease-in-out) infinite alternate;
}

.stack-card--front .skel {
  width: 54%;
}

/* Knowledge Graph — a 480×260 constellation; links radiate from the core
   (all origin at its centre, rotated to each node) plus outer ties. */
.visual-graph {
  width: 480px;
  height: 260px;
  flex-shrink: 0;
}

.graph-link {
  position: absolute;
  left: 240px;
  top: 130px;
  height: 1.5px;
  background: linear-gradient(90deg, rgba(120, 255, 220, 0.45), rgba(120, 255, 220, 0.08));
  transform-origin: 0 50%;
}

.graph-link--outer {
  background: linear-gradient(90deg, rgba(120, 220, 255, 0.25), rgba(120, 220, 255, 0.05));
}

.graph-node {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: rgba(150, 255, 225, 0.95);
  box-shadow:
    0 0 14px rgba(90, 240, 200, 0.75),
    0 0 42px rgba(90, 240, 200, 0.3);
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
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 1px solid rgba(160, 255, 230, 0.4);
  background: radial-gradient(circle, rgba(40, 220, 180, 0.28), rgba(40, 220, 180, 0.05) 70%);
  box-shadow: 0 0 40px rgba(60, 230, 190, 0.4);
  transform: translate(-50%, -50%);
  color: rgba(210, 255, 240, 0.95);
}

@keyframes pp-node-pulse {
  from {
    opacity: 0.55;
    transform: translate(-50%, -50%) scale(0.85);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.12);
  }
}

/* AI Engine — a breathing core; tokens rise through the field like sparks. */
.visual-engine {
  display: flex;
  align-items: center;
  justify-content: center;
}

.engine-core {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112px;
  height: 112px;
  border-radius: 32px;
  border: 1px solid rgba(190, 170, 255, 0.45);
  background: linear-gradient(to bottom, rgba(140, 110, 255, 0.24), rgba(140, 110, 255, 0.06));
  box-shadow:
    0 0 50px rgba(130, 100, 255, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
  color: rgba(235, 228, 255, 0.95);
}

.engine-halo {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(140, 110, 255, 0.3), transparent 70%);
  transform: translate(-50%, -50%);
  animation: pp-halo 6s var(--ease-in-out) infinite alternate;
}

@keyframes pp-halo {
  from {
    opacity: 0.55;
    transform: translate(-50%, -50%) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.15);
  }
}

.engine-particle {
  position: absolute;
  left: calc(3% + var(--i) * 6.7%);
  bottom: 6%;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(180, 160, 255, 0.85);
  opacity: 0;
  animation: pp-rise calc(5.5s + var(--i) * 0.35s) linear calc(var(--i) * -1.35s) infinite;
}

.engine-particle:nth-child(even) {
  width: 3px;
  height: 3px;
  background: rgba(120, 190, 255, 0.8);
}

@keyframes pp-rise {
  0% {
    transform: translateY(0);
    opacity: 0;
  }
  12% {
    opacity: 0.75;
  }
  80% {
    opacity: 0.25;
  }
  100% {
    transform: translateY(-230px);
    opacity: 0;
  }
}

/* Roadmap — one line of light; the shipped chapters glow, the next pulses. */
.visual-roadmap {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: min(680px, 88vw);
  height: auto;
  padding-top: 60px;
  margin-bottom: var(--space-6);
  /* The line and its stops breathe as ONE object — dots never leave the track. */
  animation: app-float 14s var(--ease-in-out) infinite alternate;
}

.roadmap-track {
  position: absolute;
  left: 2%;
  right: 2%;
  top: 67px;
  height: 2px;
  border-radius: 1px;
  background: linear-gradient(
    90deg,
    rgba(255, 200, 140, 0.7),
    rgba(255, 200, 140, 0.45) 62%,
    rgba(255, 200, 140, 0.1)
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
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(255, 205, 150, 0.95);
  box-shadow:
    0 0 16px rgba(255, 185, 120, 0.7),
    0 0 44px rgba(255, 185, 120, 0.3);
}

.roadmap-stop.is-soon .roadmap-dot {
  background: transparent;
  border: 2px solid rgba(255, 205, 150, 0.75);
  box-shadow: 0 0 20px rgba(255, 185, 120, 0.4);
  /* In-flow pulse — pp-node-pulse carries the graph nodes' centring
     translate and would lift this dot off the track. */
  animation: pp-soon-pulse 3.5s var(--ease-in-out) infinite alternate;
}

@keyframes pp-soon-pulse {
  from {
    opacity: 0.55;
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
  color: rgba(255, 240, 224, 0.8);
}

/* ------------------------------------------------------------------ */
/* Chrome + transitions                                                */
/* ------------------------------------------------------------------ */

/* Bead rail — the deck's only chrome, resting at the right edge. */
.presentation-beads {
  position: absolute;
  right: clamp(16px, 3vw, 40px);
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: center;
}

.bead {
  width: 6px;
  height: 6px;
  padding: 0;
  border: none;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.28);
  cursor: pointer;
  transition:
    background-color 600ms var(--ease-out),
    box-shadow 600ms var(--ease-out);
}

.bead.active {
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.45);
}

.bead:focus-visible {
  outline: var(--border-width-md) solid var(--color-focus-ring);
  outline-offset: 2px;
}

/*
 * Optical refocus at stage scale — the camera re-aims at the next exhibit:
 * defocus, slight vertical compression, a short drift opposite the travel.
 * Phase 11 slows it to ~1s (the atmosphere underneath dissolves over 1.6s,
 * so light always settles after the content). No overshoot; reduced motion
 * collapses it to an instant swap via the global override.
 */
.refocus-next-enter-active,
.refocus-next-leave-active,
.refocus-prev-enter-active,
.refocus-prev-leave-active {
  transition:
    opacity 1000ms var(--ease-out),
    transform 1000ms var(--ease-out),
    filter 1000ms var(--ease-out);
}

.refocus-next-enter-from {
  opacity: 0;
  transform: translateY(64px) scaleY(0.98);
  filter: blur(12px);
}
.refocus-next-leave-to {
  opacity: 0;
  transform: translateY(-50px) scaleY(0.97);
  filter: blur(12px);
}
.refocus-prev-enter-from {
  opacity: 0;
  transform: translateY(-64px) scaleY(0.98);
  filter: blur(12px);
}
.refocus-prev-leave-to {
  opacity: 0;
  transform: translateY(50px) scaleY(0.97);
  filter: blur(12px);
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 640px) {
  .pp-slide {
    padding: var(--space-6);
    padding-bottom: 128px;
  }

  /* The compositions shrink as one object; their layout box is clipped by
     the viewport wrapper, so the scaled artwork stays centred. */
  .visual-tutor,
  .visual-notes,
  .visual-stack,
  .visual-graph,
  .visual-engine {
    transform: scale(0.72);
    margin-block: -28px;
  }

  .roadmap-label {
    font-size: var(--text-xs);
  }
}
</style>
