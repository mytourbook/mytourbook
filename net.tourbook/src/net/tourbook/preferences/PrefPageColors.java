package net.tourbook.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageColors extends PreferencePage implements IWorkbenchPreferencePage {

	public PrefPageColors() {
		noDefaultAndApplyButton();
	}

	public PrefPageColors(final String title) {
		super(title);
	}

	public PrefPageColors(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(final Composite parent) {
		return null;
	}

	public void init(final IWorkbench workbench) {

	}

}
