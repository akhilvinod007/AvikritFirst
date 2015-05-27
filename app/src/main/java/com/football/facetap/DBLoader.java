package com.football.facetap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.football.facetap.R;
import com.google.android.gms.common.api.GoogleApiClient;

public class DBLoader extends GameDataLoadingActivity  {
	String category="";
    String playMode="";
    GoogleApiClient mGoogleApiClient;
    String opponentName;
    boolean multiPlayer=false;
    Activity launchActivity;
    View layoutView;
	public static enum GameLaunchmode  {ONLINEMODE_SINGLE,ONLINEMODE_MULTI, OFFLINEMODE,QUITTINGGAME}; // I am not sure why I added QUITTINGGAME here. WIll remove it later if it is needed
	
	private static final int FIVE_SEC_INTERVAL_EXPIRE=0;
	private static final int LOADING_TIMER_EXPIRE=1;
	public static final int BACK_BUTTON_PRESS=2;
	boolean launchLock=false;
	boolean timerExpire=false;
	private long timerValue=60000;
	private long timerTick=5000;
    public boolean question_handshake_flag= false;
    public int opponentQuestionCount = 0 ;
    public enum Qhs_state_enum{QHS_INIT_INVITEE,QHS_INIT_INVITER,QHS_I_MSG_RECEIVED,QHS_AWAIT_ACK_OF_SEQ_SENT,
        QHS_AWAIT_SEQ_OF_QUES,QHS_AWAIT_RESP_I_MSG, QHS_ACK_OF_SEQ_SENT_RECV, QHS_SEQ_OF_QUES_RECV, QHS_J_MSG_RECEIVED, QHS_START_GAME};
    public Qhs_state_enum qhs_state = Qhs_state_enum.QHS_INIT_INVITEE;
	DBLoaderCountDownTimer activityTimer = new DBLoaderCountDownTimer(timerValue , timerTick);
    public byte [] JBuffer = null;


    public DBLoader(String category, String mode)//, String opponentName)
    {
       this.category = category;
        //this.opponentName = opponentName;
        playMode = mode;
    }

    public DBLoader(){

    }

    public interface Listener {
        public void onLoadingScreenQuit();
        public void gameLaunchSinglePlayerFragmentStart(String category, GameLaunchmode launchmode);
        public void gameLaunchMultiPlayerFragmentStart();
        public void sendReliableMessageToOpponent(byte [] data_buff);

    }

    Listener listener = null;

    public void setListener(Listener l)
    {
        listener = l;
    }
	public class DBLoaderCountDownTimer extends CountDownTimer {
		public long secsFinished;
		int tickCount=0;
		public DBLoaderCountDownTimer(long startTime, long interval) {
			super(startTime, interval);
		}

		@Override
		public void onFinish() {
			// if ready then move to next activity else offer to play offline/quit
			timerExpire=true;
            if(!multiPlayer) {
                if (questionObjectArrayReady.size() >= 20 && !launchLock) {
                    gameLaunchSinglePlayer(GameLaunchmode.ONLINEMODE_SINGLE);
                } else {
                    onCreateDialog(LOADING_TIMER_EXPIRE);
                }
            } else {
                gameLaunchMultiPlayer();
            }


		}


