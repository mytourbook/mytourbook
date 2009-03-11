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
//  Read/Write binary fields
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2007/02/25  Martin D. Flynn
//     -Made 'decodeLong'/'encodeLong' public.
//     -Moved to 'org.opengts.util'.
//  2007/03/11  Martin D. Flynn
//     -Added check for remaining available read/write bytes
//  2008/02/04  Martin D. Flynn
//     -Added 'encodeDouble'/'decodeDouble' methods for encoding and decoding
//      32-bit and 64-bit IEEE 754 floating-point values.
//     -Added Big/Little-Endian flag
//     -Added 'writeZeroFill' method
//     -Fixed 'writeBytes' to proper blank fill written fields
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;

public class Payload
{
    
    // ------------------------------------------------------------------------

    public  static final int        DEFAULT_MAX_PAYLOAD_LENGTH = 255;
    
    public  static final byte       EMPTY_BYTE_ARRAY[] = new byte[0];
    
    private static final boolean    DEFAULT_BIG_ENDIAN = true;

    // ------------------------------------------------------------------------

    private byte        payload[] = null;
    private int         size = 0;
    private int         index = 0;
    private boolean     bigEndian = DEFAULT_BIG_ENDIAN;

    // ------------------------------------------------------------------------

    public Payload()
    {
        // DESTINATION: configure for creating a new binary payload
        this(DEFAULT_MAX_PAYLOAD_LENGTH, DEFAULT_BIG_ENDIAN);
    }

    public Payload(int maxPayloadLen)
    {
        // DESTINATION: configure for creating a new binary payload
        this(maxPayloadLen, DEFAULT_BIG_ENDIAN);
    }

    public Payload(int maxPayloadLen, boolean bigEndian)
    {
        // DESTINATION: configure for creating a new binary payload
        this.payload = new byte[maxPayloadLen];
        this.size    = 0; // no 'size' yet
        this.index   = 0; // start at index '0' for writing
        this.setBigEndian(bigEndian);
    }

    // ------------------------------------------------------------------------

    public Payload(byte b[])
    {
        // SOURCE: configure for reading a binary payload
        this(b, DEFAULT_BIG_ENDIAN);
    }

    public Payload(byte b[], boolean bigEndian)
    {
        // SOURCE: configure for reading a binary payload
        this(b, 0, ((b != null)? b.length : 0), bigEndian);
    }

    public Payload(byte b[], int ofs, int len)
    {
        this(b, ofs, len, DEFAULT_BIG_ENDIAN);
    }
    
    public Payload(byte b[], int ofs, int len, boolean bigEndian)
    {
        // SOURCE: configure for reading a binary payload
        this();
        if ((b == null) || (ofs >= b.length)) {
            this.payload = new byte[0];
            this.size    = 0;
            this.index   = 0;
        } else
        if ((ofs == 0) && (b.length == len)) {
            this.payload = b;
            this.size    = b.length;
            this.index   = 0;
        } else {
            if (len > (b.length - ofs)) { len = b.length - ofs; }
            this.payload = new byte[len];
            System.arraycopy(b, ofs, this.payload, 0, len);
            this.size    = len;
            this.index   = 0;
        }
        this.setBigEndian(bigEndian);
    }
    
    // ------------------------------------------------------------------------

    /* set true for big-endian, false for little-endian numeric encoding */
    public void setBigEndian(boolean bigEndFirst)
    {
        this.bigEndian = bigEndFirst;
    }
    
    /* return true if big-endian, false for little-endian */
    public boolean getBigEndian()
    {
        return this.bigEndian;
    }
    
    
    /* return true if big-endian, false if little-endian */
    public boolean isBigEndian()
    {
        return this.bigEndian;
    }
    
    // ------------------------------------------------------------------------

    /* return number of bytes written to payload, or total bytes available to read */
    public int getSize()
    {
        return this.size;
    }
    
    /* reset payload to empty state */
    public void clear()
    {
        this.size  = 0;
        this.index = 0;
    }
    
    // ------------------------------------------------------------------------

    /* return backing byte array (as-is) */
    private byte[] _getBytes()
    {
        return this.payload;
    }
    
