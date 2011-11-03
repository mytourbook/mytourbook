/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm, Markus Stipp
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

package net.tourbook.device.cm4xxm;

/*
 * author & copyright: Markus Stipp
 */

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import net.tourbook.data.DataUtil;

/**
 * Contains all data read from the device except the tour data
 */
public class CM4XXMDeviceData {

	/**
	 * cccc (h) "B723"
	 */
	public String	deviceType;

	/**
	 * pppp (h) wheel perimeter bike 1 (mm)
	 */
	public int		wheelPerimeter1;

	/**
	 * qqqq (h) wheel perimeter bike 2 (mm)
	 */
	public int		wheelPerimeter2;

	/**
	 * ???? (h) unknown Data
	 */
	public int		unknownData1;
	public int		unknownData2;
	public int		unknownData3;
	public int		unknownData4;
	public int		unknownData5;
	public int		unknownData6;
	public int		unknownData7;

	/**
	 * aaaa (h) home altitude (m) "FFFF" not set
	 */
	public int		homeAltitude;

	/**
	 * wwww (h) weight (kg)
	 */
	public int		personWeight;

	/**
	 * llll (h) total distance at end of last tour bike 1 (km)
	 */
	public int		totalDistance1;

	/**
	 * kkkk (h) total distance at end of last tour bike 2 (km)
	 */
	public int		totalDistance2;

	/**
	 * uuuu (h) total altitude up at end of last tour (m)
	 */
	public int		totalAltitudeUp1;

	/**
	 * dddd (h) total altitude down at end of last tour (m)
	 */
	public int		totalAltitudeUp2;

	/**
	 * aaaa (h) max altitude (m)
	 */
	public int		maxAltitude;

	/**
	 * hh (d) hour of total travel time bike 1
	 */
	public short	totalTravelTimeHour1;

	public short	totalTravelTimeHour2;

	/**
	 * ss (d) seconds of total travel time
	 */
	public short	totalTravelTimeSec1;

	/**
	 * mm (d) minute of total travel time
	 */
	public short	totalTravelTimeMin1;

	/**
	 * ss (d) seconds of total travel time
	 */
	public short	totalTravelTimeSec2;

	/**
	 * mm (d) minute of total travel time
	 */
	public short	totalTravelTimeMin2;

	/**
	 * oooo (h) next free memory offset
	 */
	public int		offsetNextMemory;

	/**
	 * dddd (o) offset of last DD-record
	 */
	public int		offsetDDRecord;

	/**
	 * yyyy (d) year of transfer
	 */
	public short	transferYear;

	/**
	 * mm (d) month of transfer
	 */
	public short	transferMonth;

	/**
	 * dd (d) day of transfer
	 */
	public short	transferDay;

	public CM4XXMDeviceData() {}

	/**
	 * @param fileRawData
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public void readFromFile(RandomAccessFile fileRawData) throws IOException,
			NumberFormatException {

		byte[] buffer = new byte[5];

		fileRawData.read(buffer);
		deviceType = new String(buffer, 0, 4);

		fileRawData.read(buffer);
		wheelPerimeter1 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		wheelPerimeter2 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		unknownData1 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		homeAltitude = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		personWeight = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		transferMonth = Short.parseShort(new String(buffer, 0, 2));
		transferDay = Short.parseShort(new String(buffer, 2, 2));

		fileRawData.read(buffer);
		transferYear = Short.parseShort(new String(buffer, 0, 4));

		fileRawData.read(buffer);
		unknownData2 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		unknownData3 = Integer.parseInt(new String(buffer, 0, 4), 16);

		offsetNextMemory = DataUtil.readFileOffset(fileRawData, buffer);
		offsetDDRecord = DataUtil.readFileOffset(fileRawData, buffer);

		fileRawData.read(buffer);
		totalAltitudeUp1 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalAltitudeUp2 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		unknownData4 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		unknownData5 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		unknownData6 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		unknownData7 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalDistance1 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalDistance2 = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalTravelTimeMin1 = Short.parseShort(new String(buffer, 2, 2));
		totalTravelTimeSec1 = Short.parseShort(new String(buffer, 0, 2));

		fileRawData.read(buffer);
		totalTravelTimeMin2 = Short.parseShort(new String(buffer, 2, 2));
		totalTravelTimeSec2 = Short.parseShort(new String(buffer, 0, 2));

		fileRawData.read(buffer);
		totalTravelTimeHour1 = Short.parseShort(new String(buffer, 0, 2));
		totalTravelTimeHour2 = Short.parseShort(new String(buffer, 2, 2));
	}

	public void dumpData() {

		PrintStream out = System.out;

		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("DEVICE DATA"); //$NON-NLS-1$
		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("Transfer date:     " + transferDay + "." + transferMonth + "." + transferYear); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		out.println("Device:        " + deviceType); //$NON-NLS-1$
		out.println("Wheel perimeter bike 1:   " + wheelPerimeter1 + " mm"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Wheel perimeter bike 2:   " + wheelPerimeter2 + " mm"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Person weight:     " + personWeight + " kg"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println();
		out.println("Home altitude:     " + homeAltitude + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Max Altitude:      " + maxAltitude + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println();
		out.println();
		out.println();
		out.println("Total distance bike 1:      " + (totalDistance1) //$NON-NLS-1$
				+ " km"); //$NON-NLS-1$
		out.println("Total distance bike 2:      " + (totalDistance2) //$NON-NLS-1$
				+ " km"); //$NON-NLS-1$
		out.println("Total altitude up bike1:   " + totalAltitudeUp1 + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total altitude up bike2:   " + totalAltitudeUp2 + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total travel time bike 1:     " //$NON-NLS-1$
				+ totalTravelTimeHour1
				+ ":" //$NON-NLS-1$
				+ totalTravelTimeMin1
				+ ":" + totalTravelTimeSec1); //$NON-NLS-1$
		out.println("Total travel time bike 2:     " //$NON-NLS-1$
				+ totalTravelTimeHour2
				+ ":" //$NON-NLS-1$
				+ totalTravelTimeMin2
				+ ":" + totalTravelTimeSec2); //$NON-NLS-1$
		out.println();
		out.println("Offset last DD record:   " + offsetDDRecord); //$NON-NLS-1$
		out.println("Offset next free memory: " + offsetNextMemory); //$NON-NLS-1$
		out.println();
		out.println("Unknown Parameter 1:" + unknownData1); //$NON-NLS-1$
		out.println("Unknown Parameter 2:" + unknownData2); //$NON-NLS-1$
		out.println("Unknown Parameter 3:" + unknownData3); //$NON-NLS-1$
		out.println("Unknown Parameter 4:" + unknownData4); //$NON-NLS-1$
		out.println("Unknown Parameter 5:" + unknownData5); //$NON-NLS-1$
		out.println("Unknown Parameter 6:" + unknownData6); //$NON-NLS-1$
		out.println("Unknown Parameter 7:" + unknownData7); //$NON-NLS-1$

	}

}
