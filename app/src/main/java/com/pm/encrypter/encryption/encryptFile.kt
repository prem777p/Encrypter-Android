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
        val salt = generateSalt()
        val key = deriveKeyFromPassword(password, salt)

        // CHANGE: Use CBC instead of GCM
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key) // This generates a random IV
        val iv = cipher.iv

        encryptedFileOutputStream.write(salt.size)
        encryptedFileOutputStream.write(salt)
        encryptedFileOutputStream.write(iv.size)
        encryptedFileOutputStream.write(iv)

        // This now streams properly without buffering the whole file
        CipherOutputStream(encryptedFileOutputStream, cipher).use { cipherOut ->
            originalFileStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                var bytes = input.read(buffer)
                while (bytes != -1) {
                    cipherOut.write(buffer, 0, bytes)
                    bytes = input.read(buffer)
                }
            }
        }
    }
}