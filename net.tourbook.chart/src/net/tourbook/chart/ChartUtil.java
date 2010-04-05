/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

	public static final String			EMPTY_STRING	= "";								//$NON-NLS-1$
	private static final String			SYMBOL_DASH		= "-";								//$NON-NLS-1$
	public static final String			DASH_WITH_SPACE	= " - ";							//$NON-NLS-1$

	private static final String			FORMAT_0F		= "0f";
	private static final String			FORMAT_MM_SS	= "%d:%02d";						//$NON-NLS-1$

	private final static NumberFormat	_nf				= NumberFormat.getNumberInstance();

	private static StringBuilder		_sbFormatter	= new StringBuilder();
	private static Formatter			_formatter		= new Formatter(_sbFormatter);

	/**
	 * Checks if an image can be reused, this is true if the image exists and has the same size
	 * 
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static boolean canReuseImage(final Image image, final Rectangle rect) {

		// check if we could reuse the existing image

		if ((image == null) || image.isDisposed()) {
			return false;
		} else {
			// image exist, check for the bounds
			final Rectangle oldBounds = image.getBounds();

			if (!((oldBounds.width == rect.width) && (oldBounds.height == rect.height))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * creates a new image
	 * 
	 * @param display
	 * @param image
	 *            image which will be disposed if the image is not null
	 * @param rect
	 * @return returns a new created image
	 */
	public static Image createImage(final Display display, final Image image, final Rectangle rect) {

		if ((image != null) && !image.isDisposed()) {
			image.dispose();
		}

		return new Image(display, rect.width, rect.height);
	}

	public static Color disposeResource(final Color resource) {
		if ((resource != null) && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	public static Cursor disposeResource(final Cursor resource) {
		if ((resource != null) && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	/**
	 * disposes a resource
	 * 
	 * @param image
	 * @return
	 */
	public static Image disposeResource(final Image resource) {
		if ((resource != null) && !resource.isDisposed()) {
			resource.dispose();
		}
		return null;
	}

	public static String format_mm_ss(final long time) {

		_sbFormatter.setLength(0);

		if (time < 0) {
			_sbFormatter.append(SYMBOL_DASH);
		}

		final long timeAbs = time < 0 ? 0 - time : time;

		return _formatter.format(FORMAT_MM_SS, (timeAbs / 60), (timeAbs % 60)).toString();
	}

	/**
	 * @param value
	 *            The value which is formatted
	 * @param divisor
	 *            Divisor by which the value is divided
	 * @param precision
	 *            Decimal numbers after the decimal point
	 * @param removeDecimalZero
	 *            True removes trailing zeros after a decimal point
	 * @return
	 */
	public static String formatInteger(	final int value,
										final int divisor,
										final int precision,
										final boolean removeDecimalZero) {

		final float divValue = (float) value / divisor;

		String format = Messages.Format_number_float;

		format += (removeDecimalZero && (divValue % 1 == 0)) ? FORMAT_0F : Integer.toString(precision) + 'f';

		return new Formatter().format(format, divValue).toString();
	}

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

	public static String formatValue(final int value, final int unitType, final float divisor, final boolean showSeconds) {

		String valueText = EMPTY_STRING;

		// format the unit label
		switch (unitType) {
		case ChartDataSerie.AXIS_UNIT_NUMBER:
			final float divValue = value / divisor;
			if (divValue % 1 == 0) {
				_nf.setMinimumFractionDigits(0);
			} else {
				_nf.setMinimumFractionDigits(1);
			}
			valueText = _nf.format(divValue);
			break;

		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H:
			valueText = new Formatter().format(
					Messages.Format_time_hhmm,
					(long) (value / 3600),
					(long) ((value % 3600) / 60)).toString();
			break;

		case ChartDataSerie.AXIS_UNIT_MINUTE_SECOND:
			valueText = format_mm_ss(value);
			break;

		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:

			if (showSeconds) {
				valueText = new Formatter().format(
						Messages.Format_time_hhmmss,
						(long) (value / 3600),
						(long) ((value % 3600) / 60),
						(long) ((value % 3600) % 60)).toString();
			} else {
				valueText = new Formatter().format(
						Messages.Format_time_hhmm,
						(long) (value / 3600),
						(long) ((value % 3600) / 60)).toString();
			}
			break;

		case ChartDataSerie.AXIS_UNIT_DAY:
			_nf.setMinimumFractionDigits(1);
			valueText = _nf.format(value);
			break;

		default:
			break;
		}

		return valueText;
	}

	/**
	 * @param unitValue
	 * @return Returns minUnitValue rounded to the number of 50/20/10/5/2/1
	 */
	public static float roundDecimalValue(final int unitValue) {

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
	 * @param unitValue
	 * @return Returns minUnitValue rounded to the number 60/30/20/10/5/2/1
	 */
	public static float roundTimeValue(final int unitValue) {

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

}
