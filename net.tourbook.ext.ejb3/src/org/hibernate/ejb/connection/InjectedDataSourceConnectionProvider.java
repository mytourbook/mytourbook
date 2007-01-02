//$Id: InjectedDataSourceConnectionProvider.java 10552 2006-10-04 02:45:35Z epbernard $
package org.hibernate.ejb.connection;

import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.DatasourceConnectionProvider;

/**
 * A connection provider that uses an injected <tt>DataSource</tt>.
 * Setters has to be called before configure()
 *
 * @author Emmanuel Bernard
 * @see org.hibernate.connection.ConnectionProvider
 */
public class InjectedDataSourceConnectionProvider extends DatasourceConnectionProvider {
	//TODO make datasource connection provider properties protected in 3.3
	private String user;
	private String pass;

	private static final Log log = LogFactory.getLog( InjectedDataSourceConnectionProvider.class );

	public void setDataSource(DataSource ds) {
		super.setDataSource( ds );
	}

	public void configure(Properties props) throws HibernateException {
		user = props.getProperty( Environment.USER );
		pass = props.getProperty( Environment.PASS );

		if ( getDataSource() == null ) throw new HibernateException( "No datasource provided" );
		log.info( "Using provided datasource" );
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (user != null || pass != null) {
			return getDataSource().getConnection(user, pass);
		}
		else {
			return getDataSource().getConnection();
		}
	}
}
