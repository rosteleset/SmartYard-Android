package com.sesameware.smartyard_oem.ui.main.settings.trackedEvents.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesameware.smartyard_oem.R
import com.sesameware.domain.model.response.TrackedEvent

@Composable
fun TrackedEventItem(
    event: TrackedEvent,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = colorResource(id = R.color.light_background),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getEventName(event.eventType) + if (event.eventDetail?.isNotEmpty() == true) ": " + event.eventDetail else "",
                    fontSize = 16.sp,
                    color = Color.Black
                )
                if (event.comments?.isNotEmpty() == true) {
                    Text(
                        text = event.comments ?: "",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}
