package com.agrathava.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.agrathava.theme.MonochromeTheme

enum class SidebarNavItem(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Default.Home),
    NAVIGATION("Navigation", Icons.Default.Place),
    MANUAL_CONTROL("Manual Control", Icons.AutoMirrored.Filled.AltRoute),
    ROBOT_STATUS("Robot Status", Icons.Default.SmartToy),
    MAPS("Maps", Icons.Default.Map),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun Sidebar(
    modifier: Modifier = Modifier,
    selectedItem: SidebarNavItem = SidebarNavItem.HOME,
    onItemSelected: (SidebarNavItem) -> Unit = {},
    isConnected: Boolean = true,
    connectionAddress: String = "192.168.1.108:8080"
) {
    val colors = MonochromeTheme.colors
    val typography = MonochromeTheme.typography
    val spacing = MonochromeTheme.spacing
    val shapes = MonochromeTheme.shapes

    Surface(
        modifier = modifier
            .width(330.dp)
            .fillMaxHeight(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = spacing.space6 * 1.5f, horizontal = spacing.space4 * 1.5f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: App Title & Navigation Items
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space6 * 1.5f)
            ) {
                // Title: Robot Controller
                Text(
                    text = "Robot Controller",
                    style = typography.h4.copy(
                        fontSize = typography.h4.fontSize * 1.5f,
                        lineHeight = typography.h4.lineHeight * 1.5f
                    ),
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = spacing.space2 * 1.5f,
                        vertical = spacing.space1 * 1.5f
                    )
                )

                // Navigation Items List
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.space2 * 1.5f)
                ) {
                    SidebarNavItem.entries.forEach { item ->
                        val isSelected = selectedItem == item
                        val bg = if (isSelected) colors.interactiveSurface else Color.Transparent
                        val fg = if (isSelected) colors.primaryText else colors.secondaryText

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp)
                                .clip(shapes.buttons)
                                .background(bg)
                                .clickable { onItemSelected(item) }
                                .padding(horizontal = spacing.space3 * 1.5f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.space3 * 1.5f)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = fg,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = item.title,
                                    style = typography.bodySmall.copy(
                                        fontSize = typography.bodySmall.fontSize * 1.5f,
                                        lineHeight = typography.bodySmall.lineHeight * 1.5f
                                    ),
                                    color = fg,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Section: Connection Status
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    color = colors.subtleBorder,
                    thickness = 1.5.dp
                )

                Spacer(modifier = Modifier.height(spacing.space4 * 1.5f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3 * 1.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space2 * 1.5f)
                ) {
                    // Green Connection Dot
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF22C55E) else colors.disabledText)
                    )

                    Column {
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            style = typography.label.copy(
                                fontSize = typography.label.fontSize * 1.5f,
                                lineHeight = typography.label.lineHeight * 1.5f
                            ),
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = connectionAddress,
                            style = typography.monoCaption.copy(
                                fontSize = typography.monoCaption.fontSize * 1.5f,
                                lineHeight = typography.monoCaption.lineHeight * 1.5f
                            ),
                            color = colors.mutedText
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Sidebar Preview", showBackground = true)
@Composable
fun SidebarPreview() {
    MonochromeTheme(darkTheme = false) {
        Sidebar()
    }
}
