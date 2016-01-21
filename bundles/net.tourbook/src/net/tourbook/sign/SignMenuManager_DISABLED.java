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


public class SignMenuManager_DISABLED {

//	private ITourSignSetter			_tourSignSetter;
//
//	private ActionOpenPrefDialog	_actionOpenTourSignPrefs;
//	private ActionRemoveMarkerImage	_actionRemoveMarkerImage;
//
//	private TourSign				_currentTourSign;
//
//	private class ActionRemoveMarkerImage extends Action {
//
//		public ActionRemoveMarkerImage() {
//
//			super(Messages.Action_Sign_RemoveMarkerImage, AS_PUSH_BUTTON);
//		}
//
//		@Override
//		public void run() {
//			_tourSignSetter.removeTourSign();
//		}
//	}
//
//	private class ActionTourSign extends Action {
//
//		private final TourSign	__tourSign;
//
//		public ActionTourSign(final TourSign tourSign) {
//
//			super(tourSign.getSignName(), AS_PUSH_BUTTON);
//
//			__tourSign = tourSign;
//
//			/*
//			 * Disable this action when it contains the current tour sign
//			 */
//			if (_currentTourSign != null && _currentTourSign.getSignId() == tourSign.getSignId()) {
//				setEnabled(false);
//			}
//
//			// set sign image
//			setImageDescriptor(ImageDescriptor.createFromFile(null, tourSign.getImageFilePathName()));
//		}
//
//		@Override
//		public void run() {
//			_tourSignSetter.setTourSign(__tourSign);
//		}
//	}
//
//	private class ActionTourSignCategory extends Action implements IMenuCreator {
//
//		private final TourSignCategory	__signCategory;
//
//		private Menu					__categoryMenu;
//
//		public ActionTourSignCategory(final TourSignCategory signCategory) {
//
//			super(signCategory.getCategoryName(), AS_DROP_DOWN_MENU);
//
//			__signCategory = signCategory;
//
//			setMenuCreator(this);
//		}
//
//		public void dispose() {
//
//			if (__categoryMenu != null) {
//				__categoryMenu.dispose();
//				__categoryMenu = null;
//			}
//		}
//
//		public Menu getMenu(final Control parent) {
//			return null;
//		}
//
//		public Menu getMenu(final Menu parent) {
//
//			dispose();
//
//			__categoryMenu = new Menu(parent);
//
//			// Add listener to repopulate the menu each time
//			__categoryMenu.addMenuListener(new MenuAdapter() {
//				@Override
//				public void menuShown(final MenuEvent e) {
//
//					final Menu menu = (Menu) e.widget;
//
//					// dispose old items
//					final MenuItem[] items = menu.getItems();
//					for (final MenuItem item : items) {
//						item.dispose();
//					}
//
//					final SignCollection signCollection = SignManager.getSignEntries(__signCategory.getCategoryId());
//
//					// add actions
//					fill_SignCategoryActions(signCollection, __categoryMenu);
//					fill_SignActions(signCollection, __categoryMenu);
//				}
//			});
//
//			return __categoryMenu;
//		}
//	}
//
//	public SignMenuManager(final ITourSignSetter tourSignSetter) {
//
//		_tourSignSetter = tourSignSetter;
//
//		createActions();
//	}
//
//	private void addActionToMenu(final Menu menu, final Action action) {
//		new ActionContributionItem(action).fill(menu, -1);
//	}
//
//	private ActionContributionItem contribItem(final Action action) {
//		return new ActionContributionItem(action);
//	}
//
//	private void createActions() {
//
//		_actionOpenTourSignPrefs = new ActionOpenPrefDialog(
//				Messages.Action_Sign_MarkerImagePreferences,
//				PrefPageSigns.ID);
//		_actionRemoveMarkerImage = new ActionRemoveMarkerImage();
//	}
//
//	private void fill_SignActions(final SignCollection signCollection, final IMenuManager menuMgr) {
//
//		// add sign items
//		for (final TourSign menuTourSign : signCollection.tourSigns) {
//			menuMgr.add(new ActionTourSign(menuTourSign));
//		}
//	}
//
//	private void fill_SignActions(final SignCollection signCollection, final Menu menu) {
//
//		// add sign items
//		for (final TourSign menuTourSign : signCollection.tourSigns) {
//			addActionToMenu(menu, new ActionTourSign(menuTourSign));
//		}
//	}
//
//	private void fill_SignCategoryActions(final SignCollection signCollection, final IMenuManager menuMgr) {
//
//		// add category items
//		for (final TourSignCategory tourSignCategory : signCollection.tourSignCategories) {
//			menuMgr.add(new ActionTourSignCategory(tourSignCategory));
//		}
//	}
//
//	private void fill_SignCategoryActions(final SignCollection signCollection, final Menu menu) {
//
//		// add category items
//		for (final TourSignCategory tourSignCategory : signCollection.tourSignCategories) {
//			addActionToMenu(menu, new ActionTourSignCategory(tourSignCategory));
//		}
//	}
//
//	/**
//	 * Fill sign menu into a {@link IMenuManager}.
//	 *
//	 * @param menuMgr
//	 */
//	public void fillSignMenu(final IMenuManager menuMgr) {
//
//		fill_SignCategoryActions(SignManager.getRootSigns(), menuMgr);
//		fill_SignActions(SignManager.getRootSigns(), menuMgr);
//
//		menuMgr.add(new Separator());
//		menuMgr.add(_actionRemoveMarkerImage);
//
//		menuMgr.add(new Separator());
//		menuMgr.add(_actionOpenTourSignPrefs);
//	}
//
//	/**
//	 * Fill sign menu into a {@link Menu} widget.
//	 *
//	 * @param menu
//	 */
//	public void fillSignMenu(final Menu menu) {
//
//		fill_SignCategoryActions(SignManager.getRootSigns(), menu);
//		fill_SignActions(SignManager.getRootSigns(), menu);
//
//		(new Separator()).fill(menu, -1);
//		contribItem(_actionRemoveMarkerImage).fill(menu, -1);
//
//		(new Separator()).fill(menu, -1);
//		contribItem(_actionOpenTourSignPrefs).fill(menu, -1);
//	}
//
//	public void setEnabledRemoveTourSignAction(final boolean isEnabled) {
//
//		_actionRemoveMarkerImage.setEnabled(isEnabled);
//	}
//
//	public void setTourSign(final TourSign tourSign) {
//
//		_currentTourSign = tourSign;
//	}
}