    /* return a byte array representing the data currently in the payload (may be a copy) */
    public byte[] getBytes()
    {
        // return the full payload (regardless of the state of 'this.index')
        byte b[] = this._getBytes();
        if (this.size == b.length) {
            return b;
        } else {
            byte n[] = new byte[this.size];
            System.arraycopy(b, 0, n, 0, this.size);
            return n;
        }
    }
    
    // ------------------------------------------------------------------------

    /* return index */
    public int getIndex()
    {
        return this.index;
    }
    
    /* reset the read/write index to '0' */
    public void resetIndex()
    {
        // this makes Payload a data source
        this.resetIndex(0);
    }

    /* reset the read/write index to the specified value */
    public void resetIndex(int ndx)
    {
        this.index = (ndx <= 0)? 0 : ndx;
    }

    /* return remaining available read bytes */
    public int getAvailableReadLength()
    {
        return (this.size - this.index);
    }

    /* return remaining available read bytes */
    public int getAvailableWriteLength()
    {
        byte b[] = this._getBytes();
        return (b.length - this.index);
    }

    /* return true if there are at least 'length' bytes that can be read from the payload */
    public boolean isValidReadLength(int length)
    {
        return ((this.index + length) <= this.size);
    }
    
    /* return true if there are at least 'length' available bytes that can be written to the payload */
    public boolean isValidWriteLength(int length)
    {
        byte b[] = this._getBytes();
        return ((this.index + length) <= b.length);
    }

    /* return true if there are bytes available for reading */
    public boolean hasAvailableRead()
    {
        return (this.getAvailableReadLength() > 0);
    }

    /* return true if there are bytes available for reading */
    public boolean hasAvailableWrite()
    {
        return (this.getAvailableWriteLength() > 0);
    }

    // ------------------------------------------------------------------------

    /* skip bytes */
    public void readSkip(int length)
    {
        int maxLen = ((this.index + length) <= this.size)? length : (this.size - this.index);
        if (maxLen <= 0) {
            // nothing to skip
            return;
        } else {
            this.index += maxLen;
            return;
        }
    }
    
    /* skip X bytes in payload */
    public void skip(int length)
    {
        this.readSkip(length);
    }
    
    // ------------------------------------------------------------------------

    /* read length of bytes from payload */
    public byte[] readBytes(int length)
    {
        // This will read 'length' bytes, or the remaining bytes, whichever is less
        int maxLen = ((length >= 0) && ((this.index + length) <= this.size))? length : (this.size - this.index);
        if (maxLen <= 0) {
            // no room left
            return new byte[0];
        } else {
            byte n[] = new byte[maxLen];
            System.arraycopy(this._getBytes(), this.index, n, 0, maxLen);
            this.index += maxLen;
            return n;
        }
    }

    // ------------------------------------------------------------------------

    /* decode long value from bytes */
    public static long decodeLong(byte data[], int ofs, int len, boolean bigEndian, boolean signed, long dft)
    {
        if ((data != null) && (data.length >= (ofs + len))) {
            if (bigEndian) {
                // Big-Endian order
                // { 0x01, 0x02, 0x03 } -> 0x010203
                long n = (signed && ((data[ofs] & 0x80) != 0))? -1L : 0L;
                for (int i = ofs; i < ofs + len; i++) {
                    n = (n << 8) | ((long)data[i] & 0xFF); 
                }
                return n;
            } else {
                // Little-Endian order
                // { 0x01, 0x02, 0x03 } -> 0x030201
                long n = (signed && ((data[ofs + len - 1] & 0x80) != 0))? -1L : 0L;
                for (int i = ofs + len - 1; i >= ofs; i--) {
                    n = (n << 8) | ((long)data[i] & 0xFF); 
                }
                return n;
            }
        } else {
            return dft;
        }
    }

    /* read long value from payload (with default) */
    public long readLong(int length, long dft)
    {
        int maxLen = ((this.index + length) <= this.size)? length : (this.size - this.index);
        if (maxLen <= 0) {
            // nothing to read
            return dft;
        } else {
            byte b[] = this._getBytes();
            long val = Payload.decodeLong(b, this.index, maxLen, this.bigEndian, true, dft);
            this.index += maxLen;
            return val;
        }
    }

