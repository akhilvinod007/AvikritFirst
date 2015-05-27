package com.football.facetap;



import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class AppLocalQuestionObject implements Parcelable {
	
	public AppLocalQuestionObject(){
		
	}
	
	private String URL;
	private String imageName;
	private String optionA;
	private String optionB;
	private String optionC;
	private String optionD;
	private String correctOption;
	private int use_count=0;
	private int downloadStatus=0;
	
	
	
	public AppLocalQuestionObject(Parcel source) {
		// TODO Auto-generated constructor stub
		Log.d("AKHIL", "ParcelData(Parcel source): time to put back parcel data");
		URL=source.readString();
		imageName=source.readString();
		optionA=source.readString();
		optionB=source.readString();
		optionC=source.readString();
		optionD=source.readString();
		correctOption=source.readString();
		use_count=source.readInt();
		downloadStatus=source.readInt();
		
	}

	/* SETTERS*/
	
	public void setCorrectOption(String rightOption){
		correctOption=rightOption;
	}
	
	public void setImageName(String imagename){
		imageName = imagename;
	}
	
	public void setOptionA( String option){
		optionA=option;
	}
	public void setOptionB( String option){
		optionB=option;
	}
	public void setOptionC(String option){
		 optionC=option;
	}
	public void setOptionD(String option){
		 optionD=option;
	}
	public void setImageServingURL( String url){
		URL=url;
	}
	
	public void setDownloadStatus(int status) {
		downloadStatus=status;
	}
	
	/* GETTERS*/
	public String getImageServingURL(){
		return URL;
	}
	public void setUseCount(int usecount){
		use_count=usecount;
	}
	public String getOptionA(){
		return optionA;
	}
	public String getOptionB(){
		return optionB;
	}
	public String getOptionC(){
		return optionC;
	}
	public String getOptionD(){
		return optionD;
	}
	public String getCorrectOption(){
		return correctOption;
	}
	
	public String getImageName(){
		return imageName;
	}
	
	public int getCount(){
		return use_count;
	}
	public int getDownloadStatus(){
		return downloadStatus;
	}
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		Log.d("AKHIL", "writing to the parcel");
		
		dest.writeString(URL);
		dest.writeString(imageName);
		dest.writeString(optionA);
		dest.writeString(optionB);
		dest.writeString(optionC);
		dest.writeString(optionD);
		dest.writeString(correctOption);
		dest.writeInt(use_count);
		dest.writeInt(downloadStatus);
		
	}
	
	public static final  Parcelable.Creator<AppLocalQuestionObject> CREATOR = new Parcelable.Creator<AppLocalQuestionObject>() {
	      public AppLocalQuestionObject createFromParcel(Parcel source) {
	            return new AppLocalQuestionObject(source);
	      }
	      public AppLocalQuestionObject[] newArray(int size) {
	            return new AppLocalQuestionObject[size];
	      }
	};
		
} 


