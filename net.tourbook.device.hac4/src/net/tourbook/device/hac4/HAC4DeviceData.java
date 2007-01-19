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
/*
 * Author: Wolfgang Schramm Created: 23.05.2005
 */

package net.tourbook.device.hac4;

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.HashMap;

import net.tourbook.data.DataUtil;


/**
 * Contains all data read from the device except the tour data
 * 
 * @author Wolfgang Schramm
 */
public class HAC4DeviceData {

	/**
	 * cccc (h) "B735"
	 */
	public String									deviceType;

	/**
	 * pppp (h) wheel perimeter (mm)
	 */
	public int										wheelPerimeter;

	/**
	 * wwww (h) weight (kg)
	 */
	public int										personWeight;

	/**
	 * aaaa (h) home altitude (m) "FFFF" not set
	 * 
	 */
	public int										homeAltitude;

	/**
	 * bbbb (h) 1. pulse upper bound (bpm)
	 */
	public int										pulse1UpperBound;

	/**
	 * cccc (h) 1. pulse lower bound (bpm)
	 */
	public int										pulse1LowerBound;

	/**
	 * dddd (h) 2. pulse upper bound (bpm)
	 */
	public int										pulse2UpperBound;

	/**
	 * eeee (h) 2. pulse lower bound (bpm)
	 */
	public int										pulse2LowerBound;

	/**
	 * aa (d) 1. count down minutes
	 */
	public short									count1min;

	/**
	 * bb (d) 1. count down seconds
	 */
	public short									count1sec;

	/**
	 * cc (d) 2. count down minutes
	 */
	public short									count2min;

	/**
	 * dd (d) 2. count down seconds
	 */
	public short									count2sec;

	/**
	 * llll (h) total distance at end of last tour (km) * 2^16
	 */
	public int										totalDistanceHigh;

	/**
	 * kkkk (h) total distance at end of last tour (km)
	 */
	public int										totalDistanceLow;

	/**
	 * eeee (h) altitude error correction
	 */
	public int										altitudeError;

	/**
	 * uuuu (h) total altitude up at end of last tour (m)
	 */
	public int										totalAltitudeUp;

	/**
	 * dddd (h) total altitude down at end of last tour (m)
	 */
	public int										totalAltitudeDown;

	/**
	 * aaaa (h) max altitude (m)
	 */
	public int										maxAltitude;

	/**
	 * hh (d) hour of total travel time
	 */
	public short									totalTravelTimeHourLow;

	/**
	 * HH (d) hour of total travel time * 100
	 */
	public short									totalTravelTimeHourHigh;

	/**
	 * ss (d) seconds of total travel time
	 */
	public short									totalTravelTimeSec;

	/**
	 * mm (d) minute of total travel time
	 */
	public short									totalTravelTimeMin;

	/**
	 * oooo (h) next free memory offset
	 */
	public int										offsetNextMemory;

	/**
	 * cccc (o) offset of last CC-record
	 */
	public int										offsetCCRecord;

	/**
	 * dddd (o) offset of last DD-record
	 */
	public int										offsetDDRecord;

	/**
	 * eeee (o) offset of last compare record
	 */
	public int										offsetCompareRecord;

	/**
	 * yyyy (d) year of transfer
	 */
	public short									transferYear;

	/**
	 * mm (d) month of transfer
	 */
	public short									transferMonth;

	/**
	 * dd (d) day of transfer
	 */
	public short									transferDay;


	private static final HashMap<String, String>	deviceNames	= new HashMap<String, String>();


