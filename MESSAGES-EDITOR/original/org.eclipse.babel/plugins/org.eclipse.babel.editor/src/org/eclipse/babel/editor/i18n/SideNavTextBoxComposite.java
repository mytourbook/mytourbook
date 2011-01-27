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
package org.eclipse.babel.editor.i18n;

import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.babel.core.message.tree.visitor.NodePathRegexVisitor;
import org.eclipse.babel.editor.MessagesEditor;
import org.eclipse.babel.editor.MessagesEditorChangeAdapter;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Tree for displaying and navigating through resource bundle keys.
 * @author Pascal Essiembre
 */
public class SideNavTextBoxComposite extends Composite {

        
    /** Whether to synchronize the add text box with tree key selection. */
    private boolean syncAddTextBox = true;
    
    /** Text box to add a new key. */
    private Text addTextBox;

    private MessagesEditor editor;
    
    /**
     * Constructor.
     * @param parent parent composite
     * @param keyTree key tree
     */
    public SideNavTextBoxComposite(
            Composite parent,
            final MessagesEditor editor) {
        super(parent, SWT.NONE);
        this.editor = editor;

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.verticalAlignment = GridData.CENTER;
        gridData.grabExcessHorizontalSpace = true;
        setLayoutData(gridData);

        // Text box
        addTextBox = new Text(this, SWT.BORDER);
        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        addTextBox.setLayoutData(gridData);

        // Add button
        final Button addButton = new Button(this, SWT.PUSH);
        addButton.setText(MessagesEditorPlugin.getString("key.add")); //$NON-NLS-1$
        addButton.setEnabled(false);
        addButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                addKey(addTextBox.getText());
            }
        });

        addTextBox.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                String key = addTextBox.getText();
                if (event.character == SWT.CR && isNewKey(key)) {
                    addKey(key);
                } else if (key.length() > 0){
                    NodePathRegexVisitor visitor = new NodePathRegexVisitor(
                            "^" + key + ".*");  //$NON-NLS-1$//$NON-NLS-2$
                    editor.getKeyTreeModel().accept(visitor, null);
                    KeyTreeNode node = visitor.getKeyTreeNode();
                    if (node != null) {
                        syncAddTextBox = false;
                        editor.setSelectedKey(node.getMessageKey());
                    }
                }
            }
        });
        addTextBox.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                addButton.setEnabled(isNewKey(addTextBox.getText()));
            }
        });
        editor.addChangeListener(new MessagesEditorChangeAdapter() {
            public void selectedKeyChanged(String oldKey, String newKey) {
                if (syncAddTextBox && newKey != null) {
                    addTextBox.setText(newKey);
                }
                syncAddTextBox = true;
            }
        });
        
    }
    
    private void addKey(String key) {
        editor.getBundleGroup().addMessages(key);
        editor.setSelectedKey(key);
    }
    
    private boolean isNewKey(String key) {
        return !editor.getBundleGroup().isMessageKey(key) && key.length() > 0;
    }
}
