/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.srtm;

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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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

import de.byteholder.geoclipse.map.TileImageCache;
import de.byteholder.geoclipse.mapprovider.MP;

public final class PrefPageSRTMColors extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer {

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

	private final static IPreferenceStore	_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();

	private final IDialogSettings			_state					= TourbookPlugin.getDefault() //
																			.getDialogSettingsSection("SRTMColors");	//$NON-NLS-1$

	private static ArrayList<SRTMProfile>	_profileList			= new ArrayList<SRTMProfile>();
	private static SRTMProfile				_selectedProfile		= null;

	private Button							_btnEditProfile;
	private Button							_btnAddProfile			= null;
	private Button							_btnRemoveProfile		= null;
	private Button							_btnDuplicateProfile;

	private int								_defaultImageWidth		= 300;
	private int								_imageHeight			= 40;

	private int								_oldImageWidth			= -1;
	private Composite						_viewerContainer;

	private CheckboxTableViewer				_profileViewer;
	private ColumnManager					_columnManager;

	/**
	 * contains the table column widget for the profile color
	 */
	private TableColumn						_tcProfileImage;

	private TableColumnDefinition			_colDefImage;

	/**
	 * index of the profile image, this can be changed when the columns are reordered with the mouse
	 * or the column manager
	 */
	private int								_profileImageColumn		= 0;

	private BooleanFieldEditor				_booleanEditorApplyOption;
	private PixelConverter					_pc;

	private static int						_maxProfileId;

	private static boolean					_isCreateDefault;

	private class ProfileContentProvider implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return _profileList.toArray(new SRTMProfile[_profileList.size()]);
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
	 * 
	 * @return Returns the file which was created to save the default srtm profiles
	 */
	private static File createXmlDefaultProfiles() {

		BufferedWriter writer = null;
		final File file = getProfileFile();

		try {

			int profileId = -1;

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlRoot = getXMLRoot();

			IMemento profile = createXmlProfile(xmlRoot,//
					++profileId,
					"Default 1", //$NON-NLS-1$
					"profile-default-1", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, -9000, 2, 10, 31);
			createXmlVertex(profile, -1, 14, 76, 255);
			createXmlVertex(profile, 0, 0, 156, 0);
			createXmlVertex(profile, 200, 204, 204, 153);
			createXmlVertex(profile, 400, 204, 153, 0);
			createXmlVertex(profile, 600, 126, 91, 0);
			createXmlVertex(profile, 1000, 127, 127, 128);
			createXmlVertex(profile, 2000, 223, 223, 240);
			createXmlVertex(profile, 8000, 255, 255, 255);

			profile = createXmlProfile(xmlRoot, //
					++profileId,
					"Default 2", //$NON-NLS-1$
					"profile-default-2", //$NON-NLS-1$
					SRTMProfile.DEFAULT_IS_SHADOW,
					SRTMProfile.DEFAULT_SHADOW_VALUE,
					SRTMProfile.DEFAULT_SRTM_RESOLUTION);
			createXmlVertex(profile, -9000, 2, 10, 31);
			createXmlVertex(profile, -1, 14, 76, 255);
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
			createXmlVertex(profile, -9000, 2, 10, 31);
			createXmlVertex(profile, -1, 14, 76, 255);
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
			createXmlVertex(profile, -9000, 2, 10, 31);
			createXmlVertex(profile, -1, 14, 76, 255);
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
			createXmlVertex(profile, -9000, 2, 10, 31);
			createXmlVertex(profile, -1, 14, 76, 255);
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
			createXmlVertex(profile, -9000, 2, 10, 31);
			createXmlVertex(profile, -1, 14, 76, 255);
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

		return file;
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
		for (final SRTMProfile profile : _profileList) {
			if (profile != null) {
				profile.disposeImage();
			}
		}
	}

	private static File getProfileFile() {
		final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		return stateLocation.append(PROFILE_FILE_NAME).toFile();
	}

	public static SRTMProfile getSelectedProfile() {
		return _selectedProfile;
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
		_profileList.clear();

		/*
		 * get profile file
		 */

		final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		File profileFile = stateLocation.append(PROFILE_FILE_NAME).toFile();

		if (profileFile.exists() == false) {

			/*
			 * check if the file exists in the old location from version <= 10.3
			 */

			final IPath stateRoot = stateLocation.removeLastSegments(1);
			final IPath oldFilePath = stateRoot.append("net.tourbook.srtm"); //$NON-NLS-1$

			profileFile = oldFilePath.append(PROFILE_FILE_NAME).toFile();
		}

		// check if file is available
		if (profileFile.exists() == false) {

			// create default profile
			final File createdProfileFile = createXmlDefaultProfiles();

			// check again after the file was created
			if (createdProfileFile.exists() == false) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),//
						"Default Profile", //$NON-NLS-1$
						NLS.bind("SRTM color profile file {0} could not be created", profileFile.getAbsolutePath())); //$NON-NLS-1$
				return;
			}

