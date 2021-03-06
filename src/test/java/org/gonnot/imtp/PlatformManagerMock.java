package org.gonnot.imtp;
import com.agf.test.common.LogString;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.Service.Slice;
import jade.core.ServiceDescriptor;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.util.Vector;
/**
 *
 */
class PlatformManagerMock implements PlatformManager {
    private String platformName = null;
    private LogString log = new LogString();
    private String addNodeReturnValue;
    private ServiceException serviceFailure;
    private RuntimeException runtimeFailure;


    PlatformManagerMock() {
    }


    PlatformManagerMock(LogString log) {
        this.log = new LogString("platformManager", log);
    }


    public String getPlatformName() throws IMTPException {
        return platformName;
    }


    public PlatformManagerMock mockGetPlatformNameToReturn(String name) {
        this.platformName = name;
        return this;
    }


    public PlatformManager mockAddNodeToReturn(String value) {
        this.addNodeReturnValue = value;
        return this;
    }


    public PlatformManager mockAddNodeToFail(ServiceException failure) {
        this.serviceFailure = failure;
        return this;
    }


    public PlatformManager mockAddNodeToFail(RuntimeException failure) {
        this.runtimeFailure = failure;
        return this;
    }


    public String getLocalAddress() {
        return null;
    }


    public void setLocalAddress(String addr) {

    }


    public String addNode(NodeDescriptor descriptor, Vector nodeServices, boolean propagated)
          throws IMTPException, ServiceException, JADESecurityException {
        log.call("addNode", toString(descriptor), nodeServices, propagated);
        if (serviceFailure != null) {
            throw serviceFailure;
        }
        if (runtimeFailure != null) {
            throw runtimeFailure;
        }
        return addNodeReturnValue;
    }


    private String toString(NodeDescriptor descriptor) {
        if (descriptor == null) {
            return "null";
        }
        return "NodeDescriptor(" + descriptor.getName() + ")";
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


    public void adopt(Node node, Node[] children) throws IMTPException {

    }


    public void ping() throws IMTPException {

    }
}
