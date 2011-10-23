/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */
package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.io.IOException;
import java.net.SocketException;
import net.codjo.agent.test.AgentAssert.Assertion;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
import org.gonnot.imtp.engine.DummyCommand;
import org.gonnot.imtp.mock.NodeMock;
import org.gonnot.imtp.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import websocket4j.client.WebSocket;
import static net.codjo.test.common.matcher.JUnitMatchers.assertThat;
import static net.codjo.test.common.matcher.JUnitMatchers.is;
import static org.gonnot.imtp.CommandCodec.encode;
import static org.gonnot.imtp.util.TestUtil.assertTrue;
/**
 *
 */
public class IMTPMainTest {
    @SuppressWarnings({"PublicField"}) @Rule public ExpectedException thrown = ExpectedException.none();
    private WebSocket clientWebSocket;
    private IMTPMain imtp;
    private DummyCommand dummyCommand = new DummyCommand(10);


    @Before
    public void setUp() throws Exception {
        NodeMock node = new NodeMock("local-server-node");
        imtp = new IMTPMain(node);

        imtp.start("unused", TestUtil.SERVER_PORT);
    }


    @After
    public void tearDown() throws Exception {
        try {
            stopClient();
        }
        finally {
            if (imtp != null) {
                imtp.shutDown();
            }
        }
    }


    @Test
    public void test_nominalCase() throws Exception {
        startClient();

        clientWebSocket.sendMessage(encode(dummyCommand));

        String result = clientWebSocket.getMessage();

        assertThat(result, is(encode(resultOf(dummyCommand))));
    }


    @Test
    public void test_activeConnectionsUpdatedWhen_startConnection() throws Exception {
        assertTrue(activeConnectionCountIs(0));

        startClient();

        assertTrue(activeConnectionCountIs(1));
    }


    @Test
    public void test_activeConnectionsUpdatedWhen_stopConnection() throws Exception {
        startClient();

        assertTrue(activeConnectionCountIs(1));

        stopClient();

        assertTrue(activeConnectionCountIs(0));
    }


    @Test
    public void test_activeConnectionsUpdatedWhen_shutdownServer() throws Exception {
        startClient();

        assertTrue(activeConnectionCountIs(1));

        imtp.shutDown();

        assertTrue(activeConnectionCountIs(0));
    }


    @Test
    public void test_shutdown_triggeredByBadCommandMessageFormat() throws Exception {
        startClient();

        clientWebSocket.sendMessage("message");
        String result = clientWebSocket.getMessage();

        assertThat(result, is("a result"));
    }


    @Test
    public void test_shutdown_closeAllActiveConnection() throws Exception {
        thrown.expect(SocketException.class);
        thrown.expectMessage("Broken pipe");

        startClient();

        imtp.shutDown();

        clientWebSocket.sendMessage("message not sent because connection is closed");
    }


    private void startClient() throws IOException {
        clientWebSocket = new WebSocket("localhost", TestUtil.SERVER_PORT, IMTPMain.PLATFORM_URI);
    }


    private void stopClient() throws IOException {
        clientWebSocket.close();
    }


    private Assertion activeConnectionCountIs(final int expected) {
        return new Assertion() {
            public void check() throws Throwable {
                assertThat(imtp.getActiveConnectionCount(), is(expected));
            }
        };
    }


    private static Result resultOf(Command command) throws IMTPException, JADESecurityException, ServiceException {
        return Result.value(command.execute(null, null), command.getCommandId());
    }
}
