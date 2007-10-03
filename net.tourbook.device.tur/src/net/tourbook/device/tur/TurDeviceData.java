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

/**
 * @author Markus Stipp
 */

package net.tourbook.device.tur;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Contains all data read from the device except the tour data
 * 
 * @author Wolfgang Schramm
 */
public class TurDeviceData {

	public String	deviceHeader;
	public String	fileVersion;
	public String	deviceDistance;
	public String	deviceTime;
	public String	deviceAltUp;
	public String	deviceAltDown;
	public String	deviceMode;

	public String	personLastName;
	public String	personFirstName;
	public String	personBirthday;
	public String	personClub;
	public String	personWeight;
	public String	personMaxPulse;
	public String	personMinPulse;

	public String	bikeName;
	public String	bikeWeight;

	public String	tourTitle;
	public String	tourStartPlace;
	public String	tourEndPlace;
	public String	tourStartDate;
	public String	tourStartTime;
	public String	tourDescription;
	public String	tourDistance;
	public String	tourTotalTime;
	public String	tourMaxHeight;
	public String	tourAltUp;
	public String	tourAvgGradePerMinute;
	public String	tourAvgGrade;
	public String	tourAvgSpeed;
	public String	tourAvgTemperature;
	public String	tourAvgPulse;

	public String	dummy;

	public TurDeviceData() {}

