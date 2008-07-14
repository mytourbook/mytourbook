package net.tourbook.ui;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

public final class ActionOpenPrefDialog extends Action {

	private String	fPrefPageId;

	public ActionOpenPrefDialog(final String text, final String prefPageId) {

		setText(text);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__options));

		fPrefPageId = prefPageId;
	}

	@Override
	public void run() {
		PreferencesUtil.createPreferenceDialogOn(//
		Display.getCurrent().getActiveShell(),
				fPrefPageId,
				null,
				null).open();
	}
}
