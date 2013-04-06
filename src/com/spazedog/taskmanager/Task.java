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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public abstract class Task<Params, Progress, Result>  {
    public final static String TAG = "TaskManager_Async";
    
    private final static Map<String, Boolean> mOngoing = new HashMap<String, Boolean>();
    
    private TaskManager mManager;
    private AsyncTask<Params, Progress, Result> mTask;
    
    private Map<String, Runnable> mExecute = new HashMap<String, Runnable>();
    private Map<String, Boolean> mCheck = new HashMap<String, Boolean>();
    
    private Boolean mUIAttached = false;
    private Boolean mFinished = false;
    private Boolean mInitiated = false;
    private String mTag;
    protected final Object mLock = new Object();
    
    public Task(FragmentActivity aActivity) {
        this(aActivity, TAG);
    }
    
    public Task(FragmentActivity aActivity, String aTag) {
        FragmentManager lManager = aActivity.getSupportFragmentManager();
        
        if ((mManager = ((TaskManager) lManager.findFragmentByTag(TaskManager.TAG))) == null) {
            mManager = new TaskManager();
            
            lManager.beginTransaction().add(mManager, TaskManager.TAG).commit();
        }
        
        mTag = aTag;
    }
    
    private void runMethod(String aMethodName, Runnable aMethod) {
        synchronized (mLock) {
            if (!mFinished) {
                if (mExecute.size() > 0 || !mUIAttached) {
                    Log.i("TaskManager.AsyncTask()", "Adding " + aMethodName + "() to the execution list");
                    mExecute.put(aMethodName, aMethod);
                    
                } else {
                    getActivity().runOnUiThread(aMethod);
                    mCheck.put(aMethodName, true);
                }
            }
        }
    }
    
    public Boolean check(String aMethodName) {
        return mCheck.get(aMethodName) != null && mCheck.get(aMethodName) == true;
    }
    
    public FragmentActivity getActivity() {
        return mManager.getActivity();
    }
    
    protected void onUIReady() {}
    protected void onPreExecute() {}
    protected abstract Result doInBackground(Params... params);
    protected void onProgressUpdate(Progress... values) {}
    protected void onPostExecute(Result result) {}
    protected void onCancelled() {}
    
    public Task<Params, Progress, Result> execute(Params... params) {
        if (mOngoing.get(mTag) == null) {
            mManager.addReceiver(new TaskReceiver() {
                @Override
                public Boolean onAttachUI() {
                    synchronized (Task.this.mLock) {
                        Task.this.mUIAttached = true;
                        
                        if (!Task.this.mFinished) {
                            if (Task.this.mInitiated) {
                                Log.i("TaskManager.AsyncTask()", "Executing onUIReady()");
                                Task.this.onUIReady();
                            }
                            
                            for (String name : Task.this.mExecute.keySet()) {
                                Task.this.getActivity().runOnUiThread(mExecute.get(name));
                                Task.this.mCheck.put(name, true);
                                
                                if (Task.this.mFinished) {
                                    return true;
                                }
                            }
                            
                            Task.this.mExecute = new HashMap<String, Runnable>();
                            
                        } else {
                            return true;
                        }
                        
                        return false;
                    }
                }
                
                @Override
                public Boolean onDetachUI() {
                    synchronized (Task.this.mLock) {
                        return (Task.this.mUIAttached = false);
                    }
                }
            });
            
            mTask = new AsyncTask<Params, Progress, Result>() {
                @Override
                protected void onPreExecute() {
                    runMethod("onPreExecute", new Runnable() {
                        public void run() {
                            Task.this.mInitiated = true;
                            
                            synchronized (Task.this.mLock) {
                                Log.i("TaskManager.AsyncTask()", "Executing onUIReady()");
                                Task.this.onUIReady();
                            }
                            
                            Log.i("TaskManager.AsyncTask()", "Executing onPreExecute()");
                            Task.this.onPreExecute();
                        }
                    });
                }
    
                @Override
                protected Result doInBackground(Params... params) {
                    Log.i("TaskManager.AsyncTask()", "Executing doInBackground()");
                    return Task.this.doInBackground(params);
                }
                
                @Override
                protected void onProgressUpdate(final Progress... values) {
                    runMethod("onProgressUpdate", new Runnable() {
                        public void run() {
                            Log.i("TaskManager.AsyncTask()", "Executing onProgressUpdate()");
                            Task.this.onProgressUpdate(values);
                        }
                    });
                }
                
                @Override
                protected void onPostExecute(final Result result) {
                    runMethod("onPostExecute", new Runnable() {
                        public void run() {
                            Log.i("TaskManager.AsyncTask()", "Executing onPostExecute()");
                            Task.this.mFinished = true;
                            Task.this.onPostExecute(result);
                            
                            Task.mOngoing.remove(Task.this.mTag);
                        }
                    });
                }
                
                @Override
                protected void onCancelled() {
                    runMethod("onCancelled", new Runnable() {
                        public void run() {
                            Log.i("TaskManager.AsyncTask()", "Executing onCancelled()");
                            Task.this.mFinished = true;
                            Task.this.onCancelled();
                            
                            Task.mOngoing.remove(Task.this.mTag);
                        }
                    });
                }
            };
    
            mOngoing.put(mTag, true);
            
            mTask.execute(params);
        }

        return this;
    }
    
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mTask.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return mTask.isCancelled();
    }

    public Result get() throws InterruptedException, ExecutionException {
        return mTask.get();
    }

    public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mTask.get(timeout, unit);
    }

    public AsyncTask.Status getStatus() {
        return mTask.getStatus();
    }
}

