package org.wso2.carbon.ui.transports.fileupload;

/**
 * This exception is thrown when file uploading fails
 */
public class FileUploadFailedException extends Exception {
    public FileUploadFailedException() {
        
    }

    public FileUploadFailedException(String s) {
        super(s);    
    }

    public FileUploadFailedException(String s, Throwable throwable) {
        super(s, throwable);    
    }

    public FileUploadFailedException(Throwable throwable) {
        super(throwable);    
    }
}
