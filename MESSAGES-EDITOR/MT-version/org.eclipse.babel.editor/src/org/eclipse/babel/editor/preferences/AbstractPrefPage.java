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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Plugin base preference page.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public abstract class AbstractPrefPage extends PreferencePage implements
        IWorkbenchPreferencePage {

    /** Number of pixels per field indentation  */
    protected final int indentPixels = 20;
    
    /** Controls with errors in them. */
    protected final Map<Text,String> errors = new HashMap<Text,String>();
    
    /**
     * Constructor.
     */
    public AbstractPrefPage() {
        super();
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage
     *      #init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
        setPreferenceStore(
                MessagesEditorPlugin.getDefault().getPreferenceStore());
    }

    protected Composite createFieldComposite(Composite parent) {
        return createFieldComposite(parent, 0);
    }
    protected Composite createFieldComposite(Composite parent, int indent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = indent;
        gridLayout.marginHeight = 0;
        gridLayout.verticalSpacing = 0;
        composite.setLayout(gridLayout);
        return composite;
    }

    protected class IntTextValidatorKeyListener extends KeyAdapter {
        
        private String errMsg = null;
        
        /**
         * Constructor.
         * @param errMsg error message
         */
        public IntTextValidatorKeyListener(String errMsg) {
            super();
            this.errMsg = errMsg;
        }
        /**
         * @see org.eclipse.swt.events.KeyAdapter#keyPressed(
         *          org.eclipse.swt.events.KeyEvent)
         */
        public void keyReleased(KeyEvent event) {
            Text text = (Text) event.widget;
            String value = text.getText(); 
            event.doit = value.matches("^\\d*$"); //$NON-NLS-1$
            if (event.doit) {
                errors.remove(text);
                if (errors.isEmpty()) {
                    setErrorMessage(null);
                    setValid(true);
                } else {
                    setErrorMessage(
                            (String) errors.values().iterator().next());
                }
            } else {
                errors.put(text, errMsg);
                setErrorMessage(errMsg);
                setValid(false);
            }
        }
    }

    protected class DoubleTextValidatorKeyListener extends KeyAdapter {
        
        private String errMsg;
        private double minValue;
        private double maxValue;
        
        /**
         * Constructor.
         * @param errMsg error message
         */
        public DoubleTextValidatorKeyListener(String errMsg) {
            super();
            this.errMsg = errMsg;
        }
        /**
         * Constructor.
         * @param errMsg error message
         * @param minValue minimum value (inclusive)
         * @param maxValue maximum value (inclusive)
         */
        public DoubleTextValidatorKeyListener(
                String errMsg, double minValue, double maxValue) {
            super();
            this.errMsg = errMsg;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
        
        /**
         * @see org.eclipse.swt.events.KeyAdapter#keyPressed(
         *          org.eclipse.swt.events.KeyEvent)
         */
        public void keyReleased(KeyEvent event) {
            Text text = (Text) event.widget;
            String value = text.getText(); 
            boolean valid = value.length() > 0;
            if (valid) {
                valid = value.matches("^\\d*\\.?\\d*$"); //$NON-NLS-1$
            }
            if (valid && minValue != maxValue) {
                double doubleValue = Double.parseDouble(value);
                valid = doubleValue >= minValue && doubleValue <= maxValue;
            }
            event.doit = valid;
            if (event.doit) {
                errors.remove(text);
                if (errors.isEmpty()) {
                    setErrorMessage(null);
                    setValid(true);
                } else {
                    setErrorMessage(errors.values().iterator().next());
                }
            } else {
                errors.put(text, errMsg);
                setErrorMessage(errMsg);
                setValid(false);
            }
        }
    }
    
    protected void setWidthInChars(Control field, int widthInChars) {
        GridData gd = new GridData();
        gd.widthHint = UIUtils.getWidthInChars(field, widthInChars);
        field.setLayoutData(gd);
    }
}
