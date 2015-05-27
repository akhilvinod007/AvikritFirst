package com.football.facetap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;








import com.appspot.friendzone_engine.androidapi.Androidapi;
import com.appspot.friendzone_engine.androidapi.Androidapi.GetImagesArray;
import com.appspot.friendzone_engine.androidapi.model.QuestionObject;
import com.appspot.friendzone_engine.androidapi.model.QuestionObjectCollection;




public class BackgroundQuestionDataFetchTask extends AsyncTask<Void, Void, QuestionObjectCollection> {
	GameDataLoadingActivity activity;
	Boolean updateToQuestionArray;
	String category;
	public BackgroundQuestionDataFetchTask(GameDataLoadingActivity activity, String category,Boolean flag ){
		this.activity=activity;
		this.category=category;
		updateToQuestionArray=flag;
	}
	@SuppressWarnings("static-access")
	@Override
	protected QuestionObjectCollection doInBackground(Void ... args) {
		// Retrieve service handle.
		Androidapi apiServiceHandle = AppConstants.getApiServiceHandle();


		ConnectivityManager connec = (ConnectivityManager)activity.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connec != null && 
				(connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) || 
				(connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)){ 

			try {
				/* 1. First see if there are images pending to be downloaded in the activity. If yes, download those images first. 
				 * 		Once images are downloaded, add update in DB, also add them to the ready List.
				 * 2. If there are no images in the pending download list/ once the images in the pending list are completed,
				 * 	  then try and download more question data. Once this question data is downloaded, put it in the DB with download status =1. 
				 * 3. Try and download  the images corresponding to the data obtained in step 2.
				 *  
				 * */

				if(activity.questionObjectArrayPendingDownload.size() !=0)
				{
					/*Step 1 of above*/
					Log.d("AKHIL",activity.questionObjectArrayPendingDownload.size()+" number of Images present in pending list at start of backgroundTask");
					Iterator<AppLocalQuestionObject> pendingListIterator =  activity.questionObjectArrayPendingDownload.iterator();
					while(pendingListIterator.hasNext())
					{
						AppLocalQuestionObject tempObject = pendingListIterator.next();
						downloadImage(tempObject.getImageServingURL(), tempObject.getImageName());
						//update DB and readyList. Remove the object from the pending array list
						DBOperations.updateDownloadStatusToDB(activity.db.getWritableDatabase(), category, tempObject.getImageName(),"2");
						pendingListIterator.remove();
						if(updateToQuestionArray)
						{
							tempObject.setDownloadStatus(2);
							activity.questionObjectArrayReady.add(tempObject);
						}

					}
					Log.d("AKHIL","already pending images are downloaded");
				}

				// the second argument here needs to be worked out properly . I think the second argument is the (total number of images present in the DB)-20
				Log.d("AKHIL","Contacting server for more image details");
				
				// get the total number of images that have status=1 or 2 in the DB
				int downloadedImageCount=DBOperations.getDownloadedImagesCount(activity.db.getReadableDatabase(), category);
				GetImagesArray getImagesCommand = apiServiceHandle.getImagesArray(category, downloadedImageCount);
				QuestionObjectCollection questionObjectCollection = getImagesCommand.execute();


				ArrayList<QuestionObject> questionObjectCollectionIteratortList = (ArrayList<QuestionObject>) questionObjectCollection.getItems(); 
				if (questionObjectCollectionIteratortList !=null ){
					Log.d("AKHIL","Server returned non null result");
					Iterator<QuestionObject> questionObjectCollectionIterator = questionObjectCollectionIteratortList.iterator();
					while(questionObjectCollectionIterator.hasNext()) {
						QuestionObject tempObject = questionObjectCollectionIterator.next();
						AppLocalQuestionObject tempAppLocalQuestionObject = new AppLocalQuestionObject();
						tempAppLocalQuestionObject.setCorrectOption(tempObject.getCorrectOption());
						tempAppLocalQuestionObject.setOptionA(tempObject.getOptionA());
						tempAppLocalQuestionObject.setOptionB(tempObject.getOptionB());
						tempAppLocalQuestionObject.setOptionC(tempObject.getOptionC());
						tempAppLocalQuestionObject.setOptionD(tempObject.getOptionD());
						tempAppLocalQuestionObject.setImageName(tempObject.getImageName());
						tempAppLocalQuestionObject.setImageServingURL(tempObject.getImageServingURL());
						tempAppLocalQuestionObject.setDownloadStatus(1);
						/*	insert the information downloaded to questionObjectArrayPendingDownload. We can further proceed with the download of data
						 * 	also insert the data into the DB
						 * 
						 * */
						activity.questionObjectArrayPendingDownload.add(tempAppLocalQuestionObject);
						DBOperations.writeToDB(tempAppLocalQuestionObject, activity.db.getWritableDatabase(), category);


					}

					Iterator<AppLocalQuestionObject> pendingListIterator =  activity.questionObjectArrayPendingDownload.iterator();
					while(pendingListIterator.hasNext())
					{
						AppLocalQuestionObject tempObject = pendingListIterator.next();
						downloadImage(tempObject.getImageServingURL(), tempObject.getImageName());
						//update DB and readyList. Remove the object from the pending array list
						Log.d("AKHIL", tempObject.getImageName()+"   has been dwonloaded");
						DBOperations.updateDownloadStatusToDB(activity.db.getWritableDatabase(), category, tempObject.getImageName(),"2");
						Log.d("AKHIL", tempObject.getImageName()+"   has been added to DB");

						pendingListIterator.remove();
						if(updateToQuestionArray){
							tempObject.setDownloadStatus(2);
							activity.questionObjectArrayReady.add(tempObject);
						}
					}
				}
				return questionObjectCollection;
			} catch (IOException e) {
				Log.d("AKHIL", "Exception during API call"+e.getMessage());
			}

		}else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED ||
				connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED ) {            
			//Not connected.        
			Log.d("AKHIL","Not connected to the internet");
		} 


		return null;
	}

	private  void downloadImage(String imageServingurl, String filename) throws IOException {

		InputStream stream = null;
		byte buffer[] = new byte[1024];
		Log.d("AKHIL","downloading     "+imageServingurl);


		try {
			//stream = getHttpConnection(url);
			File dataDir = activity.getActivity().getApplicationContext().getFilesDir();
			if(!dataDir.exists()) {
				Log.d("AKHIL","Directory doesn't exist. Creating it");
				dataDir.mkdir();
			} else {
				Log.d("AKHIL","Directory  exists");
			}
			File imageFile = new File(dataDir,filename);

			Log.d("AKHIL","image path is"+ imageFile.getAbsolutePath());

			FileOutputStream outputStream = new FileOutputStream(imageFile);

			URL url = new URL(imageServingurl);
			URLConnection connection = url.openConnection();
			ConnectivityManager connec = (ConnectivityManager)activity.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			if (connec != null && 
					(connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) || // 1 is for mobile
					(connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)){  // 0 is for wi-fi
				//You are connected, do something online.
				try {

					httpConnection.setRequestMethod("GET");
					httpConnection.setConnectTimeout(10000);
					httpConnection.connect();

					if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
						stream = httpConnection.getInputStream();
						int dataread=0;
						while((dataread=stream.read(buffer))!=-1) {
							outputStream.write(buffer,0,dataread);
						}        
						outputStream.close();
						stream.close();
					}
				} catch (Exception ex) {
					Log.d("AKHIL","could not download the image"+ ex.getMessage());
					ex.printStackTrace();
				}
				finally {
					httpConnection.disconnect();
				}
			} else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED ||
					connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED ) {            
				//Not connected.        

			} 


		} catch (IOException e1) {
			Log.d("AKHIL", "Internet connectivity issue in downloading image");
			e1.printStackTrace();
			throw e1;
		}

	}



	@Override
	protected void onPostExecute(QuestionObjectCollection questions) {

		if (questions !=null)
		{
			// here we can start the next async task of downloading the actual pics !!!


		}
		else
		{
			//Toast.makeText(getApplicationContext(), "You must be connected to the internet", Toast.LENGTH_LONG).show();
		}
	}



}
