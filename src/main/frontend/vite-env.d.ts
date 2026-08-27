/* eslint-disable @typescript-eslint/triple-slash-reference --
 * A triple-slash reference is the only way to pull in an ambient type package. Using
 * `import 'vite/client'` instead would turn this file into a module, which stops the
 * global/ImportMeta declarations from applying. This is the form Vite documents.
 */
/// <reference types="vite/client" />

// Brings in Vite's ambient client types: side-effect imports of '*.css', asset modules
// ('*.svg', '*.png', ...), import.meta.hot, and import.meta.glob.
//
// Vite 7 pulled these in transitively, so the project typechecked without an explicit
// reference; Vite 8 no longer does, which surfaced as TS2882 on every `import './x.css'`.
// Referencing vite/client directly is what Vite documents, and it covers the whole asset
// surface rather than just the CSS declarations we happened to need.
//
// Project-specific VITE_* env vars are declared in the root types.d.ts; the ImportMetaEnv
// interfaces merge.
