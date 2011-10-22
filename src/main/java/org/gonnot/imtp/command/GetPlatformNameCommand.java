/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.command;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.PlatformManager;
/**
 *
 */
class GetPlatformNameCommand extends Command<String> {

    @Override
    public String execute(PlatformManager platformManager, Node localNode) throws IMTPException {
        return platformManager.getPlatformName();
    }
}
