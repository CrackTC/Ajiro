#include <jni.h>
#include <string>
#include <android/log.h>
#include "ajiro.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_getAssetIndexPath(JNIEnv *env, jobject, jstring file_dir) {
    auto pFileDir = env->GetStringUTFChars(file_dir, nullptr);
    auto result = GetAssetIndexPath(pFileDir);
    env->ReleaseStringUTFChars(file_dir, pFileDir);
    auto resultStr = env->NewStringUTF(result);
    free(result);
    return resultStr;
}

extern "C"
JNIEXPORT jint JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_loadIndex(JNIEnv *env, jobject, jstring path) {
    auto pPath = env->GetStringUTFChars(path, nullptr);
    auto result = LoadIndex(pPath);
    env->ReleaseStringUTFChars(path, pPath);
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_guessUserId(JNIEnv *env, jobject, jstring file_dir) {
    auto pFileDir = env->GetStringUTFChars(file_dir, nullptr);
    auto result = GuessUserId(pFileDir);
    env->ReleaseStringUTFChars(file_dir, pFileDir);
    auto resultStr = env->NewStringUTF(result);
    free(result);
    return resultStr;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_decName(JNIEnv *env, jobject, jstring user_id,
                                                jstring enc_name) {
    auto pUserId = env->GetStringUTFChars(user_id, nullptr);
    auto pEncName = env->GetStringUTFChars(enc_name, nullptr);
    auto result = DecName(pUserId, pEncName);
    env->ReleaseStringUTFChars(user_id, pUserId);
    env->ReleaseStringUTFChars(enc_name, pEncName);
    auto resultStr = env->NewStringUTF(result);
    free(result);
    return resultStr;
}
extern "C"
JNIEXPORT jint JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_extractText(JNIEnv *env, jobject, jstring user_id,
                                                    jstring file_dir, jstring name,
                                                    jstring output_dir) {
    auto pUserId = env->GetStringUTFChars(user_id, nullptr);
    auto pFileDir = env->GetStringUTFChars(file_dir, nullptr);
    auto pName = env->GetStringUTFChars(name, nullptr);
    auto pOutputDir = env->GetStringUTFChars(output_dir, nullptr);
    auto result = ExtractText(pUserId, pFileDir, pName, pOutputDir);
    env->ReleaseStringUTFChars(user_id, pUserId);
    env->ReleaseStringUTFChars(file_dir, pFileDir);
    env->ReleaseStringUTFChars(name, pName);
    env->ReleaseStringUTFChars(output_dir, pOutputDir);

    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_patchText(JNIEnv *env, jobject, jstring user_id,
                                                  jstring file_dir, jstring name,
                                                  jstring input_dir) {
    auto pUserId = env->GetStringUTFChars(user_id, nullptr);
    auto pFileDir = env->GetStringUTFChars(file_dir, nullptr);
    auto pName = env->GetStringUTFChars(name, nullptr);
    auto pInputDir = env->GetStringUTFChars(input_dir, nullptr);
    auto result = PatchText(pUserId, pFileDir, pName, pInputDir);
    env->ReleaseStringUTFChars(user_id, pUserId);
    env->ReleaseStringUTFChars(file_dir, pFileDir);
    env->ReleaseStringUTFChars(name, pName);
    env->ReleaseStringUTFChars(input_dir, pInputDir);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_exportAsset(JNIEnv *env, jobject, jstring user_id,
                                                    jstring file_dir, jstring name,
                                                    jstring output_dir) {
    auto pUserId = env->GetStringUTFChars(user_id, nullptr);
    auto pFileDir = env->GetStringUTFChars(file_dir, nullptr);
    auto pName = env->GetStringUTFChars(name, nullptr);
    auto pOutputDir = env->GetStringUTFChars(output_dir, nullptr);
    auto result = ExportAsset(pUserId, pFileDir, pName, pOutputDir);
    env->ReleaseStringUTFChars(user_id, pUserId);
    env->ReleaseStringUTFChars(file_dir, pFileDir);
    env->ReleaseStringUTFChars(name, pName);
    env->ReleaseStringUTFChars(output_dir, pOutputDir);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_quickPatchSingle(JNIEnv *env, jobject, jstring user_id,
                                                         jstring file_dir, jstring unity3d_file) {
    auto pUserId = env->GetStringUTFChars(user_id, nullptr);
    auto pFileDir = env->GetStringUTFChars(file_dir, nullptr);
    auto pUnity3dFile = env->GetStringUTFChars(unity3d_file, nullptr);
    auto result = QuickPatchSingle(pUserId, pFileDir, pUnity3dFile);
    env->ReleaseStringUTFChars(user_id, pUserId);
    env->ReleaseStringUTFChars(file_dir, pFileDir);
    env->ReleaseStringUTFChars(unity3d_file, pUnity3dFile);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_patchFinish(JNIEnv *, jobject) {
    PatchFinish();
}
extern "C"
JNIEXPORT void JNICALL
Java_zip_sora_ajiro_nativelib_NativeLib_restoreBackup(JNIEnv *env, jobject, jstring user_id,
                                                      jstring file_dir) {
    auto pUserId = env->GetStringUTFChars(user_id, nullptr);
    auto pFileDir = env->GetStringUTFChars(file_dir, nullptr);
    RestoreBackup(pUserId, pFileDir);
    env->ReleaseStringUTFChars(user_id, pUserId);
    env->ReleaseStringUTFChars(file_dir, pFileDir);
}