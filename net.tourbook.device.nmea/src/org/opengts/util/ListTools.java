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
//  This class provides many List/Collection/Array based utilities.
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/03/28  Martin D. Flynn
//     -Added additional 'toMap' methods
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
//  2008/12/01  Martin D. FLynn
//     -Fixed typo in 'StringComparator' constructor that caused the 'ignoreCase'
//      attribute to be improperly initialized.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.reflect.*;
import java.util.*;

public class ListTools
{

    // ------------------------------------------------------------------------

    /**
    *** Validates and returns an offset that fits within the specified length
    *** @param ofs     The offset to validate.
    *** @param length  The length used to constrain the offset (assumed to be valid)
    *** @return The constrained offset value 
    **/
    private static int _constrainOffset(int ofs, int length)
    {
        if ((ofs < 0) || (length <= 0)) {
            return 0;
        } else
        if (ofs >= length) {
            return length - 1;
        } else {
            return ofs;
        }
    }

    /**
    *** Validates and returns an offset that fits within the specified length
    *** @param ofs     The offset within 'length' (assumed to be valid)
    *** @param len     The length to validate/constrain
    *** @param length  The length used to constrain the specified 'len' (assumed to be valid)
    *** @return The constrained length value 
    **/
    private static int _constrainLength(int ofs, int len, int length)
    {
        if (len < 0) {
            return length;
        } else
        if (len > (length - ofs)) {
            return length - ofs;
        } else {
            return len;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified Map is null/empty
    *** @param M  The Map instance
    *** @return True if the specified Map is null/empty
    **/
    public static boolean isEmpty(Map<?,?> M)
    {
        return ((M == null) || M.isEmpty());
    }

    /**
    *** Returns true if the specified Collection is null/empty
    *** @param C  The Collection instance
    *** @return True if the specified Collection is null/empty
    **/
    public static boolean isEmpty(Collection<?> C)
    {
        return ((C == null) || C.isEmpty());
    }

    /**
    *** Returns true if the specified Object array is null/empty
    *** @param A  The array instance
    *** @return True if the specified array is null/empty
    **/
    public static <T> boolean isEmpty(T A[])
    {
        return ((A == null) || (A.length == 0));
    }

    /**
    *** Returns true if the specified byte array is null/empty
    *** @param B  The byte array instance
    *** @return True if the specified byte array is null/empty
    **/
    public static boolean isEmpty(byte B[])
    {
        return ((B == null) || (B.length == 0));
    }

    /**
    *** Returns true if the specified int array is null/empty
    *** @param I  The int array instance
    *** @return True if the specified int array is null/empty
    **/
    public static boolean isEmpty(int I[])
    {
        return ((I == null) || (I.length == 0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the specified array elements to a List
    *** @param a  The object array to copy to a List instance
    *** @return The List instance containing the elements of the specified array
    **/
    public static <T> java.util.List<T> toList(T a[])
    {
        return ListTools.toList(a, null);
    }

    /**
    *** Copies the specified array elements to a List
    *** @param a    The object array to copy to a List instance
    *** @param list The List instance to which the elements of the Object array will be copied
    *** @return The List instance containing the elements of the specified array
    **/
    public static <T> java.util.List<T> toList(T a[], java.util.List<T> list)
    {
        return ListTools.toList(a, 0, -1, list);
    }

    /**
    *** Copies the specified array elements to a List
    *** @param a    The object array to copy to a List instance
    *** @param ofs  The offset within the specified Object array to begin copying to List
    *** @param len  The number of elements from the specified Object array to copy to List
    *** @param list The List instance to which the elements of the object array will be copied
    *** @return The List instance containing the elements of the specified array
    **/
    public static <T> java.util.List<T> toList(T a[], int ofs, int len, java.util.List<T> list)
    {
        java.util.List<T> v = (list != null)? list : new Vector<T>();
        int alen = (a != null)? a.length : 0;
        ofs = _constrainOffset(ofs, alen);
        len = _constrainLength(ofs, len, alen);
        for (int i = ofs; i < len; i++) { v.add(a[i]); }
        return v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the Enumeration to a List
    *** @param e  The Enumeration to copy to a List instance
    *** @return The List instance containing the elements of the Enumeration
    **/
    public static <T> java.util.List<T> toList(Enumeration<T> e)
    {
        return ListTools.toList(e, null);
    }

    /**
    *** Copies the Enumeration to a List
    *** @param e  The Enumeration to copy to a List instance
    *** @param list The List instance to which the elements from the Enumeration will be copied
    *** @return The List instance containing the elements of the Enumeration
    **/
    public static <T> java.util.List<T> toList(Enumeration<T> e, java.util.List<T> list)
    {
        java.util.List<T> v = (list != null)? list : new Vector<T>();
        if (e != null) { for (;e.hasMoreElements();) { v.add(e.nextElement()); } }
        return v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the Iterator to a List
    *** @param i  The Iterator to copy to a List instance
    *** @return The List instance containing the elements of the Iterator
    **/
    public static <T> java.util.List<T> toList(Iterator<T> i)
    {
        return ListTools.toList(i, null);
    }

    /**
    *** Copies the Iterator to a List
    *** @param i  The Iterator to copy to a List instance
    *** @param list The List instance to which the elements from the Iterator will be copied
    *** @return The List instance containing the elements of the Iterator
    **/
    public static <T> java.util.List<T> toList(Iterator<T> i, java.util.List<T> list)
    {
        java.util.List<T> v = (list != null)? list : new Vector<T>();
        if (i != null) { for (;i.hasNext();) { v.add(i.next()); } }
        return v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the Set to a List
    *** @param s  The Set to copy to a List instance
    *** @return The List instance containing the elements of the Set
    **/
    public static <T> java.util.List<T> toList(Set<T> s)
    {
        return ListTools.toList(s, null);
    }

    /**
    *** Copies the Set to a List
    *** @param s  The Set to copy to a List instance
    *** @param list The List instance to which the elements from the Set will be copied
    *** @return The List instance containing the elements of the Set
    **/
    public static <T> java.util.List<T> toList(Set<T> s, java.util.List<T> list)
    {
        return ListTools.toList(((s != null)? s.iterator() : null), list);
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the StringTokenizer to a List
    *** @param st  The StringTokenizer to copy to a List instance
    *** @return The List instance containing the elements of the StringTokenizer
    **/
    public static java.util.List<String> toList(StringTokenizer st)
    {
        return ListTools.toList(st, (java.util.List<String>)null);
    }

    /**
    *** Copies the StringTokenizer to a List
    *** @param st   The StringTokenizer to copy to a List instance
    *** @param list The List instance to which the elements from the StringTokenizer will be copied
    *** @return The List instance containing the elements of the StringTokenizer
    **/
    public static java.util.List<String> toList(StringTokenizer st, java.util.List<String> list)
    {
        java.util.List<String> v = (list != null)? list : new Vector<String>();
        if (st != null) { for (;st.hasMoreTokens();) { v.add(st.nextToken()); } }
        return v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the contents of the specified List to a new List
    *** @param ls  The List to copy to a new List instance
    *** @return The List instance containing the elements of the specified List
    **/
    public static <T> java.util.List<T> toList(java.util.List<T> ls)
    {
        return ListTools.toList(ls, null);
    }

    /**
    *** Copies the specified list to a new List
    *** @param ls   The List to copy to a new List instance
    *** @param list The List instance to which the elements from the specified List will be copied
    *** @return The List instance containing the elements of the specified List
    **/
    public static <T> java.util.List<T> toList(java.util.List<T> ls, java.util.List<T> list)
    {
        java.util.List<T> v = (list != null)? list : new Vector<T>();
        if (ls != null) { v.addAll(ls); }
        return v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if all elements of the specified list are subclasses of the specified Class
    *** @param list  The List to check
    *** @param type  The Class object used to check against all elements in the List
    *** @return True if all elements in the List are subclasses of the specified Class
    **/
    public static <T> boolean isClassType(java.util.List<?> list, Class<T> type)
    {
        if ((type == null) || (type == Object.class)) {
            return true;
        } else
        if (list != null) {
            for (Iterator<?> i = list.iterator(); i.hasNext();) {
                Object obj = i.next();
                if ((obj != null) && !type.isAssignableFrom(obj.getClass())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the specified Enumeration to a new array of the specified Class type
    *** @param e    The Enumeration to copy to the new array
    *** @param type The Class type of the return array
    *** @return An array containing the elements from the Enumeration
    **/
    public static <T> T[] toArray(Enumeration<?> e, Class<T> type)
    {
        return ListTools.toArray(ListTools.toList(e), type);
    }

    /**
    *** Copies the specified Collection to a new array
    *** @param list The Collection to copy to the new array
    *** @return An array containing the elements from the Collection
    **/
    public static Object[] toArray(Collection<?> list)
    {
        return ListTools.toArray(list, null);
    }

    /**
    *** Copies the specified Collection to a new array of the specified Class type
    *** @param list The Collection to copy to the new array
    *** @param type The Class type of the return array
    *** @return An array containing the elements from the Collection
    **/
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection<?> list, Class<T> type)
    {
        if (type == null) { type = (Class<T>)Object.class; }
        if (list != null) {
            T array[] = (T[])Array.newInstance(type, list.size());  // "unchecked cast"
            return list.toArray(array);
        } else {
            return (T[])Array.newInstance(type, 0);  // "unchecked cast"
        }
    }

    /**
    *** Creates a new array containing the elements in the specified array
    *** @param arry  The array containing elements to be copied to a new array
    *** @param ofs   The offset within 'arry' to begin copying
    *** @param len   The number of elements to copy to the new array
    *** @return  The new array
    **/
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(T arry[], int ofs, int len)
    {
        if (arry != null) {
            int alen = arry.length;
            ofs = _constrainOffset(ofs, alen);
            len = _constrainLength(ofs, len, alen);
            Class type = arry.getClass().getComponentType();
            T newArry[] = (T[])Array.newInstance(type, len);  // "unchecked cast"
            System.arraycopy(arry, ofs, newArry, 0, len);
            return newArry;
        } else {
            return arry;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String array comprised of 'toString()' calls to the specified Object array
    *** @param a  The Object array
    *** @return The String array
    **/
    public static String[] toStringArray(Object a[])
    {
        if (a != null) {
            String v[] = new String[a.length];
            for (int i = 0; i < a.length; i++) { v[i] = a[i].toString(); }
            return v;
        } else {
            return new String[0];
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies the specified Object array to a new Set
    *** @param a  The array to copy to a new Set
    *** @return The Set (HashSet) containing the elements from specified array
    **/
    public static <T> Set<T> toSet(T a[])
    {
        return ListTools.toSet(a, null);
    }

    /**
    *** Copies the specified Object array to a new Set
    *** @param a    The array to copy to the Set
    *** @param set  The Set to which the array elements will be copied
    *** @return The Set containing the elements from specified array
    **/
    public static <T> Set<T> toSet(T a[], Set<T> set)
    {
        return ListTools.toSet(a, 0, -1, set);
    }

    /**
    *** Copies the specified Object array to a new Set
    *** @param a    The array to copy to the Set
    *** @param ofs  The offset within 'a' to begin copying
    *** @param len  The number of elements to copy to the new array
    *** @param set  The Set to which the array elements will be copied
    *** @return The Set containing the elements from specified array
    **/
    public static <T> Set<T> toSet(T a[], int ofs, int len, Set<T> set)
    {
        Set<T> v = (set != null)? set : new HashSet<T>();
        int alen = (a != null)? a.length : 0;
        ofs = _constrainOffset(ofs, alen);
        len = _constrainLength(ofs, len, alen);
        for (int i = ofs; i < len; i++) { v.add(a[i]); }
        return v;
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a new Map instance containing the elements from the specified array
    *** @param arry  The array from which the Map instance will be created.  For each row major
    ***              element in the array, the first column will be the key, and the second column
    ***              will be the value.
    *** @return The Map (OrderedMap) created from the specified array
    **/
    public static Map<Object,Object> toMap(Object arry[][])
    {
        return ListTools.toMap(arry, (Map<Object,Object>)null);
    }

    /**
    *** Creates a new Map instance containing the elements from the specified array
    *** @param arry  The array from which the Map instance will be created.  For each row major
    ***              element in the array, the first column will be the key, and the second column
    ***              will be the value.
    *** @param map   The map instance to which key/value elements from the specified array will be
    ***              copied.  If null, a new OrderedMap will be created.
    *** @return The Map created from the specified array
    **/
    public static Map<Object,Object> toMap(Object arry[][], Map<Object,Object> map)
    {
        Map<Object,Object> m = (map != null)? map : new OrderedMap<Object,Object>();
        if (arry != null) {
            for (int i = 0; i < arry.length; i++) {
                if (arry[i].length >= 2) {
                    Object key = arry[i][0], val = arry[i][1];
                    if ((key != null) && (val != null)) {
                        m.put(key, val);
                    }
                }
            }
        }
        return m;
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a Map instance containing the elements from the specified array.
    *** The key for a given element is the value returned from the specified method
    *** on the array element.
    *** @param keyMethod  The method name invoked on the array element to retrieve the
    ***                   element key.
    *** @param arry       The array added to the map.
    *** @return A new OrderedMap instance containing the array elements
    **/
    public static Map<Object,Object> toMap(String keyMethod, Object arry[])
    {
        return ListTools.toMap(keyMethod, arry, (Map<Object,Object>)null);
    }

    /**
    *** Creates a Map instance containing the elements from the specified array.
    *** The key for a given element is the value returned from the specified method
    *** on the array element.
    *** @param keyMethod  The method name invoked on the array element to retrieve the
    ***                   element key.
    *** @param arry       The array added to the map.
    *** @param map        The map to which the array elements will be added.  If null,
    ***                   a new OrderedMap instance will be created.
    *** @return The Map instance containing the array elements
    **/
    public static Map<Object,Object> toMap(String keyMethod, Object arry[], Map<Object,Object> map)
    {
        Map<Object,Object> m = (map != null)? map : new OrderedMap<Object,Object>();
        if ((arry != null) && (keyMethod != null) && !keyMethod.equals("")) {
            for (int i = 0; i < arry.length; i++) {
                Object val = arry[i];
                if (val != null) {
                    try {
                        MethodAction ma = new MethodAction(arry[i], keyMethod);
                        Object key = ma.invoke();
                        if (key != null) {
                            m.put(key, val);
                        }
                    } catch (Throwable th) {
                        Print.logError("Error creating map: " + th);
                    }
                }
            }
        }
        return m;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified Object to the specified List
    *** @param list  The List to which the specified object will be added
    *** @param obj   The Object to add to the specified List
    *** @return The 'list' instance to which the object was added
    **/
    public static <T> java.util.List<T> add(java.util.List<T> list, T obj)
    {
        return ListTools.insert(list, obj, -1);
    }

    /**
    *** Inserts the specified Object to the specified List, at the specified location
    *** @param list  The List to which the specified object will be inserted
    *** @param obj   The Object to insert into the specified List
    *** @param ndx   The location in the List instance where the object will be inserted
    *** @return The 'list' instance to which the object was inserted
    **/
    public static <T> java.util.List<T> insert(java.util.List<T> list, T obj, int ndx)
    {
        if (list != null) {
            list.add(ndx, obj);
        }
        return list;
    }

    /**
    *** Removes the element at the specified index from the specified List
    *** @param list  The List from which the element at the specified index will be removed
    *** @param ndx   The index of the element to remove from the specified List
    *** @return The 'list' instance from which the element was removed
    **/
    public static <T> java.util.List<T> remove(java.util.List<T> list, int ndx)
    {
        if (list != null) {
            list.remove(ndx);
        }
        return list;
    }

    /**
    *** Creates a new array with the specified object appended to the end of the specified array
    *** @param list  The array to which the object will be appended
    *** @param obj   The object to append to the specified array
    *** @return The new Object array to which the specified object instance was appended
    **/
    public static <T> T[] add(T list[], T obj)
    {
        return ListTools.insert(list, obj, -1);
    }

    /**
    *** Creates a new array with the specified object inserted into specified array
    *** @param list  The array to which the object will be inserted
    *** @param obj   The object to insert into the specified array
    *** @param index The location where the object will be inserted
    *** @return The new Object array to which the specified object instance was inserted
    **/
    @SuppressWarnings("unchecked")
    public static <T> T[] insert(T list[], T obj, int index)
        // throws ArrayStoreException
    {
        if (list != null) {
            int ndx = ((index > list.length) || (index < 0))? list.length : index;
            Class type = list.getClass().getComponentType();
            int size = (list.length > ndx)? (list.length + 1) : (ndx + 1);
            T array[] = (T[])Array.newInstance(type, size);  // "unchecked cast"
            if (ndx > 0) {
                int maxLen = (list.length >= ndx)? ndx : list.length;
                System.arraycopy(list, 0, array, 0, maxLen);
            }
            array[ndx] = obj; // <-- may throw ArrayStoreException
            if (ndx < list.length) {
                int maxLen = list.length - ndx;
                System.arraycopy(list, ndx, array, ndx + 1, maxLen);
            }
            return array;
        } else {
            return null;
        }
    }

    /**
    *** Removes the element at the specified index from the specified array
    *** @param list  The array from which the element will be removed
    *** @param ndx   The index of the element to remove from the specified array
    *** @return The new Object array from which the specified element was removed
    **/
    @SuppressWarnings("unchecked")
    public static <T> T[] remove(T list[], int ndx)
    {
        if ((list != null) && (ndx >= 0) && (ndx < list.length)) {
            Class type = list.getClass().getComponentType();
            T array[] = (T[])Array.newInstance(type, list.length - 1);  // "unchecked cast"
            if (ndx > 0) {
                System.arraycopy(list, 0, array, 0, ndx); 
            }
            if (ndx < (list.length - 1)) {
                System.arraycopy(list, ndx + 1, array, ndx, list.length - ndx - 1);
            }
            return array;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the index of the specified element within the specified List
    *** @param list  The List instance containing the specified element
    *** @param item  The List element for which the index will be returned
    *** @return The index of the specified element within the specified List, or -1 if
    ***         the specified element was not found in the specified List
    **/
    public static <T> int indexOf(java.util.List<T> list, T item)
    {
        if (list == null) {
            return -1;
        } else {
            return list.indexOf(item);
        }
    }

    /**
    *** Returns the index of the specified case-insensitive String value within the specified List.
    *** @param list  The List containing String values
    *** @param item  The String value for which the index will be returned
    *** @return The index of the specified case-insensitive String value within the specified
    ***         List, or -1 if the case-insensitive String value was not found.
    **/
    public static int indexOfIgnoreCase(java.util.List<String> list, String item)
    {
        if (list == null) {
            return -1;
        } else {
            int index = 0;
            for (Iterator i = list.iterator(); i.hasNext(); index++) {
                Object listObj = i.next();
                String listStr = (listObj != null)? listObj.toString() : null;
                if (listStr == item) { // also takes care of 'null == null'
                    return index;
                } else
                if ((listStr != null) && listStr.equalsIgnoreCase(item)) {
                    return index;
                }
            }
            return -1;
        }
    }

    /**
    *** Returns the index of the specified object within the specified array
    *** @param list  The array containing the specified object
    *** @param item  The element for which the index will be returned
    *** @return The index of the specified element within the specified array, or -1 if
    ***         the specified element was not found in the specified array
    **/
    public static <T> int indexOf(T list[], T item)
    {
        return ListTools.indexOf(list, 0, -1, item);
    }

    /**
    *** Returns the index of the specified object within the specified array
    *** @param list  The array containing the specified object
    *** @param ofs   The offset within the array to begin searching
    *** @param len   The number of elements to search within the array
    *** @param item  The element for which the index will be returned
    *** @return The index of the specified element within the specified array, or -1 if
    ***         the specified element was not found in the specified array
    **/
    public static <T> int indexOf(T list[], int ofs, int len, T item)
    {
        if (list == null) {

            /* no list */
            return -1;

        } else {

            /* constrain offset/length */
            int alen = (list != null)? list.length : 0;
            ofs = _constrainOffset(ofs, alen);
            len = _constrainLength(ofs, len, alen);

            /* loop through array checking for item */
            for (int i = ofs; i < len; i++) {
                if (list[i] == item) { // also takes care of 'null == null'
                    return i;
                } else
                if ((list[i] != null) && list[i].equals(item)) {
                    return i;
                }
            }

            /* still not found */
            return -1;

        }
    }

    /**
    *** Returns the index of the specified case-insensitive String value within the specified array.
    *** @param list  The array containing String values
    *** @param item  The String value for which the index will be returned
    *** @return The index of the specified case-insensitive String value within the specified
    ***         array, or -1 if the case-insensitive String value was not found.
    **/
    public static int indexOfIgnoreCase(String list[], String item)
    {
        if (list == null) {
            return -1;
        } else {
            for (int i = 0; i < list.length; i++) {
                if (list[i] == item) { // also takes care of 'null == null'
                    return i;
                } else
                if ((list[i] != null) && list[i].equalsIgnoreCase(item)) {
                    return i;
                }
            }
            return -1;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified map contains the specified key
    *** @param map   The Map instance to search
    *** @param key   The key which is tested for inclusion in the specified Map
    *** @return True if the specified Map contains the specified key
    **/
    public static <K,V> boolean containsKey(java.util.Map<K,V>  map, K key)
    {
        return (map != null) && map.containsKey(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified list contains the specified element
    *** @param list  The List instance to search
    *** @param item  The element which is tested for inclusion in the specified List
    *** @return True if the specified List contains the specified element
    **/
    public static <T> boolean contains(java.util.List<T>  list, T item)
    {
        return (ListTools.indexOf(list, item) >= 0);
    }

    /**
    *** Returns true if the specified list contains the specified case-insensitive String value
    *** @param list  The List instance to search
    *** @param item  The case-insensitive String which is tested for inclusion in the specified List
    *** @return True if the List contains the specified case-insensitive String value
    **/
    public static boolean containsIgnoreCase(java.util.List<String> list, String item)
    {
        return (ListTools.indexOfIgnoreCase(list, item) >= 0);
    }

    /**
    *** Returns true if the specified list contains the specified element
    *** @param list  The array instance to search
    *** @param item  The element which is tested for inclusion in the specified array
    *** @return True if the array contains the specified element
    **/
    public static <T> boolean contains(T list[], T item)
    {
        return (ListTools.indexOf(list, 0, -1, item) >= 0);
    }

    /**
    *** Returns true if the specified list contains the specified element
    *** @param list  The array instance to search
    *** @param ofs   The offset within the array to begin searching
    *** @param len   The number of elements to search
    *** @param item  The element which is tested for inclusion in the specified array
    *** @return True if the array contains the specified element
    **/
    public static <T> boolean contains(T list[], int ofs, int len, T item)
    {
        return (ListTools.indexOf(list, ofs, len, item) >= 0);
    }

    /**
    *** Returns true if the specified array contains the specified case-insensitive String value
    *** @param list  The array instance to search
    *** @param item  The element which is tested for inclusion in the specified array
    *** @return True if the array contains the specified element
    **/
    public static boolean containsIgnoreCase(String list[], String item)
    {
        return (ListTools.indexOfIgnoreCase(list, item) >= 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an Iterator for the specified Iterable
    *** @param i The Iterable
    *** @return An iterator
    **/
    public static <T> Iterator<T> toIterator(Iterable<T> i)
    {
        if (i != null) {
            return i.iterator();
        } else {
            return new Iterator<T>() {
                public boolean hasNext() { return false; }
                public T next() { throw new NoSuchElementException("Null Iterable"); }
                public void remove() { throw new UnsupportedOperationException(); }
            };
        }
    }

    /**
    *** Converts the specified Enumeration into an Iterator
    *** @param e The Enumeration
    *** @return An iterator
    **/
    public static <T> Iterator<T> toIterator(final Enumeration<T> e)
    {
        return new Iterator<T>() {
            public boolean hasNext() { 
                return (e != null)? e.hasMoreElements() : false; 
            }
            public T next() {
                if (e != null) {
                    return e.nextElement(); 
                } else {
                    throw new NoSuchElementException("Null Enumeration"); 
                }
            }
            public void remove() { 
                throw new UnsupportedOperationException(); 
            }
        };
    }

    /**
    *** Returns an iterator over the elements in the specified array
    *** @param list  The array
    *** @return An iterator
    **/
    public static <T> Iterator<T> toIterator(final T list[])
    {
        return new Iterator<T>() {
            private int ndx = 0;
            public boolean hasNext() { 
                return ((list != null) && (this.ndx < list.length)); 
            }
            public T next() {
                if ((list != null) && (this.ndx < list.length)) {
                    return list[this.ndx++];
                } else {
                    throw new NoSuchElementException("end of array"); 
                }
            }
            public void remove() { 
                throw new UnsupportedOperationException(); 
            }
        };
    }

    // ------------------------------------------------------------------------

    /**
    *** Sorts the specified List based on the specified Comparator
    *** @param list  The list to sort (in-place)
    *** @param comp  The Comparator used to sort the specified List
    *** @return The sorted List (in-place)
    **/
    public static <T> java.util.List<T> sort(java.util.List<T> list, Comparator<? super T> comp)
    {
        return ListTools.sort(list, comp, true);
    }

    /**
    *** Sorts the specified List based on the specified Comparator and sort order
    *** @param list  The list to sort (in-place)
    *** @param comp  The Comparator used to sort the specified List
    *** @param forwardOrder True to sort based on the Comparator, false to sort based on the
    ***                     reverse order of the Comparator
    *** @return The sorted List (in-place)
    **/
    public static <T> java.util.List<T> sort(java.util.List<T> list, Comparator<? super T> comp, boolean forwardOrder)
    {
        if (list != null) {
            Comparator<? super T> c = comp;
            if (c == null) { 
                c = new StringComparator<T>(forwardOrder);
            } else
            if (forwardOrder) {
                c = comp;
            } else {
                c = new ReverseOrderComparator<T>(comp);
            }
            Collections.sort(list, c);
        }
        return list;
    }

    /**
    *** Sorts the specified array based on the specified Comparator
    *** @param list  The array to sort (in-place)
    *** @param comp  The Comparator used to sort the specified array
    *** @return The sorted array (in-place)
    **/
    public static <T> T[] sort(T list[], Comparator<? super T> comp)
    {
        return ListTools.sort(list, comp, true);
    }

    /**
    *** Sorts the specified array based on the specified Comparator
    *** @param list  The array to sort (in-place)
    *** @param comp  The Comparator used to sort the specified array
    *** @param forwardOrder True to sort based on the Comparator, false to sort based on the
    ***                     reverse order of the Comparator
    *** @return The sorted array (in-place)
    **/
    public static <T> T[] sort(T list[], Comparator<? super T> comp, boolean forwardOrder)
    {
        if (list != null) {
            Comparator<? super T> c = comp;
            if (c == null) { 
                c = new StringComparator<T>(forwardOrder);
            } else
            if (forwardOrder) {
                c = comp;
            } else {
                c = new ReverseOrderComparator<T>(comp);
            }
            Arrays.sort(list, c);
        }
        return list;
    }

    /**
    *** Sorts the specified String array in ascending order
    *** @param list The array to sort (in-place)
    *** @return The sorted array (in-place)
    **/
    public static String[] sort(String list[])
    {
        return (String[])ListTools.sort(list, new StringComparator<String>(), true);
    }

    /**
    *** Sorts the specified String array
    *** @param list The array to sort (in-place)
    *** @param forwardOrder  True to sort ascending, false descending
    *** @return The sorted array (in-place)
    **/
    public static String[] sort(String list[], boolean forwardOrder)
    {
        return (String[])ListTools.sort(list, new StringComparator<String>(), forwardOrder);
    }

    /**
    *** StringComparator class for sorting objects based on their 'toString()' value
    **/
    public static class StringComparator<T>
        implements Comparator<T>
    {
        private boolean ascending  = true;
        private boolean ignoreCase = false;
        public StringComparator() {
            this(true, false);
        }
        public StringComparator(boolean ascending) {
            this(ascending, false);
        }
        public StringComparator(boolean ascending, boolean ignoreCase) {
            this.ascending  = ascending;
            this.ignoreCase = ignoreCase;
        }
        public int compare(T o1, T o2) {
            String s1 = (o1 != null)? o1.toString() : "";
            String s2 = (o2 != null)? o2.toString() : "";
            if (this.ignoreCase) {
                s1 = s1.toLowerCase();
                s2 = s2.toLowerCase();
            }
            return this.ascending? s1.compareTo(s2) : s2.compareTo(s1);
        }
        public boolean equals(Object other) {
            if (other instanceof StringComparator) {
                StringComparator sc = (StringComparator)other;
                return (this.ascending == sc.ascending) && (this.ignoreCase == sc.ignoreCase);
            }
            return false;
        }
    }

    /**
    *** ReverseOrderComparator class which reserses the sort order of other Comparators
    **/
    public static class ReverseOrderComparator<T>
        implements Comparator<T>
    {
        private Comparator<? super T> otherComp = null;
        public ReverseOrderComparator(Comparator<? super T> comp) {
            if (comp != null) {
                this.otherComp = comp;
            } else {
                this.otherComp = new StringComparator<T>();
            }
        }
        public int compare(T o1, T o2) {
            int compVal = this.otherComp.compare(o1, o2);
            if (compVal > 0) { return -1; }
            if (compVal < 0) { return  1; }
            return 0;
        }
        public boolean equals(Object obj) {
            if (obj instanceof ReverseOrderComparator) {
                ReverseOrderComparator descComp = (ReverseOrderComparator)obj;
                return this.otherComp.equals(descComp.otherComp);
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Reverses the order of the elements in the specified array (in-place)
    *** @param list  The array in which to reverse the order of elements (in-place)
    *** @return The array with the element order reversed (in-place)
    **/
    public static <T> T[] reverseOrder(T list[])
    {
        if ((list != null) && (list.length > 1)) {
            int len = list.length / 2;
            for (int i = 0; i < len; i++) {
                int i2 = (list.length - 1) - i;
                T obj    = list[i];
                list[i]  = list[i2];
                list[i2] = obj;
            }
        }
        return list;
    }

    /**
    *** Reverses the order of the elements in the specified List (in-place)
    *** @param list  The List in which to reverse the order of elements (in-place)
    *** @return The List with the element order reversed (in-place)
    **/
    public static <T> java.util.List<T> reverseOrder(java.util.List<T> list)
    {
        Collections.reverse(list);
        return list;
    }

    // ------------------------------------------------------------------------

    /**
    *** CollectionProxy class
    **/
    public static class CollectionProxy<E>
        implements Collection<E>
    {
        private Collection<E> delegate = null;
        public CollectionProxy(Collection<E> c) {
            this.delegate = c;
        }
        public boolean add(E o) {
            return this.delegate.add(o);
        }
        public boolean addAll(Collection<? extends E> c) {
            return this.delegate.addAll(c);
        }
        public void clear() {
            this.delegate.clear();
        }
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }
        public boolean containsAll(Collection<?> c) {
            return this.delegate.containsAll(c);
        }
        public boolean equals(Object o) {
            if (o instanceof CollectionProxy) {
                return this.delegate.equals(((CollectionProxy)o).delegate);
            } else {
                return false;
            }
        }
        public int hashCode() {
            return this.delegate.hashCode();
        }
        public boolean isEmpty() {
            return this.delegate.isEmpty();
        }
        public Iterator<E> iterator() {
            return this.delegate.iterator();
        }
        public boolean remove(Object o) {
            return this.delegate.remove(o);
        }
        public boolean removeAll(Collection<?> c) {
            return this.delegate.removeAll(c);
        }
        public boolean retainAll(Collection<?> c) {
            return this.delegate.retainAll(c);
        }
        public int size() {
            return this.delegate.size();
        }
        public Object[] toArray() {
            return this.delegate.toArray();
        }
        public <T> T[] toArray(T[] a) {
            return this.delegate.toArray(a);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
