package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.ProfileImpl;
import jade.core.Service.Slice;
import jade.core.ServiceDescriptor;
import jade.core.ServiceException;
import jade.mtp.TransportAddress;
import jade.security.JADESecurityException;
import jade.util.leap.List;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.agf.test.common.matcher.JUnitMatchers.*;
/**
 *
 */
public class WebSocketIMTPManagerTest {
    private static final boolean IS_MAIN_CONTAINER = true;
    public static final boolean PERIPHERAL_CONTAINER = false;
    public static final String LOCALHOST = "localhost";
    private WebSocketIMTPManager webSocketIMTPManager;
    private WebSocketIMTPManager main;
    private WebSocketIMTPManager peripheral;


    @Before
    public void setUp() throws Exception {
        webSocketIMTPManager = new WebSocketIMTPManager();
    }


    @After
    public void tearDown() throws Exception {
        try {
            if (peripheral != null) {
                peripheral.shutDown();
            }
        }
        finally {
            if (main != null) {
                main.shutDown();
            }
            webSocketIMTPManager.shutDown();
        }
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


    @Test
    public void test_getPlatformManagerProxy() throws Exception {
        final String platformName = "a plateform name -" + System.currentTimeMillis();
        PlatformManager platformManager = new PlatformManagerMock() {
            @Override
            public String getPlatformName() throws IMTPException {
                return platformName;
            }
        };

        main = createMainContainer();
        main.exportPlatformManager(platformManager);

        peripheral = createPeripheralContainer();
        PlatformManager platformManagerProxy = peripheral.getPlatformManagerProxy();

        assertThat(platformManagerProxy.getPlatformName(), is(platformName));
    }


    private static WebSocketIMTPManager createMainContainer() throws IMTPException {
        WebSocketIMTPManager imtp = new WebSocketIMTPManager();
        imtp.initialize(new ProfileImpl("localhost", 69, null, IS_MAIN_CONTAINER));
        return imtp;
    }


    private static WebSocketIMTPManager createPeripheralContainer() throws IMTPException {
        WebSocketIMTPManager imtp = new WebSocketIMTPManager();
        imtp.initialize(new ProfileImpl("localhost", 69, null, PERIPHERAL_CONTAINER));
        return imtp;
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


    private static class PlatformManagerMock implements PlatformManager {
        public String getPlatformName() throws IMTPException {
            return null;
        }


        public String getLocalAddress() {
            return null;
        }


        public void setLocalAddress(String addr) {

        }


        public String addNode(NodeDescriptor dsc, Vector nodeServices, boolean propagated)
              throws IMTPException, ServiceException, JADESecurityException {
            return null;
        }


        public void removeNode(NodeDescriptor dsc, boolean propagated) throws IMTPException, ServiceException {

        }


        public void addSlice(ServiceDescriptor service, NodeDescriptor dsc, boolean propagated)
              throws IMTPException, ServiceException {

        }


        public void removeSlice(String serviceKey, String sliceKey, boolean propagated)
              throws IMTPException, ServiceException {

        }


        public void addReplica(String newAddr, boolean propagated) throws IMTPException, ServiceException {

        }


        public void removeReplica(String address, boolean propagated) throws IMTPException, ServiceException {

        }


        public Slice findSlice(String serviceKey, String sliceKey) throws IMTPException, ServiceException {
            return null;
        }


        public Vector findAllSlices(String serviceKey) throws IMTPException, ServiceException {
            return null;
        }


        public void adopt(Node n, Node[] children) throws IMTPException {

        }


        public void ping() throws IMTPException {

        }
    }
}
