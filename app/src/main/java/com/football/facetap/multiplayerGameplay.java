package com.football.facetap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Random;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.os.CountDownTimer;
//import com.appspot.friendzone_engine.androidapi.model.QuestionObjectCollection;
import com.football.facetap.R;


@SuppressWarnings("unused")
public class multiplayerGameplay extends GameDataLoadingActivity implements View.OnClickListener {
    private final int MAX_QUESTION_SCORE=100;
    private final int TILE_TAP_PENALTY=5;
    private final int BUTTON_CLICK_PENALTY=25;
    private static final int GAME_OVER = 0;
    private static final int GAME_CANCEL=1;
    //private static final int GAME_LIVES_OVER=2;
    // this could be offline or online
    /*option clicking will be disabled till the user clicks at least one tile. Maybe 5 lives logic mitigates the need for this,
     * but I am keeping it this way for the time being. buttonLock implements this logic
     *
     */
    String gameMode;
    View layoutView;
    Activity launchActivity;

    String opponentName;
    Boolean buttonLock=true;
    Boolean finalOpponentScoreReceived=false;
    int questionScore=MAX_QUESTION_SCORE;
    int currentScore=0;
    int currentOpponentScore=0;


    int numberOfWrongTries=0;
   // int lifeImageIds[] = new int[5];
    Boolean updateDone=false; // this is used to ascertain if the DB has been updated after the game gets over
    private static String category="";
    ArrayList<AppLocalQuestionObject> questionObjectArray;  // the main arrayList which contains all the data
    ImageView  imagewrapper;
    int [] tilesStatusArray =  new int[25]; // 0 indicates disappearing of tile is OK. 1 indicates it is not OK
    private ProgressBar playerOneProgress;
    private ProgressBar playerTwoProgress;
    ArrayList<Integer> correctOption= new ArrayList<Integer>();
    MyCountDownTimer countdowntimer;
    enum GameStatus{ON,OVER, DOWNLOADING,PAUSE,WAIT_FOR_OPPONENT_FINAL_SCORE};
    GameStatus status= GameStatus.DOWNLOADING;
    GameStatus previousStatus= GameStatus.DOWNLOADING;
    int current_question_number=0;
    int optionViewIds[]= new int[4];
    long globalmillisUntilFinished=100000;
    private final long gameDuration=100000;

    public static interface Listener {
        public void onMultiplePlayGameOver();
        public void broadcastScoreToOpponent(int currentScore,boolean finalScore);
        public void multiplePlayGameCancel();


    }
    Listener listener = null;

    @Override
    public void onClick(View v) {

        switch(v.getId())
        {
            case R.id.tile_01:
            case R.id.tile_02:
            case R.id.tile_03:
            case R.id.tile_04:
            case R.id.tile_05:
            case R.id.tile_06:
            case R.id.tile_07:
            case R.id.tile_08:
            case R.id.tile_09:
            case R.id.tile_10:
            case R.id.tile_11:
            case R.id.tile_12:
            case R.id.tile_13:
            case R.id.tile_14:
            case R.id.tile_15:
            case R.id.tile_16:
            case R.id.tile_17:
            case R.id.tile_18:
            case R.id.tile_19:
            case R.id.tile_20:
            case R.id.tile_21:
            case R.id.tile_22:
            case R.id.tile_23:
            case R.id.tile_24:
            case R.id.tile_25:
                boxClick(v);
                break;
            case R.id.optionA :
            case R.id.optionB:
            case R.id.optionC:
            case R.id.optionD:
                optionClick(v);
                break;
            default:
                Log.d("AKHIL", "SOme error scenario not taken care of properly");
                break;


        }
    }



    public  void setListener(Listener l){
     listener = l;
    }
    public multiplayerGameplay(){

    }
    public multiplayerGameplay(String category, String gameMode, String oppName)
    {
        this.category=category;
        this.gameMode=gameMode;
        this.opponentName = oppName;
        if( gameMode.equals("ONLINE")) {
            Log.d("AKHIL","Launching game in online mode");
            questionObjectArray=questionObjectArrayReady;
        } else {
            Log.d("AKHIL","Launching game in offline mode");
            questionObjectArray=questionObjectArrayOffline;
        }
    }

