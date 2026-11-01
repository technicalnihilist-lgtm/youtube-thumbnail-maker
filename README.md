# Thumbnail Studio

A lightweight, offline-first YouTube thumbnail maker for Android. Every pixel is rendered on the device with the platform graphics stack — no AI models, no cloud services, no accounts, no trackers. It launches and works fully in airplane mode.

- **Canvas:** fixed 1280 × 720 (16:9) document, exported at exactly that size.
- **Editor:** text, photos, shapes, stickers, gradient/solid/photo backdrops, layers, undo/redo, snapping, safe-area guides.
- **Export:** PNG or JPEG straight to `Pictures/Thumbnail Studio`, plus Android sharesheet.
- **On-device AI:** subject cutout and photo text recognition, both local.
- **Size:** 7.0 MB release APK in 1.1.0 (5.9 MB in 1.0.0), minSdk 24, targetSdk 34.

## On-device AI (1.1.0)

Two real machine-learning models run on the phone. Nothing is uploaded, there is no API key,
no account and no cloud inference:

| Feature | Model | Where it runs |
| --- | --- | --- |
| **AI cutout** — lift the subject out of a photo | ML Kit Subject Segmentation | On device; model delivered by Google Play services |
| **AI text** — read text already in a photo into an editable layer | ML Kit Text Recognition (Latin) | On device; model delivered by Google Play services |

Both models are delivered by Google Play services instead of being bundled, which keeps the APK
at ~7 MB; the first use of an AI feature may let Play services fetch the model once, and after
that inference is fully offline. Both features degrade gracefully: if Play services is missing,
the AI cutout explains itself and the flood-fill cutout plus the manual erase brush still work. `AiText` cleans the raw OCR output
(picks the likely headline, drops noise, respects a 90-character budget) and is unit tested.

## What is rule-based (and what is not AI)

The "Smart" tab is deliberately **not** generative AI — it is deterministic design maths, and the
app labels it that way:

| Tool | How it actually works |
| --- | --- |
| Contrast checker | WCAG 2.1 relative-luminance maths |
| Auto-fix contrast | Picks black/white text against the sampled backdrop |
| Palette generator | Colour-harmony rotation (complementary / analogous / triadic / mono) |
| Palette from design | Bucketed colour quantisation of the backdrop |
| Smart layouts | Four rule-based composition templates |
| Background cutout (manual) | Edge flood-fill ("magic wand") chroma removal — the AI cutout above is the ML path |
| Erase brush | Manual per-pixel masking |
| Adjustments | `ColorMatrix` grading + 3×3 unsharp kernel |

If a tool is not AI, the app does not call it AI.

## Features

**Text**
Multiple text layers, 8 bundled typefaces plus custom `.ttf`/`.otf` import, bold/italic/underline/uppercase, letter and line spacing, three alignments, rotation, curved (arc) text, gradient fill, outline, drop shadow, glow, extruded 3D depth, background highlight boxes, and automatic shrink-to-fit so text never overflows its box. Eight style presets (gaming, music, tech, education, vlog, news, cinematic, 3D pop).

**Images**
Gallery import (Photo Picker, no permissions), replace, corner radius, border, horizontal flip, brightness / contrast / saturation / hue / sharpness, **on-device AI subject cutout**, colour-background cutout, manual erase brush, **on-device AI text extraction**, EXIF-correct decoding, memory-bounded bitmap cache.

**Shapes & stickers**
Rectangle, rounded rectangle, ellipse, triangle, star, burst, arrow, line, frame, badge, speech bubble, heart — solid or gradient fill, border, rotation. 24 emoji stickers that scale and rotate.

**Canvas & workflow**
Pinch-to-zoom (0.6×–8×), pan, drag / resize / rotate handles with hit-testing, snap to centre guides, grid, safe-area overlay, layers panel with visibility, lock, reorder, duplicate, delete; 40-step undo/redo; debounced autosave; project reopening; duplicate design; save any design as a reusable template.

**Templates**
15 original templates generated in code (so they cost the APK almost nothing and nothing is downloaded): Gaming, Music, Technology, Education, Vlogs, Cinematic, News, Reviews, Podcast, Entertainment. All elements stay editable.

**Export**
1280 × 720 PNG (lossless) or JPEG (quality slider), MediaStore write on Android 10+, app-private file plus sharesheet on older versions, background rendering off the main thread, YouTube feed-size preview in the editor.

## Install

Download the APK from the [latest release](../../releases/latest) and open it on the phone (allow "install unknown apps" for your browser/file manager). Requires Android 7.0 (API 24) or newer.

