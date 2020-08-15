package my.boxman;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;

public class myRecogViewMap extends View {

    myRecogView m_Recog;            //父控件指针，以便使用父控件的功能
    ArrayList<Point> curPoints = new ArrayList<Point>();  //识别出来的物件
    Rect cur_Rect = new Rect();  //点击的格子

    Paint myPaint = new Paint();
    int m_nArenaTop;                //舞台 Top 距屏幕顶的距离
    int m_nPicWidth, m_nPicHeight;  //关卡图的像素尺寸
    int m_nMapTop, m_nMapLeft;      //图片有效区域的左上角
    float m_nWidth = 50;            //素材尺寸，即关卡图每个格子的像素尺寸
    int m_nLine_Color = 0;          //网格线颜色
    int m_nObj = -1;                //选中的物件（XSB元素）

    public Matrix mMatrix = new Matrix();         //图片原始变换矩阵
    public Matrix mCurrentMatrix = new Matrix();  //当前变换矩阵
    private Matrix mMapMatrix = new Matrix();     //onDraw()用的当前变换矩阵
    float m_fTop, m_fLeft, m_fScale, mScale;      //关卡图的当前上边界、左边界、缩放倍数；原始缩放倍数
    public float mMaxScale = 9;                   //最大缩放级别
    float[] values = new float[9];

    // 识别参数默认值
    int toleranceValueColor          = 270;    // 颜色容差
    int toleranceValueDifferentColor = 9;      // 误差点数

    final char[] myXSB = { '-', '#', '$', '*', '.', '@', '+' };

