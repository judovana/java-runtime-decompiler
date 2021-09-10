package org.jrd.frontend.frame.overwrite;

public final class LatestPaths {
    private String lastManualUpload;
    private String lastSaveSrc;
    private String lastSaveBin;
    private String filesToCompile;
    private String outputExternalFilesDir;
    private String outputBinaries;

    public LatestPaths() {
        lastManualUpload = System.getProperty("user.home");
        lastSaveSrc = System.getProperty("user.home");
        lastSaveBin = System.getProperty("user.home");
        filesToCompile = System.getProperty("user.home");
        outputExternalFilesDir = System.getProperty("user.home");
        outputBinaries = System.getProperty("user.home");
    }

    public String getLastManualUpload() {
        return lastManualUpload;
    }

    public void setLastManualUpload(String lastManualUpload) {
        this.lastManualUpload = lastManualUpload;
    }

    public String getLastSaveSrc() {
        return lastSaveSrc;
    }

    public void setLastSaveSrc(String lastSaveSrc) {
        this.lastSaveSrc = lastSaveSrc;
    }

    public String getLastSaveBin() {
        return lastSaveBin;
    }

    public void setLastSaveBin(String lastSaveBin) {
        this.lastSaveBin = lastSaveBin;
    }

    public String getFilesToCompile() {
        return filesToCompile;
    }

    public void setFilesToCompile(String filesToCompile) {
        this.filesToCompile = filesToCompile;
    }

    public String getOutputExternalFilesDir() {
        return outputExternalFilesDir;
    }

    public void setOutputExternalFilesDir(String outputExternalFilesDir) {
        this.outputExternalFilesDir = outputExternalFilesDir;
    }

    public String getOutputBinaries() {
        return outputBinaries;
    }

    public void setOutputBinaries(String outputBinaries) {
        this.outputBinaries = outputBinaries;
    }
}
