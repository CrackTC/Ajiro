// https://gist.github.com/gitanuj/888ef7592be1d3f617f6
package zip.sora.ajiro.utils

import android.os.FileObserver
import java.io.File
import java.util.Stack

class RecursiveFileObserver(
    private val mPath: String,
    private var mMask: Int = ALL_EVENTS,
    private val onRecursiveEvent: (event: Int, file: File) -> Unit
) : FileObserver(mPath, mMask) {

    private val mObservers = mutableMapOf<String, FileObserver>()


    init {
        // 确保包含必要的掩码以维持递归逻辑
        mMask = mMask or CREATE or DELETE_SELF
    }

    private fun startWatching(path: String) {
        synchronized(mObservers) {
            mObservers.remove(path)?.stopWatching()

            val observer = SingleFileObserver(path, mMask)
            observer.startWatching()
            mObservers[path] = observer
        }
    }

    override fun startWatching() {
        val stack = Stack<String>()
        stack.push(mPath)

        // 递归监听所有子目录
        while (stack.isNotEmpty()) {
            val parent = stack.pop()
            startWatching(parent)

            val path = File(parent)
            val files = path.listFiles()
            if (files != null) {
                for (file in files) {
                    if (shouldWatch(file)) {
                        stack.push(file.absolutePath)
                    }
                }
            }
        }
    }

    private fun shouldWatch(file: File): Boolean {
        return file.isDirectory && file.name != "." && file.name != ".."
    }

    private fun stopWatching(path: String) {
        synchronized(mObservers) {
            mObservers.remove(path)?.stopWatching()
        }
    }

    override fun stopWatching() {
        synchronized(mObservers) {
            for (observer in mObservers.values) {
                observer.stopWatching()
            }
            mObservers.clear()
        }
    }

    override fun onEvent(event: Int, path: String?) {
        val file = if (path == null) File(mPath) else File(mPath, path)
        notifyListener(event, file)
    }

    private fun notifyListener(event: Int, file: File) {
        onRecursiveEvent(event and ALL_EVENTS, file)
    }

    private inner class SingleFileObserver(
        private val filePath: String, mask: Int
    ) : FileObserver(filePath, mask) {

        override fun onEvent(event: Int, path: String?) {
            val file = if (path == null) File(filePath) else File(filePath, path)

            when (event and ALL_EVENTS) {
                DELETE_SELF -> this@RecursiveFileObserver.stopWatching(filePath)
                CREATE -> {
                    if (shouldWatch(file)) {
                        this@RecursiveFileObserver.startWatching(file.absolutePath)
                    }
                }
            }

            this@RecursiveFileObserver.notifyListener(event, file)
        }
    }
}