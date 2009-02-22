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
/**
 * @author Wolfgang Schramm
 * @author Alfred Barten
 */
package net.tourbook.ext.srtm;

import java.util.Collections;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogAdjustSRTMColors extends TitleAreaDialog {

	private static final int		maxColor		= 100;
	ColorChooser					colorChooser;
	private final IDialogSettings	fDialogSettings;

	// private ScrolledComposite compositeColorButtons;
	private Composite				compositeColorButtons;
	private Composite				compositeChooser;
	private static Button[]			colorButtons	= new Button[maxColor];
	private static Text[]			elevFields		= new Text[maxColor];
	private static Button[]			checkButtons	= new Button[maxColor];
	private RGBVertexList			rgbVertexList;

	public DialogAdjustSRTMColors(final Shell parentShell) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fDialogSettings = Activator.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.dialog_adjust_srtm_colors_dialog_title);
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_adjust_srtm_colors_dialog_area_title);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		Button addButton = createButton(parent, 42, Messages.dialog_adjust_srtm_colors_button_add, false);
		addButton.addListener(SWT.Selection | SWT.Dispose, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
					int rgbVertexListSize = rgbVertexList.size();
					disposeFields(rgbVertexListSize);
					RGBVertex rgbVertex = new RGBVertex();
					rgbVertexList.add(rgbVertexListSize, rgbVertex);
					setFields();
					break;
				case SWT.Dispose:
					break;
				}
			}
		});

		Button removeButton = createButton(parent, 43, Messages.dialog_adjust_srtm_colors_button_remove, false);
		removeButton.addListener(SWT.Selection | SWT.Dispose, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
					int rgbVertexListSize = rgbVertexList.size();
					for (int ix = rgbVertexListSize - 1; ix >= 0; ix--) {
						if (checkButtons[ix].getSelection()) {
							rgbVertexList.remove(ix);
						}
					}
					disposeFields(rgbVertexListSize);
					setFields();
					break;
				case SWT.Dispose:
					break;
				}
			}
		});

		Button sortButton = createButton(parent, 44, Messages.dialog_adjust_srtm_colors_button_sort, false);
		sortButton.addListener(SWT.Selection | SWT.Dispose, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
					sortColors();
					break;
				case SWT.Dispose:
					break;
				}
			}
		});

		super.createButtonsForButtonBar(parent);
		// set text for the OK button
		String okText = null;
		okText = Messages.app_action_save;
		getButton(IDialogConstants.OK_ID).setText(okText);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		Composite composite = (Composite) super.createDialogArea(parent);
		createUI(composite);
		updateUI();
		return composite;
	}

	private void createUI(final Composite parent) {

		final Composite compositeBase = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(compositeBase);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(compositeBase);
		compositeColorButtons = new Composite(compositeBase, SWT.NONE);
		compositeChooser = new Composite(compositeBase, SWT.NONE);

		colorChooser = new ColorChooser(compositeChooser);
//		final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
//		scrolledComposite.setExpandVertical(true);
//		scrolledComposite.setExpandHorizontal(true);
//
//		compositeColorButtons = new Composite(scrolledComposite, SWT.NONE);
//		GridLayoutFactory.fillDefaults().applyTo(compositeColorButtons);
//
//		scrolledComposite.setContent(compositeColorButtons);
//		scrolledComposite.addControlListener(new ControlAdapter() {
//			@Override
//			public void controlResized(final ControlEvent e) {
//				scrolledComposite.setMinSize(compositeColorButtons.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//			}
//		});

		setFields();

		colorChooser.setChooser();

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	@Override
	protected void okPressed() {
		sortColors();
		super.okPressed();
	}

	private void updateUI() {}

	@SuppressWarnings("unchecked")
	private void sortColors() {
		int rgbVertexListSize = rgbVertexList.size();
		rgbVertexList.clear();
		for (int ix = 0, ixn = 0; ix < rgbVertexListSize; ix++) {
			if (elevFields[ix].getText().equals("")) //$NON-NLS-1$
				continue; // remove empty fields
			Long elev = new Long(elevFields[ix].getText());
			RGBVertex rgbVertex = new RGBVertex();
			rgbVertex.setElev(elev.longValue());
			rgbVertex.setRGB(colorButtons[ix].getBackground().getRGB());
			rgbVertexList.add(ixn, rgbVertex);
			ixn++;
		}
		Collections.sort(rgbVertexList);
		disposeFields(rgbVertexListSize);
		setFields();
	}

	private void setFields() {
		compositeColorButtons.pack(); // necessary if # of fields doesn't change!
		GridDataFactory.fillDefaults().grab(false, false).applyTo(compositeColorButtons);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(compositeColorButtons);
		for (int ix = 0; ix < rgbVertexList.size(); ix++) {
			elevFields[ix] = new Text(compositeColorButtons, SWT.SINGLE);
			elevFields[ix].setText("" + ((RGBVertex) rgbVertexList.get(ix)).getElevation()); //$NON-NLS-1$
			// elevFields[ix].setSize(100, 50);TODO 
			elevFields[ix].setEditable(true);
			elevFields[ix].setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));

			colorButtons[ix] = new Button(compositeColorButtons, SWT.PUSH);
			colorButtons[ix].setBackground(new Color(compositeColorButtons.getDisplay(),
					((RGBVertex) rgbVertexList.get(ix)).getRGB()));
			colorButtons[ix].setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
			setButtonLayoutData(colorButtons[ix]);
			colorButtons[ix].setToolTipText(""
					+ ((RGBVertex) rgbVertexList.get(ix)).getRGB().red
					+ "/" //$NON-NLS-1$
					+ ((RGBVertex) rgbVertexList.get(ix)).getRGB().green
					+ "/" //$NON-NLS-1$
					+ ((RGBVertex) rgbVertexList.get(ix)).getRGB().blue);
			colorButtons[ix].addListener(SWT.Selection | SWT.Dispose, new Listener() {
				public void handleEvent(Event e) {
					switch (e.type) {
					case SWT.Selection:
//    					RGB rgb = new ColorDialog(compositeColorButtons.getShell()).open();
//    					if(rgb != null) {
//    						Color buttonColor = new Color(compositeColorButtons.getDisplay(), rgb); 
//    						((Button)(e.widget)).setBackground(buttonColor);
//    						sortColors();
//    					}
						Color buttonColor = new Color(compositeColorButtons.getDisplay(), colorChooser.getRGB());
						((Button) (e.widget)).setBackground(buttonColor);
						sortColors();

						break;
					case SWT.Dispose:
						// xxx.dispose();
						break;
					}
				}
			});

			checkButtons[ix] = new Button(compositeColorButtons, SWT.CHECK);
			checkButtons[ix].setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		}
		compositeColorButtons.pack();
	}

	private void disposeFields(int length) {
		for (int ix = 0; ix < length; ix++) {
			colorButtons[ix].dispose();
			elevFields[ix].dispose();
			checkButtons[ix].dispose();
		}
	}

	public void setRGBVertexList(RGBVertexList rgbVertexList) {
		this.rgbVertexList = rgbVertexList;
	}

	public RGBVertexList getRgbVertexList() {
		return rgbVertexList;
	}
}
