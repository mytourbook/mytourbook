/*******************************************************************************
 * Copyright (C) 2005, 2020  Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *  ********************************************************************************
 *
 * @author JOSM - contributors - adapted for MyTourbook by Meinhard Ritscher
 *
 ********************************************************************************
 *
 This class implements a default ProxySelector
 see http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html
 *******************************************************************************/
package net.tourbook.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;

public class DefaultProxySelector extends ProxySelector {

   private static final String SYS_PROP__JAVA_NET_USE_SYSTEM_PROXIES = "java.net.useSystemProxies"; //$NON-NLS-1$

   private static final String USE_SYSTEM_PROXIES                    = System.getProperty(SYS_PROP__JAVA_NET_USE_SYSTEM_PROXIES);

   static {

      if (USE_SYSTEM_PROXIES != null && USE_SYSTEM_PROXIES.equals(Boolean.TRUE.toString())) {

         Util.logSystemProperty_Value(DefaultProxySelector.class,
               SYS_PROP__JAVA_NET_USE_SYSTEM_PROXIES,
               USE_SYSTEM_PROXIES,
               "System proxy is used"); //$NON-NLS-1$
      }
   }

   /**
    * The {@see ProxySelector} provided by the JDK will retrieve proxy information
    * from the system settings, if the system property <tt>java.net.useSystemProxies</tt>
    * is defined <strong>at startup</strong>. It has no effect if the property is set
    * later by the application.
    * We therefore read the property at class loading time and remember it's value.
    */
   private static boolean JVM_WILL_USE_SYSTEM_PROXIES = false;

   {
      final String v = USE_SYSTEM_PROXIES;
      if (v != null && v.equals(Boolean.TRUE.toString())) {
         JVM_WILL_USE_SYSTEM_PROXIES = true;
      }
   }
   private ProxyPolicy       proxyPolicy;

   private InetSocketAddress httpProxySocketAddress;

   private InetSocketAddress socksProxySocketAddress;
   private ProxySelector     delegate;

   public enum ProxyPolicy {
      NO_PROXY("no-proxy"), //$NON-NLS-1$
      USE_SYSTEM_SETTINGS("use-system-settings"), //$NON-NLS-1$
      USE_HTTP_PROXY("use-http-proxy"), //$NON-NLS-1$
      USE_SOCKS_PROXY("use-socks-proxy"); //$NON-NLS-1$

      private String policyName;

      ProxyPolicy(final String policyName) {
         this.policyName = policyName;
      }

      static public ProxyPolicy fromName(String policyName) {
         if (policyName == null) {
            return null;
         }
         policyName = policyName.trim().toLowerCase();
         for (final ProxyPolicy pp : values()) {
            if (pp.getName().equals(policyName)) {
               return pp;
            }
         }
         return null;
      }

      public String getName() {
         return policyName;
      }
   }

   /**
    * A typical example is:
    *
    * <pre>
    * PropertySelector delegate = PropertySelector.getDefault();
    * PropertySelector.setDefault(new DefaultPropertySelector(delegate));
    * </pre>
    *
    * @param delegate
    *           the proxy selector to delegate to if system settings are used. Usually
    *           this is the proxy selector found by ProxySelector.getDefault() before this proxy
    *           selector is installed
    */
   public DefaultProxySelector(final ProxySelector delegate) {
      this.delegate = delegate;
      initFromPreferences();
   }

   protected static int parseProxyPortValue(final String value) {
      if (value == null) {
         return 0;
      }
      int port = 0;
      try {
         port = Integer.parseInt(value);
      } catch (final NumberFormatException e) {
         System.err.println("Unexpected format for port number in in preference. Got " + value + UI.SYMBOL_DOT); //$NON-NLS-1$
         System.err.println("The proxy will not be used."); //$NON-NLS-1$
         return 0;
      }
      if (port <= 0 || port > 65535) {
         System.err.println("Illegal port number in preference. Got " + value + UI.SYMBOL_DOT); //$NON-NLS-1$
         System.err.println("The proxy will not be used."); //$NON-NLS-1$
         return 0;
      }
      return port;
   }

