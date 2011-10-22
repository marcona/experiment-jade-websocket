/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import org.gonnot.imtp.engine.SenderEngine.WebSocketGlue;
/**
 *
 */
public abstract class Command<T> {
    public abstract T execute(PlatformManager platformManager, Node localNode)
          throws IMTPException, JADESecurityException, ServiceException;


    protected T handle(NetworkChannel networkChannel, T result) throws IMTPException {
        return result;
    }


    public T handle(WebSocketGlue webSocket, T result) throws IMTPException {
        // todo : this method replaces the previous one (switch visibility to protected)
        return result;
    }


    public int getCommandId() {
        return System.identityHashCode(this);
    }
}
