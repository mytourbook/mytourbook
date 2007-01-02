//$Id: Ejb3Configuration.java 10912 2006-12-04 17:10:27Z epbernard $
package org.hibernate.ejb;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.naming.BinaryRefAddr;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.MappingNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.ejb.connection.InjectedDataSourceConnectionProvider;
import org.hibernate.ejb.instrument.InterceptFieldClassFileTransformer;
import org.hibernate.ejb.packaging.JarVisitor;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.ejb.transaction.JoinableCMTTransactionFactory;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.ejb.util.LogHelper;
import org.hibernate.ejb.util.NamingHelper;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.event.EventListeners;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.reflection.java.xml.XMLContext;
import org.hibernate.secure.JACCConfiguration;
import org.hibernate.transaction.JDBCTransactionFactory;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;
import org.hibernate.util.XMLHelper;
import org.jboss.util.file.ArchiveBrowser;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * Allow a fine tuned configuration of an EJB 3.0 EntityManagerFactory
 *
 * A Ejb3Configuration object is only guaranteed to create one EntityManagerFactory.
 * Multiple usage of #buildEntityManagerFactory() is not guaranteed.
 *
 * After #buildEntityManagerFactory() has been called, you no longer can change the configuration
 * state (no class adding, no property change etc)
 *
 * When serialized / deserialized or retrieved from the JNDI, you no longer can change the
 * configuration state (no class adding, no property change etc)
 *
 * Putting the configuration in the JNDI is an expensive operation that requires a partial
 * serialization
 *
 * @author Emmanuel Bernard
 */
public class Ejb3Configuration implements Serializable, Referenceable {
	private static final String IMPLEMENTATION_NAME = HibernatePersistence.class.getName();
	private static final String META_INF_ORM_XML = "META-INF/orm.xml";
	private static Log log = LogFactory.getLog( Ejb3Configuration.class );
	private static EntityNotFoundDelegate ejb3EntityNotFoundDelegate = new Ejb3EntityNotFoundDelegate();

