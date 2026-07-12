import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/**
 * Pointer-driven spotlight for the glass scene.
 *
 * Tracks the pointer over `target` and writes three CSS custom properties on
 * it — `--spot-x`, `--spot-y` (px) and `--spot-r` (px, animated 0 ↔ radius) —
 * which GlassScene consumes in a radial-gradient mask to melt a soft hole
 * into the frosted veil.
 *
 * Engineering constraints:
 * - GPU-friendly: only CSS variables are written; no Vue reactivity, no
 *   re-renders, no layout reads inside the frame loop.
 * - The rAF loop runs only while the values are still converging; once the
 *   spring settles it stops until the next pointer event.
 * - Disabled entirely for touch/coarse pointers and under
 *   `prefers-reduced-motion` — the veil then stays uniformly frosted.
 */
export function useSpotlight(target: Ref<HTMLElement | null>) {
  /** False on touch devices / reduced motion — lets callers drop the mask. */
  const active = ref(false)

  // Interpolated state (current) chasing the pointer (goal).
  let currentX = 0
  let currentY = 0
  let currentR = 0
  let goalX = 0
  let goalY = 0
  let goalR = 0
  let frame = 0
  let radius = 340

  const supportsHover = window.matchMedia('(hover: hover) and (pointer: fine)')
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')

  function apply() {
    const el = target.value
    if (!el) return
    el.style.setProperty('--spot-x', `${currentX.toFixed(1)}px`)
    el.style.setProperty('--spot-y', `${currentY.toFixed(1)}px`)
    el.style.setProperty('--spot-r', `${currentR.toFixed(1)}px`)
  }

  function tick() {
    // Exponential approach — large feather of movement, ~60 FPS friendly.
    currentX += (goalX - currentX) * 0.14
    currentY += (goalY - currentY) * 0.14
    currentR += (goalR - currentR) * 0.1
    apply()

    const settled =
      Math.abs(goalX - currentX) < 0.5 &&
      Math.abs(goalY - currentY) < 0.5 &&
      Math.abs(goalR - currentR) < 0.5
    frame = settled ? 0 : requestAnimationFrame(tick)
  }

  function schedule() {
    if (frame === 0) {
      frame = requestAnimationFrame(tick)
    }
  }

  function onPointerMove(event: PointerEvent) {
    const el = target.value
    if (!el) return
    const rect = el.getBoundingClientRect()
    goalX = event.clientX - rect.left
    goalY = event.clientY - rect.top
    goalR = radius
    schedule()
  }

  function onPointerEnter(event: PointerEvent) {
    const el = target.value
    if (!el) return
    // Bloom in from the entry point instead of sweeping across the scene.
    const rect = el.getBoundingClientRect()
    currentX = goalX = event.clientX - rect.left
    currentY = goalY = event.clientY - rect.top
    goalR = radius
    schedule()
  }

  function onPointerLeave() {
    goalR = 0
    schedule()
  }

  function enable() {
    const el = target.value
    if (!el || active.value) return
    active.value = true
    radius = parseFloat(getComputedStyle(el).getPropertyValue('--spotlight-radius')) || 340
    el.addEventListener('pointerenter', onPointerEnter)
    el.addEventListener('pointermove', onPointerMove)
    el.addEventListener('pointerleave', onPointerLeave)
  }

  function disable() {
    const el = target.value
    active.value = false
    cancelAnimationFrame(frame)
    frame = 0
    if (el) {
      el.removeEventListener('pointerenter', onPointerEnter)
      el.removeEventListener('pointermove', onPointerMove)
      el.removeEventListener('pointerleave', onPointerLeave)
      el.style.setProperty('--spot-r', '0px')
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

  onBeforeUnmount(() => {
    disable()
    supportsHover.removeEventListener('change', evaluate)
    reducedMotion.removeEventListener('change', evaluate)
  })

  return { active }
}
