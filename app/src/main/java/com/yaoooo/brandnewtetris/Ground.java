package com.yaoooo.brandnewtetris;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class Ground {
    private int shiftCounter=0;//shift条件计数器.每等于6就清零并shift一次
    private int accelerateCounter=0;//加速条件计数器,每等于20就清零并且加速一次
    private int deleteLineNum=0;  //消除的行数
    public int obstacle[][];//=new int[MyApp.HEIGHT][MyApp.WIDTH];
    public int color[][];//=new int[MyApp.HEIGHT][MyApp.WIDTH];   //颜色数组，保存每格的颜色。
    private int deleteNumAtOnce=0;//接受一个图形时消除的行数

    //初始化obstacle数组
    public void initObstacle(){
        obstacle=new int[MyApp.HEIGHT+32][MyApp.WIDTH+3];
        color=new int[MyApp.HEIGHT+32][MyApp.WIDTH+3];
        if(MyApp.isDual){
            //当是双人模式的时候,就在中间放置两个障碍物
            obstacle[22][2]=1;
            color[22][2]= Color.GRAY;
            obstacle[25][7]=1;
            color[25][7]= Color.GRAY;
        }
        else{//单人模式就在第21排 全部设置成障碍物
            for(int x=0;x<10;x++){
                obstacle[24][x]=1;
            }
        }
    }

    /**
     移动整个ground
     *如果是UP就是向上移动,
     * 否则就是向下移动
     */
    private int i,j;
    public void shiftGround(boolean isUp){
        //如果是UP
        if(isUp) {
            for (i = 4; i <=44; i++) {
                for (j = 0; j < 10; j++) {
                    if (i >= 0 && j >= 0) {//不知为何这里会出现-1的指针 ,所以判断一下
                        obstacle[i][j] = obstacle[i + 1][j];
                        color[i][j] = color[i + 1][j];
                    }
                }
            }
        }
        //否则整体向下移动
        else{
            for ( i = 44; i >= 4; i--) {
                for ( j = 0; j < MyApp.WIDTH; j++) {
                    if (i >= 0 && j >= 0) {//不知为何这里会出现-1的指针 ,所以判断一下
                        obstacle[i][j] = obstacle[i - 1][j];
                        color[i][j] = color[i - 1][j];
                    }
                }
            }
        }
    }

    /**
     * 联机时,在accept对方shape的时候,我方就不会再产生下一个图形了..这里有问题
     *该死的蓝牙,现已基本解决
     * @param shape
     */
    public   void accept(Shape shape){
        for(int y=0;y<4;y++){
            for(int x=0;x<4;x++){
                if(
                        shape.getFlagByPoint(x,y)&&x+shape.getLeft()>=0
                        &&y+shape.getTop()>=0
//                        &&(y+shape.getTop())/100==0
                        ){//这里会出现莫名的负数情况,三位数情况
                    obstacle[shape.getTop()+y][shape.getLeft()+x]=1;
                    color[shape.getTop()+y][shape.getLeft()+x]=shape.getColor();
                }
            }
        }
        //把当前shape的阵营传进去
        /**问题根源出在这里,如果是对方的shape,进行这一操作就会卡死
         * 该死的滥用,现已基本解决*/
        boolean onMySide=shape.isOnMySide();
        if(onMySide) {
            deleteFullLine();
        }
        else{
            oppDeleteFullLine();
        }
    }

    /**千万不要改,这里已经OK*/
    public void drawMe(Canvas canvas){
        Paint paint=new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<MyApp.BOTTOM;i++){
            for(int j=0;j<MyApp.WIDTH;j++){
                if(obstacle[i+4][j]==1){
                    paint.setColor(color[i+4][j]);
                    if(MyApp.canvasBG){
                        paint.setAlpha(200);
                    }
                    canvas.drawRect(MyApp.scrnLeft+j*MyApp.CELL_SIZE,
                            MyApp.scrnTop+   i*MyApp.CELL_SIZE,
                            MyApp.scrnLeft+(j+1)*MyApp.CELL_SIZE,
                            MyApp.scrnTop+ (i+1)*MyApp.CELL_SIZE,paint);
                }
            }
        }
    }

    /**对方shape造成的delete*/
    public void oppDeleteFullLine(){
        deleteNumAtOnce=0;//计数器清零
        for (int i = 4; i <= 44; i++) {
            boolean full = true;
            for (int j = 0; j < MyApp.WIDTH; j++) {
                if(i>=0&&j>=0) {//不知为何这里会出现-1的指针 ,所以判断一下
                    if (obstacle[i][j] == 0)
                        full = false;
                }
            }
            if (full) {
                deleteLine(i, false);
                deleteNumAtOnce++;
                i--;
            }
        }
    }
    /**
     * 我方shape造成的delete事件
     * */
    public void deleteFullLine() {
        deleteNumAtOnce=0;//计数器清零
        for (int i = MyApp.HEIGHT - 1; i >= 4; i--) {
            boolean full = true;
            for (int j = 0; j < MyApp.WIDTH; j++) {
                if(i>=0&&j>=0) {//不知为何这里会出现-1的指针 ,所以判断一下
                    if (obstacle[i][j] == 0)
                        full = false;
                }
            }
            if (full) {
                deleteLine(i, true);
                accelerateCounter++;//下落速度控制
                deleteLineNum++;//这个是消除的行数计数
                shiftCounter++;//这个是ground是否shift的条件计数器
                deleteNumAtOnce++;
                i++;
            }
        }
        switch (deleteNumAtOnce){
            case 1:
                MyApp.mController.getSp().play(MyApp.mController.soundID_delete1,
                        1,1,0,0,1);
                break;
            case 2:
                MyApp.mController.getSp().play(MyApp.mController.soundID_delete2,
                        1,1,0,0,1);
                break;
            case 3:
                MyApp.mController.getSp().play(MyApp.mController.soundID_delete3,
                        1,1,0,0,1);
                break;
            case   4:
                MyApp.mController.getSp().play(MyApp.mController.soundID_delete4,
                        1,1,0,0,1);
                break;
        }
    }

    //根据造成消行的当前shape所属阵营决定delete方向
    private void deleteLine(int lineNum,boolean isOnMySide){
        //如果属于我方阵营
        if(isOnMySide) {
            for (int i = lineNum; i >= 4; i--) {
                for (int j = 0; j < MyApp.WIDTH; j++) {
                    obstacle[i][j] = obstacle[i - 1][j];
                    color[i][j] = color[i - 1][j];
                }
            }
        }
        //属于对方阵营
        else{
            for (int i = lineNum; i <= 44; i++) {
                for (int j = 0; j < MyApp.WIDTH; j++) {
                    if(i>=0&&j>=0) {
                        obstacle[i][j] = obstacle[i + 1][j];
                        color[i][j] = color[i + 1][j];
                    }
                }
            }
        }
    }

    /**根据移动方向与是否旋转决定能否移动*/
    public boolean isMovable(Shape shape, int action ){
        int left=shape.getLeft();
        int top=shape.getTop();
        switch (action) {
            case Shape.LEFT:
                left--;
                break;
            case Shape.RIGHT:
                left++;
                break;
            case Shape.DOWN:
                top++;
                break;
        }
        /**由于需要两个判断方式,根据单人和双人决定采用哪一种
         * 同时还在这里面判定了是否射穿.
         * */
        if(MyApp.isDual){
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    if (shape.isMember(x, y, action == Shape.ROTATE)) {
                        //射穿判断
                        if(top+y-4>MyApp.BOTTOM){
                            shape.isThrough=true;
                            return  false;
                        }
                        //不能移动判断
                        if(left + x < 0
                                || left + x >= MyApp.WIDTH
                                || obstacle[top + y][left + x] == 1)//obstacle和top一一对应,故不用减
                            return false;
                    }
                }
            }
        }
        else {//单人模式
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    if (shape.isMember(x, y, action == Shape.ROTATE)) {
                        if (top + y  > MyApp.HEIGHT//单人模式的HEIGHT是24 .
                                || left + x < 0
                                || left + x >= MyApp.WIDTH
                                || obstacle[top + y][left + x] == 1)//obstacle和top一一对应,故不用减
                            return false;
                    }
                }
            }
        }
        return true;
    }

    /**不要再修改,已经OK*/
    //计算阴影top位置
    public void shadowTop(Shape shape) {
        int distance[]={48,48,48,48};
        int minDistance;
        int k=0;
        for (int y = 0; y < 4; y++){
            for (int x = 0;x < 4; x++) {
                if(shape.getFlagByPoint(x,y)){
                    for(int i=shape.getTop()+y+1;i<=MyApp.HEIGHT;i++){//因为单人模式需要扫描到21行,所以取等于
                        if(obstacle[i][shape.getLeft()+x]==1){
                            distance[k]=i-shape.getTop()-y-1;
                            k++;
                            break;
                        }
                    }
                }
            }
        }
        minDistance=min(min(distance[0],distance[1]),min(distance[2],distance[3]));
        shape.setShadowTop(shape.getTop()+minDistance);
    }

    /**计算对方阴影top*/
    public void oppShadowTop(Shape shape){
        int distance[]={48,48,48,48};
        int minDistance;
        int k=0;
        for (int y = 0; y < 4; y++){
            for (int x = 0;x < 4; x++) {
                if(shape.getFlagByPoint(x,y)){
                    for(int i=shape.getTop()+y+1;i>=4;i--){
                        if(obstacle[i][shape.getLeft()+x]==1){
                            distance[k]=shape.getTop()+y-i-1;
                            k++;
                            break;
                        }
                    }
                }
            }
        }
        minDistance=min(min(distance[0],distance[1]),min(distance[2],distance[3]));
        shape.setShadowTop(shape.getTop()-minDistance);
    }

    public int min(int a,int b){
        if(a>=b)
            return b;
        else
            return a;
    }

    //判断游戏结束
    public boolean isOver() {
        for (int i = 0; i < MyApp.WIDTH; i++) {
            if (obstacle[3][i] == 1) {
                //结束了,但是先sleep一下,否则可能accept和uwin消息挤到一起连接起来了
                try {
                    Thread.sleep(50);
                }catch (Exception e){
                    Log.i("Ground.isOver()","Thread.sleep出错");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 在双人游戏中
     * 同时借用这个函数,根据分数决定是否要shiftGround
     * @return
     */
    public int getDeleteLineNum(){
        //如果是双人游戏
        if(MyApp.isDual) {
            //每消2行,获得整体下移的资格
            if (shiftCounter>=3) {
                shiftCounter=0;//条件计数清零
                //通知对方整体上移一格
                MyApp.mController.sendMessage("moveUp");
                //自己整体下移一格
                shiftGround(false);
            }
        }
        else{//如果是单人游戏,就会随着分数提高而加速下落
            if(accelerateCounter>=10){//每得到10分就加速一个单位
                accelerateCounter=0;
                MyApp.autoDownTime--;
            }
        }
        return deleteLineNum;}
}
