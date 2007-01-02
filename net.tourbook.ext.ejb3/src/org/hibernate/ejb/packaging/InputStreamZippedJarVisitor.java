//$Id: InputStreamZippedJarVisitor.java 9796 2006-04-26 06:46:52Z epbernard $
package org.hibernate.ejb.packaging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Work on a JAR that can only be accessed through a inputstream
 * This is less efficient than the {@link FileZippedJarVisitor}
 *
 * @author Emmanuel Bernard
 */
public class InputStreamZippedJarVisitor extends JarVisitor {
	private static Log log = LogFactory.getLog( InputStreamZippedJarVisitor.class );

	public InputStreamZippedJarVisitor(URL url, Filter[] filters) {
		super( url, filters );
	}

	public InputStreamZippedJarVisitor(String fileName, Filter[] filters) {
		super( fileName, filters );
	}

	protected void doProcessElements() throws IOException {
		JarInputStream jis;
		try {
			jis = new JarInputStream( jarUrl.openStream() );
		}
		catch (Exception ze) {
			log.warn( "Unable to find file (ignored): " + jarUrl, ze );
			return;
		}
		JarEntry entry;
		while ( ( entry = jis.getNextJarEntry() ) != null ) {
			if ( !entry.isDirectory() ) {
				int size;
				byte[] tmpByte = new byte[ 4096 ];
				byte[] entryBytes = new byte[0];
				for ( ; ; ) {
					size = jis.read( tmpByte );
					if ( size == -1 ) break;
					byte[] current = new byte[ entryBytes.length + size ];
					System.arraycopy( entryBytes, 0, current, 0, entryBytes.length );
					System.arraycopy( tmpByte, 0, current, entryBytes.length, size );
					entryBytes = current;
				}
				//this is bad cause we actually read everything instead of walking it lazily
				addElement(
						entry.getName(),
						new ByteArrayInputStream( entryBytes ),
						new ByteArrayInputStream( entryBytes )
				);
			}
		}
		jis.close();
	}
}
