package com.pm.encrypter.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

fun shareFile(uri: Uri, context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share file via"))
}