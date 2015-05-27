package com.football.facetap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.football.facetap.R;



public class InitialLoading extends Activity {


	enum GameLaunchFlag {OK,NOTOK};
	MyCountDownTimer countdownTimer;
	GameLaunchFlag launchflag= GameLaunchFlag.OK;
	Animation rotate;
	int flagCountDownExpired = 0; // 1 indicates that it has not expired
	ImageView imageView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.initiallogoscreen);
		Typeface temptypeface = Typeface.createFromAsset(getAssets(), "Xoxoxa.ttf");
		imageView= (ImageView)findViewById(R.id.imageView1);
		rotate= AnimationUtils.loadAnimation(this, R.anim.rotate);
		imageView.startAnimation(rotate);
		TextView tempview =(TextView)findViewById(R.id.gameTitle);
		tempview.setTypeface(temptypeface);
		countdownTimer = new MyCountDownTimer(5000, 1000);
		countdownTimer.start();
	}




	@Override
	public void onBackPressed(){
		Log.d("AKHIL","Back pressed on initial launch screen");
		onCreateDialog(0);
	}


	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog=null;
		Builder builder =null;
		switch (id) {
		case 0:
			builder = new AlertDialog.Builder(this);
			builder.setTitle("Quit Game?");
			builder.setCancelable(true);
			launchflag=GameLaunchFlag.NOTOK;
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
			// cancel the countdown timer and set the gameLaunchFlag = NOTOK
			launchflag=GameLaunchFlag.NOTOK;
			countdownTimer.cancel();
			InitialLoading.this.finish();
			//Apparently the async task is not getting destroyed...hmmmmm....
		}
	}


	private final class BackButtonCancelOnClickListener implements
	DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {

			launchflag = GameLaunchFlag.OK;
			
			// have not implemented the logic to restart the animation. Too much of a headache
			if(flagCountDownExpired==1) {


				Intent intent = new Intent(getBaseContext(),HomeScreen.class);
				intent.putExtra("CATEGORY", "Football");
				Log.d("AKHIL","from the new package here ");
				startActivity(intent);
				InitialLoading.this.finish();
			}


		}
	}
	public class MyCountDownTimer extends CountDownTimer {

		public MyCountDownTimer( long startTime, long interval) {
			super(startTime, interval);

		}

		@Override
		public void onFinish() {
			flagCountDownExpired=1;
			if(launchflag== GameLaunchFlag.OK){
				Intent intent = new Intent(getBaseContext(),HomeScreen.class);
				intent.putExtra("CATEGORY", "Football");
				startActivity(intent);
				InitialLoading.this.finish();

			}
		} 

		@Override
		public void onTick(long millisUntilFinished) {

			if(millisUntilFinished <3000) {
				TextView tempview =(TextView)findViewById(R.id.gameTitle);
				tempview.setVisibility(View.VISIBLE);
			}

		}

	}

	
	@Override
	public void onPause()
	{
		super.onPause();
		launchflag=GameLaunchFlag.NOTOK;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(flagCountDownExpired==1) {
			Intent intent = new Intent(getBaseContext(),HomeScreen.class);
			intent.putExtra("CATEGORY", "Football");
			startActivity(intent);
			InitialLoading.this.finish();
		}
	}
}
