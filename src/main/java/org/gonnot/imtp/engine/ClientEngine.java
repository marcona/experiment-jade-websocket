package org.gonnot.imtp.engine;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.apache.log4j.Logger;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import static org.gonnot.imtp.util.JadeExceptionUtil.imtpException;
/**
 *
 */
public class ClientEngine {
    private static final Logger LOG = Logger.getLogger(ClientEngine.class);
    private ClientWebSocket clientWebSocket;
    private WebSocketReader websockReader = new WebSocketReader();
    private Map<Integer, ResultPointer> activeCommands
          = Collections.synchronizedMap(new HashMap<Integer, ResultPointer>());


    public ClientEngine(ClientWebSocket clientWebSocket) {
        this.clientWebSocket = clientWebSocket;
        LOG.info("start IMTP ClientEngine...");
        websockReader.start();
    }


    public <T> T execute(Command<T> command)
          throws InterruptedException, IMTPException, ServiceException, JADESecurityException {
        if (websockReader.shutdownActivated) {
            throw new IMTPException("IMTP connection has been shutdown.");
        }

        ResultPointer resultPointer = new ResultPointer();
        activeCommands.put(command.getCommandId(), resultPointer);
        clientWebSocket.send(command);

        resultPointer.acquire();

        Result result = resultPointer.getResult();

        if (result.hasFailed()) {
            throwFailure(result.getFailure());
        }

        //noinspection unchecked
        return command.handle(this.clientWebSocket, (T)result.getResult());
    }


    public void shutdown() {
        LOG.info("shutdown IMTP ClientEngine...");
        websockReader.shutdown();
    }


    Thread getWebsockReader() {
        return websockReader;
    }


    private void throwFailure(Throwable failure) throws IMTPException, ServiceException, JADESecurityException {
        if (failure instanceof IMTPException) {
            throw (IMTPException)failure;
        }
        if (failure instanceof ServiceException) {
            throw (ServiceException)failure;
        }
        if (failure instanceof JADESecurityException) {
            throw (JADESecurityException)failure;
        }
        throw imtpException("Unexpected server error", failure);
    }


    private class WebSocketReader extends Thread {
        private volatile boolean shutdownActivated = false;


        private WebSocketReader() {
            super("WebSocketReader");
        }


        @Override
        public void run() {
            try {
                while (!shutdownActivated) {
                    Result result = clientWebSocket.receive();

                    ResultPointer resultPointer = activeCommands.get(result.getCommandId());
                    if (resultPointer == null) {
                        LOG.error("Received a result for an unknown request : " + result);
                        continue;
                    }

                    resultPointer.setResult(result);
                    resultPointer.release();
                }
            }
            catch (Throwable e) {
                if (shutdownActivated) {
                    return;
                }
                LOG.info("Unexpected error...", e);
            }
        }


        public void shutdown() {
            shutdownActivated = true;
            super.interrupt();
        }
    }
    private static class ResultPointer {
        private Semaphore semaphore = new Semaphore(0);
        private Result result;


        private ResultPointer() {
        }


        public void setResult(Result result) {
            this.result = result;
        }


        public Result getResult() {
            return result;
        }


        public void acquire() throws InterruptedException {
            semaphore.acquire();
        }


        public void release() {
            semaphore.release();
        }
    }
    public static interface ClientWebSocket {
        public void send(Command command);


        public Result receive() throws InterruptedException;
    }
}
