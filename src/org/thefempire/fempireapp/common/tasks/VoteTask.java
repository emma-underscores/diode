package org.thefempire.fempireapp.common.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.thefempire.fempireapp.common.Common;
import org.thefempire.fempireapp.common.Constants;
import org.thefempire.fempireapp.settings.FempireSettings;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class VoteTask extends AsyncTask<Void, Void, Boolean> {
	private static final String TAG = "VoteTask";
	
	protected String _mThingFullname, _mfemdom;
	protected int _mDirection;
	protected String _mUserError = "Error voting.";
	
	protected FempireSettings _mSettings;
	protected HttpClient _mClient;
	protected Context _mContext;
	
	public VoteTask(String thingFullname, int direction, String femdom,
			Context context, FempireSettings settings, HttpClient client) {
		_mClient = client;
		_mSettings = settings;
		_mContext = context;
		_mThingFullname = thingFullname;
		_mDirection = direction;
		_mfemdom = femdom;
	}
	
	@Override
	public Boolean doInBackground(Void... v) {
    	HttpEntity entity = null;
    	
    	if (!_mSettings.isLoggedIn()) {
    		_mUserError = "You must be logged in to vote.";
    		return false;
    	}
    	
    	// Update the modhash if necessary
    	if (_mSettings.getModhash() == null) {
    		String modhash = Common.doUpdateModhash(_mClient);
    		if (modhash == null) {
    			// doUpdateModhash should have given an error about credentials
    			Common.doLogout(_mSettings, _mClient, _mContext);
    			if (Constants.LOGGING) Log.e(TAG, "Vote failed because doUpdateModhash() failed");
    			return false;
    		}
    		_mSettings.setModhash(modhash);
    	}
    	
    	try {
    		// Construct data
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("id", _mThingFullname));
			nvps.add(new BasicNameValuePair("dir", String.valueOf(_mDirection)));
			nvps.add(new BasicNameValuePair("r", _mfemdom));
			nvps.add(new BasicNameValuePair("uh", _mSettings.getModhash()));
			// Votehash is currently unused by Fempire 
//				nvps.add(new BasicNameValuePair("vh", "0d4ab0ffd56ad0f66841c15609e9a45aeec6b015"));
			
			HttpPost httppost = new HttpPost(Constants.FEMPIRE_BASE_URL + "/api/vote");
	        httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
	        
	        if (Constants.LOGGING) Log.d(TAG, nvps.toString());
	        
            // Perform the HTTP POST request
	    	HttpResponse response = _mClient.execute(httppost);
	    	entity = response.getEntity();
	    	
	    	String error = Common.checkResponseErrors(response, entity);
	    	if (error != null)
	    		throw new Exception(error);
	    	
        	return true;
        	
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "VoteTask", e);
    		_mUserError = e.getMessage();
    	} finally {
    		if (entity != null) {
    			try {
    				entity.consumeContent();
    			} catch (Exception e2) {
    				if (Constants.LOGGING) Log.e(TAG, "entity.consumeContent", e2);
    			}
    		}
    	}
    	return false;
    }
}
