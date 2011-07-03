package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.PlatformManager;
/**
 *
 */
class GetPlatformNameCommand implements Command<String> {

    public String execute(PlatformManager platformManager) throws IMTPException {
        return platformManager.getPlatformName();
    }
}
