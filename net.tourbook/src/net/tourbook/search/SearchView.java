/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class SearchView extends ViewPart {

	public static final String		ID					= "net.tourbook.search.SearchView"; //$NON-NLS-1$

	private static final String		SEARCH_RESULT_CSS	= "/html/search-result.css";

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	private final IDialogSettings	_state				= TourbookPlugin.getState(ID);
	private PostSelectionProvider	_postSelectionProvider;

	private IPropertyChangeListener	_prefChangeListener;

	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;
	private String					_htmlCss;

	/**
	 * Set time that an error displays the correct time.
	 */
	private long					_searchStartTime	= System.currentTimeMillis();

	/*
	 * UI controls
	 */
	private Browser					_browser;

	private Button					_btnSearch;

	private Text					_txtSearch;

	private Text					_txtStatus;

	/**
	 * Copied from {@link org.eclipse.jdt.internal.ui.dialogs.GenerateToStringDialog} and adjusted.
	 */
	private class LuceneSuggestProposalProvider implements IContentProposalProvider {

		private String	latestContents;
		private int		latestPosition;

		private class Proposal implements IContentProposal {

			final private String	proposal;
			private int				position;

			public Proposal(final String proposal) {
				this.proposal = proposal;
				this.position = proposal.length();
			}

			public String getContent() {

				final int overlap = stringOverlap(latestContents.substring(0, latestPosition), proposal);

				position = proposal.length() - overlap;

				return proposal.substring(overlap);
			}

			public int getCursorPosition() {
				return position;
			}

			public String getDescription() {
//				return parser.getVariableDescriptions().get(proposal);
				return null;
			}

			public String getLabel() {
				return proposal;
			}
		}

		public IContentProposal[] getProposals(final String contents, final int position) {


			final List<Proposal> primaryProposals = new ArrayList<Proposal>();
			final List<Proposal> secondaryProposals = new ArrayList<Proposal>();

			final String[] proposalStrings = MTSearchManager.getProposals(contents, position);
//			final String[] proposalStrings = {
//					//
//					"aa",
//					"ab",
//					"ab1",
//					"ab2",
//					"ab3",
//					"ab4",
//					"ab5",
//					"aba",
//					"abb",
//					"abc",
//					"abd",
//					"abz",
//			//
//			};
			final String contentToCursor = contents.substring(0, position);

			for (final String proposalString : proposalStrings) {

				if (stringOverlap(contentToCursor, proposalString) > 0) {
					primaryProposals.add(new Proposal(proposalString));
				} else {
					secondaryProposals.add(new Proposal(proposalString));
				}
			}

			this.latestContents = contents;
			this.latestPosition = position;

			primaryProposals.addAll(secondaryProposals);

			return primaryProposals.toArray(new IContentProposal[0]);
		}

		/**
		 * Checks if the end of the first string is equal to the beginning of of the second string.
		 * 
		 * @param s1
		 *            first String
		 * @param s2
		 *            second String
		 * @return length of overlapping segment (0 if strings don't overlap)
		 */
		private int stringOverlap(final String s1, final String s2) {

			final int l1 = s1.length();

			for (int l = 1; l <= Math.min(s1.length(), s2.length()); l++) {

				boolean ok = true;

				for (int i = 0; i < l; i++) {
					if (s1.charAt(l1 - l + i) != s2.charAt(i)) {
						ok = false;
						break;
					}
				}

				if (ok) {
					return l;
				}
			}

			return 0;
		}
	}

	public class MTContentProposalAdapter extends ContentProposalAdapter {

		public MTContentProposalAdapter(final Control control,
										final IControlContentAdapter controlContentAdapter,
										final IContentProposalProvider proposalProvider,
										final KeyStroke keyStroke,
										final char[] autoActivationCharacters) {

			super(control, controlContentAdapter, proposalProvider, keyStroke, autoActivationCharacters);
		}

	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == SearchView.this) {
					saveState();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == SearchView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						for (final TourData tourData : modifiedTours) {

						}
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();
	}

	private void createActions() {

		fillActionBars();
	}

	private String createHTML_10_Head() {

		final String html = ""// //$NON-NLS-1$
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" //$NON-NLS-1$
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" //$NON-NLS-1$
				+ _htmlCss
				+ "\n"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_20_Body(final SearchResult searchResult) {

		final StringBuilder sb = new StringBuilder();

		for (final Entry<String, SearchResultItem> entry : searchResult.items.entrySet()) {

			sb.append("<div class='result-item'>"); //$NON-NLS-1$
			{
				final SearchResultItem resultItem = entry.getValue();

				final String tourTitle = resultItem.tourTitle;
				if (tourTitle != null) {
					sb.append("<div class='result-title'>"); //$NON-NLS-1$
					{
						sb.append(tourTitle);
					}
					sb.append("</div>\n"); //$NON-NLS-1$
				}

				final String tourDescription = resultItem.tourDescription;
				if (tourDescription != null) {
					sb.append("<div class='result-description'>"); //$NON-NLS-1$
					{
						sb.append(tourDescription);
					}
					sb.append("</div>\n"); //$NON-NLS-1$
				}
			}
			sb.append("</div>\n"); //$NON-NLS-1$
		}

		return sb.toString();
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI();

		createUI(parent);
		createActions();

		addTourEventListener();
		addPrefListener();
		addPartListener();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.spacing(0, 2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		{
			createUI_10_Query(container);
			createUI_20_Status(container);
			createUI_30_Result(container);
		}
	}

	private Composite createUI_10_Query(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(2, 2, 2, 2)
				.spacing(5, 0)
				.numColumns(2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		{
// ALTERNATIVE TEXT CONTROL
//tasktop.TextSearchControl
			/*
			 * Text: Search field
			 */
			_txtSearch = new Text(container, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtSearch);
			_txtSearch.addTraverseListener(new TraverseListener() {
				@Override
				public void keyTraversed(final TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_RETURN) {
						onSelectSearch();
					}
				}
			});

			final ContentProposalAdapter contentAssistCommandAdapter = new MTContentProposalAdapter(
					_txtSearch,
					new TextContentAdapter(),
					new LuceneSuggestProposalProvider(),
					KeyStroke.getInstance(SWT.ARROW_DOWN),
					new char[] { '$' });

			contentAssistCommandAdapter.setPropagateKeys(false);

			/*
			 * Button: Search
			 */
			_btnSearch = new Button(container, SWT.NONE);
			_btnSearch.setText("&Search");
			_btnSearch.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectSearch();
				}
			});
		}

		return container;
	}

	private void createUI_20_Status(final Composite parent) {

		_txtStatus = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.BEGINNING)
				.applyTo(_txtStatus);

		_txtStatus.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
	}

	private void createUI_30_Result(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

			} catch (final Exception e) {

				/*
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 */
				_browser = new Browser(parent, SWT.MOZILLA);
			}

			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {
					onBrowserLocationChanging(event);
				}
			});

			_browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(final ProgressEvent event) {
					onBrowserCompleted(event);
				}
			});

		} catch (final SWTError e) {

			_btnSearch.setEnabled(false);
			_txtSearch.setEnabled(false);

			final String message = e.getMessage();

			updateUI_Status(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, message), false);

			StatusUtil.log(e);
		}
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

