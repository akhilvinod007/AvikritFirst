package com.football.facetap;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


import android.graphics.Bitmap;
import android.support.v4.app.Fragment;


public abstract class GameDataLoadingActivity extends Fragment {
	public static DatabaseHelper db;
	public  static  ArrayList<AppLocalQuestionObject> questionObjectArrayReady ;//= new ArrayList<AppLocalQuestionObject>();
	public  static  ArrayList<AppLocalQuestionObject> questionObjectArrayPendingDownload;// = new ArrayList<AppLocalQuestionObject>();
	public static ArrayList<AppLocalQuestionObject> questionObjectArrayOffline;// = new ArrayList<AppLocalQuestionObject>();
	public static  ConcurrentHashMap<String,Bitmap> map;
	public static int [] questionSequence= new int[20];


}
