/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.map2.Messages;
import net.tourbook.map3.ui.DialogTourTrackConfig;
import net.tourbook.map3.view.Map3View;

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

public class ActionShowTourInMap3 extends ContributionItem {

	private static final String		ID	= "ACTION_TRACK_LAYER_PROPERTIES_ID";	//$NON-NLS-1$

	private Map3View				_map3View;

	private DialogTourTrackConfig	_tourTrackConfigDialog;

	private ToolBar					_toolBar;
	private ToolItem				_actionTrackLayer;

	private boolean					_isActionEnabled;
	private boolean					_isActionSelected;

	/*
	 * UI controls
	 */
	private Control					_parent;

	/*
	 * UI resources
	 */
	private Image					_actionImage;
	private Image					_actionImageDisabled;

	public ActionShowTourInMap3(final Map3View map3View, final Control parent) {

		super(ID);

		_map3View = map3View;

		_parent = parent;

		_actionImage = TourbookPlugin//
				.getImageDescriptor(Messages.image_action_show_tour_in_map)
				.createImage();
		_actionImageDisabled = TourbookPlugin//
				.getImageDescriptor(Messages.image_action_show_tour_in_map_disabled)
				.createImage();
	}

	public void closeDialog() {

		_tourTrackConfigDialog.hideNow();
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionTrackLayer == null && toolbar != null) {

			// action is not yet created

			_toolBar = toolbar;

			toolbar.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(final DisposeEvent e) {
					onDispose();
				}
			});

			toolbar.addMouseMoveListener(new MouseMoveListener() {
				public void mouseMove(final MouseEvent e) {

					final Point mousePosition = new Point(e.x, e.y);
					final ToolItem hoveredItem = toolbar.getItem(mousePosition);

					onMouseMove(hoveredItem, e);
				}
			});

			_actionTrackLayer = new ToolItem(toolbar, SWT.CHECK);

			// !!! image must be set before enable state is set
			_actionTrackLayer.setImage(_actionImage);
			_actionTrackLayer.setDisabledImage(_actionImageDisabled);

			_actionTrackLayer.setSelection(_isActionSelected);
			_actionTrackLayer.setEnabled(_isActionEnabled);

			_actionTrackLayer.setToolTipText(Messages.map_action_show_tour_in_map);

			_actionTrackLayer.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelect();
				}
			});

			_tourTrackConfigDialog = new DialogTourTrackConfig(_parent, _toolBar, _map3View);

			updateUI_Tooltip();
		}
	}

	private void onDispose() {

		_actionImage.dispose();
		_actionImageDisabled.dispose();

		_actionTrackLayer.dispose();
		_actionTrackLayer = null;
	}

	private void onMouseMove(final ToolItem hoveredItem, final MouseEvent mouseEvent) {

		if (_actionTrackLayer.getSelection() == false || _actionTrackLayer.isEnabled() == false) {

			// tour track is not displayed is not active

			return;
		}

		final boolean isToolItemHovered = hoveredItem == _actionTrackLayer;

		Rectangle itemBounds = null;

		if (isToolItemHovered) {

			itemBounds = hoveredItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openConfigDialog(itemBounds, true);
		}
	}

	private void onSelect() {

		updateUI_Tooltip();

		final boolean isTrackVisible = _actionTrackLayer.getSelection();

		/*
		 * show/hide tour track properties
		 */
		if (isTrackVisible) {

			final Rectangle itemBounds = _actionTrackLayer.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openConfigDialog(itemBounds, false);

		} else {

			_tourTrackConfigDialog.close();
		}

		_map3View.actionShowTour(isTrackVisible);
	}

	private void openConfigDialog(final Rectangle itemBounds, final boolean isOpenDelayed) {

		// ensure other dialogs are closed
		_map3View.closeAllColorSelectDialogs();

		_tourTrackConfigDialog.open(itemBounds, isOpenDelayed);
	}

	/**
	 * Set enable/disable and selection for this action.
	 * 
	 * @param isSelected
	 * @param isEnabled
	 */
	public void setState(final boolean isSelected, final boolean isEnabled) {

		if (_actionTrackLayer == null) {

			_isActionEnabled = isEnabled;
			_isActionSelected = isSelected;

		} else {

			_actionTrackLayer.setSelection(isSelected);
			_actionTrackLayer.setEnabled(isEnabled);
		}

		updateUI_Tooltip();
	}

	public void updateMeasurementSystem() {

		if (_tourTrackConfigDialog == null) {
			return;
		}

		_tourTrackConfigDialog.updateMeasurementSystem();
	}

	private void updateUI_Tooltip() {

		if (_actionTrackLayer.getSelection()) {

			// hide tooltip because the track properties dialog is displayed

			_actionTrackLayer.setToolTipText(UI.EMPTY_STRING);

		} else {

			_actionTrackLayer.setToolTipText(Messages.map_action_show_tour_in_map);
		}
	}

}
