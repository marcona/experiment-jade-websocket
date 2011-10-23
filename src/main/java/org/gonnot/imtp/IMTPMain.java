/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.PlatformManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import org.gonnot.imtp.engine.ExecutorEngine;
import org.gonnot.imtp.engine.ExecutorEngine.WebSocketGlue;
import websocket4j.server.WebServerSocket;
import websocket4j.server.WebSocket;
import static java.util.Collections.synchronizedList;
import static org.gonnot.imtp.util.JadeExceptionUtil.imtpException;
/**
 *
 */
class IMTPMain {
    private static final Logger LOG = Logger.getLogger(IMTPMain.class);
    static final String PLATFORM_URI = "/platform";

    private PlatformManager platformManager;
    private WebServerSocket socketServer;
    private volatile boolean closing;
    private Node localNode;
    private List<ExecutorEngine> activeClientConnection = synchronizedList(new ArrayList<ExecutorEngine>());


    IMTPMain(Node localNode) {
        this.localNode = localNode;
    }


    public void setPlatformManager(PlatformManager manager) {
        this.platformManager = manager;
    }


    public void start(int port) throws IMTPException {
        try {
            socketServer = new WebServerSocket(port);
            new HandleSocketServerThread().start();
        }
        catch (IOException e) {
            throw imtpException("Unable to start the WebSocket server", e);
        }
    }


    public void handleIncomingClientConnection() throws IOException {
        while (!closing) {
            final WebSocket clientWebSocket = socketServer.accept();
            if (closing) {
                silentClose(clientWebSocket);
                return;
            }
            if (PLATFORM_URI.equals(clientWebSocket.getRequestUri())) {
                LOG.info("New connection request - GET " + clientWebSocket.getRequestUri()
                         + " from " + clientWebSocket.getRemoteId());
                new ExecutorEngine(new WebSocketGlueImpl(clientWebSocket)).init(platformManager, localNode);
            }
            else {
                LOG.warn("Unsupported request - GET " + clientWebSocket.getRequestUri()
                         + " from " + clientWebSocket.getRemoteId());
                silentClose(clientWebSocket);
            }
        }
    }


    public void shutDown() {
        if (closing) {
            return;
        }
        LOG.info("Shutdown IMTP main server and all active connections...");
        closing = true;

        for (ExecutorEngine engine : copyOf(activeClientConnection)) {
            safeShutdown(engine);
        }

        try {
            socketServer.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket server", e);
        }
    }


    int getActiveConnectionCount() {
        return activeClientConnection.size();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------------------------


    private void handleErrorDisplay(Throwable error, String normalExit, String failingExit) {
        if (closing) {
            LOG.info(normalExit);
        }
        else {
            LOG.error(failingExit, error);
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


    private static void safeShutdown(ExecutorEngine engine) {
        try {
            engine.shutdown();
        }
        catch (Throwable e) {
            LOG.warn("Engine shutdown in error (IMTP shutdown will continue)", e);
        }
    }


    private static ExecutorEngine[] copyOf(List<ExecutorEngine> sockets) {
        return sockets.toArray(new ExecutorEngine[sockets.size()]);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Utility classes
    // -----------------------------------------------------------------------------------------------------------------

    private class WebSocketGlueImpl implements WebSocketGlue {
        private final WebSocket webSocket;


        WebSocketGlueImpl(WebSocket webSocket) {
            this.webSocket = webSocket;
        }


        public String getRemoteClientId() {
            return webSocket.getRemoteId();
        }


        public void send(Result result) throws IOException {
            webSocket.sendMessage(CommandCodec.encode(result));
        }


        public Command receive() throws InterruptedException, IOException {
            return CommandCodec.decode(webSocket.getMessage());
        }


        public void open(ExecutorEngine engine) {
            activeClientConnection.add(engine);
        }


        public void close(ExecutorEngine engine) {
            silentClose(webSocket);
            activeClientConnection.remove(engine);
        }
    }
    private class HandleSocketServerThread extends Thread {
        private HandleSocketServerThread() {
            super("incoming-connection-listener");
        }


        @Override
        public void run() {
            try {
                handleIncomingClientConnection();
            }
            catch (Throwable e) {
                handleErrorDisplay(e, "WebSocket IMTP is closed", "Unexpected Error (WebSocket IMTP is down)");
            }
        }
    }
}

