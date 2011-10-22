package org.gonnot.imtp.engine;
import jade.core.Node;
import jade.core.PlatformManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
/**
 *
 */
public class CommandExecutorEngine {
    private static final Logger LOG = Logger.getLogger(CommandExecutorEngine.class);
    private static final int EXECUTOR_THREAD_MAX_COUNT = 5;
    private WebSocketReaderWriter socketReaderWriter = new WebSocketReaderWriter();
    private WebSocketGlue webSocket;
    private PlatformManager platformManager;
    private Node node;


    public CommandExecutorEngine(WebSocketGlue webSocket) {
        this.webSocket = webSocket;

        LOG.info("bootstrap IMTP ServerEngine...");
        socketReaderWriter.start();
    }


    public void shutdown() {
        LOG.info("shutdown IMTP ServerEngine...");
        socketReaderWriter.shutdown();
    }


    Thread getSocketReaderWriter() {
        return socketReaderWriter;
    }


    public void init(PlatformManager localManager, Node localNode) {
        this.platformManager = localManager;
        this.node = localNode;
    }


    public static interface WebSocketGlue {
        public void send(Result result);


        public Command receive() throws InterruptedException;
    }
    private class WebSocketReaderWriter extends Thread {
        private volatile boolean shutdownActivated = false;
        private ExecutorService executorService;


        private WebSocketReaderWriter() {
            super("WebSocketReaderWriter");
        }


        @Override
        public void run() {
            executorService = Executors.newFixedThreadPool(EXECUTOR_THREAD_MAX_COUNT);
            while (!shutdownActivated) {
                Command command = null;
                try {
                    command = webSocket.receive();
                    executorService.execute(new CommandExecutor(command));
                }
                catch (Throwable error) {
                    if (shutdownActivated) {
                        return;
                    }
                    webSocket.send(Result.failure(error, command));
                }
            }
        }


        public void shutdown() {
            shutdownActivated = true;
            executorService.shutdown();
            super.interrupt();
        }


        private class CommandExecutor implements Runnable {
            private Command command;


            CommandExecutor(Command command) {
                this.command = command;
            }


            public void run() {
                try {
                    Object commandResult = command.execute(platformManager, node);
                    webSocket.send(Result.value(commandResult, command.getCommandId()));
                }
                catch (Throwable error) {
                    if (shutdownActivated) {
                        return;
                    }
                    webSocket.send(Result.failure(error, command));
                }
            }
        }
    }
}
