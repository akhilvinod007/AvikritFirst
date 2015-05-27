package com.football.facetap;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageLoadingHelper {

	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}
	
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
	        int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeResource(res, resId, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    Bitmap tempreturnvalue= BitmapFactory.decodeResource(res, resId, options);
	    Bitmap returnvalue=  Bitmap.createScaledBitmap(tempreturnvalue, reqWidth, reqHeight, true);
	    tempreturnvalue.recycle();
	    return returnvalue;
	}
	
	public static Bitmap decodeSampledBitmapFromFile(Activity activity,String fileName, int reqWidth,
			int reqHeight ){
		final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    //Log.d("AKHIL","inside decodeSampledBitmapFromFile. Trying to decode"+activity.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName);
	    BitmapFactory.decodeFile(activity.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName,options);
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    
	    Bitmap tempreturnvalue=  BitmapFactory.decodeFile(activity.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName,options);
	    
	    Bitmap returnvalue=  Bitmap.createScaledBitmap(tempreturnvalue, reqWidth, reqHeight, true);
	    tempreturnvalue.recycle();
	    if(returnvalue!=null){
	    return returnvalue;
	    }
	    
	    	Log.d("AKHIL","There is some error in the image loading");
	    	return null;
	    
	}
	
	
}
