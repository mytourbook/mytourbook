/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

public class Util {

	public static final String			EMPTY_STRING	= "";								//$NON-NLS-1$
	private static final String			SYMBOL_DASH		= "-";								//$NON-NLS-1$
	public static final String			DASH_WITH_SPACE	= " - ";							//$NON-NLS-1$

	private static final String			FORMAT_0F		= "0f";							//$NON-NLS-1$
	private static final String			FORMAT_MM_SS	= "%d:%02d";						//$NON-NLS-1$
	private static final String			FORMAT_HH_MM	= "%d:%02d";						//$NON-NLS-1$
	private static final String			FORMAT_HH_MM_SS	= "%d:%02d:%02d";					//$NON-NLS-1$

	private final static NumberFormat	_nf0			= NumberFormat.getNumberInstance();
	private final static NumberFormat	_nf1			= NumberFormat.getNumberInstance();
	{
		_nf0.setMinimumFractionDigits(0);
		_nf1.setMinimumFractionDigits(1);
	}

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

	public static String format_hh_mm(final long time) {

		_sbFormatter.setLength(0);

		if (time < 0) {
			_sbFormatter.append(SYMBOL_DASH);
		}

		final long timeAbsolute = time < 0 ? 0 - time : time;

		return _formatter.format(FORMAT_HH_MM, //
				(timeAbsolute / 3600),
				(timeAbsolute % 3600) / 60).toString();
	}

	public static String format_hh_mm_ss(final long time) {

		_sbFormatter.setLength(0);

		if (time < 0) {
			_sbFormatter.append(SYMBOL_DASH);
		}

		final long timeAbsolute = time < 0 ? 0 - time : time;

		return _formatter.format(FORMAT_HH_MM_SS, //
				(timeAbsolute / 3600),
				(timeAbsolute % 3600) / 60,
				(timeAbsolute % 3600) % 60).toString();
	}

	public static String format_hh_mm_ss_Optional(final long value) {

		boolean isShowSeconds = true;
		final int seconds = (int) ((value % 3600) % 60);

		if (isShowSeconds && seconds == 0) {
			isShowSeconds = false;
		}

		String valueText;
		if (isShowSeconds) {

			// show seconds only when they are available

			valueText = format_hh_mm_ss(value);
		} else {
			valueText = format_hh_mm(value);
		}

		return valueText;
	}

	public static String format_mm_ss(final long time) {

		_sbFormatter.setLength(0);

		if (time < 0) {
			_sbFormatter.append(SYMBOL_DASH);
		}

		final long timeAbsolute = time < 0 ? 0 - time : time;

		return _formatter.format(FORMAT_MM_SS, //
				(timeAbsolute / 60),
				(timeAbsolute % 60)).toString();
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

		format += (removeDecimalZero && (divValue % 1 == 0)) ? //
				FORMAT_0F
				: Integer.toString(precision) + 'f';

		return String.format(format, divValue).toString();
	}

	public static String formatValue(final double value, final int unitType, final float divisor, boolean isShowSeconds) {

		String valueText = EMPTY_STRING;

		// format the unit label
		switch (unitType) {
		case ChartDataSerie.AXIS_UNIT_NUMBER:
		case ChartDataSerie.X_AXIS_UNIT_NUMBER_CENTER:
			final double divValue = value / divisor;
			if (divValue % 1 == 0) {
				valueText = _nf0.format(divValue);
			} else {
				valueText = _nf1.format(divValue);
			}
			break;

		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H:

			valueText = format_hh_mm((long) value);

			break;

		case ChartDataSerie.AXIS_UNIT_MINUTE_SECOND:
			valueText = format_mm_ss((long) value);
			break;

		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND:

			// seconds are displayed when they are not 0

			final int seconds = (int) ((value % 3600) % 60);
			if (isShowSeconds && seconds == 0) {
				isShowSeconds = false;
			}

			// !!! the missing break; is intentional !!!

		case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:

			if (isShowSeconds) {

				// show seconds only when they are available

				valueText = format_hh_mm_ss((long) value);
			} else {
				valueText = format_hh_mm((long) value);
			}
			break;

		case ChartDataSerie.X_AXIS_UNIT_DAY:
			valueText = _nf1.format(value);
			break;

		default:
			break;
		}

		return valueText;
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



	/**
	 * Round floating value by removing the trailing part, which causes problem when creating units.
	 * For the value 200.00004 the .00004 part will be removed
	 * 
	 * @param graphValue
	 * @param graphUnit
	 * @return
	 */
	public static float truncateFloatToUnit(final float graphValue, final float graphUnit) {

		if (graphUnit < 1) {

			final float gvDiv1 = graphValue / graphUnit;
			final long gvDiv2 = (long) (gvDiv1);
			final float gvDiv3 = gvDiv2 * graphUnit;

			return gvDiv3;

		} else {

			// graphUnit >= 1

			final float gvDiv1 = graphValue * graphUnit;
			final long gvDiv2 = (long) (gvDiv1);
			final float gvDiv3 = gvDiv2 / graphUnit;

			return gvDiv3;
		}
	}

}
