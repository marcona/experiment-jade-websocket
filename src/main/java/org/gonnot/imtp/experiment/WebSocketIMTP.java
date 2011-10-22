/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.experiment;
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
public class WebSocketIMTP implements IMTPManager {
    private static final int DEFAULT_WEBSOCKET_PORT = 80;
    private Logger logger = Logger.getLogger("WebSocketIMTP(n/a)");
    private String mainHost;
    private int mainPort;
    private String localHost;
    private int localPort;
    private WebSocketIMTPServer server;
    private WebSocketIMTPClient client;
    private WebSocketNode localNode;


    public void initialize(Profile profile) throws IMTPException {
        logger.info("Configuring Intra platform communication through websockets...");
        boolean isMain = profile.getBooleanProperty(Profile.MAIN, true);

        mainHost = getHost(Profile.MAIN_HOST, profile, getLocalHost());
        mainPort = getPort(Profile.MAIN_PORT, profile, DEFAULT_WEBSOCKET_PORT);

        localHost = getHost(Profile.LOCAL_HOST, profile, mainHost);
        localPort = getPort(Profile.LOCAL_PORT, profile, mainPort);

        if (isMain) {
            logger = Logger.getLogger("WebSocketIMTP(main)");
            logger.info("Start WebSocket server : ws://" + localHost + ":" + localPort);
            server = new WebSocketIMTPServer();
            server.start(localPort);

            // Create the local node for a server
            localNode = new WebSocketNode(PlatformManager.NO_NAME, isMain);
            localNode.startServer(localPort + 1);
            server.setLocalNode(localNode);
        }
        else {
            logger = Logger.getLogger("WebSocketIMTP(peripheral)");
            logger.info("Initiate WebSocket connection : ws://" + mainHost + ":" + mainPort);
            client = new WebSocketIMTPClient();
            client.start(mainHost, mainPort);

            // Create the local node for a client
            localNode = new WebSocketRemoteNode(PlatformManager.NO_NAME, isMain);
            localNode.startClient(mainHost, mainPort + 1);
        }
    }


    public void shutDown() {
        logger.info("shutdown of the IMTP...");

        if (localNode != null) {
            try {
                localNode.exit();
                localNode.stopWebsocket();
            }
            catch (IMTPException imtpe) {
                logger.info(
                      "Error during the closing of the local node (Should never happen since this is a local call)",
                      imtpe);
            }
        }

        if (server != null) {
            server.stop();
        }
        if (client != null) {
            client.stop();
        }
    }


    public Node getLocalNode() throws IMTPException {
        return localNode;
    }


    public void exportPlatformManager(PlatformManager platformManager) throws IMTPException {
        server.setPlatformManager(platformManager);
    }


    public void unexportPlatformManager(PlatformManager sm) throws IMTPException {
        server.setPlatformManager(null);
    }


    public PlatformManager getPlatformManagerProxy() throws IMTPException {
        return client.getPlatformManagerProxy();
    }


    public PlatformManager getPlatformManagerProxy(String addr) throws IMTPException {
        unsuported("unsuported - getPlatformManagerProxy addr");
        return null;
    }


    public void reconnected(PlatformManager pm) {
        unsuported("unsurpoted - reconnected");
    }


    public Slice createSliceProxy(String serviceName, Class itf, Node where) throws IMTPException {
        logger.info("MyIMTPManager.createSliceProxy " + serviceName
                    + " - " + itf.getCanonicalName()
                    + " - " + where.getName());
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
        TransportAddress address = new WebSocketAddress(localHost, String.valueOf(localPort));
        List list = new ArrayList(1);
        list.add(address);
        return list;
    }


    public TransportAddress stringToAddr(String addr) throws IMTPException {
        unsuported("unsuported - stringToAddr");
        return null;
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


    private static String getHost(String property, Profile profile, String defaultHost) throws IMTPException {
        String host = profile.getParameter(property, defaultHost);
        try {
            return InetAddress.getByName(host).getHostName();
        }
        catch (UnknownHostException e) {
            throw new IMTPException("Unable to resolve " + host, e);
        }
    }


    private void unsuported(String message) {
        logger.info("### --> " + message + " <-- ###");
        throw new RuntimeException(message);
    }
}
