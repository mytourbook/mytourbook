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

import org.eclipse.babel.core.message.MessagesBundleGroup;
import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

/**
 * A rename processor for {@link IResource}. The processor will rename the resource and
 * load rename participants if references should be renamed as well.
 *
 * @since 3.4
 */
public class RenameKeyProcessor extends RenameProcessor {

	private KeyTreeNode fKeyNode;

	private MessagesBundleGroup fMessageBundleGroup;

	private String fNewResourceName;

	private boolean fRenameChildKeys;

	private RenameKeyArguments fRenameArguments; // set after checkFinalConditions

	/**
	 * Creates a new rename resource processor.
	 *
	 * @param keyNode the resource to rename.
	 * @param messagesBundleGroup 
	 */
	public RenameKeyProcessor(KeyTreeNode keyNode, MessagesBundleGroup messagesBundleGroup) {
		if (keyNode == null) {
			throw new IllegalArgumentException("key node must not be null"); //$NON-NLS-1$
		}

		fKeyNode = keyNode;
		fMessageBundleGroup = messagesBundleGroup;
		fRenameArguments= null;
		fRenameChildKeys= true;
		setNewResourceName(keyNode.getMessageKey()); // Initialize new name
	}

	/**
	 * Returns the new key node
	 *
	 * @return the new key node
	 */
	public KeyTreeNode getNewKeyTreeNode() {
		return fKeyNode;
	}

	/**
	 * Returns the new resource name
	 *
	 * @return the new resource name
	 */
	public String getNewResourceName() {
		return fNewResourceName;
	}

	/**
	 * Sets the new resource name
	 *
	 * @param newName the new resource name
	 */
	public void setNewResourceName(String newName) {
		Assert.isNotNull(newName);
		fNewResourceName= newName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		/*
		 * This method allows fatal and non-fatal problems to be shown to
		 * the user.  Currently there are none so we return null to indicate
		 * this. 
		 */
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			fRenameArguments = new RenameKeyArguments(getNewResourceName(), fRenameChildKeys, false);

			ResourceChangeChecker checker = (ResourceChangeChecker) context.getChecker(ResourceChangeChecker.class);
			IResourceChangeDescriptionFactory deltaFactory = checker.getDeltaFactory();

			// TODO figure out what we want to do here....
//			ResourceModifications.buildMoveDelta(deltaFactory, fKeyNode, fRenameArguments);

			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	/**
	 * Validates if the a name is valid. This method does not change the name settings on the refactoring. It is intended to be used
	 * in a wizard to validate user input.
	 *
	 * @param newName the name to validate
	 * @return returns the resulting status of the validation
	 */
	public RefactoringStatus validateNewElementName(String newName) {
		Assert.isNotNull(newName);

		if (newName.length() == 0) {
        	return RefactoringStatus.createFatalErrorStatus("New name for key must be entered");
		}
		if (newName.startsWith(".")) {
        	return RefactoringStatus.createFatalErrorStatus("Key cannot start with a '.'");
		}
		if (newName.endsWith(".")) {
            return RefactoringStatus.createFatalErrorStatus("Key cannot end with a '.'");
		}
		
		String [] parts = newName.split("\\.");
		for (String part : parts) {
			if (part.length() == 0) {
	            return RefactoringStatus.createFatalErrorStatus("Key cannot contain an empty part between two periods");
			}
			if (!part.matches("([A-Z]|[a-z]|[0-9])*")) {
	            return RefactoringStatus.createFatalErrorStatus("Key can contain only letters, digits, and periods");
			}
		}
		
		if (fMessageBundleGroup.isMessageKey(newName)) {
        	return RefactoringStatus.createFatalErrorStatus(MessagesEditorPlugin.getString("dialog.error.exists"));
		}

       	return new RefactoringStatus();
	}

	protected RenameKeyDescriptor createDescriptor() {
		RenameKeyDescriptor descriptor= new RenameKeyDescriptor();
		descriptor.setDescription(MessageFormat.format("Rename resource bundle key ''{0}''", fKeyNode.getMessageKey()));
		descriptor.setComment(MessageFormat.format("Rename resource ''{0}'' to ''{1}''", new Object[] { fKeyNode.getMessageKey(), fNewResourceName }));
		descriptor.setFlags(RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE);
		descriptor.setNewName(getNewResourceName());
		descriptor.setRenameChildKeys(fRenameChildKeys);
		return descriptor;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			RenameKeyChange change = new RenameKeyChange(fMessageBundleGroup, getNewKeyTreeNode(), fNewResourceName, fRenameChildKeys);
			change.setDescriptor(new RefactoringChangeDescriptor(createDescriptor()));
			return change;
		} finally {
			pm.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public Object[] getElements() {
		return new Object[] { fKeyNode };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public String getIdentifier() {
		return "org.eclipse.babel.editor.refactoring.renameKeyProcessor"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public String getProcessorName() {
		return "Rename Resource Bundle Key";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public boolean isApplicable() {
		if (this.fKeyNode == null)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#loadParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		// TODO: figure out participants to return here
		return new RefactoringParticipant[0];
		
//		String[] affectedNatures= ResourceProcessors.computeAffectedNatures(fResource);
//		return ParticipantManager.loadRenameParticipants(status, this, fResource, fRenameArguments, null, affectedNatures, shared);
	}

	/**
	 * Returns <code>true</code> if the refactoring processor also renames the child keys
	 *
	 * @return <code>true</code> if the refactoring processor also renames the child keys
	 */
	public boolean getRenameChildKeys() {
		return fRenameChildKeys;
	}

	/**
	 * Specifies if the refactoring processor also updates the child keys. 
	 * The default behaviour is to update the child keys.
	 *
	 * @param renameChildKeys <code>true</code> if the refactoring processor should also rename the child keys
	 */
	public void setRenameChildKeys(boolean renameChildKeys) {
		fRenameChildKeys = renameChildKeys;
	}

}