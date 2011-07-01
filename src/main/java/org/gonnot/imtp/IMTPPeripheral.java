package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.io.IOException;
import org.gonnot.imtp.command.Command;
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
        public String synchronousCall(Command command)
              throws IOException, IMTPException, ServiceException, JADESecurityException {
            socketClient.sendMessage(CommandCodec.encode(command));
            String encodedResult = socketClient.getMessage();
            Result result = CommandCodec.decodeResult(encodedResult);
            if (result.hasFailed()) {
                Throwable failure = result.getFailure();
                if (failure instanceof IMTPException) {
                    throw (IMTPException)failure;
                }
                if (failure instanceof ServiceException) {
                    throw (ServiceException)failure;
                }
                if (failure instanceof JADESecurityException) {
                    throw (JADESecurityException)failure;
                }
                throw new IMTPException("Unexpected server error", failure);
            }
            return result.getResult();
        }
    }
}
