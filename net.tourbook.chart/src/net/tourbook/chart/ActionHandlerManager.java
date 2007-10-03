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
package net.tourbook.chart;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

// author:  Wolfgang Schramm
// created: 2007-08-12

/**
 * This manager aktivates/deactivates chart action handlers
 */
public class ActionHandlerManager {

	private static ActionHandlerManager		fInstance;

	private ICommandService					fCommandService;
	private IHandlerService					fHandlerService;

	/**
	 * map for all action handlers
	 */
	private HashMap<String, ActionHandler>	fActionHandlers;

	private ActionHandlerManager() {}

	static ActionHandlerManager getInstance() {

		if (fInstance == null) {
			fInstance = new ActionHandlerManager();
		}
		return fInstance;
	}

	private void activateHandlers() {

		/*
		 * it would be better to define the expression in the
		 * org.eclipse.core.expressions.definitions extension, but in Eclipse 3.3 the
		 * ReferenceExpression is only for eclipse internal use, wolfgang 9.8.2007
		 */
		Expression partIdExpression = new Expression() {

			@Override
			public void collectExpressionInfo(ExpressionInfo info) {
				info.addVariableNameAccess(ISources.ACTIVE_PART_ID_NAME);
			}

			@Override
			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {

				Object var = context.getVariable(ISources.ACTIVE_PART_ID_NAME);

				if (var instanceof String) {

//					String stringVar = (String) var;

					/*
					 * check if the active part ID contains a tour chart
					 */
//					if (stringVar.equalsIgnoreCase("net.tourbook.tour.TourEditor")) {
//						return EvaluationResult.TRUE;
////					} else if (stringVar.equalsIgnoreCase("net.tourbook.views.TourChartView")) {
////						return EvaluationResult.TRUE;
//					}
					return EvaluationResult.TRUE;
				}

				return EvaluationResult.FALSE;
			}
		};

		// activate the handler for all tour chart actions
		for (Iterator<ActionHandler> iterator = fActionHandlers.values().iterator(); iterator.hasNext();) {

			ActionHandler actionHandler = iterator.next();

			final IHandlerActivation handlerActivation = fHandlerService.activateHandler(actionHandler.getCommandId(),
					actionHandler,
					partIdExpression);

			actionHandler.setHandlerActivation(handlerActivation);
		}
	}

	/**
	 * Creates all action handlers for a chart and activate them in the handler service
	 * {@link IHandlerService}
	 */
	void createActionHandlers() {

		// check if the handlers are created
		if (fActionHandlers != null) {
			return;
		}

		IServiceLocator workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		fCommandService = ((ICommandService) workbenchWindow.getService(ICommandService.class));
		fHandlerService = ((IHandlerService) workbenchWindow.getService(IHandlerService.class));

		fActionHandlers = new HashMap<String, ActionHandler>();

		fActionHandlers.put(Chart.COMMAND_ID_ZOOM_IN, new ActionHandlerZoomIn());
		fActionHandlers.put(Chart.COMMAND_ID_ZOOM_OUT, new ActionHandlerZoomOut());

		fActionHandlers.put(Chart.COMMAND_ID_FIT_GRAPH, new ActionHandlerFitGraph());

		fActionHandlers.put(Chart.COMMAND_ID_PART_PREVIOUS, new ActionHandlerPartPrevious());
		fActionHandlers.put(Chart.COMMAND_ID_PART_NEXT, new ActionHandlerPartNext());

		activateHandlers();
	}

	/**
	 * Get the action handler for the command
	 * 
	 * @param commandId
	 * @return
	 */
	ActionHandler getActionHandler(String commandId) {

		if (fActionHandlers == null) {
			return null;
		}

		return fActionHandlers.get(commandId);
	}

	/**
	 * Set the state for all action handlers from their action proxy and update the UI state
	 * 
	 * @param chart
	 */
	void updateActionHandlers(Chart chart) {

		if (fActionHandlers == null) {
			return;
		}

		for (ActionProxy actionProxy : chart.fChartActionProxies.values()) {

			ActionHandler actionHandler = fActionHandlers.get(actionProxy.getCommandId());

			if (actionHandler != null) {
				actionHandler.setTourChart(chart);
				actionHandler.setChecked(actionProxy.isChecked());
				actionHandler.setEnabled(actionProxy.isEnabled());
			}
		}

		updateUIState();
	}

	/**
	 * Update the UI check state for one action
	 */
	void updateUICheckState(String commandId) {
		fCommandService.refreshElements(commandId, null);
	}

	/**
	 * Update the UI enablement state for all chart actions
	 */
	void updateUIEnablementState() {

		for (final ActionHandler actionHandler : fActionHandlers.values()) {
			actionHandler.fireHandlerChanged();
		}
	}

	/**
	 * Update the UI enablement/checked state for all chart actions
	 */
	void updateUIState() {

		if (fActionHandlers == null) {
			return;
		}

		for (final ActionHandler actionHandler : fActionHandlers.values()) {
			actionHandler.fireHandlerChanged();
			fCommandService.refreshElements(actionHandler.getCommandId(), null);
		}
	}

}
