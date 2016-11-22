package com.yaoooo.brandnewtetris;

public interface ShapeListener {

    void redraw(Shape shape);

    boolean isShapeMoveDownable(Shape shape);

    /**It's obsolete now*///因为需要在ShapeDriver的线程里调用本方法,故将其写入接口中
//    void write(Shape shape);//
    //因为需要在ShapeDriver的线程里调用本方法,故将其写入接口中
    void sendMessage(String str);


    String makeMsg(boolean updateOppShape);
}
