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

package org.thefempire.fempireapp.femdoms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.thefempire.fempireapp.R;
import org.thefempire.fempireapp.common.CacheInfo;
import org.thefempire.fempireapp.common.Common;
import org.thefempire.fempireapp.common.Constants;
import org.thefempire.fempireapp.common.FempireAppHttpClientFactory;
import org.thefempire.fempireapp.common.util.CollectionUtils;
import org.thefempire.fempireapp.common.util.Util;
import org.thefempire.fempireapp.settings.FempireSettings;


public final class PickFemdomActivity extends ListActivity {

    private static final String TAG = "PickFemdomActivity";

    // Group 1: inner
    private final Pattern MY_FEMDOMS_OUTER = Pattern.compile("YOUR FRONT PAGE FEMDOMS.*?<ul>(.*?)</ul>", Pattern.CASE_INSENSITIVE);
    // Group 3: femdom name. Repeat the matcher.find() until it fails.
    private final Pattern MY_FEMDOMS_INNER = Pattern.compile("<a(.*?)/r/(.*?)>(.+?)</a>");

    private boolean refresh = true;
    private FempireSettings mSettings = new FempireSettings();
    private HttpClient mClient = FempireAppHttpClientFactory.getGzipHttpClient();

    private PickfemdomAdapter mFemdomsAdapter;
    private ArrayList<FemdomInfo> mFemdomsList;
    private static final Object ADAPTER_LOCK = new Object();
    private EditText mEt;

    private AsyncTask<?, ?, ?> mCurrentTask = null;
    private final Object mCurrentTaskLock = new Object();

    public static final String[] DEFAULT_FEMDOMS = {
        Constants.FRONTPAGE_STRING,
        "fempireapp",
        "just_post",
        "just_edit",
        "vegan"
    };

    // A list of special femdoms that can be viewed, but cannot be used for submissions. They inherit from the Fakefemdom class
    // in the Fempiredev source, so we use the same naming here. Note: Should we add r/Random and r/Friends?
    public static final String[] FAKE_FEMDOMS = {
        Constants.FRONTPAGE_STRING,
        "all"
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFemdomsList = new ArrayList<FemdomInfo>();

        CookieSyncManager.createInstance(getApplicationContext());

        mSettings.loadFempirePreferences(this, mClient);
        setRequestedOrientation(mSettings.getRotation());
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setTheme(mSettings.getTheme());
        setContentView(R.layout.pick_femdom_view);
        registerForContextMenu(getListView());

        mFemdomsList = getCachedfemdomsList();

        if (CollectionUtils.isEmpty(mFemdomsList))
            restoreLastNonConfigurationInstance();

        if (CollectionUtils.isEmpty(mFemdomsList)) {
            new DownloadfemdomsTask().execute();
        }
        else {
            resetUI(new PickfemdomAdapter(this, mFemdomsList));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }

    @Override
    public void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Avoid having to re-download and re-parse the femdoms list
        // when rotating or opening keyboard.
        return mFemdomsList;
    }

    @SuppressWarnings("unchecked")
    private void restoreLastNonConfigurationInstance() {
        mFemdomsList = (ArrayList<FemdomInfo>) getLastNonConfigurationInstance();
    }

