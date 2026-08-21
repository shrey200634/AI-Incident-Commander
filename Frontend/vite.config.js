import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {


     '/api/auth': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },

      // Command Service (writes via Gateway or Direct)
      '/api/incidents': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      // Query Service (reads via Gateway or Direct)
      '/api/query': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      // DLQ Admin Controller (Query Service)
      '/api/admin': {
        target: 'http://localhost:18082',
        changeOrigin: true,
      },
      // Agent Service Test Endpoints
      '/test': {
        target: 'http://localhost:18083',
        changeOrigin: true,
      },
      // WebSocket Relay (Query Service)
      '/ws': {
        target: 'http://localhost:18082',
        ws: true,
      },
    },
  },
})
