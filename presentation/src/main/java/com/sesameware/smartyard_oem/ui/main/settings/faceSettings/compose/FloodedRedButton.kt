package com.sesameware.smartyard_oem.ui.main.settings.faceSettings.compose

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.sesameware.smartyard_oem.R

@Composable
fun FloodedRedButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    FloodedButton(
        title = title,
        onClick = onClick,
        normalColor = colorResource(R.color.negative),
        pressedColor = colorResource(R.color.negative_pressed),
        modifier = modifier,
        enabled = enabled
    )
}

@Preview
@Composable
fun FloodedRedButtonPreview() {
    val context = LocalContext.current
    FloodedRedButton(
        title = "Title",
        onClick = { Toast.makeText(context, "Add Clicked", Toast.LENGTH_SHORT).show() }
    )
}
