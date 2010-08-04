package net.tourbook.device.garmin.fit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.data.TimeData;
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
import net.tourbook.util.StatusUtil;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.garmin.fit.Decode;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;

/**
 * Garmin FIT data reader based on official Garmin SDK.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 * 
 */
public class NewFitDataReader extends TourbookDevice {

    @Override
    public String getDeviceModeName(int modeId) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public int getTransferDataSize() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public boolean processDeviceData(String fileName, DeviceData deviceData, HashMap<Long, TourData> tourDataMap) {
	boolean returnValue = false;

	FileInputStream fis = null;

	try {
	    fis = new FileInputStream(fileName);

	    Decode decode = new Decode();
	    MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

	    TourData tourData = createTourData(fileName);

	    // it should get List instead of ArrayList
	    ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

	    FileIdMesgListenerImpl fileIdMesgListener = new FileIdMesgListenerImpl(tourData);
	    broadcaster.addListener(fileIdMesgListener);

	    FileCreatorMesgListenerImpl fileCreatorMesgListener = new FileCreatorMesgListenerImpl(tourData);
	    broadcaster.addListener(fileCreatorMesgListener);

	    ActivityMesgListenerImpl activityMesgListener = new ActivityMesgListenerImpl(tourData);
	    broadcaster.addListener(activityMesgListener);

	    SessionMesgListenerImpl sessionMesgListener = new SessionMesgListenerImpl(tourData);
	    broadcaster.addListener(sessionMesgListener);

	    DeviceInfoMesgListenerImpl deviceInfoMesgListener = new DeviceInfoMesgListenerImpl(tourData);
	    broadcaster.addListener(deviceInfoMesgListener);

	    LapMesgListenerImpl lapMesgListener = new LapMesgListenerImpl(tourData);
	    broadcaster.addListener(lapMesgListener);

	    RecordMesgListenerImpl recordMesgListener = new RecordMesgListenerImpl(timeDataList);
	    broadcaster.addListener(recordMesgListener);

	    broadcaster.run(fis);

	    Long tourId = tourData.createTourId(fileIdMesgListener.getSerialNumberAsString());
	    if (!tourDataMap.containsKey(tourId)) {
		tourData.createTimeSeries(timeDataList, true);

		//tourData.computeTourDrivingTime();

		tourData.computeComputedValues();
		tourData.computeAltimeterGradientSerie();

		tourDataMap.put(tourId, tourData);
	    }

	    returnValue = true;
	} catch (FileNotFoundException e) {
	    StatusUtil.log("Could not read data file '" + fileName + "'", e);
	} finally {
	    IOUtils.closeQuietly(fis);
	}

	return returnValue;
    }

    @Override
    public boolean validateRawData(String fileName) {
	boolean returnValue = false;
	FileInputStream fis = null;

	try {
	    fis = new FileInputStream(fileName);
	    returnValue = Decode.checkIntegrity(fis);
	} catch (FileNotFoundException e) {
	    StatusUtil.log("Could not read data file '" + fileName + "'", e);
	} catch (FitRuntimeException e) {
	    StatusUtil.log("Invalid data file '" + fileName + "'", e);
	} finally {
	    IOUtils.closeQuietly(fis);
	}

	return returnValue;
    }

    @Override
    public String buildFileNameFromRawData(String rawDataFileName) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public boolean checkStartSequence(int byteIndex, int newByte) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public SerialParameters getPortParameters(String portName) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public int getStartSequenceSize() {
	// TODO Auto-generated method stub
	return 0;
    }

    private TourData createTourData(String fileName) {
	TourData tourData = new TourData();
	String tourTitle = FilenameUtils.getBaseName(fileName);

	tourData.setTourTitle(tourTitle);
	tourData.setTourDescription(tourTitle); 

	tourData.setDeviceTimeInterval((short) -1);

	tourData.importRawDataFile = fileName;
	tourData.setTourImportFilePath(fileName);

	return tourData;
    }

}
