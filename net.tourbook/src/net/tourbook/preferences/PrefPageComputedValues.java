/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageComputedValues extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	STATE_COMPUTED_VALUE_MIN_ALTITUDE	= "computedValue.minAltitude";						//$NON-NLS-1$
	private static final String	STATE_COMPUTED_VALUE_SELECTED_TAB	= "computedValue.selectedTab";						//$NON-NLS-1$

	public static final int[]	ALTITUDE_MINIMUM					= new int[] {
			1,
			2,
			3,
			4,
			5,
			6,
			7,
			8,
			9,
			10,
			12,
			14,
			16,
			18,
			20,
			25,
			30,
			35,
			40,
			45,
			50,
			60,
			70,
			80,
			90,
			100,
			120,
			140,
			160,
			180,
			200,
			250,
			300,
			350,
			400,
			450,
			500,
			600,
			700,
			800,
			900,
			1000													};

	public static final int		DEFAULT_MIN_ALTITUDE_INDEX			= 4;
	public static final int		DEFAULT_MIN_ALTITUDE				= ALTITUDE_MINIMUM[DEFAULT_MIN_ALTITUDE_INDEX];

	private IPreferenceStore	fPrefStore							= TourbookPlugin.getDefault().getPreferenceStore();
	private NumberFormat		fNf									= NumberFormat.getNumberInstance();

	private Combo				fCboMinAltitude;
	private Spinner				fSpinnerMinTime;

	private TabFolder			fTabFolder;

	@Override
	protected Control createContents(final Composite parent) {

		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		fTabFolder = new TabFolder(parent, SWT.TOP);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fTabFolder);
		{
			final TabItem tabElevation = new TabItem(fTabFolder, SWT.NONE);
			tabElevation.setControl(createUIElevationGain(fTabFolder));
			tabElevation.setText(Messages.compute_tourValueElevation_group_computeTourAltitude);

			final TabItem tabSpeed = new TabItem(fTabFolder, SWT.NONE);
			tabSpeed.setControl(createUISpeed(fTabFolder));
			tabSpeed.setText(Messages.compute_tourValueSpeed_group_speed);

			/**
			 * 4.8.2009 week no/year is currently disabled because a new field in the db is required
			 * which holds the year of the week<br>
			 * <br>
			 * all plugins must be adjusted which set's the week number of a tour
			 */
//			createUIWeek(container);
		}

		return fTabFolder;
	}

	private Control createUIElevationGain(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(3).applyTo(container);
		{
			// label: min alti diff
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.compute_tourValueElevation_label_minAltiDifference);

			// combo: min altitude
			fCboMinAltitude = new Combo(container, SWT.READ_ONLY);
			fCboMinAltitude.setVisibleItemCount(20);
			for (final int minAlti : PrefPageComputedValues.ALTITUDE_MINIMUM) {
				fCboMinAltitude.add(Integer.toString((int) (minAlti / UI.UNIT_VALUE_ALTITUDE)));
			}

			// label: unit
			label = new Label(container, SWT.NONE);
			label.setText(UI.UNIT_LABEL_ALTITUDE);

			// label: description
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(300, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.compute_tourValueElevation_label_description);

			final Composite btnContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(btnContainer);
			GridLayoutFactory.fillDefaults().applyTo(btnContainer);
			{
				// button: compute computed values
				final Button btnComputValues = new Button(btnContainer, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(btnComputValues);
				btnComputValues.setText(Messages.compute_tourValueElevation_button_computeValues);
				btnComputValues.setToolTipText(Messages.compute_tourValueElevation_button_computeValues_tooltip);
				btnComputValues.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onComputeElevationGainValues();
					}
				});
			}
		}
		return container;
	}

	private Control createUISpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(3).applyTo(container);
		{
			// label: min alti diff
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.compute_tourValueSpeed_label_speedTimeSlice);

			// combo: min altitude
			fSpinnerMinTime = new Spinner(container, SWT.BORDER);
			fSpinnerMinTime.setMaximum(1000);
			fSpinnerMinTime.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeProperty();
				}
			});
			fSpinnerMinTime.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeProperty();
				}
			});

			// label: unit
			label = new Label(container, SWT.NONE);
			label.setText(Messages.app_unit_seconds);

			// label: description
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(300, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.compute_tourValueSpeed_label_description);

			final Composite btnContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(btnContainer);
			GridLayoutFactory.fillDefaults().applyTo(btnContainer);
			{
				// button: compute computed values
				final Button btnComputValues = new Button(btnContainer, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(btnComputValues);
				btnComputValues.setText(Messages.compute_tourValueSpeed_button_computeValues);
				btnComputValues.setToolTipText(Messages.compute_tourValueSpeed_button_computeValues_tooltip);
				btnComputValues.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onComputeSpeedValues();
					}
				});
			}
		}

		return container;
	}

	private void fireModifyEvent() {

		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {
		saveUIState();
		return super.okToLeave();
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {

		// set new values in the pref store

		// spinner: compute value time slice
		fPrefStore.setValue(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE, fSpinnerMinTime.getSelection());

		// force all tours to recompute the speed
		TourManager.getInstance().clearTourDataCache();

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
	}

	private void onComputeElevationGainValues() {

		final int altiMin = ALTITUDE_MINIMUM[fCboMinAltitude.getSelectionIndex()];

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.compute_tourValueElevation_dlg_computeValues_title,
				NLS.bind(Messages.compute_tourValueElevation_dlg_computeValues_message, Integer
						.toString((int) (altiMin / UI.UNIT_VALUE_ALTITUDE)), UI.UNIT_LABEL_ALTITUDE))) {

			saveState();

			fNf.setMinimumFractionDigits(0);
			fNf.setMaximumFractionDigits(0);
			final int[] elevation = new int[] { 0, 0 };

			TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

				public boolean computeTourValues(final TourData oldTourData) {

					// keep old value
					elevation[0] += oldTourData.getTourAltUp();

					return oldTourData.computeAltitudeUpDown();
				}

				public String getResultText() {

					final int prefMinAltitude = TourbookPlugin.getDefault()//
							.getPreferenceStore()
							.getInt(PrefPageComputedValues.STATE_COMPUTED_VALUE_MIN_ALTITUDE);

					return NLS.bind(Messages.compute_tourValueElevation_resultText, //
							new Object[] {
									prefMinAltitude,
									UI.UNIT_LABEL_ALTITUDE,
									fNf.format((elevation[1] - elevation[0]) / UI.UNIT_VALUE_ALTITUDE),
									UI.UNIT_LABEL_ALTITUDE //
							});
				}

				public String getSubTaskText(final TourData savedTourData) {

					String subTaskText = null;

					if (savedTourData != null) {

						// get new value
						elevation[1] += savedTourData.getTourAltUp();

						subTaskText = NLS.bind(Messages.compute_tourValueElevation_subTaskText,//
								new Object[] {
										fNf.format((elevation[1] - elevation[0]) / UI.UNIT_VALUE_ALTITUDE),
										UI.UNIT_LABEL_ALTITUDE //
								});
					}

					return subTaskText;
				}
			});

			fireModifyEvent();
		}
	}

	private void onComputeSpeedValues() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.compute_tourValueSpeed_dlg_computeValues_title,
				NLS.bind(
						Messages.compute_tourValueSpeed_dlg_computeValues_message,
						fSpinnerMinTime.getSelection(),
						Messages.app_unit_seconds//
						))) {

			saveState();

			fNf.setMinimumFractionDigits(0);
			fNf.setMaximumFractionDigits(0);
			final int[] tourMax = new int[] { 0, 0 };

			TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

				public boolean computeTourValues(final TourData oldTourData) {

					// get old value
					tourMax[0] += oldTourData.getMaxSpeed();

					oldTourData.computeSpeedSerie();

					return true;
				}

				public String getResultText() {
					return NLS.bind(Messages.compute_tourValueSpeed_resultText, //
							new Object[] {
									fSpinnerMinTime.getSelection(),
									Messages.app_unit_seconds,
									fNf.format((tourMax[1] - tourMax[0]) / UI.UNIT_VALUE_DISTANCE),
									UI.UNIT_LABEL_SPEED //
							});
				}

				public String getSubTaskText(final TourData savedTourData) {

					String subTaskText = null;

					if (savedTourData != null) {

						// get new value
						tourMax[1] += savedTourData.getMaxSpeed();

						subTaskText = NLS.bind(Messages.compute_tourValueSpeed_subTaskText,//
								new Object[] {
										fNf.format((tourMax[1] - tourMax[0]) / UI.UNIT_VALUE_DISTANCE),
										UI.UNIT_LABEL_SPEED //
								});
					}

					return subTaskText;
				}
			});

			fireModifyEvent();
		}
	}

	@Override
	public boolean performCancel() {
		saveUIState();
		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		fCboMinAltitude.select(DEFAULT_MIN_ALTITUDE_INDEX);
		fSpinnerMinTime.setSelection(fPrefStore.getDefaultInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE));

		onChangeProperty();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		/*
		 * find pref minAlti value in list
		 */
		final int prefMinAltitude = fPrefStore.getInt(STATE_COMPUTED_VALUE_MIN_ALTITUDE);

		int minAltiIndex = -1;
		int listIndex = 0;
		for (final int minAltiInList : ALTITUDE_MINIMUM) {
			if (minAltiInList == prefMinAltitude) {
				minAltiIndex = listIndex;
				break;
			}

			listIndex++;
		}

		if (minAltiIndex == -1) {
			minAltiIndex = DEFAULT_MIN_ALTITUDE_INDEX;
		}

		fCboMinAltitude.select(minAltiIndex);

		fSpinnerMinTime.setSelection(fPrefStore.getInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE));
		fTabFolder.setSelection(fPrefStore.getInt(STATE_COMPUTED_VALUE_SELECTED_TAB));

	}

	private void saveState() {

		fPrefStore.setValue(STATE_COMPUTED_VALUE_MIN_ALTITUDE, ALTITUDE_MINIMUM[fCboMinAltitude.getSelectionIndex()]);
		fPrefStore.setValue(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE, fSpinnerMinTime.getSelection());
	}

	private void saveUIState() {
		fPrefStore.setValue(STATE_COMPUTED_VALUE_SELECTED_TAB, fTabFolder.getSelectionIndex());
	}

}
