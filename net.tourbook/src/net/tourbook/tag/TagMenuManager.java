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
import java.util.HashSet;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class TagMenuManager {

	private static final String				SETTINGS_SECTION_RECENT_TAGS	= "TagManager.RecentTags";						//$NON-NLS-1$
	private static final String				STATE_RECENT_TAGS				= "tagId";										//$NON-NLS-1$
	private static final String				STATE_PREVIOUS_TAGS				= "";											//$NON-NLS-1$

	private static final IPreferenceStore	_prefStore						= TourbookPlugin.getDefault() //
																					.getPreferenceStore();

	private static final IDialogSettings	_state							= TourbookPlugin.getDefault() //
																					.getDialogSettingsSection(
																							SETTINGS_SECTION_RECENT_TAGS);
	private static IPropertyChangeListener	_prefChangeListener;

	private static TagMenuManager			_currentInstance;
	private static boolean					_isAdvMenu;

	private static ActionRecentTag[]		_actionsRecentTags;
	private static ActionAllPreviousTags	_actionAllPreviousTags;

	/**
	 * number of tags which are displayed in the context menu or saved in the dialog settings, it's
	 * max number is 9 to have a unique accelerator key
	 */
	private static LinkedList<TourTag>		_recentTags						= new LinkedList<TourTag>();

	/**
	 * Contains all tags which are added by the last add action
	 */
	private static HashMap<Long, TourTag>	_allPreviousTags				= new HashMap<Long, TourTag>();

	private static int						_maxRecentActions				= -1;

	/**
	 * Contains tag id's for all selected tours
	 */
	private static HashSet<Long>			_allTourTagIds;
	private static boolean					_isEnableRecentTagActions;

	private static int						_taggingAutoOpenDelay;
	private static boolean					_isTaggingAutoOpen;
	private static boolean					_isTaggingAnimation;

	private boolean							_isSaveTour;
	private ITourProvider					_tourProvider;

	private ActionContributionItem			_actionAddTagAdvanced;
	private ActionAddTourTag				_actionAddTag;
	private ActionRemoveTourTag				_actionRemoveTag;
	private ActionRemoveAllTags				_actionRemoveAllTags;

	private AdvancedMenuForActions			_advancedMenuToAddTags;

	/**
	 *
	 */
	private static class ActionAllPreviousTags extends Action {

		public ActionAllPreviousTags() {
			super(UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
		}

		@Override
		public void run() {

			if (_isAdvMenu) {

				final ActionAddTourTag actionAddTagAdvanced = (ActionAddTourTag) _currentInstance._actionAddTagAdvanced
						.getAction();

				actionAddTagAdvanced.setTourTag(isChecked(), _allPreviousTags);
			} else {

				_currentInstance.saveTourTags(_allPreviousTags, isChecked());
			}
		}
	}

	/**
	 *
	 */
	private static class ActionRecentTag extends Action {

		private TourTag	_tag;

		public ActionRecentTag() {
			super(UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
		}

		@Override
		public void run() {

			final ActionAddTourTag actionAddTagAdvanced = (ActionAddTourTag) _currentInstance._actionAddTagAdvanced
					.getAction();

			if (_isAdvMenu) {
				actionAddTagAdvanced.setTourTag(isChecked(), _tag);
			} else {
				_currentInstance.saveTourTags(_tag, isChecked());
			}
		}

		private void setupTagAction(final TourTag tag, final String tagText) {
			setText(tagText);
			_tag = tag;
		}

		@Override
		public String toString() {
			return "ActionRecentTag [_tag=" + _tag + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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
					runnableRemoveAllTags();
				}
			});
		}
	}

	/**
	 * @param tourProvider
	 * @param isSaveTour
	 */
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
						|| property.equals(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION)
						|| property.equals(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY)) {

					restoreAutoOpen();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	static void enableRecentTagActions(final boolean isAddTagEnabled, final Set<Long> existingTagIds) {

		if (_actionsRecentTags == null) {
			return;
		}

		final boolean isExistingTagIds = _allTourTagIds != null && _allTourTagIds.size() > 0;
		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {

			final TourTag actionTag = actionRecentTag._tag;
			if (actionTag == null) {
				actionRecentTag.setEnabled(false);
				continue;
			}

			final long recentTagId = actionTag.getTagId();
			boolean isTagEnabled;
			boolean isTagChecked;

			if (isExistingTagIds && _isEnableRecentTagActions) {

				// disable action when it's tag id is contained in allExistingTagIds

				boolean isExistTagId = false;

				for (final long existingTagId : _allTourTagIds) {
					if (recentTagId == existingTagId) {
						isExistTagId = true;
						break;
					}
				}

				isTagEnabled = isExistTagId == false;
				isTagChecked = isExistTagId;

			} else {
				isTagEnabled = _isEnableRecentTagActions;
				isTagChecked = false;
			}

			if (isTagEnabled && existingTagIds.contains(recentTagId)) {
				isTagChecked = true;
			}

			actionRecentTag.setEnabled(isTagEnabled);
			actionRecentTag.setChecked(isTagChecked);
		}

		if (_allPreviousTags != null && isAddTagEnabled) {

			boolean isTagChecked = true;

			for (final TourTag previousTag : _allPreviousTags.values()) {
				if (_allTourTagIds.contains(previousTag.getTagId()) == false) {
					isTagChecked = false;
					break;
				}
			}

			_actionAllPreviousTags.setChecked(isTagChecked);
			_actionAllPreviousTags.setEnabled(isTagChecked == false);

		} else {

			_actionAllPreviousTags.setChecked(false);
			_actionAllPreviousTags.setEnabled(false);
		}

	}

	private static void restoreAutoOpen() {
		_isTaggingAutoOpen = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN);
		_isTaggingAnimation = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION);
		_taggingAutoOpenDelay = _prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY);
	}

	public static void restoreTagState() {

		final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();

		final String[] recentTagIds = _state.getArray(STATE_RECENT_TAGS);
		if (recentTagIds != null) {

			for (final String tagId : recentTagIds) {
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

		final String[] previousTagIds = _state.getArray(STATE_PREVIOUS_TAGS);
		if (previousTagIds != null) {

			for (final String tagId : previousTagIds) {
				try {
					final TourTag tag = allTags.get(Long.valueOf(tagId));
					if (tag != null) {
						_allPreviousTags.put(tag.getTagId(), tag);
					}
				} catch (final NumberFormatException e) {
					// ignore
				}
			}
		}
	}

	public static void saveTagState() {

		if (_maxRecentActions > 0) {

			final String[] tagIds = new String[Math.min(_maxRecentActions, _recentTags.size())];
			int tagIndex = 0;

			for (final TourTag tag : _recentTags) {

				tagIds[tagIndex++] = Long.toString(tag.getTagId());

				if (tagIndex == _maxRecentActions) {
					break;
				}
			}

			_state.put(STATE_RECENT_TAGS, tagIds);
		}

		if (_allPreviousTags.size() > 0) {

			final String[] tagIds = new String[_allPreviousTags.size()];
			int tagIndex = 0;

			for (final TourTag tag : _allPreviousTags.values()) {
				tagIds[tagIndex++] = Long.toString(tag.getTagId());
			}

			_state.put(STATE_PREVIOUS_TAGS, tagIds);
		}
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

		_actionAllPreviousTags = new ActionAllPreviousTags();
	}

	static void updatePreviousTagState(final HashMap<Long, TourTag> modifiedTags) {

		// check if all previous tags are contained in the modified tags
		for (final TourTag previousTag : _allPreviousTags.values()) {
			if (modifiedTags.containsKey(previousTag.getTagId()) == false) {
				_actionAllPreviousTags.setChecked(false);
				return;
			}
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

		for (final TourTag previousTag : _allPreviousTags.values()) {

			final TourTag tourTag = allTourTags.get(previousTag.getTagId());
			if (tourTag != null) {
				previousTag.setTagName(tourTag.getTagName());
			}
		}
	}

	/**
	 * @param isAddTagEnabled
	 * @param isRemoveTagEnabled
	 */
	private void enableTagActions(final boolean isAddTagEnabled, final boolean isRemoveTagEnabled) {

		_currentInstance = this;

		final ActionAddTourTag actionAddTagAdvanced = (ActionAddTourTag) _actionAddTagAdvanced.getAction();

		actionAddTagAdvanced.setEnabled(isAddTagEnabled);
		_actionAddTag.setEnabled(isAddTagEnabled);

		_actionRemoveTag.setEnabled(isRemoveTagEnabled);
		_actionRemoveAllTags.setEnabled(isRemoveTagEnabled);

		enableRecentTagActions(isAddTagEnabled, _allTourTagIds);
	}

	/**
	 * Add/remove tags
	 * 
	 * @param isTourSelected
	 *            Is <code>true</code> when at least one tour is selected
	 * @param isOneTour
	 *            Is <code>true</code> when one single tour is selected
	 * @param oneTourTagIds
	 *            Contains {@link TourTag} ids when one tour is selected
	 */
	public void enableTagActions(	final boolean isTourSelected,
									final boolean isOneTour,
									final ArrayList<Long> oneTourTagIds) {

		final boolean isAddTagEnabled = isTourSelected;
		final boolean isRemoveTagEnabled;
		final HashSet<Long> allTourTagIds = new HashSet<Long>();

		if (isOneTour) {

			// one tour is selected

			if (oneTourTagIds != null && oneTourTagIds.size() > 0) {

				// at least one tag is within the tour

				isRemoveTagEnabled = true;

				if (oneTourTagIds != null) {
					for (final Long tagId : oneTourTagIds) {
						allTourTagIds.add(tagId);
					}
				}

			} else {

				// tags are not available

				isRemoveTagEnabled = false;
			}
		} else {

			// multiple tours are selected

			isRemoveTagEnabled = isTourSelected;
		}

		_isEnableRecentTagActions = isAddTagEnabled;
		_allTourTagIds = allTourTagIds;

		enableTagActions(isAddTagEnabled, isRemoveTagEnabled);
	}

	/**
	 * @param isAddTagEnabled
	 * @param isRemoveTagEnabled
	 * @param tourTags
	 */
	public void enableTagActions(	final boolean isAddTagEnabled,
									final boolean isRemoveTagEnabled,
									final Set<TourTag> tourTags) {

		// get tag id's from all tags
		final HashSet<Long> allExistingTagIds = new HashSet<Long>();
		if (tourTags != null) {
			for (final TourTag tourTag : tourTags) {
				allExistingTagIds.add(tourTag.getTagId());
			}
		}

		_isEnableRecentTagActions = isAddTagEnabled;
		_allTourTagIds = allExistingTagIds;

		enableTagActions(isAddTagEnabled, isRemoveTagEnabled);
	}

	void fillMenuWithRecentTags(final IMenuManager menuMgr, final Menu menu) {

		if (_recentTags.size() == 0) {
			return;
		}

		if (_maxRecentActions < 1) {
			return;
		}

		// add all previous tags
		if (_allPreviousTags.size() > 0) {

			final Collection<TourTag> allPreviousTags = _allPreviousTags.values();

			// check if the first previous tag is the same as the first recent tag
			if (_allPreviousTags.size() > 1 || allPreviousTags.iterator().next().equals(_recentTags.get(0)) == false) {

				final StringBuilder sb = new StringBuilder();
				boolean isFirst = true;

				for (final TourTag recentTag : allPreviousTags) {
					if (isFirst) {
						isFirst = false;
					} else {
						sb.append(UI.COMMA_SPACE);
					}
					sb.append(recentTag.getTagName());
				}

				if (menu == null) {

					_actionAllPreviousTags.setText(UI.SPACE4 + UI.MNEMONIC + 0 + UI.SPACE2 + sb.toString());
					menuMgr.add(new ActionContributionItem(_actionAllPreviousTags));

				} else {

					_actionAllPreviousTags.setText(UI.MNEMONIC + 0 + UI.SPACE2 + sb.toString());
					new ActionContributionItem(_actionAllPreviousTags).fill(menu, -1);
				}
			}
		}

		// add all recent tags
		int tagIndex = 0;
		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
			try {

				final TourTag tag = _recentTags.get(tagIndex);

				if (menu == null) {

					actionRecentTag.setupTagAction(
							tag,
							(UI.SPACE4 + UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getTagName()));
					menuMgr.add(new ActionContributionItem(actionRecentTag));

				} else {

					actionRecentTag.setupTagAction(//
							tag,
							(UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getTagName()));
					new ActionContributionItem(actionRecentTag).fill(menu, -1);
				}

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
		{
			menuMgr.add(_actionAddTagAdvanced);
			menuMgr.add(_actionAddTag);

			fillMenuWithRecentTags(menuMgr, null);

			menuMgr.add(_actionRemoveTag);
			menuMgr.add(_actionRemoveAllTags);
		}

		_isAdvMenu = false;
	}

	ITourProvider getTourProvider() {
		return _tourProvider;
	}

	/**
	 * This is called when the menu is hidden which contains the tag actions.
	 */
	public void onHideMenu() {
		_advancedMenuToAddTags.onHideParentMenu();
	}

	/**
	 * This is called when the menu is displayed which contains the tag actions.
	 * 
	 * @param menuEvent
	 * @param menuParentControl
	 * @param menuPosition
	 */
	public void onShowMenu(final MenuEvent menuEvent, final Control menuParentControl, final Point menuPosition) {

		_advancedMenuToAddTags.onShowParentMenu(//
				menuEvent,
				menuParentControl,
				_isTaggingAutoOpen,
				_isTaggingAnimation,
				_taggingAutoOpenDelay,
				menuPosition);
	}

	private void runnableRemoveAllTags() {

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
	 * Set/Save for multiple tour tags
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

				// update recent tags
				for (final TourTag tag : tagCollection) {
					_recentTags.remove(tag);
					_recentTags.addFirst(tag);
				}

				// it's possible that both hash maps are the same when previous tags has been added as last
				if (_allPreviousTags != modifiedTags) {
					_allPreviousTags.clear();
					_allPreviousTags.putAll(modifiedTags);
				}

				saveAndNotify(modifiedTags, modifiedTours);
			}
		};

		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

	/**
	 * Set/Save one tour tag
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

	void setIsAdvanceMenu() {
		_isAdvMenu = true;
	}
}
