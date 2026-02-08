// https://blog.xxin.xyz/2024/04/28/Shizuku%E5%BC%80%E5%8F%91/
package zip.sora.ajiro.utils

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

data class ShizukuContext(
    val shizukuRunning: Boolean,
    val shizukuGranted: Boolean?,
    val shizukuBound: Boolean,
    val requestPermission: () -> Unit
)

val LocalShizukuContext = compositionLocalOf {
    ShizukuContext(
        shizukuRunning = false,
        shizukuGranted = false,
        shizukuBound = false,
        requestPermission = {}
    )
}

fun checkShizukuPermission() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

suspend fun Context.requestShizukuPermission(requestCode: Int): Boolean {
    val granted = checkShizukuPermission()
    if (granted) {
        return true
    }

    if (Shizuku.isPreV11()) {
        Toast.makeText(this, "动态权限申请需要 Shizuku 版本 >= 11", Toast.LENGTH_SHORT).show()
        return false
    }

    return suspendCancellableCoroutine { continuation ->
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(resultRequestCode: Int, grantResult: Int) {
                Shizuku.removeRequestPermissionResultListener(this)
                val granted =
                    resultRequestCode == requestCode && grantResult == PackageManager.PERMISSION_GRANTED
                continuation.resume(granted)
            }
        }
        continuation.invokeOnCancellation { Shizuku.removeRequestPermissionResultListener(listener) }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(requestCode)
    }
}