/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionComputeElevationGain extends Action {

	private final ITourProviderByID	_tourProvider;

	private NumberFormat			_nf0	= NumberFormat.getNumberInstance();
	private NumberFormat			_nf1	= NumberFormat.getNumberInstance();
	{
		_nf0.setMinimumFractionDigits(0);
		_nf0.setMaximumFractionDigits(0);
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	public ActionComputeElevationGain(final ITourProviderByID tourProvider) {

		super(null, AS_PUSH_BUTTON);

		_tourProvider = tourProvider;

		setText(Messages.Action_Compute_ElevationGain);
	}

	@Override
	public void run() {

		final ArrayList<Long> tourIds = new ArrayList<Long>(_tourProvider.getSelectedTourIDs());

		final float prefDPTolerance = TourbookPlugin.getPrefStore().getFloat(
				ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE);

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.Compute_TourValue_ElevationGain_Title,
				NLS.bind(//
						Messages.Compute_TourValue_ElevationGain_Message,
						tourIds.size(),
						_nf1.format(prefDPTolerance))//
				//
				) == false) {
			return;
		}

		final int[] elevation = new int[] { 0, 0 };

		final IComputeTourValues configComputeTourValue = new IComputeTourValues() {

			public boolean computeTourValues(final TourData tourData) {

				// keep old value
				elevation[0] += tourData.getTourAltUp();

				return tourData.computeAltitudeUpDown();
			}

			public String getResultText() {

				return NLS.bind(Messages.Compute_TourValue_ElevationGain_ResultText, //
						new Object[] {
								prefDPTolerance,
								_nf0.format((elevation[1] - elevation[0]) / UI.UNIT_VALUE_ALTITUDE),
								net.tourbook.common.UI.UNIT_LABEL_ALTITUDE //
						});
			}

			public String getSubTaskText(final TourData savedTourData) {

				String subTaskText = null;

				if (savedTourData != null) {

					// summarize new values
					elevation[1] += savedTourData.getTourAltUp();

					subTaskText = NLS.bind(Messages.compute_tourValueElevation_subTaskText,//
							new Object[] {
									_nf0.format((elevation[1] - elevation[0]) / UI.UNIT_VALUE_ALTITUDE),
									net.tourbook.common.UI.UNIT_LABEL_ALTITUDE //
							});
				}

				return subTaskText;
			}
		};

		TourDatabase.computeValuesForAllTours(configComputeTourValue, tourIds);

		/*
		 * Fire event
		 */
		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
	}
}
