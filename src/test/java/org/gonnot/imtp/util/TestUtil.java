/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.util;
import java.lang.Thread.State;
import java.util.concurrent.Semaphore;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.AgentAssert.Assertion;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.test.common.LogString;
import static net.codjo.test.common.matcher.JUnitMatchers.assertThat;
import static net.codjo.test.common.matcher.JUnitMatchers.is;
/**
 *
 */
public class TestUtil {
    private TestUtil() {
    }


    public static String className(Object object) {
        return object != null ? object.getClass().getSimpleName() : "n/a";
    }


    public static void assertTrue(AgentAssert.Assertion assertion) {
        // TODO[BORIS] create static method in Agent
        new AgentContainerFixture().assertUntilOk(assertion);
    }


    public static Assertion threadStateIS(final Thread thread, final State state) {
        return new Assertion() {
            public void check() throws Throwable {
                assertThat(thread.getState(), is(state));
            }
        };
    }


    public static Assertion logStringIs(final LogString log, final String expected) {
        return new Assertion() {
            public void check() throws Throwable {
                assertThat(log.getContent(), is(expected));
            }
        };
    }


    public static void acquire(Semaphore semaphore) {
        try {
            semaphore.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Unexpected InterruptedException");
        }
    }


    public static void sleep(int waitTime) {
        try {
            Thread.sleep(waitTime);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
