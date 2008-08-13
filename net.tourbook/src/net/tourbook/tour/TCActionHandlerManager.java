/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.HashMap;

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
class TCActionHandlerManager {

	private static TCActionHandlerManager		fInstance;

	private ICommandService						fCommandService;
	private IHandlerService						fHandlerService;

	/**
	 * map for all action handlers
	 */
	private HashMap<String, TCActionHandler>	fActionHandlers;

	static TCActionHandlerManager getInstance() {

		if (fInstance == null) {
			fInstance = new TCActionHandlerManager();
		}
		return fInstance;
	}

	private TCActionHandlerManager() {}

	/**
	 * Create all action handlers used by the tour chart
	 */
	void createActionHandlers() {

		// check if the handlers are created
		if (fActionHandlers != null) {
			return;
		}

		final IServiceLocator workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		fCommandService = ((ICommandService) workbenchWindow.getService(ICommandService.class));
		fHandlerService = ((IHandlerService) workbenchWindow.getService(IHandlerService.class));
//		IContextService contextService = (IContextService) getSite().getService(IContextService.class);

		fActionHandlers = new HashMap<String, TCActionHandler>();

		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_TOUR_COMPARE, new ActionHandlerGraphTourCompare());

		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_ALTITUDE, new ActionHandlerGraphAltitude());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_PULSE, new ActionHandlerGraphPulse());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_SPEED, new ActionHandlerGraphSpeed());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_PACE, new ActionHandlerGraphPace());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_POWER, new ActionHandlerGraphPower());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_TEMPERATURE, new ActionHandlerGraphTemperature());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_CADENCE, new ActionHandlerGraphCadence());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_ALTIMETER, new ActionHandlerGraphAltimeter());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_GRADIENT, new ActionHandlerGraphGradient());

		fActionHandlers.put(TourChart.COMMAND_ID_X_AXIS_TIME, new ActionHandlerXAxisTime());
		fActionHandlers.put(TourChart.COMMAND_ID_X_AXIS_DISTANCE, new ActionHandlerXAxisDistance());

		fActionHandlers.put(TourChart.COMMAND_ID_CHART_OPTIONS, new ActionHandlerChartOptions());
		fActionHandlers.put(TourChart.COMMAND_ID_SHOW_START_TIME, new ActionHandlerShowStartTime());
//		fActionHandlers.put(TourChart.COMMAND_ID_CAN_SCROLL_CHART, new ActionHandlerCanScrollChart());
		fActionHandlers.put(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, new ActionHandlerCanAutoZoomToSlider());

		setupHandlers();
	}

	/**
	 * Get the action handler for the command or <code>null</code> when an action handler is not
	 * available
	 * 
	 * @param commandId
	 * @return
	 */
	TCActionHandler getActionHandler(final String commandId) {

		if (fActionHandlers == null) {
			return null;
		}

		return fActionHandlers.get(commandId);
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
		for (final TCActionHandler actionHandler : fActionHandlers.values()) {

			final IHandlerActivation handlerActivation = fHandlerService.activateHandler(actionHandler.getCommandId(),
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
	void updateTourActionHandlers(final IWorkbenchPartSite partSite, final TourChart tourChart) {

		for (final TCActionProxy actionProxy : tourChart.fActionProxies.values()) {

			final TCActionHandler actionHandler = fActionHandlers.get(actionProxy.getCommandId());

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
	void updateUICheckState(final String commandId) {
		if (fCommandService != null) {
			fCommandService.refreshElements(commandId, null);
		}
	}

	/**
	 * Update the UI enablement/checked state for all tour actions
	 */
	void updateUIState() {

		for (final TCActionHandler actionHandler : fActionHandlers.values()) {
			actionHandler.fireHandlerChanged();
			fCommandService.refreshElements(actionHandler.getCommandId(), null);
		}
	}

}
