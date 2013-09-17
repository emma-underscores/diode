package org.thefempire.fempireapp;

import android.app.Application;

public class FempireApp extends Application {
	private static FempireApp application;
	
	public FempireApp(){
		application = this;
	}
	
	public static FempireApp getApplication(){
		return application;
	}
}
