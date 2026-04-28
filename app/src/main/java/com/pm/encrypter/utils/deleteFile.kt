package com.pm.encrypter.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast

fun deleteFile(uri: Uri, context: Context) {
    try {
        context.contentResolver.delete(uri, null, null)
        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
    }
}