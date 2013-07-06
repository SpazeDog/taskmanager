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

public abstract class Daemon implements IDaemon {
	
	public final static String TAG = "Thread";

	private DaemonThread mThread;
	
	private IManager mManager;
	
	private Object mLock = new Object();
	
	private Runnable mRunnable;
	
	private Boolean mStarted = false;
	
	private String mTag;
	
	private static void log(String aMethod, String aMessage) {
		Utils.log(TAG, aMethod, aMessage);
	}
	
	public final static IDaemon getDaemon(android.support.v4.app.Fragment aFragment, String aTag) {
		return getDaemon(aFragment.getActivity(), aTag);
	}
	
	public final static IDaemon getDaemon(android.support.v4.app.FragmentActivity aActivity, String aTag) {
		IManager lManager = Utils.getManager(aActivity);
		
		if (lManager != null) {
			return lManager.getDaemon(aTag);
		}
		
		return null;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public final static IDaemon getDaemon(android.app.Fragment aFragment, String aTag) {
		return getDaemon(aFragment.getActivity(), aTag);
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
		this(aFragment.getActivity(), aTag);
	}
	
	public Daemon(android.support.v4.app.FragmentActivity aActivity, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Daemon");
		
		mManager = Utils.getManager(aActivity);
		mTag = aTag;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Daemon(android.app.Fragment aFragment, String aTag) {
		this(aFragment.getActivity(), aTag);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Daemon(android.app.Activity aActivity, String aTag) {
		log("construct", "[" + aTag + "] Initiating a new Daemon");
		
		mManager = Utils.getManager(aActivity);
		mTag = aTag;
	}
	
	protected abstract void run();
	
	public final void run(Runnable runnable) {
		mRunnable = runnable;
	}
	
	public final void destroy() {
		synchronized (mLock) {
			mManager.removeDaemon(this);
			
			stop();

			mManager = null;
			mRunnable = null;
		}
	}
	
	public final void stop() {
		synchronized (mLock) {
			if (mThread != null) {
				mThread.sendStop();
			}
			
			mThread = null;
		}
	}
	
	public final void start() throws IllegalStateException {
		start(1000, 0);
	}
	
	public final void start(Integer timeout) throws IllegalStateException {
		start(timeout, 0);
	}
	
	public final void start(Integer timeout, Integer delay) throws IllegalStateException {
		synchronized (mLock) {
			if (mThread == null && mManager != null && (mManager.getDaemon(mTag) == null || mStarted)) {
				mThread = new DaemonThread(timeout, delay);
				mThread.start();

				if (!mStarted) {
					mManager.addDaemon(mTag, this);
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
			if (mThread != null) {
				mThread.sendPause();
			}
		}
	}
	
	@Override
	public final void onResume() {
		synchronized (mLock) {
			if (mThread != null) {
				mThread.sendResume();
			}
		}
	}
	
    private final class DaemonThread extends Thread {
        private Boolean mPaused = false;
        private Boolean mRunning = false;
        private Boolean mStopped = false;
        
        private Integer mTimeout;
        private Integer mDelay;

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
						Thread.sleep(mDelay);
						
						mDelay = 0;
					}
                	
					if (mRunnable != null) {
						mRunnable.run();
						
					} else {
						Daemon.this.run();
					}
                	
                	Thread.sleep(mTimeout);
                	
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
