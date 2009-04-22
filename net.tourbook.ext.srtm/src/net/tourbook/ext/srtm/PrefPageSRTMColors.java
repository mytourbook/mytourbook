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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.TableColumnDefinition;
import net.tourbook.util.UI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.byteholder.geoclipse.map.MapImageCache;
import de.byteholder.geoclipse.map.TileFactoryInfo;

public final class PrefPageSRTMColors extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer {

	private final static IPreferenceStore	fPrefStore				= Activator.getDefault().getPreferenceStore();

	private static final String				PROFILE_FILE_NAME		= "srtmprofiles.xml";								//$NON-NLS-1$
	private static final String				PROFILE_XML_ROOT		= "srtmprofiles";									//$NON-NLS-1$

	private static final String				MEMENTO_CHILD_PROFILE	= "profile";										//$NON-NLS-1$
	private static final String				TAG_PROFILE_ID			= "profileId";										//$NON-NLS-1$
	private static final String				TAG_NAME				= "name";											//$NON-NLS-1$
	private static final String				TAG_IMAGE_PATH			= "imagePath";										//$NON-NLS-1$
	private static final String				TAG_IS_SHADOW			= "isShadow";										//$NON-NLS-1$
	private static final String				TAG_SHADOW_VALUE		= "shadowValue";									//$NON-NLS-1$
	private static final String				TAG_RESOLUTION			= "resolution";									//$NON-NLS-1$

	private static final String				MEMENTO_CHILD_VERTEX	= "vertex";										//$NON-NLS-1$
	private static final String				TAG_ALTITUDE			= "altitude";										//$NON-NLS-1$
	private static final String				TAG_RED					= "red";											//$NON-NLS-1$
	private static final String				TAG_GREEN				= "green";											//$NON-NLS-1$
	private static final String				TAG_BLUE				= "blue";											//$NON-NLS-1$

	private final IDialogSettings			fState					= Activator.getDefault()
																			.getDialogSettingsSection("SRTMColors");	//$NON-NLS-1$

	private static ArrayList<SRTMProfile>	fProfileList			= new ArrayList<SRTMProfile>();
	private static SRTMProfile				fSelectedProfile		= null;

	private Button							fBtnEditProfile;
	private Button							fBtnAddProfile			= null;
	private Button							fBtnRemoveProfile		= null;
	private Button							fBtnDuplicateProfile;

	private int								fDefaultImageWidth		= 300;
	private int								fImageHeight			= 40;

	private int								fOldImageWidth			= -1;
	private Composite						fProfileContainer;

	private CheckboxTableViewer				fProfileViewer;
	private ColumnManager					fColumnManager;

	/**
	 * contains the table column widget for the profile color
	 */
	private TableColumn						fTcProfileImage;

	private TableColumnDefinition			fColDefImage;

	/**
	 * index of the profile image, this can be changed when the columns are reordered with the mouse
	 * or the column manager
	 */
	private int								fProfileImageColumn		= 0;

	private BooleanFieldEditor				fBooleanEditorApplyOption;

	private static int						fMaxProfileId;

	private static boolean					fIsCreateDefault;

