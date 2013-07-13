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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class ChildManager extends Fragment implements IManager {
	/*
	 * Nested fragments cannot use the Retain Instance option.
	 * So we use a child manager on fragments and use the activity's manager to store our Task and Daemon instances
	 */
	
	public final static String TAG = "TaskManager_ChildFragment";
	
	private WeakReference<IParentManager> mManager;
	
	private String mName;
	
    private Map<String, ITask> mTasks = new HashMap<String, ITask>();
    private Map<String, IDaemon> mDaemons = new HashMap<String, IDaemon>();
	
	protected Boolean mUIAttached = false;
	
    protected final Object mLock = new Object();
    
	private static void log(String aMethod, String aMessage) {
		Utils.log("ChildFragment", aMethod, aMessage);
	}
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
    	mName = getParentFragment().getClass().getName();
    }
	
    @Override
    public void onStart() {
    	super.onStart();
    	
    	mManager = new WeakReference<IParentManager>((IParentManager) Utils.getManager(getActivity()));
    	
    	synchronized (mLock) {
    		Map<String, ITask> tasks = mManager.get().getChildTasks(mName);
    		if (tasks != null) {
    			log("onStart", "Restoring " + tasks.size() + " tasks to the task list");
    			
    			for (String key : tasks.keySet()) {
    				mTasks.put(key, tasks.remove(key));
    			}
    		}
    		
    		Map<String, IDaemon> daemons = mManager.get().getChildDaemons(mName);
    		if (daemons != null) {
    			log("onStart", "Restoring " + daemons.size() + " daemons to the task list");
    			
    			for (String key : daemons.keySet()) {
    				mDaemons.put(key, daemons.remove(key));
    			}
    		}
    	}
    }
    
    @Override
    public void onStop() {
    	super.onStart();
    	
    	synchronized (mLock) {
	    	if (mTasks.size() > 0) {
	    		log("onStop", "Saving " + mTasks.size() + " tasks to the parent TaskManager");
	    		
	    		mManager.get().addChildTasks(mName, mTasks);
	    	}
	    	
	    	if (mDaemons.size() > 0)
	    		log("onStop", "Saving " + mDaemons.size() + " daemons to the parent TaskManager");
	    	
	    		mManager.get().addChildDaemons(mName, mDaemons);
	    	
	    	mTasks = new HashMap<String, ITask>();
	    	mDaemons = new HashMap<String, IDaemon>();
    	}
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
}
