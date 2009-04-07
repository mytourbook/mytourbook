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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
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

public final class PrefPageSRTMColors extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer {

	private final static IPreferenceStore	fPrefStore				= Activator.getDefault().getPreferenceStore();

	private static final String				PROFILE_FILE_NAME		= "srtmprofiles.xml";								//$NON-NLS-1$

	private static final String				PROFILE_XML_ROOT		= "srtmprofiles";									//$NON-NLS-1$
	private static final String				MEMENTO_CHILD_PROFILE	= "profile";										//$NON-NLS-1$

	private static final String				TAG_PROFILE_ID			= "profileId";										//$NON-NLS-1$
	private static final String				TAG_NAME				= "name";											//$NON-NLS-1$
	private static final String				TAG_IMAGE_PATH			= "imagePath";										//$NON-NLS-1$
	private static final String				MEMENTO_CHILD_VERTEX	= "vertex";										//$NON-NLS-1$

	private static final String				TAG_ALTITUDE			= "altitude";										//$NON-NLS-1$
	private static final String				TAG_RED					= "red";											//$NON-NLS-1$
	private static final String				TAG_GREEN				= "green";											//$NON-NLS-1$
	private static final String				TAG_BLUE				= "blue";											//$NON-NLS-1$
	private final IDialogSettings			fState					= Activator.getDefault()
																			.getDialogSettingsSection("SRTMColors");	//$NON-NLS-1$

	private static ArrayList<SRTMProfile>	fProfileList			= new ArrayList<SRTMProfile>();
	private static SRTMProfile				fSelectedProfile		= null;

	private Button							fBtnAddProfile			= null;

	private Button							fBtnRemoveProfile		= null;
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

	private static int						fMaxProfileId;

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

			IMemento profile = createXmlProfile(xmlRoot, ++profileId, "Default 1", "default-1"); //$NON-NLS-1$ //$NON-NLS-2$
			createXmlVertex(profile, 0, 14, 76, 255);
			createXmlVertex(profile, 100, 198, 235, 197);
			createXmlVertex(profile, 200, 0, 102, 0);
			createXmlVertex(profile, 350, 255, 204, 153);
			createXmlVertex(profile, 500, 204, 153, 0);
			createXmlVertex(profile, 750, 153, 51, 0);
			createXmlVertex(profile, 1000, 102, 51, 0);
			createXmlVertex(profile, 1500, 92, 67, 64);
			createXmlVertex(profile, 2000, 204, 255, 255);

			profile = createXmlProfile(xmlRoot, ++profileId, "Default 2", "default-2"); //$NON-NLS-1$ //$NON-NLS-2$
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

			profile = createXmlProfile(xmlRoot, ++profileId, "Default 3", "default-3"); //$NON-NLS-1$ //$NON-NLS-2$
			createXmlVertex(profile, 0, 14, 76, 255);
			createXmlVertex(profile, 500, 166, 219, 156);
			createXmlVertex(profile, 1000, 51, 153, 0);
			createXmlVertex(profile, 2000, 102, 51, 0);
			createXmlVertex(profile, 3000, 51, 51, 51);
			createXmlVertex(profile, 4000, 204, 255, 255);
			createXmlVertex(profile, 8850, 255, 255, 255);

			profile = createXmlProfile(xmlRoot, ++profileId, "Default 4", "default-4"); //$NON-NLS-1$ //$NON-NLS-2$
			createXmlVertex(profile, 0, 255, 255, 255);
			createXmlVertex(profile, 1000, 178, 81, 0);
			createXmlVertex(profile, 2000, 100, 0, 59);
			createXmlVertex(profile, 3000, 0, 102, 127);

