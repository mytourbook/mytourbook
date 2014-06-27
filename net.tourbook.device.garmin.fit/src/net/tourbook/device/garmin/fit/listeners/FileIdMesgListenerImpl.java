package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitContext;
import net.tourbook.device.garmin.fit.FitDataReaderException;
import net.tourbook.device.garmin.fit.types.GarminProduct;
import net.tourbook.device.garmin.fit.types.Manufacturer;

import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;

public class FileIdMesgListenerImpl extends AbstractMesgListener implements FileIdMesgListener {

	public FileIdMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final FileIdMesg mesg) {

		final File type = mesg.getType();
		if (type != File.ACTIVITY) {
			throw new FitDataReaderException("Invalid file type: " //$NON-NLS-1$
					+ type.name()
					+ ", expected: " //$NON-NLS-1$
					+ File.ACTIVITY.name());
		}

		final Long serialNumber = mesg.getSerialNumber();
		if (serialNumber == null) {
			throw new FitDataReaderException("File serial number is missing"); //$NON-NLS-1$
		}

		context.setDeviceId(serialNumber.toString());

		final Integer manufacturer = mesg.getManufacturer();
		if (manufacturer != null) {
			final Manufacturer manufacturerEnum = Manufacturer.valueOf(manufacturer);
			if (manufacturerEnum != null) {
				context.setManufacturer(manufacturerEnum.name());
			} else {
				context.setManufacturer(manufacturer.toString());
			}
		}

		final Integer garminProduct = mesg.getGarminProduct();
		if (garminProduct != null) {
			final GarminProduct garminProductEnum = GarminProduct.valueOf(garminProduct);
			if (garminProductEnum != null) {
				context.setGarminProduct(garminProductEnum.name());
			} else {
				context.setGarminProduct(garminProduct.toString());
			}
		}
	}

}
