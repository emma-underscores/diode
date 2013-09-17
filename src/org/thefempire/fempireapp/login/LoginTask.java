package org.thefempire.fempireapp.login;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.thefempire.fempireapp.common.CacheInfo;
import org.thefempire.fempireapp.common.Common;
import org.thefempire.fempireapp.common.Constants;
import org.thefempire.fempireapp.common.FempireAppHttpClientFactory;
import org.thefempire.fempireapp.common.util.StringUtils;
import org.thefempire.fempireapp.settings.FempireSettings;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieSyncManager;

public class LoginTask extends AsyncTask<Void, Void, Boolean> {
	
	private static final String TAG = "LoginTask";
	
	protected String mUsername;
	private String mPassword;
	protected String mUserError = null;
	
	private FempireSettings mSettings;
	private HttpClient mClient;
	private Context mContext;
	
	protected LoginTask(String username, String password, FempireSettings settings, HttpClient client, Context context) {
		mUsername = username;
		mPassword = password;
		mSettings = settings;
		mClient = client;
		mContext = context;
	}
	
	@Override
	public Boolean doInBackground(Void... v) {
		return doLogin(mUsername, mPassword, mSettings, mClient, mContext);
    }
	
    /**
     * On success stores the session cookie and modhash in your FempireSettings.
     * On failure does not modify FempireSettings. 
     * Should be called from a background thread.
     * @return Error message, or null on success
     */
    private Boolean doLogin(String username, String password, FempireSettings settings, HttpClient client, Context context) {
		String status = "";
    	String userError = "Error logging in. Please try again.";
    	HttpEntity entity = null;
    	try {
    		// Construct data
    		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    		nvps.add(new BasicNameValuePair("user", username.toString()));
    		nvps.add(new BasicNameValuePair("passwd", password.toString()));
    		nvps.add(new BasicNameValuePair("api_type", "json"));
    		
            HttpPost httppost = new HttpPost(Constants.FEMPIRE_LOGIN_URL);
            httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            
            // Set timeout to 45 seconds for login
            HttpParams params = httppost.getParams();
	        HttpConnectionParams.setConnectionTimeout(params, 45000);
	        HttpConnectionParams.setSoTimeout(params, 45000);
	        
            // Perform the HTTP POST request
        	HttpResponse response = client.execute(httppost);
        	status = response.getStatusLine().toString();
        	if (!status.contains("OK")) {
        		throw new HttpException(status);
        	}
        	
        	entity = response.getEntity();
        	
        	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
        	String line = in.readLine();
        	in.close();
        	entity.consumeContent();
        	if (StringUtils.isEmpty(line)) {
        		throw new HttpException("No content returned from login POST");
        	}
        	
        	if (Constants.LOGGING) Common.logDLong(TAG, line);
        	
        	if (FempireAppHttpClientFactory.getCookieStore().getCookies().isEmpty())
        		throw new HttpException("Failed to login: No cookies");
        	
        	final JsonFactory jsonFactory = new JsonFactory();
        	final JsonParser jp = jsonFactory.createJsonParser(line);
        	
        	// Go to the errors
        	while (jp.nextToken() != JsonToken.FIELD_NAME || !Constants.JSON_ERRORS.equals(jp.getCurrentName()))
        		;
        	if (jp.nextToken() != JsonToken.START_ARRAY)
        		throw new IllegalStateException("Login: expecting errors START_ARRAY");
        	if (jp.nextToken() != JsonToken.END_ARRAY) {
	        	if (line.contains("WRONG_PASSWORD")) {
	        		userError = "Bad password.";
	        		throw new Exception("Wrong password");
	        	} else {
	        		// Could parse for error code and error description but using whole line is easier.
	        		throw new Exception(line);
	        	}
        	}

        	// Getting here means you successfully logged in.
        	// Congratulations!
        	// You are a true Fempire master!
        	
        	// Get modhash
        	while (jp.nextToken() != JsonToken.FIELD_NAME || !Constants.JSON_MODHASH.equals(jp.getCurrentName()))
        		;
        	jp.nextToken();
        	settings.setModhash(jp.getText());

        	// Could grab cookie from JSON too, but it lacks expiration date and stuff. So grab from HttpClient.
			List<Cookie> cookies = FempireAppHttpClientFactory.getCookieStore().getCookies();
        	for (Cookie c : cookies) {
        		if (c.getName().equals("fempire_session")) {
        			settings.setfemdomsessionCookie(c);
        			break;
        		}
        	}
        	settings.setUsername(username);
        	
        	CookieSyncManager.getInstance().sync();
        	CacheInfo.invalidateAllCaches(context);
        	
        	return true;

    	} catch (Exception e) {
    		mUserError = userError;
    		if (entity != null) {
    			try {
    				entity.consumeContent();
    			} catch (Exception e2) {
    				if (Constants.LOGGING) Log.e(TAG, "entity.consumeContent()", e);
    			}
    		}
    		if (Constants.LOGGING) Log.e(TAG, "doLogin()", e);
        }
        return false;
    }
    
}

