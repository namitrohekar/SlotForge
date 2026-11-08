import { useEffect, useRef } from 'react'
import { spring } from '../utils/spring.js'

const SLOP = 10
const PROJECTION_SECONDS = 0.1
const STALE_VELOCITY_MS = 80
const RUBBER_BAND = 0.55
const SHEET_SPRING = { damping: 0.8, response: 0.3 }

const isSheet = () => window.matchMedia('(max-width: 47.99rem)').matches

const rubberBand = (distance, limit) => -(1 - 1 / ((-distance * RUBBER_BAND) / limit + 1)) * limit

// Drag a phone sheet down to dismiss it. Keyboard users have Escape and the Close button.
export function useSheetDrag(sheetRef, onDismiss) {
  const gesture = useRef(null)
  const motion = useRef(null)

  const present = (y) => {
    const sheet = sheetRef.current
    sheet.style.transform = `translateY(${y}px)`
    sheet.style.setProperty('--sheet-progress', String(Math.max(0, 1 - y / sheet.offsetHeight)))
  }

  const reset = () => {
    const sheet = sheetRef.current
    motion.current?.stop()
    motion.current = null
    gesture.current = null
    sheet.style.removeProperty('transform')
    sheet.style.removeProperty('transition')
    sheet.style.removeProperty('--sheet-progress')
  }

  const onPointerDown = (event) => {
    if (event.button !== 0 || !isSheet() || event.target.closest('button')) return

    // Start from what is on screen, so a sheet can be caught in the middle of any animation.
    const sheet = sheetRef.current
    const y = new DOMMatrixReadOnly(getComputedStyle(sheet).transform).m42
    motion.current?.stop()
    motion.current = null
    sheet.style.transition = 'none'
    present(y)

    gesture.current = {
      active: Math.abs(y) > 1,
      startY: event.clientY,
      grabY: event.clientY - y,
      y,
      velocity: 0,
      time: event.timeStamp,
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const onPointerMove = (event) => {
    const drag = gesture.current
    if (!drag) return

    if (!drag.active) {
      if (Math.abs(event.clientY - drag.startY) < SLOP) return
      drag.active = true
      drag.grabY = event.clientY - drag.y
    }

    const raw = event.clientY - drag.grabY
    const y = raw >= 0 ? raw : rubberBand(raw, sheetRef.current.offsetHeight)
    const seconds = (event.timeStamp - drag.time) / 1000

    if (seconds > 0) drag.velocity = 0.7 * ((y - drag.y) / seconds) + 0.3 * drag.velocity
    drag.y = y
    drag.time = event.timeStamp
    present(y)
  }

  const onPointerEnd = (event) => {
    const drag = gesture.current
    if (!drag) return
    gesture.current = null

    const velocity = event.timeStamp - drag.time > STALE_VELOCITY_MS ? 0 : drag.velocity
    const height = sheetRef.current.offsetHeight
    const dismiss = drag.y + velocity * PROJECTION_SECONDS > height / 2

    motion.current = spring({
      ...SHEET_SPRING,
      from: drag.y,
      to: dismiss ? height : 0,
      velocity,
      onFrame: present,
      onRest: () => {
        motion.current = null
        if (dismiss) onDismiss()
        else reset()
      },
    })
  }

  useEffect(() => () => motion.current?.stop(), [])

  return {
    reset,
    handleProps: { onPointerDown, onPointerMove, onPointerUp: onPointerEnd, onPointerCancel: onPointerEnd },
  }
}
