package net.tourbook.map.vtm;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class MapVtmView extends ViewPart {

	public static final String	ID	= "net.tourbook.map.vtm.MapVtmView";

	private GdxMapApp			_gdxMapApp;

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
	}

	private void createUI(final Composite parent) {

		final Composite swtContainer = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		final Frame awtContainer = SWT_AWT.new_Frame(swtContainer);

		final Canvas awtCanvas = new Canvas();
		awtContainer.setLayout(new BorderLayout());
		awtCanvas.setIgnoreRepaint(true);

		awtContainer.add(awtCanvas);
		awtCanvas.setFocusable(true);
		awtCanvas.requestFocus();

		_gdxMapApp = new GdxMapApp();
		_gdxMapApp.run(awtCanvas);
	}

	@Override
	public void setFocus() {}

}
