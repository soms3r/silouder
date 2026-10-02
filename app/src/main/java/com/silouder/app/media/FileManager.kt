package com.silouder.app.media

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import com.silouder.app.crypto.CryptoEngine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * File & Media Manager for Telegram/Signal-grade Offline & P2P Media Sharing.
 * Supports:
 * 1. Safe local storage of photos, voice notes, and documents.
 * 2. AES-256-GCM chunked file encryption & decryption before transmission.
 * 3. Voice note recording via Android MediaRecorder.
 * 4. Human-readable file size and MIME resolution.
 */
class FileManager(private val context: Context) {

    private val attachmentsDir: File = File(context.filesDir, "attachments").apply {
        if (!exists()) mkdirs()
    }

    private var activeRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    fun getAttachmentsDir(): File = attachmentsDir

    /**
     * Copies a user-selected URI into the app's internal attachments storage.
     */
    fun copyUriToInternalStorage(uri: Uri, preferredName: String? = null): File? {
        return try {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                .ifEmpty { "bin" }
            val name = preferredName ?: "file_${System.currentTimeMillis()}.$extension"
            val destFile = File(attachmentsDir, "${UUID.randomUUID()}_$name")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Starts recording a voice note into an AAC/M4A file.
     */
    fun startVoiceRecording(): File? {
        return try {
            stopVoiceRecording() // Ensure any existing recorder is released

            val voiceFile = File(attachmentsDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = voiceFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(voiceFile.absolutePath)
                prepare()
                start()
            }
            activeRecorder = recorder
            voiceFile
        } catch (e: Exception) {
            e.printStackTrace()
            activeRecorder?.release()
            activeRecorder = null
            null
        }
    }

    /**
     * Stops the active voice note recording and returns the recorded file.
     */
    fun stopVoiceRecording(): File? {
        val file = currentRecordingFile
        try {
            activeRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            activeRecorder = null
            currentRecordingFile = null
        }
        return file
    }

    /**
     * Encrypts a file using AES-256-GCM for direct P2P transmission.
     */
    fun encryptFile(inputFile: File, keyBytes: ByteArray): Pair<File, ByteArray> {
        val outputFile = File(attachmentsDir, "${inputFile.name}.enc")
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)

        val normalizedKey = if (keyBytes.size == 32) keyBytes else {
            MessageDigest.getInstance("SHA-256").digest(keyBytes)
        }

        val secretKey = SecretKeySpec(normalizedKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                CipherOutputStream(fos, cipher).use { cos ->
                    fis.copyTo(cos)
                }
            }
        }

        return Pair(outputFile, iv)
    }

    /**
     * Decrypts an incoming AES-256-GCM encrypted file.
     */
    fun decryptFile(encryptedFile: File, outputFile: File, keyBytes: ByteArray, iv: ByteArray): File {
        val normalizedKey = if (keyBytes.size == 32) keyBytes else {
            MessageDigest.getInstance("SHA-256").digest(keyBytes)
        }

        val secretKey = SecretKeySpec(normalizedKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        FileInputStream(encryptedFile).use { fis ->
            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(outputFile).use { fos ->
                    cis.copyTo(fos)
                }
            }
        }
        return outputFile
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
