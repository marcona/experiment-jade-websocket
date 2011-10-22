/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.mock;
import jade.core.HorizontalCommand;
import jade.core.Node;
import jade.core.Service;
import jade.core.Service.Slice;
import jade.core.ServiceException;
import jade.core.VerticalCommand;
/**
 *
 */
public class SliceMock implements Slice {
    private Service service;
    private Node nodeMock;


    public SliceMock() {
    }


    public SliceMock(Service service, Node nodeMock) {
        this.service = service;
        this.nodeMock = nodeMock;
    }


    public Service getService() {
        return service;
    }


    public Node getNode() throws ServiceException {
        return nodeMock;
    }


    public VerticalCommand serve(HorizontalCommand cmd) {
        return null;
    }
}
