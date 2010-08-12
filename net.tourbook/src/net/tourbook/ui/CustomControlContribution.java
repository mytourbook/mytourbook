/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * !!! This is a copy of ControlContribution which will force the height for the toolbar with a
 * image in a push toolitem. this is a really hack but there was no other solution to enlarge the
 * height for a toolbar !!!
 */
public abstract class CustomControlContribution extends ContributionItem {

	private Image	_image;

	/**
	 * Creates a control contribution item with the given id.
	 * 
	 * @param id
	 *            the contribution item id
	 */
	protected CustomControlContribution(final String id) {
		super(id);
	}

	/**
	 * Computes the width of the given control which is being added to a tool bar. This is needed to
	 * determine the width of the tool bar item containing the given control.
	 * <p>
	 * The default implementation of this framework method returns
	 * <code>control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x</code>. Subclasses may override
	 * if required.
	 * </p>
	 * 
	 * @param control
	 *            the control being added
	 * @return the width of the control
	 */
	protected int computeWidth(final Control control) {
		return control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
	}

	/**
	 * Creates and returns the control for this contribution item under the given parent composite.
	 * <p>
	 * This framework method must be implemented by concrete subclasses.
	 * </p>
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the new control
	 */
	protected abstract Control createControl(Composite parent);

	@Override
	public void dispose() {
		_image.dispose();
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code> method calls the
	 * <code>createControl</code> framework method. Subclasses must implement
	 * <code>createControl</code> rather than overriding this method.
	 */
	@Override
	public final void fill(final Composite parent) {
		createControl(parent);
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code> method throws an
	 * exception since controls cannot be added to menus.
	 */
	@Override
	public final void fill(final Menu parent, final int index) {
		Assert.isTrue(false, "Can't add a control to a menu");//$NON-NLS-1$
	}

	/**
	 * The control item implementation of this <code>IContributionItem</code> method calls the
	 * <code>createControl</code> framework method to create a control under the given parent, and
	 * then creates a new tool item to hold it. Subclasses must implement <code>createControl</code>
	 * rather than overriding this method.
	 */
	@Override
	public final void fill(final ToolBar toolbar, final int index) {

		// toolbar is currently (10.7) two times created
		if (_image != null) {
			_image.dispose();
			_image = null;
		}

		final Control control = createControl(toolbar);
		final Point controlSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

		final ToolItem toolItem = new ToolItem(toolbar, SWT.SEPARATOR);
		toolItem.setControl(control);
		toolItem.setWidth(controlSize.x);
 
		final PaletteData palette = new PaletteData(new RGB[] { new RGB(0, 0, 0) });

		final ImageData imageData = new ImageData(1, controlSize.y, 1, palette);
//		final ImageData imageData = new ImageData(1, 1, 1, new PaletteData(new RGB[] { new RGB(0, 0, 0) }));
//		final ImageData imageData = new ImageData(1, //
//				//
////				textSize.y + 10,
//				controlSize.y,
//				1,
//				new PaletteData(new RGB[] { new RGB(0, 0, 0) }));
		imageData.transparentPixel = 0;
		_image = new Image(Display.getCurrent(), imageData);

		final ToolItem imageItem = new ToolItem(toolbar, SWT.PUSH);
		imageItem.setImage(_image);
	}
}
