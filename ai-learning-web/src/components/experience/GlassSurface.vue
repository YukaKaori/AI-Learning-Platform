<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, useId } from 'vue'

/**
 * Liquid-glass surface ported from React Bits' <GlassSurface />.
 *
 * A per-instance SVG displacement filter (fed by a generated data-URI map)
 * refracts whatever sits behind the element via `backdrop-filter: url(#…)`.
 * Browsers that can't apply SVG filters as backdrop-filters (Safari, Firefox)
 * fall back to a plain frosted-glass look.
 *
 * `class`/`style` from the caller fall through to the root element.
 */
type Channel = 'R' | 'G' | 'B'

const props = withDefaults(
  defineProps<{
    /** Width in px (number) or any CSS length (string). */
    width?: number | string
    /** Height in px (number) or any CSS length (string). */
    height?: number | string
    borderRadius?: number
    /** Border width factor for the displacement map edge. */
    borderWidth?: number
    /** Brightness percentage of the displacement map core. */
    brightness?: number
    /** Opacity of the displacement map core. */
    opacity?: number
    /** Blur of the displacement map core, px. */
    blur?: number
    /** Output blur (feGaussianBlur stdDeviation). */
    displace?: number
    /** Background frost opacity (0-1). */
    backgroundOpacity?: number
    /** Backdrop saturation factor. */
    saturation?: number
    /** Main displacement scale. */
    distortionScale?: number
    redOffset?: number
    greenOffset?: number
    blueOffset?: number
    xChannel?: Channel
    yChannel?: Channel
    /** Blend mode between the two gradient plates of the map. */
    mixBlendMode?: string
  }>(),
  {
    width: 200,
    height: 80,
    borderRadius: 20,
    borderWidth: 0.07,
    brightness: 50,
    opacity: 0.93,
    blur: 11,
    displace: 0,
    backgroundOpacity: 0,
    saturation: 1,
    distortionScale: -180,
    redOffset: 0,
    greenOffset: 10,
    blueOffset: 20,
    xChannel: 'R',
    yChannel: 'G',
    mixBlendMode: 'difference',
  },
)

const uniqueId = useId().replace(/:/g, '-')
const filterId = `glass-filter-${uniqueId}`
const redGradId = `red-grad-${uniqueId}`
const blueGradId = `blue-grad-${uniqueId}`

const containerRef = ref<HTMLElement | null>(null)
const svgSupported = ref(false)
// Rendered size of the surface; the displacement map is regenerated from it.
const measured = ref({ width: 400, height: 200 })

const displacementMap = computed(() => {
  const { width: actualWidth, height: actualHeight } = measured.value
  const edgeSize = Math.min(actualWidth, actualHeight) * (props.borderWidth * 0.5)

  const svgContent = `
    <svg viewBox="0 0 ${actualWidth} ${actualHeight}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="${redGradId}" x1="100%" y1="0%" x2="0%" y2="0%">
          <stop offset="0%" stop-color="#0000"/>
          <stop offset="100%" stop-color="red"/>
        </linearGradient>
        <linearGradient id="${blueGradId}" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color="#0000"/>
          <stop offset="100%" stop-color="blue"/>
        </linearGradient>
      </defs>
      <rect x="0" y="0" width="${actualWidth}" height="${actualHeight}" fill="black"></rect>
      <rect x="0" y="0" width="${actualWidth}" height="${actualHeight}" rx="${props.borderRadius}" fill="url(#${redGradId})" />
      <rect x="0" y="0" width="${actualWidth}" height="${actualHeight}" rx="${props.borderRadius}" fill="url(#${blueGradId})" style="mix-blend-mode: ${props.mixBlendMode}" />
      <rect x="${edgeSize}" y="${edgeSize}" width="${actualWidth - edgeSize * 2}" height="${actualHeight - edgeSize * 2}" rx="${props.borderRadius}" fill="hsl(0 0% ${props.brightness}% / ${props.opacity})" style="filter:blur(${props.blur}px)" />
    </svg>
  `

  return `data:image/svg+xml,${encodeURIComponent(svgContent)}`
})

const containerStyle = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  borderRadius: `${props.borderRadius}px`,
  '--glass-frost': String(props.backgroundOpacity),
  '--glass-saturation': String(props.saturation),
  '--filter-id': `url(#${filterId})`,
}))

// SVG filters inside backdrop-filter only work in Chromium; WebKit and
// Firefox parse the value but render nothing, so they get the fallback skin.
function supportsSVGFilters(): boolean {
  const isWebkit = /Safari/.test(navigator.userAgent) && !/Chrome/.test(navigator.userAgent)
  const isFirefox = /Firefox/.test(navigator.userAgent)
  if (isWebkit || isFirefox) return false

  const div = document.createElement('div')
  div.style.backdropFilter = `url(#${filterId})`
  return div.style.backdropFilter !== ''
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  svgSupported.value = supportsSVGFilters()

  if (containerRef.value) {
    resizeObserver = new ResizeObserver((entries) => {
      const rect = entries[0]?.contentRect
      if (rect && rect.width > 0 && rect.height > 0) {
        measured.value = { width: rect.width, height: rect.height }
      }
    })
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})
</script>

