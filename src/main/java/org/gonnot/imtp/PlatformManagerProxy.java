/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.Service.Slice;
import jade.core.ServiceDescriptor;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.gonnot.imtp.command.CommandFactory;
import org.gonnot.imtp.command.NetworkChannel;

/**
 *
 */
class PlatformManagerProxy implements PlatformManager {
    private Logger logger = Logger.getLogger("PlatformManagerProxy");
    private NetworkChannel network;


    PlatformManagerProxy(NetworkChannel network) {
        this.network = network;
    }


    public String getPlatformName() throws IMTPException {
        return network.execute3(CommandFactory.getPlatformName());
    }


    public String getLocalAddress() {
        unsupported("getLocalAddress");
        return null;
    }


    public void setLocalAddress(String addr) {
        unsupported("setLocalAddress");
    }


    public String addNode(NodeDescriptor descriptor, Vector nodeServices, boolean propagated)
          throws IMTPException, ServiceException, JADESecurityException {
        return network.execute(CommandFactory.addNode(descriptor, nodeServices, propagated));
    }


    public void removeNode(NodeDescriptor dsc, boolean propagated) throws IMTPException, ServiceException {
        unsupported("removeNode");
    }


    public void addSlice(ServiceDescriptor service, NodeDescriptor dsc, boolean propagated)
          throws IMTPException, ServiceException {
        unsupported("addSlice");
    }


    public void removeSlice(String serviceKey, String sliceKey, boolean propagated)
          throws IMTPException, ServiceException {
        unsupported("removeSlice");
    }


    public void addReplica(String newAddr, boolean propagated) throws IMTPException, ServiceException {
        unsupported("addReplica");
    }


    public void removeReplica(String address, boolean propagated) throws IMTPException, ServiceException {
        unsupported("removeReplica");
    }


    public Slice findSlice(String serviceKey, String sliceKey) throws IMTPException, ServiceException {
        return network.execute2(CommandFactory.findSlice(serviceKey, sliceKey));
    }


    public Vector findAllSlices(String serviceKey) throws IMTPException, ServiceException {
        unsupported("findAllSlices");
        return null;
    }


    public void adopt(Node node, Node[] children) throws IMTPException {
        unsupported("adopt");
    }


    public void ping() throws IMTPException {
        unsupported("ping");
    }


    private void tobeimplemented(String message) {
        logger.info("### --> " + message + " <-- ### -------------> TO BE IMPLEMENTED");
    }


    private void unsupported(String message) {
        logger.info("### --> " + message + " <-- ###");
        throw new RuntimeException(message);
    }
}
