package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.PlatformManager;
import java.io.IOException;
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


    public void shutDown() {
        closing = true;
        try {
            socketServer.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket server", e);
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
                Command command = CommandCodec.decode(webSocket.getMessage());
                webSocket.sendMessage(CommandCodec.encode(executeCommand(command)));
            }
        }


        private Result executeCommand(Command command) {
            try {
                return Result.value(command.execute(platformManager));
            }
            catch (Throwable e) {
                return Result.failure(e);
            }
        }
    }
}

