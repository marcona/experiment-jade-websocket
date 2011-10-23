/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp.engine;
import jade.core.IMTPException;
import jade.core.Node;
import jade.core.PlatformManager;
import jade.core.ServiceException;
import jade.security.JADESecurityException;
import org.gonnot.imtp.command.Command;
/**
 *
 */
public class DummyCommand extends Command<String> {
    private int id = -1;


    public DummyCommand() {
    }


    public DummyCommand(int id) {
        this.id = id;
    }


    @Override
    public String execute(PlatformManager platformManager, Node localNode) throws IMTPException,
                                                                                  JADESecurityException,
                                                                                  ServiceException {
        return "resultOf(command[" + getCommandId() + "].execute(...))";
    }


    @Override
    public int getCommandId() {
        if (id == -1) {
            return super.getCommandId();
        }
        return id;
    }
}
