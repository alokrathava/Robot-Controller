package com.agrathava.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agrathava.theme.MonochromeButton
import com.agrathava.theme.MonochromeButtonSize
import com.agrathava.theme.MonochromeButtonVariant
import com.agrathava.theme.MonochromeCard
import com.agrathava.theme.MonochromeStatusPill
import com.agrathava.theme.MonochromeTheme
import com.agrathava.theme.StatusLevel

enum class SideMenuItem(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    DASHBOARD("Dashboard", Icons.Default.SmartToy, "Main Robot Controls"),
    NAVIGATION("Map & Navigation", Icons.Default.Map, "Autonomous Navigation"),
    CONNECTION("Network Setup", Icons.Default.Wifi, "IP & Wi-Fi Settings"),
    BATTERY("System Status", Icons.Default.BatteryChargingFull, "Battery & Health"),
    SETTINGS("Settings", Icons.Default.Settings, "Robot Preferences")
}

@Composable
fun SideMenu(
    selectedItem: SideMenuItem,
    onItemSelected: (SideMenuItem) -> Unit,
    modifier: Modifier = Modifier,
    robotName: String = "ROBOT-01",
    connectionStatusText: String = "Connected",
    robotImagePainter: Painter = painterResource(id = R.drawable.robot_splash),
    onDisconnectClick: () -> Unit = {},
    isInitiallyCollapsed: Boolean = false
) {
    var isCollapsed by remember { mutableStateOf(isInitiallyCollapsed) }
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes

    val menuWidth = if (isCollapsed) 72.dp else 240.dp

    Surface(
        modifier = modifier
            .width(menuWidth)
            .fillMaxHeight()
            .animateContentSize()
            .border(1.dp, colors.subtleBorder, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
        color = colors.surface,
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = spacing.space4, horizontal = spacing.space2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Robot Header & Collapse Toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Toggle Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space2),
                    horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isCollapsed) {
                        Text(
                            text = "NAVIGATION",
                            style = typography.caption,
                            color = colors.mutedText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.interactiveSurface)
                            .clickable { isCollapsed = !isCollapsed },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = if (isCollapsed) "Expand Menu" else "Collapse Menu",
                            tint = colors.primaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Robot Profile / Avatar Card
                if (!isCollapsed) {
                    MonochromeCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.interactiveSurface,
                        borderColor = colors.defaultBorder
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(shapes.smallControls)
                                    .background(colors.hoverSurface)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = robotImagePainter,
                                    contentDescription = "Robot Avatar",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column {
                                Text(
                                    text = robotName,
                                    style = typography.label,
                                    color = colors.primaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                MonochromeStatusPill(
                                    text = connectionStatusText,
                                    level = StatusLevel.Active
                                )
                            }
                        }
                    }
                } else {
                    // Compact Avatar for Collapsed State
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(shapes.smallControls)
                            .background(colors.interactiveSurface)
                            .border(1.dp, colors.defaultBorder, shapes.smallControls)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = robotImagePainter,
                            contentDescription = "Robot Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.space2))

                // Navigation Item List
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SideMenuItem.entries.forEach { item ->
                        val isSelected = selectedItem == item
                        SideMenuItemRow(
                            item = item,
                            isSelected = isSelected,
                            isCollapsed = isCollapsed,
                            onClick = { onItemSelected(item) }
                        )
                    }
                }
            }

            // Bottom Section: Quick Disconnect Action Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isCollapsed) {
                    MonochromeButton(
                        onClick = onDisconnectClick,
                        variant = MonochromeButtonVariant.Secondary,
                        size = MonochromeButtonSize.Standard,
                        icon = Icons.Default.PowerSettingsNew,
                        text = "Disconnect",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(shapes.buttons)
                            .background(colors.hoverSurface)
                            .border(1.dp, colors.strongBorder, shapes.buttons)
                            .clickable(onClick = onDisconnectClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Disconnect Robot",
                            tint = colors.primaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SideMenuItemRow(
    item: SideMenuItem,
    isSelected: Boolean,
    isCollapsed: Boolean,
    onClick: () -> Unit
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes

    val bg = if (isSelected) colors.primaryActionBg else Color.Transparent
    val fg = if (isSelected) colors.primaryActionFg else colors.secondaryText
    val border = if (isSelected) colors.primaryActionBg else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shapes.buttons)
            .background(bg)
            .border(1.dp, border, shapes.buttons)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.space3),
        contentAlignment = if (isCollapsed) Alignment.Center else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.spacedBy(spacing.space3)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )

            if (!isCollapsed) {
                Column {
                    Text(
                        text = item.title,
                        style = typography.label,
                        color = fg,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(name = "SideMenu Expanded", showBackground = true)
@Composable
fun SideMenuExpandedPreview() {
    MonochromeTheme(darkTheme = false) {
        SideMenu(
            selectedItem = SideMenuItem.DASHBOARD,
            onItemSelected = {},
            isInitiallyCollapsed = false
        )
    }
}

@Preview(name = "SideMenu Collapsed", showBackground = true)
@Composable
fun SideMenuCollapsedPreview() {
    MonochromeTheme(darkTheme = false) {
        SideMenu(
            selectedItem = SideMenuItem.DASHBOARD,
            onItemSelected = {},
            isInitiallyCollapsed = true
        )
    }
}
