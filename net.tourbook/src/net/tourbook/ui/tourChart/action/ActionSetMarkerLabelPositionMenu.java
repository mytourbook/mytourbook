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
package net.tourbook.ui.tourChart.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ActionSetMarkerLabelPositionMenu extends Action implements IMenuCreator {

	private Menu						_menu;

	private TourChart					_tourChart;
	private TourMarker					_tourMarker;

	private ArrayList<ContributionItem>	_allActions	= new ArrayList<ContributionItem>();

	private class ActionHorizontalPosition extends Action {

		public ActionHorizontalPosition() {

			super(Messages.Tour_Action_Marker_PositionHorizontal, AS_PUSH_BUTTON);
			setEnabled(false);
		}

		@Override
		public void run() {}
	}

	private class ActionSetMarkerPosition extends Action {

		private int	labelPosId;

		public ActionSetMarkerPosition(final String positionText, final int labelPosId) {

			super(positionText, AS_CHECK_BOX);

			this.labelPosId = labelPosId;
		}

		@Override
		public void run() {
			_tourChart.actionSetMarkerLabelPosition(_tourMarker, labelPosId);
		}
	}

	private class ActionVerticalPosition extends Action {

		public ActionVerticalPosition() {

			super(Messages.Tour_Action_Marker_PositionVertical, AS_PUSH_BUTTON);
			setEnabled(false);
		}

		@Override
		public void run() {}
	}

	public ActionSetMarkerLabelPositionMenu(final TourChart tourChart) {

		super(Messages.Tour_Action_Marker_SetLabelPosition, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_tourChart = tourChart;

		createActions();
	}

	private ActionContributionItem contribItem(final Action action) {

		return new ActionContributionItem(action);
	}

//	static {
//
//		final String[] LABEL_POSITIONS = new String[] { //
//		//
//			Messages.Tour_Marker_Position_vertical_above, // 				0
//			Messages.Tour_Marker_Position_vertical_below, //				1
//			Messages.Tour_Marker_Position_vertical_chart_top, // 			2
//			Messages.Tour_Marker_Position_vertical_chart_bottom, // 		3
//			Messages.Tour_Marker_Position_horizontal_above_left, // 		4
//			Messages.Tour_Marker_Position_horizontal_above_centered, // 	5
//			Messages.Tour_Marker_Position_horizontal_above_right, // 		6
//			Messages.Tour_Marker_Position_horizontal_below_left, // 		7
//			Messages.Tour_Marker_Position_horizontal_below_centered, // 	8
//			Messages.Tour_Marker_Position_horizontal_below_right, // 		9
//			Messages.Tour_Marker_Position_horizontal_left, // 				10
//			Messages.Tour_Marker_Position_horizontal_right, // 				11
//		};
//	}
//
//	public final static int	LABEL_POS_VERTICAL_ABOVE_GRAPH				= 0;
//	public final static int	LABEL_POS_VERTICAL_BELOW_GRAPH				= 1;
//	public final static int	LABEL_POS_VERTICAL_TOP_CHART				= 2;
//	public final static int	LABEL_POS_VERTICAL_BOTTOM_CHART				= 3;
//
//	public final static int	LABEL_POS_HORIZONTAL_ABOVE_GRAPH_LEFT		= 4;
//	public final static int	LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED	= 5;
//	public final static int	LABEL_POS_HORIZONTAL_ABOVE_GRAPH_RIGHT		= 6;
//	public final static int	LABEL_POS_HORIZONTAL_BELOW_GRAPH_LEFT		= 7;
//	public final static int	LABEL_POS_HORIZONTAL_BELOW_GRAPH_CENTERED	= 8;
//	public final static int	LABEL_POS_HORIZONTAL_BELOW_GRAPH_RIGHT		= 9;
//	public final static int	LABEL_POS_HORIZONTAL_GRAPH_LEFT				= 10;
//	public final static int	LABEL_POS_HORIZONTAL_GRAPH_RIGHT			= 11;

	private void createActions() {

		/*
		 * Horizontal
		 */
		_allActions.add(contribItem(new ActionHorizontalPosition()));
		_allActions.add(new Separator());

		/*
		 * Marker point
		 */
		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_MarkerPoint_Left,
				TourMarker.LABEL_POS_HORIZONTAL_GRAPH_LEFT)));

		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_MarkerPoint_Right,
				TourMarker.LABEL_POS_HORIZONTAL_GRAPH_RIGHT)));

		/*
		 * Above
		 */
		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Horizontal_AboveLeft,
				TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_LEFT)));

		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Horizontal_AboveCentered,
				TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED)));

		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Horizontal_AboveRight,
				TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_RIGHT)));

		/*
		 * Below
		 */
		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Horizontal_BelowLeft,
				TourMarker.LABEL_POS_HORIZONTAL_BELOW_GRAPH_LEFT)));

		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Horizontal_BelowCentered,
				TourMarker.LABEL_POS_HORIZONTAL_BELOW_GRAPH_CENTERED)));

		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Horizontal_BelowRight,
				TourMarker.LABEL_POS_HORIZONTAL_BELOW_GRAPH_RIGHT)));

		///////////////////////////////////////////////////////////////////////////

		/*
		 * Vertical
		 */
		_allActions.add(new Separator());
		_allActions.add(contribItem(new ActionVerticalPosition()));

		/*
		 * Marker point
		 */
		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Vertical_MarkerPoint_Above,
				TourMarker.LABEL_POS_VERTICAL_ABOVE_GRAPH)));

		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Vertical_MarkerPoint_Below,
				TourMarker.LABEL_POS_VERTICAL_BELOW_GRAPH)));

		/*
		 * Chart border
		 */
		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Vertical_Chart_Top,
				TourMarker.LABEL_POS_VERTICAL_TOP_CHART)));

		_allActions.add(contribItem(new ActionSetMarkerPosition(
				Messages.Tour_Marker_Position_Vertical_Chart_Bottom,
				TourMarker.LABEL_POS_VERTICAL_BOTTOM_CHART)));
	}

	public void dispose() {

		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	private void fillMenu(final Menu menu) {

		final int currentLabelPosition = _tourMarker.getLabelPosition();

		for (final ContributionItem contribItem : _allActions) {

			if (contribItem instanceof ActionContributionItem) {

				final ActionContributionItem actionItem = (ActionContributionItem) contribItem;
				final IAction action = actionItem.getAction();

				if (action instanceof ActionSetMarkerPosition) {

					final ActionSetMarkerPosition posAction = (ActionSetMarkerPosition) action;

					final boolean isCurrentPosition = posAction.labelPosId == currentLabelPosition;

					posAction.setChecked(isCurrentPosition);
					posAction.setEnabled(!isCurrentPosition);
				}
			}

			contribItem.fill(_menu, -1);
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();

		_menu = new Menu(parent);

		// Add listener to repopulate the menu each time
		_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {

				// dispose old menu items
				for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
					menuItem.dispose();
				}

				fillMenu(_menu);
			}
		});

		return _menu;
	}

	public void setTourMarker(final TourMarker tourMarker) {
		_tourMarker = tourMarker;
	}

}
