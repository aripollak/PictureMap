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
package com.drewChanged.lang.test;

import junit.framework.TestCase;

/**
 * Created by dnoakes on 07-May-2005 11:13:18 using IntelliJ IDEA.
 */
public class TestHelper
{
    public static void assertEqualArrays(byte[] array1, byte[] array2)
    {
        TestCase.assertEquals("Equal array length", array1.length, array2.length);

        for (int i = 0; i<array1.length; i++)
            TestCase.assertEquals("Equal value at index " + i, array1[i], array2[i]);
    }
}