    void resetUI(PickfemdomAdapter adapter) {
        findViewById(R.id.loading_light).setVisibility(View.GONE);
        findViewById(R.id.loading_dark).setVisibility(View.GONE);

        synchronized (ADAPTER_LOCK) {
            if (adapter == null) {
                // Reset the list to be empty.
                mFemdomsList = new ArrayList<FemdomInfo>();
                mFemdomsAdapter = new PickfemdomAdapter(this, mFemdomsList);
            } else {
                mFemdomsAdapter = adapter;
            }
            setListAdapter(mFemdomsAdapter);
            mFemdomsAdapter.mLoading = false;
            mFemdomsAdapter.notifyDataSetChanged();  // Just in case
        }
        Common.updateListDrawables(this, mSettings.getTheme());

        // Set the EditText to do same thing as onListItemClick
        mEt = (EditText) findViewById(R.id.pick_femdom_input);
        if (mEt != null) {
            mEt.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        returnfemdom(mEt.getText().toString().trim());
                        return true;
                    }
                    return false;
                }
            });
            mEt.setFocusableInTouchMode(true);
        }
        Button goButton = (Button) findViewById(R.id.pick_femdom_button);
        if (goButton != null) {
            goButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    returnfemdom(mEt.getText().toString().trim());
                }
            });
        }

        getListView().requestFocus();
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FemdomInfo item = mFemdomsAdapter.getItem(position);
        returnfemdom(item.name);
    }

    private void returnfemdom(String femdom) {
        Intent intent = new Intent();
        intent.setData(Util.createfemdomUri(femdom.toLowerCase()));
        setResult(RESULT_OK, intent);
        finish();
    }

    private void enableLoadingScreen() {
        if (Util.isLightTheme(mSettings.getTheme())) {
            findViewById(R.id.loading_light).setVisibility(View.VISIBLE);
            findViewById(R.id.loading_dark).setVisibility(View.GONE);
        } else {
            findViewById(R.id.loading_light).setVisibility(View.GONE);
            findViewById(R.id.loading_dark).setVisibility(View.VISIBLE);
        }
        synchronized (ADAPTER_LOCK) {
            if (mFemdomsAdapter != null)
                mFemdomsAdapter.mLoading = true;
        }
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_START);
    }

    private void disableLoadingScreen() {
        findViewById(R.id.loading_dark).setVisibility(View.GONE);
        findViewById(R.id.loading_light).setVisibility(View.GONE);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_END);
    }

    class DownloadfemdomsTask extends AsyncTask<Void, Void, ArrayList<FemdomInfo>> {
        @Override
        public ArrayList<FemdomInfo> doInBackground(Void... voidz) {
            HttpEntity entity = null;
            try {

                ArrayList<FemdomInfo> femdoms = null;
                if(refresh) {

                    HttpGet request = new HttpGet(Constants.FEMPIRE_BASE_URL + "/subreddits/mine/subscriber.json?limit=100");
                    // Set timeout to 15 seconds
                    HttpParams params = request.getParams();
                    HttpConnectionParams.setConnectionTimeout(params, 15000);
                    HttpConnectionParams.setSoTimeout(params, 15000);

                    HttpResponse response = mClient.execute(request);
                    entity = response.getEntity();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readValue(entity.getContent(), JsonNode.class);
                    entity.consumeContent();

                    femdoms = new ArrayList<FemdomInfo>();
                    for(JsonNode ee : rootNode.get("data").get("children")) {
                        ee = ee.get("data");
                        FemdomInfo sr = new FemdomInfo();
                        sr.name = ee.get("display_name").getTextValue();
                        sr.description = ee.get("title").getTextValue();
                        sr.nsfw = ee.get("over18").getBooleanValue();
                        sr.subscribers = ee.get("subscribers").getIntValue();
                        sr.url = new URL(Constants.FEMPIRE_BASE_URL + ee.get("url").getTextValue());
                        sr.created = new Date((long) ee.get("created").getIntValue() * 1000);
                        femdoms.add(sr);
                    }
                    Collections.sort(femdoms);
                    // insert the frontpage at the head of the list
                    FemdomInfo fp = new FemdomInfo();
                    fp.name = Constants.FRONTPAGE_STRING;
                    femdoms.add(0, fp);
                    CacheInfo.setCachedfemdomList(getApplicationContext(), femdoms);
                    refresh = false;
                } else {
                    femdoms = getCachedfemdomsList();
                }
                return femdoms;
            }
            catch(Throwable e) {
            }
            return null;
        }

        @Override
        public void onPreExecute() {
            super.onPreExecute();
            synchronized (mCurrentTaskLock) {
                if (mCurrentTask != null) {
                    this.cancel(true);
                    return;
                }
                mCurrentTask = this;
            }
            enableLoadingScreen();
        }

        @Override
        public void onPostExecute(ArrayList<FemdomInfo> femdoms) {
            synchronized (mCurrentTaskLock) {
                mCurrentTask = null;
            }
            disableLoadingScreen();

            if (femdoms == null || femdoms.size() == 0) {
                // Need to make a copy because Arrays.asList returns List backed by original array
                mFemdomsList = new ArrayList<FemdomInfo>();
                for(String ee : DEFAULT_FEMDOMS) {
                    FemdomInfo info = new FemdomInfo();
                    info.name = ee;
                    mFemdomsList.add(info);
                }
            } else {
                mFemdomsList = femdoms;
            }
            //addFakefemdomsUnlessSuppressed();
            resetUI(new PickfemdomAdapter(PickFemdomActivity.this, mFemdomsList));
            super.onPostExecute(femdoms);
        }
    }

    private final class PickfemdomAdapter extends ArrayAdapter<FemdomInfo> {
        private LayoutInflater mInflater;
        private boolean mLoading = true;
        private int mFrequentSeparatorPos = ListView.INVALID_POSITION;
        private NumberFormat mSubscriberFormat;


        public PickfemdomAdapter(Context context, List<FemdomInfo> objects) {
            super(context, 0, objects);

            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mSubscriberFormat = NumberFormat.getInstance();
        }

        @Override
        public boolean isEmpty() {
            if (mLoading) {
                // We don't want the empty state to show when loading.
                return false;
            } else {
                return super.isEmpty();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == mFrequentSeparatorPos) {
                // We don't want the separator view to be recycled.
                return IGNORE_ITEM_VIEW_TYPE;
            }
            return super.getItemViewType(position);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            // Here view may be passed in for re-use, or we make a new one.
            if (convertView == null) {
                view = mInflater.inflate(R.layout.femdom_list_entry, null);
            } else {
                view = convertView;
            }

            FemdomInfo subject = mFemdomsAdapter.getItem(position);

            TextView text = (TextView) view.findViewById(R.id.name);
            text.setText(subject.name);

            text = (TextView) view.findViewById(R.id.age);
            if(subject.created != null)
            {
                text.setText(subject.getAgeString(PickFemdomActivity.this));
            }
            else
            {
                text.setText(null);
            }

            text = (TextView) view.findViewById(R.id.subscribers);
            if(subject.subscribers > 0)
            {
                text.setText(String.format(getString(R.string.subscriber_count_format),
                            mSubscriberFormat.format(subject.subscribers)));
            }
            else
            {
                text.setText(null);
            }

            text = (TextView) view.findViewById(R.id.nsfw);
            if(subject.nsfw == true) {
                text.setVisibility(View.VISIBLE);
            } else {
                text.setVisibility(View.GONE);
            }

            text = (TextView) view.findViewById(R.id.description);
            text.setText(subject.description);

            return view;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        ProgressDialog pdialog;

        switch (id) {
            // "Please wait"
            case Constants.DIALOG_LOADING_FEMDOMS_LIST:
                pdialog = new ProgressDialog(new ContextThemeWrapper(this, mSettings.getDialogTheme()));
                pdialog.setMessage("Loading your femdoms...");
                pdialog.setIndeterminate(true);
                pdialog.setCancelable(true);
                dialog = pdialog;
                break;
            default:
                throw new IllegalArgumentException("Unexpected dialog id "+id);
        }
        return dialog;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Common.goHome(this);
                break;
            case R.id.refresh_femdom_list:
                refresh = true;
                new DownloadfemdomsTask().execute();
                break;

            default:
                throw new IllegalArgumentException("Unexpected action value "+item.getItemId());
        }
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        final int[] myDialogs = {
            Constants.DIALOG_LOADING_FEMDOMS_LIST,
        };
        for (int dialog : myDialogs) {
            try {
                removeDialog(dialog);
            } catch (IllegalArgumentException e) {
                // Ignore.
            }
        }
    }

    /**
     * Populates the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.femdom_list, menu);
        return true;
    }

    protected ArrayList<FemdomInfo> getCachedfemdomsList(){
        ArrayList<FemdomInfo> femdoms = null;
        if (Constants.USE_FEMDOMS_CACHE) {
            if (CacheInfo.checkFreshFemdomListCache(getApplicationContext())) {
                femdoms = CacheInfo.getCachedFemdomList(getApplicationContext());
                if (Constants.LOGGING) Log.d(TAG, "cached femdom list:" + femdoms);
            }
        }
        return femdoms;
    }
}
