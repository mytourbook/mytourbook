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
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/09/16  Martin D. Flynn
//     -Moved to package 'org.opengts.util'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;

/**
*** This class calculates a Fletcher checksum
**/

public class FletcherChecksum
{
    
    // ------------------------------------------------------------------------

    private int C[] = { 0, 0 };
    
    /**
    *** Constructor
    **/
    public FletcherChecksum()
    {
        this.reset();
    }
    
    /**
    *** Constructor
    *** @param b  The initial byte array
    **/
    public FletcherChecksum(byte b[])
    {
        this();
        this.runningChecksum(b);
    }

    // ------------------------------------------------------------------------

    /**
    *** Resets the internal Fletcher checksum accumulator
    **/
    public void reset()
    {
        this.C[0] = 0;
        this.C[1] = 0;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the current values of the checksum accumulator.
    *** @return A 2-integer array containing the current values of the checksum accumulator.
    **/
    public int[] getValues()
    {
        int F[] = new int[2];
        F[0] = C[0] & 0xFF;
        F[1] = C[1] & 0xFF;
        return F;
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Returns true if the current checksum is valid
    *** @return True if the current checksum is valid, false if invalid.
    **/
    public boolean isValid()
    {
        byte F[] = this.getChecksum();
        //Print.logDebug("F0=" + (F[0]&0xFF) + ", F1=" + (F[1]&0xFF));
        return (F[0] == 0) && (F[1] == 0);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the current value of the checksum
    *** @return A 2-byte array containing the checksum
    **/
    public byte[] getChecksum()
    {
        byte F[] = new byte[2];
        F[0] = (byte)((C[0] - C[1])      & 0xFF);
        F[1] = (byte)((C[1] - (C[0]<<1)) & 0xFF);
        return F;
    }

    /**
    *** Returns the current value of the checksum as an integer
    *** @return The current value of the checksum as an integer
    **/
    public int getChecksumAsInt()
    {
        byte cs[] = this.getChecksum();
        return ((((int)cs[0] & 0xFF) << 8) | ((int)cs[1] & 0xFF));
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Adds the specified byte array to the current running checksum accumulator.
    *** @param b  The byte array to add to the current running checksum accumulator.
    **/
    public void runningChecksum(byte b[])
    {
        if (b != null) {
            for (int i = 0; i < b.length; i++) {
                C[0] = C[0] + ((int)b[i] & 0xFF);
                C[1] = C[1] + C[0];
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Command-line entry point (bug/testing purposes)
    *** @param argv The command-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String val = RTConfig.getString("val","");
        FletcherChecksum fc = new FletcherChecksum();
        fc.runningChecksum(val.getBytes());
        Print.sysPrintln("0x"+StringTools.toHexString(fc.getChecksumAsInt(),16));
    }
    
}

