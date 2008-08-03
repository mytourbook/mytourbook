package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMapAppearance extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		MAP_TOUR_SYMBOL_LINE	= "line";
	public static final String		MAP_TOUR_SYMBOL_DOT		= "dot";

	private final IPreferenceStore	fPrefStore				= TourbookPlugin.getDefault().getPreferenceStore();
	private boolean					fIsModified;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		addField(new RadioGroupFieldEditor(ITourbookPreferences.MAP_LAYOUT_SYMBOL,
				Messages.pref_map_layout_symbol,
				1,
				new String[][] {
						{ Messages.pref_map_layout_symbol_line, MAP_TOUR_SYMBOL_LINE },
						{ Messages.pref_map_layout_symbol_dot, MAP_TOUR_SYMBOL_DOT } },
				parent,
				true));

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);

		// show lines
		final IntegerFieldEditor editor = new IntegerFieldEditor(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH,
				Messages.pref_map_layout_symbol_width,
				container);
		addField(editor);
		UI.setFieldWidth(container, editor, UI.DEFAULT_FIELD_WIDTH);
		editor.setValidRange(1, 50);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	protected void performDefaults() {
		fIsModified = true;
		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK && fIsModified) {

			fIsModified = false;

			// fire one event for all modifications
			getPreferenceStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		fIsModified = true;
		super.propertyChange(event);
	}
}
