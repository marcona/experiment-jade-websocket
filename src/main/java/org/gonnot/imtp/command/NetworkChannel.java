package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;

import static org.gonnot.imtp.util.JadeExceptionUtil.imtpException;
/**
 *
 */
public abstract class NetworkChannel {
    public abstract Result send(Command command) throws IMTPException;


    public final <T> T execute(Command<T> command) throws IMTPException, ServiceException, JADESecurityException {
        Result result = send(command);
        if (result.hasFailed()) {
            throwFailure(result.getFailure());
        }
        //noinspection unchecked
        return (T)result.getResult();
    }


    public final <T> T execute2(Command<T> command) throws IMTPException, ServiceException {
        try {
            return execute(command);
        }
        catch (JADESecurityException ex) {
            throw imtpException("Unexpected JADE Security Exception", ex);
        }
    }


    public final <T> T execute3(Command<T> command) throws IMTPException {
        try {
            return execute(command);
        }
        catch (JADESecurityException ex) {
            throw imtpException("Unexpected JADE Security Exception", ex);
        }
        catch (ServiceException ex) {
            throw imtpException("Unexpected Service Exception", ex);
        }
    }


    private void throwFailure(Throwable failure) throws IMTPException, ServiceException, JADESecurityException {
        if (failure instanceof IMTPException) {
            throw (IMTPException)failure;
        }
        if (failure instanceof ServiceException) {
            throw (ServiceException)failure;
        }
        if (failure instanceof JADESecurityException) {
            throw (JADESecurityException)failure;
        }
        throw imtpException("Unexpected server error", failure);
    }
}
