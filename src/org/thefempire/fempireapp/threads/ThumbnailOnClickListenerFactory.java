package org.thefempire.fempireapp.threads;

import org.thefempire.fempireapp.things.ThingInfo;

import android.app.Activity;
import android.view.View.OnClickListener;

public interface ThumbnailOnClickListenerFactory {
	OnClickListener getThumbnailOnClickListener(ThingInfo threadThingInfo, Activity activity);
}