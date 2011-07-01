package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
/**
 *
 */
public interface Command {
    String execute(PlatformManager platformManager) throws IMTPException, JADESecurityException,
                                                           ServiceException;
}
