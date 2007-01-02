//$Id: HibernateEntityManagerFactory.java 8194 2005-09-18 23:26:49Z epbernard $
package org.hibernate.ejb;

import java.io.Serializable;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

/**
 * @author Gavin King
 */
public interface HibernateEntityManagerFactory extends EntityManagerFactory, Serializable {
	public SessionFactory getSessionFactory();
}
