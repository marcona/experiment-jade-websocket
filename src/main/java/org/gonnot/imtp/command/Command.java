package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
/**
 *
 */
public abstract class Command<T> {
    public abstract T execute(PlatformManager platformManager, Node localNode)
          throws IMTPException, JADESecurityException, ServiceException;


    protected T handle(NetworkChannel networkChannel, T result) throws IMTPException {
        return result;
    }
}
