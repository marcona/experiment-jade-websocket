/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import org.junit.Test;
import static net.codjo.test.common.matcher.JUnitMatchers.assertThat;
import static net.codjo.test.common.matcher.JUnitMatchers.fail;
import static net.codjo.test.common.matcher.JUnitMatchers.is;
import static org.gonnot.imtp.command.CommandFactory.getPlatformName;
/**
 *
 */
public class NetworkChannelTest {
    @Test
    public void testExecute() throws Exception {
        NetworkChannel networkChannel = new NetworkChannel() {
            @Override
            public Result send(Command command) {
                return Result.value("a name");
            }
        };

        assertThat(networkChannel.execute(simpleCommand()), is("a name"));
    }


    @Test(expected = IMTPException.class)
    public void testUnexpectedFailure() throws Exception {
        failingNetworkChannelWith(new NullPointerException("")).execute(simpleCommand());
    }


    @Test(expected = IMTPException.class)
    public void testIMTPFailure() throws Exception {
        failingNetworkChannelWith(new IMTPException("")).execute(simpleCommand());
    }


    @Test(expected = JADESecurityException.class)
    public void testSecurityFailure() throws Exception {
        failingNetworkChannelWith(new JADESecurityException("")).execute(simpleCommand());
    }


    @Test(expected = ServiceException.class)
    public void testServiceFailure() throws Exception {
        failingNetworkChannelWith(new ServiceException("")).execute(simpleCommand());
    }


    @Test
    public void testDefaultCatchByExecute2() throws Exception {
        try {
            failingNetworkChannelWith(new JADESecurityException("error cause")).execute2(simpleCommand());
            fail();
        }
        catch (IMTPException ex) {
            assertException(ex, "JADE Security", "jade.security.JADESecurityException", "error cause");
        }
    }


    @Test
    public void testDefaultCatchByExecute3() throws Exception {
        try {
            failingNetworkChannelWith(new JADESecurityException("error cause")).execute3(simpleCommand());
            fail();
        }
        catch (IMTPException ex) {
            assertException(ex, "JADE Security", "jade.security.JADESecurityException", "error cause");
        }

        try {
            failingNetworkChannelWith(new ServiceException("error cause")).execute3(simpleCommand());
            fail();
        }
        catch (IMTPException ex) {
            assertException(ex, "Service", "jade.core.ServiceException", "error cause");
        }
    }


    private static NetworkChannel failingNetworkChannelWith(final Throwable failure) {
        return new NetworkChannel() {
            @Override
            public Result send(Command command) {
                return Result.failure(failure);
            }
        };
    }


    private static void assertException(IMTPException ex,
                                        String exceptionType,
                                        String exceptionClass,
                                        String exceptionMessage) {
        assertThat(ex.getMessage(), is("Unexpected " + exceptionType + " Exception"
                                       + " [nested " + exceptionClass + ": " + exceptionMessage + "]"));
        assertThat(ex.getNested().getMessage(), is(exceptionMessage));
        assertThat(ex.getCause().getMessage(), is(exceptionMessage));
    }


    private static Command<String> simpleCommand() {
        return getPlatformName();
    }
}