	private class ProfileContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return fProfileList.toArray(new SRTMProfile[fProfileList.size()]);
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}

	}

	public class ProfileSorter extends ViewerSorter {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
			return ((SRTMProfile) obj1).getProfileName().compareTo(((SRTMProfile) obj2).getProfileName());
		}
	}

	/**
	 * save default profile into a xml file
	 */
	private static void createXmlDefaultProfiles() {

		BufferedWriter writer = null;

		try {

			int profileId = -1;

			final File file = getProfileFile();
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlRoot = getXMLRoot();

			IMemento profile = createXmlProfile(xmlRoot,//
					++profileId,
					"Default 1", //$NON-NLS-1$ 
					"profile-default-1", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, 0, 14, 76, 255);
			createXmlVertex(profile, 100, 198, 235, 197);
			createXmlVertex(profile, 200, 0, 102, 0);
			createXmlVertex(profile, 350, 255, 204, 153);
			createXmlVertex(profile, 500, 204, 153, 0);
			createXmlVertex(profile, 750, 153, 51, 0);
			createXmlVertex(profile, 1000, 102, 51, 0);
			createXmlVertex(profile, 1500, 92, 67, 64);
			createXmlVertex(profile, 2000, 204, 255, 255);

			profile = createXmlProfile(xmlRoot, //
					++profileId,
					"Default 2", //$NON-NLS-1$ 
					"profile-default-2", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, 0, 14, 76, 255);
			createXmlVertex(profile, 100, 179, 244, 129);
			createXmlVertex(profile, 200, 144, 239, 129);
			createXmlVertex(profile, 300, 104, 225, 172);
			createXmlVertex(profile, 400, 113, 207, 57);
			createXmlVertex(profile, 500, 255, 255, 0);
			createXmlVertex(profile, 600, 255, 153, 0);
			createXmlVertex(profile, 700, 153, 0, 0);
			createXmlVertex(profile, 800, 255, 0, 51);
			createXmlVertex(profile, 900, 255, 204, 204);
			createXmlVertex(profile, 1000, 204, 255, 255);

			profile = createXmlProfile(xmlRoot, //
					++profileId,
					"Default 3", //$NON-NLS-1$ 
					"profile-default-3", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, 0, 14, 76, 255);
			createXmlVertex(profile, 500, 166, 219, 156);
			createXmlVertex(profile, 1000, 51, 153, 0);
			createXmlVertex(profile, 2000, 102, 51, 0);
			createXmlVertex(profile, 3000, 51, 51, 51);
			createXmlVertex(profile, 4000, 204, 255, 255);
			createXmlVertex(profile, 8850, 255, 255, 255);

			profile = createXmlProfile(xmlRoot,//
					++profileId,
					"Default 4", //$NON-NLS-1$ 
					"profile-default-4", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, 0, 255, 255, 255);
			createXmlVertex(profile, 1000, 178, 81, 0);
			createXmlVertex(profile, 2000, 100, 0, 59);
			createXmlVertex(profile, 3000, 0, 102, 127);

			profile = createXmlProfile(xmlRoot,//
					++profileId,
					"Default 5", //$NON-NLS-1$ 
					"profile-default-5", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, 0, 0, 0, 255);
			createXmlVertex(profile, 1000, 127, 0, 215);
			createXmlVertex(profile, 2000, 255, 0, 0);
			createXmlVertex(profile, 3000, 195, 103, 0);
			createXmlVertex(profile, 4000, 190, 193, 0);
			createXmlVertex(profile, 5000, 122, 190, 0);
			createXmlVertex(profile, 6000, 20, 141, 0);
			createXmlVertex(profile, 7000, 105, 231, 202);
			createXmlVertex(profile, 8000, 255, 255, 255);

			profile = createXmlProfile(xmlRoot, //
					++profileId,
					"Default 6", //$NON-NLS-1$ 
					"profile-default-6", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, 0, 255, 255, 255);
			createXmlVertex(profile, 100, 92, 43, 0);
			createXmlVertex(profile, 150, 166, 77, 0);
			createXmlVertex(profile, 200, 106, 148, 0);
			createXmlVertex(profile, 250, 35, 161, 48);
			createXmlVertex(profile, 300, 54, 134, 255);
			createXmlVertex(profile, 350, 130, 255, 255);

			xmlRoot.save(writer);

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static IMemento createXmlProfile(	final XMLMemento xmlMemento,
												final int profileId,
												final String name,
												final String profileImagePath,
												final boolean isShadow,
												final float shadowValue,
												final String resolution) {

		final IMemento xmlProfile = xmlMemento.createChild(MEMENTO_CHILD_PROFILE);

		xmlProfile.putInteger(TAG_PROFILE_ID, profileId);
		xmlProfile.putString(TAG_NAME, name);
		xmlProfile.putString(TAG_IMAGE_PATH, profileImagePath);
		xmlProfile.putBoolean(TAG_IS_SHADOW, isShadow);
		xmlProfile.putString(TAG_RESOLUTION, resolution);
		xmlProfile.putFloat(TAG_SHADOW_VALUE, shadowValue);

		return xmlProfile;
	}

	private static void createXmlVertex(final IMemento mementoProfile,
										final int altitude,
										final int red,
										final int green,
										final int blue) {

		final IMemento xmlVertex = mementoProfile.createChild(MEMENTO_CHILD_VERTEX);
		xmlVertex.putInteger(TAG_ALTITUDE, altitude);
		xmlVertex.putInteger(TAG_RED, red);
		xmlVertex.putInteger(TAG_GREEN, green);
		xmlVertex.putInteger(TAG_BLUE, blue);
	}

	private static void disposeProfileImages() {
		for (final SRTMProfile profile : fProfileList) {
			if (profile != null) {
				profile.disposeImage();
			}
		}
	}

	private static File getProfileFile() {
		final IPath stateLocation = Platform.getStateLocation(Activator.getDefault().getBundle());
		final File file = stateLocation.append(PROFILE_FILE_NAME).toFile();
		return file;
	}

	public static SRTMProfile getSelectedProfile() {
		return fSelectedProfile;
	}

	private static XMLMemento getXMLRoot() {

		try {

			final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			// create root element
			final Element element = document.createElement(PROFILE_XML_ROOT);
			element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (final ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	/**
	 * load profiles and set profile to the previous profile
	 */
	public static void initVertexLists() {
		loadProfiles();
		restoreSelectedProfile();
	}

	/**
	 * load profiles from profile file
	 */
	private static void loadProfiles() {

		// cleanup existing profiles
		disposeProfileImages();
		fProfileList.clear();

		/*
		 * get profile file
		 */
		final File profileFile = getProfileFile();

		// check if file is available
		if (profileFile.exists() == false) {

			// create default profile
			createXmlDefaultProfiles();

			if (profileFile.exists() == false) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Default Profile", //$NON-NLS-1$
						NLS.bind("Profile file {0} could not be created", profileFile.getAbsolutePath())); //$NON-NLS-1$
				return;
			}
		}

		InputStreamReader reader = null;

		try {

			final ArrayList<RGBVertex> vertexList = new ArrayList<RGBVertex>();
			fMaxProfileId = -1;

			reader = new InputStreamReader(new FileInputStream(profileFile), "UTF-8"); //$NON-NLS-1$
			final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);

			/*
			 * read all profiles
			 */
			final IMemento[] xmlProfiles = xmlRoot.getChildren(MEMENTO_CHILD_PROFILE);
			for (final IMemento xmlProfile : xmlProfiles) {

				/*
				 * read profile properties
				 */
				final Integer profileId = xmlProfile.getInteger(TAG_PROFILE_ID);
				if (profileId == null) {
					continue;
				}

				final String profileName = xmlProfile.getString(TAG_NAME);
				if (profileName == null) {
					continue;
				}

				final String profilePath = xmlProfile.getString(TAG_IMAGE_PATH);
				if (profilePath == null) {
					continue;
				}

				final Boolean profileShadowState = xmlProfile.getBoolean(TAG_IS_SHADOW);
				if (profileShadowState == null) {
					continue;
				}

				Float profileShadowValue = xmlProfile.getFloat(TAG_SHADOW_VALUE);
				if (profileShadowValue == null) {
					// set default value
					profileShadowValue = SRTMProfile.DEFAULT_SHADOW_VALUE;
				}

				final String profileResolution = xmlProfile.getString(TAG_RESOLUTION);
				if (profileResolution == null) {
					continue;
				}

				/*
				 * read all vertexes
				 */
				vertexList.clear();
				final IMemento[] xmlVertexes = xmlProfile.getChildren(MEMENTO_CHILD_VERTEX);
				for (final IMemento xmlVertex : xmlVertexes) {

					final Integer altitude = xmlVertex.getInteger(TAG_ALTITUDE);
					if (altitude == null) {
						continue;
					}
					final Integer red = xmlVertex.getInteger(TAG_RED);
					if (red == null) {
						continue;
					}
					final Integer green = xmlVertex.getInteger(TAG_GREEN);
					if (green == null) {
						continue;
					}
					final Integer blue = xmlVertex.getInteger(TAG_BLUE);
					if (blue == null) {
						continue;
					}

					vertexList.add(new RGBVertex(red, green, blue, altitude));
				}

				/*
				 * create profile
				 */
				SRTMProfile profile;
				fProfileList.add(profile = new SRTMProfile());

				profile.setProfileId(profileId);
				profile.setProfileName(profileName);
				profile.setTilePath(profilePath);
				profile.setShadowState(profileShadowState);
				profile.setShadowValue(profileShadowValue);
				profile.setResolution(profileResolution);

				profile.setVertexList(vertexList);

				// get max profile id 
				fMaxProfileId = Math.max(fMaxProfileId, profileId);
			}

		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final WorkbenchException e) {
			e.printStackTrace();
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			if (fProfileList.size() == 0) {

				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Read Profiles", //$NON-NLS-1$
						"Profiles could not be created, creating default profiles."); //$NON-NLS-1$

				// prevent endless loops
				if (fIsCreateDefault) {
					fIsCreateDefault = false;
				} else {

					// create default profile
					createXmlDefaultProfiles();

					fIsCreateDefault = true;
					loadProfiles();
				}

			} else {

				// create profile key for the saved profile
				for (final SRTMProfile profile : fProfileList) {
					profile.createSavedProfileKey();
				}
			}
		}
	}

	private static void restoreSelectedProfile() {

		/*
		 * get selected profile
		 */
		final int prefProfileId = fPrefStore.getInt(IPreferences.SRTM_COLORS_SELECTED_PROFILE_ID);
		for (final SRTMProfile profile : fProfileList) {
			if (profile.getProfileId() == prefProfileId) {
				fSelectedProfile = profile;
				break;
			}
		}
		if (fSelectedProfile == null) {
			// set default profile
			fSelectedProfile = fProfileList.get(0);
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		initVertexLists();

		final Composite container = createUI(parent);

		reloadViewer();

		restoreState();

		// reselected profile
		selectProfileInViewer(fSelectedProfile);

		enableActions();

		return container;
	}

	/**
	 * Creates a new profile which is not yet attached to the model
	 * 
	 * @return
	 */
	private SRTMProfile createNewProfile() {

		final SRTMProfile profile = new SRTMProfile();

		final int newProfileId = ++fMaxProfileId;

		profile.setProfileId(newProfileId);
		profile.setProfileName(Messages.prefPage_srtm_default_profile_name);

		// make a unique tile path
		profile.setTilePath(Messages.prefPage_srtm_default_profile_path + "-" + newProfileId);//$NON-NLS-1$

		profile.setDefaultVertexes();

		return profile;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		final Composite profileContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(profileContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 0).applyTo(profileContainer);
//		profileContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUIProfileList(profileContainer);
			createUIButtons(profileContainer);
		}

		createUIApplyOption(parent);

		return container;
	}

	private void createUIApplyOption(final Composite parent) {

		/*
		 * checkbox: pace min/max value
		 */
		fBooleanEditorApplyOption = new BooleanFieldEditor(IPreferences.SRTM_APPLY_WHEN_PROFILE_IS_SELECTED,
				Messages.prefPage_srtm_profile_option_apply_when_selected,
				parent);
		fBooleanEditorApplyOption.setPreferenceStore(fPrefStore);
		fBooleanEditorApplyOption.setPage(this);
		fBooleanEditorApplyOption.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				if (((Boolean) event.getNewValue())) {
					// apply profile
					final Object firstElement = ((StructuredSelection) fProfileViewer.getSelection()).getFirstElement();
					if (firstElement instanceof SRTMProfile) {
						onSelectProfile((SRTMProfile) firstElement, true);
					}
				}
			}
		});
	}

	private void createUIButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			/*
			 * button: edit profile
			 */
			fBtnEditProfile = new Button(container, SWT.NONE);
			fBtnEditProfile.setText(Messages.prefPage_srtm_profile_edit);
			fBtnEditProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onEditProfile();
				}
			});
			setButtonLayoutData(fBtnEditProfile);

			/*
			 * button: add profile
			 */
			fBtnAddProfile = new Button(container, SWT.NONE);
			fBtnAddProfile.setText(Messages.prefPage_srtm_profile_add);
			fBtnAddProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddProfile();
				}
			});
			setButtonLayoutData(fBtnAddProfile);

			/*
			 * button: remove profile
			 */
			fBtnRemoveProfile = new Button(container, SWT.NONE);
			fBtnRemoveProfile.setText(Messages.prefPage_srtm_profile_remove);
			fBtnRemoveProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onRemoveProfile();
				}
			});
			setButtonLayoutData(fBtnRemoveProfile);

			/*
			 * button: duplicate profile
			 */
			fBtnDuplicateProfile = new Button(container, SWT.NONE);
			fBtnDuplicateProfile.setText(Messages.prefPage_srtm_profile_duplicate);
			fBtnDuplicateProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onDuplicateProfile();
				}
			});
			setButtonLayoutData(fBtnDuplicateProfile);

			/*
			 * button: adjust columns
			 */
			final Button btnAdjustColumns = new Button(container, SWT.NONE);
			btnAdjustColumns.setText(Messages.prefPage_srtm_btn_adjust_columns);
			btnAdjustColumns.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					fColumnManager.openColumnDialog();
				}
			});
			setButtonLayoutData(btnAdjustColumns);
			final GridData gd = (GridData) btnAdjustColumns.getLayoutData();
			gd.verticalIndent = 20;
		}
	}

	private void createUIProfileList(final Composite parent) {

		// define all columns for the viewer
		fColumnManager = new ColumnManager(this, fState);
		defineViewerColumns(parent);

		fProfileContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fProfileContainer);
		GridLayoutFactory.fillDefaults().applyTo(fProfileContainer);
