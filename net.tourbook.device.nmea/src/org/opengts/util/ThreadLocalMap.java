// ----------------------------------------------------------------------------
// Copyright 2006-2009, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Provides a per-thread Map instance
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

public class ThreadLocalMap<K,V>
    extends ThreadLocal<Map<K,V>>
    implements Map<K,V>
{

    // ------------------------------------------------------------------------

    private Class<Map<K,V>>         mapClass = null;
    private ThreadLocal<Map<K,V>>   threadLocal = null;

    public ThreadLocalMap()
    {
        this(null);
    }

    public ThreadLocalMap(final Class<Map<K,V>> mapClass) 
    {
        super();
        this.mapClass = mapClass;
        this.threadLocal = new ThreadLocal<Map<K,V>>() {
            protected Map<K,V> initialValue() {
                if (ThreadLocalMap.this.mapClass == null) {
                    return new Hashtable<K,V>();
                } else {
                    try {
                        return ThreadLocalMap.this.mapClass.newInstance(); // throw ClassCastException
                    } catch (Throwable t) {
                        // Give up and try a Hashtable
                        Print.logException("Error instantiating: " + StringTools.className(ThreadLocalMap.this.mapClass), t);
                        return new Hashtable<K,V>();
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------------

    public Class<Map<K,V>> getMapClass() 
    {
        return this.mapClass;
    }

    protected Map<K,V> getMap()
    {
        Map<K,V> map = (Map<K,V>)this.threadLocal.get();
        if (map == null) {
            Print.logError("'<ThreadLocal>.get()' has return null!");
        }
        return map;
    }

    // ------------------------------------------------------------------------
    // Map interface

    public void clear()
    {
        this.getMap().clear();
    }

    public boolean containsKey(Object key)
    {
        return this.getMap().containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return this.getMap().containsValue(value);
    }

    public Set<Map.Entry<K,V>> entrySet()
    {
        return this.getMap().entrySet();
    }

    public boolean equals(Object o)
    {
        if (o instanceof ThreadLocalMap) {
            return this.getMap().equals(((ThreadLocalMap)o).getMap());
        } else {
            return false;
        }
    }

    public V get(Object key)
    {
        return (key != null)? this.getMap().get(key) : null;
    }

    public int hashCode()
    {
        return this.getMap().hashCode();
    }

    public boolean isEmpty()
    {
        return this.getMap().isEmpty();
    }

    public Set<K> keySet()
    {
        return this.getMap().keySet();
    }

    public V put(K key, V value)
    {
        if (key == null) {
            Print.logStackTrace("Null key");
            return null;
        } else
        if (value == null) {
            this.getMap().remove(key);
            return null;
        } else {
            return this.getMap().put(key, value);
        }
    }

    public void putAll(Map<? extends K, ? extends V> t)
    {
        this.getMap().putAll(t);
    }

    public V remove(Object key)
    {
        return this.getMap().remove(key);
    }

    public int size()
    {
        return this.getMap().size();
    }

    public Collection<V> values()
    {
        return this.getMap().values();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
