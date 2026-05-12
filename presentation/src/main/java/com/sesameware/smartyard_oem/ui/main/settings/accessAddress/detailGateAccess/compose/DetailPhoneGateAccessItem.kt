package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.sesameware.data.DataModule
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.formatPhoneWith
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.getContact

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun DetailPhoneGateAccessItem(
    phoneNumber: String,
    onSmsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.card_corner)))
            .background(colorResource(id = R.color.light_background))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val contact = remember(phoneNumber) { getContact(context, phoneNumber) }
        GlideImage(
            model = contact?.avatar,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            loading = placeholder(painterResource(R.drawable.ic_userpic)),
            failure = placeholder(painterResource(R.drawable.ic_userpic)),
        ) {
            it.circleCrop()
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = contact?.name ?: phoneNumber.formatPhoneWith(DataModule.phonePattern),
            color = colorResource(id = R.color.accent),
            fontSize = 13.sp,
            fontFamily = FontFamily(Font(R.font.source_sans_pro_regular)),
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        menuExpanded = false
                        onSmsClick()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.text_sms),
                        color = colorResource(R.color.accent),
                        fontFamily = FontFamily(Font(R.font.source_sans_pro_regular))
                    )
                }
                DropdownMenuItem(
                    onClick = {
                        menuExpanded = false
                        onDeleteClick()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.text_delete),
                        color = colorResource(R.color.accent),
                        fontFamily = FontFamily(Font(R.font.source_sans_pro_regular))
                    )
                }
            }
        }
    }
}