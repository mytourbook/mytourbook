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
package net.tourbook.ui.views.tourBook;

import java.util.ArrayList;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionYearSubCategorySelect extends Action implements IMenuCreator {

	private TourBookView						_tourViewer;
	private ArrayList<ActionYearSubCategorySet>	_yearSubCategoryActions;
	private Menu								_yearSubCategoryMenu;
	
	private class ActionYearSubCategorySet extends Action {

		private static final String	OSX_SPACER_STRING	= " ";	//$NON-NLS-1$

		private int					_subCategory;

		public ActionYearSubCategorySet(final int tourItemType, final String tourItemTypeName) {

			super(OSX_SPACER_STRING + tourItemTypeName, AS_RADIO_BUTTON);
			
			_subCategory = tourItemType;
		
		}
		
		public int getSubCategory() {
			return _subCategory;
		}

		@Override
		public void run() {
			if (_tourViewer.getYearSub() != _subCategory) {
				_tourViewer.setYearSub(_subCategory);
				_tourViewer.reopenFirstSelectedTour();
			}
		}

	}

	public ActionYearSubCategorySelect(final TourBookView tourViewer) {

		super(Messages.action_tourbook_year_sub, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_tourViewer = tourViewer;

		_yearSubCategoryActions = new ArrayList<ActionYearSubCategorySet>();

		final ActionYearSubCategorySet setToMonth = new ActionYearSubCategorySet(
				TourItem.ITEM_TYPE_MONTH,
				Messages.action_tourbook_year_sub_month);
		_yearSubCategoryActions.add(setToMonth);

		final ActionYearSubCategorySet setToWeek = new ActionYearSubCategorySet(
				TVITourBookItem.ITEM_TYPE_WEEK,
				Messages.action_tourbook_year_sub_week);
		_yearSubCategoryActions.add(setToWeek);
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_yearSubCategoryMenu, -1);
	}

	@Override
	public void dispose() {
		if (_yearSubCategoryMenu != null) {
			_yearSubCategoryMenu.dispose();
			_yearSubCategoryMenu = null;
		}
	}

	@Override
	public Menu getMenu(final Control parent) {
		return null;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		_yearSubCategoryMenu = new Menu(parent);

		for (final ActionYearSubCategorySet setSub : _yearSubCategoryActions) {
			addActionToMenu(setSub);
		}

		return _yearSubCategoryMenu;
	}

	@Override
	public void run() {
		//
	}

	public void setSubCategoryChecked(final int tourItemType) {
		
		for (final ActionYearSubCategorySet setTo : _yearSubCategoryActions) {
			if (setTo.getSubCategory() == tourItemType) {
				setTo.setChecked(true);
			} else {
				setTo.setChecked(false);
			}
		}
	}

}
