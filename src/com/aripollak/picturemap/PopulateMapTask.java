package com.aripollak.picturemap;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.widget.Toast;

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
    		GeoPoint point = ImageUtilities.getGPSInfo(imageLocation);    		
    		Bitmap thumb = ImageUtilities.getThumb(imageLocation, 60);
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
	
}


