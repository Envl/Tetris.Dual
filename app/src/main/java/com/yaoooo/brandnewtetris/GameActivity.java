package com.yaoooo.brandnewtetris;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.io.IOException;


public class GameActivity extends Activity implements View.OnLongClickListener,View.OnClickListener,IReturnWelcome{
    ShapeFactory shapeFactory=new ShapeFactory();
    Ground ground=new Ground();
    Ground oppGround=new Ground();
    Controller controller;
    public GameView mGameView;
    Button btnLeft;
    Button btnRight;
    Button btnRotate;
    Button btnDrop;
    private MediaPlayer game_music_bg;            //游戏背景音乐
    int lastTouchX=0;
    int lastTouchY=0;//上次onTouch事件时候的x和y值
    boolean isFirstTouch=true;

    //重写的IReturnWelcome接口的方法,以让gameview调用到
    public  void returnWelcome(){
        finish();
    }
    public Activity getGA(){
        return this;
    }

    protected void onPause(){
        super.onPause();
        if(MyApp.isDual){
            finish();
            MyApp.mBTService.stop();
        }
        MyApp.mController.controllerAlive=false;//要利用这个flag关闭Controller的线程.
         game_music_bg.pause();
        finish();
    }

    protected  void onResume(){
        super.onResume();
        //根据屏幕分辨率决定单位的大小
        changeUnitbyWidth(getApplicationContext());
        if(mGameView!=null){
            //有了单位大小之后,就能初始化Size了
            mGameView.initSize();
            //把自己传给gameview.这样就能通过gameview调用我的函数了
            mGameView.setiReturnWelcome(this);
            game_music_bg.start();
        }
    }
    protected void onDestroy(){
        game_music_bg.stop();
        game_music_bg.release();
        super.onDestroy();

    }
    /**有空再写*/
    //截获用户按下返回键
    public void onBackPressed(){
//        MyApp.mController.onBackPressed();
        MyApp.userBack=true;
        onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_game);
        MyApp.gameActivity=this;
        //环境变量传给全局引用
        MyApp.gameContext=getApplicationContext();
        //按钮实例化并且连接到监听器
        btnLeft=(Button)findViewById(R.id.btn_left);
        btnLeft.setOnLongClickListener(this);
//               btnLeft .setOnTouchListener(this);
        btnLeft.setOnClickListener(this);
        btnDrop=(Button)findViewById(R.id.btn_drop);
//               btnDrop .setOnTouchListener(this);
        btnDrop.setOnClickListener(this);
        btnRight=(Button)findViewById(R.id.btn_right);
        btnRight.setOnLongClickListener(this);
//        btnRight.setOnTouchListener(this);
        btnRight.setOnClickListener(this);
        btnRotate=(Button)findViewById(R.id.btn_rotate);
//              btnRotate  .setOnTouchListener(this);
        btnRotate.setOnClickListener(this);

        //如果是canvas绘制背景,那么按钮改成白色的
        if(MyApp.canvasBG){
            btnRotate.setBackgroundResource(R.drawable.wu);
            btnDrop.setBackgroundResource(R.drawable.wd);
            btnLeft.setBackgroundResource(R.drawable.wl);
            btnRight.setBackgroundResource(R.drawable.wr);
        }
        mGameView=(GameView)findViewById(R.id.gameView);
        //根据屏幕分辨率决定单位的大小
        changeUnitbyWidth(getApplicationContext());//这里面获取到了屏幕宽高
        //Controller建立时,已经有了屏幕宽高信息
        controller=new Controller(shapeFactory,ground,oppGround,mGameView);
        //controller传递给全局引用
        MyApp.mController=controller;
        controller.newGame();

        // 实例化背景音乐并循环播放
        game_music_bg= MediaPlayer.create(this, R.raw.gamebg);
        game_music_bg.setLooping(true);                     //设为循环播放
        try {
            game_music_bg.prepare();
        } catch (IllegalStateException e){
            Log.i("GameActivity.Create", "准备背景音乐出错");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //设置按钮的监听器
//    @Override
//    public boolean onTouch(View v,MotionEvent m) {
////        if (m.getAction() == MotionEvent.ACTION_DOWN) {
////            if (m.getX() - lastTouchX < 200&&!isFirstTouch) {
////           int x=1;
////            }
////            else {
//                controller.onTouch(v, m);
////            }
////        }
//        if(m.getAction()==MotionEvent.ACTION_UP){
//            lastTouchX=0;
//        }
//        return false;
//        /**
//         return true了,那么触屏事件就不会向下分发了,
//         touch优先级高于onclick
//         所以这里要return false
//         让onclick函数得到调用.
//         */
//    }
    @Override
    public void onClick(View v){
        controller.onClick(v);
    }
    @Override
    public boolean onLongClick(View v){
        controller.onLongClick(v);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        controller.onTouchEvent(event);
        return true;
    }


    //根据屏幕宽度改变unit大小
    public static void changeUnitbyWidth(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        MyApp.mScreenHeight=size.y;
        MyApp.mScreenWidth=size.x;
        //单机默认是下面这个
        MyApp.CELL_SIZE=size.x/20;
        //双人就是下面这样
        if(MyApp.isDual)
            MyApp.CELL_SIZE=size.x/30;
    }
}
