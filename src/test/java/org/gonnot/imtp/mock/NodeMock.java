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
