package net.tourbook.ext.srtm;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageSRTM extends PreferencePage implements IWorkbenchPreferencePage {

	public PrefPageSRTM() {
		noDefaultAndApplyButton();
	}

	public PrefPageSRTM(final String title) {
		super(title);
	}

	public PrefPageSRTM(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(final Composite parent) {
		return null;
	}

	public void init(final IWorkbench workbench) {

	}

}
