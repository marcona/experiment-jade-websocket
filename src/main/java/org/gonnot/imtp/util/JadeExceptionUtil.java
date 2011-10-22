/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

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
