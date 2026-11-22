# Verification plan

| Scenario | Observable outcome |
| --- | --- |
| Fresh launch and mode selection | Home offers both modes, galleries differ and opening a template creates an editable design. |
| A/B import and selection | Import distinct local photos, tap each directly, transform and replace A, verify B at every step. Repeat with B. |
| Undo and redo | Undo a pinch, replacement, layer reorder and cutout. Redo returns to the matching document each time. |
| Airplane mode | On a fresh install, enable airplane mode before launch. Import local photos in both modes. Remove backgrounds independently, cancel one preview, apply the other, replace its backdrop, save an editable design and export a PNG. |
| Urdu export | Edit a multiline string containing “اپنا ووٹ ضرور ڈالیں” and mixed English and Urdu, compare shaping, wrapping and alignment in preview and export. |
| Export | Verify PNG/JPEG MIME types, transparency, requested pixel dimensions and matching artwork. Open gallery output and share an actual image attachment. Test API 24–28's document picker. |
| Persistence | Move and select a layer, force-stop, reopen from My Designs and compare images, transformations, selection, text and styles. |
| Small screen and rotation | Use compact and landscape layouts; verify focused panel remains operable and selection survives rotation. |
| Memory | Check 4K with layered photos, large EXIF JPEG files, repeated replacements and background-removal cancellation. |
| Legacy comparison | Play through the original build, compare artwork, divider, text presets, photo crop, sticker catalogs and export, and record discrepancies. |
| Signed upgrade | Install the last signed legacy build, then install a native release signed with the same lineage. Verify app ID, version increase, and access to legacy gallery outputs. |

## Automated coverage and remaining manual checks

The API 35 run installs debug and instrumentation APKs from the verify job, enables airplane mode, and exercises both editor entry points, tool-panel Back navigation, direct selection and drag isolation of seeded photos, offline segmentation, project reopening, and PNG/JPEG output. The Urdu render is captured from the native renderer as a separate artifact. Production signing is a separate release gate.

Use a physical device or emulator for the system Photo Picker replacement flow, pinch and rotate, undo/redo, font scaling at 1.3× or higher, and 320×640, 360×800 and 412×915 dp layout checks. Verify the share sheet with a recipient app and the document-picker export fallback on API 24–28. Compare the native artwork and templates with the installed legacy app before marking parity complete.
