package org.jrd.frontend.frame.main.decompilerview;

import org.jrd.backend.core.ClassInfo;
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

    void overwriteClass(DecompilerWrapper selectedDecompiler, ClassInfo name, String buffer, byte[] binBuffer, int tab) {
        if (name == null) {

        } else if (name.getName() == null || name.getName().trim().isEmpty()) {
            name = new ClassInfo("???", name.getLocation(), name.getClassLoader());
        }

        final OverwriteClassDialog overwriteClassDialog = new OverwriteClassDialog(
                name, lastLoaded, buffer, binBuffer, decompilationController.getVmInfo(), decompilationController.getVmManager(),
                decompilationController.getPluginManager(), selectedDecompiler, tab, decompilationController.isVerbose(),
                decompilationController
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
