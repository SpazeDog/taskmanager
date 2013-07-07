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

public abstract class Task<Params, Progress, Result> implements ITask {
	
	public final static String TAG = "Async";
	
	private final static Integer RUN_NORMAL = 0;
	private final static Integer SKIP_CHECK = 1;
	private final static Integer SKIP_ALL = 2;
	
	private String mCaller;
	
	private IManager mManager;
	
	private Boolean mSupport = false;
	private Boolean mFragment = false;
	private String mFragmentTag;
	
	protected final Object mLock = new Object();
	
	private Map<String, Runnable> mPendingMethods = new HashMap<String, Runnable>();
	private final ArrayList<String> mExecutedMethods = new ArrayList<String>();
	
	private static void log(String aMethod, String aMessage) {
		Utils.log(TAG, aMethod, aMessage);
	}
	
	public final static ITask getTask(android.support.v4.app.Fragment aFragment, String aTag) {
		return getTask(aFragment.getActivity(), aTag);
	}
	
	public final static ITask getTask(android.support.v4.app.FragmentActivity aActivity, String aTag) {
		IManager lManager = Utils.getManager(aActivity);
		
		if (lManager != null) {
			return lManager.getTask(aTag);
		}
		
		return null;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public final static ITask getTask(android.app.Fragment aFragment, String aTag) {
		return getTask(aFragment.getActivity(), aTag);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public final static ITask getTask(android.app.Activity aActivity, String aTag) {
		IManager lManager = Utils.getManager(aActivity);
		
		if (lManager != null) {
			return lManager.getTask(aTag);
		}
		
		return null;
	}
	
	public Task(android.support.v4.app.Fragment aFragment, String aTag) {
		this(aFragment.getActivity(), aTag);
		
		mFragmentTag = aFragment.getTag();
		mFragment = true;
	}
	
	public Task(android.support.v4.app.FragmentActivity aActivity, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Task");
		
		mCaller = aTag;
		mManager = Utils.getManager(aActivity);
		mSupport = true;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Task(android.app.Fragment aFragment, String aTag) {
		this(aFragment.getActivity(), aTag);
		
		mFragmentTag = aFragment.getTag();
		mFragment = true;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Task(android.app.Activity aActivity, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Task");
		
		mCaller = aTag;
		mManager = Utils.getManager(aActivity);
	}
	
	@Override
	public void onAttachUI() {
		synchronized (mLock) {
			log("onAttachUI", "[" + mCaller + "] Entering UI state");
			
            run("onUIReady", new Runnable() {
                public void run() {
                	Task.this.runUIReady(false);
                }
                
            }, SKIP_ALL);
			
			if (mPendingMethods.size() > 0) {
				Map<String, Runnable> lPending = mPendingMethods;
				mPendingMethods = new HashMap<String, Runnable>();
				
				for (String name : lPending.keySet()) {
					run(name, lPending.get(name));
				}
			}
		}
	}
	
	@Override
	public void onDetachUI() {
		synchronized (mLock) {
			log("onDetachUI", "[" + mCaller + "] Leaving UI state");
			
			run("onUIPause", new Runnable() {
                public void run() {
                	Task.this.runUIPause(false);
                }
                
            }, SKIP_ALL);
		}
	}
	
	private void run(String aMethod, Runnable aCode) {
		synchronized (mLock) {
			run(aMethod, aCode, RUN_NORMAL);
		}
	}
	
	private void run(String aMethod, Runnable aCode, Integer aAction) {
		synchronized (mLock) {
			if (aAction > RUN_NORMAL || !mExecutedMethods.contains(aMethod)) {
				if (!mExecutedMethods.contains("onPostExecute") && !mExecutedMethods.contains("onCancelled")) {
					if (aAction < SKIP_ALL && (mPendingMethods.size() > 0 || !mManager.isUIAttached())) {
						log("run", "[" + mCaller + "] The UI is currently not pressent, adding method " + aMethod + "() to the pending list");
						mPendingMethods.put(aMethod, aCode);
						
					} else if (aAction == SKIP_ALL || mManager.isUIAttached()) {
						if (mSupport) {
							((android.support.v4.app.FragmentActivity) getActivityObject()).runOnUiThread(aCode);
							
						} else {
							((android.app.Activity) getActivityObject()).runOnUiThread(aCode);
						}
						
						if (!mExecutedMethods.contains(aMethod)) {
							mExecutedMethods.add(aMethod);
						}
						
						if (aMethod.equals("onPostExecute") || aMethod.equals("onCancelled")) {
							log("onAttachUI", "[" + mCaller + "] Cleaning up and closing this Task");
							
							mManager.removeTask(this);
							mManager = null;
						}
					}
					
				} else {
					log("run", "[" + mCaller + "] This Task is no longer active, canceling the call to the method" + aMethod + "()");
				}
				
			} else {
				log("run", "[" + mCaller + "] The method " + aMethod + "() has already been executed, canceling this call");
			}
		}
	}
	
	private void runUIReady(Boolean aForce) {
		if (aForce || mExecutedMethods.contains("onPreExecute")) {
			log("run", "[" + mCaller + "] Executing method onUIReady()");
			onUIReady();
		}
	}
	
	private void runUIPause(Boolean aForce) {
		if (aForce || mExecutedMethods.contains("onUIReady")) {
			log("run", "[" + mCaller + "] Executing method onUIPause()");
			onUIPause();
		}
	}
	
	@SuppressLint("NewApi")
	public Object getActivityObject() {
		if (mSupport) 
			return ((android.support.v4.app.Fragment) mManager).getActivity();
		
		return ((android.app.Fragment) mManager).getActivity();
	}
	
	@SuppressLint("NewApi")
	public Object getObject() {
		if (mFragment && mFragmentTag != null) {
			return getFragmentObject(mFragmentTag);
			
		} else if (!mFragment) {
			return getActivityObject();
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
    
    public void execute(Params... params) throws IllegalStateException {
    	if (mManager != null && mManager.getTask(mCaller) == null) {
    		mManager.addTask(mCaller, this);
    		cTask.execute(params);
    		
    	} else {
    		throw new IllegalStateException("This task has either already been started, or has finished!");
    	}
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
                    Task.this.run("onUIReady", new Runnable() {
                        public void run() {
                        	Task.this.runUIReady(true);
                        }
                        
                    }, SKIP_ALL);
                    
                    log("run", "[" + mCaller + "] Executing method onPreExecute()");
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
			
			Task.log("run", "[" + Task.this.mCaller + "] Executing method doInBackground()");

            return Task.this.doInBackground(params);
		}
		
        @Override
        protected void onProgressUpdate(final Progress... values) {
            run("onProgressUpdate", new Runnable() {
                public void run() {
                	log("run", "[" + mCaller + "] Executing method onProgressUpdate()");
                    Task.this.onProgressUpdate(values);
                }
                
            }, SKIP_CHECK);
        }
        
        @Override
        protected void onPostExecute(final Result result) {
            run("onPostExecute", new Runnable() {
                public void run() {
                	log("run", "[" + mCaller + "] Executing method onPostExecute()");
                    Task.this.onPostExecute(result);
                }
            });
        }
        
        @Override
        protected void onCancelled() {
            run("onCancelled", new Runnable() {
                public void run() {
                	log("run", "[" + mCaller + "] Executing method onCancelled()");
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
