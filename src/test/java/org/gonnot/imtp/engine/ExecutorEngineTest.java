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
import jade.core.ServiceException;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.Stack;
import java.util.concurrent.Semaphore;
import net.codjo.agent.test.AgentAssert.Assertion;
import net.codjo.test.common.LogString;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import org.gonnot.imtp.engine.ExecutorEngine.WebSocketGlue;
import org.gonnot.imtp.mock.NodeMock;
import org.gonnot.imtp.mock.PlatformManagerMock;
import org.gonnot.imtp.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static net.codjo.test.common.matcher.JUnitMatchers.assertThat;
import static net.codjo.test.common.matcher.JUnitMatchers.is;
import static net.codjo.test.common.matcher.JUnitMatchers.notNullValue;
import static org.gonnot.imtp.util.TestUtil.acquire;
import static org.gonnot.imtp.util.TestUtil.assertTrue;
import static org.gonnot.imtp.util.TestUtil.className;
import static org.gonnot.imtp.util.TestUtil.threadStateIS;
/**
 *
 */
public class ExecutorEngineTest {
    private WebSocketGlueMock webSocket = new WebSocketGlueMock();
    private ExecutorEngine executorEngine;


    @Before
    public void setUp() throws Exception {
        executorEngine = new ExecutorEngine(webSocket);
    }


    @After
    public void tearDown() throws Exception {
        executorEngine.shutdown();
    }


    @Test
    public void test_commandExecution() throws Exception {
        Command myCommand = new Command<String>() {
            @Override
            public String execute(PlatformManager platformManager, Node localNode) {
                return "result of my command";
            }
        };
        webSocket.pushCommand(myCommand);

        waitForServerReply();

        assertThat(webSocket.lastSentResult(), is("result of my command"));
        assertThat(webSocket.resultForCommand(myCommand), is(notNullValue()));
    }


    @Test
    public void test_parametersCanBeUsedDuringCommandExecution() throws Exception {
        executorEngine.init(new PlatformManagerMock(), new NodeMock("local"));

        webSocket.pushCommand(new Command<String>() {
            @Override
            public String execute(PlatformManager platformManager, Node localNode) {
                return "result using parameters (" + className(platformManager) + ", " + className(localNode) + ")";
            }
        });

        waitForServerReply();

        assertThat(webSocket.lastSentResult(), is("result using parameters (PlatformManagerMock, NodeMock)"));
    }


    @Test
    public void test_commandExecutionAreMultiThreaded() throws Exception {
        webSocket.pushCommand(new DummyCommand(10));
        webSocket.pushCommand(new DummyCommand(20));

        waitForServerReply(2);

        assertThat(webSocket.resultForCommand(10), is("resultOf(command[10].execute(...))"));
        assertThat(webSocket.resultForCommand(20), is("resultOf(command[20].execute(...))"));
    }


    @Test
    public void test_commandExecutionAreMultiThreaded_bug() throws Exception {
        final Semaphore semaphore = new Semaphore(0);

        Command<String> firstCommandWaitForSecond = new Command<String>() {
            @Override
            public String execute(PlatformManager platformManager, Node localNode) {
                acquire(semaphore);
                return "1";
            }
        };
        Command<String> secondCommand = new Command<String>() {
            @Override
            public String execute(PlatformManager platformManager, Node localNode) {
                semaphore.release();
                return "2";
            }
        };

        webSocket.pushCommand(firstCommandWaitForSecond);
        webSocket.pushCommand(secondCommand);

        waitForServerReply(2);

        assertThat(webSocket.resultForCommand(firstCommandWaitForSecond), is("1"));
        assertThat(webSocket.resultForCommand(secondCommand), is("2"));
    }


    @Test
    public void test_commandFailureArePropagated() throws Exception {
        webSocket.pushCommand(new Command<String>() {
            @Override
            public String execute(PlatformManager platformManager, Node localNode) throws ServiceException {
                throw new ServiceException("I failed...");
            }
        });

        waitForServerReply();

        assertThat(webSocket.lastResult().hasFailed(), is(true));
        assertThat(webSocket.lastResult().getFailure().getMessage(), is("I failed..."));
    }


