package com.yaoooo.brandnewtetris;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

/**
 * Created by 铭 on 2015/5/15.
 */
public class MyApp extends Application{
    public static int theme=1;
        final public static int THEME_COFFEE=0;
        final public static int THEME_GOODDATA=1;
        final public static int THEME_Wordpress=2;

    public static Bundle savedInstanceState;
    public static boolean canvasBG=true;//表示背景是否由canvas绘制
    public static int autoDownTime=40;//是自动下落间隔时间,随着难度增加而减少
    public static Bitmap mGameBGFlow=null;//用于向下不断移动的图
    public static Bitmap mGameBGStatic=null;//静态背景
    public static int  mScreenWidth=0;
    public static int  mScreenHeight=0;
    public static int mBitmapHeight=0;
    public static boolean isNewGame=false;//表示这是一次新游戏
    public static boolean userBack=false;//用户按返回或者主页按钮,退出了游戏界面
    public static BTService mBTService=null;
    public static Activity welcomeActivity=null;
    public static Activity gameActivity=null;
    public static int scrnTop;
    public static int scrnLeft;
    public static Ground mGround;
    public static Ground mOppGround;
    public static Controller mController=null;//controller的全局引用
    public static  int CELL_SIZE=1;
    public static boolean gameStarted=false;//游戏主机是否已经开始游戏
    public static boolean isServer=false;//用来标识自己是否主机
    public static Context welcomeContext;
    public static Context gameContext;
    public static BluetoothSocket mBTSocket;
    public static boolean isConnected=false;//表示是否连接到对方主机
    public static int mState=0;
    public static boolean isDual=false;//是否双人游戏
    public static int BOTTOM=20;
    public static int HEIGHT=24;
    public static final int WIDTH=10;
    public static boolean updateGameBG=false;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
}
