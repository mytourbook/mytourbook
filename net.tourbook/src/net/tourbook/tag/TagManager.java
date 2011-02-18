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
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.IMenuManager;

/**
 */
public class TagManager {

	public static final String[]	EXPAND_TYPE_NAMES	= {
			Messages.app_action_expand_type_flat,
			Messages.app_action_expand_type_year_day,
			Messages.app_action_expand_type_year_month_day };

	public static final int[]		EXPAND_TYPES		= {
			TourTag.EXPAND_TYPE_FLAT,
			TourTag.EXPAND_TYPE_YEAR_DAY,
			TourTag.EXPAND_TYPE_YEAR_MONTH_DAY			};

	public static void enableRecentTagActions(final boolean isEnabled, final ArrayList<Long> allExistingTagIds) {

//		if (_actionsRecentTags == null) {
//			return;
//		}
//
//		final boolean isExistingTagIds = allExistingTagIds != null && allExistingTagIds.size() > 0;
//
//		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
//
//			final TourTag actionTag = actionRecentTag._tag;
//			if (actionTag == null) {
//				actionRecentTag.setEnabled(false);
//				continue;
//			}
//
//			if (isExistingTagIds && isEnabled) {
//
//				// disable action when it's tag id is contained in allExistingTagIds
//
//				boolean isExistTagId = false;
//
//				final long recentTagId = actionTag.getTagId();
//
//				for (final long existingTagId : allExistingTagIds) {
//					if (recentTagId == existingTagId) {
//						isExistTagId = true;
//						break;
//					}
//				}
//
//				actionRecentTag.setEnabled(isExistTagId == false);
//
//			} else {
//				actionRecentTag.setEnabled(isEnabled);
//			}
//		}
	}

	public static void enableRecentTagActions(final boolean isEnabled, final Set<TourTag> allExistingTags) {

//		if (_actionsRecentTags == null) {
//			return;
//		}
//
//		final boolean isExistingTags = allExistingTags != null && allExistingTags.size() > 0;
//
//		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
//
//			final TourTag actionTag = actionRecentTag._tag;
//			if (actionTag == null) {
//				actionRecentTag.setEnabled(false);
//				continue;
//			}
//
//			if (isExistingTags && isEnabled) {
//
//				// disable action when it's tag id is contained in allExistingTags
//
//				boolean isExistTagId = false;
//
//				final long recentTagId = actionTag.getTagId();
//
//				for (final TourTag existingTag : allExistingTags) {
//					if (recentTagId == existingTag.getTagId()) {
//						isExistTagId = true;
//						break;
//					}
//				}
//
//				actionRecentTag.setEnabled(isExistTagId == false);
//
//			} else {
//				actionRecentTag.setEnabled(isEnabled);
//			}
//		}
	}

	/**
	 * Create the menu entries for the recently used tags
	 * 
	 * @param menuMgr
	 * @param isSaveTour
	 */
	public static void fillMenuRecentTags(	final IMenuManager menuMgr,
											final ITourProvider tourProvider,
											final boolean isAddMode,
											final boolean isSaveTour) {

//		if (_recentTags.size() == 0) {
//			return;
//		}
//
//		if (_maxRecentActions < 1) {
//			return;
//		}
//
//		_tourProvider = tourProvider;
//		_actionRecentAddTag = null;
//
//		_isSaveTour = isSaveTour;
//
//		// add tag's
//		int tagIndex = 0;
//		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
//			try {
//
//				final TourTag tag = _recentTags.get(tagIndex);
//
//				actionRecentTag.setTag(tag, (UI.SPACE4 + UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getTagName()));
//
//				menuMgr.add(actionRecentTag);
//
//			} catch (final IndexOutOfBoundsException e) {
//				// there are no more recent tags
//				break;
//			}
//
//			tagIndex++;
//		}
	}








}
