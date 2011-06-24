package org.gonnot.imtp;
import jade.core.BaseNode;
import jade.core.HorizontalCommand;
import jade.core.IMTPException;
import java.util.Arrays;
import org.apache.log4j.Logger;
/**
 *
 */
class WebSocketNode extends BaseNode {
    private Logger logger = Logger.getLogger("WebSocketNode(n/a)");


    WebSocketNode(String nodeName, boolean hasLocalPlatformManager) {
        super(nodeName, hasLocalPlatformManager);
    }


    public Object accept(HorizontalCommand cmd) throws IMTPException {
        logger.info("accept(" + cmd.getClass().getSimpleName() + "," + cmd.getName() + "," + cmd.getInteraction()
                    + "," + cmd.getService() + "," + Arrays.asList(cmd.getParams()) + ")");

//        if (terminating) {
//            throw new IMTPException("Dead node");
//        }
        try {
            return serveHorizontalCommand(cmd);
        }
        catch (jade.core.ServiceException e) {
            throw new IMTPException("Service Error", e);
        }
    }


    public boolean ping(boolean hang) throws IMTPException {
        unsupported("ping");
        return false;
    }


    public void interrupt() throws IMTPException {
        unsupported("interrupt");
    }


    public void exit() throws IMTPException {
        unsupported("exit");
    }


    public void setLogger(Logger logger) {
        this.logger = logger;
    }


    private void tobeimplemented(String message) {
        logger.info("### --> " + message + " <-- ### -------------> TO BE IMPLEMENTED");
    }


    private void unsupported(String message) {
        logger.info("### --> " + message + " <-- ###");
        throw new RuntimeException(message);
    }
}
