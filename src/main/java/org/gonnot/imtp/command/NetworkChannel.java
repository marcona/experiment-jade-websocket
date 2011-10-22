/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

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
        return command.handle(this, (T)result.getResult());
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
