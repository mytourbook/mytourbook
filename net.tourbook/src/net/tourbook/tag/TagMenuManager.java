/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.ICommandIds;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.UI;
import net.tourbook.util.AdvancedMenuForActions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class TagMenuManager {

	private static final String				SETTINGS_SECTION_RECENT_TAGS	= "TagManager.RecentTags";						//$NON-NLS-1$
	private static final String				SETTINGS_TAG_ID					= "tagId";										//$NON-NLS-1$

	private static final IPreferenceStore	_prefStore						= TourbookPlugin.getDefault() //
																					.getPreferenceStore();

	private static final IDialogSettings	_state							= TourbookPlugin.getDefault() //
																					.getDialogSettingsSection(
																							SETTINGS_SECTION_RECENT_TAGS);
	private static IPropertyChangeListener	_prefChangeListener;

	private ActionContributionItem			_actionAddTagAdvanced;

	private ActionAddTourTag				_actionAddTag;
	private ActionRemoveTourTag				_actionRemoveTag;
	private ActionRemoveAllTags				_actionRemoveAllTags;
	private AdvancedMenuForActions			_advancedMenuToAddTags;
	/**
	 * number of tags which are displayed in the context menu or saved in the dialog settings, it's
	 * max number is 9 to have a unique accelerator key
	 */
	private static LinkedList<TourTag>		_recentTags						= new LinkedList<TourTag>();

	private static int						_maxRecentActions				= -1;

	private static ActionRecentTag[]		_actionsRecentTags;
	private boolean							_isSaveTour;

	private ITourProvider					_tourProvider;

	private static int						_taggingAutoOpenDelay;
	private static boolean					_isTaggingAutoOpen;

	/**
	 *
	 */
	private static class ActionRecentTag extends Action {

		private TourTag	_tag;

		@Override
		public void run() {
//			_actionAddTag.setTourTag(isChecked(), _tag);
		}

		private void setupTagAction(final TourTag tag, final String tagText) {
			setText(tagText);
			_tag = tag;
		}
	}

	/**
	 * Removes all tags
	 */
	private class ActionRemoveAllTags extends Action {

		public ActionRemoveAllTags() {
			super(Messages.action_tag_remove_all, AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					removeAllTagsRunnable();
				}
			});
		}
	}

	public TagMenuManager(final ITourProvider tourProvider, final boolean isSaveTour) {

		_tourProvider = tourProvider;
		_isSaveTour = isSaveTour;

		if (_prefChangeListener == null) {
			// static var's are not yet initialized
			addPrefListener();
			setupRecentActions();
			restoreAutoOpen();
		}

		_actionAddTagAdvanced = new ActionContributionItem(new ActionAddTourTag(this, null));
		_actionAddTagAdvanced.setId(ICommandIds.ACTION_ADD_TAG);

		_actionAddTag = new ActionAddTourTag(this);
		_actionRemoveTag = new ActionRemoveTourTag(this);
		_actionRemoveAllTags = new ActionRemoveAllTags();

		_advancedMenuToAddTags = new AdvancedMenuForActions(_actionAddTagAdvanced);

	}

	private static void addPrefListener() {

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// check if the number of recent tags has changed
				if (property.equals(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS)) {

					setupRecentActions();

				} else if (property.equals(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN)
						|| property.equals(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY)) {

					restoreAutoOpen();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Adds {@link TourTag}'s to the list of the recently used tags
	 * 
	 * @param tagCollection
	 */
	private static void addRecentTag(final Collection<TourTag> tagCollection) {

		for (final TourTag tag : tagCollection) {
			_recentTags.remove(tag);
			_recentTags.addFirst(tag);
		}
	}

	private static void enableRecentTagActions(final boolean isEnabled, final ArrayList<Long> allExistingTagIds) {

		if (_actionsRecentTags == null) {
			return;
		}

		final boolean isExistingTagIds = allExistingTagIds != null && allExistingTagIds.size() > 0;

		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {

			final TourTag actionTag = actionRecentTag._tag;
			if (actionTag == null) {
				actionRecentTag.setEnabled(false);
				continue;
			}

			if (isExistingTagIds && isEnabled) {

				// disable action when it's tag id is contained in allExistingTagIds

				boolean isExistTagId = false;

				final long recentTagId = actionTag.getTagId();

				for (final long existingTagId : allExistingTagIds) {
					if (recentTagId == existingTagId) {
						isExistTagId = true;
						break;
					}
				}

				actionRecentTag.setEnabled(isExistTagId == false);

			} else {
				actionRecentTag.setEnabled(isEnabled);
			}
		}
	}

	private static void enableRecentTagActions(final boolean isEnabled, final Set<TourTag> allExistingTags) {

		if (_actionsRecentTags == null) {
			return;
		}

		final boolean isExistingTags = allExistingTags != null && allExistingTags.size() > 0;

		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {

			final TourTag actionTag = actionRecentTag._tag;
			if (actionTag == null) {
				actionRecentTag.setEnabled(false);
				continue;
			}

			if (isExistingTags && isEnabled) {

				// disable action when it's tag id is contained in allExistingTags

				boolean isExistTagId = false;

				final long recentTagId = actionTag.getTagId();

				for (final TourTag existingTag : allExistingTags) {
					if (recentTagId == existingTag.getTagId()) {
						isExistTagId = true;
						break;
					}
				}

				actionRecentTag.setEnabled(isExistTagId == false);

			} else {
				actionRecentTag.setEnabled(isEnabled);
			}
		}
	}

	private static void restoreAutoOpen() {
		_isTaggingAutoOpen = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN);
		_taggingAutoOpenDelay = _prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY);
	}

	public static void restoreTagState() {

		final String[] allStateTagIds = _state.getArray(SETTINGS_TAG_ID);
		if (allStateTagIds == null) {
			return;
		}

		final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();
		for (final String tagId : allStateTagIds) {
			try {
				final TourTag tag = allTags.get(Long.valueOf(tagId));
				if (tag != null) {
					_recentTags.add(tag);
				}
			} catch (final NumberFormatException e) {
				// ignore
			}
		}
	}

	public static void saveTagState() {

		if (_maxRecentActions < 1) {
			// tour types are not initialized or not visible, do nothing
			return;
		}

		final String[] tagIds = new String[Math.min(_maxRecentActions, _recentTags.size())];
		int tagIndex = 0;

		for (final TourTag tag : _recentTags) {
			tagIds[tagIndex++] = Long.toString(tag.getTagId());

			if (tagIndex == _maxRecentActions) {
				break;
			}
		}

		_state.put(SETTINGS_TAG_ID, tagIds);
	}

	/**
	 * create actions for recenct tags
	 */
	private static void setupRecentActions() {

		_maxRecentActions = TourbookPlugin
				.getDefault()
				.getPreferenceStore()
				.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS);

		_actionsRecentTags = new ActionRecentTag[_maxRecentActions];

		for (int actionIndex = 0; actionIndex < _actionsRecentTags.length; actionIndex++) {
			_actionsRecentTags[actionIndex] = new ActionRecentTag();
		}
	}

	/**
	 * Update names of all recent tags
	 */
	public static void updateRecentTagNames() {

		final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

		for (final TourTag recentTag : _recentTags) {

			final TourTag tourTag = allTourTags.get(recentTag.getTagId());
			if (tourTag != null) {
				recentTag.setTagName(tourTag.getTagName());
			}
		}
	}

	/**
	 * @param isAddTagEnabled
	 * @param isRemoveTagEnabled
	 */
	private void enableTagActions(final boolean isAddTagEnabled, final boolean isRemoveTagEnabled) {

		((ActionAddTourTag) _actionAddTagAdvanced.getAction()).setEnabled(isAddTagEnabled);
		_actionAddTag.setEnabled(isAddTagEnabled);

		_actionRemoveTag.setEnabled(isRemoveTagEnabled);
		_actionRemoveAllTags.setEnabled(isRemoveTagEnabled);
	}

	/**
	 * @param isAddTagEnabled
	 * @param isRemoveTagEnabled
	 * @param existingTagIds
	 */
	public void enableTagActions(	final boolean isAddTagEnabled,
									final boolean isRemoveTagEnabled,
									final ArrayList<Long> existingTagIds) {

		enableTagActions(isAddTagEnabled, isRemoveTagEnabled);
		enableRecentTagActions(isAddTagEnabled, existingTagIds);
	}

	/**
	 * @param isAddTagEnabled
	 * @param isRemoveTagEnabled
	 * @param tourTags
	 */
	public void enableTagActions(	final boolean isAddTagEnabled,
									final boolean isRemoveTagEnabled,
									final Set<TourTag> tourTags) {

		enableTagActions(isAddTagEnabled, isRemoveTagEnabled);
		enableRecentTagActions(isAddTagEnabled, tourTags);
	}

	/**
	 * @param menu
	 */
	void fillMenuWithRecentTags(final Menu menu) {

		if (_recentTags.size() == 0) {
			return;
		}

		if (_maxRecentActions < 1) {
			return;
		}

		// add tag's
		int tagIndex = 0;
		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
			try {

				final TourTag tag = _recentTags.get(tagIndex);

				actionRecentTag.setupTagAction(tag, (UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getTagName()));

				new ActionContributionItem(actionRecentTag).fill(menu, -1);

			} catch (final IndexOutOfBoundsException e) {
				// there are no more recent tags
				break;
			}

			tagIndex++;
		}
	}

	/**
	 * @param menuMgr
	 */
	public void fillTagMenu(final IMenuManager menuMgr) {

		// all all tour tag actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionAddTagAdvanced);
		menuMgr.add(_actionAddTag);
		menuMgr.add(_actionRemoveTag);
		menuMgr.add(_actionRemoveAllTags);
	}

	public ITourProvider getTourProvider() {
		return _tourProvider;
	}

	/**
	 * This is called when the context menu is displayed which contains the tag actions.
	 * 
	 * @param menuEvent
	 * @param menuParentControl
	 */
	public void onShowTagMenu(final MenuEvent menuEvent, final Control menuParentControl) {

		_advancedMenuToAddTags
				.onShowParentMenu(menuEvent, menuParentControl, _isTaggingAutoOpen, _taggingAutoOpenDelay);
	}

	private void removeAllTagsRunnable() {

		// get tours which tour type should be changed
		final ArrayList<TourData> modifiedTours = _tourProvider.getSelectedTours();

		if (modifiedTours == null || modifiedTours.size() == 0) {
			return;
		}

		final HashMap<Long, TourTag> modifiedTags = new HashMap<Long, TourTag>();

		// remove tag in all tours (without tours from an editor)
		for (final TourData tourData : modifiedTours) {

			// get all tag's which will be removed
			final Set<TourTag> tourTags = tourData.getTourTags();

			for (final TourTag tourTag : tourTags) {
				modifiedTags.put(tourTag.getTagId(), tourTag);
			}

			// remove all tour tags
			tourTags.clear();
		}

		saveAndNotify(modifiedTags, modifiedTours);
	}

	/**
	 * Save modified tours and notify tour provider
	 * 
	 * @param modifiedTags
	 * @param modifiedTours
	 */
	private void saveAndNotify(final HashMap<Long, TourTag> modifiedTags, ArrayList<TourData> modifiedTours) {
		if (_isSaveTour) {

			// save all tours with the removed tags

			modifiedTours = TourManager.saveModifiedTours(modifiedTours);

		} else {

			// tours are not saved but the tour provider must be notified that tours has changed

			if (_tourProvider instanceof ITourProvider2) {
				((ITourProvider2) _tourProvider).toursAreModified(modifiedTours);
			} else {
				TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(modifiedTours));
			}
		}

		TourManager.fireEvent(TourEventId.NOTIFY_TAG_VIEW, new ChangedTags(modifiedTags, modifiedTours, false));
	}

	/**
	 * Set tour tag for multiple tours
	 * 
	 * @param modifiedTags
	 * @param isAddMode
	 */
	void saveTourTags(final HashMap<Long, TourTag> modifiedTags, final boolean isAddMode) {

		final Runnable runnable = new Runnable() {

			public void run() {

				final ArrayList<TourData> modifiedTours = _tourProvider.getSelectedTours();

				// get tours which tag should be changed
				if (modifiedTours == null || modifiedTours.size() == 0) {
					return;
				}

				final Collection<TourTag> tagCollection = modifiedTags.values();

				// add the tag into all selected tours
				for (final TourData tourData : modifiedTours) {

					// set tag into tour
					final Set<TourTag> tourTags = tourData.getTourTags();

					if (isAddMode) {
						// add tag to the tour
						tourTags.addAll(tagCollection);
					} else {
						// remove tag from tour
						tourTags.removeAll(tagCollection);
					}
				}

				addRecentTag(tagCollection);

				saveAndNotify(modifiedTags, modifiedTours);
			}
		};

		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

	/**
	 * Set tour tag for one tour
	 * 
	 * @param tag
	 * @param tourProvider
	 * @param isAddMode
	 * @param isSaveTour
	 */
	void saveTourTags(final TourTag tag, final boolean isAddMode) {

		final HashMap<Long, TourTag> tags = new HashMap<Long, TourTag>();
		tags.put(tag.getTagId(), tag);

		saveTourTags(tags, isAddMode);
	}
}
