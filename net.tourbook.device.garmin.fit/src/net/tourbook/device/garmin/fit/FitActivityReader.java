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
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;

/**
 * Garmin FIT activity reader based on official Garmin SDK.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitActivityReader extends TourbookDevice {

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDeviceModeName(final int modeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTransferDataSize() {
		// TODO Auto-generated method stub
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

			final Decode decode = new Decode();
			final MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

			final FitActivityContext context = new FitActivityContext(
					this,
					importFilePath,
					alreadyImportedTours,
					newlyImportedTours);

			final FileIdMesgListenerImpl fileIdMesgListener = new FileIdMesgListenerImpl(context);
			broadcaster.addListener(fileIdMesgListener);

			final FileCreatorMesgListenerImpl fileCreatorMesgListener = new FileCreatorMesgListenerImpl(context);
			broadcaster.addListener(fileCreatorMesgListener);

			final DeviceInfoMesgListenerImpl deviceInfoMesgListener = new DeviceInfoMesgListenerImpl(context);
			broadcaster.addListener(deviceInfoMesgListener);

			final ActivityMesgListenerImpl activityMesgListener = new ActivityMesgListenerImpl(context);
			broadcaster.addListener(activityMesgListener);

			final SessionMesgListenerImpl sessionMesgListener = new SessionMesgListenerImpl(context);
			broadcaster.addListener(sessionMesgListener);

			final LapMesgListenerImpl lapMesgListener = new LapMesgListenerImpl(context);
			broadcaster.addListener(lapMesgListener);

			final RecordMesgListenerImpl recordMesgListener = new RecordMesgListenerImpl(context);
			broadcaster.addListener(recordMesgListener);

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
