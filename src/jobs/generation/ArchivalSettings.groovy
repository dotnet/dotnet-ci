package jobs.generation;

class ArchivalSettings {
    String[] filesToArchive = null;
    String[] filesToExclude = null;
    boolean failIfNothingArchived = true
    private String[] archiveStatus = defaultSuccessArchiveStatus
    private static String[] defaultSuccessArchiveStatus = ['SUCCESS', 'SUCCESS']
    private static String[] defaultFailingArchiveStatus = ['ABORTED', 'FAILURE']
    private static String[] defaultAlwaysArchiveStatus = ['FAILURE', 'SUCCESS']

    void setArchiveOnFailure() {
        archiveStatus = defaultFailingArchiveStatus
    }

    void setArchiveOnSuccess() {
        archiveStatus = defaultSuccessArchiveStatus
    }

    void setAlwaysArchive() {
        archiveStatus = defaultAlwaysArchiveStatus
    }

    String[] getArchiveStatusRange() {
        return archiveStatus
    }

    void addFiles(String archiveBlob) {
        if (filesToArchive == null) {
            filesToArchive = [archiveBlob]
        } else {
            filesToArchive += archiveBlob
        }
    }

    void excludeFiles(String archiveBlob) {
        if (filesToExclude == null) {
            filesToExclude = [archiveBlob]
        } else {
            filesToExclude += archiveBlob
        }
    }

    boolean setFailIfNothingArchived() {
        failIfNothingArchived = true
    }

    boolean setDoNotFailIfNothingArchived() {
        failIfNothingArchived = false
    }
}
