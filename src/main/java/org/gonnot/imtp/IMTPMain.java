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
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import websocket4j.server.WebServerSocket;
import websocket4j.server.WebSocket;
import static org.gonnot.imtp.util.JadeExceptionUtil.imtpException;
/**
 *
 */
class IMTPMain implements Runnable {
    private static final Logger LOG = Logger.getLogger(IMTPMain.class);
    static final String PLATFORM_URI = "/platform";

    private PlatformManager platformManager;
    private WebServerSocket socketServer;
    private boolean closing;
    private Node localNode;
    private List<WebSocket> activeSockets = Collections.synchronizedList(new ArrayList<WebSocket>());


    IMTPMain(Node localNode) {
        this.localNode = localNode;
    }


    public void setPlatformManager(PlatformManager manager) {
        this.platformManager = manager;
    }


    public void start(String localHost, int port) throws IMTPException {
        try {
            socketServer = new WebServerSocket(port);
            new Thread(this).start();
        }
        catch (IOException e) {
            throw imtpException("Unable to start the WebSocket server", e);
        }
    }


    public void run() {
        try {
            handleClientContainerFirstConnection();
        }
        catch (Throwable e) {
            handleErrorDisplay(e, "WebSocket IMTP is closed", "Unexpected Error (WebSocket IMTP is down)");
        }
    }


    public void handleClientContainerFirstConnection() throws IOException {
        while (true) {
            WebSocket ws = socketServer.accept();
            if (PLATFORM_URI.equals(ws.getRequestUri())) {
                LOG.info("New connection request - GET " + ws.getRequestUri() + " from " + ws.getRemoteId());
                activeSockets.add(ws);
                (new PlatformManagerProxyReader(ws)).start();
            }
            else {
                LOG.warn("Unsupported request - GET " + ws.getRequestUri());
                silentClose(ws);
            }
        }
    }


    public void shutDown() {
        if (closing) {
            return;
        }
        LOG.info("Shutdown IMTP main servers and all active connections...");
        closing = true;
        try {
            socketServer.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket server", e);
        }
        finally {
            for (WebSocket socket : copyOf(activeSockets)) {
                silentClose(socket);
            }
        }
    }


    int getActiveConnectionCount() {
        return activeSockets.size();
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


    private static WebSocket[] copyOf(List<WebSocket> sockets) {
        return sockets.toArray(new WebSocket[sockets.size()]);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Multi-Threads glue
    // -----------------------------------------------------------------------------------------------------------------

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
                Command command = CommandCodec.decode(webSocket.getMessage());
                webSocket.sendMessage(CommandCodec.encode(executeCommand(command)));
            }
        }


        private Result executeCommand(Command command) {
            try {
                return Result.value(command.execute(platformManager, localNode), command);
            }
            catch (Throwable e) {
                return Result.failure(e);
            }
        }
    }
}

