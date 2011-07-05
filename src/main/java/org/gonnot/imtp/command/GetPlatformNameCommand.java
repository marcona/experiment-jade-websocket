package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.PlatformManager;
/**
 *
 */
class GetPlatformNameCommand extends Command<String> {

    @Override
    public String execute(PlatformManager platformManager, Node localNode) throws IMTPException {
        return platformManager.getPlatformName();
    }
}
