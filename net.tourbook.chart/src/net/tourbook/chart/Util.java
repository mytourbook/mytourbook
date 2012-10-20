/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

	public static String formatValue(final float value, final int unitType, final float divisor, boolean isShowSeconds) {

		String valueText = EMPTY_STRING;

		// format the unit label
		switch (unitType) {
		case ChartDataSerie.AXIS_UNIT_NUMBER:
		case ChartDataSerie.X_AXIS_UNIT_NUMBER_CENTER:
			final float divValue = value / divisor;
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

	public static double getMajorDecimalValue(final double graphUnit) {

		double unit = graphUnit;
		int multiplier = 1;

		while (unit > 1000) {
			multiplier *= 10;
			unit /= 10;
		}

		unit = graphUnit / multiplier;

		unit = //
		unit == 1000 ? 5000 : unit == 500 ? 1000 : unit == 200 ? 1000 : //
				unit == 100 ? 500 : unit == 50 ? 200 : unit == 20 ? 100 : //
						unit == 10 ? 50 : unit == 5 ? 20 : unit == 2 ? 10 : //
								unit == 1 ? 5 : unit == 0.5 ? 2.0 : unit == 0.2 ? 1 : //
										unit == 0.1 ? 0.5 : unit == 0.05 ? 0.2 : unit == 0.02 ? 0.1 : //
												unit == 0.01 ? 0.05 : unit == 0.005 ? 0.02 : unit == 0.002
														? 0.01
														: 0.005;

		unit *= multiplier;

		return unit;
	}

	/**
	 * @param unitValue
	 * @param is24HourFormatting
	 * @return Returns minUnitValue rounded to the number 60/30/20/10/5/2/1
	 */
	public static long getMajorTimeValue(final long unitValue, final boolean is24HourFormatting) {

		long unit = unitValue;
		int multiplier = 1;

//		System.out.println();
//		System.out.println("getMajorTimeValue");
//		System.out.println(unit + "\ttime: " + System.currentTimeMillis());
//		// TODO remove SYSTEM.OUT.PRINTLN

		while (unit > 240) {
			multiplier *= 60;
			unit /= 60;
		}

//		System.out.println(unit + "\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (is24HourFormatting) {

			if (multiplier >= 3600) {

				unit = //
						//
//				unit >= 120 ? 360 : //
				unit >= 360 ? 3600 : //
						unit >= 60 ? 360 : //
//								unit >= 30 ? 60 : //
								unit >= 15 ? 60 : //
//												unit >= 12 ? 12 : //
										12;

			} else {

				unit = //
						//
				unit >= 120 ? 720 : //
//						unit >= 60 ? 360 : //
						unit >= 30 ? 360 : //
								unit >= 15 ? 120 : //
										unit >= 10 ? 60 : //
												unit >= 5 ? 30 : //
														unit >= 2 ? 10 : //
																5;
			}

		} else {

			if (multiplier >= 3600) {

				unit = //
						//
				unit >= 720 ? 3600 : //
						unit >= 240 ? 720 : //
								unit >= 120 ? 240 : //
										unit >= 24 ? 120 : //
												10;
//				unit = //
//						//
//				unit >= 120 ? 600 : //
//						unit >= 60 ? 300 : //
////							unit >= 30 ? 60 : //
////									unit >= 20 ? 60 : //
//								unit >= 10 ? 60 : //
//										unit >= 5 ? 20 : //
//												unit >= 2 ? 10 : //
//														10;
			} else {

				// multiplier < 1 hour (3600 sec)

				unit = //
						//
				unit >= 120 ? 600 : //
						unit >= 60 ? 300 : //
								unit >= 30 ? 120 : //
										unit >= 15 ? 60 : //
												unit >= 10 ? 60 : //
														unit >= 5 ? 20 : //
																10;
			}
		}

//		System.out.println(unit + "\tmultiplier: " + multiplier);
//		// TODO remove SYSTEM.OUT.PRINTLN

		unit *= multiplier;

//		System.out.println(unit + "\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		return unit;
	}

	public static long getValueScaling(final double graphUnit) {

		if (graphUnit > 1 || graphUnit < 1) {

			double scaledValue = 1;

			if (graphUnit < 1) {
				scaledValue = 1 / graphUnit;
			} else {
				scaledValue = graphUnit;
			}

			long valueScale = 1;

			while (scaledValue > 1) {
				valueScale *= 10;
				scaledValue /= 10;
			}

			// add an additional factor to prevent rounding problems
			valueScale *= 1000;

			return valueScale;

		} else {

			// add an additional factor to prevent rounding problems
			return 1000;
		}
	}

	/**
	 * @param unitValue
	 * @return Returns unit value rounded to the number of 50/20/10/5/2/1
	 */
	public static double roundDecimalValue(final float unitValue) {

		double unit = unitValue;
		int multiplier = 1;

		while (unit > 100) {
			multiplier *= 10;
			unit /= 10;
		}

//		unit = unitValue / multiplier;

		unit = unit > 50 ? 50 : //
				unit > 20 ? 20 : //
						unit > 10 ? 10 : //
								unit > 5 ? 5 : //
										unit > 2 ? 2 : //
												unit > 1 ? 1 : //
														unit > 0.5 ? 0.5 : //
																unit > 0.2 ? 0.2 : //
																		unit > 0.1 ? 0.1 : //
																				unit > 0.05 ? 0.05 : //
																						unit > 0.02 ? 0.02 : //
																								0.01;

		unit *= multiplier;

		return unit;
	}

	/**
	 * Round floating value to an inverse long value.
	 * 
	 * @param graphValue
	 * @param graphUnit
	 * @return
	 */
	public static long roundFloatToUnitInverse(final float graphValue, final float graphUnit) {

		if (graphUnit < 1) {

			if (graphValue < 0) {

				final float value1 = graphValue / graphUnit;
				final float value2 = value1 - 0.5f;
				final long value3 = (long) (value2);

				return value3;

			} else {

				final float value1 = graphValue / graphUnit;
				final float value2 = value1 + 0.5f;
				final long value3 = (long) (value2);

				return value3;
			}

		} else {

			// graphUnit > 1

			return (long) (graphValue * graphUnit);
		}
	}

	/**
	 * Round number of units to a 'month suitable' format.
	 * 
	 * @param defaultUnitValue
	 * @return
	 */
	public static int roundMonthUnits(final int defaultUnitValue) {

		float unit = defaultUnitValue;

		while (unit > 144) {
			unit /= 12;
		}

		unit = //
				//
		unit >= 12 ? 12 : //
				unit >= 6 ? 6 : //
						unit >= 4 ? 4 : //
								unit > 3 ? 3 : //
										unit > 2 ? 2 : //
												1;
		return (int) unit;
	}

	/**
	 * @param defaultUnitValue
	 * @param is24HourFormatting
	 * @return Returns minUnitValue rounded to the number 60/30/20/10/5/2/1
	 */
	public static float roundTimeValue(final float defaultUnitValue, final boolean is24HourFormatting) {

		float unit = defaultUnitValue;
		int multiplier = 1;

		if (is24HourFormatting) {

			while (unit > 120) {
				multiplier *= 60;
				unit /= 60;
			}

			if (multiplier >= 3600) {

				unit = //
						//
				unit >= 120 ? 120 : //
						unit >= 60 ? 60 : //
								unit >= 30 ? 30 : //
										unit >= 15 ? 15 : //
												unit >= 12 ? 12 : //
														unit > 6 ? 6 : //
																unit > 5 ? 5 : //
																		unit > 2 ? 2 : //
																				1;

			} else {

				unit = //
						//
				unit >= 120 ? 120 : //
						unit >= 60 ? 60 : //
								unit >= 30 ? 30 : //
										unit >= 15 ? 15 : //
												unit >= 10 ? 10 : //
														5;
			}

		} else {

			if (unit > 3600) {

				// unit > 1 hour (>3600 sec)

				while (unit > 3600) {
					multiplier *= 60;
					unit /= 60;
				}

				if (multiplier >= 3600) {

					// multiplier >= 1 hour

					unit = //
							//
					unit > 720 ? 720 : //
							unit > 240 ? 240 : //
									unit > 120 ? 120 : //
											unit > 24 ? 24 : //
													10;
//					unit = //
//							//
//					unit > 1000 ? 1000 : //
//							unit > 500 ? 500 : //
//									unit > 100 ? 100 : //
//											unit > 50 ? 50 : //
//													10;

				} else {

					// multiplier < 1 hour (3600 sec)

					unit = //
							//
					unit > 3000 ? 3000 : //
							unit > 2400 ? 2400 : //
									unit > 600 ? 600 : //
											unit > 300 ? 300 : //
													60;
				}

			} else {

				// unit < 1 hour (< 3600 sec)

				while (unit > 120) {
					multiplier *= 60;
					unit /= 60;
				}

				unit = //
						//
				unit > 120 ? 120 : //
						unit > 60 ? 60 : //
								unit > 30 ? 30 : //
										unit > 15 ? 15 : //
												unit > 10 ? 10 : //
														unit > 5 ? 5 : //
																unit > 2 ? 2 : //
																		1;
			}
		}

//		System.out.println(unit + "\tmultiplier: " + multiplier);
//		// TODO remove SYSTEM.OUT.PRINTLN

		unit *= multiplier;

//		System.out.println(unit + "\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		return (long) unit;
	}

	/**
	 * Round floating value by removing the trailing part, which causes problem when creating units.
	 * For the value 200.00004 the .00004 part will be removed
	 * 
	 * @param graphValue
	 * @param graphUnit
	 * @return
	 */
	public static double roundValueToUnit(final double graphValue, final double graphUnit, final boolean isMinValue) {

		if (graphUnit < 1) {

			if (graphValue < 0) {

				final double gvDiv1 = graphValue / graphUnit;
				final int gvDiv2 = (int) (gvDiv1 - 0.5f);
				final double gvDiv3 = gvDiv2 * graphUnit;

				return gvDiv3;

			} else {

				final double gvDiv1 = graphValue / graphUnit;
				final int gvDiv2 = (int) (gvDiv1 + (isMinValue ? -0.5f : 0.5f));
				final double gvDiv3 = gvDiv2 * graphUnit;

				return gvDiv3;
			}

		} else {

			// graphUnit >= 1

			if (graphValue < 0) {

				final double gvDiv1 = graphValue * graphUnit;
				final long gvDiv2 = (long) (gvDiv1 + (isMinValue ? -0.5f : 0.5f));
				final double gvDiv3 = gvDiv2 / graphUnit;

				return gvDiv3;

			} else {

				// graphValue >= 0

				final double gvDiv1 = graphValue * graphUnit;
				final long gvDiv2 = (long) (gvDiv1 + 0.5f);
				final double gvDiv3 = gvDiv2 / graphUnit;

				return gvDiv3;
			}
		}
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