    /* read long value from payload */
    public long readLong(int length)
    {
        return this.readLong(length, 0L);
    }
    
    // ------------------------------------------------------------------------

    /* read unsigned long value from payload (with default) */
    public long readULong(int length, long dft)
    {
        int maxLen = ((this.index + length) <= this.size)? length : (this.size - this.index);
        if (maxLen <= 0) {
            // nothing to read
            return dft;
        } else {
            byte b[] = this._getBytes();
            long val = Payload.decodeLong(b, this.index, maxLen, this.bigEndian, false, dft);
            this.index += maxLen;
            return val;
        }
    }

    /* read unsigned long value from payload */
    public long readULong(int length)
    {
        return this.readULong(length, 0L);
    }

    // ------------------------------------------------------------------------
    
    /* decode double value from bytes */
    public static double decodeDouble(byte data[], int ofs, int len, boolean bigEndian, double dft)
    {
        // 'len' must be at lest 4
        if ((data != null) && (len >= 4) && (data.length >= (ofs + len))) {
            int flen = (len >= 8)? 8 : 4;
            long n = 0L;
            if (bigEndian) {
                // Big-Endian order
                // { 0x01, 0x02, 0x03, 0x04 } -> 0x01020304
                for (int i = ofs; i < ofs + flen; i++) {
                    n = (n << 8) | ((long)data[i] & 0xFF);
                }
            } else {
                // Little-Endian order
                // { 0x01, 0x02, 0x03, 0x04 } -> 0x04030201
                for (int i = ofs + flen - 1; i >= ofs; i--) {
                    n = (n << 8) | ((long)data[i] & 0xFF);
                }
            }
            if (flen == 8) {
                //Print.logInfo("Decoding 64-bit float " + n);
                return Double.longBitsToDouble(n);
            } else {
                //Print.logInfo("Decoding 32-bit float " + n);
                return (double)Float.intBitsToFloat((int)n);
            }
        } else {
            return dft;
        }
    }

    /* read double value from payload (with default) */
    public double readDouble(int length, double dft)
    {
        // 'length' must be at least 4
        int maxLen = ((this.index + length) <= this.size)? length : (this.size - this.index);
        if (maxLen <= 0) {
            // nothing to read
            return dft;
        } else {
            byte b[] = this._getBytes();
            double val = Payload.decodeDouble(b, this.index, maxLen, this.bigEndian, dft);
            this.index += maxLen;
            return val;
        }
    }

    /* read double value from payload */
    public double readDouble(int length)
    {
        // 'length' must be at least 4
        return this.readDouble(length, 0.0);
    }

    // ------------------------------------------------------------------------

    /* read a variable length string from the payload */
    public String readString(int length)
    {
        return this.readString(length, true);
    }
    
    /* read a string from the payload */
    public String readString(int length, boolean varLength)
    {
        // Read until (whichever comes first):
        //  1) length bytes have been read
        //  2) a null (0x00) byte is found (if 'varLength==true')
        //  3) end of data is reached
        int maxLen = ((this.index + length) <= this.size)? length : (this.size - this.index);
        if (maxLen <= 0) {
            // no room left
            return "";
        } else {
            int m;
            byte b[] = this._getBytes();
            if (varLength) {
                // look for the end-of-data, or a terminating null (0x00)
                for (m = 0; (m < maxLen) && ((this.index + m) < this.size) && (b[this.index + m] != 0); m++);
            } else {
                // look for end of data only
                m = ((this.index + maxLen) < this.size)? maxLen : (this.size - this.index);
            }
            String s = StringTools.toStringValue(b, this.index, m);
            this.index += m;
            if (m < maxLen) { this.index++; }
            return s;
        }
    }
    
    // ------------------------------------------------------------------------

    /* read an encoded GPS point (latitude,longitude) from the payload */
    public GeoPoint readGPS(int length)
    {
        int maxLen = ((this.index + length) <= this.size)? length : (this.size - this.index);
        if (maxLen < 6) {
            // not enough bytes to decode GeoPoint
            GeoPoint gp = new GeoPoint();
            if (maxLen > 0) { this.index += maxLen; }
            return gp;
        } else
        if (length < 8) {
            // 6 <= len < 8
            GeoPoint gp = GeoPoint.decodeGeoPoint(this._getBytes(), this.index, length);
            this.index += maxLen; // 6
            return gp;
        } else {
            // 8 <= len
            GeoPoint gp = GeoPoint.decodeGeoPoint(this._getBytes(), this.index, length);
            this.index += maxLen; // 8
            return gp;
        }
    }

