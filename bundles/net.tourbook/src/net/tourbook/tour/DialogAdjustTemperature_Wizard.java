/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class DialogAdjustTemperature_Wizard extends Wizard {

	private final IPreferenceStore				_prefStore										= TourbookPlugin
																										.getPrefStore();

	private DialogAdjustTemperature_WizardPage	_wizardPage;

	private ArrayList<TourData>					_selectedTours;
	private ITourProvider2						_tourProvider;

	private static final String					LOG_TEMP_ADJUST_001_START						= Messages.Log_TemperatureAdjustment_001_Start;
	private static final String					LOG_TEMP_ADJUST_002_END							= Messages.Log_TemperatureAdjustment_002_End;
	private static final String					LOG_TEMP_ADJUST_003_TOUR_CHANGES				= Messages.Log_TemperatureAdjustment_003_TourChanges;
	private static final String					LOG_TEMP_ADJUST_005_TOUR_IS_TOO_SHORT			= Messages.Log_TemperatureAdjustment_005_TourIsTooShort;
	private static final String					LOG_TEMP_ADJUST_006_IS_ABOVE_TEMPERATURE		= Messages.Log_TemperatureAdjustment_006_IsAboveTemperature;
	private static final String					LOG_TEMP_ADJUST_010_NO_TEMPERATURE_DATA_SERIE	= Messages.Log_TemperatureAdjustment_010_NoTemperatureDataSeries;
	private static final String					LOG_TEMP_ADJUST_011_NO_TIME_DATA_SERIE			= Messages.Log_TemperatureAdjustment_011_NoTimeDataSeries;

	public DialogAdjustTemperature_Wizard(final ArrayList<TourData> selectedTours, final ITourProvider2 tourProvider) {

		super();

		setNeedsProgressMonitor(true);

		_selectedTours = selectedTours;
		_tourProvider = tourProvider;
	}

	@Override
	public void addPages() {

		_wizardPage = new DialogAdjustTemperature_WizardPage(Messages.Dialog_AdjustTemperature_Dialog_Title);

		addPage(_wizardPage);
	}

	@Override
	public String getWindowTitle() {
		return Messages.Dialog_AdjustTemperature_Dialog_Title;
	}

	@Override
	public boolean performFinish() {

		final long start = System.currentTimeMillis();

		TourLogManager.showLogView();

		TourLogManager.logTitle(LOG_TEMP_ADJUST_001_START);

		try {

			getContainer().run(true, true, performFinish_getRunnable());

		} catch (InvocationTargetException | InterruptedException e) {
			StatusUtil.log(e);
		}

		TourLogManager.logDefault(String.format(//
				LOG_TEMP_ADJUST_002_END,
				(System.currentTimeMillis() - start) / 1000.0));

		return true;
	}

	private IRunnableWithProgress performFinish_getRunnable() {

		_wizardPage.saveState();

		final float avgMinimumTemperature = _prefStore.getFloat(//
				ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE);
		final int durationTime = _prefStore.getInt(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME);

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {

			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				monitor.beginTask(Messages.Dialog_AdjustTemperature_Label_Progress_Task, _selectedTours.size());

				// sort tours by date
				Collections.sort(_selectedTours);

				int workedTours = 0;
				final ArrayList<TourData> savedTours = new ArrayList<TourData>();

				for (final TourData tourData : _selectedTours) {

					if (monitor.isCanceled()) {
						break;
					}

					monitor.worked(1);

					monitor.subTask(NLS.bind(
							Messages.Dialog_AdjustTemperature_Label_Progress_SubTask,
							++workedTours,
							_selectedTours.size()));

					final float oldTourAvgTemperature = tourData.getAvgTemperature();

					// skip tours which avg temperature is above the minimum avg temperature
					if (oldTourAvgTemperature > avgMinimumTemperature) {

						TourLogManager.logSubInfo(String.format(
								LOG_TEMP_ADJUST_006_IS_ABOVE_TEMPERATURE,
								TourManager.getTourDateTimeShort(tourData),
								oldTourAvgTemperature,
								avgMinimumTemperature));

						continue;
					}

					if (runnableAdjustTemperature(tourData, durationTime)) {

						// tour is modified, save it

						final TourData savedTourData = TourManager.saveModifiedTour(tourData, false);

						if (savedTourData != null) {
							savedTours.add(savedTourData);
						}
					}

				}

				// update the UI
				if (savedTours.size() > 0) {

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {

							Util.clearSelection();

							/*
							 * Ensure the tour data editor contains the correct tour data
							 */
							TourData tourDataInEditor = null;

							final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
							if (tourDataEditor != null) {
								tourDataInEditor = tourDataEditor.getTourData();
							}

							final TourEvent tourEvent = new TourEvent(savedTours);
							tourEvent.tourDataEditorSavedTour = tourDataInEditor;
							TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);

							// do a reselection of the selected tours to fire the multi tour data selection
							_tourProvider.toursAreModified(savedTours);
						}
					});
				}
			}
		};

		return runnable;
	}

	/**
	 * @param tourData
	 * @param avgTemperature
	 * @return Returns <code>true</code> when the tour is modified, otherwise <code>false</code>.
	 */
	private boolean runnableAdjustTemperature(final TourData tourData, final int durationTime) {

		final int[] timeSerie = tourData.timeSerie;
		final float[] temperatureSerie = tourData.temperatureSerie;

		// ensure data are available
		if (temperatureSerie == null) {

			TourLogManager.logSubError(String.format(
					LOG_TEMP_ADJUST_010_NO_TEMPERATURE_DATA_SERIE,
					TourManager.getTourDateTimeShort(tourData)));

			return false;
		}

		if (timeSerie == null) {

			TourLogManager.logSubError(String.format(
					LOG_TEMP_ADJUST_011_NO_TIME_DATA_SERIE,
					TourManager.getTourDateTimeShort(tourData)));

			return false;
		}

		/*
		 * Get initial temperature
		 */
		float initialTemperature = Integer.MIN_VALUE;

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final int relativeTime = timeSerie[serieIndex];

			if (relativeTime > durationTime) {
				initialTemperature = temperatureSerie[serieIndex];
				break;
			}
		}

		// an initial temperature could not be computed because the tour is too short
		if (initialTemperature == Integer.MIN_VALUE) {

			TourLogManager.logSubError(String.format(
					LOG_TEMP_ADJUST_005_TOUR_IS_TOO_SHORT,
					TourManager.getTourDateTimeShort(tourData)));

			return false;
		}

		/*
		 * Adjust temperature
		 */
		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			final int relativeTime = timeSerie[serieIndex];

			if (relativeTime > durationTime) {
				break;
			}

			temperatureSerie[serieIndex] = initialTemperature;
		}

		final float oldAvgTemperature = tourData.getAvgTemperature();

		tourData.computeAvg_Temperature();

		final float newAvgTemperature = tourData.getAvgTemperature();

		TourLogManager.addSubLog(TourLogState.IMPORT_OK, String.format(
				LOG_TEMP_ADJUST_003_TOUR_CHANGES,
				TourManager.getTourDateTimeShort(tourData),
				oldAvgTemperature,
				newAvgTemperature,
				oldAvgTemperature - newAvgTemperature));

		return true;
	}

}
