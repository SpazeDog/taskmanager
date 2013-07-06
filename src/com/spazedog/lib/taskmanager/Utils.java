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
	
	public static IManager getManager(android.support.v4.app.FragmentActivity aActivity) {
		android.support.v4.app.FragmentManager fm = aActivity.getSupportFragmentManager();
		
		IManager lManager;
		
		if ((lManager = ((IManager) fm.findFragmentByTag(SupportTaskManager.TAG))) == null) {
			fm.beginTransaction().add((android.support.v4.app.Fragment) (lManager = new SupportTaskManager()), SupportTaskManager.TAG).commit();
		}
		
		return lManager;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static IManager getManager(android.app.Activity aActivity) {
		android.app.FragmentManager fm = aActivity.getFragmentManager();
		
		IManager lManager;
		
		if ((lManager = ((IManager) fm.findFragmentByTag(TaskManager.TAG))) == null) {
			fm.beginTransaction().add((android.app.Fragment) (lManager = new TaskManager()), TaskManager.TAG).commit();
		}
		
		return lManager;
	}
}
