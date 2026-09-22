import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const apiUrl = loadEnv(mode, process.cwd()).VITE_API_URL ?? 'http://localhost:8080'
  const securityHeaders = {
    'Content-Security-Policy': `default-src 'self'; connect-src 'self' ${apiUrl}; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none'`,
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'no-referrer'
  }

  return {
    plugins: [vue()],
    server: { port: 5173, strictPort: true },
    preview: { host: '0.0.0.0', port: 5173, strictPort: true, headers: securityHeaders }
  }
})
