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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Plugin generic preference page.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class GeneralPrefPage extends AbstractPrefPage  {
    
    /* Preference fields. */
    private Text keyGroupSeparator;

    private Text filterLocales;
    
    private Button convertEncodedToUnicode;

    private Button supportNL;
//    private Button supportFragments;
//    private Button loadOnlyFragmentResources;
    
    private Button keyTreeHierarchical;
    private Button keyTreeExpanded;

    private Button fieldTabInserts;
    
//    private Button noTreeInEditor;
    
    private Button setupRbeNatureAutomatically;
    
    /**
     * Constructor.
     */
    public GeneralPrefPage() {
        super();
    }

    /**
     * @see org.eclipse.jface.preference.PreferencePage
     *         #createContents(org.eclipse.swt.widgets.Composite)
     */
    protected Control createContents(Composite parent) {
        MsgEditorPreferences prefs = MsgEditorPreferences.getInstance();
        
//        IPreferenceStore prefs = getPreferenceStore();
        Composite field = null;
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        
        // Key group separator
        field = createFieldComposite(composite);
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.groupSep")); //$NON-NLS-1$
        keyGroupSeparator = new Text(field, SWT.BORDER);
        keyGroupSeparator.setText(prefs.getGroupLevelSeparator());
//                prefs.getString(MsgEditorPreferences.GROUP__LEVEL_SEPARATOR));
        keyGroupSeparator.setTextLimit(2);
        
        field = createFieldComposite(composite);
        Label filterLocalesLabel = new Label(field, SWT.NONE);
        filterLocalesLabel.setText(
                MessagesEditorPlugin.getString("prefs.filterLocales.label")); //$NON-NLS-1$
        filterLocalesLabel.setToolTipText(
        		MessagesEditorPlugin.getString("prefs.filterLocales.tooltip")); //$NON-NLS-1$
        filterLocales = new Text(field, SWT.BORDER);
        filterLocales.setText(prefs.getFilterLocalesStringMatcher());
//                prefs.getString(MsgEditorPreferences.GROUP__LEVEL_SEPARATOR));
        filterLocales.setTextLimit(22);
        setWidthInChars(filterLocales, 16);

        
        // Convert encoded to unicode?
        field = createFieldComposite(composite);
        convertEncodedToUnicode = new Button(field, SWT.CHECK);
        convertEncodedToUnicode.setSelection(
                prefs.isUnicodeEscapeEnabled());
//                prefs.getBoolean(MsgEditorPreferences.CONVERT_ENCODED_TO_UNICODE));
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.convertEncoded")); //$NON-NLS-1$

        // Support "NL" localization structure
        field = createFieldComposite(composite);
        supportNL = new Button(field, SWT.CHECK);
        supportNL.setSelection(prefs.isNLSupportEnabled());
                //prefs.getBoolean(MsgEditorPreferences.SUPPORT_NL));
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.supportNL")); //$NON-NLS-1$

        // Setup rbe validation builder on java projects automatically.
        field = createFieldComposite(composite);
        setupRbeNatureAutomatically = new Button(field, SWT.CHECK);
        setupRbeNatureAutomatically.setSelection(prefs.isBuilderSetupAutomatically());
                //prefs.getBoolean(MsgEditorPreferences.SUPPORT_NL));
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.setupValidationBuilderAutomatically")); //$NON-NLS-1$

//        // Support loading resources from fragment 
//        field = createFieldComposite(composite);
//        supportFragments = new Button(field, SWT.CHECK);
//        supportFragments.setSelection(prefs.isShowSupportEnabled());
//        new Label(field, SWT.NONE).setText(
//                MessagesEditorPlugin.getString("prefs.supportFragments")); //$NON-NLS-1$
//        
//        // Support loading resources from fragment 
//        field = createFieldComposite(composite);
//        loadOnlyFragmentResources = new Button(field, SWT.CHECK);
//        loadOnlyFragmentResources.setSelection(
//                prefs.isLoadingOnlyFragmentResources());
//                //MsgEditorPreferences.getLoadOnlyFragmentResources());
//        new Label(field, SWT.NONE).setText(
//                MessagesEditorPlugin.getString("prefs.loadOnlyFragmentResources")); //$NON-NLS-1$
        
        // Default key tree mode (tree vs flat)
        field = createFieldComposite(composite);
        keyTreeHierarchical = new Button(field, SWT.CHECK);
        keyTreeHierarchical.setSelection(
                prefs.isKeyTreeHierarchical());
//                prefs.getBoolean(MsgEditorPreferences.KEY_TREE_HIERARCHICAL));
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.keyTree.hierarchical"));//$NON-NLS-1$

        // Default key tree expand status (expanded vs collapsed)
        field = createFieldComposite(composite);
        keyTreeExpanded = new Button(field, SWT.CHECK);
        keyTreeExpanded.setSelection(prefs.isKeyTreeExpanded());
//                prefs.getBoolean(MsgEditorPreferences.KEY_TREE_EXPANDED)); //$NON-NLS-1$
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.keyTree.expanded")); //$NON-NLS-1$

        // Default tab key behaviour in text field
        field = createFieldComposite(composite);
        fieldTabInserts = new Button(field, SWT.CHECK);
        fieldTabInserts.setSelection(prefs.isFieldTabInserts());
//                prefs.getBoolean(MsgEditorPreferences.FIELD_TAB_INSERTS));
        new Label(field, SWT.NONE).setText(
                MessagesEditorPlugin.getString("prefs.fieldTabInserts")); //$NON-NLS-1$
        
//        field = createFieldComposite(composite);
//        noTreeInEditor = new Button(field, SWT.CHECK);
//        noTreeInEditor.setSelection(prefs.isEditorTreeHidden());
////                prefs.getBoolean(MsgEditorPreferences.EDITOR_TREE_HIDDEN)); //$NON-NLS-1$
//        new Label(field, SWT.NONE).setText(
//                MessagesEditorPlugin.getString("prefs.noTreeInEditor")); //$NON-NLS-1$
        
        refreshEnabledStatuses();
        return composite;
    }

    /**
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        IPreferenceStore prefs = getPreferenceStore();
        prefs.setValue(MsgEditorPreferences.GROUP__LEVEL_SEPARATOR,
                keyGroupSeparator.getText());
        prefs.setValue(MsgEditorPreferences.FILTER_LOCALES_STRING_MATCHERS,
                filterLocales.getText());
        prefs.setValue(MsgEditorPreferences.UNICODE_UNESCAPE_ENABLED,
                convertEncodedToUnicode.getSelection());
        prefs.setValue(MsgEditorPreferences.NL_SUPPORT_ENABLED,
                supportNL.getSelection());
        prefs.setValue(MsgEditorPreferences.ADD_MSG_EDITOR_BUILDER_TO_JAVA_PROJECTS,
                setupRbeNatureAutomatically.getSelection());
        prefs.setValue(MsgEditorPreferences.KEY_TREE_HIERARCHICAL,
                keyTreeHierarchical.getSelection());
        prefs.setValue(MsgEditorPreferences.KEY_TREE_EXPANDED,
                keyTreeExpanded.getSelection());
        prefs.setValue(MsgEditorPreferences.FIELD_TAB_INSERTS,
                fieldTabInserts.getSelection());
//        prefs.setValue(MsgEditorPreferences.EDITOR_TREE_HIDDEN,
//                noTreeInEditor.getSelection());
        refreshEnabledStatuses();
        return super.performOk();
    }
    
    
    /**
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        IPreferenceStore prefs = getPreferenceStore();
        keyGroupSeparator.setText(
                prefs.getDefaultString(MsgEditorPreferences.GROUP__LEVEL_SEPARATOR));
        filterLocales.setText(
                prefs.getDefaultString(MsgEditorPreferences.FILTER_LOCALES_STRING_MATCHERS));
        convertEncodedToUnicode.setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.UNICODE_UNESCAPE_ENABLED));
        supportNL.setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.NL_SUPPORT_ENABLED));
        keyTreeHierarchical.setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.KEY_TREE_HIERARCHICAL));
        keyTreeHierarchical.setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.KEY_TREE_EXPANDED));
        fieldTabInserts.setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.FIELD_TAB_INSERTS));
        setupRbeNatureAutomatically.setSelection(prefs.getDefaultBoolean(
                MsgEditorPreferences.ADD_MSG_EDITOR_BUILDER_TO_JAVA_PROJECTS));
        
        refreshEnabledStatuses();
        super.performDefaults();
    }
    
    private void refreshEnabledStatuses() {
    }

}
