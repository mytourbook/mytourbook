package net.tourbook.ui.views.calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class CalendarForm extends ViewForm {

	private CalendarComponents	_calendarComponents;

	CalendarForm(final Composite parent, final int style) {

		super(parent, style);

		setBorderVisible(false);

		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		final GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 10;
		gl.marginHeight = 10;
		gl.verticalSpacing = 10;
		setLayout(gl);

		// set the layout for the calendar
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		_calendarComponents = new CalendarComponents(this, style);
		setContent(_calendarComponents);

	}

	public CalendarComponents getComponents() {
		return _calendarComponents;
	}

}
