package com.pm.encrypter.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.pm.encrypter.model.FileItem

fun getFilesFromFolder(subfolder: String, context: Context): List<FileItem> {

    val list = mutableListOf<FileItem>()

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_MODIFIED
    )

    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("%Download/Encrypter/${subfolder} File%")


    val cursor = context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
    )

    cursor?.use {
        val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

        while (it.moveToNext()) {
            val id = it.getLong(idIndex)
            val name = it.getString(nameIndex)
            val size = it.getLong(sizeIndex)
            val date = it.getLong(dateIndex)

            val uri = ContentUris.withAppendedId(
                MediaStore.Files.getContentUri("external"),
                id
            )

            if (isUriValid(uri, context)) {
                list.add(FileItem(uri, name, size, date))
            }
        }
    }

    return list
}

fun isUriValid(uri: Uri, context: Context): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.close()
        true
    } catch (e: Exception) {
        false
    }
}