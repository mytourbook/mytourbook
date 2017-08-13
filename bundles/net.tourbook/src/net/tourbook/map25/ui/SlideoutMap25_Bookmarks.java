/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.ui;

import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map 2.5D properties slideout.
 */
public class SlideoutMap25_Bookmarks extends ToolbarSlideout {

	private PixelConverter	_pc;

	private Font			_boldFont;
	{
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	private Map25View _map25View;

	/*
	 * UI controls
	 */

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param map25View
	 */
	public SlideoutMap25_Bookmarks(	final Control ownerControl,
									final ToolBar toolBar,
									final Map25View map25View) {

		super(ownerControl, toolBar);

		_map25View = map25View;
	}

	private void createActions() {

	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);

			}
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		label.setFont(_boldFont);
		label.setText("Map Bookmarks");
		GridDataFactory
				.fillDefaults()//
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(label);
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.indent(_pc.convertWidthInCharsToPixels(5), 0)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

//		tbm.add(_actionRestoreDefaults);

		tbm.update(true);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

	}

	private void restoreState() {

	}

}
