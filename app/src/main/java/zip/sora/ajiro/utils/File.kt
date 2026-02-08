package zip.sora.ajiro.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import zip.sora.ajiro.DOWNLOAD_SUBDIR_NAME
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipFile

fun Context.copyFileToDownloadFolder(file: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBDIR_NAME"
            )
        }

        val uri: Uri? =
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val inputStream = FileInputStream(file)
            val outputStream = contentResolver.openOutputStream(it)

            outputStream?.use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }
        }
    } else {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOAD_SUBDIR_NAME
        )
        if (!folder.exists()) folder.mkdirs()
        val destFile = File(folder, file.name)
        FileInputStream(file).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}

fun File.unzipTo(targetDirectory: File, onProgress: (Int, Int, String) -> Unit) {
    ZipFile(this).use { zip ->
        val rootEntryName = zip.entries().nextElement().name.split("/").firstOrNull() + "/"
        val size = zip.size()
        zip.entries().asSequence().forEachIndexed { index, entry ->
            if (entry.name.startsWith(rootEntryName)) {
                val outFile = File(targetDirectory, entry.name.removePrefix(rootEntryName))
                onProgress(index, size, outFile.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}