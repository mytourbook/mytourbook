package org.hibernate.ejb.packaging;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.util.StringHelper;

/**
 * Parse a JAR of any form (zip file, exploded directory, ...)
 * apply a set of filters (File filter, Class filter, Package filter)
 * and return the appropriate matching sets of elements
 *
 * @author Emmanuel Bernard
 */
//TODO shortcut when filters are null or empty
public abstract class JarVisitor {
	private static Log log = LogFactory.getLog( JarVisitor.class );
	protected String unqualifiedJarName;
	protected URL jarUrl;
	private boolean done = false;
	private List<Filter> filters = new ArrayList<Filter>();
	private Set<FileFilter> fileFilters = new HashSet<FileFilter>();
	private Set<JavaElementFilter> classFilters = new HashSet<JavaElementFilter>();
	;
	private Set<JavaElementFilter> packageFilters = new HashSet<JavaElementFilter>();
	private Set[] entries;

	/**
	 * Get the JAR URL of the JAR containing the given entry
	 *
	 * @param url
	 * @param entry
	 * @return the JAR URL
	 * @throws IllegalArgumentException if none URL is found
	 */
	public static final URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		URL jarUrl;
		String file = url.getFile();
		if ( ! entry.startsWith( "/" ) ) entry = "/" + entry;
		file = file.substring( 0, file.length() - entry.length() );
		if ( file.endsWith( "!" ) ) file = file.substring( 0, file.length() - 1 );
		try {
			String protocol = url.getProtocol();

			if ( "jar".equals( protocol )
					|| "wsjar".equals( protocol ) ) { //Websphere has it's own way
				jarUrl = new URL( file );
			}
			else if ( "zip".equals( protocol ) ) { //Weblogic has it's own way
				//we have extracted the zip file, so it should be read as a file
				jarUrl = new URL( "file", null, file );
			}
			else if ("code-source".equals( url.getProtocol() ) ) {
				//OC4J prevent ejb.jar access (ie everything without path
				//fix contributed by the community
				jarUrl = new File(file).toURL();
			}
			else {
				jarUrl = new URL( protocol, url.getHost(), url.getPort(), file );
			}
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(
					"Unable to determine JAR Url from " + url + ". Cause: " + e.getMessage()
			);
		}
		return jarUrl;
	}

	/**
	 * Build a JarVisitor on the given JAR URL applying th given filters
	 *
	 * @throws IllegalArgumentException if the URL is malformed
	 */
	public static final JarVisitor getVisitor(URL jarUrl, Filter[] filters) throws IllegalArgumentException {
		String protocol = jarUrl.getProtocol();
		if ( "jar".equals( protocol ) ) {
			//FIXME remove this code, this should not happen
			return new InputStreamZippedJarVisitor( jarUrl, filters );
		}
		else if ( StringHelper.isEmpty( protocol ) || "file".equals( protocol ) ) {
			File file;
			try {
				file = new File( jarUrl.toURI().getSchemeSpecificPart() );
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(
						"Unable to visit JAR " + jarUrl + ". Cause: " + e.getMessage()
				);
			}
			if ( file.isDirectory() ) {
				return new ExplodedJarVisitor( jarUrl, filters );
			}
			else {
				return new FileZippedJarVisitor( jarUrl, filters );
			}
		}
		else {
			//let's assume the url can return the jar as a zip stream
			return new InputStreamZippedJarVisitor( jarUrl, filters );
		}
	}

	/**
	 * Build a jar visitor from its jar string path
	 */
	private JarVisitor(String jarPath) {
		URL jarUrl;
		try {
			//is it an url
			jarUrl = new URL( jarPath );
		}
		catch (MalformedURLException e) {
			try {
				//consider it as a file path
				jarUrl = new URL( "file:" + jarPath );
			}
			catch (MalformedURLException ee) {
				throw new IllegalArgumentException( "Unable to find jar:" + jarPath, ee );
			}
		}
		this.jarUrl = jarUrl;
		unqualify();
	}

	protected JarVisitor(String fileName, Filter[] filters) {
		this( fileName );
		initFilters( filters );
	}

	private void initFilters(Filter[] filters) {
		for ( Filter filter : filters ) {
			if ( filter instanceof FileFilter ) {
				fileFilters.add( (FileFilter) filter );
			}
			else if ( filter instanceof ClassFilter ) {
				classFilters.add( (ClassFilter) filter );
			}
			else if ( filter instanceof PackageFilter ) {
				packageFilters.add( (PackageFilter) filter );
			}
			else {
				throw new AssertionError( "Unknown filter type: " + filter.getClass().getName() );
			}
			this.filters.add( filter );
		}
		int size = this.filters.size();
		this.entries = new Set[ size ];
		for ( int index = 0; index < size ; index++ ) {
			this.entries[index] = new HashSet<Entry>();
		}
	}

	protected JarVisitor(URL url, Filter[] filters) {
		this( url );
		initFilters( filters );
	}

	/**
	 * Get a JarVisitor to the jar <code>jarPath</code> applying the given filters
	 *
	 * @throws IllegalArgumentException if the jarPath is incorrect
	 */
	public static final JarVisitor getVisitor(String jarPath, Filter[] filters) throws IllegalArgumentException {
		File file = new File( jarPath );
		if ( file.isFile() ) {
			return new InputStreamZippedJarVisitor( jarPath, filters );
		}
		else {
			return new ExplodedJarVisitor( jarPath, filters );
		}
	}

	private JarVisitor(URL url) {
		jarUrl = url;
		unqualify();
	}

	protected void unqualify() {
		//FIXME weak algorithm subject to AOOBE
		String fileName = jarUrl.getFile();
		int slash = fileName.lastIndexOf( "/" );
		if ( slash != -1 ) {
			fileName = fileName.substring(
					fileName.lastIndexOf( "/" ) + 1,
					fileName.length()
			);
		}
		if ( fileName.length() > 4 && fileName.endsWith( "ar" ) && fileName.charAt( fileName.length() - 4 ) == '.' ) {
			fileName = fileName.substring( 0, fileName.length() - 4 );
		}
		unqualifiedJarName = fileName;
		log.debug( "Searching mapped entities in jar/par: " + jarUrl );
	}

	/**
	 * Get the unqualified Jar name (ie wo path and wo extension)
	 */
	public String getUnqualifiedJarName() {
		return unqualifiedJarName;
	}

	public Filter[] getFilters() {
		return filters.toArray( new Filter[ filters.size() ] );
	}

	/**
	 * Return the matching entries for each filter in the same order the filter where passed
	 *
	 * @return array of Set of JarVisitor.Entry
	 * @throws IOException if something went wrong
	 */
	public final Set[] getMatchingEntries() throws IOException {
		if ( !done ) {
			//avoid url access and so on
			if ( filters.size() > 0 ) doProcessElements();
			done = true;
		}
		return entries;
	}

	protected abstract void doProcessElements() throws IOException;

	//TODO avoid 2 input stream when not needed
	protected final void addElement(String entryName, InputStream is, InputStream secondIs) throws IOException {
		if ( entryName.endsWith( "package-info.class" ) ) {
			String name = entryName.substring( 0, entryName.length() - ".package-info.class".length() )
					.replace( '/', '.' );
			executeJavaElementFilter( name, packageFilters, is, secondIs );
		}
		else if ( entryName.endsWith( ".class" ) ) {
			String name = entryName.substring( 0, entryName.length() - ".class".length() ).replace( '/', '.' );
			log.debug( "Filtering: " + name );
			executeJavaElementFilter( name, classFilters, is, secondIs );
		}
		else {
			String name = entryName;
			boolean accepted = false;
			for ( FileFilter filter : fileFilters ) {
				if ( filter.accept( name ) ) {
					accepted = true;
					InputStream localIs;
					if ( filter.getStream() ) {
						localIs = secondIs;
					}
					else {
						localIs = null;
						secondIs.close();
					}
					is.close();
					log.debug( "File Filter matched for " + name );
					Entry entry = new Entry( name, localIs );
					int index = this.filters.indexOf( filter );
					this.entries[index].add( entry );
				}
			}
			if (!accepted) {
				//not accepted free resources
				is.close();
				secondIs.close();
			}
		}
	}

	private void executeJavaElementFilter(
			String name, Set<JavaElementFilter> filters, InputStream is, InputStream secondIs
	) throws IOException {
		boolean accepted = false;
		for ( JavaElementFilter filter : filters ) {
			if ( filter.accept( name ) ) {
				//FIXME cannot currently have a class filtered twice but matching once
				// need to copy the is
				boolean match = checkAnnotationMatching( is, filter );
				if ( match ) {
					accepted = true;
					InputStream localIs;
					if ( filter.getStream() ) {
						localIs = secondIs;
					}
					else {
						localIs = null;
						secondIs.close();
					}
					log.debug( "Java element filter matched for " + name );
					Entry entry = new Entry( name, localIs );
					int index = this.filters.indexOf( filter );
					this.entries[index].add( entry );
					break; //we matched
				}
			}
		}
		if (!accepted) {
			is.close();
			secondIs.close();
		}
	}

	private boolean checkAnnotationMatching(InputStream is, JavaElementFilter filter) throws IOException {
		if ( filter.getAnnotations().length == 0 ) {
			is.close();
			return true;
		}
		DataInputStream dstream = new DataInputStream( is );
		ClassFile cf = null;

		try {
			cf = new ClassFile( dstream );
		}
		finally {
			dstream.close();
			is.close();
		}
		boolean match = false;
		AnnotationsAttribute visible = (AnnotationsAttribute) cf.getAttribute( AnnotationsAttribute.visibleTag );
		if ( visible != null ) {
			for ( Class annotation : filter.getAnnotations() ) {
				match = visible.getAnnotation( annotation.getName() ) != null;
				if ( match ) break;
			}
		}
		return match;
	}

	/**
	 * Filter used when searching elements in a JAR
	 */
	public static abstract class Filter {
		private boolean retrieveStream;

		protected Filter(boolean retrieveStream) {
			this.retrieveStream = retrieveStream;
		}

		public boolean getStream() {
			return retrieveStream;
		}
	}

	/**
	 * Filter use to match a file by its name
	 */
	public static abstract class FileFilter extends Filter {

		/**
		 * @param retrieveStream Give back an open stream to the matching element or not
		 */
		public FileFilter(boolean retrieveStream) {
			super( retrieveStream );
		}

		/**
		 * Return true if the fully qualified file name match
		 */
		public abstract boolean accept(String name);
	}

	/**
	 * Filter a Java element (class or package per fully qualified name and annotation existence)
	 * At least 1 annotation has to annotate the element and the accept method must match
	 * If none annotations are passed, only the accept method must pass.
	 */
	public static abstract class JavaElementFilter extends Filter {
		private Class[] annotations;

		/**
		 * @param retrieveStream Give back an open stream to the matching element or not
		 * @param annotations	Array of annotations that must be present to match (1 of them should annotate the element
		 */
		protected JavaElementFilter(boolean retrieveStream, Class[] annotations) {
			super( retrieveStream );
			this.annotations = annotations == null ? new Class[]{} : annotations;
		}

		public Class[] getAnnotations() {
			return annotations;
		}

		/**
		 * Return true if the fully qualified name match
		 */
		public abstract boolean accept(String javaElementName);
	}

	/**
	 * Filter on class elements
	 *
	 * @see JavaElementFilter
	 */
	public static abstract class ClassFilter extends JavaElementFilter {
		/**
		 * @see JavaElementFilter#JavaElementFilter(boolean, Class[])
		 */
		protected ClassFilter(boolean retrieveStream, Class[] annotations) {
			super( retrieveStream, annotations );
		}
	}

	/**
	 * Filter on pachage element
	 *
	 * @see JavaElementFilter
	 */
	public static abstract class PackageFilter extends JavaElementFilter {
		/**
		 * @see JavaElementFilter#JavaElementFilter(boolean, Class[])
		 */
		protected PackageFilter(boolean retrieveStream, Class[] annotations) {
			super( retrieveStream, annotations );
		}
	}

	/**
	 * Represent a JAR entry
	 * Contains a name and an optional Input stream to the entry
	 */
	public static class Entry {
		private String name;
		private InputStream is;

		public Entry(String name, InputStream is) {
			this.name = name;
			this.is = is;
		}

		public String getName() {
			return name;
		}

		public InputStream getInputStream() {
			return is;
		}

		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( o == null || getClass() != o.getClass() ) return false;

			final Entry entry = (Entry) o;

			if ( !name.equals( entry.name ) ) return false;

			return true;
		}

		public int hashCode() {
			return name.hashCode();
		}
	}
}
