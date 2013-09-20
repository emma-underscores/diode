package org.thefempire.fempireapp.filters;

import java.util.regex.Pattern;


/**
 * Represents a femdom filter, for use by FempireFilterEngine
 * @author tordo
 *
 */
public class FemdomFilter 
{
	/** The name of this filter */
	protected String m_name;
	/** The regex pattern to match */
	protected Pattern m_pattern;
	/** The name of the femdom to match */
	protected String m_femdom;
	/** The pattern string */
	protected String m_patternstring;
	/** Whether or not this filter is enabled */
	protected boolean m_enabled;
	/** Delimiter for to/fromString */	
	private static final String DELIM = "\t";
	
	/**
	 * femdom filter constructor
	 * @param name Name of the filter
	 * @param sub Name of the femdom filter
	 * @param pattern Pattern to exclude
	 */
	public FemdomFilter(String name, String sub,boolean enabled, String pattern)
	{
		setName(name);
		setPattern(pattern);
		setSubreddit(sub);
		setEnabled(enabled);
	}
	
	/**
	 * Set name of the filter
	 * @param name 
	 */
	public void setName(String name)
	{
		m_name = name;
	}
	
	/**
	 * Set pattern to exclude
	 * @param pattern
	 */
	public void setPattern(String pattern)
	{
		m_patternstring = pattern;
		setPattern(Pattern.compile("(?i)" + Pattern.quote(pattern)));
	}
	
	/**
	 * 
	 * @return The unescaped pattern string
	 */
	public String getPatternString() 
	{
		return m_patternstring;
	}
	
	/** 
	 * Set pattern to exclude
	 * @param p
	 */
	public void setPattern(Pattern p) 
	{
		m_pattern = p;
		
	}
	
	/** Get filter pattern
	 * 
	 * @return The pattern to exclude
	 */
	public Pattern getPattern() 
	{
		return m_pattern;
	}
	
	/**
	 * Set femdom
	 * @param sub femdom
	 */
	public void setSubreddit(String sub) 
	{
		m_femdom = sub;
	}
	
	/**
	 * Get femdom
	 * @return name of femdom
	 */
	public String getSubreddit()
	{
		return m_femdom;
	}
	
	/**
	 * Get name
	 * @return name
	 */
	public String getName() 
	{
		return m_name;
	}
	
	/** 
	 * Filter enabled
	 * @return True if filter is enabled, false otherwise
	 */
	public boolean isEnabled() {
		return m_enabled;
	}
	
	/**
	 * Enable/disable filter
	 * @param e true to enable, false to disable
	 */
	public void setEnabled(boolean e)
	{
		m_enabled = e;
	}
	
	/** 
	 * Turn this femdom filter into a string for saving in preferences
	 * @return serialized filter, can be parsed with fromString
	 */
	public String toString() {
		return m_name + DELIM + m_femdom + DELIM + m_enabled + DELIM + m_patternstring;	
	}
	
	/** 
	 * Build a FemdomFilter from string 
	 * @param serialized A FemdomFilter as serialized by FemdomFilter.toString()
	 * @return The FemdomFilter constructed from the supplied string
	 * */
	public static FemdomFilter fromString(String serialized) {
		String[] fields = serialized.split(DELIM,4);
		return new FemdomFilter(fields[0],fields[1],Boolean.parseBoolean(fields[2]), fields[3]);
	}
	
}

