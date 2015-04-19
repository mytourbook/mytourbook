/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.listeners.ActivityMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.BikeProfileMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.DeviceInfoMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.EventMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.FileCreatorMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.FileIdMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.LapMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.RecordMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.SessionMesgListenerImpl;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import com.garmin.fit.Decode;
import com.garmin.fit.Field;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;

/**
 * Garmin FIT activity reader based on the official Garmin SDK.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 * @author Wolfgang Schramm
 */
public class FitDataReader extends TourbookDevice {

	@SuppressWarnings("unused")
	private void addAllLogListener(final MesgBroadcaster broadcaster) {

		broadcaster.addListener(new MesgListener() {
			@Override
			public void onMesg(final Mesg mesg) {

				long garminTimestamp = 0;

				for (final Field field : mesg.getFields()) {

					final String fieldName = field.getName();

//					if (fieldName.equals("temperature")) { //$NON-NLS-1$
//						int a = 0;
//						a++;
//					}

					if (fieldName.equals("timestamp")) { //$NON-NLS-1$
						garminTimestamp = (Long) field.getValue();
					}

					/*
					 * Set fields which should NOT be displayed in the log
					 */
					if (fieldName.equals("") // //$NON-NLS-1$

							|| fieldName.equals("timestamp") //$NON-NLS-1$

							//
							// record data
							//

							|| fieldName.equals("event") //$NON-NLS-1$
							|| fieldName.equals("event_type") //$NON-NLS-1$
							|| fieldName.equals("message_index") //$NON-NLS-1$

							|| fieldName.equals("altitude") //$NON-NLS-1$
							|| fieldName.equals("cadence") //$NON-NLS-1$
							|| fieldName.equals("distance") //$NON-NLS-1$
							|| fieldName.equals("fractional_cadence") //$NON-NLS-1$
							|| fieldName.equals("grade") //$NON-NLS-1$
							|| fieldName.equals("heart_rate") //$NON-NLS-1$
							|| fieldName.equals("position_lat") //$NON-NLS-1$
							|| fieldName.equals("position_long") //$NON-NLS-1$
							|| fieldName.equals("speed") //$NON-NLS-1$
							|| fieldName.equals("compressed_speed_distance") //$NON-NLS-1$
							|| fieldName.equals("temperature") //$NON-NLS-1$

							|| fieldName.equals("front_gear") //$NON-NLS-1$
							|| fieldName.equals("front_gear_num") //$NON-NLS-1$
							|| fieldName.equals("rear_gear") //$NON-NLS-1$
							|| fieldName.equals("rear_gear_num") //$NON-NLS-1$

							|| fieldName.equals("enhanced_altitude") //$NON-NLS-1$
							|| fieldName.equals("enhanced_speed") //$NON-NLS-1$
							|| fieldName.equals("enhanced_avg_speed") //$NON-NLS-1$
							|| fieldName.equals("enhanced_max_speed") //$NON-NLS-1$

							//
							// lap data
							//
							|| fieldName.equals("avg_cadence") //$NON-NLS-1$
							|| fieldName.equals("avg_fractional_cadence") //$NON-NLS-1$
							|| fieldName.equals("avg_heart_rate") //$NON-NLS-1$
							|| fieldName.equals("avg_speed") //$NON-NLS-1$
							|| fieldName.equals("data") //$NON-NLS-1$
							|| fieldName.equals("device_index") //$NON-NLS-1$
							|| fieldName.equals("device_type") //$NON-NLS-1$
							|| fieldName.equals("event_group") //$NON-NLS-1$
							|| fieldName.equals("end_position_lat") //$NON-NLS-1$
							|| fieldName.equals("end_position_long") //$NON-NLS-1$
							|| fieldName.equals("intensity") //$NON-NLS-1$
							|| fieldName.equals("lap_trigger") //$NON-NLS-1$
							|| fieldName.equals("max_cadence") //$NON-NLS-1$
							|| fieldName.equals("max_fractional_cadence") //$NON-NLS-1$
							|| fieldName.equals("max_heart_rate") //$NON-NLS-1$
							|| fieldName.equals("max_speed") //$NON-NLS-1$
							|| fieldName.equals("total_calories") //$NON-NLS-1$
							|| fieldName.equals("total_fat_calories") //$NON-NLS-1$
							|| fieldName.equals("sport") //$NON-NLS-1$
							|| fieldName.equals("start_position_lat") //$NON-NLS-1$
							|| fieldName.equals("start_position_long") //$NON-NLS-1$
							|| fieldName.equals("start_time") //$NON-NLS-1$
							|| fieldName.equals("total_ascent") //$NON-NLS-1$
							|| fieldName.equals("total_descent") //$NON-NLS-1$
							|| fieldName.equals("total_cycles") //$NON-NLS-1$
							|| fieldName.equals("total_distance") //$NON-NLS-1$
							|| fieldName.equals("total_elapsed_time") //$NON-NLS-1$
							|| fieldName.equals("total_timer_time") //$NON-NLS-1$
							//
							|| fieldName.equals("unknown") //$NON-NLS-1$
					//
					) {
						continue;
					}

					final long linuxTime = (garminTimestamp * 1000) + com.garmin.fit.DateTime.OFFSET;

					System.out.println(String.format("%s %d %s %-5d %-30s %20s %s", //$NON-NLS-1$
							new DateTime(linuxTime), // show readable date/time
							linuxTime / 1000,
							Long.toString(garminTimestamp),
							field.getNum(),
							fieldName,
							field.getValue(),
							field.getUnits()));
				}
			}
		});
	}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	@Override
	public String getDeviceModeName(final int modeId) {
		return null;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return 0;
	}

