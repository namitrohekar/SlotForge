// Runs before first paint, so the page never flashes the wrong theme.
;(function () {
  var theme
  try {
    theme = JSON.parse(localStorage.getItem('slotforge-preferences')).state.theme || 'system'
  } catch {
    theme = 'system'
  }
  if (theme === 'system') {
    theme = matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  document.documentElement.dataset.theme = theme
})()