		@Override
		public void onTick(long millisUntilFinished) {
			/*
			 * The primary logic that is to be implemented here is that we have to check if we are ready to proceed with launching the game.
			 * If we are then:
			 * 		1. cancel the ongoing background async activities for downloading the question data and the associated images
			 * 		2. Proceed with launching the game play activity
			 * 		3. Oops ! Don't forget to close all the DB connections etc.
			 * 
			 * If we are not, then :
			 * 		1. Ask the user if he wants to continue waiting for online loading or he wants to play in the offline mode.
			 */

			//Log.d("AKHIL","Timer tick   +" +millisUntilFinished);
			secsFinished = millisUntilFinished;
			if(tickCount!=0){
				Log.d("AKHIL","size of ready questions is"+questionObjectArrayReady.size());
				Log.d("AKHIL","size of pending questions is"+ questionObjectArrayPendingDownload.size());
				Log.d("AKHIL","size of offline questions is"+ questionObjectArrayOffline.size());
			}
			tickCount++;
            if(!multiPlayer) {
                if (questionObjectArrayReady.size() >= 20 && !launchLock) {
                    gameLaunchSinglePlayer(GameLaunchmode.ONLINEMODE_SINGLE);
                } else {
                    if (tickCount % 3 == 0) {
                        onCreateDialog(FIVE_SEC_INTERVAL_EXPIRE);
                    }
                }
            } else {
                qhs_state_handler();
            }
		}
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private class InitDBLoaderBackground extends AsyncTask<String, Void, Void> {	

        //String category;
        //boolean multiplayer;
		@Override
		protected Void doInBackground(String... arg0) {
			// TODO Auto-generated method stub

			 category = arg0[0];
             playMode = arg0[1];
			// check if the DB is populated already and if it isn't , populate it
			SQLiteDatabase readabledb = db.getReadableDatabase();
			Cursor cursor= readabledb.rawQuery("select count(*) from "+category,null);
			

			if (cursor != null) {
				cursor.moveToFirst();                       // Always one row returned.
				Log.d("AKHIL","Number of entries in the table for the category "+category+ "  returned are   "+cursor.getInt(0));
			}
			Cursor result1 = readabledb.rawQuery("select image_name, optionA, optionB, optionC, optionD, correctOption, useCount from "+category, null);

			Log.d("AKHIL","Number of rows  are"+result1.getCount());



			if(cursor.getInt(0) > 0){
				// rows present in the table, read DB and populate the AppLocalQuestionObject from there

				Log.d("AKHIL","populating array list directly from DB");
				Cursor result = readabledb.rawQuery("select image_name, URL, optionA, optionB, optionC, optionD, correctOption, download_status, useCount from "+category, null);
				while(result.moveToNext()){
					AppLocalQuestionObject tempObject = new AppLocalQuestionObject();
					tempObject.setImageName(result.getString(0));
					tempObject.setImageServingURL(result.getString(1));
					tempObject.setOptionA(result.getString(2));
					tempObject.setOptionB(result.getString(3));
					tempObject.setOptionC(result.getString(4));
					tempObject.setOptionD(result.getString(5));
					tempObject.setCorrectOption(result.getString(6));
					tempObject.setDownloadStatus(Integer.parseInt(result.getString(7)));
					tempObject.setUseCount(result.getInt(8));    
					/** 
					 * 
					 * download status =0  indicates that the image is preloaded.
					 *  download status =1 indicates that the image is from the internet but the download is not complete
					 *  download status =2 indicates that the image is from the internet and the downloading of it is complete 
					 *  
					 *  */

					if(( tempObject.getDownloadStatus() == 0 || tempObject.getDownloadStatus() == 2) ) {		
						questionObjectArrayOffline.add(tempObject);
						if( tempObject.getCount() == 0 ){
							questionObjectArrayReady.add(tempObject);
						}
					} else if ( tempObject.getDownloadStatus() == 1) {
						questionObjectArrayPendingDownload.add(tempObject);
					} else {
						Log.d("AKHIL","ERROR ");
					}
					tempObject=null;
				}
				readabledb.close();
			}
			else{

				try{

					Log.d("AKHIL","populating array list from file and populating DB");   
					BufferedReader br = new BufferedReader(new InputStreamReader(getActivity().getAssets().open(category)));
					String line="";
					String cvsSplitBy=",";
					SQLiteDatabase writabledb= db.getWritableDatabase();
					while ((line = br.readLine()) != null) {
						//Log.d("AKHIL","Line is   "+line);
						// use comma as separator
						String[] questionDataArray = line.split(cvsSplitBy);
						AppLocalQuestionObject tempObject = new AppLocalQuestionObject();
						tempObject.setImageName(questionDataArray[0]);
						tempObject.setOptionA(questionDataArray[1]);
						tempObject.setOptionB(questionDataArray[2]);
						tempObject.setOptionC(questionDataArray[3]);
						tempObject.setOptionD(questionDataArray[4]);
						tempObject.setCorrectOption(questionDataArray[5]);
						tempObject.setDownloadStatus(0);
						questionObjectArrayReady.add(tempObject);
                        if(multiPlayer){
                           questionObjectArrayOffline.add(tempObject);
                        }
						DBOperations.writeToDB(tempObject,writabledb, category);

						tempObject=null;
					}
					br.close();
					writabledb.close();

				}
				catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					Log.d("AKHIL","1"+e.getMessage());
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.d("AKHIL","2"+e.getMessage());
					e.printStackTrace();
				}

			}	
			return null;	
		}


