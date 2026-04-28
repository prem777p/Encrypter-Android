package com.pm.encrypter.utils

import java.io.InputStream

class ProgressInputStream(
    private val inputStream: InputStream,
    private val totalBytes: Long,
    private val onProgress: (Int) -> Unit
) : InputStream() {

    private var bytesRead: Long = 0

    override fun read(): Int {
        val data = inputStream.read()
        if (data != -1) {
            bytesRead++
            reportProgress()
        }
        return data
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = inputStream.read(buffer, offset, length)
        if (count > 0) {
            bytesRead += count
            reportProgress()
        }
        return count
    }

    private fun reportProgress() {
        if (totalBytes > 0) {
            val progress = ((bytesRead * 100) / totalBytes).toInt()
            onProgress(progress)
        }
    }

    override fun close() {
        inputStream.close()
    }
}