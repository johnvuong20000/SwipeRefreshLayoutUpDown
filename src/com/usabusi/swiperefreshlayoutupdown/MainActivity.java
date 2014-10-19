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
import com.usabusi.swiperefreshlayoutupdown.view.SwipeRefreshLayoutUpDown.OnLoadListener;
import com.usabusi.swiperefreshlayoutupdown.view.SwipeRefreshLayoutUpDown.OnRefreshListener;


@SuppressLint("ResourceAsColor")
public class MainActivity extends Activity implements OnRefreshListener, OnLoadListener{

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
        mSwipeLayout.setOnLoadListener(this);
        mSwipeLayout.setColor(android.R.color.holo_blue_bright,
	                            android.R.color.holo_green_light,
	                            android.R.color.holo_orange_light,
	                            android.R.color.holo_red_light);
        mSwipeLayout.setMode(SwipeRefreshLayoutUpDown.Mode.BOTH);
        mSwipeLayout.setLoadNoFull(false);
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
        "value 6"
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

    @Override
    public void onLoad() {
//        values.add("Add " + values.size());
//        values.add("Add " + values.size());
//        values.add("Add " + values.size());
//        values.add("Add " + values.size());
//        values.add("Add " + values.size());
//        values.add("Add " + values.size());
//        values.add("Add " + values.size());
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mSwipeLayout.setLoading(false);
//                mListAdapter.notifyDataSetChanged();
//            }
//        }, 1000);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(500); // 0.5 seconds
                    values.add("Add " + values.size());
                    values.add("Add " + values.size());
                    values.add("Add " + values.size());
                    values.add("Add " + values.size());
                    values.add("Add " + values.size());
                    values.add("Add " + values.size());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                mSwipeLayout.setLoading(false);
                mListAdapter.notifyDataSetChanged();
            }
        }.execute();

    }
}
