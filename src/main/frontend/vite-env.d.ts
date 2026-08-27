/* eslint-disable @typescript-eslint/triple-slash-reference --
 * Ambient type packages can only be pulled in by a triple-slash reference. `import`
 * would make this a module, which stops the global declarations from applying.
 */
/// <reference types="vite/client" />

// Vite's ambient client types: side-effect imports of '*.css', asset modules ('*.svg',
// '*.png', ...), import.meta.hot, and import.meta.glob.
//
// Project-specific VITE_* variables are declared in the root types.d.ts; the two
// ImportMetaEnv interfaces merge.
