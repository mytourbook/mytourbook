/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.common.tooltip;

import net.tourbook.common.UI;
import net.tourbook.common.form.SashLeftFixedForm;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for the tour tag filter
 */
public class SlideoutTest extends AdvancedSlideout {

	private static final String		STATE_SASH_WIDTH_CONTAINER		= "STATE_SASH_WIDTH_CONTAINER";		//$NON-NLS-1$
	private static final String		STATE_SASH_WIDTH_TAG_CONTAINER	= "STATE_SASH_WIDTH_TAG_CONTAINER";	//$NON-NLS-1$

	private static IDialogSettings	_state;
	private ToolItem				_tourTagFilterItem;
	private PixelConverter			_pc;

	/**
	 * @param toolItem
	 * @param state
	 */
	public SlideoutTest(final ToolItem toolItem,
						final IDialogSettings state) {

		super(
				toolItem.getParent(),
				state,
				new int[] { 700, 400, 700, 400 });

		_tourTagFilterItem = toolItem;
		_state = state;

		setShellFadeOutDelaySteps(30);
//		setTitleText(Messages.Slideout_TourTagFilter_Label_Title);
	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		final double uniqueId = Math.random();
		final String stateSectionName = "TourTagFilterSlideout" + uniqueId;

		System.out.println(
				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] createSlideoutContent()")
						+ ("\t: " + stateSectionName));
// TODO remove SYSTEM.OUT.PRINTLN

//		_state = TourbookPlugin.getState(stateSectionName);


		initUI(parent);
		createUI(parent);
	}

	private void createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{
			final Composite sashContainer = new Composite(shellContainer, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(sashContainer);
			GridLayoutFactory.swtDefaults().applyTo(sashContainer);
			{
				// left part
				final Composite containerProfiles = createUI_200_Profiles(sashContainer);

				// sash
				final Sash sash = new Sash(sashContainer, SWT.VERTICAL);

				// right part
				final Composite containerTags = createUI_300_Tags(sashContainer);

				new SashLeftFixedForm(//
						sashContainer,
						containerProfiles,
						sash,
						containerTags,
						_state,
						STATE_SASH_WIDTH_CONTAINER,
						33);
			}

			/**
			 * Very Important !
			 * <p>
			 * Do a layout NOW, otherwise the initial profile container is using the whole width of
			 * the slideout :-(
			 * <p>
			 * It was also necessary to set the width hint for the left part, very very confusing
			 * but now it seams to layout properly
			 */
//			shellContainer.layout(true, true);
		}
	}

	private Composite createUI_200_Profiles(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		GridDataFactory
				.fillDefaults()
				//				.hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.extendedMargins(0, 3, 0, 0)
				.applyTo(container);
		{

		}

		return container;
	}

	private Composite createUI_300_Tags(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.extendedMargins(3, 0, 0, 0)
				.applyTo(container);
		{
//			createUI_320_TagContainer(container);
		}

		return container;
	}

	private void createUI_320_TagContainer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, true)
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			// left part
			final Composite containerTagList = createUI_330_TagCloud(container);

			// sash
			final Sash sash = new Sash(container, SWT.VERTICAL);

			// right part
			final Composite containerTagViewer = createUI_340_AllTags(container);

			new SashLeftFixedForm(//
					container,
					containerTagList,
					sash,
					containerTagViewer,
					_state,
					STATE_SASH_WIDTH_TAG_CONTAINER,
					50);
		}
	}

	private Composite createUI_330_TagCloud(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()
				.numColumns(1)
				.spacing(0, 2)
				.applyTo(container);
		{}

		return container;
	}

	private Composite createUI_340_AllTags(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()
				.spacing(0, 2)
				.applyTo(container);
		{}

		return container;
	}

	@Override
	protected Rectangle getParentBounds() {

		final Rectangle itemBounds = _tourTagFilterItem.getBounds();
		final Point itemDisplayPosition = _tourTagFilterItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

		itemBounds.x = itemDisplayPosition.x;
		itemBounds.y = itemDisplayPosition.y;

		return itemBounds;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

	}

	@Override
	protected void onFocus() {

	}

}
