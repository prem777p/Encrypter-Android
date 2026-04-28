package com.pm.encrypter.utils

import android.content.Context

fun Context.getThemeColor(attr: Int): Int {
    val typedValue = android.util.TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}