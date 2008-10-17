/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;

public class QuickEditDialog extends TitleAreaDialog implements ITourProvider {

	private Text					fTextTitle;
	private Text					fTextDescription;

	private TourData				fTourData;

	private final IDialogSettings	fDialogSettings;
	private Link					fTagLink;
	private Label					fLblTourTags;
	private Link					fTourTypeLink;
	private CLabel					fLblTourType;

	private ActionSetTourTag		fActionAddTag;
	private ActionSetTourTag		fActionRemoveTag;
	private ActionRemoveAllTags		fActionRemoveAllTags;
	private ActionOpenPrefDialog	fActionOpenTagPrefs;
	private ActionOpenPrefDialog	fActionOpenTourTypePrefs;

	private static final Calendar	fCalendar			= GregorianCalendar.getInstance();
	private static final DateFormat	fDateFormatter		= DateFormat.getDateInstance(DateFormat.FULL);
	private static final DateFormat	fTimeFormatter		= DateFormat.getTimeInstance(DateFormat.SHORT);

	/**
	 * this width is used as a hint for the width of the description field, this value also
	 * influences the width of the columns in this editor
	 */
	private int						fTextColumnWidth	= 150;
	private ITourPropertyListener	fTourPropertyListener;

	public QuickEditDialog(final Shell parentShell, final TourData tourData) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fTourData = tourData;
		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part, final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED && propertyData instanceof TourProperties) {

					final TourProperties tourProperties = (TourProperties) propertyData;
					final ArrayList<TourData> modifiedTours = tourProperties.getModifiedTours();
					if (modifiedTours != null) {

						// get modified tours

						final long viewTourId = fTourData.getTourId();

						// update modified tour
						for (final TourData tourData : modifiedTours) {
							if (tourData.getTourId() == viewTourId) {

								// get modified tour
								fTourData = tourData;

								updateUI();

								// there can be only one tour
								return;
							}
						}
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	@Override
	public void create() {

		super.create();

		getShell().setText(Messages.dialog_quick_edit_dialog_title);
		setTitle(Messages.dialog_quick_edit_dialog_area_title);

		fCalendar.set(fTourData.getStartYear(),
				fTourData.getStartMonth() - 1,
				fTourData.getStartDay(),
				fTourData.getStartHour(),
				fTourData.getStartMinute());

		setMessage(fDateFormatter.format(fCalendar.getTime()) + "  " + fTimeFormatter.format(fCalendar.getTime())); //$NON-NLS-1$
	}

	private void createActions() {

		/*
		 * tag actions
		 */
		fActionAddTag = new ActionSetTourTag(this, true, false);
		fActionRemoveTag = new ActionSetTourTag(this, false, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this, false);
		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fActionOpenTourTypePrefs = new ActionOpenPrefDialog(Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText(Messages.dialog_quick_edit_dialog_save);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(dlgAreaContainer);
		createActions();
		createMenus();

		// add listener
		addTourPropertyListener();
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				TourManager.getInstance().removePropertyListener(fTourPropertyListener);
			}
		});

		updateUI();

		return dlgAreaContainer;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		/*
		 * tag menu
		 */
		MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final boolean isTagSet = fTourData.getTourTags().size() > 0;

				// enable actions
				fActionRemoveTag.setEnabled(isTagSet);
				fActionRemoveAllTags.setEnabled(isTagSet);

				// set menu items
				menuMgr.add(fActionAddTag);
				menuMgr.add(fActionRemoveTag);
				menuMgr.add(fActionRemoveAllTags);

				TagManager.fillRecentTagsIntoMenu(menuMgr, QuickEditDialog.this, true, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTagPrefs);
			}
		});

		// set menu for the tag item
		fTagLink.setMenu(menuMgr.createContextMenu(fTagLink));

		/*
		 * tour type menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourType.fillMenu(menuMgr, QuickEditDialog.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		fTourTypeLink.setMenu(menuMgr.createContextMenu(fTourTypeLink));
	}

	private void createUI(final Composite parent) {

		Label label;
		final GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);

		final PixelConverter pixelConverter = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

		// title
		label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_editor_label_tour_title);
		fTextTitle = new Text(container, SWT.BORDER);
		fTextTitle.setLayoutData(gd);

		// description
		label = new Label(container, SWT.NONE);
		label.setText(Messages.tour_editor_label_description);
		label.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));
		fTextDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory//
		.fillDefaults()
				.grab(true, true)
				.hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(4))
				.applyTo(fTextDescription);

		/*
		 * tags
		 */
		fTagLink = new Link(container, SWT.NONE);
		fTagLink.setText(Messages.tour_editor_label_tour_tag);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(fTagLink);
		fTagLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(fTagLink);
			}
		});

		fLblTourTags = new Label(container, SWT.WRAP);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				// hint is necessary that the width is not expanded when the text is long
				.hint(fTextColumnWidth, SWT.DEFAULT)
				.applyTo(fLblTourTags);

		/*
		 * tour type
		 */
		fTourTypeLink = new Link(container, SWT.NONE);
		fTourTypeLink.setText(Messages.tour_editor_label_tour_type);
		fTourTypeLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(fTourTypeLink);
			}
		});

		fLblTourType = new CLabel(container, SWT.NONE);
		GridDataFactory.swtDefaults()//
				.grab(true, false)
				.applyTo(fLblTourType);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(fTourData);

		return selectedTours;
	}

	@Override
	protected void okPressed() {

		/*
		 * update tourdata from the fields
		 */
		fTourData.setTourTitle(fTextTitle.getText().trim());
		fTourData.setTourDescription(fTextDescription.getText().trim());

		super.okPressed();
	}

	private void updateUI() {

		// set field content
		fTextTitle.setText(fTourData.getTourTitle());
		fTextDescription.setText(fTourData.getTourDescription());

		UI.updateUITourType(fTourData, fLblTourType);
		UI.updateUITags(fTourData, fLblTourTags);
	}
}
