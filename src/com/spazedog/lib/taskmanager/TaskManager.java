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

import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TaskManager extends Fragment implements IManager, IParentManager {
    public final static String TAG = "TaskManager_Fragment";
    
    protected final Object mLock = new Object();
    
    private Map<String, ITask> mTasks = new HashMap<String, ITask>();
    private Map<String, IDaemon> mDaemons = new HashMap<String, IDaemon>();
    
    private Map<String, Map<String, ITask>> mChildTasks = new HashMap<String, Map<String, ITask>>();
    private Map<String, Map<String, IDaemon>> mChildDaemons = new HashMap<String, Map<String, IDaemon>>();
    
    protected Boolean mUIAttached = false;
    
	private static void log(String aMethod, String aMessage) {
		Utils.log("Fragment", aMethod, aMessage);
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
	    synchronized (mLock) {
            mUIAttached = true;

            if (mTasks.size() > 0) {
            	log("onResume", "Announcing UI attachment to " + mTasks.size() + " tasks");
            	
	            for (String key : mTasks.keySet()) {
	                mTasks.get(key).onAttachUI(this);
	            }
            }
            
            if (mDaemons.size() > 0) {
            	log("onResume", "Announcing resume to " + mDaemons.size() + " daemons");
            	
	            for (String key : mDaemons.keySet()) {
	            	mDaemons.get(key).onResume(this);
	            }
            }
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();

	    synchronized (mLock) {
            mUIAttached = false;
            
            if (mTasks.size() > 0) {
            	log("onPause", "Announcing UI detachment to " + mTasks.size() + " tasks");
            	
	            for (String key : mTasks.keySet()) {
	                mTasks.get(key).onDetachUI();
	            }
            }
            
            if (mDaemons.size() > 0) {
	            for (String key : mDaemons.keySet()) {
	            	log("onPause", "Announcing pause to " + mDaemons.size() + " daemons");
	            	
	            	mDaemons.get(key).onPause();
	            }
            }
    	}
    }
    
    @Override
    public void addTask(String aTag, ITask aTask) {
        synchronized (mLock) {
        	log("addTask", "Adding new task " + aTag);
        	
        	mTasks.put(aTag, aTask);
        }
    }
    
    @Override
    public void removeTask(String aTag) {
        synchronized (mLock) {
        	log("removeTask", "Removing task " + aTag);
        	
            mTasks.remove(aTag);
        }
    }
    
    @Override
    public ITask getTask(String aTag) {
        synchronized (mLock) {
        	return mTasks.get(aTag);
        }
    }
    
    @Override
    public void addDaemon(String aTag, IDaemon aDaemon) {
        synchronized (mLock) {
        	log("addDaemon", "Adding daemon " + aTag);
        	
        	mDaemons.put(aTag, aDaemon);
        }
    }
    
    @Override
    public void removeDaemon(String aTag) {
        synchronized (mLock) {
        	log("removeDaemon", "Removing daemon " + aTag);
        	
        	mDaemons.remove(aTag);
        }
    }
    
    @Override
    public IDaemon getDaemon(String aTag) {
        synchronized (mLock) {
        	return mDaemons.get(aTag);
        }
    }

    @Override
    public Boolean isUIAttached() {
    	return mUIAttached;
    }

	@Override
	public void addChildTasks(String aClass, Map<String, ITask> aTasks) {
		log("addChildTasks", "Storing child tasks from " + aClass);
		
		mChildTasks.put(aClass, aTasks);
	}

	@Override
	public Map<String, ITask> getChildTasks(String aClass) {
		log("getChildTasks", "Returning child tasks to " + aClass);
		
		return mChildTasks.remove(aClass);
	}

	@Override
	public void addChildDaemons(String aClass, Map<String, IDaemon> aDaemons) {
		log("addChildTasks", "Storing child daemons from " + aClass);
		
		mChildDaemons.put(aClass, aDaemons);
	}

	@Override
	public Map<String, IDaemon> getChildDaemons(String aClass) {
		log("getChildDaemons", "Returning child daemons to " + aClass);
		
		return mChildDaemons.remove(aClass);
	}
}
