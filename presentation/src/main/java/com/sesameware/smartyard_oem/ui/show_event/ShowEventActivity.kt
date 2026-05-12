package com.sesameware.smartyard_oem.ui.show_event

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.sesameware.smartyard_oem.R

class ShowEventActivity : ComponentActivity() {
    private var title: String = ""
    private var body: String = ""
    private var date: String = ""
    private var imageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        title = intent.getStringExtra(EVENT_TITLE) ?: title
        body = intent.getStringExtra(EVENT_BODY) ?: body
        date = intent.getStringExtra(EVENT_DATE) ?: date
        imageUrl = intent.getStringExtra(EVENT_IMAGE_URL) ?: imageUrl

        setContent {
            EventDialog {
                finish()
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun EventDialog(onDismissRequest: () -> Unit) {
        Dialog(
            onDismissRequest = { onDismissRequest() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp, 8.dp),
                    )
                    Text(
                        text = body,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .padding(16.dp, 8.dp)
                            .fillMaxWidth(),
                    )
                    Text(
                        text = date,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .padding(16.dp, 8.dp)
                            .fillMaxWidth()
                    )
                    GlideImage(
                        model = imageUrl,
                        contentDescription = "",
                        failure = placeholder(R.drawable.ic_no_photography_24),
                        modifier = Modifier.padding(16.dp, 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(
                            onClick = { onDismissRequest() },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text(stringResource(R.string.dialog_close))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EVENT_TITLE = "EVENT_TITLE"
        const val EVENT_BODY = "EVENT_BODY"
        const val EVENT_DATE = "EVENT_DATE"
        const val EVENT_IMAGE_URL = "EVENT_IMAGE_URL"
    }
}
