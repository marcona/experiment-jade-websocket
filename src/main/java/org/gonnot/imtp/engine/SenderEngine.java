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
public class SenderEngine {
    private static final Logger LOG = Logger.getLogger(SenderEngine.class);
    private WebSocketGlue webSocket;
    private WebSocketReader webSocketReader = new WebSocketReader();
    private Map<Integer, ResultPointer> activeCommands
          = Collections.synchronizedMap(new HashMap<Integer, ResultPointer>());


    public SenderEngine(WebSocketGlue webSocket) {
        this.webSocket = webSocket;
        LOG.info("start IMTP ClientEngine...");
        webSocketReader.start();
    }


    public <T> T execute(Command<T> command)
          throws InterruptedException, IMTPException, ServiceException, JADESecurityException {
        if (webSocketReader.shutdownActivated) {
            throw new IMTPException("IMTP connection has been shutdown.");
        }

        ResultPointer resultPointer = new ResultPointer();
        activeCommands.put(command.getCommandId(), resultPointer);
        webSocket.send(command);

        resultPointer.acquire();

        activeCommands.remove(command.getCommandId());
        Result result = resultPointer.getResult();

        if (result.hasFailed()) {
            throwFailure(result.getFailure());
        }

        //noinspection unchecked
        return command.handle(this.webSocket, (T)result.getResult());
    }


    public void shutdown() {
        LOG.info("shutdown IMTP ClientEngine...");
        webSocketReader.shutdown();
    }


    Thread getWebSocketReader() {
        return webSocketReader;
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
                    Result result = webSocket.receive();

                    ResultPointer resultPointer = activeCommands.get(result.getCommandId());
                    if (resultPointer == null) {
                        LOG.error("Received a result for an unknown request(" + result.getCommandId() + ") " + result);
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
    public static interface WebSocketGlue {
        public void send(Command command);


        public Result receive() throws InterruptedException;
    }
}
