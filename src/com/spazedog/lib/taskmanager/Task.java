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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public abstract class Task<Params, Progress, Result> implements ITask {
	
	public final static String TAG = "TaskManager_Async";
	
	public static Boolean LOG = false;
	
	private final static ArrayList<String> oOngoing = new ArrayList<String>();
	
	private String mCaller;
	
	private String mFragmentTag;
	
	private IManager mManager;
	
	private Boolean mSupport = false;
	
	protected final Object mLock = new Object();
	
	private Map<String, Runnable> mPendingMethods = new HashMap<String, Runnable>();
	private final ArrayList<String> mExecutedMethods = new ArrayList<String>();
	
	private static void log(String aMethod, String aMessage) {
		if (LOG) {
			Log.i("TaskManager." + aMethod, aMessage);
		}
	}
	
	public Task(android.support.v4.app.Fragment aFragment, String aTag) {
		this(aFragment.getActivity(), aTag);
		
		mFragmentTag = aFragment.getTag();
	}
	
	public Task(android.support.v4.app.FragmentActivity aActivity, String aTag) {
		StackTraceElement directCaller = Thread.currentThread().getStackTrace()[4];
		mCaller = directCaller.getClassName() + "." + directCaller.getMethodName() + "()#" + aTag;

		log("Task", "Creating new Task for " + mCaller);
		
		android.support.v4.app.FragmentManager fm = aActivity.getSupportFragmentManager();
		
		if ((mManager = ((IManager) fm.findFragmentByTag(SupportTaskManager.TAG))) == null) {
			log("Task", "No TaskManager has been added to this activity, initiating a new instance");
			fm.beginTransaction().add((android.support.v4.app.Fragment) (mManager = new SupportTaskManager()), SupportTaskManager.TAG).commit();
		}
		
		mSupport = true;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Task(android.app.Fragment aFragment, String aTag) {
		this(aFragment.getActivity(), aTag);
		
		mFragmentTag = aFragment.getTag();
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Task(android.app.Activity aActivity, String aTag) {
		StackTraceElement directCaller = Thread.currentThread().getStackTrace()[4];
		mCaller = directCaller.getClassName() + "." + directCaller.getMethodName() + "()#" + aTag;

		log("Task", "Creating new Task for " + mCaller);
		
		android.app.FragmentManager fm = aActivity.getFragmentManager();
		
		if ((mManager = ((IManager) fm.findFragmentByTag(TaskManager.TAG))) == null) {
			log("Task", "No TaskManager has been added to this activity, initiating a new instance");
			fm.beginTransaction().add((android.app.Fragment) (mManager = new TaskManager()), TaskManager.TAG).commit();
		}
	}
	
	@Override
	public Boolean onAttachUI() {
		synchronized (mLock) {
			runUIReady(false);
			
			if (mPendingMethods.size() > 0) {
				log("onAttachUI", "Executing pending methods");
				
				Map<String, Runnable> lPending = mPendingMethods;
				mPendingMethods = new HashMap<String, Runnable>();
				
				for (String name : lPending.keySet()) {
					if (run(name, lPending.get(name))) {
						return true;
					}
				}
			}
			
			return false;
		}
	}
	
	@Override
	public Boolean onDetachUI() {
		synchronized (mLock) {
			runUIPause(false);
			
			return false;
		}
	}
	
	private Boolean run(String aMethod, Runnable aCode) {
		synchronized (mLock) {
			if (!mExecutedMethods.contains("onPostExecute") && !mExecutedMethods.contains("onCancelled")) {
				if (mPendingMethods.size() > 0 || !mManager.isUIAttached()) {
					log("run", "UI is not pressent, adding " + aMethod + "() to the list of pending methods");
					mPendingMethods.put(aMethod, aCode);
					
				} else {
					log("run", "UI is pressent, running " + aMethod + "()");
					
					if (mSupport) {
						((android.support.v4.app.FragmentActivity) getActivityObject()).runOnUiThread(aCode);
						
					} else {
						((android.app.Activity) getActivityObject()).runOnUiThread(aCode);
					}
					
					mExecutedMethods.add(aMethod);
					
					if (aMethod.equals("onPostExecute") || aMethod.equals("onCancelled")) {
						for (int i=0; i < oOngoing.size(); i++) {
							if (oOngoing.get(i).equals(mCaller)) {
								oOngoing.remove(i); break;
							}
						}
						
						// True = Remove Task from TaskManagers receiver list
						return true;
					}
				}
				
			} else {
				log("run", "This task has finished. Canceling the call to " + aMethod + "()"); return true;
			}
			
			return false;
		}
	}
	
	private void runUIReady(Boolean aForce) {
		if (aForce || mExecutedMethods.contains("onPreExecute")) {
			log("AsyncTask", "Executing onUIReady()");
			
			onUIReady();
			
			if (!mExecutedMethods.contains("onUIReady")) {
				mExecutedMethods.add("onUIReady");
			}
		}
	}
	
	private void runUIPause(Boolean aForce) {
		if (aForce || mExecutedMethods.contains("onUIReady")) {
			log("AsyncTask", "Executing onUIPause()");
			
			onUIPause();
			
			if (!mExecutedMethods.contains("onUIPause")) {
				mExecutedMethods.add("onUIPause");
			}
		}
	}
	
	@SuppressLint("NewApi")
	public Object getActivityObject() {
		if (mSupport) 
			return ((android.support.v4.app.Fragment) mManager).getActivity();
		
		return ((android.app.Fragment) mManager).getActivity();
	}
	
	@SuppressLint("NewApi")
	public Object getFragmentObject() {
		if (mFragmentTag != null) {
			if (mSupport) 
				return ((android.support.v4.app.Fragment) mManager).getActivity().getSupportFragmentManager().findFragmentByTag(mFragmentTag);
			
			return ((android.app.Fragment) mManager).getActivity().getFragmentManager().findFragmentByTag(mFragmentTag);
		}
		
		return null;
	}
	
	@SuppressLint("NewApi")
	public Object getFragmentObject(String aTag) {
		if (mSupport) 
			return ((android.support.v4.app.Fragment) mManager).getActivity().getSupportFragmentManager().findFragmentByTag(aTag);
		
		return ((android.app.Fragment) mManager).getActivity().getFragmentManager().findFragmentByTag(aTag);
	}
	
	@SuppressLint("NewApi")
	public Object getFragmentObject(Integer aId) {
		if (mSupport) 
			return ((android.support.v4.app.Fragment) mManager).getActivity().getSupportFragmentManager().findFragmentById(aId);
		
		return ((android.app.Fragment) mManager).getActivity().getFragmentManager().findFragmentById(aId);
	}
	
	/* ###
	 * # AsyncTask methods
	 * ### 
	 */
	protected void onUIPause() {}
    protected void onUIReady() {}
    protected void onPreExecute() {}
    protected abstract Result doInBackground(Params... params);
    protected void onProgressUpdate(Progress... values) {}
    protected void onPostExecute(Result result) {}
    protected void onCancelled() {}
    
    public void publishProgress(Progress... values) {
    	cTask.publicPublishProgress(values);
    }
    
    public boolean cancel(boolean mayInterruptIfRunning) {
        return cTask.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return cTask.isCancelled();
    }

    public Result get() throws InterruptedException, ExecutionException {
        return cTask.get();
    }

    public Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return cTask.get(timeout, unit);
    }

    public AsyncTask.Status getStatus() {
        return cTask.getStatus();
    }
    
    public Boolean execute(Params... params) {
    	if (!oOngoing.contains(mCaller)) {
    		log("execute", "Adding receiver for this Task");
    		
    		mManager.addTask(this);
    		oOngoing.add(mCaller);
    		
    		cTask.execute(params);
    		
    		return true;
    		
    	} else {
    		log("execute", "Another Task with the same name is already running in this activity!");
    	}
    	
    	return false;
    }
	
	/* ###
	 * # Internal AsyncTask instance
	 * ### 
	 */
    private InnerAsyncTask<Params, Progress, Result> cTask = new InnerAsyncTask<Params, Progress, Result>() {
        @Override
        protected void onPreExecute() {
            run("onPreExecute", new Runnable() {
                public void run() {
                	Task.this.runUIReady(true);
                    
                	log("AsyncTask", "Executing onPreExecute()");
                    Task.this.onPreExecute();
                }
            });
        }
        
		@Override
		protected Result doInBackground(Params... params) {
			while (!Task.this.mExecutedMethods.contains("onPreExecute")) {
				try {
					Thread.sleep(300);
					
				} catch (InterruptedException e) {}
			}

			log("AsyncTask", "Executing doInBackground()");
            return Task.this.doInBackground(params);
		}
		
        @Override
        protected void onProgressUpdate(final Progress... values) {
            run("onProgressUpdate", new Runnable() {
                public void run() {
                	log("AsyncTask", "Executing onProgressUpdate()");
                    Task.this.onProgressUpdate(values);
                }
            });
        }
        
        @Override
        protected void onPostExecute(final Result result) {
            run("onPostExecute", new Runnable() {
                public void run() {
                	log("AsyncTask", "Executing onPostExecute()");
                    Task.this.onPostExecute(result);
                }
            });
        }
        
        @Override
        protected void onCancelled() {
            run("onPostExecute", new Runnable() {
                public void run() {
                	log("AsyncTask", "Executing onCancelled()");
                    Task.this.onCancelled();
                }
            });
        }
    };
    
    public abstract class InnerAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    	public void publicPublishProgress(Progress... values) {
    		this.publishProgress(values);
    	}
	}
}
