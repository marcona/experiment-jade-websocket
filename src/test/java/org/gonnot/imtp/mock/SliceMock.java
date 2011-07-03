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

    private ServiceMock service;
    private NodeMock nodeMock;


    public SliceMock(ServiceMock service, NodeMock nodeMock) {
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
