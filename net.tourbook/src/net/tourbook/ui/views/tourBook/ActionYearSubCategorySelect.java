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
package net.tourbook.ui.views.tourBook;

import java.util.ArrayList;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionYearSubCategorySelect extends Action implements IMenuCreator {

	private TourBookView						tourViewer;
	private ArrayList<ActionYearSubCategorySet>	yearSubCategoryActions;
	private Menu								yearSubCategoryMenu;
	
	private class ActionYearSubCategorySet extends Action {

		private static final String	OSX_SPACER_STRING	= " ";	//$NON-NLS-1$

		private int					subCategory;

		public ActionYearSubCategorySet(final int tourItemType, final String tourItemTypeName) {

			super(OSX_SPACER_STRING + tourItemTypeName, AS_RADIO_BUTTON);
			
			subCategory = tourItemType;
		
		}
		
		public int getSubCategory() {
			return subCategory;
		}

		@Override
		public void run() {
			if (tourViewer.getYearSub() != subCategory) {
				tourViewer.setYearSub(subCategory);
				tourViewer.reopenFirstSelectedTour();
			}
		}

	}

	public ActionYearSubCategorySelect(final TourBookView tourViewer) {

		super("Year Sub Categories", AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		yearSubCategoryActions = new ArrayList<ActionYearSubCategorySet>();

		this.tourViewer = tourViewer;

		final ActionYearSubCategorySet setToMonth = new ActionYearSubCategorySet(
				TourItem.ITEM_TYPE_MONTH,
				Messages.action_tourbook_year_sub_month);
		yearSubCategoryActions.add(setToMonth);

		final ActionYearSubCategorySet setToWeek = new ActionYearSubCategorySet(
				TVITourBookItem.ITEM_TYPE_WEEK,
				Messages.action_tourbook_year_sub_week);
		yearSubCategoryActions.add(setToWeek);

		setText(Messages.action_tourbook_year_sub);
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(yearSubCategoryMenu, -1);
	}

	@Override
	public void dispose() {
		if (yearSubCategoryMenu != null) {
			yearSubCategoryMenu.dispose();
			yearSubCategoryMenu = null;
		}
	}

	@Override
	public Menu getMenu(final Control parent) {
		return null;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		yearSubCategoryMenu = new Menu(parent);

		for (final ActionYearSubCategorySet setSub : yearSubCategoryActions) {
			addActionToMenu(setSub);
		}

		return yearSubCategoryMenu;
	}

	@Override
	public void run() {
		//
	}

	public void setSubCategoryChecked(final int tourItemType) {
		
		for (final ActionYearSubCategorySet setTo : yearSubCategoryActions) {
			if (setTo.getSubCategory() == tourItemType) {
				setTo.setChecked(true);
			} else {
				setTo.setChecked(false);
			}
		}
	}

}