	@Override
	public int getTransferDataSize() {
		return 0;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		boolean returnValue = false;

		FileInputStream fis = null;

		try {

			fis = new FileInputStream(importFilePath);

			final MesgBroadcaster broadcaster = new MesgBroadcaster(new Decode());

			final FitContext context = new FitContext(//
					this,
					importFilePath,
					alreadyImportedTours,
					newlyImportedTours);

			// setup all fit listeners
			broadcaster.addListener(new ActivityMesgListenerImpl(context));
			broadcaster.addListener(new BikeProfileMesgListenerImpl(context));
			broadcaster.addListener(new DeviceInfoMesgListenerImpl(context));
			broadcaster.addListener(new EventMesgListenerImpl(context));
			broadcaster.addListener(new FileCreatorMesgListenerImpl(context));
			broadcaster.addListener(new FileIdMesgListenerImpl(context));
			broadcaster.addListener(new LapMesgListenerImpl(context));
			broadcaster.addListener(new RecordMesgListenerImpl(context));
			broadcaster.addListener(new SessionMesgListenerImpl(context));

//			//
//			// START - show debug info
//			//
//
//			System.out.println();
//			System.out.println();
//			System.out.println((System.currentTimeMillis() + " [" + getClass().getSimpleName() + "]")
//					+ (" \t" + importFilePath));
//			System.out.println();
//			System.out.println(String.format(//
//					"%s %-5s %-30s %20s %s", //$NON-NLS-1$
//					"Timestamp",
//					"Num",
//					"Name",
//					"Value",
//					"Units"));
//			System.out.println();
//
//			addAllLogListener(broadcaster);
//
//			//
//			// END - show debug info
//			//

			broadcaster.run(fis);

			context.finalizeTour();

			returnValue = true;

		} catch (final FileNotFoundException e) {
			StatusUtil.log("Could not read data file '" + importFilePath + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			IOUtils.closeQuietly(fis);
		}

		return returnValue;
	}

	@Override
	public boolean validateRawData(final String fileName) {

		boolean returnValue = false;
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(fileName);
			returnValue = Decode.checkIntegrity(fis);
		} catch (final FileNotFoundException e) {
			StatusUtil.log("Could not read data file '" + fileName + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (final FitRuntimeException e) {
			StatusUtil.log("Invalid data file '" + fileName + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			IOUtils.closeQuietly(fis);
		}

		return returnValue;
	}

}
