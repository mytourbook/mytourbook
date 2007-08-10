package net.tourbook.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

// author:  wolfgang
// create:	2007-08-10

public class HandlerUtil {

	/**
	 * @param execEvent
	 * @return Returns a ToolItem which triggered the event or <code>null</code> when another
	 *         control triggered the event
	 */
	public static ToolItem getToolItem(ExecutionEvent execEvent) {

		Object trigger = execEvent.getTrigger();

		if (trigger instanceof Event) {

			Event event = (Event) trigger;
			Widget widget = event.widget;

			if (widget instanceof ToolItem) {
				return (ToolItem) widget;
			}
		}

		return null;
	}

}
