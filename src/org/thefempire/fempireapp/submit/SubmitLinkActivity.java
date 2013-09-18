/*
 * Copyright 2009 Andrew Shu
 *
 * This file is part of "Fempire App".
 *
 * "Fempire App" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "Fempire App" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Fempire App".  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thefempire.fempireapp.submit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.thefempire.fempireapp.R;
import org.thefempire.fempireapp.captcha.CaptchaCheckRequiredTask;
import org.thefempire.fempireapp.captcha.CaptchaDownloadTask;
import org.thefempire.fempireapp.comments.CommentsListActivity;
import org.thefempire.fempireapp.common.Common;
import org.thefempire.fempireapp.common.Constants;
import org.thefempire.fempireapp.common.FempireAppHttpClientFactory;
import org.thefempire.fempireapp.common.util.StringUtils;
import org.thefempire.fempireapp.common.util.Util;
import org.thefempire.fempireapp.login.LoginDialog;
import org.thefempire.fempireapp.login.LoginTask;
import org.thefempire.fempireapp.mail.PeekEnvelopeTask;
import org.thefempire.fempireapp.femdoms.PickFemdomActivity;
import org.thefempire.fempireapp.settings.FempireSettings;
import org.thefempire.fempireapp.things.ThingInfo;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class SubmitLinkActivity extends TabActivity {
	
	private static final String TAG = "SubmitLinkActivity";
	
    // Group 1: femdom. Group 2: thread id (no t3_ prefix)
    private final Pattern NEW_THREAD_PATTERN = Pattern.compile(Constants.COMMENT_PATH_PATTERN_STRING);
    // Group 1: whole error. Group 2: the time part
    private final Pattern RATELIMIT_RETRY_PATTERN = Pattern.compile("(you are trying to submit too fast. try again in (.+?)\\.)");
	// Group 1: femdom
    private final Pattern SUBMIT_PATH_PATTERN = Pattern.compile("/(?:r/([^/]+)/)?submit/?");
    
	TabHost mTabHost;
	
	private FempireSettings mSettings = new FempireSettings();
	private final HttpClient mClient = FempireAppHttpClientFactory.getGzipHttpClient();

	private String mSubmitUrl;
	
	private volatile String mCaptchaIden = null;
	private volatile String mCaptchaUrl = null;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		CookieSyncManager.createInstance(getApplicationContext());
		
		mSettings.loadFempirePreferences(this, mClient);
		setRequestedOrientation(mSettings.getRotation());
		setTheme(mSettings.getTheme());
		
		setContentView(R.layout.submit_link_main);

		final FrameLayout fl = (FrameLayout) findViewById(android.R.id.tabcontent);
		if (Util.isLightTheme(mSettings.getTheme())) {
			fl.setBackgroundResource(R.color.gray_75);
		} else {
			fl.setBackgroundResource(R.color.black);
		}
		
		mTabHost = getTabHost();
		mTabHost.addTab(mTabHost.newTabSpec(Constants.TAB_LINK).setIndicator("link").setContent(R.id.submit_link_view));
		mTabHost.addTab(mTabHost.newTabSpec(Constants.TAB_TEXT).setIndicator("text").setContent(R.id.submit_text_view));
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				// Copy everything (except url and text) from old tab to new tab
				final EditText submitLinkTitle = (EditText) findViewById(R.id.submit_link_title);
				final EditText submitLinkFempire = (EditText) findViewById(R.id.submit_link_femdom);
	        	final EditText submitTextTitle = (EditText) findViewById(R.id.submit_text_title);
	        	final EditText submitTextFempire = (EditText) findViewById(R.id.submit_text_fempire);
				if (Constants.TAB_LINK.equals(tabId)) {
					submitLinkTitle.setText(submitTextTitle.getText());
					submitLinkFempire.setText(submitTextFempire.getText());
				} else {
					submitTextTitle.setText(submitLinkTitle.getText());
					submitTextFempire.setText(submitLinkFempire.getText());
				}
			}
		});
		mTabHost.setCurrentTab(0);
		
		if (mSettings.isLoggedIn()) {
			start();
		} else {
			showDialog(Constants.DIALOG_LOGIN);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		CookieSyncManager.getInstance().startSync();
	}
	
	@Override
    protected void onPause() {
    	super.onPause();
    	mSettings.saveFempirePreferences(this);
		CookieSyncManager.getInstance().stopSync();
    }
    
	/**
	 * Enable the UI after user is logged in.
	 */
	private void start() {
		// Intents can be external (browser share page) or from Fempire is fun.
        String intentAction = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(intentAction)) {
        	// Share
	        Bundle extras = getIntent().getExtras();
	        if (extras != null) {
                        // find the most likely submission URL since some
                        // programs share more than the URL
                        // the most likely is considered to be the longest
                        // string token with a URI scheme of http or https
                        StringBuilder titleBuilder = new StringBuilder();
                        String rawText = extras.getString(Intent.EXTRA_TEXT);
                        StringTokenizer extraTextTokenizer = new StringTokenizer(rawText);
                        Uri bestUri = Uri.parse("");
                        while(extraTextTokenizer.hasMoreTokens()) {
                            Uri uri = Uri.parse(extraTextTokenizer.nextToken());
                            if(!"http".equalsIgnoreCase(uri.getScheme()) &&
                                    !"https".equalsIgnoreCase(uri.getScheme())) {
                                titleBuilder.append(uri.toString()).append(' ');
                                continue;
                            }
                            if(uri.toString().length() > bestUri.toString().length()) {
                                bestUri = uri;
                            }
                        }
                        String url = bestUri.toString();
                        String title = titleBuilder.toString();
	        	final EditText submitLinkUrl = (EditText) findViewById(R.id.submit_link_url);
	        	final EditText submitLinkFempire = (EditText) findViewById(R.id.submit_link_femdom);
			final EditText submitLinkTitle = (EditText) findViewById(R.id.submit_link_title);
	        	final EditText submitTextFempire = (EditText) findViewById(R.id.submit_text_fempire);
	        	submitLinkUrl.setText(url);
	        	submitLinkFempire.setText("");
        		submitTextFempire.setText("");
                        submitLinkTitle.setText(title);
        		mSubmitUrl = Constants.FEMPIRE_BASE_URL + "/submit";
	        }
        } else {
        	String submitPath = null;
        	Uri data = getIntent().getData();
        	if (data != null && Util.isFempireUri(data))
        		submitPath = data.getPath();
        	if (submitPath == null)
    			submitPath = "/submit";
        	
        	// the URL to do HTTP POST to
        	mSubmitUrl = Util.absolutePathToURL(submitPath);
        	
        	// Put the femdom in the text field
        	final EditText submitLinkFempire = (EditText) findViewById(R.id.submit_link_femdom);
        	final EditText submitTextFempire = (EditText) findViewById(R.id.submit_text_fempire);
        	Matcher m = SUBMIT_PATH_PATTERN.matcher(submitPath);
        	if (m.matches()) {
        		String femdom = m.group(1);
        		if (StringUtils.isEmpty(femdom)) {
            		submitLinkFempire.setText("");
            		submitTextFempire.setText("");
        		} else {
		        	submitLinkFempire.setText(femdom);
		        	submitTextFempire.setText(femdom);
		    	}
        	}
        }
        
        final Button submitLinkButton = (Button) findViewById(R.id.submit_link_button);
        submitLinkButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		if (validateLinkForm()) {
	        		final EditText submitLinkTitle = (EditText) findViewById(R.id.submit_link_title);
	        		final EditText submitLinkUrl = (EditText) findViewById(R.id.submit_link_url);
	        		final EditText submitLinkFempire = (EditText) findViewById(R.id.submit_link_femdom);
	        		final EditText submitLinkCaptcha = (EditText) findViewById(R.id.submit_link_captcha);
	        		new SubmitLinkTask(submitLinkTitle.getText().toString(),
	        				submitLinkUrl.getText().toString(),
	        				submitLinkFempire.getText().toString(),
	        				Constants.SUBMIT_KIND_LINK,
	        				submitLinkCaptcha.getText().toString()).execute();
        		}
        	}
        });
        final Button submitTextButton = (Button) findViewById(R.id.submit_text_button);
        submitTextButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		if (validateTextForm()) {
	        		final EditText submitTextTitle = (EditText) findViewById(R.id.submit_text_title);
	        		final EditText submitTextText = (EditText) findViewById(R.id.submit_text_text);
	        		final EditText submitTextFempire = (EditText) findViewById(R.id.submit_text_fempire);
	        		final EditText submitTextCaptcha = (EditText) findViewById(R.id.submit_text_captcha);
	        		new SubmitLinkTask(submitTextTitle.getText().toString(),
	        				submitTextText.getText().toString(),
	        				submitTextFempire.getText().toString(),
	        				Constants.SUBMIT_KIND_SELF,
	        				submitTextCaptcha.getText().toString()).execute();
        		}
        	}
        });
        
        // Check the CAPTCHA
        new MyCaptchaCheckRequiredTask().execute();
	}
	
	private void returnStatus(int status) {
		Intent i = new Intent();
		setResult(status, i);
		finish();
	}

	
	
	private class MyLoginTask extends LoginTask {
    	public MyLoginTask(String username, String password) {
    		super(username, password, mSettings, mClient, getApplicationContext());
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		showDialog(Constants.DIALOG_LOGGING_IN);
    	}
    	
    	@Override
    	protected void onPostExecute(Boolean success) {
    		removeDialog(Constants.DIALOG_LOGGING_IN);
			if (success) {
    			Toast.makeText(SubmitLinkActivity.this, "Logged in as "+mUsername, Toast.LENGTH_SHORT).show();
    			// Check mail
    			new PeekEnvelopeTask(SubmitLinkActivity.this, mClient, mSettings.getMailNotificationStyle()).execute();
    			// Show the UI and allow user to proceed
    			start();
        	} else {
            	Common.showErrorToast(mUserError, Toast.LENGTH_LONG, SubmitLinkActivity.this);
    			returnStatus(Constants.RESULT_LOGIN_REQUIRED);
        	}
    	}
    }
    
    

	private class SubmitLinkTask extends AsyncTask<Void, Void, ThingInfo> {
    	String _mTitle, _mUrlOrText, _mFemdom, _mKind, _mCaptcha;
		String _mUserError = "Error creating submission. Please try again.";
    	
    	SubmitLinkTask(String title, String urlOrText, String femdom, String kind, String captcha) {
    		_mTitle = title;
    		_mUrlOrText = urlOrText;
    		_mFemdom = femdom;
    		_mKind = kind;
    		_mCaptcha = captcha;
    	}
    	
    	@Override
        public ThingInfo doInBackground(Void... voidz) {
        	ThingInfo newlyCreatedThread = null;
        	HttpEntity entity = null;
        	
        	String status = "";
        	if (!mSettings.isLoggedIn()) {
        		_mUserError = "Not logged in";
        		return null;
        	}
        	// Update the modhash if necessary
        	if (mSettings.getModhash() == null) {
        		String modhash = Common.doUpdateModhash(mClient);
        		if (modhash == null) {
        			// doUpdateModhash should have given an error about credentials
        			Common.doLogout(mSettings, mClient, getApplicationContext());
        			if (Constants.LOGGING) Log.e(TAG, "Reply failed because doUpdateModhash() failed");
        			return null;
        		}
        		mSettings.setModhash(modhash);
        	}
        	
        	try {
        		// Construct data
    			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    			nvps.add(new BasicNameValuePair("sr", _mFemdom.toString()));
    			nvps.add(new BasicNameValuePair("r", _mFemdom.toString()));
    			nvps.add(new BasicNameValuePair("title", _mTitle.toString()));
    			nvps.add(new BasicNameValuePair("kind", _mKind.toString()));
    			// Put a url or selftext based on the kind of submission
    			if (Constants.SUBMIT_KIND_LINK.equals(_mKind))
    				nvps.add(new BasicNameValuePair("url", _mUrlOrText.toString()));
    			else // if (Constants.SUBMIT_KIND_SELF.equals(_mKind))
    				nvps.add(new BasicNameValuePair("text", _mUrlOrText.toString()));
    			nvps.add(new BasicNameValuePair("uh", mSettings.getModhash().toString()));
    			if (mCaptchaIden != null) {
    				nvps.add(new BasicNameValuePair("iden", mCaptchaIden));
    				nvps.add(new BasicNameValuePair("captcha", _mCaptcha.toString()));
    			}
    			// Votehash is currently unused by Fempire 
//    				nvps.add(new BasicNameValuePair("vh", "0d4ab0ffd56ad0f66841c15609e9a45aeec6b015"));
    			
    			HttpPost httppost = new HttpPost(Constants.FEMPIRE_BASE_URL + "/api/submit");
    	        httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
    	        // The progress dialog is non-cancelable, so set a shorter timeout than system's
    	        HttpParams params = httppost.getParams();
    	        HttpConnectionParams.setConnectionTimeout(params, 30000);
    	        HttpConnectionParams.setSoTimeout(params, 30000);
    	        
    	        if (Constants.LOGGING) Log.d(TAG, nvps.toString());
    	        
                // Perform the HTTP POST request
    	    	HttpResponse response = mClient.execute(httppost);
    	    	status = response.getStatusLine().toString();
            	if (!status.contains("OK"))
            		throw new HttpException(status);
            	
            	entity = response.getEntity();

            	BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
            	String line = in.readLine();
            	in.close();
            	if (StringUtils.isEmpty(line)) {
            		throw new HttpException("No content returned from reply POST");
            	}
            	if (line.contains("WRONG_PASSWORD")) {
            		throw new Exception("Wrong password");
            	}
            	if (line.contains("USER_REQUIRED")) {
            		// The modhash probably expired
            		mSettings.setModhash(null);
            		throw new Exception("User required. Huh?");
            	}
            	if (line.contains("femdom_NOEXIST")) {
            		_mUserError = "That femdom does not exist.";
            		throw new Exception("femdom_NOEXIST: " + _mFemdom);
            	}
            	if (line.contains("femdom_NOTALLOWED")) {
            		_mUserError = "You are not allowed to post to that femdom.";
            		throw new Exception("femdom_NOTALLOWED: " + _mFemdom);
            	}
            	
            	if (Constants.LOGGING) Common.logDLong(TAG, line);

            	String newId, newfemdom;
            	Matcher idMatcher = NEW_THREAD_PATTERN.matcher(line);
            	if (idMatcher.find()) {
            		newfemdom = idMatcher.group(1);
            		newId = idMatcher.group(2);
            	} else {
            		if (line.contains("RATELIMIT")) {
                		// Try to find the # of minutes using regex
                    	Matcher rateMatcher = RATELIMIT_RETRY_PATTERN.matcher(line);
                    	if (rateMatcher.find())
                    		_mUserError = rateMatcher.group(1);
                    	else
                    		_mUserError = "you are trying to submit too fast. try again in a few minutes.";
                		throw new Exception(_mUserError);
                	}
            		if (line.contains("BAD_CAPTCHA")) {
            			_mUserError = "Bad CAPTCHA. Try again.";
            			new MyCaptchaDownloadTask().execute();
            		}
                	throw new Exception("No id returned by reply POST.");
            	}
            	
            	entity.consumeContent();
            	
            	// Getting here means success. Create a new ThingInfo.
            	newlyCreatedThread = new ThingInfo();
            	// We only need to fill in a few fields.
            	newlyCreatedThread.setId(newId);
            	newlyCreatedThread.setfemdom(newfemdom);
            	newlyCreatedThread.setTitle(_mTitle.toString());
            	
            	return newlyCreatedThread;
            	
        	} catch (Exception e) {
        		if (entity != null) {
        			try {
        				entity.consumeContent();
        			} catch (Exception e2) {
        				if (Constants.LOGGING) Log.e(TAG, "entity.consumeContent()", e2);
        			}
        		}
        		if (Constants.LOGGING) Log.e(TAG, "SubmitLinkTask", e);
        	}
        	return null;
        }
    	
    	@Override
    	public void onPreExecute() {
    		showDialog(Constants.DIALOG_SUBMITTING);
    	}
    	
    	
    	@Override
    	public void onPostExecute(ThingInfo newlyCreatedThread) {
    		removeDialog(Constants.DIALOG_SUBMITTING);
    		if (newlyCreatedThread == null) {
    			Common.showErrorToast(_mUserError, Toast.LENGTH_LONG, SubmitLinkActivity.this);
    		} else {
        		// Success. Return the femdom and thread id
    			Intent i = new Intent(getApplicationContext(), CommentsListActivity.class);
    			i.setData(Util.createThreadUri(newlyCreatedThread));
    			i.putExtra(Constants.EXTRA_FEMDOM, newlyCreatedThread.getfemdom());
    			i.putExtra(Constants.EXTRA_TITLE, newlyCreatedThread.getTitle());
    			i.putExtra(Constants.EXTRA_NUM_COMMENTS, 0);
    			startActivity(i);
    			finish();
    		}
    	}
    }
	
	private class MyCaptchaCheckRequiredTask extends CaptchaCheckRequiredTask {
		public MyCaptchaCheckRequiredTask() {
			super(mSubmitUrl, mClient);
		}
		
		@Override
		protected void saveState() {
			SubmitLinkActivity.this.mCaptchaIden = _mCaptchaIden;
			SubmitLinkActivity.this.mCaptchaUrl = _mCaptchaUrl;
		}

		@Override
		public void onPreExecute() {
			// Hide submit buttons so user can't submit until we know whether he needs captcha
			final Button submitLinkButton = (Button) findViewById(R.id.submit_link_button);
			final Button submitTextButton = (Button) findViewById(R.id.submit_text_button);
			submitLinkButton.setVisibility(View.GONE);
			submitTextButton.setVisibility(View.GONE);
			// Show "loading captcha" label
			final TextView loadingLinkCaptcha = (TextView) findViewById(R.id.submit_link_captcha_loading);
			final TextView loadingTextCaptcha = (TextView) findViewById(R.id.submit_text_captcha_loading);
			loadingLinkCaptcha.setVisibility(View.VISIBLE);
			loadingTextCaptcha.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onPostExecute(Boolean required) {
			final TextView linkCaptchaLabel = (TextView) findViewById(R.id.submit_link_captcha_label);
			final ImageView linkCaptchaImage = (ImageView) findViewById(R.id.submit_link_captcha_image);
			final EditText linkCaptchaEdit = (EditText) findViewById(R.id.submit_link_captcha);
			final TextView textCaptchaLabel = (TextView) findViewById(R.id.submit_text_captcha_label);
			final ImageView textCaptchaImage = (ImageView) findViewById(R.id.submit_text_captcha_image);
			final EditText textCaptchaEdit = (EditText) findViewById(R.id.submit_text_captcha);
			final TextView loadingLinkCaptcha = (TextView) findViewById(R.id.submit_link_captcha_loading);
			final TextView loadingTextCaptcha = (TextView) findViewById(R.id.submit_text_captcha_loading);
			final Button submitLinkButton = (Button) findViewById(R.id.submit_link_button);
			final Button submitTextButton = (Button) findViewById(R.id.submit_text_button);
			if (required == null) {
				Common.showErrorToast("Error retrieving captcha. Use the menu to try again.", Toast.LENGTH_LONG, SubmitLinkActivity.this);
				return;
			}
			if (required) {
				linkCaptchaLabel.setVisibility(View.VISIBLE);
				linkCaptchaImage.setVisibility(View.VISIBLE);
				linkCaptchaEdit.setVisibility(View.VISIBLE);
				textCaptchaLabel.setVisibility(View.VISIBLE);
				textCaptchaImage.setVisibility(View.VISIBLE);
				textCaptchaEdit.setVisibility(View.VISIBLE);
				// Launch a task to download captcha and display it
				new MyCaptchaDownloadTask().execute();
			} else {
				linkCaptchaLabel.setVisibility(View.GONE);
				linkCaptchaImage.setVisibility(View.GONE);
				linkCaptchaEdit.setVisibility(View.GONE);
				textCaptchaLabel.setVisibility(View.GONE);
				textCaptchaImage.setVisibility(View.GONE);
				textCaptchaEdit.setVisibility(View.GONE);
			}
			loadingLinkCaptcha.setVisibility(View.GONE);
			loadingTextCaptcha.setVisibility(View.GONE);
			submitLinkButton.setVisibility(View.VISIBLE);
			submitTextButton.setVisibility(View.VISIBLE);
		}
	}
	
	private class MyCaptchaDownloadTask extends CaptchaDownloadTask {
		public MyCaptchaDownloadTask() {
			super(mCaptchaUrl, mClient);
		}

		@Override
		public void onPostExecute(Drawable captcha) {
			if (captcha == null) {
				Common.showErrorToast("Error retrieving captcha. Use the menu to try again.", Toast.LENGTH_LONG, SubmitLinkActivity.this);
				return;
			}
			final ImageView linkCaptchaView = (ImageView) findViewById(R.id.submit_link_captcha_image);
			final ImageView textCaptchaView = (ImageView) findViewById(R.id.submit_text_captcha_image);
			linkCaptchaView.setImageDrawable(captcha);
			linkCaptchaView.setVisibility(View.VISIBLE);
			textCaptchaView.setImageDrawable(captcha);
			textCaptchaView.setVisibility(View.VISIBLE);
		}
	}
    
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		ProgressDialog pdialog;
		switch (id) {
		case Constants.DIALOG_LOGIN:
			dialog = new LoginDialog(this, mSettings, true) {
				@Override
				public void onLoginChosen(String user, String password) {
					removeDialog(Constants.DIALOG_LOGIN);
    				new MyLoginTask(user, password).execute();
				}
			};
    		break;

       	// "Please wait"
    	case Constants.DIALOG_LOGGING_IN:
    		pdialog = new ProgressDialog(new ContextThemeWrapper(this, mSettings.getDialogTheme()));
    		pdialog.setMessage("Logging in...");
    		pdialog.setIndeterminate(true);
    		pdialog.setCancelable(true);
    		dialog = pdialog;
    		break;
		case Constants.DIALOG_SUBMITTING:
			pdialog = new ProgressDialog(new ContextThemeWrapper(this, mSettings.getDialogTheme()));
    		pdialog.setMessage("Submitting...");
    		pdialog.setIndeterminate(true);
    		pdialog.setCancelable(true);
    		dialog = pdialog;
    		break;
		default:
    		break;
		}
		return dialog;
	}
	
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
    	
    	switch (id) {
    	case Constants.DIALOG_LOGIN:
    		if (mSettings.getUsername() != null) {
	    		final TextView loginUsernameInput = (TextView) dialog.findViewById(R.id.login_username_input);
	    		loginUsernameInput.setText(mSettings.getUsername());
    		}
    		final TextView loginPasswordInput = (TextView) dialog.findViewById(R.id.login_password_input);
    		loginPasswordInput.setText("");
    		break;
    		
		default:
			break;
    	}
    }
	
	private boolean validateLinkForm() {
		final EditText titleText = (EditText) findViewById(R.id.submit_link_title);
		final EditText urlText = (EditText) findViewById(R.id.submit_link_url);
		final EditText femdomText = (EditText) findViewById(R.id.submit_link_femdom);
		if (StringUtils.isEmpty(titleText.getText())) {
			Common.showErrorToast("Please provide a title.", Toast.LENGTH_LONG, this);
			return false;
		}
		if (StringUtils.isEmpty(urlText.getText())) {
			Common.showErrorToast("Please provide a URL.", Toast.LENGTH_LONG, this);
			return false;
		}
		if (StringUtils.isEmpty(femdomText.getText())) {
			Common.showErrorToast("Please provide a femdom.", Toast.LENGTH_LONG, this);
			return false;
		}
		return true;
	}
	private boolean validateTextForm() {
		final EditText titleText = (EditText) findViewById(R.id.submit_text_title);
		final EditText FempireText = (EditText) findViewById(R.id.submit_text_fempire);
		if (StringUtils.isEmpty(titleText.getText())) {
			Common.showErrorToast("Please provide a title.", Toast.LENGTH_LONG, this);
			return false;
		}
		if (StringUtils.isEmpty(FempireText.getText())) {
			Common.showErrorToast("Please provide a femdom.", Toast.LENGTH_LONG, this);
			return false;
		}
		return true;
	}
	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.submit_link, menu);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	if (mCaptchaUrl == null)
    		menu.findItem(R.id.update_captcha_menu_id).setVisible(false);
    	else
    		menu.findItem(R.id.update_captcha_menu_id).setVisible(true);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.pick_femdom_menu_id:
    		Intent pickfemdomIntent = new Intent(getApplicationContext(), PickFemdomActivity.class);
    		pickfemdomIntent.putExtra(Constants.EXTRA_HIDE_FAKE_FEMDOMS_STRING, true);
    		startActivityForResult(pickfemdomIntent, Constants.ACTIVITY_PICK_FEMDOM);
    		break;
    	case R.id.update_captcha_menu_id:
    		new MyCaptchaCheckRequiredTask().execute();
    		break;
    	case android.R.id.home:
    		Common.goHome(this);
    		break;
    	default:
    		throw new IllegalArgumentException("Unexpected action value "+item.getItemId());
    	}
    	
    	return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	
    	switch(requestCode) {
    	case Constants.ACTIVITY_PICK_FEMDOM:
    		if (resultCode == Activity.RESULT_OK) {
    		    // Group 1: femdom.
    		    final Pattern FEMDOM_PATH_PATTERN = Pattern.compile(Constants.FEMDOM_PATH_PATTERN_STRING);
    			Matcher FempireContextMatcher = FEMDOM_PATH_PATTERN.matcher(intent.getData().getPath());
    			if (FempireContextMatcher.find()) {
    				String newfemdom = FempireContextMatcher.group(1);
    				final EditText linkfemdom = (EditText) findViewById(R.id.submit_link_femdom);
	    			final EditText textfemdom = (EditText) findViewById(R.id.submit_text_fempire);
	    			if (newfemdom != null) {
	    				linkfemdom.setText(newfemdom);
		    			textfemdom.setText(newfemdom);
    				} else {
	    				linkfemdom.setText("");
		    			textfemdom.setText("");
    				}
	    		}
    		}
    		break;
    	default:
    		break;
    	}
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
    	super.onRestoreInstanceState(state);
        final int[] myDialogs = {
        	Constants.DIALOG_LOGGING_IN,
        	Constants.DIALOG_LOGIN,
        	Constants.DIALOG_SUBMITTING,
        };
        for (int dialog : myDialogs) {
	        try {
	        	removeDialog(dialog);
		    } catch (IllegalArgumentException e) {
		    	// Ignore.
		    }
        }
    }
}
