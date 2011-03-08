/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime.actions;

import org.eclipse.babel.runtime.external.FormattedTranslatableText;
import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

class TextInputContentProvider implements IStructuredContentProvider, 
  ITreeContentProvider {
  	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  	}

  	public void dispose() {
  	}

  	public Object[] getElements(Object parent) {
  		return getChildren(parent);
  	}

  	public Object getParent(Object child) {
  		// This does not appear to be required to be implemented
  		return null;
  	}

  	public Object [] getChildren(Object parent) {
  		if (parent instanceof ITranslatableText[]) {
  			return (Object[])parent;
  		} else if (parent instanceof FormattedTranslatableText) {
  			return ((FormattedTranslatableText)parent).getDependentTexts();
  		}
  		return new Object[0];
  	}

  	public boolean hasChildren(Object parent) {
  		return (parent instanceof FormattedTranslatableText);
  	}
  }