package net.tourbook.ui.views.calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class CalendarComponents extends Composite {

	private CalendarForm	_calForm;
	private CalendarGraph	_calGraph;

	private Display			_display;

	CalendarComponents(final CalendarForm parent, final int style) {

		// create composite with vertical scrollbars
		super(parent, SWT.V_SCROLL | SWT.NO_BACKGROUND);

		_display = Display.getCurrent();
		
		GridData gd;
		_calForm = parent;

		// set layout for the components
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		setLayoutData(gd);

		// set layout for this chart
		final GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);

//		final CLabel label = new CLabel(this, SWT.FLAT | SWT.CENTER);
//		label.setText("Calendar");
//		label.setBackground(_display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
//		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
//		label.setLayoutData(gd);

		_calGraph = new CalendarGraph(parent, this, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		_calGraph.setLayoutData(gd);

	}

	public CalendarGraph getGraph() {
		return _calGraph;
	}

}
