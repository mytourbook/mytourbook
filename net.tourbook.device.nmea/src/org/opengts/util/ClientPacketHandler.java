// ----------------------------------------------------------------------------
// Copyright 2006-2009, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Socket client packet handler
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.net.*;
import javax.net.ssl.*;
import javax.net.*;

public interface ClientPacketHandler
{

    // called when new client session initiated
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText);
    
    // return initial response to the open session
    public byte[] getInitialPacket() throws Exception;
    
    // return final response to the session before it closes
    public byte[] getFinalPacket(boolean hasError) throws Exception;

    // return actual packet length based on this partial packet
    public int getActualPacketLength(byte packet[], int packetLen); // non-text
    
    // process packet and return response
    public byte[] getHandlePacket(byte cmd[]) throws Exception;
    
    // return the port for UDP Datagram responses
    public int getResponsePort(); // may return '0' to default to "<ServerSocketThread>.getRemotePort()"

    // return true to terminate session
    public boolean terminateSession();
    
    // called after client session terminated
    public void sessionTerminated(Throwable err, long readCount, long writeCount);
    
}
