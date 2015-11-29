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
package org.eclipse.babel.editor.widgets;

import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;


/**
 * Special text control that regognized the difference between a 
 * <code>null</code> values and an empty string.  When a <code>null</code>
 * value is supplied, the control background is of a different color.
 * Pressing the backspace button when the field is currently empty will
 * change its value from empty string to <code>null</code>.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class NullableText extends Composite {

    private final Text text;
    private final Color defaultColor;
    private final Color nullColor;

    private boolean isnull;
    
    private KeyListener keyListener = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            if (SWT.BS == e.character) {
                if (text.getText().length() == 0) {
                    renderNull();
                }
            }
        }
        public void keyReleased(KeyEvent e) {
            if (text.getText().length() > 0) {
                renderNormal();
            }
        }
    };
    
    /**
     * Constructor.
     */
    public NullableText(Composite parent, int style) {
        super(parent, SWT.NONE);
        text = new Text(this, style);
        defaultColor = text.getBackground();
        nullColor = UIUtils.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
        
        GridLayout gridLayout = new GridLayout(1, false);        
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        setLayout(gridLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        setLayoutData(gd);

        initComponents();
    }

    public void setOrientation(int orientation) {
        text.setOrientation(orientation);
    }
    
    public void setText(String text) {
        isnull = text == null;
        if (isnull) {
            this.text.setText(""); //$NON-NLS-1$x
            renderNull();
        } else {
            this.text.setText(text);
            renderNormal();
        }
    }
    public String getText() {
        if (isnull) {
            return null;
        }
        return this.text.getText();
    }
    
    /**
     * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        text.setEnabled(enabled);
    }

    private void initComponents() {
        GridData gridData = new GridData(
                GridData.FILL, GridData.FILL, true, true);
        text.setLayoutData(gridData);

        text.addKeyListener(keyListener);
        
    }
    

    private void renderNull() {
        isnull = true;
        if (isEnabled()) {
            text.setBackground(nullColor);
//            try {
//                text.setBackgroundImage(UIUtils.getImage("null.bmp"));
//            } catch (Throwable t) {
//                t.printStackTrace();
//            }
        } else {
            text.setBackground(UIUtils.getSystemColor(
                    SWT.COLOR_WIDGET_BACKGROUND));
//            text.setBackgroundImage(null);
        }
    }
    private void renderNormal() {
        isnull = false;
        if (isEnabled()) {
            text.setBackground(defaultColor);
        } else {
            text.setBackground(UIUtils.getSystemColor(
                    SWT.COLOR_WIDGET_BACKGROUND));
        }
//          text.setBackgroundImage(null);
    }

    /**
     * @see org.eclipse.swt.widgets.Control#addFocusListener(
     *              org.eclipse.swt.events.FocusListener)
     */
    public void addFocusListener(FocusListener listener) {
        text.addFocusListener(listener);
    }
    /**
     * @see org.eclipse.swt.widgets.Control#addKeyListener(
     *              org.eclipse.swt.events.KeyListener)
     */
    public void addKeyListener(KeyListener listener) {
        text.addKeyListener(listener);
    }
    /**
     * @see org.eclipse.swt.widgets.Control#removeFocusListener(
     *              org.eclipse.swt.events.FocusListener)
     */
    public void removeFocusListener(FocusListener listener) {
        text.removeFocusListener(listener);
    }
    /**
     * @see org.eclipse.swt.widgets.Control#removeKeyListener(
     *              org.eclipse.swt.events.KeyListener)
     */
    public void removeKeyListener(KeyListener listener) {
        text.removeKeyListener(listener);
    }

    /**
     * @param editable true if editable false otherwise.
     * If never called it is editable by default.
     */
    public void setEditable(boolean editable) {
        text.setEditable(editable);
    }
    
}