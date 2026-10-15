package com.alokrathava.sdk.internal.serialization

import java.util.zip.Inflater

fun decompressZlib(compressedBytes: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(compressedBytes)
    
    // We estimate initial size as large as we can reasonably afford or just use a small buffer.
    val buffer = ByteArray(1024 * 16)
    var result = ByteArray(0)
    
    try {
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) {
                break
            }
            if (count > 0) {
                val newResult = ByteArray(result.size + count)
                System.arraycopy(result, 0, newResult, 0, result.size)
                System.arraycopy(buffer, 0, newResult, result.size, count)
                result = newResult
            }
        }
    } catch (e: Exception) {
        // Handle decompression error
    } finally {
        inflater.end()
    }
    
    return result
}
