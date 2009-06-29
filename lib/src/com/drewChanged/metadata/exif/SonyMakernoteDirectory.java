/*
 * This is public domain software - that is, you can do whatever you want
 * with it, and include it software that is licensed under the GNU or the
 * BSD license, or whatever other licence you choose, including proprietary
 * closed source licenses.  I do ask that you leave this header in tact.
 *
 * If you make modifications to this code that you think would benefit the
 * wider community, please send me a copy and I'll post it on my site.
 *
 * If you make use of this code, I'd appreciate hearing about it.
 *   drew@drewnoakes.com
 * Latest version of this software kept at
 *   http://drewnoakes.com/
 */
package com.drewChanged.metadata.exif;

import com.drewChanged.metadata.Directory;

import java.util.Hashtable;

/**
 * Describes tags specific to Sony cameras.
 */
@SuppressWarnings("unchecked")
public class SonyMakernoteDirectory extends Directory
{
	protected static final Hashtable _tagNameMap = new Hashtable();
	
	public String getName()
    {
		return "Sony Makernote";
	}

	protected Hashtable getTagNameMap()
    {
		return _tagNameMap;
	}
}
