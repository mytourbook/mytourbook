/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import java.io.File;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class UI {

//	long startTime = System.currentTimeMillis();

//	long endTime = System.currentTimeMillis();
//	System.out.println("Execution time : " + (endTime - startTime) + " ms");

	public static final boolean								IS_LINUX						= "gtk"	.equals(SWT.getPlatform());			//$NON-NLS-1$
	public static final boolean								IS_OSX							= "carbon".equals(SWT.getPlatform()) //$NON-NLS-1$
																									|| "cocoa".equals(SWT.getPlatform());	//$NON-NLS-1$
	public static final boolean								IS_WIN							= "win32".equals(SWT.getPlatform()) //$NON-NLS-1$
																									|| "wpf".equals(SWT.getPlatform());	//$NON-NLS-1$

	public static final String								EMPTY_STRING					= "";											//$NON-NLS-1$
	public static final String								SPACE							= " ";											//$NON-NLS-1$
	public static final String								SPACE2							= "  ";										//$NON-NLS-1$
	public static final String								SPACE4							= "    ";										//$NON-NLS-1$
	public static final String								COLON_SPACE						= ": ";										//$NON-NLS-1$
	public static final String								COMMA_SPACE						= ", ";										//$NON-NLS-1$
	public static final String								UNDERSCORE						= "_";											//$NON-NLS-1$
	public static final String								DASH							= "-";											//$NON-NLS-1$
	public static final String								DASH_WITH_SPACE					= " - ";										//$NON-NLS-1$
	public static final String								DASH_WITH_DOUBLE_SPACE			= "   -   ";									//$NON-NLS-1$
	public static final String								SLASH_WITH_SPACE				= " / ";										//$NON-NLS-1$
	public static final String								EMPTY_STRING_FORMAT				= "%s";										//$NON-NLS-1$
	public static final String								MNEMONIC						= "&";											//$NON-NLS-1$
	public static final String								BREAK_TIME_MARKER				= "x";											//$NON-NLS-1$

	public static final char								TAB								= '\t';
	public static final char								DOT								= '.';

	/**
	 * contains a new line
	 */
	public static final String								NEW_LINE						= "\n";										//$NON-NLS-1$

	/**
	 * contains 2 new lines
	 */
	public static final String								NEW_LINE2						= "\n\n";										//$NON-NLS-1$

	public static final String								SYSTEM_NEW_LINE					= System
																									.getProperty("line.separator");		//$NON-NLS-1$

	public static final String								IS_NOT_INITIALIZED				= "IS NOT INITIALIZED";						//$NON-NLS-1$

	public static final String								VIEW_COLOR_CATEGORY				= "view.color.category";						//$NON-NLS-1$
	public static final String								VIEW_COLOR_TITLE				= "view.color.title";							//$NON-NLS-1$
	public static final String								VIEW_COLOR_SUB					= "view.color.sub";							//$NON-NLS-1$
	public static final String								VIEW_COLOR_SUB_SUB				= "view.color.sub-sub";						//$NON-NLS-1$
	public static final String								VIEW_COLOR_TOUR					= "view.color.tour";							//$NON-NLS-1$
	public static final String								VIEW_COLOR_BG_SEGMENTER_UP		= "view.colorBG.segmenterUp";					//$NON-NLS-1$
	public static final String								VIEW_COLOR_BG_SEGMENTER_DOWN	= "view.colorBG.segmenterDown";				//$NON-NLS-1$

	public static final int									DEFAULT_FIELD_WIDTH				= 40;
	public static final int									FORM_FIRST_COLUMN_INDENT		= 16;

	/**
	 * layout hint for a description field
	 */
	public static final int									DEFAULT_DESCRIPTION_WIDTH		= 350;

	public static final String								UTF_8							= "UTF-8";										//$NON-NLS-1$

	/*
	 * labels for the different measurement systems
	 */
	public static final String								UNIT_METER						= "m";											//$NON-NLS-1$
	private static final String								UNIT_ALTITUDE_M					= "m";											//$NON-NLS-1$
	public static final String								UNIT_DISTANCE_KM				= "km";										//$NON-NLS-1$
	private static final String								UNIT_SPEED_KM_H					= "km/h";										//$NON-NLS-1$
	private static final String								UNIT_TEMPERATURE_C				= "\u00B0C";									//$NON-NLS-1$
	private static final String								UNIT_ALTIMETER_M_H				= "m/h";										//$NON-NLS-1$
	private static final String								UNIT_PACE_MIN_P_KM				= "min/km";									//$NON-NLS-1$
	public static final String								UNIT_WEIGHT_KG					= "kg";										//$NON-NLS-1$
	public static final String								UNIT_MBYTES						= "MByte";										//$NON-NLS-1$

	private static final String								UNIT_DISTANCE_YARD				= "yd";										//$NON-NLS-1$
	private static final String								UNIT_ALTITUDE_FT				= "ft";										//$NON-NLS-1$
	public static final String								UNIT_DISTANCE_MI				= "mi";										//$NON-NLS-1$
	private static final String								UNIT_SPEED_MPH					= "mph";										//$NON-NLS-1$
	private static final String								UNIT_TEMPERATURE_F				= "\u00B0F";									//$NON-NLS-1$
	private static final String								UNIT_ALTIMETER_FT_H				= "ft/h";										//$NON-NLS-1$
	private static final String								UNIT_PACE_MIN_P_MILE			= "min/mi";									//$NON-NLS-1$

	public static final String								UNIT_LABEL_POWER				= "Watt";										//$NON-NLS-1$
	public static final String								UNIT_LABEL_MS					= "ms";										//$NON-NLS-1$

	public static final String								SYMBOL_AVERAGE					= "\u00f8";									//$NON-NLS-1$
	public static final String								SYMBOL_AVERAGE_WITH_SPACE		= "\u00f8 ";									//$NON-NLS-1$
	public static final String								SYMBOL_DASH						= "\u2212";									//$NON-NLS-1$
	public static final String								SYMBOL_DIFFERENCE				= "\u0394";									//$NON-NLS-1$
	public static final String								SYMBOL_DIFFERENCE_WITH_SPACE	= "\u0394 ";									//$NON-NLS-1$
	public static final String								SYMBOL_DOUBLE_HORIZONTAL		= "\u2550";									//$NON-NLS-1$
	public static final String								SYMBOL_DOUBLE_VERTICAL			= "\u2551";									//$NON-NLS-1$
	public static final String								SYMBOL_DEGREE					= "\u00B0";									//$NON-NLS-1$
	public static final String								SYMBOL_ELLIPSIS					= "\u2026";									//$NON-NLS-1$
	public static final String								SYMBOL_INFINITY					= "\u221E";									//$NON-NLS-1$
	public static final String								SYMBOL_SUM_WITH_SPACE			= "\u2211 ";									//$NON-NLS-1$
	public static final String								SYMBOL_TAU						= "\u03c4";									//$NON-NLS-1$

	public static final String								SYMBOL_DOT						= ".";											//$NON-NLS-1$
	public static final String								SYMBOL_COLON					= ":";											//$NON-NLS-1$
	public static final String								SYMBOL_EQUAL					= "=";											//$NON-NLS-1$
	public static final String								SYMBOL_GREATER_THAN				= ">";											//$NON-NLS-1$
	public static final String								SYMBOL_LESS_THAN				= "<";											//$NON-NLS-1$
	public static final String								SYMBOL_PERCENTAGE				= "%";											//$NON-NLS-1$
	public static final String								SYMBOL_WIND_WITH_SPACE			= "W ";										//$NON-NLS-1$
	public static final String								SYMBOL_EXCLAMATION_POINT		= "!";											//$NON-NLS-1$

	/**
	 * Imperial system for distance
	 */
	public static final float								UNIT_MILE						= 1.609344f;

	/**
	 * Imperial system for small distance, 1 yard = 3 feet = 36 inches = 0,9144 Meter
	 */
	public static final float								UNIT_YARD						= 0.9144f;

	/**
	 * Imperial system for height
	 */
	public static final float								UNIT_FOOT						= 0.3048f;

	/**
	 * contains the system of measurement value for distances relative to the metric system, the
	 * metric system is <code>1</code>
	 */
	public static float										UNIT_VALUE_DISTANCE				= 1;

	/**
	 * contains the system of measurement value for small distances relative to the metric system,
	 * the metric system is <code>1</code>
	 */
	public static float										UNIT_VALUE_DISTANCE_SMALL		= 1;

	/**
	 * contains the system of measurement value for altitudes relative to the metric system, the
	 * metric system is <code>1</code>
	 */
	public static float										UNIT_VALUE_ALTITUDE				= 1;

	/**
	 * contains the system of measurement value for the temperature, is set to <code>1</code> for
	 * the metric system
	 */
	public static float										UNIT_VALUE_TEMPERATURE			= 1;

	// (Celcius * 9/5) + 32 = Fahrenheit
	public static final float								UNIT_FAHRENHEIT_MULTI			= 1.8f;
	public static final float								UNIT_FAHRENHEIT_ADD				= 32;

	public static final String								UNIT_LABEL_TIME					= "h";											//$NON-NLS-1$

	/**
	 * contains the unit label in the currenty measurement system for the distance values
	 */
	public static String									UNIT_LABEL_DISTANCE;
	public static String									UNIT_LABEL_DISTANCE_SMALL;
	public static String									UNIT_LABEL_ALTITUDE;
	public static String									UNIT_LABEL_ALTIMETER;
	public static String									UNIT_LABEL_TEMPERATURE;
	public static String									UNIT_LABEL_SPEED;
	public static String									UNIT_LABEL_PACE;
	public static String									UNIT_LABEL_DIRECTION			= "\u00B0";									//$NON-NLS-1$

	private static final String								TOUR_TYPE_PREFIX				= "tourType";									//$NON-NLS-1$
//	private static final String						WEATHER_CLOUDS_PREFIX			= "weatherClouds-";						//$NON-NLS-1$

	public final static ImageRegistry						IMAGE_REGISTRY;

	/*
	 * image keys for images which are stored in the image registry
	 */
	public static final String								IMAGE_EMPTY_16					= "_empty16";									//$NON-NLS-1$

	public static final String								IMAGE_TOUR_TYPE_FILTER			= "tourType-filter";							//$NON-NLS-1$
	public static final String								IMAGE_TOUR_TYPE_FILTER_SYSTEM	= "tourType-filter-system";					//$NON-NLS-1$

	private static final int								TOUR_TYPE_IMAGE_WIDTH			= 16;
	private static final int								TOUR_TYPE_IMAGE_HEIGHT			= 16;

	private static UI										instance;

	public static final DateFormat							TimeFormatterShort				= DateFormat
																									.getTimeInstance(DateFormat.SHORT);
	public static final DateFormat							DateFormatterShort				= DateFormat
																									.getDateInstance(DateFormat.SHORT);
	public static final DateFormat							DateFormatterLong				= DateFormat
																									.getDateInstance(DateFormat.LONG);
	public static final DateFormat							DateFormatterFull				= DateFormat
																									.getDateInstance(DateFormat.FULL);
	public static final SimpleDateFormat					MonthFormatter					= new SimpleDateFormat(
																									"MMM");								//$NON-NLS-1$
	public static final SimpleDateFormat					WeekDayFormatter				= new SimpleDateFormat(
																									"EEEE");								//$NON-NLS-1$

	public static final DateTimeFormatter					DTFormatterShort				= DateTimeFormat
																									.shortDateTime();

	private static DateFormat								_dateFormatterShort;
	private static DateFormat								_timeFormatterShort;

	private static StringBuilder							_formatterSB					= new StringBuilder();
	private static Formatter								_formatter						= new Formatter(
																									_formatterSB);

	public static Styler									TAG_STYLER;
	public static Styler									TAG_CATEGORY_STYLER;
	public static Styler									TAG_SUB_STYLER;
	public static Styler									PHOTO_FOLDER_STYLER;
	public static Styler									PHOTO_FILE_STYLER;

	private final static HashMap<String, Image>				_imageCache						= new HashMap<String, Image>();
	private final static HashMap<String, ImageDescriptor>	_imageCacheDescriptor			= new HashMap<String, ImageDescriptor>();
	private final static HashMap<String, Boolean>			_dirtyImages					= new HashMap<String, Boolean>();

	static {

		updateUnits();
		setViewColorsFromPrefStore();
		setPhotoColorsFromPrefStore();

		/*
		 * load images into the image registry
		 */
		IMAGE_REGISTRY = TourbookPlugin.getDefault().getImageRegistry();

		IMAGE_REGISTRY.put(IMAGE_EMPTY_16, TourbookPlugin.getImageDescriptor(Messages.Image___Empty16));

		// tour type images
		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER, //
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter));
		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER_SYSTEM,//
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter_system));

		// data transfer
		IMAGE_REGISTRY.put(RawDataView.IMAGE_DATA_TRANSFER,//
				TourbookPlugin.getImageDescriptor(Messages.Image__RawData_Transfer));
		IMAGE_REGISTRY.put(RawDataView.IMAGE_DATA_TRANSFER_DIRECT,//
				TourbookPlugin.getImageDescriptor(Messages.Image__RawData_TransferDirect));
		IMAGE_REGISTRY.put(RawDataView.IMAGE_IMPORT,//
				TourbookPlugin.getImageDescriptor(Messages.Image__RawData_Import));

		// weather images
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_CLEAR, //
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_sunny));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_PART_CLOUDS, //
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_cloudy));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_OVERCAST, //
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_clouds));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_LIGHTNING,//
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_lightning));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_RAIN,//
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_rain));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SNOW,//
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_snow));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SCATTERED_SHOWERS,//
				TourbookPlugin.getImageDescriptor(Messages.Image__Weather_ScatteredShowers));
		IMAGE_REGISTRY.put(IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT,//
				TourbookPlugin.getImageDescriptor(Messages.Image__Weather_Severe));

		/*
		 * set tag styler
		 */
		TAG_CATEGORY_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_CATEGORY, null);
		TAG_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_TITLE, null);
		TAG_SUB_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_SUB, null);

		/*
		 * set photo styler
		 */
		PHOTO_FOLDER_STYLER = StyledString.createColorRegistryStyler(
				ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER,
				null);
		PHOTO_FILE_STYLER = StyledString.createColorRegistryStyler(ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE, null);
	}

	private UI() {}

	public static void adjustScaleValueOnMouseScroll(final MouseEvent event) {

		boolean isCtrlKey;
		boolean isShiftKey;

		if (UI.IS_OSX) {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD3) > 0;
//			isAltKey = (event.stateMask & SWT.MOD3) > 0;
		} else {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD2) > 0;
