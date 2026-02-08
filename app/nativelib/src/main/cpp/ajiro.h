#pragma once

extern "C" {
char *GetAssetIndexPath(const char *pFileDir);
int LoadIndex(const char *pPath);
char *GuessUserId(const char *pFileDir);
char *DecName(const char *pUserId, const char *pEncName);
int ExtractText(const char *pUserId, const char *pFileDir, const char *pName, const char *pOutputDir);
bool PatchText(const char *pUserId, const char *pFileDir, const char *pName, const char *pInputDir);
bool ExportAsset(const char *pUserId, const char *pFileDir, const char *pName, const char *pOutputDir);
bool QuickPatchSingle(const char *pUserId, const char *pFileDir, const char *pUnity3dFile);
void PatchFinish();
void RestoreBackup(const char *pUserId, const char *pFileDir);
}