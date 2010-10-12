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
// Change History:
//  2007/11/28  Martin D. Flynn
//     -Initial release
//  2009/01/28  Martin D. Flynn
//     -Improved command-line interface
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class DatagramMessage
{

    // ------------------------------------------------------------------------

    protected DatagramSocket datagramSocket  = null;
    protected DatagramPacket sendPacket = null;
    protected DatagramPacket recvPacket = null;

    /* subclassing only */
    protected DatagramMessage()
    {
    }

    /* receiving messages */
    public DatagramMessage(int port)
        throws IOException, UnknownHostException
    {
        this.datagramSocket = new DatagramSocket(port);
    }

    /* sending messages */
    public DatagramMessage(String destHost, int destPort)
        throws IOException, UnknownHostException
    {
        this(InetAddress.getByName(destHost), destPort);
    }

    /* sending messages */
    public DatagramMessage(InetAddress destHost, int destPort)
        throws IOException
    {
        this.datagramSocket = new DatagramSocket();
        this.setRemoteHost(destHost, destPort);
    }

    // ------------------------------------------------------------------------

    /* close datagram socket */
    public void close()
        throws IOException
    {
        this.datagramSocket.close();
    }

    // ------------------------------------------------------------------------

    /* set the remote(destination) host */
    public void setRemoteHost(String host, int port)
        throws IOException
    {
        this.setRemoteHost(InetAddress.getByName(host), port);
    }

    /* set the remote(destination) host */
    public void setRemoteHost(InetAddress host, int port)
        throws IOException
    {
        if (this.sendPacket != null) {
            this.sendPacket.setAddress(host);
            this.sendPacket.setPort(port);
        } else {
            this.sendPacket = new DatagramPacket(new byte[0], 0, host, port);
        }
    }
    
    public DatagramPacket getSendPacket()
    {
        return this.sendPacket;
    }

    // ------------------------------------------------------------------------

    /* send a String to the remote host */
    public void send(String msg)
        throws IOException
    {
        this.send(StringTools.getBytes(msg));
    }

    /* send an array of bytes to the remote host */
    public void send(byte data[])
        throws IOException
    {
        if (data != null) {
            this.send(data, data.length);
        } else {
            throw new IOException("Nothing to send");
        }
    }

    /* send an array of bytes to the remote host */
    public void send(byte data[], int len)
        throws IOException
    {
        this.send(data, len, 1);
    }

    /* send an array of bytes to the remote host */
    public void send(byte data[], int len, int count)
        throws IOException
    {
        if (this.sendPacket == null) {
            throw new IOException("'setRemoteHost' not specified");
        } else
        if ((data == null) || (len <= 0) || (count <= 0)) {
            throw new IOException("Nothing to send");
        } else {
            this.sendPacket.setData(data);
            this.sendPacket.setLength(len);
            for (; count > 0; count--) {
                this.datagramSocket.send(this.sendPacket);
            }
        }
    }

    // ------------------------------------------------------------------------

    private static final int DEFAULT_PACKET_SIZE = 1024;

    /* receive an array of bytes */
    public byte[] receive(int maxBuffSize)
        throws IOException
    {

        /* receive data */
        byte dbuff[] = new byte[(maxBuffSize > 0)? maxBuffSize : DEFAULT_PACKET_SIZE];
        this.recvPacket = new DatagramPacket(dbuff, dbuff.length);
        this.datagramSocket.receive(this.recvPacket);
        byte newBuff[] = new byte[this.recvPacket.getLength()];
        System.arraycopy(this.recvPacket.getData(), 0, newBuff, 0, this.recvPacket.getLength());

        /* return received data */
        return newBuff;

    }
    
    public DatagramPacket getReceivePacket()
    {
        return this.recvPacket;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_HOST[]      = new String[] { "host" , "h"       };
    private static final String ARG_PORT[]      = new String[] { "port" , "p"       };
    private static final String ARG_SEND[]      = new String[] { "send"             };
    private static final String ARG_RECEIVE[]   = new String[] { "recv", "receive"  };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + DatagramMessage.class.getName() + " {options}");
        Print.logInfo("'Send' Options:");
        Print.logInfo("  -host=<host>    The destination host");
        Print.logInfo("  -port=<port>    The destination port");
        Print.logInfo("  -send=<data>    The data to send (prefix with '0x' for hex data)");
        Print.logInfo("'Receive' Options:");
        Print.logInfo("  -port=<port>    The port on which to listen for incoming data");
        Print.logInfo("  -recv           Set to 'receive' mode");
        System.exit(1);
    }

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String host = RTConfig.getString(ARG_HOST, null);
        int    port = RTConfig.getInt(ARG_PORT, 0);

        /* send data */
        if (RTConfig.hasProperty(ARG_SEND)) {
            if (StringTools.isBlank(host)) {
                Print.logError("Target host not specified");
                usage();
            }
            if (port <= 0) {
                Print.logError("Target port not specified");
                usage();
            }
            try {
                DatagramMessage dgm = new DatagramMessage(host, port);
                String dataStr = RTConfig.getString(ARG_SEND,"0x923739BD071600010130313037353430303033373438363817B7D13901");
                byte data[] = dataStr.startsWith("0x")? StringTools.parseHex(dataStr,null) : dataStr.getBytes();
                dgm.send(data);
                Print.logInfo("Data sent to %s:%d", host, port);
                System.exit(0);
            } catch (Throwable th) {
                Print.logException("Error", th);
                System.exit(99);
            }
        }

        /* receive data */
        if (RTConfig.hasProperty(ARG_RECEIVE)) {
            if (port <= 0) {
                Print.logError("Target port not specified");
                usage();
            }
            if (!StringTools.isBlank(host)) {
                Print.logWarn("Specified 'host' will be ignored");
            }
            try {
                DatagramMessage dgm = new DatagramMessage(port);
                Print.sysPrintln("Waiting for incoming data ...");
                byte data[] = dgm.receive(1000);
                SocketAddress sa = dgm.getReceivePacket().getSocketAddress();
                if (sa instanceof InetSocketAddress) {
                    InetAddress hostAddr = ((InetSocketAddress)sa).getAddress();
                    Print.logInfo("Received from host '" + hostAddr + "': " + StringTools.toHexString(data));
                }
                System.exit(0);
            } catch (Throwable th) {
                Print.logException("Error", th);
                System.exit(99);
            }
        }
        
        /* show usage */
        usage();
        
    }
    
}
