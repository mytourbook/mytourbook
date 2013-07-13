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
package net.tourbook.map3.view;

import net.tourbook.common.UI;
import net.tourbook.map3.Activator;

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

public class ActionTourTrackProperties extends ContributionItem {

	private static final String		ID	= "ACTION_TRACK_LAYER_PROPERTIES_ID";	//$NON-NLS-1$

//	private static final String		STATE_IS_TRACK_LAYER_VISIBLE	= "STATE_IS_TRACK_LAYER_VISIBLE";		//$NON-NLS-1$

	private Map3View				_map3View;

	private TourTrackPropertiesUI	_trackLayerProperties;

	private ToolBar					_toolBar;

	private ToolItem				_actionTrackLayer;
	private IDialogSettings			_state;

	/*
	 * UI controls
	 */
	private Control					_parent;

	private Image					_actionImage;
	private Image					_actionImageDisabled;

	public ActionTourTrackProperties(final Map3View map3View, final Control parent, final IDialogSettings state) {

		super(ID);

		_map3View = map3View;

		_parent = parent;
		_state = state;

		_actionImage = Activator.getImageDescriptor(Messages.Image_Map3_TourTracks).createImage();
		_actionImageDisabled = Activator.getImageDescriptor(Messages.Image_Map3_TourTracks_Disabled).createImage();
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionTrackLayer == null && toolbar != null) {

			// action is not yet created

			_toolBar = toolbar;

			_actionTrackLayer = new ToolItem(toolbar, SWT.CHECK);

			_actionTrackLayer.setImage(_actionImage);

			_actionTrackLayer.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAction();
				}
			});

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

			_trackLayerProperties = new TourTrackPropertiesUI(_parent, _toolBar, _state);

			// send notifications to the map to update displayed photos
//			_trackLayerProperties.addPropertiesListener(_map3View);
		}
	}

	private void onAction() {

		updateUI();

		final boolean isTrackVisible = _actionTrackLayer.getSelection();

		if (isTrackVisible) {

			final Rectangle itemBounds = _actionTrackLayer.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			_trackLayerProperties.open(itemBounds, false);

		} else {

			_trackLayerProperties.close();
		}

//		_map3View.actionPhotoProperties(isTrackVisible);
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
		}

		_trackLayerProperties.open(itemBounds, true);
	}

	void restoreState() {

		_actionTrackLayer.setSelection(Map3Manager.getTourTrackLayer().isEnabled());

		updateUI();

//		_map3View.actionPhotoProperties(isFilterActive);

		// update AFTER photo filter is activated
//		updateUI_FotoFilterStats();
	}

	void setEnabled(final boolean isEnabled) {

		_actionTrackLayer.setEnabled(isEnabled);
		_actionTrackLayer.setImage(isEnabled ? _actionImage : _actionImageDisabled);
	}

	private void updateUI() {

		if (_actionTrackLayer.getSelection()) {

			// hide tooltip because the photo filter is displayed

			_actionTrackLayer.setToolTipText(UI.EMPTY_STRING);

		} else {

			_actionTrackLayer.setToolTipText(Messages.Map3_Action_OpenTrackLayerProperties_Tooltip);
		}
	}

}
