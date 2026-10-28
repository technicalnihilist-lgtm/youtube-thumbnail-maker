# Changelog

All notable changes to Thumbnail Studio. Versions follow Semantic Versioning.

## [1.0.0] — 2026-10-28

First public release.

### Added
- 1280 × 720 (16:9) document model with exact export dimensions.
- Editor canvas: pinch zoom, pan, drag, uniform resize, rotation, snapping to centre guides.
- Layers panel: select, rename-aware list, visibility, lock, reorder, duplicate, delete.
- 40-step undo/redo with gesture coalescing (a drag is one history entry).
- Text engine: 8 bundled typefaces, custom font import, bold/italic/underline/uppercase,
  letter and line spacing, alignment, arc (curved) text, gradient fill, outline, shadow,
  glow, 3D extrusion, highlight boxes, auto shrink-to-fit.
- Eight text style presets: gaming, music, tech, education, vlog, news, cinematic, 3D pop.
- Photo layers: gallery import, replace, corner radius, border, flip, brightness/contrast/
  saturation/hue/sharpness, EXIF-correct decode.
- Background cutout by edge flood-fill, plus a manual erase brush with brush-size control.
- 12 shapes and 24 stickers, solid or gradient fills, borders, rotation.
- Backdrops: solid, gradient (8 presets, angle control), photo with blur; vignette and darken.
- 15 programmatic templates across 9 categories; save any design as a reusable template.
- Smart tools: WCAG contrast checks, one-tap contrast fix, readability scrim, palette
  generation and extraction, four smart layouts, design score and checklist, feed preview.
- Export to PNG or JPEG (quality slider) into `Pictures/Thumbnail Studio`, plus sharesheet.
- Debounced autosave, project list with thumbnails, duplicate, delete, reopen.
- Dark and light themes, Material 3 UI, translucent panels, adaptive launcher icon.
- Optional GitHub Releases update check (Settings → Updates).
- 23 unit tests covering project model, transforms, serialisation, templates, contrast maths,
  palettes, layouts, export naming and version comparison.

### Known limitations
- R8 code shrinking is disabled for release builds (build time/memory trade-off); the release
  APK is ~5.9 MB without it.
- No on-device automated UI test: the build environment used for this release had no
  hardware-accelerated emulator. Interactive flows were reviewed and unit-tested, but a
  device smoke test is recommended for the first install.
- Curved text is single-line by design.
- No video, no animation, no cloud sync.
