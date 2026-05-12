package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.smartyard_oem.R

val ButtonTypography = Typography().copy(
    button = TextStyle(
        fontFamily = FontFamily(Font(R.font.source_sans_pro_regular)),
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )
)

@Composable
fun BrandedOutlinedButton(
    title: String,
    onClick: () -> Unit
) {
    val brandColor = colorResource(R.color.brand)
    MaterialTheme(
        typography = ButtonTypography
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .height(57.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, brandColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = brandColor)
        ) {
            Text(text = title)
        }
    }
}

@Preview
@Composable
fun BrandedOutlinedButtonPreview() {
    val context = LocalContext.current
    BrandedOutlinedButton(
        title = "Title",
        onClick = { Toast.makeText(context, "Add Clicked", Toast.LENGTH_SHORT).show() }
    )
}