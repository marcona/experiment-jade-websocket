package org.gonnot.imtp.engine;
import jade.core.Node;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import java.lang.Thread.State;
import java.util.Stack;
import java.util.concurrent.Semaphore;
import net.codjo.agent.test.AgentAssert.Assertion;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import org.gonnot.imtp.engine.ServerEngine.ServerWebSocket;
import org.gonnot.imtp.mock.NodeMock;
import org.gonnot.imtp.mock.PlatformManagerMock;
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
public class ServerEngineTest {
    private ServerWebSocketMock webSocket = new ServerWebSocketMock();
    private ServerEngine serverEngine;


    @Before
    public void setUp() throws Exception {
        serverEngine = new ServerEngine(webSocket);
    }


    @After
    public void tearDown() throws Exception {
        serverEngine.shutdown();
    }


    @Test
    public void test_simpleCommand() throws Exception {
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
    public void test_commandParameter() throws Exception {
        serverEngine.init(new PlatformManagerMock(), new NodeMock("local"));

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
    public void test_multipleCommand() throws Exception {
        webSocket.pushCommand(new DummyCommand(10));
        webSocket.pushCommand(new DummyCommand(20));

        waitForServerReply(2);

        assertThat(webSocket.resultForCommand(10), is("resultOf(command[10].execute(...))"));
        assertThat(webSocket.resultForCommand(20), is("resultOf(command[20].execute(...))"));
    }


    @Test
    public void test_shutdown() throws Exception {
        assertTrue(threadStateIS(serverEngine.getSocketReaderWriter(), State.WAITING));

        serverEngine.shutdown();

        assertTrue(threadStateIS(serverEngine.getSocketReaderWriter(), State.TERMINATED));
    }


    @Test
    public void test_commandFailure() throws Exception {
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
    public void test_multiThreadExecution() throws Exception {
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


    private static class ServerWebSocketMock implements ServerWebSocket {
        private Semaphore waitForCommand = new Semaphore(0);
        private Stack<Command> commands = new Stack<Command>();
        private Stack<Result> sentResults = new Stack<Result>();


        public void send(Result result) {
            sentResults.insertElementAt(result, 0);
        }


        public Command receive() throws InterruptedException {
            waitForCommand.acquire();
            return commands.pop();
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
    }
}