	/**
	 * @param fileRawData
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public void readFromFile(FileInputStream fileRawData) throws IOException, NumberFormatException {

		deviceHeader = TurFileUtil.readText(fileRawData);

		fileVersion = TurFileUtil.readText(fileRawData) + "." + //$NON-NLS-1$
				TurFileUtil.readText(fileRawData)
				+ "." + //$NON-NLS-1$
				TurFileUtil.readText(fileRawData);

		tourTitle = TurFileUtil.readText(fileRawData);
		tourStartPlace = TurFileUtil.readText(fileRawData);
		tourEndPlace = TurFileUtil.readText(fileRawData);
		tourStartDate = TurFileUtil.readText(fileRawData);
		tourStartTime = TurFileUtil.readText(fileRawData);

		int lineCount = Integer.parseInt(TurFileUtil.readText(fileRawData));
		tourDescription = TurFileUtil.readDescription(fileRawData, lineCount);
		tourDistance = TurFileUtil.readText(fileRawData);
		tourTotalTime = TurFileUtil.readText(fileRawData);
		dummy = TurFileUtil.readText(fileRawData);
		tourMaxHeight = TurFileUtil.readText(fileRawData);
		tourAltUp = TurFileUtil.readText(fileRawData);
		tourAvgGradePerMinute = TurFileUtil.readText(fileRawData);
		tourAvgGrade = TurFileUtil.readText(fileRawData);
		tourAvgSpeed = TurFileUtil.readText(fileRawData);
		tourAvgTemperature = TurFileUtil.readText(fileRawData);
		tourAvgPulse = TurFileUtil.readText(fileRawData);
		dummy = TurFileUtil.readText(fileRawData);
		deviceMode = TurFileUtil.readText(fileRawData);
		dummy = TurFileUtil.readText(fileRawData);

		// Skip spare fields
		for (int i = 0; i < 7; i++) {
			dummy = TurFileUtil.readText(fileRawData);
		}

		personLastName = TurFileUtil.readText(fileRawData);
		personFirstName = TurFileUtil.readText(fileRawData);
		personBirthday = TurFileUtil.readText(fileRawData);
		personClub = TurFileUtil.readText(fileRawData);
		personWeight = TurFileUtil.readText(fileRawData);
		personMaxPulse = TurFileUtil.readText(fileRawData);
		personMinPulse = TurFileUtil.readText(fileRawData);
		bikeName = TurFileUtil.readText(fileRawData);
		bikeWeight = TurFileUtil.readText(fileRawData);
		deviceDistance = TurFileUtil.readText(fileRawData);
		deviceTime = TurFileUtil.readText(fileRawData);
		deviceAltUp = TurFileUtil.readText(fileRawData);
		deviceAltDown = TurFileUtil.readText(fileRawData);

		// Skip spare fields
		for (int i = 0; i < 10; i++) {
			dummy = TurFileUtil.readText(fileRawData);
		}

		// dumpData();
	}

	public void dumpData() {

		PrintStream out = System.out;

		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("File Data"); //$NON-NLS-1$
		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println(""); //$NON-NLS-1$

		out.println("Header:  " + deviceHeader); //$NON-NLS-1$
		out.println("Version: " + fileVersion); //$NON-NLS-1$
		out.println(""); //$NON-NLS-1$

		out.println("Device-Data:"); //$NON-NLS-1$
		out.println("-------------"); //$NON-NLS-1$
		out.println("Total Distance: " + deviceDistance); //$NON-NLS-1$
		out.println("Total Time:     " + deviceTime); //$NON-NLS-1$
		out.println("Total Alt Up    " + deviceAltUp); //$NON-NLS-1$
		out.println("Total Alt Down  " + deviceAltDown); //$NON-NLS-1$
		out.println("Device Mode:    " + deviceMode); //$NON-NLS-1$
		out.println(""); //$NON-NLS-1$

		out.println("Biker-Data:"); //$NON-NLS-1$
		out.println("-------------"); //$NON-NLS-1$
		out.println("FirstName: " + personFirstName); //$NON-NLS-1$
		out.println("LastName:  " + personLastName); //$NON-NLS-1$
		out.println("Birthday:  " + personBirthday); //$NON-NLS-1$
		out.println("Club:      " + personClub); //$NON-NLS-1$
		out.println("Weight:    " + personWeight); //$NON-NLS-1$
		out.println("MaxPulse:  " + personMaxPulse); //$NON-NLS-1$
		out.println("MinPulse:  " + personMinPulse); //$NON-NLS-1$
		out.println(""); //$NON-NLS-1$

		out.println("Bike-Data:"); //$NON-NLS-1$
		out.println("-------------"); //$NON-NLS-1$
		out.println("Bike:   " + bikeName); //$NON-NLS-1$
		out.println("Weight: " + bikeWeight); //$NON-NLS-1$
		out.println(""); //$NON-NLS-1$

		out.println("Tour-Data:"); //$NON-NLS-1$
		out.println("-------------"); //$NON-NLS-1$
		out.println("Title: " + tourTitle); //$NON-NLS-1$
		out.println("StartPlace: " + tourStartPlace); //$NON-NLS-1$
		out.println("EndPlace:   " + tourEndPlace); //$NON-NLS-1$
		out.println("StartDate:  " + tourStartDate); //$NON-NLS-1$
		out.println("StartTime:  " + tourStartTime); //$NON-NLS-1$
		out.println("Description: "); //$NON-NLS-1$
		out.println("-------------"); //$NON-NLS-1$
		out.println(tourDescription);
		out.println("-------------"); //$NON-NLS-1$
		out.println("Distance:   " + tourDistance); //$NON-NLS-1$
		out.println("Time:       " + tourTotalTime); //$NON-NLS-1$
		out.println("Max Height: " + tourMaxHeight); //$NON-NLS-1$
		out.println("Alt Up:     " + tourAltUp); //$NON-NLS-1$
		out.println("Avg Grade pm: " + tourAvgGradePerMinute); //$NON-NLS-1$
		out.println("Avg Grade:  " + tourAvgGrade); //$NON-NLS-1$
		out.println("Avg Speed:  " + tourAvgSpeed); //$NON-NLS-1$
		out.println("Avg Temp:   " + tourAvgTemperature); //$NON-NLS-1$
		out.println("Avg Pulse:  " + tourAvgPulse); //$NON-NLS-1$

	}

}
