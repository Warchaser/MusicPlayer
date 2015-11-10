package com.warchaser.musicplayer.material;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.warchaser.musicplayer.R;

public class Material extends ActionBarActivity {

    private RecyclerView mRecyclerView;

    private RecyclerAdapter mAdapter;

    private String mData[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.material);

        initializeView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_material, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeData(){
        mData = new String[]{"item1","item2"};
    }

    private void initializeView(){
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        mAdapter = new RecyclerAdapter(this, mData);

    }
}
