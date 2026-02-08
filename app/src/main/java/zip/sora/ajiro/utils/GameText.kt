package zip.sora.ajiro.utils

import java.io.File
import java.io.InputStream

class GameText(val id: String, var text: String)

fun loadGameText(file: File): List<GameText> =
    loadGameText(file.inputStream())

fun loadGameText(inputStream: InputStream): List<GameText> =
    inputStream.reader().use { reader ->
        reader.readText().split('|').map { it ->
            it.split('^', limit = 2).let {
                GameText(it[0], it[1])
            }
        }
    }

fun saveGameText(file: File, texts: List<GameText>) {
    file.writeText(texts.joinToString("|") { "${it.id}^${it.text}" })
}