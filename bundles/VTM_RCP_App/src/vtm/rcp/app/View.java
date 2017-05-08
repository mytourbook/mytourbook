package vtm.rcp.app;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class View extends ViewPart {

	public static final String	ID	= "VTM_RCP_App.view";

	private GdxMapApp			_gdxMapApp;

	@Override
	public void createPartControl(Composite parent) {

		createUI(parent);
	}

	private void createUI(Composite parent) {

		final Composite swtContainer = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		final Frame awtContainer = SWT_AWT.new_Frame(swtContainer);

		Canvas awtCanvas = new Canvas();
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
