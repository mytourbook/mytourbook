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
package net.tourbook.util;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TypedListener;

/**
 * Opens a submenu automatically when a menu item is hovered. This is done with the Arm listener of
 * the parent menu.
 */
public class AdvancedMenuForActions {

	private final ContextArmListener	_contextArmListener	= new ContextArmListener();

	private long						_armActionItemTime;
	private long						_armOtherItemTime;

	private ActionContributionItem		_actionContributionItem;

	private Control						_menuParentControl;
	private Point						_advMenuPosition;

	private boolean						_isAdvMenuOpen		= false;

	private boolean						_isAutoOpen			= false;
	private int							_autoOpenDelay		= 500;

	private int							_armOpenCounter;

	private class ContextArmListener implements ArmListener {

		@Override
		public void widgetArmed(final ArmEvent event) {
			onArmEvent(event);
		}
	}

	/**
	 * @param actionContributionItem
	 */
	public AdvancedMenuForActions(final ActionContributionItem actionContributionItem) {

		_actionContributionItem = actionContributionItem;

		final IAction action = actionContributionItem.getAction();
		if (action instanceof IAdvancedMenuForActions) {

			((IAdvancedMenuForActions) action).setAdvancedMenuProvider(this);
		}
	}

	private void onArmEvent(final ArmEvent event) {

		final MenuItem menuItem = (MenuItem) event.widget;

		if (menuItem.isEnabled() == false || _isAutoOpen == false) {
			_armOtherItemTime = event.time & 0xFFFFFFFFL;
			return;
		}

		final Object itemData = menuItem.getData();
		if (itemData instanceof ActionContributionItem) {

			final String itemId = ((ActionContributionItem) itemData).getId();
			final String actionId = _actionContributionItem.getId();

			if (itemId != null && itemId.equals(actionId)) {

				/*
				 * the item is hovered which is associated with the action for the advanced menu
				 */

				_armActionItemTime = event.time & 0xFFFFFFFFL;

				Display.getCurrent().timerExec(_autoOpenDelay, new Runnable() {

					private int	__armOpenCounter	= ++_armOpenCounter;

					public void run() {

						if (_armOpenCounter > __armOpenCounter) {
							// another open is executed
							return;
						}

						onArmEventRunnable();
					}
				});

				return;
			}
		}

		_armOtherItemTime = event.time & 0xFFFFFFFFL;
	}

	private void onArmEventRunnable() {

		if (_isAdvMenuOpen) {
			// action menu is already open
			return;
		}

		// check if a hide event has occured
		if (_armOtherItemTime >= _armActionItemTime) {
			return;
		}

		// hide menu which contains the add tag action

		final Menu parentMenu = _menuParentControl.getMenu();
		if (parentMenu != null && parentMenu.isDisposed() == false) {
			parentMenu.setVisible(false);
		}

		// run async because the hide menu action is also doing cleanup in async mode
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				openAdvancedMenu();
			}
		});
	}

	/**
	 * This is called when the parent menu is displayed. An arm listener is added to each menu item
	 * in the parent menu.
	 * 
	 * @param menuEvent
	 * @param menuParentControl
	 * @param isAutoOpen
	 * @param autoOpenDelay
	 * @param menuPosition
	 */
	public void onShowParentMenu(	final MenuEvent menuEvent,
									final Control menuParentControl,
									final boolean isAutoOpen,
									final int autoOpenDelay,
									final Point menuPosition) {

		_menuParentControl = menuParentControl;
		_isAutoOpen = isAutoOpen;
		_autoOpenDelay = autoOpenDelay;
		_advMenuPosition = menuPosition;

		final Menu menu = (Menu) menuEvent.widget;

		// add arm listener to each menu item
		for (final MenuItem menuItem : menu.getItems()) {

			/*
			 * check if an arm listener is already set
			 */
			final Listener[] itemArmListeners = menuItem.getListeners(SWT.Arm);
			boolean isArmAvailable = false;

			for (final Listener listener : itemArmListeners) {
				if (listener instanceof TypedListener) {
					if (((TypedListener) listener).getEventListener() instanceof ContextArmListener) {
						isArmAvailable = true;
						break;
					}
				}
			}

			if (isArmAvailable == false) {
				menuItem.addArmListener(_contextArmListener);
			}
		}

		// reset data from previous menu
		final IAction action = _actionContributionItem.getAction();
		if (action instanceof IAdvancedMenuForActions) {
			((IAdvancedMenuForActions) action).resetData();
		}
	}

	/**
	 * Opens the menu which is associated with this {@link AdvancedMenuForActions}
	 */
	public void openAdvancedMenu() {

		if (_isAdvMenuOpen) {
			return;
		}

		final IAction action = _actionContributionItem.getAction();
		if (action instanceof IMenuCreator) {

			// create menu

			final Menu actionMenu = ((IMenuCreator) action).getMenu(_menuParentControl);
			if (actionMenu != null && actionMenu.isDisposed() == false) {

				actionMenu.addMenuListener(new MenuListener() {

					@Override
					public void menuHidden(final MenuEvent e) {
						_isAdvMenuOpen = false;
					}

					@Override
					public void menuShown(final MenuEvent e) {

						_isAdvMenuOpen = true;

						final IAction action = _actionContributionItem.getAction();
						if (action instanceof IAdvancedMenuForActions) {
							((IAdvancedMenuForActions) action).onShowMenu();
						}
					}
				});

				actionMenu.setLocation(_advMenuPosition.x, _advMenuPosition.y);
				actionMenu.setVisible(true);
			}
		}
	}

}
