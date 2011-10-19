package org.gonnot.imtp.engine;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import org.apache.log4j.Logger;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
/**
 *
 */
public class ServerEngine {
    private static final Logger LOG = Logger.getLogger(ServerEngine.class);
    private WebSocketReaderWriter socketReaderWriter = new WebSocketReaderWriter();
    private ServerWebSocket serverWebSocket;


    public ServerEngine(ServerWebSocket serverWebSocket) {
        this.serverWebSocket = serverWebSocket;
        socketReaderWriter.start();
    }


    public void shutdown() {
        LOG.info("shutdown IMTP ServerEngine...");
        // Todo
    }


    public static interface ServerWebSocket {
        public void send(Result result);


        public Command receive() throws InterruptedException;
    }
    private class WebSocketReaderWriter extends Thread {
        private WebSocketReaderWriter() {
            super("WebSocketReaderWriter");
        }


        @Override
        public void run() {
            Command receive = null;
            try {
                receive = serverWebSocket.receive();
                Object commandResult = receive.execute(null, null);
                serverWebSocket.send(Result.value(commandResult, receive.getCommandId()));
            }
            catch (InterruptedException e) {
                e.printStackTrace();  // Todo
            }
            catch (JADESecurityException e) {
                e.printStackTrace();  // Todo
            }
            catch (IMTPException e) {
                e.printStackTrace();  // Todo
            }
            catch (ServiceException e) {
                e.printStackTrace();  // Todo
            }
        }
    }
}
