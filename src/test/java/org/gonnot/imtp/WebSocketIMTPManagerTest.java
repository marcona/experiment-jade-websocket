package org.gonnot.imtp;
import jade.core.Node;
import jade.core.PlatformManager;
import jade.core.ProfileImpl;
import jade.core.Service.Slice;
import jade.mtp.TransportAddress;
import jade.util.leap.List;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Before;
import org.junit.Test;

import static com.agf.test.common.matcher.JUnitMatchers.*;
/**
 *
 */
public class WebSocketIMTPManagerTest {
    private static final boolean IS_MAIN_CONTAINER = true;
    public static final String LOCALHOST = "localhost";
    private WebSocketIMTPManager webSocketIMTPManager;


    @Before
    public void setUp() throws Exception {
        webSocketIMTPManager = new WebSocketIMTPManager();
    }


    @Test
    public void test_getLocalAdress_defaultValue() throws Exception {
        webSocketIMTPManager.initialize(new ProfileImpl());

        List localAddresses = webSocketIMTPManager.getLocalAddresses();

        assertThat(toString(localAddresses), is(replaceVariable("${localhost-canonicalName}:1099")));
    }


    @Test
    public void test_getLocalAdress() throws Exception {
        webSocketIMTPManager.initialize(new ProfileImpl(InetAddress.getLocalHost().getHostAddress(), 69, null));

        List localAddresses = webSocketIMTPManager.getLocalAddresses();

        assertThat(toString(localAddresses), is(replaceVariable("${localhost-canonicalName}:69")));
    }


    @Test
    public void test_getLocalNode() throws Exception {
        webSocketIMTPManager.initialize(new ProfileImpl(LOCALHOST, -1, "unused", IS_MAIN_CONTAINER));
        Node localNode = webSocketIMTPManager.getLocalNode();

        assertThat(localNode.hasPlatformManager(), is(true));
        assertThat(localNode.getName(), is(PlatformManager.NO_NAME));
    }


    @Test
    public void test_createSliceProxy() throws Exception {
        Node node = new WebSocketNode("Main-Container", true);

        Slice sliceProxy = webSocketIMTPManager.createSliceProxy("jade.core.management.AgentManagement",
                                                                 jade.core.management.AgentManagementSlice.class,
                                                                 node);

        assertThat(sliceProxy.getNode(), is(sameInstance(node)));
        assertThat(sliceProxy.getClass().getName(), is("jade.core.management.AgentManagementProxy"));
        assertThat(sliceProxy.getService(), nullValue());
    }


    private String replaceVariable(String string) throws UnknownHostException {
        return string
              .replaceAll("\\$\\{localhost-canonicalName\\}", InetAddress.getLocalHost().getCanonicalHostName());
    }


    private String toString(List addresses) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < addresses.size(); i++) {
            TransportAddress transportAddress = (TransportAddress)addresses.get(i);
            result.append(transportAddress.getHost()).append(":").append(transportAddress.getPort());
            if (i + 1 < addresses.size()) {
                result.append(", ");
            }
        }
        return result.toString();
    }
}
