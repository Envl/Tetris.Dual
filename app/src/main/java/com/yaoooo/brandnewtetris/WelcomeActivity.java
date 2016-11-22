package com.yaoooo.brandnewtetris;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Created by 铭 on 2015/5/15.
 */
public class WelcomeActivity extends ActionBarActivity {
    //用于在Log中标明  当前操作发生在本 类中   打上这个TAG 便于调试
    private static final String TAG = "BluetoothChatFragment";
    // 请求代码
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;//请求连接设备
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;//请求启用蓝牙

    private ImageView mImageView;
    /**按键*/
    private Button mSingleButton;
    private Button mCreateButton;
    private Button mJoinButton;
    private Button mWordpress;
    private Button mGoodData;
    private Button mCoffee;

    /**保存对方设备名字*/
    private String mConnectedDeviceName = null;

    /**蓝牙适配器*/
    private BluetoothAdapter mBluetoothAdapter;
    /**蓝牙服务,不要这个类内的变量了,只要MyApp里面的全局变量*/
//    private BTService mBTService = null;
    /**初始化控件*/
    private void initViews(){
        mImageView=(ImageView)findViewById(R.id.iv_theme);
        /**实例化按钮*/
        mSingleButton = (Button) findViewById(R.id.btn_single);
        mCreateButton = (Button) findViewById(R.id.btn_create);
        mJoinButton = (Button) findViewById(R.id.btn_join);
        mWordpress=(Button)findViewById(R.id.btn_wordpress);
        mGoodData=(Button)findViewById(R.id.btn_gooddata);
        mCoffee=(Button)findViewById(R.id.btn_coffee);
        /**设置监听*/
        mWordpress.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                mImageView.setBackgroundResource(R.drawable.color_wordpress);
                MyApp.canvasBG=true;
                MyApp.theme=MyApp.THEME_Wordpress;
                mSingleButton.setBackgroundResource(R.drawable.btn_bg_wordpress);
                mCreateButton.setBackgroundResource(R.drawable.btn_bg_wordpress);
                mJoinButton.setBackgroundResource(R.drawable.btn_bg_wordpress);
            }
        });
        mCoffee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.canvasBG=true;
                MyApp.theme=MyApp.THEME_COFFEE;
                mImageView.setBackgroundResource(R.drawable.btn_bg_coffee);
                mSingleButton.setBackgroundResource(R.drawable.btn_bg_coffee);
                mCreateButton.setBackgroundResource(R.drawable.btn_bg_coffee);
                mJoinButton.setBackgroundResource(R.drawable.btn_bg_coffee);
            }
        });
        mGoodData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApp.canvasBG=true;
                mImageView.setBackgroundResource(R.drawable.btn_bg_purple);
                MyApp.theme=MyApp.THEME_GOODDATA;
                mSingleButton.setBackgroundResource(R.drawable.btn_bg_purple);
                mCreateButton.setBackgroundResource(R.drawable.btn_bg_purple);
                mJoinButton.setBackgroundResource(R.drawable.btn_bg_purple);
            }
        });
        mSingleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initAttri(1);//1代表单机
                Intent intent = new Intent(WelcomeActivity.this, GameActivity.class);
                startActivity(intent);
                MyApp.isNewGame=true;
            }
        });
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initAttri(2);//2代表创建游戏
                //开启蓝牙,启动蓝牙服务
                setupBTandService();
                MyApp.isNewGame=true;
                Toast.makeText(getApplicationContext(),
                        "正在等待对手连接",Toast.LENGTH_SHORT).show();

            }
        });
        mJoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initAttri(3);//3代表加入游戏
                //开启蓝牙,启动蓝牙服务
                setupBTandService();
                //启动搜索设备的activity   之后会在获取到设备MAC以后打开连接线程
                MyApp.isNewGame=true;
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                //现在默认使用insecure的静默连接方式
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            }
        });
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_welcome);
        //在全局变量存储WelcomeActivityContext
        MyApp.welcomeContext = getApplicationContext();
        //初始化所有控件
        initViews();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (MyApp.mBTService!= null) {//退出程序的时候检查服务是否停止,若没有,则停止服务.
            MyApp.mBTService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MyApp.updateGameBG=false;
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (MyApp.mBTService != null) {

            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (MyApp.mBTService.getState() == BTService.STATE_NONE) {

                //Resume回软件时,如果发现ChatService是STATE_NONE时,就要重新start该    ChatService
                // Start the Bluetooth chat services
                MyApp.mBTService.start();
            }
        }

    }
    /**
     * 初始化一些参数
     1  MyApp.BOTTOM
     2  MyApp.isDual
     3    MyApp.HEIGHT
     4  MyApp.isServer
     5  MyApp.serverStarted=false;  联机默认暂停游戏,因为不同设备的启动时间
     不一定相同,于是先暂停,都加载完毕游戏才同步开始
     6 MyApp.mBTSocket
     * @param mode
     * mode==1   单机
     * mode==2   创建游戏
     * mode==3   加入游戏
     */
    private void initAttri(int mode){
        MyApp.userBack=false;
        if(mode==1){
            MyApp.BOTTOM=20;
            MyApp.isDual=false;
            MyApp.HEIGHT=24;
            MyApp.gameStarted=true;
        }
        else if(mode==2){
            MyApp.isDual=true;
            MyApp.autoDownTime=30;
            MyApp.BOTTOM=40;
            MyApp.HEIGHT=48;
            MyApp.isServer=true;
            MyApp.gameStarted=false;
        }
        else if(mode==3){
            MyApp.isDual=true;
            MyApp.autoDownTime=30;
            MyApp.BOTTOM=40;
            MyApp.HEIGHT=48;
            MyApp.isServer=false;
            MyApp.gameStarted=false;
        }
    }
    /**
     * 获取蓝牙适配器,并且启动蓝牙服务
     */
    private void setupBTandService(){
        //实例化蓝牙适配器
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        //本设备不支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "蓝牙不可用", Toast.LENGTH_LONG).show();
        }
        else {//支持蓝牙才会打开蓝牙服务
            if(MyApp.isServer) {
                //如果是主机保证可被检测到
                ensureDiscoverable();
            }
            if (MyApp.mBTService == null) {
                //如果服务没有启动,那么启动服务
                //打开蓝牙
                if (!mBluetoothAdapter.isEnabled()) {
                    //静默打开蓝牙
                    mBluetoothAdapter.enable();
                    Toast.makeText(this,"正在打开蓝牙~",Toast.LENGTH_SHORT).show();
                    try{
                        Thread.sleep(4000);//需要花一定时间等待蓝牙开启
                    }
                    catch (Exception e){

                    }
                }  MyApp.mBTService = new BTService(mHandler,mBluetoothAdapter);
            }
            //打开蓝牙
            if (!mBluetoothAdapter.isEnabled()) {
                //静默打开蓝牙
                mBluetoothAdapter.enable();
            }
            //启动服务开始监听
            MyApp.mBTService.start();
        }
    }


    /**
     * 保证蓝牙可被检测到
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) { //SCAN MODE 就是指 能不能被连接  能不能被搜索到
            //在这里就是如果发现  本机蓝牙 不能被发现被连接  就将其设置成可以被发现和连接的状态
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * 这个Handler获取从 BluetoothChatService返回的信息
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Activity activity=getParent();
            switch (msg.what) {
                case MyApp.MESSAGE_STATE_CHANGE: //当前状态改变
                    switch (msg.arg1) {
                        case BTService.STATE_CONNECTED:
                            /**
                             * 成功连接到远程设备*
                             *在这里需要做:
                             *  1 打开GameActivity
                             *  2 MyApp.isConnected=true
                             */
                            if(MyApp.isServer) {
                                Toast.makeText(getApplicationContext(), "敌人正在赶来的路上", Toast.LENGTH_SHORT).show();
                            }
                            Intent  intent=new Intent(WelcomeActivity.this, GameActivity.class);
                            startActivity(intent);
                            MyApp.isConnected=true;
                            break;
                        case BTService.STATE_CONNECTING:
                            break;
                        case BTService.STATE_LISTEN:
                        case BTService.STATE_NONE:
                            break;
                    }
                    break;
                case MyApp.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MyApp.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case MyApp.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(MyApp.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                //Service 借用这个handle函数发送Toast
                case MyApp.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(MyApp.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
    /**startActivityForResult()调用出来的Activity,  finish()以后就会调用这个函数
     * 并且在调用出来的Activity    finish()之前
     * 使用 setResult(int resultCode, Intent data)函数就能传递
     * resultCode和 一个包含数据的Intent给本函数以实现信息的传递.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this,"Secure",Toast.LENGTH_SHORT).show();
                    connectDevice(data, true);

                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                /**目前我就是用的这种方式,*/
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this,"敌人正在赶来的路上",Toast.LENGTH_SHORT).show();
                    connectDevice(data, false);//date里面就是对方的蓝牙MAC地址

                }
                break;
            case REQUEST_ENABLE_BT:
                //通过request方式成功打开了蓝牙
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled,
                    //setupChat();
                    /**蓝牙已打开

                     */
                } else {
                    // User did not enable Bluetooth or an error occurred
//                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "蓝牙没打开",
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
        }

    }
    /**
     * 尝试连接对方设备
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        MyApp.mBTService.connect(device, secure);
    }
}
