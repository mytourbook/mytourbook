/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.editor.preferences;

import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Plugin preference page for reporting/performance options.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class ReportingPrefPage extends AbstractPrefPage {
    
    /* Preference fields. */
    private Combo reportMissingVals;
    private Combo reportDuplVals;
    private Combo reportSimVals;
    private Text reportSimPrecision;
    private Button[] reportSimValsMode = new Button[2];
    
    /**
     * Constructor.
     */
    public ReportingPrefPage() {
        super();
    }

    /**
     * @see org.eclipse.jface.preference.PreferencePage#createContents(
     *         org.eclipse.swt.widgets.Composite)
     */
    protected Control createContents(Composite parent) {
        IPreferenceStore prefs = getPreferenceStore();
        Composite field = null;
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        
        new Label(composite, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.perform.intro1")); //$NON-NLS-1$
        new Label(composite, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.perform.intro2")); //$NON-NLS-1$
        new Label(composite, SWT.NONE).setText(" "); //$NON-NLS-1$
        
        // Report missing values?
        field = createFieldComposite(composite);
    	GridData gridData = new GridData();
    	gridData.grabExcessHorizontalSpace = true;
    	field.setLayoutData(gridData);
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.perform.missingVals")); //$NON-NLS-1$
        reportMissingVals = new Combo(field, SWT.READ_ONLY);
        populateCombo(reportMissingVals,
        		prefs.getInt(MsgEditorPreferences.REPORT_MISSING_VALUES_LEVEL));
//        reportMissingVals.setSelection(
//                prefs.getBoolean(MsgEditorPreferences.REPORT_MISSING_VALUES));

        // Report duplicate values?
        field = createFieldComposite(composite);
    	gridData = new GridData();
    	gridData.grabExcessHorizontalSpace = true;
    	field.setLayoutData(gridData);
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.perform.duplVals")); //$NON-NLS-1$
        reportDuplVals = new Combo(field, SWT.READ_ONLY);
        populateCombo(reportDuplVals,
        		prefs.getInt(MsgEditorPreferences.REPORT_DUPL_VALUES_LEVEL));
        
        // Report similar values?
        field = createFieldComposite(composite);
    	gridData = new GridData();
    	gridData.grabExcessHorizontalSpace = true;
    	field.setLayoutData(gridData);

        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.perform.simVals")); //$NON-NLS-1$
        reportSimVals = new Combo(field, SWT.READ_ONLY);
        populateCombo(reportSimVals,
        		prefs.getInt(MsgEditorPreferences.REPORT_SIM_VALUES_LEVEL));
        reportSimVals.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                refreshEnabledStatuses();
            }
        });
        
        Composite simValModeGroup = new Composite(composite, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = indentPixels;
        gridLayout.marginHeight = 0;
        gridLayout.verticalSpacing = 0;
        simValModeGroup.setLayout(gridLayout);
        
        // Report similar values: word count
        reportSimValsMode[0] = new Button(simValModeGroup, SWT.RADIO);
        reportSimValsMode[0].setSelection(prefs.getBoolean(
                MsgEditorPreferences.REPORT_SIM_VALUES_WORD_COMPARE));
        new Label(simValModeGroup, SWT.NONE).setText(MessagesEditorPlugin.getString(
                "prefs.perform.simVals.wordCount")); //$NON-NLS-1$
        
        // Report similar values: Levensthein
        reportSimValsMode[1] = new Button(simValModeGroup, SWT.RADIO);
        reportSimValsMode[1].setSelection(prefs.getBoolean(
                MsgEditorPreferences.REPORT_SIM_VALUES_LEVENSTHEIN));
        new Label(simValModeGroup, SWT.NONE).setText(MessagesEditorPlugin.getString(
                "prefs.perform.simVals.levensthein")); //$NON-NLS-1$
        
        // Report similar values: precision level
        field = createFieldComposite(composite, indentPixels);
        new Label(field, SWT.NONE).setText(MessagesEditorPlugin.getString(
                "prefs.perform.simVals.precision")); //$NON-NLS-1$
        reportSimPrecision = new Text(field, SWT.BORDER);
        reportSimPrecision.setText(
                prefs.getString(MsgEditorPreferences.REPORT_SIM_VALUES_PRECISION));
        reportSimPrecision.setTextLimit(6);
        setWidthInChars(reportSimPrecision, 6);
        reportSimPrecision.addKeyListener(new DoubleTextValidatorKeyListener(
                MessagesEditorPlugin.getString(
                        "prefs.perform.simVals.precision.error"), //$NON-NLS-1$
                0, 1));
        
        refreshEnabledStatuses();
        
        return composite;
    }

    /**
     * Creates the items in the combo and select the item that matches the
     * current value.
     * @param combo
     * @param selectedLevel
     */
    private void populateCombo(Combo combo, int selectedLevel) {
    	combo.add(MessagesEditorPlugin.getString("prefs.perform.message.ignore"));
    	combo.add(MessagesEditorPlugin.getString("prefs.perform.message.info"));
    	combo.add(MessagesEditorPlugin.getString("prefs.perform.message.warning"));
    	combo.add(MessagesEditorPlugin.getString("prefs.perform.message.error"));
    	combo.select(selectedLevel);
    	GridData gridData = new GridData();
    	gridData.grabExcessHorizontalSpace = true;
    	gridData.horizontalAlignment = SWT.RIGHT;
    	combo.setLayoutData(gridData);
    }
    

    /**
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        IPreferenceStore prefs = getPreferenceStore();
        prefs.setValue(MsgEditorPreferences.REPORT_MISSING_VALUES_LEVEL,
                reportMissingVals.getSelectionIndex());
        prefs.setValue(MsgEditorPreferences.REPORT_DUPL_VALUES_LEVEL,
                reportDuplVals.getSelectionIndex());
        prefs.setValue(MsgEditorPreferences.REPORT_SIM_VALUES_LEVEL,
                reportSimVals.getSelectionIndex());
        prefs.setValue(MsgEditorPreferences.REPORT_SIM_VALUES_WORD_COMPARE,
                reportSimValsMode[0].getSelection());
        prefs.setValue(MsgEditorPreferences.REPORT_SIM_VALUES_LEVENSTHEIN,
                reportSimValsMode[1].getSelection());
        prefs.setValue(MsgEditorPreferences.REPORT_SIM_VALUES_PRECISION,
                Double.parseDouble(reportSimPrecision.getText()));
        refreshEnabledStatuses();
        return super.performOk();
    }
    
    
    /**
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        IPreferenceStore prefs = getPreferenceStore();
        reportMissingVals.select(prefs.getDefaultInt(
                MsgEditorPreferences.REPORT_MISSING_VALUES_LEVEL));
        reportDuplVals.select(prefs.getDefaultInt(
                MsgEditorPreferences.REPORT_DUPL_VALUES_LEVEL));
        reportSimVals.select(prefs.getDefaultInt(
                MsgEditorPreferences.REPORT_SIM_VALUES_LEVEL));
        reportSimValsMode[0].setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.REPORT_SIM_VALUES_WORD_COMPARE));
        reportSimValsMode[1].setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.REPORT_SIM_VALUES_LEVENSTHEIN));
        reportSimPrecision.setText(Double.toString(prefs.getDefaultDouble(
                MsgEditorPreferences.REPORT_SIM_VALUES_PRECISION)));
        refreshEnabledStatuses();
        super.performDefaults();
    }

    /*default*/ void refreshEnabledStatuses() {
        boolean isReportingSimilar = reportSimVals.getSelectionIndex()
        			!= MsgEditorPreferences.VALIDATION_MESSAGE_IGNORE;

        for (int i = 0; i < reportSimValsMode.length; i++) {
            reportSimValsMode[i].setEnabled(isReportingSimilar);
        }
        reportSimPrecision.setEnabled(isReportingSimilar);
    }
    
}
