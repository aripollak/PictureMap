package com.aripollak.picturemap;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MainActivity extends MapActivity {

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button button = (Button)findViewById(R.id.choosepic);
        button.setOnClickListener(mGetImageListener);
        
        MapView map = (MapView) findViewById(R.id.map);
        map.setBuiltInZoomControls(true);
    }
    
    private final OnClickListener mGetImageListener = new OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);              
            //intent.addCategory(Intent.CATEGORY_OPENABLE);
            // TODO: specify a content uri for the camera bucket by using a ContentProvider?
            // TODO: Just get called from gallery for now? 
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), 0);
    	}
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	Log.d(this.getLocalClassName(), "onActivityResult Received " + intent);
    	if (intent == null)
    		return;
    	Cursor cursor = managedQuery(intent.getData(), null, null, null, null);
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
    }
    
    @Override
    protected boolean isRouteDisplayed() { return false; }
}