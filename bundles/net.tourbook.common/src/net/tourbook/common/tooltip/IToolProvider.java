package net.tourbook.common.tooltip;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public interface IToolProvider {

	/**
	 * Creates the UI for this tool.
	 * 
	 * @param parent
	 */
	public void createToolUI(Composite parent);

	/**
	 * @return Returns location where the tool shoul be displayed the first time or
	 *         <code>null</code> when location is not available.
	 */
	public Point getInitialLocation();

	/**
	 * @return Returns tooltip area which is associated which this tool provider, can be
	 *         <code>null</code>.
	 */
	public Object getToolTipArea();

	/**
	 * @return Returns a title which is displayed as tooltip when the tooltip header in a movable
	 *         tooltip is hovered.
	 */
	public String getToolTitle();

	/**
	 * @return Returns <code>true</code> when the tooltip is flexible which means it can be moved to
	 *         another position.
	 */
	public boolean isFlexTool();

	/**
	 * Initial location should be used only once.
	 */
	public void resetInitialLocation();

	/**
	 * Associate tool provider with the tooltip in which the UI is displayed. This association is
	 * used when the tool is movable. Movable tools are not disposed when they are hidden.
	 * 
	 * @param toolTipArea
	 */
	public void setToolTipArea(Object toolTipArea);

//	/**
//	 * Set focus to a control after the tooltip
//	 */
//	public void setFocus();

}
