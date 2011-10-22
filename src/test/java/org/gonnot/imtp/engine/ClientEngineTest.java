package org.gonnot.imtp.engine;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.lang.Thread.State;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import org.gonnot.imtp.engine.ClientEngine.ClientWebSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static net.codjo.test.common.matcher.JUnitMatchers.assertThat;
import static net.codjo.test.common.matcher.JUnitMatchers.is;
import static org.gonnot.imtp.util.TestUtil.assertTrue;
import static org.gonnot.imtp.util.TestUtil.threadStateIS;
public class ClientEngineTest {
    @SuppressWarnings({"PublicField"}) @Rule public ExpectedException thrown = ExpectedException.none();
    private ClientEngine clientEngine;
    private ClientWebSocketMock channelMock;
    private ExecutorService executors;


    @Before
    public void setUp() throws Exception {
        channelMock = new ClientWebSocketMock();
        clientEngine = new ClientEngine(channelMock);
    }


    @After
    public void tearDown() throws Exception {
        if (executors != null) {
            executors.shutdown();
        }
        clientEngine.shutdown();
    }


    @Test
    public void test_simpleRequest() throws Exception {
        Command<String> command = aCommand();

        mockServerResponse("answer from server", command);

        String result = clientEngine.execute(command);

        assertThat(result, is("answer from server"));
    }


    @Test
    public void test_serverResultCanBeUpdated() throws Exception {
        DummyCommand command = new DummyCommand() {
            @Override
            public String handle(ClientWebSocket clientWebSocket, String result) {
                return result + " - updated on the client side";
            }
        };

        mockServerResponse("answer from server", command);

        String result = clientEngine.execute(command);

        assertThat(result, is("answer from server - updated on the client side"));
    }


    @Test
    public void test_commandExecutionAreMultiThreaded() throws Exception {
        executors = Executors.newFixedThreadPool(2);

        Future<String> firstResult = executors.submit(commandExecution(aCommand(1)));
        Future<String> secondResult = executors.submit(commandExecution(aCommand(2)));

        mockServerResponse(Result.value("answer 2", 2));
        mockServerResponse(Result.value("answer 1", 1));

        assertThat(firstResult.get(), is("answer 1"));
        assertThat(secondResult.get(), is("answer 2"));
    }


    private Callable<String> commandExecution(final Command<String> command) {
        return new Callable<String>() {
            public String call() throws Exception {
                return clientEngine.execute(command);
            }
        };
    }


    @Test
    public void test_shutdownRunner() throws Exception {
        thrown.expect(IMTPException.class);

        thrown.expectMessage("IMTP connection has been shutdown.");

        clientEngine.shutdown();

        assertTrue(threadStateIS(clientEngine.getWebSocketReader(), State.TERMINATED));

        clientEngine.execute(aCommand());
    }


    @Test
    public void test_errorIMTPExceptionCase() throws Exception {
        checkErrorManagementFor(new IMTPException("Server Error"));
    }


    @Test
    public void test_errorServiceExceptionCase() throws Exception {
        checkErrorManagementFor(new ServiceException("Server Error"));
    }


    @Test
    public void test_errorJADESecurityExceptionCase() throws Exception {
        checkErrorManagementFor(new JADESecurityException("Server Error"));
    }


    @Test
    public void test_errorUnexpectedExceptionCase() throws Exception {
        checkErrorManagementFor(new NullPointerException("Server Error"),
                                IMTPException.class, "Unexpected server error");
    }


    private void checkErrorManagementFor(Throwable error) throws Exception {
        checkErrorManagementFor(error, error.getClass(), error.getMessage());
    }


    private void checkErrorManagementFor(Throwable error,
                                         Class<? extends Throwable> resultingError,
                                         String resultingMessage) throws Exception {
        Command<String> command = aCommand();
        mockServerResponse(Result.failure(error, command));

        thrown.expect(resultingError);
        thrown.expectMessage(resultingMessage);

        clientEngine.execute(command);
    }


    private void mockServerResponse(Result response) {
        channelMock.mockResponse(response);
    }


    private void mockServerResponse(String response, Command<String> command) {
        channelMock.mockResponse(Result.value(response, command.getCommandId()));
    }


    private Command<String> aCommand() {
        return new DummyCommand();
    }


    private Command<String> aCommand(final int id) {
        return new DummyCommand(id);
    }


    private class ClientWebSocketMock implements ClientWebSocket {
        private Semaphore waitCommandSent = new Semaphore(0, true);
        private Semaphore waitResults = new Semaphore(0);
        private Stack<Result> results = new Stack<Result>();


        public void send(Command command) {
            waitCommandSent.release();
        }


        public Result receive() throws InterruptedException {
            waitCommandSent.acquire();
            waitResults.acquire();
            return results.pop();
        }


        public void mockResponse(Result mockedResponse) {
            results.insertElementAt(mockedResponse, 0);
            waitResults.release();
        }
    }
}
