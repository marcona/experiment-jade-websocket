/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.mock;
import jade.core.BaseNode;
import jade.core.HorizontalCommand;
import jade.core.IMTPException;
/**
 *
 */
public class NodeMock extends BaseNode {
    public NodeMock(String nodeName) {
        super(nodeName, false);
    }


    public Object accept(HorizontalCommand cmd) throws IMTPException {
        return null;
    }


    public boolean ping(boolean hang) throws IMTPException {
        return false;
    }


    public void interrupt() throws IMTPException {
    }


    public void exit() throws IMTPException {
    }
}