			profileFile = createdProfileFile;
		}

		InputStreamReader reader = null;

		try {

			final ArrayList<RGBVertex> vertexList = new ArrayList<RGBVertex>();
			_maxProfileId = -1;

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

					vertexList.add(new RGBVertex(altitude, red, green, blue));
				}

				/*
				 * create profile
				 */
				SRTMProfile profile;
				_profileList.add(profile = new SRTMProfile());

				profile.setProfileId(profileId);
				profile.setProfileName(profileName);
				profile.setTilePath(profilePath);
				profile.setShadowState(profileShadowState);
				profile.setShadowValue(profileShadowValue);
				profile.setResolution(profileResolution);

				profile.setVertices(vertexList);

				// get max profile id
				_maxProfileId = Math.max(_maxProfileId, profileId);
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

			if (_profileList.size() == 0) {

				// !!! this message is confusing the user !!!
//				MessageDialog.openError(Display.getCurrent().getActiveShell(), //
//						"Read Profiles", //$NON-NLS-1$
//						"SRTM color profile is not available, default profile is created."); //$NON-NLS-1$

				// prevent endless loops
				if (_isCreateDefault) {
					_isCreateDefault = false;
				} else {

					// create default profile
					createXmlDefaultProfiles();

					_isCreateDefault = true;
					loadProfiles();
				}

			} else {

				// create profile key for the saved profile
				for (final SRTMProfile profile : _profileList) {
					profile.createSavedProfileKey();
				}
			}
		}
	}

	private static void restoreSelectedProfile() {

		/*
		 * get selected profile
		 */
		final int prefProfileId = _prefStore.getInt(IPreferences.SRTM_COLORS_SELECTED_PROFILE_ID);
		for (final SRTMProfile profile : _profileList) {
			if (profile.getProfileId() == prefProfileId) {
				_selectedProfile = profile;
				break;
			}
		}
		if (_selectedProfile == null) {
			// set default profile
			_selectedProfile = _profileList.get(0);
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		initVertexLists();

		_pc = new PixelConverter(parent);

		final Composite container = createUI(parent);

		reloadViewer();

		restoreState();

		// reselected profile
		selectProfileInViewer(_selectedProfile);

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

		final int newProfileId = ++_maxProfileId;

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
		{
			final Composite profileContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(profileContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 0).applyTo(profileContainer);
			{
				createUI_10_ProfileViewer(profileContainer);
				createUI_20_Actions(profileContainer);
			}
		}

		createUI30ApplyOption(parent);

		return container;
	}

	private void createUI_10_ProfileViewer(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_12_ProfileViewer(_viewerContainer);
		}
	}

	private void createUI_12_ProfileViewer(final Composite parent) {

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

				if (event.index == _profileImageColumn) {

					final TableItem item = (TableItem) event.item;
					final SRTMProfile profile = (SRTMProfile) item.getData();
					final Image image = profile.getRgbVertexImage().getValidatedImage(
							getImageWidth(),
							_imageHeight,
							true);

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
		_profileViewer = new CheckboxTableViewer(table);

		_columnManager.createColumns(_profileViewer);

		_tcProfileImage = _colDefImage.getTableColumn();
		_profileImageColumn = _colDefImage.getCreateIndex();

		_profileViewer.setContentProvider(new ProfileContentProvider());
		_profileViewer.setSorter(new ProfileSorter());

		_profileViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				final SRTMProfile checkedProfile = (SRTMProfile) event.getElement();

				// ignore the same profile
				if (_selectedProfile != null && checkedProfile == _selectedProfile) {

					// prevent unchecking selected profile
					event.getCheckable().setChecked(checkedProfile, true);

					return;
				}

				// select checked profile
				selectProfileInViewer(checkedProfile);
			}
		});

		_profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final Object firstElement = ((StructuredSelection) event.getSelection()).getFirstElement();
				if (firstElement instanceof SRTMProfile) {
					onSelectProfile((SRTMProfile) firstElement, false);
				}
			}
		});

		_profileViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				onEditProfile();
			}
		});

	}

	private void createUI_20_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			/*
			 * button: edit profile
			 */
			_btnEditProfile = new Button(container, SWT.NONE);
			_btnEditProfile.setText(Messages.prefPage_srtm_profile_edit);
			_btnEditProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onEditProfile();
				}
			});
			setButtonLayoutData(_btnEditProfile);

			/*
			 * button: add profile
			 */
			_btnAddProfile = new Button(container, SWT.NONE);
			_btnAddProfile.setText(Messages.prefPage_srtm_profile_add);
			_btnAddProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onAddProfile();
				}
			});
			setButtonLayoutData(_btnAddProfile);

			/*
			 * button: remove profile
			 */
			_btnRemoveProfile = new Button(container, SWT.NONE);
			_btnRemoveProfile.setText(Messages.prefPage_srtm_profile_remove);
			_btnRemoveProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onRemoveProfile();
				}
			});
			setButtonLayoutData(_btnRemoveProfile);

			/*
			 * button: duplicate profile
			 */
			_btnDuplicateProfile = new Button(container, SWT.NONE);
			_btnDuplicateProfile.setText(Messages.prefPage_srtm_profile_duplicate);
			_btnDuplicateProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onDuplicateProfile();
				}
			});
			setButtonLayoutData(_btnDuplicateProfile);

			/*
			 * button: adjust columns
			 */
			final Button btnAdjustColumns = new Button(container, SWT.NONE);
			btnAdjustColumns.setText(Messages.prefPage_srtm_btn_adjust_columns);
			btnAdjustColumns.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					_columnManager.openColumnDialog();
				}
			});
			setButtonLayoutData(btnAdjustColumns);
			final GridData gd = (GridData) btnAdjustColumns.getLayoutData();
			gd.verticalIndent = 20;
		}
	}

	private void createUI30ApplyOption(final Composite parent) {

		/*
		 * checkbox: pace min/max value
		 */
		_booleanEditorApplyOption = new BooleanFieldEditor(
				IPreferences.SRTM_APPLY_WHEN_PROFILE_IS_SELECTED,
				Messages.prefPage_srtm_profile_option_apply_when_selected,
				parent);
		_booleanEditorApplyOption.setPreferenceStore(_prefStore);
		_booleanEditorApplyOption.setPage(this);
		_booleanEditorApplyOption.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				if (((Boolean) event.getNewValue())) {
					// apply profile
					final Object firstElement = ((StructuredSelection) _profileViewer.getSelection()).getFirstElement();
					if (firstElement instanceof SRTMProfile) {
						onSelectProfile((SRTMProfile) firstElement, true);
					}
				}
			}
		});
	}

	private void defineAllColumns() {

		defineColumn_ProfileName();
		defineColumn_ShadowState();
		defineColumn_ShadowValue();
		defineColumn_Resolution();
		defineColumn_Color();
		defineColumn_TileImagePath();
		defineColumn_ProfileId();
	}

	/**
	 * column: color
	 */
	private void defineColumn_Color() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "color", SWT.LEAD); //$NON-NLS-1$
		_colDefImage = colDef;

		colDef.setColumnLabel(Messages.profileViewer_column_label_color);
		colDef.setColumnHeader(Messages.profileViewer_column_label_color_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_color_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(50));
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
	}

	/**
	 * column: id
	 */
	private void defineColumn_ProfileId() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "profileId", SWT.TRAIL); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_id);
		colDef.setColumnHeader(Messages.profileViewer_column_label_id_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_id_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(Integer.toString(vertexList.getProfileId()));
			}
		});
	}

	/**
	 * column: profile name
	 */
	private void defineColumn_ProfileName() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "profileName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_name);
		colDef.setColumnHeader(Messages.profileViewer_column_label_name_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_name_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(vertexList.getProfileName());
			}
		});
	}

	/**
	 * column: resolution
	 */
	private void defineColumn_Resolution() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "resolution", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_resolution);
		colDef.setColumnHeader(Messages.profileViewer_column_label_resolution_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_resolution_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile profile = (SRTMProfile) cell.getElement();
				cell.setText(getResolutionUI(profile));
			}
		});
	}

	/**
	 * column: shadow state
	 */
	private void defineColumn_ShadowState() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "shadowState", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_isShadow);
		colDef.setColumnHeader(Messages.profileViewer_column_label_isShadow_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_isShadow_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(5));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile profile = (SRTMProfile) cell.getElement();
				cell.setText(profile.isShadowState() ? Messages.app_ui_Y : Messages.app_ui_N);
			}
		});
	}

	/**
	 * column: shadow value
	 */
	private void defineColumn_ShadowValue() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "shadowValue", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_shadowValue);
		colDef.setColumnHeader(Messages.profileViewer_column_label_shadowValue_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_shadowValue_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile profile = (SRTMProfile) cell.getElement();
				cell.setText(Float.toString(profile.getShadowValue()));
			}
		});
	}

	/**
	 * column: image path
	 */
	private void defineColumn_TileImagePath() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "tileImagePath", SWT.LEAD); //$NON-NLS-1$
		colDef.setColumnLabel(Messages.profileViewer_column_label_imagePath);
		colDef.setColumnHeader(Messages.profileViewer_column_label_imagePath_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_imagePath_tooltip);
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(vertexList.getTilePath());
			}
		});
	}

	private void deleteAllOfflineImages() {

		for (final SRTMProfile profile : _profileList) {
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

			for (final String element : children) {
				final boolean success = deleteDir(new File(directory, element));
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

			final MP mp = ElevationColor.getMapProvider();
			if (mp == null) {
				// TODO initialize map provider
				return;
			}

			final IPath tileCacheOSPathFolder = getOfflineFolder(mp);
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

			final MP mp = ElevationColor.getMapProvider();
			if (mp == null) {
				// TODO initialize map provider
				return;
			}

			final IPath tileCacheOSPathFolder = getOfflineFolder(mp);
			if (tileCacheOSPathFolder == null) {
				return;
			}

			// loop: all profiles
			for (final SRTMProfile profile : _profileList) {
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
		return TourbookPlugin.getDefault().getPreferenceStore();
	}

	private void editNewProfile(final SRTMProfile newProfile) {

		try {

			// open color chooser dialog

			final DialogSelectSRTMColors dialog = new DialogSelectSRTMColors(
					Display.getCurrent().getActiveShell(),
					null,
					newProfile,
					_profileList,
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

		final StructuredSelection selection = (StructuredSelection) _profileViewer.getSelection();

		_btnRemoveProfile.setEnabled(selection.size() == 1 && _profileList.size() > 1);
	}

	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	private int getImageWidth() {

		int width;
		if (_tcProfileImage == null) {
			width = _defaultImageWidth;
		} else {
			width = _tcProfileImage.getWidth();
		}

		return width;
	}

	private IPath getOfflineFolder(final MP mp) {

		IPath tileCacheOSPathFolder = null;

		final String tileCacheOSPath = TileImageCache.getTileCacheOSPath();
		if (tileCacheOSPath != null) {
			tileCacheOSPathFolder = new Path(tileCacheOSPath).append(mp.getOfflineFolder());
		}

		return tileCacheOSPathFolder;
	}

	/**
	 * Type of map is changed IFF one of colors, shadow state or grid is changed.
	 * 
	 * @return Hashcode for the unique key for the current profile settings
	 */
	private int getProfileKeyHashCode() {
		return _selectedProfile.getProfileKey().hashCode();
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
		return _profileViewer;
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

		final SRTMProfile newProfile = _selectedProfile.clone();

		final int newProfileId = ++_maxProfileId;

		newProfile.setProfileId(newProfileId);

		editNewProfile(newProfile);
	}

	private void onEditProfile() {

		final Object firstElement = ((StructuredSelection) _profileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof SRTMProfile) {

			final SRTMProfile originalProfile = (SRTMProfile) firstElement;

			try {

				// open color chooser dialog

				final SRTMProfile dialogProfile = originalProfile.clone();

				final DialogSelectSRTMColors dialog = new DialogSelectSRTMColors(
						Display.getCurrent().getActiveShell(),
						originalProfile,
						dialogProfile,
						_profileList,
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
		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.prefPage_srtm_dlg_delete_profile_title,
				Messages.prefPage_srtm_dlg_delete_profile_msg) == false) {
			return;
		}

		int selectedIndex = _profileViewer.getTable().getSelectionIndex();

		// update model
		_profileList.remove(_selectedProfile);

		// update viewer
		_profileViewer.remove(_selectedProfile);

		// remove images
		deleteOfflineImages(_selectedProfile);
		_selectedProfile.disposeImage();

		/*
		 * select a new profile from the same position
		 */
		final int profileSize = _profileList.size();
		selectedIndex = selectedIndex >= profileSize ? profileSize - 1 : selectedIndex;
		_selectedProfile = (SRTMProfile) _profileViewer.getElementAt(selectedIndex);

		selectProfileInViewer(_selectedProfile);

		saveAllProfiles();
	}

	private void onResizeImageColumn() {

		final int newImageWidth = getImageWidth();

		// check if the width has changed
		if (newImageWidth == _oldImageWidth) {
			return;
		}

		// recreate images
		disposeProfileImages();
	}

	private void onSelectProfile(final SRTMProfile selectedProfile, final boolean isForceSelection) {

		// ignore same profile
		if (isForceSelection == false && _selectedProfile != null && _selectedProfile == selectedProfile) {
			enableActions();
			return;
		}

		// uncheck previous profile
		if (_selectedProfile != null) {
			_profileViewer.setChecked(_selectedProfile, false);
		}

		// check selected profile
		_selectedProfile = selectedProfile;
		_profileViewer.setChecked(_selectedProfile, true);

		enableActions();

		if (_booleanEditorApplyOption.getBooleanValue()) {

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

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.prefPage_srtm_confirm_defaults_title,
				Messages.prefPage_srtm_confirm_defaults_message)) {

			deleteAllOfflineImages();

			createXmlDefaultProfiles();

			initVertexLists();

			reloadViewer();

			_selectedProfile = _profileList.get(0);
			selectProfileInViewer(_selectedProfile);

			_booleanEditorApplyOption.loadDefault();

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

		_viewerContainer.setRedraw(false);
		{
			_profileViewer.getTable().dispose();

			createUI_12_ProfileViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _profileViewer;
	}

	public void reloadViewer() {
		_profileViewer.setInput(new Object[0]);
	}

	private void restoreState() {

		_booleanEditorApplyOption.load();
	}

	private void saveAllProfiles() {

		deleteOfflineImagesWhenModified();

		saveProfileXMLFile();

		saveState();
	}

	void saveProfile(final SRTMProfile originalProfile, final SRTMProfile dialogProfile, final boolean isNewProfile) {

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
			originalProfile.copyFromOtherProfile(dialogProfile);
		}

		originalProfile.disposeImage();

		// update viewer
		_profileViewer.refresh();
		_profileViewer.setSelection(new StructuredSelection(originalProfile), true);

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
		final int profileIndex = _profileList.indexOf(newProfile);
		if (profileIndex != -1) {

			// save existing profile

			final SRTMProfile newSavedProfile = _profileList.get(profileIndex);
			saveProfileModified(newSavedProfile, newProfile);

		} else {

			// save new profile

			newProfile.createSavedProfileKey();

			// update model
			_profileList.add(newProfile);

			// rotate image
			newProfile.disposeImage();

			// update viewer
			_profileViewer.add(newProfile);

			// select new profile
			_profileViewer.setAllChecked(false);
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

			for (final SRTMProfile profile : _profileList) {

				final IMemento xmlProfile = createXmlProfile(
						xmlRoot,
						profile.getProfileId(),
						profile.getProfileName(),
						profile.getTilePath(),
						profile.isShadowState(),
						profile.getShadowValue(),
						profile.getResolution());

				for (final RGBVertex vertex : profile.getRgbVertexImage().getRgbVertices()) {

					final RGB rgb = vertex.getRGB();

					createXmlVertex(xmlProfile, vertex.getValue(), rgb.red, rgb.green, rgb.blue);
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
		if (_selectedProfile != null) {

			_prefStore.setValue(IPreferences.SRTM_COLORS_SELECTED_PROFILE_ID, _selectedProfile.getProfileId());

			// this will fire a profile change event
			_prefStore.setValue(IPreferences.SRTM_COLORS_SELECTED_PROFILE_KEY, getProfileKeyHashCode());
		}
	}

	/**
	 * save state of the pref page
	 */
	private void saveState() {
		_booleanEditorApplyOption.store();
		saveSelectedProfile();
		saveViewerState();
	}

	private void saveViewerState() {

		// viewer state
		_columnManager.saveState(_state);
	}

	/**
	 * select profile and make it visible,
	 */
	private void selectProfileInViewer(final SRTMProfile profile) {

		_profileViewer.setChecked(_selectedProfile, true);

		_profileViewer.getTable().setFocus();

		_profileViewer.setSelection(new StructuredSelection(profile), true);
	}
}
