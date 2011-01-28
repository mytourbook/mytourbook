package net.tourbook.export.openlayers;

import static org.eclipse.ui.browser.IWorkbenchBrowserSupport.AS_VIEW;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import net.tourbook.data.TourData;
import net.tourbook.export.ExportTourHTML;
import net.tourbook.export.OpenLayersExportUtil;
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.extension.export.ExportTourExtension;
import net.tourbook.mapping.TourMapView;
import net.tourbook.util.StatusUtil;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.util.Util;

/**
 * The main class of the OpenLayers export plugin. Offers an action which opens an external browser
 * to show the current map position and displayed tours with OpenLayers on Google or Bing Maps, ...
 */
public class OpenLayersExportPlugin {

	private static final String	OPENLAYERS_BROWSER_ID	= "net.tourbook.export.openlayers"; //$NON-NLS-1$

	private final TourMapView	_tourMapView;

	private String				_fileExtension;

	public OpenLayersExportPlugin(final TourMapView tourMapView) {
		this._tourMapView = tourMapView;
		this._fileExtension = ExportTourHTML.GPX_EXTENSION;

		// initialize velocity
		VelocityService.init();
	}

	public void actionOpenWebMapBrowser() {

		final String targetDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$

		try {

			ExportTourExtension exportTour = OpenLayersExportUtil.getExportTour(_fileExtension);

			if (exportTour != null) {

				final List<TourData> tourDataList = _tourMapView.getTourDataList();

				// write tour files
				List<String> allFilePath = exportTour.exportTourHeadless(tourDataList, targetDir);

				// delete temporary file when program exits
				for (String filePath : allFilePath) {
					final File tourTmpFile = new File(filePath);
					tourTmpFile.deleteOnExit();
				}

				// write HTML
				final ExportOpenLayers exportOpenLayers = new ExportOpenLayers(_fileExtension);
				final String htmlName = exportOpenLayers.doExportHTML(tourDataList, targetDir, getMapContext());

				// open browser
				browse(targetDir, htmlName);

			} else {
				StatusUtil.log("No ExportTourExtension found for extension '" + _fileExtension + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}

		} catch (final IOException e) {
			StatusUtil.log(e);
		}
	}

	private MapContext getMapContext() {

		Map map = _tourMapView.getMap();

		MapContext mapContext = new MapContext();
		mapContext.setLat(map.getGeoCenter().latitude);
		mapContext.setLon(map.getGeoCenter().longitude);
		mapContext.setZoom(map.getZoom());
		return mapContext;
	}

	private void browse(String tmpDir, String htmlName) {

		URI uri = new File(tmpDir, htmlName).toURI();

		final Util.IBrowserGetter browserGetter = new Util.IBrowserGetter() {
			@Override
			public IWebBrowser getBrowser(IWorkbenchBrowserSupport support) throws PartInitException {

				return support.createBrowser(AS_VIEW, OPENLAYERS_BROWSER_ID, "Browser Maps", null);
			}
		};
		Util.openLink(browserGetter, uri.toString());
	}

	public String getFileExtension() {
		return _fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this._fileExtension = fileExtension;
	}
}
