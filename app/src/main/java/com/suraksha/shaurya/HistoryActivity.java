package com.suraksha.shaurya;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.suraksha.shaurya.db.AppDB;
import com.suraksha.shaurya.db.AppEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private AppDB database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        database = AppDB.createDatabase(this);
        List<AppEntity> history = database.getDao().fetchChatRooms();
        if (history.size() == 0) {
            findViewById(R.id.no_history).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.no_history).setVisibility(View.GONE);
        }
        Adapter adapter = new Adapter(history);
        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(adapter);
    }

    class Adapter extends BaseAdapter {
        private final List<AppEntity> mList;

        Adapter(List<AppEntity> list) {
            this.mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public AppEntity getItem(int i) {
            return mList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater mInflater = (LayoutInflater)
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = mInflater.inflate(R.layout.item_history, null);
            }

            TextView time = view.findViewById(R.id.time);
            TextView number = view.findViewById(R.id.number);
            TextView message = view.findViewById(R.id.message);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
            Date humanReadable = new Date(mList.get(position).getTime());

            time.setText(sdf.format(humanReadable));
            number.setText(mList.get(position).getReceiverNumber());
            message.setText(mList.get(position).getMessage());

            return view;
        }
    }
}