    public class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer( long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            if(status == GameStatus.ON)
            {
                Log.d("AKHIL","setting status to over and doing some housekeeping with the DB");
                status=GameStatus.OVER;
                onCreateDialog(GAME_OVER);
            }
            resetTileOverlay();
            // show dialog box that the game is over because you ran out of time
            //listener.broadcastScoreToOpponent(currentScore,true);

        }
        @Override
        public void onTick(long millisUntilFinished) {
            globalmillisUntilFinished=millisUntilFinished;
            //playerOneProgress.setProgress((int) ( 100 - millisUntilFinished / 1000));


        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        launchActivity= getActivity();
        layoutView = inflater.inflate(R.layout.multiplayer_gameplay, container, false);
        imagewrapper = (ImageView) layoutView.findViewById(R.id.imagewrapper);
        Typeface temptypeface = Typeface.createFromAsset(getActivity().getAssets(), "Digiface Regular.ttf");
        TextView tempview = (TextView)layoutView.findViewById(R.id.score);
        tempview.setTypeface(temptypeface);
        temptypeface = Typeface.createFromAsset(getActivity().getAssets(), "Xoxoxa.ttf");
        int baseTileResourceId=R.id.tile_01;
        for(int i=0;i<25;i++) {
            tilesStatusArray[i]=0;
            layoutView.findViewById(baseTileResourceId+i).setOnClickListener(this);
        }
        tempview = (TextView)layoutView.findViewById(R.id.OpponentName);
        tempview.setText(opponentName);
        playerOneProgress = (ProgressBar) layoutView.findViewById(R.id.playerOneProgressBar);
        playerTwoProgress = (ProgressBar) layoutView.findViewById(R.id.playerTwoProgressBar);
        optionViewIds[0]=R.id.optionA;
        optionViewIds[1]=R.id.optionB;
        optionViewIds[2]=R.id.optionC;
        optionViewIds[3]=R.id.optionD;
        Resources resources = getResources();
        for(int i : optionViewIds)
        {
            tempview = (TextView)layoutView.findViewById(i);
            tempview.setOnClickListener(this);
            tempview.setTypeface(temptypeface);
        }
		/*layout housekeeping ends*/
        BackgroundQuestionDataFetchTask backgroundImageLoadingTask = new BackgroundQuestionDataFetchTask(this, category,false);
        backgroundImageLoadingTask.execute();
        Log.d("AKHIL","Size of arraylist received here is"+questionObjectArray.size());
        for (int i=0;i<20;i++){
            AppLocalQuestionObject tempAssetObject = questionObjectArray.get(questionSequence[i]);
            if(tempAssetObject.getCorrectOption().equals(tempAssetObject.getOptionA()))
            {
                correctOption.add(optionViewIds[0]);
            }
            else if (tempAssetObject.getCorrectOption().equals(tempAssetObject.getOptionB())){
                correctOption.add(optionViewIds[1]);
            }
            else if (tempAssetObject.getCorrectOption().equals(tempAssetObject.getOptionC())){
                correctOption.add(optionViewIds[2]);
            }
            else if (tempAssetObject.getCorrectOption().equals(tempAssetObject.getOptionD())){

                correctOption.add(optionViewIds[3]);
            }
        }
        imagewrapper.setBackground(new BitmapDrawable(getResources(),map.get(questionObjectArray.get(questionSequence[current_question_number]).getImageName())));
        Button button= (Button)layoutView.findViewById(R.id.optionA);
        button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionA());
        button= (Button)layoutView.findViewById(R.id.optionB);
        button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionB());
        button= (Button)layoutView.findViewById(R.id.optionC);
        button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionC());
        button= (Button)layoutView.findViewById(R.id.optionD);
        button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionD());
        status=GameStatus.ON;
        previousStatus=GameStatus.ON;
        return layoutView;
    }


    public void resetTileOverlay()
    {
        int baseTileResourceId=R.id.tile_01;
        int temp=0;
        while(temp<25)
        {
            View tileView=(View) layoutView.findViewById(baseTileResourceId + temp);
            //Log.d("AKHIL","set"+baseTileResourceId+temp);
            tilesStatusArray[temp]=0;
            tileView.setVisibility(View.VISIBLE);
            temp++;
        }

    }



    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle icicle) {
		/*layout housekeeping starts*/
        super.onCreate(icicle);
    }



    public void optionClick(View view)
    {
        Log.d("AKHIL","question number is "+current_question_number);
        int answerSelected= view.getId();
        TextView scoreBox= (TextView)layoutView.findViewById(R.id.score) ;
        if(status==GameStatus.ON && !buttonLock)
        {
            if(answerSelected==correctOption.get(current_question_number))
            {
                buttonLock=true;
                if(currentScore>0) {
                    playerOneProgress.setProgress((int)(((float)currentScore / 2000) * 100));
                    Log.d("AKHIL","Progress is being set to "+ (int)(((float)currentScore / 2000) * 100) );
                }
                currentScore+=questionScore;
                listener.broadcastScoreToOpponent(currentScore,false);
                questionScore=MAX_QUESTION_SCORE;
                scoreBox.setText(String.format("%05d",currentScore));
                Toast.makeText(launchActivity.getApplicationContext(), "Correct!!", Toast.LENGTH_SHORT).show();

                resetTileOverlay();
                Log.d("AKHIL","removing "+questionObjectArray.get(questionSequence[current_question_number]).getImageName());
                map.remove(questionObjectArray.get(questionSequence[current_question_number]).getImageName());
                current_question_number++;
                if( current_question_number <20)
                {
                    Button button= (Button)launchActivity.findViewById(R.id.optionA);
                    button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionA());
                    button= (Button)launchActivity.findViewById(R.id.optionB);
                    button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionB());
                    button= (Button)launchActivity.findViewById(R.id.optionC);
                    button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionC());
                    button= (Button)launchActivity.findViewById(R.id.optionD);
                    button.setText(questionObjectArray.get(questionSequence[current_question_number]).getOptionD());
                    imagewrapper.setBackground(new BitmapDrawable(getResources(),
                            map.get(questionObjectArray.get(questionSequence[current_question_number]).getImageName())));

                    if(current_question_number <=17){
                        Log.d("AKHIL","Image loading task being called for "+questionObjectArray.get(questionSequence[current_question_number+2]).getImageName());
                        //imageLoadingTask.execute(questionObjectArray.get(current_question_number+2));


                        if(questionObjectArray.get(questionSequence[current_question_number+2]).getDownloadStatus()==0) {
                            Log.d("AKHIL","download status is 0");
                            map.put(questionObjectArray.get(questionSequence[current_question_number+2]).getImageName(),
                                    ImageLoadingHelper.decodeSampledBitmapFromResource(getResources(),
                                            getResources().getIdentifier(questionObjectArray.get(questionSequence[current_question_number+2]).getImageName(),
                                                    "drawable",launchActivity.getPackageName()),
                                            (this.getResources().getDisplayMetrics().widthPixels*70)/100,
                                            (this.getResources().getDisplayMetrics().heightPixels*58)/100));
                        } else if (questionObjectArray.get(questionSequence[current_question_number+2]).getDownloadStatus()==2) {
                            Log.d("AKHIL","download status is 2");
                            map.put(questionObjectArray.get(questionSequence[current_question_number+2]).getImageName(),
                                    ImageLoadingHelper.decodeSampledBitmapFromFile(launchActivity,
                                            questionObjectArray.get(questionSequence[current_question_number+2]).getImageName(),
                                            (this.getResources().getDisplayMetrics().widthPixels*70)/100,
                                            (this.getResources().getDisplayMetrics().heightPixels*58)/100));

                        }
                        else {
                            Log.d("AKHIL","download status is invalid I suppose");
                        }
                    }
                }
                else{
                    countdowntimer.cancel();
                    Log.d("AKHIL","Game over here.");
                    //Toast.makeText(getApplicationContext(), "Game Over. Score is"+currentScore, Toast.LENGTH_SHORT).show();
                    //listener.broadcastScoreToOpponent(currentScore,true);
                    onCreateDialog(GAME_OVER);
                    status=GameStatus.OVER;
                    previousStatus=status;
                }
            }
            else{

                questionScore-=BUTTON_CLICK_PENALTY;
                Toast.makeText(launchActivity.getApplicationContext(), "Wrong!!", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            return;
            // need to go to the screen after a game is over from here or pop up or something. However , I don't expect this leg to hit ever
        }
    }


    public void updateOpponentProgress(int opponentScore)
    {
        if(opponentScore>0) {
            currentOpponentScore = opponentScore;
            Log.d("AKHIL","Progress bar status is "+(int)(((float)opponentScore/2000)*100));
            playerTwoProgress.setProgress( (int)(((float)opponentScore/2000)*100));
        }
    }
    //handle for updating score on clicking the tiles overlay
    public void boxClick(View tileView)
    {
        buttonLock=false;
        if(status==GameStatus.ON && tilesStatusArray[tileView.getId()- R.id.tile_01]==0)
        {
            Animation disappear= AnimationUtils.loadAnimation(launchActivity, R.anim.tile_vanish);
            tileView.startAnimation(disappear);
            tilesStatusArray[tileView.getId()- R.id.tile_01]=1;
            tileView.setVisibility(View.INVISIBLE);
            questionScore-=TILE_TAP_PENALTY;
        }
    }




    public void onBackPressed(){
        Log.d("AKHIL","Back pressed");
        onCreateDialog(GAME_CANCEL);
    }



    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog=null;
        Builder builder =null;
        switch (id) {
            case GAME_OVER:
                listener.broadcastScoreToOpponent(currentScore,true);
                builder = new AlertDialog.Builder(launchActivity);
                Log.d("AKHIL","GAME_OVER");
                if(finalOpponentScoreReceived) {
                    if(currentOpponentScore > currentScore)
                        builder.setTitle("YOU LOST");
                    else
                        builder.setTitle("YOU WON");
                    builder.setMessage("Your score " + currentScore+"\nOpponent Score "+currentOpponentScore);
                    builder.setCancelable(false);
                    builder.setPositiveButton("Play Again", new GameOverPlayAgainOnClickListener());
                    builder.setNegativeButton("Quit", new GameOverQuitOnClickListener());
                    dialog = builder.create();
                    dialog.show();
                }
                else{
                    status = GameStatus.WAIT_FOR_OPPONENT_FINAL_SCORE;
                }
                break;
            case GAME_CANCEL:
                builder = new AlertDialog.Builder(launchActivity);
                builder.setTitle(" QUIT GAME ??");
                builder.setCancelable(true);
                builder.setPositiveButton("Yes", new BackButtonConfirmOnClickListener());
                builder.setNegativeButton("No", new BackButtonCancelOnClickListener());
                dialog= builder.create();
                dialog.show();
                break;

        }
        return dialog;
    }

    private final class BackButtonConfirmOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
           /* Intent intent = new Intent(multiplayerGameplay.this,HomeScreen.class);

            startActivity(intent);
            multiplayerGameplay.this.finish();

*/
            //Apparently the async task is not getting destroyed...hmmmmm....
            listener.multiplePlayGameCancel();
        }
    }

    private final class BackButtonCancelOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {

        }
    }
    private final class GameOverQuitOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            displayRotatingSpinner();

            /*
            Intent i = new Intent(multiplayerGameplay.this,HomeScreen.class);
            startActivity(i);
            multiplayerGameplay.this.finish();
            */
            listener.onMultiplePlayGameOver();
        }
    }

    private final class GameOverPlayAgainOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            displayRotatingSpinner();
            listener.onMultiplePlayGameOver();
        }
    }

    @Override
    public void onDestroy()
    {
        // maybe some cleanup animation can be shown

        // Don't know if this is the best place to do this. But let it be for the time being.

        if(!updateDone) {
            Log.d("AKHIL","Updating DB from onDestroy");
            for (int i=0;i<=current_question_number;i++){
                if(i!=20){
                    DBOperations.useCountUpdatetoDB( db.getWritableDatabase(),category,questionObjectArray.
                            get(questionSequence[i]).getImageName(), questionObjectArray.get(questionSequence[i]).getCount()+1 );
                }
            }
        }
        super.onDestroy();


    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d("AKHIL","on Pause");
        countdowntimer.cancel();
        previousStatus=status;
        status= GameStatus.PAUSE;

    }


    @Override
    public void onResume()
    {
        super.onResume();
        Log.d("AKHIL","on REsumne");
        Log.d("AKHIL", "status is "+status);
        status=previousStatus;
        Log.d("AKHIL", "status is "+previousStatus+"     "+globalmillisUntilFinished);
        if(status==GameStatus.ON)
        {
            //countdowntimer = new MyCountDownTimer((TextView)findViewById(R.id.countdowntimer),globalmillisUntilFinished , 1000);
            countdowntimer = new MyCountDownTimer(globalmillisUntilFinished , 500);
            countdowntimer.start();
        }

    }

    public void displayRotatingSpinner() {
        ProgressBar bar;
        Log.d("AKHIL","displaying rotating spinner");
        bar = (ProgressBar) layoutView.findViewById(R.id.spinningwait);
        bar.setVisibility(View.VISIBLE);
        for (int i=0;i<=current_question_number;i++){
            if(i!=20){
                DBOperations.useCountUpdatetoDB( db.getWritableDatabase(),category,questionObjectArray.
                        get(questionSequence[i]).getImageName(), questionObjectArray.get(questionSequence[i]).getCount()+1 );
            }
        }
        updateDone=true;
        bar.setVisibility(View.GONE);
    }
}


