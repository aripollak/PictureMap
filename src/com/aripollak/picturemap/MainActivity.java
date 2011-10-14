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

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

//TODO: cache thumbnails and locations
// TODO: make about box!
// TODO: add intent to share location with Maps?
// TODO: add forward/back arrows to scroll through images
// TODO: let people search for stuff by date/picture
// TODO: let people re-geotag pictures
public class MainActivity extends MapActivity {
	static final String TAG = "PictureMap";
	MapView mMapView;
	List<Overlay> mMapOverlays;
	ImageOverlay mImageOverlay;
	MyLocationOverlay mMyLocationOverlay;
	PopulateMapTask mPopulateMapTask;
	PictureCallout mPopup;
	BroadcastReceiver mReceiver;
	
	/* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        // so callout progress will show up:
        setTheme(android.R.style.Theme_Light);
        setContentView(R.layout.main);

        /*Button button = (Button)findViewById(R.id.choosepic);
        button.setOnClickListener(mGetImageListener); */
        
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setBuiltInZoomControls(true);
        
        // Can't embed the popup in main.xml since we can't seem to access
        // the MapView.LayoutParams-specific fields from there.
    	mPopup = (PictureCallout)
    				getLayoutInflater().inflate(R.layout.popup, null);
		
		MapView.LayoutParams params = new MapView.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				new GeoPoint(0, 0), MapView.LayoutParams.BOTTOM_CENTER);
        mMapView.addView(mPopup, params);
        
		
        mMapOverlays = mMapView.getOverlays();
        mMyLocationOverlay = new CustomMyLocationOverlay(
        							getApplicationContext(), mMapView);
        
    	// If we just had a configuration change, re-use the old image overlay
    	ImageOverlay oldInstance = (ImageOverlay) getLastNonConfigurationInstance();
        if (oldInstance != null) {
        	mImageOverlay = oldInstance;
        	mImageOverlay.mMapView = mMapView;
        	mImageOverlay.mPopup = mPopup;
        	if (mImageOverlay.getFocus() != null)
        		mImageOverlay.onFocusChanged(mImageOverlay, mImageOverlay.getFocus());
        } else {
        	populateMap();
        }
        
    	mMapOverlays.add(mImageOverlay);
    	mMapOverlays.add(mMyLocationOverlay);
    	
    	addReceiver();
    }

	private void populateMap() {
		Drawable mDrawable = getResources().getDrawable(
								android.R.drawable.ic_menu_myplaces);
		mImageOverlay = new ImageOverlay(mDrawable, mMapView, mPopup);
		Intent intent = getIntent();
		String action = intent.getAction();
		Uri uri = null;
		if (action != null && action.equals(Intent.ACTION_SEND) &&
				intent.hasExtra(Intent.EXTRA_STREAM)) {
		    // Handle Share from the Gallery app
			uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
		}
    	if (mPopulateMapTask != null)
    		mPopulateMapTask.cancel(true);
		mPopulateMapTask = new PopulateMapTask(this);
		mPopulateMapTask.execute(uri);
	}

	/** Repopulate the map if the media scanner has scanned again */
    private void addReceiver() {
    	IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
    	intentFilter.addDataScheme("file");
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(
                        Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
					Log.d(TAG, "Media scanner is finished");
                	mMapOverlays.remove(mImageOverlay);
                	populateMap();
                	mMapOverlays.add(mImageOverlay);

                }
            }
        };
        registerReceiver(mReceiver, intentFilter);
	}
    
    
	@Override
    protected void onResume() {
    	super.onResume();
    	mMyLocationOverlay.enableMyLocation();
    	// TODO: scan for new pictures, don't re-process stuff that's already in the overlay or cached
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mMyLocationOverlay.disableMyLocation();
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	if (mPopulateMapTask == null || 
    			mPopulateMapTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
    		return mImageOverlay;
    	} else {
    		// Don't save instance if we haven't finished populating the map
    		// since the old thread will be in a weird state
    		return null;
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	// Don't keep running in case this activity will get restarted
    	if (mPopulateMapTask != null)
    		mPopulateMapTask.cancel(true);
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

    
    /* If the back button is pressed and the picture callout is visible,
     * hide the callout. 
     */
    @Override
    public void onBackPressed() {
    	if (mImageOverlay.getFocus() != null) {
    		mImageOverlay.setFocus(null);
    	} else {
    		super.onBackPressed();
    	}
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mMapView.isSatellite()) {
        	menu.findItem(R.id.map_view).setVisible(true);
    		menu.findItem(R.id.satellite_view).setVisible(false);
        } else {
        	menu.findItem(R.id.map_view).setVisible(false);
        	menu.findItem(R.id.satellite_view).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	super.onOptionsItemSelected(item);
    	switch (item.getItemId()) {
    	case R.id.my_location:
    		GeoPoint point = mMyLocationOverlay.getMyLocation();
    		if (point == null)
    			Toast.makeText(this, R.string.location_unavailable,
    						   Toast.LENGTH_SHORT).show();
    		else
    			mMapView.getController().animateTo(point);
    		return true;
    	case R.id.satellite_view:
    		// TODO: Save this setting
    		mMapView.setSatellite(true);
    		return true;
    	case R.id.map_view:
    		mMapView.setSatellite(false);
    		return true;
    	}
    	return false;
    }

    
    @Override
    protected boolean isRouteDisplayed() { return false; }
    

    // TODO: implement onSnapToItem
	protected class ImageOverlay extends ItemizedOverlay<OverlayItem> 
	implements com.google.android.maps.ItemizedOverlay.OnFocusChangeListener {

		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		protected MapView mMapView;
		protected PictureCallout mPopup;
		
		public ImageOverlay(Drawable defaultMarker, MapView mapView, PictureCallout popup) {
			super(boundCenterBottom(defaultMarker));
			setDrawFocusedItem(false);
			mMapView = mapView;
			mPopup = popup;
			setOnFocusChangeListener(this);
			populate();
		}
		
		
		public void addOverlay(OverlayItem item) {
			boundCenterBottom(item.getMarker(0));
			mOverlays.add(0, item); // put older items at the beginning
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
			mPopup.onFocusChanged(overlay, item);
			if (item == null)
				return;

			MapController controller = mMapView.getController();
			GeoPoint itemPoint = item.getPoint();
			((MapView.LayoutParams) mPopup.getLayoutParams()).point = itemPoint;
			
			// try to center image on the screen
			int latitudeAdjust = (int) (mMapView.getLatitudeSpan() / 3.5);
			GeoPoint scrollPoint = new GeoPoint(
							itemPoint.getLatitudeE6() + latitudeAdjust,
							itemPoint.getLongitudeE6());
			controller.animateTo(scrollPoint);
		}
		
		@Override
		protected int getIndexToDraw(int drawingOrder) {
			super.getIndexToDraw(drawingOrder);
			return drawingOrder; // show newer items on top (higher rank)
		}
		
	}

	
	class CustomMyLocationOverlay extends MyLocationOverlay {

		public CustomMyLocationOverlay(Context context, MapView mapView) {
			super(context, mapView);
		}
		
		/** Override this so a tap on the My Location point has absolutely 
		 *  no effect, thus allowing images under the point to still be selected. */
		@Override
		public boolean onTap(GeoPoint p, MapView map) {
			return false;
		}
	}

}