<template>
  <div
    ref="containerRef"
    class="glass-surface"
    :class="svgSupported ? 'glass-surface--svg' : 'glass-surface--fallback'"
    :style="containerStyle"
  >
    <svg class="glass-surface__filter" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <filter
          :id="filterId"
          color-interpolation-filters="sRGB"
          x="0%"
          y="0%"
          width="100%"
          height="100%"
        >
          <feImage
            :href="displacementMap"
            x="0"
            y="0"
            width="100%"
            height="100%"
            preserveAspectRatio="none"
            result="map"
          />

          <feDisplacementMap
            in="SourceGraphic"
            in2="map"
            :scale="distortionScale + redOffset"
            :xChannelSelector="xChannel"
            :yChannelSelector="yChannel"
            result="dispRed"
          />
          <feColorMatrix
            in="dispRed"
            type="matrix"
            values="1 0 0 0 0
                    0 0 0 0 0
                    0 0 0 0 0
                    0 0 0 1 0"
            result="red"
          />

          <feDisplacementMap
            in="SourceGraphic"
            in2="map"
            :scale="distortionScale + greenOffset"
            :xChannelSelector="xChannel"
            :yChannelSelector="yChannel"
            result="dispGreen"
          />
          <feColorMatrix
            in="dispGreen"
            type="matrix"
            values="0 0 0 0 0
                    0 1 0 0 0
                    0 0 0 0 0
                    0 0 0 1 0"
            result="green"
          />

          <feDisplacementMap
            in="SourceGraphic"
            in2="map"
            :scale="distortionScale + blueOffset"
            :xChannelSelector="xChannel"
            :yChannelSelector="yChannel"
            result="dispBlue"
          />
          <feColorMatrix
            in="dispBlue"
            type="matrix"
            values="0 0 0 0 0
                    0 0 0 0 0
                    0 0 1 0 0
                    0 0 0 1 0"
            result="blue"
          />

          <feBlend in="red" in2="green" mode="screen" result="rg" />
          <feBlend in="rg" in2="blue" mode="screen" result="output" />
          <feGaussianBlur in="output" :stdDeviation="displace" />
        </filter>
      </defs>
    </svg>

    <div class="glass-surface__content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.glass-surface {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: opacity 0.26s ease-out;
}

.glass-surface__filter {
  width: 100%;
  height: 100%;
  pointer-events: none;
  position: absolute;
  inset: 0;
  opacity: 0;
  z-index: -1;
}

.glass-surface__content {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  border-radius: inherit;
  position: relative;
  z-index: 1;
}

.glass-surface--svg {
  background: light-dark(hsl(0 0% 100% / var(--glass-frost, 0)), hsl(0 0% 0% / var(--glass-frost, 0)));
  backdrop-filter: var(--filter-id) saturate(var(--glass-saturation, 1));
  box-shadow:
    0 0 2px 1px light-dark(color-mix(in oklch, black, transparent 85%), color-mix(in oklch, white, transparent 65%))
      inset,
    0 0 10px 4px light-dark(color-mix(in oklch, black, transparent 90%), color-mix(in oklch, white, transparent 85%))
      inset,
    0px 4px 16px rgba(17, 17, 26, 0.05),
    0px 8px 24px rgba(17, 17, 26, 0.05),
    0px 16px 56px rgba(17, 17, 26, 0.05),
    0px 4px 16px rgba(17, 17, 26, 0.05) inset,
    0px 8px 24px rgba(17, 17, 26, 0.05) inset,
    0px 16px 56px rgba(17, 17, 26, 0.05) inset;
}

.glass-surface--fallback {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(12px) saturate(1.8) brightness(1.1);
  -webkit-backdrop-filter: blur(12px) saturate(1.8) brightness(1.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 8px 32px 0 rgba(31, 38, 135, 0.2),
    0 2px 16px 0 rgba(31, 38, 135, 0.1),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.4),
    inset 0 -1px 0 0 rgba(255, 255, 255, 0.2);
}

/* The app toggles theme with `html.dark` (see tokens.css), so key the dark
   fallback off that instead of prefers-color-scheme. */
html.dark .glass-surface--fallback {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px) saturate(1.8) brightness(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.8) brightness(1.2);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow:
    inset 0 1px 0 0 rgba(255, 255, 255, 0.2),
    inset 0 -1px 0 0 rgba(255, 255, 255, 0.1);
}

@supports not (backdrop-filter: blur(10px)) {
  .glass-surface--fallback {
    background: rgba(255, 255, 255, 0.4);
    box-shadow:
      inset 0 1px 0 0 rgba(255, 255, 255, 0.5),
      inset 0 -1px 0 0 rgba(255, 255, 255, 0.3);
  }

  .glass-surface--fallback::before {
    content: '';
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.15);
    border-radius: inherit;
    z-index: -1;
  }

  html.dark .glass-surface--fallback {
    background: rgba(0, 0, 0, 0.4);
  }

  html.dark .glass-surface--fallback::before {
    background: rgba(255, 255, 255, 0.05);
  }
}

.glass-surface:focus-visible {
  outline: 2px solid light-dark(#007aff, #0a84ff);
  outline-offset: 2px;
}
</style>
