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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;

import com.drewChanged.metadata.Metadata;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

class PictureCallout extends TableLayout {
	static final String TAG = "PicturePopup";
	Context mContext;
	ImageView mImageView;
	ProgressBar mProgressBar;
	Button mViewButton;
	OverlayItem mLastItem;
	ShowImage mShowImage;
	
	// TODO: change layout when we're in landscape mode
	public PictureCallout(Context context, AttributeSet attrs) {		
		super(context, attrs);
		mContext = context;
		
		mImageView = new ImageView(context);
		mImageView.setVisibility(GONE);
		addView(mImageView, 150, 150);
		
		mProgressBar = new ProgressBar(context);
		mProgressBar.setIndeterminate(true);
		addView(mProgressBar, 150, 150);
		
		mViewButton = new Button(context);
		mViewButton.setText("View image");
		mViewButton.setOnClickListener(mViewImageListener);
		addView(mViewButton);
	}
	
    
    public void onFocusChanged(ItemizedOverlay overlay, OverlayItem item) {
		// This seems to be required all the time, even when
		// an image is already focused and we're just switching
    	setVisibility(GONE);
    	mLastItem = item;
    	if (mShowImage != null) {
    		mShowImage.cancel(true);
    		mShowImage = null;
    	}
		if (item == null)
			return;
		
		mShowImage = new ShowImage();
		mShowImage.execute(item.getTitle());
		setVisibility(VISIBLE);
    }
    
	/* Re-scales image for use in the popup bubble */
	class ShowImage extends AsyncTask<String, Integer, Bitmap>{
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressBar.setVisibility(VISIBLE);
			mImageView.setVisibility(GONE);
		}
		
		@Override
		protected Bitmap doInBackground(String... locations) {
			Metadata metadata = ImageUtilities.readMetadata(locations[0]);
			return ImageUtilities.getThumb(metadata, locations[0], 150);
		}
		
		@Override
		protected void onPostExecute(Bitmap bm) {
			super.onPostExecute(bm);
			mImageView.setImageBitmap(bm);
			mProgressBar.setVisibility(GONE);
			mImageView.setVisibility(VISIBLE);
		}
	}
	
    /** Clicked on View Picture button */
    private final OnClickListener mViewImageListener = new OnClickListener() {
    	@Override
    	public void onClick(View v) {
    		Uri uri = Uri.withAppendedPath(
						Images.Media.EXTERNAL_CONTENT_URI,
						mLastItem.getSnippet());
    		Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/jpeg");
            mContext.startActivity(Intent.createChooser(
            		intent, mContext.getResources().getString(R.string.select_image)));
    	}
    };
}
