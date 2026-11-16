// A closed popover has no accessible name in the test DOM, so a row's menu is found by its label attribute.
export const menuNamed = (name) => document.querySelector(`[role="menu"][aria-label="${name}"]`)
