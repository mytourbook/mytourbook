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
package net.tourbook.map.bookmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.oscim.core.MapPosition;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class MapBookmarkManager {

// SET_FORMATTING_OFF

	private static final String			CONFIG_FILE_NAME				= "map-bookmarks.xml";			//$NON-NLS-1$
	
	private static final Bundle			_bundle							= TourbookPlugin.getDefault().getBundle();
	private static final IPath			_stateLocation					= Platform.getStateLocation(_bundle);
	//
	/**
	 * Version number is not yet used.
	 */
	private static final int			CONFIG_VERSION					= 1;
	//
	// !!! this is a code formatting separator !!!
	static {}
	//
	// common attributes
	private static final String				ATTR_ID							= "id";							//$NON-NLS-1$

	//
	/*
	 * Root
	 */
	private static final String				TAG_ROOT						= "MapBookmarks";				//$NON-NLS-1$
	private static final String				ATTR_CONFIG_VERSION				= "configVersion";				//$NON-NLS-1$
	//
	/*
	 * Bookmarks
	 */
	private static final String				TAG_ALL_BOOKMARKS				= "AllBookmarks";				//$NON-NLS-1$
	private static final String				TAG_BOOKMARK					= "Bookmark";					//$NON-NLS-1$
	//
	private static final String				TAG_ALL_RECENT_BOOKMARKS		= "AllRecentBookmarks";			//$NON-NLS-1$
	private static final String				TAG_RECENT_BOOKMARK				= "RecentBookmark";				//$NON-NLS-1$
	//
	private static final String				ATTR_NAME						= "name";						//$NON-NLS-1$
	private static final String				ATTR_MAP_POSITION_X				= "mapPositionX";				//$NON-NLS-1$
	private static final String				ATTR_MAP_POSITION_Y				= "mapPositionY";				//$NON-NLS-1$
	private static final String				ATTR_MAP_POSITION_SCALE			= "mapPositionScale";			//$NON-NLS-1$
	private static final String				ATTR_MAP_POSITION_BEARING		= "mapPositionBearing";			//$NON-NLS-1$
	private static final String				ATTR_MAP_POSITION_TILT			= "mapPositionTilt";			//$NON-NLS-1$
	private static final String				ATTR_MAP_POSITION_ZOOM_LEVEL	= "mapPositionZoomLevel";		//$NON-NLS-1$
	//
	private static final String				TAG_OPTIONS						= "Options";					//$NON-NLS-1$
	private static final String				ATTR_NUMBER_OF_BOOKMARK_ITEMS	= "numberOfBookmarkItems";		//$NON-NLS-1$
	private static final String				ATTR_NUMBER_OF_RECENT_BOOKMARKS	= "numberOfRecentBookmarks";	//$NON-NLS-1$
	//
	private static final int				NUM_RECENT_BOOKMARKS_DEFAULT	= 5;
	static final int						NUM_RECENT_BOOKMARKS_MIN		= 0;
	static final int						NUM_RECENT_BOOKMARKS_MAX		= 20;
	//
	private static final int				NUM_BOOKMARK_ITEMS_DEFAULT		= 20;
	static final int						NUM_BOOKMARK_ITEMS_MIN			= 3;
	static final int						NUM_BOOKMARK_ITEMS_MAX			= 100;
	//
// SET_FORMATTING_ON
	//
	/**
	 * Contains all configurations which are loaded from a xml file.
	 */
	private static ArrayList<MapBookmark>	_allBookmarks					= new ArrayList<>();
	//
	/**
	 * Keep last recent used bookmarks
	 */
	private static LinkedList<MapBookmark>	_allRecentBookmarks				= new LinkedList<>();

	public static int						numberOfBookmarkItems			= NUM_BOOKMARK_ITEMS_DEFAULT;
	public static int						numberOfRecentBookmarks			= NUM_RECENT_BOOKMARKS_DEFAULT;

	/**
	 * Instance of the map bookmark view or <code>null</code> when not opened
	 */
	private static MapBookmarkView			_mapBookmarkView;

	private static final ListenerList		_bookmarkListeners				= new ListenerList(ListenerList.IDENTITY);

	static {

		// load bookmarks
		readBookmarksFromXml();
	}

	private static class ActionBookmark_Add extends Action {

		private IMapBookmarks __mapBookmarks;

		public ActionBookmark_Add(final IMapBookmarks mapBookmarks) {

			super(Messages.Action_Map_AddBookmark, AS_PUSH_BUTTON);

			__mapBookmarks = mapBookmarks;
		}

		@Override
		public void run() {
			actionBookmark_Add(__mapBookmarks);
		}

	}

	private static class ActionBookmark_Update extends Action {

		private IMapBookmarks __mapBookmarks;

		public ActionBookmark_Update(final String text, final IMapBookmarks mapBookmarks) {

			super(text, AS_PUSH_BUTTON);

			__mapBookmarks = mapBookmarks;
		}

		@Override
		public void run() {
			actionBookmark_Update(__mapBookmarks);
		}

	}

	private static class ActionRecentBookmark extends Action {

		private String			__bookmarkId;
		private IMapBookmarks	__mapBookmarks;

		public ActionRecentBookmark(final String name,
									final String bookmarkId,
									final IMapBookmarks mapBookmarks) {

			super(name, AS_PUSH_BUTTON);

			__bookmarkId = bookmarkId;
			__mapBookmarks = mapBookmarks;
		}

		@Override
		public void run() {

			for (final MapBookmark bookmark : getAllBookmarks()) {

				if (__bookmarkId.equals(bookmark.id)) {

					__mapBookmarks.moveToMapLocation(bookmark);

					return;
				}
			}
		}
	}

	private static class DialogAddBookmark extends InputDialog {

		public DialogAddBookmark(	final Shell parentShell,
									final String dialogTitle,
									final String dialogMessage,
									final String initialValue,
									final IInputValidator validator) {

			super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
		}

		@Override
		protected void createButtonsForButtonBar(final Composite parent) {

			super.createButtonsForButtonBar(parent);

			// set text for the OK button
			final Button _btnSave = getButton(IDialogConstants.OK_ID);
			_btnSave.setText(Messages.Map_Bookmark_Button_Add);
		}

		@Override
		protected Point getInitialLocation(final Point initialSize) {

			try {

				final Point cursorLocation = Display.getCurrent().getCursorLocation();

				// center below cursor location
				cursorLocation.x -= initialSize.x / 2;
				cursorLocation.y += 10;

				return cursorLocation;

			} catch (final NumberFormatException ex) {

				return super.getInitialLocation(initialSize);
			}
		}

	}

	private static void actionBookmark_Add(final IMapBookmarks mapBookmarks) {

		final DialogAddBookmark addDialog = new DialogAddBookmark(

				Display.getDefault().getActiveShell(),

				Messages.Map_Bookmark_Dialog_AddBookmark_Title,
				Messages.Map_Bookmark_Dialog_AddBookmark_Message,

				UI.EMPTY_STRING,
				new IInputValidator() {

					@Override
					public String isValid(final String newText) {

						if (newText.trim().length() == 0) {

							//							return "Set a name for the bookmark";//Messages.Dialog_MapBookmark_Dialog_InvalidName;
							return Messages.Map_Bookmark_Dialog_ValidationAddName;
						}

						return null;
					}
				});

		addDialog.open();

		if (addDialog.getReturnCode() != Window.OK) {
			return;
		}

		final MapLocation mapLocation = mapBookmarks.getMapLocation();

		final MapBookmark newBookmark = new MapBookmark();

		newBookmark.name = addDialog.getValue();
		newBookmark.setMapPosition(mapLocation.getMapPosition());

		_allBookmarks.add(newBookmark);

		setLastSelectedBookmark(newBookmark);
		
		onUpdateBookmark(newBookmark);
	}

	private static void actionBookmark_Update(final IMapBookmarks mapBookmarks) {

		if (_allRecentBookmarks.size() > 0) {

			final MapBookmark lastSelectedBookmark = _allRecentBookmarks.getFirst();

			// update last selected bookmark
			if (lastSelectedBookmark != null) {

				// update map position
				final MapLocation mapLocation = mapBookmarks.getMapLocation();

				lastSelectedBookmark.setMapPosition(mapLocation.getMapPosition());

				onUpdateBookmark(lastSelectedBookmark);
			}
		}
	}

	public static void addBookmarkListener(final IMapBookmarkListener listener) {
		_bookmarkListeners.add(listener);
	}

	private static XMLMemento create_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// config version
		xmlRoot.putInteger(ATTR_CONFIG_VERSION, CONFIG_VERSION);

		return xmlRoot;
	}

	public static void fillContextMenu_RecentBookmarks(final IMenuManager menuMgr, final IMapBookmarks mapBookmarks) {

		// add bookmarks
		menuMgr.add(new ActionBookmark_Add(mapBookmarks));

		// update bookmark
		if (_allRecentBookmarks.size() > 0) {

			final MapBookmark lastSelectedBookmark = _allRecentBookmarks.getFirst();

			// update last selected bookmark
			if (lastSelectedBookmark != null) {

				final ActionBookmark_Update actionUpdateBookmark = new ActionBookmark_Update(

						// set text with last selected bookmark
						NLS.bind(Messages.Action_Map_UpdateBookmark, lastSelectedBookmark.name),

						mapBookmarks);

				menuMgr.add(actionUpdateBookmark);
			}
		}

		if (getAllRecentBookmarks().size() <= 0 || numberOfRecentBookmarks <= 0) {
			return;
		}

		/*
		 * Recent bookmarks
		 */
		menuMgr.add(new Separator());

		int index = 0;
		final LinkedList<MapBookmark> allRecentBookmarks = getAllRecentBookmarks();

		for (final MapBookmark bookmark : allRecentBookmarks) {

			final String name = UI.SYMBOL_MNEMONIC + (++index) + UI.SPACE2 + bookmark.name;

			menuMgr.add(new ActionRecentBookmark(name, bookmark.id, mapBookmarks));

			// check if enough items are created
			if (index >= numberOfRecentBookmarks) {
				return;
			}
		}
	}

	public static void fillContextMenu_RecentBookmarks(final Menu menu, final IMapBookmarks mapBookmarks) {

		// add bookmarks
		fillMenuItem(menu, new ActionBookmark_Add(mapBookmarks));

		// update bookmark
		if (_allRecentBookmarks.size() > 0) {

			final MapBookmark lastSelectedBookmark = _allRecentBookmarks.getFirst();

			// update last selected bookmark
			if (lastSelectedBookmark != null) {

				final ActionBookmark_Update actionUpdateBookmark = new ActionBookmark_Update(

						// set text with last selected bookmark
						NLS.bind(Messages.Action_Map_UpdateBookmark, lastSelectedBookmark.name),

						mapBookmarks);

				fillMenuItem(menu, actionUpdateBookmark);
			}
		}

		if (getAllRecentBookmarks().size() <= 0 || numberOfRecentBookmarks <= 0) {
			return;
		}

		/*
		 * Recent bookmarks
		 */
//		new Separator().fill(menu, -1);

		int index = 0;
		final LinkedList<MapBookmark> allRecentBookmarks = getAllRecentBookmarks();

		for (final MapBookmark bookmark : allRecentBookmarks) {

			final String name = UI.SPACE4 + UI.SYMBOL_MNEMONIC + (++index) + UI.SPACE2 + bookmark.name;

			final ActionRecentBookmark action = new ActionRecentBookmark(name, bookmark.id, mapBookmarks);
			final ActionContributionItem contribItem = new ActionContributionItem(action);

			contribItem.fill(menu, -1);

			// check if enough items are created
			if (index >= numberOfRecentBookmarks) {
				return;
			}
		}
	}

	private static void fillMenuItem(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	public static void fireBookmarkEvent(final MapBookmark mapBookmark) {

		final Object[] allListeners = _bookmarkListeners.getListeners();
		for (final Object listener : allListeners) {
			((IMapBookmarkListener) (listener)).onSelectBookmark(mapBookmark);
		}
	}

	public static ArrayList<MapBookmark> getAllBookmarks() {
		return _allBookmarks;
	}

	public static LinkedList<MapBookmark> getAllRecentBookmarks() {
		return _allRecentBookmarks;
	}

	private static File getXmlFile() {

		final File xmlFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

		return xmlFile;
	}

	public static void onDeleteBookmark(final MapBookmark deletedBookmark) {

		MapBookmarkManager.getAllBookmarks().remove(deletedBookmark);
		MapBookmarkManager.getAllRecentBookmarks().remove(deletedBookmark);

		if (_mapBookmarkView != null) {
			_mapBookmarkView.onDeleteBookmark(deletedBookmark);
		}
	}

	public static void onUpdateBookmark(final MapBookmark updatedBookmark) {
		
		if (_mapBookmarkView != null) {
			_mapBookmarkView.onUpdateBookmark(updatedBookmark);
		}
	}

	private static void parse_10_Options(final XMLMemento xmlRoot) {

		final XMLMemento xmlOptions = (XMLMemento) xmlRoot.getChild(TAG_OPTIONS);

		if (xmlOptions == null) {
			return;
		}

		numberOfBookmarkItems = Util.getXmlInteger(
				xmlOptions,
				ATTR_NUMBER_OF_BOOKMARK_ITEMS,
				NUM_BOOKMARK_ITEMS_DEFAULT,
				NUM_BOOKMARK_ITEMS_MIN,
				NUM_BOOKMARK_ITEMS_MAX);

		numberOfRecentBookmarks = Util.getXmlInteger(
				xmlOptions,
				ATTR_NUMBER_OF_RECENT_BOOKMARKS,
				NUM_RECENT_BOOKMARKS_DEFAULT,
				NUM_RECENT_BOOKMARKS_MIN,
				NUM_RECENT_BOOKMARKS_MAX);
	}

	/**
	 * @param xmlRoot
	 *            Can be <code>null</code> when not available
	 * @param allBookmarks
	 */
	private static void parse_20_Bookmarks(	final XMLMemento xmlRoot,
											final ArrayList<MapBookmark> allBookmarks) {

		final XMLMemento xmlAllBookmarks = (XMLMemento) xmlRoot.getChild(TAG_ALL_BOOKMARKS);

		if (xmlAllBookmarks == null) {
			return;
		}

		for (final IMemento mementoBookmark : xmlAllBookmarks.getChildren()) {

			final XMLMemento xmlBookmark = (XMLMemento) mementoBookmark;

			try {

				final String xmlConfigType = xmlBookmark.getType();

				if (xmlConfigType.equals(TAG_BOOKMARK)) {

					// <Bookmark>

					final MapBookmark bookmark = new MapBookmark();

					parse_22_Bookmarks_One(xmlBookmark, bookmark);

					allBookmarks.add(bookmark);
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlBookmark), e);
			}
		}
	}

	private static void parse_22_Bookmarks_One(final XMLMemento xmlBookmark, final MapBookmark bookmark) {

// SET_FORMATTING_OFF
// SET_FORMATTING_ON

		bookmark.id = Util.getXmlString(xmlBookmark, ATTR_ID, Long.toString(System.nanoTime()));
		bookmark.name = Util.getXmlString(xmlBookmark, ATTR_NAME, UI.EMPTY_STRING);

		/*
		 * Map position
		 */
		final MapPosition mapPosition = new MapPosition();

		mapPosition.x = Util.getXmlDouble(xmlBookmark, ATTR_MAP_POSITION_X, 0.5);
		mapPosition.y = Util.getXmlDouble(xmlBookmark, ATTR_MAP_POSITION_Y, 0.5);
		mapPosition.scale = Util.getXmlDouble(xmlBookmark, ATTR_MAP_POSITION_SCALE, 1);

		mapPosition.bearing = Util.getXmlFloat(xmlBookmark, ATTR_MAP_POSITION_BEARING, 0f);
		mapPosition.tilt = Util.getXmlFloat(xmlBookmark, ATTR_MAP_POSITION_TILT, 0f);
		mapPosition.zoomLevel = Util.getXmlInteger(xmlBookmark, ATTR_MAP_POSITION_ZOOM_LEVEL, 1);

		bookmark.setMapPosition(mapPosition);
	}

	private static void parse_30_RecentBookmarks(	final XMLMemento xmlRoot,
													final LinkedList<MapBookmark> allRecentBookmarks) {

		// <AllLastRecentUsedBookmarks>
		final XMLMemento xmlAllRecentBookmarks = (XMLMemento) xmlRoot.getChild(TAG_ALL_RECENT_BOOKMARKS);

		if (xmlAllRecentBookmarks == null) {
			return;
		}

		for (final IMemento mementoRecentBookmark : xmlAllRecentBookmarks.getChildren()) {

			final XMLMemento xmlRecentBookmark = (XMLMemento) mementoRecentBookmark;

			try {

				final String xmlType = xmlRecentBookmark.getType();

				if (xmlType.equals(TAG_RECENT_BOOKMARK)) {

					// <LastRecentUsedBookmark>

					final String recentId = xmlRecentBookmark.getString(ATTR_ID);

					if (recentId == null) {
						continue;
					}

					for (final MapBookmark mapBookmark : _allBookmarks) {

						if (recentId.equals(mapBookmark.id)) {

							// found lru bookmark

							allRecentBookmarks.add(mapBookmark);

							break;
						}
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(Util.dumpMemento(xmlRecentBookmark), e);
			}
		}

	}

	/**
	 * Read or create configuration a xml file
	 * 
	 * @return
	 */
	private static synchronized void readBookmarksFromXml() {

		InputStreamReader reader = null;

		try {

			XMLMemento xmlRoot = null;

			// try to get bookmarks from saved xml file
			final File xmlFile = getXmlFile();
			final String absoluteFilePath = xmlFile.getAbsolutePath();
			final File inputFile = new File(absoluteFilePath);

			if (inputFile.exists()) {

				try {

					reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
					xmlRoot = XMLMemento.createReadRoot(reader);

				} catch (final Exception e) {
					// ignore
				}
			}

			if (xmlRoot == null) {
				return;
			}

			parse_10_Options(xmlRoot);

			// parse xml
			parse_20_Bookmarks(xmlRoot, _allBookmarks);

			// must be done AFTER the bookmarks are created
			parse_30_RecentBookmarks(xmlRoot, _allRecentBookmarks);

			// fill up recent bookmarks with the remaining bookmarks
			for (final MapBookmark mapBookmark : _allBookmarks) {

				final boolean isAvailable = _allRecentBookmarks.contains(mapBookmark);

				if (isAvailable == false) {

					_allRecentBookmarks.addLast(mapBookmark);
				}
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(reader);
		}
	}

	public static void removeBookmarkListener(final IMapBookmarkListener listener) {
		_bookmarkListeners.remove(listener);
	}

	public static void saveState() {

		final XMLMemento xmlRoot = create_Root();

		saveState_10_Options(xmlRoot);
		saveState_20_RecentBookmarks(xmlRoot);
		saveState_30_Bookmarks(xmlRoot);

		Util.writeXml(xmlRoot, getXmlFile());
	}

	private static void saveState_10_Options(final XMLMemento xmlRoot) {

		// <Options>
		final IMemento xmlOptions = xmlRoot.createChild(TAG_OPTIONS);

		xmlOptions.putInteger(ATTR_NUMBER_OF_RECENT_BOOKMARKS, numberOfRecentBookmarks);
		xmlOptions.putInteger(ATTR_NUMBER_OF_BOOKMARK_ITEMS, numberOfBookmarkItems);
	}

	private static void saveState_20_RecentBookmarks(final XMLMemento xmlRoot) {

		// <AllRecentBookmarks>
		final IMemento xmlAllRecentBookmarks = xmlRoot.createChild(TAG_ALL_RECENT_BOOKMARKS);

		for (final MapBookmark mapBookmark : _allRecentBookmarks) {

			// <RecentBookmark>
			final IMemento xmlLRU = xmlAllRecentBookmarks.createChild(TAG_RECENT_BOOKMARK);

			xmlLRU.putString(ATTR_ID, mapBookmark.id);
		}

	}

	private static void saveState_30_Bookmarks(final XMLMemento xmlRoot) {

		// <AllBookmarks>
		final IMemento xmlAllBookmarks = xmlRoot.createChild(TAG_ALL_BOOKMARKS);

		for (final MapBookmark bookmark : _allBookmarks) {

			// <Bookmark>
			final IMemento xmlBookmark = xmlAllBookmarks.createChild(TAG_BOOKMARK);
			{
				xmlBookmark.putString(ATTR_ID, bookmark.id);
				xmlBookmark.putString(ATTR_NAME, bookmark.name);

				/*
				 * Map position
				 */
				final MapPosition mapPosition = bookmark.getMapPosition();

				Util.setXmlDouble(xmlBookmark, ATTR_MAP_POSITION_X, mapPosition.x);
				Util.setXmlDouble(xmlBookmark, ATTR_MAP_POSITION_Y, mapPosition.y);
				Util.setXmlDouble(xmlBookmark, ATTR_MAP_POSITION_SCALE, mapPosition.scale);

				xmlBookmark.putFloat(ATTR_MAP_POSITION_BEARING, mapPosition.bearing);
				xmlBookmark.putFloat(ATTR_MAP_POSITION_TILT, mapPosition.tilt);
				xmlBookmark.putInteger(ATTR_MAP_POSITION_ZOOM_LEVEL, mapPosition.zoomLevel);
			}
		}
	}

	public static void setLastSelectedBookmark(final MapBookmark selectedBookmark) {

		_allRecentBookmarks.remove(selectedBookmark);
		_allRecentBookmarks.addFirst(selectedBookmark);
	}

	public static void setMapBookmarkView(final MapBookmarkView mapBookmarkView) {

		_mapBookmarkView = mapBookmarkView;
	}

}
