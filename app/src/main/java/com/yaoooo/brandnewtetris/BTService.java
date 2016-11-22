package com.yaoooo.brandnewtetris;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BTService {
    //debug TAG
    private static final String TAG = "BluetoothChatService";

    //连接使用的名字
    private static final String NAME_SECURE = "BluetoothSecure";
    private static final String NAME_INSECURE = "BluetoothInsecure";
    //连接使用的UUID
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
//    private Intent intent4Controller;

    //当前蓝牙状态的标记
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * 构造函数
     */
    public BTService(Handler handler,BluetoothAdapter mBluetoothAdapter) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;


    }

    /**
     * 设置当前状态
     */
    private synchronized void setState(int state) {
        Log.i("BluetoothChatService", "setState() " + mState + " -> " + state);
        mState = state;

        // 发消息给UI 告知当前连接状态有所改变,把新状态传过去
        mHandler.obtainMessage(MyApp.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * 返回当前状态
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * 开启服务,并且同时打开监听进程
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
//        if (mSecureAcceptThread == null) {
//            mSecureAcceptThread = new AcceptThread(true);
//            mSecureAcceptThread.start();
//        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * 连接到对方设备
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * 连接成功,开始管理与对方设备的连接
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);


        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions   //管理建立好的连接，进行数据传送
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MyApp.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MyApp.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * 停止service中所有进程
     */

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * 这个函数提供给外界来安全地写数据以发送出去
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * 连接失败,重新开始监听,
     * 就是借助重启服务实现
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MyApp.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MyApp.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BTService.this.start();
    }

    /**
     * 连接丢失后进行的一些操作
     */
    private void connectionLost() {
        // 通知UI连接丢失,让UI显示出来
        Message msg = mHandler.obtainMessage(MyApp.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MyApp.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        //重启BT服务
        BTService.this.start();
    }

    /**
     * 监听进程
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {

                    synchronized (BTService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the   通过得到的BluetoothDevice获取一个蓝牙Socket以进行通讯
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BTService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            String msg=null;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    //不使用这个bytes记录长度的话,读取到的字符串尾部就会跟随很多乱码
                    bytes = mInStream.read(buffer);
                    msg = new String(buffer, 0, bytes);
                    /**没有连接到远程设备时,消息发送给WelcomeActivity
                     * 让其Handler处理消息
                     * 当连接到远程设备时候
                     * 直接通过Controller的全局引用来处理消息
                     */
                    if(!MyApp.isConnected) {
                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(MyApp.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                    else{
                        if(MyApp.mController!=null){
                            //在这里面处理完,自动调用handleMsg()
                            preProceMsg(msg,bytes);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();//蓝牙服务善后以及重启
                    Looper.prepare();
                    MyApp.mController.lostConnection();//游戏线程和UI善后
                    Looper.loop();
                    // Start the service over to restart listening mode
                    BTService.this.start();
                    break;
                }
            }
        }

        /**
         * write方法,通过蓝牙通信
         */
        public  void write(byte[] buffer) {
            try {
                if (MyApp.isConnected) {//只有已经连接才能write过去
                    mOutStream.write(buffer);
                    mOutStream.flush();
                }
            } catch (IOException e) {
                Log.e("ConnectedThread.write", "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**处理对方发过来的消息,据此设置对方的shape以供显示
     * length参数是对方发过来的message的长度
     * 根据长度不同,我方的处理也不同
     * length为start或者pause时候暂时在这个函数之外进行的处理
     * 以后再移动进来
     *
     * length现在的可能性有3种   7  8  9
     * length==7 :   top和shadowtop都是一位数
     * length==8 :   top是一位数   shadowtop是两位数
     * length==9 :   top是两位数  shadowtop也是两位数
     * */
    public  void handleMsg(String message,int length){
        Shape tmpShape=new Shape();
        //标记为不是我们边的,deleteLine函数需要用到这个参数
        tmpShape.setOnMySide(false);
        tmpShape.setcolorNum(message.charAt(1) - '0');
        tmpShape.setType(message.charAt(2) - '0');
        tmpShape.setStatus(message.charAt(3) - '0');
        //left要变换   变成  6-left
        tmpShape.setLeft(6 - message.charAt(4) + '0');
        //因为在上面已经设置好了type.  所以现在可以get到type
        tmpShape.setBody(oppositeShapes[tmpShape.getType()]);
        //因为在上面设置好了colorNum  故现在可以get到colorNum设置color了
        tmpShape.setColor(shapeColor[MyApp.theme][tmpShape.getcolorNum()]);

        //top 和 shadowtop要变换成  44-top   44-shadowtop
        if(length%7==0) {
            tmpShape.setTop(44 - message.charAt(5) + '0');
//            tmpShape.setShadowTop(44 - message.charAt(6) + '0');
        }
        else if(length%8==0) {
            tmpShape.setTop(44 - message.charAt(5) + '0');
//            tmpShape.setShadowTop(44 - ((message.charAt(6) - '0') * 10 + (message.charAt(7) - '0')));
        }
        else {
            tmpShape.setTop(44 - ((message.charAt(5) - '0') * 10 + (message.charAt(6) - '0')));
            if (7 == message.length()) {
//                tmpShape.setShadowTop(44 - message.charAt(6) - '0');
            } else {
//                tmpShape.setShadowTop(44 - ((message.charAt(7) - '0') * 10 + (message.charAt(8) - '0')));
            }
        }
        //把根据消息修改好的shape传给oppositeShape
        Controller.setOppositeShape(tmpShape);
    }

    /**
     *  蓝牙的传输问题,有时候会把两个消息连接到
     *  一起发过去,原因不明,所以在这里处理一下
     */
    private void preProceMsg(String msg,int bytes){
        String tmp;
        if(msg==null){
            //因为我最初设置成NULL,所以要处理这种情况,
            return;
        }
        /**该死的蓝牙,消息发送间隔太小就会连接起来一起发送过去
         *就算flush也没用.....
         */
        //这里有可能把accept和moveUp连接起来.
        //这个if放这里是为了跳过下面的长度>10的部分
        if(msg.equals("acceptmoveUp")||msg.equals("moveUpaccept")){}
        else if(msg.equals("acceptaccept")){}
        //上下两者之间是排他的.所以用if  +  else
        else if(msg.length()>=10){
            Log.i("SJDLGJO",msg);
            //消息中包含accept
            if(msg.contains("accept")){
                //有时候甚至会accept和uwin连接到一条...什么鬼蓝牙
                if(msg.contains("uwin")){
                    MyApp.gameStarted=false;//首先停止游戏
                    //弹窗告知我赢了
                    Looper.prepare();
                    MyApp.mController.gameOver(true);
                    Looper.loop();
                }
                //根据msg长度不同,做不同处理
                if(msg.indexOf('a')==0) {//accept在首部
                    //accept在前面的时候,直接accept
                    MyApp.mController.acceptOppShape();
                    if(msg.length()==13)
                        tmp=msg.substring(6,13);
                    else if(msg.length()==14)
                        tmp=msg.substring(6,14);
                    else
                        tmp=msg.substring(6,15);
                }
                else{//accept在中间或者尾部
                    if(msg.indexOf('a')==7)
                        tmp=msg.substring(0,7);
                    else if(msg.indexOf('a')==8)
                        tmp=msg.substring(0,8);
                    else
                        tmp=msg.substring(0,9);
                    msg=tmp;
                    //accept夹在中间或者在最后的时候就handle完message
                    // 以修改当前的oppositeShape再accept
                    //因为消息中排在accept字符串前面的肯定就是
                    //将被接收的Shape的属性
                    handleMsg(msg, bytes);
                    MyApp.mController.acceptOppShape();
                    return;
                    //这里已经调用handleMsg了,所以不需要之后的
                    //处理再次调用handleMsg了,所以return
                }
                msg=tmp;
                Log.i("ConnectedThread.subed",msg);
            }
            //长度>10的消息中不包含accept
            //但是有可能包含uwin
            else if(msg.contains("uwin")){
                //弹窗告知我赢了
                Looper.prepare();
                MyApp.mController.gameOver(true);
                Looper.loop();
                //即使游戏即将结束,也是要处理msg的
                msg=msg.substring(4,msg.length());
            }
            else if(msg.charAt(0)=='m'){//其实这就是moveUp+数字的情况 比如moveUp0000514800006119
                MyApp.mController.shiftGround(true);
                msg="";
            }
        }
        Log.i("ConnectedThread.run()", msg);

        //有可能有这种消息连接起来的奇葩事情.
        //flush也无效...
        if(msg.equals("acceptmoveUp")){
            MyApp.mController.acceptOppShape();
            MyApp.mController.shiftGround(true);
        }
        else if(msg.equals("moveUpaccept")){
            MyApp.mController.shiftGround(true);
            MyApp.mController.acceptOppShape();
        }
        else if(msg.equals("acceptaccept")){
            MyApp.mController.acceptOppShape();
        }
        //我赢了
        else if(msg.equals("uwin")){
            MyApp.gameStarted=false;//首先停止游戏
            //弹窗告知我赢了
            Looper.prepare();
            MyApp.mController.gameOver(true);
            Looper.loop();
        }
        else if(msg.equals("moveUp")){
            //整体ground向上移动一格
            MyApp.mController.shiftGround(true);
        }
        //暂停功能
        else if (msg.equals("pause")) {
            MyApp.gameStarted = false;
            Log.i("serverStarted被改成false", "就是这样");
        }
        else if(msg.equals("startGame")){
            MyApp.gameStarted=true;
        }
        else if(msg.equals("accept")){
            MyApp.mController.acceptOppShape();
        }
        else {
            /**下面要用一个函数对msg进行分析*/
            if (!msg.isEmpty()) {
                MyApp.gameStarted=true;//每收到一个消息,那么就是started为true
                if(bytes>=7) {
                    handleMsg(msg, bytes);
                }
                MyApp.mController.redraw();
            }
        }
    }

    //为了描述对方shape的形状和status  用来存储所有shape的形状
    //这个和ShapeFactory里面的是关于数组中央中心对称的
    private int oppositeShapes[][][]=new int[][][]{
            { //田
                    {0,0,0,0, 0,0,0,0, 0,0,1,1, 0,0,1,1}},
            { // J
                    {0,0,0,0, 0,0,0,0, 0,1,0,0, 0,1,1,1 },
                    {0,0,0,0, 0,0,1,1, 0,0,1,0, 0,0,1,0},
                    {0,0,0,0, 0,0,0,0, 0,1,1,1, 0,0,0,1},
                    {0,0,0,0, 0,0,0,1, 0,0,0,1, 0,0,1,1 }},
            { // L
                    {0,0,0,0, 0,0,0,0, 0,0,0,1, 0,1,1,1 },
                    {0,0,0,0, 0,0,1,0, 0,0,1,0, 0,0,1,1},
                    {0,0,0,0, 0,0,0,0, 0,1,1,1, 0,1,0,0 },
                    {0,0,0,0, 0,0,1,1, 0,0,0,1, 0,0,0,1}},//这一状态在两边的坐标有时候会不一致
            {//04  OK
                    {0,0,0,0, 0,0,0,0, 0,1,1,0, 0,0,1,1 },
                    {0,0,0,1, 0,0,1,1, 0,0,1,0, 0,0,0,0}},
            {//05  OK
                    {0,0,0,0, 0,0,0,0, 0,0,1,1, 0,1,1,0 },
                    {0,0,0,0, 0,0,1,0, 0,0,1,1, 0,0,0,1}},
            {//06
                    {0,0,0,0, 0,0,0,0, 0,1,1,1, 0,0,1,0 },
                    {0,0,0,0, 0,0,0,1, 0,0,1,1, 0,0,0,1},
                    {0,0,0,0, 0,0,0,0, 0,0,1,0, 0,1,1,1},
                    {0,0,0,0, 0,0,1,0, 0,0,1,1, 0,0,1,0}},
            {//07
                    {0,0,0,0, 0,0,0,0, 0,0,0,0, 1,1,1,1},
                    {0,0,0,1, 0,0,0,1, 0,0,0,1, 0,0,0,1}},
    };
    private int shapeColor[][]={
            {Color.WHITE},
            {Color.WHITE},
            {Color.WHITE},
            {Color.parseColor("#2C72C7"),Color.parseColor("#E85A90"),
                    Color.parseColor("#7794C9"),Color.parseColor("#B33D5A"),Color.parseColor("#9BA3AE"),
                    Color.parseColor("#7F90AE"),Color.LTGRAY},
            {Color.rgb(212, 123, 45),
                    Color.rgb(210, 156, 145),Color.rgb(188, 145, 22),
                    Color.rgb(121, 167, 213),Color.GRAY,Color.rgb(32,73,212),
                    Color.rgb(156,0,231)},
            {Color.parseColor("#36A3CA"),Color.parseColor("#E85A90"),
                    Color.parseColor("#7794C9"),Color.parseColor("#B33D5A"),Color.parseColor("#FD8A21"),
                    Color.parseColor("#7F90AE"),Color.rgb(23,199,96)}
    };

}
