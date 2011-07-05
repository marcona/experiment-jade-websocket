package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.PlatformManager;
import java.io.IOException;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.NetworkChannel;
import org.gonnot.imtp.command.Result;
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
            platformManagerProxy = new PlatformManagerProxy(new NetworkChannelImpl());
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


    private class NetworkChannelImpl extends NetworkChannel {
        @Override
        public Result send(Command command) throws IMTPException {
            try {
                socketClient.sendMessage(CommandCodec.encode(command));
                String encodedResult = socketClient.getMessage();
                return CommandCodec.decodeResult(encodedResult);
            }
            catch (IOException cause) {
                throw new IMTPException("Communication error", cause);
            }
        }
    }
}
