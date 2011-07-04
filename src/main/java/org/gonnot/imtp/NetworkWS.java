package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import org.gonnot.imtp.command.Command;
/**
 *
 */
interface NetworkWS {
    public abstract <T> T synchronousCall(Command<T> command)
          throws IMTPException, ServiceException, JADESecurityException;
}
