/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.experiment;
import jade.core.IMTPException;
import jade.core.PlatformManager;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.gonnot.imtp.experiment.RemoteProtocol.Result;
import websocket4j.server.WebServerSocket;
import websocket4j.server.WebSocket;
/**
 *
 */
class WebSocketIMTPServer implements Runnable {
    private static final Logger LOG = Logger.getLogger(WebSocketIMTPServer.class);
    static final String PLATFORM_URI = "/platform";
    private WebServerSocket socketServer;
    private PlatformManager platformManager;
    private boolean closing;
    private WebSocketNode localNode;


    public void setLocalNode(WebSocketNode localNode) {
        this.localNode = localNode;
    }


    public void start(int port) throws IMTPException {
        try {
            socketServer = new WebServerSocket(port);
            new Thread(this).start();
        }
        catch (IOException e) {
            throw new IMTPException("Unable to start the WebSocket server", e);
        }
    }


    public void stop() {
        closing = true;
        try {
            socketServer.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket server", e);
        }
    }


    public void setPlatformManager(PlatformManager platformManager) {
        this.platformManager = platformManager;
    }


    public void run() {
        try {
            handleClientContainerFirstConnection();
        }
        catch (Throwable e) {
            handleErrorDisplay(e, "WebSocket IMTP is closed", "Unexpected Error (WebSocket IMTP is down)");
        }
    }


    private void handleErrorDisplay(Throwable e, String normalExit, String failingExit) {
        if (closing) {
            LOG.info(normalExit);
        }
        else {
            LOG.error(failingExit, e);
        }
    }


    public void handleClientContainerFirstConnection() throws IOException {
        while (true) {
            WebSocket ws = socketServer.accept();
            if (PLATFORM_URI.equals(ws.getRequestUri())) {
                LOG.info("New connection request - GET " + ws.getRequestUri());
                (new PlatformManagerProxyReader(ws)).start();
            }
            else {
                LOG.warn("Unsupported request - GET " + ws.getRequestUri());
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


    private class PlatformManagerProxyReader extends Thread {
        private WebSocket webSocket;


        PlatformManagerProxyReader(WebSocket webSocket) {
            this.webSocket = webSocket;
        }


        @Override
        public void run() {
            try {
                handleConnection();
            }
            catch (Throwable e) {
                handleErrorDisplay(e,
                                   "WebSocket IMTP (PlatformManagerProxyReader) is closed",
                                   "Unexpected Error in PlatformManagerProxyReader (Local node has been pushed out of the platform)");
            }
            finally {
                silentClose(webSocket);
            }
        }


        private void handleConnection() throws IOException {
            while (true) {
                String message = webSocket.getMessage();
                Result result = RemoteProtocol.extractCommandFrom(message).executeOn(platformManager, localNode);
                webSocket.sendMessage(RemoteProtocol.toString(result));
                // @TODO - Should handle IMTPException here and return exception
            }
        }
    }
}
