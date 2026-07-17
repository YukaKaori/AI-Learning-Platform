import { onBeforeUnmount, onMounted, ref, shallowRef, watch, type Ref } from 'vue'

export interface GlassSpotlightOptions {
  /** Element the light reacts to (a GlassSurface root). Enables proximity. */
  card?: Ref<HTMLElement | null>
  /**
   * Base light radius in px. Falls back to the stage's computed
   * `--glass-light-radius` token, then 360.
   */
  radius?: number
  /**
   * Distance (px) from the card edge over which proximity ramps 1 → 0.
   * Defaults to the base radius.
   */
  influence?: number
}

interface Point {
  x: number
  y: number
}

/**
 * Interactive optical lighting for glass stages — the engine behind the
 * "living" GlassSurface.
 *
 * Tracks the pointer over `stage` and drives CSS custom properties via
 * requestAnimationFrame interpolation (rendering is never bound to mousemove;
 * pointer events only move the goalposts):
 *
 *   on the stage element
 *     --glass-light-x / --glass-light-y   smoothed light position (px)
 *     --glass-light-radius                effective light radius (px)
 *     --glass-light-strength              0..1 light presence, proximity-shaped
 *   on the card element (options.card), in card-local coordinates
 *     the same four, plus
 *     --glass-proximity                   0..1 pointer closeness to the card
 *
 * Rendering must consume those variables (gradient positions, mask radii,
 * layer opacities). The returned refs update every frame and exist for logic
 * and tests — binding them in templates would re-render Vue at 60fps.
 *
 * Engineering constraints (same contract as useSpotlight):
 * - Only CSS variables are written inside the frame loop; element rects are
 *   cached and re-measured at most once per frame, and only when a resize /
 *   scroll / observer callback marked them dirty.
 * - One ResizeObserver total (stage + card); it is created on enable and
 *   never re-created per event.
 * - The loop stops once every value settles; pointer events restart it.
 * - Disabled for coarse pointers and under prefers-reduced-motion — the
 *   travelling light never ignites (strength stays 0) while the permanent
 *   CSS-default lighting of GlassSurface stays untouched.
 */
