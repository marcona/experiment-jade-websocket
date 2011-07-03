package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.Service.Slice;
import jade.core.ServiceException;
import java.util.Vector;
/**
 *
 */
public class CommandFactory {
    private CommandFactory() {
    }


    public static Command<String> getPlatformName() {
        return new GetPlatformNameCommand();
    }


    @SuppressWarnings({"UseOfObsoleteCollectionType"})
    public static Command<String> addNode(NodeDescriptor descriptor, Vector nodeServices, boolean propagated) {
        return new AddNodeCommand(descriptor, nodeServices, propagated);
    }


    public static Command<Slice> findSlice(final String serviceKey, final String sliceKey) {
        return new Command<Slice>() {
            public Slice execute(PlatformManager platformManager) throws IMTPException, ServiceException {
                return platformManager.findSlice(serviceKey, sliceKey);
            }
        };
    }
}
