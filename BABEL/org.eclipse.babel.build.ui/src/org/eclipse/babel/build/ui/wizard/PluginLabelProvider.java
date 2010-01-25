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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class PluginLabelProvider extends LabelProvider{

	public PluginLabelProvider() {
	}
	
	@Override
	public Image getImage(Object element) {
		//return BuildToolImages.PLUGIN.createImage();
		return null;
	}

	@Override
	public String getText(Object element) {
		return element == null ? "" : element.toString(); //$NON-NLS-1$
	}
}