export function useGlassSpotlight(stage: Ref<HTMLElement | null>, options: GlassSpotlightOptions = {}) {
  const card = options.card

  /** False on coarse pointers / reduced motion — the light stays dormant. */
  const active = ref(false)
  /** Raw pointer position, viewport px. Updated from pointer events. */
  const cursor = shallowRef<Point>({ x: 0, y: 0 })
  /** Eased pointer position, viewport px. Updated once per frame. */
  const smoothedCursor = shallowRef<Point>({ x: 0, y: 0 })
  /** 0..1 eased presence of the light (blooms on enter, decays on leave). */
  const intensity = ref(0)
  /** 0..1 eased closeness of the light to the card. */
  const proximity = ref(0)
  /** Final light strength written to CSS: intensity shaped by proximity. */
  const brightness = ref(0)
  /** Effective light radius in px (base, gently swelled by proximity). */
  const radius = ref(options.radius ?? 0)

  // Interpolated state (current) chasing the pointer (goal).
  let currentX = 0
  let currentY = 0
  let currentIntensity = 0
  let currentProximity = 0
  let goalX = 0
  let goalY = 0
  let goalIntensity = 0
  let frame = 0
  let baseRadius = options.radius ?? 360

  // Rects are read outside the loop's hot path: measured when dirty, at most
  // once per frame.
  let rectsDirty = true
  let stageRect: DOMRect | null = null
  let cardRect: DOMRect | null = null
  let resizeObserver: ResizeObserver | null = null

  const supportsHover = window.matchMedia('(hover: hover) and (pointer: fine)')
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')

  function measureRects() {
    stageRect = stage.value?.getBoundingClientRect() ?? null
    cardRect = card?.value?.getBoundingClientRect() ?? null
    rectsDirty = false
  }

  function markRectsDirty() {
    rectsDirty = true
    // An idle light still needs one frame to re-aim proximity after a resize.
    if (active.value && currentIntensity > 0.001) schedule()
  }

  /** Distance from a point to the nearest edge of a rect (0 when inside). */
  function distanceToRect(x: number, y: number, rect: DOMRect) {
    const dx = Math.max(rect.left - x, 0, x - rect.right)
    const dy = Math.max(rect.top - y, 0, y - rect.bottom)
    return Math.hypot(dx, dy)
  }

  function apply(effectiveRadius: number) {
    const stageEl = stage.value
    if (!stageEl) return
    const strength = brightness.value

    if (stageRect) {
      stageEl.style.setProperty('--glass-light-x', `${(currentX - stageRect.left).toFixed(1)}px`)
      stageEl.style.setProperty('--glass-light-y', `${(currentY - stageRect.top).toFixed(1)}px`)
    }
    stageEl.style.setProperty('--glass-light-radius', `${effectiveRadius.toFixed(1)}px`)
    stageEl.style.setProperty('--glass-light-strength', strength.toFixed(3))

    const cardEl = card?.value
    if (cardEl && cardRect) {
      cardEl.style.setProperty('--glass-light-x', `${(currentX - cardRect.left).toFixed(1)}px`)
      cardEl.style.setProperty('--glass-light-y', `${(currentY - cardRect.top).toFixed(1)}px`)
      cardEl.style.setProperty('--glass-light-radius', `${effectiveRadius.toFixed(1)}px`)
      cardEl.style.setProperty('--glass-light-strength', strength.toFixed(3))
      cardEl.style.setProperty('--glass-proximity', currentProximity.toFixed(3))
    }
  }

  function tick() {
    if (rectsDirty) measureRects()

    // Exponential approach — long, calm attenuation instead of a snap.
    currentX += (goalX - currentX) * 0.12
    currentY += (goalY - currentY) * 0.12
    currentIntensity += (goalIntensity - currentIntensity) * 0.075

    const influence = options.influence ?? baseRadius
    const goalProximity = cardRect
      ? Math.max(0, Math.min(1, 1 - distanceToRect(currentX, currentY, cardRect) / influence))
      : 0
    currentProximity += (goalProximity - currentProximity) * 0.1

    smoothedCursor.value = { x: currentX, y: currentY }
    intensity.value = currentIntensity
    proximity.value = currentProximity
    // Proximity lifts the light, it never replaces the base presence.
    brightness.value = currentIntensity * (0.7 + 0.3 * currentProximity)
    const effectiveRadius = baseRadius * (1 + 0.12 * currentProximity)
    radius.value = effectiveRadius

    apply(effectiveRadius)

    const settled =
      Math.abs(goalX - currentX) < 0.5 &&
      Math.abs(goalY - currentY) < 0.5 &&
      Math.abs(goalIntensity - currentIntensity) < 0.004 &&
      Math.abs(goalProximity - currentProximity) < 0.004
    frame = settled ? 0 : requestAnimationFrame(tick)
  }

  function schedule() {
    if (frame === 0) {
      frame = requestAnimationFrame(tick)
    }
  }

  function onPointerMove(event: PointerEvent) {
    goalX = event.clientX
    goalY = event.clientY
    cursor.value = { x: goalX, y: goalY }
    schedule()
  }

  function onPointerEnter(event: PointerEvent) {
    goalX = event.clientX
    goalY = event.clientY
    cursor.value = { x: goalX, y: goalY }
    // Ignite in place instead of sweeping in from the last exit point.
    if (currentIntensity < 0.02) {
      currentX = goalX
      currentY = goalY
    }
    goalIntensity = 1
    rectsDirty = true
    schedule()
  }

  function onPointerLeave() {
    goalIntensity = 0
    schedule()
  }

  function enable() {
    const el = stage.value
    if (!el || active.value) return
    active.value = true
    if (options.radius == null) {
      baseRadius = parseFloat(getComputedStyle(el).getPropertyValue('--glass-light-radius')) || 360
    }
    rectsDirty = true
    el.addEventListener('pointerenter', onPointerEnter)
    el.addEventListener('pointermove', onPointerMove)
    el.addEventListener('pointerleave', onPointerLeave)
    window.addEventListener('resize', markRectsDirty)
    window.addEventListener('scroll', markRectsDirty, { passive: true, capture: true })
    resizeObserver = new ResizeObserver(markRectsDirty)
    resizeObserver.observe(el)
    if (card?.value) resizeObserver.observe(card.value)
  }

  function disable() {
    const stageEl = stage.value
    active.value = false
    cancelAnimationFrame(frame)
    frame = 0
    currentIntensity = 0
    currentProximity = 0
    goalIntensity = 0
    intensity.value = 0
    proximity.value = 0
    brightness.value = 0
    resizeObserver?.disconnect()
    resizeObserver = null
    window.removeEventListener('resize', markRectsDirty)
    window.removeEventListener('scroll', markRectsDirty, { capture: true })
    if (stageEl) {
      stageEl.removeEventListener('pointerenter', onPointerEnter)
      stageEl.removeEventListener('pointermove', onPointerMove)
      stageEl.removeEventListener('pointerleave', onPointerLeave)
      stageEl.style.setProperty('--glass-light-strength', '0')
    }
    const cardEl = card?.value
    if (cardEl) {
      cardEl.style.setProperty('--glass-light-strength', '0')
      cardEl.style.setProperty('--glass-proximity', '0')
    }
  }

  function evaluate() {
    if (supportsHover.matches && !reducedMotion.matches) {
      enable()
    } else {
      disable()
    }
  }

  onMounted(() => {
    evaluate()
    supportsHover.addEventListener('change', evaluate)
    reducedMotion.addEventListener('change', evaluate)
  })

  // The card element usually mounts after the stage (template ref through a
  // component instance) — fold it into the existing observer when it lands.
  if (card) {
    watch(card, (el, previous) => {
      if (previous && resizeObserver) resizeObserver.unobserve(previous)
      if (el && resizeObserver) resizeObserver.observe(el)
      rectsDirty = true
    })
  }

  onBeforeUnmount(() => {
    disable()
    supportsHover.removeEventListener('change', evaluate)
    reducedMotion.removeEventListener('change', evaluate)
  })

  return { active, cursor, smoothedCursor, intensity, radius, proximity, brightness }
}
