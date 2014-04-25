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
package net.tourbook.sign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.ICommandIds;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.AdvancedMenuForActions;
import net.tourbook.common.util.ToolTip;
import net.tourbook.data.TourData;
import net.tourbook.data.TourSign;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.UI;

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

public class SignMenuManager {

	private static final String				SETTINGS_SECTION_RECENT_TAGS	= "SignManager.RecentSigns";						//$NON-NLS-1$
	private static final String				STATE_RECENT_TAGS				= "tagId";											//$NON-NLS-1$
	private static final String				STATE_PREVIOUS_TAGS				= "";												//$NON-NLS-1$

	private static final IPreferenceStore	_prefStore						= TourbookPlugin.getPrefStore();
	private static final IDialogSettings	_state							= TourbookPlugin
																					.getState(SETTINGS_SECTION_RECENT_TAGS);

	private static IPropertyChangeListener	_prefChangeListener;

	private static SignMenuManager			_currentInstance;
	private static boolean					_isAdvMenu;

	private static ActionRecentSign[]		_actionsRecentSigns;
	private static ActionAllPreviousSigns	_actionAllPreviousSigns;

	/**
	 * number of tags which are displayed in the context menu or saved in the dialog settings, it's
	 * max number is 9 to have a unique accelerator key
	 */
	private static LinkedList<TourSign>		_recentSigns					= new LinkedList<TourSign>();

	/**
	 * Contains all tags which are added by the last add action
	 */
	private static HashMap<Long, TourSign>	_allPreviousSigns				= new HashMap<Long, TourSign>();

	private static int						_maxRecentActions				= -1;

	/**
	 * Contains tag id's for all selected tours
	 */
	private static HashSet<Long>			_allTourSignIds;
	private static boolean					_isEnableRecentSignActions;

	private static int						_taggingAutoOpenDelay;
	private static boolean					_isSigningAutoOpen;
	private static boolean					_isSigningAnimation;

	private boolean							_isSaveTour;
	private ITourProvider					_tourProvider;

	private ActionContributionItem			_actionAddSignAdvanced;
	private ActionAddSign					_actionAddSign;
	private ActionRemoveSign				_actionRemoveSign;
	private ActionRemoveAllSigns			_actionRemoveAllSigns;

	private AdvancedMenuForActions			_advancedMenuToAddSigns;

	/**
	 *
	 */
	private static class ActionAllPreviousSigns extends Action {

		public ActionAllPreviousSigns() {
			super(UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
		}

		@Override
		public void run() {

			if (_isAdvMenu) {

				final ActionAddSign actionAddSignAdvanced = (ActionAddSign) _currentInstance._actionAddSignAdvanced
						.getAction();

				actionAddSignAdvanced.setTourSign(isChecked(), _allPreviousSigns);
			} else {

				_currentInstance.saveTourSigns(_allPreviousSigns, isChecked());
			}
		}
	}

	/**
	 *
	 */
	private static class ActionRecentSign extends Action {

		private TourSign	_tag;

