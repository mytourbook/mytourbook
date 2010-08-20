package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitActivityContext;
import net.tourbook.device.garmin.fit.FitActivityReaderException;
import net.tourbook.device.garmin.fit.types.GarminProduct;
import net.tourbook.device.garmin.fit.types.Manufacturer;

import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;

public class FileIdMesgListenerImpl extends AbstractMesgListener implements FileIdMesgListener {

	public FileIdMesgListenerImpl(FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(FileIdMesg mesg) {
		File type = mesg.getType();
		if (type != File.ACTIVITY) {
			throw new FitActivityReaderException("Invalid file type: "
					+ type.name()
					+ ", expected: "
					+ File.ACTIVITY.name());
		}

		Long serialNumber = mesg.getSerialNumber();
		if (serialNumber == null) {
			throw new FitActivityReaderException("File serial number is missing");
		}

		context.setDeviceId(serialNumber.toString());

		Integer manufacturer = mesg.getManufacturer();
		if (manufacturer != null) {
			Manufacturer manufacturerEnum = Manufacturer.valueOf(manufacturer);
			if (manufacturerEnum != null) {
				context.setManufacturer(manufacturerEnum.name());
			} else {
				context.setManufacturer(manufacturer.toString());
			}
		}

		Integer garminProduct = mesg.getGarminProduct();
		if (garminProduct != null) {
			GarminProduct garminProductEnum = GarminProduct.valueOf(garminProduct);
			if (garminProductEnum != null) {
				context.setGarminProduct(garminProductEnum.name());
			} else {
				context.setGarminProduct(garminProduct.toString());
			}
		}
	}

}
