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
//  This class provides an ordered HashMap
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Replace method 'keys()' with 'keyIterator()'
//     -Added method 'keyArray(...)'
//     -Added methods 'valueArray(...)' and 'valueIterator()'
//     -Added initial Java 5 'generics'
//  2008/07/27  Martin D. Flynn
//     -Changed to retain original ordering when existing entries are re-added
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** <code>OrderedMap</code> provides a HashMap where values can also be retrieved in
*** the order they were added
**/

public class OrderedMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{

    // ------------------------------------------------------------------------

    private OrderedSet<K>       keyOrder = null;
    private Map<String,String>  ignoredCaseMap = null;

    /**
    *** Constructor
    **/
    public OrderedMap() 
    {
        super();
        this.keyOrder = new OrderedSet<K>(true);
    }
    
    /**
    *** Constructor
    *** @param map  A map from which all contents will be copied
    **/
    public OrderedMap(Map<K,V> map) 
    {
        this();
        this.putAll(map);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if all lookups should be performed with case-insensitive keys 
    *** @return True if all lookups should be performed with case-insensitive keys
    **/
    public boolean isIgnoreCase()
    {
        return (this.ignoredCaseMap != null);
    }

    /** 
    *** Sets the case-insensitive key-lookup mode
    *** @param ignoreCase  True to perform case-insensitive key lookups
    **/
    public void setIgnoreCase(boolean ignoreCase)
    {
        if (ignoreCase) {
            if (this.ignoredCaseMap == null) {
                this.ignoredCaseMap = new HashMap<String,String>();
                for (Iterator<K> i = this.keyOrder.iterator(); i.hasNext();) {
                    K key = (K)i.next();
                    if (key instanceof String) {
                        this.ignoredCaseMap.put(((String)key).toLowerCase(), (String)key);
                    }
                }
            }
        } else {
            if (this.ignoredCaseMap != null) {
                this.ignoredCaseMap = null;
            }
        }
    }

    /**
    *** Maps the specified String key to its map lookup key.  If the specified key is not 
    *** a String, then the key argument is simple returned.
    *** @param key  The key 
    *** @return The mapped lookup key
    **/
    public Object keyCaseFilter(Object key)
    {
        if ((this.ignoredCaseMap != null) && (key instanceof String)) {
            String k = (String)this.ignoredCaseMap.get(((String)key).toLowerCase());
            if (k != null) {
                //if (!k.equals(key)) { Print.logStackTrace("Filtered key: " + key + " ==> " + k); }
                return k;
            }
        }
        return key;
    }

    // ------------------------------------------------------------------------

    /**
    *** Clears the contents of this map
    **/
    public void clear()
    {
        super.clear();
        this.keyOrder.clear();
        if (this.ignoredCaseMap != null) {
            this.ignoredCaseMap.clear(); 
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Return a set of Map.Entry elements
    *** @return A set of Map.Entry elements
    **/
    public Set<Map.Entry<K,V>> entrySet()
    {
        // Attempting to return an ordered set of 'Map.Entry' entries.
        // The effect this will have on calls to this method from HashMap itself
        // isn't fully known.

        /* Map.Entry map */
        Set<Map.Entry<K,V>> es = super.entrySet(); // unordered
        Map<K,Map.Entry<K,V>> meMap = new HashMap<K,Map.Entry<K,V>>();
        for (Iterator<Map.Entry<K,V>> i = es.iterator(); i.hasNext();) {
            Map.Entry<K,V> me = (Map.Entry<K,V>)i.next();
            K key = me.getKey();
            meMap.put(key, me);
        }

        /* place in keyOrder */
        OrderedSet<Map.Entry<K,V>> entSet = new OrderedSet<Map.Entry<K,V>>();
        for (Iterator<K> i = this.keyOrder.iterator(); i.hasNext();) {
            K key = (K)i.next();
            Map.Entry<K,V> me  = (Map.Entry<K,V>)meMap.get(key);
            if (me == null) { Print.logError("Map.Entry is null!!!"); }
            entSet.add(me);
        }
        return entSet;

    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns a shallow copy of an ordered set of keys from this map
    *** (Warning: The returned 'keySet' is not backed by the map, thus any Iterator 'remove()' 
    *** calls performed on the returned set will not remove the item from this map)
    *** @return A shallow copy of an ordered set of keys from this map
    **/
    public Set<K> keySet()
    {
        return new OrderedSet<K>(this.keyOrder);
    }

    /** 
    *** Returns the ordered set of keys from this map<br>
    *** Warning: The returned 'keySet' is not backed by the map, thus any Iterator 'remove()' 
    *** calls performed on the returned set will not remove the item from this map<br>
    *** Warning: The returned value is the actual internal Ordered Set used to maintain ordering of
    *** this ordered map.  Changes to the map will be reflected in this returned OrderedSet.  
    *** The returned OrderedSet must not be modified!  Use with caution!
    *** @return The ordered set of keys from this map
    **/
    public OrderedSet<K> orderedKeySet()
    {
        return this.keyOrder;
    }

    /**
    *** Returns an array of key elements from this map
    *** @return An array of key elements from this map
    **/
    public K[] keyArray(Class<K> arrayType)
    {
        return ListTools.toArray(this.keyOrder, arrayType);
    }

    /**
    *** Returns an Iterator over the keys in this map
    *** @return An Iterator over the keys in this map
    **/
    public Iterator<K> keyIterator()
    {
        return new Iterator<K>() {
            private Iterator<K> i = OrderedMap.this.keyOrder.iterator();
            public boolean hasNext() {
                return i.hasNext();
            }
            public K next() {
                return i.next();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns an ordered set of values from this map
    *** @return An ordered set of values from this map
    **/
    public Set<V> valueSet()
    {
        OrderedSet<V> valSet = new OrderedSet<V>();
        for (Iterator<K> i = this.keyOrder.iterator(); i.hasNext();) {
            valSet.add(super.get(i.next()));
        }
        return valSet;
    }

    /**
    *** Returns an array of value elements from this map
    *** @return An array of value elements from this map
    **/
    public V[] valueArray(Class<V> arrayType)
    {
        return ListTools.toArray(this.valueSet(), arrayType);
    }

    /**
    *** Returns an Iterator over the values in this map
    *** @return An Iterator over the values in this map
    **/
    public Iterator<V> valueIterator()
    {
        return new Iterator<V>() {
            private Iterator<V> i = OrderedMap.this.values().iterator();
            public boolean hasNext() {
                return i.hasNext();
            }
            public V next() {
                return i.next();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a Collection of values in this map
    *** @return A Collection of values in this map
    **/
    public Collection<V> values()
    {
        // All this work is to make sure the returned Collection is still backed by the Map
        return new ListTools.CollectionProxy<V>(super.values()) {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private Iterator<K> i = OrderedMap.this.keySet().iterator();
                    public boolean hasNext() {
                        return i.hasNext();
                    }
                    public V next() {
                        return OrderedMap.this.get(i.next());
                    }
                    public void remove() {
                        throw new UnsupportedOperationException("'remove' not supported here");
                    }
                };
            }
            public Object[] toArray() {
                return ListTools.toList(this.iterator()).toArray();
            }
            public <T> T[] toArray(T[] a) {
                return ListTools.toList(this.iterator()).toArray(a);
            }
        };
    }

    // ------------------------------------------------------------------------

    /**
    *** Puts the specified key/value into the map at the specified index.
    *** @param ndx   The index where the key/value are to be placed
    *** @param key   The map key
    *** @param value The map value
    *** @return The previous value associated with the specified key.
    **/
    public V put(int ndx, K key, V value) 
    {
        if ((this.ignoredCaseMap != null) && (key instanceof String)) {
            this.ignoredCaseMap.put(((String)key).toLowerCase(), (String)key);
        }
        this.keyOrder.add(ndx, key);
        return super.put(key, value);
    }

    /**
    *** Puts the specified key/value into the map.
    *** @param key   The map key
    *** @param value The map value
    *** @return The previous value associated with the specified key.
    **/
    public V put(K key, V value) 
    {
        if ((this.ignoredCaseMap != null) && (key instanceof String)) {
            this.ignoredCaseMap.put(((String)key).toLowerCase(), (String)key);
        }
        this.keyOrder.add(key);
        return super.put(key, value);
    }

    /**
    *** Puts the specified key/value Strings into the map.
    *** @param key   The map key String
    *** @param value The map value String
    *** @return The previous value associated with the specified key.
    **/
    public V setProperty(K key, V value) 
    {
        return this.put(key, value);
    }

    /**
    *** Copies the contents of the specified map into this map.<br>
    *** Note that if the specified is not ordered, then the order in which the elements are
    *** placed into this map is unpredictable.
    *** @param map  The map to copy to this map
    **/
    public void putAll(Map<? extends K, ? extends V> map)
    {
        if (map != null) {
            for (Iterator<? extends K> i = map.keySet().iterator(); i.hasNext();) {
                K key = i.next();
                V val = map.get(key);
                this.put(key, val);
            }
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns true if this map contains the specified case-insensitive key
    *** @param key  The key
    *** @return True if this map contains the specified case-insensitive key
    **/
    public boolean containsKeyIgnoreCase(String key)
    {
        if (key != null) {
            // TODO: Optimize!
            for (Iterator<K> i = this.keyOrder.iterator(); i.hasNext();) {
                Object k = i.next();
                if ((k instanceof String) && key.equalsIgnoreCase((String)k)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
    *** Returns true if this map contains the specified key
    *** @param key  The key
    *** @return True if this map contains the specified key
    **/
    public boolean containsKey(Object key)
    {
        return super.containsKey(this.keyCaseFilter(key));
    }
    
    /**
    *** Returns the index of the specified key in this map
    *** @param key  The key
    *** @return The index of the specified key
    **/
    public int indexOfKey(Object key)
    {
        return this.keyOrder.indexOf(this.keyCaseFilter(key));
    }

    // ------------------------------------------------------------------------

    /**
    *** Removes the specified key from this map.
    *** @param key  The key to remove
    *** @return The previous value associated with this key
    **/
    public V remove(Object key)
    {
        Object k = this.keyCaseFilter(key);
        if ((this.ignoredCaseMap != null) && (key instanceof String)) {
            this.ignoredCaseMap.remove(((String)key).toLowerCase());
        }
        this.keyOrder.remove(k);
        return super.remove(k);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified key
    *** @param key  The key
    *** @return The value for the specified key, or null if the key is not found
    ***         in this map.
    **/
    public V get(Object key)
    {
        return super.get(this.keyCaseFilter(key));
    }

    /**
    *** Gets the value for the specified key
    *** @param key  The key
    *** @param dft  The default value returned if the key does not exist in this map.
    *** @return The value for the specified key.  The default value is returned if the
    ***         specified key is not found in this map.
    **/
    public String getProperty(String key, String dft)
    {
        if (this.containsKey((Object)key)) {
            Object val = this.get(key);
            return (val != null)? val.toString() : null;
        } else {
            return dft;
        }
    }

    /**
    *** Gets the value for the specified key
    *** @param key  The key
    *** @return The value for the specified key.  Null is returned if the
    ***         specified key is not found in this map.
    **/
    public String getProperty(String key)
    {
        return this.getProperty(key, null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the key at the specified index.
    *** @param ndx  The key index
    *** @return The key ar the specified index
    **/
    public K getKey(int ndx)
    {
        return ((ndx >= 0) && (ndx < this.keyOrder.size()))? this.keyOrder.get(ndx) : null;
    }

    /**
    *** Gets the value at the specified index.
    *** @param ndx  The value index
    *** @return The value ar the specified index
    **/
    public V getValue(int ndx)
    {
        Object key = this.getKey(ndx); // returns null if 'ndx' is invalid
        return (key != null)? super.get(key) : null;
    }

    /**
    *** Removes the key/value at the specified index
    *** @param ndx  The index of the key/value to remove
    **/
    public void remove(int ndx)
    {
        this.remove(this.getKey(ndx));
    }

    // ------------------------------------------------------------------------

}
