/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
	 * Earth radius is 6367 km.
	 */
	static final double			EARTH_RADIUS	= 6367d;

	// --- Mathematic constants ---
	private static final double	DEGRAD			= Math.PI / 180.0d;

	/**
	 * Convert a byte[] array to readable string format. This makes the "hex" readable!
	 * 
	 * @return result String buffer in String format
	 * @param in
	 *            byte[] buffer to convert to string format
	 */

	public static String byteArrayToHexString(final byte in[]) {

		byte ch = 0x00;

		int i = 0;

		if (in == null || in.length <= 0) {
			return null;
		}

		final String pseudo[] = { "0", //$NON-NLS-1$
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

		final StringBuilder out = new StringBuilder(in.length * 2 + in.length);

		while (i < in.length) {

			// Strip off high nibble
			ch = (byte) (in[i] & 0xF0);

			// shift the bits down
			ch = (byte) (ch >>> 4);

			// must do this is high order bit is on!
			ch = (byte) (ch & 0x0F);

			// convert the nibble to a String Character
			out.append(pseudo[ch]);

			// Strip off low nibble
			ch = (byte) (in[i] & 0x0F);

			// convert the nibble to a String Character
			out.append(pseudo[ch]);

			// add space between two bytes
			out.append(' ');

			i++;
		}

		final String rslt = new String(out);

		return rslt;
	}

	public static int convert1ByteBCD(final byte[] buffer, final int offset) {

		final byte bufferByte = buffer[offset];

		final int nipple1 = (bufferByte & 0x0F);
		final int nipple2 = ((bufferByte & 0xF0) >> 4) * 10;

		return nipple2 + nipple1;
	}

	/**
	 * get a 2 byte value (unsigned integer) from the buffer, by swapping the high and low byte
	 * 
	 * @param buffer
	 * @param offset
	 * @return Returns unsigned integer address
	 */
	public static int get2ByteData(final byte[] buffer, final int offset) {
		final int byte1 = (buffer[offset] & 0xFF) << 0;
		final int byte2 = (buffer[offset + 1] & 0xFF) << 8;
		return byte2 + byte1;
	}

	public static int get2ByteData(final RandomAccessFile file) throws IOException {

		final int ch1 = file.read();
		final int ch2 = file.read();
		if ((ch1 | ch2) < 0) {
			throw new EOFException();
		}
		final int offset = (ch2 << 8) + (ch1 << 0);
		return (offset);
	}

	/**
	 * @param buffer
	 *            buffer where the data are stored
	 * @param offset
	 *            position in the buffer where the data are read
	 * @return
	 */
	public static long get4ByteData(final byte[] buffer, final int offset) {
		final long byte1 = (buffer[offset] & 0xFF) << 0;
		final long byte2 = (buffer[offset + 1] & 0xFF) << 8;
		final long byte3 = (buffer[offset + 2] & 0xFF) << 16;
		final long byte4 = (buffer[offset + 3] & 0xFF) << 24;
		return byte4 + byte3 + byte2 + byte1;
	}

	/**
	 * @param timeData
	 * @param rawData
	 * @throws IOException
	 */
	public static void readTimeSlice(final int data, final TimeData timeData) throws IOException {

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
				timeData.altitude = (-16 + ((timeData.altitude + 16) * 7));
			}
		} else {
			// +
			timeData.altitude = (short) ((data & 0x0FC0) >> 6);
			if (timeData.altitude > 16) {
				timeData.altitude = (16 + ((timeData.altitude - 16) * 7));
			}
		}

		// distance (6 bits)
		timeData.distance = (short) (data & 0x003F) * 10;
	}

	/**
	 * @return Return the distance in meters between two positions
	 */
	public static double computeDistance(double latitude1, double longitude1, double latitude2, double longitude2) {

		double a, c;

		// convert the degree values to radians before calculation
		latitude1 = latitude1 * DEGRAD;
		longitude1 = longitude1 * DEGRAD;
		latitude2 = latitude2 * DEGRAD;
		longitude2 = longitude2 * DEGRAD;

		final double dlon = longitude2 - longitude1;
		final double dlat = latitude2 - latitude1;

		a = Math.pow(Math.sin(dlat / 2), 2)
				+ Math.cos(latitude1)
				* Math.cos(latitude2)
				* Math.pow(Math.sin(dlon / 2), 2);

		c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return (EARTH_RADIUS * c) * 1000;
	}

}
