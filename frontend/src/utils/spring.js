const MAX_FRAME_SECONDS = 0.064
const STEP_SECONDS = 1 / 240
const REST_DISTANCE = 0.5
const REST_SPEED = 5

// Animates one number with real velocity, for gestures that a CSS curve cannot continue.
// damping and response mean the same as Apple's: 1 has no bounce, response is the period in seconds.
export function spring({ from, to, velocity = 0, damping = 1, response = 0.4, onFrame, onRest }) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    onFrame(to)
    onRest?.()
    return { stop() {} }
  }

  const stiffness = ((2 * Math.PI) / response) ** 2
  const friction = (4 * Math.PI * damping) / response
  let position = from
  let speed = velocity
  let last = performance.now()
  let frame

  const tick = (now) => {
    let remaining = Math.min((now - last) / 1000, MAX_FRAME_SECONDS)
    last = now

    while (remaining > 0) {
      const step = Math.min(remaining, STEP_SECONDS)
      speed += (-stiffness * (position - to) - friction * speed) * step
      position += speed * step
      remaining -= step
    }

    if (Math.abs(position - to) < REST_DISTANCE && Math.abs(speed) < REST_SPEED) {
      onFrame(to)
      onRest?.()
      return
    }

    onFrame(position)
    frame = requestAnimationFrame(tick)
  }

  frame = requestAnimationFrame(tick)
  return { stop: () => cancelAnimationFrame(frame) }
}
