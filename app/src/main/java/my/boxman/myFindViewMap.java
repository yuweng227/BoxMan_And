package my.boxman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

public class myFindViewMap extends View {

    public int m_iR = -1, m_iC = -1;  //单击的节点坐标
    private myFindView m_Find;  //父控件指针，以便使用父控件的功能

    Paint myPaint = new Paint();
    public char[][] m_cArray;

    private Rect m_rTrun;  //可以触发“旋转”的区域
    private Rect m_rLevel;  //可以触发“切换源关卡与相似关卡”的区域
    private Bitmap bitTrun;
    private Bitmap bitLevel;

    boolean m_Level_All;  //是否显示关卡全貌

    int w_bkPic, h_bkPic, w_bkNum, h_bkNum;  //（舞台用）背景图片的宽、高；及其平铺时的横、纵个数
    int m_nPicWidth, m_nPicHeight, m_nRows, m_nCols;  //关卡尺寸
    int m_PicWidth = 50;   //素材尺寸，即关卡图每个格子的像素尺寸
    Matrix mMatrix = new Matrix();  //图片原始变换矩阵
    Matrix mCurrentMatrix = new Matrix();  //当前变换矩阵
    Matrix mMapMatrix = new Matrix();  //当前变换矩阵
    float m_fTop, m_fLeft, m_fScale, mScale;  //关卡图的当前上边界、左边界、缩放倍数；原始缩放倍数
    float[] values = new float[9];

