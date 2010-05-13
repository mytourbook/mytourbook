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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 */
public class TagManager {

	public static final String[]			EXPAND_TYPE_NAMES				= {
			Messages.app_action_expand_type_flat,
			Messages.app_action_expand_type_year_day,
			Messages.app_action_expand_type_year_month_day					};

	public static final int[]				EXPAND_TYPES					= {
			TourTag.EXPAND_TYPE_FLAT,
			TourTag.EXPAND_TYPE_YEAR_DAY,
			TourTag.EXPAND_TYPE_YEAR_MONTH_DAY								};

	private static final String				SETTINGS_SECTION_RECENT_TAGS	= "TagManager.RecentTags";						//$NON-NLS-1$
	private static final String				SETTINGS_TAG_ID					= "tagId";										//$NON-NLS-1$

	private static IDialogSettings			_state							= TourbookPlugin
																					.getDefault()
																					.getDialogSettingsSection(
																							SETTINGS_SECTION_RECENT_TAGS);

	/**
	 * number of tags which are displayed in the context menu or saved in the dialog settings, it's
	 * max number is 9 to have a unique accelerator key
	 */
	private static LinkedList<TourTag>		_recentTags						= new LinkedList<TourTag>();

	private static ActionRecentTag[]		_actionsRecentTags;

	private static ITourProvider			_tourProvider;
	private static boolean					_isAddMode;
	private static boolean					_isSaveTour;

	private static IPropertyChangeListener	_prefChangeListener;

	private static int						_maxTags						= -1;

	private static class ActionRecentTag extends Action {

		private TourTag	_tag;

		@Override
		public void run() {
			setTagIntoTour(_tag, _tourProvider, _isAddMode, _isSaveTour);
		}

		private void setTag(final TourTag tag) {
			_tag = tag;
		}
	}

	/**
	 * Adds the {@link TourTag} to the list of the recently used tags
	 * 
	 * @param tourTag
	 */
	private static void addRecentTag(final TourTag tourTag) {
		_recentTags.remove(tourTag);
		_recentTags.addFirst(tourTag);
	}

	public static void enableRecentTagActions(final boolean isEnabled) {

		if (_actionsRecentTags == null) {
			return;
		}

		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
			actionRecentTag.setEnabled(isEnabled);
		}
	}

	/**
	 * Create the menu entries for the recently used tags
	 * 
	 * @param menuMgr
	 * @param isSaveTour
	 */
	public static void fillRecentTagsIntoMenu(	final IMenuManager menuMgr,
												final ITourProvider tourProvider,
												final boolean isAddMode,
												final boolean isSaveTour) {

		if (_actionsRecentTags == null) {
			initTagManager();
		}

		if (_recentTags.size() == 0) {
			return;
		}

		if (_maxTags < 1) {
			return;
		}

		_tourProvider = tourProvider;
		_isAddMode = isAddMode;
		_isSaveTour = isSaveTour;

		// add tag's
		int tagIndex = 0;
		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
			try {

				final TourTag tag = _recentTags.get(tagIndex);

				actionRecentTag.setTag(tag);
				actionRecentTag.setText(//
						(UI.SPACE4 + UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getTagName())); //$NON-NLS-1$

				menuMgr.add(actionRecentTag);

			} catch (final IndexOutOfBoundsException e) {
				// there are no more recent tags
				break;
			}

			tagIndex++;
		}
	}

	private static void initTagManager() {

		setActions();

		// create pref listener
		_prefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {
				final String property = event.getProperty();

				// check if the number of recent tags has changed
				if (property.equals(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS)) {
					setActions();
				}
			}
		};

		// add pref listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(_prefChangeListener);
	}

	public static void restoreState() {

		final String[] allStateTagIds = _state.getArray(SETTINGS_TAG_ID);
		if (allStateTagIds == null) {
			return;
		}

		final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();
		for (final String tagId : allStateTagIds) {
			try {
				final TourTag tag = allTags.get(Long.parseLong(tagId));
				if (tag != null) {
					_recentTags.add(tag);
				}
			} catch (final NumberFormatException e) {
				// ignore
			}
		}
	}

	public static void saveState() {

		if (_maxTags < 1) {
			// tour types are not initialized or not visible, do nothing
			return;
		}

		final String[] tagIds = new String[Math.min(_maxTags, _recentTags.size())];
		int tagIndex = 0;

		for (final TourTag tag : _recentTags) {
			tagIds[tagIndex++] = Long.toString(tag.getTagId());

			if (tagIndex == _maxTags) {
				break;
			}
		}

		_state.put(SETTINGS_TAG_ID, tagIds);
	}

	/**
	 * create actions for recenct tags
	 */
	private static void setActions() {

		_maxTags = TourbookPlugin
				.getDefault()
				.getPreferenceStore()
				.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS);

		_actionsRecentTags = new ActionRecentTag[_maxTags];

		for (int actionIndex = 0; actionIndex < _actionsRecentTags.length; actionIndex++) {
			_actionsRecentTags[actionIndex] = new ActionRecentTag();
		}
	}

	public static void setTagIntoTour(	final TourTag tourTag,
										final ITourProvider tourProvider,
										final boolean isAddMode,
										final boolean isSaveTour) {

		final Runnable runnable = new Runnable() {

			public void run() {

				// get tours which tag should be changed
				ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

				if (selectedTours == null || selectedTours.size() == 0) {
					return;
				}

				// add the tag into all selected tours
				for (final TourData tourData : selectedTours) {

					// set tag into tour
					final Set<TourTag> tourTags = tourData.getTourTags();

					if (isAddMode) {
						// add tag to the tour
						tourTags.add(tourTag);
					} else {
						// remove tag from tour
						tourTags.remove(tourTag);
					}
				}

				addRecentTag(tourTag);

				if (isSaveTour) {

					// save all tours with the removed tags
					final ArrayList<TourData> savedTours = TourManager.saveModifiedTours(selectedTours);
					selectedTours = savedTours;

				} else {

					// tours are not saved but the tour provider must be notified

					TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(selectedTours));
				}

				TourManager.fireEvent(TourEventId.NOTIFY_TAG_VIEW, //
						new ChangedTags(tourTag, selectedTours, false));
			}

		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

	/**
	 * Update the names of all recent tags
	 */
	public static void updateTagNames() {

		final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

		for (final TourTag recentTag : _recentTags) {

			final TourTag tourTag = allTourTags.get(recentTag.getTagId());
			if (tourTag != null) {
				recentTag.setTagName(tourTag.getTagName());
			}
		}
	}
}
