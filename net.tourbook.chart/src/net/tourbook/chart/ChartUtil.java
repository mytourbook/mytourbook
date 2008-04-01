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
package net.tourbook.chart;

import java.text.NumberFormat;
import java.util.Formatter;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartUtil {

	private final static NumberFormat	fNf	= NumberFormat.getNumberInstance();

	/**
	 * Formats a value according to the defined unit
	 * 
	 * @param value
	 * @param data
	 * @return
	 */
	public static String formatValue(final int value, final int unitType) {
		return formatValue(value, unitType, 1, false);
	}

	public static String formatValue(final int value, final int unitType, final float divisor, boolean showSeconds) {

		String valueText = ""; //$NON-NLS-1$

		// format the unit label
		switch (unitType) {
		case ChartDataSerie.AXIS_UNIT_NUMBER:
			float divValue = (float) value / divisor;
			if (divValue % 1 == 0) {
				fNf.setMinimumFractionDigits(0);
			} else {
				fNf.setMinimumFractionDigits(1);
			}
			valueText = fNf.format(divValue);
			break;

		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H:
			valueText = new Formatter().format(Messages.Format_time_hhmm,
					(long) (value / 3600),
					(long) ((value % 3600) / 60)).toString();
			break;

		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:

			if (showSeconds) {
				valueText = new Formatter().format(Messages.Format_time_hhmmss,
						(long) (value / 3600),
						(long) ((value % 3600) / 60),
						(long) ((value % 3600) % 60)).toString();
			} else {
				valueText = new Formatter().format(Messages.Format_time_hhmm,
						(long) (value / 3600),
						(long) ((value % 3600) / 60)).toString();
			}
			break;

		case ChartDataSerie.AXIS_UNIT_YEAR:
			fNf.setMinimumFractionDigits(1);
			valueText = fNf.format(value);
			break;

		default:
			break;
		}

		return valueText;
	}

	/**
	 * @param value
	 *        The value which is formatted
	 * @param divisor
	 *        Divisor by which the value is divided
	 * @param precision
	 *        Decimal numbers after the decimal point
	 * @param removeDecimalZero
	 *        True removes trailing zeros after a decimal point
	 * @return
	 */
	public static String formatInteger(final int value, int divisor, int precision, boolean removeDecimalZero) {

		float divValue = (float) value / divisor;

		String format = Messages.Format_number_float;

		format += (removeDecimalZero && (divValue % 1 == 0)) ? "0f" : Integer //$NON-NLS-1$
		.toString(precision) + 'f';

		return new Formatter().format(format, divValue).toString();
	}

	/**
	 * @param unitValue
	 * @return Returns minUnitValue rounded to the number 60/30/20/10/5/2/1
	 */
	public static float roundTimeValue(int unitValue) {

		float unit = unitValue;
		int multiplier = 1;

		while (unit > 120) {
			multiplier *= 60;
			unit /= 60;
		}

		unit = unit > 120 ? 120 : //
				unit > 60 ? 60 : //
						unit > 30 ? 30 : //
								unit > 20 ? 20 : //
										unit > 10 ? 10 : //
												unit > 5 ? 5 : //
														unit > 2 ? 2 : 1;
		unit *= multiplier;

		return unit;
	}

	/**
	 * @param unitValue
	 * @return Returns minUnitValue rounded to the number of 50/20/10/5/2/1
	 */
	public static float roundDecimalValue(int unitValue) {

		float unit = unitValue;
		int multiplier = 1;

		while (unit > 100) {
			multiplier *= 10;
			unit /= 10;
		}

		unit = (float) unitValue / multiplier;
		unit = unit > 50 ? 50 : unit > 20 ? 20 : unit > 10 ? 10 : unit > 5 ? 5 : unit > 2 ? 2 : 1;
		unit *= multiplier;

		return unit;
	}

	/**
	 * creates a new image
	 * 
	 * @param display
	 * @param image
	 *        image which will be disposed if the image is not null
	 * @param rect
	 * @return returns a new created image
	 */
	public static Image createImage(Display display, Image image, Rectangle rect) {

		if (image != null && !image.isDisposed()) {
			image.dispose();
		}

		return new Image(display, rect.width, rect.height);
	}

	/**
	 * disposes a resource
	 * 
	 * @param image
	 * @return
	 */
	public static Image disposeResource(Image resource) {
		if (resource != null && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	public static Cursor disposeResource(Cursor resource) {
		if (resource != null && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	public static Color disposeResource(Color resource) {
		if (resource != null && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	/**
	 * Checks if an image can be reused, this is true if the image exists and has the same size
	 * 
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static boolean canReuseImage(final Image image, final Rectangle rect) {

		// check if we could reuse the existing image

		if (image == null || image.isDisposed()) {
			return false;
		} else {
			// image exist, check for the bounds
			Rectangle oldBounds = image.getBounds();

			if (!(oldBounds.width == rect.width && oldBounds.height == rect.height)) {
				return false;
			}
		}
		return true;
	}

}
