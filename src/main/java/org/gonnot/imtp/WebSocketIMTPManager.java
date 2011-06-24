package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.IMTPManager;
import jade.core.Node;
import jade.core.PlatformManager;
import jade.core.Profile;
import jade.core.Service;
import jade.core.Service.Slice;
import jade.core.SliceProxy;
import jade.mtp.TransportAddress;
import jade.util.leap.ArrayList;
import jade.util.leap.List;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
/**
 *
 */
public class WebSocketIMTPManager implements IMTPManager {
    private Logger logger = Logger.getLogger("WebSocketIMTPManager(n/a)");
    private String localHost;
    private int localPort;
    private Node localNode;


    public void initialize(Profile profile) throws IMTPException {
        logger.info("Configuring Intra platform communication through websockets...");
        boolean isMain = profile.getBooleanProperty(Profile.MAIN, true);
        String mainHost = getHost(Profile.MAIN_HOST, profile, getLocalHost());
        int mainPort = getPort(Profile.MAIN_PORT, profile, -1);

        localHost = getHost(Profile.LOCAL_HOST, profile, mainHost);
        localPort = getPort(Profile.LOCAL_PORT, profile, mainPort);

        localNode = new WebSocketNode(PlatformManager.NO_NAME, isMain);
        if (isMain) {
            logger = Logger.getLogger("WebSocketIMTPManager(main)");
            ((WebSocketNode)localNode).setLogger(Logger.getLogger("WebsocketNode(main)"));
        }
        else {
            logger = Logger.getLogger("WebSocketIMTPManager(peripheral)");
            ((WebSocketNode)localNode).setLogger(Logger.getLogger("WebsocketNode(peripheral)"));
        }
    }


    public void shutDown() {
        tobeimplemented("shutDown");
    }


    public Node getLocalNode() throws IMTPException {
        return localNode;
    }


    public void exportPlatformManager(PlatformManager mgr) throws IMTPException {
        tobeimplemented("exportPlatformManager");
    }


    public void unexportPlatformManager(PlatformManager sm) throws IMTPException {
        tobeimplemented("unexportPlatformManager");
    }


    public PlatformManager getPlatformManagerProxy() throws IMTPException {
        unsupported("getPlatformManagerProxy");
        return null;
    }


    public PlatformManager getPlatformManagerProxy(String addr) throws IMTPException {
        unsupported("getPlatformManagerProxy");
        return null;
    }


    public void reconnected(PlatformManager pm) {
        unsupported("reconnected");
    }


    public Slice createSliceProxy(String serviceName, Class itf, Node where) throws IMTPException {
        logger.info("createSliceProxy(" + serviceName + ", " + itf.getCanonicalName() + ", " + where.getName() + ")");
        try {
            Class proxyClass = Class.forName(serviceName + "Proxy");
            Service.Slice proxy = (Service.Slice)proxyClass.newInstance();
            if (proxy instanceof SliceProxy) {
                ((SliceProxy)proxy).setNode(where);
            }
            else {
                throw new IMTPException("Class " + proxyClass.getName() + " is not a slice proxy.");
            }
            return proxy;
        }
        catch (Exception e) {
            throw new IMTPException("Error creating a slice proxy", e);
        }
    }


    public List getLocalAddresses() throws IMTPException {
        List list = new ArrayList(1);
        list.add(new WebSocketAddress(localHost, String.valueOf(localPort)));
        return list;
    }


    public TransportAddress stringToAddr(String addr) throws IMTPException {
        unsupported("stringToAddr");
        return null;
    }


    private void tobeimplemented(String message) {
        logger.info("### --> " + message + " <-- ### -------------> TO BE IMPLEMENTED");
    }


    private void unsupported(String message) {
        logger.info("### --> " + message + " <-- ###");
        throw new RuntimeException(message);
    }


    private static String getHost(String property, Profile profile, String defaultHost) throws IMTPException {
        String host = profile.getParameter(property, defaultHost);
        try {
            return InetAddress.getByName(host).getCanonicalHostName();
        }
        catch (UnknownHostException e) {
            throw new IMTPException("Unable to resolve " + host, e);
        }
    }


    private static int getPort(String property, Profile profile, int defaultPort) {
        String portStr = profile.getParameter(property, null);
        if (portStr == null) {
            return defaultPort;
        }
        else {
            return Integer.parseInt(portStr);
        }
    }


    private static String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