    public myRecogViewMap(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myRecogViewMap(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myRecogViewMap(Context context) {
        super(context);

        setOnTouchListener(new TouchListener());
        initView();
    }

    private void initView() {
        m_nArenaTop = 0;
    }

    //计算原始变换矩阵
    private Matrix getInnerMatrix(Matrix matrix) {
        if (matrix == null) matrix = new Matrix();
        else matrix.reset();
        //原图大小
        RectF tempSrc = new RectF(0, 0, m_nPicWidth, m_nPicHeight);
        //控件大小
        RectF tempDst = new RectF(0, 0, getWidth(), getHeight() - m_nArenaTop);
        //计算fit center矩阵
        matrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);

        return matrix;
    }

    //缩放、拖拽
    private class TouchListener implements OnTouchListener {
        private static final int MODE_NONE   = 0;
        private static final int MODE_DRAG   = 1;  //拖动模式
        private static final int MODE_ZOOM   = 2;  //缩放模式
        private int mMode = MODE_NONE;      //当前模式

        private float mStartDis;  //缩放开始时的手指间距
        private PointF mStartPoint = new PointF();  //触点坐标
        private PointF mid = new PointF();  //手势中心点

        private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            //单击，抬起时触发
            public boolean onSingleTapUp(MotionEvent e) {

                float x = (e.getX() - m_fLeft) / m_fScale;
                float y = (e.getY() - m_fTop ) / m_fScale;

                int xx = (int) x, yy = (int) y, r, c;

                r = (int) ((yy - m_nMapTop ) / m_nWidth);
                c = (int) ((xx - m_nMapLeft) / m_nWidth);

                xx = (int) (c * m_nWidth + m_nMapLeft);
                yy = (int) (r * m_nWidth + m_nMapTop);

                cur_Rect.set(xx, yy, (int) (xx + m_nWidth), (int) (yy + m_nWidth));   // 设置样本格子 -- 焦点区域
                cur_Rect.set(xx, yy, (int) (xx + m_nWidth), (int) (yy + m_nWidth));   // 设置样本格子 -- 焦点区域

                if (m_nObj >= 0) {    // 手动 XSB 模式
                    m_Recog.myBackup();
                    if (m_nObj == 4) {  // 若选中的物件是“目标点”
                        if (m_Recog.m_cArray[r][c] == '@') {
                            m_Recog.m_cArray[r][c] = '+';
                        } else if (m_Recog.m_cArray[r][c] == '+') {
                            m_Recog.m_cArray[r][c] = '@';
                        } else if (m_Recog.m_cArray[r][c] == '$') {
                            m_Recog.m_cArray[r][c] = '*';
                        } else if (m_Recog.m_cArray[r][c] == '*') {
                            m_Recog.m_cArray[r][c] = '$';
                        } else if (m_Recog.m_cArray[r][c] == '.') {
                            m_Recog.m_cArray[r][c] = '-';
                        } else {
                            m_Recog.m_cArray[r][c] = '.';
                        }
                    } else if (m_nObj == 5) {  // 若选中的物件是“仓管员”
                        for (int i = 0; i < m_Recog.m_cArray.length; i++) {
                            for (int j = 0; j < m_Recog.m_cArray.length; j++) {
                                if (i != r || j != c) {
                                    if (m_Recog.m_cArray[i][j] == '@') {
                                        m_Recog.m_cArray[i][j] = '-';
                                    } else if (m_Recog.m_cArray[i][j] == '+') {
                                        m_Recog.m_cArray[i][j] = '.';
                                    }
                                }
                            }
                        }
                        if (m_Recog.m_cArray[r][c] == '@') {
                            m_Recog.m_cArray[r][c] = '-';
                        } else if (m_Recog.m_cArray[r][c] == '+') {
                            m_Recog.m_cArray[r][c] = '.';
                        } else if (m_Recog.m_cArray[r][c] == '.') {
                            m_Recog.m_cArray[r][c] = '+';
                        } else {
                            m_Recog.m_cArray[r][c] = '@';
                        }
                    } else {  // 选中了其他物件
                        if (m_Recog.m_cArray[r][c] == myXSB[m_nObj]) {
                            m_Recog.m_cArray[r][c] = '-';
                        } else {
                            m_Recog.m_cArray[r][c] = myXSB[m_nObj];
                        }
                    }
                    m_Recog.myCount();
                }

                invalidate();
                return true;
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {  //onTouchEvent
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mStartPoint.set(event.getX(), event.getY());  //记录触点坐标

                    //单触点按下时，默认首先触发拖动模式
                    mMode = MODE_DRAG;

                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_UP:  //只要有触点抬起，即结束拖动或缩放
                case MotionEvent.ACTION_CANCEL:
                    mMode = MODE_NONE;
                case MotionEvent.ACTION_UP:
                    reSetMatrix();
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mMode == MODE_ZOOM) setZoomMatrix(event);  //缩放
                    else if (mMode == MODE_DRAG) setDragMatrix(event);  //拖动
                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mMode = MODE_ZOOM;  //多触点按下，即设置缩放模式开始
                    mStartDis = distance(event);
                    midPoint(mid, event);
                    break;
            }
            //无论如何都处理各种外部手势
            mGestureDetector.onTouchEvent(event);
            return true;
        }

        //设置拖拽状态下的 Matrix
        public void setDragMatrix(MotionEvent event) {
            if (isZoomChanged()) {
                //计算移动距离
                float dx = event.getX() - mStartPoint.x;
                float dy = event.getY() - mStartPoint.y;

                mStartPoint.set(event.getX(), event.getY());

                //在当前基础上移动
                mCurrentMatrix.getValues(values);
                dy = checkDyBound(values, dy);
                dx = checkDxBound(values, dx);

                mCurrentMatrix.postTranslate(dx, dy);
                invalidate();
            }
        }

        //图片是否在原始缩放基础上又进行了缩放？控制是否能够拖动
        private boolean isZoomChanged() {
            mCurrentMatrix.getValues(values);
            float scale = values[Matrix.MSCALE_X];  //获取当前缩放级别

            return scale != mScale;   //与原始缩放级别做比较
        }

        //检验dy，使图片边界尽量不离开屏幕边界
        private float checkDyBound(float[] values, float dy) {
            float height = getHeight() - m_nArenaTop;
            if (m_nPicHeight * values[Matrix.MSCALE_Y] < height) return 0;
            if (values[Matrix.MTRANS_Y] + dy > 0)
                dy = -values[Matrix.MTRANS_Y];
            else if (values[Matrix.MTRANS_Y] + dy < -(m_nPicHeight * values[Matrix.MSCALE_Y] - height))
                dy = -(m_nPicHeight * values[Matrix.MSCALE_Y] - height) - values[Matrix.MTRANS_Y];
            return dy;
        }

        //检验dx，使图片边界尽量不离开屏幕边界
        private float checkDxBound(float[] values, float dx) {
            float width = getWidth();
            if (m_nPicWidth * values[Matrix.MSCALE_X] < width) return 0;
            if (values[Matrix.MTRANS_X] + dx > 0)
                dx = -values[Matrix.MTRANS_X];
            else if (values[Matrix.MTRANS_X] + dx < -(m_nPicWidth * values[Matrix.MSCALE_X] - width))
                dx = -(m_nPicWidth * values[Matrix.MSCALE_X] - width) - values[Matrix.MTRANS_X];
            return dx;
        }

        //设置缩放 Matrix
        private void setZoomMatrix(MotionEvent event) {

            if (event.getPointerCount() < 2) return;  //只有同时触屏两个点的时候才执行

            float endDis = distance(event);   // 结束距离

            if (endDis > 10f) {      // 两个手指并拢在一起的时候像素大于10
                float scale = endDis / mStartDis;     // 得到缩放倍数
                mStartDis = endDis;               //重置距离
                mCurrentMatrix.getValues(values);
                scale = checkMaxScale(scale, values);
                PointF centerF = getCenter(scale, values);
                mCurrentMatrix.postScale(scale, scale, centerF.x, centerF.y);
            }
        }

        //计算缩放的中心点，主要是控制图片边界尽量不离开屏幕边界
        private PointF getCenter(float scale, float[] values) {
            float cx = mid.x;
            float cy = mid.y;
            float height = getHeight() - m_nArenaTop;

            if (scale > 1) {  //放大时，若图片边缘小于屏幕边缘，则以屏幕中心为缩放中心
                if (m_nPicWidth * scale * values[Matrix.MSCALE_X] < getWidth()) cx = getWidth() / 2;
                if (m_nPicHeight * scale * values[Matrix.MSCALE_Y] < height) cy = height/2 + m_nArenaTop;
            } else {  //缩小时，若图片边缘会离开屏幕边缘，则以屏幕边缘为缩放中心
                if (m_nPicWidth * scale * values[Matrix.MSCALE_X] < getWidth()) cx = getWidth() / 2;
                else {
                    if ((cx - values[Matrix.MTRANS_X]) * scale < cx) cx = 0;
                    if (((m_nPicWidth - cx) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X]) * scale < getWidth())
                        cx = getWidth();
                }
                if (m_nPicHeight * scale * values[Matrix.MSCALE_Y] < height) cy = height/2 + m_nArenaTop;
                else {
                    if ((cy - values[Matrix.MTRANS_Y]) * scale < cy) cy = 0;
                    if (((m_nPicHeight - cy) * values[Matrix.MSCALE_Y] + values[Matrix.MTRANS_Y]) * scale < height)
                        cy = height;
                }
            }
            return new PointF(cx, cy);
        }

