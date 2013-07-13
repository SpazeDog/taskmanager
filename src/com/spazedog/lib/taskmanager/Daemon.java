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
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;

public abstract class Daemon<Params, Result> implements IDaemon {
	
	public final static String TAG = "Thread";

	private DaemonThread mThread;
	
	private WeakReference<IManager> mManager;
	
	private Object mLock = new Object();
	
	private Boolean mStarted = false;
	
	private String mTag;
	
	private Params[] mParams;
	
	private Boolean mSupport = false;
	private Boolean mFragment = false;
	
	protected Boolean mReady = true;
	
	private Integer mTimeout = 1000;
	private Integer mDelay = 0;
	
	private ArrayList<Runnable> mPendingMethods = new ArrayList<Runnable>();
	
	private static void log(String aMethod, String aMessage) {
		Utils.log(TAG, aMethod, aMessage);
	}
	
	public final static IDaemon getDaemon(android.support.v4.app.Fragment aFragment, String aTag) {
		IManager lManager = Utils.getManager(aFragment);
		
		if (lManager != null) {
			return lManager.getDaemon(aTag);
		}
		
		return null;
	}
	
	public final static IDaemon getDaemon(android.support.v4.app.FragmentActivity aActivity, String aTag) {
		IManager lManager = Utils.getManager(aActivity);
		
		if (lManager != null) {
			return lManager.getDaemon(aTag);
		}
		
		return null;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public final static IDaemon getDaemon(android.app.Fragment aFragment, String aTag) {
		IManager lManager = Utils.getManager(aFragment);
		
		if (lManager != null) {
			return lManager.getDaemon(aTag);
		}
		
		return null;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public final static IDaemon getDaemon(android.app.Activity aActivity, String aTag) {
		IManager lManager = Utils.getManager(aActivity);
		
		if (lManager != null) {
			return lManager.getDaemon(aTag);
		}
		
		return null;
	}
	
	public Daemon(android.support.v4.app.Fragment aFragment, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Daemon");
		
		mManager = new WeakReference<IManager>(Utils.getManager(aFragment));
		mTag = aTag;
		mSupport = true;
		mFragment = true;
	}
	
	public Daemon(android.support.v4.app.FragmentActivity aActivity, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Daemon");
		
		mManager = new WeakReference<IManager>(Utils.getManager(aActivity));
		mTag = aTag;
		mSupport = true;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Daemon(android.app.Fragment aFragment, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Daemon");
		
		mManager = new WeakReference<IManager>(Utils.getManager(aFragment));
		mTag = aTag;
		mFragment = true;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Daemon(android.app.Activity aActivity, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Daemon");
		
		mManager = new WeakReference<IManager>(Utils.getManager(aActivity));
		mTag = aTag;
	}
	
	@SuppressLint("NewApi")
	public Object getActivityObject() {
        if (mManager != null) {
		    if (mSupport) 
			    return ((android.support.v4.app.Fragment) mManager.get()).getActivity();
		
		    return ((android.app.Fragment) mManager.get()).getActivity();
        }

        return null;
	}
	
	@SuppressLint("NewApi")
	public Object getObject() {
        if (mManager != null) {
		    if (mFragment) {
			    if (mSupport) {
				    return ((android.support.v4.app.Fragment) mManager.get()).getParentFragment();
				
			    } else {
				    return ((android.app.Fragment) mManager.get()).getParentFragment();
			    }
			
		    } else {
			    return getActivityObject();
		    }
        }
		
		return null;
	}
	
	protected abstract void doInBackground(Params... params);
	protected void receiver(Result result) {}
	
	protected final void sendToReceiver(final Result result) {
        run(new Runnable() {
            public void run() {
                Daemon.this.receiver(result);
            }
        });
	}
	
	private void run(Runnable aCode) {
		synchronized (mLock) {
			if (mPendingMethods.size() > 0 || mManager == null || !mManager.get().isUIAttached()) {
				mPendingMethods.add(aCode);
				
			} else {
				if (mSupport) {
					((android.support.v4.app.FragmentActivity) getActivityObject()).runOnUiThread(aCode);
					
				} else {
					((android.app.Activity) getActivityObject()).runOnUiThread(aCode);
				}
			}
		}
	}
	
	private void runPending() {
		synchronized (mLock) {
			if (mPendingMethods.size() > 0) {
				ArrayList<Runnable> pending = mPendingMethods;
				mPendingMethods = new ArrayList<Runnable>();
				
				while (pending.size() > 0) {
					run(pending.remove(0));
				}
			}
		}
	}
	
	public final Daemon<Params, Result> setTimeout(Integer aTimeout) {
		mTimeout = aTimeout;
		
		return this;
	}
	
	public final Daemon<Params, Result> setTimeout(Integer aTimeout, Integer aDelay) {
		mTimeout = aTimeout;
		mDelay = aDelay;
		
		return this;
	}
	
	public final void destroy() {
		synchronized (mLock) {
            if (mManager != null) {
			    mManager.get().removeDaemon(mTag);
			
			    stop();

			    mManager = null;
            }
		}
	}
	
	public final void stop() {
		synchronized (mLock) {
			if (mThread != null) {
				mThread.sendStop();
			}
			
			mThread = null;
			mParams = null;
			
			while (mPendingMethods.size() > 0) {
				mPendingMethods.remove(0);
			}
		}
	}
	
	public final void start(Params... params) throws IllegalStateException {
		synchronized (mLock) {
			if (mThread == null && mManager != null && mManager.get() != null && (mManager.get().getDaemon(mTag) == null || mStarted)) {
				mThread = new DaemonThread(mTimeout, mDelay);
				mThread.start();
				
				mParams = params;

				if (!mStarted) {
					mManager.get().addDaemon(mTag, this);
					mStarted = true;
				}
				
			} else {
				throw new IllegalStateException("This daemon has either already been started, or has finished!");
			}
		}
	}
	
	@Override
	public final void onPause() {
		synchronized (mLock) {
			log("onPause", "[" + mTag + "] Entering Pause state");
			
			if (mReady) {
				if (mThread != null) {
					mThread.sendPause();
				}
				
			} else {
				log("onPause", "[" + mTag + "] The onResume() has not yet been executed, skipping");
			}
		}
	}

	@Override
	public final void onResume(IManager manager) {
		synchronized (mLock) {
			log("onResume", "[" + mTag + "] Entering Resume state");
			
			mManager = new WeakReference<IManager>(manager);
			mReady = false;
			
			if (getActivityObject() != null) {
				mReady = true;
				
				if (mThread != null) {
					runPending();
					
					mThread.sendResume();
				}
				
			} else {
				log("onResume", "[" + mTag + "] The UI is not ready, skipping");
			}
		}
	}
	
    private final class DaemonThread extends Thread {
        private Boolean mPaused = false;
        private Boolean mRunning = false;
        private Boolean mStopped = false;

        private Object mLock = new Object();
        
        public DaemonThread(Integer aTimeout, Integer aDelay) {
        	mTimeout = aTimeout;
        	mDelay = aDelay;
        }
        
        public void sendStop() {
        	synchronized (mLock) {
        		if (mRunning) {
	        		if (mPaused) {
	        			sendResume();
	        		}
	        		
	        		mStopped = true;
        		}
        	}
        }
        
        public void sendPause() {
        	synchronized (mLock) {
        		if (mRunning) {
        			mPaused = true;
        		}
        	}
        }
        
        public void sendResume() {
        	synchronized (mLock) {
        		if (mRunning) {
	        		mPaused = false;
	        		mLock.notifyAll();
        		}
        	}
        }
        
        @Override
        public void run() {
        	log("run", "[" + mTag + "] Starting the daemon");
        	
        	mRunning = true;
        	
            try {
				while (!mStopped) {
					if ((int) mDelay > 0) {
						Thread.sleep(Daemon.this.mDelay);
						
						mDelay = 0;
					}
                	
					Daemon.this.doInBackground(Daemon.this.mParams);
                	
                	Thread.sleep(Daemon.this.mTimeout);
                	
                	synchronized (mLock) {
                		while (mPaused) {
                			try {
                				log("run", "[" + mTag + "] Pausing the daemon");
                				
                				mLock.wait();
                				
                				log("run", "[" + mTag + "] Resuming the daemon");
                				
                			} catch (InterruptedException e) {}
                		}
                	}
                }
				
				this.join();
                
            } catch (InterruptedException e) {}
            
            log("run", "[" + mTag + "] Stopping the daemon");
            
            mRunning = false;
        }
    }
}
