package com.yaoooo.brandnewtetris;

/**
 * Created by 铭 on 2015/5/15.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

public class Controller extends Thread  {
    //public class Controller  implements ShapeListener{
    final Handler mHandler = new Handler();
    private boolean isDual;
    public boolean controllerAlive=true;//定义当前Controller是否存活,用这个flag控制线程的存亡
    private Shape shape=null;//自己的shape\
    private Shape nextShape=null;//自己的下一个shape
    private Shape secondNextShape=null;//下下个shape
    private Shape thirdNextShape=null;//下下下个shape'
    private   static Ground ground;//public为了在BTService中使用

    public static void setOppositeShape(Shape newOppShape) {
        Controller.oppositeShape = newOppShape;
        ground.oppShadowTop(oppositeShape);

    }

    private static Shape toBeAccepted=null;//用来保存将要被accpet的对方shape
    private static    Shape oppositeShape;//对方的Shape,
    private static Shape oppositeNextShape;
    private int counter=0;
    private String Msg="";//这个负责及其之间蓝牙传递信息
    private String previousMsg="";//存储上一个message,如果与当前message匹配,那么就不发送.

    public SoundPool getSp() {
        return sp;
    }

    private SoundPool sp;           //声明SoundPool,用于播放音效
    //记录音乐文件ID
    public int soundID_action,soundID_dowm,soundID_drop,soundID_rotation,
    soundID_delete1,soundID_delete2,soundID_delete3,soundID_delete4;
    private ShapeFactory shapeFactory;
    private  Ground oppGround;
    private GameView mGameView;
    private float startX,startY;
    private float endX ,endY;
    private boolean isTouched=false;//是否触摸按钮
    private boolean isFirstTouch=false;
    private boolean repeating=false;
    private boolean isRepeated=false;//是否进行过repeaWork函数中的自动下落行为
    /**接收对方shape*/
    public void acceptOppShape(){
        //蓝牙连接会有不可靠的时候,所以我自己计算对方shape的shadowTop然后accetp之
        ground.oppShadowTop(oppositeShape);
        //然后把对方shape的top改成对方shadowTop
        oppositeShape.setTop(oppositeShape.getShadowTop());
        //接收之
        ground.accept(oppositeShape);
    }


    /**构造函数*/
    public Controller(ShapeFactory shapeFactory,Ground ground,Ground oppGround,GameView gameView){
        this.shapeFactory=shapeFactory;
        this.ground=ground;
        this.oppGround=oppGround;
        this.mGameView=gameView;
        Activity gameActivity=MyApp.gameActivity;
        isDual=MyApp.isDual;
        MyApp.mGround=this.ground;
        MyApp.mOppGround=oppGround;
        //实例化SoundPool
        sp = new SoundPool(6, AudioManager.STREAM_MUSIC,0);
        //加载音乐文件获取其数据ID;
        soundID_action=sp.load(gameActivity,R.raw.action,1);
        soundID_dowm=sp.load(gameActivity,R.raw.down,1);
        soundID_drop=sp.load(gameActivity,R.raw.drop,1);
        soundID_rotation=sp.load(gameActivity, R.raw.rotation, 1);
        soundID_delete1=sp.load(gameActivity, R.raw.delete1, 1);
        soundID_delete2=sp.load(gameActivity, R.raw.delete2, 1);
        soundID_delete3=sp.load(gameActivity, R.raw.delete3, 1);
        soundID_delete4=sp.load(gameActivity, R.raw.delete4, 1);


        //根据主题加载不同背景
        if(MyApp.theme==MyApp.THEME_GOODDATA){
            MyApp.mGameBGStatic=BitmapFactory.decodeResource(gameActivity.getResources(),
                    R.raw.gradientbg);
            MyApp.mGameBGStatic= resizeBitmap(MyApp.mGameBGStatic);
        }else if(MyApp.theme==MyApp.THEME_COFFEE){
            MyApp.mGameBGStatic=BitmapFactory.decodeResource(gameActivity.getResources(),
                    R.raw.gradientbg);
            MyApp.mGameBGStatic= resizeBitmap(MyApp.mGameBGStatic);
        }else if(MyApp.theme==MyApp.THEME_Wordpress) {
            MyApp.mGameBGStatic = BitmapFactory.decodeResource(gameActivity.getResources(),
                    R.raw.gradientbg);
            MyApp.mGameBGStatic= resizeBitmap(MyApp.mGameBGStatic);
        }
        //加载流动背景
        MyApp.mGameBGFlow = BitmapFactory.decodeResource(gameActivity.getResources(),
                R.raw.transparentbg);
        //把图片变得和屏幕大小一致
        MyApp.mGameBGFlow= resizeBitmap(MyApp.mGameBGFlow);
    }
    private  Bitmap resizeBitmap(Bitmap bm){
        int width=bm.getWidth();
        int heignt=bm.getHeight();
        float scaleRateW=(float)MyApp.mScreenWidth/(float)width;
        float scaleRateH=(float)MyApp.mScreenHeight/(float)heignt;
        Matrix matrix=new Matrix();
        matrix.postScale(scaleRateW, scaleRateH);
        Bitmap tmp=Bitmap.createBitmap(bm,0,0,width,heignt,matrix,true);
        bm.recycle();
        return tmp;
    }

    public void redraw(){
        mGameView.myDraw(shape, oppositeShape, nextShape,
                secondNextShape,thirdNextShape, oppositeNextShape,
                MyApp.mGameBGStatic,MyApp.mGameBGFlow);
    }

    public void newGame(){
        //一定要初始化Ground的障碍物数组
        ground.initObstacle();
        oppGround.initObstacle();
        shape=shapeFactory.getShape();
        nextShape=shapeFactory.getShape();
        secondNextShape=shapeFactory.getShape();
        thirdNextShape=shapeFactory.getShape();
        nextShape.setTop(4);
        nextShape.setLeft(11);
        secondNextShape.setTop(4);
        secondNextShape.setLeft(15);
        thirdNextShape.setTop(8);
        thirdNextShape.setLeft(15);
        //Controller已经初始化完成,那么就可以画背景
        MyApp.updateGameBG=true;
        mGameView.mBitposY1= -MyApp.mScreenHeight;

        /**这里要根据是单人游戏还是双人游戏来决定是否要暂停一下
         * 双人游戏暂停是为了保证双方能够同步开始
         * 因为机器性能不同,游戏初始化所要花费的时间不同
         * */
        if(isDual) {
            oppositeShape=null;
            if(MyApp.isServer)//如果是主机才执行暂停界面
                serverPauseDialog();
        }
        this.start();
    }

    private void repeatWork(final String direction) {
        new Thread() {
            @Override
            public void run() {
                isRepeated=false;
                repeating=true;
                while (isTouched) {
                 if(direction.equals("l")){
                     if (ground.isMovable(shape, Shape.LEFT)) {
//                         MyApp.mController.sendMessage(makeMsg(false));
                         sp.play(soundID_action, 1, 1, 0, 0, 1);
                         shape.moveLeft();
                     }
                     else{
                         isTouched=false;
                     }
                 }
                 else{
                     if (ground.isMovable(shape, Shape.RIGHT)) {
//                         MyApp.mController.sendMessage(makeMsg(false));
                         sp.play(soundID_action, 1, 1, 0, 0, 1);
                         shape.moveRight();
                     }
                     else{
                         isTouched=false;
                     }
                 }
//                        sp.play(soundID_action,1,1,0,0,1);
                    try {
                    Thread.sleep(60);
                        isRepeated=true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
                repeating=false;//除了while循环,说明没有在repeat了
            }
        }.start();
    }
    public void onClick(View v){
        if(MyApp.gameStarted) {//主机开始了游戏才响应用户的操作.
            switch (v.getId()) {
                case R.id.btn_left:
                    if (ground.isMovable(shape, Shape.LEFT)) {
                        MyApp.mController.sendMessage(makeMsg(false));
                        shape.moveLeft();
                        new Thread(){
                            public void run(){
                                sp.play(soundID_action, 1, 1, 0, 0, 1);
                            }
                        }.start();
                    }
                    break;
                case R.id.btn_right:
                    if (ground.isMovable(shape, Shape.RIGHT)) {
                        MyApp.mController.sendMessage(makeMsg(false));
                        shape.moveRight();
                        new Thread(){
                            public void run(){
                                sp.play(soundID_action, 1, 1, 0, 0, 1);
                            }
                        }.start();
                    }
                    break;
                case R.id.btn_rotate:
                    if (ground.isMovable(shape, Shape.ROTATE)) {
                        MyApp.mController.sendMessage(makeMsg(false));
                        shape.rotate();
                        new Thread(){
                            public void run(){
                                sp.play(soundID_rotation, 1, 1, 0, 0, 1);
                            }
                        }.start();
                    }
                    break;
                case R.id.btn_drop:
                    shape.drop();
                    MyApp.mController.sendMessage(makeMsg(true));
                    counter = 60;//设置counter进入run的循环.借用run来检测能否下落
                    new Thread(){
                        public void run(){
                            sp.play(soundID_drop, 1, 1, 0, 0, 1);
                        }
                    }.start();
                    break;

            }
//            mGameView.myDraw(shape,oppositeShape,nextShape,
//                    secondNextShape,thirdNextShape ,oppositeNextShape,MyApp.mGameBG0,MyApp.mGameBG0);
        }
    }
    public void onLongClick(View v){
        if(v.getId()==R.id.btn_left){
            isTouched=true;
            repeatWork("l");
        }
        else if(v.getId()==R.id.btn_right){
            isTouched=true;
            repeatWork("r");
        }
    }
//    public void onTouch(View v,MotionEvent e) {
//        if(e.getAction()==MotionEvent.ACTION_DOWN)
//        switch (v.getId()) {
//            case R.id.btn_left:
//                if (ground.isMovable(shape, Shape.LEFT)) {
////                    MyApp.mController.sendMessage(makeMsg(false));
//                    shape.moveLeft();
////                    new Thread() {
////                        public void run() {
////                            sp.play(soundID_action, 1, 1, 0, 0, 1);
////                        }
////                    }.start();
//                }
//                break;
//            case R.id.btn_right:
//                if (ground.isMovable(shape, Shape.RIGHT)) {
////                    MyApp.mController.sendMessage(makeMsg(false));
//                    shape.moveRight();
////                    new Thread() {
////                        public void run() {
////                            sp.play(soundID_action, 1, 1, 0, 0, 1);
////                        }
////                    }.start();
//                }
//                break;
//        }
//
//    }

    public boolean onTouchEvent(MotionEvent event){
        int X=(int)event.getX();
        int Y=(int)event.getY();
        if(MyApp.gameStarted) {//主机开始了游戏才响应用户的操作.
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = X;
                    startY = Y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    endX = X;
                    endY = Y;
                    float dx = Math.abs(endX - startX);
                    float dy = Math.abs(endY - startY);
                    if (dx >=2*dy&& dx-dy>=4 ) {
                        if (endX - startX > 0) {
                            if((endX-startX)*3/(MyApp.CELL_SIZE)>=1) {
                                if (ground.isMovable(shape, Shape.RIGHT)) {
                                    startX=X;
                                    sp.play(soundID_action,1,1,0,0,1);
                                    shape.moveRight();
                                }
                            }
                        } else {
                            if((endX-startX)*3/(MyApp.CELL_SIZE)<=-1) {
                                if (ground.isMovable(shape, Shape.LEFT)) {
                                    sp.play(soundID_action,1,1,0,0,1);
                                    shape.moveLeft();
                                    startX=X;
                                }
                            }
                        }
                        //对自己的shape进行操作以后也要发送给对方
//                        MyApp.mController.sendMessage(makeMsg(false));
//                        mGameView.myDraw(shape, oppositeShape, nextShape,
//                                secondNextShape,thirdNextShape,oppositeNextShape,MyApp.mGameBG0,MyApp.mGameBG0);

                    }
                    else {
                        if (endY - startY > 18) {
                            if((endY-startY)*3/(MyApp.CELL_SIZE)>=1){
                                if (isShapeMoveDownable(shape)) {
                                    sp.play(soundID_dowm,1,1,0,0,1);
                                    shape.moveDown();
                                    startY=Y;
                                }
                            }
                        }
                        //对自己的shape进行操作以后也要发送给对方
//                        MyApp.mController.sendMessage(makeMsg(false));
//                        mGameView.myDraw(shape, oppositeShape, nextShape,
//                                secondNextShape,thirdNextShape,oppositeNextShape,MyApp.mGameBG0,MyApp.mGameBG0);

                    }
            }
            MyApp.mController.sendMessage(makeMsg(false));

        }
       return true;
    }

    public void run(){
        while (controllerAlive&&!ground.isOver()){
            if(counter>=MyApp.autoDownTime) {//通过counter来控制自动下落时间间隔
                counter=0;//归零重新计时
//                mGameView.myDraw(shape, oppositeShape, nextShape,
//                        secondNextShape,thirdNextShape,oppositeNextShape,MyApp.mGameBG0,MyApp.mGameBG0);
                if (isShapeMoveDownable(shape)) {
                    if (MyApp.gameStarted) {
                        shape.moveDown();
                        MyApp.mController.sendMessage(makeMsg(false));
                    }
                }
            }
            try {
                Thread.sleep(10);
                mGameView.mBitposY1+=3;
                mGameView.mBitposY0+=3;
                redraw();//重绘整个画面
                counter++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /**
         * 运行到这里,说明游戏结束
         * 因为如果对手赢了,BTService
         * 就执行了一次gameOver函数
         * 并且设置gameStarted=false
         * 那么就不需要
         * 在这里再次执行了,
         */
        if(MyApp.gameStarted) {
            Looper.prepare();
            mHandler.post(new Runnable() {
                public void run() {
                    gameOver(false);
                }
            });
            Looper.loop();
        }

    }

    //封装一下用于在ConnectedThread中调用
    public void shiftGround(boolean isUp){
        ground.shiftGround(isUp);
    }

    public   boolean isShapeMoveDownable(Shape shape) {

        /**如果使用ShapeDriver,那么这里是必须存在的,否则就会出现拥挤在空中的情况
         * 但是,目前还看不太懂为什么*/
//        if(this.shape!=shape){
//            return false;
//        }

        if (ground.isMovable(shape, Shape.DOWN)) {
            return true;
        }
        //没有射穿才接收,否则不接收
        if(!shape.isThrough) {

            MyApp.mController.sendMessage(makeMsg(true));//这里要accept, 所以要通知对方accept消息
            ground.accept(shape);
            MyApp.mController.sendMessage("accept");
        }

        //接收或者射穿之后,那么就需要产生新的shape
        if(!ground.isOver()){
            this.shape=nextShape;
            this.shape.setLeft(4);
            this.shape.setTop(1);
            nextShape=secondNextShape;
            secondNextShape=thirdNextShape;
            thirdNextShape=shapeFactory.getShape();
            nextShape.setTop(4);
            nextShape.setLeft(11);
            secondNextShape.setTop(4);
            secondNextShape.setLeft(15);
            thirdNextShape.setTop(8);
            thirdNextShape.setLeft(15);
        }
        return false;
    }

    /**现在采用传送字符串的方式发消息*/
    public  synchronized void sendMessage(String tmp) {
        if (isDual&&MyApp.isConnected&&(!tmp.equals(""))) {
            //是双人游戏且连接到对方主机,且消息长度非零才发送
            MyApp.mBTService.write(tmp.getBytes());
        }
    }

    /**根据当前shape制作message以供传输
     * 参数doAccept表示是否需要接收对方Shape
     * */
    public String makeMsg(boolean doAccept){
        //第零步 previousMsg存储当前message
        previousMsg=Msg;
        //第一步,信息清空
        Msg="";
        //第0位.  0表示不接收  1代表接收
        //然而现在我并没有再使用这一位数据
        //历史遗留问题.  ... it's obsolete now
        if(doAccept){
            Msg+="1";
        }
        else {
            Msg += "0";
        }
        //第1位,颜色  是一个 一位数字
        Msg+=shape.getcolorNum().toString();
        //第2位,类型   哪种形状  是一个 一位数字
        Msg+= shape.getType( ).toString();
        //第3位,状态   形状的状态  是一个 一位数字
        Msg+= shape.getStatus( ).toString();
        //第4位,left  是一个 一位数字
        //这里实际上是0的时候有可能传过去的是-1  原因不明
        Msg+= String.valueOf(shape.getLeft( ));//所以我传过去之前强行abs
        //第5和第6位, top   可能一位  可能两位
        Msg+= shape.getTop().toString();
        //第7位和第8位,  shadowtop     可能一位   可能两位
        Msg+= shape.getShadowTop().toString();
        //第9位 isMovedownable判断请求
//        message+=

        //如果message长度超过9那么就要substring 不知为何会有message含有两份自己
        if(Msg.length()>9){
            Msg=Msg.substring(0,9);
        }
        //如果和上次消息内容相同,那么就没必要再传.
        if(previousMsg.equals(Msg)){
            return "";
        }
        else {
            return Msg;
        }
    }

    /**
     *  只有游戏过程中断开连接才执行这个函数
     *  如果游戏结束,那么isConnected就会被
     *  设置成,false ,此时不需要执行这个函数
     */
    public void lostConnection(){
        MyApp.isConnected = false;//此时连接已经丢失
            AlertDialog.Builder builder = new AlertDialog.Builder(mGameView.getContext());
            builder.setMessage("对方离开了游戏");
            builder.setTitle("//(ToT)//");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //这里要做一些游戏结束的处理

                    //关闭弹出窗口
                    dialog.dismiss();
                    //回到欢迎界面
                    mGameView.iReturnWelcome.returnWelcome();
                }
            });
            //用户必须点击
            builder.setCancelable(false);
        if(MyApp.gameStarted) {
            builder.create().show();
        }
    }
    /**游戏结束*/
    public void gameOver(boolean isWin){
        sp.release();//结束游戏时候,释放音效资源
        AlertDialog.Builder builder = new AlertDialog.Builder(mGameView.getContext());
        if(isWin){
            builder.setMessage("就是这么厉害~\n"+"总分"+ground.getDeleteLineNum());
        }
        else{
            builder.setMessage("不要哭~\n" + "总分" + ground.getDeleteLineNum());
            MyApp.mController.sendMessage("uwin");//通知对方我输了
        }
        MyApp.isConnected=false;//通知对方后,连接断开
        /**释放图片资源*/
        MyApp.mGameBGStatic.recycle();
        MyApp.mGameBGFlow.recycle();
        MyApp.mGameBGStatic=null;
        MyApp.mGameBGFlow=null;

        builder.setTitle("游戏结束");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //这里要做一些游戏结束的处理

                //关闭弹出窗口
                dialog.dismiss();
                //杀死游戏线程
                controllerAlive = false;
                //关闭蓝牙服务
                if (isDual) {
                    MyApp.mBTService.stop();
                }
                //回到欢迎界面
                mGameView.iReturnWelcome.returnWelcome();
            }
        });
        //用户必须点击
        builder.setCancelable(false);
        if(!MyApp.userBack) {//如果是用户点击返回键,那么就不弹出窗口
            builder.create().show();
        }
        MyApp.gameStarted = false;
    }
    /**暂停游戏*/
    protected void serverPauseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mGameView.getContext());
        builder.setMessage("请开始游戏");
        builder.setTitle("游戏等待中");
        builder.setPositiveButton("开始游戏", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //开始游戏
                //通知对方更改paused状态
                //传送start表示开始游戏.终止暂停状态
                MyApp.mController.sendMessage("startGame");
                //也让自己的开始游戏标志为真
                MyApp.gameStarted = true;
                dialog.dismiss();
            }
        });
        //用户必须点击
        builder.setCancelable(false);
        builder.create().show();
    }
    /**这个有空再写*/
    //通过这个间接调用GameActivity的该函数
    public void onBackPressed(){

    }
}
