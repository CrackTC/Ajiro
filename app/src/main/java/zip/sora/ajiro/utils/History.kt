package zip.sora.ajiro.utils

import android.content.Context
import zip.sora.ajiro.HISTORY_FILENAME
import java.io.File

private var history: MutableList<String>? = null

private fun Context.loadHistory() {
    val fileDir = getExternalFilesDir(null)!!
    val historyFile = File(fileDir, HISTORY_FILENAME)
    history = if (historyFile.exists()) {
        historyFile.readText().split('\n').filter { it.isNotEmpty() }.toMutableList()
    } else {
        mutableListOf()
    }
}

private fun Context.saveHistory() {
    val fileDir = getExternalFilesDir(null)!!
    val historyFile = File(fileDir, HISTORY_FILENAME)
    history?.let {
        historyFile.writeText(it.joinToString("\n"))
    }
}

fun Context.getHistory(): List<String> {
    if (history == null) {
        loadHistory()
    }
    return history!!
}

fun Context.addHistory(name: String) {
    if (history == null) {
        loadHistory()
    }
    history!!.run {
        remove(name)
        add(0, name)
        saveHistory()
    }
}

fun Context.clearHistory() {
    if (history == null) {
        loadHistory()
    }
    history!!.run {
        clear()
        saveHistory()
    }
}