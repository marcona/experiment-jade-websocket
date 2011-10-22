/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.engine;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.io.IOException;
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
    private WebSocketResultReader resultReader = new WebSocketResultReader();
    private volatile boolean shutdownActivated = false;
    private Map<Integer, ResultPointer> activeCommands
          = Collections.synchronizedMap(new HashMap<Integer, ResultPointer>());


    public SenderEngine(WebSocketGlue webSocket) {
        this.webSocket = webSocket;
        LOG.info("start command sender engine to " + webSocket.getRemoteId());
        resultReader.start();
    }


    public <T> T execute(Command<T> command)
          throws InterruptedException, IMTPException, ServiceException, JADESecurityException {
        if (shutdownActivated) {
            throw new IMTPException("IMTP connection has been shutdown.");
        }

        ResultPointer resultPointer = openAsActiveCommand(command);

        webSocket.send(command);
        resultPointer.acquire();

        Result result = closeAsActiveCommand(command, resultPointer);

        if (result.hasFailed()) {
            throwFailure(result.getFailure());
        }

        //noinspection unchecked
        return command.handle(this.webSocket, (T)result.getResult());
    }


    private void handleIncomingResults() {
        try {
            while (!shutdownActivated) {
                Result result = webSocket.receive();

                handleResult(result);
            }
        }
        catch (Throwable e) {
            if (e instanceof InterruptedException) {
                // TODO specific management
                LOG.warn("Unexpected thread interruption...");
            }
            else {
                LOG.error("Unexpected error...", e);
            }

            shutdown();
        }
    }


    private void handleResult(Result result) {
        ResultPointer resultPointer = activeCommands.get(result.getCommandId());
        if (resultPointer == null) {
            LOG.error("Received a result for an unknown request(" + result.getCommandId() + ") " + result);
            return;
        }

        resultPointer.setResult(result);
        resultPointer.release();
    }


    public void shutdown() {
        if (isShutdown()) {
            return;
        }
        LOG.info("stop command sender engine to " + webSocket.getRemoteId());
        shutdownActivated = true;
        // TODO propagate interrupted exception to all active commands.
        resultReader.shutdownResultReader();
    }


    boolean isShutdown() {
        return shutdownActivated;
    }


    Thread getResultReader() {
        return resultReader;
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


    private <T> Result closeAsActiveCommand(Command<T> command, ResultPointer resultPointer) {
        activeCommands.remove(command.getCommandId());
        return resultPointer.getResult();
    }


    private <T> ResultPointer openAsActiveCommand(Command<T> command) {
        ResultPointer resultPointer = new ResultPointer();
        activeCommands.put(command.getCommandId(), resultPointer);
        return resultPointer;
    }


    private class WebSocketResultReader extends Thread {

        private WebSocketResultReader() {
            super("result-reader");
        }


        @Override
        public void run() {
            handleIncomingResults();
        }


        public void shutdownResultReader() {
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


        public Result receive() throws InterruptedException, IOException;


        String getRemoteId();
    }
}
