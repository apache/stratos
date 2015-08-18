package org.wso2.carbon.ui.transports.fileupload;

/**
 * This exception will be thrown when the maximum allowed file size limit is exceeded. This can
 * be applicable to a single file or a collection of files.
 */
public class FileSizeLimitExceededException extends Exception {

    public FileSizeLimitExceededException(long fileSizeLimitInMB) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded");
    }

    public FileSizeLimitExceededException(String msg, long fileSizeLimitInMB) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded. " + msg);
    }

    public FileSizeLimitExceededException(String msg, long fileSizeLimitInMB, Throwable throwable) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded. " + msg, throwable);
    }

    public FileSizeLimitExceededException(Throwable throwable, long fileSizeLimitInMB) {
        super("File size limit of " + fileSizeLimitInMB + " MB has been exceeded", throwable);
    }
}
