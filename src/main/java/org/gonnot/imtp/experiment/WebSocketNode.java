/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.experiment;
import jade.core.BaseNode;
import jade.core.HorizontalCommand;
import jade.core.IMTPException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.gonnot.imtp.experiment.RemoteProtocol.NetworkChannel;
import org.gonnot.imtp.experiment.RemoteProtocol.NodeAccept;
import websocket4j.server.WebServerSocket;
import websocket4j.server.WebSocket;
/**
 *
 */
class WebSocketNode extends BaseNode implements Runnable {
    protected transient Logger logger;
    static final String NODE_URI = "/node";
    private final Object terminationLock = new Object();
    private boolean terminating = false;
    private transient WebServerSocket socketServer;
    private transient NetworkChannel networkChannel;


    WebSocketNode(String name, boolean hasLocalPlatformManager) {
        super(name, hasLocalPlatformManager);
        logger = Logger.getLogger("[Server] ServerNode(executed on server)");
    }


    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        if (this.getClass() == WebSocketNode.class) {
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$ WebSocketNode $$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        }
        else if (this.getClass() == WebSocketRemoteNode.class) {
            System.out.println("$$$$$$$$$$$$$$$ remote $$$$$$$$$$$$$$$$$$$$$$$");
        }
        else {
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$ WebSocket ?????? $$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        }
    }


    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.getClass() == WebSocketNode.class) {
            logger = Logger.getLogger("[Client] ServerNode(executed on client)");
        }
        else if (this.getClass() == WebSocketRemoteNode.class) {
            logger = Logger.getLogger("[Server] RemoteNode(executed on server)");
        }
        else {
            logger = Logger.getLogger("[???] ??Node(executed on ???)");
        }
    }


    public void startServer(int port) throws IMTPException {
        try {
            socketServer = new WebServerSocket(port);
            new Thread(this).start();
        }
        catch (IOException e) {
            throw new IMTPException("Unable to start the WebSocket Node server", e);
        }
    }


    public void startClient(String host, int port) throws IMTPException {
    }


    public void stopWebsocket() {
        if (socketServer == null) {
            return;
        }
        try {
            socketServer.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket Node server", e);
        }
    }


    public Object accept(HorizontalCommand cmd) throws IMTPException {
        logger.info("accept(" + cmd.getClass().getSimpleName() + "," + cmd.getName() + "," + cmd.getInteraction()
                    + "," + cmd.getService() + "," + Arrays.asList(cmd.getParams()) + ")");

        if (networkChannel != null) {
            return NodeAccept.command(cmd).remoteExecute(networkChannel);
        }

        if (terminating) {
            throw new IMTPException("Dead node");
        }
        try {
            return serveHorizontalCommand(cmd);
        }
        catch (jade.core.ServiceException e) {
            throw new IMTPException("Service Error", e);
        }
    }


    public boolean ping(boolean hang) throws IMTPException {
        logger.info("ping(" + hang + ")");
        if (hang) {
            waitTermination();
        }
        return terminating;
    }


    public void interrupt() throws IMTPException {
        notifyTermination();
    }


    public void exit() throws IMTPException {
        terminating = true;
        notifyTermination();
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


    public void run() {
        try {
            handleClientNodeFirstConnection();
        }
        catch (Throwable e) {
            handleErrorDisplay(e, "WebSocket IMTP is closed", "Unexpected Error (WebSocket IMTP is down)");
        }
    }


    private void handleClientNodeFirstConnection() throws IOException {
        while (true) {
            WebSocket ws = socketServer.accept();
            if (NODE_URI.equals(ws.getRequestUri())) {
                logger.info("New node connection request - GET " + ws.getRequestUri());
                (new NodeProxyReader(ws)).start();
            }
            else {
                logger.warn("Unsupported node request - GET " + ws.getRequestUri());
                silentClose(ws);
            }
        }
    }


    private static void silentClose(WebSocket ws) {
        try {
            ws.close();
        }
        catch (IOException e) {
            ;
        }
    }


    private void handleErrorDisplay(Throwable e, String normalExit, String failingExit) {
        if (terminating) {
            logger.info(normalExit);
        }
        else {
            logger.error(failingExit, e);
        }
    }


    public void setNetworkChannel(NetworkChannel networkChannel) {
        this.networkChannel = networkChannel;
    }


    private class NodeProxyReader extends Thread {
        private WebSocket webSocket;


        NodeProxyReader(WebSocket webSocket) {
            this.webSocket = webSocket;
        }


        @Override
        public void run() {
            try {
                handleConnection();
            }
            catch (Throwable e) {
                handleErrorDisplay(e,
                                   "WebSocket IMTP (NodeProxyReader) is closed",
                                   "Unexpected Error in NodeProxyReader (Local node has been pushed out of the platform)");
            }
            finally {
                silentClose(webSocket);
            }
        }


        private void handleConnection() throws IOException {
            while (true) {
                String message = webSocket.getMessage();
//                Result result = RemoteNodeProtocol.extractCommandFrom(message).executeOn(WebSocketNode.this);
//                webSocket.sendMessage(RemoteNodeProtocol.toString(result));
                // @TODO - Should handle IMTPException here and return exception
            }
        }
    }
}
