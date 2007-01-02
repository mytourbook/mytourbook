//$Id: HibernateQuery.java 9796 2006-04-26 06:46:52Z epbernard $
package org.hibernate.ejb;

import javax.persistence.Query;

public interface HibernateQuery extends Query {
	public org.hibernate.Query getHibernateQuery();
}
