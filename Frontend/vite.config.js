import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Command Service (writes)
      '/api/incidents': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      // Query Service (reads)
      '/api/query': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      // DLQ Admin Controller (Query Service)
      '/api/admin': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      // Agent Service Test Endpoints
      '/test': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      // WebSocket Relay (Query Service)
      '/ws': {
        target: 'http://localhost:8082',
        ws: true,
      },
    },
  },
})
