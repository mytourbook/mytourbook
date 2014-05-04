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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.AdvancedMenuForActions;
import net.tourbook.common.util.IAdvancedMenuForActions;
import net.tourbook.data.TourData;
import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Add sign(s) from the selected tours
 */
public class ActionAddSign extends Action implements IMenuCreator, IAdvancedMenuForActions {

	private static final String		SPACE_PRE_TAG		= "   ";							//$NON-NLS-1$

	private SignMenuManager			_signMenuMgr;
	private Menu					_menu;

	/**
	 * contains all signs for all selected tours in the viewer
	 */
	private Set<TourSign>			_selectedTourSigns	= new HashSet<TourSign>();

	private ArrayList<TourData>		_selectedTours;
	/**
	 * Contains all signs which will be added
	 */
	private HashMap<Long, TourSign>	_modifiedSigns		= new HashMap<Long, TourSign>();

	private AdvancedMenuForActions	_advancedMenuProvider;

	private ActionOpenPrefDialog	_actionOpenSignPrefs;

	private ActionOK				_actionOK;
	private Action					_actionAddSignTitle;
	private Action					_actionRecentSignTitle;

	private boolean					_isAdvancedMenu;

	private final class ActionCancel extends Action {

		private ActionCancel() {

			super(Messages.Action_Tag_AutoOpenCancel);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Cancel));
		}

		@Override
		public void run() {
			resetData();
		}
	}

	private class ActionModifiedSign extends Action {

		private final TourSign	__tourSign;

		public ActionModifiedSign(final TourSign tourSign) {

			super(SPACE_PRE_TAG + tourSign.getSignName(), AS_CHECK_BOX);

			__tourSign = tourSign;

			// this sign is always checked, unchecking it will also remove it
			setChecked(true);
		}

		@Override
		public void run() {

			// uncheck/remove this sign
			_modifiedSigns.remove(__tourSign.getSignId());

			// reopen action menu
			_advancedMenuProvider.openAdvancedMenu();
		}
	}

	private class ActionModifiedSigns extends Action {

		public ActionModifiedSigns() {

			super(Messages.Action_Tag_Add_AutoOpen_ModifiedTags);

			// this action is only for info
			setEnabled(false);
		}
	}

	private final class ActionOK extends Action {

		private ActionOK() {

			super(Messages.Action_Tag_AutoOpenOK);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_OK));
		}

		@Override
		public void run() {
			saveSigns();
		}
	}

	private class ActionTourSign extends Action {

		private final TourSign	__tourSign;

		public ActionTourSign(final TourSign tourSign) {

			super(tourSign.getSignName(), AS_CHECK_BOX);

			__tourSign = tourSign;
		}

		@Override
		public void run() {

			if (_isAdvancedMenu == false) {

				// add sign
				_modifiedSigns.put(__tourSign.getSignId(), __tourSign);

				saveSigns();

			} else {

				setTourSign(isChecked(), __tourSign);
			}
		}
	}

	/**
	 * 
	 */
	private class ActionTourSignCategory extends Action implements IMenuCreator {

		private Menu					__categoryMenu;

		private final ActionAddSign		__actionAddTourSign;
		private final TourSignCategory	__signCategory;

		public ActionTourSignCategory(final ActionAddSign actionAddTourSign, final TourSignCategory signCategory) {

			super(signCategory.getCategoryName(), AS_DROP_DOWN_MENU);

			__actionAddTourSign = actionAddTourSign;
			__signCategory = signCategory;

			setMenuCreator(this);
		}

		public void dispose() {
			if (__categoryMenu != null) {
				__categoryMenu.dispose();
				__categoryMenu = null;
			}
		}

		public Menu getMenu(final Control parent) {
			return null;
		}

		public Menu getMenu(final Menu parent) {

			dispose();
			__categoryMenu = new Menu(parent);

			// Add listener to repopulate the menu each time
			__categoryMenu.addMenuListener(new MenuAdapter() {
				@Override
				public void menuShown(final MenuEvent e) {

					final Menu menu = (Menu) e.widget;

					// dispose old items
					final MenuItem[] items = menu.getItems();
					for (final MenuItem item : items) {
						item.dispose();
					}

					final SignCollection signCollection = SignManager.getSignEntries(__signCategory.getCategoryId());

					// add actions
					__actionAddTourSign.createCategoryActions(signCollection, __categoryMenu);
					__actionAddTourSign.createSignActions(signCollection, __categoryMenu);
				}
			});

			return __categoryMenu;
		}
	}

	/**
	 * @param tourProvider
	 * @param isAddMode
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved and a
	 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the {@link TourData}
	 *            from the tour provider is only updated
	 */
	public ActionAddSign(final SignMenuManager signMenuManager) {

		super(Messages.action_tag_add, AS_DROP_DOWN_MENU);

		createDefaultAction(signMenuManager);

		setMenuCreator(this);
	}

	/**
	 * This constructor creates a push button action without a drop down menu
	 * 
	 * @param signMenuMgr
	 * @param isAutoOpen
	 *            This parameter is ignored but it indicates that the menu auto open behaviour is
	 *            used.
	 */
	public ActionAddSign(final SignMenuManager signMenuMgr, final Object isAutoOpen) {

		super(Messages.Action_Tag_Add_AutoOpen, AS_PUSH_BUTTON);

		createDefaultAction(signMenuMgr);
	}

	private void addActionToMenu(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	private void createCategoryActions(final SignCollection signCollection, final Menu menu) {

		// add sign categories
		for (final TourSignCategory signCategory : signCollection.tourSignCategories) {
			addActionToMenu(menu, new ActionTourSignCategory(this, signCategory));
		}
	}

	private void createDefaultAction(final SignMenuManager signMenuMgr) {

		_signMenuMgr = signMenuMgr;

		_actionAddSignTitle = new Action(Messages.Action_Tag_Add_AutoOpen_Title) {};
		_actionAddSignTitle.setEnabled(false);

		_actionRecentSignTitle = new Action(Messages.Action_Tag_Add_RecentTags) {};
		_actionRecentSignTitle.setEnabled(false);

		_actionOK = new ActionOK();

		_actionOpenSignPrefs = new ActionOpenPrefDialog(
				Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);
	}

	private void createSignActions(final SignCollection signCollection, final Menu menu) {

		final ArrayList<TourSign> allTourSigns = signCollection.tourSigns;
		if (allTourSigns == null) {
			return;
		}

		// add sign items
		for (final TourSign menuTourSign : allTourSigns) {

			// check the sign when it's set in the tour
			final ActionTourSign actionTourSign = new ActionTourSign(menuTourSign);

			final boolean isModifiedSigns = _modifiedSigns.size() > 0;
			final boolean isSelectedSigns = _selectedTourSigns != null;

			boolean isSignChecked = false;
			final boolean isOneTour = _selectedTours != null && _selectedTours.size() == 1;
			boolean isModifiedSign = false;

			if (isSelectedSigns && isOneTour) {

				/*
				 * only when one tour is selected check the sign otherwise it's confusing, a
				 * three-state check could solve this problem but is not available
				 */

				final long signId = menuTourSign.getSignId();

				if (isSelectedSigns) {

					for (final TourSign _selectedTourSign : _selectedTourSigns) {
						if (_selectedTourSign.getSignId() == signId) {
							isSignChecked = true;
							break;
						}
					}
				}

			}

			if (isModifiedSigns) {

				if (isSignChecked == false && _modifiedSigns.containsValue(menuTourSign)) {
					isSignChecked = true;
					isModifiedSign = true;
				}

			}

			actionTourSign.setChecked(isSignChecked);

			// disable signs which are not signged
			if (isModifiedSign) {

				// modified signs are always enabled

			} else if (isOneTour) {
				actionTourSign.setEnabled(!isSignChecked);
			}

			addActionToMenu(menu, actionTourSign);
		}
	}

	public void dispose() {

		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	/**
	 * Fill the context menu and check/disable signs for the selected tours
	 * 
	 * @param menu
	 */
	private void fillMenu(final Menu menu) {

		// dispose old items
		final MenuItem[] items = menu.getItems();
		for (final MenuItem item : items) {
			item.dispose();
		}

		// check if a tour is selected
		_selectedTours = _signMenuMgr.getTourProvider().getSelectedTours();
		if (_selectedTours == null || _selectedTours.size() == 0) {
			// a tour is not selected
			return;
		}

		// get all signs for all tours
		_selectedTourSigns.clear();
		for (final TourData tourData : _selectedTours) {
			final Set<TourSign> signs = tourData.getTourSigns();
			if (signs != null) {
				_selectedTourSigns.addAll(signs);
			}
		}

		// add signs, create actions for the root signs

		final SignCollection rootSignCollection = TourDatabase.getRootSigns();

		if (_isAdvancedMenu == false) {

			createCategoryActions(rootSignCollection, menu);
			createSignActions(rootSignCollection, menu);

		} else {

			/*
			 * this action is managed by the advanced menu provider
			 */

			// create title menu items

			addActionToMenu(menu, _actionAddSignTitle);

			(new Separator()).fill(menu, -1);
			{
				createCategoryActions(rootSignCollection, menu);
				createSignActions(rootSignCollection, menu);
			}

			fillRecentSigns(menu);

			final boolean isModifiedSigns = _modifiedSigns.size() > 0;

			(new Separator()).fill(menu, -1);
			{
				// show newly added signs

				addActionToMenu(menu, new ActionModifiedSigns());

				if (isModifiedSigns) {

					// create actions
					final ArrayList<TourSign> modifiedSigns = new ArrayList<TourSign>(_modifiedSigns.values());
					Collections.sort(modifiedSigns);

					for (final TourSign tourSign : modifiedSigns) {
						addActionToMenu(menu, new ActionModifiedSign(tourSign));
					}
				}
			}

			(new Separator()).fill(menu, -1);
			{
				addActionToMenu(menu, _actionOK);
				addActionToMenu(menu, new ActionCancel());

			}
			(new Separator()).fill(menu, -1);
			{
				addActionToMenu(menu, _actionOpenSignPrefs);
			}

			/*
			 * enable actions
			 */
			_actionOK.setEnabled(isModifiedSigns);
			_actionOK.setImageDescriptor(isModifiedSigns
					? TourbookPlugin.getImageDescriptor(Messages.Image__App_OK)
					: null);
		}
	}

	/**
	 * @param menu
	 */
	private void fillRecentSigns(final Menu menu) {

		if ((TourDatabase.getAllTourSigns().size() > 0)) {

			// signs are available

			(new Separator()).fill(menu, -1);
			{
				addActionToMenu(menu, _actionRecentSignTitle);
				_signMenuMgr.fillMenuWithRecentSigns(null, menu);
			}
		}
	}

	public Menu getMenu(final Control parent) {

		_isAdvancedMenu = true;

		dispose();

		_menu = new Menu(parent);

		// Add listener to repopulate the menu each time
		_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {
				fillMenu((Menu) e.widget);
			}
		});

		return _menu;
	}

	public Menu getMenu(final Menu parent) {

		_isAdvancedMenu = false;
		_modifiedSigns.clear();

		dispose();

		_menu = new Menu(parent);

		// Add listener to repopulate the menu each time
		_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {
				fillMenu((Menu) e.widget);
			}
		});

		return _menu;
	}

	@Override
	public void onShowMenu() {

		_signMenuMgr.setIsAdvanceMenu();

		SignMenuManager.enableRecentSignActions(true, _modifiedSigns.keySet());
	}

	@Override
	public void resetData() {
		_modifiedSigns.clear();
	}

	@Override
	public void run() {
		_advancedMenuProvider.openAdvancedMenu();
	}

	private void saveOrReopenSignMenu(final boolean isAddSign) {

		if (_isAdvancedMenu == false) {

			saveSigns();

		} else {

			if (isAddSign == false) {
				/*
				 * it is possible that a sign was remove which is contained within the previous
				 * signs, uncheck this action that is displays the correct checked signs
				 */
				SignMenuManager.updatePreviousSignState(_modifiedSigns);
			}

			// reopen action menu
			_advancedMenuProvider.openAdvancedMenu();
		}
	}

	private void saveSigns() {

		if (_modifiedSigns.size() > 0) {
			_signMenuMgr.saveTourSigns(_modifiedSigns, true);
		}
	}

	@Override
	public void setAdvancedMenuProvider(final AdvancedMenuForActions advancedMenuProvider) {
		_advancedMenuProvider = advancedMenuProvider;
	}

	@Override
	public void setEnabled(final boolean enabled) {

		if (_isAdvancedMenu == false) {

			// ensure signs are available
			final HashMap<Long, TourSign> allSigns = TourDatabase.getAllTourSigns();

			super.setEnabled(enabled && allSigns.size() > 0);

		} else {

			super.setEnabled(enabled);
		}
	}

	/**
	 * Add/Remove all previous signs
	 * 
	 * @param isAddSign
	 * @param _allPreviousSigns
	 */
	void setTourSign(final boolean isAddSign, final HashMap<Long, TourSign> _allPreviousSigns) {

		for (final TourSign tourSign : _allPreviousSigns.values()) {

			if (isAddSign) {
				// add sign
				_modifiedSigns.put(tourSign.getSignId(), tourSign);
			} else {
				// remove sign
				_modifiedSigns.remove(tourSign.getSignId());
			}
		}

		saveOrReopenSignMenu(isAddSign);
	}

	/**
	 * Add or remove a tour sign
	 * 
	 * @param isAddSign
	 * @param tourSign
	 */
	void setTourSign(final boolean isAddSign, final TourSign tourSign) {

		if (isAddSign) {
			// add sign
			_modifiedSigns.put(tourSign.getSignId(), tourSign);
		} else {
			// remove sign
			_modifiedSigns.remove(tourSign.getSignId());
		}

		saveOrReopenSignMenu(isAddSign);
	}

	@Override
	public String toString() {
		return "ActionAddTourSign [getText()=" + getText() + ", hashCode()=" + hashCode() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
