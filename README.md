taskmanager
===========

A custom Android AsyncTask and Thread

AsyncTask and Thread is two great tools, allowing one to run background tasks while interacting with the UI. However it does come with a few issues.

The AsyncTask class has problems when a user flips the screen or in other ways creates a detachment of the UI. AsyncTask is not equipped to handle these circumstances and might in some cases produce FC issues.

The Thread class takes to much time and coding to set up properly. It is also missing important options to pause and stop any background processes, especially when an Activity get's paused (No need to keep running when not added to a service).

TaskManager extends these two tools to allow you an quick and easy setup without a lot of coding and with all of these issues taken care off.

AsyncTask Usage
------

The extension of `AsyncTask` is called `Task`. The class setup is just the same as with a regular `AsyncTask`, only Task includes two additional methods `onUIReady()` and `onUIPause()` which is invoked everytime the UI is detached and re-attached. Also `onUIReady()` is the first method to be called when you start executing the Task. 

```java
// Execute from inside an Activity
public class MyActivity extends Activity {

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Task<Context, Void, Boolean>(this, "nameOfMyAsyncLoader") {
            @Override
            protected void onUIReady() {
                MyActivity activity = (MyActivity) getObject();

                if (activity.mProgressDialog == null) {
                    activity.mProgressDialog = ProgressDialog.show( (Activity) getActivityObject(), "", "Loading...");
                }
            }

            @Override
            protected void doInBackground(Context... params) {
                return someMethod( params[0] );
            }

            @Override
            protected void onPostExecute(Boolean result) {
                MyActivity activity = (MyActivity) getObject();

                activity.mProgressDialog.dismiss();

                if ( result ) {
                    ...

                } else {
                    ...
                }
            }

        }.execute( getApplicationContext() );
    }
}
```

You can use this on both Activity's and Fragments. In both cases just parse `this` to the constructor and use `getObject()` to get the instance. Note that Fragments does not use the same Manager as Activity's, so it is important to parse the right instance to the constructor. Do not parse the Activity instance when adding it to a Fragment. If you do, then you will use the lifecycle of the Acticity rather than the Fragment to controll the UI Attachment and Detachment.

Thread Usage
------

The extension of `Thread` is called `Daemon`. Unlike the `AsyncTask` extension `Task`, this does not provide the same setup as it's parent class. This class is ment for continuous background loop work and comes with pre-built tools for looping, pausing, stopping and also UI interaction to avoid the setup and usage of Handlers. 

```java
public class MyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	    try {
		    new Daemon<Void, Integer>(this, "nameOfMyThread") {
			    @Override
			    protected void doInBackground(Void... params) {
				    // This will be re-executed every 1 second

                    if ( /* Something */ ) {
                        sendToReceiver(1);
                    }
			    }
			
			    @Override
			    protected void receiver(Integer result) {
				    // ... UI Interation ...
			    }
			
		    }.setTimeout(1000).start();
		
	    } catch (IllegalStateException e) {}
    }

    @Override
    protected void onBackPressed() {
        Daemon.getDaemon(this, "nameOfMyThread").destroy();
    }
}
```

This background process will be executed every 1 second and one can use `sendToReceiver()` to interact with UI (It like using a Handler). Each time the Activity or Fragments which owns this Daemon is paused, stopped or destroyed, the daemon will be paused and some saved instances are released. You can also manually call `destroy()` to completly remove it. 

