/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

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
