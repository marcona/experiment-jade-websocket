package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.util.Vector;
/**
 *
 */
@SuppressWarnings({"UseOfObsoleteCollectionType"})
class AddNodeCommand implements Command {
    private NodeDescriptor descriptor;
    private Vector nodeServices;
    private boolean propagated;


    AddNodeCommand(NodeDescriptor descriptor, Vector nodeServices, boolean propagated) {
        this.descriptor = descriptor;
        this.nodeServices = nodeServices;
        this.propagated = propagated;
    }


    public String execute(PlatformManager platformManager) throws IMTPException, JADESecurityException,
                                                                  ServiceException {
        return platformManager.addNode(descriptor, nodeServices, propagated);
    }
}
