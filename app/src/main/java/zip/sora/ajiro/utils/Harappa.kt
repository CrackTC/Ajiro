package zip.sora.ajiro.utils

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import zip.sora.ajiro.HARAPPA_OWNER
import zip.sora.ajiro.HARAPPA_REPO
import zip.sora.ajiro.SHA_FILENAME
import java.io.File

private val client = HttpClient(Android)

suspend fun downloadFile(url: String, outputFile: File, onProgress: (Long, Long) -> Unit) {
    val response = client.get(url) {
        onDownload { bytesSentTotal, contentLength ->
            if (contentLength != null && contentLength > 0) {
                onProgress(bytesSentTotal, contentLength)
            }
        }
    }
    response.bodyAsChannel().copyAndClose(outputFile.writeChannel())
}

suspend fun getLatestCommitSha(): String? {
    val url = "https://api.github.com/repos/$HARAPPA_OWNER/$HARAPPA_REPO/commits/main"

    try {
        val response = client.get(url) {
            header(HttpHeaders.Accept, "application/vnd.github.VERSION.sha")
        }
        return if (response.status == HttpStatusCode.OK) {
            response.bodyAsText().trim()
        } else {
            null
        }
    } catch (_: Exception) {
        return null
    }
}

fun getLocalSha(fileDir: String): String? {
    val shaFile = File(fileDir, SHA_FILENAME)
    return if (shaFile.exists()) {
        shaFile.readText().trim()
    } else {
        null
    }
}

fun setLocalSha(fileDir: String, sha: String) {
    val shaFile = File(fileDir, SHA_FILENAME)
    shaFile.writeText(sha)
}