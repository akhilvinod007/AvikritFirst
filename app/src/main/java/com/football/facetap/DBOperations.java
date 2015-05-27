package com.football.facetap;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBOperations {

	static void writeToDB(AppLocalQuestionObject object,SQLiteDatabase writabledb, String category)
	{
		
		writabledb.beginTransaction();
		ContentValues cv=new ContentValues();
		//Log.d("AKHIL","writing data for "+object.getImageName()+  "to DB.");
		cv.put("image_name",object.getImageName());
		cv.put("optionA", object.getOptionA());
		cv.put("optionB", object.getOptionB());
		cv.put("optionC", object.getOptionC());
		cv.put("optionD", object.getOptionD());
		cv.put("correctOption",object.getCorrectOption());
		cv.put("useCount", "0");
		cv.put("URL", "");
		cv.put("download_status",object.getDownloadStatus());
		
		if(writabledb.insert(category,null,cv)==-1){
			Log.d("AKHIL","ERROR in writing to db ");
		}
		writabledb.setTransactionSuccessful();
		writabledb.endTransaction();
		
	}

	
	static void updateDownloadStatusToDB(SQLiteDatabase writabledb, String category, String imageName, String newDownloadStatus) {
		
		writabledb.beginTransaction();
		
		writabledb.execSQL("UPDATE "+category+" SET download_status = '"+newDownloadStatus+"' where image_name = '"+imageName+"'" );
		writabledb.setTransactionSuccessful();
		writabledb.endTransaction();
	}
	
	static void useCountUpdatetoDB( SQLiteDatabase writabledb, String category, String imageName, int newUseCount) {
		
		writabledb.beginTransaction();
		
		writabledb.execSQL("UPDATE "+category+" SET useCount = '"+newUseCount+"' where image_name = '"+imageName+"'" );
		writabledb.setTransactionSuccessful();
		writabledb.endTransaction();
		
	}
	
	static int getDownloadedImagesCount(SQLiteDatabase readabledb, String category) {
		
		//SQLiteDatabase readabledb = db.getReadableDatabase();
		int retValue=0;
		Cursor cursor= readabledb.rawQuery("select count(*) from "+category+" where download_status > 0",null);
		

		if (cursor != null) {
			cursor.moveToFirst();
			retValue=cursor.getInt(0);
			// Always one row returned.
			Log.d("AKHIL","Number of downloaded questions in the table for the category "+category+ "  returned are   "+			retValue);
		}
		
	return retValue;	
	}
}