//		fProfileContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUIProfileViewer(fProfileContainer);
	}

	private void createUIProfileViewer(final Composite parent) {

		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			public void handleEvent(final Event event) {

				if (event.index == fProfileImageColumn) {

					final TableItem item = (TableItem) event.item;
					final SRTMProfile profile = (SRTMProfile) item.getData();
					final Image image = profile.getImage(getImageWidth(), fImageHeight, true);

					if (image != null) {

						final Rectangle rect = image.getBounds();

						switch (event.type) {
						case SWT.MeasureItem:

							event.width += rect.width;
							event.height = Math.max(event.height, rect.height + 2);

							break;

						case SWT.PaintItem:

							final int x = event.x + event.width;
							final int offset = Math.max(0, (event.height - rect.height) / 2);
							event.gc.drawImage(image, x, event.y + offset);

							break;
						}
					}
				}
			}
		};
		table.addListener(SWT.MeasureItem, paintListener);
		table.addListener(SWT.PaintItem, paintListener);

		/*
		 * create viewer
		 */
		fProfileViewer = new CheckboxTableViewer(table);

		fColumnManager.createColumns(fProfileViewer);
		fTcProfileImage = fColDefImage.getTableColumn();
		fProfileImageColumn = fColDefImage.getCreateIndex();

		fProfileViewer.setContentProvider(new ProfileContentProvider());
		fProfileViewer.setSorter(new ProfileSorter());

		fProfileViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				final SRTMProfile checkedProfile = (SRTMProfile) event.getElement();

				// ignore the same profile
				if (fSelectedProfile != null && checkedProfile == fSelectedProfile) {

					// prevent unchecking selected profile
					event.getCheckable().setChecked(checkedProfile, true);

					return;
				}

				// select checked profile
				selectProfileInViewer(checkedProfile);
			}
		});

		fProfileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final Object firstElement = ((StructuredSelection) event.getSelection()).getFirstElement();
				if (firstElement instanceof SRTMProfile) {
					onSelectProfile((SRTMProfile) firstElement, false);
				}
			}
		});

		fProfileViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				onEditProfile();
			}
		});

	}

	private void defineViewerColumns(final Composite parent) {

		TableColumnDefinition colDef;
		final PixelConverter pixelConverter = new PixelConverter(parent);

		/*
		 * column: profile name
		 */
		colDef = new TableColumnDefinition(fColumnManager, "profileName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_name);
		colDef.setColumnHeader(Messages.profileViewer_column_label_name_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_name_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(vertexList.getProfileName());
			}
		});

		/*
		 * column: shadow state
		 */
		colDef = new TableColumnDefinition(fColumnManager, "shadowState", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_isShadow);
		colDef.setColumnHeader(Messages.profileViewer_column_label_isShadow_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_isShadow_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile profile = (SRTMProfile) cell.getElement();
				cell.setText(profile.isShadowState() ? Messages.app_ui_Y : Messages.app_ui_N);
			}
		});

		/*
		 * column: shadow value
		 */
		colDef = new TableColumnDefinition(fColumnManager, "shadowValue", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_shadowValue);
		colDef.setColumnHeader(Messages.profileViewer_column_label_shadowValue_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_shadowValue_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile profile = (SRTMProfile) cell.getElement();
				cell.setText(Float.toString(profile.getShadowValue()));
			}
		});

		/*
		 * column: resolution
		 */
		colDef = new TableColumnDefinition(fColumnManager, "resolution", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_resolution);
		colDef.setColumnHeader(Messages.profileViewer_column_label_resolution_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_resolution_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile profile = (SRTMProfile) cell.getElement();
				cell.setText(getResolutionUI(profile));
			}
		});

		/*
		 * column: color
		 */
		colDef = new TableColumnDefinition(fColumnManager, "color", SWT.LEAD); //$NON-NLS-1$
		fColDefImage = colDef;

		colDef.setColumnLabel(Messages.profileViewer_column_label_color);
		colDef.setColumnHeader(Messages.profileViewer_column_label_color_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_color_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(50));
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			/*
			 * !!! set dummy label provider, otherwise an error occures !!!
			 */
			@Override
			public void update(final ViewerCell cell) {}
		});

		colDef.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeImageColumn();
			}
		});

		/*
		 * column: image path
		 */
		colDef = new TableColumnDefinition(fColumnManager, "tileImagePath", SWT.LEAD); //$NON-NLS-1$
		colDef.setColumnLabel(Messages.profileViewer_column_label_imagePath);
		colDef.setColumnHeader(Messages.profileViewer_column_label_imagePath_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_imagePath_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(vertexList.getTilePath());
			}
		});

		/*
		 * column: id
		 */
		colDef = new TableColumnDefinition(fColumnManager, "profileId", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_id);
		colDef.setColumnHeader(Messages.profileViewer_column_label_id_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_id_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(Integer.toString(vertexList.getProfileId()));
			}
		});
	}

	private void deleteAllOfflineImages() {

		for (final SRTMProfile profile : fProfileList) {
			deleteOfflineImages(profile);
		}
	}

	/**
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
	 * Deletes all files and subdirectories. If a deletion fails, the method stops attempting to
	 * delete and returns false. <br>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * 
	 * @param directory
	 * @return Returns <code>true</code> if all deletions were successful
	 */
	private boolean deleteDir(final File directory) {

		if (directory.isDirectory()) {

			final String[] children = directory.list();

			for (int i = 0; i < children.length; i++) {
				final boolean success = deleteDir(new File(directory, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		final boolean isDeleted = directory.delete();

		return isDeleted;
	}

	/**
	 * Delete tile images for the given profile
	 * 
	 * @param profile
	 */
	private void deleteOfflineImages(final SRTMProfile profile) {

		try {

			final TileFactoryInfo srtmFactoryInfo = ElevationColor.getTileFactoryInfo();

			final String tileCacheOSPath = MapImageCache.getTileCacheOSPath();
			IPath tileCacheOSPathFolder = null;
			if (tileCacheOSPath != null) {
				tileCacheOSPathFolder = srtmFactoryInfo.getTileOSPathFolder(tileCacheOSPath);
			}
			if (tileCacheOSPathFolder == null) {
				return;
			}

			// get profile folder
			final File profileFolder = tileCacheOSPathFolder.append(profile.getTilePath()).toFile();
			if (profileFolder.exists() == false) {
				return;
			}

			// check if the folder contains files/folders
			final String[] fileList = profileFolder.list();
			if (fileList == null || fileList.length == 0) {
				return;
			}

			deleteDir(profileFolder);

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * delete all tile images which profile key has changed
	 */
	private void deleteOfflineImagesWhenModified() {

		try {

			final TileFactoryInfo srtmFactoryInfo = ElevationColor.getTileFactoryInfo();

			final String tileCacheOSPath = MapImageCache.getTileCacheOSPath();
			IPath tileCacheOSPathFolder = null;
			if (tileCacheOSPath != null) {
				tileCacheOSPathFolder = srtmFactoryInfo.getTileOSPathFolder(tileCacheOSPath);
			}

			if (tileCacheOSPathFolder == null) {
				return;
			}

			// loop: all profiles
			for (final SRTMProfile profile : fProfileList) {
				if (profile.getProfileKeyHashCode() != profile.getSavedProfileKeyHashCode()) {

					// profile key has changed

					// get profile folder
					final File profileFolder = tileCacheOSPathFolder.append(profile.getTilePath()).toFile();
					if (profileFolder.exists() == false) {
						continue;
					}

					// check if the folder contains files/folders
					final String[] fileList = profileFolder.list();
					if (fileList == null || fileList.length == 0) {
						continue;
					}

					deleteDir(profileFolder);
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void dispose() {
		disposeProfileImages();
		super.dispose();
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	private void editNewProfile(final SRTMProfile newProfile) {

		try {

			// open color chooser dialog

			final DialogSelectSRTMColors dialog = new DialogSelectSRTMColors(Display.getCurrent().getActiveShell(),
					null,
					newProfile,
					fProfileList,
					this,
					true);

			if (dialog.open() == Window.OK) {
				saveProfileNew(newProfile);
			}

			// image orientation has changed
			disposeProfileImages();

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void enableActions() {

		final StructuredSelection selection = (StructuredSelection) fProfileViewer.getSelection();

		fBtnRemoveProfile.setEnabled(selection.size() == 1 && fProfileList.size() > 1);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	private int getImageWidth() {

		int width;
		if (fTcProfileImage == null) {
			width = fDefaultImageWidth;
		} else {
			width = fTcProfileImage.getWidth();
		}

		return width;
	}

	/**
	 * Type of map is changed IFF one of colors, shadow state or grid is changed.
	 * 
	 * @return Hashcode for the unique key for the current profile settings
	 */
	private int getProfileKeyHashCode() {
		return fSelectedProfile.getProfileKey().hashCode();
	}

	private String getResolutionUI(final SRTMProfile profile) {

		final String resolution = profile.getResolution();

		if (IPreferences.SRTM_RESOLUTION_VERY_FINE.equals(resolution)) {
			return Messages.profileViewer_column_content_resolution_veryFine;
		} else if (IPreferences.SRTM_RESOLUTION_FINE.equals(resolution)) {
			return Messages.profileViewer_column_content_resolution_fine;
		} else if (IPreferences.SRTM_RESOLUTION_ROUGH.equals(resolution)) {
			return Messages.profileViewer_column_content_resolution_rough;
		} else if (IPreferences.SRTM_RESOLUTION_VERY_ROUGH.equals(resolution)) {
			return Messages.profileViewer_column_content_resolution_veryRough;
		}

		return UI.EMPTY_STRING;
	}

	public ColumnViewer getViewer() {
		return fProfileViewer;
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {

		saveViewerState();

		return super.okToLeave();
	}

	/**
	 * create profile
	 */
	private void onAddProfile() {

		final SRTMProfile newProfile = createNewProfile();

		editNewProfile(newProfile);
	}

	private void onDuplicateProfile() {

		final SRTMProfile newProfile = new SRTMProfile(fSelectedProfile);

		final int newProfileId = ++fMaxProfileId;

		newProfile.setProfileId(newProfileId);

		editNewProfile(newProfile);
	}

	private void onEditProfile() {

		final Object firstElement = ((StructuredSelection) fProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof SRTMProfile) {

			final SRTMProfile originalProfile = (SRTMProfile) firstElement;

			try {

				// open color chooser dialog

				final SRTMProfile dialogProfile = new SRTMProfile(originalProfile);

				final DialogSelectSRTMColors dialog = new DialogSelectSRTMColors(Display.getCurrent().getActiveShell(),
						originalProfile,
						dialogProfile,
						fProfileList,
						this,
						false);

				if (dialog.open() == Window.OK) {
					saveProfileModified(originalProfile, dialogProfile);
				}
				
				// image orientation has changed
				disposeProfileImages();

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void onRemoveProfile() {

		// confirm removal
		if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				Messages.prefPage_srtm_dlg_delete_profile_title,
				Messages.prefPage_srtm_dlg_delete_profile_msg) == false) {
			return;
		}

		int selectedIndex = fProfileViewer.getTable().getSelectionIndex();

		// update model
		fProfileList.remove(fSelectedProfile);

		// update viewer
		fProfileViewer.remove(fSelectedProfile);

		// remove images
		deleteOfflineImages(fSelectedProfile);
		fSelectedProfile.disposeImage();

		/*
		 * select a new profile from the same position
		 */
		final int profileSize = fProfileList.size();
		selectedIndex = selectedIndex >= profileSize ? profileSize - 1 : selectedIndex;
		fSelectedProfile = (SRTMProfile) fProfileViewer.getElementAt(selectedIndex);

		selectProfileInViewer(fSelectedProfile);

		saveAllProfiles();
	}

	private void onResizeImageColumn() {

		final int newImageWidth = getImageWidth();

		// check if the width has changed
		if (newImageWidth == fOldImageWidth) {
			return;
		}

		// recreate images
		disposeProfileImages();
	}

	private void onSelectProfile(final SRTMProfile selectedProfile, final boolean isForceSelection) {

		// ignore same profile
		if (isForceSelection == false && fSelectedProfile != null && fSelectedProfile == selectedProfile) {
			enableActions();
			return;
		}

		// uncheck previous profile
		if (fSelectedProfile != null) {
			fProfileViewer.setChecked(fSelectedProfile, false);
		}

		// check selected profile
		fSelectedProfile = selectedProfile;
		fProfileViewer.setChecked(fSelectedProfile, true);

		enableActions();

		if (fBooleanEditorApplyOption.getBooleanValue()) {

			// apply profile

			saveSelectedProfile();
		}
	}

	@Override
	public boolean performCancel() {

		saveViewerState();

		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				Messages.prefPage_srtm_confirm_defaults_title,
				Messages.prefPage_srtm_confirm_defaults_message)) {

			deleteAllOfflineImages();

			createXmlDefaultProfiles();

			initVertexLists();

			reloadViewer();

			fSelectedProfile = fProfileList.get(0);
			selectProfileInViewer(fSelectedProfile);

			fBooleanEditorApplyOption.loadDefault();

			saveState();
		}

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		fProfileContainer.setRedraw(false);
		{
			fProfileViewer.getTable().dispose();

			createUIProfileViewer(fProfileContainer);
			fProfileContainer.layout();

			// update the viewer
			reloadViewer();
		}
		fProfileContainer.setRedraw(true);

		return fProfileViewer;
	}

	public void reloadViewer() {
		fProfileViewer.setInput(new Object[0]);
	}

	private void restoreState() {

		fBooleanEditorApplyOption.load();
	}

	private void saveAllProfiles() {

		deleteOfflineImagesWhenModified();

		saveProfileXMLFile();

		saveState();
	}

	void saveProfile(	final SRTMProfile originalProfile,
						final SRTMProfile dialogProfile,
						final boolean isNewProfile) {

		if (isNewProfile) {
			saveProfileNew(dialogProfile);
		} else {
			saveProfileModified(originalProfile, dialogProfile);
		}
	}

	/**
	 * Save profile which was edited
	 * 
	 * @param originalProfile
	 * @param dialogProfile
	 */
	private void saveProfileModified(final SRTMProfile originalProfile, final SRTMProfile dialogProfile) {

		// delete tile images when tile path has changed
		if (originalProfile.getTilePath().equalsIgnoreCase(dialogProfile.getTilePath()) == false) {
			deleteOfflineImages(originalProfile);
		}

		if (originalProfile.equals(dialogProfile) == false) {
			/*
			 * when a new profile is applied in the color dialog, both profiles are the same,
			 * cloning is not required, it would remove the vertexes
			 */
			originalProfile.cloneProfile(dialogProfile);
		}

		originalProfile.disposeImage();

		// update viewer
		fProfileViewer.refresh();
		fProfileViewer.setSelection(new StructuredSelection(originalProfile), true);

		saveAllProfiles();

		originalProfile.createSavedProfileKey();
	}

	/**
	 * Save profile which was newly created
	 * 
	 * @param newProfile
	 */
	private void saveProfileNew(final SRTMProfile newProfile) {

		/*
		 * when a new profile is applied in the color dialog, it is not new anymore
		 */
		final int profileIndex = fProfileList.indexOf(newProfile);
		if (profileIndex != -1) {

			// save existing profile

			final SRTMProfile newSavedProfile = fProfileList.get(profileIndex);
			saveProfileModified(newSavedProfile, newProfile);

		} else {

			// save new profile

			newProfile.createSavedProfileKey();

			// update model
			fProfileList.add(newProfile);

			// rotate image
			newProfile.disposeImage();

			// update viewer
			fProfileViewer.add(newProfile);

			// select new profile
			fProfileViewer.setAllChecked(false);
			selectProfileInViewer(newProfile);

			saveAllProfiles();
		}
	}

	private void saveProfileXMLFile() {

		BufferedWriter writer = null;

		try {

			final File file = getProfileFile();
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlRoot = getXMLRoot();

			for (final SRTMProfile profile : fProfileList) {

				final IMemento xmlProfile = createXmlProfile(xmlRoot,
						profile.getProfileId(),
						profile.getProfileName(),
						profile.getTilePath(),
						profile.isShadowState(),
						profile.getShadowValue(),
						profile.getResolution());

				for (final RGBVertex vertex : profile.getVertexList()) {
					final RGB rgb = vertex.getRGB();
					createXmlVertex(xmlProfile, (int) vertex.getElevation(), rgb.red, rgb.green, rgb.blue);
				}
			}

			xmlRoot.save(writer);

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * save selected profile, this will fire an event to update existing maps
	 */
	private void saveSelectedProfile() {
		if (fSelectedProfile != null) {
			
			fPrefStore.setValue(IPreferences.SRTM_COLORS_SELECTED_PROFILE_ID, fSelectedProfile.getProfileId());
			
			// this will fire a profile change event
			fPrefStore.setValue(IPreferences.SRTM_COLORS_SELECTED_PROFILE_KEY, getProfileKeyHashCode());
		}
	}

	/**
	 * save state of the pref page
	 */
	private void saveState() {
		fBooleanEditorApplyOption.store();
		saveSelectedProfile();
		saveViewerState();
	}

	private void saveViewerState() {

		// viewer state
		fColumnManager.saveState(fState);
	}

	/**
	 * select profile and make it visible,
	 */
	private void selectProfileInViewer(final SRTMProfile profile) {

		fProfileViewer.setChecked(fSelectedProfile, true);

		fProfileViewer.getTable().setFocus();

		fProfileViewer.setSelection(new StructuredSelection(profile), true);
	}
}
