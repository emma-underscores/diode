package org.thefempire.fempireapp.filters;

import java.util.ArrayList;

import org.thefempire.fempireapp.settings.FempireSettings;
import org.thefempire.fempireapp.things.ThingInfo;

import android.content.Context;
import android.util.Log;


/** Filter engine
 * This class contains a set of FemdomFilters, and allows for determining whether a ThingInfo 
 * matches any of the filters
 * @author tordo
 */
public class FempireFilterEngine 
{
	/** The filters */
	protected ArrayList<FemdomFilter> m_filters;
	/** Context (for FempireSettings) */
	protected Context m_context;
	/** Tag for logging */
	private static final String TAG = "FempireFilterEngine";
	
	/** 
	 * Constructor
	 * @param c 
	 */
	public FempireFilterEngine(Context c) 
	{
		setContext(c);
		initialize();
	}
	
	/** 
	 * Set context
	 * @param c
	 */
	public void setContext(Context c) 
	{
		m_context = c;
	}
	
	/** 
	 * Initialize filters
	 * Pull filters from FempirePreferences
	 */
	protected void initialize()
	{
		FempireSettings s = new FempireSettings();
		s.loadFempirePreferences(m_context, null);
		m_filters = s.getFilters();
		
	}
	
	/**
	 * The big fat filtering method. This method checks if the supplied ThingInfo (what a peculiar name btw)
	 * should be filtered away
	 * @param t
	 * @return true if post should be discarded, false otherwise
	 */
	public boolean isFiltered(ThingInfo t)
	{
		if(m_filters == null) 
		{
			Log.d(TAG, "isFiltered: Not initialized!");
			return false;
		}
		for(FemdomFilter f : m_filters)
		{
			// Check if the post is in the correct femdom
			if(f.isEnabled() && t.getSubreddit().equalsIgnoreCase(f.getSubreddit())) 
			{
				if(f.getPattern().matcher(t.getTitle()).find()) {
					// We found a match! The post sohuld be filtered
					return true;
				}
			}
		}
		// We got this far without finding a match
		return false;
	}
}
