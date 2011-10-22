/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.experiment;
import jade.mtp.TransportAddress;
/**
 *
 */
class WebSocketAddress implements TransportAddress {
    private String host;
    private String port;


    WebSocketAddress(String host, String port) {
        this.host = host;
        this.port = port;
    }


    public String getProto() {
        return null;
    }


    public String getHost() {
        return host;
    }


    public String getPort() {
        return port;
    }


    public String getFile() {
        return "";
    }


    public String getAnchor() {
        return "";
    }
}
