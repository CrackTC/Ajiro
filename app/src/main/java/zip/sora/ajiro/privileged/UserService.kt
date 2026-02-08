package zip.sora.ajiro.privileged

import android.content.Context
import android.os.FileObserver
import zip.sora.ajiro.MLTD_PACKAGE_NAME
import zip.sora.ajiro.nativelib.NativeLib
import zip.sora.ajiro.utils.RecursiveFileObserver
import kotlin.system.exitProcess

enum class ExtractTextResult(val message: String) {
    SUCCESS(""),
    NOT_FOUND("无法找到对应的资源文件，这可能是游戏的资源更新或清除了资源缓存导致的，可以尝试重启 App"),
    NOT_TEXT("所选资源不包含文本资源"),
    DEC_FAIL("解密失败，资源可能不是游戏文本"),
    UNKNOWN("未知错误")
}

fun Int.toExtractTextResult() = when (this) {
    0 -> ExtractTextResult.SUCCESS
    1 -> ExtractTextResult.NOT_FOUND
    2 -> ExtractTextResult.NOT_TEXT
    3 -> ExtractTextResult.DEC_FAIL
    else -> ExtractTextResult.UNKNOWN
}

class UserService : IUserService.Stub() {
    val nativeLib = NativeLib()

    override fun destroy() = exitProcess(0)
    override fun exit() = destroy()

    private lateinit var fileDir: String
    private lateinit var userId: String
    override fun init(fileDir: String): Boolean {
        this.fileDir = fileDir
        val path = nativeLib.getAssetIndexPath(fileDir)
        if (path == null) return false
        if (nativeLib.loadIndex("$fileDir/$path") == -1) return false
        nativeLib.guessUserId(fileDir).let {
            if (it == null) return false
            userId = it
        }
        return true
    }

    private var observer: FileObserver? = null
    override fun startWatchFiles(listener: IWatchListener) {
        if (observer != null) return

        RecursiveFileObserver("$fileDir/Cache00", FileObserver.OPEN) { event, file ->
            if (file.isFile && (event and FileObserver.OPEN) != 0) {
                listener.onOpenFile(decName(file.name))
            }
        }.also { observer = it }.startWatching()
    }

    override fun stopWatchFiles() {
        observer?.let {
            it.stopWatching()
            observer = null
        }
    }

    override fun decName(encName: String): String =
        nativeLib.decName(userId, encName)

    override fun extractText(name: String, outputDir: String): Int =
        nativeLib.extractText(userId, fileDir, name, outputDir)

    override fun patchText(name: String, inputDir: String): Boolean =
        nativeLib.patchText(userId, fileDir, name, inputDir)

    override fun exportAsset(name: String, outputDir: String) =
        nativeLib.exportAsset(userId, fileDir, name, outputDir)


    override fun quickPatchSingle(unity3dFile: String) =
        nativeLib.quickPatchSingle(userId, fileDir, unity3dFile)

    override fun patchFinish() = nativeLib.patchFinish()
    override fun restoreBackup() = nativeLib.restoreBackup(userId, fileDir)

    companion object {
        var instance: IUserService? = null
    }
}

fun Context.initUserService(service: IUserService) =
    service.init(
        getExternalFilesDir(null)!!.absolutePath.replace(
            packageName,
            MLTD_PACKAGE_NAME
        )
    )