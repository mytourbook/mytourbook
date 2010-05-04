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
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class UI {

//	long startTime = System.currentTimeMillis();

//	long endTime = System.currentTimeMillis();
//	System.out.println("Execution time : " + (endTime - startTime) + " ms");

	public static final String						EMPTY_STRING					= "";										//$NON-NLS-1$
	public static final String						SPACE							= " ";										//$NON-NLS-1$
	public static final String						COLON_SPACE						= ": ";									//$NON-NLS-1$
	public static final String						UNDERSCORE						= "_";										//$NON-NLS-1$
	public static final String						DASH							= "-";										//$NON-NLS-1$
	public static final String						DASH_WITH_SPACE					= " - ";									//$NON-NLS-1$
	public static final String						DASH_WITH_DOUBLE_SPACE			= "   -   ";								//$NON-NLS-1$
	public static final String						SLASH_WITH_SPACE				= " / ";									//$NON-NLS-1$
	public static final String						EMPTY_STRING_FORMAT				= "%s";									//$NON-NLS-1$
	public static final char						TAB								= '\t';
	public static final char						DOT								= '.';

	/**
	 * contains a new line
	 */
	public static final String						NEW_LINE						= "\n";									//$NON-NLS-1$
	/**
	 * contains 2 new lines
	 */
	public static final String						NEW_LINE2						= "\n\n";									//$NON-NLS-1$

	public static final String						SYSTEM_NEW_LINE					= System
																							.getProperty("line.separator");	//$NON-NLS-1$

	public static final String						IS_NOT_INITIALIZED				= "IS NOT INITIALIZED";					//$NON-NLS-1$

	public static final String						VIEW_COLOR_CATEGORY				= "view.color.category";					//$NON-NLS-1$
	public static final String						VIEW_COLOR_TITLE				= "view.color.title";						//$NON-NLS-1$
	public static final String						VIEW_COLOR_SUB					= "view.color.sub";						//$NON-NLS-1$
	public static final String						VIEW_COLOR_SUB_SUB				= "view.color.sub-sub";					//$NON-NLS-1$
	public static final String						VIEW_COLOR_TOUR					= "view.color.tour";						//$NON-NLS-1$
	public static final String						VIEW_COLOR_BG_SEGMENTER_UP		= "view.colorBG.segmenterUp";				//$NON-NLS-1$
	public static final String						VIEW_COLOR_BG_SEGMENTER_DOWN	= "view.colorBG.segmenterDown";			//$NON-NLS-1$

	public static final int							DEFAULT_FIELD_WIDTH				= 40;
	public static final int							FORM_FIRST_COLUMN_INDENT		= 16;

	public static final String						UTF_8							= "UTF-8";									//$NON-NLS-1$

	/*
	 * labels for the different measurement systems
	 */
	private static final String						UNIT_ALTITUDE_M					= "m";										//$NON-NLS-1$
	public static final String						UNIT_DISTANCE_KM				= "km";									//$NON-NLS-1$
	private static final String						UNIT_SPEED_KM_H					= "km/h";									//$NON-NLS-1$
	private static final String						UNIT_FAHRENHEIT_C				= "\u00B0C";								//$NON-NLS-1$
	private static final String						UNIT_ALTIMETER_M_H				= "m/h";									//$NON-NLS-1$
	private static final String						UNIT_PACE_MIN_P_KM				= "min/km";								//$NON-NLS-1$

	private static final String						UNIT_ALTITUDE_FT				= "ft";									//$NON-NLS-1$
	public static final String						UNIT_DISTANCE_MI				= "mi";									//$NON-NLS-1$
	private static final String						UNIT_SPEED_MPH					= "mph";									//$NON-NLS-1$
	private static final String						UNIT_FAHRENHEIT_F				= "\u00B0F";								//$NON-NLS-1$
	private static final String						UNIT_ALTIMETER_FT_H				= "ft/h";									//$NON-NLS-1$
	private static final String						UNIT_PACE_MIN_P_MILE			= "min/mi";								//$NON-NLS-1$

	public static final String						SYMBOL_AVERAGE					= "\u00f8";								//$NON-NLS-1$
	public static final String						SYMBOL_AVERAGE_WITH_SPACE		= "\u00f8 ";								//$NON-NLS-1$
	public static final String						SYMBOL_DIFFERENCE_WITH_SPACE	= "\u0394 ";								//$NON-NLS-1$
	public static final String						SYMBOL_SUM_WITH_SPACE			= "\u2211 ";								//$NON-NLS-1$
	public static final String						SYMBOL_DOUBLE_HORIZONTAL		= "\u2550";								//$NON-NLS-1$
	public static final String						SYMBOL_DOUBLE_VERTICAL			= "\u2551";								//$NON-NLS-1$
	public static final String						SYMBOL_WIND_WITH_SPACE			= "W ";									//$NON-NLS-1$

	public static final float						UNIT_MILE						= 1.609344f;
	public static final float						UNIT_FOOT						= 0.3048f;

	/**
	 * contains the system of measurement value for distances relative to the metric system, the
	 * metric systemis <code>1</code>
	 */
	public static float								UNIT_VALUE_DISTANCE				= 1;

	/**
	 * contains the system of measurement value for altitudes relative to the metric system, the
	 * metric system is <code>1</code>
	 */
	public static float								UNIT_VALUE_ALTITUDE				= 1;

	/**
	 * contains the system of measurement value for the temperature, is set to <code>1</code> for
	 * the metric system
	 */
	public static float								UNIT_VALUE_TEMPERATURE			= 1;

	// (Celcius * 9/5) + 32 = Fahrenheit
	public static final float						UNIT_FAHRENHEIT_MULTI			= 1.8f;
	public static final float						UNIT_FAHRENHEIT_ADD				= 32;

	public static final String						UNIT_LABEL_TIME					= "h";										//$NON-NLS-1$

	/**
	 * contains the unit label in the currenty measurement system for the distance values
	 */
	public static String							UNIT_LABEL_DISTANCE;
	public static String							UNIT_LABEL_ALTITUDE;
	public static String							UNIT_LABEL_ALTIMETER;
	public static String							UNIT_LABEL_TEMPERATURE;
	public static String							UNIT_LABEL_SPEED;
	public static String							UNIT_LABEL_PACE;
	public static String							UNIT_LABEL_DIRECTION			= "\u00B0";								//$NON-NLS-1$

	private static final String						TOUR_TYPE_PREFIX				= "tourType";								//$NON-NLS-1$
//	private static final String						WEATHER_CLOUDS_PREFIX			= "weatherClouds-";						//$NON-NLS-1$

	public final static ImageRegistry				IMAGE_REGISTRY;

	/*
	 * image keys for images which are stored in the image registry
	 */
	public static final String						IMAGE_EMPTY_16					= "_empty16";								//$NON-NLS-1$

	public static final String						IMAGE_TOUR_TYPE_FILTER			= "tourType-filter";						//$NON-NLS-1$
	public static final String						IMAGE_TOUR_TYPE_FILTER_SYSTEM	= "tourType-filter-system";				//$NON-NLS-1$

	public static final String						IMAGE_WEATHER_SUNNY				= "weather-sunny";							//$NON-NLS-1$
	public static final String						IMAGE_WEATHER_CLOUDY			= "weather-cloudy";						//$NON-NLS-1$
	public static final String						IMAGE_WEATHER_CLOUDS			= "weather-clouds";						//$NON-NLS-1$
	public static final String						IMAGE_WEATHER_LIGHTNING			= "weather-lightning";						//$NON-NLS-1$
	public static final String						IMAGE_WEATHER_RAIN				= "weather-rain";							//$NON-NLS-1$
	public static final String						IMAGE_WEATHER_SNOW				= "weather-snow";							//$NON-NLS-1$

	private static final int						TOUR_TYPE_IMAGE_WIDTH			= 16;
	private static final int						TOUR_TYPE_IMAGE_HEIGHT			= 16;

	private static UI								instance;

	public static final DateFormat					TimeFormatterShort				= DateFormat
																							.getTimeInstance(DateFormat.SHORT);
	public static final DateFormat					DateFormatterShort				= DateFormat
																							.getDateInstance(DateFormat.SHORT);
	public static final DateFormat					DateFormatterLong				= DateFormat
																							.getDateInstance(DateFormat.LONG);
	public static final DateFormat					DateFormatterFull				= DateFormat
																							.getDateInstance(DateFormat.FULL);
	public static final SimpleDateFormat			MonthFormatter					= new SimpleDateFormat("MMM");				//$NON-NLS-1$
	public static final SimpleDateFormat			WeekDayFormatter				= new SimpleDateFormat("EEEE");			//$NON-NLS-1$

	private static DateFormat						fDateFormatterShort;
	private static DateFormat						fTimeFormatterShort;

	private static StringBuilder					fFormatterSB					= new StringBuilder();
	private static Formatter						fFormatter						= new Formatter(fFormatterSB);

	public static Styler							TAG_STYLER;
	public static Styler							TAG_CATEGORY_STYLER;
	public static Styler							TAG_SUB_STYLER;

	private final static HashMap<String, Image>		_imageCache						= new HashMap<String, Image>();
	private final static HashMap<String, Boolean>	_dirtyImages					= new HashMap<String, Boolean>();

	static {

		updateUnits();
		setViewColorsFromPrefStore();

		/*
		 * load images into the image registry
		 */
		IMAGE_REGISTRY = TourbookPlugin.getDefault().getImageRegistry();

		IMAGE_REGISTRY.put(IMAGE_EMPTY_16, TourbookPlugin.getImageDescriptor(Messages.Image___Empty16));

		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER, //
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter));
		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER_SYSTEM,//
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter_system));

		IMAGE_REGISTRY.put(IMAGE_WEATHER_SUNNY, //
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_sunny));
		IMAGE_REGISTRY.put(IMAGE_WEATHER_CLOUDY, //
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_cloudy));
		IMAGE_REGISTRY.put(IMAGE_WEATHER_CLOUDS, //
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_clouds));
		IMAGE_REGISTRY.put(IMAGE_WEATHER_LIGHTNING,//
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_lightning));
		IMAGE_REGISTRY.put(IMAGE_WEATHER_RAIN,//
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_rain));
		IMAGE_REGISTRY.put(IMAGE_WEATHER_SNOW,//
				TourbookPlugin.getImageDescriptor(Messages.Image__weather_snow));

		/*
		 * set styler
		 */
		TAG_CATEGORY_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_CATEGORY, null);
		TAG_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_TITLE, null);
		TAG_SUB_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_SUB, null);
	}

	private UI() {}

	public static void adjustSpinnerValueOnMouseScroll(final MouseEvent event) {

		// accelerate with Ctrl + Shift key
		int accelerator = (event.stateMask & SWT.CONTROL) != 0 ? 10 : 1;
		accelerator *= (event.stateMask & SWT.SHIFT) != 0 ? 5 : 1;

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
	 * Compares two {@link TourData}
	 * 
	 * @param tourData1
	 * @param tourData2
	 * @return Returns <code>true</code> when they are the same, otherwise this is an internal error
	 * @throws MyTourbookException
	 *             throws this exception when {@link TourData} are corrupted
	 */
	public static boolean checkTourData(final TourData tourData1, final TourData tourData2) throws MyTourbookException {

		if (tourData1 == null || tourData2 == null) {
			return true;
		}

		if (tourData1.getTourId().longValue() == tourData2.getTourId().longValue() && tourData1 != tourData2) {

			final StringBuilder sb = new StringBuilder()//
					.append("ERROR: ") //$NON-NLS-1$
					.append("The internal structure of the application is out of synch.") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("You can solve the problem by:") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("- restarting the application") //$NON-NLS-1$
					.append(UI.NEW_LINE)
					.append("- close the tour editor in all perspectives") //$NON-NLS-1$
					.append(UI.NEW_LINE)
					.append("- save/revert tour and select another tour") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append(UI.NEW_LINE)
					.append("The tour editor contains the selected tour, but the data are different.") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("Tour in Editor:") //$NON-NLS-1$
					.append(tourData2.toStringWithHash())
					.append(UI.NEW_LINE)
					.append("Selected Tour:") //$NON-NLS-1$
					.append(tourData1.toStringWithHash())
					.append(UI.NEW_LINE2)
					.append(UI.NEW_LINE)
					.append("You should also inform the author of the application how this error occured. ") //$NON-NLS-1$
					.append(
							"However it isn't very easy to find out, what actions are exactly done, before this error occured. ") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("These actions must be reproducable otherwise the bug cannot be identified."); //$NON-NLS-1$

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error: Out of Synch", sb.toString()); //$NON-NLS-1$

			throw new MyTourbookException(sb.toString());
		}

		return true;
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
		}

		return containedTourId;
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

		fFormatterSB.setLength(0);

		return fFormatter.format(Messages.Format_hhmm, (time / 3600), ((time % 3600) / 60));
	}

	/**
	 * ignore hours when they are 0
	 * 
	 * @param time
	 * @return
	 */
	public static String format_hh_mm_ss(final long time) {

		fFormatterSB.setLength(0);

		if (time >= 3600) {

			// display hours

			return fFormatter.format(//
					Messages.Format_hhmmss,
					(time / 3600),
					((time % 3600) / 60),
					((time % 3600) % 60)).toString();

		} else {

			// ignore hours

			return fFormatter.format(//
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

		fFormatterSB.setLength(0);
		// display hours

		return fFormatter.format(//
				Messages.Format_hhmmss,
				(time / 3600),
				((time % 3600) / 60),
				((time % 3600) % 60)).toString();
	}

	public static String format_mm_ss(final long time) {

		fFormatterSB.setLength(0);

		if (time < 0) {
			fFormatterSB.append(UI.DASH);
		}

		final long timeAbs = time < 0 ? 0 - time : time;

		return fFormatter.format(Messages.Format_hhmm, (timeAbs / 60), (timeAbs % 60)).toString();
	}

	public static String format_yyyymmdd_hhmmss(final int year,
												final int month,
												final int day,
												final int hour,
												final int minute,
												final int second) {

		fFormatterSB.setLength(0);

		return fFormatter.format(//
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

		fFormatterSB.setLength(0);

		return fFormatter.format(//
				Messages.Format_yyyymmdd_hhmmss,
				tourData.getStartYear(),
				tourData.getStartMonth(),
				tourData.getStartDay(),
				tourData.getStartHour(),
				tourData.getStartMinute(),
				tourData.getStartSecond())//
				.toString();
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

		if (fDateFormatterShort == null) {

			final DateFormat dateInstance = DateFormat.getDateInstance(DateFormat.SHORT);
			if (dateInstance instanceof SimpleDateFormat) {

				final SimpleDateFormat sdf = (SimpleDateFormat) (fDateFormatterShort = dateInstance);

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

		return fDateFormatterShort;
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

		if (fTimeFormatterShort == null) {

			final DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.SHORT);
			if (timeInstance instanceof SimpleDateFormat) {

				final SimpleDateFormat sdf = (SimpleDateFormat) (fTimeFormatterShort = timeInstance);

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

		return fTimeFormatterShort;
	}

	public static UI getInstance() {

		if (instance == null) {
			instance = new UI();
		}

		return instance;
	}

	private static String getSQLExceptionText(final SQLException e) {

		final StringBuilder sb = new StringBuilder()//
				.append("SQLException") //$NON-NLS-1$
				.append(UI.NEW_LINE2)
				.append("SQLState: " + (e).getSQLState()) //$NON-NLS-1$
				.append(UI.NEW_LINE)
				.append("Severity: " + (e).getErrorCode()) //$NON-NLS-1$
				.append(UI.NEW_LINE)
				.append("Message: " + (e).getMessage()) //$NON-NLS-1$
				.append(UI.NEW_LINE);

		return sb.toString();
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

	/**
	 * Checks if a tour in the {@link TourDataEditorView} is modified and shows the editor when it's
	 * modified. A message dialog informs the user about the modified tour and the requested actions
	 * cannot be done.
	 * 
	 * @return Returns <code>true</code> when the tour is modified in the {@link TourDataEditorView}
	 */
	public static boolean isTourEditorModified() {

		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
		if (tourDataEditor != null && tourDataEditor.isDirty()) {

			openTourEditor(true);

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.dialog_is_tour_editor_modified_title,
					Messages.dialog_is_tour_editor_modified_message);

			return true;
		}

		return false;
	}

	/**
	 * Opens the menu for a control aligned below the control on the left side
	 * 
	 * @param control
	 *            Controls which menu is opened
	 */
	public static void openControlMenu(final Control control) {

		final Rectangle rect = control.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = control.getParent().toDisplay(pt);

		final Menu menu = control.getMenu();

		if (menu != null && menu.isDisposed() == false) {
			menu.setLocation(pt.x, pt.y);
			menu.setVisible(true);
		}
	}

	public static TourDataEditorView openTourEditor(final boolean isActive) {

		IViewPart viewPart = null;

		TourDataEditorView tourEditor = null;

		try {

			final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			final String viewId = TourDataEditorView.ID;
			viewPart = page.showView(viewId, null, IWorkbenchPage.VIEW_VISIBLE);

			if (viewPart instanceof TourDataEditorView) {
				tourEditor = (TourDataEditorView) viewPart;

				if (isActive) {

					page.showView(viewId, null, IWorkbenchPage.VIEW_ACTIVATE);

				} else if (page.isPartVisible(viewPart) == false || isActive) {

					page.bringToTop(viewPart);
				}
// this does not restore the part when it's in a fast view
//
//			final IWorkbenchPartReference partRef = page.getReference(viewPart);
//			final int partState = page.getPartState(partRef);
//			page.setPartState(partRef, IWorkbenchPage.STATE_MAXIMIZED);
//			page.setPartState(partRef, IWorkbenchPage.STATE_RESTORED);

			}

		} catch (final PartInitException e) {
			e.printStackTrace();
		}

		return tourEditor;
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
	public static void restoreSashWeight(final SashForm sash,
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
	 * set the tag colors in the JFace color registry from the pref store
	 * 
	 * @param prefs
	 */
	public static void setViewColorsFromPrefStore() {

		/*
		 * set colors
		 */
		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		colorRegistry.put(VIEW_COLOR_CATEGORY, //
				PreferenceConverter.getColor(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_CATEGORY));
		colorRegistry.put(VIEW_COLOR_TITLE, //
				PreferenceConverter.getColor(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_TITLE));
		colorRegistry.put(VIEW_COLOR_SUB, //
				PreferenceConverter.getColor(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB));
		colorRegistry.put(VIEW_COLOR_SUB_SUB, //
				PreferenceConverter.getColor(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB_SUB));
		colorRegistry.put(VIEW_COLOR_TOUR, //
				PreferenceConverter.getColor(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_TOUR));

		colorRegistry.put(VIEW_COLOR_BG_SEGMENTER_UP, //
				PreferenceConverter.getColor(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_UP));
		colorRegistry.put(VIEW_COLOR_BG_SEGMENTER_DOWN, //
				PreferenceConverter.getColor(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_DOWN));
	}

	public static GridData setWidth(final Control control, final int width) {
		final GridData gd = new GridData();
		gd.widthHint = width;
		control.setLayoutData(gd);
		return gd;
	}

	public static void showSQLException(SQLException e) {

		while (e != null) {

			final String sqlExceptionText = getSQLExceptionText(e);

			System.out.println(sqlExceptionText);
			e.printStackTrace();

			if (e instanceof SQLException) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "SQL Error", //$NON-NLS-1$
						sqlExceptionText);
			}

			e = e.getNextException();
		}

	}

	public static void showSQLException(final SQLException e, final String sqlStatement) {

		MessageDialog.openError(Display.getCurrent().getActiveShell(), "SQL Error", "SQL statement: "//$NON-NLS-1$ //$NON-NLS-2$
				+ UI.NEW_LINE2
				+ sqlStatement
				+ getSQLExceptionText(e));
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

		tourTagLabel.pack(true);
	}

	public static void updateUITourType(final TourType tourType, final CLabel lblTourType, final boolean isTextDisplayed) {

		// tour type
		if (tourType == null) {
			lblTourType.setText(UI.EMPTY_STRING);
			lblTourType.setImage(null);
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

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		/*
		 * distance
		 */
		if (prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE).equals(
				ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI)) {

			// set imperial measure system

			UNIT_VALUE_DISTANCE = UNIT_MILE;

			UNIT_LABEL_DISTANCE = UNIT_DISTANCE_MI;
			UNIT_LABEL_SPEED = UNIT_SPEED_MPH;
			UNIT_LABEL_PACE = UNIT_PACE_MIN_P_MILE;

		} else {

			// default is the metric measure system

			UNIT_VALUE_DISTANCE = 1;

			UNIT_LABEL_DISTANCE = UNIT_DISTANCE_KM;
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
			UNIT_LABEL_TEMPERATURE = UNIT_FAHRENHEIT_F;

		} else {

			// default is the metric measure system

			UNIT_VALUE_TEMPERATURE = 1;
			UNIT_LABEL_TEMPERATURE = UNIT_FAHRENHEIT_C;
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
	}

	private void drawTourTypeImage(final long typeId, final GC gcImage) {

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
			drawingColors.colorDark = display.getSystemColor(SWT.COLOR_WHITE);
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