	/**
	 * 
	 */
	public HAC4DeviceData() {

		deviceNames.put("B735", "HAC4"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceNames.put("B723", "CM414M"); //$NON-NLS-1$ //$NON-NLS-2$
	}


	/**
	 * Converts the decoded device name into the device product name
	 * 
	 * @param deviceCodeName
	 * @return product name
	 */
	public static String getDecodedDeviceType(String deviceCodeName) {

		if (deviceNames.containsKey(deviceCodeName)) {
			return (String) deviceNames.get(deviceCodeName);
		} else {
			return (deviceCodeName);
		}
	}


	/**
	 * @param fileRawData
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public void readFromFile(RandomAccessFile fileRawData) throws IOException,
			NumberFormatException
	{

		byte[] buffer = new byte[5];

		fileRawData.read(buffer);
		deviceType = new String(buffer, 0, 4);

		fileRawData.read(buffer);
		wheelPerimeter = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		personWeight = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		homeAltitude = Integer.parseInt(new String(buffer, 0, 4), 16);

		// pulse 1/2 upper/lower
		fileRawData.read(buffer);
		pulse1UpperBound = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		pulse1LowerBound = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		pulse2UpperBound = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		pulse2LowerBound = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		count1min = Short.parseShort(new String(buffer, 0, 2));
		count1sec = Short.parseShort(new String(buffer, 2, 2));

		fileRawData.read(buffer);
		count2min = Short.parseShort(new String(buffer, 0, 2));
		count2sec = Short.parseShort(new String(buffer, 2, 2));

		fileRawData.read(buffer);
		altitudeError = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalDistanceHigh = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalDistanceLow = Integer.parseInt(new String(buffer, 0, 4), 16);

		offsetNextMemory = DataUtil.readFileOffset(fileRawData, buffer);

		fileRawData.read(buffer);
		transferYear = Short.parseShort(new String(buffer, 0, 4));

		fileRawData.read(buffer);
		transferMonth = Short.parseShort(new String(buffer, 0, 2));
		transferDay = Short.parseShort(new String(buffer, 2, 2));

		fileRawData.read(buffer);
		totalAltitudeUp = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalAltitudeDown = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		maxAltitude = Integer.parseInt(new String(buffer, 0, 4), 16);

		fileRawData.read(buffer);
		totalTravelTimeHourLow = Short.parseShort(new String(buffer, 0, 2));
		totalTravelTimeHourHigh = Short.parseShort(new String(buffer, 2, 2));

		fileRawData.read(buffer);
		totalTravelTimeSec = Short.parseShort(new String(buffer, 0, 2));
		totalTravelTimeMin = Short.parseShort(new String(buffer, 2, 2));

		offsetCCRecord = DataUtil.readFileOffset(fileRawData, buffer);
		offsetDDRecord = DataUtil.readFileOffset(fileRawData, buffer);
		offsetCompareRecord = DataUtil.readFileOffset(fileRawData, buffer);
		
//		dumpData();
	}


	public void dumpData() {

		PrintStream out = System.out;

		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("DEVICE DATA"); //$NON-NLS-1$
		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("Transfer date:			" + transferDay + "." + transferMonth + "." + transferYear); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		out.println("Device:				" + deviceType); //$NON-NLS-1$
		out.println("Wheel perimeter:		" + wheelPerimeter + " mm"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Person weight:			" + personWeight + " kg"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println();
		out.println("Home altitude:			" + homeAltitude + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Altitude error correction:	" + altitudeError); //$NON-NLS-1$
		out.println("Max Altitude:			" + maxAltitude + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println();
		out.println("Pulse 1 upper bound		" + pulse1UpperBound + " bpm"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Pulse 1 lower bound:		" + pulse1LowerBound + " bpm"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Pulse 2 upper bound:		" + pulse2UpperBound + " bpm"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Pulse 2 lower bound:		" + pulse2LowerBound + " bpm"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println();
		out.println("1. Count down:			" + count1min + ":" + count1sec); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("2. Count down:			" + count2min + ":" + count2sec); //$NON-NLS-1$ //$NON-NLS-2$
		out.println();
		out.println("Total distance:			" + ((totalDistanceHigh * (2 ^ 16)) + totalDistanceLow) //$NON-NLS-1$
				+ " km"); //$NON-NLS-1$
		out.println("Total altitude up:		" + totalAltitudeUp + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total altitude down:		" + totalAltitudeDown + " m"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total travel time: 		" //$NON-NLS-1$
				+ ((totalTravelTimeHourHigh * 100) + totalTravelTimeHourLow) + ":" //$NON-NLS-1$
				+ totalTravelTimeMin + ":" + totalTravelTimeSec); //$NON-NLS-1$
		out.println();
		out.println("Offset last CC record:		" + offsetCCRecord); //$NON-NLS-1$
		out.println("Offset last DD record:		" + offsetDDRecord); //$NON-NLS-1$
		out.println("Offset next free memory:	" + offsetNextMemory); //$NON-NLS-1$
		out.println("Offset compare record:		" + offsetCompareRecord); //$NON-NLS-1$

	}


}