    public myFindViewMap(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myFindViewMap(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myFindViewMap(Context context) {
        super(context);

        setOnTouchListener(new TouchListener());
        initView();
    }

    private void initView() {
        int myWitth = 120;
        m_rTrun = new Rect();
        m_rLevel = new Rect();
        m_rTrun.set(myMaps.m_nWinWidth-myWitth*2, 20, myMaps.m_nWinWidth-myWitth, myWitth+20); //旋转
        m_rLevel.set(m_rTrun.right, m_rTrun.top, myMaps.m_nWinWidth, m_rTrun.bottom); //切换源关卡与相似关卡
        bitTrun = Bitmap.createBitmap(myWitth, myWitth, myMaps.cfg); //旋转按钮图片
        Canvas cvs01 = new Canvas(bitTrun);
        Drawable dw01 = myMaps.res.getDrawable(R.drawable.trbtn);
        dw01.setBounds(0, 0, myWitth, myWitth);
        dw01.draw(cvs01);
        bitLevel = Bitmap.createBitmap(myWitth, myWitth, myMaps.cfg); //切换源关卡与相似关卡按钮图片
        Canvas cvs02 = new Canvas(bitLevel);
        Drawable dw02 = myMaps.res.getDrawable(R.drawable.cb_pressed);
        dw02.setBounds(0, 0, myWitth, myWitth);
        dw02.draw(cvs02);
    }

    //计算原始变换矩阵
    private Matrix getInnerMatrix(Matrix matrix) {
        if (matrix == null) matrix = new Matrix();
        else matrix.reset();
        //原图大小
        RectF tempSrc = new RectF(0, 0, m_nPicWidth, m_nPicHeight);
        //控件大小
        RectF tempDst = new RectF(0, 0, getWidth(), getHeight());
        //计算fit center矩阵
        matrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);

        return matrix;
    }

    //缩放、拖拽
    private class TouchListener implements OnTouchListener {
        private static final int MODE_NONE = 0;
        private static final int MODE_DRAG = 1;  //拖动模式
        private static final int MODE_ZOOM = 2;  //缩放模式
        private int mMode = MODE_NONE;      //当前模式

        float mMaxScale = 3;   //最大缩放级别
        private float mStartDis;  //缩放开始时的手指间距
        private PointF mStartPoint = new PointF(), mClickPoint = new PointF();  //第一触点，相对及绝对坐标
        private PointF mid = new PointF();  //手势中心点

        private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            //单触点，不滑动，略延迟触发
            public void onShowPress(MotionEvent e) {
            }

            public void onLongPress(MotionEvent e) {
                invalidate();
            }

            public boolean onDoubleTap(MotionEvent e) {
                //双击两个按钮区域
                if (m_rTrun.contains((int) e.getX(), (int) e.getY()) || m_rLevel.contains((int) e.getX(), (int) e.getY())) {
                    return true;
                }

                //当手指快速第二次按下触发,此时必须是单指模式才允许执行doubleTap
                if (mMode == MODE_DRAG &&
                    Math.abs(e.getRawX() - mClickPoint.x) < 50f &&  //双击范围控制
                    Math.abs(e.getRawY() - mClickPoint.y) < 50f) {
                    m_Level_All = !m_Level_All;
                    if (m_Level_All) {  //关卡全貌
                        if (m_Find.m_Level) {
                            m_cArray = m_Find.m_cArray1;  //源关卡 -- 全貌
                            m_nRows = m_Find.Rows1;
                            m_nCols = m_Find.Cols1;
                        } else {
                            m_cArray = m_Find.m_cArray2;  //相似关卡 -- 全貌
                            m_nRows = m_Find.Rows2;
                            m_nCols = m_Find.Cols2;
                        }
                    } else {   //瘦关卡
                        if (m_Find.m_Level) {
                            m_cArray = m_Find.m_cArray3;  //源关卡 -- 瘦
                            m_nRows = m_Find.Rows3;
                            m_nCols = m_Find.Cols3;
                        } else {
                            m_cArray = m_Find.m_cArray4;  //相似关卡 -- 瘦
                            m_nRows = m_Find.Rows4;
                            m_nCols = m_Find.Cols4;
                        }
                    }

                    initArena();
                    invalidate();
                }
                return true;
            }

//			public boolean onSingleTapConfirmed(MotionEvent e) {
//				return true;
//			}

            public boolean onSingleTapUp(MotionEvent e) {
                if (m_rTrun.contains((int) e.getX(), (int) e.getY())) {  //旋转
                    m_Find.myTrun();
                    return true;
                } else if (m_rLevel.contains((int) e.getX(), (int) e.getY())) {  //切换源关卡与相似关卡
                    m_Find.myLevel();
                    return true;
                }

                if (mMode == MODE_NONE)
                    doACT((int) e.getX(), (int) e.getY());
                return true;
            }

        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {  //onTouchEvent
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:  //触发点击
                    //计算点击坐标
                    doACT((int) event.getX(), (int) event.getY());
                    mMode = MODE_DRAG;
                    mStartPoint.set(event.getX(), event.getY());  //相对坐标
                    mClickPoint.set(event.getRawX(), event.getRawY());  //触点的屏幕坐标，控制双击的范围和单击的抖动时使用

                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    reSetMatrix();
                    invalidate();
                    mMode = MODE_NONE;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mMode == MODE_ZOOM) setZoomMatrix(event);
                    else if (mMode == MODE_DRAG) setDragMatrix(event);
                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mMode = MODE_ZOOM;
                    mStartDis = distance(event);
                    midPoint(mid, event);
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
            float height = getHeight();
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
            float height = getHeight();

            if (scale > 1) {  //放大时，若图片边缘小于屏幕边缘，则以屏幕中心为缩放中心
                if (m_nPicWidth * scale * values[Matrix.MSCALE_X] < getWidth()) cx = getWidth() / 2;
                if (m_nPicHeight * scale * values[Matrix.MSCALE_Y] < height) cy = height / 2;
            } else {  //缩小时，若图片边缘会离开屏幕边缘，则以屏幕边缘为缩放中心
                if (m_nPicWidth * scale * values[Matrix.MSCALE_X] < getWidth()) cx = getWidth() / 2;
                else {
                    if ((cx - values[Matrix.MTRANS_X]) * scale < cx) cx = 0;
                    if (((m_nPicWidth - cx) * values[Matrix.MSCALE_X] + values[Matrix.MTRANS_X]) * scale < getWidth())
                        cx = getWidth();
                }
                if (m_nPicHeight * scale * values[Matrix.MSCALE_Y] < height) cy = height / 2;
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

        //计算两点间的距离
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    //计算使用哪个墙壁图片，同时判断出是否画墙顶
    private Rect getWall(Rect rt, int r, int c) {

        if (myMaps.isSkin_200 == 200) {
            rt.set(0, 0, 50, 50);
            return rt;
        }

        int bz = 0;
        int[][] dir = {
                {1, 2, 4, 8, 3, 7, 11},  //0 转
                {2, 4, 8, 1, 6, 7, 14},  //1
                {4, 8, 1, 2, 12, 13, 14},  //2
                {8, 1, 2, 4, 9, 13, 11},  //3
                {4, 2, 1, 8, 6, 7, 14},  //4
                {8, 4, 2, 1, 12, 13, 14},  //5
                {1, 8, 4, 2, 9, 13, 11},  //6
                {2, 1, 8, 4, 3, 7, 11}   //7
        };

        //看看哪个方向上有“墙”
        if (c > 0 && m_cArray[r][c - 1] == '#') bz |= dir[myMaps.m_nTrun][0]; //左
        if (r > 0 && m_cArray[r - 1][c] == '#') bz |= dir[myMaps.m_nTrun][1]; //上
        if (c < m_cArray[0].length - 1 && m_cArray[r][c + 1] == '#') bz |= dir[myMaps.m_nTrun][2]; //右
        if (r < m_cArray.length - 1 && m_cArray[r + 1][c] == '#') bz |= dir[myMaps.m_nTrun][3]; //下

        switch (bz) {
            case 1:
                rt.set(150, 0, 200, 50);
                break;  //仅左
            case 2:
                rt.set(0, 150, 50, 200);
                break;  //仅上
            case 3:
                rt.set(150, 150, 200, 200);
                break; //左、上
            case 4:
                rt.set(50, 0, 100, 50);
                break;  //仅右
            case 5:
                rt.set(100, 0, 150, 50);
                break; //左、右
            case 6:
                rt.set(50, 150, 100, 200);
                break; //右、上
            case 7:
                rt.set(100, 150, 150, 200);
                break; //左、上、右
            case 8:
                rt.set(0, 50, 50, 100);
                break; //仅下
            case 9:
                rt.set(150, 50, 200, 100);
                break; //左、下
            case 10:
                rt.set(0, 100, 50, 150);
                break; //上、下
            case 11:
                rt.set(150, 100, 200, 150);
                break;  //左、上、下
            case 12:
                rt.set(50, 50, 100, 100);
                break;  //右、下
            case 13:
                rt.set(100, 50, 150, 100);
                break; //左、右、下
            case 14:
                rt.set(50, 100, 100, 150);
                break; //上、右、下
            case 15:
                rt.set(100, 100, 150, 150);
                break; //四方向全有
            default:
                rt.set(0, 0, 50, 50);  //四方向全无
        }

        return rt;
    }

    //绘制游戏画面
    Rect rt = new Rect();
    Rect rt1 = new Rect();
    Rect rt4 = new Rect();
    int ss, r, c;
    char ch;

    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        setBackgroundColor(myMaps.m_Sets[4]);  //设置背景色

        if (myMaps.bkPict != null) {
            for (int i = 0; i <= w_bkNum; i++) {
                for (int j = 0; j <= h_bkNum; j++)
                    canvas.drawBitmap(myMaps.bkPict, w_bkPic * i, h_bkPic * j, null);
            }
        }

        canvas.save();
        mCurrentMatrix.getValues(values);
        mMapMatrix.setValues(values);
        m_fTop = values[Matrix.MTRANS_Y];
        m_fLeft = values[Matrix.MTRANS_X];
        m_fScale = values[Matrix.MSCALE_X];
        canvas.setMatrix(mMapMatrix);

        for (int i = 0; i < m_nRows; i++) {
            for (int j = 0; j < m_nCols; j++) {
                switch (myMaps.m_nTrun) {
                    case 1:
                        rt.left = m_PicWidth * (m_nRows - 1 - i);
                        rt.top = m_PicWidth * j;
                        r = j;
                        c = m_nRows - 1 - i;
                        break;
                    default:
                        rt.left = m_PicWidth * j;
                        rt.top = m_PicWidth * i;
                        r = i;
                        c = j;
                }
                rt.right = rt.left + m_PicWidth;
                rt.bottom = rt.top + m_PicWidth;

                //相似区域
                if (myMaps.m_nTrun == 0) {
                    if (m_Find.m_Level) {
                        if (r == m_Find.mSelect[0][0] && c == m_Find.mSelect[0][1]) {
                            rt4.left = rt.left;
                            rt4.top = rt.top;
                        } else if (r == m_Find.mSelect[0][2] && c == m_Find.mSelect[0][3]) {
                            rt4.right = rt.right;
                            rt4.bottom = rt.bottom;
                        }
                    } else {
                        if (r == m_Find.mSelect[1][0] && c == m_Find.mSelect[1][1]) {
                            rt4.left = rt.left;
                            rt4.top = rt.top;
                        } else if (r == m_Find.mSelect[1][2] && c == m_Find.mSelect[1][3]) {
                            rt4.right = rt.right;
                            rt4.bottom = rt.bottom;
                        }
                    }
                } else {  //旋转
                    if (m_Find.m_Level) {
                        if (r == m_Find.mSelect[0][1] && c == m_nRows - 1 - m_Find.mSelect[0][0]) {
                            rt4.right = rt.right;
                            rt4.top = rt.top;
                        } else if (r == m_Find.mSelect[0][3] && c == m_nRows - 1 - m_Find.mSelect[0][2]) {
                            rt4.left = rt.left;
                            rt4.bottom = rt.bottom;
                        }
                    } else {
                        if (r == m_Find.mSelect[1][1] && c == m_nRows - 1 - m_Find.mSelect[1][0]) {
                            rt4.right = rt.right;
                            rt4.top = rt.top;
                        } else if (r == m_Find.mSelect[1][3] && c == m_nRows - 1 - m_Find.mSelect[1][2]) {
                            rt4.left = rt.left;
                            rt4.bottom = rt.bottom;
                        }
                    }
                }

                ch = m_cArray[i][j];

                myPaint.setARGB(255, 0, 0, 0);
                //第一、二层显示————地板、逆推水印
                if (ch != '#') {
                    rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);  //地板————第一层
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                }
                //第三层显示————目标点
                if (ch == '.' || ch == '*' || ch == '+') {
                    rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                }
                //第四层显示————箱子或人
                switch (ch) {
                    case '#':   //墙壁属于第一层显示
                        rt1 = getWall(rt1, i, j);  //计算使用哪个“墙”图
                        canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        break;
                    case '$':
                        rt1.set(50, 250-myMaps.isSkin_200, 100, 300-myMaps.isSkin_200);
                        canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        break;
                    case '*':
                        rt1.set(50, 300-myMaps.isSkin_200, 100, 350-myMaps.isSkin_200);
                        canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        break;
                    case '@':
                        rt1.set(100, 250-myMaps.isSkin_200, 150, 300-myMaps.isSkin_200);
                        canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        break;
                    case '+':
                        rt1.set(100, 300-myMaps.isSkin_200, 150, 350-myMaps.isSkin_200);
                        canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                } //end switch
            }  //end for j
        } //end for i

        //瘦关卡时，画出相似区域框
        if (!m_Level_All) {
            myPaint.setARGB(255, 255, 0, 0);  //设置颜色
            myPaint.setStyle(Paint.Style.STROKE);
            myPaint.setStrokeWidth(3);
            canvas.drawRect(rt4, myPaint);
        }

        canvas.restore();

        myPaint.setARGB(255, 0, 0, 0);
        canvas.drawBitmap(bitTrun, m_rTrun.left, m_rTrun.top, null);   //画按钮
        canvas.drawBitmap(bitLevel, m_rLevel.left, m_rLevel.top, null);   //画按钮

        //关卡信息
        ss = sp2px(myMaps.ctxDealFile, 16);
        myPaint.setTextSize(ss);  //设置字体大小
        myPaint.setARGB(255, 255, 255, 255);  //设置字体颜色
        myPaint.setStyle(Paint.Style.FILL);
        myPaint.setStrokeWidth(1);

        //显示关卡信息
        if (m_Find.m_Level) {
            myPaint.setARGB(255, 255, 255, 255);  //设置字体颜色
            canvas.drawText("源关卡 " +  m_Find.mTrun + " 转后的比对图", 0, ss, myPaint);
            canvas.drawText(m_Find.m_Set_Pos1, 0, ss*2, myPaint);
        } else {
            myPaint.setARGB(255, 0, 255, 255);
            canvas.drawText("相似关卡" + (myMaps.curMap.Solved ? "【有解】，" : "，") + "相似度：" + m_Find.mSimilarity + "%", 0, ss, myPaint);
            canvas.drawText(m_Find.m_Set_Pos2, 0, ss*2, myPaint);
        }
        canvas.drawText("点击位置：" + mGetCur(m_iR, m_iC), 0, ss*3, myPaint);
    }

    private int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    //计算击位置 -- 更新游标
    private void doACT(int i, int j) {

        if (i < m_fLeft || j < m_fTop) {  //界外
            m_iC = -1;
            m_iR = -1;
        } else {
            switch (myMaps.m_nTrun) {
                case 1:
                    m_iR = m_cArray.length - 1 - ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                    m_iC = ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
                    break;
                default:
                    m_iC = ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                    m_iR = ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
            }
        }
        if (m_iR < 0 || m_iR >= m_nRows || m_iC < 0 || m_iC >= m_nCols) {
            m_iC = -1;
            m_iR = -1;
            return;
        }

        invalidate();
    }

    //舞台初始化
    public void initArena() {

        //计算关卡图的像素尺寸
        if (myMaps.m_nTrun % 2 == 0) {
            m_nPicWidth = m_PicWidth * m_nCols;
            m_nPicHeight = m_PicWidth * m_nRows;
        } else {
            m_nPicWidth = m_PicWidth * m_nRows;
            m_nPicHeight = m_PicWidth * m_nCols;
        }

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

        //计算（舞台用）背景图片的尺寸，以及平铺时的水平、垂直数量
        if (myMaps.bkPict != null) {
            w_bkPic = myMaps.bkPict.getWidth();
            h_bkPic = myMaps.bkPict.getHeight();
            w_bkNum = myMaps.m_nWinWidth / w_bkPic + 1;
            h_bkNum = myMaps.m_nWinHeight / h_bkPic + 1;
        }

        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);

        if (w > 0 && h > 0 && (h != old_h || w != old_w)) {
            setArena();  //重置舞台场地
        }
    }

    //游标计算
    public String mGetCur(int r, int c) {

        if (r < 0 || c < 0) return "";

        StringBuilder s = new StringBuilder();

        int k = c / 26 + 64;
        if (k > 64) s.append((char) (byte) k);

        s.append((char) ((byte) (c % 26 + 65))).append(String.valueOf(1 + r));

        return s.toString();
    }

    public void Init(myFindView v) {
        m_Find = v;
    }
}
