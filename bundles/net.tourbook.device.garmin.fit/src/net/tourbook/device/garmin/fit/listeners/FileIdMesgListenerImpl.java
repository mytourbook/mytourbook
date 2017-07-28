/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitContext;
import net.tourbook.device.garmin.fit.FitDataReaderException;
import net.tourbook.device.garmin.fit.types.GarminProduct;
import net.tourbook.device.garmin.fit.types.Manufacturer;
import net.tourbook.tour.TourLogManager;

import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;

public class FileIdMesgListenerImpl extends AbstractMesgListener implements FileIdMesgListener {

	public FileIdMesgListenerImpl(final FitContext context) {
		super(context);
	}

	@Override
	public void onMesg(final FileIdMesg mesg) {

		/*
		 * File Type
		 */
		final File type = mesg.getType();
		if (type != File.ACTIVITY) {
			throw new FitDataReaderException(
					String.format(//
							"Invalid file type: %s, expected: %s", //$NON-NLS-1$
							type.name(),
							File.ACTIVITY.name()));
		}

		/*
		 * Serial Number
		 */
		final Long serialNumber = mesg.getSerialNumber();
		if (serialNumber == null) {
			TourLogManager.logError("File serial number is missing, device id cannot not be set");//$NON-NLS-1$
		} else {
			context.setDeviceId(serialNumber.toString());
		}

		/*
		 * Manufacturer
		 */
		final Integer manufacturer = mesg.getManufacturer();
		if (manufacturer != null) {
			final Manufacturer manufacturerEnum = Manufacturer.valueOf(manufacturer);
			if (manufacturerEnum != null) {
				context.setManufacturer(manufacturerEnum.name());
			} else {
				context.setManufacturer(manufacturer.toString());
			}
		}

		/*
		 * Product
		 */
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