//			isAltKey = (event.stateMask & SWT.MOD3) > 0;
		}

		// accelerate with Ctrl + Shift key
		int accelerator = isCtrlKey ? 10 : 1;
		accelerator *= isShiftKey ? 5 : 1;

		final Scale scale = (Scale) event.widget;
		final int increment = scale.getIncrement();
		final int oldValue = scale.getSelection();
		final int valueDiff = ((event.count > 0 ? increment : -increment) * accelerator);

		scale.setSelection(oldValue + valueDiff);
	}

	public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event) {

		boolean isCtrlKey;
		boolean isShiftKey;

		if (UI.IS_OSX) {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD3) > 0;
//			isAltKey = (event.stateMask & SWT.MOD3) > 0;
		} else {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD2) > 0;
//			isAltKey = (event.stateMask & SWT.MOD3) > 0;
		}

		// accelerate with Ctrl + Shift key
		int accelerator = isCtrlKey ? 10 : 1;
		accelerator *= isShiftKey ? 5 : 1;

		final Spinner spinner = (Spinner) event.widget;
		final int newValue = ((event.count > 0 ? 1 : -1) * accelerator);

		spinner.setSelection(spinner.getSelection() + newValue);
	}

	/**
	 * Change the title for the application
	 * 
	 * @param newTitle
	 *            new title for the application or <code>null</code> to set the original title
	 */
	public static void changeAppTitle(final String newTitle) {

		final Display display = Display.getDefault();

		if (display != null) {

			// Look at all the shells and pick the first one that is a workbench window.
			final Shell shells[] = display.getShells();
			for (final Shell shell : shells) {

				final Object data = shell.getData();

				// Check whether this shell points to the Application main window's shell:
				if (data instanceof IWorkbenchWindow) {

					String title;
					if (newTitle == null) {
						title = Messages.App_Title;
					} else {
						title = newTitle;
					}

					shell.setText(title);
					break;
				}
			}
		}
	}

	/**
	 * @param file
	 * @return Returns <code>true</code> when the file should be overwritten, otherwise
	 *         <code>false</code>
	 */
	public static boolean confirmOverwrite(final File file) {

		final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final MessageDialog dialog = new MessageDialog(
				shell,
				Messages.app_dlg_confirmFileOverwrite_title,
				null,
				NLS.bind(Messages.app_dlg_confirmFileOverwrite_message, file.getPath()),
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL },
				0);
		dialog.open();

		return dialog.getReturnCode() == 0;
	}

	public static boolean confirmOverwrite(final FileCollisionBehavior fileCollision, final File file) {

		final boolean[] isOverwrite = { false };

		final int fileCollisionValue = fileCollision.value;

		if (fileCollisionValue == FileCollisionBehavior.REPLACE_ALL) {

			// overwrite is already confirmed
			isOverwrite[0] = true;

		} else if (fileCollisionValue == FileCollisionBehavior.ASK
				|| fileCollisionValue == FileCollisionBehavior.REPLACE
				|| fileCollisionValue == FileCollisionBehavior.KEEP) {

			Display.getDefault().syncExec(new Runnable() {
				public void run() {

					final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					final MessageDialog dialog = new MessageDialog(
							shell,
							Messages.app_dlg_confirmFileOverwrite_title,
							null,
							NLS.bind(Messages.app_dlg_confirmFileOverwrite_message, file.getPath()),
							MessageDialog.QUESTION,
							new String[] {
									IDialogConstants.YES_LABEL,
									IDialogConstants.YES_TO_ALL_LABEL,
									IDialogConstants.NO_LABEL,
									IDialogConstants.NO_TO_ALL_LABEL },
							0);
					dialog.open();

					final int returnCode = dialog.getReturnCode();
					switch (returnCode) {

					case -1: // dialog was canceled
						fileCollision.value = FileCollisionBehavior.DIALOG_IS_CANCELED;
						break;

					case 0: // YES
						fileCollision.value = FileCollisionBehavior.REPLACE;
						isOverwrite[0] = true;
						break;

					case 1: // YES_TO_ALL
						fileCollision.value = FileCollisionBehavior.REPLACE_ALL;
						isOverwrite[0] = true;
						break;

					case 2: // NO
						fileCollision.value = FileCollisionBehavior.KEEP;
						break;

					case 3: // NO_TO_ALL
						fileCollision.value = FileCollisionBehavior.KEEP_ALL;
						break;

					default:
						break;
					}
				}
			});

		}

		return isOverwrite[0];
	}

	/**
	 * Checks if tour id is contained in the property data
	 * 
	 * @param propertyData
	 * @param checkedTourId
	 * @return Returns the tour id when it is contained in the property data, otherwise it returns
	 *         <code>null</code>
	 */
	public static Long containsTourId(final Object propertyData, final long checkedTourId) {

		Long containedTourId = null;

		if (propertyData instanceof SelectionTourId) {

			final Long tourId = ((SelectionTourId) propertyData).getTourId();
			if (checkedTourId == tourId) {
				containedTourId = tourId;
			}

		} else if (propertyData instanceof SelectionTourIds) {

			for (final Long tourId : ((SelectionTourIds) propertyData).getTourIds()) {
				if (checkedTourId == tourId) {
					containedTourId = tourId;
					break;
				}
			}

		} else if (propertyData instanceof TourEvent) {

			final TourEvent tourEvent = (TourEvent) propertyData;
			final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

			if (modifiedTours != null) {
				for (final TourData tourData : modifiedTours) {
					if (tourData.getTourId().longValue() == checkedTourId) {
						containedTourId = checkedTourId;
						break;
					}
				}
			}
		}

		return containedTourId;
	}

	/**
	 * Display text as a bulleted list
	 * 
	 * @param parent
	 * @param bulletText
	 * @param startLine
	 *            Line where bullets should be started, 0 is the first line
	 * @param spanHorizontal
	 * @param horizontalHint
	 * @param backgroundColor
	 *            background color or <code>null</code> when color should not be set
	 * @return Returns the bulleted list as styled text
	 */
	public static StyledText createBullets(	final Composite parent,
											final String bulletText,
											final int startLine,
											final int spanHorizontal,
											final int horizontalHint,
											final Color backgroundColor) {

		StyledText styledText = null;

		try {

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.span(spanHorizontal, 1)
					.hint(horizontalHint, SWT.DEFAULT)
					.applyTo(container);
			GridLayoutFactory.fillDefaults()//
					.numColumns(1)
					.margins(5, 5)
					.applyTo(container);

			container.setBackground(backgroundColor == null ? //
					container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
					: backgroundColor);
			{
				final StyleRange style = new StyleRange();
				style.metrics = new GlyphMetrics(0, 0, 10);

				final Bullet bullet = new Bullet(style);
				final int lineCount = Util.countCharacter(bulletText, '\n');

				styledText = new StyledText(container, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(styledText);
				styledText.setText(bulletText);

				styledText.setLineBullet(startLine, lineCount, bullet);
				styledText.setLineWrapIndent(startLine, lineCount, 10);

				styledText.setBackground(backgroundColor == null ? //
						container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
						: backgroundColor);
			}

		} catch (final Exception e) {
			// ignore exception when there are less lines as required
			StatusUtil.log(e);
		}

		return styledText;
	}

	/**
	 * Creates a label which is wrapped and grabs the width.
	 * 
	 * @param formToolkit
	 * @param parent
	 * @param labelText
	 * @return
	 */
	public static Composite createLabel(final FormToolkit formToolkit, final Composite parent, final String labelText) {

		final Composite container = formToolkit.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			final Label label = formToolkit.createLabel(container, labelText, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
		}

		return container;
	}

	/**
	 * Disables all controls and their children
	 */
	public static void disableAllControls(final Composite container) {

		disableAllControlsInternal(container);

		// !!! force controls (text,combo...) to be updated !!!
		container.update();
	}

	/**
	 * !!!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!!
	 */
	private static void disableAllControlsInternal(final Composite container) {

		for (final Control child : container.getChildren()) {

			if (child instanceof Composite) {
				disableAllControlsInternal((Composite) child);
			}

			child.setEnabled(false);
		}
	}

	public static Formatter format_hh_mm(final long time) {

		_formatterSB.setLength(0);

		return _formatter.format(Messages.Format_hhmm, (time / 3600), ((time % 3600) / 60));
	}

	/**
	 * Hours are ignored when they are 0. An empty string is returned when time = <code>-1</code>
	 * 
	 * @param time
	 * @return
	 */
	public static String format_hh_mm_ss(final long time) {

		if (time == -1) {
			return UI.EMPTY_STRING;
		}

		_formatterSB.setLength(0);

		if (time >= 3600) {

			// display hours

			return _formatter.format(//
					Messages.Format_hhmmss,
					(time / 3600),
					((time % 3600) / 60),
					((time % 3600) % 60)).toString();

		} else {

			// ignore hours

			return _formatter.format(//
					Messages.Format_hhmm,
					((time % 3600) / 60),
					((time % 3600) % 60)).toString();
		}
	}

	/**
	 * force hours to be displayed
	 * 
	 * @param time
	 * @return
	 */
	public static String format_hhh_mm_ss(final long time) {

		_formatterSB.setLength(0);

		return _formatter.format(//
				Messages.Format_hhmmss,
				(time / 3600),
				((time % 3600) / 60),
				((time % 3600) % 60)).toString();
	}

	public static String format_mm_ss(final long time) {

		_formatterSB.setLength(0);

		if (time < 0) {
			_formatterSB.append(UI.DASH);
		}

		final long timeAbs = time < 0 ? 0 - time : time;

		return _formatter.format(Messages.Format_hhmm, (timeAbs / 60), (timeAbs % 60)).toString();
	}

	public static String format_yyyymmdd_hhmmss(final int year,
												final int month,
												final int day,
												final int hour,
												final int minute,
												final int second) {

		_formatterSB.setLength(0);

		return _formatter.format(//
				Messages.Format_yyyymmdd_hhmmss,
				year,
				month,
				day,
				hour,
				minute,
				second)//
				.toString();
	}

	public static String format_yyyymmdd_hhmmss(final TourData tourData) {

		if (tourData == null) {
			return UI.EMPTY_STRING;
		}

		_formatterSB.setLength(0);

		return _formatter.format(//
				Messages.Format_yyyymmdd_hhmmss,
				tourData.getStartYear(),
				tourData.getStartMonth(),
				tourData.getStartDay(),
				tourData.getStartHour(),
				tourData.getStartMinute(),
				tourData.getStartSecond())//
				.toString();
	}

	/**
	 * @param degreeDirection
	 * @return Returns cardinal direction
	 */
	public static String getCardinalDirectionText(final int degreeDirection) {

		return IWeather.windDirectionText[getCardinalDirectionTextIndex(degreeDirection)];
	}

	/**
	 * @param degreeDirection
	 * @return Returns cardinal direction index for {@link IWeather#windDirectionText}
	 */
	public static int getCardinalDirectionTextIndex(final int degreeDirection) {

		final float degree = (degreeDirection + 22.5f) / 45.0f;

		final int directionIndex = ((int) degree) % 8;

		return directionIndex;
	}

	public static ColumnPixelData getColumnPixelWidth(final PixelConverter pixelConverter, final int width) {
		return new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(width), false);
	}

	/******************************************************************************
	 * this method is copied from the following source and was adjusted
	 * 
	 * <pre>
	 * Product: Compiere ERP &amp; CRM Smart Business Solution                    *
	 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
	 * This program is free software; you can redistribute it and/or modify it    *
	 * under the terms version 2 of the GNU General Public License as published   *
	 * by the Free Software Foundation. This program is distributed in the hope   *
	 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
	 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
	 * See the GNU General Public License for more details.                       *
	 * You should have received a copy of the GNU General Public License along    *
	 * with this program; if not, write to the Free Software Foundation, Inc.,    *
	 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
	 * For the text or an alternative of this public license, you may reach us    *
	 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
	 * or via info@compiere.org or http://www.compiere.org/license.html           *
	 * </pre>
	 * 
	 * @return date formatter with leading zeros for month and day and 4-digit year
	 */
	public static DateFormat getFormatterDateShort() {

		if (_dateFormatterShort == null) {

			final DateFormat dateInstance = DateFormat.getDateInstance(DateFormat.SHORT);
			if (dateInstance instanceof SimpleDateFormat) {

				final SimpleDateFormat sdf = (SimpleDateFormat) (_dateFormatterShort = dateInstance);

				String oldPattern = sdf.toPattern();

				//	some short formats have only one M and d (e.g. ths US)
				if (oldPattern.indexOf("MM") == -1 && oldPattern.indexOf("dd") == -1) {//$NON-NLS-1$ //$NON-NLS-2$
					String newPattern = UI.EMPTY_STRING;
					for (int i = 0; i < oldPattern.length(); i++) {
						if (oldPattern.charAt(i) == 'M') {
							newPattern += "MM"; //$NON-NLS-1$
						} else if (oldPattern.charAt(i) == 'd') {
							newPattern += "dd"; //$NON-NLS-1$
						} else {
							newPattern += oldPattern.charAt(i);
						}
					}
					sdf.applyPattern(newPattern);
				}

				//	Unknown short format => use JDBC
				if (sdf.toPattern().length() != 8) {
					sdf.applyPattern("yyyy-MM-dd"); //$NON-NLS-1$
				}

				//	4 digit year
				if (sdf.toPattern().indexOf("yyyy") == -1) { //$NON-NLS-1$
					oldPattern = sdf.toPattern();
					String newPattern = UI.EMPTY_STRING;
					for (int i = 0; i < oldPattern.length(); i++) {
						if (oldPattern.charAt(i) == 'y') {
							newPattern += "yy"; //$NON-NLS-1$
						} else {
							newPattern += oldPattern.charAt(i);
						}
					}
					sdf.applyPattern(newPattern);
				}

				sdf.setLenient(true);
			}
		}

		return _dateFormatterShort;
	}


	/******************************************************************************
	 * this method is copied from the following source and was adjusted
	 * 
	 * <pre>
	 * Product: Compiere ERP &amp; CRM Smart Business Solution                    *
	 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
	 * This program is free software; you can redistribute it and/or modify it    *
	 * under the terms version 2 of the GNU General Public License as published   *
	 * by the Free Software Foundation. This program is distributed in the hope   *
	 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
	 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
	 * See the GNU General Public License for more details.                       *
	 * You should have received a copy of the GNU General Public License along    *
	 * with this program; if not, write to the Free Software Foundation, Inc.,    *
	 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
	 * For the text or an alternative of this public license, you may reach us    *
	 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
	 * or via info@compiere.org or http://www.compiere.org/license.html           *
	 * </pre>
	 * 
	 * @return date formatter with leading zeros for month and day and 4-digit year
	 */
	public static DateFormat getFormatterTimeShort() {

		if (_timeFormatterShort == null) {

			final DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.SHORT);
			if (timeInstance instanceof SimpleDateFormat) {

				final SimpleDateFormat sdf = (SimpleDateFormat) (_timeFormatterShort = timeInstance);

				final String oldPattern = sdf.toPattern();

				//	some short formats have only one h (e.g. ths US)
				if (oldPattern.indexOf("hh") == -1) {//$NON-NLS-1$

					String newPattern = UI.EMPTY_STRING;

					for (int i = 0; i < oldPattern.length(); i++) {
						if (oldPattern.charAt(i) == 'h') {
							newPattern += "hh"; //$NON-NLS-1$
						} else {
							newPattern += oldPattern.charAt(i);
						}
					}

					sdf.applyPattern(newPattern);
				}

				sdf.setLenient(true);
			}
		}

		return _timeFormatterShort;
	}

	public static UI getInstance() {

		if (instance == null) {
			instance = new UI();
		}

		return instance;
	}

	/**
	 * Checks if propertyData has the same tour as the oldTourData
	 * 
	 * @param propertyData
	 * @param oldTourData
	 * @return Returns {@link TourData} from the propertyData or <code>null</code> when it's another
	 *         tour
	 */
	public static TourData getTourPropertyTourData(final TourEvent propertyData, final TourData oldTourData) {

		final ArrayList<TourData> modifiedTours = propertyData.getModifiedTours();
		if (modifiedTours == null) {
			return null;
		}

		final long oldTourId = oldTourData.getTourId();

		for (final TourData tourData : modifiedTours) {
			if (tourData.getTourId() == oldTourId) {

				// nothing more to do, only one tour is supported
				return tourData;
			}
		}

		return null;
	}

	public static void restoreCombo(final Combo combo, final String[] comboItems) {

		if (comboItems == null || comboItems.length == 0) {
			return;
		}

		for (final String pathItem : comboItems) {
			combo.add(pathItem);
		}

		// restore last used path
		combo.setText(comboItems[0]);
	}

	/**
	 * Restore the sash weight from a memento
	 * 
	 * @param sash
	 * @param fMemento
	 * @param weightKey
	 * @param sashDefaultWeight
	 */
	public static void restoreSashWeight(	final SashForm sash,
											final IMemento fMemento,
											final String weightKey,
											final int[] sashDefaultWeight) {

		final int[] sashWeights = sash.getWeights();
		final int[] newWeights = new int[sashWeights.length];

		for (int weightIndex = 0; weightIndex < sashWeights.length; weightIndex++) {

			final Integer mementoWeight = fMemento.getInteger(weightKey + Integer.toString(weightIndex));

			if (mementoWeight == null) {
				try {
					newWeights[weightIndex] = sashDefaultWeight[weightIndex];

				} catch (final ArrayIndexOutOfBoundsException e) {
					newWeights[weightIndex] = 100;
				}
			} else {
				newWeights[weightIndex] = mementoWeight;
			}
		}

		sash.setWeights(newWeights);
	}

	public static ImageData rotate(final ImageData srcData, final int direction) {

		final int bytesPerPixel = srcData.bytesPerLine / srcData.width;
		final int destBytesPerLine = (direction == SWT.DOWN) ? //
				srcData.width * bytesPerPixel
				: srcData.height * bytesPerPixel;

		final byte[] newData = new byte[srcData.data.length];
		int width = 0, height = 0;

		for (int srcY = 0; srcY < srcData.height; srcY++) {
			for (int srcX = 0; srcX < srcData.width; srcX++) {

				int destX = 0, destY = 0, destIndex = 0, srcIndex = 0;

				switch (direction) {
				case SWT.LEFT: // left 90 degrees
					destX = srcY;
					destY = srcData.width - srcX - 1;
					width = srcData.height;
					height = srcData.width;
					break;
				case SWT.RIGHT: // right 90 degrees
					destX = srcData.height - srcY - 1;
					destY = srcX;
					width = srcData.height;
					height = srcData.width;
					break;
				case SWT.DOWN: // 180 degrees
					destX = srcData.width - srcX - 1;
					destY = srcData.height - srcY - 1;
					width = srcData.width;
					height = srcData.height;
					break;
				}

				destIndex = (destY * destBytesPerLine) + (destX * bytesPerPixel);
				srcIndex = (srcY * srcData.bytesPerLine) + (srcX * bytesPerPixel);

				System.arraycopy(srcData.data, srcIndex, newData, destIndex, bytesPerPixel);
			}
		}

		// destBytesPerLine is used as scanlinePad to ensure that no padding is required
		return new ImageData(width, height, srcData.depth, srcData.palette, destBytesPerLine, newData);
	}

	/**
	 * Store the weights for the sash in a memento
	 * 
	 * @param sash
	 * @param memento
	 * @param weightKey
	 */
	public static void saveSashWeight(final SashForm sash, final IMemento memento, final String weightKey) {

		final int[] weights = sash.getWeights();

		for (int weightIndex = 0; weightIndex < weights.length; weightIndex++) {
			memento.putInteger(weightKey + Integer.toString(weightIndex), weights[weightIndex]);
		}
	}

	/**
	 * Set grid layout with no margins for a composite
	 * 
	 * @param composite
	 */
	public static void set0GridLayout(final Composite composite) {
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 0;
		composite.setLayout(gridLayout);
	}

	public static void setDefaultColor(final Control control) {
		control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		control.setBackground(null);
	}

	/**
	 * set width for all controls in one column to the max width value
	 */
	public static void setEqualizeColumWidths(final ArrayList<Control> columnControls) {
		setEqualizeColumWidths(columnControls, 0);
	}

	public static void setEqualizeColumWidths(final ArrayList<Control> columnControls, final int additionalSpace) {

		int maxWidth = 0;

		// get max width from all first columns controls
		for (final Control control : columnControls) {

			if (control.isDisposed()) {
				// this should not happen, but it did during testing
				return;
			}

			final int width = control.getSize().x + additionalSpace;

			maxWidth = width > maxWidth ? width : maxWidth;
		}

		// set width for all first column controls
		for (final Control control : columnControls) {
			((GridData) control.getLayoutData()).widthHint = maxWidth;
		}
	}

	public static void setErrorColor(final Text control) {
		control.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
	}

	public static GridData setFieldWidth(final Composite parent, final StringFieldEditor field, final int width) {
		final GridData gd = new GridData();
		gd.widthHint = width;
		field.getTextControl(parent).setLayoutData(gd);
		return gd;
	}

	public static void setHorizontalSpacer(final Composite parent, final int columns) {
		final Label label = new Label(parent, SWT.NONE);
		final GridData gd = new GridData();
		gd.horizontalSpan = columns;
		label.setLayoutData(gd);
	}

	/**
	 * Set photo colors in the JFace color registry from the pref store
	 */
	public static void setPhotoColorsFromPrefStore() {

		// pref store var cannot be set from a static field because it can be null !!!
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

		colorRegistry.put(ITourbookPreferences.PHOTO_VIEWER_COLOR_FOREGROUND, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.PHOTO_VIEWER_COLOR_FOREGROUND));

		colorRegistry.put(ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));

		colorRegistry.put(ITourbookPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND));

		colorRegistry.put(ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER));

		colorRegistry.put(ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE));
	}

	/**
	 * Set tag colors in the JFace color registry from the pref store
	 */
	public static void setViewColorsFromPrefStore() {

		// pref store var cannot be set from a static field because it can be null !!!
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

		colorRegistry.put(VIEW_COLOR_CATEGORY, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.VIEW_LAYOUT_COLOR_CATEGORY));
		colorRegistry.put(VIEW_COLOR_TITLE, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.VIEW_LAYOUT_COLOR_TITLE));
		colorRegistry.put(VIEW_COLOR_SUB, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB));
		colorRegistry.put(VIEW_COLOR_SUB_SUB, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB_SUB));
		colorRegistry.put(VIEW_COLOR_TOUR, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.VIEW_LAYOUT_COLOR_TOUR));

		colorRegistry.put(VIEW_COLOR_BG_SEGMENTER_UP, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_UP));
		colorRegistry.put(VIEW_COLOR_BG_SEGMENTER_DOWN, //
				PreferenceConverter.getColor(prefStore, ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_DOWN));
	}

	public static GridData setWidth(final Control control, final int width) {
		final GridData gd = new GridData();
		gd.widthHint = width;
		control.setLayoutData(gd);
		return gd;
	}

	public static void showMessageInfo(final String title, final String message) {

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}

	public static void showSQLException(SQLException e) {

		while (e != null) {

			final String sqlExceptionText = Util.getSQLExceptionText(e);

			StatusUtil.log(sqlExceptionText);

			MessageDialog.openError(Display.getCurrent().getActiveShell(), //
					"SQL Error",//$NON-NLS-1$
					sqlExceptionText);

			e = e.getNextException();
		}

		StatusUtil.log(e);
	}

	public static void showSQLException(final SQLException e, final String sqlStatement) {

		final String message = "SQL statement: " + UI.NEW_LINE2 + sqlStatement + Util.getSQLExceptionText(e); //$NON-NLS-1$

		MessageDialog.openError(Display.getCurrent().getActiveShell(), //
				"SQL Error", //$NON-NLS-1$
				message);

		StatusUtil.log(message);
		StatusUtil.log(e);
	}

	public static void updateUITags(final TourData tourData, final Label tourTagLabel) {

		// tour tags
		final Set<TourTag> tourTags = tourData.getTourTags();

		if (tourTags == null || tourTags.size() == 0) {
			tourTagLabel.setText(UI.EMPTY_STRING);
		} else {

			// get all tag id's
			final ArrayList<Long> tagIds = new ArrayList<Long>();
			for (final TourTag tourTag : tourTags) {
				tagIds.add(tourTag.getTagId());
			}

			final String tagLabels = TourDatabase.getTagNames(tagIds);
			tourTagLabel.setText(tagLabels);
			tourTagLabel.setToolTipText(tagLabels);
		}

//		tourTagLabel.pack(true);
	}

	/**
	 * Sets the tour type image and text into a {@link CLabel}
	 * 
	 * @param tourData
	 * @param lblTourType
	 * @param isTextDisplayed
	 */
	public static void updateUITourType(final TourData tourData, final CLabel lblTourType, final boolean isTextDisplayed) {

		final TourType tourType = tourData.getTourType();

		// tour type
		if (tourType == null) {
			lblTourType.setText(UI.EMPTY_STRING);
			lblTourType.setImage(UI.getInstance().getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
		} else {
			lblTourType.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
			lblTourType.setText(isTextDisplayed ? tourType.getName() : UI.EMPTY_STRING);
		}

		lblTourType.pack(true);
		lblTourType.redraw(); // display changed tour image
	}

	/**
	 * update units from the pref store into the application variables
	 */
	public static void updateUnits() {

		// pref store var cannot be set from a static field because it can be null !!!
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		/*
		 * distance
		 */
		if (prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE).equals(
				ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI)) {

			// set imperial measure system

			UNIT_VALUE_DISTANCE = UNIT_MILE;
			UNIT_VALUE_DISTANCE_SMALL = UNIT_YARD;

			UNIT_LABEL_DISTANCE = UNIT_DISTANCE_MI;
			UNIT_LABEL_DISTANCE_SMALL = UNIT_DISTANCE_YARD;

			UNIT_LABEL_SPEED = UNIT_SPEED_MPH;
			UNIT_LABEL_PACE = UNIT_PACE_MIN_P_MILE;

		} else {

			// default is the metric measure system

			UNIT_VALUE_DISTANCE = 1;
			UNIT_VALUE_DISTANCE_SMALL = 1;

			UNIT_LABEL_DISTANCE = UNIT_DISTANCE_KM;
			UNIT_LABEL_DISTANCE_SMALL = UNIT_METER;

			UNIT_LABEL_SPEED = UNIT_SPEED_KM_H;
			UNIT_LABEL_PACE = UNIT_PACE_MIN_P_KM;
		}

		/*
		 * altitude
		 */
		if (prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE).equals(
				ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_FOOT)) {

			// set imperial measure system

			UNIT_VALUE_ALTITUDE = UNIT_FOOT;

			UNIT_LABEL_ALTITUDE = UNIT_ALTITUDE_FT;
			UNIT_LABEL_ALTIMETER = UNIT_ALTIMETER_FT_H;

		} else {

			// default is the metric measure system

			UNIT_VALUE_ALTITUDE = 1;

			UNIT_LABEL_ALTITUDE = UNIT_ALTITUDE_M;
			UNIT_LABEL_ALTIMETER = UNIT_ALTIMETER_M_H;
		}

		/*
		 * temperature
		 */
		if (prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE).equals(
				ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPTERATURE_F)) {

			// set imperial measure system

			UNIT_VALUE_TEMPERATURE = UNIT_FAHRENHEIT_ADD;
			UNIT_LABEL_TEMPERATURE = UNIT_TEMPERATURE_F;

		} else {

			// default is the metric measure system

			UNIT_VALUE_TEMPERATURE = 1;
			UNIT_LABEL_TEMPERATURE = UNIT_TEMPERATURE_C;
		}
	}

	/**
	 * create image tour type image from scratch
	 */
	private Image createTourTypeImage(final long typeId, final String colorId) {

		final Display display = Display.getCurrent();

		final Image tourTypeImage = new Image(display, TOUR_TYPE_IMAGE_WIDTH, TOUR_TYPE_IMAGE_HEIGHT);
		final GC gcImage = new GC(tourTypeImage);
		{
			drawTourTypeImage(typeId, gcImage);
		}
		gcImage.dispose();

		/*
		 * set transparency
		 */
		final ImageData imageData = tourTypeImage.getImageData();
		tourTypeImage.dispose();

		final int transparentPixel = imageData.getPixel(0, 0);
		imageData.transparentPixel = transparentPixel;

		final Image transparentImage = new Image(display, imageData);

		// keep image in cache
		_imageCache.put(colorId, transparentImage);

		return transparentImage;
	}

	/**
	 * dispose resources
	 */
	public void dispose() {
		disposeImages();
	}

	private void disposeImages() {

//		System.out.println("disposeImages:\t");
		for (final Image image : _imageCache.values()) {
			image.dispose();
		}

		_imageCache.clear();
		_imageCacheDescriptor.clear();
	}

	private void drawTourTypeImage(final long typeId, final GC gcImage) {

		if (typeId == TourDatabase.ENTITY_IS_NOT_SAVED) {
			// make the image invisible
			return;
		}

		final Display display = Display.getCurrent();
		final DrawingColors drawingColors = getTourTypeColors(display, typeId);

		final Color colorBright = drawingColors.colorBright;
		final Color colorDark = drawingColors.colorDark;
		final Color colorLine = drawingColors.colorLine;
		final Color colorTransparent = new Color(display, 0x01, 0x00, 0x00);

		gcImage.setBackground(colorTransparent);
		gcImage.fillRectangle(0, 0, TOUR_TYPE_IMAGE_WIDTH, TOUR_TYPE_IMAGE_HEIGHT);

		gcImage.setForeground(colorBright);
		gcImage.setBackground(colorDark);
		gcImage.fillGradientRectangle(4, 4, TOUR_TYPE_IMAGE_WIDTH - 8, TOUR_TYPE_IMAGE_HEIGHT - 8, false);

		gcImage.setForeground(colorLine);
		gcImage.drawRectangle(3, 3, TOUR_TYPE_IMAGE_WIDTH - 7, TOUR_TYPE_IMAGE_HEIGHT - 7);

		drawingColors.dispose();
		colorTransparent.dispose();
	}

	/**
	 * @param display
	 * @param graphColor
	 * @return return the color for the graph
	 */
	private DrawingColors getTourTypeColors(final Display display, final long tourTypeId) {

		final DrawingColors drawingColors = new DrawingColors();
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		TourType colorTourType = null;

		for (final TourType tourType : tourTypes) {
			if (tourType.getTypeId() == tourTypeId) {
				colorTourType = tourType;
				break;
			}
		}

		if (colorTourType == null || colorTourType.getTypeId() == TourDatabase.ENTITY_IS_NOT_SAVED) {

			// tour type was not found use default color

			drawingColors.colorBright = display.getSystemColor(SWT.COLOR_WHITE);
			drawingColors.colorDark = drawingColors.colorBright;
			drawingColors.colorLine = display.getSystemColor(SWT.COLOR_DARK_GRAY);

			// prevent disposing the colors
			drawingColors.mustBeDisposed = false;

		} else {

			drawingColors.colorBright = new Color(display, colorTourType.getRGBBright());
			drawingColors.colorDark = new Color(display, colorTourType.getRGBDark());
			drawingColors.colorLine = new Color(display, colorTourType.getRGBLine());
		}

		return drawingColors;
	}

	/**
	 * @param typeId
	 * @return Returns an image which represents the tour type
	 */
	public Image getTourTypeImage(final long typeId) {

		final String keyColorId = TOUR_TYPE_PREFIX + typeId;
		final Image existingImage = _imageCache.get(keyColorId);

		// check if image is available
		if (existingImage != null && existingImage.isDisposed() == false) {

			// check if the image is dirty
			if (_dirtyImages.size() == 0 || _dirtyImages.containsKey(keyColorId) == false) {

				// image is available and not dirty
				return existingImage;
			}
		}

		// create image for the tour type

		if (existingImage == null || existingImage.isDisposed()) {

			return createTourTypeImage(typeId, keyColorId);

		} else {

			// old tour type image is available and not disposed but needs to be updated

			return updateTourTypeImage(existingImage, typeId, keyColorId);
		}

	}

	/**
	 * The image descriptor is cached because the creation takes system resources and it's called
	 * very often
	 * 
	 * @param tourTypeId
	 *            Tour type id
	 * @return Returns image descriptor for the tour type id
	 */
	public ImageDescriptor getTourTypeImageDescriptor(final long tourTypeId) {

		final String keyColorId = TOUR_TYPE_PREFIX + tourTypeId;
		final ImageDescriptor existingDescriptor = _imageCacheDescriptor.get(keyColorId);

		if (existingDescriptor != null) {
			return existingDescriptor;
		}

		final Image tourTypeImage = getTourTypeImage(tourTypeId);
		final ImageDescriptor newImageDesc = ImageDescriptor.createFromImage(tourTypeImage);

		_imageCacheDescriptor.put(keyColorId, newImageDesc);

		return newImageDesc;
	}

	public String getTourTypeLabel(final long tourTypeId) {

		for (final TourType tourType : TourDatabase.getAllTourTypes()) {
			if (tourType.getTypeId() == tourTypeId) {
				return tourType.getName();
			}
		}

		return UI.EMPTY_STRING;
	}

	/**
	 * set dirty state for all tour type images, images cannot be disposed because they are
	 * displayed in the UI
	 */
	public void setTourTypeImagesDirty() {

		for (final String imageId : _imageCache.keySet()) {

			if (imageId.startsWith(TOUR_TYPE_PREFIX)) {
				_dirtyImages.put(imageId, true);
			}
		}

		_imageCacheDescriptor.clear();
	}

	/**
	 * updates an existing tour type image
	 * 
	 * @param existingImage
	 */
	private Image updateTourTypeImage(final Image existingImage, final long typeId, final String keyColorId) {

		final Display display = Display.getCurrent();

		final Image tourTypeImage = new Image(display, TOUR_TYPE_IMAGE_WIDTH, TOUR_TYPE_IMAGE_HEIGHT);
		GC gc = new GC(tourTypeImage);
		{
			drawTourTypeImage(typeId, gc);
		}
		gc.dispose();

		/*
		 * set transparency
		 */
		final ImageData imageData = tourTypeImage.getImageData();
		tourTypeImage.dispose();

		final int transparentPixel = imageData.getPixel(0, 0);
		imageData.transparentPixel = transparentPixel;

		/*
		 * update existing image
		 */
		final Image transparentImage = new Image(display, imageData);
		gc = new GC(existingImage);
		{
			gc.drawImage(transparentImage, 0, 0);
		}
		gc.dispose();
		transparentImage.dispose();

		_dirtyImages.remove(keyColorId);

		return existingImage;
	}
}
