package net.tourbook.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

// author:  wolfgang
// create:	2007-08-10

public class HandlerUtil {

	/**
	 * Gets the triggered item from the {@link ExecutionEvent} and returns the status
	 * 
	 * @param execEvent
	 * @return Returns <code>true</code> when the item is checked or <code>null</code> when the
	 *         item is not from type {@link MenuItem} or {@link ToolItem}
	 */
	public static Boolean isItemChecked(ExecutionEvent execEvent) {

		Object trigger = execEvent.getTrigger();

		if (trigger instanceof Event) {

			Event event = (Event) trigger;
			Widget widget = event.widget;

			if (widget instanceof MenuItem) {
				return ((MenuItem) widget).getSelection();
			}

			if (widget instanceof ToolItem) {
				return ((ToolItem) widget).getSelection();
			}
		}

		return null;
	}

}
