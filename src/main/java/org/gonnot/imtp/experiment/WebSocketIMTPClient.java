package org.gonnot.imtp.experiment;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.Service.Slice;
import jade.core.ServiceDescriptor;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.io.IOException;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.gonnot.imtp.experiment.RemoteProtocol.NetworkChannel;
import websocket4j.client.WebSocket;

import static org.gonnot.imtp.experiment.RemoteProtocol.GetPlatformName;
/**
 *
 */
class WebSocketIMTPClient {
    private static final Logger LOG = Logger.getLogger(WebSocketIMTPClient.class);
    private WebSocket socketClient;
    private NetworkChannel networkChannel = new WSNetChannel();
    private boolean closing;


    public void start(String host, int port) throws IMTPException {
        try {
            socketClient = new WebSocket(host, port, WebSocketIMTPServer.PLATFORM_URI);
        }
        catch (IOException e) {
            throw new IMTPException("Unable to start the WebSocket client", e);
        }
    }


    public void stop() {
        closing = true;
        try {
            socketClient.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket client", e);
        }
    }


    public PlatformManager getPlatformManagerProxy() {
        return new PlatformManagerProxy();
    }


    private class PlatformManagerProxy implements PlatformManager {

        public String getPlatformName() throws IMTPException {
            ensureNotClosing();
            return GetPlatformName.command().remoteExecute(networkChannel);
        }


        public String getLocalAddress() {
            methodImpl("remoteCall.getLocalAddress()");
            return null;
        }


        public void setLocalAddress(String addr) {
            methodImpl("remoteCall.setLocalAddress()");
        }


        public String addNode(NodeDescriptor dsc, Vector nodeServices, boolean propagated)
              throws IMTPException, ServiceException, JADESecurityException {
            ensureNotClosing();
            return RemoteProtocol.AddNode.command(dsc, nodeServices, propagated).remoteExecute(networkChannel);
        }


        public void removeNode(NodeDescriptor dsc, boolean propagated) throws IMTPException, ServiceException {
            methodImpl("remoteCall.removeNode()");
        }


        public void addSlice(ServiceDescriptor service, NodeDescriptor dsc, boolean propagated)
              throws IMTPException, ServiceException {
            methodImpl("remoteCall.addSlice()");
        }


        public void removeSlice(String serviceKey, String sliceKey, boolean propagated)
              throws IMTPException, ServiceException {
            methodImpl("remoteCall.removeSlice()");
        }


        public void addReplica(String newAddr, boolean propagated) throws IMTPException, ServiceException {
            methodImpl("remoteCall.addReplica()");
        }


        public void removeReplica(String address, boolean propagated) throws IMTPException, ServiceException {
            methodImpl("remoteCall.removeReplica()");
        }


        public Slice findSlice(String serviceKey, String sliceKey) throws IMTPException, ServiceException {
            ensureNotClosing();
            return RemoteProtocol.FindSlice.command(serviceKey, sliceKey).remoteExecute(networkChannel);
        }


        public Vector findAllSlices(String serviceKey) throws IMTPException, ServiceException {
            methodImpl("remoteCall.findAllSlices()");
            return null;
        }


        public void adopt(Node n, Node[] children) throws IMTPException {
            methodImpl("remoteCall.adopt()");
        }


        public void ping() throws IMTPException {
            methodImpl("remoteCall.ping()");
        }
    }

    private class WSNetChannel implements NetworkChannel {
        private final Object connectionLock = new Object();


        public String remoteCall(String message) throws IMTPException {
            try {
                synchronized (connectionLock) {
                    socketClient.sendMessage(message);
                    return socketClient.getMessage();
                }
            }
            catch (IOException e) {
                throw new IMTPException("Connection is broken", e);
            }
        }
    }


    private void methodImpl(String message) {
        ensureNotClosing();
        unsuported(message);
    }


    private void ensureNotClosing() {
        if (closing) {
            throw new IllegalStateException("Closing proxy...");
        }
    }


    private void unsuported(String message) {
        LOG.info("### --> " + message + " <-- ###");
        System.exit(-1);
//        throw new RuntimeException(message);
    }
}
