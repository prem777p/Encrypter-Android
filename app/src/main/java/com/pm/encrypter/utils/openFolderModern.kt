package com.pm.encrypter.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.core.net.toUri
import java.io.File

fun openFolderModern(context: Context, folderType: String) {

    val (uri, type) = pathAndType(folderType)

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, type)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open folder", Toast.LENGTH_SHORT).show()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        context.startActivity(intent)
    }
}

fun pathAndType(folderType: String): Pair<Uri, String> {
    val path = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "Encrypter/$folderType"
    )

    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) Pair(Uri.fromFile(path), "*/*")
    else Pair(
        "content://com.android.externalstorage.documents/document/primary:Download/Encrypter/$folderType".toUri(),
        DocumentsContract.Document.MIME_TYPE_DIR
    )
}
