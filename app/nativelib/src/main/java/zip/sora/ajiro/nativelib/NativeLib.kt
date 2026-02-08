package zip.sora.ajiro.nativelib

class NativeLib {
    external fun getAssetIndexPath(fileDir: String): String?
    external fun loadIndex(path: String): Int
    external fun guessUserId(fileDir: String): String?
    external fun decName(userId: String, encName: String): String
    external fun extractText(userId: String, fileDir: String, name: String, outputDir: String): Int
    external fun patchText(userId: String, fileDir: String, name: String, inputDir: String): Boolean
    external fun exportAsset(userId: String, fileDir: String, name: String, outputDir: String): Boolean
    external fun quickPatchSingle(userId: String, fileDir: String, unity3dFile: String): Boolean
    external fun patchFinish()
    external fun restoreBackup(userId: String, fileDir: String)

    companion object {
        init {
            System.loadLibrary("nativelib")
        }
    }
}