package org.gonnot.imtp;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.gonnot.imtp.command.Command;
import org.gonnot.imtp.command.Result;
/**
 *
 */
public class CommandCodec {
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
