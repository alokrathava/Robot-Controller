# Jetpack Compose Design System Guide

This document serves as the official reference guide for the **Monochrome Design System** in the Robot Controller app, implemented in the `:core:theme` module.

All AI coding agents MUST consult and follow this document when adding or modifying Jetpack Compose UI components.

---

## 1. Module Structure & Package Location

Theme tokens and reusable components are located in `:core:theme`:
```
com.alokrathava.theme
├── Color.kt        # Monochrome palette, Dark/Light colors
├── Theme.kt        # MonochromeTheme composable & composition locals
├── Type.kt         # Typography styles
├── Spacing.kt      # Spacing scale & component paddings
├── Shapes.kt       # Corner radius tokens
└── Components.kt   # Standardized UI controls (Buttons, TextFields, Cards)
```

---

## 2. Theme Provider & Usage

Wrap screens or root components with `MonochromeTheme`:

```kotlin
MonochromeTheme(darkTheme = isSystemInDarkTheme()) {
    // Screen content
}
```

Access theme tokens directly using `MonochromeTheme`:
- **Colors**: `MonochromeTheme.colors`
- **Typography**: `MonochromeTheme.typography`
- **Spacing**: `MonochromeTheme.spacing`
- **Shapes**: `MonochromeTheme.shapes`

---

## 3. Spacing System (`Spacing.kt`)

Avoid hardcoding arbitrary DP spacing values. Always use `MonochromeTheme.spacing`:

| Token | Scale Value | Usage |
| :--- | :--- | :--- |
| `space1` | `4.dp` | Micro spacing, icon gaps |
| `space2` | `8.dp` | Small component gap, compact padding |
| `space3` | `12.dp` | List item gaps |
| `space4` | `16.dp` | Standard padding, row/column default gaps |
| `space5` | `20.dp` | Medium container padding |
| `space6` | `24.dp` | Card internal padding |
| `space8` | `32.dp` | Large section spacing |
| `space10` | `40.dp` | Major section gap |
| `space12` | `48.dp` | Touch target / hero padding |

Component padding tokens:
- `inputPadding`: `16.dp`
- `cardPadding`: `24.dp`
- `sectionSpacing`: `40.dp`

---

## 4. Color Palette (`Color.kt`)

Always use semantic colors from `MonochromeTheme.colors`:

- `background`: Main app background
- `surface`: Elevated container background
- `textPrimary`: Primary text
- `textSecondary`: Muted secondary text
- `primaryActionBg`: Action button fill
- `subtleBorder`: Card / Divider borders

**Rule**: Do NOT use hardcoded hex values (`Color(0xFF...)`) in UI code. Use `MonochromeTheme.colors`.

---

## 5. Reusable Components (`Components.kt`)

Before creating custom buttons, input fields, or cards, ALWAYS search and reuse existing components from `Components.kt`:

### Buttons (`MonochromeButton`)
Variants: `Primary`, `Secondary`, `Ghost`
Sizes: `Standard` (40dp), `Large` (48dp)

```kotlin
MonochromeButton(
    text = "Connect Robot",
    onClick = { viewModel.connect() },
    variant = MonochromeButtonVariant.Primary
)
```

### Text Input
Use standard monochrome text fields defined in `Components.kt`.

---

## 6. Touch Targets & Accessibility

1. Minimum touch target for interactive elements is `48.dp`.
2. Always provide `contentDescription` for non-decorative icons and images.
3. Ensure high contrast in both dark and light modes using semantic color tokens.