	private static class Ejb3EntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException("Unable to find " + entityName  + " with id " + id);
		}
	}

	static {
		Version.touch();
	}

	private AnnotationConfiguration cfg;
	private SettingsFactory settingsFactory;
	//made transient and not restored in deserialization on purpose, should no longer be called after restoration
	private transient EventListenerConfigurator listenerConfigurator;
	private PersistenceUnitTransactionType transactionType;
	private boolean discardOnClose;
	//made transient and not restored in deserialization on purpose, should no longer be called after restoration
	private transient ClassLoader overridenClassLoader;


	public Ejb3Configuration() {
		settingsFactory = new InjectionSettingsFactory();
		cfg = new AnnotationConfiguration( settingsFactory );
		cfg.setEntityNotFoundDelegate( ejb3EntityNotFoundDelegate );
		listenerConfigurator = new EventListenerConfigurator( this );
	}

	/**
	 * Used to inject a datasource object as the connection provider.
	 * If used, be sure to <b>not override</b> the {@link Environment.CONNECTION_PROVIDER}
	 * property
	 */
	public void setDataSource(DataSource ds) {
		if ( ds != null ) {
			Map cpInjection = new HashMap();
			cpInjection.put( "dataSource", ds );
			( (InjectionSettingsFactory) settingsFactory ).setConnectionProviderInjectionData( cpInjection );
			this.setProperty( Environment.CONNECTION_PROVIDER, InjectedDataSourceConnectionProvider.class.getName() );
		}
	}

	/**
	 * create a factory from a parsed persistence.xml
	 * Especially the scanning of classes and additional jars is done already at this point.
	 */
	private Ejb3Configuration configure(PersistenceMetadata metadata, Map overrides) {
		log.debug( "Creating Factory: " + metadata.getName() );

		Map workingVars = new HashMap();
		workingVars.put( HibernatePersistence.PERSISTENCE_UNIT_NAME, metadata.getName() );

		if ( StringHelper.isNotEmpty( metadata.getJtaDatasource() ) ) {
			this.setProperty( Environment.DATASOURCE, metadata.getJtaDatasource() );
		}
		else if ( StringHelper.isNotEmpty( metadata.getNonJtaDatasource() ) ) {
			this.setProperty( Environment.DATASOURCE, metadata.getNonJtaDatasource() );
		}
		defineTransactionType( metadata.getTransactionType(), workingVars );
		if ( metadata.getClasses().size() > 0 ) {
			workingVars.put( HibernatePersistence.CLASS_NAMES, metadata.getClasses() );
		}
		if ( metadata.getPackages().size() > 0 ) {
			workingVars.put( HibernatePersistence.PACKAGE_NAMES, metadata.getPackages() );
		}
		if ( metadata.getMappingFiles().size() > 0 ) {
			workingVars.put( HibernatePersistence.XML_FILE_NAMES, metadata.getMappingFiles() );
		}
		if ( metadata.getHbmfiles().size() > 0 ) {
			workingVars.put( HibernatePersistence.HBXML_FILES, metadata.getHbmfiles() );
		}
		Properties props = new Properties();
		props.putAll( metadata.getProps() );
		if ( overrides != null ) {
			for ( Map.Entry entry : (Set<Map.Entry>) overrides.entrySet() ) {
				Object value = entry.getValue();
				props.put( entry.getKey(), value == null ? "" :  value ); //alter null, not allowed in properties
			}
		}
		configure( props, workingVars );
		return this;
	}

	/**
	 * Build the configuration from an entity manager name and given the
	 * appropriate extra properties. Those properties override the one get through
	 * the peristence.xml file.
	 * If the persistence unit name is not found or does not match the Persistence Provider, null is returned
	 *
	 * @param persistenceUnitName persistence unit name
	 * @param integration properties passed to the persistence provider
	 * @return configured Ejb3Configuration or null if no persistence unit match
	 */
	public Ejb3Configuration configure(String persistenceUnitName, Map integration) {
		try {
			log.debug( "Look up for persistence unit: " + persistenceUnitName );
			integration = integration == null ?
					CollectionHelper.EMPTY_MAP :
					Collections.unmodifiableMap( integration );
			Enumeration<URL> xmls = Thread.currentThread()
					.getContextClassLoader()
					.getResources( "META-INF/persistence.xml" );
			if ( ! xmls.hasMoreElements() ) {
				log.info( "Could not find any META-INF/persistence.xml file in the classpath");
			}
			while ( xmls.hasMoreElements() ) {
				URL url = xmls.nextElement();
				log.trace( "Analyse of persistence.xml: " + url );
				List<PersistenceMetadata> metadataFiles = PersistenceXmlLoader.deploy(
						url,
						integration,
						cfg.getEntityResolver()
				);
				for ( PersistenceMetadata metadata : metadataFiles ) {
					log.trace( metadata.toString() );

					if ( metadata.getProvider() == null || IMPLEMENTATION_NAME.equalsIgnoreCase(
							metadata.getProvider()
					) ) {
						//correct provider
						URL jarURL = JarVisitor.getJarURLFromURLEntry( url, "/META-INF/persistence.xml" );
						JarVisitor.Filter[] persistenceXmlFilter = getFilters( metadata, integration, metadata.getExcludeUnlistedClasses() );
						JarVisitor visitor = JarVisitor.getVisitor( jarURL, persistenceXmlFilter );
						if ( metadata.getName() == null ) {
							metadata.setName( visitor.getUnqualifiedJarName() );
						}
						if ( persistenceUnitName == null && xmls.hasMoreElements() ) {
							throw new PersistenceException( "No name provided and several persistence units found" );
						}
						else if ( persistenceUnitName == null || metadata.getName().equals( persistenceUnitName ) ) {
							addMetadataFromVisitor( visitor, metadata );
							JarVisitor.Filter[] otherXmlFilter = getFilters( metadata, integration, false );
							for ( String jarFile : metadata.getJarFiles() ) {
								visitor = JarVisitor.getVisitor( jarFile, otherXmlFilter );
								addMetadataFromVisitor( visitor, metadata );
							}
							return configure( metadata, integration );
						}
					}
				}
			}
			return null;
		}
		catch (Exception e) {
			if ( e instanceof PersistenceException) {
				throw (PersistenceException) e;
			}
			else {
				throw new PersistenceException( e );
			}
		}
	}

	private static void addMetadataFromVisitor(JarVisitor visitor, PersistenceMetadata metadata) throws IOException {
		Set[] entries = visitor.getMatchingEntries();
		JarVisitor.Filter[] filters = visitor.getFilters();
		int size = filters.length;
		List<String> classes = metadata.getClasses();
		List<String> packages = metadata.getPackages();
		List<InputStream> hbmFiles = metadata.getHbmfiles();
		for ( int index = 0; index < size ; index++ ) {
			Iterator homogeneousEntry = entries[index].iterator();
			while ( homogeneousEntry.hasNext() ) {
				JarVisitor.Entry entry = (JarVisitor.Entry) homogeneousEntry.next();
				if ( filters[index] instanceof JarVisitor.ClassFilter ) {
					classes.add( entry.getName() );
				}
				else if ( filters[index] instanceof JarVisitor.PackageFilter ) {
					packages.add( entry.getName() );
				}
				else if ( filters[index] instanceof JarVisitor.FileFilter ) {
					hbmFiles.add( entry.getInputStream() );
					metadata.getMappingFiles().remove( entry.getName() );
				}
			}
		}
	}

	/**
	 * Process configuration from a PersistenceUnitInfo object
	 * Typically called by the container
	 */
	public Ejb3Configuration configure(PersistenceUnitInfo info, Map integration) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Processing " + LogHelper.logPersistenceUnitInfo( info ) );
		}
		else {
			log.info( "Processing PersistenceUnitInfo [\n\tname: " + info.getPersistenceUnitName() + "\n\t...]" );
		}

		integration = integration != null ? Collections.unmodifiableMap( integration ) : CollectionHelper.EMPTY_MAP;
		String provider = (String) integration.get( HibernatePersistence.PROVIDER );
		if ( provider == null ) provider = info.getPersistenceProviderClassName();
		if ( provider != null && ! provider.trim().startsWith( IMPLEMENTATION_NAME ) ) {
			log.info( "Required a different provider: " + provider );
			return null;
		}
		if ( info.getClassLoader() == null ) {
			throw new IllegalStateException(
					"[PersistenceUnit: " + info.getPersistenceUnitName() == null ? "" : info.getPersistenceUnitName()
							+ "] " + "PersistenceUnitInfo.getClassLoader() id null" );
		}
		//set the classloader
		Thread thread = Thread.currentThread();
		ClassLoader contextClassLoader = thread.getContextClassLoader();
		boolean sameClassLoader = info.getClassLoader().equals( contextClassLoader );
		if ( ! sameClassLoader ) {
			overridenClassLoader = info.getClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		else {
			overridenClassLoader = null;
		}

		try {
			Map workingVars = new HashMap();
			workingVars.put( HibernatePersistence.PERSISTENCE_UNIT_NAME, info.getPersistenceUnitName() );
			List<String> entities = new ArrayList<String>( 50 );
			if ( info.getManagedClassNames() != null ) entities.addAll( info.getManagedClassNames() );
			List<InputStream> hbmFiles = new ArrayList<InputStream>();
			List<String> packages = new ArrayList<String>();
			List<String> xmlFiles = new ArrayList<String>( 50 );
			if ( info.getMappingFileNames() != null ) xmlFiles.addAll( info.getMappingFileNames() );
			if ( ! xmlFiles.contains( META_INF_ORM_XML ) ) xmlFiles.add( META_INF_ORM_XML );
			//		Object overridenTxType = integration.get( HibernatePersistence.TRANSACTION_TYPE );
			//		if (overridenTxType != null) {
			//			defineTransactionType( overridenTxType, info.getPersistenceUnitName() );
			//		}
			//		else {
			//		}

			boolean[] detectArtifactForOtherJars = getDetectedArtifacts( info.getProperties(), null, false );
			boolean[] detectArtifactForMainJar = getDetectedArtifacts( info.getProperties(), null, info.excludeUnlistedClasses() );
			for ( URL jar : info.getJarFileUrls() ) {
				if ( detectArtifactForOtherJars[0] ) scanForClasses( jar, packages, entities );
				if ( detectArtifactForOtherJars[1] ) scanForHbmXmlFiles( jar, hbmFiles );
			}
			if ( detectArtifactForMainJar[0] ) scanForClasses( info.getPersistenceUnitRootUrl(), packages, entities );
			if ( detectArtifactForMainJar[1] ) scanForHbmXmlFiles( info.getPersistenceUnitRootUrl(), hbmFiles );

			Properties properties = info.getProperties() != null ?
					info.getProperties() :
					new Properties();
			ConfigurationHelper.overrideProperties( properties, integration );

			//FIXME entities is used to enhance classes and to collect annotated entities this should not be mixed
			//fill up entities with the on found in xml files
			addXMLEntities( xmlFiles, info, entities );

			//FIXME send the appropriate entites.
			if ( "true".equalsIgnoreCase( properties.getProperty( HibernatePersistence.USE_CLASS_ENHANCER ) ) ) {
				info.addTransformer( new InterceptFieldClassFileTransformer( entities ) );
			}

			workingVars.put( HibernatePersistence.CLASS_NAMES, entities );
			workingVars.put( HibernatePersistence.PACKAGE_NAMES, packages );
			workingVars.put( HibernatePersistence.XML_FILE_NAMES, xmlFiles );
			if ( hbmFiles.size() > 0 ) workingVars.put( HibernatePersistence.HBXML_FILES, hbmFiles );

			//datasources
			Boolean isJTA = null;
			boolean overridenDatasource = false;
			if ( integration.containsKey( HibernatePersistence.JTA_DATASOURCE ) ) {
				String dataSource = (String) integration.get( HibernatePersistence.JTA_DATASOURCE );
				overridenDatasource = true;
				properties.setProperty( Environment.DATASOURCE, dataSource );
				isJTA = Boolean.TRUE;
			}
			if ( integration.containsKey( HibernatePersistence.NON_JTA_DATASOURCE ) ) {
				String dataSource = (String) integration.get( HibernatePersistence.NON_JTA_DATASOURCE );
				overridenDatasource = true;
				properties.setProperty( Environment.DATASOURCE, dataSource );
				if (isJTA == null) isJTA = Boolean.FALSE;
			}

			if ( ! overridenDatasource && ( info.getJtaDataSource() != null || info.getNonJtaDataSource() != null ) ) {
				isJTA = info.getJtaDataSource() != null ? Boolean.TRUE : Boolean.FALSE;
				this.setDataSource(
						isJTA ? info.getJtaDataSource() : info.getNonJtaDataSource()
				);
				this.setProperty(
						Environment.CONNECTION_PROVIDER, InjectedDataSourceConnectionProvider.class.getName()
				);
			}
			/*
			 * If explicit type => use it
			 * If a JTA DS is used => JTA transaction,
			 * if a non JTA DS is used => RESOURCe_LOCAL
			 * if none, set to JavaEE default => JTA transaction
			 */
			PersistenceUnitTransactionType transactionType = info.getTransactionType();
			if (transactionType == null) {
				if (isJTA == Boolean.TRUE) {
					transactionType = PersistenceUnitTransactionType.JTA;
				}
				else if ( isJTA == Boolean.FALSE ) {
					transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
				}
				else {
					transactionType = PersistenceUnitTransactionType.JTA;
				}
			}
			defineTransactionType( transactionType, workingVars );
			configure( properties, workingVars );
		}
		finally {
			//After EMF, set the CCL back
			if ( ! sameClassLoader ) {
				thread.setContextClassLoader( contextClassLoader );
			}
		}
		return this;
	}

	private void addXMLEntities(List<String> xmlFiles, PersistenceUnitInfo info, List<String> entities) {
		//TODO handle inputstream related hbm files
		ClassLoader newTempClassLoader = info.getNewTempClassLoader();
		if (newTempClassLoader == null) {
			log.warn( "Persistence provider caller does not implements the EJB3 spec correctly. PersistenceUnitInfo.getNewTempClassLoader() is null." );
			return;
		}
		XMLHelper xmlHelper = new XMLHelper();
		List errors = new ArrayList();
		SAXReader saxReader = xmlHelper.createSAXReader( "XML InputStream", errors, cfg.getEntityResolver() );
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
			//saxReader.setFeature( "http://apache.org/xml/features/validation/dynamic", true );
			//set the default schema locators
			saxReader.setProperty( "http://apache.org/xml/properties/schema/external-schemaLocation",
					"http://java.sun.com/xml/ns/persistence/orm orm_1_0.xsd");
		}
		catch (SAXException e) {
			saxReader.setValidation( false );
		}

		for ( String xmlFile : xmlFiles ) {

			InputStream resourceAsStream = newTempClassLoader.getResourceAsStream( xmlFile );
			if (resourceAsStream == null) continue;
			BufferedInputStream is = new BufferedInputStream( resourceAsStream );
			try {
				errors.clear();
				org.dom4j.Document doc = saxReader.read( is );
				if ( errors.size() != 0 ) {
					throw new MappingException( "invalid mapping", (Throwable) errors.get( 0 ) );
				}
				Element rootElement = doc.getRootElement();
				if ( rootElement != null && "entity-mappings".equals( rootElement.getName() ) ) {
					Element element = rootElement.element( "package" );
					String defaultPackage = element != null ? element.getTextTrim() : null;
					List<Element> elements = rootElement.elements( "entity" );
					for (Element subelement : elements ) {
						String classname = XMLContext.buildSafeClassName( subelement.attributeValue( "class" ), defaultPackage );
						if ( ! entities.contains( classname ) ) {
							entities.add( classname );
						}
					}
					elements = rootElement.elements( "mapped-superclass" );
					for (Element subelement : elements ) {
						String classname = XMLContext.buildSafeClassName( subelement.attributeValue( "class" ), defaultPackage );
						if ( ! entities.contains( classname ) ) {
							entities.add( classname );
						}
					}
					elements = rootElement.elements( "embeddable" );
					for (Element subelement : elements ) {
						String classname = XMLContext.buildSafeClassName( subelement.attributeValue( "class" ), defaultPackage );
						if ( ! entities.contains( classname ) ) {
							entities.add( classname );
						}
					}
				}
				else if ( rootElement != null && "hibernate-mappings".equals( rootElement.getName() ) ) {
					//FIXME include hbm xml entities to enhance them but entities is also used to collect annotated entities
				}
			}
			catch (DocumentException e) {
				throw new MappingException( "Could not parse mapping document in input stream", e );
			}
			finally {
				try {
					is.close();
				}
				catch (IOException ioe) {
					log.warn( "Could not close input stream", ioe );
				}
			}
		}
	}

	private void defineTransactionType(Object overridenTxType, Map workingVars) {
		if ( overridenTxType == null ) {
//			if ( transactionType == null ) {
//				transactionType = PersistenceUnitTransactionType.JTA; //this is the default value
//			}
			//nothing to override
		}
		else if ( overridenTxType instanceof String ) {
			transactionType = PersistenceXmlLoader.getTransactionType( (String) overridenTxType );
		}
		else if ( overridenTxType instanceof PersistenceUnitTransactionType ) {
			transactionType = (PersistenceUnitTransactionType) overridenTxType;
		}
		else {
			throw new PersistenceException( getExceptionHeader( workingVars ) +
					HibernatePersistence.TRANSACTION_TYPE + " of the wrong class type"
							+ ": " + overridenTxType.getClass()
			);
		}

	}

	public Ejb3Configuration setProperty(String key, String value) {
		cfg.setProperty( key, value );
		return this;
	}

	private boolean[] getDetectedArtifacts(Properties properties, Map overridenProperties, boolean excludeIfNotOverriden) {
		boolean[] result = new boolean[2];
		result[0] = false; //detect classes
		result[1] = false; //detect hbm
		String detect = overridenProperties != null ?
				(String) overridenProperties.get( HibernatePersistence.AUTODETECTION ) :
				null;
		detect = detect == null ?
				properties.getProperty( HibernatePersistence.AUTODETECTION) :
				detect;
		if (detect == null && excludeIfNotOverriden) {
			//not overriden so we comply with the spec
			return result;
		}
		else if (detect == null){
			detect = "class,hbm";
		}
		StringTokenizer st = new StringTokenizer( detect, ", ", false );
		while ( st.hasMoreElements() ) {
			String element = (String) st.nextElement();
			if ( "class".equalsIgnoreCase( element ) ) result[0] = true;
			if ( "hbm".equalsIgnoreCase( element ) ) result[1] = true;
		}
		log.debug( "Detect class: " + result[0] + "; detect hbm: " + result[1] );
		return result;
	}

	private JarVisitor.Filter[] getFilters(PersistenceMetadata metadata, Map overridenProperties, boolean excludeIfNotOverriden) {
		Properties properties = metadata.getProps();
		final List<String> mappingFiles = metadata.getMappingFiles();
		boolean[] result = getDetectedArtifacts( properties, overridenProperties, excludeIfNotOverriden );

		int size = ( result[0] ? 2 : 0 ) + 1; //class involves classes and packages, xml files are always involved because of orm.xml
		JarVisitor.Filter[] filters = new JarVisitor.Filter[size];
		if ( result[0] ) {
			filters[0] = new JarVisitor.PackageFilter( false, null ) {
				public boolean accept(String javaElementName) {
					return true;
				}
			};
			filters[1] = new JarVisitor.ClassFilter(
					false, new Class[]{
					Entity.class,
					MappedSuperclass.class,
					Embeddable.class}
			) {
				public boolean accept(String javaElementName) {
					return true;
				}
			};
		}
		if ( result[1] ) {
			filters[size - 1] = new JarVisitor.FileFilter( true ) {
				public boolean accept(String javaElementName) {
					return javaElementName.endsWith( "hbm.xml" )
							|| javaElementName.endsWith( META_INF_ORM_XML )
							|| mappingFiles.contains( javaElementName );
				}
			};
		}
		else {
			filters[size - 1] = new JarVisitor.FileFilter( true ) {
				public boolean accept(String javaElementName) {
					return javaElementName.endsWith( META_INF_ORM_XML )
							|| mappingFiles.contains( javaElementName );
				}
			};
		}
		return filters;
	}

	private void scanForHbmXmlFiles(URL jar, List<InputStream> hbmxmls) {
		Iterator it = ArchiveBrowser.getBrowser(
				jar, new ArchiveBrowser.Filter() {
			public boolean accept(String filename) {
				return filename.endsWith( ".hbm.xml" );
			}
		}
		);

		while ( it.hasNext() ) {
			InputStream stream = (InputStream) it.next();
			hbmxmls.add( stream );
		}
	}

	private void scanForClasses(URL jar, List<String> packages, List<String> entities) {
		Iterator it = null;
		try {
			it = ArchiveBrowser.getBrowser(
					jar, new ArchiveBrowser.Filter() {
				public boolean accept(String filename) {
					return filename.endsWith( ".class" );
				}
			}
			);
		}
		catch (RuntimeException e) {
			throw new RuntimeException( "error trying to scan <jar-file>: " + jar.toString(), e );
		}

		// need to look into every entry in the archive to see if anybody has tags
		// defined.
		while ( it.hasNext() ) {
			InputStream stream = (InputStream) it.next();
			DataInputStream dstream = new DataInputStream( stream );
			ClassFile cf = null;
			try {
				try {
					cf = new ClassFile( dstream );
				}
				finally {
					dstream.close();
					stream.close();
				}
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
			if ( cf.getName().endsWith( ".package-info" ) ) {
				int idx = cf.getName().indexOf( ".package-info" );
				String pkgName = cf.getName().substring( 0, idx );
				log.info( "found package: " + pkgName );
				packages.add( pkgName );
				continue;
			}

			AnnotationsAttribute visible = (AnnotationsAttribute) cf.getAttribute( AnnotationsAttribute.visibleTag );
			if ( visible != null ) {
				boolean isEntity = visible.getAnnotation( Entity.class.getName() ) != null;
				if ( isEntity ) {
					log.info( "found EJB3 Entity bean: " + cf.getName() );
					entities.add( cf.getName() );
				}
				boolean isEmbeddable = visible.getAnnotation( Embeddable.class.getName() ) != null;
				if ( isEmbeddable ) {
					log.info( "found EJB3 @Embeddable: " + cf.getName() );
					entities.add( cf.getName() );
				}
				boolean isEmbeddableSuperclass = visible.getAnnotation( MappedSuperclass.class.getName() ) != null;
				if ( isEmbeddableSuperclass ) {
					log.info( "found EJB3 @MappedSuperclass: " + cf.getName() );
					entities.add( cf.getName() );
				}
			}
		}
	}

	/**
	 * create a factory from a list of properties and
	 * HibernatePersistence.CLASS_NAMES -> Collection<String> (use to list the classes from config files
	 * HibernatePersistence.PACKAGE_NAMES -> Collection<String> (use to list the mappings from config files
	 * HibernatePersistence.HBXML_FILES -> Collection<InputStream> (input streams of hbm files)
	 * HibernatePersistence.LOADED_CLASSES -> Collection<Class> (list of loaded classes)
	 * <p/>
	 * <b>Used by JBoss AS only</b>
	 * @deprecated use the Java Persistence API
	 */
	// This is used directly by JBoss so don't remove until further notice.  bill@jboss.org
	public EntityManagerFactory createEntityManagerFactory(Map workingVars) {
		Properties props = new Properties();
		if ( workingVars != null ) {
			props.putAll( workingVars );
			//remove huge non String elements for a clean props
			props.remove( HibernatePersistence.CLASS_NAMES );
			props.remove( HibernatePersistence.PACKAGE_NAMES );
			props.remove( HibernatePersistence.HBXML_FILES );
			props.remove( HibernatePersistence.LOADED_CLASSES );
		}
		configure( props, workingVars );
		return buildEntityManagerFactory();
	}

	/**
	 * Process configuration and build an EntityManagerFactory <b>when</b> the configuration is ready
	 * @deprecated
	 */
	public EntityManagerFactory createEntityManagerFactory() {
		configure( cfg.getProperties(), new HashMap() );
		return buildEntityManagerFactory();
	}

	public EntityManagerFactory buildEntityManagerFactory() {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			NamingHelper.bind(this);
			return new EntityManagerFactoryImpl(
					cfg.buildSessionFactory(),
					transactionType,
					discardOnClose
			);
		}
		catch (HibernateException e) {
			throw new PersistenceException( e );
		}
		finally {
			if (thread != null) {
				thread.setContextClassLoader( contextClassLoader );
			}
		}
	}

	public Reference getReference() throws NamingException {
		log.debug("Returning a Reference to the Ejb3Configuration");
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] serialized;
		try {
			out = new ObjectOutputStream( stream );
			out.writeObject( this );
			out.close();
			serialized = stream.toByteArray();
			stream.close();
		}
		catch (IOException e) {
			NamingException namingException = new NamingException( "Unable to serialize Ejb3Configuration" );
			namingException.setRootCause( e );
			throw namingException;
		}

		return new Reference(
				Ejb3Configuration.class.getName(),
				new BinaryRefAddr("object", serialized ),
				Ejb3ConfigurationObjectFactory.class.getName(),
				null
		);
	}

	/**
	 * create a factory from a canonical workingVars map and the overriden properties
	 *
	 */
	private Ejb3Configuration configure(
			Properties properties, Map workingVars
	) {
		Properties preparedProperties = prepareProperties( properties, workingVars );
		if ( workingVars == null ) workingVars = CollectionHelper.EMPTY_MAP;

		if ( preparedProperties.containsKey( HibernatePersistence.CFG_FILE ) ) {
			String cfgFileName = preparedProperties.getProperty( HibernatePersistence.CFG_FILE );
			cfg.configure( cfgFileName );
		}

		cfg.addProperties( preparedProperties ); //persistence.xml has priority over hibernate.Cfg.xml

		addClassesToSessionFactory( workingVars );

		//processes specific properties
		List<String> jaccKeys = new ArrayList<String>();


		Configuration defaultConf = new AnnotationConfiguration();
		Interceptor defaultInterceptor = defaultConf.getInterceptor();
		NamingStrategy defaultNamingStrategy = defaultConf.getNamingStrategy();

		Iterator propertyIt = preparedProperties.keySet().iterator();
		while ( propertyIt.hasNext() ) {
			Object uncastObject = propertyIt.next();
			//had to be safe
			if ( uncastObject != null && uncastObject instanceof String ) {
				String propertyKey = (String) uncastObject;
				if ( propertyKey.startsWith( HibernatePersistence.CLASS_CACHE_PREFIX ) ) {
					setCacheStrategy( propertyKey, preparedProperties, true, workingVars );
				}
				else if ( propertyKey.startsWith( HibernatePersistence.COLLECTION_CACHE_PREFIX ) ) {
					setCacheStrategy( propertyKey, preparedProperties, false, workingVars );
				}
				else if ( propertyKey.startsWith( HibernatePersistence.JACC_PREFIX )
						&& ! ( propertyKey.equals( HibernatePersistence.JACC_CONTEXT_ID )
						|| propertyKey.equals( HibernatePersistence.JACC_ENABLED ) ) ) {
					jaccKeys.add( propertyKey );
				}
			}
		}
		if ( preparedProperties.containsKey( HibernatePersistence.INTERCEPTOR )
				&& ( cfg.getInterceptor() == null
				|| cfg.getInterceptor().equals( defaultInterceptor ) ) ) {
			//cfg.setInterceptor has precedence over configuration file
			String interceptorName = preparedProperties.getProperty( HibernatePersistence.INTERCEPTOR );
			try {
				Class interceptor = classForName( interceptorName );
				cfg.setInterceptor( (Interceptor) interceptor.newInstance() );
			}
			catch (ClassNotFoundException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Unable to find interceptor class: " + interceptorName, e
				);
			}
			catch (IllegalAccessException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Unable to access interceptor class: " + interceptorName, e
				);
			}
			catch (InstantiationException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Unable to instanciate interceptor class: " + interceptorName, e
				);
			}
			catch (ClassCastException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Interceptor class does not implement Interceptor interface: " + interceptorName, e
				);
			}
		}
		if ( preparedProperties.containsKey( HibernatePersistence.NAMING_STRATEGY )
				&& ( cfg.getNamingStrategy() == null
				|| cfg.getNamingStrategy().equals( defaultNamingStrategy ) ) ) {
			//cfg.setNamingStrategy has precedence over configuration file
			String namingStrategyName = preparedProperties.getProperty( HibernatePersistence.NAMING_STRATEGY );
			try {
				Class namingStragegy = classForName( namingStrategyName );
				cfg.setNamingStrategy( (NamingStrategy) namingStragegy.newInstance() );
			}
			catch (ClassNotFoundException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Unable to find naming strategy class: " + namingStrategyName, e
				);
			}
			catch (IllegalAccessException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Unable to access naming strategy class: " + namingStrategyName, e
				);
			}
			catch (InstantiationException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Unable to instanciate naming strategy class: " + namingStrategyName, e
				);
			}
			catch (ClassCastException e) {
				throw new PersistenceException(
						getExceptionHeader(workingVars) + "Naming strategyy class does not implement NmaingStrategy interface: " + namingStrategyName,
						e
				);
			}
		}

		if ( jaccKeys.size() > 0 ) {
			addSecurity( jaccKeys, preparedProperties, workingVars );
		}

		//initialize listeners
		listenerConfigurator.setProperties( preparedProperties );
		listenerConfigurator.configure();

		//some spec compliance checking
		//TODO centralize that?
		if ( ! "true".equalsIgnoreCase( cfg.getProperty( Environment.AUTOCOMMIT ) ) ) {
			log.warn( Environment.AUTOCOMMIT + " = false break the EJB3 specification" );
		}
		discardOnClose = preparedProperties.getProperty( HibernatePersistence.DISCARD_PC_ON_CLOSE )
				.equals( "true" );
		return this;
	}

	private void addClassesToSessionFactory(Map workingVars) {
		if ( workingVars.containsKey( HibernatePersistence.CLASS_NAMES ) ) {
			Collection<String> classNames = (Collection<String>) workingVars.get(
					HibernatePersistence.CLASS_NAMES
			);
			addNamedAnnotatedClasses( this, classNames, workingVars );
		}
		if ( workingVars.containsKey( HibernatePersistence.LOADED_CLASSES ) ) {
			Collection<Class> classes = (Collection<Class>) workingVars.get( HibernatePersistence.LOADED_CLASSES );
			for ( Class clazz : classes ) {
				cfg.addAnnotatedClass( clazz );
			}
		}
		if ( workingVars.containsKey( HibernatePersistence.PACKAGE_NAMES ) ) {
			Collection<String> packages = (Collection<String>) workingVars.get(
					HibernatePersistence.PACKAGE_NAMES
			);
			for ( String pkg : packages ) {
				cfg.addPackage( pkg );
			}
		}
		if ( workingVars.containsKey( HibernatePersistence.XML_FILE_NAMES ) ) {
			Collection<String> xmlFiles = (Collection<String>) workingVars.get(
					HibernatePersistence.XML_FILE_NAMES
			);
			for ( String xmlFile : xmlFiles ) {
				Boolean useMetaInf = null;
				try {
					if ( xmlFile.endsWith( META_INF_ORM_XML ) ) useMetaInf = true;
					cfg.addResource( xmlFile );
				}
				catch( MappingNotFoundException e ) {
					if ( ! xmlFile.endsWith( META_INF_ORM_XML ) ) {
						throw new PersistenceException( getExceptionHeader(workingVars)
								+ "Unable to find XML mapping file in classpath: " + xmlFile);
					}
					else {
						useMetaInf = false;
						//swallow it, the META-INF/orm.xml is optional
					}
				}
				catch( MappingException me ) {
					throw new PersistenceException( getExceptionHeader(workingVars)
								+ "Error while reading JPA XML file: " + xmlFile, me);
				}
				if ( log.isInfoEnabled() ) {
					if ( Boolean.TRUE.equals( useMetaInf ) ) {
						log.info( getExceptionHeader( workingVars ) + META_INF_ORM_XML + " found");
					}
					else if (Boolean.FALSE.equals( useMetaInf ) ) {
						log.info( getExceptionHeader( workingVars ) + "no " + META_INF_ORM_XML + " found");
					}
				}
			}
		}
		if ( workingVars.containsKey( HibernatePersistence.HBXML_FILES ) ) {
			Collection<InputStream> hbmXmlFiles = (Collection<InputStream>) workingVars.get(
					HibernatePersistence.HBXML_FILES
			);
			for ( InputStream is : hbmXmlFiles ) {
				//addInputStream has the responsibility to close the stream
				cfg.addInputStream( is );
			}
		}
	}

	private String getExceptionHeader(Map workingVars) {
		if ( workingVars != null ) {
			String puName = (String) workingVars.get( HibernatePersistence.PERSISTENCE_UNIT_NAME);
			puName = puName == null ? "" : puName;
			String header = "[PersistenceUnit: " + puName + "] ";
			return header;
		}
		else {
			return "";
		}
	}

	private Properties prepareProperties(Properties properties, Map workingVars) {
		Properties preparedProperties = new Properties();

		//defaults different to Hibernate
		preparedProperties.setProperty( Environment.RELEASE_CONNECTIONS, "auto" );
		preparedProperties.setProperty( Environment.JPAQL_STRICT_COMPLIANCE, "true" );
		//settings that always apply to a compliant EJB3
		preparedProperties.setProperty( Environment.AUTOCOMMIT, "true" );
		preparedProperties.setProperty( Environment.USE_IDENTIFIER_ROLLBACK, "false" );
		preparedProperties.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "false" );
		preparedProperties.setProperty( HibernatePersistence.DISCARD_PC_ON_CLOSE, "false" );

		//override the new defaults with the user defined ones
		if ( properties != null ) preparedProperties.putAll( properties );
		if (transactionType == null) {
			//if it has not been set, the user use a programmatic way
			transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		defineTransactionType(
				preparedProperties.getProperty( HibernatePersistence.TRANSACTION_TYPE ),
				workingVars
		);
		boolean hasTxStrategy = StringHelper.isNotEmpty(
				preparedProperties.getProperty( Environment.TRANSACTION_STRATEGY )
		);
		if ( ! hasTxStrategy && transactionType == PersistenceUnitTransactionType.JTA ) {
			preparedProperties.setProperty(
					Environment.TRANSACTION_STRATEGY, JoinableCMTTransactionFactory.class.getName()
			);
		}
		else if ( ! hasTxStrategy && transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
			preparedProperties.setProperty( Environment.TRANSACTION_STRATEGY, JDBCTransactionFactory.class.getName() );
		}
		if ( hasTxStrategy ) {
			log.warn(
					"Overriding " + Environment.TRANSACTION_STRATEGY + " is dangerous, this might break the EJB3 specification implementation"
			);
		}
		if ( preparedProperties.getProperty( Environment.FLUSH_BEFORE_COMPLETION ).equals( "true" ) ) {
			preparedProperties.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			log.warn( "Defining " + Environment.FLUSH_BEFORE_COMPLETION + "=true ignored in HEM" );
		}
		return preparedProperties;
	}

	private Class classForName(String className) throws ClassNotFoundException {
		return ReflectHelper.classForName( className, this.getClass() );
	}

	private void setCacheStrategy(String propertyKey, Map properties, boolean isClass, Map workingVars) {
		String role = propertyKey.substring(
				( isClass ? HibernatePersistence.CLASS_CACHE_PREFIX
						.length() : HibernatePersistence.COLLECTION_CACHE_PREFIX.length() )
						+ 1
		);
		//dot size added
		String value = (String) properties.get( propertyKey );
		StringTokenizer params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			StringBuilder error = new StringBuilder( "Illegal usage of " );
			error.append(
					isClass ? HibernatePersistence.CLASS_CACHE_PREFIX : HibernatePersistence.COLLECTION_CACHE_PREFIX
			);
			error.append( ": " ).append( propertyKey ).append( " " ).append( value );
			throw new PersistenceException( getExceptionHeader(workingVars) + error.toString() );
		}
		String usage = params.nextToken();
		String region = null;
		if ( params.hasMoreTokens() ) {
			region = params.nextToken();
		}
		if ( isClass ) {
			boolean lazyProperty = true;
			if ( params.hasMoreTokens() ) {
				lazyProperty = "all".equalsIgnoreCase( params.nextToken() );
			}
			cfg.setCacheConcurrencyStrategy( role, usage, region, lazyProperty );
		}
		else {
			cfg.setCollectionCacheConcurrencyStrategy( role, usage, region );
		}
	}

	private void addSecurity(List<String> keys, Map properties, Map workingVars) {
		log.debug( "Adding security" );
		if ( !properties.containsKey( HibernatePersistence.JACC_CONTEXT_ID ) ) {
			throw new PersistenceException( getExceptionHeader(workingVars) +
					"Entities have been configured for JACC, but "
							+ HibernatePersistence.JACC_CONTEXT_ID
							+ " has not been set"
			);
		}
		String contextId = (String) properties.get( HibernatePersistence.JACC_CONTEXT_ID );
		setProperty( Environment.JACC_CONTEXTID, contextId );

		int roleStart = HibernatePersistence.JACC_PREFIX.length() + 1;

		for ( String key : keys ) {
			JACCConfiguration jaccCfg = new JACCConfiguration( contextId );
			try {
				String role = key.substring( roleStart, key.indexOf( '.', roleStart ) );
				int classStart = roleStart + role.length() + 1;
				String clazz = key.substring( classStart, key.length() );
				String actions = (String) properties.get( key );
				jaccCfg.addPermission( role, clazz, actions );
			}
			catch (IndexOutOfBoundsException e) {
				throw new PersistenceException( getExceptionHeader(workingVars) +
						"Illegal usage of " + HibernatePersistence.JACC_PREFIX + ": " + key );
			}
		}
	}

	private void addNamedAnnotatedClasses(
			Ejb3Configuration cfg, Collection<String> classNames, Map workingVars
	) {
		for ( String name : classNames ) {
			try {
				Class clazz = classForName( name );
				cfg.addAnnotatedClass( clazz );
			}
			catch (ClassNotFoundException cnfe) {
				Package pkg;
				try {
					pkg = classForName( name + ".package-info" ).getPackage();
				}
				catch (ClassNotFoundException e) {
					pkg = null;
				}
				if ( pkg == null ) {
					throw new PersistenceException( getExceptionHeader(workingVars) +  "class or package not found", cnfe );
				}
				else {
					cfg.addPackage( name );
				}
			}
		}
	}


	public Settings buildSettings() throws HibernateException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			return settingsFactory.buildSettings( cfg.getProperties() );
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addProperties(Properties props) {
		cfg.addProperties( props );
		return this;
	}

	public Ejb3Configuration addAnnotatedClass(Class persistentClass) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addAnnotatedClass( persistentClass );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration configure(String resource) throws HibernateException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			Properties properties = new Properties();
			properties.setProperty( HibernatePersistence.CFG_FILE, resource);
			configure( properties, new HashMap() );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addPackage(String packageName) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addPackage( packageName );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addFile(String xmlFile) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addFile( xmlFile );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addClass(Class persistentClass) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addClass( persistentClass );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addFile(File xmlFile) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addFile( xmlFile );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public void buildMappings() {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.buildMappings();
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Iterator getClassMappings() {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			return cfg.getClassMappings();
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public EventListeners getEventListeners() {
		return cfg.getEventListeners();
	}

	SessionFactory buildSessionFactory() throws HibernateException {
		return cfg.buildSessionFactory();
	}

	public Iterator getTableMappings() {
		return cfg.getTableMappings();
	}

	public PersistentClass getClassMapping(String persistentClass) {
		return cfg.getClassMapping( persistentClass );
	}

	public org.hibernate.mapping.Collection getCollectionMapping(String role) {
		return cfg.getCollectionMapping( role );
	}

	public void setEntityResolver(EntityResolver entityResolver) {
		cfg.setEntityResolver( entityResolver );
	}

	public Map getNamedQueries() {
		return cfg.getNamedQueries();
	}

	public Interceptor getInterceptor() {
		return cfg.getInterceptor();
	}

	public Properties getProperties() {
		return cfg.getProperties();
	}

	public Ejb3Configuration setInterceptor(Interceptor interceptor) {
		cfg.setInterceptor( interceptor );
		return this;
	}

	public Ejb3Configuration setProperties(Properties properties) {
		cfg.setProperties( properties );
		return this;
	}

	public Map getFilterDefinitions() {
		return cfg.getFilterDefinitions();
	}

	public void addFilterDefinition(FilterDefinition definition) {
		cfg.addFilterDefinition( definition );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject object) {
		cfg.addAuxiliaryDatabaseObject( object );
	}

	public NamingStrategy getNamingStrategy() {
		return cfg.getNamingStrategy();
	}

	public Ejb3Configuration setNamingStrategy(NamingStrategy namingStrategy) {
		cfg.setNamingStrategy( namingStrategy );
		return this;
	}

	public void setListeners(String type, String[] listenerClasses) {
		cfg.setListeners( type, listenerClasses );
	}

	public void setListeners(String type, Object[] listeners) {
		cfg.setListeners( type, listeners );
	}

	/**
	 * This API is intended to give a read-only configuration.
	 * It is sueful when working with SchemaExport or any Configuration based
	 * tool.
	 * DO NOT update configuration through it.
	 */
	public AnnotationConfiguration getHibernateConfiguration() {
		//TODO make it really read only (maybe through proxying)
		return cfg;
	}

	public Ejb3Configuration addInputStream(InputStream xmlInputStream) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addInputStream( xmlInputStream );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addResource(String path) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addResource( path );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addResource(String path, ClassLoader classLoader) throws MappingException {
		cfg.addResource( path, classLoader );
		return this;
	}
}
