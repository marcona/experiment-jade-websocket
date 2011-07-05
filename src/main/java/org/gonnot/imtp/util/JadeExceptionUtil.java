package org.gonnot.imtp.util;
import jade.core.IMTPException;
/**
 *
 */
public class JadeExceptionUtil {
    private JadeExceptionUtil() {
    }


    public static IMTPException imtpException(String message, Throwable failure) {
        IMTPException imtpException = new IMTPException(message, failure);
        imtpException.initCause(failure);
        return imtpException;
    }
}