		public ActionRecentSign() {
			super(UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
		}

		@Override
		public void run() {

			final ActionAddSign actionAddSignAdvanced = (ActionAddSign) _currentInstance._actionAddSignAdvanced
					.getAction();

			if (_isAdvMenu) {
				actionAddSignAdvanced.setTourSign(isChecked(), _tag);
			} else {
				_currentInstance.saveTourSigns(_tag, isChecked());
			}
		}

		private void setupSignAction(final TourSign tag, final String tagText) {
			setText(tagText);
			_tag = tag;
		}

		@Override
		public String toString() {
			return "ActionRecentSign [_tag=" + _tag + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	/**
	 * Removes all tags
	 */
	private class ActionRemoveAllSigns extends Action {

		public ActionRemoveAllSigns() {
			super(Messages.action_tag_remove_all, AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				@Override
				public void run() {
					runnableRemoveAllSigns();
				}
			});
		}
	}

	/**
	 * @param tourProvider
	 * @param isSaveTour
	 */
	public SignMenuManager(final ITourProvider tourProvider, final boolean isSaveTour) {

		_tourProvider = tourProvider;
		_isSaveTour = isSaveTour;

		if (_prefChangeListener == null) {
			// static var's are not yet initialized
			addPrefListener();
			setupRecentActions();
			restoreAutoOpen();
		}

		_actionAddSignAdvanced = new ActionContributionItem(new ActionAddSign(this, null));
		_actionAddSignAdvanced.setId(ICommandIds.ACTION_ADD_TAG);

		_actionAddSign = new ActionAddSign(this);
		_actionRemoveSign = new ActionRemoveSign(this);
		_actionRemoveAllSigns = new ActionRemoveAllSigns();

		_advancedMenuToAddSigns = new AdvancedMenuForActions(_actionAddSignAdvanced);

	}

	private static void addPrefListener() {

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			@Override
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

	static void enableRecentSignActions(final boolean isAddSignEnabled, final Set<Long> existingSignIds) {

		if (_actionsRecentSigns == null) {
			return;
		}

		final boolean isExistingSignIds = _allTourSignIds != null && _allTourSignIds.size() > 0;
		for (final ActionRecentSign actionRecentSign : _actionsRecentSigns) {

			final TourSign actionSign = actionRecentSign._tag;
			if (actionSign == null) {
				actionRecentSign.setEnabled(false);
				continue;
			}

			final long recentSignId = actionSign.getSignId();
			boolean isSignEnabled;
			boolean isSignChecked;

			if (isExistingSignIds && _isEnableRecentSignActions) {

				// disable action when it's tag id is contained in allExistingSignIds

				boolean isExistSignId = false;

				for (final long existingSignId : _allTourSignIds) {
					if (recentSignId == existingSignId) {
						isExistSignId = true;
						break;
					}
				}

				isSignEnabled = isExistSignId == false;
				isSignChecked = isExistSignId;

			} else {
				isSignEnabled = _isEnableRecentSignActions;
				isSignChecked = false;
			}

			if (isSignEnabled && existingSignIds.contains(recentSignId)) {
				isSignChecked = true;
			}

			actionRecentSign.setEnabled(isSignEnabled);
			actionRecentSign.setChecked(isSignChecked);
		}

		if (_allPreviousSigns != null && isAddSignEnabled) {

			boolean isSignChecked = true;

			for (final TourSign previousSign : _allPreviousSigns.values()) {
				if (_allTourSignIds.contains(previousSign.getSignId()) == false) {
					isSignChecked = false;
					break;
				}
			}

			_actionAllPreviousSigns.setChecked(isSignChecked);
			_actionAllPreviousSigns.setEnabled(isSignChecked == false);

		} else {

			_actionAllPreviousSigns.setChecked(false);
			_actionAllPreviousSigns.setEnabled(false);
		}

	}

	private static void restoreAutoOpen() {
		_isSigningAutoOpen = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN);
		_isSigningAnimation = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION);
		_taggingAutoOpenDelay = _prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY);
	}

	public static void restoreSignState() {

		final HashMap<Long, TourSign> allSigns = TourDatabase.getAllTourSigns();

		final String[] recentSignIds = _state.getArray(STATE_RECENT_TAGS);
		if (recentSignIds != null) {

			for (final String tagId : recentSignIds) {
				try {
					final TourSign tag = allSigns.get(Long.valueOf(tagId));
					if (tag != null) {
						_recentSigns.add(tag);
					}
				} catch (final NumberFormatException e) {
					// ignore
				}
			}
		}

		final String[] previousSignIds = _state.getArray(STATE_PREVIOUS_TAGS);
		if (previousSignIds != null) {

			for (final String tagId : previousSignIds) {
				try {
					final TourSign tag = allSigns.get(Long.valueOf(tagId));
					if (tag != null) {
						_allPreviousSigns.put(tag.getSignId(), tag);
					}
				} catch (final NumberFormatException e) {
					// ignore
				}
			}
		}
	}

