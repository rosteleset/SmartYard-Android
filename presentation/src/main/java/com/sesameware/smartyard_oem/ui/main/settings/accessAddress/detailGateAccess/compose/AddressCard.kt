package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.detailGateAccess.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import com.sesameware.smartyard_oem.R

@Composable
fun AddressCard(
    address: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = address,
        color = colorResource(id = R.color.accent),
        fontSize = 18.sp,
        fontFamily = FontFamily(Font(R.font.source_sans_pro_regular)),
        textAlign = TextAlign.Start,
        modifier = modifier
            .fillMaxWidth()
             .background(
                 color = colorResource(R.color.light_background),
                 shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner))
             )
            .padding(24.dp)
    )
}

@Preview
@Composable
fun AddressCardPreview() {
    val address = stringResource(R.string.address_sample)
    AddressCard(address = address)
}