//		tbm.add(_actionTourBlogMarker);
	}

	private void initUI() {

		try {

			/*
			 * load css from file
			 */
			final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(SEARCH_RESULT_CSS);
			final URL fileUrl = FileLocator.toFileURL(bundleUrl);
			final URI fileUri = fileUrl.toURI();
			final File file = new File(fileUri);

			final String cssContent = Util.readContentFromFile(file.getAbsolutePath());

			_htmlCss = "<style>" + cssContent + "</style>"; //$NON-NLS-1$ //$NON-NLS-2$

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}
	}

	private void onBrowserCompleted(final ProgressEvent event) {
		// TODO Auto-generated method stub

	}

	private void onBrowserLocationChanging(final LocationEvent event) {
		// TODO Auto-generated method stub

	}

	private void onSelectSearch() {

		_searchStartTime = System.currentTimeMillis();

		final String searchText = _txtSearch.getText().trim();

		// check if search text is valid
		if (searchText.length() == 0) {
			updateUI_Status("No result", false);
			return;
		}
		if (searchText.startsWith("*") || searchText.startsWith("?")) {
			updateUI_Status("* or ? is not allowed as first character", false);
			return;
		}

		final SearchResult searchResult = MTSearchManager.search(searchText);

		if (searchResult.items.size() == 0) {
			updateUI_Status("No result", false);
			return;
		}

		updateUI_SearchResult(searchResult);
	}

	private void saveState() {

	}

	@Override
	public void setFocus() {

		_txtSearch.setFocus();
	}

	private void updateUI_SearchResult(final SearchResult searchResult) {

		final String html = "" // //$NON-NLS-1$
				+ "<!DOCTYPE html>\n" // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
				+ "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>\n" //$NON-NLS-1$
				+ ("<head>\n" + createHTML_10_Head() + "\n</head>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("<body>\n" + createHTML_20_Body(searchResult) + "\n</body>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</html>"; //$NON-NLS-1$

		_browser.setRedraw(true);
		_browser.setText(html);

		updateUI_Status(String.format("Hits: %d", searchResult.totalHits), true);
	}

	/**
	 * @param statusText
	 * @param isResultAvailable
	 */
	private void updateUI_Status(final String statusText, final boolean isResultAvailable) {

		/*
		 * Update status
		 */
		final long time = System.currentTimeMillis() - _searchStartTime;

		String text;
		if (statusText == null) {
			text = String.format("%d ms", time);
		} else {
			text = String.format("%s - %d ms", statusText, time);
		}

		_txtStatus.setText(text);

		/*
		 * Clear browser when an error occured.
		 */
		if (!isResultAvailable && _browser != null) {

			_browser.setRedraw(true);
			_browser.setText(UI.EMPTY_STRING);
		}
	}
}
