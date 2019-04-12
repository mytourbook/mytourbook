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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

import com.skedgo.converter.TimezoneMapper;

public class DialogSetTimeZone_Wizard extends Wizard {

	private static final String				LOG_SET_TIMEZONE_001_START_FROM_LIST	= Messages.Log_SetTimeZone_001_Start_FromList;
	private static final String				LOG_SET_TIMEZONE_001_START_FROM_GEO		= Messages.Log_SetTimeZone_001_Start_FromGeo;
	private static final String				LOG_SET_TIMEZONE_001_START_REMOVE		= Messages.Log_SetTimeZone_001_Start_Remove;
	private static final String				LOG_SET_TIMEZONE_002_END				= Messages.Log_SetTimeZone_002_End;
	private static final String				LOG_SET_TIMEZONE_010_SET_SELECTED		= Messages.Log_SetTimeZone_010_SetSelected;
	private static final String				LOG_SET_TIMEZONE_011_SET_FROM_GEO		= Messages.Log_SetTimeZone_011_SetFromGeo;
	private static final String				LOG_SET_TIMEZONE_012_NO_GEO				= Messages.Log_SetTimeZone_012_NoGeo;
	private static final String				LOG_SET_TIMEZONE_013_REMOVED			= Messages.Log_SetTimeZone_013_Removed;
	//

	private final IPreferenceStore			_prefStore								= TourbookPlugin.getPrefStore();

	private DialogSetTimeZone_WizardPage	_wizardPage;

	private ArrayList<TourData>				_selectedTours;
	private ITourProvider2					_tourProvider;

	private boolean _isHelpAvailable;
	private boolean	_keepTime;
	
	public DialogSetTimeZone_Wizard(final ArrayList<TourData> selectedTours, final ITourProvider2 tourProvider) {

		super();

		setNeedsProgressMonitor(true);

		_selectedTours = selectedTours;
		_tourProvider = tourProvider;
		
		_isHelpAvailable = true;
		_keepTime = false;
	}

	@Override
	public void addPages() {

		_wizardPage = new DialogSetTimeZone_WizardPage(Messages.Dialog_SetTimeZone_Dialog_Title);

		addPage(_wizardPage);
	}

	@Override
	public String getWindowTitle() {
		return Messages.Dialog_SetTimeZone_Dialog_Title;
	}

    @Override
	public boolean isHelpAvailable() {
    	    	
    	if (_isHelpAvailable) {
    		_isHelpAvailable = false;
    	} else {
	    	_keepTime = true;
    	}
        return false;
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
				LOG_SET_TIMEZONE_002_END,
				(System.currentTimeMillis() - start) / 1000.0));

		return true;
	}

	private IRunnableWithProgress performFinish_getRunnable() {

		_wizardPage.saveState();

		final int timeZoneAction = _prefStore.getInt(ITourbookPreferences.DIALOG_SET_TIME_ZONE_ACTION);
		final String timeZoneId = _prefStore.getString(ITourbookPreferences.DIALOG_SET_TIME_ZONE_SELECTED_ZONE_ID);
		final ZoneId selectedzoneId = ZoneId.of(timeZoneId);

		/*
		 * Create start log message
		 */
		String startLogMessage = null;

		switch (timeZoneAction) {
		case DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_GEO_POSITION:

			startLogMessage = NLS.bind(LOG_SET_TIMEZONE_001_START_FROM_GEO, _selectedTours.size());
			break;

		case DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_LIST:

			startLogMessage = NLS.bind(LOG_SET_TIMEZONE_001_START_FROM_LIST, timeZoneId, _selectedTours.size());
			break;

		case DialogSetTimeZone.TIME_ZONE_ACTION_REMOVE_TIME_ZONE:

			startLogMessage = NLS.bind(LOG_SET_TIMEZONE_001_START_REMOVE, _selectedTours.size());
			break;
		}

		TourLogManager.addLog(TourLogState.DEFAULT, startLogMessage);

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {

			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				monitor.beginTask(Messages.Dialog_SetTimeZone_Label_Progress_Task, _selectedTours.size());

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
							Messages.Dialog_SetTimeZone_Label_Progress_SubTask,
							++workedTours,
							_selectedTours.size()));

					final String tourDateTime = TourManager.getTourDateTimeShort(tourData);

			        final long tourStartTimeUTC_MS = tourData.getTourStartTimeMS();
			        final ZonedDateTime tourStartPrevZone = tourData.getTourStartTime();
			         
					switch (timeZoneAction) {

					case DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_LIST:

						// set time zone which is selected in a list
						tourData.setTimeZoneId(selectedzoneId.getId());
				        
						TourLogManager.addLog(
								TourLogState.DEFAULT,
								NLS.bind(LOG_SET_TIMEZONE_010_SET_SELECTED, tourDateTime));

						break;

					case DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_GEO_POSITION:

						// set time zone from the tour geo position

						if (tourData.latitudeSerie != null) {

							// get time zone from lat/lon
							final double lat = tourData.latitudeSerie[0];
							final double lon = tourData.longitudeSerie[0];

							final String rawZoneId = TimezoneMapper.latLngToTimezoneString(lat, lon);
							final ZoneId zoneId = ZoneId.of(rawZoneId);

							tourData.setTimeZoneId(zoneId.getId());

							TourLogManager.addLog(
									TourLogState.DEFAULT,
									NLS.bind(LOG_SET_TIMEZONE_011_SET_FROM_GEO, zoneId.getId(), tourDateTime));

						} else {

							TourLogManager.addLog(
									TourLogState.IMPORT_ERROR,
									NLS.bind(LOG_SET_TIMEZONE_012_NO_GEO, tourDateTime));
						}

						break;

					case DialogSetTimeZone.TIME_ZONE_ACTION_REMOVE_TIME_ZONE:

						// remove time zone

						tourData.setTimeZoneId(null);

						TourLogManager.addLog(
								TourLogState.DEFAULT,
								NLS.bind(LOG_SET_TIMEZONE_013_REMOVED, tourDateTime));

						break;

					default:
						// this should not happen
						continue;
					}

					if(_keepTime) {
				        final ZonedDateTime tourStartNewZone = tourData.getTourStartTime();
				        final int prevZoneOffsetMS = tourStartPrevZone.getOffset().getTotalSeconds() * 1000;
				        final int newZoneOffsetMS = tourStartNewZone.getOffset().getTotalSeconds() * 1000;
				        tourData.setTourStartTimeMS(tourStartTimeUTC_MS + prevZoneOffsetMS - newZoneOffsetMS);
					}

					final TourData savedTourData = TourManager.saveModifiedTour(tourData, false);

					if (savedTourData != null) {
						savedTours.add(savedTourData);
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
