package org.gonnot.imtp.command;
import jade.core.HorizontalCommand;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.Service.Slice;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.util.Vector;
import org.gonnot.imtp.WebSocketNode;
import org.gonnot.imtp.util.JadeExceptionUtil;
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
    public static Command<String> addNode(final NodeDescriptor descriptor,
                                          final Vector nodeServices,
                                          final boolean propagated) {
        return new Command<String>() {
            @Override
            public String execute(PlatformManager platformManager, Node localNode)
                  throws IMTPException, JADESecurityException, ServiceException {
                return platformManager.addNode(descriptor, nodeServices, propagated);
            }
        };
    }


    public static Command<Slice> findSlice(final String serviceKey, final String sliceKey) {
        return new Command<Slice>() {
            @Override
            public Slice execute(PlatformManager platformManager, Node localNode)
                  throws IMTPException, ServiceException {
                return platformManager.findSlice(serviceKey, sliceKey);
            }


            @Override
            protected Slice handle(NetworkChannel networkChannel, Slice result) throws IMTPException {
                try {
                    if (result.getNode() instanceof WebSocketNode) {
                        ((WebSocketNode)result.getNode()).setChannel(networkChannel);
                    }
                }
                catch (ServiceException e) {
                    throw JadeExceptionUtil.imtpException("unexpected error", e);
                }
                return result;
            }
        };
    }


    public static Command<Object> accept(final HorizontalCommand horizontalCommand) {
        return new Command<Object>() {
            @Override
            public Object execute(PlatformManager platformManager, Node localNode) throws IMTPException {
                return localNode.accept(horizontalCommand);
            }
        };
    }
}
