/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourMarker;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

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

public class ActionTourMarkerFilter extends ContributionItem {

	private IDialogSettings				_state					= TourbookPlugin.getState(//
																		getClass().getSimpleName());

	private ToolBar						_toolBar;
	private ToolItem					_actionToolItem;

	private TourMarkerAllView			_tourMarkerAllView;
	private SlideoutTourMarkerFilter	_slideoutTourMarkerFilter;

	/*
	 * UI controls
	 */
	private Control						_parent;

	private Image						_actionImage;

	public ActionTourMarkerFilter(final TourMarkerAllView tourMarkerAllView, final Control parent) {

		_tourMarkerAllView = tourMarkerAllView;
		_parent = parent;

		_actionImage = TourbookPlugin.getImageDescriptor(Messages.Image__tour_options).createImage();
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionToolItem == null && toolbar != null) {

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

			_actionToolItem = new ToolItem(toolbar, SWT.PUSH);

			// !!! image must be set before enable state is set
			_actionToolItem.setImage(_actionImage);

			_actionToolItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelect();
				}
			});

			_slideoutTourMarkerFilter = new SlideoutTourMarkerFilter(_parent, _toolBar, _state, _tourMarkerAllView);
		}
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

			openSlideout(itemBounds, true);
		}
	}

	private void onSelect() {

		if (_slideoutTourMarkerFilter.isToolTipVisible() == false) {

			final Rectangle itemBounds = _actionToolItem.getBounds();

			final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

			itemBounds.x = itemDisplayPosition.x;
			itemBounds.y = itemDisplayPosition.y;

			openSlideout(itemBounds, false);

		} else {

			_slideoutTourMarkerFilter.close();
		}
	}

	private void openSlideout(final Rectangle itemBounds, final boolean isOpenDelayed) {

		_slideoutTourMarkerFilter.open(itemBounds, isOpenDelayed);
	}

}
