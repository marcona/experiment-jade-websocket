/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp;
import jade.core.IMTPException;
import jade.core.PlatformManager;
import java.io.IOException;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.NetworkChannel;
import org.gonnot.imtp.command.Result;
import websocket4j.client.WebSocket;
import static org.gonnot.imtp.util.JadeExceptionUtil.imtpException;
/**
 *
 */
class IMTPPeripheral {
    private PlatformManager platformManagerProxy;
    private WebSocket socketClient;


    IMTPPeripheral() {
    }


    public PlatformManager getPlatformManagerProxy() {
        return platformManagerProxy;
    }


    public void start(String host, int port) throws IMTPException {
        try {
            socketClient = new WebSocket(host, port, IMTPMain.PLATFORM_URI);
            platformManagerProxy = new PlatformManagerProxy(new NetworkChannelImpl());
        }
        catch (IOException e) {
            throw imtpException("Unable to start the WebSocket client", e);
        }
    }


    public void shutDown() {
        try {
            socketClient.close();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to stop the WebSocket client", e);
        }
    }


    private class NetworkChannelImpl extends NetworkChannel {
        @Override
        public Result send(Command command) throws IMTPException {
            try {
                socketClient.sendMessage(CommandCodec.encode(command));
                String encodedResult = socketClient.getMessage();
                return CommandCodec.decodeResult(encodedResult);
            }
            catch (IOException cause) {
                throw imtpException("Communication error", cause);
            }
        }
    }
}
