package com.yaoooo.brandnewtetris;

/**
 * Created by 铭 on 2015/5/15.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.security.auth.callback.Callback;

public class  GameView extends SurfaceView implements SurfaceHolder.Callback,Callback {
    private SurfaceHolder sfh;
    private Canvas canvas;
    private Paint paint;

    public void setiReturnWelcome(IReturnWelcome iReturnWelcome) {
        this.iReturnWelcome = iReturnWelcome;
    }
    public IReturnWelcome iReturnWelcome;
    //    private Ground ground;
    private Shape shape;
    private Shape nextShape;
    private Shape secondNextShape;
    private Shape thirdNextShape;
    private Shape oppositeNextShape;
    /**记录两张背景图片时时更新的Y坐标**/
    public int mBitposY0 =0;
    public int mBitposY1 =-MyApp.mBitmapHeight;
    private double nextShapesScale=0.7;

    public void initSize(){
        //设置字体大小
        paint.setTextSize(2 * MyApp.CELL_SIZE);
        MyApp.scrnLeft=2*MyApp.CELL_SIZE;
        MyApp.scrnTop=2*MyApp.CELL_SIZE;
        if(MyApp.isDual){
            //如果是双人游戏,那么就要 x3 因为dual的cell_size是单人的2/3
            paint.setTextSize(3 * MyApp.CELL_SIZE);
            MyApp.scrnLeft=3*MyApp.CELL_SIZE;
        }
    }

    public GameView(Context context,AttributeSet attrs){
        super(context, attrs);
        sfh=this.getHolder();
        sfh.addCallback(this);
        paint=new Paint();
        initSize();
        paint.setTextAlign(Paint.Align.CENTER);
    }

    //画背景网格
    public void drawFlag(Canvas canvas,Paint paint){
        for(int i=0;i<=MyApp.BOTTOM;i++){
            canvas.drawLine(MyApp.scrnLeft,
                    MyApp.scrnTop+MyApp.CELL_SIZE*i,
                    MyApp.scrnLeft+MyApp.WIDTH*MyApp.CELL_SIZE,
                    MyApp.scrnTop+MyApp.CELL_SIZE*i,paint);
        }
        for(int i=0;i<=MyApp.WIDTH;i++){
            canvas.drawLine(MyApp.scrnLeft+MyApp.CELL_SIZE*i,
                    MyApp.scrnTop,
                    MyApp.scrnLeft+MyApp.CELL_SIZE*i,
                    MyApp.scrnTop+(MyApp.BOTTOM)*MyApp.CELL_SIZE,paint);
        }
    }

    public void myDraw(Shape shape,Shape oppositeShape,Shape nextShape,
            Shape secondNextShape,Shape thirdNextShape,Shape oppositeNextShape,
                       Bitmap mGameBGStatic,Bitmap mGameBGFlow){
        try{
//            this.ground=ground;
            this.shape=shape;
            this.nextShape=nextShape;
            this.secondNextShape=secondNextShape;
            this.thirdNextShape=thirdNextShape;
            this.oppositeNextShape=oppositeNextShape;

            canvas=sfh.lockCanvas();

            if(canvas!=null){
                this.setKeepScreenOn(true);
                /** 绘制游戏背景 **/
                //静态背景
                if(MyApp.theme==MyApp.THEME_GOODDATA){//canvas绘制背景 红色主题,加白色方块
                    paint.setColor(Color.parseColor("#96443E"));
                    canvas.drawRect(0, 0, MyApp.mScreenWidth, MyApp.mScreenHeight, paint);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(200);
                }
                else if(MyApp.theme==MyApp.THEME_Wordpress){//蓝色主题
                    paint.setColor(Color.parseColor("#08445A"));
                    canvas.drawRect(0, 0, MyApp.mScreenWidth, MyApp.mScreenHeight, paint);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(200);
                }
                else if(MyApp.theme==MyApp.THEME_COFFEE){//根据位图绘制背景
                    paint.setColor(Color.parseColor("#70461C"));
                    canvas.drawRect(0, 0, MyApp.mScreenWidth, MyApp.mScreenHeight, paint);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(200);
                }
                //动态背景
                canvas.drawBitmap(mGameBGFlow, 0, mBitposY0, paint);
                canvas.drawBitmap(mGameBGFlow, 0, mBitposY1, paint);
                if (mBitposY0 >= MyApp.mScreenHeight) {
                    mBitposY0 = -MyApp.mScreenHeight;
                }
                if (mBitposY1 >= MyApp.mScreenHeight) {
                    mBitposY1 = -MyApp.mScreenHeight;
                }
                //网格
                drawFlag(canvas, paint);
                /**画出自己的图形*/
                //显示得分
                canvas.drawText("Score",
                        MyApp.scrnLeft+14*MyApp.CELL_SIZE,
                        MyApp.scrnTop+13*MyApp.CELL_SIZE,paint);
                canvas.drawText("" + MyApp.mGround.getDeleteLineNum(),
                        MyApp.scrnLeft + 14 * MyApp.CELL_SIZE,
                        MyApp.scrnTop + 16 * MyApp.CELL_SIZE, paint);
                //画出障碍物
                MyApp.mGround.drawMe(canvas);
                 //画出当前图形
                shape.drawMe(canvas,1);
                //给接下来的图形画线框
                paint.setStrokeWidth(2);
                canvas.drawLine(MyApp.scrnLeft + (float)(10.5 * MyApp.CELL_SIZE ),
                        MyApp.scrnTop + (float)(4.2* MyApp.CELL_SIZE) ,
                        MyApp.scrnLeft + (float)(14.5 * MyApp.CELL_SIZE) ,
                        MyApp.scrnTop +  (float)(4.2* MyApp.CELL_SIZE),paint);
                canvas.drawLine(MyApp.scrnLeft + (float)(14.5 * MyApp.CELL_SIZE),
                        MyApp.scrnTop +  (float)(4.2* MyApp.CELL_SIZE) ,
                        MyApp.scrnLeft + (float)(14.5 * MyApp.CELL_SIZE) ,
                        MyApp.scrnTop + 8* MyApp.CELL_SIZE ,paint);
                //画出接下来的图形
                nextShape.drawMe(canvas,0.8);
                secondNextShape.drawMe(canvas, nextShapesScale);
                thirdNextShape.drawMe(canvas, nextShapesScale);
                //画出阴影
                MyApp.mGround.shadowTop(shape);
                shape.drawShadow(canvas);

                /**画出对方的图形*/
                if(MyApp.isDual) {
                    if (oppositeShape != null) {//非空才画
                        oppositeShape.drawMe(canvas,1);
                        oppositeShape.drawShadow(canvas);
                    }
                    //画出对方next图形
//                    if (oppositeNextShape != null) {
//                        oppositeNextShape.drawMe(canvas);
//                    }
                }
            }
        }catch(Exception e){
        }finally {
            if(canvas!=null)
                sfh.unlockCanvasAndPost(canvas);
        }
    }

    /**个myDraw专门给ConnectedThread使用的.
     *
     * 但是现在的问题就是,一旦调用这个函数,联机游戏时候就会莫名
     * 有一方停止出现下一个shape  只能显示对方的shape
     *
     * ConnectedThread一旦收到消息,就会立马调用这个函数
     * 于是能够做到实时在我这边同步accept对方的shape
     * 这个函数同时根据doAccept的值,来决定要不要对
     * 对方的shape执行accept操作
     * */
    public   void myDraw(Shape oppositeShape){
        try{
            canvas=sfh.lockCanvas();
            if(canvas!=null){
                //背景涂白
                canvas.drawRGB(255, 255, 255);
                //网格
                drawFlag(canvas, paint);

                /**画出自己的图形*/
                //显示得分
                canvas.drawText("Score",
                        MyApp.scrnLeft+15*MyApp.CELL_SIZE,
                        MyApp.scrnTop+13*MyApp.CELL_SIZE,paint);
                canvas.drawText(""+MyApp.mGround.getDeleteLineNum(),
                        MyApp.scrnLeft+15 * MyApp.CELL_SIZE,
                        MyApp.scrnTop+16 * MyApp.CELL_SIZE, paint);
                //画出当前图形
                shape.drawMe(canvas,1);
                nextShape.drawMe(canvas,nextShapesScale);
                //画出阴影
                MyApp.mGround.shadowTop(this.shape);
                shape.drawShadow(canvas);

                /**画出对方的图形*/
                oppositeShape.drawMe(canvas,nextShapesScale);
                //因为对方传过来的shape已经处理过,
                // 是包含阴影位置的,所以我不需要计算其阴影位置了
//                   this.ground.shadowTop(oppositeShape);
                oppositeShape.drawShadow(canvas);

                /**只要在这里accpet对方的shape,那么我方就会"卡"死*/
                //如果storedOppositeShape非空,那么就accept之
//                if(oppositeShape.isDoAccept()){
//                    MyApp.mGround.accept(oppositeShape);
//                }

                //画出障碍物
                MyApp.mGround.drawMe(canvas);
            }
        }catch(Exception e){
        }finally {
            if(canvas!=null)
                sfh.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
