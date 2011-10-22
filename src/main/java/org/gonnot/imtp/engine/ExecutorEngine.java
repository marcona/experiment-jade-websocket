/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.engine;
import jade.core.Node;
import jade.core.PlatformManager;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
/**
 *
 */
public class ExecutorEngine {
    private static final Logger LOG = Logger.getLogger(ExecutorEngine.class);
    private static final int EXECUTOR_THREAD_MAX_COUNT = 5;
    private SocketReaderThread socketReaderThread = new SocketReaderThread();
    private WebSocketGlue webSocket;
    private PlatformManager platformManager;
    private Node node;
    private volatile boolean shutdownActivated = false;


    public ExecutorEngine(WebSocketGlue webSocket) {
        this.webSocket = webSocket;

        LOG.info("start executor engine for " + this.webSocket.getRemoteClientId());
        socketReaderThread.start();
    }


    public void init(PlatformManager localManager, Node localNode) {
        this.platformManager = localManager;
        this.node = localNode;
    }


    @SuppressWarnings({"ConstantConditions"})
    private void handleIncomingCommands(ExecutorService executorService) {
        while (!shutdownActivated) {
            try {
                final Command command = webSocket.receive();
                executorService.execute(handleOneCommand(command));
            }
            catch (Throwable error) {
                shutdown();
            }
        }
    }


    private Runnable handleOneCommand(final Command command) {
        return new Runnable() {
            public void run() {
                try {
                    webSocket.send(executeCommand(command));
                }
                catch (Throwable error) {
                    shutdown();
                }
            }
        };
    }


    private Result executeCommand(Command command) {
        try {
            Object result = command.execute(platformManager, node);
            return Result.value(result, command);
        }
        catch (Throwable e) {
            return Result.failure(e, command);
        }
    }


    public void shutdown() {
        if (shutdownActivated) {
            return;
        }
        LOG.info("stop executor engine of " + webSocket.getRemoteClientId());
        shutdownActivated = true;
        socketReaderThread.shutdownThreads();
    }


    boolean isShutdown() {
        return shutdownActivated;
    }


    Thread getSocketReaderThread() {
        return socketReaderThread;
    }


    public static interface WebSocketGlue {
        public String getRemoteClientId();


        public void send(Result result) throws IOException;


        public Command receive() throws InterruptedException, IOException;
    }
    private class SocketReaderThread extends Thread {
        private ExecutorService executorService;


        private SocketReaderThread() {
            super("executor-engine");
        }


        @Override
        public void run() {
            executorService = Executors.newFixedThreadPool(EXECUTOR_THREAD_MAX_COUNT);
            handleIncomingCommands(executorService);
        }


        void shutdownThreads() {
            executorService.shutdownNow();
            super.interrupt();
        }
    }
}
