import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // In dev, the browser talks to Vite and Vite forwards /api to Spring Boot,
    // so no backend URL is hardcoded in components and CORS isn't involved.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
