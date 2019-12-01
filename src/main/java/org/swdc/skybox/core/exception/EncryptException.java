package org.swdc.skybox.core.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

public class EncryptException extends Exception {

    private String message;

    public EncryptException(Exception ex) {
        PrintWriter printWriter = new PrintWriter(new StringWriter());
        ex.printStackTrace(printWriter);
        this.initCause(ex);
        this.message = printWriter.toString();
    }

    @Override
    public String toString() {
        return message;
    }
}
