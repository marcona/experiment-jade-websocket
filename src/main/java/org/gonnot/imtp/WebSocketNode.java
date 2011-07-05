package org.gonnot.imtp;
import jade.core.BaseNode;
import jade.core.HorizontalCommand;
import jade.core.IMTPException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import org.apache.log4j.Logger;

import static org.gonnot.imtp.util.JadeExceptionUtil.imtpException;
/**
 *
 */
class WebSocketNode extends BaseNode {
    private transient Logger logger = Logger.getLogger("WebSocketNode(n/a)");
    private final Object terminationLock = new Object();
    private boolean terminating = false;


    WebSocketNode(String nodeName, boolean hasLocalPlatformManager) {
        super(nodeName, hasLocalPlatformManager);
    }


    public Object accept(HorizontalCommand cmd) throws IMTPException {
        logger.debug("accept(" + cmd.getClass().getSimpleName() + "," + cmd.getName() + "," + cmd.getInteraction()
                     + "," + cmd.getService() + "," + Arrays.asList(cmd.getParams()) + ")");

//        if (terminating) {
//            throw JadeExceptionUtil.imtpException("Dead node");
//        }
        try {
            return serveHorizontalCommand(cmd);
        }
        catch (jade.core.ServiceException e) {
            throw imtpException("Service Error", e);
        }
    }


    public boolean ping(boolean hang) throws IMTPException {
        tobeimplemented("ping");
        if (hang) {
            waitTermination();
        }
        return terminating;
    }


    private void waitTermination() {
        synchronized (terminationLock) {
            try {
                terminationLock.wait();
            }
            catch (InterruptedException ie) {
                ;// Do nothing
            }
        }
    }


    private void notifyTermination() {
        synchronized (terminationLock) {
            terminationLock.notifyAll();
        }
    }


    public void interrupt() throws IMTPException {
        notifyTermination();
        unsupported("interrupt");
    }


    public void exit() throws IMTPException {
        terminating = true;
        notifyTermination();
        unsupported("exit");
    }


    public void setLogger(Logger logger) {
        this.logger = logger;
    }


    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        logger = WebSocketIMTPManager.recreateLogger(this);
    }


    private void tobeimplemented(String message) {
        logger.info("### --> " + message + " <-- ### -------------> TO BE IMPLEMENTED");
    }


    private void unsupported(String message) {
        logger.info("### --> " + message + " <-- ###");
        throw new RuntimeException(message);
    }
}
