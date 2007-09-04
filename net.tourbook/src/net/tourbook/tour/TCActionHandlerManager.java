package net.tourbook.tour;

import java.util.HashMap;
import java.util.Iterator;

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

// author:  Wolfgang Schramm
// created: 2007-08-08

/**
 * This manager aktivates/deactivates tour chart action handlers (TC = TourChart)
 */
class TCActionHandlerManager {

	private static TCActionHandlerManager		fInstance;

	private ICommandService						fCommandService;
	private IHandlerService						fHandlerService;

	/**
	 * map for all action handlers
	 */
	private HashMap<String, TCActionHandler>	fActionHandlers;

	private TCActionHandlerManager() {}

	static TCActionHandlerManager getInstance() {

		if (fInstance == null) {
			fInstance = new TCActionHandlerManager();
		}
		return fInstance;
	}

	/**
	 * Create all action handlers used by the tour chart
	 */
	void createActionHandlers() {

		// check if the handlers are created
		if (fActionHandlers != null) {
			return;
		}

		IServiceLocator workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		fCommandService = ((ICommandService) workbenchWindow.getService(ICommandService.class));
		fHandlerService = ((IHandlerService) workbenchWindow.getService(IHandlerService.class));
//		IContextService contextService = (IContextService) getSite().getService(IContextService.class);

		fActionHandlers = new HashMap<String, TCActionHandler>();

		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_ALTITUDE, new ActionHandlerGraphAltitude());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_PULSE, new ActionHandlerGraphPulse());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_SPEED, new ActionHandlerGraphSpeed());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_TEMPERATURE,
				new ActionHandlerGraphTemperature());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_CADENCE, new ActionHandlerGraphCadence());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_ALTIMETER, new ActionHandlerGraphAltimeter());
		fActionHandlers.put(TourChart.COMMAND_ID_GRAPH_GRADIENT, new ActionHandlerGraphGradient());

		fActionHandlers.put(TourChart.COMMAND_ID_X_AXIS_TIME, new ActionHandlerXAxisTime());
		fActionHandlers.put(TourChart.COMMAND_ID_X_AXIS_DISTANCE, new ActionHandlerXAxisDistance());

		fActionHandlers.put(TourChart.COMMAND_ID_CHART_OPTIONS, new ActionHandlerChartOptions());
		fActionHandlers.put(TourChart.COMMAND_ID_SHOW_START_TIME, new ActionHandlerShowStartTime());
		fActionHandlers.put(TourChart.COMMAND_ID_CAN_SCROLL_CHART,
				new ActionHandlerCanScrollChart());
		fActionHandlers.put(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER,
				new ActionHandlerCanAutoZoomToSlider());

		setupHandlers();
	}

	/**
	 * Get the action handler for the command or <code>null</code> when an action handler is not
	 * available
	 * 
	 * @param commandId
	 * @return
	 */
	TCActionHandler getActionHandler(String commandId) {

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
		Expression partIdExpression = new Expression() {

			@Override
			public void collectExpressionInfo(ExpressionInfo info) {
				info.addVariableNameAccess(ISources.ACTIVE_PART_ID_NAME);
			}

			@Override
			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {

				Object var = context.getVariable(ISources.ACTIVE_PART_ID_NAME);

				if (var instanceof String) {

					String stringVar = (String) var;

					/*
					 * check if the active part ID contains a tour chart
					 */
					if (stringVar.equalsIgnoreCase("net.tourbook.tour.TourEditor")) {
						return EvaluationResult.TRUE;
//					} else if (stringVar.equalsIgnoreCase("net.tourbook.views.TourChartView")) {
//						return EvaluationResult.TRUE;
					}
				}

				return EvaluationResult.FALSE;
			}
		};

		// activate the handler for all tour chart actions
		for (Iterator<TCActionHandler> iterator = fActionHandlers.values().iterator(); iterator.hasNext();) {

			TCActionHandler actionHandler = iterator.next();

			final IHandlerActivation handlerActivation = fHandlerService.activateHandler(actionHandler.getCommandId(),
					actionHandler,
					partIdExpression);

			actionHandler.setHandlerActivation(handlerActivation);
		}
	}

	/**
	 * Set the state for all action handlers from their action proxy and update the UI state
	 * 
	 * @param partSite
	 */
	void updateTourActionHandlers(IWorkbenchPartSite partSite, TourChart tourChart) {

		for (TCActionProxy actionProxy : tourChart.fActionProxies.values()) {

			TCActionHandler actionHandler = fActionHandlers.get(actionProxy.getCommandId());

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
	void updateUICheckState(String commandId) {
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
