package net.tourbook.tour;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

// author:  Wolfgang Schramm
// created: 2007-08-08

/**
 * This manager aktivates/deactivates tour chart action handlers
 */
public class TourChartActionHandlerManager {

	private static TourChartActionHandlerManager	fInstance;

	private ICommandService							fCommandService;
	private IHandlerService							fHandlerService;

	private HashMap<String, TourChartActionHandler>	fActionHandlers;

	private TourChartActionHandlerManager() {}

	public static TourChartActionHandlerManager getInstance() {

		if (fInstance == null) {
			fInstance = new TourChartActionHandlerManager();
		}
		return fInstance;
	}

	public void activateActions(TourChart tourChart) {

		/*
		 * update the enable/check state in each action handler
		 */
		for (TourChartActionProxy actionProxy : tourChart.fActionProxies.values()) {

			TourChartActionHandler actionHandler = fActionHandlers.get(actionProxy.getCommandId());

			if (actionHandler != null) {
				actionHandler.setTourChart(tourChart);
				actionHandler.setChecked(actionProxy.isChecked());
				actionHandler.setEnabled(actionProxy.isEnabled());
			}
		}

		updateUIState();
	}

	/**
	 * Create all actions for the tour chart
	 * 
	 * @param commandService
	 * @param handlerService
	 */
	public void createActionHandlers(IWorkbenchWindow workbenchWindow) {

		if (fActionHandlers != null) {
			// all handlers are created
			return;
		}

		fCommandService = ((ICommandService) workbenchWindow.getService(ICommandService.class));
		fHandlerService = ((IHandlerService) workbenchWindow.getService(IHandlerService.class));

		fActionHandlers = new HashMap<String, TourChartActionHandler>();

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

		fActionHandlers.put(TourChart.COMMAND_ID_CHARTOPTIONS, new ActionHandlerChartOptions());
		fActionHandlers.put(TourChart.COMMAND_ID_SHOWSTARTTIME, new ActionHandlerShowStartTime());

		/*
		 * 'normally' the expression should be done with the
		 * org.eclipse.core.expressions.definitions extension, but the ReferenceExpression is in
		 * Eclipse 3.3 only for internal use, wolfgang 9.8.2007
		 */
		Expression partIdExpression = new Expression() {

			public void collectExpressionInfo(ExpressionInfo info) {
				info.addVariableNameAccess(ISources.ACTIVE_PART_ID_NAME);
			}

			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {

				Object var = context.getVariable(ISources.ACTIVE_PART_ID_NAME);

				if (var instanceof String) {

					String stringVar = (String) var;

					/*
					 * check if the active part ID contains a tour chart
					 */
					if (stringVar.equalsIgnoreCase("net.tourbook.tour.TourEditor")) {
						return EvaluationResult.TRUE;
					} else if (stringVar.equalsIgnoreCase("net.tourbook.views.TourChartView")) {
						return EvaluationResult.TRUE;
					}
				}

				return EvaluationResult.FALSE;
			}
		};

		// activate the handler for all tour chart actions
		for (Iterator<TourChartActionHandler> iterator = fActionHandlers.values().iterator(); iterator.hasNext();) {

			TourChartActionHandler actionHandler = (TourChartActionHandler) iterator.next();

			final IHandlerActivation handlerActivation = fHandlerService.activateHandler(actionHandler.getCommandId(),
					actionHandler,
					partIdExpression);

			actionHandler.setHandlerActivation(handlerActivation);
		}
	}

	public TourChartActionHandler getActionHandler(String commandId) {
		return fActionHandlers.get(commandId);
	}

	/**
	 * Update the UI check state for a command
	 */
	public void updateUICheckState(String commandId) {
		fCommandService.refreshElements(commandId, null);
	}

	/**
	 * Update the UI enablement/checked state for all tour actions
	 */
	public void updateUIState() {

		for (final TourChartActionHandler actionHandler : fActionHandlers.values()) {
			actionHandler.fireHandlerChanged();
			fCommandService.refreshElements(actionHandler.getCommandId(), null);
		}
	}

}
