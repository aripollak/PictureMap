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
 * Created by dnoakes on 22-Nov-2002 16:45:19 using IntelliJ IDEA.
 */
package com.drewChanged.metadata;

/**
 *
 */
public class DefaultTagDescriptor extends TagDescriptor
{
    public DefaultTagDescriptor(Directory directory)
    {
        super(directory);
    }

    public String getTagName(int tagType)
    {
        String hex = Integer.toHexString(tagType).toUpperCase();
        while (hex.length() < 4) hex = "0" + hex;
        return "Unknown tag 0x" + hex;
    }

    public String getDescription(int tagType)
    {
        return _directory.getString(tagType);
    }
}
