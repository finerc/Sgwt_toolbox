package com.example.pc.eliminating_pqb;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class RankActivity extends AppCompatActivity {

    private List<User> userList = new ArrayList<>();
    private MyDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);
        dbHelper = new MyDatabaseHelper(this, "User.db", null, 2);
        initview();
    }

    @Override       //这里是实现了自动更新
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        dbHelper = new MyDatabaseHelper(this, "User.db", null, 2);
        initview();
    }

    protected void initview()
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("User",null,null,null,null,null,"score desc");
        userList.clear();
        int i=1;
        if(cursor.moveToFirst()){
            do{
                User user = new User();
                user.setName(cursor.getString(cursor.getColumnIndex("name")));
                user.setScore(cursor.getInt(cursor.getColumnIndex("score")));
                user.setRank(i);
                userList.add(user);
                i++;
            }while(cursor.moveToNext());
        }
        cursor.close();
        UserAdapter adapter = new UserAdapter(RankActivity.this,R.layout.rank_item, userList);
        ListView listView = (ListView)findViewById(R.id.list_view);
        listView.setAdapter(adapter);
    }
}
