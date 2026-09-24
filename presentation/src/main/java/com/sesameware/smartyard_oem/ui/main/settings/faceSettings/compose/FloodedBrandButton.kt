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
fun FloodedBrandButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    FloodedButton(
        title = title,
        onClick = onClick,
        normalColor = colorResource(R.color.brand),
        pressedColor = colorResource(R.color.brand_pressed),
        modifier = modifier,
        enabled = enabled
    )
}

@Preview
@Composable
fun FloodedBrandButtonPreview() {
    val context = LocalContext.current
    FloodedBrandButton(
        title = "Title",
        onClick = { Toast.makeText(context, "Add Clicked", Toast.LENGTH_SHORT).show() }
    )
}
