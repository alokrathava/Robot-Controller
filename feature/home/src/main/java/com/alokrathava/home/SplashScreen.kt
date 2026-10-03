package com.alokrathava.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alokrathava.theme.MonochromeButton
import com.alokrathava.theme.MonochromeButtonSize
import com.alokrathava.theme.MonochromeButtonVariant
import com.alokrathava.theme.MonochromeTheme

@Composable
fun SplashScreen(
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectMode: ((String) -> Unit)? = null
) {
    val isInspection = LocalInspectionMode.current
    var isVisible by remember { mutableStateOf(isInspection) }

    LaunchedEffect(Unit) {
        if (!isInspection) {
            isVisible = true
        }
    }

    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(spacing.space4),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(800)) + slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        ) {
            SplashScreenContent(
                onConnectClick = onConnectClick,
                onSelectMode = onSelectMode
            )
        }
    }
}

@Composable
fun SplashScreenContent(
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectMode: ((String) -> Unit)? = null
) {
    val colors = MonochromeTheme.colors
    val spacing = MonochromeTheme.spacing
    val typography = MonochromeTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title & Subtitle
        Text(
            text = "ROBOT CONTROLLER",
            style = typography.h2,
            color = colors.primaryText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(spacing.space1))

        Text(
            text = "System Ready • Autonomous Companion Connected",
            style = typography.bodySmall,
            color = colors.secondaryText,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.space4))

        // Robot Vector Image Asset from res/drawable/robot_splash.xml
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.wrapContentSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.robot_splash),
                contentDescription = "Robot Controller Illustration",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(220.dp)
                    .height(320.dp)
            )
        }

        Spacer(modifier = Modifier.height(spacing.space4))

        // Action CTA Button
        MonochromeButton(
            onClick = onConnectClick,
            variant = MonochromeButtonVariant.Primary,
            size = MonochromeButtonSize.Large,
            text = "Connect to the Robot",
            icon = Icons.Default.PowerSettingsNew,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
        )
    }
}

@Preview(name = "1920x1080 High DPI Light Mode", showBackground = true, device = "spec:width=1920px,height=1080px,dpi=240")
@Composable
fun SplashScreenPreview() {
    MonochromeTheme(darkTheme = false) {
        SplashScreen(
            onConnectClick = {}
        )
    }
}
