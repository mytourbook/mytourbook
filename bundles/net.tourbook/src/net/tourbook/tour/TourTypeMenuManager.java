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
package net.tourbook.tour;

import java.util.ArrayList;
import java.util.LinkedList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 * Manage recently used tour types and fills the context menu
 * <p>
 * The method {@link #fillMenuRecentTourTypes} creates the actions and must be called before the
 * actions are enabled/disabled with {@link #enableRecentTourTypeActions}
 */
public class TourTypeMenuManager {

	private static final String				STATE_ID			= "TourTypeManager.RecentTourTypes";				//$NON-NLS-1$
	private static final String				STATE_TOUR_TYPE_ID	= "TourTypeId";									//$NON-NLS-1$

	private static final IPreferenceStore	_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();

	/**
	 * Tour type manager state is saved in {@link #STATE_ID}
	 */
	private static IDialogSettings			_state				= TourbookPlugin.getDefault()//
																		.getDialogSettingsSection(STATE_ID);

	/**
	 * number of tour types which are displayed in the context menu or saved in the dialog settings,
	 * it's max number is 9 to have a unique accelerator key
	 */
	private static LinkedList<TourType>		_recentTourTypes	= new LinkedList<TourType>();

	/**
	 * Contains actions which are displayed in the menu
	 */
	private static RecentTourTypeAction[]	_actionsRecentTourTypes;

	private static int						_maxTourTypes		= -1;

	private static ITourProvider			_tourProvider;

	private static IPropertyChangeListener	_prefChangeListener;

	private static boolean					_isInitialized		= false;
	private static boolean					_isSaveTour;

	private static class RecentTourTypeAction extends Action {

		private TourType	__tourType;

		@Override
		public void run() {
			setTourTypeIntoTour(__tourType, _tourProvider, _isSaveTour);
		}

		private void setTourType(final TourType tourType) {
			__tourType = tourType;
		}
	}

	private static void addPrefChangeListener() {
		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// check if the number of recent tour types has changed
				if (property.equals(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES)) {
					setActions();
				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					updateTourTypes();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Adds the {@link TourType} to the list of the recently used tour types
	 * 
	 * @param tourType
	 */
	private static void addRecentTourType(final TourType tourType) {
		_recentTourTypes.remove(tourType);
		_recentTourTypes.addFirst(tourType);
	}

	/**
	 * @param isEnabled
	 * @param existingTourTypeId
	 */
	public static void enableRecentTourTypeActions(final boolean isEnabled, final long existingTourTypeId) {

		if (_isInitialized == false) {
			initTourTypeManager();
		}

		for (final RecentTourTypeAction actionRecentTourType : _actionsRecentTourTypes) {

			final TourType tourType = actionRecentTourType.__tourType;
			if (tourType == null) {

				// disable tour type

				actionRecentTourType.setEnabled(false);

				// hide image because it looks ugly (on windows) when it's disabled
				actionRecentTourType.setImageDescriptor(null);

				continue;
			}

			final long tourTypeId = tourType.getTypeId();

			if (isEnabled) {

				// enable tour type

				boolean isExistingTourTypeId = false;

				// check if the existing tour type should be enabled
				if (existingTourTypeId != TourDatabase.ENTITY_IS_NOT_SAVED && tourTypeId == existingTourTypeId) {
					isExistingTourTypeId = true;
				}

				actionRecentTourType.setEnabled(isExistingTourTypeId == false);

				if (isExistingTourTypeId) {

					// hide image because it looks ugly (on windows) when it's disabled
					actionRecentTourType.setImageDescriptor(null);

				} else {

					// set tour type image
//					final Image tourTypeImage = UI.getInstance().getTourTypeImage(tourTypeId);
//					actionRecentTourType.setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));

					actionRecentTourType.setImageDescriptor(TourTypeImage.getTourTypeImageDescriptor(tourTypeId));
				}

			} else {

				// disable tour type

				actionRecentTourType.setEnabled(false);

				// hide image because it looks ugly (on windows) when it's disabled
				actionRecentTourType.setImageDescriptor(null);
			}
		}
	}

	/**
	 * Create the menu entries for the recently used tour types
	 * 
	 * @param menuMgr
	 * @param tourProvider
	 * @param isSaveTour
	 */
	public static void fillMenuWithRecentTourTypes(	final IMenuManager menuMgr,
												final ITourProvider tourProvider,
												final boolean isSaveTour) {

		if (_isInitialized == false) {
			initTourTypeManager();
		}

		if (_recentTourTypes.size() == 0) {
			return;
		}

		if (_maxTourTypes < 1) {
			return;
		}

		_tourProvider = tourProvider;
		_isSaveTour = isSaveTour;

		// add tour types
		int tourTypeIndex = 0;
		for (final RecentTourTypeAction actionRecentTourType : _actionsRecentTourTypes) {
			try {

				final TourType recentTourType = _recentTourTypes.get(tourTypeIndex);

				actionRecentTourType.setTourType(recentTourType);
				actionRecentTourType.setText(//
						(UI.SPACE4 + UI.MNEMONIC + (tourTypeIndex + 1) + UI.SPACE2 + recentTourType.getName()));

				menuMgr.add(actionRecentTourType);

			} catch (final IndexOutOfBoundsException e) {
				// there are no more recent tour types
				break;
			}

			tourTypeIndex++;
		}
	}

	private static synchronized void initTourTypeManager() {

		setActions();

		addPrefChangeListener();

		_isInitialized = true;
	}

	public static void restoreState() {

		final String[] allStateTourTypeIds = _state.getArray(STATE_TOUR_TYPE_ID);
		if (allStateTourTypeIds == null) {
			return;
		}

		/*
		 * get all tour types from the database which are saved in the state
		 */
		final ArrayList<TourType> dbTourTypes = TourDatabase.getAllTourTypes();
		for (final String stateTourTypeIdItem : allStateTourTypeIds) {
			try {

				final long stateTourTypeId = Long.parseLong(stateTourTypeIdItem);

				for (final TourType dbTourType : dbTourTypes) {
					if (dbTourType.getTypeId() == stateTourTypeId) {
						_recentTourTypes.add(dbTourType);
						break;
					}
				}
			} catch (final NumberFormatException e) {
				// ignore
			}
		}
	}

	public static void saveState() {

		if (_maxTourTypes < 1) {
			// tour types are not initialized or not visible, do nothing
			return;
		}

		final String[] stateTourTypeIds = new String[Math.min(_maxTourTypes, _recentTourTypes.size())];
		int tourTypeIndex = 0;

		for (final TourType recentTourType : _recentTourTypes) {
			stateTourTypeIds[tourTypeIndex++] = Long.toString(recentTourType.getTypeId());

			if (tourTypeIndex == _maxTourTypes) {
				break;
			}
		}

		_state.put(STATE_TOUR_TYPE_ID, stateTourTypeIds);
	}

	/**
	 * create actions for recenct tour types
	 */
	private static void setActions() {

		_maxTourTypes = TourbookPlugin
				.getDefault()
				.getPreferenceStore()
				.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES);

		_actionsRecentTourTypes = new RecentTourTypeAction[_maxTourTypes];

		for (int actionIndex = 0; actionIndex < _actionsRecentTourTypes.length; actionIndex++) {
			_actionsRecentTourTypes[actionIndex] = new RecentTourTypeAction();
		}
	}

	public static void setTourTypeIntoTour(	final TourType tourType,
											final ITourProvider tourProvider,
											final boolean isSaveTour) {

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {

				final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
				if (selectedTours == null || selectedTours.size() == 0) {
					return;
				}

				// set tour type in all tours (without tours which are opened in an editor)
				for (final TourData tourData : selectedTours) {
					tourData.setTourType(tourType);
				}

				// keep tour type for the recent menu
				addRecentTourType(tourType);

				if (isSaveTour) {

					// save all tours with the modified tour type
					TourManager.saveModifiedTours(selectedTours);

				} else {

					// tours are not saved but the tour provider must be notified

					if (tourProvider instanceof ITourProvider2) {
						((ITourProvider2) tourProvider).toursAreModified(selectedTours);
					} else {
						TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(selectedTours));
					}
				}

			}
		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

	/**
	 * Tour types has changed
	 */
	private static void updateTourTypes() {

		final ArrayList<TourType> dbTourTypes = TourDatabase.getAllTourTypes();
		final LinkedList<TourType> validTourTypes = new LinkedList<TourType>();

		// check if the tour types are still available
		for (final TourType recentTourType : _recentTourTypes) {

			final long recentTypeId = recentTourType.getTypeId();

			for (final TourType dbTourType : dbTourTypes) {

				if (recentTypeId == dbTourType.getTypeId()) {
					validTourTypes.add(dbTourType);
					break;
				}
			}
		}

		// set updated list
		_recentTourTypes.clear();
		_recentTourTypes = validTourTypes;
	}
}
