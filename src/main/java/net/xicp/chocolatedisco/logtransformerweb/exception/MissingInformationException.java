package net.xicp.chocolatedisco.logtransformerweb.exception;

/**
 * Created by SunYu on 2018/1/3.
 */
public class MissingInformationException extends RuntimeException {
    public MissingInformationException(String message) {
        super(message);
    }

    public MissingInformationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingInformationException(Throwable cause) {
        super(cause);
    }

    public MissingInformationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MissingInformationException() {

    }
}
