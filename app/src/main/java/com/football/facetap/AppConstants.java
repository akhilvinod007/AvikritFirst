package com.football.facetap;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.appspot.friendzone_engine.androidapi.Androidapi;

//import javax.annotation.Nullable;

public class AppConstants {

	public static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();

    /**
     * Class instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();


    /**
     * Retrieve a Helloworld api service handle to access the API.
     */
    public static Androidapi getApiServiceHandle() {
        // Use a builder to help formulate the API request.
        Androidapi.Builder androidServiceHandle = new Androidapi.Builder(AppConstants.HTTP_TRANSPORT,
                AppConstants.JSON_FACTORY,null);
        //androidServiceHandle.setRootUrl("http://192.168.1.8:8080/_ah/api/");
        return androidServiceHandle.build();
    }
	
}
