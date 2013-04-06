taskmanager
===========

A custom Android AsyncTask

Usage
------

AsyncTask is a great tool, allowing one to run background tasks while interacting with the UI before, while and after. However it does come with a few issues, mostly related to the way Android act when a user flips the screen or in other ways creates a detachment of the UI. AsyncTask is not equipped to handle these circumstances.

TaskManager is an extension of `AsyncTask` which fixes it's issues (Based on an idea of Santiago Lezica) and on top of that, provides additional features. 

* **Features**
 - Makes sure that UI inteaction waits until the UI is pressent
 - Makes sure that flipping the screen does not produce multiple instances running the same tasks
 - Adds a new inteaction to AsyncTask `onUIReady()` which is invoked whenever te UI is re-created from detached state

```java
public class MyActivity extends FragmentActivity {

    private ProgressDialog mProgressDialog;

    private Boolean mLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mLoading = savedInstanceState.getBoolean("LoadingState", 0);
        }

        if (!mLoading) {
            asyncLoader();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("LoadingState", mLoading);

        super.onSaveInstanceState(savedInstanceState);
    }

    private void asyncLoader() {
        new Task<Context, Void, Boolean>(this, "nameOfMyAsyncLoader") {
            @Override
            protected void onUIReady() {
                ((MyActivity) getActivity()).mProgressDialog = ProgressDialog.show(getActivity(), "", "Loading...");
            }

            @Override
            protected void onPreExecute() {
                ((MyActivity) getActivity()).mLoading = true;
            }

            @Override
            protected void doInBackground(Context... params) {
                return someMethod( params[0] );
            }

            @Override
            protected void onPostExecute(Boolean result) {
                ((MyActivity) getActivity()).mProgressDialog.dismiss();
                ((MyActivity) getActivity()).mLoading = false;

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

The `onUIReady()` is executed as the very first one and after that each time the UI is re-created. None of the UI inteactive methods are executed if the UI is detached. Instead they will be stored and executed as soon as the UI is re-created, and in the correct order. 

Also, if the UI is re-created while async is running, TaskManager will not allow it to create a new instance with the name `nameOfMyAsyncLoader`. So you could leave out `mLoading` without any issues, although it is good practice to keep it.

Note that because the UI inteactive methods might be stored until the UI is pressent, you could end up having `onPreExecute()` being executed before `onUIReady()` and `onPreExecute()`. As these are mostly used to create loading indicators, it does not really matter as they will be executed as soon as something like loading indicators are needed.

But if you for some reason need `onPreExecute()` to wait until `onUIReady()` and `onPreExecute()` is done, you can easily do so.

```java
protected void doInBackground(Context... params) {
    while (!this.check("onPreExecute")) {
        try {
            Thread.sleep(300);

        } catch (InterruptedException e) {}
    }

    return someMethod( params[0] );
}
```