   /**
    * The {@see ProxySelector} provided by the JDK will retrieve proxy information
    * from the system settings, if the system property <tt>java.net.useSystemProxies</tt>
    * is defined <strong>at startup</strong>. If the property is set later by the application,
    * this has no effect.
    *
    * @return true, if <tt>java.net.useSystemProxies</tt> was set to true at class initialization
    *         time
    */
   public static boolean willJvmRetrieveSystemProxies() {
      return JVM_WILL_USE_SYSTEM_PROXIES;
   }

   @Override
   public void connectFailed(final URI uri, final SocketAddress sa, final IOException ioe) {
      // Just log something. The network stack will also throw an exception which will be caught
      // somewhere else
      //
      System.out.println("Error: Connection to proxy " + sa.toString() + " for URI " + uri.toString() + " failed. Exception was: " + ioe.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }

   private void initFromPreferences() {
      final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

      // String value = "USE-HTTP-PROXY";
      proxyPolicy = ProxyPolicy.fromName(prefStore.getString(IPreferences.PROXY_METHOD));

      int port = parseProxyPortValue(prefStore.getString(IPreferences.PROXY_SERVER_PORT));
      String host = prefStore.getString(IPreferences.PROXY_SERVER_ADDRESS);
      if (!StringUtils.isNullOrEmpty(host) && port > 0) {
         httpProxySocketAddress = new InetSocketAddress(host, port);
      } else {
         httpProxySocketAddress = null;
         if (proxyPolicy.equals(ProxyPolicy.USE_HTTP_PROXY)) {
            System.err.println("Warning: Unexpected parameters for HTTP proxy. Got host " + host + " and port " + port + UI.SYMBOL_DOT); //$NON-NLS-1$ //$NON-NLS-2$
            System.err.println("The proxy will not be used."); //$NON-NLS-1$
         }
      }

      port = parseProxyPortValue(prefStore.getString(IPreferences.SOCKS_PROXY_SERVER_PORT));
      host = prefStore.getString(IPreferences.SOCKS_PROXY_SERVER_ADDRESS);

      if (!StringUtils.isNullOrEmpty(host) && port > 0) {
         socksProxySocketAddress = new InetSocketAddress(host, port);
      } else {
         socksProxySocketAddress = null;
         if (proxyPolicy.equals(ProxyPolicy.USE_SOCKS_PROXY)) {
            System.err.println("Warning: Unexpected parameters for SOCKS proxy. Got host " + host + " and port " + port + UI.SYMBOL_DOT); //$NON-NLS-1$ //$NON-NLS-2$
            System.err.println("The proxy will not be used."); //$NON-NLS-1$
         }
      }
   }

   @Override
   public List<Proxy> select(final URI uri) {
      Proxy proxy;
      switch (proxyPolicy) {
      case USE_SYSTEM_SETTINGS:
         if (!JVM_WILL_USE_SYSTEM_PROXIES) {
            System.err.println(
                  "Warning: the JVM is not configured to lookup proxies from the system settings. The property ''java.net.useSystemProxies'' was missing at startup time.  Will not use a proxy."); //$NON-NLS-1$
            return Collections.singletonList(Proxy.NO_PROXY);
         }
         // delegate to the former proxy selector
         final List<Proxy> ret = delegate.select(uri);
         return ret;
      case NO_PROXY:
         return Collections.singletonList(Proxy.NO_PROXY);
      case USE_HTTP_PROXY:
         if (httpProxySocketAddress == null) {
            return Collections.singletonList(Proxy.NO_PROXY);
         }
         proxy = new Proxy(Type.HTTP, httpProxySocketAddress);
         return Collections.singletonList(proxy);
      case USE_SOCKS_PROXY:
         if (socksProxySocketAddress == null) {
            return Collections.singletonList(Proxy.NO_PROXY);
         }
         proxy = new Proxy(Type.SOCKS, socksProxySocketAddress);
         return Collections.singletonList(proxy);
      }
      // should not happen
      return null;
   }
}
