package com.football.facetap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.api.GoogleApiClient;

//public class HomeScreen extends Activity {
public class HomeScreen extends Fragment implements View.OnClickListener {
/*
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen_layout);
		}
*/


    Activity launchActivity;
	public interface Listener {
		public void onSinglePlayClick(String category);
		public void displayMultiplayerLoadingScreen();
	}

	Listener listener = null;
	public void setListener(Listener l)
	{
		listener = l;
	}
	View layoutView;
	GoogleApiClient mGoogleApiClient;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		layoutView = inflater.inflate(R.layout.home_screen_layout, container, false);
		//Initially only the Sign in BUtton is visible. Once we get logged in , the signout button gets visible and the sign in button gets invisble
		/*
        launchActivity= getActivity();
		layoutView.findViewById(R.id.button_sign_out_play_mode_activity).setVisibility(View.INVISIBLE);
		layoutView.findViewById(R.id.single_play_button).setOnClickListener(this);
		layoutView.findViewById(R.id.multi_play_button).setOnClickListener(this);
		mGoogleApiClient= PlayModeSelection.mGoogleApiClient;*/

        launchActivity= getActivity();
		layoutView.findViewById(R.id.play_button).setOnClickListener(this);
		layoutView.findViewById(R.id.sharebutton).setOnClickListener(this);
		layoutView.findViewById(R.id.ratebutton).setOnClickListener(this);
		layoutView.findViewById(R.id.audiobutton).setOnClickListener(this);
		layoutView.findViewById(R.id.statsbutton).setOnClickListener(this);


		return layoutView;
	}

/*
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
*/
	@Override
	public void onClick(View v) {

		switch (v.getId()){
			case R.id.play_button:
                Log.d("AKHIL","on Click received in HomeScreen");
				listener.onSinglePlayClick("Football");
				break;
			case R.id.sharebutton:
				//onMultiPlayClick(v);
				break;
            case R.id.ratebutton:
                break;
            case R.id.audiobutton:
                break;
            case R.id.statsbutton:
                break;

		}
	}


    public void onBackPressed(){
        Log.d("AKHIL","Back pressed");
        onCreateDialog(0);
    }

    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog=null;
        Builder builder =null;
        switch (id) {
            case 0:
                builder = new AlertDialog.Builder(launchActivity);
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
            launchActivity.finish();

        }
    }
}
