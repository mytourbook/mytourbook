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

import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * {@link Change} that renames a resource bundle key.
 */
public class RenameKeyChange extends Change {

	private final MessagesBundleGroup fMessagesBundleGroup;

	private final String fNewName;

	private final boolean fRenameChildKeys;

	private final KeyTreeNode fKeyTreeNode;

	private ChangeDescriptor fDescriptor;

	/**
	 * Creates the change.
	 *
	 * @param keyTreeNode the node in the model to rename
	 * @param newName the new name. Must not be empty
	 * @param renameChildKeys true if child keys are also to be renamed, false if just this one key is to be renamed
	 */
	protected RenameKeyChange(MessagesBundleGroup messageBundleGroup, KeyTreeNode keyTreeNode, String newName, boolean renameChildKeys) {
		if (keyTreeNode == null || newName == null || newName.length() == 0) {
			throw new IllegalArgumentException();
		}

		fMessagesBundleGroup = messageBundleGroup;
		fKeyTreeNode= keyTreeNode;
		fNewName= newName;
		fRenameChildKeys = renameChildKeys;
		fDescriptor= null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#getDescriptor()
	 */
	public ChangeDescriptor getDescriptor() {
		return fDescriptor;
	}

	/**
	 * Sets the change descriptor to be returned by {@link Change#getDescriptor()}.
	 *
	 * @param descriptor the change descriptor
	 */
	public void setDescriptor(ChangeDescriptor descriptor) {
		fDescriptor= descriptor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#getName()
	 */
	public String getName() {
		return MessageFormat.format("Rename {0} to {1}", new Object [] { fKeyTreeNode.getMessageKey(), fNewName});
	}

	/**
	 * Returns the new name.
	 *
	 * @return return the new name
	 */
	public String getNewName() {
		return fNewName;
	}

	/**
	 * This implementation of {@link Change#isValid(IProgressMonitor)} tests the modified resource using the validation method
	 * specified by {@link #setValidationMethod(int)}.
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 2); //$NON-NLS-1$
		try {
			RefactoringStatus result = new RefactoringStatus();
			return result;
		} finally {
			pm.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#initializeValidationData(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void initializeValidationData(IProgressMonitor pm) {
	}

	public Object getModifiedElement() {
		return "what is this for?";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("Rename resource bundle key", 1);

			// Find the root - we will need this later
			KeyTreeNode root = fKeyTreeNode.getParent();
			while (root.getName() != null) {
				root = root.getParent();
			}
			
			if (fRenameChildKeys) {
				String key = fKeyTreeNode.getMessageKey();
				String keyPrefix = fKeyTreeNode.getMessageKey() + ".";
				Collection<KeyTreeNode> branchNodes = fKeyTreeNode.getBranch();
				for (KeyTreeNode branchNode : branchNodes) {
					String oldKey = branchNode.getMessageKey();
					if (oldKey.equals(key) || oldKey.startsWith(keyPrefix)) {
						String newKey = fNewName + oldKey.substring(key.length());
						fMessagesBundleGroup.renameMessageKeys(oldKey, newKey);
					}
				}
			} else {
				fMessagesBundleGroup.renameMessageKeys(fKeyTreeNode.getMessageKey(), fNewName);
			}
			
			String oldName= fKeyTreeNode.getMessageKey();
		
			// Find the node that was created with the new name
			String segments [] = fNewName.split("\\.");
			KeyTreeNode renamedKey = root;
			for (String segment : segments) {
				renamedKey = renamedKey.getChild(segment);
			}
			
			assert(renamedKey != null);
			return new RenameKeyChange(fMessagesBundleGroup, renamedKey, oldName, fRenameChildKeys);
		} finally {
			pm.done();
		}
	}
}
