/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.IOpeningDialog;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ActionCollateTours extends ContributionItem implements IOpeningDialog {

	private static final String		IMAGE_TOUR_INFO				= Messages.Image__TourBook_CollatedTours;
	private static final String		IMAGE_TOUR_INFO_DISABLED	= Messages.Image__TourBook_CollatedTours_Disabled;

	private static final String		ID							= "net.tourbook.ui.views.tourBook.ActionCollateTours";	//$NON-NLS-1$

	private IDialogSettings			_state						= TourbookPlugin.getState(ID);
	private String					_dialogId					= getClass().getCanonicalName();

	private TourBookView			_tourBookView;

	private ToolBar					_toolBar;
	private ToolItem				_actionToolItem;

	private SlideoutCollateTours	_slideoutCollateTours;

	/*
	 * UI controls
	 */
	private Control					_parent;

	private Image					_imageEnabled;
	private Image					_imageDisabled;

	public ActionCollateTours(final TourBookView tourBookView, final Control parent) {

		_tourBookView = tourBookView;
		_parent = parent;

		_imageEnabled = TourbookPlugin.getImageDescriptor(IMAGE_TOUR_INFO).createImage();
		_imageDisabled = TourbookPlugin.getImageDescriptor(IMAGE_TOUR_INFO_DISABLED).createImage();
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionToolItem == null && toolbar != null) {

			toolbar.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent e) {
					_actionToolItem.dispose();
					_actionToolItem = null;
				}
			});

			_toolBar = toolbar;

			_actionToolItem = new ToolItem(toolbar, SWT.CHECK);
			_actionToolItem.setImage(_imageEnabled);
			_actionToolItem.setDisabledImage(_imageDisabled);
			_actionToolItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAction();
				}
			});

			toolbar.addMouseMoveListener(new MouseMoveListener() {
				@Override
				public void mouseMove(final MouseEvent e) {

					final Point mousePosition = new Point(e.x, e.y);
					final ToolItem hoveredItem = toolbar.getItem(mousePosition);

					onMouseMove(hoveredItem, e);
				}
			});

			_slideoutCollateTours = new SlideoutCollateTours(_parent, _toolBar, _state, _tourBookView);

			updateUI();
		}
	}

	@Override
	public String getDialogId() {
		return _dialogId;
	}

	@Override
	public void hideDialog() {
		_slideoutCollateTours.hideNow();
	}

	private void onAction() {

		updateUI();

		final boolean isTourInfoVisible = _actionToolItem.getSelection();

		if (isTourInfoVisible) {

			final Rectangle itemBounds = _actionToolItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openSlideout(itemBounds, false);

		} else {

			_slideoutCollateTours.close();
		}

		_tourBookView.actionSelectViewType();
	}

	private void onMouseMove(final ToolItem item, final MouseEvent mouseEvent) {

		// ignore other items
		if (item != _actionToolItem) {
			return;
		}

		if (_actionToolItem.getSelection() == false || _actionToolItem.isEnabled() == false) {

			// marker is not displayed

			return;
		}

		final boolean isToolItemHovered = item == _actionToolItem;

		Rectangle itemBounds = null;

		if (isToolItemHovered) {

			itemBounds = item.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;
		}

		openSlideout(itemBounds, true);
	}

	private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

		// ensure other dialogs are closed
		_tourBookView.closeOpenedDialogs(this);

		_slideoutCollateTours.open(itemBounds, isOpenDelayed);
	}

	public void setEnabled(final boolean isEnabled) {

		_actionToolItem.setEnabled(isEnabled);

		if (isEnabled && _actionToolItem.getSelection() == false) {

			// show default icon
			_actionToolItem.setImage(_imageEnabled);
		}
	}

	public void setSelected(final boolean isSelected) {

		if (_actionToolItem == null) {
			// this happened
			return;
		}

		_actionToolItem.setSelection(isSelected);

		updateUI();
	}

	private void updateUI() {

		if (_actionToolItem.getSelection()) {

			// hide tooltip because the tour info options slideout is displayed

			_actionToolItem.setToolTipText(UI.EMPTY_STRING);

		} else {

			_actionToolItem.setToolTipText(Messages.Action_TourBook_CollateTours);
		}
	}
}
