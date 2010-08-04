package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.FitDataReaderException;
import net.tourbook.device.garmin.fit.types.GarminProduct;
import net.tourbook.device.garmin.fit.types.Manufacturer;

import com.garmin.fit.DateTime;
import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;

public class FileIdMesgListenerImpl extends AbstractTourDataMesgListener implements FileIdMesgListener {

    private Long serialNumber;

    public FileIdMesgListenerImpl(TourData tourData) {
	super(tourData);
    }

    @Override
    public void onMesg(FileIdMesg mesg) {
	File type = mesg.getType();
	if (type != File.ACTIVITY) {
	    throw new FitDataReaderException("Invalid file type: " + type.name() + ", expected: "
		    + File.ACTIVITY.name());
	}

	serialNumber = mesg.getSerialNumber();
	if (serialNumber == null) {
	    throw new FitDataReaderException("File serial number is missing");
	}

	tourData.setDeviceId(serialNumber.toString());

	Integer manufacturer = mesg.getManufacturer();
	if (manufacturer != null) {
	    Manufacturer manufacturerEnum = Manufacturer.valueOf(manufacturer);
	    // TODO
	}

	Integer garminProduct = mesg.getGarminProduct();
	if (garminProduct != null) {
	    GarminProduct garminProductEnum = GarminProduct.valueOf(garminProduct);
	    if (garminProductEnum != null) {
		tourData.setDeviceName(garminProductEnum.name());
	    } else {
		tourData.setDeviceName(garminProduct.toString());
	    }
	}

	DateTime timeCreated = mesg.getTimeCreated();
	if (timeCreated != null) {
	    // TODO
	}
    }

    public Long getSerialNumber() {
	if (serialNumber == null) {
	    throw new IllegalStateException("Message(s) has not been processed");
	}
	return serialNumber;
    }

    public String getSerialNumberAsString() {
	return getSerialNumber().toString();
    }

}