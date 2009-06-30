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
package com.drewChanged.imaging.jpeg;

//import java.io.*;
//import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Holds a collection of Jpeg data segments.  This need not necessarily be all segments
 * within the Jpeg.  For example, it may be convenient to port about only the non-image
 * segments when analysing (or serializing) metadata.
 */
@SuppressWarnings("unchecked")
public class JpegSegmentData 
{
    static final long serialVersionUID = 7110175216435025451L;
    
    /** A map of byte[], keyed by the segment marker */
	private final Hashtable _segmentDataMap;

    public JpegSegmentData()
    {
        _segmentDataMap = new Hashtable(10);
    }

    public void addSegment(byte segmentMarker, byte[] segmentBytes)
    {
        Vector segmentList = getOrCreateSegmentList(segmentMarker);
        segmentList.addElement(segmentBytes);
    }

    public byte[] getSegment(byte segmentMarker)
    {
        return getSegment(segmentMarker, 0);
    }

    public byte[] getSegment(byte segmentMarker, int occurrence)
    {
        final Vector segmentList = getSegmentList(segmentMarker);
//        if (segmentList!=null) System.out.println("segmentList size: "+segmentList.size());
//        else System.out.println("segmentList size is null");
        if (segmentList==null || segmentList.size()<=occurrence)
            return null;
        else
            return (byte[]) segmentList.elementAt(occurrence);
    }

    public int getSegmentCount(byte segmentMarker)
    {
        final Vector segmentList = getSegmentList(segmentMarker);
        if (segmentList==null)
            return 0;
        else
            return segmentList.size();
    }

    public void removeSegmentOccurrence(byte segmentMarker, int occurrence)
    {
        final Vector segmentList = (Vector)_segmentDataMap.get(new Byte(segmentMarker));
        segmentList.removeElementAt(occurrence);
    }

    public void removeSegment(byte segmentMarker)
    {
        _segmentDataMap.remove(new Byte(segmentMarker));
    }

    private Vector getSegmentList(byte segmentMarker)
    {
        return (Vector)_segmentDataMap.get(new Byte(segmentMarker));
    }

    private Vector getOrCreateSegmentList(byte segmentMarker)
    {
        Vector segmentList;
        Byte key = new Byte(segmentMarker);
        if (_segmentDataMap.containsKey(key)) {
            segmentList = (Vector)_segmentDataMap.get(key);
        } else {
            segmentList = new Vector();
            _segmentDataMap.put(key, segmentList);
        }
        return segmentList;
    }

    public boolean containsSegment(byte segmentMarker)
    {
        return _segmentDataMap.containsKey(new Byte(segmentMarker));
    }
//
//    public static void ToFile(File file, JpegSegmentData segmentData) throws IOException
//    {
//        ObjectOutputStream outputStream = null;
//        try
//        {
//            outputStream = new ObjectOutputStream(new FileOutputStream(file));
//            outputStream.writeObject(segmentData);
//        }
//        finally
//        {
//            if (outputStream!=null)
//                outputStream.close();
//        }
//    }

//    public static JpegSegmentData FromFile(File file) throws IOException, ClassNotFoundException
//    {
//        ObjectInputStream inputStream = null;
//        try
//        {
//            inputStream = new ObjectInputStream(new FileInputStream(file));
//            return (JpegSegmentData)inputStream.readObject();
//        }
//        finally
//        {
//            if (inputStream!=null)
//                inputStream.close();
//        }
//    }
}
