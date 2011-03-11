/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.babel.build.ui.wizard;

import org.eclipse.swt.widgets.Shell;

public class LanguagePackFilterDialog extends FilterDialog {
	
	public LanguagePackFilterDialog(final Shell parent, final BuildToolWizardConfigurationPage caller) {
		super(parent, caller);
		
		parent.setText(Messages.getString("LanguagePackFilterDialog_Title"));
	}
	
	protected void setChosenFilter(String filter) {
		fCallingPage.setLanguagePackFilter(filter);
	}
}
