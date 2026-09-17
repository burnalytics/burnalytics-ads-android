package com.burnalytics.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BurnalyticsRoundControl(
    text: String,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xB3000000), CircleShape)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
internal fun BurnalyticsSkipControl(
    secondsRemaining: Int,
    onClick: () -> Unit,
) {
    val enabled = secondsRemaining <= 0
    val text = if (enabled) "Skip" else secondsRemaining.toString()
    val description = if (enabled) "Skip ad" else "Skip available in $secondsRemaining seconds"
    Box(
        modifier = Modifier
            .height(36.dp)
            .background(Color(0xB3000000), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick)
            .padding(horizontal = if (enabled) 13.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
internal fun BurnalyticsLearnMoreButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFF9800), RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Learn more", onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "Learn more",
            style = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

