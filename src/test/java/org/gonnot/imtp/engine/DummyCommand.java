package org.gonnot.imtp.engine;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import org.gonnot.imtp.command.Command;
/**
 *
 */
class DummyCommand extends Command<String> {
    private int id = -1;


    DummyCommand() {
    }


    DummyCommand(int id) {
        this.id = id;
    }


    @Override
    public String execute(PlatformManager platformManager, Node localNode) throws IMTPException,
                                                                                  JADESecurityException,
                                                                                  ServiceException {
        return "resultOf(command[" + getCommandId() + "].execute(...))";
    }


    @Override
    public int getCommandId() {
        if (id == -1) {
            return super.getCommandId();
        }
        return id;
    }
}
