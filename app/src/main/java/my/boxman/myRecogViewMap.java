package my.boxman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
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
    ArrayList<Integer> curPoints = new ArrayList<Integer>();  //识别出来的物件

    private PointF mClickPoint = new PointF();  //第一触点，相对及绝对坐标，判断双击时使用
    Paint myPaint = new Paint();
    int m_nArenaTop;                //舞台 Top 距屏幕顶的距离
    int m_nPicWidth, m_nPicHeight;  //关卡图的像素尺寸
    int m_nMapTop, m_nMapLeft, m_nMapBottom, m_nMapRight;      //图片有效区域的左、上、右、下
    float m_nWidth = -1;            //格子的尺寸 -- 默认为“未定”
    int m_nRows = 2;                //垂直方向的格子数
    int m_nCols = 2;                //水平方向的格子数
    boolean isLeftTop = true;       //默认：左、上边的指示灯点亮
    int m_nObj = -1;                //选中的物件（XSB元素）
    int cur_Row = -1, cur_Col;      //点击的格子
    Rect cur_Rect = new Rect();     //点击的格子
    RectF L_Rect = new RectF();     //左边线指示灯
    RectF T_Rect = new RectF();     //上边线指示灯
    RectF R_Rect = new RectF();     //右边线指示灯
    RectF B_Rect = new RectF();     //下边线指示灯
    int m_Lamp = -1;                //长按的指示灯
    boolean isLamp = true;          //长按的指示灯，仓管员元素闪烁用

    int[] m_SampleArray0 = new int[1024];           //样本的比较数组
    int my_Color0, my_Color1;                       //图片的颜色偏重
    int my_Grey0, my_Grey1;                         //图片的灰度值

    public Matrix mMatrix = new Matrix();         //图片原始变换矩阵
    public Matrix mCurrentMatrix = new Matrix();  //当前变换矩阵
    private Matrix mMapMatrix = new Matrix();     //onDraw()用的当前变换矩阵
    float m_fTop, m_fLeft, m_fScale, mScale;      //关卡图的当前上边界、左边界、缩放倍数；原始缩放倍数
    public float mMaxScale = 32;                   //最大缩放级别
    float[] values = new float[9];

    // 识别参数默认值
    int mSimilarity = 6;              // 默认相似度
    boolean isRecog = true;           // 是否为识别模式

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

            // 双击边界线附近，可以微调边界线位置
            public boolean onDoubleTap(MotionEvent e) {
                //双击范围控制
                if (Math.abs(e.getRawX() - mClickPoint.x) > 50f || Math.abs(e.getRawY() - mClickPoint.y) > 50f) {
                    return true;
                }

                float x = (e.getX() - m_fLeft) / m_fScale;
                float y = (e.getY() - m_fTop ) / m_fScale;

                if (Math.abs(x - m_nMapLeft) < 200) {                    // 左边线左右
                    if (Math.abs(y - m_nMapTop) > 200 && Math.abs(y - m_nMapBottom) > 200) {
                        cur_Rect.top = -1;   // 取消焦点格子
                        if (x < m_nMapLeft) {
                            if (m_nMapLeft > 1) {
                                m_nMapLeft--;
                            }
                        } else if (x > m_nMapLeft) {
                            if (m_nMapLeft+50 < m_nMapRight) {
                                m_nMapLeft++;
                            }
                        }
                        invalidate();
                    }
                } else if (Math.abs(x - m_nMapRight) < 200) {            // 右边线左右
                    if (Math.abs(y - m_nMapTop) > 200 && Math.abs(y - m_nMapBottom) > 200) {
                        cur_Rect.top = -1;   // 取消焦点格子
                        if (x < m_nMapRight) {
                            if (m_nMapRight-50 > m_nMapLeft) {
                                m_nMapRight--;
                            }
                        } else if (x > m_nMapRight) {
                            if (m_nMapRight+1 < m_nPicWidth-1) {
                                m_nMapRight++;
                            }
                        }
                        invalidate();
                    }
                } else if (Math.abs(y - m_nMapTop) < 200) {              // 上边线上下
                    if (Math.abs(x - m_nMapLeft) > 200 && Math.abs(x - m_nMapRight) > 200) {
                        cur_Rect.top = -1;   // 取消焦点格子
                        if (y < m_nMapTop) {
                            if (m_nMapTop > 1) {
                                m_nMapTop--;
                            }
                        } else if (y > m_nMapTop) {
                            if (m_nMapTop+50 < m_nMapBottom) {
                                m_nMapTop++;
                            }
                        }
                        invalidate();
                    }
                } else if (Math.abs(y - m_nMapBottom) < 200) {           // 下边线上下
                    if (Math.abs(x - m_nMapLeft) > 200 && Math.abs(x - m_nMapRight) > 200) {
                        cur_Rect.top = -1;   // 取消焦点格子
                        if (y < m_nMapBottom) {
                            if (m_nMapBottom-50 > m_nMapTop) {
                                m_nMapBottom--;
                            }
                        } else if (y > m_nMapBottom) {
                            if (m_nMapBottom+1 < m_nPicHeight-1) {
                                m_nMapBottom++;
                            }
                        }
                        invalidate();
                    }
                }
                return true;
            }

            // 长按指示灯
            public void onLongPress(MotionEvent e) {

                float x = (e.getX() - m_fLeft) / m_fScale;
                float y = (e.getY() - m_fTop ) / m_fScale;

                if (L_Rect.contains(x, y)) {            // 左灯亮
                    m_Lamp = 0;
                    isLeftTop = true;
                } else if (T_Rect.contains(x, y)) {     // 上灯亮
                    m_Lamp = 1;
                    isLeftTop = true;
                } else if (R_Rect.contains(x, y)) {     // 右灯亮
                    m_Lamp = 2;
                    isLeftTop = false;
                } else if (B_Rect.contains(x, y)) {     // 下灯亮
                    m_Lamp = 3;
                    isLeftTop = false;
                } else {
                    m_Lamp = -1;
                }

                if (m_Lamp >= 0) {          //长按了指示灯
                    m_Recog.setColor(-1);   // 取消底行 XSB 元素高亮

                    isLamp = false;         // 仓管员闪烁 -- 是否亮起
                    m_Recog.actNum = 5;     // 定时器功能选择 -- 仓管员闪烁
                    m_Recog.UpData(1);  // 启动定时器
                }
            }

            //单击，抬起时触发
            public boolean onSingleTapUp(MotionEvent e) {

                float x = (e.getX() - m_fLeft) / m_fScale;
                float y = (e.getY() - m_fTop ) / m_fScale;

                if (m_nObj < 0) {    // 识别模式
                    // 切换边线指示灯
                    if (L_Rect.contains(x, y) || T_Rect.contains(x, y)) {     // 左上边角
                        isLeftTop = true;
                        invalidate();
                    } else if (R_Rect.contains(x, y) || B_Rect.contains(x, y)) {   // 右下边角
                        isLeftTop = false;
                        invalidate();
                    }
                }

                int xx = (int) x, yy = (int) y;

                // 有效区域之外
                if (yy < m_nMapTop || xx < m_nMapLeft || yy >= m_nMapBottom || xx >= m_nMapRight || m_nRows < 3 || m_nCols < 3) {
                    cur_Rect.top = -1;   // 取消焦点格子
                    return true;
                }

                m_nWidth = (float) (m_nMapRight - m_nMapLeft + 1) / m_nCols;

                cur_Col = (int) ((x - m_nMapLeft + 1) / m_nWidth);
                cur_Row = (int) ((y - m_nMapTop + 1) / m_nWidth);

                // 消除因计算误差，对焦点格子的影响
                if (cur_Row >= m_nRows) {
                    cur_Rect.top = -1;   // 取消焦点格子
                    return true;
                }

                xx = (int) (cur_Col * m_nWidth + m_nMapLeft);
                yy = (int) (cur_Row * m_nWidth + m_nMapTop);

                cur_Rect.set(xx, yy, (int) (xx + m_nWidth), (int) (yy + m_nWidth));   // 设置样本格子 -- 焦点区域

                setPR();        // 增强焦点格子的显示效果

                if (m_nObj >= 0) {              // 选择了底行的 XSB 元素，然后单击格子
                    m_Recog.myBackup();
                    if (isRecog) {              // 识别模式
                        if (m_nObj == 5) {      // 若选中的物件是“仓管员”
                            for (int i = 0; i < m_Recog.m_cArray.length; i++) {
                                for (int j = 0; j < m_Recog.m_cArray[0].length; j++) {
                                    if (i != cur_Row || j != cur_Col) {
                                        if (m_Recog.m_cArray[i][j] == '@') {
                                            m_Recog.m_cArray[i][j] = '-';
                                        } else if (m_Recog.m_cArray[i][j] == '+') {
                                            m_Recog.m_cArray[i][j] = '.';
                                        }
                                    }
                                }
                            }
                            if (m_Recog.m_cArray[cur_Row][cur_Col] == '+' || m_Recog.m_cArray[cur_Row][cur_Col] == '*' || m_Recog.m_cArray[cur_Row][cur_Col] == '.') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '+';
                            } else {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '@';
                            }
                        } else {
                            m_Recog.doAction();
                        }
                    } else {                     // 编辑模式
                        if (m_nObj == 4) {       // 若选中的物件是“目标点”
                            if (m_Recog.m_cArray[cur_Row][cur_Col] == '@') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '+';
                            } else if (m_Recog.m_cArray[cur_Row][cur_Col] == '+') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '@';
                            } else if (m_Recog.m_cArray[cur_Row][cur_Col] == '$') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '*';
                            } else if (m_Recog.m_cArray[cur_Row][cur_Col] == '*') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '$';
                            } else if (m_Recog.m_cArray[cur_Row][cur_Col] == '.') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '-';
                            } else {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '.';
                            }
                        } else if (m_nObj == 5) {  // 若选中的物件是“仓管员”
                            for (int i = 0; i < m_Recog.m_cArray.length; i++) {
                                for (int j = 0; j < m_Recog.m_cArray[0].length; j++) {
                                    if (i != cur_Row || j != cur_Col) {
                                        if (m_Recog.m_cArray[i][j] == '@') {
                                            m_Recog.m_cArray[i][j] = '-';
                                        } else if (m_Recog.m_cArray[i][j] == '+') {
                                            m_Recog.m_cArray[i][j] = '.';
                                        }
                                    }
                                }
                            }
                            if (m_Recog.m_cArray[cur_Row][cur_Col] == '@') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '-';
                            } else if (m_Recog.m_cArray[cur_Row][cur_Col] == '+') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '.';
                            } else if (m_Recog.m_cArray[cur_Row][cur_Col] == '.') {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '+';
                            } else {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '@';
                            }
                        } else {  // 选中了其他物件
                            if (m_Recog.m_cArray[cur_Row][cur_Col] == myXSB[m_nObj]) {
                                m_Recog.m_cArray[cur_Row][cur_Col] = '-';
                            } else {
                                m_Recog.m_cArray[cur_Row][cur_Col] = myXSB[m_nObj];
                            }
                        }
                    }
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
                    mClickPoint.set(event.getRawX(), event.getRawY());  //触点的屏幕坐标，控制双击的范围和单击的抖动时使用

                    //单触点按下时，默认首先触发拖动模式
                    mMode = MODE_DRAG;

                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_UP:  //只要有触点抬起，即结束拖动或缩放
                case MotionEvent.ACTION_CANCEL:
                    mMode = MODE_NONE;
                case MotionEvent.ACTION_UP:
                    m_Lamp = -1;          // 取消长按指示灯
                    isLamp = true;
                    m_Recog.actNum = 0;   // 取消定时器功能
                    reSetMatrix();
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mMode == MODE_ZOOM) setZoomMatrix(event);  //缩放
                    else if (mMode == MODE_DRAG) {
                        if (m_Lamp < 0) {
                            setDragMatrix(event);  //拖动图片
                        } else {
                            MoveSideLine(event);   //拖动边线
                        }
                    }
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

        //拖动边线
        private void MoveSideLine(MotionEvent event) {
            //计算移动距离
            int dx = (int) ((event.getX() - mStartPoint.x) / m_fScale);
            int dy = (int) ((event.getY() - mStartPoint.y) / m_fScale);

            mStartPoint.set(event.getX(), event.getY());

            switch (m_Lamp) {
                case 0:    //左边线
                    if (m_nMapLeft + dx > 0 && m_nMapLeft + dx + 50 < m_nMapRight) {
                        m_nMapLeft = m_nMapLeft + dx;
                    }
                    break;
                case 1:    //上边线
                    if (m_nMapTop + dy > 0 && m_nMapTop + dy + 50 < m_nMapBottom) {
                        m_nMapTop = m_nMapTop + dy;
                    }
                    break;
                case 2:    //右边线
                    if (m_nMapRight + dx < m_nPicWidth-1 && m_nMapLeft + 50 < m_nMapRight + dx) {
                        m_nMapRight = m_nMapRight + dx;
                    }
                    break;
                case 3:    //下边线
                    if (m_nMapBottom + dy < m_nPicHeight-1 && m_nMapTop + 50 < m_nMapBottom + dy) {
                        m_nMapBottom = m_nMapBottom + dy;
                    }
                    break;
            }
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

        // 显示参考底图
        if (myMaps.edPict != null) {
            canvas.drawBitmap(myMaps.edPict, null, new Rect(0, 0, m_nPicWidth, m_nPicHeight), myPaint);
        }

        myPaint.setStrokeWidth((float) 1);              //设置线宽
        //画指示器
        L_Rect.set(m_nMapLeft-50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2-50, m_nMapLeft+50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2+50);
        T_Rect.set(m_nMapLeft + (m_nMapRight-m_nMapLeft)/2-50, m_nMapTop-50, m_nMapLeft + (m_nMapRight-m_nMapLeft)/2+50, m_nMapTop+50);
        R_Rect.set(m_nMapRight-50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2-50, m_nMapRight+50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2+50);
        B_Rect.set(m_nMapLeft + (m_nMapRight-m_nMapLeft)/2-50, m_nMapBottom-50, m_nMapLeft + (m_nMapRight-m_nMapLeft)/2+50, m_nMapBottom+50);
        myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        if (isLeftTop) {   // 点亮左、上边线指示灯
            myPaint.setARGB(159, 255, 255, 255);
            canvas.drawOval(R_Rect, myPaint);
            canvas.drawOval(B_Rect, myPaint);
            if (isLamp) {
                myPaint.setARGB(159, 255, 0, 255);
            }
            canvas.drawOval(L_Rect, myPaint);
            canvas.drawOval(T_Rect, myPaint);
        } else {           // 点亮右、下边线指示灯
            myPaint.setARGB(159, 255, 255, 255);
            canvas.drawOval(L_Rect, myPaint);
            canvas.drawOval(T_Rect, myPaint);
            if (isLamp) {
                myPaint.setARGB(159, 255, 0, 255);
            }
            canvas.drawOval(R_Rect, myPaint);
            canvas.drawOval(B_Rect, myPaint);
        }
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setARGB(159, 0, 0, 0);
        myPaint.setStrokeWidth((float) 10);              //设置线宽
        canvas.drawOval(L_Rect, myPaint);
        canvas.drawOval(T_Rect, myPaint);
        canvas.drawOval(R_Rect, myPaint);
        canvas.drawOval(B_Rect, myPaint);
        myPaint.setARGB(159, 255, 255, 255);
        myPaint.setStrokeWidth((float) 5);              //设置线宽
        canvas.drawOval(L_Rect, myPaint);
        canvas.drawOval(T_Rect, myPaint);
        canvas.drawOval(R_Rect, myPaint);
        canvas.drawOval(B_Rect, myPaint);

        myPaint.setStrokeWidth((float) 1);              //设置线宽
//        myPaint.setPathEffect(new DashPathEffect(new float[]{8, 4}, 0));
        myPaint.setStyle(Paint.Style.STROKE);

        // 画边框
        rt.set(m_nMapLeft+1, m_nMapTop+1, m_nMapRight+1, m_nMapBottom+1);
        myPaint.setARGB(255, 0, 0, 0);
        canvas.drawRect(rt, myPaint);
        rt.set(m_nMapLeft, m_nMapTop, m_nMapRight, m_nMapBottom);
        myPaint.setARGB(255, 255, 255, 255);
        canvas.drawRect(rt, myPaint);

        // 画格线
        if (m_nRows > 2 && m_nCols > 2) {
            m_nWidth = (float) (m_nMapRight - m_nMapLeft + 1) / m_nCols;
            m_nRows = (int) ((m_nMapBottom - m_nMapTop + 1 + m_nWidth / 2) / m_nWidth);

            float xx, yy;
            // 画黑色竖线
            for (int k = 1; k < m_nCols; k++) {
                xx = m_nMapLeft + k * m_nWidth;
                myPaint.setARGB(255, 0, 0, 0);
                canvas.drawLine((int) xx + 1, m_nMapTop + 1, (int) xx + 1, m_nMapBottom - 2, myPaint);
            }

            // 画黑色横线
            for (int k = 1; k < m_nRows; k++) {
                yy = m_nMapTop + k * m_nWidth;
                myPaint.setARGB(255, 0, 0, 0);
                canvas.drawLine(m_nMapLeft + 1, (int) yy + 1, m_nMapRight - 2, (int) yy + 1, myPaint);
            }

            // 画白色竖线
            for (int k = 1; k < m_nCols; k++) {
                xx = m_nMapLeft + k * m_nWidth;
                myPaint.setARGB(255, 255, 255, 255);
                canvas.drawLine((int) xx, m_nMapTop, (int) xx, m_nMapBottom - 1, myPaint);
            }

            // 画白色横线
            for (int k = 1; k < m_nRows; k++) {
                yy = m_nMapTop + k * m_nWidth;
                myPaint.setARGB(255, 255, 255, 255);
                canvas.drawLine(m_nMapLeft, (int) yy, m_nMapRight - 1, (int) yy, myPaint);
            }

            // 显示样本格子
            if (cur_Rect.top >= 0) {
                myPaint.setARGB(159, 255, 0, 255);
                myPaint.setStyle(Paint.Style.FILL);
                rt.set(cur_Rect.left + mPoff, cur_Rect.top + mPoff, cur_Rect.left + mPoff + mPR, cur_Rect.top + mPoff + mPR);
                canvas.drawRect(cur_Rect.left, cur_Rect.top, rt.left, cur_Rect.bottom, myPaint);
                canvas.drawRect(rt.right, cur_Rect.top, cur_Rect.right, cur_Rect.bottom, myPaint);
                canvas.drawRect(rt.left, cur_Rect.top, rt.right, rt.top, myPaint);
                canvas.drawRect(rt.left, rt.bottom, rt.right, cur_Rect.bottom, myPaint);
            }

            // 根据格子的大小，调整字体的大小
            ss = sp2px(myMaps.ctxDealFile, 11);
            cc = 0;
            if (m_nWidth <= ss + 6) {
                ss /= 2;
                cc = 1;
            }
            if (m_nWidth <= ss / 2 + 15) {
                ss /= 5;
                cc = 2;
            }
            // 显示识别出来的 XSB
            for (int i = 0; i < m_nRows; i++) {
                for (int j = 0; j < m_nCols; j++) {
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
                        } else {  //格子可以正常显示 XSB 字符时，将其变成全角显示，这样定位准确
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
        }

        canvas.restore();

        // 提示关卡尺寸
        if (m_nRows > 2 && m_nCols > 2) {
            ss = sp2px(myMaps.ctxDealFile, 16);
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setTextSize(ss);
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStrokeWidth(5);
            canvas.drawText("关卡尺寸: " + m_nCols + " × " + m_nRows, 10, ss / 2 * 3, myPaint);
            myPaint.setARGB(255, 255, 255, 255);
            myPaint.setStrokeWidth(3);
            canvas.drawText("关卡尺寸: " + m_nCols + " × " + m_nRows, 10, ss / 2 * 3, myPaint);

            m_Recog.myCount();

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
        }

//        if (cur_Rect.top >= 0) {  // debug
//            myPaint.setARGB(255, 255, 255, 255);
//            rt.set(getRight() - mPR * 3 - 4, 2, getRight(), mPR * 3 + 6);
//            canvas.drawRect(rt, myPaint);
//            rt.set(getRight() - mPR * 3 - 2, 4, getRight() - 2, mPR * 3 + 4);
//            canvas.drawBitmap(myMaps.edPict, new Rect(cur_Rect.left + mPoff, cur_Rect.top + mPoff, cur_Rect.right - mPoff, cur_Rect.bottom - mPoff), rt, myPaint);
//        }
    }

    int mPoff = (int) (m_nWidth / 5);         // 焦点格子的外框宽度
    int mPR = (int) (m_nWidth - mPoff * 2);   // 焦点格子的内部边长
    public void setPR() {
        if (m_nWidth < 20) {
            mPoff = (int) (m_nWidth / 4);
        } else {
            mPoff = (int) (m_nWidth / 5);
        }
        mPR = (int) (m_nWidth - mPoff * 2);
    }

    // 分析图片，进行识别
    public ArrayList<Integer> findSubimages() {
        ArrayList<Integer> locs = new ArrayList<Integer>();

        Rect rt0 = new Rect();  // 样本
        Rect rt1 = new Rect();  // 图片格子
        Rect rt2 = new Rect();  // 临时

        int ww = cur_Rect.right - cur_Rect.left - 6;  // 实际取样尺寸，即周边各让出 3 个像素
        Bitmap img0 = Bitmap.createBitmap(ww, ww, myMaps.cfg);   // 样本图片
        Bitmap img1 = Bitmap.createBitmap(ww, ww, myMaps.cfg);   // 格子图片
        Canvas cvs0 = new Canvas(img0);   // 样本画布
        Canvas cvs1 = new Canvas(img1);   // 格子画布

        rt2.set(0, 0, ww, ww);

        rt0.set(cur_Rect.left + 3, cur_Rect.top + 3, cur_Rect.left + 3 + ww, cur_Rect.top + 3 + ww);  // 样本范围
        cvs0.drawBitmap(myMaps.edPict, rt0, rt2, myMaps.myPaint);   // 样本图片
        my_Color0 = getPixelDeviateWeightsArray(m_SampleArray0, img0, 0);

        // 从“左上角”开始搜索子图
        float x;
        float y = m_nMapTop;
        for (int r = 0; r < m_nRows; r++) {
            x = m_nMapLeft;
            for (int c = 0; c < m_nCols; c++) {
                if (m_nObj == 0 || m_Recog.m_cArray[r][c] == '-') {              // 锁定之前识别出来的元素
                    if (r == cur_Row && c == cur_Col) {                                        // 样本格子
                        locs.add(c << 16 | r);
                    } else {
                        rt1.set((int)x + 3, (int)y + 3, (int)x + 3 + ww, (int)y + 3 + ww);  // 格子范围
                        cvs1.drawBitmap(myMaps.edPict, rt1, rt2, myMaps.myPaint);                  // 格子图片
//                        System.out.printf("============== 位置: %d, %d", c, r);
                        if (isSubimage(img1)) {
                            locs.add(c << 16 | r);
                        }
                    }
                }
                x += m_nWidth;
            }
            y += m_nWidth;
        }
        return locs;
    }

    // 与样本比较
    boolean isSubimage(Bitmap img1) {

        int[] m_SampleArray1 = new int[1024];           //格子的比较数组

        my_Color1 = getPixelDeviateWeightsArray(m_SampleArray1, img1, 1);

        double v = calSimilarity(m_SampleArray0, m_SampleArray1) * 10;

//        System.out.printf(", 相似度: %3.2f", v);
//        System.out.print(", 色相: " + my_Color0 + ", " + my_Color1);
//        System.out.print(", 平均灰度值误差: " + Math.abs(my_Grey0 - my_Grey1));

        return ((int)v >= mSimilarity && my_Color1 == my_Color0 && Math.abs(my_Grey0 - my_Grey1) < 5);
    }

    // 取得最大颜色序号
    private int maxColor (int r, int g, int b) {
        if (r - g > 10 && r - b > 10) return 1;         // R
        else if (g - r > 10 && g - b > 10) return 2;    // G
        else if (b - r > 10 && b - g > 10) return 3;    // B
        else if (r - b > 10) return 4;                  // RG
        else if (r - g > 10) return 5;                  // RB
        else if (g - b > 10) return 6;                  // BG
        else return 0;                                  // RGB
    }

    // 获取图片转换灰度图后的像素的比较数组（平均值的离差）
    public int getPixelDeviateWeightsArray(int[] dest, Bitmap img, int avGrey) {
        // 转换至灰度图
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高
        int red, green, blue;
        int[] color = { 0, 0, 0, 0, 0, 0, 0 };
        long sumGrey = 0;

        int[] pixels = new int[width * height]; //通过位图的大小创建像素点数组

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                red = ((grey & 0x00FF0000) >> 16);
                green = ((grey & 0x0000FF00) >> 8);
                blue = (grey & 0x000000FF);

                //计算图块的颜色偏重
                color[maxColor(red, green, blue)]++;

                grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                sumGrey += grey;

                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        if (avGrey == 0) {
            my_Grey0 = (int) (sumGrey / (height*width));
        } else {
            my_Grey1 = (int) (sumGrey / (height*width));
        }
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        image.setPixels(pixels, 0, width, 0, 0, width, height);

        // 缩放至32x32像素缩略图
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) 32 / width);
        matrix.postScale(scaleWidth, scaleWidth);
        image = image.createBitmap(image, 0, 0, width, height, matrix, true);

        // 获取像素数组
        width = image.getWidth();
        height = image.getHeight();
        pixels = new int[width * height];
        image.getPixels(pixels, 0, width, 0, 0, width, height);

        // 获取灰度图的平均像素颜色值
        long sumRed = 0;
        for (int i = 0; i < pixels.length; i++) {
            red = ((pixels[i] & 0x00FF0000) >> 16);
            sumRed += red;
        }
        int averageColor = (int) (sumRed / pixels.length);

        // 获取灰度图的像素比较数组（平均值的离差）
        for (int i = 0; i < pixels.length; i++) {
            dest[i] = ((pixels[i] & 0x00FF0000) >> 16) - averageColor > 0 ? 1 : 0;
        }

//        System.out.println("========================== " + dest.length);

        int m = color[0], n = 0;
        for (int k = 1; k < 7; k++) {
            if (m < color[k]) {
                m = color[k];
                n = k;
            }
        }
        return n;
    }

    // 通过汉明距离计算相似度
    public double calSimilarity(int[] a, int[] b) {
        // 获取两个缩略图的平均像素比较数组的汉明距离（距离越大差异越大）
        int hammingDistance = 0;
        for (int i = 0; i < a.length; i++) {
            hammingDistance += a[i] == b[i] ? 0 : 1;
        }

        // 通过汉明距离计算相似度
        int length = 32*32;
        double similarity = (length - hammingDistance) / (double) length;

        // 使用指数曲线调整相似度结果
        return Math.pow(similarity, 2);
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
        m_nWidth   = -1;    //格子的尺寸
        m_nRows    = 2;     //水平方向的格子数
        m_nMapTop  = 50;
        m_nMapLeft = 50;
        m_nMapBottom = m_nPicHeight-50;
        m_nMapRight = m_nPicWidth-50;
        //画指示器
        L_Rect.set(m_nMapLeft-50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2-50, m_nMapLeft+50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2+50);
        T_Rect.set(m_nMapLeft + (m_nMapRight-m_nMapLeft)/2-50, m_nMapTop-50, m_nMapLeft + (m_nMapRight-m_nMapLeft)/2+50, m_nMapTop+50);
        R_Rect.set(m_nMapRight-50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2-50, m_nMapRight+50, m_nMapTop + (m_nMapBottom-m_nMapTop)/2+50);
        B_Rect.set(m_nMapLeft + (m_nMapRight-m_nMapLeft)/2-50, m_nMapBottom-50, m_nMapLeft + (m_nMapRight-m_nMapLeft)/2+50, m_nMapBottom+50);

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

        cur_Rect.top = -1;   // 开始时，没有焦点格子
        m_Lamp = -1;         // 开始时，没有长按的指示灯
        isRecog = true;      // 开始时，默认为识别模式

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