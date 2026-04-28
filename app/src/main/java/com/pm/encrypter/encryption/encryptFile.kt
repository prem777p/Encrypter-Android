package com.pm.encrypter.encryption

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream

suspend fun encryptFileWithPassword(
    originalFileStream: InputStream,
    encryptedFileOutputStream: OutputStream,
    password: String
) {
    withContext(Dispatchers.IO) {
        // 1. Generate Salt and Derive Key
        val salt = generateSalt()
        val key = deriveKeyFromPassword(password, salt)

        // 2. Initialize Cipher
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        // 3. Write Salt Length and Salt
        encryptedFileOutputStream.write(salt.size)
        encryptedFileOutputStream.write(salt)

        // 4. Write IV Length and IV
        encryptedFileOutputStream.write(iv.size)
        encryptedFileOutputStream.write(iv)

        // 5. Encrypt the file data
        CipherOutputStream(encryptedFileOutputStream, cipher).use { cipherOut ->
            originalFileStream.use { input ->
                input.copyTo(cipherOut, bufferSize = 8192)
            }
        }
    }
}