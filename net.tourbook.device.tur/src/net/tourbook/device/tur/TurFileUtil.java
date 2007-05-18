/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

/**
 * @author Markus Stipp
 */

package net.tourbook.device.tur;

import java.io.IOException;
import java.io.InputStream;

public class TurFileUtil {
	public static int readByte(InputStream in) throws IOException {
		int val = 0;
			byte buf[] = new byte[1];
			in.read(buf, 0, 1);
			val = unsign(buf[0]);
		return val;
	}

	public static String readText(InputStream in) {
		String buf = "";
		try {
			byte cBuf[] = new byte[1];
			do {
				in.read(cBuf, 0, 1);
				if (cBuf[0] == 10)
					break;
				buf = buf + (char) unsign(cBuf[0]);
			} while (cBuf[0] != 0);
		} catch (IOException e) {
			return buf;
		}
		return buf;
	}

	public static String readDescription(InputStream in, int lineCount) {
		String resultStr = new String();
		for (int i = 0; i < lineCount; i++) {
			resultStr = resultStr + readText(in);
			if (i + 1 < lineCount)
				resultStr = resultStr + "\n";
		}

		return resultStr;
	}

	public static int unsign(byte b) {
		int val = b;
		if (val < 0) {
			val *= -1;
			val = (128 - val) + 128;
		}
		return val;
	}

}
