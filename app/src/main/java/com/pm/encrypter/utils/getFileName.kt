package com.pm.encrypter.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun getFileName(uri: Uri, context: Context): String {
    var name = "file"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && index != -1) {
            name = it.getString(index)
        }
    }
    return name
}