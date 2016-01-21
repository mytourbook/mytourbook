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
package net.tourbook.map3.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.map2.Messages;
import net.tourbook.map3.ui.DialogSelectMap3Color;
import net.tourbook.map3.view.Map3View;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ActionTourColor extends ContributionItem implements IOpeningDialog {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private MapGraphId				_graphId;
	private String					_dialogId;
	private boolean					_isGradientColorProvider;

	private Map3View				_map3View;

	private String					_toolTipText;

	private ToolBar					_toolBar;
	private ToolItem				_actionColor;

	private boolean					_isActionEnabled;
	private boolean					_isActionChecked;

	private DialogSelectMap3Color	_colorSelectDialog;

	/*
	 * UI controls
	 */
	private Control					_parent;

	ActionTourColor(final MapGraphId graphId,
					final boolean isGradientColorProvider,
					final Map3View mapView,
					final Control parent,
					final String toolTipText) {

		_graphId = graphId;
		_dialogId = getClass().getCanonicalName() + _graphId.name();

		_isGradientColorProvider = isGradientColorProvider;

		_map3View = mapView;
		_parent = parent;

		_toolTipText = toolTipText;
	}

	/**
	 * Create tour color action, this is done here to separate map2 Messages from map3 Messages.
	 * 
	 * @param colorId
	 * @param map3View
	 * @param parent
	 * @return
	 */
	public static ActionTourColor createAction(final MapGraphId colorId, final Map3View map3View, final Composite parent) {

		switch (colorId) {
		case Altitude:
			return new ActionTourColor(//
					MapGraphId.Altitude,
					true,
					map3View,
					parent,
					Messages.map_action_tour_color_altitude_tooltip);

		case Gradient:
			return new ActionTourColor(//
					MapGraphId.Gradient,
					true,
					map3View,
					parent,
					Messages.map_action_tour_color_gradient_tooltip);

		case Pace:
			return new ActionTourColor(//
					MapGraphId.Pace,
					true,
					map3View,
					parent,
					Messages.map_action_tour_color_pase_tooltip);

		case Pulse:
			return new ActionTourColor(//
					MapGraphId.Pulse,
					true,
					map3View,
					parent,
					Messages.map_action_tour_color_pulse_tooltip);

		case Speed:
			return new ActionTourColor(//
					MapGraphId.Speed,
					true,
					map3View,
					parent,
					Messages.map_action_tour_color_speed_tooltip);

		case HrZone:
			return new ActionTourColor(//
					MapGraphId.HrZone,
					false,
					map3View,
					parent,
					Messages.Tour_Action_ShowHrZones_Tooltip);

		default:
			break;
		}

		return null;
	}

	private boolean canColorSelectorBeDisplayed() {

		return _prefStore.getBoolean(ITourbookPreferences.MAP3_IS_COLOR_SELECTOR_DISPLAYED);
	}

	public void disposeColors() {

		if (_colorSelectDialog != null) {

			// ensure the dialog is recreated when needed

			if (_colorSelectDialog.disposeColors()) {

				/*
				 * When colors are disposed, the dialog needs to be recreated. It's possible that
				 * color providers are added or removed.
				 */

				_colorSelectDialog.close();
			}
		}
	}

	@Override
	public void fill(final ToolBar toolbar, final int index) {

		if (_actionColor == null && toolbar != null) {

			// action is not yet created

			// keep toolbar for tooltip/dialog positioning
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

			_actionColor = new ToolItem(toolbar, SWT.CHECK);

			// !!! image must be set before enable state is set
			_actionColor.setImage(net.tourbook.ui.UI.getGraphImage(_graphId));
			_actionColor.setDisabledImage(net.tourbook.ui.UI.getGraphImageDisabled(_graphId));

			_actionColor.setEnabled(_isActionEnabled);
			_actionColor.setSelection(_isActionChecked);

			_actionColor.setToolTipText(_toolTipText);

			_actionColor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelect();
				}
			});

			if (_isGradientColorProvider) {

				// only gradient color provider can selected in the color selection dialog

				_colorSelectDialog = new DialogSelectMap3Color(_parent, _toolBar, _map3View, _graphId);
			}

			updateUI_Tooltip();
		}
	}

	@Override
	public String getDialogId() {

		return _dialogId;
	}

	public ToolItem getToolItem() {

		return _actionColor;
	}

	@Override
	public void hideDialog() {

		if (_colorSelectDialog != null) {

			_colorSelectDialog.hideNow();

			// dispose dialog for testing only otherwise use hideNow()
//			_colorSelectDialog.close();
		}
	}

	private void onDispose() {

		_actionColor.dispose();
		_actionColor = null;
	}

	private void onMouseMove(final ToolItem hoveredItem, final MouseEvent mouseEvent) {

		if (_colorSelectDialog == null || _actionColor.getSelection() == false || _actionColor.isEnabled() == false) {

			// color is not active

			return;
		}

		if (canColorSelectorBeDisplayed()) {

			final boolean isToolItemHovered = hoveredItem == _actionColor;

			Rectangle itemBounds = null;

			if (isToolItemHovered) {

				itemBounds = hoveredItem.getBounds();

				final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

				itemBounds.x = itemDisplayPosition.x;
				itemBounds.y = itemDisplayPosition.y;

				openDialog(itemBounds, true);
			}
		}
	}

	private void onSelect() {

		if (_colorSelectDialog != null && _colorSelectDialog.isToolTipVisible()) {

			// color select dialog is already open

			// ensure this action is checked
			_actionColor.setSelection(true);

			return;
		}

		// ensure only one color is selected
		_map3View.checkSelectedColorActions(_graphId, _actionColor);

		updateUI_Tooltip();

		// set color in map
		_map3View.actionSetMapColor(_graphId);

		if (canColorSelectorBeDisplayed()) {

			// show drop down color select dialog

			if (_colorSelectDialog == null) {

				// even when a select dialog is not available, close other dialogs also

				_map3View.closeOpenedDialogs(this);

			} else {

				final boolean isColorSelected = _actionColor.getSelection();

				// Show/hide color dialog
				if (isColorSelected) {

					final Rectangle itemBounds = _actionColor.getBounds();

					final Point itemDisplayPosition = _toolBar.toDisplay(itemBounds.x, itemBounds.y);

					itemBounds.x = itemDisplayPosition.x;
					itemBounds.y = itemDisplayPosition.y;

					openDialog(itemBounds, false);

				} else {

					_colorSelectDialog.close();
				}
			}
		}
	}

	private void openDialog(final Rectangle itemBounds, final boolean isOpenDelayed) {

		// ensure other dialogs are closed
		_map3View.closeOpenedDialogs(this);

		_colorSelectDialog.open(itemBounds, isOpenDelayed);
	}

	/**
	 * Set enable/disable and selection for this action.
	 * 
	 * @param isChecked
	 * @param isEnabled
	 */
	public void setChecked(final boolean isChecked) {

		if (_actionColor == null) {

			_isActionChecked = isChecked;

		} else {

			_actionColor.setSelection(isChecked);
		}

		updateUI_Tooltip();
	}

	/**
	 * Set enable/disable for this action.
	 * 
	 * @param isEnabled
	 */
	public void setEnabled(final boolean isEnabled) {

		if (_actionColor == null) {

			_isActionEnabled = isEnabled;

		} else {

			_actionColor.setEnabled(isEnabled);
		}

		updateUI_Tooltip();
	}

	@Override
	public String toString() {
		return String.format("ActionTourColor [_graphId=%s]", _graphId); //$NON-NLS-1$
	}

	/**
	 * Set tooltip for the action button.
	 */
	private void updateUI_Tooltip() {

		if (_actionColor == null) {
			return;
		}

		if (_colorSelectDialog != null && _actionColor.getSelection()) {

			// hide tooltip because the color selection dialog is displayed

			_actionColor.setToolTipText(UI.EMPTY_STRING);

		} else {

			_actionColor.setToolTipText(_toolTipText);
		}
	}
}
