package com.skylight.apollo;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.kandaovr.sdk.util.Constants;
import com.skylight.apollo.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BrowseActivity extends AppCompatActivity {

    private List<MediaItem> mItems;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);
        setTitle(R.string.activity_browse);

        mListView = (ListView)findViewById(R.id.listview);
        new LoadPlaylistTask().execute();
    }

    private void displayItems(List<MediaItem> items){
        mItems = items;

        List<String> listData = new ArrayList<>();
        for(MediaItem item: items){
            listData.add(item.path);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                listData);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                MediaItem item = mItems.get(position);
                Intent intent = new Intent(BrowseActivity.this, ReplayActivity.class);
                intent.putExtra("mediaType", item.type);
                intent.putExtra("mediaPath", item.path);
                startActivity(intent);
            }
        });
    }

    private class MediaItem {
        public int type;
        public String path;

        public MediaItem(int type, String path){
            this.type = type;
            this.path = path;
        }
    }

    private class LoadPlaylistTask extends AsyncTask<Void, Void, List<MediaItem>> {

        private Exception mException = null;

        @Override
        protected List<MediaItem> doInBackground(Void... params) {
            try {
                // load the list of videos
                List<MediaItem> result = new ArrayList<MediaItem>();
                File files[] = new File(Util.VIDEO_DIR).listFiles();
                if(files != null){
                    for(File file : files){
                        result.add(new MediaItem(Constants.MEDIA_TYPE_VIDEO, file.getAbsolutePath()));
                    }
                }

                // load the list of pictures
                files = new File(Util.PICTURE_DIR).listFiles();
                if(files != null){
                    for(File file : files){
                        result.add(new MediaItem(Constants.MEDIA_TYPE_PICTURE, file.getAbsolutePath()));
                    }
                }
                return result;
            } catch (Exception exception) {
                mException = exception;
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<MediaItem> result) {

            if(isCancelled()){
                return;
            }

            if(result == null){
                Toast.makeText(BrowseActivity.this, "Cannot load files", Toast.LENGTH_SHORT).show();
                return;
            }

            displayItems(result);
        }
    }
}
