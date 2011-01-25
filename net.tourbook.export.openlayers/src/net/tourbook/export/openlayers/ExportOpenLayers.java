package net.tourbook.export.openlayers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.export.ExportUtil;
import net.tourbook.ui.UI;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * Builds the OpenLayers HTML file from a Velocity template 
 */
public class ExportOpenLayers {

	private static final String	HTML_EXTENSION		= "html";									//$NON-NLS-1$
	private static final String	HTML_TARGET_PREFIX	= "MyTourbook_";							//$NON-NLS-1$
	// workspace resource paths
	private static final String	HTML_SOURCE_DIR		= "/html-template";							//$NON-NLS-1$
	private static final String	HTML_SOURCE_NAME	= "openlayers.html.vm";						//$NON-NLS-1$
	private static final String	HTML_SOURCE_PATH	= HTML_SOURCE_DIR + "/" + HTML_SOURCE_NAME;	//$NON-NLS-1$

	private final String		_fileExtension;

	public ExportOpenLayers(final String fileExtension) {
		this._fileExtension = fileExtension;
	}

	/**
	 * Single tour export file (may contain multiple tracks), custom html name (for exporting)
	 */
	public String doExportHTML(final String tourFileName, final String targetDir, final String htmlTargetName)
			throws IOException {

		List<LayerContext> layerContextList = new ArrayList<LayerContext>();
		LayerContext layerContext = new LayerContext();
		layerContext.setFileName(tourFileName);
		// File name as layer name 
		// (one layer for multiple tours, so no single tour name for layer)
		layerContext.setTrackName(tourFileName);
		layerContextList.add(layerContext);

		return doExportHTML(layerContextList, targetDir, htmlTargetName, null);
	}

	/**
	 * Multiple tour export files / layers, custom html name (for exporting)
	 */
	public String doExportHTML(final List<TourData> tourDataList, final String targetDir, final String htmlTargetName)
			throws IOException {

		List<LayerContext> layerContextList = getLayerContextList(tourDataList);
		return doExportHTML(layerContextList, targetDir, htmlTargetName, null);
	}

	/**
	 * Multiple tour export files / layers, default html name (for opening in browser)
	 */
	public String doExportHTML(final List<TourData> tourDataList, final String targetDir, final MapContext mapContext)
			throws IOException {

		File htmlTmpFile = File.createTempFile(HTML_TARGET_PREFIX, '.' + HTML_EXTENSION);
		htmlTmpFile.deleteOnExit();

		List<LayerContext> layerContextList = getLayerContextList(tourDataList);
		return doExportHTML(layerContextList, targetDir, htmlTmpFile.getName(), mapContext);
	}

	private String doExportHTML(final List<LayerContext> layerContextList,
								final String targetDir,
								final String htmlTargetName,
								final MapContext mapContext) throws IOException {

		// workspace resource path
		final String htmlTargetPath = new StringBuilder(targetDir).append("/").append(htmlTargetName).toString(); //$NON-NLS-1$

		final VelocityContext context = new VelocityContext();
		context.put("map", mapContext); //$NON-NLS-1$
		context.put("tracks", layerContextList); //$NON-NLS-1$

		final Reader templateReader = new InputStreamReader(this.getClass().getResourceAsStream(HTML_SOURCE_PATH));
		final File exportFile = new File(htmlTargetPath);
		final Writer exportWriter = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(exportFile),
				UI.UTF_8));

		try {
			Velocity.evaluate(context, exportWriter, "MyTourbook", templateReader); //$NON-NLS-1$
		} finally {
			exportWriter.close();
		}

		return htmlTargetName;
	}

//TODO NR: template customizable: extract to workspace, don't overwrite if already exists, and process from there
// requires plug-in org.eclipse.core.filesystem
//	
//	URL url = null;
//	IFileStore source = null;
//	IFileStore target = null;
//	try {
//
//		url = this.getClass().getResource(htmlSourcePath);
//		System.out.println("url = " + url);
//		url = FileLocator.resolve(url);
//		System.out.println("url = " + url);
//		URI uri = url.toURI();
//		System.out.println("uri = " + uri);
//
//		IFileSystem fileSystem = EFS.getLocalFileSystem();
//		source = fileSystem.getStore(uri);
//		target = fileSystem.getStore(new File(tmpDir, htmlName).toURI());
//
//		// copy html file to target
//		source.copy(target, EFS.OVERWRITE, null);
//	} catch (final CoreException e) {
//	StatusUtil.log("Error copying '" + source + "' to '" + target + "'", e); //$NON-NLS-1$
//} catch (URISyntaxException e) {
//	StatusUtil.log("Invalid URI: '" + url + "'", e); //$NON-NLS-1$

	/**
	 * Build Layer value objects for velocity context
	 */
	private List<LayerContext> getLayerContextList(final List<TourData> tourDataList) {

		List<LayerContext> layerContextList = new ArrayList<LayerContext>();
		for (final Iterator<TourData> iter = tourDataList.iterator(); iter.hasNext();) {
			final TourData tourData = iter.next();

			final String fileName = ExportUtil.getFileName(tourData, _fileExtension);

			LayerContext gpxLayer = new LayerContext();
			// Layer Name
			final String tourTitle = tourData.getTourTitle();
			if (tourTitle != null && tourTitle.length() > 0) {
				gpxLayer.setTrackName(tourTitle);
			} else {
				gpxLayer.setTrackName(fileName);
			}
			// URL (file name)
			gpxLayer.setFileName(fileName);
			layerContextList.add(gpxLayer);
		}

		return layerContextList;
	}
}
