package com.example.pc.eliminating_pqb;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends AppCompatActivity implements Runnable{

    private MyDatabaseHelper dbHelper;
    ImageButton[] imgbtn = new ImageButton[8*8];
    private int map[][] = new int[8][8];
    private int mark[][] = new int[8][8];
    TextView text;
    ImageButton cur_Button;
    Point prepoint, nextpoint;
    boolean isPrePoint = true, isSolved = false, update_flag = false;
    private int id, imgNum, alpha = 255, score = 0;
    private Thread thread;
    private MediaPlayer music = null;
    ProgressBar Bar;
    private MyCountDownTimer timer;
    private final long TIME = 10 * 1000L;
    private final long INTERVAL = 1000L;
    double mulHit = 1;

    protected class  MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long time = millisUntilFinished / 1000;
            Bar.setProgress((int) time);
        }

        @Override
        public void onFinish() {
            final EditText inputServer = new EditText(GameActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
            builder.setTitle("留下您的大名").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                            Intent intent = new Intent(GameActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    });
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    SQLiteDatabase db =dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("score",score);
                    values.put("name",inputServer.getText().toString());
                    db.insert("User",null,values);
                    Toast.makeText(GameActivity.this,"Insert success!",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(GameActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });
            builder.setCancelable(false);
            builder.show();
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    private void wait(int time)
    {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void run() {                         //运行线程， 检查是否有可消除方块
        if(find()) {
            int n = 10;
            alpha = 255;
            PlayMusic();
            Log.d("state", "hideBtn");
            while (n-- != 0) {
                wait(80);
                mHandler.sendEmptyMessage(0);
            }
            update_flag = true;                     //是否连续消除
            mulHit+=0.2;
            isSolved = true;                        //是否有过更新
            wait(800);
            mHandler.sendEmptyMessage(1);
        } else if (isSolved == false&&prepoint!=null) {                 //如果已经过更新，两个选中按钮就不必在交换回来， 否则要交换回来
            swapMap(prepoint.x, prepoint.y, nextpoint.x, nextpoint.y);
            wait(300);
            mHandler.sendEmptyMessage(2);
            wait(300);
            mHandler.sendEmptyMessage(3);
        }
    }

    private void PlayMusic(){                       //每一次消除音效
        new Thread(new Runnable() {
            @Override
            public void run() {
                music = MediaPlayer.create(GameActivity.this, R.raw.music);
                music.start();
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        timer.cancel();
        GameActivity.this.finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        dbHelper = new MyDatabaseHelper(this, "User.db", null, 2);            //数据库

        LinearLayout vlayout = (LinearLayout)findViewById(R.id.vlayout);
        TableLayout tlayout = new TableLayout(this);
        Bar = (ProgressBar)findViewById(R.id.bar);
        TableRow row[] = new TableRow[8];
        for(int i=0;i<8;i++){
            row[i] = new TableRow(this);
            row[i].setGravity(Gravity.CENTER);
            for(int j=0;j<8;j++){
                imgbtn[8*i+j] = new ImageButton(this);
                imgbtn[8*i+j].setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
                initBtn(i,j);
                imgbtn[8*i+j].setActivated(false);
                imgbtn[8*i+j].setOnClickListener(listener);
                row[i].addView(imgbtn[8*i+j]);
            }
            tlayout.addView(row[i]);
        }
        text = new TextView(this);
        text.setText("分数 : 0");
        text.setGravity(Gravity.CENTER);
        text.setTextSize(30);
        vlayout.addView(tlayout);
        vlayout.addView(text);

        while(find()){
            Log.d("jie","有找到解");
            updateState();
        }

       if (timer == null) {
            timer = new MyCountDownTimer(TIME, INTERVAL);
        }
        timer.start();
        score = 0;
        text.setText("分数 : 0");
    }

    private void initBtn(int i,int j)
    {
        imgbtn[8*i+j].setBackgroundDrawable
                (this.getResources().getDrawable(getStyle()));
        Point p = new Point(i,j,imgbtn[8*i+j],id);
        imgbtn[8*i+j].setTag(p);
        map[i][j] = imgNum;
    }

    private void swapImage()
    {
        prepoint.v.setBackgroundDrawable
                (GameActivity.this.getResources().getDrawable(nextpoint.id));
        nextpoint.v.setBackgroundDrawable
                (GameActivity.this.getResources().getDrawable(prepoint.id));
        int tmp = prepoint.id;
        prepoint.id = nextpoint.id;
        nextpoint.id = tmp;
        prepoint.v.setTag(prepoint);
        nextpoint.v.setTag(nextpoint);
    }

    public Handler mHandler=new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch(msg.what){
                case 0:{
                    hideBtn();
                    break;
                }
                case 1:{
                    if(update_flag) {

                        updateState();
                        //text.setText("分数 " + ((Integer)score).toString());
                        Thread thread = new Thread(GameActivity.this);
                        new Thread(thread).start();
                    }
                    //thread = new Thread(GameActivity.this);
                    //thread.start();
                    break;
                }
                case 2:{
                    swapImage();
                    prepoint = null;
                    nextpoint = null;
                    break;
                }
                case 3:{
                    updateState();
                    Thread thread = new Thread(GameActivity.this);
                    new Thread(thread).start();
                    Log.d("state", "updateState");
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };


    private int getStyle(){
        imgNum = (int)(1+Math.random()*(6-1+1));
        switch(imgNum){
            case 1:return id = R.drawable.button1;
            case 2:return id = R.drawable.button2;
            case 3:return id = R.drawable.button3;
            case 4:return id = R.drawable.button4;
            case 5:return id = R.drawable.button5;
            case 6:return id = R.drawable.button6;
            default:return id = 0;
        }
    }

    private void updateBtn(int x1,int y1,int x2,int y2)                  //更新imagebutton为其上面剩余的ImageButton
    {
        map[x1][y1] = map[x2][y2];
        Point p = (Point) (imgbtn[8 * x1 + y1].getTag());
        p.id = ((Point) (imgbtn[8 * x2 + y2].getTag())).id;
        imgbtn[8 * x1 + y1].setTag(p);
        imgbtn[8 * x1 + y1].setBackgroundDrawable
                (GameActivity.this.getResources().getDrawable(p.id));
    }

    private void updateBtn(int i,int j)                                 //更新ImageButton为新的随机生成ImageButton
    {
        imgbtn[8*i+j].setBackgroundDrawable
                (GameActivity.this.getResources().getDrawable(getStyle()));
        map[i][j] = imgNum;
        Point p = (Point)(imgbtn[8*i+j].getTag());
        p.id = id;
        imgbtn[8*i+j].setTag(p);
    }

    private void hideBtn(){                                             //使Button的消除有一个渐变的过程
        alpha -= 25;
        for(int i=0;i<8;i++){
            for(int j=0;j<8;j++){
                if(mark[i][j]==1){
                    map[i][j] = -1;
                    imgbtn[8*i+j].getBackground().setAlpha(alpha);
                    imgbtn[8*i+j].setClickable(false);
                    if(alpha<=5)
                        imgbtn[8*i+j].setVisibility(ImageButton.INVISIBLE);
                }
            }
        }
    }


    private void updateState()
    {
        mulHit = 1;
        text.setText("分数 : "+Integer.toString(score));
        for(int i=0;i<8;i++){
            for(int j=0;j<8;j++){
                imgbtn[8*i+j].getBackground().setAlpha(255);
            }
        }
        for(int j=0;j<8;j++){
            int k;
            for(int i=7;i>=0;i--){
                if(mark[i][j]==0)
                {
                    continue;
                }
                else
                {
                    for(k=i-1;k>=0;k--)
                    {
                        if(mark[k][j]==0)
                            break;
                    }
                    if(k<0)
                        updateBtn(i,j);
                    else
                    {
                        mark[k][j] = 2;
                        updateBtn(i,j,k,j);
                    }
                }
            }
        }

        for(int i=0;i<8;i++) {
            for (int j = 0; j < 8; j++) {
                if (mark[i][j] == 1) {
                    imgbtn[8 * i + j].setClickable(true);
                    imgbtn[8*i+j].setVisibility(ImageButton.VISIBLE);
                }
            }
        }
    }

    private boolean find()                      //查找消除的算法， 先查行再查列， 可消除的均做标记
    {
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                mark[i][j] = 0;
            }
        }
        boolean flag = false;
        // every row
        for (int i = 0; i < 8; i++)
        {
            int count = 1;
            for (int j = 0; j < 7; j++)
            {
                if (map[i][j] == map[i][j + 1])
                {
                    count++;
                    if (count == 3)
                    {
                        if(map[i][j]!=-1)
                            flag = true;
                        mark[i][j - 1] = 1;
                        mark[i][j] = 1;
                        mark[i][j + 1] = 1;
                        if(map[i][j]!=-1)
                            score += 15*mulHit;
                    } else if (count > 3)
                    {
                        if(map[i][j]!=-1)
                            flag = true;
                        mark[i][j + 1] = 1;
                        if(map[i][j]!=-1)
                            score += 5*mulHit;
                    }
                }
                else count = 1;
            }
        }

        for (int j = 0; j < 8; j++)
        {
            int count = 1;
            for (int i = 0; i < 7; i++)
            {
                if (map[i][j] == map[i+1][j])
                {
                    count++;
                    if (count == 3)
                    {
                        if(map[i][j]!=-1)
                            flag = true;
                        mark[i-1][j] = 1;
                        mark[i][j] = 1;
                        mark[i+1][j] = 1;
                        if(map[i][j]!=-1)
                            score += 15*mulHit;
                    } else if (count > 3)
                    {
                        if(map[i][j]!=-1)
                            flag = true;
                        mark[i+1][j] = 1;
                        if(map[i][j]!=-1)
                            score += 5*mulHit;
                    }
                }
                else count = 1;
            }
        }
        return flag;
    }

    private void swapMap(int x1,int y1,int x2,int y2)
    {
        int tmp = map[x1][y1];
        map[x1][y1] = map[x2][y2];
        map[x2][y2] = tmp;
    }

    private boolean check(int i,int j){
        int left = i-2>0?(i-2):0;
        int right = i+2<7?(i+2):7;
        int top = j-2>0?(j-2):0;
        int bottom = j+2<7?(j+2):7;
        if(right-left+1>=3) {
            for (int k = left + 1; k < right; k++) {
                if(map[k][j]==map[k-1][j]&&map[k][j]==map[k+1][j])
                    return true;
            }
        }
        if(bottom-top+1>=3) {
            for (int k = top + 1; k < bottom; k++) {
                if(map[k][j]==map[k-1][j]&&map[k][j]==map[k+1][j])
                    return true;
            }
        }
        return false;
    }

    ImageButton.OnClickListener listener = new ImageButton.OnClickListener(){//创建监听对象

        @Override
        public void onClick(View view) {
            if (view.isActivated()) {
                view.setActivated(false);
            } else {
                view.setActivated(true);
            }
            if (isPrePoint) {
                prepoint = (Point) view.getTag();
                isPrePoint = false;
            } else {
                nextpoint = (Point) view.getTag();
                isPrePoint = true;
                if(nextpoint==prepoint) {
                    return;
                }
                if (((prepoint.x - nextpoint.x == 1 || prepoint.x - nextpoint.x == -1) && prepoint.y == nextpoint.y) ||
                        (prepoint.y - nextpoint.y == 1 || prepoint.y - nextpoint.y == -1) && prepoint.x == nextpoint.x) {
                    swapMap(prepoint.x,prepoint.y,nextpoint.x,nextpoint.y);
                    swapImage();
                    isSolved = false;
                    update_flag = false;
                    Thread thread = new Thread(GameActivity.this);
                    new Thread(thread).start();
                    for(int i=0;i<8;i++) {
                        for (int j = 0; j < 8; j++) {
                            imgbtn[8 * i + j].setActivated(false);
                        }
                    }
                }else{
                    prepoint.v.setActivated(false);
                    prepoint = (Point) view.getTag();
                    isPrePoint = false;
                }
            }
        }

    };
}
