//$Id: FileZippedJarVisitor.java 10242 2006-08-11 04:21:33Z epbernard $
package org.hibernate.ejb.packaging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Work on a JAR that can be accessed through a File
 *
 * @author Emmanuel Bernard
 */
public class FileZippedJarVisitor extends JarVisitor {
	private static Log log = LogFactory.getLog( FileZippedJarVisitor.class );

	public FileZippedJarVisitor(String fileName, Filter[] filters) {
		super( fileName, filters );
	}

	public FileZippedJarVisitor(URL url, Filter[] filters) {
		super( url, filters );
	}

	protected void doProcessElements() throws IOException {
		JarFile jarFile;
		try {
			jarFile = new JarFile( jarUrl.toURI().getSchemeSpecificPart() );
		}
		catch (IOException ze) {
			log.warn( "Unable to find file (ignored): " + jarUrl, ze );
			return;
		}
		catch (URISyntaxException e) {
			log.warn( "Malformed url: " + jarUrl, e );
			return;
		}
		Enumeration<? extends ZipEntry> entries = jarFile.entries();
		while ( entries.hasMoreElements() ) {
			ZipEntry entry = entries.nextElement();
			if ( !entry.isDirectory() ) {
				addElement(
						entry.getName(),
						new BufferedInputStream( jarFile.getInputStream( entry ) ),
						new BufferedInputStream( jarFile.getInputStream( entry ) )
				);
			}
		}
	}
}
