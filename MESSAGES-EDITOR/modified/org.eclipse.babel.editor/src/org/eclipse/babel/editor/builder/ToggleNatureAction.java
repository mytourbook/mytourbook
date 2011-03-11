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
package org.eclipse.babel.editor.builder;

import org.eclipse.babel.core.util.BabelUtils;
import org.eclipse.babel.editor.util.UIUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class ToggleNatureAction implements IObjectActionDelegate {
	
	/**
	 * Method call during the start up of the plugin or during
	 * a change of the preference MsgEditorPreferences#ADD_MSG_EDITOR_BUILDER_TO_JAVA_PROJECTS.
	 * <p>
	 * Goes through the list of opened projects and either remove all the
	 * natures or add them all for each opened java project if the nature was not there.
	 * </p>
	 */
	public static void addOrRemoveNatureOnAllJavaProjects(boolean doAdd) {
		IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projs.length; i++) {
			IProject project = projs[i];
			addOrRemoveNatureOnProject(project, doAdd, true);
		}
	}
	
	/**
	 * 
	 * @param project The project to setup if necessary
	 * @param doAdd true to add, false to remove.
	 * @param onlyJavaProject when true the nature will be added or removed
	 * if and only if the project has a jdt-java nature 
	 */
	public static void addOrRemoveNatureOnProject(IProject project,
			boolean doAdd, boolean onlyJavaProject) {
		try {
			if (project.isAccessible() && (!onlyJavaProject ||
					UIUtils.hasNature(project, UIUtils.JDT_JAVA_NATURE))) {
				if (doAdd) {
					if (project.getNature(Nature.NATURE_ID) == null) {
						toggleNature(project);
					}
				} else {
					if (project.getNature(Nature.NATURE_ID) != null) {
						toggleNature(project);
					}
				}
			}
		} catch (CoreException ce) {
			ce.printStackTrace();//REMOVEME
		}

	}
	

	private ISelection selection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			for (Object element : ((IStructuredSelection) selection).toList()) {
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element)
							.getAdapter(IProject.class);
				}
				if (project != null) {
					toggleNature(project);
				}
			}
		}
	}

	/**
	 * Called when the selection is changed.
	 * Update the state of the action (enabled/disabled) and its label.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * Toggles sample nature on a project
	 * 
	 * @param project
	 *            to have sample nature added or removed
	 */
	private static void toggleNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			for (int i = 0; i < natures.length; ++i) {
				if (Nature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i,
							natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					return;
				}
			}

			// Add the nature
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = Nature.NATURE_ID;
			System.out.println("New natures: " + BabelUtils.join(newNatures, ", "));
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		} catch (CoreException e) {
		}
	}

}
