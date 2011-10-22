/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.experiment;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import jade.core.HorizontalCommand;
import jade.core.IMTPException;
import jade.core.NodeDescriptor;
import jade.core.PlatformManager;
import jade.core.Service;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
/**
 *
 */
public class RemoteProtocol {
    private RemoteProtocol() {
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Xml
    private static Result extractResultfrom(String resultString) {
        return (Result)xstream().fromXML(resultString);
    }


    public static String toString(Result result) {
        System.out.println("result = " + xstream().toXML(result).replaceAll("\n", "\n\t"));
        return xstream().toXML(result);
    }


    public static String of(Command remoteCommand) {
        System.out.println("command = " + xstream().toXML(remoteCommand).replaceAll("\n", "\n\t"));
        return xstream().toXML(remoteCommand);
    }


    static Command extractCommandFrom(String command) {
        return (Command)xstream().fromXML(command);
    }


    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.setClassLoader(RemoteProtocol.class.getClassLoader());
        xstream.alias("GetPlatformName", GetPlatformName.class);
        xstream.alias("AddNode", AddNode.class);
        xstream.alias("FindSlice", FindSlice.class);

        xstream.alias("FailureResult", FailureResult.class);
        xstream.alias("OneStringResult", OneStringResult.class);
        xstream.alias("OneSliceResult", OneSliceResult.class);

        xstream.useAttributeFor("result", String.class);
        return xstream;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Command

    public static abstract class Command {
        public final Result executeOn(PlatformManager platformManager, WebSocketNode localNode) {
            try {
                return executeByServer(platformManager, localNode);
            }
            catch (Exception e) {
                StringWriter out = new StringWriter();
                e.printStackTrace(new PrintWriter(out));
                return new FailureResult(e.getMessage() + "(" + e.getClass().getSimpleName() + ")", out.toString());
            }
        }


        public abstract Result executeByServer(PlatformManager platformManager, WebSocketNode localNode)
              throws Exception;


        protected Result remoteExecuteImpl(NetworkChannel networkChannel) throws IMTPException {
            Result result;

            try {
                result = extractResultfrom(networkChannel.remoteCall(of(this)));
            }
            catch (Throwable e) {
                // TODO to be improved (removed :-) )
                e.printStackTrace();
                System.exit(-1);
                return null;
            }

            if (result instanceof FailureResult) {
                throw new IMTPException(((FailureResult)result).getErrorMessage());
            }

            return result;
        }
    }

    public static class GetPlatformName extends Command {

        public static GetPlatformName command() {
            return new GetPlatformName();
        }


        @Override
        public Result executeByServer(PlatformManager platformManager, WebSocketNode localNode) throws IMTPException {
            return new OneStringResult(platformManager.getPlatformName());
        }


        public String remoteExecute(NetworkChannel networkChannel) throws IMTPException {
            return ((OneStringResult)remoteExecuteImpl(networkChannel)).getResult();
        }
    }
    @SuppressWarnings({"UseOfObsoleteCollectionType"})
    public static class AddNode extends Command {
        private NodeDescriptor nodeDescriptor;
        private Vector nodeServices;
        private boolean propagated;


        public AddNode(NodeDescriptor nodeDescriptor, Vector nodeServices, boolean propagated) {
            this.nodeDescriptor = nodeDescriptor;
            this.nodeServices = nodeServices;
            this.propagated = propagated;
        }


        public static AddNode command(NodeDescriptor dsc, Vector nodeServices, boolean propagated) {
            return new AddNode(dsc, nodeServices, propagated);
        }


        @Override
        public Result executeByServer(PlatformManager platformManager, WebSocketNode localNode)
              throws IMTPException, JADESecurityException, ServiceException {
            return new OneStringResult(platformManager.addNode(nodeDescriptor, nodeServices, propagated));
        }


        public String remoteExecute(NetworkChannel networkChannel) throws IMTPException {
            Result result = remoteExecuteImpl(networkChannel);
            return ((OneStringResult)result).getResult();
        }
    }
    public static class FindSlice extends Command {
        private String serviceKey;
        private String sliceKey;


        public FindSlice(String serviceKey, String sliceKey) {
            this.serviceKey = serviceKey;
            this.sliceKey = sliceKey;
        }


        public static FindSlice command(String serviceKey, String sliceKey) {
            return new FindSlice(serviceKey, sliceKey);
        }


        @Override
        public Result executeByServer(PlatformManager platformManager, WebSocketNode localNode)
              throws IMTPException, ServiceException {
            return new OneSliceResult(platformManager.findSlice(serviceKey, sliceKey));
        }


        public Service.Slice remoteExecute(NetworkChannel networkChannel) throws IMTPException {
//            return null;
            OneSliceResult oneSliceResult = (OneSliceResult)remoteExecuteImpl(networkChannel);
            try {
                ((WebSocketNode)oneSliceResult.getResult().getNode()).setNetworkChannel(networkChannel);
            }
            catch (ServiceException e) {
                throw new IMTPException("Marche pas!", e);
            }
            return oneSliceResult.getResult();
        }
    }
    public static class NodeAccept extends Command {
        private HorizontalCommand cmd;


        public NodeAccept(HorizontalCommand cmd) {
            this.cmd = cmd;
        }


        public static NodeAccept command(HorizontalCommand cmd) {
            return new NodeAccept(cmd);
        }


        @Override
        public Result executeByServer(PlatformManager platformManager, WebSocketNode localNode)
              throws IMTPException, ServiceException {
            return new OneObjectResult(localNode.accept(cmd));
        }


        public Object remoteExecute(NetworkChannel networkChannel) throws IMTPException {
            OneObjectResult oneSliceResult = (OneObjectResult)remoteExecuteImpl(networkChannel);
            return oneSliceResult.getResult();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Result

    public static interface Result {

    }

    public static class FailureResult implements Result {
        private String errorMessage;
        private String stacktrace;


        public FailureResult(String errorMessage, String stacktrace) {
            this.errorMessage = errorMessage;
            this.stacktrace = stacktrace;
        }


        public String getErrorMessage() {
            return errorMessage;
        }


        public String getStacktrace() {
            return stacktrace;
        }
    }
    public static class OneStringResult implements Result {
        private String result;


        public OneStringResult(String result) {
            this.result = result;
        }


        public String getResult() {
            return result;
        }
    }
    public static class OneSliceResult implements Result {
        private Service.Slice result;


        public OneSliceResult(Service.Slice result) {
            this.result = result;
        }


        public Service.Slice getResult() {
            return result;
        }
    }
    public static class OneObjectResult implements Result {
        private Object result;


        public OneObjectResult(Object result) {
            this.result = result;
        }


        public Object getResult() {
            return result;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Network

    public static interface NetworkChannel {
        String remoteCall(String message) throws IMTPException;
    }
}
