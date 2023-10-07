package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.decompiling.DecompilerWrapper;
import org.jrd.frontend.frame.overwrite.LatestPaths;
import org.jrd.frontend.frame.overwrite.OverwriteClassDialog;
import org.jrd.frontend.utility.ScreenFinder;

class ClassOverwriter {
    private final DecompilationController decompilationController;
    private LatestPaths lastLoaded = new LatestPaths();

    ClassOverwriter(DecompilationController decompilationController) {
        this.decompilationController = decompilationController;
    }

    void overwriteClass(DecompilerWrapper selectedDecompiler, String name, String buffer, byte[] binBuffer, boolean isBinary) {
        if (name == null || name.trim().isEmpty()) {
            name = "???";
        }

        final OverwriteClassDialog overwriteClassDialog = new OverwriteClassDialog(
                name, lastLoaded, buffer, binBuffer, decompilationController.getVmInfo(), decompilationController.getVmManager(),
                decompilationController.getPluginManager(), selectedDecompiler, isBinary, decompilationController.isVerbose()
        );

        ScreenFinder.centerWindowToCurrentScreen(overwriteClassDialog);
        overwriteClassDialog.setVisible(true);

        lastLoaded.setLastManualUpload(overwriteClassDialog.getManualUploadPath());
        lastLoaded.setLastSaveSrc(overwriteClassDialog.getSaveSrcPath());
        lastLoaded.setLastSaveBin(overwriteClassDialog.getSaveBinPath());
        lastLoaded.setFilesToCompile(overwriteClassDialog.getFilesToCompile());
        lastLoaded.setOutputExternalFilesDir(overwriteClassDialog.getOutputExternalFilesDir());
        lastLoaded.setOutputBinaries(overwriteClassDialog.getOutputBinaries());
    }
}
