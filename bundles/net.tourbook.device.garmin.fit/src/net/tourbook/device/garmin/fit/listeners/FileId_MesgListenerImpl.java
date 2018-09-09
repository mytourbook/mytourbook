/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import net.tourbook.tour.TourLogManager;

import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.FileIdMesgListener;
import com.garmin.fit.GarminProduct;
import com.garmin.fit.Manufacturer;

public class FileId_MesgListenerImpl extends AbstractMesgListener implements FileIdMesgListener {

	public FileId_MesgListenerImpl(final FitContext context) {
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
		final Integer manufacturerId = mesg.getManufacturer();
		if (manufacturerId != null) {

			final String manufacturerText = Manufacturer.getStringFromValue(manufacturerId);

			if (manufacturerText.length() > 0) {
				context.setManufacturer(manufacturerText);
			} else {
				context.setManufacturer(manufacturerId.toString());
			}
		}

		/*
		 * Product
		 */
		final Integer garminProductId = mesg.getGarminProduct();
		if (garminProductId != null) {

			final String garminProductName = GarminProduct.getStringFromValue(garminProductId);

			if (garminProductName.length() > 0) {
				context.setGarminProduct(garminProductName);
			} else {
				context.setGarminProduct(garminProductId.toString());
			}
		}
	}

}
