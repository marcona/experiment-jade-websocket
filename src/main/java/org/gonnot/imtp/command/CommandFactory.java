package org.gonnot.imtp.command;
import jade.core.NodeDescriptor;
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
}