    @Test
    public void test_shutdown_stopsThreadsAndWebsocketGlue() throws Exception {
        assertTrue(threadStateIS(executorEngine.getSocketReaderThread(), State.WAITING));

        executorEngine.shutdown();

        assertTrue(threadStateIS(executorEngine.getSocketReaderThread(), State.TERMINATED));
        assertThat(executorEngine.isShutdown(), is(true));
        assertThat(webSocket.isClosed(), is(true));
    }


    @Test
    public void test_shutdown_stopsCurrentCommandExecution() throws Exception {
        final LogString logString = new LogString();
        final Semaphore semaphore = new Semaphore(0);

        webSocket.pushCommand(new Command<String>() {
            @Override
            public String execute(PlatformManager platformManager, Node localNode) {
                logString.info("start running...");
                semaphore.release();
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    logString.info("...but interrupted");
                    return null;
                }
                logString.info("should never be displayed (executors are destroyed)");
                return null;
            }
        });

        semaphore.acquire();
        executorEngine.shutdown();

        assertTrue(TestUtil.logStringIs(logString, "start running..., ...but interrupted"));
    }


    @Test
    public void test_shutdown_isTriggeredByWebsocketReadingError() throws Exception {
        webSocket.pushFailureDuringReadingCommand(new IOException("mock socket closure"));

        assertTrue(threadStateIS(executorEngine.getSocketReaderThread(), State.TERMINATED));
        assertThat(executorEngine.isShutdown(), is(true));
    }


    @Test
    public void test_shutdown_isTriggeredByWebsocketWritingError() throws Exception {
        webSocket.pushFailureDuringResultPost(new IOException("mock socket closure"));

        webSocket.pushCommand(new DummyCommand());

        assertTrue(threadStateIS(executorEngine.getSocketReaderThread(), State.TERMINATED));
        assertThat(executorEngine.isShutdown(), is(true));
    }


    private void waitForServerReply() {
        waitForServerReply(1);
    }


    private void waitForServerReply(final int replyCount) {
        assertTrue(new Assertion() {
            public void check() throws Throwable {
                assertThat(webSocket.sentResultCount(), is(replyCount));
            }
        });
    }


    private static class WebSocketGlueMock implements WebSocketGlue {
        private Semaphore waitForCommand = new Semaphore(0);
        private Stack<Command> commands = new Stack<Command>();
        private Stack<Result> sentResults = new Stack<Result>();
        private IOException socketFailure;
        private boolean sendFailure = false;
        private boolean receiveFailure = false;
        private boolean closed;


        public String getRemoteClientId() {
            return "/127.0.0.1:52686";
        }


        public void send(Result result) throws IOException {
            if (sendFailure) {
                throw socketFailure;
            }
            sentResults.insertElementAt(result, 0);
        }


        public Command receive() throws InterruptedException, IOException {
            waitForCommand.acquire();
            if (receiveFailure) {
                throw socketFailure;
            }
            return commands.pop();
        }


        public void close() {
            closed = true;
        }


        public void pushCommand(Command mockedResponse) {
            commands.insertElementAt(mockedResponse, 0);
            waitForCommand.release();
        }


        private String lastSentResult() {
            if (sentResultCount() == 0) {
                return null;
            }
            return (String)sentResults.peek().getResult();
        }


        private Result lastResult() {
            if (sentResultCount() == 0) {
                return null;
            }
            return sentResults.peek();
        }


        public int sentResultCount() {
            return sentResults.size();
        }


        public String resultForCommand(int commandId) {
            for (Result result : sentResults) {
                if (result.getCommandId() == commandId) {
                    return (String)result.getResult();
                }
            }
            return null;
        }


        public String resultForCommand(Command command) {
            return resultForCommand(command.getCommandId());
        }


        public void pushFailureDuringReadingCommand(IOException failure) {
            receiveFailure = true;
            this.socketFailure = failure;
            waitForCommand.release();
        }


        public void pushFailureDuringResultPost(IOException failure) {
            sendFailure = true;
            this.socketFailure = failure;
        }


        public boolean isClosed() {
            return closed;
        }
    }
}
