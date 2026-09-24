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
fun FloodedGreenButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    FloodedButton(
        title = title,
        onClick = onClick,
        normalColor = colorResource(R.color.positive),
        pressedColor = colorResource(R.color.positive_pressed),
        modifier = modifier,
        enabled = enabled
    )
}

@Preview
@Composable
fun FloodedGreenButtonPreview() {
    val context = LocalContext.current
    FloodedGreenButton(
        title = "Title",
        onClick = { Toast.makeText(context, "Add Clicked", Toast.LENGTH_SHORT).show() }
    )
}