        //图片缩放倍数控制
        private float checkMaxScale(float scale, float[] values) {
            if (mScale >= mMaxScale)
                scale = mScale / values[Matrix.MSCALE_X];
            else if (scale * values[Matrix.MSCALE_X] > mMaxScale)  //大于最大倍数限制时
                scale = mMaxScale / values[Matrix.MSCALE_X];
            else if (scale * values[Matrix.MSCALE_X] < mScale * 0.9F) //小于原始缩放倍数的 90% 时
                scale = mScale * 0.9F / values[Matrix.MSCALE_X];

            return scale; //两极缩放级别之间，正常缩放
        }

        //重置 Matrix，在小于原始地图时恢复
        private void reSetMatrix() {
            if (checkRest()) mCurrentMatrix.set(mMatrix);
        }

        //小于原始缩放级别时，则需要重置
        private boolean checkRest() {
            mCurrentMatrix.getValues(values);
            float scale = values[Matrix.MSCALE_X]; //获取当前缩放级别
            return scale < mScale;
        }

        //取手势中心点
        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }

        //计算两指间的距离
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    Rect rt = new Rect();
    int ss, cc;
    char ch;
    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        myPaint.setARGB(255, 255, 255, 255);
        setBackgroundColor(Color.BLACK);  //背景色设置

        canvas.save();

        // 计算缩放等参数
        mCurrentMatrix.getValues(values);
        values[Matrix.MTRANS_Y] += m_nArenaTop;
        mMapMatrix.setValues(values);
        m_fTop = values[Matrix.MTRANS_Y];
        m_fLeft = values[Matrix.MTRANS_X];
        m_fScale = values[Matrix.MSCALE_X];
        canvas.setMatrix(mMapMatrix);

        // 显示原图
        if (myMaps.edPict != null) {
            canvas.drawBitmap(myMaps.edPict, null, new Rect(0, 0, m_nPicWidth, m_nPicHeight), myPaint);
        }

        myPaint.setStyle(Paint.Style.FILL);
        myPaint.setStrokeWidth((float) 1);              //设置线宽

        // 设置网格线颜色
        if (m_nLine_Color == 1) {            // 网格线为白色
            myPaint.setARGB(159, 255, 255, 255);
        } else if (m_nLine_Color == 2) {     // 网格线为黑色
            myPaint.setARGB(159, 0, 0, 0);
        } else {                             // 默认，网格线为紫色
            myPaint.setARGB(159, 255, 0, 255);
        }
        // 画横线
        int k = 0;
        while (m_nMapTop + k * m_nWidth < m_nPicHeight) {
            canvas.drawLine(m_nMapLeft, m_nMapTop + k * m_nWidth, m_nPicWidth, m_nMapTop + k * m_nWidth, myPaint);
            k++;
        }
        // 画竖线
        k = 0;
        while (m_nMapLeft + k * m_nWidth < m_nPicWidth) {
            canvas.drawLine(m_nMapLeft + k * m_nWidth, m_nMapTop, m_nMapLeft + k * m_nWidth, m_nPicHeight, myPaint);
            k++;
        }

        // 显示样本格子
        if (cur_Rect.top >= m_nMapTop && cur_Rect.left >= m_nMapLeft) {
            rt.set(cur_Rect.left+mPoff, cur_Rect.top+mPoff, cur_Rect.left+mPoff+mPR, cur_Rect.top+mPoff+mPR);
            canvas.drawRect(cur_Rect.left, cur_Rect.top, rt.left, cur_Rect.bottom, myPaint);
            canvas.drawRect(rt.right, cur_Rect.top, cur_Rect.right, cur_Rect.bottom, myPaint);
            canvas.drawRect(rt.left, cur_Rect.top, rt.right, rt.top, myPaint);
            canvas.drawRect(rt.left, rt.bottom, rt.right, cur_Rect.bottom, myPaint);
        }

        // 显示识别出来的 XSB
        ss = sp2px(myMaps.ctxDealFile, 11);
        cc = 0;
        if (m_nWidth <= ss + 6) {
            ss /= 2;
            cc = 1;
        }
        if (m_nWidth <= ss/2 + 15) {
            ss /= 5;
            cc = 2;
        }
        for (int i = 0; i < m_Recog.m_cArray.length; i++) {
            for (int j = 0; j < m_Recog.m_cArray.length; j++) {
                if (m_Recog.m_cArray[i][j] != '-') {
                    ch = m_Recog.m_cArray[i][j];
                    if (cc == 2) {  //当样本格子比较小的时候，没有足够的空间显示 XSB 字符，此时，使用替换字符显示
                        switch (ch) {
                            case '#':
                                ch = '■';
                                break;
                            case '$':
                                ch = '▲';
                                break;
                            case '*':
                                ch = '◇';
                                break;
                            case '@':
                                ch = '↑';
                                break;
                            case '+':
                                ch = '＋';
                                break;
                        }
                    } else if (cc == 1) {  //当样本格子比较小的时候，没有足够的空间显示 XSB 字符，此时，使用替换字符显示
                        switch (ch) {
                            case '#':
                                ch = '■';
                                break;
                            case '$':
                                ch = '▲';
                                break;
                            case '*':
                                ch = '◇';
                                break;
                            case '@':
                                ch = '∏';
                                break;
                            case '+':
                                ch = '＋';
                                break;
                        }
                    } else{  //格子可以正常显示 XSB 字符时，将其变成全角显示，这样定位准确
                        switch (ch) {
                            case '#':
                                ch = '＃';
                                break;
                            case '$':
                                ch = '＄';
                                break;
                            case '*':
                                ch = '*';
                                break;
                            case '@':
                                ch = '＠';
                                break;
                            case '+':
                                ch = '＋';
                                break;
                        }
                    }
                    showXSB(canvas, (int) (j * m_nWidth + m_nMapLeft), (int) (i * m_nWidth + m_nMapTop), ch, ss);
                }
            }
        }

        canvas.restore();

        ss = sp2px(myMaps.ctxDealFile, 16);
        myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        myPaint.setTextSize(ss);
        myPaint.setARGB(255, 0, 0, 0);
        myPaint.setStrokeWidth(5);
        canvas.drawText("样本像素数: " + mPR + " × " + mPR + " ＝ " + mPR * mPR, 10, ss / 2 * 3, myPaint);
        myPaint.setARGB(255, 255, 255, 255);
        myPaint.setStrokeWidth(3);
        canvas.drawText("样本像素数: " + mPR + " × " + mPR + " ＝ " + mPR * mPR, 10, ss / 2 * 3, myPaint);

        // 提示箱子数和目标点数
        if (m_Recog.m_nBoxNum > 0 || m_Recog.DstNum > 0) {
            ss = sp2px(myMaps.ctxDealFile, 16);
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setTextSize(ss);
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStrokeWidth(5);
            canvas.drawText("箱子: "+  m_Recog.m_nBoxNum + "  目标点: "+  m_Recog.DstNum, 10, ss * 3, myPaint);
            myPaint.setARGB(255, 255, 255, 255);
            myPaint.setStrokeWidth(3);
            canvas.drawText("箱子: "+  m_Recog.m_nBoxNum + "  目标点: "+  m_Recog.DstNum, 10, ss * 3, myPaint);
        }

