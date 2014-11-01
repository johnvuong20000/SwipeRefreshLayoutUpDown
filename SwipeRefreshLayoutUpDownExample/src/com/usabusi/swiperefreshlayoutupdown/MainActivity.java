package com.usabusi.swiperefreshlayoutupdown;
//https://git.oschina.net/whos/SwipeRefreshAndLoadLayout/wikis/home
//http://stackoverflow.com/questions/23341735/swiperefreshlayout-swipe-down-to-refresh-but-not-move-the-view-pull-down
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

import com.usabusi.swiperefreshlayoutupdown.R;
import com.usabusi.swiperefreshlayoutupdown.view.SwipeRefreshLayoutUpDown;
//import com.usabusi.swiperefreshlayoutupdown.view.SwipeRefreshLayoutUpDown.OnLoadListener;
import com.usabusi.swiperefreshlayoutupdown.view.SwipeRefreshLayoutUpDown.OnRefreshListener;

//http://stackoverflow.com/questions/24413680/swiperefreshlayout-behind-actionbar/26494724#26494724
//Instead of setting paddingTop on the SwipeRefreshLayout, setting the layout_marginTop will make the progress bar visible:
//
//        <android.support.v4.widget.SwipeRefreshLayout
//        xmlns:android="http://schemas.android.com/apk/res/android"
//        android:layout_width="match_parent"
//        android:layout_height="match_parent"
//        android:layout_marginTop="?android:attr/actionBarSize">
//How can implement offline caching of json in Android?
//http://stackoverflow.com/questions/26135824/how-can-implement-offline-caching-of-json-in-android
//I saw you are using Volley. Volley has built-in HTTP Cache mechanism.
//        So the easiest way is to support HTTP cache headers in your backend. It is very easy and it is transparent to the client side. Volley does the hard work. You can find information here
//        If you don't have access to the URL you use, you must use internal database to support your application. ContentProvider API is the best way to do that. And the following library is a great library to construct a database and write to it. https://github.com/TimotheeJeannin/ProviGen
//        Basically you need to write every item that you pull from the internet to the database and the ListView should show the items from the database using ContentProvider you have just created. When the user opens the app, show the data from the database and then immediately try to pull the new data from your backend.


//http://stackoverflow.com/questions/26601175/swiperefreshlayout-in-api-21
//        Display display = getActivity().getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int height = size.y;
//        getSwipeRefreshLayout().setProgressViewOffset(false, -200, height / 9);


@SuppressLint("ResourceAsColor")
public class MainActivity extends Activity implements OnRefreshListener{

    protected ListView mListView;
    private ArrayAdapter<String> mListAdapter;
    SwipeRefreshLayoutUpDown mSwipeLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.list);
        mListAdapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1, values);
		mListView.setAdapter(mListAdapter);


        mSwipeLayout = (SwipeRefreshLayoutUpDown) findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        //mSwipeLayout.setOnLoadListener(this);//does not use it now
        // mSwipeLayout.setColor
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
	                            android.R.color.holo_green_light,
	                            android.R.color.holo_orange_light,
	                            android.R.color.holo_red_light);
        mSwipeLayout.setPullMode(SwipeRefreshLayoutUpDown.PullMode.BOTH);
        mSwipeLayout.setLoadNoFull(false);
        //mSwipeLayout.setProgressViewOffset(false, -200, 200);
        //mSwipeLayout.setProgressViewOffset(false, -200,300);
        //mSwipeLayout.setDistanceToTriggerSync(100);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


//    ArrayList<String> values = new ArrayList<String>() {{
//        add("value 0");
//        add("value 1");
//        add("value 2");
//        add("value 3");
//        add("value 4");
//        add("value 5");
//        add("value 6");
//    }};
    ArrayList<String> values = new ArrayList<String>(Arrays.asList(
        "value 0",
        "value 1",
        "value 2",
        "value 3",
        "value 4",
        "value 5",
        "value 6",
        "value 7",
        "value 8",
        "value 9",
        "value 10",
        "value 11",
        "value 12",
        "value 13",
        "value 14",
        "value 15",
        "value 16",
        "value 17",
        "value 18",
        "value 19",
        "value 20",
        "value 21",
        "value 22",
        "value 23",
        "value 24",
        "value 25",
        "value 26",
        "value 27"
)) ;
    @Override
    public void onRefresh() {
//        values.add(0, "Add " + values.size());
//        values.add(0, "Add " + values.size());
//        values.add(0, "Add " + values.size());
//        values.add(0, "Add " + values.size());
//        values.add(0, "Add " + values.size());
//        values.add(0, "Add " + values.size());
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mSwipeLayout.setRefreshing(false);
//                mListAdapter.notifyDataSetChanged();
//            }
//        }, 1000);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
//                    int progressBarHeight = mSwipeLayout.getHeight();
//                    int progressBarBottom = mSwipeLayout.getBottom();
//                    int progressBarTop = mSwipeLayout.getTop();
//                    int top = 40;
//                    mSwipeLayout.setProgressViewOffset(false,
//                            top - progressBarBottom , top);
//                    mSwipeLayout.invalidate();
                    Thread.sleep(500); // 0.5 seconds
                    values.add(0, "Add " + values.size());
                    values.add(0, "Add " + values.size());
                    values.add(0, "Add " + values.size());
                    values.add(0, "Add " + values.size());
                    values.add(0, "Add " + values.size());
                    values.add(0, "Add " + values.size());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                mSwipeLayout.setRefreshing(false);
                mListAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

//    @Override
//    public void onLoad() {
////        values.add("Add " + values.size());
////        values.add("Add " + values.size());
////        values.add("Add " + values.size());
////        values.add("Add " + values.size());
////        values.add("Add " + values.size());
////        values.add("Add " + values.size());
////        values.add("Add " + values.size());
////        new Handler().postDelayed(new Runnable() {
////            @Override
////            public void run() {
////                mSwipeLayout.setLoading(false);
////                mListAdapter.notifyDataSetChanged();
////            }
////        }, 1000);
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                try {
//                    Thread.sleep(500); // 0.5 seconds
//                    values.add("Add " + values.size());
//                    values.add("Add " + values.size());
//                    values.add("Add " + values.size());
//                    values.add("Add " + values.size());
//                    values.add("Add " + values.size());
//                    values.add("Add " + values.size());
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void result) {
//                super.onPostExecute(result);
//                mSwipeLayout.setRefreshing(false);
//                mListAdapter.notifyDataSetChanged();
//            }
//        }.execute();
//
//    }
}
