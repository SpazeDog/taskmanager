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
 - Adds a new inteaction to AsyncTask `onUIReady()` and `onUIPause()` which is invoked whenever the UI is detached and re-attached
 - Compatible with both FragmentManager (Activity + Fragment) and the Support FragmentManager (FragmentActivity + v4 Fragment) from the v4 Support Library
 - Makes it easy to use within both Activities and Fragments

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
                ((MyActivity) getActivityObject()).mProgressDialog = ProgressDialog.show( (Activity) getActivityObject(), "", "Loading...");
            }

            @Override
            protected void doInBackground(Context... params) {
                return someMethod( params[0] );
            }

            @Override
            protected void onPostExecute(Boolean result) {
                ((MyActivity) getActivityObject()).mProgressDialog.dismiss();

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

```java
// Execute from inside an Fragment
public class MyFragment extends Fragment {

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Task<Context, Void, Boolean>(this, "nameOfMyAsyncLoader") {
            @Override
            protected void onUIReady() {
                ((MyFragment) getFragmentObject()).mProgressDialog = ProgressDialog.show( (Activity) getActivityObject(), "", "Loading...");
            }

            @Override
            protected void doInBackground(Context... params) {
                return someMethod( params[0] );
            }

            @Override
            protected void onPostExecute(Boolean result) {
                ((MyFragment) getFragmentObject()).mProgressDialog.dismiss();

                if ( result ) {
                    ...

                } else {
                    ...
                }
            }

        }.execute( getActivity().getApplicationContext() );
    }
}
```

The `onUIReady()` is executed as the very first one and after that each time the UI is re-attached (Re-created). None of the UI inteactive methods are executed if the UI is detached. Instead they will be stored in a pending container and executed as soon as the UI is re-created, and in the correct order. 

Also, if the UI is re-created while async is running, TaskManager will not allow it to create a new instance with the same name.

Note that when using TaskManager within a Fragment and make a call to `getFragmentObject()`, it will return `NULL` if you did not assign a Tag Name to it when you added it trough FragmentManager. You can also search for other Fragments by calling `getFragmentObject(TAG)` or `getFragmentObject(ID)`.
