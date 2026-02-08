package zip.sora.ajiro.privileged;

import zip.sora.ajiro.privileged.IWatchListener;

interface IUserService {
    void destroy() = 16777114;
    void exit() = 1;
    boolean init(String fileDir) = 2;
    void startWatchFiles(IWatchListener listener) = 3;
    void stopWatchFiles() = 4;
    String decName(String encName) = 5;
    int extractText(String name, String outputDir) = 6;
    boolean patchText(String name, String inputDir) = 7;
    boolean exportAsset(String name, String outputDir) = 8;
    boolean quickPatchSingle(String unity3dFile) = 9;
    void patchFinish() = 10;
    void restoreBackup() = 11;
}