		@Override
		protected void onPostExecute(Void xyz) {
			Log.d("AKHIL","Initial DB read is complete");
            // if playmode equals "M" i.e multiPlayer there will a different set of things that will be required to be executed
            // for the time being I am implementing MultiPlayer game based on preloaded set of questions
            if(!multiPlayer) {
                if (questionObjectArrayReady.size() < 20) {
                    // start the background activity for downloading images
                    Log.d("AKHIL", "Starting BackgroundTask for fetching more images");
                    BackgroundQuestionDataFetchTask questionDataFetchTask = new BackgroundQuestionDataFetchTask(DBLoader.this, category, true);
                    questionDataFetchTask.execute();
                } else {
                    // do nothing, the ticking of the countdown timer will call the start of the game play activity
                    Log.d("AKHIL", "No need of Starting BackgroundTask for fetching more images");
                }
            }
		}
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        layoutView = inflater.inflate(R.layout.dbloader, container, false);
        //Initially only the Sign in BUtton is visible. Once we get logged in , the signout button gets visible and the sign in button gets invisble

        mGoogleApiClient= PlayModeSelection.mGoogleApiClient;
        return layoutView;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/*
		 * DB Countdown timer... the first argument in the call is the amount of time for which the timer should run... the second is the tick 
		 * timer value is being set to 60 secs and the tick interval is set to 10 secs. 
		 * In our implementation, this means that the countdown timer will check if we are ready to proceed with the game every 5 secs for a total period of 60 secs.
		 * At the end of every 5 secs, the user will be asked if he wants to play in the offline mode or if he wants to wait further for actual online loading
		 * At the end of the last 5 sec interval, the user will only have the option of playing in the offline mode or quitting
		 * 
		 */
		super.onCreate(savedInstanceState);
        launchActivity = getActivity();
        if(PlayModeSelection.inviter_flag == PlayModeSelection.inviteEnum.INVITER)
            qhs_state=Qhs_state_enum.QHS_INIT_INVITER;
        else
            qhs_state=Qhs_state_enum.QHS_INIT_INVITEE;
		db=new DatabaseHelper(launchActivity.getApplicationContext());

        if(playMode.equals("M"))
            multiPlayer=true;
        // MODE can be "S" or "M" depending on whether the game is single player mode or multiplayer

		Log.d("AKHIL", "category is   onCreate     "+ category);
		questionObjectArrayOffline=new ArrayList<AppLocalQuestionObject>();
		questionObjectArrayPendingDownload=new ArrayList<AppLocalQuestionObject>();
		questionObjectArrayReady=new ArrayList<AppLocalQuestionObject>();
		map= new ConcurrentHashMap<String,Bitmap>();
		activityTimer.start();
		new InitDBLoaderBackground().execute(category,playMode);


	}


	@Override
	public void onDestroy() {
		/*
		 *  this is just to ensure proper cleaning up. also because CountDownTimer Does not seem to have a good method for finding out whether
		 * it is running or not. Cancelling the timer is necessary. THis is because maybe a situation can arise where the timer doesn't expire
		 * and the image loading that i am trying to do here takes a long time. In that case i dont want the timer to launch 2 activities.
		 * Just playing it safe, that's it.
		 * 
		 */

		super.onDestroy();
		if (activityTimer.secsFinished < timerValue)
		{
			activityTimer.cancel();
		}
		
	}

	
	void gameLaunchSinglePlayer(GameLaunchmode mode) {
        activityTimer.cancel();
        launchLock=true;

        launchPrework(mode);
        //Intent intent = new Intent(getActivity().getBaseContext(),DBLauncher.class);
        //intent.putExtra("CATEGORY",category);
        //if(mode==GameLaunchmode.ONLINEMODE_SINGLE) {
          //  listener.gameLaunchSinglePlayer(GameLaunchmode.ONLINEMODE_SINGLE);
            //intent.putExtra("GAMEMODE", "ONLINE");
        //} else if (mode==GameLaunchmode.OFFLINEMODE){
            listener.gameLaunchSinglePlayerFragmentStart(category,mode);
            //intent.putExtra("GAMEMODE", "OFFLINE");
        //}

        //startActivity(intent);
        //DBLoader.this.finish();
        //getActivity().finish();
	}

    void gameLaunchMultiPlayer() {
        activityTimer.cancel();
        launchLock=true;
        launchPrework(GameLaunchmode.ONLINEMODE_MULTI); // temporary , will change this
        //Intent intent = new Intent(getActivity().getBaseContext(),multiplayerGameplay.class);
        //intent.putExtra("CATEGORY",category);

        listener.gameLaunchMultiPlayerFragmentStart();
        //startActivity(intent);
        //DBLoader.this.finish();
        //getActivity().finish();

    }
	
