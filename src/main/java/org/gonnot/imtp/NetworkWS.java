package org.gonnot.imtp;
import java.io.IOException;
/**
 *
 */
interface NetworkWS {
    public String synchronousCall(GetPlatformNameCommand command) throws IOException;
}
