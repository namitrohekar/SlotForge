import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vitest/config'

const backend = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // One origin in development, so session cookies and CSRF behave as they will in production.
    proxy: {
      '/api': backend,
      // Keep the browser's host, or Spring rejects Google's redirect as not matching the one it sent.
      '/oauth2': { target: backend, changeOrigin: false },
      '/login/oauth2': { target: backend, changeOrigin: false },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    include: ['src/**/*.test.{js,jsx}'],
  },
})
