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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.importdata.EasyImportManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class DialogAdjustTemperature_Wizard extends Wizard {

	private final IPreferenceStore				_prefStore			= TourbookPlugin.getPrefStore();

	private DialogAdjustTemperature_WizardPage	_wizardPage;

	private ArrayList<TourData>					_selectedTours;
	private ITourProvider2						_tourProvider;

	private static PeriodType					_durationTemplate	= PeriodType.yearMonthDayTime()
//			// hide these components
																			.withMillisRemoved();

	private final NumberFormat					_nf1				= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

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

		try {

			getContainer().run(true, true, performFinish_getRunnable());

		} catch (InvocationTargetException | InterruptedException e) {
			StatusUtil.log(e);
		}

		TourLogManager.logDefault(String.format(//
				EasyImportManager.LOG_TEMP_ADJUST_002_END,
				(System.currentTimeMillis() - start) / 1000.0));

		return true;
	}

	private IRunnableWithProgress performFinish_getRunnable() {

		_wizardPage.saveState();

		final float avgTemperature = _prefStore.getFloat(ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE);
		final int durationTime = _prefStore.getInt(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME);

		final float temperature = UI.getTemperatureFromMetric(avgTemperature);
		final Period durationPeriod = new Period(0, durationTime * 1000, _durationTemplate);

		final String logText = NLS.bind(
				EasyImportManager.LOG_TEMP_ADJUST_001_START,
				new Object[] {
						durationPeriod.toString(UI.DEFAULT_DURATION_FORMATTER),
						_nf1.format(temperature),
						UI.UNIT_LABEL_TEMPERATURE });

		TourLogManager.addLog(TourLogState.DEFAULT, logText);

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
					if (oldTourAvgTemperature > avgTemperature) {

						TourLogManager.logSubInfo(String.format(
								EasyImportManager.LOG_TEMP_ADJUST_006_IS_ABOVE_TEMPERATURE,
								TourManager.getTourDateTimeShort(tourData),
								oldTourAvgTemperature,
								avgTemperature));

						continue;
					}

					if (EasyImportManager.adjustTemperature(tourData, durationTime)) {

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

}
