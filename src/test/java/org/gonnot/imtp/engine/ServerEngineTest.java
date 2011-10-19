package org.gonnot.imtp.engine;
import jade.core.Node;
import jade.core.PlatformManager;
import java.util.concurrent.Semaphore;
import net.codjo.agent.test.AgentAssert.Assertion;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import org.gonnot.imtp.engine.ServerEngine.ServerWebSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static net.codjo.test.common.matcher.JUnitMatchers.*;
/**
 *
 */
public class ServerEngineTest {
    private ServerWebSocketMock serverWebSocketMock = new ServerWebSocketMock();
    private ServerEngine serverEngine;


    @Before
    public void setUp() throws Exception {
        serverEngine = new ServerEngine(serverWebSocketMock);
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
        serverWebSocketMock.mockReceivedCommand(myCommand);

        waitForServerReply();

        assertThat(serverWebSocketMock.sentResult(), is("result of my command"));
        assertThat(serverWebSocketMock.sentResult.getCommandId(), is(myCommand.getCommandId()));
    }


    @Test
    public void test_multipleCommand() throws Exception {
        fail("to be done");
    }


    @Test
    public void test_shutdown() throws Exception {
        fail("to be done");
    }


    @Test
    public void test_commandFailure() throws Exception {
        fail("to be done");
    }


    @Test
    public void test_commandParameter() throws Exception {
        fail("to be done");
    }


    @Test
    public void test_multiThreadExecution() throws Exception {
        fail("to be done");
    }


    @Test
    public void test_unknownCommand() throws Exception {
        fail("to be done");
    }


    @Test
    public void test_unableToSendResult() throws Exception {
        fail("to be done");
    }


    private void waitForServerReply() {
        ClientEngineTest.ensureThat(new Assertion() {
            public void check() throws Throwable {
                assertThat(serverWebSocketMock.sentResult(), is(notNullValue()));
            }
        });
    }


    private static class ServerWebSocketMock implements ServerWebSocket {
        private Semaphore waitForCommand = new Semaphore(0);
        Command command;
        Result sentResult;


        public void send(Result result) {
            sentResult = result;
        }


        public Command receive() throws InterruptedException {
            waitForCommand.acquire();
            return command;
        }


        public void mockReceivedCommand(Command mockedResponse) {
            waitForCommand.release();
            command = mockedResponse;
        }


        private String sentResult() {
            if (sentResult == null) {
                return null;
            }
            return (String)sentResult.getResult();
        }
    }
}
