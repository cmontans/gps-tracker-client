import { defineConfig } from 'vite';

// Relative base so the built app works both at a domain root (Netlify) and when
// previewed from a file path / subdirectory.
export default defineConfig({
  base: './',
  build: {
    outDir: 'dist',
    target: 'es2020'
  }
});
