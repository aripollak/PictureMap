package com.aripollak.picturemap;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MainActivity extends MapActivity {
	
	MapView mMapView;
	List<Overlay> mMapOverlays;
	Drawable mDrawable;
	ImageOverlay mItemizedOverlay;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button button = (Button)findViewById(R.id.choosepic);
        button.setOnClickListener(mGetImageListener);
        
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setBuiltInZoomControls(true);
        
        mMapOverlays = mMapView.getOverlays();
        mDrawable = this.getResources().getDrawable(
        				android.R.drawable.ic_menu_myplaces);
        mItemizedOverlay = new ImageOverlay(mDrawable);
    	mMapOverlays.add(mItemizedOverlay);
    }
    
    /** Clicked on Choose Picture button */
    private final OnClickListener mGetImageListener = new OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);              
            //intent.addCategory(Intent.CATEGORY_OPENABLE);
            // TODO: specify a content uri for the camera bucket by using a ContentProvider?
            // TODO: Just get called from gallery for now? 
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(
            		intent, getString(R.string.select_image)), 0);
    	}
    };

    /** Got back a result from the picture chooser */
    // TODO: load all images with location, and let people search for
    //       stuff by date/picture?
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	if (intent == null)
    		return;
    	
    	Cursor cursor = managedQuery(intent.getData(), null, null, null, null);
    	int titleColumn = cursor.getColumnIndexOrThrow(Images.Media.TITLE);
    	int bucketNameColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_DISPLAY_NAME);
    	int bucketIdColumn = cursor.getColumnIndexOrThrow(Images.Media.BUCKET_ID);
    	int longitudeColumn = cursor.getColumnIndexOrThrow(Images.Media.LONGITUDE);
    	int latitudeColumn = cursor.getColumnIndexOrThrow(Images.Media.LATITUDE);
    	int dataColumn = cursor.getColumnIndexOrThrow(Images.Media.DATA);
    	if (cursor.moveToFirst()) {
    		String stuff = cursor.getString(bucketNameColumn);
    		stuff += " " + cursor.getString(bucketIdColumn);
    		stuff += " " + cursor.getDouble(longitudeColumn);
    		stuff += " " + cursor.getDouble(latitudeColumn);
    		stuff += " " + cursor.getString(dataColumn);
    		Log.i(this.getLocalClassName(), stuff);
    	}
    	
    	// TODO: set item's marker as the picture
    	int lat = (int) (cursor.getDouble(latitudeColumn) * 1E6);
    	int lon = (int) (cursor.getDouble(longitudeColumn) * 1E6);
    	GeoPoint point = new GeoPoint(lat, lon);
    	OverlayItem item = new OverlayItem(point, cursor.getString(titleColumn), "");
    	mItemizedOverlay.addOverlay(item);
    	mMapView.getController().animateTo(point);
    }
    
    @Override
    protected boolean isRouteDisplayed() { return false; }
    
    
	private class ImageOverlay extends ItemizedOverlay<OverlayItem> {

		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		
		public ImageOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
			populate();
		}
		
		public void addOverlay(OverlayItem item) {
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

	}
   
}