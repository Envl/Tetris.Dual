package com.yaoooo.brandnewtetris;

import android.graphics.Color;
import java.util.Random;

public class ShapeFactory {
    int type;//形状的类型   比如一条直的  四个点的方块   等等
    int colorNum;//颜色的编号,代表当前是第几个颜色
    private int shapes[][][]=new int[][][]{
            { //01
                    {1,1,0,0, 1,1,0,0, 0,0,0,0, 0,0,0,0}},
            { //02
                    {1,1,1,0, 0,0,1,0, 0,0,0,0, 0,0,0,0},
                    {0,1,0,0, 0,1,0,0, 1,1,0,0, 0,0,0,0},
                    {1,0,0,0, 1,1,1,0, 0,0,0,0, 0,0,0,0},
                    {1,1,0,0, 1,0,0,0, 1,0,0,0, 0,0,0,0}},
            { // L
                    {1,1,1,0, 1,0,0,0, 0,0,0,0, 0,0,0,0},
                    {1,1,0,0, 0,1,0,0, 0,1,0,0, 0,0,0,0},
                    {0,0,1,0, 1,1,1,0, 0,0,0,0, 0,0,0,0},
                    {1,0,0,0, 1,0,0,0, 1,1,0,0, 0,0,0,0}},
            {//04
                    {1,1,0,0, 0,1,1,0, 0,0,0,0, 0,0,0,0},
                    {0,0,0,0, 0,1,0,0, 1,1,0,0, 1,0,0,0}},

            {//05
                    {0,1,1,0, 1,1,0,0, 0,0,0,0, 0,0,0,0},
                    {1,0,0,0, 1,1,0,0, 0,1,0,0, 0,0,0,0}},
            {//06
                    {0,1,0,0, 1,1,1,0, 0,0,0,0, 0,0,0,0},
                    {1,0,0,0, 1,1,0,0, 1,0,0,0, 0,0,0,0},
                    {1,1,1,0, 0,1,0,0, 0,0,0,0, 0,0,0,0},
                    {0,1,0,0, 1,1,0,0, 0,1,0,0, 0,0,0,0}},
            {//07
                    {1,1,1,1, 0,0,0,0, 0,0,0,0, 0,0,0,0},
                    {1,0,0,0, 1,0,0,0, 1,0,0,0, 1,0,0,0}},
    };


    private int shapeColor[][]={
            //Coffee
            {Color.WHITE},
//            ,Color.parseColor("#5c442b"),
//                    Color.parseColor("#41301f")},
            //GoodData
            {Color.WHITE},
            //wordpress
            {Color.WHITE},
//            Color.parseColor("#1E8CBE"),
//                    Color.parseColor("#F5851F")},
            //GoodData  暂时弃用
            {Color.parseColor("#4A85C5"),Color.parseColor("#F0962F"),
                    Color.parseColor("#EDC516"), Color.parseColor("#7FAEDE")
                    },

            //3
            {Color.rgb(212, 123, 45),
                    Color.rgb(210, 156, 145),Color.rgb(188, 145, 22),
                    Color.rgb(121, 167, 213),Color.GRAY,Color.rgb(32,73,212),
                    Color.rgb(156,0,231)},
            {Color.rgb(66,48,31),Color.rgb(66,48,31),Color.rgb(66,48,31),
                    Color.rgb(66,48,31),Color.rgb(66,48,31),Color.rgb(66,48,31),},
            //4
            {Color.parseColor("#36A3CA"),Color.parseColor("#E85A90"),
                    Color.parseColor("#7794C9"),Color.parseColor("#B33D5A"),Color.parseColor("#FD8A21"),
                    Color.parseColor("#7F90AE"),Color.rgb(23,199,96)}
    };

    public Shape getShape(){
        Shape shape=new Shape();
        type =new Random().nextInt(shapes.length);
        shape.setType(type);
        shape.setBody(shapes[type]);
        shape.setStatus(new Random().nextInt(shapes[type].length));
        //如果这次颜色与上次相同,那么重新再取色一次,减小相同概率
        int tmpColorNum=new Random().nextInt(shapeColor[MyApp.theme].length);
        if(colorNum==tmpColorNum){
            colorNum=new Random().nextInt(shapeColor[MyApp.theme].length);
        }
        else{
            colorNum=tmpColorNum;
        }
        shape.setcolorNum(colorNum);//设置颜色编号
        shape.setColor(shapeColor[MyApp.theme][colorNum]);
        return shape;
    }

    public Shape getShape(ShapeListener listener){
        Shape shape=new Shape();
        shape.addShapeListener(listener);
        type =new Random().nextInt(shapes.length);
        shape.setType(type);
        shape.setBody(shapes[type]);
        shape.setStatus(new Random().nextInt(shapes[type].length));
        colorNum=new Random().nextInt(shapeColor.length);
        shape.setcolorNum(colorNum);//设置颜色编号
        shape.setColor(shapeColor[MyApp.theme][colorNum]);
        return shape;
    }
}
