package org.thefempire.fempireapp.threads;

import org.thefempire.fempireapp.things.ThingInfo;

import android.view.View.OnClickListener;
import android.widget.CompoundButton;


public interface ThreadClickDialogOnClickListenerFactory {
	OnClickListener getLoginOnClickListener();
	OnClickListener getLinkOnClickListener(ThingInfo thingInfo, boolean useExternalBrowser);
	OnClickListener getCommentsOnClickListener(ThingInfo thingInfo);
	CompoundButton.OnCheckedChangeListener getVoteUpOnCheckedChangeListener(ThingInfo thingInfo);
	CompoundButton.OnCheckedChangeListener getVoteDownOnCheckedChangeListener(ThingInfo thingInfo);
}