/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp;
import jade.core.PlatformManager;
import net.codjo.test.common.LogString;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.NetworkChannel;
import org.gonnot.imtp.command.Result;
import org.junit.Test;
import static net.codjo.test.common.matcher.JUnitMatchers.assertThat;
import static net.codjo.test.common.matcher.JUnitMatchers.is;

public class PlatformManagerProxyTest {
    private LogString log = new LogString();


    @Test
    public void test_getPlatformName() throws Exception {
        NetworkChannel network = new NetworkChannel() {
            @Override
            public Result send(Command command) {
                log.call("send", command.getClass().getSimpleName());
                return Result.value("a name");
            }
        };

        PlatformManager proxy = new PlatformManagerProxy(network);

        assertThat(proxy.getPlatformName(), is("a name"));
        log.assertContent("send(GetPlatformNameCommand)");
    }
}