The published APK is signed with the project's local debug key so it installs without shipping any signing secrets. See *Release signing* below.

## Build from source

Requirements: JDK 17, Android SDK with `platforms;android-34` and `build-tools;34.0.0`.

```bash
git clone https://github.com/technicalnihilist-lgtm/youtube-thumbnail-maker.git
cd thumbnail-studio
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease    # app/build/outputs/apk/release/app-release.apk
./gradlew :app:testDebugUnitTest  # 23 unit tests
```

`local.properties` (not committed) may point at your SDK:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Architecture

```
app/src/main/java/com/thumbnailstudio/app/
├── model/      Model.kt        Immutable Project/Layer document + transforms
├── render/     ThumbnailRenderer.kt  Single renderer for preview, feed preview, export
│               TextRenderer.kt       StaticLayout text, effects, arc text
│               ShapePaths.kt         Vector geometry for all shapes
│               ImagePipeline.kt      Decode, EXIF, ColorMatrix grading, sharpen, blur, cache
│               Cutout.kt             Flood-fill cutout + manual eraser
├── data/       ProjectStore.kt  JSON documents, atomic writes, thumbnails, imports
│               Templates.kt     Programmatic template library
│               SettingsStore.kt SharedPreferences-backed preferences
├── tools/      SmartTools.kt    WCAG contrast, palettes, layouts, scoring
├── export/     Exporter.kt      MediaStore + FileProvider + sharesheet
├── update/     UpdateChecker.kt Optional GitHub Releases check
└── ui/         Compose screens: home, templates, projects, editor, settings
```

**AI layer.** `ai/OnDeviceAi.kt` wraps the two ML Kit tasks in coroutines with cancellation and
error handling; `ai/AiText.kt` is pure Kotlin post-processing with no Android types, so it is
unit-tested like the rest of the logic.

**Why this shape.** The document is immutable data; the editor mutates it through a controller that owns undo/redo snapshots. One renderer draws the interactive canvas, the small feed preview and the exported file, so preview and export can never drift apart. Layers live in a fixed 1280 × 720 coordinate space and are drawn through a single canvas scale, which keeps hit-testing, handles and export maths identical.

**Storage.** Projects are individual JSON documents (kotlinx.serialization) written atomically to app storage. This avoids Room/KSP codegen — a measurable build-time and APK-size saving — while still being crash-safe. Images are copied into app storage on import; nothing is ever referenced from another app's sandbox.

**Memory.** Bitmaps decode downsampled to a 1600 px bound, are cached in an `LruCache` sized to a quarter of the heap, and the cache is flushed from `onTrimMemory`/`onLowMemory`. Export renders one 1280 × 720 bitmap off the main thread.

## Tests

`app/src/test/...` covers the parts that are pure logic and therefore worth pinning (29 cases):

- project add/replace/duplicate/reorder/visibility
- layer translate, uniform scale (with text-size coupling), rotation normalisation
- JSON round-trip of the sealed layer hierarchy
- every template builds valid, uniquely-identified layers; category coverage
- WCAG contrast ratios, readability bands, best foreground colour
- palette quantisation, harmony generation, hex round-trip
- auto-contrast fix raises the score, layout suggestions keep layers on canvas
- export filename sanitisation
- semantic version comparison for the update checker

```
> Task :app:testDebugUnitTest
29 tests completed
```

## Release signing

The repository contains no keystore and no secrets. The release build is signed with the standard local debug key so that CI and local builds produce an installable artifact:

```kotlin
release { signingConfig = signingConfigs.getByName("debug") }
```

For a Play-Store-style release, generate your own keystore and either add a `signingConfigs.release` block reading from environment variables, or provide the four standard Gradle properties in CI secrets (`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD`, and the base64 keystore). Never commit the keystore.

## Updates

Settings → Updates performs an optional GitHub Releases check (needs internet). It compares `versionName` with the latest release tag and offers the APK download link. Editing never requires it.

## Privacy

No analytics, no ads, no accounts, no telemetry, no third-party SDKs beyond AndroidX/Compose and
the two on-device ML Kit tasks. `INTERNET` is declared only for the optional update check. Photos,
text and projects never leave the device — the AI runs locally.

## Licence

No licence file is included: choose one (MIT, Apache-2.0, GPL-3.0 …) before distributing publicly.

---

## ⭐ Like this work? Give it a star

