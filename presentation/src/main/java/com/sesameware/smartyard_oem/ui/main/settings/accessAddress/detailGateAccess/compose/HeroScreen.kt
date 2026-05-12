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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.smartyard_oem.R

@Composable
fun HeroScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var titleLines by remember { mutableIntStateOf(1) }

    Box(modifier = modifier) {
        OverlapColumn(
            overlapDp = if (titleLines < 2) 24.dp else 48.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(
                    id = if (titleLines > 1) R.drawable.background_top_wide
                    else R.drawable.background_top_narrow
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
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
                        onClick = { onBackClick() })
                    .padding(16.dp)
            )

            Text(
                text = title,
                color = colorResource(id = R.color.on_top_background),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.source_sans_pro_semi_bold)),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    titleLines = textLayoutResult.lineCount
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "1. Короткий заголовок (1 строка)")
@Composable
private fun HeroScreenSingleLinePreview() {
    HeroScreen(
        title = "Адрес доступа",
        onBackClick = {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "Здесь будет контент (карточки, скролл)",
                color = Color.Gray,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.source_sans_pro_regular))
            )
        }
    }
}

@Preview(showBackground = true, name = "2. Длинный заголовок (2+ строки)")
@Composable
private fun HeroScreenMultiLinePreview() {
    HeroScreen(
        title = "Очень длинный адрес, который точно не поместится в одну строку и перенесется",
        onBackClick = {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "Здесь будет контент (карточки, скролл)",
                color = Color.Gray,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.source_sans_pro_regular))
            )
        }
    }
}