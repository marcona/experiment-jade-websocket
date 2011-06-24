package org.gonnot.imtp;
import com.agf.test.common.LogString;
import jade.core.PlatformManager;
import org.junit.Test;

import static com.agf.test.common.matcher.JUnitMatchers.*;

public class PlatformManagerProxyTest {
    private LogString log = new LogString();


    @Test
    public void test_getPlatformName() throws Exception {
        NetworkWS network = new NetworkWS() {
            public String synchronousCall(GetPlatformNameCommand command) {
                log.call("synchronousCall", command.getClass().getSimpleName());
                return "a name";
            }
        };

        PlatformManager proxy = new PlatformManagerProxy(network);

        assertThat(proxy.getPlatformName(), is("a name"));
        log.assertContent("synchronousCall(GetPlatformNameCommand)");
    }
}
