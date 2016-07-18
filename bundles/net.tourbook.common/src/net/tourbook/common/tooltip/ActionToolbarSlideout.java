/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.tooltip;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Action to open a slideout in a toolbar.
 */
public abstract class ActionToolbarSlideout extends ContributionItem implements IOpeningDialog {

	private String			_dialogId	= getClass().getCanonicalName();

	private ToolBar			_toolBar;
	private ToolItem		_actionToolItem;

	private ToolbarSlideout	_slideoutOptions;

	/*
	 * UI controls
	 */
	private Image			_imageEnabled;
	private Image			_imageDisabled;

	public ActionToolbarSlideout() {

		_imageEnabled = CommonActivator.getImageDescriptor(Messages.Image__TourOptions).createImage();
		_imageDisabled = CommonActivator.getImageDescriptor(Messages.Image__TourOptions_Disabled).createImage();
	}

	public ActionToolbarSlideout(final ImageDescriptor actionImage, final ImageDescriptor actionImageDisabled) {

		_imageEnabled = actionImage.createImage();
		_imageDisabled = actionImageDisabled.createImage();
	}

	protected abstract ToolbarSlideout createSlideout(ToolBar toolbar);

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if ((_actionToolItem == null || _actionToolItem.isDisposed()) && toolbar != null) {

			toolbar.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent e) {
					onDispose();
				}
			});

			_toolBar = toolbar;

			_actionToolItem = new ToolItem(toolbar, SWT.PUSH);
			_actionToolItem.setImage(_imageEnabled);
			_actionToolItem.setDisabledImage(_imageDisabled);
			_actionToolItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelect();
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

			_slideoutOptions = createSlideout(toolbar);
		}
	}

	@Override
	public String getDialogId() {
		return _dialogId;
	}

	@Override
	public void hideDialog() {
		_slideoutOptions.hideNow();
	}

	/**
	 * Is called before the slideout is opened, this allows to close other dialogs
	 */
	protected void onBeforeOpenSlideout() {

	}

	private void onDispose() {

		if (_actionToolItem != null) {

			_actionToolItem.dispose();
			_actionToolItem = null;
		}
	}

	private void onMouseMove(final ToolItem hoveredItem, final MouseEvent mouseEvent) {

		// ignore other items in the toolbar
		if (hoveredItem != _actionToolItem) {
			return;
		}

		if (_actionToolItem.isEnabled() == false) {

			// a graph is not displayed

			return;
		}

		final boolean isToolItemHovered = hoveredItem == _actionToolItem;

		Rectangle itemBounds = null;

		if (isToolItemHovered) {

			itemBounds = hoveredItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openSlideout(itemBounds, true);
		}
	}

	private void onSelect() {

		if (_slideoutOptions.isToolTipVisible() == false) {

			final Rectangle itemBounds = _actionToolItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openSlideout(itemBounds, false);

		} else {

			_slideoutOptions.close();
		}
	}

	private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

		// ensure other dialogs are closed
		onBeforeOpenSlideout();

		_slideoutOptions.open(itemBounds, isOpenDelayed);
	}

	public void setEnabled(final boolean isEnabled) {

		if (_actionToolItem == null) {
			// this can occure when the toolbar is not yet fully created
			return;
		}

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
	}
}
