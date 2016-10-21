package org.osmdroid.bonuspack.utils;

/**
 * Exception thrown when server returned status different from http status code 200.
 * This means there is connection to server just some problem occurred. For problem details see
 * http status code {@link #getHttpStatusCode()}.
 * */
public class StatusException extends Exception {

    private static final long serialVersionUID = 4703847528972168524L;

    private int httpStatusCode;

    public StatusException(int httpStatusCode) {
        super("HTTP STATUS CODE: "+httpStatusCode);
        this.httpStatusCode = httpStatusCode;
    }

    public StatusException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public StatusException(String message, Throwable cause, int httpStatusCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public StatusException(Throwable cause, int httpStatusCode) {
        super(cause);
        this.httpStatusCode = httpStatusCode;
    }

    /** @return httpStatusCode - http status code returned from server. */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

}
