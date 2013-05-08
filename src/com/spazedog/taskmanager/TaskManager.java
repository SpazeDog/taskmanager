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

package com.spazedog.taskmanager;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class TaskManager extends Fragment {
    public final static String TAG = "TaskManager_Fragment";
    
    protected final Object mLock = new Object();
    
    private ArrayList<TaskReceiver> mReceivers = new ArrayList<TaskReceiver>();
    protected Boolean mUIAttached = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        synchronized (mLock) {
            mUIAttached = true;

            ArrayList<TaskReceiver> lReceivers = new ArrayList<TaskReceiver>();
            
            for (int i=0; i < mReceivers.size(); i++) {
                if (!mReceivers.get(i).onAttachUI()) {
                    lReceivers.add( mReceivers.get(i) );
                }
            }
            
            mReceivers = lReceivers;
        }
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
     
        synchronized (mLock) {
            mUIAttached = false;

            ArrayList<TaskReceiver> lReceivers = new ArrayList<TaskReceiver>();
            
            for (int i=0; i < mReceivers.size(); i++) {
                if (!mReceivers.get(i).onDetachUI()) {
                    lReceivers.add( mReceivers.get(i) );
                }
            }
            
            mReceivers = lReceivers;
        }
    }
    
    public void addReceiver(TaskReceiver aReceiver) {
        synchronized (mLock) {
            mReceivers.add( (TaskReceiver) aReceiver );
        }
    }

    public Boolean isUIAttached() {
    	return mUIAttached;
    }
}

