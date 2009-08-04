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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.joda.time.DateTime;

public class PrefPageComputedValues extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	STATE_COMPUTED_VALUE_MIN_ALTITUDE	= "computedValue.minAltitude";						//$NON-NLS-1$

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

	private Combo				fCboMinAltitude;
	private Label				fLblMinAltitude;

	@Override
	protected Control createContents(final Composite parent) {

		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			// label: description
			Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.hint(300, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.compute_tourValues_label_description);

			// spacer
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().indent(0, -10).applyTo(label);

			createUIElevationGain(container);

			/**
			 * 4.8.2009 this is currently disabled because a new field in the db is required which
			 * holds the year of the week<br>
			 * <br>
			 * all plugins must be adjusted which set's the week number of a tour
			 */
//			createUIWeek(container);
		}

		return container;
	}

	private void createUIElevationGain(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		group.setText(Messages.compute_tourValueElevation_group_computeTourAltitude);
		{
			// label: min alti diff
			Label label = new Label(group, SWT.NONE);
			label.setText(Messages.compute_tourValueElevation_label_minAltiDifference);

			// combo: min altitude
			fCboMinAltitude = new Combo(group, SWT.READ_ONLY);
			fCboMinAltitude.setVisibleItemCount(20);
			for (final int minAlti : PrefPageComputedValues.ALTITUDE_MINIMUM) {
				fCboMinAltitude.add(Integer.toString((int) (minAlti / UI.UNIT_VALUE_ALTITUDE)));
			}

			// label: unit
			fLblMinAltitude = new Label(group, SWT.NONE);
			fLblMinAltitude.setText(UI.UNIT_LABEL_ALTITUDE);

			// label: description
			label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(300, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.compute_tourValueElevation_label_description);

			final Composite btnContainer = new Composite(group, SWT.NONE);
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
	}

	private void createUIWeek(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		group.setText(Messages.compute_tourValueWeek_group_week);
		{
			// button: compute computed values
			final Button btnComputValues = new Button(group, SWT.NONE);
			GridDataFactory.fillDefaults().indent(0, 10).applyTo(btnComputValues);
			btnComputValues.setText(Messages.compute_tourValueWeek_button_computeWeekValues);
			btnComputValues.setToolTipText(Messages.compute_tourValueWeek_button_computeWeekValues_tooltip);
			btnComputValues.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onComputeWeekValues();
				}
			});
		}
	}

	private void fireModifyEvent() {

		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
	}

	public void init(final IWorkbench workbench) {}

	private void onComputeElevationGainValues() {

		final int altiMin = ALTITUDE_MINIMUM[fCboMinAltitude.getSelectionIndex()];

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.compute_tourValues_dlg_computeValues_title,
				NLS.bind(
						Messages.compute_tourValues_dlg_computeValues_message,
						Integer.toString((int) (altiMin / UI.UNIT_VALUE_ALTITUDE)),
						UI.UNIT_LABEL_ALTITUDE))) {

			saveState();

			TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

				public boolean computeTourValues(final TourData tourData) {
					return tourData.computeAltitudeUpDown();
				}
			});

			fireModifyEvent();
		}
	}

	private void onComputeWeekValues() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.compute_tourValueWeek_dlg_title,
				Messages.compute_tourValueWeek_dlg_message)) {

			saveState();

			TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

				public boolean computeTourValues(final TourData tourData) {

					final DateTime dtTour = new DateTime(tourData.getStartYear(),
							tourData.getStartMonth(),
							tourData.getStartDay(),
							tourData.getStartHour(),
							tourData.getStartMinute(),
							tourData.getStartSecond(),
							0);

					tourData.setStartWeek((short) dtTour.getWeekOfWeekyear());
//					dtTour.getWeekyear()

					return true;
				}
			});

			fireModifyEvent();
		}
	}

	@Override
	protected void performDefaults() {

		fCboMinAltitude.select(DEFAULT_MIN_ALTITUDE_INDEX);

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

	}

	private void saveState() {

		fPrefStore.setValue(STATE_COMPUTED_VALUE_MIN_ALTITUDE, ALTITUDE_MINIMUM[fCboMinAltitude.getSelectionIndex()]);
	}

}
