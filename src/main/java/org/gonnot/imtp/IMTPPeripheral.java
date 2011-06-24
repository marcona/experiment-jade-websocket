package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.PlatformManager;
import java.io.IOException;
import websocket4j.client.WebSocket;
/**
 *
 */
class IMTPPeripheral {
    private PlatformManager platformManagerProxy;
    private WebSocket socketClient;


    IMTPPeripheral() {
    }


    public PlatformManager getPlatformManagerProxy() {
        return platformManagerProxy;
    }


    public void start(String host, int port) throws IMTPException {
        try {
            socketClient = new WebSocket(host, port, IMTPMain.PLATFORM_URI);
            platformManagerProxy = new PlatformManagerProxy(new NetworkWSImpl());
        }
        catch (IOException e) {
            throw new IMTPException("Unable to start the WebSocket client", e);
        }
    }


    public void shutDown() {
        try {
            socketClient.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket client", e);
        }
    }


    private class NetworkWSImpl implements NetworkWS {
        public String synchronousCall(GetPlatformNameCommand command) throws IOException {
            socketClient.sendMessage("not used");
            return socketClient.getMessage();
        }
    }
}
