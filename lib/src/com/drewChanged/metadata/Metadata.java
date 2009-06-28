/*
 * Metadata.java
 *
 * This class is public domain software - that is, you can do whatever you want
 * with it, and include it software that is licensed under the GNU or the
 * BSD license, or whatever other licence you choose, including proprietary
 * closed source licenses.  Similarly, I release this Java version under the
 * same license, though I do ask that you leave this header in tact.
 *
 * If you make modifications to this code that you think would benefit the
 * wider community, please send me a copy and I'll post it on my site.
 *
 * If you make use of this code, I'd appreciate hearing about it.
 *   drew.noakes@drewnoakes.com
 * Latest version of this software kept at
 *   http://drewnoakes.com/
 *
 * Created on 28 April 2002, 17:40
 * Modified 04 Aug 2002
 * - Adjusted javadoc
 * - Added
 * Modified 29 Oct 2002 (v1.2)
 * - Stored IFD directories in separate tag-spaces
 * - iterator() now returns an Iterator over a list of TagValue objects
 * - More get*Description() methods to detail GPS tags, among others
 * - Put spaces between words of tag name for presentation reasons (they had no
 *   significance in compound form)
 */
package com.drewChanged.metadata;

import java.util.Vector;
import java.util.Hashtable;
//import java.util.Iterator;

/**
 * Result from an exif extraction operation, containing all tags, their
 * values and support for retrieving them.
 * @author  Drew Noakes http://drewnoakes.com
 */
public final class Metadata 
{
    /**
     *
     */
    private final Hashtable directoryMap;

    /**
     * List of Directory objects set against this object.  Keeping a list handy makes
     * creation of an Iterator and counting tags simple.
     */
    private final Vector directoryList;

    /**
     * Creates a new instance of Metadata.  Package private.
     */
    public Metadata()
    {
        directoryMap = new Hashtable();
        directoryList = new Vector();
    }


// OTHER METHODS

    /**
     * Creates an Iterator over the tag types set against this image, preserving the order
     * in which they were set.  Should the same tag have been set more than once, it's first
     * position is maintained, even though the final value is used.
     * @return an Iterator of tag types set for this image
     */
//    public Iterator getDirectoryIterator()
//    {
//        return directoryList.iterator();
//    }
    public Vector getDirectoryIterator()
    {
        return directoryList;
    }
    
    
    /**
     * Returns a count of unique directories in this metadata collection.
     * @return the number of unique directory types set for this metadata collection
     */
    public int getDirectoryCount()
    {
        return directoryList.size();
    }

    /**
     * Returns a <code>Directory</code> of specified type.  If this <code>Metadata</code> object already contains
     * such a directory, it is returned.  Otherwise a new instance of this directory will be created and stored within
     * this Metadata object.
     * @param type the type of the Directory implementation required.
     * @return a directory of the specified type.
     */
    public Directory getDirectory(Class type)
    {
        if (!Directory.class.isAssignableFrom(type)) {
            throw new RuntimeException("Class type passed to getDirectory must be an implementation of com.drewChanged.metadata.Directory");
        }
        // check if we've already issued this type of directory
        if (directoryMap.containsKey(type)) {
        	System.out.println("obtain dir from directoryMap");
            return (Directory)directoryMap.get(type);
        }
        Object directory;
        try {
            directory = type.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate provided Directory type: " + type.toString());
        }
        // store the directory in case it's requested later
        System.out.println("put new dir into directoryMap!");
        directoryMap.put(type, directory);
        directoryList.addElement(directory);
        return (Directory)directory;
    }

    /**
     * Indicates whether a given directory type has been created in this metadata
     * repository.  Directories are created by calling getDirectory(Class).
     * @param type the Directory type
     * @return true if the metadata directory has been created
     */
    public boolean containsDirectory(Class type)
    {
        return directoryMap.containsKey(type);
    }
}
