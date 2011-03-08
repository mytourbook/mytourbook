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

import java.util.ArrayList;
import java.util.List;

public class BuildToolModelTable {
	private List<Object> fAvailableModels;
	private List<Object> fPreSelectedModels;

	public BuildToolModelTable() {
		fAvailableModels = new ArrayList<Object>();
		fPreSelectedModels = new ArrayList<Object>();
	}

	/**
	 * Adds the model to the model table. Takes into consideration the specified
	 * selection.
	 * @param model
	 * @param selected
	 */
	public void addToModelTable(Object model, boolean selected) {
		if (selected)
			fPreSelectedModels.add(model);
		else
			fAvailableModels.add(model);
	}

	/**
	 * Adds the model to the model table.
	 * @param model
	 */
	public void addModel(Object model) {
		fAvailableModels.add(model);
	}

	/**
	 * Removes the specified model from the model table.
	 * @param model
	 */
	public void removeModel(Object model) {
		fAvailableModels.remove(model);
	}
	
	public void addToPreselected(Object model) {
		fPreSelectedModels.add(model);
	}
	
	public void removeFromPreselected(Object model) {
		fPreSelectedModels.remove(model);
	}

	/**
	 * 
	 * @return the number of models in the table
	 */
	public int getModelCount() {
		return fPreSelectedModels.size() + fAvailableModels.size();
	}

	/**
	 * Returns the list of models stored in the model table
	 * @return the array of models
	 */
	public Object[] getModels() {
		return fAvailableModels.toArray();
	}

	/**
	 * Returns the list of preselected models stored in the model table
	 * @return the array of preselected models
	 */
	public Object[] getPreSelected() {
		return fPreSelectedModels.toArray();
	}

	/**
	 * 
	 * @return whether or not the model table contains preselected models
	 */
	public boolean hasPreSelected() {
		return fPreSelectedModels.size() > 0;
	}

	/**
	 * 
	 * @return whether or not the list of models is empty
	 */
	public boolean isEmpty() {
		return fAvailableModels.size() == 0;
	}
}