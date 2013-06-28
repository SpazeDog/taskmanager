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

            ArrayList<ITask> lTasks = new ArrayList<ITask>();
            
            for (int i=0; i < mTasks.size(); i++) {
                if (!mTasks.get(i).onAttachUI()) {
                	lTasks.add( mTasks.get(i) );
                }
            }
            
            mTasks = lTasks;
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
     
        synchronized (mLock) {
            mUIAttached = false;
            
            ArrayList<ITask> lTasks = new ArrayList<ITask>();
            
            for (int i=0; i < mTasks.size(); i++) {
                if (!mTasks.get(i).onDetachUI()) {
                	lTasks.add( mTasks.get(i) );
                }
            }
            
            mTasks = lTasks;
        }
    }
    
    public void addTask(ITask aTask) {
        synchronized (mLock) {
        	mTasks.add( (ITask) aTask );
        }
    }

    public Boolean isUIAttached() {
    	return mUIAttached;
    }
}

