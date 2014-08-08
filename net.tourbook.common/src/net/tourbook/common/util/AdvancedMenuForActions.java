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
package net.tourbook.common.util;

import net.tourbook.common.UI;

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

	/**
	 * Contains time when the action menu item is hovered
	 */
	private long						_armItemTimeAction;

	/**
	 * Contains time when other menu items are hovered
	 */
	private long						_armItemTimeOther;

	private ActionContributionItem		_actionContributionItem;

	private Control						_menuParentControl;
	private Point						_advMenuPosition;

	private boolean						_isAdvMenuOpen		= false;
	private boolean						_isAutoOpen			= false;
	private boolean						_isAnimationEnabled	= false;
	private boolean						_isAnimating;

	private int							_autoOpenDelay		= 500;
	private final int					_animationDelay		= 50;						//180; this is the same time as in BusyIndicator
	private int							_consumedAnimationTime;

	private Display						_display;
	private Runnable					_animationRunnable;
	private MenuItem					_armMenuItem;
	private String						_armActionText;

	private ToolTip						_toolTip;

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

		_display = Display.getCurrent();
		_animationRunnable = new Runnable() {
			@Override
			public void run() {
				onAnimation20Run(this);
			}
		};

	}

	private synchronized void onAnimation10Start() {

		if (_isAnimating) {
			return;
		}

		_isAnimating = true;

		_consumedAnimationTime = 0;

		_display.asyncExec(_animationRunnable);
	}

	private void onAnimation20Run(final Runnable runnable) {

		if (_armMenuItem == null) {
			return;
		}
		_consumedAnimationTime += _animationDelay;

		if (_consumedAnimationTime >= _autoOpenDelay) {

			_isAnimating = false;

			onArmEventOpenMenu();

		} else {

			if (_isAnimationEnabled) {

				int animationTime = _consumedAnimationTime;

				final StringBuilder sb = new StringBuilder();
				while (animationTime > 0) {
					sb.append(Messages.Advanced_Menu_AnimationSymbol);
					animationTime -= _animationDelay;
				}

				_armMenuItem.setText(_armActionText + UI.SPACE + sb.toString());
			}

			_display.timerExec(_animationDelay, runnable);
		}
	}

	private void onArmEvent(final ArmEvent event) {

		final MenuItem menuItem = (MenuItem) event.widget;

		if (_isAutoOpen && menuItem.isEnabled()) {

			final Object itemData = menuItem.getData();
			if (itemData instanceof ActionContributionItem) {

				final String itemId = ((ActionContributionItem) itemData).getId();
				final String actionId = _actionContributionItem.getId();

				if (itemId != null && itemId.equals(actionId)) {

					/*
					 * the item is hovered which is associated with the action for the advanced menu
					 */

					_armItemTimeAction = System.currentTimeMillis();

					_armMenuItem = menuItem;
					_armActionText = _actionContributionItem.getAction().getText();

					onAnimation10Start();

					return;
				}
			}
		}

		restoreMenuItemText();

		_armMenuItem = null;
		_isAnimating = false;

		// system time is needed because OSX has the same time for all arm events until the menu is closed
		_armItemTimeOther = System.currentTimeMillis();
	}

	private void onArmEventOpenMenu() {

		// it's possible that a tool tip is displayed
		if (_toolTip != null) {
			_toolTip.hide();
		}

		// check if menu is already open
		if (_isAdvMenuOpen) {
			return;
		}

		// check if a hide event has occured
		if (_armItemTimeOther >= _armItemTimeAction) {
			return;
		}

		// hide parent menu
		final Menu parentMenu = _menuParentControl.getMenu();
		if (parentMenu != null && parentMenu.isDisposed() == false) {
			parentMenu.setVisible(false);
		}

		// run async because the hide menu action is also doing cleanup in async mode
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				openAdvancedMenu();
			}
		});
	}

	public void onHideParentMenu() {
		restoreMenuItemText();
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
	 * @param toolTip
	 */
	public void onShowParentMenu(	final MenuEvent menuEvent,
									final Control menuParentControl,
									final boolean isAutoOpen,
									final boolean isAnimationEnabled,
									final int autoOpenDelay,
									final Point menuPosition,
									final ToolTip toolTip) {

		_menuParentControl = menuParentControl;
		_isAutoOpen = isAutoOpen;
		_isAnimationEnabled = isAnimationEnabled;
		_autoOpenDelay = autoOpenDelay;
		_advMenuPosition = menuPosition;
		_toolTip = toolTip;

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

			/*
			 * it happened that the text of the menu item was not reset when the menu was opened
			 * with a mouse click and not automatically
			 */
			if (_armMenuItem != null && _armMenuItem.isDisposed() == false && _armMenuItem == menuItem) {
				_armMenuItem.setText(_armActionText);
			}
		}

		// reset data from previous menu
		final IAction action = _actionContributionItem.getAction();
		if (action instanceof IAdvancedMenuForActions) {
			((IAdvancedMenuForActions) action).resetData();
		}
	}

	/**
	 * Open menu which is associated with this {@link AdvancedMenuForActions}
	 */
	public void openAdvancedMenu() {

		// it's possible that a tool tip is displayed
		if (_toolTip != null) {
			_toolTip.hide();
		}

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
						_isAnimating = false;

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

	private void restoreMenuItemText() {

		if (_armMenuItem != null && _armMenuItem.isDisposed() == false) {
			_armMenuItem.setText(_armActionText);
		}
	}

}
