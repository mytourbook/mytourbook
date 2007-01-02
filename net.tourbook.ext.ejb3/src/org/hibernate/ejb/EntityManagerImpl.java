//$Id: EntityManagerImpl.java 10282 2006-08-18 15:17:00Z epbernard $
package org.hibernate.ejb;

import java.util.Map;
import javax.persistence.PersistenceContextType;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public class EntityManagerImpl extends AbstractEntityManagerImpl {

	private static Log log = LogFactory.getLog( EntityManagerImpl.class );
	protected Session session;
	protected SessionFactory sessionFactory;
	protected boolean open;
	protected boolean discardOnClose;

	public EntityManagerImpl(
			SessionFactory sessionFactory, PersistenceContextType pcType,
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose, Map properties
	) {
		super( pcType, transactionType, properties );
		this.sessionFactory = sessionFactory;
		this.open = true;
		this.discardOnClose = discardOnClose;
		postInit();
	}

	public Session getSession() {

		if ( !open ) throw new IllegalStateException( "EntityManager is closed" );
		return getRawSession();
	}

	protected Session getRawSession() {
		if ( session == null ) {
			session = sessionFactory.openSession();
			if ( persistenceContextType == PersistenceContextType.TRANSACTION ) {
				( (SessionImplementor) session ).setAutoClear( true );
			}
		}
		return session;
	}

	public void close() {

		if ( !open ) throw new IllegalStateException( "EntityManager is closed" );
		if ( !discardOnClose && isTransactionInProgress() ) {
			//delay the closing till the end of the enlisted transaction
			getSession().getTransaction().registerSynchronization(
					new Synchronization() {
						public void beforeCompletion() {
							//nothing to do
						}

						public void afterCompletion(int i) {
							if ( session != null ) {
								if ( session.isOpen() ) {
									log.debug( "Closing entity manager after transaction completion" );
									session.close();
								}
								else {
									log.warn( "Entity Manager closed by someone else ("
											+ Environment.AUTO_CLOSE_SESSION
											+ " must not be used)");
								}
							}
							//TODO session == null should not happen
						}
					}
			);
		}
		else {
			//close right now
			if ( session != null ) session.close();
		}
		open = false;
	}

	public boolean isOpen() {
		//adjustFlushMode(); //don't adjust, can't be done on closed EM
		try {
			if ( open ) getSession().isOpen(); //to force enlistment in tx
			return open;
		}
		catch (HibernateException he) {
			throwPersistenceException( he );
			return false;
		}
	}

}
