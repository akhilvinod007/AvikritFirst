package com.football.facetap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.Plus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayModeSelection extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnInvitationReceivedListener,RoomUpdateListener,
        RealTimeMessageReceivedListener,RoomStatusUpdateListener,
        DBLoader.Listener,
        DBLauncher.Listener,
        multiplayerGameplay.Listener,
        PlayModeSelectionMainScreen.Listener
{

    public enum currScreenEnum {MAIN_SELECTION_FRAGMENT, DB_LOADING_FRAGMENT,MULTI_LOADING_FRAGMENT, SINGLE_PLAYER_FRAGMENT, MULTI_PLAYER_FRAGMENT,WAITING_ROOM_FRAGMENT};
    currScreenEnum currentScreen = currScreenEnum.MAIN_SELECTION_FRAGMENT;
    public static GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SELECT_PLAYERS=9002;
    private static final int RC_WAITING_ROOM=9003;
    //String mIncomingInvitationId = null;
    String opponentName;
    String mRoomId = null;
    String TAG  = "AKHIL";
    public enum inviteEnum {INVITER, INVITEE,INVALID};
    public static inviteEnum inviter_flag = inviteEnum.INVALID;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    private PlayModeSelectionMainScreen mMainMenuFragment;
    private DBLoader mDBLoaderFragment;
    private multiplayerGameplay mMultipleGamePlayFragment;
    private DBLauncher mDBLauncherFragment;// This is the single player fragment actually
    private LoadingScreenFragment mLoadingScreenFragment;
    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    public void gameLaunchMultiPlayerFragmentStart()
    {
        if(mMultipleGamePlayFragment==null)
           mMultipleGamePlayFragment = new multiplayerGameplay("Football","OFFLINE",opponentName);
        mMultipleGamePlayFragment.setListener(this);
        currentScreen= currScreenEnum.MULTI_PLAYER_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mMultipleGamePlayFragment).commit();
    }

    @Override
    public void sendReliableMessageToOpponent(byte[] data_buff) {
        for(byte b: data_buff)
          Log.d("AKHIL","   "+b);
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient,null, data_buff, mRoomId,
                    p.getParticipantId());
        }
    }


    @Override
    public void onSinglePlayGameOver() {
        currentScreen= currScreenEnum.MAIN_SELECTION_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mMainMenuFragment).commit();

    }

    @Override
    public void onMultiplePlayGameOver() {

        currentScreen= currScreenEnum.MAIN_SELECTION_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mMainMenuFragment).commit();
    }

    @Override
    public void onSinglePlayClick(String category) {
        if(mDBLoaderFragment==null)
           mDBLoaderFragment = new DBLoader(category,"S");//,null);
        mDBLoaderFragment.setListener(this);
        currentScreen= currScreenEnum.DB_LOADING_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mDBLoaderFragment).commit();

    }

    // Client used to interact with Google APIs.



    public void  onLoadingScreenQuit(){
        currentScreen= currScreenEnum.MAIN_SELECTION_FRAGMENT;
        if(mMainMenuFragment==null)
        {
            mMainMenuFragment = new PlayModeSelectionMainScreen();
            mMainMenuFragment.setListener(this);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mMainMenuFragment).commit();

    }


    public void gameLaunchSinglePlayerFragmentStart(String category, DBLoader.GameLaunchmode launchmode) {

        Log.d("AKHIL","gameLaunchSinglePlayerFragmentStart");
        currentScreen = currScreenEnum.SINGLE_PLAYER_FRAGMENT;
        if(DBLoader.GameLaunchmode.OFFLINEMODE   == launchmode)
           mDBLauncherFragment = new DBLauncher(category,"OFFLINE");
        else
           mDBLauncherFragment = new DBLauncher(category,"ONLINE");
        mDBLauncherFragment.setListener(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mDBLauncherFragment).commit();
    }

    @Override
   public void onStart(){

       if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
           Log.w(TAG,
                   "GameHelper: client was already connected on onStart()");
       } else {
           Log.d(TAG,"Connecting client.");
           mGoogleApiClient.connect();
       }
       super.onStart();
   }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_mode_selector);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
        mMainMenuFragment = new PlayModeSelectionMainScreen();
        mMainMenuFragment.setListener(this);
        getSupportFragmentManager().beginTransaction().add(R.id.play_mode_selection_root,
                mMainMenuFragment).commit();

        //View temp = findViewById(R.id.button_sign_out_play_mode_activity);
        //temp.setVisibility(View.INVISIBLE);


    }


    public void multiplayerGameStart()
    {
        currentScreen = currScreenEnum.MULTI_PLAYER_FRAGMENT;
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            opponentName=p.getDisplayName();
            Log.d("AKHIL","player name is "+p.getDisplayName());
        }
        mDBLoaderFragment = new DBLoader("Football","M");//,opponentName);
        mDBLoaderFragment.setListener(this);
        currentScreen= currScreenEnum.DB_LOADING_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mDBLoaderFragment).commit();

    }
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected() called. Sign in successful!");
        Log.d(TAG, "Sign-in succeeded.");
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);
        // Now make the sign-in button invisible and make the sign out button visible
        if(currentScreen == currScreenEnum.MAIN_SELECTION_FRAGMENT) {
            View temp_button = findViewById(R.id.button_sign_in_play_mode_activity);
            temp_button.setVisibility(View.INVISIBLE);
            temp_button = findViewById(R.id.button_sign_out_play_mode_activity);
            temp_button.setVisibility(View.VISIBLE);
        }

        Games.setViewForPopups(mGoogleApiClient, getWindow().getDecorView().findViewById(android.R.id.content));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void multiplePlayGameCancel(){
        Log.d("AKHIL","Multiple Play game cancelled by user in the main activity");
        currentScreen= currScreenEnum.MAIN_SELECTION_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mMainMenuFragment).commit();
    }
    public void onSinglePlayGameCancel(){
        Log.d("AKHIL","Single Play game cancelled by user in the main activity");
        currentScreen= currScreenEnum.MAIN_SELECTION_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mMainMenuFragment).commit();

    }

    public void onSinglePlayGamePlayAgain(){
        Log.d("AKHIL","Single Play game cancelled by user in the main activity");
        currentScreen= currScreenEnum.DB_LOADING_FRAGMENT;

        Log.d("AKHIL","I hope this works. I had previously initialized this variable");
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mDBLoaderFragment).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_SIGN_IN:
                Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                        + responseCode + ", intent=" + intent);
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (responseCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    Log.d(TAG,"Error again !!!");
                }
                break;
            case RC_SELECT_PLAYERS:
                Log.d(TAG,"RC_SELECT Players");
                handleSelectPlayersResult(responseCode, intent);
                break;
            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "Starting game (waiting room returned OK).");
                    currentScreen = currScreenEnum.DB_LOADING_FRAGMENT;
                    Log.d("AKHIL","oppoenent"+opponentName);
                    multiplayerGameStart();
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;

        }
    }

    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        //mSecondsLeft = 0;
        //stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
        }
        currentScreen = currScreenEnum.MAIN_SELECTION_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mMainMenuFragment).commit();
    }
    private void handleSelectPlayersResult(int response, Intent data) {

        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

        //final ArrayList<String> invitees = data.getStringArrayListExtra(Games.Players);

        //data.getS
        //opponentName=invitees.get(0);
        //Log.d(TAG,"Inviting   "+ opponentName);
        Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        //switchToScreen(R.id.screen_wait);
        //keepScreenOn();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
        Log.d(TAG, "Room created, waiting for it to be ready...");

    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN);
        }
    }
    /**
     * Resolve a connection failure from
     * {@link com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener#onConnectionFailed(com.google.android.gms.common.ConnectionResult)}
     *
     * @param activity the Activity trying to resolve the connection failure.
     * @param client the GoogleAPIClient instance of the Activity.
     * @param result the ConnectionResult received by the Activity.
     * @param requestCode a request code which the calling Activity can use to identify the result
     *                    of this resolution in onActivityResult.
     * @return true if the connection failure is resolved, false otherwise.
     */
    public  boolean resolveConnectionFailure(Activity activity,
                                                   GoogleApiClient client, ConnectionResult result, int requestCode
                                                   ) {

        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(activity, requestCode);
                return true;
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                client.connect();
                return false;
            }
        } else {
            // not resolvable... so show an error message
            int errorCode = result.getErrorCode();
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
                    activity, requestCode);
            if (dialog != null) {
                dialog.show();
            } else {
                // no built-in dialog: show the fallback error message
                //showAlert(activity, fallbackErrorMessage);
                Log.d(TAG,"Some thing got really fucked up here");
            }
            return false;
        }
    }


    public void displayMultiplayerLoadingScreen(){
        if(mLoadingScreenFragment==null)
           mLoadingScreenFragment = LoadingScreenFragment.newInstance();
        currentScreen= currScreenEnum.MULTI_LOADING_FRAGMENT;
        getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                mLoadingScreenFragment).commit();

    }


    ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    String mIncomingInvitationId = null;

    // Message buffer for sending messages
    byte[] mMsgBuf = new byte[10];

    public int getOpponentScore(){
        return mOpponentScore;
    }
    public void broadcastScoreToOpponent(int currentScore, boolean finalScore){
        int temp=currentScore;
        byte[] temp_buf = new byte[4];
        Log.d("AKHIL","in boradcastscore   "+ currentScore);
        // First byte in message indicates whether it's a final score or not
        mMsgBuf[0] = (byte) (finalScore ? 'F' : 'U');


        String temp_String = ""+currentScore;
        System.out.println("NUmber"+ temp_String);



        try{
            byte[] byteArray = temp_String.getBytes();
            mMsgBuf[1]=(byte)byteArray.length;
            //Log.d("AKHIL","bytes.length of the string "+);
            System.arraycopy(byteArray,0,mMsgBuf,2,byteArray.length);
        }
        catch(Exception e)
        {
            Log.d("AKHIL", "There is some error in converting the temp string to Byte Array");
        }

        int thisScore =  Integer.parseInt(new String(mMsgBuf,2,(int)mMsgBuf[1]));
        Log.d("AKHIL","Decoded Score is "+thisScore);
        //System.arraycopy(BigInteger.valueOf(currentScore),0,mMsgBuf,1,4);

        // Second byte is the score.
      //  mMsgBuf[1] = (byte) currentScore;

        // Send to every other participant.
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (finalScore) {
                // final score notification must be sent via reliable message
                Log.d("AKHIL","Sending the final score now");
                Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, mMsgBuf,
                        mRoomId, p.getParticipantId());
            } else {
                // it's an interim score notification, so we can use unreliable
                Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, mMsgBuf, mRoomId,
                        p.getParticipantId());
            }
        }
    }
    @Override
    public void onInvitationReceived(final Invitation invitation) {
        Log.d(TAG,"Invitation recevied");
        mIncomingInvitationId = invitation.getInvitationId();
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.incoming_invite_dialog);
        TextView text = (TextView) dialog.findViewById(R.id.playerName);
        text.setText(invitation.getInviter().getDisplayName()+" is inviting you to play");
        Button dialogButton = (Button) dialog.findViewById(R.id.AcceptInvite);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptInviteToRoom(mIncomingInvitationId);
                dialog.dismiss();
                //multiplayerGameStart();

            }
        });
        dialogButton = (Button) dialog.findViewById(R.id.declineInvite);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }



    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        inviter_flag=inviteEnum.INVITEE;
        //switchToScreen(R.id.screen_wait);
        //keepScreenOn();
        //resetGameVars();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());

    }

    @Override
    public void onInvitationRemoved(String s) {


        // when is this called ??
    }

    public void googlePlusLogout(View view)
    {
        mGoogleApiClient.disconnect();
        View sign_in_button = findViewById(R.id.button_sign_in_play_mode_activity);
        view.setVisibility(View.INVISIBLE);
        sign_in_button.setVisibility(View.VISIBLE);

    }

    @Override
    public void onBackPressed(){
        switch(currentScreen)
        {
            case DB_LOADING_FRAGMENT:
                mDBLoaderFragment.onCreateDialog(DBLoader.BACK_BUTTON_PRESS);
                break;
            case MAIN_SELECTION_FRAGMENT:
                Log.d("AKHIL","Back pressed on main fragment");
                Intent intent = new Intent(PlayModeSelection.this,HomeScreen.class);
                startActivity(intent);
                PlayModeSelection.this.finish();
                break;
            case SINGLE_PLAYER_FRAGMENT:
                Log.d("AKHIL","Back Button pressed on the Single player fragment");
                mDBLauncherFragment.onBackPressed();
                break;
            case MULTI_PLAYER_FRAGMENT:
                Log.d("AKHIL","Back Button pressed on the Single player fragment");
                mMultipleGamePlayFragment.onBackPressed();
                break;
            case MULTI_LOADING_FRAGMENT:
                Log.d("AKHIL","Back button pressed on Multi Loading Screen");
                // this is a bad situation, you know.. the player bailed on the opponenent . this might be very common
                // situation where connections are very slow and players get frustrated or something and actually take this option.
                leaveRoom();
                currentScreen= currScreenEnum.MAIN_SELECTION_FRAGMENT;
                getSupportFragmentManager().beginTransaction().replace(R.id.play_mode_selection_root,
                        mMainMenuFragment).commit();
                break;
            default:
                break;


        }
    }
    public void googlePlusLogin(View view){
        Log.d("AKHIL","login button pressed");
        mGoogleApiClient.connect();
    }

    int mOpponentScore;// = new HashMap<String, Integer>();
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {

        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.d("AKHIL","buf.tostring" + buf.toString());
        for(byte b : buf)
           Log.d(TAG, "Message received: " +(char) b);




        Log.d("AKHIL","buf length received is"+buf.length);
        if (buf[0] == 'F' || buf[0] == 'U') {
            int thisScore =  Integer.parseInt(new String(buf,2,(int)(buf[1])));
            Log.d("AKHIL", "Score received is "+thisScore);
            if (thisScore > mOpponentScore) {
                mOpponentScore=thisScore;
                mMultipleGamePlayFragment.updateOpponentProgress(mOpponentScore);
            }
            if ((char) buf[0] == 'F') {
                Log.d("AKHIL", "Final Score received is "+thisScore);
                mMultipleGamePlayFragment.finalOpponentScoreReceived=true;
                if(mMultipleGamePlayFragment.status == multiplayerGameplay.GameStatus.WAIT_FOR_OPPONENT_FINAL_SCORE) {
                    mMultipleGamePlayFragment.status = multiplayerGameplay.GameStatus.OVER;
                    mMultipleGamePlayFragment.onCreateDialog(0);// 0 creates Game OVer dialog box
                }
            }
        } else if (buf[0]=='I'){

            // this means you are the Invitee
            if(mDBLoaderFragment.qhs_state == DBLoader.Qhs_state_enum.QHS_INIT_INVITEE)
            {
                // You are in the right place if you are the invitee, the state machine of the HS is working properly
                // Just update the opponent question count variable and the qhs handshake varaible state
                int opponetQuestionCountReceived =  Integer.parseInt(new String(buf,2,(int)(buf[1])));
                Log.d("AKHIL", "opponetQuestionCountReceived received is "+opponetQuestionCountReceived);
                mDBLoaderFragment.opponentQuestionCount = opponetQuestionCountReceived;
                mDBLoaderFragment.qhs_state = DBLoader.Qhs_state_enum.QHS_I_MSG_RECEIVED;
            }
            else {

                Log.d("AKHIL","Something went wrong with the State machine of the HS :(");
            }
        } else if (buf[0]=='J'){
            if(mDBLoaderFragment.qhs_state == DBLoader.Qhs_state_enum.QHS_AWAIT_RESP_I_MSG){
                // Just copy the J Buff you got here into the local buffer.
                mDBLoaderFragment.JBuffer = new byte[buf.length];
                System.arraycopy(buf,0,mDBLoaderFragment.JBuffer,0,buf.length);
                mDBLoaderFragment.qhs_state = DBLoader.Qhs_state_enum.QHS_J_MSG_RECEIVED;
            }
        } else if (buf[0]=='S'){
            if(mDBLoaderFragment.qhs_state == DBLoader.Qhs_state_enum.QHS_AWAIT_ACK_OF_SEQ_SENT){
                mDBLoaderFragment.qhs_state = DBLoader.Qhs_state_enum.QHS_ACK_OF_SEQ_SENT_RECV;
            } else {
                Log.d("AKHIL","Some error ://///");
            }
        }


    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);

            return;
        }
        showWaitingRoom(room);

    }

    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }
    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.d(TAG, "*** Error: onRoomConnected, status " + statusCode);
            //showGameError();
            return;
        }
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);

    }

    @Override
    public void onLeftRoom(int i, String s) {

    }

    @Override
    public void onRoomConnected(int i, Room room) {

    }

    @Override
    public void onRoomConnecting(Room room) {

    }

    @Override
    public void onRoomAutoMatching(Room room) {

    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> strings) {

    }

    @Override
    public void onPeerDeclined(Room room, List<String> strings) {

    }

    @Override
    public void onPeerJoined(Room room, List<String> strings) {

    }

    @Override
    public void onPeerLeft(Room room, List<String> strings) {

    }

    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        // get room ID, participants and my ID:
        mRoomId = room.getRoomId();
        mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        //for (Participant p : mParticipants)
          // opponentName = p.getDisplayName();
        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
        //multiplayerGameStart();



    }

    @Override
    public void onDisconnectedFromRoom(Room room) {

    }

    @Override
    public void onPeersConnected(Room room, List<String> strings) {

    }

    @Override
    public void onPeersDisconnected(Room room, List<String> strings) {

    }

    @Override
    public void onP2PConnected(String s) {

    }

    @Override
    public void onP2PDisconnected(String s) {

    }
}
