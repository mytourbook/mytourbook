/*******************************************************************************
 * Copyright (c) 2009 Nigel Westbury
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial implementation
 ******************************************************************************/
package org.eclipse.babel.editor.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

/**
 * This class contains the data that a processor provides to its rename resource
 * bundle key participants.
 */
public class RenameKeyArguments extends RefactoringArguments {

	private String fNewName;

	private boolean fRenameChildKeys;

	private boolean fUpdateReferences;

	/**
	 * Creates new rename arguments.
	 * 
	 * @param newName
	 *            the new name of the element to be renamed
	 * @param renameChildKeys
	 *            <code>true</code> if child keys are to be renamed;
	 *            <code>false</code> otherwise
	 * @param updateReferences
	 *            <code>true</code> if reference updating is requested;
	 *            <code>false</code> otherwise
	 */
	public RenameKeyArguments(String newName, boolean renameChildKeys, boolean updateReferences) {
		Assert.isNotNull(newName);
		fNewName= newName;
		fRenameChildKeys = renameChildKeys;
		fUpdateReferences= updateReferences;
	}

	/**
	 * Returns the new element name.
	 *
	 * @return the new element name
	 */
	public String getNewName() {
		return fNewName;
	}

	/**
	 * Returns whether child keys are to be renamed or not.
	 * 
	 * @return returns <code>true</code> if child keys are to be renamed;
	 *         <code>false</code> otherwise
	 */
	public boolean getRenameChildKeys() {
		return fRenameChildKeys;
	}

	/**
	 * Returns whether reference updating is requested or not.
	 * 
	 * @return returns <code>true</code> if reference updating is requested;
	 *         <code>false</code> otherwise
	 */
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}

	public String toString() {
		return "rename to " + fNewName //$NON-NLS-1$
		+ (fRenameChildKeys ? " (rename child keys)" : " (don't rename child keys)") //$NON-NLS-1$//$NON-NLS-2$
		+ (fUpdateReferences ? " (update references)" : " (don't update references)"); //$NON-NLS-1$//$NON-NLS-2$
	}
}
