package org.gonnot.imtp.mock;
import jade.core.BaseService;
/**
 *
 */
public class ServiceMock extends BaseService {
    private final String serviceKey;


    public ServiceMock(String serviceKey) {
        this.serviceKey = serviceKey;
    }


    public String getName() {
        return (serviceKey == null) ? "mock" : serviceKey;
    }
}
