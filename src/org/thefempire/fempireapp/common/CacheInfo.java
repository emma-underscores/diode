/*
 * Copyright 2010 Andrew Shu
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

package org.thefempire.fempireapp.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import org.thefempire.fempireapp.femdoms.FemdomInfo;

import android.content.Context;
import android.util.Log;


public class CacheInfo implements Serializable {
	static final long serialVersionUID = 39;
	static final String TAG = "CacheInfo";
	
	static final Object CACHE_LOCK = new Object();
	
	// timestamps for each cache
	public long femdomTime = 0;
	public long threadTime = 0;
	public long femdomListTime = 0;
	
	// the ids for the cached JSON objects
	public String femdomUrl = null;
	public String threadUrl = null;
	public ArrayList<FemdomInfo> femdomList = null;

	
	
	/**
	 * Copy the contents of an InputStream to cache file, then close the InputStream,
	 * and return a new FileInputStream to the just-written file.
	 * @param context
	 * @param in
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static FileInputStream writeThenRead(Context context, InputStream in, String filename) throws IOException {
		synchronized (CACHE_LOCK) {
			FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
	    	byte[] buf = new byte[1024];
	    	int len = 0;
	    	long total = 0;  // for debugging
	    	while ((len = in.read(buf)) > 0) {
	    		fos.write(buf, 0, len);
	    		total += len;
	    	}
	    	if (Constants.LOGGING) Log.d(TAG, total + " bytes written to cache file: " + filename);
	    	fos.close();
	    	in.close();
    	}
    	
    	// return a new InputStream
   		return context.openFileInput(filename);
	}
	
	public static boolean checkFreshfemdomCache(Context context) {
    	long time = System.currentTimeMillis();
    	long femdomTime = getCachedfemdomTime(context);
		return Math.abs(time - femdomTime) <= Constants.DEFAULT_FRESH_DURATION;
	}
    
    public static boolean checkFreshThreadCache(Context context) {
    	long time = System.currentTimeMillis();
    	long threadTime = getCachedThreadTime(context);
		return Math.abs(time - threadTime) <= Constants.DEFAULT_FRESH_DURATION;
    }
    
    public static boolean checkFreshFemdomListCache(Context context) {
    	long time = System.currentTimeMillis();
    	long femdomListTime = getCachedFemdomListTime(context);
    	return Math.abs(time - femdomListTime) <= Constants.DEFAULT_FRESH_FEMDOM_LIST_DURATION;
    }
    
    static CacheInfo getCacheInfo(Context context) throws IOException, ClassNotFoundException {
    	CacheInfo ci;
    	synchronized (CACHE_LOCK) {
	    	FileInputStream fis = context.openFileInput(Constants.FILENAME_CACHE_INFO);
	    	ObjectInputStream ois = new ObjectInputStream(fis);
	    	ci = (CacheInfo) ois.readObject();
	    	ois.close();
	    	fis.close();
    	}
    	return ci;
    }
    
    public static String getCachedFemdomUrl(Context context) {
    	try {
    		return getCacheInfo(context).femdomUrl;
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    		return null;
    	}
    }
    
    static long getCachedfemdomTime(Context context) {
    	try {
    		return getCacheInfo(context).femdomTime;
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    		return 0;
    	}
    }

    public static String getCachedThreadUrl(Context context) {
    	try {
    		return getCacheInfo(context).threadUrl;
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    		return null;
    	}
    }
    
    static long getCachedThreadTime(Context context) {
    	try {
    		return getCacheInfo(context).threadTime;
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    		return 0;
    	}
    }
    
    public static ArrayList<FemdomInfo> getCachedFemdomList(Context context) {
    	try {
    		return getCacheInfo(context).femdomList;
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    		return null;
    	}
    }
    
    static long getCachedFemdomListTime(Context context) {
    	try {
    		return getCacheInfo(context).femdomListTime;
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    		return 0;
    	}
    }
    
    @SuppressWarnings("unused")
	public
    static void invalidateAllCaches(Context context) {
    	if (!Constants.USE_COMMENTS_CACHE && !Constants.USE_THREADS_CACHE && !Constants.USE_FEMDOMS_CACHE)
    		return;
    	
    	try {
    		synchronized (CACHE_LOCK) {
		    	FileOutputStream fos = context.openFileOutput(Constants.FILENAME_CACHE_INFO, Context.MODE_PRIVATE);
		    	ObjectOutputStream oos = new ObjectOutputStream(fos);
		    	oos.writeObject(new CacheInfo());
		    	oos.close();
		    	fos.close();
		    	if (Constants.LOGGING) Log.d(TAG, "invalidateAllCaches: wrote blank CacheInfo");
    		}
    	} catch (IOException e) {
    		if (Constants.LOGGING) Log.e(TAG, "invalidateAllCaches: Error writing CacheInfo", e);
    	}
    }
    
    @SuppressWarnings("unused")
	public
    static void invalidateCachedFemdom(Context context) {
    	if (!Constants.USE_THREADS_CACHE)
    		return;
    	
    	CacheInfo ci = null;
    	try {
    		ci = getCacheInfo(context);
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    	}
		if (ci == null)
			ci = new CacheInfo();
    	
    	try {
    		synchronized (CACHE_LOCK) {
		    	FileOutputStream fos = context.openFileOutput(Constants.FILENAME_CACHE_INFO, Context.MODE_PRIVATE);
		    	ObjectOutputStream oos = new ObjectOutputStream(fos);
		    	ci.femdomUrl = null;
		    	ci.femdomTime = 0;
		    	oos.writeObject(ci);
		    	oos.close();
		    	fos.close();
    		}
    	} catch (IOException e) {
    		if (Constants.LOGGING) Log.e(TAG, "invalidateCachedfemdom: Error writing CacheInfo", e);
    	}
    }
    
    @SuppressWarnings("unused")
	public
    static void invalidateCachedThread(Context context) {
    	if (!Constants.USE_COMMENTS_CACHE)
    		return;
    	
    	CacheInfo ci = null;
    	try {
    		ci = getCacheInfo(context);
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    	}
		if (ci == null)
			ci = new CacheInfo();
    	
    	try {
    		synchronized (CACHE_LOCK) {
		    	FileOutputStream fos = context.openFileOutput(Constants.FILENAME_CACHE_INFO, Context.MODE_PRIVATE);
		    	ObjectOutputStream oos = new ObjectOutputStream(fos);
		    	ci.threadUrl = null;
		    	ci.threadTime = 0;
		    	oos.writeObject(ci);
		    	oos.close();
		    	fos.close();
    		}
    	} catch (IOException e) {
    		if (Constants.LOGGING) Log.e(TAG, "invalidateCachedThreadId: Error writing CacheInfo", e);
    	}
    }
    
    @SuppressWarnings("unused")
	public
    static void setCachedfemdomUrl(Context context, String femdomUrl) throws IOException {
    	if (!Constants.USE_THREADS_CACHE)
    		return;
    	
    	CacheInfo ci = null;
    	try {
    		ci = getCacheInfo(context);
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    	}
		if (ci == null)
			ci = new CacheInfo();
    	
		synchronized (CACHE_LOCK) {
			FileOutputStream fos = context.openFileOutput(Constants.FILENAME_CACHE_INFO, Context.MODE_PRIVATE);
	    	ObjectOutputStream oos = new ObjectOutputStream(fos);
	    	ci.femdomUrl = femdomUrl;
	    	ci.femdomTime = System.currentTimeMillis();
	    	oos.writeObject(ci);
	    	oos.close();
	    	fos.close();
		}
    }

    @SuppressWarnings("unused")
	public
    static void setCachedThreadUrl(Context context, String threadUrl) throws IOException {
    	if (!Constants.USE_COMMENTS_CACHE)
    		return;
    	
    	CacheInfo ci = null;
    	try {
    		ci = getCacheInfo(context);
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    	}
		if (ci == null)
			ci = new CacheInfo();

		synchronized (CACHE_LOCK) {
			FileOutputStream fos = context.openFileOutput(Constants.FILENAME_CACHE_INFO, Context.MODE_PRIVATE);
	    	ObjectOutputStream oos = new ObjectOutputStream(fos);
	    	ci.threadUrl = threadUrl;
	    	ci.threadTime = System.currentTimeMillis();
	    	oos.writeObject(ci);
	    	oos.close();
	    	fos.close();
		}
    }
    
    public static void setCachedfemdomList(Context context, ArrayList<FemdomInfo> femdomList) throws IOException {
    	if (!Constants.USE_FEMDOMS_CACHE)
    		return;
    	
    	CacheInfo ci = null;
    	try {
    		ci = getCacheInfo(context);
    	} catch (Exception e) {
    		if (Constants.LOGGING) Log.e(TAG, "error w/ getCacheInfo", e);
    	}
		if (ci == null)
			ci = new CacheInfo();

		synchronized (CACHE_LOCK) {
			FileOutputStream fos = context.openFileOutput(Constants.FILENAME_CACHE_INFO, Context.MODE_PRIVATE);
	    	ObjectOutputStream oos = new ObjectOutputStream(fos);
	    	ci.femdomList = femdomList;
	    	ci.femdomListTime = System.currentTimeMillis();
	    	oos.writeObject(ci);
	    	oos.close();
	    	fos.close();
		}
    }
}
