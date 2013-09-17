package org.thefempire.fempireapp.threads;

import org.thefempire.fempireapp.R;
import org.thefempire.fempireapp.settings.FempireSettings;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;


public class ThreadClickDialog extends Dialog {

	public ThreadClickDialog(Context context, FempireSettings settings) {
		super(context, settings.getDialogNoTitleTheme());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.thread_click_dialog);

		Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		LayoutParams params = getWindow().getAttributes(); 
		params.width = LayoutParams.FILL_PARENT;
		if (display.getOrientation() == Configuration.ORIENTATION_LANDSCAPE)
			params.height = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
		
		setCanceledOnTouchOutside(true);
	}

}
