package cop.swt.widgets.viewers.table.celleditors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;

public final class TraverseListenerSet
{
	private TraverseListenerSet()
	{}

	/*
	 * static
	 */

	public static TraverseListener allowTabKey = new TraverseListener()
	{
		@Override
		public void keyTraversed(TraverseEvent e)
		{
			e.doit |= (e.detail == SWT.TRAVERSE_TAB_NEXT) || (e.detail == SWT.TRAVERSE_TAB_PREVIOUS);
		}
	};

	public static TraverseListener allowReturn = new TraverseListener()
	{
		@Override
		public void keyTraversed(TraverseEvent e)
		{
			e.doit |= e.detail == SWT.TRAVERSE_RETURN;
		}
	};

	public static TraverseListener allowEscape = new TraverseListener()
	{
		@Override
		public void keyTraversed(TraverseEvent e)
		{
			e.doit |= e.detail == SWT.TRAVERSE_ESCAPE;
		}
	};
}
