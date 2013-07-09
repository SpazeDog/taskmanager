/*
 * This file is part of the TaskManager Project: https://github.com/spazedog/taskmanager
 *  
 * Copyright (c) 2013 Daniel Bergløv
 *
 * TaskManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * TaskManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public License
 * along with TaskManager. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.lib.taskmanager;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

public class Utils {
	public static Boolean LOG = true;
	
	public static void log(String aTag, String aMethod, String aMessage) {
		if (LOG) {
			Log.i("TaskManager." + aTag + "::" + aMethod, aMessage);
		}
	}
	
	public static IManager getManager(android.support.v4.app.Fragment aFragment) {
		return buildSupportManager(aFragment, true);
	}
	
	public static IManager getManager(android.support.v4.app.FragmentActivity aActivity) {
		return buildSupportManager(aActivity, false);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static IManager getManager(android.app.Fragment aFragment) {
		return buildSupportManager(aFragment, true);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static IManager getManager(android.app.Activity aActivity) {
		return buildSupportManager(aActivity, false);
	}

	private static IManager buildSupportManager(Object object, Boolean fragment) {
		android.support.v4.app.FragmentManager fm = fragment ? ((android.support.v4.app.Fragment) object).getChildFragmentManager() : ((android.support.v4.app.FragmentActivity) object).getSupportFragmentManager();
		
		IManager lManager;
		
		if ((lManager = ((IManager) fm.findFragmentByTag( (fragment ? SupportChildManager.TAG : SupportTaskManager.TAG) ))) == null) {
			if (fragment) {
				log("Utils", "buildSupportManager", "Attching a new ChildManager to " + ((android.support.v4.app.Fragment) object).getClass().getName());
				
				buildSupportManager(((android.support.v4.app.Fragment) object).getActivity(), false);
				
				fm.beginTransaction().add((android.support.v4.app.Fragment) (lManager = new SupportChildManager()), SupportChildManager.TAG).commit();
				
			} else {
				log("Utils", "buildSupportManager", "Attching a new TaskManager to " + ((android.support.v4.app.FragmentActivity) object).getClass().getName());
				
				fm.beginTransaction().add((android.support.v4.app.Fragment) (lManager = new SupportTaskManager()), SupportTaskManager.TAG).commit();
			}
		}
		
		return lManager;
	}
	
	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static IManager buildManager(Object object, Boolean fragment) {
		android.app.FragmentManager fm = fragment ? ((android.app.Fragment) object).getChildFragmentManager() : ((android.app.Activity) object).getFragmentManager();
		
		IManager lManager;
		
		if ((lManager = ((IManager) fm.findFragmentByTag( (fragment ? ChildManager.TAG : TaskManager.TAG) ))) == null) {
			if (fragment) {
				log("Utils", "buildManager", "Attching a new ChildManager to " + ((android.app.Fragment) object).getClass().getName());
				
				buildSupportManager(((android.app.Fragment) object).getActivity(), false);
				
				fm.beginTransaction().add((android.app.Fragment) (lManager = new ChildManager()), ChildManager.TAG).commit();
				
			} else {
				log("Utils", "buildManager", "Attching a new TaskManager to " + ((android.app.Activity) object).getClass().getName());
				
				fm.beginTransaction().add((android.app.Fragment) (lManager = new TaskManager()), TaskManager.TAG).commit();
			}
		}
		
		return lManager;
	}
}
