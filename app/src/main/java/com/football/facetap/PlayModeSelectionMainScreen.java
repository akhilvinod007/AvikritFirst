package com.football.facetap;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;


public class PlayModeSelectionMainScreen extends Fragment implements View.OnClickListener
{
    View layoutView;
    GoogleApiClient mGoogleApiClient;


    public PlayModeSelectionMainScreen() {

    }

    public interface Listener {
        public void onSinglePlayClick(String category);
        public void displayMultiplayerLoadingScreen();
    }

    Listener listener = null;
    public void setListener(Listener l)
    {
       listener = l;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        layoutView = inflater.inflate(R.layout.activity_play_mode_selection, container, false);
        //Initially only the Sign in BUtton is visible. Once we get logged in , the signout button gets visible and the sign in button gets invisble
        layoutView.findViewById(R.id.button_sign_out_play_mode_activity).setVisibility(View.INVISIBLE);
        layoutView.findViewById(R.id.single_play_button).setOnClickListener(this);
        layoutView.findViewById(R.id.multi_play_button).setOnClickListener(this);
        mGoogleApiClient= PlayModeSelection.mGoogleApiClient;
        return layoutView;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case com.football.facetap.R.id.single_play_button:
                listener.onSinglePlayClick("Football");
                break;
            case com.football.facetap.R.id.multi_play_button:
                onMultiPlayClick(v);
                break;

        }
    }

    public void onMultiPlayClick(View view)
    {
        // Show the dialog box for inviting player or to play against a random player
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.multiplaye_mode_selector_dialog);

        Button dialogButton = (Button) dialog.findViewById(R.id.invite_friend);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                //intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 2);
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1);
                PlayModeSelection.inviter_flag= PlayModeSelection.inviteEnum.INVITER;
                getActivity().startActivityForResult(intent, 9002);
                listener.displayMultiplayerLoadingScreen();
                dialog.dismiss();
            }
        });
        dialogButton = (Button) dialog.findViewById(R.id.random_select);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

}
