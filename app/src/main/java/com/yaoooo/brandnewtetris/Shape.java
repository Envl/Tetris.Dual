package com.yaoooo.brandnewtetris;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Shape {
    private int body[][];
    private int status;
    private int colorNum;//当前颜色在我的颜色数组中的编号
    //用来标记对方的shape是否需要被我方接收
    private boolean doAccept=false;//这个参数只对对方有效
    private int type;
    private boolean onMySide=true;
    private ShapeListener listener;
    private int color;    //shape的颜色  这是真正的RGB值
    private int left=4;
    private int top=0;
    private int shadowTop=48; //阴影top
    public  boolean isThrough=false;//是否射穿了,不能是静态的,因为每个shape都是重新生成的

    public final static int ROTATE =0;
    public final static int LEFT=1;
    public final static int RIGHT=2;
    public final static int DOWN=3;
    public final static int UP=4;


    public boolean isDoAccept() {
        return doAccept;
    }
    public void setDoAccept(boolean doAccept) {
        this.doAccept = doAccept;
    }
    /**
     * 在传给对方之前要调用这个函数
     * 更改Shape的阵营为对方阵营
     * @param onMySide
     */
    public void setOnMySide(boolean onMySide) {
        this.onMySide = onMySide;
    }
    public boolean isOnMySide() {
        return onMySide;
    }
    public ShapeListener getListener() {
        return listener;
    }
    public void moveUP(){top--;}
    public void moveLeft(){
        left--;
    }
    public void moveRight(){
        left++;
    }
    public void moveDown(){
        top++;
    }
    public void rotate(){
        status=(status+1)%body.length;
    }

    /**直接下落方块*/
    public void drop(){
        if(shadowTop<=42){
            top=shadowTop;
        }
        else{
            this.isThrough=true;//否则就是射穿了
            setTop(80);
        }
    }

    public void drawMe(Canvas canvas,double scale){
        Paint paint=new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        if(MyApp.canvasBG){
            paint.setAlpha(200);
            paint.setColor(Color.WHITE);
        }
        int tmpSize=MyApp.CELL_SIZE;
        tmpSize*=scale;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                if (getFlagByPoint(x, y)) {
                    if ((top + y >= 4) && (top + y <= 43)) {//加上这个条件防止双人时候出界了的绘画
                        /**这个是画自己本体*/
                        canvas.drawRect(MyApp.scrnLeft + left * MyApp.CELL_SIZE + x * tmpSize,
                                MyApp.scrnTop + (top) * MyApp.CELL_SIZE + (y - 4) * tmpSize,
                                MyApp.scrnLeft + (left) * MyApp.CELL_SIZE + (x + 1) * tmpSize,
                                MyApp.scrnTop + (top) * MyApp.CELL_SIZE + (y - 3) * tmpSize, paint);
                    }
                }
            }
        }
    }

    public void drawShadow(Canvas canvas){
        Paint paint=new Paint();
        paint.setColor(color);
       paint.setAntiAlias(true);
        if(MyApp.canvasBG) {
            paint.setStyle(Paint.Style.FILL);
        }else{
            paint.setStyle(Paint.Style.STROKE);
        }
        paint.setStrokeWidth(4);
//        paint.setAlpha(120);    //设置透明度
        for(int y=0;y<4;y++) {
            for (int x = 0; x < 4; x++) {
                //shadowTop<底部  >顶部才画出来
                if (getFlagByPoint(x, y)&&shadowTop<MyApp.BOTTOM+4
                        ) {
                    canvas.drawRect(MyApp.scrnLeft+(left + x) * MyApp.CELL_SIZE,
                            MyApp.scrnTop+ (shadowTop + y-4) * MyApp.CELL_SIZE,
                            MyApp.scrnLeft+ (left + x + 1) * MyApp.CELL_SIZE,
                            MyApp.scrnTop+ (shadowTop + y -3) * MyApp.CELL_SIZE, paint);

                }
            }
        }
    }
    //返回这个点是不是4*4矩阵中shape的一部分
    boolean getFlagByPoint(int x, int y){
        return body[status][y*4+x]==1;
    }

    public boolean isMember(int x,int y,boolean isRotate){
        int tempStatus=status;
        if(isRotate){
            tempStatus=(status+1)%body.length;
        }
        return body[tempStatus][y*4+x]==1;
    }

    public void addShapeListener(ShapeListener l){
        if(l!=null){
            this.listener=l;
        }
    }

    public Integer getTop(){
        return top;
    }
    public void setTop(int top){
        this.top=top;
    }

    public Integer getLeft(){ return left; }
    public void setLeft(int left){this.left=left;}

    public Integer getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public void setBody(int[][] body) {
        this.body = body;
    }

    public Integer getcolorNum() {
        return colorNum;
    }
    public void setcolorNum(int newColorNum){
        this.colorNum=newColorNum;
    }

    public void setColor(int color) {
        this.color = color;
    }
    public int getColor(){
        return this.color;
    }
    public Integer getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Integer getShadowTop() {
        return shadowTop;
    }
    public void setShadowTop(int shadowTop) {
        this.shadowTop = shadowTop;
    }
}
