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
package net.tourbook.ui.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.DialogMergeTours;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionMergeTour extends Action {

	private ITourProvider	fTourProvider;

	private TourData		fIntoTourData;
	private TourData		fFromTourData;

	private int[]			fBackupFromTimeSerie;
	private int[]			fBackupFromDistanceSerie;
	private int[]			fBackupFromAltitudeSerie;

	private int[]			fBackupIntoTemperatureSerie;
	private int[]			fBackupIntoGradientSerie;

	public ActionMergeTour(final ITourProvider tourProvider) {

		setText(Messages.app_action_merge_tour);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__merge_tours));

		fTourProvider = tourProvider;
	}

	private int[] backupDataSerie(final int[] source) {

		int[] backup = null;
		if (source != null) {
			final int serieLength = source.length;
			backup = new int[serieLength];
			System.arraycopy(source, 0, backup, 0, serieLength);
		}

		return backup;
	}

	private void createDataBackup() {

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		fBackupFromTimeSerie = backupDataSerie(fFromTourData.timeSerie);
		fBackupFromDistanceSerie = backupDataSerie(fFromTourData.distanceSerie);
		fBackupFromAltitudeSerie = backupDataSerie(fFromTourData.altitudeSerie);

		fBackupIntoTemperatureSerie = backupDataSerie(fIntoTourData.temperatureSerie);
		fBackupIntoGradientSerie = backupDataSerie(fIntoTourData.gradientSerie);

//		fTourData.setMergedTourTimeOffset(timeOffset);
//		fTourData.setMergedAltitudeOffset(metricAltiDiff);

	}

	/**
	 * @return {@link TourData} for the selected tour or <code>null</code> when the selected tour is
	 *         not valid
	 */
	private boolean getSelectedTour() {

		if (UI.isTourEditorModified()) {
			return false;
		}

		// get selected tour, make sure only one tour is selected
		final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
		if (selectedTours == null || selectedTours.size() != 1) {
			return false;
		}

		final TourData intoTourData = selectedTours.get(0);
		TourData fromTourData;

		final Long mergeFromTourId = intoTourData.getMergeFromTourId();
		if (mergeFromTourId == null) {

			// check if the tour can be merged

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					Messages.merge_tour_dlg_invalid_tour_message);

			return false;

		} else if ((fromTourData = TourManager.getInstance().getTourData(mergeFromTourId)) == null) {

			// check if merge from tour is available

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					NLS.bind(Messages.merge_tour_dlg_invalid_tour_data_message,
							mergeFromTourId,
							TourManager.getTourTitle(intoTourData)));

			// remove invalid merge tour id
			intoTourData.setMergeFromTourId(null);
			TourManager.saveModifiedTour(intoTourData);

			return false;

		} else if (intoTourData.altitudeSerie == null
				|| intoTourData.altitudeSerie.length == 0
				|| intoTourData.timeSerie == null
				|| intoTourData.timeSerie.length == 0
				|| fromTourData.altitudeSerie == null
				|| fromTourData.altitudeSerie.length == 0
				|| fromTourData.timeSerie == null
				|| fromTourData.timeSerie.length == 0) {

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					Messages.merge_tour_dlg_invalid_serie_data_message);

			return false;
		}

		fIntoTourData = intoTourData;
		fFromTourData = fromTourData;

		return true;
	}

	/**
	 * Restore values which have been modified in the dialog
	 * 
	 * @param selectedTour
	 */
	private void restoreDataBackup() {

		fFromTourData.timeSerie = fBackupFromTimeSerie;
		fFromTourData.distanceSerie = fBackupFromDistanceSerie;
		fFromTourData.altitudeSerie = fBackupFromAltitudeSerie;

		fIntoTourData.temperatureSerie = fBackupIntoTemperatureSerie;
		fIntoTourData.gradientSerie = fBackupIntoGradientSerie;

//			fTourData.setMergedTourTimeOffset(timeOffset);
//			fTourData.setMergedAltitudeOffset(metricAltiDiff);

	}

	@Override
	public void run() {

		if (getSelectedTour() == false) {
			return;
		}

		createDataBackup();

		if (new DialogMergeTours(Display.getCurrent().getActiveShell(), fIntoTourData).open() == Window.OK) {

			// save merged tour
			TourManager.saveModifiedTour(fIntoTourData);

		} else {

			// restore data because the tour could be modified
			restoreDataBackup();
		}
	}

}
