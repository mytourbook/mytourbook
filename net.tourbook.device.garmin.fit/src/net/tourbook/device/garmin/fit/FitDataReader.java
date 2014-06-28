/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import net.tourbook.device.garmin.fit.listeners.DeviceInfoMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.FileCreatorMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.FileIdMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.LapMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.RecordMesgListenerImpl;
import net.tourbook.device.garmin.fit.listeners.SessionMesgListenerImpl;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.apache.commons.io.IOUtils;

import com.garmin.fit.Decode;
import com.garmin.fit.Field;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;

/**
 * Garmin FIT activity reader based on official Garmin SDK.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 * @author Wolfgang Schramm
 */
public class FitDataReader extends TourbookDevice {

	@SuppressWarnings("unused")
	private void addDebugListener(final MesgBroadcaster broadcaster) {

		broadcaster.addListener(new MesgListener() {
			@Override
			public void onMesg(final Mesg mesg) {
				for (final Field field : mesg.getFields()) {

					final String fieldName = field.getName();

//					if (fieldName.equals("temperature")) {
//						int a = 0;
//						a++;
//					}

					if (fieldName.equals("") //
							//
							// record data
							//
							|| fieldName.equals("timestamp")
							|| fieldName.equals("event")
							|| fieldName.equals("event_type")
//							|| fieldName.equals("message_index")
							|| fieldName.equals("altitude")
							|| fieldName.equals("cadence")
							|| fieldName.equals("distance")
							|| fieldName.equals("grade")
							|| fieldName.equals("heart_rate")
							|| fieldName.equals("position_lat")
							|| fieldName.equals("position_long")
							|| fieldName.equals("speed")
							|| fieldName.equals("temperature")
							//
							// lap data
							//
							|| fieldName.equals("avg_speed")
							|| fieldName.equals("avg_heart_rate")
							|| fieldName.equals("avg_cadence")
							|| fieldName.equals("data")
							|| fieldName.equals("device_index")
							|| fieldName.equals("device_type")
							|| fieldName.equals("event_group")
							|| fieldName.equals("end_position_lat")
							|| fieldName.equals("end_position_long")
							|| fieldName.equals("intensity")
							|| fieldName.equals("lap_trigger")
							|| fieldName.equals("max_cadence")
							|| fieldName.equals("max_heart_rate")
							|| fieldName.equals("max_speed")
							|| fieldName.equals("total_calories")
							|| fieldName.equals("total_fat_calories")
							|| fieldName.equals("sport")
							|| fieldName.equals("start_position_lat")
							|| fieldName.equals("start_position_long")
							|| fieldName.equals("start_time")
							|| fieldName.equals("total_ascent")
							|| fieldName.equals("total_descent")
							|| fieldName.equals("total_cycles")
							|| fieldName.equals("total_distance")
							|| fieldName.equals("total_elapsed_time")
							|| fieldName.equals("total_timer_time")
							//
							|| fieldName.equals("unknown")
					//
					) {
						continue;
					}
					System.out.println(String.format(
							"%-5d%-30s%20s %s",
							field.getNum(),
							fieldName,
							field.getValue(),
							field.getUnits()));
				}
			}
		});

//		broadcaster.addListener(new FileIdMesgListener() {
//			@Override
//			public void onMesg(final FileIdMesg mesg) {}
//		});
//		broadcaster.addListener(new FileCreatorMesgListener() {
//			@Override
//			public void onMesg(final FileCreatorMesg mesg) {}
//		});
//		broadcaster.addListener(new SoftwareMesgListener() {
//			@Override
//			public void onMesg(final SoftwareMesg mesg) {}
//		});
//		broadcaster.addListener(new SlaveDeviceMesgListener() {
//			@Override
//			public void onMesg(final SlaveDeviceMesg mesg) {}
//		});
//		broadcaster.addListener(new CapabilitiesMesgListener() {
//			@Override
//			public void onMesg(final CapabilitiesMesg mesg) {}
//		});
//		broadcaster.addListener(new FileCapabilitiesMesgListener() {
//			@Override
//			public void onMesg(final FileCapabilitiesMesg mesg) {}
//		});
//		broadcaster.addListener(new MesgCapabilitiesMesgListener() {
//			@Override
//			public void onMesg(final MesgCapabilitiesMesg mesg) {}
//		});
//		broadcaster.addListener(new FieldCapabilitiesMesgListener() {
//			@Override
//			public void onMesg(final FieldCapabilitiesMesg mesg) {}
//		});
//		broadcaster.addListener(new DeviceSettingsMesgListener() {
//			@Override
//			public void onMesg(final DeviceSettingsMesg mesg) {}
//		});
//		broadcaster.addListener(new UserProfileMesgListener() {
//			@Override
//			public void onMesg(final UserProfileMesg mesg) {}
//		});
//		broadcaster.addListener(new HrmProfileMesgListener() {
//			@Override
//			public void onMesg(final HrmProfileMesg mesg) {}
//		});
//		broadcaster.addListener(new SdmProfileMesgListener() {
//			@Override
//			public void onMesg(final SdmProfileMesg mesg) {}
//		});
//		broadcaster.addListener(new BikeProfileMesgListener() {
//			@Override
//			public void onMesg(final BikeProfileMesg mesg) {}
//		});
//		broadcaster.addListener(new ZonesTargetMesgListener() {
//			@Override
//			public void onMesg(final ZonesTargetMesg mesg) {}
//		});
//		broadcaster.addListener(new SportMesgListener() {
//			@Override
//			public void onMesg(final SportMesg mesg) {}
//		});
//		broadcaster.addListener(new HrZoneMesgListener() {
//			@Override
//			public void onMesg(final HrZoneMesg mesg) {}
//		});
//		broadcaster.addListener(new SpeedZoneMesgListener() {
//			@Override
//			public void onMesg(final SpeedZoneMesg mesg) {}
//		});
//		broadcaster.addListener(new CadenceZoneMesgListener() {
//			@Override
//			public void onMesg(final CadenceZoneMesg mesg) {}
//		});
//		broadcaster.addListener(new PowerZoneMesgListener() {
//			@Override
//			public void onMesg(final PowerZoneMesg mesg) {}
//		});
//		broadcaster.addListener(new MetZoneMesgListener() {
//			@Override
//			public void onMesg(final MetZoneMesg mesg) {}
//		});
//		broadcaster.addListener(new GoalMesgListener() {
//			@Override
//			public void onMesg(final GoalMesg mesg) {}
//		});
//		broadcaster.addListener(new ActivityMesgListener() {
//			@Override
//			public void onMesg(final ActivityMesg mesg) {}
//		});
//		broadcaster.addListener(new SessionMesgListener() {
//			@Override
//			public void onMesg(final SessionMesg mesg) {}
//		});
//		broadcaster.addListener(new LapMesgListener() {
//			@Override
//			public void onMesg(final LapMesg mesg) {}
//		});
//		broadcaster.addListener(new LengthMesgListener() {
//			@Override
//			public void onMesg(final LengthMesg mesg) {}
//		});
//		broadcaster.addListener(new RecordMesgListener() {
//			@Override
//			public void onMesg(final RecordMesg mesg) {}
//		});
//		broadcaster.addListener(new EventMesgListener() {
//			@Override
//			public void onMesg(final EventMesg mesg) {}
//		});
//		broadcaster.addListener(new DeviceInfoMesgListener() {
//			@Override
//			public void onMesg(final DeviceInfoMesg mesg) {}
//		});
//		broadcaster.addListener(new HrvMesgListener() {
//			@Override
//			public void onMesg(final HrvMesg mesg) {}
//		});
//		broadcaster.addListener(new CourseMesgListener() {
//			@Override
//			public void onMesg(final CourseMesg mesg) {}
//		});
//		broadcaster.addListener(new CoursePointMesgListener() {
//			@Override
//			public void onMesg(final CoursePointMesg mesg) {}
//		});
//		broadcaster.addListener(new WorkoutMesgListener() {
//			@Override
//			public void onMesg(final WorkoutMesg mesg) {}
//		});
//		broadcaster.addListener(new WorkoutStepMesgListener() {
//			@Override
//			public void onMesg(final WorkoutStepMesg mesg) {}
//		});
//		broadcaster.addListener(new ScheduleMesgListener() {
//			@Override
//			public void onMesg(final ScheduleMesg mesg) {}
//		});
//		broadcaster.addListener(new TotalsMesgListener() {
//			@Override
//			public void onMesg(final TotalsMesg mesg) {}
//		});
//		broadcaster.addListener(new WeightScaleMesgListener() {
//			@Override
//			public void onMesg(final WeightScaleMesg mesg) {}
//		});
//		broadcaster.addListener(new BloodPressureMesgListener() {
//			@Override
//			public void onMesg(final BloodPressureMesg mesg) {}
//		});
//		broadcaster.addListener(new MonitoringInfoMesgListener() {
//			@Override
//			public void onMesg(final MonitoringInfoMesg mesg) {}
//		});
//		broadcaster.addListener(new MonitoringMesgListener() {
//			@Override
//			public void onMesg(final MonitoringMesg mesg) {}
//		});
//		broadcaster.addListener(new MemoGlobMesgListener() {
//			@Override
//			public void onMesg(final MemoGlobMesg mesg) {}
//		});
//		broadcaster.addListener(new PadMesgListener() {
//			@Override
//			public void onMesg(final PadMesg mesg) {}
//		});
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

			// original listener
			broadcaster.addListener(new FileIdMesgListenerImpl(context));
			broadcaster.addListener(new FileCreatorMesgListenerImpl(context));
			broadcaster.addListener(new DeviceInfoMesgListenerImpl(context));
			broadcaster.addListener(new ActivityMesgListenerImpl(context));
			broadcaster.addListener(new SessionMesgListenerImpl(context));
			broadcaster.addListener(new LapMesgListenerImpl(context));
			broadcaster.addListener(new RecordMesgListenerImpl(context));

//			// debug info
//			System.out.println();
//			System.out.println();
//			System.out.println((System.currentTimeMillis() + " [" + getClass().getSimpleName() + "]")
//					+ (" \t" + importFilePath));
//			System.out.println();
//
//			addDebugListener(broadcaster);

			broadcaster.run(fis);

			context.processData();

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
