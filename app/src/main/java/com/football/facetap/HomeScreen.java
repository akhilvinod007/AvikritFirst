package com.football.facetap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class HomeScreen extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen_layout);
		}

	public void onPlayClick(View view)
	{
        Intent intent = new Intent(getBaseContext(),PlayModeSelection.class);
        startActivity(intent);
        HomeScreen.this.finish();
	}

	@Override
	public void onBackPressed(){
		Log.d("AKHIL","Back pressed");
		onCreateDialog(0);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog=null;
		Builder builder =null;
		switch (id) {
		case 0:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(" QUIT GAME ??");
			builder.setCancelable(true);
			builder.setPositiveButton("Yes", new BackButtonConfirmOnClickListener());
			builder.setNegativeButton("No", null);
			dialog= builder.create();
			dialog.show();
			break;
		}
		return dialog;
	}
	
	private final class BackButtonConfirmOnClickListener implements
	DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			HomeScreen.this.finish();

		}
	}
	
}
