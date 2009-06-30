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
 *
 * Created by dnoakes on Oct 10, 2003 using IntelliJ IDEA.
 */
package com.drewChanged.metadata.jpeg;

import com.drewChanged.metadata.Directory;

import java.util.Hashtable;

/**
 *
 * @author Drew Noakes http://drewnoakes.com
 */
@SuppressWarnings("unchecked")
public class JpegCommentDirectory extends Directory {

	/** This is in bits/sample, usually 8 (12 and 16 not supported by most software). */
	public static final int TAG_JPEG_COMMENT = 0;

	protected static final Hashtable tagNameMap = new Hashtable();

	static {
        tagNameMap.put(new Integer(TAG_JPEG_COMMENT), "Jpeg Comment");
	}

    public JpegCommentDirectory() {
		this.setDescriptor(new JpegCommentDescriptor(this));
	}

	public String getName() {
		return "JpegComment";
	}

	protected Hashtable getTagNameMap() {
		return tagNameMap;
	}
}
