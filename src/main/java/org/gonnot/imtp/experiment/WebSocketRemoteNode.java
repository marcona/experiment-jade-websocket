package org.gonnot.imtp.experiment;
import jade.core.HorizontalCommand;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.Service.Slice;
import jade.core.ServiceException;
import java.io.IOException;
import org.apache.log4j.Logger;
import websocket4j.client.WebSocket;
/**
 *
 */
class WebSocketRemoteNode extends WebSocketNode {
    private transient WebSocket socketClient;
    private transient NodeProxy nodeProxy = new NodeProxy();


    WebSocketRemoteNode(String name, boolean hasLocalPlatformManager) {
        super(name, hasLocalPlatformManager);
        logger = Logger.getLogger("[Client] RemoteNode(executed on client)");
    }

    @Override
    public void startClient(String host, int port) throws IMTPException {
        try {
            socketClient = new WebSocket(host, port, NODE_URI);
        }
        catch (IOException e) {
            throw new IMTPException("Unable to start the WebSocket NODE client", e);
        }
    }


    @Override
    public void stopWebsocket() {
        if (socketClient == null) {
            return;
        }
        try {
            socketClient.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket NODE client", e);
        }
    }


    @Override
    public Object accept(HorizontalCommand cmd) throws IMTPException {
        try {
            return super.serveHorizontalCommand(cmd);
        }
        catch (ServiceException se) {
            throw new IMTPException("Service Error", se);
        }
    }


    @Override
    public void interrupt() throws IMTPException {
        logger.info("interrupt()");
        super.interrupt();
    }


    @Override
    public void exit() throws IMTPException {
        logger.info("exit()");
        super.exit();
    }


    /**
     * @see jade.imtp.rmi.NodeAdapter#platformManagerDead(String, String)
     */
    @Override
    public void platformManagerDead(String deadPMAddr, String notifyingPMAddr) throws IMTPException {
        super.platformManagerDead(deadPMAddr, notifyingPMAddr);
    }


    private class NodeProxy implements Node {

        public void setName(String name) {
            unsuported("setName");
        }


        public String getName() {
            unsuported("getName");
            return null;
        }


        public boolean hasPlatformManager() {
            unsuported("hasPlatformManager");
            return false;
        }


        public void exportSlice(String serviceName, Slice localSlice) {
            unsuported("exportSlice");
        }


        public void unexportSlice(String serviceName) {
            unsuported("unexportSlice");
        }


        public Object accept(HorizontalCommand cmd) throws IMTPException {
            unsuported("accept");
            return null;
        }


        public boolean ping(boolean hang) throws IMTPException {
            unsuported("ping");
            return false;
        }


        public void interrupt() throws IMTPException {
            unsuported("interrupt");
        }


        public void exit() throws IMTPException {
            unsuported("exit");
        }


        public void platformManagerDead(String deadPmAddress, String notifyingPmAddr) throws IMTPException {
            unsuported("platformManagerDead");
        }


        private void unsuported(String message) {
            logger.info("### --> " + message + " <-- ###");
            throw new RuntimeException(message);
        }
    }
}
