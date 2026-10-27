package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.smartyard_oem.R

@Composable
fun WideHeroScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    content: @Composable () -> Unit
) {
    val titleFontFamily = remember { FontFamily(Font(R.font.source_sans_pro_semi_bold)) }

    Box(modifier = modifier) {
        OverlapColumn(
            overlapDp = 48.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_top_wide),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            )

            val cornerSize = dimensionResource(R.dimen.surface_corner_radius)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = cornerSize, topEnd = cornerSize))
                    .background(colorResource(R.color.shaded_background))
            ) {
                content()
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = "Back",
                tint = colorResource(id = R.color.on_top_background),
                modifier = Modifier
                    .padding(start = 8.dp, top = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackClick
                    )
                    .padding(16.dp)
            )

            BasicText(
                text = title,
                style = TextStyle(
                    color = colorResource(id = R.color.on_top_background),
                    fontSize = 32.sp,
                    fontFamily = titleFontFamily
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 16.sp,
                    maxFontSize = 32.sp,
                    stepSize = 2.sp
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 8.dp)
            )

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = colorResource(id = R.color.on_filled),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "WideHeroScreen (2 & 2)")
@Composable
private fun WideHeroScreenPreview() {
    WideHeroScreen(
        title = "Очень длинный адрес доступа, который гарантированно займет две строки на экране",
        subtitle = "Очень длинный подзаголовок, который также аккуратно перенесется на две строки без обрезки",
        onBackClick = {}
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
            Text(text = "Контент экрана", color = Color.Gray)
        }
    }
}