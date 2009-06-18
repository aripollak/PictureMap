/* Copyright (c) 2009 Ari Pollak <aripollak@gmail.com>
 * 
 * YOU MAY NOT LOOK AT THIS PROGRAM.
 */

package com.aripollak.picturemap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;


public class MainActivity extends MapActivity {
	
	MapView mMapView;
	View mPopup;
	List<Overlay> mMapOverlays;
	Drawable mDrawable;
	ImageOverlay mItemizedOverlay;
	MyLocationOverlay mMyLocationOverlay;
	
	/* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        /*Button button = (Button)findViewById(R.id.choosepic);
        button.setOnClickListener(mGetImageListener); */
        
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setBuiltInZoomControls(true);
        
		// TODO: make the button do something, and make it look like a caption
        mPopup = getLayoutInflater().inflate(R.layout.popup, null);
        //AttributeSet set = Xml.asAttributeSet(getResources().getXml(R.layout.popup));
        //MapView.LayoutParams params = new MapView.LayoutParams(this, set);
		//params.mode = MapView.LayoutParams.MODE_MAP;
		//params.alignment = MapView.LayoutParams.BOTTOM_CENTER;
		MapView.LayoutParams params = new MapView.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				new GeoPoint(0, 0), MapView.LayoutParams.BOTTOM_CENTER);
		mMapView.addView(mPopup, params);

        mMapOverlays = mMapView.getOverlays();
        mDrawable = this.getResources().getDrawable(
        				android.R.drawable.ic_menu_myplaces);
        mItemizedOverlay = new ImageOverlay(mDrawable, mMapView);
        mMyLocationOverlay = new MyLocationOverlay(getApplicationContext(), mMapView);

        populateMap();
        
    	mMapOverlays.add(mItemizedOverlay);
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
    
/*    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    }
*/
    
    
    /** Clicked on Choose Picture button */
    /* private final OnClickListener mGetImageListener = new OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);              
            //intent.addCategory(Intent.CATEGORY_OPENABLE);
            // TODO: specify a content uri for the camera bucket by using a ContentProvider?
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(
            		intent, getString(R.string.select_image)), 0);
    	}
    }; */

    /** Populate the map overlay with all the images we find */ 
    // TODO: let people search for stuff by date/picture
    // TODO: scale images according to current zoom level
    // TODO: Implement an intent to get called from Share in the gallery?
    private void populateMap() {
    	// Get the last 50 images from the external image store
    	Cursor cursor = managedQuery(Images.Media.EXTERNAL_CONTENT_URI, null, 
    								 null, null, Images.Media.DATE_TAKEN + " DESC LIMIT 50");
    	int idColumn = cursor.getColumnIndexOrThrow(Images.Media._ID);
    	int titleColumn = cursor.getColumnIndexOrThrow(Images.Media.TITLE);
    	int bucketNameColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_DISPLAY_NAME);
    	int bucketIdColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_ID);
    	int longitudeColumn = cursor.getColumnIndexOrThrow(Images.Media.LONGITUDE);
    	int latitudeColumn = cursor.getColumnIndexOrThrow(Images.Media.LATITUDE);
    	int dataColumn = cursor.getColumnIndexOrThrow(Images.Media.DATA);
    	
    	if (!cursor.moveToFirst()) {
    		return;
    	}
    	
    	do {
    		if (cursor.isNull(latitudeColumn))
    			continue;
        	int lat = (int) (cursor.getDouble(latitudeColumn) * 1E6);
        	int lon = (int) (cursor.getDouble(longitudeColumn) * 1E6);
        	int imageId = cursor.getInt(idColumn);
    		StringBuilder stuff = new StringBuilder("" + imageId);
    		stuff.append(" ").append(cursor.getString(bucketNameColumn));
    		stuff.append(" ").append(cursor.getString(bucketIdColumn));
    		stuff.append(" ").append(lat);
    		stuff.append(" ").append(lon);
    		stuff.append(" ").append(cursor.getString(dataColumn));
    		Log.d(this.getLocalClassName(), stuff.toString());
    		
    		// Retrieve thumbnail bitmap from thumbnail content provider
    		Cursor thumbCursor = managedQuery(
    				 Images.Thumbnails.EXTERNAL_CONTENT_URI,
    				 null,
    				 Images.Thumbnails.IMAGE_ID + " = " + imageId, 
					 null, null);
			if (!thumbCursor.moveToFirst()) {
				Log.i(this.getLocalClassName(), "No data for thumbnail");
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
				// TODO: keep aspect ratio
				thumb = Bitmap.createScaledBitmap(thumb, 50, 50, true);
			} catch (FileNotFoundException e) {
				Toast.makeText(this,
							   "File not found getting thumbnail",
							   Toast.LENGTH_SHORT);
				e.printStackTrace();
				continue;
			} catch (IOException e) {
				Toast.makeText(this,
						       "I/O Exception getting thumbnail",
						       Toast.LENGTH_SHORT);
				e.printStackTrace();
				continue;
			}
    		
			// add the thumbnail as the marker
        	GeoPoint point = new GeoPoint(lat, lon);
        	OverlayItem item = new OverlayItem(point, cursor.getString(titleColumn), "" + imageId);
        	item.setMarker(new BitmapDrawable(thumb));
        	mItemizedOverlay.addOverlay(item);
        	mMapView.getController().animateTo(point);
    	} while (cursor.moveToNext());
    	
    }
    
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

	    // pop up a balloon when clicking on an image marker
		public void onFocusChanged(ItemizedOverlay overlay, OverlayItem item) {
			if (item == null) {
				mPopup.setVisibility(View.GONE);
				return;
			}

			((MapView.LayoutParams)mPopup.getLayoutParams()).point = item.getPoint();
			mPopup.setVisibility(View.VISIBLE);
		}
	}
   
}
