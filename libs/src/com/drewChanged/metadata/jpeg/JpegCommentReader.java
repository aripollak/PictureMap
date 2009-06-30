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

import com.drewChanged.imaging.jpeg.JpegProcessingException;
import com.drewChanged.imaging.jpeg.JpegSegmentReader;
import com.drewChanged.metadata.Metadata;
import com.drewChanged.metadata.MetadataReader;

//import java.io.File;
import java.io.InputStream;

/**
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public class JpegCommentReader implements MetadataReader
{
    /**
     * The COM data segment.
     */
    private final byte[] _data;

    /**
     * Creates a new JpegReader for the specified Jpeg jpegFile.
     */
//    public JpegCommentReader(File jpegFile) throws JpegProcessingException
//    {
//        this(new JpegSegmentReader(jpegFile).readSegment(JpegSegmentReader.SEGMENT_COM));
//    }

    /** Creates a JpegCommentReader for a JPEG stream.
     *
     * @param is JPEG stream. Stream will be closed.
     */
    public JpegCommentReader(InputStream is) throws JpegProcessingException
    {
        this(new JpegSegmentReader(is).readSegment(JpegSegmentReader.SEGMENT_APPD));
    }

    public JpegCommentReader(byte[] data)
    {
        _data = data;
    }

    /**
     * Performs the Jpeg data extraction, returning a new instance of <code>Metadata</code>.
     */
    public Metadata extract()
    {
        return extract(new Metadata());
    }

    /**
     * Performs the Jpeg data extraction, adding found values to the specified
     * instance of <code>Metadata</code>.
     */
    public Metadata extract(Metadata metadata)
    {
        if (_data==null) {
            return metadata;
        }

        JpegCommentDirectory directory = (JpegCommentDirectory)metadata.getDirectory(JpegCommentDirectory.class);

        directory.setString(JpegCommentDirectory.TAG_JPEG_COMMENT, new String(_data));

        return metadata;
    }
}
