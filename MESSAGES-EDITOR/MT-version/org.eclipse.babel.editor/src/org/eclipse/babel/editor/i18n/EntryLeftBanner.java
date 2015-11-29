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

import java.util.Locale;

import org.eclipse.babel.editor.i18n.actions.FoldingAction;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.babel.editor.widgets.ActionButton;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

/**
 * Tree for displaying and navigating through resource bundle keys.
 * @author Pascal Essiembre
 */
public class EntryLeftBanner extends Composite {

    /**
     * Constructor.
     * @param parent parent composite
     * @param keyTree key tree
     */
    public EntryLeftBanner(
            Composite parent,
            final I18NEntry i18NEntry) {
        super(parent, SWT.NONE);
        
        RowLayout layout = new RowLayout();
        setLayout(layout);
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        
        final IAction foldAction = new FoldingAction(i18NEntry);
        new ActionButton(this, foldAction);

//        Button commentButton = new Button(compos, SWT.TOGGLE);
//        commentButton.setImage(UIUtils.getImage("comment.gif"));
        
        
        Link localeLabel = new Link(this, SWT.NONE);
        localeLabel.setFont(UIUtils.createFont(localeLabel, SWT.BOLD));
        
        boolean isEditable = i18NEntry.isEditable();
        localeLabel.setText("<a>" + UIUtils.getDisplayName(
                i18NEntry.getLocale()) + "</a>" + (!isEditable
                        ? " (" +  MessagesEditorPlugin.getString(
                                "editor.readOnly") + ")" : ""));
        
        localeLabel.setToolTipText(
    			MessagesEditorPlugin.getString("editor.i18nentry.resourcelocation",
    					i18NEntry.getResourceLocationLabel())); //$NON-NLS-1$
        
        localeLabel.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                i18NEntry.getResourceBundleEditor().setActivePage(
                        i18NEntry.getLocale());
            }
        });

        //TODO have "show country flags" in preferences.
        //TODO have text aligned bottom next to flag icon.
        Image countryIcon = loadCountryIcon(i18NEntry.getLocale());
        if (countryIcon != null) {
            Label imgLabel = new Label(this, SWT.NONE);
            imgLabel.setImage(countryIcon);
        }
    }

    
    /**
     * Loads country icon based on locale country.
     * @param countryLocale the locale on which to grab the country
     * @return an image, or <code>null</code> if no match could be made
     */
    private Image loadCountryIcon(Locale countryLocale) {
        Image image = null;
        String countryCode = null;
        if (countryLocale != null && countryLocale.getCountry() != null) {
            countryCode = countryLocale.getCountry().toLowerCase();
        }
        if (countryCode != null && countryCode.length() > 0) {
            String imageName = "countries/" + //$NON-NLS-1$
                    countryCode.toLowerCase() + ".gif"; //$NON-NLS-1$
            image = UIUtils.getImage(imageName);
        }
//        if (image == null) {
//            image = UIUtils.getImage("countries/blank.gif"); //$NON-NLS-1$
//        }
        return image;
    }

}
