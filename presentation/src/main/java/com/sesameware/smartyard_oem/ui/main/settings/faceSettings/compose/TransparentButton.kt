package com.sesameware.smartyard_oem.ui.main.settings.faceSettings.compose

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose.ButtonTypography

@Composable
fun TransparentButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    val textColor = colorResource(R.color.no_accent)
    val backgroundColor = Color.Transparent
    MaterialTheme(
        typography = ButtonTypography
    ) {
        TextButton(
            enabled = enabled,
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(57.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = backgroundColor,
                contentColor = textColor
            )
        ) {
            Text(text = title)
        }
    }
}

@Preview
@Composable
fun TransparentButtonPreview() {
    val context = LocalContext.current
    TransparentButton(
        title = "Title",
        onClick = { Toast.makeText(context, "Add Clicked", Toast.LENGTH_SHORT).show() }
    )
}
