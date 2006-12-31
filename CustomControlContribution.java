/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.tourbook.ui;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * this is a copy of ControlContribution which will force the height for the
 * toolbar with a image in a push toolitem. this is a really hack but there was
 * no other solution to enlarge the height for a toolbar
 */
public abstract class CustomControlContribution extends ContributionItem {
	private Image	image;

	/**
	 * Creates a control contribution item with the given id.
	 * 
	 * @param id
	 *        the contribution item id
	 */
	protected CustomControlContribution(String id) {
		super(id);
	}

	/**
	 * Computes the width of the given control which is being added to a tool
	 * bar. This is needed to determine the width of the tool bar item
	 * containing the given control.
	 * <p>
	 * The default implementation of this framework method returns
	 * <code>control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x</code>.
	 * Subclasses may override if required.
	 * </p>
	 * 
	 * @param control
	 *        the control being added
	 * @return the width of the control
	 */
	protected int computeWidth(Control control) {
		return control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
	}

	/**
	 * Creates and returns the control for this contribution item under the
	 * given parent composite.
	 * <p>
	 * This framework method must be implemented by concrete subclasses.
	 * </p>
	 * 
	 * @param parent
	 *        the parent composite
	 * @return the new control
	 */
	protected abstract Control createControl(Composite parent);

	/**
	 * The control item implementation of this <code>IContributionItem</code>
	 * method calls the <code>createControl</code> framework method.
	 * Subclasses must implement <code>createControl</code> rather than
	 * overriding this method.
	 */
	public final void fill(Composite parent) {
		createControl(parent);
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code>
	 * method throws an exception since controls cannot be added to menus.
	 */
	public final void fill(Menu parent, int index) {
		Assert.isTrue(false, "Can't add a control to a menu");//$NON-NLS-1$
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code>
	 * method calls the <code>createControl</code> framework method to create
	 * a control under the given parent, and then creates a new tool item to
	 * hold it. Subclasses must implement <code>createControl</code> rather
	 * than overriding this method.
	 */
	public final void fill(ToolBar toolbar, int index) {

		Control control = createControl(toolbar);

		ToolItem ti = new ToolItem(toolbar, SWT.SEPARATOR);
		ti.setControl(control);
		ti.setWidth(computeWidth(control));

		// create dummy item to force the height
		int height = control.getSize().y;
		ImageData imageData = new ImageData(1, height, 1, new PaletteData(
				new RGB[] { new RGB(0, 0, 0) }));
		imageData.transparentPixel = 0;
		image = new Image(Display.getCurrent(), imageData);

		ToolItem imageItem = new ToolItem(toolbar, SWT.PUSH);
		imageItem.setImage(image);
	}

	public void dispose() {
		image.dispose();
	}

}