    // ------------------------------------------------------------------------

    /* encode a long value into the specified byte array */
    public static int encodeLong(byte data[], int ofs, int len, boolean bigEndian, long val)
    {
        if ((data != null) && (data.length >= (ofs + len))) {
            long n = val;
            if (bigEndian) {
                // Big-Endian order
                for (int i = (ofs + len - 1); i >= ofs; i--) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            } else {
                // Little-Endian order
                for (int i = ofs; i < ofs + len; i++) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            }
            return len;
        } else {
            return 0;
        }
    }

    /* write the specified long to the payload */
    public int writeLong(long val, int wrtLen)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* write float/double */
        byte b[] = this._getBytes();
        int maxLen = ((this.index + wrtLen) <= b.length)? wrtLen : (b.length - this.index);
        if (maxLen < wrtLen) {
            // not enough bytes to encode long
            return 0;
        }

        /* write long */
        Payload.encodeLong(b, this.index, maxLen, this.bigEndian, val);
        this.index += maxLen;
        if (this.size < this.index) { this.size = this.index; }
        return maxLen;
        
    }

    /* write the specified long (as unsigned) to the payload */
    public int writeULong(long val, int length)
    {
        return this.writeLong(val, length);
    }

    // ------------------------------------------------------------------------

    /* encode a long value into the specified byte array */
    public static int encodeDouble(byte data[], int ofs, int len, boolean bigEndian, double val)
    {
        // 'len' must be at least 4
        if ((data != null) && (len >= 4) && (data.length >= (ofs + len))) {
            int flen = (len >= 8)? 8 : 4;
            long n = (flen == 8)? Double.doubleToRawLongBits(val) : (long)Float.floatToRawIntBits((float)val);
            if (bigEndian) {
                // Big-Endian order
                for (int i = (ofs + flen - 1); i >= ofs; i--) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            } else {
                // Little-Endian order
                for (int i = ofs; i < ofs + flen; i++) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            }
            return len;
        } else {
            return 0;
        }
    }

    /* write the specified long to the payload */
    public int writeDouble(double val, int wrtLen)
    {
        // 'wrtLen' should be either 4 or 8

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* write float/double */
        byte b[] = this._getBytes();
        int maxLen = ((this.index + wrtLen) <= b.length)? wrtLen : (b.length - this.index);
        if (maxLen < 4) {
            // not enough bytes to encode float/double
            return 0;
        }

        /* write float/double */
        if (wrtLen < 8) {
            // 4 <= wrtLen < 8  [float]
            int len = Payload.encodeDouble(b, this.index, 4, this.bigEndian, val);
            this.index += 4;
            if (this.size < this.index) { this.size = this.index; }
            return 4;
        } else {
            // 8 <= wrtLen      [double]
            int len = Payload.encodeDouble(b, this.index, 8, this.bigEndian, val);
            this.index += 8;
            if (this.size < this.index) { this.size = this.index; }
            return 8;
        }
        
    }

    // ------------------------------------------------------------------------

    /* write a zero-filled buffer to the payload */
    public int writeZeroFill(int wrtLen)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* check for available space to write the data */
        byte b[] = this._getBytes();
        int maxLen = ((this.index + wrtLen) <= b.length)? wrtLen : (b.length - this.index);
        if (maxLen <= 0) {
            // no room left
            return 0;
        }

        /* fill field bytes with '0's, and adjust pointers */
        for (int m = 0; m < maxLen; m++) { b[this.index + m] = 0; }
        this.index += maxLen;
        if (this.size < this.index) { this.size = this.index; }

        /* return number of bytes written */
        return maxLen;

    }

    // ------------------------------------------------------------------------

    /* write an array of bytes to the payload */
    public int writeBytes(byte n[], int nOfs, int nLen, int wrtLen)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* adjust nOfs/nLen to fit within the byte array */
        if ((nOfs < 0) || (nLen <= 0) || (n == null)) {
            // invalid offset/length, or byte array
            return this.writeZeroFill(wrtLen);
        } else
        if (nOfs >= n.length) {
            // 'nOfs' is outside the array, nothing to write
            return this.writeZeroFill(wrtLen);
        } else
        if ((nOfs + nLen) > n.length) {
            // 'nLen' would extend beyond the end of the array
            nLen = n.length - nOfs; // nLen will be > 0
        }

        /* check for available space to write the data */
        byte b[] = this._getBytes();
        int maxLen = ((this.index + wrtLen) <= b.length)? wrtLen : (b.length - this.index);
        if (maxLen <= 0) {
            // no room left
            return 0;
        }

        /* write byte field */
        // copy 'm' bytes to buffer at current index
        int m = (nLen < maxLen)? nLen : maxLen;
        System.arraycopy(n, nOfs, b, this.index, m);

        /* fill remaining field bytes with '0's, and adjust pointers */
        for (;m < maxLen; m++) { b[this.index + m] = 0; }
        this.index += maxLen;
        if (this.size < this.index) { this.size = this.index; }

        /* return number of bytes written */
        return maxLen;

    }

    /* write an array of bytes to the payload */
    public int writeBytes(byte n[], int wrtLen)
    {
        return (n == null)? 
            this.writeZeroFill(wrtLen) :
            this.writeBytes(n, 0, n.length, wrtLen);
    }

    /* write an array of bytes to the payload */
    public int writeBytes(byte n[])
    {
        return (n == null)?
            0 :
            this.writeBytes(n, 0, n.length, n.length);
    }
    
    // ------------------------------------------------------------------------

    /* write a string to the payload */
    public int writeString(String s, int wrtLen)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* check for available space to write the data */
        byte b[] = this._getBytes();
        int maxLen = ((this.index + wrtLen) <= b.length)? wrtLen : (b.length - this.index);
        if (maxLen <= 0) {
            // no room left
            return 0;
        }
        
        /* empty string ('maxLen' is at least 1) */
        if ((s == null) || s.equals("")) {
            b[this.index++] = (byte)0;  // string terminator
            if (this.size < this.index) { this.size = this.index; }
            return 1;
        }

        /* write string bytes, and adjust pointers */
        byte n[] = StringTools.getBytes(s);
        int m = (n.length < maxLen)? n.length : maxLen;
        System.arraycopy(n, 0, b, this.index, m);
        this.index += m;
        if (m < maxLen) { 
            b[this.index++] = (byte)0; // terminate string
            m++;
        }
        if (this.size < this.index) { this.size = this.index; }

        /* return number of bytes written */
        return m;

    }

    // ------------------------------------------------------------------------

    /* encode a GPS point into the payload */
    public int writeGPS(double lat, double lon, int length)
    {
        return this.writeGPS(new GeoPoint(lat,lon), length);
    }
    
    /* encode a GPS point into the payload */
    public int writeGPS(GeoPoint gp, int wrtLen)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* check for available space to write the data */
        byte b[] = this._getBytes();
        int maxLen = ((this.index + wrtLen) <= b.length)? wrtLen : (b.length - this.index);
        if (maxLen < 6) {
            // not enough bytes to encode GeoPoint
            return 0;
        }
        
        /* write GPS point */
        if (wrtLen < 8) {
            // 6 <= wrtLen < 8
            int len = 6;
            GeoPoint.encodeGeoPoint(gp, b, this.index, len);
            this.index += len;
            if (this.size < this.index) { this.size = this.index; }
            // TODO: zero-fill (wrtLen - len) bytes?
            return len;
        } else {
            // 8 <= wrtLen
            int len = 8;
            GeoPoint.encodeGeoPoint(gp, b, this.index, len);
            this.index += len;
            if (this.size < this.index) { this.size = this.index; }
            // TODO: zero-fill (wrtLen - len) bytes?
            return len;
        }
        
    }

    // ------------------------------------------------------------------------

    /* return hex string representation */
    public String toString()
    {
        return StringTools.toHexString(this.payload, 0, this.size);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}

