/*
 * Copyright (c) - codjo.net framework (2011) - All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 2.0, a copy of which has been included with this
 * distribution in the LICENSE.txt file.
 */

package org.gonnot.imtp;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
/**
 *
 */
class CommandCodec {
    private CommandCodec() {
    }


    public static String encode(Object remoteCommand) {
        return xstream().toXML(remoteCommand);
    }


    public static Command decode(String message) {
        return (Command)xstream().fromXML(message);
    }


    public static Result decodeResult(String message) {
        return (Result)xstream().fromXML(message);
    }


    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.setClassLoader(CommandCodec.class.getClassLoader());
//        xstream.alias("GetPlatformName", GetPlatformName.class);
//        xstream.alias("AddNode", AddNode.class);
//        xstream.alias("FindSlice", FindSlice.class);
//
//        xstream.alias("FailureResult", FailureResult.class);
//        xstream.alias("OneStringResult", OneStringResult.class);
//        xstream.alias("OneSliceResult", OneSliceResult.class);
//
//        xstream.useAttributeFor("result", String.class);
        return xstream;
    }
}
