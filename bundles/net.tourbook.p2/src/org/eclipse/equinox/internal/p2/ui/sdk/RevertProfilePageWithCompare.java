/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.equinox.p2.ui.RevertProfilePage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Same as {@link RevertProfilePage} but adds a compare button to compare profiles.
 * 
 * @see RevertProfilePage
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.0
 */
public class RevertProfilePageWithCompare extends RevertProfilePage {

	private static final int COMPARE_ID = IDialogConstants.CLIENT_ID + 2;
	Button compareButton;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.about.InstallationPage#createPageButtons(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageButtons(Composite parent) {
		if (ProvisioningUI.getDefaultUI().getProfileId() == null)
			return;
		compareButton = createButton(parent, COMPARE_ID, ProvUIMessages.RevertProfilePage_CompareLabel);
		compareButton.setToolTipText(ProvUIMessages.RevertProfilePage_CompareTooltip);
		compareButton.setEnabled(computeCompareEnablement(getSelection()));
		super.createPageButtons(parent);
	}

	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case COMPARE_ID :
				compare();
				break;
			default :
				super.buttonPressed(buttonId);
				break;
		}
	}

	boolean computeCompareEnablement(IStructuredSelection structuredSelection) {
		// compare is enabled if there are two elements selected
		Object[] selection = structuredSelection.toArray();
		if (selection.length == 2) {
			for (int i = 0; i < selection.length; i++) {
				if (!(selection[i] instanceof RollbackProfileElement))
					return false;
			}
			return true;
		}
		return false;
	}

	protected void handleSelectionChanged(IStructuredSelection selection) {
		super.handleSelectionChanged(selection);
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				if (compareButton != null)
					compareButton.setEnabled(false);
			} else {
				// multiple selections, we can compare
				if (compareButton != null)
					compareButton.setEnabled(computeCompareEnablement(selection));
			}
		} else {
			// Nothing is selected
			if (compareButton != null)
				compareButton.setEnabled(false);
		}
	}

	private RollbackProfileElement[] getRollbackProfileElementsToCompare() {
		// expecting two items selected
		RollbackProfileElement[] result = new RollbackProfileElement[2];
		IStructuredSelection selection = getSelection();
		int i = 0;
		for (Object selected : selection.toList()) {
			if (selected != null && selected instanceof RollbackProfileElement) {
				result[i++] = (RollbackProfileElement) selected;
			}
			if (i == 2)
				break;
		}
		return result;
	}

	void compare() {
		final RollbackProfileElement[] rpe = getRollbackProfileElementsToCompare();
		CompareUI.openCompareDialog(new ProfileCompareEditorInput(rpe));
	}

	private class ProfileCompareEditorInput extends CompareEditorInput {
		private Object root;
		private ProvElementNode l;
		private ProvElementNode r;

		public ProfileCompareEditorInput(RollbackProfileElement[] rpe) {
			super(new CompareConfiguration());
			Assert.isTrue(rpe.length == 2);
			l = new ProvElementNode(rpe[0]);
			r = new ProvElementNode(rpe[1]);
		}

		protected Object prepareInput(IProgressMonitor monitor) {
			initLabels();
			Differencer d = new Differencer();
			root = d.findDifferences(false, monitor, null, null, l, r);
			return root;
		}

		private void initLabels() {
			CompareConfiguration cc = getCompareConfiguration();
			cc.setLeftEditable(false);
			cc.setRightEditable(false);
			cc.setLeftLabel(l.getName());
			cc.setLeftImage(l.getImage());
			cc.setRightLabel(r.getName());
			cc.setRightImage(r.getImage());
		}

		public String getOKButtonLabel() {
			return IDialogConstants.OK_LABEL;
		}
	}

	private class ProvElementNode implements IStructureComparator, ITypedElement, IStreamContentAccessor {
		private ProvElement pe;
		private IInstallableUnit iu;
		final static String BLANK = ""; //$NON-NLS-1$
		private String id = BLANK;

		public ProvElementNode(Object input) {
			pe = (ProvElement) input;
			iu = ProvUI.getAdapter(pe, IInstallableUnit.class);
			if (iu != null) {
				id = iu.getId();
			}
		}

		public Object[] getChildren() {
			Set<ProvElementNode> children = new HashSet<ProvElementNode>();
			if (pe instanceof RollbackProfileElement) {
				Object[] c = ((RollbackProfileElement) pe).getChildren(null);
				for (int i = 0; i < c.length; i++) {
					children.add(new ProvElementNode(c[i]));
				}
			} else if (pe instanceof InstalledIUElement) {
				Object[] c = ((InstalledIUElement) pe).getChildren(null);
				for (int i = 0; i < c.length; i++) {
					children.add(new ProvElementNode(c[i]));
				}
			}
			return children.toArray();
		}

		/**
		 * Implementation based on <code>id</code>.
		 * @param other the object to compare this <code>ProvElementNode</code> against.
		 * @return <code>true</code> if the <code>ProvElementNodes</code>are equal; <code>false</code> otherwise.
		 */
		public boolean equals(Object other) {
			if (other instanceof ProvElementNode)
				return id.equals(((ProvElementNode) other).id);
			return super.equals(other);
		}

		/**
		 * Implementation based on <code>id</code>.
		 * @return a hash code for this object.
		 */
		public int hashCode() {
			return id.hashCode();
		}

		public Image getImage() {
			return pe.getImage(null);
		}

		public String getName() {
			if (iu != null) {
				return iu.getProperty(IInstallableUnit.PROP_NAME, null);
			}
			return pe.getLabel(null);
		}

		public String getType() {
			return ITypedElement.UNKNOWN_TYPE;
		}

		public InputStream getContents() {
			String contents = BLANK;
			if (iu != null) {
				contents = iu.getVersion().toString();
			}
			return new ByteArrayInputStream(contents.getBytes());
		}
	}

}
