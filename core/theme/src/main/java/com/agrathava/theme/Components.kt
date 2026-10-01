package com.agrathava.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MonochromeButtonVariant {
    Primary,
    Secondary,
    Ghost
}

enum class MonochromeButtonSize(val height: Dp, val horizontalPadding: Dp) {
    Standard(40.dp, 16.dp),
    Large(48.dp, 20.dp)
}

/**
 * Monochrome Design System Primary / Secondary / Ghost Button
 */
@Composable
fun MonochromeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MonochromeButtonVariant = MonochromeButtonVariant.Primary,
    size: MonochromeButtonSize = MonochromeButtonSize.Standard,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    text: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes
    val typography = MonochromeTheme.typography

    val containerColor = when (variant) {
        MonochromeButtonVariant.Primary -> if (enabled) colors.primaryActionBg else colors.disabledSurface
        MonochromeButtonVariant.Secondary -> Color.Transparent
        MonochromeButtonVariant.Ghost -> Color.Transparent
    }

    val contentColor = when (variant) {
        MonochromeButtonVariant.Primary -> if (enabled) colors.primaryActionFg else colors.disabledText
        MonochromeButtonVariant.Secondary -> if (enabled) colors.secondaryActionText else colors.disabledText
        MonochromeButtonVariant.Ghost -> if (enabled) colors.primaryText else colors.disabledText
    }

    val border = when (variant) {
        MonochromeButtonVariant.Secondary -> BorderStroke(
            1.dp,
            if (enabled) colors.secondaryActionBorder else colors.disabledSurface
        )
        else -> null
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(size.height),
        enabled = enabled,
        shape = shapes.buttons,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colors.disabledSurface,
            disabledContentColor = colors.disabledText
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = size.horizontalPadding, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                if (text != null || content != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            if (text != null) {
                Text(
                    text = text,
                    style = typography.label,
                    color = contentColor
                )
            } else if (content != null) {
                content()
            }
        }
    }
}

/**
 * Monochrome Design System Surface Card
 */
@Composable
fun MonochromeCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MonochromeTheme.colors.surface,
    borderColor: Color = MonochromeTheme.colors.hoverSurface,
    padding: Dp = MonochromeTheme.spacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shapes = MonochromeTheme.shapes
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shapes.cards)
            .border(1.dp, borderColor, shapes.cards)
            .then(clickableModifier),
        color = backgroundColor,
        shape = shapes.cards
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content
        )
    }
}

/**
 * Monochrome Design System Input Field
 */
@Composable
fun MonochromeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes
    val typography = MonochromeTheme.typography

    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isError -> colors.strongBorder
        isFocused -> colors.focusBorder
        else -> colors.defaultBorder
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = typography.label,
                color = colors.secondaryText,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(shapes.inputs)
                .background(if (enabled) colors.surface else colors.disabledSurface)
                .border(1.dp, borderColor, shapes.inputs)
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = colors.mutedText,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = typography.bodySmall,
                            color = colors.placeholderText()
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        singleLine = singleLine,
                        textStyle = typography.bodySmall.copy(color = if (enabled) colors.primaryText else colors.disabledText),
                        cursorBrush = SolidColor(colors.primaryText),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }

        if (helperText != null) {
            Text(
                text = helperText,
                style = typography.caption,
                color = if (isError) colors.primaryText else colors.mutedText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MonochromeColors.placeholderText(): Color {
    return if (isDark) Gray500 else Gray400
}

enum class StatusLevel {
    Default,
    Active,
    Inactive,
    Critical
}

/**
 * Monochrome Design System Status Pill Component
 */
@Composable
fun MonochromeStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    level: StatusLevel = StatusLevel.Default
) {
    val colors = MonochromeTheme.colors
    val shapes = MonochromeTheme.shapes
    val typography = MonochromeTheme.typography

    val (bg, fg, border) = when (level) {
        StatusLevel.Default -> Triple(colors.interactiveSurface, colors.secondaryText, colors.defaultBorder)
        StatusLevel.Active -> Triple(colors.primaryActionBg, colors.primaryActionFg, colors.primaryActionBg)
        StatusLevel.Inactive -> Triple(colors.disabledSurface, colors.mutedText, colors.subtleBorder)
        StatusLevel.Critical -> Triple(colors.surface, colors.primaryText, colors.strongBorder)
    }

    Box(
        modifier = modifier
            .clip(shapes.pills)
            .background(bg)
            .border(1.dp, border, shapes.pills)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = typography.caption,
                color = fg
            )
        }
    }
}
