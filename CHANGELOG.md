# Changelog

All notable changes to Thumbnail Studio. Versions follow Semantic Versioning.

## [1.1.0] — 2026-10-28

### Added — real on-device AI
- **AI cutout**: subject segmentation (ML Kit, on-device) lifts the main subject out of a
  photo with a transparent background. Replaces manual masking for the common case; the
  flood-fill and erase-brush tools remain for edge cases.
- **AI text**: on-device Latin text recognition reads text already present in a photo and
  drops it into the design as a new, fully editable text layer.
- `AiText` post-processing: picks the most likely headline lines out of raw OCR output,
  strips noise and respects a character budget (unit tested).
- Settings → About now states exactly which models run on the device and that nothing is
  uploaded.

### Changed
- Version 1.1.0 (versionCode 2), APK 7.0 MB (1.0.0 was 5.9 MB). Both ML models are delivered
  by Google Play services rather than bundled, which keeps the APK small; the first use of an
  AI feature may let Play services fetch the model once, after which inference is fully offline.
- Editor → Photo panel shows an "On-device AI" section with progress and error states, and
  degrades gracefully when a device has no Play services (the flood-fill cutout and erase brush
  keep working).
- ML Kit adds `ACCESS_NETWORK_STATE`, used only to decide whether a model is available locally.

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
