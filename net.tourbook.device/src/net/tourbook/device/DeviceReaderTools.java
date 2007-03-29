/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.device;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.tourbook.data.TimeData;

public class DeviceReaderTools {

	/**
	 * Convert a byte[] array to readable string format. This makes the "hex"
	 * readable!
	 * 
	 * @return result String buffer in String format
	 * @param in
	 *        byte[] buffer to convert to string format
	 */

	public static String byteArrayToHexString(byte in[]) {

		byte ch = 0x00;

		int i = 0;

		if (in == null || in.length <= 0) {
			return null;
		}

		String pseudo[] = { "0", //$NON-NLS-1$
				"1", //$NON-NLS-1$
				"2", //$NON-NLS-1$
				"3", //$NON-NLS-1$
				"4", //$NON-NLS-1$
				"5", //$NON-NLS-1$
				"6", //$NON-NLS-1$
				"7", //$NON-NLS-1$
				"8", //$NON-NLS-1$
				"9", //$NON-NLS-1$
				"A", //$NON-NLS-1$
				"B", //$NON-NLS-1$
				"C", //$NON-NLS-1$
				"D", //$NON-NLS-1$
				"E", //$NON-NLS-1$
				"F" }; //$NON-NLS-1$

		StringBuffer out = new StringBuffer(in.length * 2 + in.length);

		while (i < in.length) {

			// Strip off high nibble
			ch = (byte) (in[i] & 0xF0);

			// shift the bits down
			ch = (byte) (ch >>> 4);

			// must do this is high order bit is on!
			ch = (byte) (ch & 0x0F);

			// convert the nibble to a String Character
			out.append(pseudo[(int) ch]);

			// Strip off low nibble
			ch = (byte) (in[i] & 0x0F);

			// convert the nibble to a String Character
			out.append(pseudo[(int) ch]);

			// add space between two bytes
			out.append(' ');

			i++;
		}

		String rslt = new String(out);

		return rslt;
	}
	public static int get2ByteData(RandomAccessFile file) throws IOException {

		int ch1 = file.read();
		int ch2 = file.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		int offset = (ch2 << 8) + (ch1 << 0);
		return (offset);
	}

	public static int convert1ByteBCD(byte[] buffer, int offset) {

		byte bufferByte = buffer[offset];

		int nipple1 = (bufferByte & 0x0F);
		int nipple2 = ((bufferByte & 0xF0) >> 4) * 10;

		return nipple2 + nipple1;
	}

	/**
	 * get a 2 byte value (unsigned integer) from the buffer, by swapping the
	 * high and low byte
	 * 
	 * @param buffer
	 * @param offset
	 * @return Returns unsigned integer address
	 */
	public static int get2ByteData(byte[] buffer, int offset) {
		int byte1 = (buffer[offset] & 0xFF) << 0;
		int byte2 = (buffer[offset + 1] & 0xFF) << 8;
		return byte2 + byte1;
	}

	public static long get4ByteData(byte[] buffer, int offset) {
		long byte1 = (buffer[offset] & 0xFF) << 0;
		long byte2 = (buffer[offset + 1] & 0xFF) << 8;
		long byte3 = (buffer[offset + 2] & 0xFF) << 16;
		long byte4 = (buffer[offset + 3] & 0xFF) << 24;
		return byte4 + byte3 + byte2 + byte1;
	}

	/**
	 * @param timeData
	 * @param rawData
	 * @throws IOException
	 */
	public static void readTimeSlice(int data, TimeData timeData) throws IOException {

		// pulse (4 bits)
		if ((data & 0x8000) != 0) {
			// -
			timeData.pulse = (short) ((0xFFF0 | ((data & 0xF000) >> 12)));
		} else {
			// +
			timeData.pulse = (short) (((data & 0xF000) >> 12));
		}

		// altitude (6 bits)
		if ((data & 0x0800) != 0) {
			// -
			timeData.altitude = (short) (0xFFC0 | ((data & 0x0FC0) >> 6));
			if (timeData.altitude < -16) {
				timeData.altitude = (short) (-16 + ((timeData.altitude + 16) * 7));
			}
		} else {
			// +
			timeData.altitude = (short) ((data & 0x0FC0) >> 6);
			if (timeData.altitude > 16) {
				timeData.altitude = (short) (16 + ((timeData.altitude - 16) * 7));
			}
		}

		// distance (6 bits)
		timeData.distance = (data & 0x003F) * 10;
	}
}
