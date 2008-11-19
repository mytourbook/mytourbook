/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
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

	public static final String						EMPTY_STRING					= "";											//$NON-NLS-1$
	public static final String						DASH_WITH_SPACE					= " - ";										//$NON-NLS-1$
	public static final String						EMPTY_STRING_FORMAT				= "%s";										//$NON-NLS-1$

	/**
	 * contains a new line string
	 */
	public static final String						NEW_LINE						= "\n";										//$NON-NLS-1$
	public static final String						NEW_LINE2						= "\n\n";										//$NON-NLS-1$

	public static final String						SYSTEM_NEW_LINE					= System.getProperty("line.separator");		//$NON-NLS-1$

	public static final String						IS_NOT_INITIALIZED				= "IS NOT INITIALIZED";						//$NON-NLS-1$

	public static final String						VIEW_COLOR_CATEGORY				= "view.color.category";						//$NON-NLS-1$
	public static final String						VIEW_COLOR_TITLE				= "view.color.title";							//$NON-NLS-1$
	public static final String						VIEW_COLOR_SUB					= "view.color.sub";							//$NON-NLS-1$
	public static final String						VIEW_COLOR_SUB_SUB				= "view.color.sub-sub";						//$NON-NLS-1$
	public static final String						VIEW_COLOR_TOUR					= "view.color.tour";							//$NON-NLS-1$

	public static final int							DEFAULT_FIELD_WIDTH				= 40;

	/*
	 * labels for the different measurement systems
	 */
	private static final String						UNIT_ALTITUDE_M					= "m";											//$NON-NLS-1$
	public static final String						UNIT_DISTANCE_KM				= "km";										//$NON-NLS-1$
	private static final String						UNIT_SPEED_KM_H					= "km/h";										//$NON-NLS-1$
	private static final String						UNIT_FAHRENHEIT_C				= "\u00B0C";									//$NON-NLS-1$
	private static final String						UNIT_ALTIMETER_M_H				= "m/h";										//$NON-NLS-1$
	private static final String						UNIT_PACE_MIN_P_KM				= "min/km";									//$NON-NLS-1$

	private static final String						UNIT_ALTITUDE_FT				= "ft";										//$NON-NLS-1$
	public static final String						UNIT_DISTANCE_MI				= "mi";										//$NON-NLS-1$
	private static final String						UNIT_SPEED_MPH					= "mph";										//$NON-NLS-1$
	private static final String						UNIT_FAHRENHEIT_F				= "\u00B0F";									//$NON-NLS-1$
	private static final String						UNIT_ALTIMETER_FT_H				= "ft/h";										//$NON-NLS-1$
	private static final String						UNIT_PACE_MIN_P_MILE			= "min/mi";									//$NON-NLS-1$

	public static final String						SYMBOL_AVERAGE					= "\u00D8";									//$NON-NLS-1$
	public static final String						SYMBOL_AVERAGE_WITH_SPACE		= "\u00D8 ";									//$NON-NLS-1$

	public static final float						UNIT_MILE						= 1.609344f;
	private static final float						UNIT_FOOT						= 0.3048f;

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

	public static final String						UNIT_LABEL_TIME					= "h";											//$NON-NLS-1$

	/**
	 * contains the unit label in the currenty measurement system for the distance values
	 */
	public static String							UNIT_LABEL_DISTANCE;
	public static String							UNIT_LABEL_ALTITUDE;
	public static String							UNIT_LABEL_ALTIMETER;
	public static String							UNIT_LABEL_TEMPERATURE;
	public static String							UNIT_LABEL_SPEED;
	public static String							UNIT_LABEL_PACE;

	private static final String						TOUR_TYPE_PREFIX				= "tourType";									//$NON-NLS-1$

	public final static ImageRegistry				IMAGE_REGISTRY;

	public static final String						IMAGE_TOUR_TYPE_FILTER			= "tourType-filter";							//$NON-NLS-1$
	public static final String						IMAGE_TOUR_TYPE_FILTER_SYSTEM	= "tourType-filter-system";					//$NON-NLS-1$

	private static final int						TOUR_TYPE_IMAGE_WIDTH			= 16;
	private static final int						TOUR_TYPE_IMAGE_HEIGHT			= 16;

	private static UI								instance;

	public static final DateFormat					TimeFormatterShort				= DateFormat.getTimeInstance(DateFormat.SHORT);
	public static final DateFormat					DateFormatterShort				= DateFormat.getDateInstance(DateFormat.SHORT);
	public static final DateFormat					DateFormatterLong				= DateFormat.getDateInstance(DateFormat.LONG);
	public static final DateFormat					DateFormatterFull				= DateFormat.getDateInstance(DateFormat.FULL);
	public static final SimpleDateFormat			MonthFormatter					= new SimpleDateFormat("MMM");					//$NON-NLS-1$
	public static final SimpleDateFormat			WeekDayFormatter				= new SimpleDateFormat("EEEE");				//$NON-NLS-1$

	public static Styler							TAG_STYLER;
	public static Styler							TAG_CATEGORY_STYLER;
	public static Styler							TAG_SUB_STYLER;

	private final static HashMap<String, Image>		fImageCache						= new HashMap<String, Image>();
	private final static HashMap<String, Boolean>	fDirtyImages					= new HashMap<String, Boolean>();

	static {

		updateUnits();
		setTagColorsFromPrefStore();

		/*
		 * load images into the image registry
		 */
		IMAGE_REGISTRY = TourbookPlugin.getDefault().getImageRegistry();

		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER,
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter));
		IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER_SYSTEM,
				TourbookPlugin.getImageDescriptor(Messages.Image__undo_tour_type_filter_system));

		/*
		 * set styler
		 */
		TAG_CATEGORY_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_CATEGORY, null);
		TAG_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_TITLE, null);
		TAG_SUB_STYLER = StyledString.createColorRegistryStyler(VIEW_COLOR_SUB, null);
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
					.append("However it isn't very easy to find out, what actions are exactly done, before this error occured. ") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("These actions must be reproducable otherwise the bug cannot be identified."); //$NON-NLS-1$

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error: Out of Synch", sb.toString()); //$NON-NLS-1$

			throw new MyTourbookException(sb.toString());
		}

		return true;
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

	public static final String formatSeconds(final long value) {

		return new Formatter().format(Messages.Format_hhmmss,
				(value / 3600),
				((value % 3600) / 60),
				((value % 3600) % 60)).toString();

	}

	public static ColumnPixelData getColumnPixelWidth(final PixelConverter pixelConverter, final int width) {
		return new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(width), false);
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

	/**
	 * Checks if a tour in the {@link TourDataEditorView} is modified and shows the editor when it's
	 * modified
	 * 
	 * @return Returns <code>true</code> when the tour is modified in the {@link TourDataEditorView}
	 */
	public static boolean isTourEditorModified() {

		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
		if (tourDataEditor != null && tourDataEditor.isDirty()) {

			openTourEditor(true);

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
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

	public static void openTourEditor(final boolean isActive) {

		try {
			final IWorkbenchWindow wbWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			final IWorkbenchPage page = wbWindow.getActivePage();

			final String viewId = TourDataEditorView.ID;

			final IViewPart viewPart = page.showView(viewId, null, IWorkbenchPage.VIEW_VISIBLE);

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

		} catch (final PartInitException e) {
			e.printStackTrace();
		}
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
	public static void setTagColorsFromPrefStore() {

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
	}

	public static GridData setWidth(final Control control, final int width) {
		final GridData gd = new GridData();
		gd.widthHint = width;
		control.setLayoutData(gd);
		return gd;
	}

	public static void showSQLException(SQLException e) {
		while (e != null) {
			System.out.println("\n---SQLException Caught---\n"); //$NON-NLS-1$
			System.out.println("SQLState: " + (e).getSQLState()); //$NON-NLS-1$
			System.out.println("Severity: " + (e).getErrorCode()); //$NON-NLS-1$
			System.out.println("Message: " + (e).getMessage()); //$NON-NLS-1$
			e.printStackTrace();
			e = e.getNextException();
		}
	}

	public static void updateUITags(final TourData tourData, final Label tourTagLabel) {

		// tour tags
		final Set<TourTag> tourTags = tourData.getTourTags();

		if (tourTags == null || tourTags.size() == 0) {
			tourTagLabel.setText(UI.EMPTY_STRING);
		} else {

			// sort tour tags by name
			final ArrayList<TourTag> tourTagList = new ArrayList<TourTag>(tourTags);
			Collections.sort(tourTagList, new Comparator<TourTag>() {
				public int compare(final TourTag tt1, final TourTag tt2) {
					return tt1.getTagName().compareTo(tt2.getTagName());
				}
			});

			final StringBuilder sb = new StringBuilder();
			int index = 0;
			for (final TourTag tourTag : tourTagList) {

				if (index > 0) {
					sb.append(", "); //$NON-NLS-1$
				}

				sb.append(tourTag.getTagName());

				index++;
			}
			tourTagLabel.setText(sb.toString());
			tourTagLabel.setToolTipText(sb.toString());
		}
		tourTagLabel.pack(true);
	}

	public static void updateUITourType(final TourData tourData, final CLabel lblTourType) {

		// tour type
		final TourType tourType = tourData.getTourType();
		if (tourType == null) {
			lblTourType.setText(UI.EMPTY_STRING);
			lblTourType.setImage(null);
		} else {
			lblTourType.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
			lblTourType.setText(tourType.getName());
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
		if (prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE)
				.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI)) {

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
		if (prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE)
				.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_FOOT)) {

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
		if (prefStore.getString(ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE)
				.equals(ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPTERATURE_F)) {

			// set imperial measure system

			UNIT_VALUE_TEMPERATURE = UNIT_FAHRENHEIT_ADD;
			UNIT_LABEL_TEMPERATURE = UNIT_FAHRENHEIT_F;

		} else {

			// default is the metric measure system

			UNIT_VALUE_TEMPERATURE = 1;
			UNIT_LABEL_TEMPERATURE = UNIT_FAHRENHEIT_C;
		}
	}

	public static VerifyListener verifyListenerTypeLong() {

		return new VerifyListener() {
			public void verifyText(final VerifyEvent e) {
				if (e.text.equals("")) { //$NON-NLS-1$
					return;
				}
				try {
					Long.parseLong(e.text);
				} catch (final NumberFormatException e1) {
					e.doit = false;
				}
			}
		};
	}

	private UI() {}

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
		fImageCache.put(colorId, transparentImage);

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
		for (final Image image : fImageCache.values()) {
			image.dispose();
		}
		fImageCache.clear();
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
		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();

		TourType colorTourType = null;

		for (final TourType tourType : tourTypes) {
			if (tourType.getTypeId() == tourTypeId) {
				colorTourType = tourType;
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
		final Image existingImage = fImageCache.get(keyColorId);

		// check if image is available
		if (existingImage != null && existingImage.isDisposed() == false) {

			// check if the image is dirty

			if (fDirtyImages.size() == 0 || fDirtyImages.containsKey(keyColorId) == false) {

				// image is available and not dirty
				return existingImage;
			}
		}

		// create image for the tour type

		if (existingImage == null || existingImage.isDisposed()) {

			return createTourTypeImage(typeId, keyColorId);

		} else {

			// old tour type image is available and not disposed but is dirty, update the image

			return updateTourTypeImage(existingImage, typeId, keyColorId);
		}

	}

	/**
	 * dispose all tour type images
	 */
	public void setTourTypeImagesDirty() {

		for (final Iterator<String> iterator = fImageCache.keySet().iterator(); iterator.hasNext();) {

			final String imageId = iterator.next();

			if (imageId.startsWith(TOUR_TYPE_PREFIX)) {

				fDirtyImages.put(imageId, true);

//				fImageCache.get(imageId).dispose();
//				iterator.remove();

//				System.out.println("setTourTypeImagesDirty:\t" + imageId);
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

		fDirtyImages.remove(keyColorId);

//		// keep image in cache
//		fImageCache.put(keyColorId, transparentImage);

		return existingImage;
	}
}
