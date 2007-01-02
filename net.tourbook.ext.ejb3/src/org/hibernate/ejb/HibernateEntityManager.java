//$Id: HibernateEntityManager.java 6948 2005-05-30 16:38:23Z epbernard $
package org.hibernate.ejb;

import javax.persistence.EntityManager;

import org.hibernate.Session;

/**
 * @author Gavin King
 */
public interface HibernateEntityManager extends EntityManager {
	public Session getSession();
}