	public static void saveSignState() {

		if (_maxRecentActions > 0) {

			final String[] tagIds = new String[Math.min(_maxRecentActions, _recentSigns.size())];
			int tagIndex = 0;

			for (final TourSign tag : _recentSigns) {

				tagIds[tagIndex++] = Long.toString(tag.getSignId());

				if (tagIndex == _maxRecentActions) {
					break;
				}
			}

			_state.put(STATE_RECENT_TAGS, tagIds);
		}

		if (_allPreviousSigns.size() > 0) {

			final String[] tagIds = new String[_allPreviousSigns.size()];
			int tagIndex = 0;

			for (final TourSign tag : _allPreviousSigns.values()) {
				tagIds[tagIndex++] = Long.toString(tag.getSignId());
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

		_actionsRecentSigns = new ActionRecentSign[_maxRecentActions];

		for (int actionIndex = 0; actionIndex < _actionsRecentSigns.length; actionIndex++) {
			_actionsRecentSigns[actionIndex] = new ActionRecentSign();
		}

		_actionAllPreviousSigns = new ActionAllPreviousSigns();
	}

	static void updatePreviousSignState(final HashMap<Long, TourSign> modifiedSigns) {

		// check if all previous tags are contained in the modified tags
		for (final TourSign previousSign : _allPreviousSigns.values()) {
			if (modifiedSigns.containsKey(previousSign.getSignId()) == false) {
				_actionAllPreviousSigns.setChecked(false);
				return;
			}
		}
	}

	/**
	 * Update names of all recent tags
	 */
	public static void updateRecentSignNames() {

		final HashMap<Long, TourSign> allTourSigns = TourDatabase.getAllTourSigns();

		for (final TourSign recentSign : _recentSigns) {

			final TourSign tourSign = allTourSigns.get(recentSign.getSignId());
			if (tourSign != null) {
				recentSign.setSignName(tourSign.getSignName());
			}
		}

		for (final TourSign previousSign : _allPreviousSigns.values()) {

			final TourSign tourSign = allTourSigns.get(previousSign.getSignId());
			if (tourSign != null) {
				previousSign.setSignName(tourSign.getSignName());
			}
		}
	}

	/**
	 * @param isAddSignEnabled
	 * @param isRemoveSignEnabled
	 */
	private void enableSignActions(final boolean isAddSignEnabled, final boolean isRemoveSignEnabled) {

		_currentInstance = this;

		final ActionAddSign actionAddSignAdvanced = (ActionAddSign) _actionAddSignAdvanced.getAction();

		actionAddSignAdvanced.setEnabled(isAddSignEnabled);
		_actionAddSign.setEnabled(isAddSignEnabled);

		_actionRemoveSign.setEnabled(isRemoveSignEnabled);
		_actionRemoveAllSigns.setEnabled(isRemoveSignEnabled);

		enableRecentSignActions(isAddSignEnabled, _allTourSignIds);
	}

	/**
	 * Add/remove tags
	 * 
	 * @param isTourSelected
	 *            Is <code>true</code> when at least one tour is selected
	 * @param isOneTour
	 *            Is <code>true</code> when one single tour is selected
	 * @param oneTourSignIds
	 *            Contains {@link TourSign} ids when one tour is selected
	 */
	public void enableSignActions(	final boolean isTourSelected,
									final boolean isOneTour,
									final ArrayList<Long> oneTourSignIds) {

		final boolean isAddSignEnabled = isTourSelected;
		final boolean isRemoveSignEnabled;
		final HashSet<Long> allTourSignIds = new HashSet<Long>();

		if (isOneTour) {

			// one tour is selected

			if (oneTourSignIds != null && oneTourSignIds.size() > 0) {

				// at least one tag is within the tour

				isRemoveSignEnabled = true;

				if (oneTourSignIds != null) {
					for (final Long tagId : oneTourSignIds) {
						allTourSignIds.add(tagId);
					}
				}

			} else {

				// tags are not available

				isRemoveSignEnabled = false;
			}
		} else {

			// multiple tours are selected

			isRemoveSignEnabled = isTourSelected;
		}

		_isEnableRecentSignActions = isAddSignEnabled;
		_allTourSignIds = allTourSignIds;

		enableSignActions(isAddSignEnabled, isRemoveSignEnabled);
	}

	/**
	 * @param isAddSignEnabled
	 * @param isRemoveSignEnabled
	 * @param tourSigns
	 */
	public void enableSignActions(	final boolean isAddSignEnabled,
									final boolean isRemoveSignEnabled,
									final Set<TourSign> tourSigns) {

		// get tag id's from all tags
		final HashSet<Long> allExistingSignIds = new HashSet<Long>();
		if (tourSigns != null) {
			for (final TourSign tourSign : tourSigns) {
				allExistingSignIds.add(tourSign.getSignId());
			}
		}

		_isEnableRecentSignActions = isAddSignEnabled;
		_allTourSignIds = allExistingSignIds;

		enableSignActions(isAddSignEnabled, isRemoveSignEnabled);
	}

	void fillMenuWithRecentSigns(final IMenuManager menuMgr, final Menu menu) {

		if (_recentSigns.size() == 0) {
			return;
		}

		if (_maxRecentActions < 1) {
			return;
		}

		// add all previous tags
		if (_allPreviousSigns.size() > 0) {

			final Collection<TourSign> allPreviousSigns = _allPreviousSigns.values();

			// check if the first previous tag is the same as the first recent tag
			if (_allPreviousSigns.size() > 1 || allPreviousSigns.iterator().next().equals(_recentSigns.get(0)) == false) {

				final StringBuilder sb = new StringBuilder();
				boolean isFirst = true;

				for (final TourSign recentSign : allPreviousSigns) {
					if (isFirst) {
						isFirst = false;
					} else {
						sb.append(UI.COMMA_SPACE);
					}
					sb.append(recentSign.getSignName());
				}

				if (menu == null) {

					_actionAllPreviousSigns.setText(UI.SPACE4 + UI.MNEMONIC + 0 + UI.SPACE2 + sb.toString());
					menuMgr.add(new ActionContributionItem(_actionAllPreviousSigns));

				} else {

					_actionAllPreviousSigns.setText(UI.MNEMONIC + 0 + UI.SPACE2 + sb.toString());
					new ActionContributionItem(_actionAllPreviousSigns).fill(menu, -1);
				}
			}
		}

		// add all recent tags
		int tagIndex = 0;
		for (final ActionRecentSign actionRecentSign : _actionsRecentSigns) {
			try {

				final TourSign tag = _recentSigns.get(tagIndex);

				if (menu == null) {

					actionRecentSign.setupSignAction(
							tag,
							(UI.SPACE4 + UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getSignName()));
					menuMgr.add(new ActionContributionItem(actionRecentSign));

				} else {

					actionRecentSign.setupSignAction(//
							tag,
							(UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getSignName()));
					new ActionContributionItem(actionRecentSign).fill(menu, -1);
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
	public void fillSignMenu(final IMenuManager menuMgr) {

		// all all tour tag actions
		menuMgr.add(new Separator());
		{
			menuMgr.add(_actionAddSignAdvanced);
			menuMgr.add(_actionAddSign);

			fillMenuWithRecentSigns(menuMgr, null);

			menuMgr.add(_actionRemoveSign);
			menuMgr.add(_actionRemoveAllSigns);
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
		_advancedMenuToAddSigns.onHideParentMenu();
	}

	/**
	 * This is called when the menu is displayed which contains the tag actions.
	 * 
	 * @param menuEvent
	 * @param menuParentControl
	 * @param menuPosition
	 * @param toolTip
	 */
	public void onShowMenu(	final MenuEvent menuEvent,
							final Control menuParentControl,
							final Point menuPosition,
							final ToolTip toolTip) {

		_advancedMenuToAddSigns.onShowParentMenu(//
				menuEvent,
				menuParentControl,
				_isSigningAutoOpen,
				_isSigningAnimation,
				_taggingAutoOpenDelay,
				menuPosition,
				toolTip);
	}

	private void runnableRemoveAllSigns() {

		// get tours which tour type should be changed
		final ArrayList<TourData> modifiedTours = _tourProvider.getSelectedTours();

		if (modifiedTours == null || modifiedTours.size() == 0) {
			return;
		}

		final HashMap<Long, TourSign> modifiedSigns = new HashMap<Long, TourSign>();

		// remove tag in all tours (without tours from an editor)
		for (final TourData tourData : modifiedTours) {

			// get all tag's which will be removed
			final Set<TourSign> tourSigns = tourData.getTourSigns();

			for (final TourSign tourSign : tourSigns) {
				modifiedSigns.put(tourSign.getSignId(), tourSign);
			}

			// remove all tour tags
			tourSigns.clear();
		}

		saveAndNotify(modifiedSigns, modifiedTours);
	}

	/**
	 * Save modified tours and notify tour provider
	 * 
	 * @param modifiedSigns
	 * @param modifiedTours
	 */
	private void saveAndNotify(final HashMap<Long, TourSign> modifiedSigns, ArrayList<TourData> modifiedTours) {

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

		TourManager.fireEventWithCustomData(TourEventId.NOTIFY_TAG_VIEW, //
				new ChangedSigns(modifiedSigns, modifiedTours, false),
				null);
	}

	/**
	 * Set/Save for multiple tour tags
	 * 
	 * @param modifiedSigns
	 * @param isAddMode
	 */
	void saveTourSigns(final HashMap<Long, TourSign> modifiedSigns, final boolean isAddMode) {

		final Runnable runnable = new Runnable() {

			@Override
			public void run() {

				final ArrayList<TourData> modifiedTours = _tourProvider.getSelectedTours();

				// get tours which tag should be changed
				if (modifiedTours == null || modifiedTours.size() == 0) {
					return;
				}

				final Collection<TourSign> tagCollection = modifiedSigns.values();

				// add the tag into all selected tours
				for (final TourData tourData : modifiedTours) {

					// set tag into tour
					final Set<TourSign> tourSigns = tourData.getTourSigns();

					if (isAddMode) {
						// add tag to the tour
						tourSigns.addAll(tagCollection);
					} else {
						// remove tag from tour
						tourSigns.removeAll(tagCollection);
					}
				}

				// update recent tags
				for (final TourSign tag : tagCollection) {
					_recentSigns.remove(tag);
					_recentSigns.addFirst(tag);
				}

				// it's possible that both hash maps are the same when previous tags has been added as last
				if (_allPreviousSigns != modifiedSigns) {
					_allPreviousSigns.clear();
					_allPreviousSigns.putAll(modifiedSigns);
				}

				saveAndNotify(modifiedSigns, modifiedTours);
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
	void saveTourSigns(final TourSign tag, final boolean isAddMode) {

		final HashMap<Long, TourSign> tags = new HashMap<Long, TourSign>();
		tags.put(tag.getSignId(), tag);

		saveTourSigns(tags, isAddMode);
	}

	void setIsAdvanceMenu() {
		_isAdvMenu = true;
	}
}
