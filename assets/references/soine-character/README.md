# Soine character — visual references

This directory holds **reference images**, not runtime assets used by the Android/iOS app.

Use these images to communicate the intended character design, expressions, motion, and bedtime scenes when creating the Blender master model and sprite animations.

## Suggested filenames

| Filename | Intended content |
| --- | --- |
| `character-turnaround.png` | Front, side, back and three-quarter views of the approved character |
| `character-poses.png` | Sitting, greeting, sleep, waking and other poses |
| `character-expressions.png` | Neutral, smile, blink, yawn, sleepy and other facial expressions |
| `character-motion-reference.png` | Poses to guide transitions and animation |
| `bedtime-scene-reference.png` | Night/bedtime/morning composition and mood |
| `ui-experience-reference.png` | Overall Soine app screens and visual direction (optional) |

These are **suggested names**, not a requirement. Keep the actual files, descriptions, and image count in sync as references are approved. Prefer short, descriptive, lowercase ASCII filenames.

## Workflow

1. Upload source/reference images here, or share them in the ChatGPT conversation for review and organization.
2. Rename files by content rather than keeping generated timestamp filenames.
3. Compare reference images against one another and select a single character master design before modeling.
4. Build the Blender character from the approved reference, and derive subsequent poses and sprite frames from the same master model.

## Important distinction

- `assets/references/soine-character/`: art direction only.
- `assets/companion/`: existing GLB proof-of-concept and related production pipeline.
- Generated intermediate sprite sheets, previews, and temporary renders should follow the art pipeline's output and Git tracking rules; do not place them here by default.

These references may be AI-generated. Keep their origin and approval status documented; check any applicable usage rights before external redistribution. This repository is public, so anything committed here is publicly visible.
