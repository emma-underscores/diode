package org.thefempire.fempireapp.comments;

import org.thefempire.fempireapp.R;
import org.thefempire.fempireapp.settings.RedditSettings;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;

/**
 * SavedCommentsDialog.java
 * @author John Zavidniak
 * The dialog that pops up when one clicks on a saved comment while
 * viewing the saved comments layout.
 */

public class SavedCommentsDialog extends Dialog
{
    
    public SavedCommentsDialog(Context context, RedditSettings settings)
    {
        super(context, settings.getDialogNoTitleTheme());
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.saved_comment_click_dialog);
        
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        if (display.getOrientation() == Configuration.ORIENTATION_LANDSCAPE)
            params.height = LayoutParams.FILL_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        
        setCanceledOnTouchOutside(true);
    }
    
}