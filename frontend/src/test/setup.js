import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(cleanup)

// jsdom does not implement these browser features.
window.matchMedia ??= (query) => ({
  matches: false,
  media: query,
  addEventListener() {},
  removeEventListener() {},
})

HTMLDialogElement.prototype.showModal ??= function showModal() {
  this.open = true
}

HTMLDialogElement.prototype.close ??= function close() {
  this.open = false
  this.dispatchEvent(new Event('close'))
}

HTMLElement.prototype.hidePopover ??= function hidePopover() {}
