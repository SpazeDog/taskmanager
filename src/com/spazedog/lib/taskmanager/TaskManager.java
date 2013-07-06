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

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TaskManager extends Fragment implements IManager {
    public final static String TAG = "TaskManager_Fragment";
    
    protected final Object mLock = new Object();
    
    private ArrayList<ITask> mTasks = new ArrayList<ITask>();
    private ArrayList<IDaemon> mDaemons = new ArrayList<IDaemon>();
    
    private ArrayList<String> mTaskKeys = new ArrayList<String>();
    private ArrayList<String> mDaemonKeys = new ArrayList<String>();
    
    protected Boolean mUIAttached = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
        synchronized (mLock) {
            mUIAttached = true;

            if (mTasks.size() > 0) {
	            for (int i=0; i < mTasks.size(); i++) {
	                mTasks.get(i).onAttachUI();
	            }
            }
            
            if (mDaemons.size() > 0) {
	            for (int i=0; i < mDaemons.size(); i++) {
	            	mDaemons.get(i).onResume();
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
	            for (int i=0; i < mTasks.size(); i++) {
	                mTasks.get(i).onDetachUI();
	            }
            }
            
            if (mDaemons.size() > 0) {
	            for (int i=0; i < mDaemons.size(); i++) {
	            	mDaemons.get(i).onPause();
	            }
            }
        }
    }
    
    @Override
    public void addTask(String aTag, ITask aTask) {
        synchronized (mLock) {
        	mTasks.add(aTask);
        	mTaskKeys.add(aTag);
        }
    }
    
    @Override
    public void removeTask(ITask aTask) {
        synchronized (mLock) {
            if (mTasks.size() > 0) {
	            for (int i=0; i < mTasks.size(); i++) {
	                if (mTasks.get(i) == aTask) {
	                	mTasks.remove(i);
	                	mTaskKeys.remove(i); break;
	                }
	            }
            }
        }
    }
    
    @Override
    public ITask getTask(String aTag) {
        synchronized (mLock) {
        	if (mTaskKeys.size() > 0) {
        		for (int i=0; i < mTaskKeys.size(); i++) {
        			if (mTaskKeys.get(i).equals(aTag)) {
        				return mTasks.get(i);
        			}
        		}
        	}
        	
        	return null;
        }
    }
    
    @Override
    public void addDaemon(String aTag, IDaemon aDaemon) {
        synchronized (mLock) {
        	mDaemons.add(aDaemon);
        	mDaemonKeys.add(aTag);
        }
    }
    
    @Override
    public void removeDaemon(IDaemon aDaemon) {
        synchronized (mLock) {
            if (mDaemons.size() > 0) {
	            for (int i=0; i < mDaemons.size(); i++) {
	                if (mDaemons.get(i) == aDaemon) {
	                	mDaemons.remove(i);
	                	mDaemonKeys.remove(i);
	                }
	            }
            }
        }
    }
    
    @Override
    public IDaemon getDaemon(String aTag) {
        synchronized (mLock) {
        	if (mDaemonKeys.size() > 0) {
        		for (int i=0; i < mDaemonKeys.size(); i++) {
        			if (mDaemonKeys.get(i).equals(aTag)) {
        				return mDaemons.get(i);
        			}
        		}
        	}
        	
        	return null;
        }
    }

    @Override
    public Boolean isUIAttached() {
    	return mUIAttached;
    }
}

