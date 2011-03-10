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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.swt.widgets.Composite;

public class BuildToolWizardPage extends WizardPage implements IModelProviderListener{

	public BuildToolWizardPage(String pageName) {
		super(pageName);
	}
	
	public void createControl(Composite parent) {
	}
	
	public void modelsChanged(IModelProviderEvent event) {
	}
	
	public void storeSettings(){}

	protected <T> T not_null(T... ts) {
		for(T t : ts){
			if(t != null){
				return t;
			}
		}
		return null;
	}
}
