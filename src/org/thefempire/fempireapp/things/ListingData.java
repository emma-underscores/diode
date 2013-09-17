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
package org.thefempire.fempireapp.things;

public class ListingData {
	private ThingListing[] children;
	private String after;
	private String before;
	private String modhash;
	
	public void setAfter(String after) {
		this.after = after;
	}
	public String getAfter() {
		return after;
	}
	
	public void setBefore(String before) {
		this.before = before;
	}
	public String getBefore() {
		return before;
	}
	
	public void setModhash(String modhash) {
		this.modhash = modhash;
	}
	public String getModhash() {
		return modhash;
	}
	
	public void setChildren(ThingListing[] children) {
		this.children = children;
	}
	public ThingListing[] getChildren() {
		return children;
	}
}