If you like this app and our work, please **star this repository** ⭐ and **[follow technicalnihilist-lgtm on GitHub](https://github.com/technicalnihilist-lgtm)**. If our work isn't for you, that's completely fine — the choice is yours. Thank you for stopping by!

<details>
<summary><b>🌍 Same message in 32 languages (A–Z + Indian languages)</b></summary>

- **A — Arabic** (العربية): ⭐ إذا أعجبك هذا التطبيق وعملنا، فيرجى منح المستودع نجمة ومتابعتنا. وإذا لم يعجبك عملنا، فلا مشكلة على الإطلاق — القرار قرارك. شكرًا لزيارتك!
- **B — Bengali** (বাংলা): ⭐ যদি আপনার এই অ্যাপ এবং আমাদের কাজ ভালো লাগে, তবে অনুগ্রহ করে রিপোজিটরিটিকে একটি স্টার দিন এবং আমাদের ফলো করুন। আমাদের কাজ যদি আপনার পছন্দ না হয়, তাতেও কোনো সমস্যা নেই — সিদ্ধান্ত আপনার। আমাদের পেজে আসার জন্য ধন্যবাদ!
- **C — Chinese** (中文): ⭐ 如果你喜欢这个应用和我们的作品，请给仓库点个星标并关注我们。如果我们的作品不适合你，也完全没关系——选择权在你。感谢你的到访！
- **D — Dutch** (Nederlands): ⭐ Vind je deze app en ons werk leuk? Geef de repository dan een ster en volg ons. Vind je ons werk niets, dan is dat helemaal prima — de keuze is aan jou. Bedankt voor je bezoek!
- **E — English** (English): ⭐ If you like this app and our work, please give the repository a star and follow us. If our work isn't for you, that's completely fine — the choice is yours. Thank you for stopping by!
- **F — French** (Français): ⭐ Si cette application et notre travail vous plaisent, mettez une étoile au dépôt et suivez-nous. Si notre travail n'est pas pour vous, aucun souci — c'est votre choix. Merci de votre visite !
- **G — German** (Deutsch): ⭐ Wenn dir diese App und unsere Arbeit gefallen, gib dem Repository einen Stern und folge uns. Wenn unsere Arbeit nichts für dich ist, ist das völlig in Ordnung — die Wahl liegt bei dir. Danke fürs Vorbeischauen!
- **H — Hindi** (हिन्दी): ⭐ अगर आपको यह ऐप और हमारा काम पसंद है, तो कृपया रिपॉजिटरी को स्टार दें और हमें फॉलो करें। अगर हमारा काम आपके लिए नहीं है, तो भी कोई बात नहीं — यह पूरी तरह आपकी मर्ज़ी है। यहाँ आने के लिए आपका धन्यवाद!
- **I — Indonesian** (Bahasa Indonesia): ⭐ Jika kamu suka aplikasi ini dan karya kami, beri bintang pada repositori ini dan ikuti kami. Kalau karya kami bukan untukmu, tidak masalah sama sekali — itu pilihanmu. Terima kasih sudah mampir!
- **J — Japanese** (日本語): ⭐ このアプリと私たちの仕事が気に入ったら、リポジトリにスターを付けてフォローしてください。合わない場合はまったく問題ありません — 選ぶのはあなたです。お越しいただきありがとうございます！
- **K — Korean** (한국어): ⭐ 이 앱과 저희 작업이 마음에 드신다면 저장소에 스타를 눌러 주시고 팔로우해 주세요. 저희 작업이 맞지 않으셔도 전혀 괜찮습니다 — 선택은 여러분의 몫입니다. 방문해 주셔서 감사합니다!
- **L — Lithuanian** (Lietuvių): ⭐ Jei tau patinka ši programa ir mūsų darbas, duok saugyklai žvaigždutę ir sek mus. Jei mūsų darbas ne tau, viskas gerai — pasirinkimas tavo. Ačiū, kad užsukai!
- **M — Malayalam** (മലയാളം): ⭐ നിങ്ങൾക്ക് ഈ ആപ്പും ഞങ്ങളുടെ പ്രവർത്തനവും ഇഷ്ടപ്പെട്ടെങ്കിൽ, ദയവായി റെപ്പോസിറ്ററിക്ക് ഒരു സ്റ്റാർ നൽകുകയും ഞങ്ങളെ ഫോളോ ചെയ്യുകയും ചെയ്യുക. ഞങ്ങളുടെ പ്രവർത്തനം നിങ്ങൾക്ക് അനുയോജ്യമല്ലെങ്കിൽ, അതും തികച്ചും കുഴപ്പമില്ല — തീരുമാനം നിങ്ങളുടേതാണ്. ഇവിടെ വന്നതിന് നന്ദി!
- **N — Nepali** (नेपाली): ⭐ यदि तपाईंलाई यो एप र हाम्रो काम मन पर्‍यो भने, कृपया रिपोजिटरीलाई स्टार दिनुहोस् र हामीलाई फलो गर्नुहोस्। यदि हाम्रो काम तपाईंलाई मन पर्दैन भने, पनि ठीकै छ — यो तपाईंको इच्छा हो। यहाँ आउनुभएकोमा धन्यवाद!
- **O — Odia** (ଓଡ଼ିଆ): ⭐ ଯଦି ଆପଣଙ୍କୁ ଏହି ଆପ୍ ଏବଂ ଆମର କାମ ଭଲ ଲାଗୁଛି, ତେବେ ଦୟାକରି ରିପୋଜିଟୋରୀକୁ ଗୋଟିଏ ଷ୍ଟାର୍ ଦିଅନ୍ତୁ ଏବଂ ଆମକୁ ଫଲୋ କରନ୍ତୁ। ଯଦି ଆମର କାମ ଆପଣଙ୍କ ପାଇଁ ନୁହେଁ, ତେବେ କିଛି ଅସୁବିଧା ନାହିଁ — ପସନ୍ଦ ଆପଣଙ୍କର। ଆମ ପାଖକୁ ଆସିଥିବାରୁ ଧନ୍ୟବାଦ!
- **P — Polish** (Polski): ⭐ Jeśli podoba Ci się ta aplikacja i nasza praca, daj repozytorium gwiazdkę i obserwuj nas. Jeśli nasza praca nie jest dla Ciebie, to zupełnie w porządku — wybór należy do Ciebie. Dziękujemy za odwiedziny!
- **Q — Quechua** (Runa Simi): ⭐ Kay app, ñuqaykup ruwasqanchikpas gustasunki chayqa, repositorioman huk estrella churay, ñuqaykutapas qatipayku. Mana gustasunki chayqa, allinmi — qampa akllayki. Hamusqaykimanta sulpayki!
- **R — Russian** (Русский): ⭐ Если вам нравится это приложение и наша работа, поставьте звёздочку репозиторию и подпишитесь на нас. Если наша работа не для вас — это совершенно нормально, выбор за вами. Спасибо, что заглянули!
- **S — Spanish** (Español): ⭐ Si te gusta esta app y nuestro trabajo, dale una estrella al repositorio y síguenos. Si nuestro trabajo no es para ti, no pasa nada — la decisión es tuya. ¡Gracias por pasarte!
- **T — Tamil** (தமிழ்): ⭐ உங்களுக்கு இந்த ஆப்பும் எங்க வேலையும் பிடிச்சிருந்தா, தயவுசெஞ்சு இந்த ரெபாசிட்டரிக்கு ஒரு ஸ்டார் குடுத்து, எங்களை ஃபாலோ பண்ணுங்க. எங்க வேலை உங்களுக்கு செட் ஆகலைன்னாலும் பரவாயில்லை — அது உங்க விருப்பம். இங்கே வந்ததுக்கு நன்றி!
- **U — Urdu** (اردو): ⭐ اگر آپ کو یہ ایپ اور ہمارا کام پسند آئے تو براہ کرم ریپازٹری کو سٹار دیں اور ہمیں فالو کریں۔ اگر ہمارا کام آپ کے لیے نہیں ہے، تو یہ بالکل ٹھیک ہے — انتخاب آپ کا ہے۔ یہاں تشریف لانے کا شکریہ!
- **V — Vietnamese** (Tiếng Việt): ⭐ Nếu bạn thích ứng dụng này và công việc của chúng tôi, hãy tặng kho lưu trữ một ngôi sao và theo dõi chúng tôi. Nếu công việc của chúng tôi không phù hợp với bạn, điều đó hoàn toàn ổn — lựa chọn là của bạn. Cảm ơn bạn đã ghé qua!
- **W — Welsh** (Cymraeg): ⭐ Os ydych chi'n hoffi'r ap hwn a'n gwaith, rhowch seren i'r storfa a dilynwch ni. Os nad yw ein gwaith ar eich cyfer, mae hynny'n hollol iawn — chi sydd i benderfynu. Diolch am alw heibio!
- **X — Xhosa** (isiXhosa): ⭐ Ukuba uyayithanda le app nomsebenzi wethu, nika irepo inkwenkwezi kwaye usilandele. Ukuba umsebenzi wethu awukufanelanga, akukho ngxaki — lukhetho lwakho. Enkosi ngokutyelela!
- **Y — Yoruba** (Yorùbá): ⭐ Bí app yìí àti iṣẹ́ wa bá wù ọ́, jọ̀wọ́ fún àkójọpọ̀ náà ní ìràwọ̀ kí o sì tẹ̀lé wa. Bí iṣẹ́ wa kò bá yẹ ọ́, kò séwu — ìwọ ni ó yàn. Ẹ ṣé fún wíwá!
- **Z — Zulu** (isiZulu): ⭐ Uma uthanda le app nomsebenzi wethu, nikeza isitoreji inkanyezi bese usilandela. Uma umsebenzi wethu ungakufanele, akunankinga — wukukhetha kwakho. Siyabonga ngokuvakasha!

**More Indian languages**

- **Assamese** (অসমীয়া): ⭐ যদি আপুনি এই এপটো আৰু আমাৰ কাম ভাল পায়, তেন্তে অনুগ্ৰহ কৰি ৰিপজিটৰীটোক এটা ষ্টাৰ দিয়ক আৰু আমাক ফ'ল' কৰক। যদি আমাৰ কাম আপোনাৰ বাবে নহয়, তেন্তে সেয়া একেবাৰে ঠিক আছে — সিদ্ধান্তটো আপোনাৰ। আমাৰ ইয়ালৈ অহাৰ বাবে ধন্যবাদ!
- **Gujarati** (ગુજરાતી): ⭐ જો તમને આ એપ અને અમારું કામ ગમતું હોય, તો પ્લીઝ રિપોઝિટરીને સ્ટાર આપો અને અમને ફોલો કરો. જો અમારું કામ તમને ન ગમતું હોય, તો તે પણ બરાબર છે — પસંદગી તમારી છે. અહીં આવવા બદલ આભાર!
- **Kannada** (ಕನ್ನಡ): ⭐ ನಿಮಗೆ ಈ ಆ್ಯಪ್ ಮತ್ತು ನಮ್ಮ ಕೆಲಸ ಇಷ್ಟವಾದರೆ, ದಯವಿಟ್ಟು ರೆಪೊಸಿಟರಿಗೆ ಸ್ಟಾರ್ ಕೊಡಿ ಮತ್ತು ನಮ್ಮನ್ನು ಫಾಲೋ ಮಾಡಿ. ನಮ್ಮ ಕೆಲಸ ನಿಮಗೆ ಸರಿಹೊಂದದಿದ್ದರೆ, ಅದೂ ಪರವಾಗಿಲ್ಲ — ಆಯ್ಕೆ ನಿಮ್ಮದು. ಭೇಟಿ ನೀಡಿದ್ದಕ್ಕೆ ಧನ್ಯವಾದಗಳು!
- **Marathi** (मराठी): ⭐ जर तुम्हाला हे ॲप आणि आमचं काम आवडलं असेल, तर कृपया रिपॉझिटरीला स्टार द्या आणि आम्हाला फॉलो करा. जर आमचं काम तुम्हाला आवडत नसेल, तर काही हरकत नाही — ती तुमची निवड आहे. इथे आल्याबद्दल धन्यवाद!
- **Punjabi** (ਪੰਜਾਬੀ): ⭐ ਜੇ ਤੁਹਾਨੂੰ ਇਹ ਐਪ ਅਤੇ ਸਾਡਾ ਕੰਮ ਪਸੰਦ ਹੈ, ਤਾਂ ਕਿਰਪਾ ਕਰਕੇ ਰਿਪੋਜ਼ਟਰੀ ਨੂੰ ਸਟਾਰ ਦਿਓ ਅਤੇ ਸਾਨੂੰ ਫਾਲੋ ਕਰੋ। ਜੇ ਸਾਡਾ ਕੰਮ ਤੁਹਾਡੇ ਲਈ ਨਹੀਂ ਹੈ, ਤਾਂ ਕੋਈ ਗੱਲ ਨਹੀਂ — ਚੋਣ ਤੁਹਾਡੀ ਹੈ। ਸਾਡੇ ਪੇਜ 'ਤੇ ਆਉਣ ਲਈ ਧੰਨਵਾਦ!
- **Telugu** (తెలుగు): ⭐ మీకు ఈ యాప్, మా పని నచ్చితే, దయచేసి రిపోజిటరీకి ఒక స్టార్ ఇచ్చి మమ్మల్ని ఫాలో అవ్వండి. ఒకవేళ మా పని మీకు నచ్చకపోతే, పర్వాలేదు — అది మీ ఇష్టం. మమ్మల్ని సందర్శించినందుకు ధన్యవాదాలు!

</details>