//        if (cur_Rect.top >= 0) {  // debug
//            myPaint.setARGB(255, 255, 255, 255);
//            rt.set(getRight() - mPR * 3 - 4, 2, getRight(), mPR * 3 + 6);
//            canvas.drawRect(rt, myPaint);
//            rt.set(getRight() - mPR * 3 - 2, 4, getRight() - 2, mPR * 3 + 4);
//            canvas.drawBitmap(myMaps.edPict, new Rect(cur_Rect.left + mPoff, cur_Rect.top + mPoff, cur_Rect.right - mPoff, cur_Rect.bottom - mPoff), rt, myPaint);
//        }
    }

    // 对整张图进行识别，识别时将锁定之前识别出来的 XSB
    int mPoff = (int) (m_nWidth / 5);         // 比对样本区域的偏移量
    int mPR = (int) (m_nWidth - mPoff * 2);  // 比对样本区域的边长
    public void setPR() {
        // 为了避免格子线造成的误差，仅查找子图的中心部分
        if (m_nWidth < 20) {
            mPoff = (int) (m_nWidth / 4);
        } else {
            mPoff = (int) (m_nWidth / 5);
        }
        mPR = (int) (m_nWidth - mPoff * 2);
    }

    public ArrayList<Point> findSubimages() {
        ArrayList<Point> locs = new ArrayList<Point>();

        setPR();

        // 从“左上角”开始搜索子图
        for (int y = m_nMapTop, k1 = 0; y < m_nPicHeight - mPR; k1++, y = (int) (m_nMapTop + k1 * m_nWidth)) {
            for (int x = m_nMapLeft, k2 = 0; x < m_nPicWidth - mPR; k2++, x = (int) (m_nMapLeft + k2 * m_nWidth)) {
                if ((m_Recog.isActNum == 4 || m_Recog.m_cArray[Math.round((y-m_nMapTop)/m_nWidth)][Math.round((x-m_nMapLeft)/m_nWidth)] == '-') && isFindSubimage(x, y)) {  // 发现子图（隐含有对之前识别出来的元素的锁定功能）
                    locs.add(new Point(x, y));
                }
            }
        }
        return locs;
    }

    // 在一个格子中查找样本
    boolean isFindSubimage(int bx, int by) {
        for (int y = by; y < by + m_nWidth - mPR; y++) {
            for (int x = bx; x < bx + m_nWidth - mPR; x++) {
                if (isSubimage(x, y)) {  // 发现样本
                    return true;
                }
            }
        }
        return false;
    }

    // 比较样本
    boolean isSubimage(int bx, int by) {

        int colorDeviationsCount               = 0;      // 颜色偏差数
        int sourceColor, compareColor, difference;

        // 按照样本的大小比对，仅比较样本的中心区域
        for (int y = 0; y < mPR; y++) {
            for (int x = 0; x < mPR; x++) {
                // 读取图片上，某点的颜色数据，以（bx，by）为左上角的区域
                if (x + bx >= m_nPicWidth || y + by >= m_nPicHeight) {
                    sourceColor = 0;
                } else {
                    sourceColor  = myMaps.edPict.getPixel(x + bx , y + by);    // 原图中，传过来的区域
                }
                if (x + cur_Rect.left + mPoff >= m_nPicWidth || y + cur_Rect.top + mPoff >= m_nPicHeight) {
                    compareColor = 0;
                } else {
                    compareColor = myMaps.edPict.getPixel(x + cur_Rect.left + mPoff, y + cur_Rect.top + mPoff);    // 样本区域
                }
                // 在 myEditView 中读取图片时，格式为 Bitmap.Config.ARGB_8888
//                difference = (int) Math.sqrt(
//                        Math.pow(Math.abs(((compareColor >>> 16) & 0xFF) - ((sourceColor >>> 16) & 0xFF)) * 3, 2) +
//                        Math.pow(Math.abs(((compareColor >>>  8) & 0xFF) - ((sourceColor >>>  8) & 0xFF)) * 4, 2) +
//                        Math.pow(Math.abs(((compareColor >>>  0) & 0xFF) - ((sourceColor >>>  0) & 0xFF)) * 2, 2)
//                );
                difference =
                        Math.abs(((compareColor >>> 16) & 0xFF) - ((sourceColor >>> 16) & 0xFF)) * 3 +
                        Math.abs(((compareColor >>>  8) & 0xFF) - ((sourceColor >>>  8) & 0xFF)) * 4 +
                        Math.abs(((compareColor >>>  0) & 0xFF) - ((sourceColor >>>  0) & 0xFF)) * 2;

                if (difference > toleranceValueColor) {
                    if (++colorDeviationsCount > toleranceValueDifferentColor)
                        return false;
                }
            }
        }
        return true;
    }

    private int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    // 显示 XSB
    private void showXSB(Canvas canvas, int x, int y, char mXSB, int ss) {
        myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        myPaint.setARGB(255, 255, 255, 255);
        if (mXSB == '.') {
            mXSB = 'o';
            myPaint.setTextSize(ss / 2);
            myPaint.setStrokeWidth(5);
            canvas.drawText("" + mXSB, x + m_nWidth / 2 - ss / 4, y + m_nWidth / 2 + ss / 6, myPaint);
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStrokeWidth(3);
            canvas.drawText("" + mXSB, x + m_nWidth / 2 - ss / 4, y + m_nWidth / 2 + ss / 6, myPaint);
        } else if (mXSB == '*') {
            myPaint.setTextSize(ss*3/2);
            myPaint.setStrokeWidth(5);
            canvas.drawText(""+mXSB, x + m_nWidth / 2 - ss / 3, y + m_nWidth / 3 + ss, myPaint);
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStrokeWidth(3);
            canvas.drawText(""+mXSB, x + m_nWidth / 2 - ss / 3, y + m_nWidth / 3 + ss, myPaint);
        } else {
            myPaint.setTextSize(ss);
            myPaint.setStrokeWidth(5);
            canvas.drawText(""+mXSB, x + m_nWidth / 2 - ss / 2, y + m_nWidth / 2 + ss / 3, myPaint);
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStrokeWidth(3);
            canvas.drawText(""+mXSB, x + m_nWidth / 2 - ss / 2, y + m_nWidth / 2 + ss / 3, myPaint);
        }
    }

    //舞台初始化
    public void initArena() {
        //计算关卡图的像素尺寸
        m_nPicWidth  = myMaps.edPict.getWidth();
        m_nPicHeight = myMaps.edPict.getHeight();
        m_nWidth = 50;
        m_nMapTop = 0;
        m_nMapLeft = 0;

        //设置监听函数，保障舞台场地的成功创建
        if (getWidth() == 0) {
            ViewTreeObserver vto = getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    setArena();
                    //舞台位图创建成功后，移除该监听函数
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        } else setArena();
    }

    //创建或重置舞台场地
    public void setArena() {
        //得到原始变换矩阵
        mMatrix = getInnerMatrix(mMatrix);  //计算原始变换矩阵
        mCurrentMatrix.set(mMatrix);  //同时得到当前变换矩阵
        mMatrix.getValues(values);
        mScale = values[Matrix.MSCALE_X];  //原始缩放倍数
        m_fTop = values[Matrix.MTRANS_Y];  //当前上边界
        m_fLeft = values[Matrix.MSCALE_X];  //当前左边界
        m_fScale = mScale;  //当前缩放倍数

        cur_Rect.top = -1;   // 开始时，没有长按的格子

        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);

        if (w > 0 && h > 0 && (h != old_h || w != old_w)) {
            setArena();  //重置舞台场地
        }
    }

    public void Init(myRecogView v) {
        m_Recog = v;
    }
}