package org.thefempire.fempireapp.user;

import java.io.InputStream;

import org.codehaus.jackson.map.JsonMappingException;
import org.thefempire.fempireapp.common.Common;
import org.thefempire.fempireapp.common.Constants;

import android.util.Log;


public class UserInfoParser {
	
	private static final String TAG = "UserInfoParser";

	public static UserInfo parseJSON(InputStream in) {
		UserListing meListing = null;
		try {
			try {
				meListing = Common.getObjectMapper().readValue(in, UserListing.class);
			} catch (JsonMappingException ex) {
				// it is not a Listing. user is not logged in.
				if (Constants.LOGGING) Log.i(TAG, "InputStream is not UserListing JSON; user is not logged in?", ex);
				return null;
			}
			
			return meListing.getData();
			
		} catch (Exception ex) {
			if (Constants.LOGGING) Log.e(TAG, "parseMeJSON", ex);
			return null;
		} finally {
			meListing = null;
		}
	}
}
