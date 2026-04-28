package com.pm.encrypter.utils

fun splitFileName(fullName: String): Pair<String, String> {
    val dotIndex = fullName.lastIndexOf(".")
    return if (dotIndex != -1) {
        val name = fullName.substring(0, dotIndex)
        val ext = fullName.substring(dotIndex + 1)
        Pair(name, ext)
    } else {
        Pair(fullName, "") // no extension
    }
}