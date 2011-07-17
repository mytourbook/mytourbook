package net.tourbook.ui.views.calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class CalendarComponents extends Composite {

	private CalendarGraph	_calGraph;

	CalendarComponents(final Composite parent, final int style) {

		// create composite with vertical scrollbars
		super(parent, SWT.NO_BACKGROUND | SWT.V_SCROLL);

		GridData gd;

		// set layout for this composite
		final GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);

		// set layout for the components

//		final CLabel label = new CLabel(this, SWT.FLAT | SWT.CENTER);
//		label.setText("Calendar");
//		label.setBackground(_display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
//		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
//		label.setLayoutData(gd);

		_calGraph = new CalendarGraph(this, SWT.NO_BACKGROUND);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		_calGraph.setLayoutData(gd);

	}

	public CalendarGraph getGraph() {
		return _calGraph;
	}

}
