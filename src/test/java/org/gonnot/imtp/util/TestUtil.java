package org.gonnot.imtp.util;
import java.lang.Thread.State;
import java.util.concurrent.Semaphore;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.AgentAssert.Assertion;
import net.codjo.agent.test.AgentContainerFixture;
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


    public static void acquire(Semaphore semaphore) {
        try {
            semaphore.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Unexpected InterruptedException");
        }
    }
}