	void launchPrework(GameLaunchmode mode)
	{
		 /*Preloading of image into map is necessary because I cant keep all the bitmaps in one arraylist in one go.
		 * 
		 * I am sending the first three images preloaded into the bitmap. There is an Async task in the actual gameplay activity which
		 * actually loads the subsequent pictures. I don't know how prudent this strategy will be in case of slower devices which do not
		 * have much RAM or processing muscle. time will tell.
		 *   
		 * Fingers crossed. Phew, long comment ! 
		 * 
		 * EDIT: am loading the pic from the main thread only because it Turns out the scheduling between the downloading images async task and the 
		 * async task mentioned above were competing with each other
		 * */
		Bitmap tempBitmap;
		ArrayList<AppLocalQuestionObject> tempArrayListRef;
		int activityWidth = this.getResources().getDisplayMetrics().widthPixels;
		int activityHeight = this.getResources().getDisplayMetrics().heightPixels;
		//if(qhs_state==Qhs_state_enum.QHS_INIT)

		
		Log.d("AKHIL","height of imageview pic is"+activityHeight/2);
		Log.d("AKHIL","width of imageview pic is"+(activityWidth*70)/100);
		/*
		 * Question Image width is approximately 70% of the available width
		 * 
		 *  Question Image height is approximately 58% of the available height
		 */



        tempArrayListRef = questionObjectArrayOffline;

        switch (mode){
            case ONLINEMODE_SINGLE:
                tempArrayListRef = questionObjectArrayReady;
            case OFFLINEMODE:
                questionSequenceGenerator(mode);
                break;
            case ONLINEMODE_MULTI:
                break;//question sequnce already generated by now , no point calling again
            default:
                Log.d("AKHIL","Error Scenario here");
                break;
        }



		for (int i=0;i<3;i++){

			AppLocalQuestionObject tempObject = tempArrayListRef.get(questionSequence[i]);
			String fileName= tempObject.getImageName();
			Log.d("AKHIL", "Loading image data for "+tempObject.getImageName());
			if (tempObject.getDownloadStatus() == 0) {
				tempBitmap = ImageLoadingHelper.decodeSampledBitmapFromResource(getResources(), 
						getResources().getIdentifier(fileName, 
								"drawable",getActivity().getPackageName()),(activityWidth*70)/100 , (activityHeight*58)/100);
				if(tempBitmap !=null) {

					Log.d("AKHIL","  filename"+ fileName);
					map.put(fileName, tempBitmap);
				} else {
					Log.d("AKHIL","BITMAP loading problem");
				}
			} else {
				tempBitmap= ImageLoadingHelper.decodeSampledBitmapFromFile(getActivity(),fileName, (activityWidth*70)/100, (activityHeight*58)/100);
				if(tempBitmap !=null) {
					map.put(fileName, tempBitmap);
				} else {
					Log.d("AKHIL","BITMAP loading problem");
				}


			}
			
		}
		
		
	}
	private  int randInt(int min, int max) {

	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random(System.currentTimeMillis());

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}

