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
