/* Copyright (c) 2009 Ari Pollak <aripollak@gmail.com>

   This file is part of Picture Map.

   Picture Map is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Picture Map is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Picture Map.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aripollak.picturemap;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.drewChanged.imaging.jpeg.JpegMetadataReader;
import com.drewChanged.imaging.jpeg.JpegProcessingException;
import com.drewChanged.lang.Rational;
import com.drewChanged.metadata.Directory;
import com.drewChanged.metadata.Metadata;
import com.drewChanged.metadata.MetadataException;
import com.drewChanged.metadata.exif.ExifDirectory;
import com.drewChanged.metadata.exif.GpsDirectory;
import com.drewChanged.metadata.jpeg.JpegDirectory;
import com.google.android.maps.GeoPoint;

public class ImageUtilities {
	static final String TAG = "ImageUtilities"; 
	
	/**
	 * @param imageLocation
	 * @return null if reading failed, the populated Metadata object otherwise
	 */
	public static Metadata readMetadata(String imageLocation) {
		if (!(imageLocation.toLowerCase().endsWith(".jpg") || 
				imageLocation.toLowerCase().endsWith(".jpeg"))) {
			return null;
		}
		File file = new File(imageLocation);
		try {
			return JpegMetadataReader.readMetadata(file);
		} catch (JpegProcessingException e) {
			//e.printStackTrace();
			return null;
		}
	}
	/** Try to read the specified image and get a Point from the 
	 *  location info inside.
	 *  @param Metadata populated image metadata
	 *  @return null if we couldn't get a proper location
	 */
	public static GeoPoint getGPSInfo(Metadata metadata) {		
		int latE6 = 0;
		int lonE6 = 0;
		
		try {
			Directory gpsDirectory = metadata.getDirectory(GpsDirectory.class);
			if (!gpsDirectory.containsTag(GpsDirectory.TAG_GPS_LATITUDE))
				return null;
			
			Rational[] temp;
			char reference;
			int degrees;
			float minutes;
			float seconds;
			
			// Read latitude reference (N or S)
			reference = (char) gpsDirectory.getInt(GpsDirectory.TAG_GPS_LATITUDE_REF);
			if (reference != 'N' && reference != 'S') {
				return null;
			}
			// Read latitude
			temp = gpsDirectory.getRationalArray(GpsDirectory.TAG_GPS_LATITUDE);
			degrees = temp[0].intValue();
			minutes = temp[1].floatValue();
			seconds = temp[2].floatValue();
			latE6 = degrees * (int)1E6;
			latE6 += (((minutes * 60.0) + seconds) / 3600f) * 1E6;
			latE6 *= (reference == 'N') ? 1 : -1;
			
			// Read longitude reference (W or E)
			reference = (char) gpsDirectory.getInt(GpsDirectory.TAG_GPS_LONGITUDE_REF);
			if (reference != 'E' && reference != 'W') {
				return null;
			}
			// Read latitude
			temp = gpsDirectory.getRationalArray(GpsDirectory.TAG_GPS_LONGITUDE);
			degrees = temp[0].intValue();
			minutes = temp[1].floatValue();
			seconds = temp[2].floatValue();
			lonE6 = degrees * (int)1E6;
			lonE6 += (((minutes * 60.0) + seconds) / 3600f) * 1E6;
			lonE6 *= (reference == 'E') ? 1 : -1;
		} catch (MetadataException e) {
			//e.printStackTrace();
			return null;
		}
		return new GeoPoint(latE6, lonE6);
	}
	
	/** Try to get a thumbnail from the specified image.
	 * @param metadata populated image metadata
	 * @param imageLocation full path to image file
	 * @param maxDimension maximum width or height, in pixels */ 
	public static Bitmap getThumb(Metadata metadata, String imageLocation,
				int maxDimension) {
		try {
			int width, height;
			Bitmap decoded = null;
			ExifDirectory exifDirectory = (ExifDirectory)
					metadata.getDirectory(ExifDirectory.class);
			byte[] thumbnailData = exifDirectory.getThumbnailData();
			if (thumbnailData != null && 
						exifDirectory.containsTag(ExifDirectory.TAG_THUMBNAIL_IMAGE_WIDTH) &&
						exifDirectory.containsTag(ExifDirectory.TAG_THUMBNAIL_IMAGE_HEIGHT)) {
				width = exifDirectory.getInt(ExifDirectory.TAG_THUMBNAIL_IMAGE_WIDTH);
				height = exifDirectory.getInt(ExifDirectory.TAG_THUMBNAIL_IMAGE_HEIGHT);
			} else {
				Directory jpegDirectory = metadata.getDirectory(JpegDirectory.class);
				if (!jpegDirectory.containsTag(JpegDirectory.TAG_JPEG_IMAGE_WIDTH) || 
						!jpegDirectory.containsTag(JpegDirectory.TAG_JPEG_IMAGE_HEIGHT))
					return null;
				
				width = jpegDirectory.getInt(JpegDirectory.TAG_JPEG_IMAGE_WIDTH);
				height = jpegDirectory.getInt(JpegDirectory.TAG_JPEG_IMAGE_HEIGHT);
			}
			// Make sure we keep the aspect ratio, with a maximum edge of maxDimension
			float factor = Math.max(width / (float)maxDimension, height / (float)maxDimension);
			int scaledWidth = Math.max(1, (int)(width / factor));
			int scaledHeight = Math.max(1, (int)(height / factor));
			// First subsample the image without loading  into memory,
			// then scale it to exactly the size we want
			BitmapFactory.Options opts = new BitmapFactory.Options();
    		opts.inSampleSize = (int) factor;
			if (thumbnailData != null) {
				decoded = BitmapFactory.decodeByteArray(
							thumbnailData, 0, thumbnailData.length, opts);
			}
			if (decoded != null) {
				// last resort, decode entire image if no proper thumbnail
				decoded = BitmapFactory.decodeFile(imageLocation, opts);
				Log.d(TAG, "Did not use exif thumbnail for" + imageLocation);
			}
    		return Bitmap.createScaledBitmap(
    				decoded, scaledWidth, scaledHeight, true);
		} catch (MetadataException e) {
			//e.printStackTrace();
			return null;
		} catch (NullPointerException e) {
			Log.d(TAG, "Couldn't create thumbnail for " + imageLocation);
			return null;
		}
	}
}
