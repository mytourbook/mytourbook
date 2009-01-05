/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 */
public class TagManager {

	public static final String[]		EXPAND_TYPE_NAMES				= {
			Messages.app_action_expand_type_flat,
			Messages.app_action_expand_type_year_day,
			Messages.app_action_expand_type_year_month_day				};

	public static final int[]			EXPAND_TYPES					= {
			TourTag.EXPAND_TYPE_FLAT,
			TourTag.EXPAND_TYPE_YEAR_DAY,
			TourTag.EXPAND_TYPE_YEAR_MONTH_DAY							};

	private static final String			SETTINGS_SECTION_RECENT_TAGS	= "recentTags";				//$NON-NLS-1$
	private static final String			SETTINGS_TAG_ID					= "tagId";						//$NON-NLS-1$

	/**
	 * number of tags which are displayed in the context menu or saved in the dialog settings, it's
	 * set to 9 to have a unique accelerator key
	 */
//	public static final int				MAX_RECENT_TAGS					= 9;
	private static LinkedList<TourTag>	fRecentTags						= new LinkedList<TourTag>();

	private static ActionRecentTag[]	fActionsRecentTags;

	private static ITourProvider		fTourProvider;
	private static boolean				fIsAddMode;

	private static boolean				fIsSaveTour;

	public static class ActionRecentTag extends Action {

		private TourTag	fTag;

		@Override
		public void run() {
			setTagIntoTour(fTag, fTourProvider, fIsAddMode, fIsSaveTour);
		}

		private void setTag(final TourTag tag) {
			fTag = tag;
		}
	}

	/**
	 * Adds the {@link TourTag} to the list of the recently used tags
	 * 
	 * @param tourTag
	 */
	public static void addRecentTag(final TourTag tourTag) {
		fRecentTags.remove(tourTag);
		fRecentTags.addFirst(tourTag);
	}

	public static void enableRecentTagActions(final boolean isEnabled) {

		if (fActionsRecentTags == null) {
			return;
		}

		for (final ActionRecentTag actionRecentTag : fActionsRecentTags) {
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

		if (fActionsRecentTags == null) {

			// create actions for recenct tags

			fActionsRecentTags = new ActionRecentTag[getMaxTags()];
			for (int actionIndex = 0; actionIndex < fActionsRecentTags.length; actionIndex++) {
				fActionsRecentTags[actionIndex] = new ActionRecentTag();
			}
		}

		if (fRecentTags.size() == 0) {
			return;
		}

		fTourProvider = tourProvider;
		fIsAddMode = isAddMode;
		fIsSaveTour = isSaveTour;

		// add separator
		menuMgr.add(new Separator());

		/*
		 * add title menu item
		 */
		final Action titleAction = new Action(Messages.action_tag_recently_used, SWT.NONE) {};
		titleAction.setEnabled(false);
		menuMgr.add(titleAction);

		// add tag's
		int tagIndex = 0;
		for (final ActionRecentTag actionRecentTag : fActionsRecentTags) {
			try {

				final TourTag tag = fRecentTags.get(tagIndex);

				actionRecentTag.setText("&" + (tagIndex + 1) + " " + tag.getTagName()); //$NON-NLS-1$ //$NON-NLS-2$
				actionRecentTag.setTag(tag);

				menuMgr.add(actionRecentTag);

			} catch (final IndexOutOfBoundsException e) {
				// there are no more recent tags
				break;
			}

			tagIndex++;
		}
	}

	private static int getMaxTags() {

		int maxTags = TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS);

		return maxTags = maxTags == 0 ? 3 : maxTags;
	}

	public static ActionRecentTag[] getRecentTagActions() {
		return fActionsRecentTags;
	}

	public static void restoreSettings() {

		final String[] tagIds = new String[Math.min(getMaxTags(), fRecentTags.size())];
		int tagIndex = 0;

		for (final TourTag tag : fRecentTags) {
			tagIds[tagIndex++] = Long.toString(tag.getTagId());
		}

		final String[] savedTagIds = TourbookPlugin.getDefault()
				.getDialogSettingsSection(SETTINGS_SECTION_RECENT_TAGS)
				.getArray(SETTINGS_TAG_ID);

		if (savedTagIds == null) {
			return;
		}

		final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();
		for (final String tagId : savedTagIds) {
			try {
				final TourTag tag = allTags.get(Long.parseLong(tagId));
				if (tag != null) {
					fRecentTags.add(tag);
				}
			} catch (final NumberFormatException e) {
				// ignore
			}
		}
	}

	public static void saveSettings() {

		final int maxTags = getMaxTags();
		final String[] tagIds = new String[Math.min(maxTags, fRecentTags.size())];
		int tagIndex = 0;

		for (final TourTag tag : fRecentTags) {
			tagIds[tagIndex++] = Long.toString(tag.getTagId());

			if (tagIndex == maxTags) {
				break;
			}
		}

		TourbookPlugin.getDefault().getDialogSettingsSection(SETTINGS_SECTION_RECENT_TAGS).put(SETTINGS_TAG_ID, tagIds);
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

				TagManager.addRecentTag(tourTag);

				if (isSaveTour) {

					// save all tours with the removed tags
					final ArrayList<TourData> savedTours = TourManager.saveModifiedTours(selectedTours);
					selectedTours = savedTours;

				} else {

					// tours are not saved but the tour provider must be notified

					TourManager.fireEvent(TourEventId.TOUR_CHANGED,
							new TourEvent(selectedTours));
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

		for (final TourTag recentTag : fRecentTags) {

			final TourTag tourTag = allTourTags.get(recentTag.getTagId());
			if (tourTag != null) {
				recentTag.setTagName(tourTag.getTagName());
			}
		}
	}
}