	public void questionSequenceGenerator(GameLaunchmode mode) {
        Log.d("AKHIL","question sequence generator entered");
		int arraySize;
        if(!multiPlayer) {
            if (mode == GameLaunchmode.ONLINEMODE_SINGLE) {
                arraySize = 20;
            } else {
                arraySize = questionObjectArrayOffline.size();
            }
        } else {
            arraySize = questionObjectArrayOffline.size();
        }

		LinkedList<Integer> helperLinkedList = new LinkedList<Integer>();
		
		for (int i=0;i<arraySize;i++){
			helperLinkedList.add(i);
		}
		
		arraySize--;
		for (int i =19,j=0;i>=0;i--,j++,arraySize--){
			questionSequence[j]=helperLinkedList.remove(randInt(0,arraySize));
			Log.d("AKHIL"," question number "+j+"     seq num"+questionSequence[j]);
		}
		
	}

	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog=null;
		Builder builder =null;
		switch (id) {
		case FIVE_SEC_INTERVAL_EXPIRE :
			if (!launchLock ) { //launchlock indicates that if some other dialog box is already present then this dialog box should nt be displayed
				builder = new AlertDialog.Builder(getActivity());
				launchLock=true;
				builder.setTitle("OOPS");
				builder.setMessage("Need some time to load the data");
				builder.setCancelable(false);
				builder.setPositiveButton("Offline Play", new OfflineGameLaunchCLickListener());
				builder.setNegativeButton("Quit", new GameQuitClickListener());
				builder.setNeutralButton("Wait", new GameQuitCancelListener());
				dialog= builder.create();  
				dialog.show();
			} else {
				Log.d("AKHIL","FIVE_SEC_INTERVAL_EXPIRE       launchLock  ="+launchLock);
			}
			break;

		case BACK_BUTTON_PRESS:
			launchLock=true;
			builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(" QUIT GAME ??");
			builder.setCancelable(false);
			builder.setPositiveButton("Yes", new GameQuitClickListener());
			builder.setNegativeButton("No", new GameQuitCancelListener());
			dialog= builder.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
			break;
		case LOADING_TIMER_EXPIRE:
			launchLock=true;
			builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("You seem to be offline");
			builder.setMessage("Launch Offline Mode? ");
			builder.setCancelable(false);
			builder.setPositiveButton("Play", new OfflineGameLaunchCLickListener());
			builder.setNegativeButton("Quit", new GameQuitClickListener());
			dialog= builder.create(); 
			dialog.show();
		}
		return dialog;
	}

    /*
	@Override
	public void onBackPressed(){
		Log.d("AKHIL","Back pressed");
		onCreateDialog(BACK_BUTTON_PRESS);
	}
	*/
	private final class OfflineGameLaunchCLickListener implements
	DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
            gameLaunchSinglePlayer(GameLaunchmode.OFFLINEMODE);
		}
	} 

	 
	private final class GameQuitCancelListener implements
	DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			launchLock=false;
			if(timerExpire){
				if (questionObjectArrayReady.size() >=20 && !launchLock) {
                    gameLaunchSinglePlayer(GameLaunchmode.ONLINEMODE_SINGLE);
				} else {
                    gameLaunchSinglePlayer(GameLaunchmode.OFFLINEMODE);
				}
			}
		}
	}
	private final class GameQuitClickListener implements
	DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
            listener.onLoadingScreenQuit();
		}
	} 


    private void qhs_state_handler(){

        Log.d("AKHIL","Enter the qhs_state_handler");
        switch(qhs_state){
            case QHS_INIT_INVITER:
                Log.d("AKHIL","Enter the qhs_state_handler QHS_INIT_INVITER");
                if(questionObjectArrayOffline.size()>0){
                    byte []data_buf = new byte[5];

                    String temp_String = ""+questionObjectArrayOffline.size();
                    Log.d("AKHIL","questionObjectArrayOffline"+ temp_String);
                    data_buf[0]='I';
                    try{
                        byte[] byteArray = temp_String.getBytes();
                        data_buf[1]= (byte)byteArray.length;
                        System.arraycopy(byteArray,0,data_buf,2,byteArray.length);
                    }
                    catch(Exception e)
                    {
                        Log.d("AKHIL", "There is some error in converting the temp string to Byte Array");
                    }
                    Log.d("AKHIL",""+data_buf);
                    listener.sendReliableMessageToOpponent(data_buf);
                    qhs_state = Qhs_state_enum.QHS_AWAIT_RESP_I_MSG;
                }

                break;
            case QHS_I_MSG_RECEIVED:
                Log.d("AKHIL","Enter the qhs_state_handler QHS_I_MSG_RECEIVED");
                if(questionObjectArrayOffline.size()>0)
                {
                    if(questionObjectArrayOffline.size()>opponentQuestionCount)
                    {
                        //TODO: send J0 message indicating request for the sequence of questions
                        byte []data_buf = new byte[2];
                        data_buf[0]='J';
                        data_buf[1]='0';
                        listener.sendReliableMessageToOpponent(data_buf);
                        qhs_state = Qhs_state_enum.QHS_AWAIT_SEQ_OF_QUES;
                    }
                    else{

                        questionSequenceGenerator(GameLaunchmode.ONLINEMODE_MULTI);
                        //byte []data_buf = new byte[55]; // J 1  , 31 digits, 19 commas
                        //data_buf[0]='J';
                        //data_buf[1]='1';
                        String temp_String = "";
                        for(int i : questionSequence)
                        {
                            temp_String=temp_String+i+",";
                        }

                        try{

                            Log.d("AKHIL","Consolidated Outgoing sequence "+temp_String);
                            byte[] byteArray = temp_String.getBytes();
                            Log.d("AKHIL","No error 1");
                            //data_buf[2]=(byte)(byteArray.length-1); MY first guess is that this is wrong
                            //System.arraycopy(byteArray,0,data_buf,3,byteArray.length-1);// the -1 at the end is to remove the last comma
                            //data_buf[2]=(byte)(byteArray.length-1);
                            byte []data_buf = new byte[byteArray.length+2]; // J 1
                            data_buf[0]='J';
                            data_buf[1]='1';
                            //System.arraycopy(byteArray,0,data_buf,2,byteArray.length-1);// the -1 at the end is to remove the last comma
                            System.arraycopy(byteArray,0,data_buf,2,byteArray.length);// the -1 at the end is to remove the last comma
                            Log.d("AKHIL","No error 2");
                            listener.sendReliableMessageToOpponent(data_buf);
                            qhs_state = Qhs_state_enum.QHS_AWAIT_ACK_OF_SEQ_SENT;
                        }
                        catch(Exception e)
                        {
                           // Log.d("AKHIL", "There is some error in converting the temp string to Byte Array");
                            Log.d("AKHIL", "There is some error in converting the temp string to Byte Array"+e+e.getMessage()+e.getStackTrace());
                        }


                    }
                }
                break;

            case QHS_AWAIT_ACK_OF_SEQ_SENT:
                Log.d("AKHIL","Enter the qhs_state_handler QHS_AWAIT_ACK_OF_SEQ_SENT");
            case QHS_AWAIT_RESP_I_MSG:
                Log.d("AKHIL","Enter the qhs_state_handler QHS_AWAIT_RESP_I_MSG");
            case QHS_INIT_INVITEE:
                Log.d("AKHIL","Enter the qhs_state_handler QHS_INIT_INVITEE");
            case QHS_AWAIT_SEQ_OF_QUES:
                Log.d("AKHIL","Enter the qhs_state_handler QHS_AWAIT_SEQ_OF_QUES");
                break;
            case QHS_J_MSG_RECEIVED:
                if(JBuffer[1]=='0'){
                    // You have to send the sequence and then wait for the ACK

                    questionSequenceGenerator(GameLaunchmode.ONLINEMODE_MULTI);

                    String temp_String = "";
                    for(int i : questionSequence)
                    {
                        temp_String=temp_String+i+",";
                    }
                    //Log.d("AKHIL","Consolidated Outgoing sequence "+temp_String);
                    try{
                        Log.d("AKHIL","Consolidated Outgoing sequence "+temp_String);
                        byte[] byteArray = temp_String.getBytes();

                        //data_buf[2]=(byte)(byteArray.length-1); MY first guess is that this is wrong
                        //System.arraycopy(byteArray,0,data_buf,3,byteArray.length-1);// the -1 at the end is to remove the last comma
                        //data_buf[2]=(byte)(byteArray.length-1);
                        Log.d("AKHIL","byte array length is "+byteArray.length);
                        byte []data_buf = new byte[byteArray.length+2];
                        data_buf[0]='J';
                        data_buf[1]='1';
                        System.arraycopy(byteArray,0,data_buf,2,byteArray.length);
                        Log.d("AKHIL", "This print should come I think");
                        listener.sendReliableMessageToOpponent(data_buf);
                        qhs_state = Qhs_state_enum.QHS_AWAIT_ACK_OF_SEQ_SENT;
                    }
                    catch(Exception e)
                    {
                       Log.d("AKHIL", "There is some error in converting the temp string to Byte Array"+e+e.getMessage()+e.getStackTrace());
                    }

                    break;
                } else if (JBuffer[1]=='1'){
                    // You have received the sequence, copy the sequence into the questionSequence
                    // fall through to QHS_SEQ_OF_QUES_RECV state
                    int k=0;
                    for(int i=2;i<JBuffer.length;){
                        int j=i;
                        while(JBuffer[j]!=','){
                            j++;
                        }
                        //int thisScore =  Integer.parseInt(new String(buf,2,(int)(buf[1])));
                        questionSequence[k++]= Integer.parseInt(new String(JBuffer,i,j-i));
                        Log.d("AKHIL","questionseq["+(k-1)+"] is "+questionSequence[k-1]);
                        i=j+1;
                    }


                    // make sure that the Handler is called from here
                }

            case QHS_SEQ_OF_QUES_RECV:
                byte []data_buf = new byte[1];
                data_buf[0]='S';
                //TODO process the sequence received and insert into your own sequence array... :( as of now
                listener.sendReliableMessageToOpponent(data_buf);
            case QHS_ACK_OF_SEQ_SENT_RECV:
            case QHS_START_GAME:
                gameLaunchMultiPlayer();
                break;
            default:
                Log.d("AKHIL","This is some error scenario");
                break;

        }
    }
}
