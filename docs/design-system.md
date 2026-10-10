# V1 Design System Foundation

This document is the canonical description of the Soine visual
design system foundation. It is the contract slice of
[#168][issue-168] (Soine 固有の visual design system を
定義・実装する). It is the foundation that the V1 polish EPIC
(#167) builds on; every later polish issue that touches a
color, a font, a corner radius, or a button should reach for one
of the tokens or components documented here.

Re-evaluate when:

- a new color, type role, or component is added to the system,
- a screen needs a new visual hierarchy the current tokens do
  not express,
- the team picks a non-system component library for a future
  platform.

## Why this exists

Before this slice, every screen reached directly for the
Material3 defaults: `Card`, `Button`, `TextButton`,
`OutlinedButton` with their default colors and shapes. The
defaults are functional, but they do not say "Soine". Two
screens that should feel like one product feel like two
different products. The design system foundation is the
smallest set of tokens and components that the bedtime /
sleeping / morning / dream / settings surfaces all reach for,
so a single visual decision moves the whole product at once.

## Palette

`SoineColors` is a small warm-dark palette with a cream
companion anchor and a midnight bed background. Every other
token is derived from these two anchors. A new screen should
compose with these tokens; introducing a new color outside the
palette is a design change that should be reviewed with the
whole palette in mind.

| Role | Color | Use |
| --- | --- | --- |
| `cream` | `#F2D9B6` | The companion anchor; primary text on the bedtime surface |
| `midnight` | `#1A1622` | The bed; the app background and the surface behind every screen |
| `dusk` | `#221D2E` | The first tonal step up from midnight; the body surface behind text |
| `twilight` | `#2B2438` | A second tonal step; rare; used for inset cards that need more separation |
| `dim` | `#352C45` | A third tonal step; reserved for the rare case the visual hierarchy needs three levels |
| `glow` | `#FFE0B2` | The primary CTA fill; the bright anchor the user reaches for |
| `ember` | `#E0A86C` | The pressed CTA accent; the warm hue that lets the button react to a touch |
| `hush` | `#8B7E9C` | The secondary text / quiet action color; the bedtime voice |
| `sunrise` | `#FFB77A` | The morning accent; reserved for the morning summary screen |
| `soft` | `#B7AFC6` | The placeholder / disabled text color |
| `divider` | `#352C4533` | A hairline of `dim` at 20% alpha |

The `SoineDesignSystemTest` pins the anchors (`cream` and
`midnight`) so a `git diff` of the wrong file does not change
them. The surface ordering is also pinned: midnight < dusk <
twilight < dim.

## Typography

`SoineTypography` is a night-oriented type scale. The largest
styles are reserved for the moment of falling asleep and waking
up. Line height is a touch more generous than the Material3
default because the bedtime screen reads at arm's length under
low light.

| Role | Size | Weight | Line height | Use |
| --- | --- | --- | --- | --- |
| `displaySmall` | 36 sp | Light | 44 sp | Reserved for the morning "おはよう" / "今日をはじめる" reveal |
| `headlineLarge` | 28 sp | SemiBold | 36 sp | Reserved for the moment of falling asleep |
| `headlineMedium` | 22 sp | SemiBold | 30 sp | Section headlines (e.g. "今日のふりかえり") |
| `headlineSmall` | 20 sp | Medium | 28 sp | Step headlines in the onboarding flow |
| `titleLarge` | 18 sp | SemiBold | 26 sp | Card titles |
| `titleMedium` | 16 sp | SemiBold | 24 sp | Button text, list-item titles |
| `titleSmall` | 14 sp | Medium | 20 sp | Sub-section headers (paired with `SoineSectionHeader`) |
| `bodyLarge` | 16 sp | Normal | 26 sp | Long body text — generous line height for low-light reading |
| `bodyMedium` | 14 sp | Normal | 22 sp | Default body text |
| `bodySmall` | 12 sp | Normal | 18 sp | Captions and metadata |
| `labelLarge` | 14 sp | SemiBold | 20 sp | Button labels for quiet actions |
| `labelMedium` | 12 sp | Medium | 16 sp | Chip labels |
| `labelSmall` | 11 sp | Medium | 14 sp | Reserved for the rare tertiary label |

The `SoineDesignSystemTest` pins the `displaySmall` line-height
ratio at >= 1.15 so a future refactor that tightens the line
height breaks the smoke gate.

## Spacing

`SoineTokens.SpacingXxs..SpacingXxl` form an 8dp scale. A new
screen should compose with these tokens; raw `.dp` literals
outside the scale are a design change.

| Token | Value | Use |
| --- | --- | --- |
| `SpacingXxs` | 2 dp | Hairline padding inside icons |
| `SpacingXs` | 4 dp | Tight padding between an icon and a label |
| `SpacingSm` | 8 dp | Default vertical rhythm inside a list row |
| `SpacingMd` | 16 dp | Default content padding |
| `SpacingLg` | 24 dp | Outer screen padding |
| `SpacingXl` | 32 dp | Hero spacing between a title and a CTA |
| `SpacingXxl` | 48 dp | Reserved for the bedtime / morning reveal |

## Radius

`SoineTokens.RadiusSm..RadiusXl` are a soft, generous curve
set. The scale is small (8-32dp) so the corners are soft without
becoming a pill.

| Token | Value | Use |
| --- | --- | --- |
| `RadiusSm` | 8 dp | Quiet buttons, chips, list items |
| `RadiusMd` | 16 dp | Primary CTA, dialog corners |
| `RadiusLg` | 24 dp | Panels, group surfaces |
| `RadiusXl` | 32 dp | The companion scene surface |

## Touch targets

`SoineTokens.PrimaryCtaHeight` is 56 dp; `SoineTokens.QuietCtaHeight`
is 48 dp. The touch-target token (`SoineTokens.MinTouchTargetDp`)
is a re-export of `AccessibilityPolicy.MIN_TOUCH_TARGET_DP` so
the design system and the accessibility policy share a single
source of truth. The `SoineDesignSystemTest` pins the equality.

## Components

The V1 component surface is intentionally small. Every component
is a Composable in `app.soine.design`. A new screen should reach
for one of these before reaching for `androidx.compose.material3.*`
directly.

| Component | Use | Touch target | Notes |
| --- | --- | --- | --- |
| `SoinePrimaryButton` | One per screen at most | 56 dp | Filled with `glow`; takes a `contentDescription` argument; pinned by `AccessibilitySourceAuditTest` |
| `SoineQuietButton` | Secondary actions | 48 dp | Text-only with `hush` color; takes a `contentDescription` argument |
| `SoinePanel` | Grouping content | n/a | Tonal elevation over `dusk`; not focusable |
| `SoineSectionHeader` | Sub-section header | n/a | `titleSmall` over `hush` |
| `SoineQuietButtonRow` | "もどる / 次へ" pair | mixed | Pairs a quiet and a primary button |
| `SoineDivider` | Hairline divider | n/a | 1 dp tall; 20% alpha |

`SoinePanel` is **not** a focusable surface. Interactive cards
(Dream Album rows, etc.) keep using `Card` so they stay
focusable. The design system ships one surface per kind so the
two visual languages do not bleed into each other.

## Theme

`SoineTheme` is a `MaterialTheme` wrapper that pins the
`darkColorScheme` to the Soine palette. The theme is
dusk-first by design; a future light-mode pass adds a second
scheme without changing call sites. A new screen should be
wrapped in `SoineTheme { ... }` rather than `MaterialTheme { ... }`.

## Accessibility interaction

The design system and the accessibility policy are deliberately
aligned:

- `SoinePrimaryButton` takes a `contentDescription` argument
  and applies it through a `.semantics { ... }` block. The
  bedtime and sleeping CTAs pass `AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION`
  and `AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION` respectively.
- `SoineQuietButton` does the same.
- `SoineTokens.MinTouchTargetDp` is a re-export of
  `AccessibilityPolicy.MIN_TOUCH_TARGET_DP`. A future contributor
  who bumps the design-system token without bumping the
  accessibility policy is caught by `SoineDesignSystemTest`.

## Re-evaluation triggers

- A new screen is added that does not reach for `SoineTheme`.
- A new CTA bypasses `SoinePrimaryButton` / `SoineQuietButton`.
- A new color is introduced outside the `SoineColors` palette.
- A new spacing / radius value is introduced outside the
  `SoineTokens` scale.
- A future polish issue (#169-#178) needs a new visual
  hierarchy the current tokens do not express.

[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
