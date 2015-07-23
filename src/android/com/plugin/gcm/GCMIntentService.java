package com.plugin.gcm;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";
	private Context gContext;
	
	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {
		gContext = context;
		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}
	public static void restartActivity(Activity act){

        Intent intent=new Intent();
        intent.setClass(act, act.getClass());
        act.startActivity(intent);
        act.finish();

}
	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			// if we are in the foreground, just surface the payload, else post it to the statusbar
            if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
                PushPlugin.sendExtras(extras);
             // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
                    createNotification(context, extras);
                    //restartActivity(gC)
                }
			}
			else {
				extras.putBoolean("foreground", false);

                // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
                    createNotification(context, extras);
                }
            }
        }
	}
	private Bitmap getBitmap(String src)
    {
		try {
	        java.net.URL url = new java.net.URL(src);
	        HttpURLConnection connection = (HttpURLConnection) url
	                .openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);
	        return myBitmap;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
    }

	public void createNotification(Context context, Bundle extras)
	{
		SharedPreferences sharedPref = context.getSharedPreferences("PUSHDATA", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);
		String message = extras.getString("message");
		String largeIcon = extras.getString("largeIcon");
		String landingPage = extras.getString("smallIcon");
		
		if(landingPage!=null){
			editor.putBoolean("IsLandingURL", true);
			editor.putString("LandingURL", landingPage);
			editor.commit();
		}else
		{
			editor.putString("LandingURL", null);
			editor.putBoolean("IsLandingURL", false);
			editor.commit();
		}
		 
		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		
//		if(landingPage!=null)
//			notificationIntent.putExtra("landingPage", landingPage);
		
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}
		
		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setDefaults(defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setSubText("")
				.setContentIntent(contentIntent)
				.setAutoCancel(true);
		 
		 if(largeIcon != null){
			 	Bitmap image = getBitmap(largeIcon);
			 	if(image!=null){
		            NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
		            bigPicStyle.bigPicture(image);
		            mBuilder.setStyle(bigPicStyle);
			 	}
		 }
			
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("It has no content.");
		}
		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}
		
		int notId = 0;
		
		try {
			notId = Integer.parseInt(extras.getString("notId"));
		}
		catch(NumberFormatException e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
		}
		catch(Exception e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
		}
		
		mNotificationManager.notify((String) appName, notId, mBuilder.build());
	}
	
	private static String getAppName(Context context)
	{
		CharSequence appName = 
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());
		
		return (String)appName;
	}
	
	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
