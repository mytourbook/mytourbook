/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.action;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.map25.Map25View;
import net.tourbook.map3.Messages;
import net.tourbook.map3.ui.DialogMap3Layer;

import org.eclipse.jface.action.ContributionItem;
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

public class ActionShowMap25Layer extends ContributionItem implements IOpeningDialog {

	private Map25View		_map25View;

	private DialogMap3Layer	_map3LayerDialog;

	private ToolBar			_toolBar;
	private ToolItem		_actionToolItem;

	private String			_dialogId	= getClass().getCanonicalName();

	/*
	 * UI controls
	 */
	private Control			_parent;

	/*
	 * UI resources
	 */
	private Image			_actionImage;

	public ActionShowMap25Layer(final Map25View map25View, final Control parent) {

		_map25View = map25View;

		_parent = parent;

      _actionImage = TourbookPlugin.getImageDescriptor(Images.Map3_Map3PropertiesView).createImage();
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionToolItem == null && toolbar != null) {

			// action is not yet created

			_toolBar = toolbar;

			toolbar.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(final DisposeEvent e) {
					onDispose();
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

			_actionToolItem = new ToolItem(toolbar, SWT.PUSH);

			// !!! image must be set before enable state is set
			_actionToolItem.setImage(_actionImage);

			_actionToolItem.setToolTipText(Messages.Map3_Action_OpenMap3PropertiesView);

			_actionToolItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelect();
				}
			});

			_map3LayerDialog = new DialogMap3Layer(_parent, _toolBar);

			updateUI_Tooltip();
		}
	}

	@Override
	public String getDialogId() {
		return _dialogId;
	}

	@Override
	public void hideDialog() {
		_map3LayerDialog.hideNow();
	}

	private void onDispose() {

		_actionImage.dispose();

		_actionToolItem.dispose();
		_actionToolItem = null;
	}

	private void onMouseMove(final ToolItem hoveredItem, final MouseEvent mouseEvent) {

		final boolean isToolItemHovered = hoveredItem == _actionToolItem;

		Rectangle itemBounds = null;

		if (isToolItemHovered) {

			itemBounds = hoveredItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openLayerDialog(itemBounds, true);
		}
	}

	private void onSelect() {

		updateUI_Tooltip();

		if (_map3LayerDialog.isToolTipVisible() == false) {

			final Rectangle itemBounds = _actionToolItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openLayerDialog(itemBounds, false);

		} else {

			_map3LayerDialog.close();
		}
	}

	private void openLayerDialog(final Rectangle itemBounds, final boolean isOpenDelayed) {

		// ensure other dialogs are closed
		_map25View.closeOpenedDialogs(this);

		_map3LayerDialog.open(itemBounds, isOpenDelayed);
	}

	private void updateUI_Tooltip() {

		if (_actionToolItem.getSelection()) {

			// hide tooltip because the track properties dialog is displayed

			_actionToolItem.setToolTipText(UI.EMPTY_STRING);

		} else {

			_actionToolItem.setToolTipText(Messages.Map3_Action_OpenMap3PropertiesView);
		}
	}

}
