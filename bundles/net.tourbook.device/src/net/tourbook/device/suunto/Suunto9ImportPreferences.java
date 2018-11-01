package net.tourbook.device.suunto;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.tourbook.common.preferences.BooleanFieldEditor2;

public class Suunto9ImportPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String	IMPORT_INTO_TITLE_TRUNCATED	= "truncated";	//$NON-NLS-1$
	public static final String	IMPORT_INTO_TITLE_ALL			= "all";			//$NON-NLS-1$

	private SelectionAdapter	_defaultSelectionListener;

	private PixelConverter		_pc;

	/*
	 * UI controls
	 */
	private Composite					_containerCharacter;

	private Group						_groupNotesImport;

	private BooleanFieldEditor2	_editBool_IgnoreSpeedValues;
	private BooleanFieldEditor		_editBool_ImportIntoDescription;
	private BooleanFieldEditor2	_editBool_ImportIntoTitle;

	private IntegerFieldEditor		_editInt_TruncatedNotes;

	private Label						_lblIgnoreSpeed;

	private Button						_chkIgnoreSpeed;
	private Button						_rdoImportAll;
	private Button						_rdoImportTruncated;

	@Override
	protected void createFieldEditors() {

		createUI();

	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		initUI(parent);

	}

	@Override
	public void init(final IWorkbench workbench) {
		//setPreferenceStore(_prefStore);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {}
		};
	}

	@Override
	protected void performDefaults() {

		_editBool_IgnoreSpeedValues.loadDefault();
		_editBool_ImportIntoDescription.loadDefault();
		_editBool_ImportIntoTitle.loadDefault();

		_editInt_TruncatedNotes.loadDefault();

	}

	@Override
	public boolean performOk() {

		//_prefStore.setValue(IPreferences.IS_TITLE_IMPORT_ALL, _rdoImportAll.getSelection());

		return super.performOk();
	}

}
