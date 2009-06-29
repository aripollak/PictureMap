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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.drewChanged.imaging.jpeg.JpegMetadataReader;
import com.drewChanged.imaging.jpeg.JpegProcessingException;
import com.drewChanged.lang.Rational;
import com.drewChanged.metadata.Directory;
import com.drewChanged.metadata.Metadata;
import com.drewChanged.metadata.MetadataException;
import com.drewChanged.metadata.exif.GpsDirectory;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;


public class MainActivity extends MapActivity {
	
	MapView mMapView;
	View mPopup;
	List<Overlay> mMapOverlays;
	Drawable mDrawable;
	ImageOverlay mImageOverlay;
	MyLocationOverlay mMyLocationOverlay;
	
	/* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        
        /*Button button = (Button)findViewById(R.id.choosepic);
        button.setOnClickListener(mGetImageListener); */
        
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setBuiltInZoomControls(true);
        
        // Can't embed the popup in main.xml since we can't seem to access
        // the MapView.LayoutParams-specific fields from there.
        mPopup = getLayoutInflater().inflate(R.layout.popup, null); 
		MapView.LayoutParams params = new MapView.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				new GeoPoint(0, 0), MapView.LayoutParams.BOTTOM_CENTER);
        mMapView.addView(mPopup, params);
		
		// TODO: make button look like a caption
		Button openImageButton = (Button) findViewById(R.id.viewButton);
		openImageButton.setOnClickListener(mViewImageListener);
		

        mMapOverlays = mMapView.getOverlays();
        mDrawable = this.getResources().getDrawable(
        				android.R.drawable.ic_menu_myplaces);
        mImageOverlay = new ImageOverlay(mDrawable, mMapView);
        mMyLocationOverlay = new MyLocationOverlay(getApplicationContext(), mMapView);

        new PopulateMapTask().execute();
        
    	mMapOverlays.add(mImageOverlay);
    	mMapOverlays.add(mMyLocationOverlay);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	mMyLocationOverlay.enableMyLocation();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mMyLocationOverlay.disableMyLocation();
    }
    
    // TODO: save currently focused map item
