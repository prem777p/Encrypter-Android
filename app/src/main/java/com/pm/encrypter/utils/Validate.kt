package com.pm.encrypter.utils

import com.pm.encrypter.R

class Validate {
    companion object {

        fun isPasswordValid(password: String): Boolean {
            val passwordRegex = Regex(
                "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9])(?=\\S+$).{8,}$"
            )
            return passwordRegex.matches(password)
        }

        fun getPasswordErrorMessages(password: String): Int {
            if (password.isEmpty()) return R.string.empty_password
            if (password.length < 8) return R.string.invalid_password_length
            if (!password.any { it.isLowerCase() }) return R.string.invalid_password_lower
            if (!password.any { it.isUpperCase() }) return R.string.invalid_password_upper
            if (!password.any { it.isDigit() }) return R.string.invalid_password_digit
            if (!password.any { !it.isLetterOrDigit() }) return R.string.invalid_password_special_char
            if (password.contains(" ")) return R.string.invalid_password_no_space
            return R.string.invalid_password
        }

    }
}