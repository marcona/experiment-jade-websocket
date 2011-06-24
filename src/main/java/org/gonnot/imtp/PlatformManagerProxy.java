package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.Service.Slice;
import jade.core.ServiceDescriptor;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.io.IOException;
import java.util.Vector;
import org.apache.log4j.Logger;
/**
 *
 */
class PlatformManagerProxy implements PlatformManager {
    private Logger logger = Logger.getLogger("PlatformManagerProxy");
    private NetworkWS network;


    PlatformManagerProxy(NetworkWS network) {
        this.network = network;
    }


    public String getPlatformName() throws IMTPException {
        try {
            return network.synchronousCall(new GetPlatformNameCommand());
        }
        catch (IOException ex) {
            throw new IMTPException(ex.getLocalizedMessage(), ex);
        }
    }


    public String getLocalAddress() {
        unsupported("getLocalAddress");
        return null;
    }


    public void setLocalAddress(String addr) {
        unsupported("setLocalAddress");
    }


    public String addNode(NodeDescriptor dsc, Vector nodeServices, boolean propagated)
          throws IMTPException, ServiceException, JADESecurityException {
        unsupported("addNode");
        return null;
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
        unsupported("findSlice");
        return null;
    }


    public Vector findAllSlices(String serviceKey) throws IMTPException, ServiceException {
        unsupported("findAllSlices");
        return null;
    }


    public void adopt(Node n, Node[] children) throws IMTPException {
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