			profile = createXmlProfile(xmlRoot, ++profileId, "Default 5", "default-5"); //$NON-NLS-1$ //$NON-NLS-2$
			createXmlVertex(profile, 0, 0, 0, 255);
			createXmlVertex(profile, 1000, 127, 0, 215);
			createXmlVertex(profile, 2000, 255, 0, 0);
			createXmlVertex(profile, 3000, 195, 103, 0);
			createXmlVertex(profile, 4000, 190, 193, 0);
			createXmlVertex(profile, 5000, 122, 190, 0);
			createXmlVertex(profile, 6000, 20, 141, 0);
			createXmlVertex(profile, 7000, 105, 231, 202);
			createXmlVertex(profile, 8000, 255, 255, 255);

			profile = createXmlProfile(xmlRoot, ++profileId, "Default 6", "default-6"); //$NON-NLS-1$ //$NON-NLS-2$
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
												final String profileImagePath) {

		final IMemento xmlProfile = xmlMemento.createChild(MEMENTO_CHILD_PROFILE);
		xmlProfile.putInteger(TAG_PROFILE_ID, profileId);
		xmlProfile.putString(TAG_NAME, name);//$NON-NLS-1$
		xmlProfile.putString(TAG_IMAGE_PATH, profileImagePath);//$NON-NLS-1$

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

	private static void disposeImages() {
		for (final SRTMProfile profile : fProfileList) {
			if (profile != null) {
				profile.disposeImage();
			}
		}
	}

	public static int getGrid() {
		/*
		 * elevation is used at every grid-th pixel in both directions; the other values are
		 * interpolated i.e. it gives the resolution of the image!
		 */
		final String srtmResolution = fPrefStore.getString(IPreferences.SRTM_RESOLUTION);
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_ROUGH)) {
			return 64;
		} else if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_ROUGH)) {
			return 16;
		} else if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_FINE)) {
			return 4;
		} else if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_FINE)) {
			return 1;
		} else {
			return 4;
		}
	}

	private static File getProfileFile() {
		final IPath stateLocation = Platform.getStateLocation(Activator.getDefault().getBundle());
		final File file = stateLocation.append(PROFILE_FILE_NAME).toFile();
		return file;
	}

	public static RGB getRGB(final int elev) {
		return fSelectedProfile.getRGB(elev);
	}

	public static String getRGBVertexListString() {
		return fSelectedProfile.toString();
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

	public static void initVertexLists() {
		loadProfiles();
		restoreSelectedProfile();
	}

	public static boolean isShadowState() {
		return fPrefStore.getBoolean(IPreferences.SRTM_SHADOW);
	}

	private static void loadProfiles() {

		// cleanup existing profiles
		disposeImages();
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
				profile.setProfilePath(profilePath);
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
						"Profiles could not be created"); //$NON-NLS-1$
			}
		}
	}

	private static void restoreSelectedProfile() {

		/*
		 * get selected profile
		 */
		final int prefProfileId = fPrefStore.getInt(IPreferences.SRTM_COLORS_ACTUAL_PROFILE);
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

		// reselected profile
		selectProfile(fSelectedProfile);

		enableActions();

		return container;
	}

	private void createProfileViewer(final Composite parent) {

		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
//		table.setLinesVisible(true);
//		table.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			public void handleEvent(final Event event) {

				if (event.index == fProfileImageColumn) {

					final TableItem item = (TableItem) event.item;
					final SRTMProfile vertexList = (SRTMProfile) item.getData();
					final Image image = vertexList.getImage();

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
					if (event.getChecked() == false) {
						// reverse uncheck state
						event.getCheckable().setChecked(checkedProfile, true);
					}
					return;
				}

				// uncheck previous profile
				if (fSelectedProfile != null && fSelectedProfile != checkedProfile) {
					fProfileViewer.setChecked(fSelectedProfile, false);
				}

				fSelectedProfile = checkedProfile;
			}
		});

		fProfileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final Object firstElement = ((StructuredSelection) event.getSelection()).getFirstElement();
				if (firstElement instanceof SRTMProfile) {

					final SRTMProfile selectedProfile = (SRTMProfile) firstElement;

					// ignore same profile
					if (fSelectedProfile != null && fSelectedProfile == selectedProfile) {
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
				}
			}
		});

		fProfileViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final Object firstElement = ((StructuredSelection) event.getSelection()).getFirstElement();
				if (firstElement instanceof SRTMProfile) {

					final SRTMProfile selectedProfile = (SRTMProfile) firstElement;
					try {
						// open color dialog

						final SRTMProfile rgbVertexListEdit = new SRTMProfile();
						rgbVertexListEdit.replaceVertexes(selectedProfile);

						final DialogSelectSRTMColors dialog = new DialogSelectSRTMColors(Display.getCurrent()
								.getActiveShell(), rgbVertexListEdit);

						if (dialog.open() == Window.OK) {

							final SRTMProfile modifiedProfile = dialog.getRgbVertexList();

							selectedProfile.replaceVertexes(modifiedProfile);
							selectedProfile.createImage(Display.getCurrent(), getImageWidth(), fImageHeight);

							// update viewer
							fProfileViewer.update(selectedProfile, null);

							fProfileViewer.getTable().redraw();
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);

		createUIResolutionOption(container);
		createUIShadowOption(container);
		createUIProfileList(container);
		createUIButtons(container);

		return container;
	}

	private void createUIButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
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
		}
	}

	private void createUIProfileList(final Composite parent) {

		// define all columns for the viewer
		fColumnManager = new ColumnManager(this, fState);
		defineViewerColumns(parent);

		fProfileContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fProfileContainer);
		GridLayoutFactory.fillDefaults().applyTo(fProfileContainer);

		createProfileViewer(fProfileContainer);
		createVertexImages();
	}

	private void createUIResolutionOption(final Composite parent) {

		final RadioGroupFieldEditor radioGroupFieldEditor = new RadioGroupFieldEditor(IPreferences.SRTM_RESOLUTION,
				Messages.prefPage_srtm_resolution_title,
				4,
				new String[][] {
						new String[] {
								Messages.prefPage_srtm_resolution_very_fine,
								IPreferences.SRTM_RESOLUTION_VERY_FINE },
						new String[] { Messages.prefPage_srtm_resolution_fine, IPreferences.SRTM_RESOLUTION_FINE },
						new String[] { Messages.prefPage_srtm_resolution_rough, IPreferences.SRTM_RESOLUTION_ROUGH },
						new String[] {
								Messages.prefPage_srtm_resolution_very_rough,
								IPreferences.SRTM_RESOLUTION_VERY_ROUGH }, },
				parent, // resolutionGroup,
				true);

		radioGroupFieldEditor.setPreferenceStore(fPrefStore);
		radioGroupFieldEditor.load();
		radioGroupFieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				fPrefStore.setValue(IPreferences.SRTM_RESOLUTION, "" + e.getNewValue()); //$NON-NLS-1$
			}
		});
	}

	private void createUIShadowOption(final Composite parent) {

		final BooleanFieldEditor booleanFieldEditor = new BooleanFieldEditor(IPreferences.SRTM_SHADOW,
				Messages.prefPage_srtm_shadow_text,
				parent);

		booleanFieldEditor.setPreferenceStore(fPrefStore);
		booleanFieldEditor.load();
		booleanFieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				fPrefStore.setValue(IPreferences.SRTM_SHADOW, "" + e.getNewValue()); //$NON-NLS-1$
			}
		});

	}

	private void createVertexImages() {

		final int imageWidth = getImageWidth();
		final Display display = Display.getCurrent();

		for (final SRTMProfile profile : fProfileList) {
			if (profile != null) {
				profile.createImage(display, imageWidth, fImageHeight);
			}
		}
	}

	private void defineViewerColumns(final Composite parent) {

		TableColumnDefinition colDef;
		final PixelConverter pixelConverter = new PixelConverter(parent);

		/*
		 * column: checkbox
		 */
		colDef = new TableColumnDefinition(fColumnManager, "checkboxColumn", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_checkbox);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			/*
			 * !!! set dummy label provider, otherwise an error occures !!!
			 */
			@Override
			public void update(final ViewerCell cell) {}
		});

		/*
		 * column: profile name
		 */
		colDef = new TableColumnDefinition(fColumnManager, "profileName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_name);
		colDef.setColumnHeader(Messages.profileViewer_column_label_name_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_name_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(true);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(vertexList.getProfileName());
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
		colDef = new TableColumnDefinition(fColumnManager, "imagePath", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.profileViewer_column_label_imagePath);
		colDef.setColumnHeader(Messages.profileViewer_column_label_imagePath_header);
		colDef.setColumnToolTipText(Messages.profileViewer_column_label_imagePath_tooltip);
		colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final SRTMProfile vertexList = (SRTMProfile) cell.getElement();
				cell.setText(vertexList.getProfilePath());
			}
		});
	}

	@Override
	public void dispose() {
		disposeImages();
		super.dispose();
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
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

		SRTMProfile profile;
		fProfileList.add(profile = new SRTMProfile());

		profile.init();
		profile.setProfileId(++fMaxProfileId);
		profile.setProfileName(Messages.prefPage_srtm_default_profile_name);
		profile.setProfilePath(Messages.prefPage_srtm_default_profile_path);

		profile.createImage(Display.getCurrent(), getImageWidth(), fImageHeight);

		fSelectedProfile = profile;

		fProfileViewer.add(profile);

		// select new profile
		fProfileViewer.setAllChecked(false);
		selectProfile(profile);
	}

	private void onRemoveProfile() {

		// confirm removal
		if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				Messages.prefPage_srtm_dlg_delete_profile_title,
				Messages.prefPage_srtm_dlg_delete_profile_msg) == false) {
			return;
		}

		final Table table = fProfileViewer.getTable();

		int selectedIndex = table.getSelectionIndex();

		fProfileList.remove(fSelectedProfile);

		// update viewer
		fProfileViewer.remove(fSelectedProfile);

		/*
		 * select profile at the same position
		 */
		final int profileSize = fProfileList.size();
		selectedIndex = selectedIndex >= profileSize ? profileSize - 1 : selectedIndex;
		fSelectedProfile = (SRTMProfile) fProfileViewer.getElementAt(selectedIndex);

		selectProfile(fSelectedProfile);
	}

	private void onResizeImageColumn() {

		final int newImageWidth = getImageWidth();

		// check if the width has changed
		if (newImageWidth == fOldImageWidth) {
			return;
		}

		disposeImages();
		createVertexImages();
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

			createXmlDefaultProfiles();

			initVertexLists();

			createVertexImages();

			reloadViewer();

			fSelectedProfile = fProfileList.get(0);
			selectProfile(fSelectedProfile);
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

			createProfileViewer(fProfileContainer);
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

	private void saveProfileXML() {

		BufferedWriter writer = null;

		try {

			final File file = getProfileFile();
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlRoot = getXMLRoot();

			for (final SRTMProfile profile : fProfileList) {

				final IMemento xmlProfile = createXmlProfile(xmlRoot,
						profile.getProfileId(),
						profile.getProfileName(),
						profile.getProfilePath());

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

	private void saveState() {

		saveProfileXML();

		// save selected profile
		if (fSelectedProfile != null) {
			fPrefStore.setValue(IPreferences.SRTM_COLORS_ACTUAL_PROFILE, fSelectedProfile.getProfileId());
		}

		saveViewerState();
	}

	private void saveViewerState() {

		// viewer state
		fColumnManager.saveState(fState);
	}

	/**
	 * select profile and make it visible, encapsulate into a list otherwise the vertexes will be
	 * used and will fail
	 */
	private void selectProfile(final SRTMProfile profile) {

		fProfileViewer.setChecked(fSelectedProfile, true);

		fProfileViewer.getTable().setFocus();

		final ArrayList<SRTMProfile> selection = new ArrayList<SRTMProfile>();
		selection.add(profile);
		fProfileViewer.setSelection(new StructuredSelection(selection), true);
	}
}
