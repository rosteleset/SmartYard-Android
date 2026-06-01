package com.sesameware.smartyard_oem.ui.custom_web_view

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag

class NfcManager(
    private val adapter: NfcAdapter
) {
    fun enableReader(
        activity: Activity,
        onTag: (Tag) -> Unit
    ) {
        adapter.enableReaderMode(
            activity,
            { tag -> onTag(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    fun disableReader(activity: Activity) {
        adapter.disableReaderMode(activity)
    }
}
