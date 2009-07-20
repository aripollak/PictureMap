package com.aripollak.picturemap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.util.Config;
import android.util.Log;
import android.widget.Toast;

import com.drewChanged.imaging.jpeg.JpegMetadataReader;
import com.drewChanged.imaging.jpeg.JpegProcessingException;
import com.drewChanged.lang.Rational;
import com.drewChanged.metadata.Directory;
import com.drewChanged.metadata.Metadata;
import com.drewChanged.metadata.MetadataException;
import com.drewChanged.metadata.exif.GpsDirectory;
import com.drewChanged.metadata.jpeg.JpegDirectory;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;


/** Populate the map overlay with all the images we find */ 
public class PopulateMapTask extends AsyncTask<Uri, OverlayItem, Integer> {
	private static final String TAG = "PopulateMapTask";

	// were we called with a single item?
	private boolean mSingleItem = false;
	private MainActivity mMainActivity;
	
	public PopulateMapTask(MainActivity ma) {
		mMainActivity = ma;
	}
	
	@Override
	protected Integer doInBackground(Uri... uris) {
    	Cursor cursor = null;
    	if (uris[0] != null) {
    		// a single picture has kindly been shared with us
    		mSingleItem = true;
    		cursor = mMainActivity.managedQuery(uris[0], null, null, null, null);
    	} else {
    		// Get the last 200 images from the external image store
    		cursor = mMainActivity.managedQuery(
    					Images.Media.EXTERNAL_CONTENT_URI, null, 
						null, null, 
    					Images.Media.DATE_MODIFIED + " DESC LIMIT 200");
    	}
    	if (cursor == null)
			return 0;
    	
    	int idColumn = cursor.getColumnIndexOrThrow(Images.Media._ID);
    	int titleColumn = cursor.getColumnIndexOrThrow(Images.Media.TITLE);
    	int bucketNameColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_DISPLAY_NAME);
    	int bucketIdColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_ID);
    	int dataColumn = cursor.getColumnIndexOrThrow(Images.Media.DATA);
    	
    	if (!cursor.moveToFirst()) {
    		return 0;
    	}
    	
    	int imagesAdded = 0;
    	do {
       		String imageLocation = cursor.getString(dataColumn);
       		int imageId = cursor.getInt(idColumn);
       		String title = cursor.getString(titleColumn);
    		GeoPoint point = getGPSInfo(imageLocation);    		
    		Bitmap thumb = getThumb(imageLocation);
    		if (point == null || thumb == null) {
    			if (mSingleItem)
    				return 0;
    			else
    				continue;
    		}
    				
			// add the thumbnail as the marker
			OverlayItem item = new OverlayItem(point, title, "" + imageId);
        	item.setMarker(new BitmapDrawable(thumb));
        	publishProgress(item);
        	imagesAdded += 1;
    	} while (cursor.moveToNext() && !isCancelled());
    	
    	return imagesAdded;
	}
	
	
	@Override
	protected void onProgressUpdate(OverlayItem... items) {
		super.onProgressUpdate(items);
    	mMainActivity.mImageOverlay.addOverlay(items[0]);
    	if (mSingleItem)
    		mMainActivity.mMapView.getController().animateTo(items[0].getPoint());
    	mMainActivity.mMapView.invalidate(); // make the map redraw
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mMainActivity.setProgressBarIndeterminateVisibility(true);
		
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		if (result == null || result == 0) {
			if (mSingleItem)
				Toast.makeText(mMainActivity, 
							   R.string.toast_no_geo,
							   Toast.LENGTH_LONG).show();
			else
				Toast.makeText(mMainActivity, 
							   R.string.toast_no_images,
							   Toast.LENGTH_LONG).show();
		}
		mMainActivity.setProgressBarIndeterminateVisibility(false);
	}
	
	/** Try to read the specified image and get a Point from the 
	 *  location info inside.
	 *  @param imageLocation path to image on filesystem
	 *  @return null if we couldn't get a proper location
	 */
	private GeoPoint getGPSInfo(String imageLocation) {
		if (!(imageLocation.toLowerCase().endsWith(".jpg") || 
				imageLocation.toLowerCase().endsWith(".jpeg"))) {
			return null;
		}
		
		int latE6 = 0;
		int lonE6 = 0;
		
		try {
			File file = new File(imageLocation);
			Metadata metadata = JpegMetadataReader.readMetadata(file);
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
		} catch (JpegProcessingException e) {
			//e.printStackTrace();
			return null;
		} catch (MetadataException e) {
			//e.printStackTrace();
			return null;
		}
		return new GeoPoint(latE6, lonE6);
	}
	
	/** Try to get a thumbnail from the specified image. */ 
	private Bitmap getThumb(String imageLocation) {
		try {
			File file = new File(imageLocation);
			Metadata metadata = JpegMetadataReader.readMetadata(file);
			Directory jpegDirectory = metadata.getDirectory(JpegDirectory.class);
			if (!jpegDirectory.containsTag(JpegDirectory.TAG_JPEG_IMAGE_WIDTH) || 
					!jpegDirectory.containsTag(JpegDirectory.TAG_JPEG_IMAGE_HEIGHT))
				return null;
			
			int width = jpegDirectory.getInt(JpegDirectory.TAG_JPEG_IMAGE_WIDTH);
			int height = jpegDirectory.getInt(JpegDirectory.TAG_JPEG_IMAGE_HEIGHT);
			// Make sure we keep the aspect ratio, with a maximum edge of 60 pixels
			float factor = Math.max(width / 60f, height / 60f);
			int scaledWidth = Math.max(1, (int)(width / factor));
			int scaledHeight = Math.max(1, (int)(height / factor));
			System.out.println(factor);
			// First subsample the image without loading  into memory,
			// then scale it to exactly the size we want
			BitmapFactory.Options opts = new BitmapFactory.Options();
    		opts.inSampleSize = (int) factor; 
    		return Bitmap.createScaledBitmap(
    				BitmapFactory.decodeFile(imageLocation, opts),
    				scaledWidth, scaledHeight, true);
		} catch (JpegProcessingException e) {
			//e.printStackTrace();
			return null;
		} catch (MetadataException e) {
			//e.printStackTrace();
			return null;
		}
	}
}


