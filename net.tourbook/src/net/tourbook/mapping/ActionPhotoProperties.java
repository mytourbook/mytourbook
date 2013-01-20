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
package net.tourbook.mapping;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.photo.PhotoProperties;

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

public class ActionPhotoProperties extends ContributionItem {

	private static final String	ID								= "ACTION_PHOTO_FILTER_ID";		//$NON-NLS-1$

	private static final String	STATE_IS_PHOTO_FILTER_ACTIVE	= "STATE_IS_PHOTO_FILTER_ACTIVE";	//$NON-NLS-1$

	private TourMapView			_mapView;

	private PhotoProperties		_photoProperties;

	private ToolBar				_toolBar;
	private ToolItem			_actionToolItem;

	private IDialogSettings		_state;

	/*
	 * UI controls
	 */
	private Control				_parent;

	private Image				_imageEnabled;
	private Image				_imageDisabled;

	public ActionPhotoProperties(final TourMapView mapView, final Control parent, final IDialogSettings state) {

		super(ID);

		_mapView = mapView;
		_parent = parent;
		_state = state;

//		_imageEnabled = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_PROPERTIES);
//		_imageDisabled = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_PROPERTIES_DISABLED);
		_imageEnabled = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER);
		_imageDisabled = UI.IMAGE_REGISTRY.get(UI.IMAGE_ACTION_PHOTO_FILTER_DISABLED);
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionToolItem == null && toolbar != null) {

			toolbar.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(final DisposeEvent e) {
					//toolItem.getImage().dispose();
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
				public void mouseMove(final MouseEvent e) {

					final Point mousePosition = new Point(e.x, e.y);

					final ToolItem hoveredItem = toolbar.getItem(mousePosition);

					onMouseMove(hoveredItem, e);
				}
			});

			_photoProperties = new PhotoProperties(_parent, _toolBar, _state);

			// send notifications to the map to update displayed photos
			_photoProperties.addPropertiesListener(_mapView);

			// this is also listening to update filter numbers in the photo properties shell
			// this MUST be called after the map
//			_photoProperties.addPropertiesListener(this);
		}
	}

	private void onAction() {

		updateUI();

		final boolean isFilterActive = _actionToolItem.getSelection();

		if (isFilterActive) {

			final Rectangle itemBounds = _actionToolItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			_photoProperties.open(itemBounds, false);

		} else {

			_photoProperties.close();
		}

		_mapView.actionPhotoProperties(isFilterActive);
	}

	private void onMouseMove(final ToolItem item, final MouseEvent mouseEvent) {

		if (_actionToolItem.getSelection() == false || _actionToolItem.isEnabled() == false) {

			// filter is not active

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

		_photoProperties.open(itemBounds, true);
	}

	void restoreState() {

		final boolean isFilterActive = Util.getStateBoolean(_state, STATE_IS_PHOTO_FILTER_ACTIVE, false);

		_actionToolItem.setSelection(isFilterActive);

		_photoProperties.restoreState();

		updateUI();

		_mapView.actionPhotoProperties(isFilterActive);

		// update AFTER photo filter is activated
//		updateUI_FotoFilterStats();
	}

	void saveState() {

		_state.put(STATE_IS_PHOTO_FILTER_ACTIVE, _actionToolItem.getSelection());

		_photoProperties.saveState();
	}

	void setEnabled(final boolean isEnabled) {

		_actionToolItem.setEnabled(isEnabled);
	}

	private void updateUI() {

		if (_actionToolItem.getSelection()) {

			// hide tooltip because the photo filter is displayed

			_actionToolItem.setToolTipText(UI.EMPTY_STRING);

		} else {

			_actionToolItem.setToolTipText(Messages.Map_Action_PhotoFilter_Tooltip);
		}
	}

}