/*    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    }
*/
    

    /** Populate the map overlay with all the images we find */ 
    // TODO: Attach to media scanner to redo map if card is re-inserted
    // TODO: let people search for stuff by date/picture
    // TODO: Implement an intent to get called from Share in the gallery?
    private class PopulateMapTask extends AsyncTask<Uri, OverlayItem, Long> {
    	@Override
		protected Long doInBackground(Uri... uris) {
	    	// Get the last 100 images from the external image store
	    	Cursor cursor = managedQuery(Images.Media.EXTERNAL_CONTENT_URI, null, 
	    								 null, null, Images.Media.DATE_TAKEN + " DESC LIMIT 100");
	    	if (cursor == null)
	    		return null;
	    	int idColumn = cursor.getColumnIndexOrThrow(Images.Media._ID);
	    	int titleColumn = cursor.getColumnIndexOrThrow(Images.Media.TITLE);
	    	int bucketNameColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_DISPLAY_NAME);
	    	int bucketIdColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_ID);
	    	int dataColumn = cursor.getColumnIndexOrThrow(Images.Media.DATA);
	    	
	    	if (!cursor.moveToFirst()) {
	    		return null;
	    	}
	    	
	    	do {
	       		String imageLocation = cursor.getString(dataColumn);
	    		GeoPoint point = getGPSInfo(imageLocation);
	    		if (point == null)
	    			continue;
	    		
	    		// Retrieve thumbnail bitmap from thumbnail content provider
	    		int imageId = cursor.getInt(idColumn);
	    		Cursor thumbCursor = managedQuery(
	    				 Images.Thumbnails.EXTERNAL_CONTENT_URI,
	    				 null,
	    				 Images.Thumbnails.IMAGE_ID + " = " + imageId, 
						 null, null);
				if (!thumbCursor.moveToFirst()) {
					Log.i(this.getClass().toString(), "No data for thumbnail");
					continue;
				}
	    		int thumbIdColumn = thumbCursor.getColumnIndexOrThrow(Images.Thumbnails._ID);
				Bitmap thumb;
				try {
					thumb = Images.Media.getBitmap(
										getContentResolver(), 
										Uri.withAppendedPath(
											Images.Thumbnails.EXTERNAL_CONTENT_URI,
											thumbCursor.getString(thumbIdColumn)));
					if (thumb == null)
						continue;
					// Make sure we keep the aspect ratio, with a maximum edge of 50 pixels
					float factor = Math.max(thumb.getHeight() / 50f, thumb.getHeight() / 50f);
					int scaledWidth = Math.max(1, (int)(thumb.getWidth() / factor));
					int scaledHeight = Math.max(1, (int)(thumb.getHeight() / factor));
					thumb = Bitmap.createScaledBitmap(
										thumb, scaledWidth, 
										scaledHeight, true);
				} catch (FileNotFoundException e) {
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
	    		
				// add the thumbnail as the marker
	        	OverlayItem item = new OverlayItem(point, cursor.getString(titleColumn), "" + imageId);
	        	item.setMarker(new BitmapDrawable(thumb));
	        	publishProgress(item);
	    	} while (cursor.moveToNext());
	    	
	    	return null;
    	}
    	
    	
    	@Override
    	protected void onProgressUpdate(OverlayItem... items) {
    		super.onProgressUpdate(items);
        	mImageOverlay.addOverlay(items[0]);
        	//mMapView.getController().animateTo(point);
        	mMapView.invalidate();
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		setProgressBarIndeterminateVisibility(true);
    		
    	}
    	
    	@Override
    	protected void onPostExecute(Long result) {
    		super.onPostExecute(result);
    		setProgressBarIndeterminateVisibility(false);
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
    			// TODO: fix JpegMetadataReader to accept File
    			File file = new File(imageLocation);
    			FileInputStream is = new FileInputStream(file);
    			long fileLength = file.length();
    			byte[] buf = new byte[(int)fileLength];
    			is.read(buf);
    			ByteArrayInputStream stream = new ByteArrayInputStream(buf);
    			Metadata metadata = JpegMetadataReader.readMetadata(stream);
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
				e.printStackTrace();
				return null;
			} catch (MetadataException e) {
				e.printStackTrace();
				return null;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
    		return new GeoPoint(latE6, lonE6);
    	}
    }
    
    
    /** Clicked on View Picture button */
    private final OnClickListener mViewImageListener = new OnClickListener() {
    	@Override
    	public void onClick(View v) {
    		int index = mImageOverlay.getLastFocusedIndex();
    		if (index == -1) {
    			Log.i(getLocalClassName(),
    					"Couldn't get focused image?");
    			return;
    		}
    		OverlayItem item = mImageOverlay.getItem(index);
    		Uri uri = Uri.withAppendedPath(
						Images.Media.EXTERNAL_CONTENT_URI,
						item.getSnippet());
    		System.out.println(item.getSnippet());
    		Intent intent = new Intent(Intent.ACTION_VIEW);
    		intent.setData(uri);
            //intent.setDataAndType(uri, "image/jpeg");
            startActivity(Intent.createChooser(
            		intent, getString(R.string.select_image)));
    	}
    };

    
    @Override
    protected boolean isRouteDisplayed() { return false; }
    

	private class ImageOverlay extends ItemizedOverlay<OverlayItem> 
	implements com.google.android.maps.ItemizedOverlay.OnFocusChangeListener {

		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		private MapView mMapView;
		
		public ImageOverlay(Drawable defaultMarker, MapView mapView) {
			super(boundCenterBottom(defaultMarker));
			mMapView = mapView;
			setOnFocusChangeListener(this);
			populate();
		}
		
		
		public void addOverlay(OverlayItem item) {
			boundCenterBottom(item.getMarker(0));
			mOverlays.add(item);
		    populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}


		@Override
		public int size() {
			return mOverlays.size();
		}

	    /** pop up a balloon when clicking on an image marker;
	     *  disable it when clicking elsewhere
	     */
		@Override
		public void onFocusChanged(ItemizedOverlay overlay, OverlayItem item) {
			if (item == null) {
				mPopup.setVisibility(View.GONE);
				return;
			}
			
			((MapView.LayoutParams) mPopup.getLayoutParams()).point = item.getPoint();
			mPopup.setVisibility(View.VISIBLE);
		}
	}

   
}
