import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [tailwindcss(), svelte()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8100',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src',
      '$lib': '/src/lib'
    }
  },
  optimizeDeps: {
    include: ['@dagrejs/dagre', '@dagrejs/graphlib'],
    esbuildOptions: {
      plugins: []
    }
  },
  build: {
    chunkSizeWarningLimit: 1700,
    commonjsOptions: {
      include: [/@dagrejs/, /node_modules/],
      transformMixedEsModules: true,
      esmExternals: true
    },
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('@xyflow') || id.includes('elkjs') || id.includes('@dagrejs')) {
              return 'vendor-viz';
            }
            if (id.includes('@tanstack')) {
              return 'vendor-table';
            }
            if (id.includes('bits-ui') || id.includes('svelte-sonner')) {
              return 'vendor-ui';
            }
            if (id.includes('date-fns') || id.includes('@internationalized/date')) {
              return 'vendor-date';
            }
          }
        }
      }
    }
  }
});
