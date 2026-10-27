package com.sesameware.smartyard_oem.ui.main.address.cctv_video.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ContentScale.Companion
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.smartyard_oem.R

@Composable
fun CameraItem(
    camera: CCTVData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = colorResource(id = R.color.light_background),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .clickable { onClick() }
        ) {
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(start = 24.dp, top = 16.dp, bottom = 16.dp),
                textAlign = TextAlign.Start,
                fontSize = 14.sp,
                color = colorResource(id = R.color.accent),
                text = camera.name,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                )
            Image(
                painter = painterResource(id = R.drawable.ic_video_camera),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(colorResource(id = R.color.no_accent))
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun CameraItemPreview() {
    CameraItem(
        camera = CCTVData(
            id = 100500,
            name = "Тестовая камера",
            latitude = null,
            longitude = null,
            token = "",
            url = "",
            _hasSound = true
        ),
        onClick = {}
    )
}