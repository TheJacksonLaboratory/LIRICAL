package org.monarchinitiative.lirical.io;

public class FileDownloadException extends Exception {
    private static final long serialVersionUID = 1L;

    public FileDownloadException() {
        super();
    }

    public FileDownloadException(String msg) {
        super(msg);
    }

    public FileDownloadException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
