package ru.madbrains.smartyard.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.madbrains.smartyard.R

val CustomFontFamily = FontFamily(
    Font(R.font.source_sans_family, FontWeight.Normal),
    Font(R.font.source_sans_pro_semi_bold, FontWeight.Bold)
)

val Typography = Typography(
    h2 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 60.sp
    ),
    h4 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        letterSpacing = 0.sp
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    subtitle1 = TextStyle(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        fontSize = 18.sp
    ),
    subtitle2 = TextStyle(
        fontSize = 6.5.sp,
        fontWeight = FontWeight.Bold,
//        fontFamily = CustomFontFamily,
    ),
    caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    overline = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal
    )
)