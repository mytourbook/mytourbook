/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import java.util.HashMap;

import net.tourbook.tour.TourEditor;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * This manager aktivates/deactivates tour chart action handlers (TC = TourChart)
 * 
 * <pre>
 * author:  Wolfgang Schramm
 * created: 2007-08-08
 * </pre>
 */
public class TCActionHandlerManager {

	private static TCActionHandlerManager		_instance;

	private ICommandService						_commandService;
	private IHandlerService						_handlerService;

	/**
	 * map for all action handlers
	 */
	private HashMap<String, TCActionHandler>	_actionHandlers;

	private TCActionHandlerManager() {}

	public static TCActionHandlerManager getInstance() {

		if (_instance == null) {
			_instance = new TCActionHandlerManager();
		}
		return _instance;
	}

	/**
	 * Create all action handlers used by the tour chart
	 */
	public void createActionHandlers() {

		// check if the handlers are created
		if (_actionHandlers != null) {
			return;
		}

		final IServiceLocator workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		_commandService = ((ICommandService) workbenchWindow.getService(ICommandService.class));
		_handlerService = ((IHandlerService) workbenchWindow.getService(IHandlerService.class));
//		IContextService contextService = (IContextService) getSite().getService(IContextService.class);

		_actionHandlers = new HashMap<String, TCActionHandler>();

		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_TOUR_COMPARE, new ActionHandlerGraphTourCompare());

		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_ALTITUDE, new ActionHandlerGraphAltitude());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_PULSE, new ActionHandlerGraphPulse());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_SPEED, new ActionHandlerGraphSpeed());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_PACE, new ActionHandlerGraphPace());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_POWER, new ActionHandlerGraphPower());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_TEMPERATURE, new ActionHandlerGraphTemperature());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_CADENCE, new ActionHandlerGraphCadence());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_ALTIMETER, new ActionHandlerGraphAltimeter());
		_actionHandlers.put(TourChart.COMMAND_ID_GRAPH_GRADIENT, new ActionHandlerGraphGradient());

		_actionHandlers.put(TourChart.COMMAND_ID_HR_ZONE_DROPDOWN_MENU, new ActionHandlerHrZoneDropDownMenu());

		String cmdId = TourChart.COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP;
		_actionHandlers.put(cmdId, new ActionHandlerHrZoneStyle(cmdId));

		cmdId = TourChart.COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT;
		_actionHandlers.put(cmdId, new ActionHandlerHrZoneStyle(cmdId));

		cmdId = TourChart.COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP;
		_actionHandlers.put(cmdId, new ActionHandlerHrZoneStyle(cmdId));

		cmdId = TourChart.COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM;
		_actionHandlers.put(cmdId, new ActionHandlerHrZoneStyle(cmdId));

		_actionHandlers.put(TourChart.COMMAND_ID_X_AXIS_TIME, new ActionHandlerXAxisTime());
		_actionHandlers.put(TourChart.COMMAND_ID_X_AXIS_DISTANCE, new ActionHandlerXAxisDistance());

		_actionHandlers.put(TourChart.COMMAND_ID_IS_SHOW_START_TIME, new ActionHandlerShowStartTime());
		_actionHandlers.put(TourChart.COMMAND_ID_IS_SHOW_SRTM_DATA, new ActionHandlerShowSRTMData());
		_actionHandlers.put(TourChart.COMMAND_ID_IS_SHOW_TOUR_MARKER, new ActionHandlerShowTourMarker());
		_actionHandlers.put(TourChart.COMMAND_ID_IS_SHOW_BREAKTIME_VALUES, new ActionHandlerShowBreaktimeValues());
		_actionHandlers.put(TourChart.COMMAND_ID_IS_SHOW_VALUEPOINT_TOOLTIP, new ActionHandlerShowValuePointToolTip());
		_actionHandlers.put(TourChart.COMMAND_ID_EDIT_CHART_PREFERENCES, new ActionHandlerEditCharPreferences());
		_actionHandlers.put(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, new ActionHandlerCanAutoZoomToSlider());
		_actionHandlers.put(
				TourChart.COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED,
				new ActionHandlerCanMoveSlidersWhenZoomed());

		setupHandlers();
	}

	/**
	 * Get the action handler for the command or <code>null</code> when an action handler is not
	 * available
	 * 
	 * @param commandId
	 * @return
	 */
	public TCActionHandler getActionHandler(final String commandId) {

		if (_actionHandlers == null) {
			return null;
		}

		return _actionHandlers.get(commandId);
	}

	/**
	 * Activate all action handlers
	 */
	private void setupHandlers() {
		/*
		 * it would be better to define the expression in the
		 * org.eclipse.core.expressions.definitions extension, but in Eclipse 3.3 the
		 * ReferenceExpression is only for eclipse internal use, wolfgang 9.8.2007
		 */
		final Expression partIdExpression = new Expression() {

			@Override
			public void collectExpressionInfo(final ExpressionInfo info) {
				info.addVariableNameAccess(ISources.ACTIVE_PART_ID_NAME);
			}

			@Override
			public EvaluationResult evaluate(final IEvaluationContext context) throws CoreException {

				final Object var = context.getVariable(ISources.ACTIVE_PART_ID_NAME);

				if (var instanceof String) {

					final String stringVar = (String) var;

					/*
					 * check if the active part ID contains a tour chart
					 */
					if (stringVar.equalsIgnoreCase(TourEditor.ID)) {
						return EvaluationResult.TRUE;
//					} else if (stringVar.equalsIgnoreCase("net.tourbook.views.TourChartView")) {
//						return EvaluationResult.TRUE;
					}
				}

				return EvaluationResult.FALSE;
			}
		};

		// activate the handler for all tour chart actions
		for (final TCActionHandler actionHandler : _actionHandlers.values()) {

			final IHandlerActivation handlerActivation = _handlerService.activateHandler(
					actionHandler.getCommandId(),
					actionHandler,
					partIdExpression);

			actionHandler.setHandlerActivation(handlerActivation);
		}
	}

	/**
	 * Set the check/enable state for all action handlers from their action proxy and update the UI
	 * state
	 * 
	 * @param partSite
	 */
	public void updateTourActionHandlers(final IWorkbenchPartSite partSite, final TourChart tourChart) {

		for (final TCActionProxy actionProxy : tourChart.getActionProxies().values()) {

			final TCActionHandler actionHandler = _actionHandlers.get(actionProxy.getCommandId());

			if (actionHandler != null) {
				actionHandler.setTourChart(tourChart);
				actionHandler.setChecked(actionProxy.isChecked());
				actionHandler.setEnabled(actionProxy.isEnabled());
			}
		}

		updateUIState();
	}

	/**
	 * Update the UI check state for one command
	 */
	public void updateUICheckState(final String commandId) {
		if (_commandService != null) {
			_commandService.refreshElements(commandId, null);
		}
	}

	/**
	 * Update the UI enablement/checked state for all tour actions
	 */
	public void updateUIState() {

		for (final TCActionHandler actionHandler : _actionHandlers.values()) {
			actionHandler.fireHandlerChanged();
			_commandService.refreshElements(actionHandler.getCommandId(), null);
		}
	}

}
