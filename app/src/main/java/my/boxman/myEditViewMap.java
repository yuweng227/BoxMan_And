package my.boxman;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.widget.Toast;

public class myEditViewMap extends View {

    static final int MOD_EDIT   = 0;  //编辑模式
    static final int MOD_SELECT = 1;  //选择模式
    int mMod = MOD_SELECT;            //当前模式

    public int m_iR, m_iC;  //单击、长按的节点坐标；
    public boolean isFistClick = true;  //指示下一次的点击是否为第一次选择点击

    int m_iR0, m_iC0;  //上次的节点坐标
    boolean isSize = true;  //指示右上角显示尺寸还是游标
    boolean isDrawing = false;  //是否正在连续绘制

    boolean isShowBkPict = true;  //是否显示识别中的背景图片

    myEditView m_Edit;  //父控件指针，以便使用父控件的功能

    Paint myPaint = new Paint();
    public selNode selNode = new selNode(), selNode2 = new selNode(); //选择区域对角点

    public int m_nArenaTop;  //舞台 Top 距屏幕顶的距离
    int m_nPicWidth, m_nPicHeight;  //关卡图的像素尺寸
    public int m_nMapLeft, m_nMapRight, m_nMapTop, m_nMapBottom;  //关卡四至
    int m_PicWidth = 50;   //素材尺寸，即关卡图每个格子的像素尺寸
    public Matrix mMatrix = new Matrix();  //图片原始变换矩阵
    public Matrix mCurrentMatrix = new Matrix();  //当前变换矩阵
    private Matrix mMapMatrix = new Matrix();  //onDraw()用的当前变换矩阵
    float m_fTop, m_fLeft, m_fScale, mScale;  //关卡图的当前上边界、左边界、缩放倍数；原始缩放倍数
    public float mMaxScale = 3;   //最大缩放级别
    float[] values = new float[9];

    Rect rtKW    = new Rect();  //皮肤中，墙（通用）
    Rect rtKF    = new Rect();  //皮肤中，地板
    Rect rtKD    = new Rect();  //皮肤中，目标
    Rect rtKB    = new Rect();  //皮肤中，箱子
    Rect rtKBD   = new Rect();  //皮肤中，目标上的的箱子
    Rect rtKM    = new Rect();  //皮肤中，人
    Rect rtKMD   = new Rect();  //皮肤中，目标上的人
    Rect rtKSel  = new Rect();  //皮肤中，选择框

    Rect rtTop = new Rect();  //顶行信息栏
    Rect rtF = new Rect();    //地板
    Rect rtW = new Rect();    //墙
    Rect rtD = new Rect();    //目标
    Rect rtB = new Rect();    //箱子
    Rect rtM = new Rect();    //人
    Rect rtSize = new Rect(); //关卡尺寸

    int cur_Obj = 0, obj_Width;  //当前素材、顶部素材矩形宽
    char[] m_Objs = {'-', '#', '.', '$', '@'};

    public myEditViewMap(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myEditViewMap(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myEditViewMap(Context context) {
        super(context);

        setOnTouchListener(new TouchListener());
        initView();
    }

    //自动求解
    protected void mySolution() {
        try {
            Intent intent3 = new Intent(Intent.ACTION_MAIN);
            intent3.addCategory(Intent.CATEGORY_LAUNCHER);
            ComponentName name = new ComponentName("net.sourceforge.sokobanyasc.joriswit.yass", "yass.YASSActivity");
            intent3.setComponent(name);
            String actName = intent3.getAction();
            intent3.setAction("nl.joriswit.sokosolver.SOLVE");
            intent3.putExtra("LEVEL", myMaps.curMap.Map);
            m_Edit.startActivityForResult (intent3, 1);
            intent3.setAction(actName);
        } catch (Exception e) {
            MyToast.showToast(myMaps.ctxDealFile, "没有找到求解器！", Toast.LENGTH_SHORT);
        }
    }

    public void mySelectAll() {
        boolean flg = false;
        for (int i = m_nMapTop; i <= m_nMapBottom; i++) {
            for (int j = m_nMapLeft; j <= m_nMapRight; j++) {
                if (m_Edit.isOK(m_Edit.m_cArray[i][j])) {
                    selNode.row  = i;
                    selNode2.row  = selNode.row;
                    flg = true;
                    break;
                }
            }
            if (flg) break;
        }
        if (flg) {  //地图中已经包含了至少一个元素
            flg = false;
            for (int i = m_nMapBottom; i >= selNode.row; i--) {
                for (int j = m_nMapLeft; j <= m_nMapRight; j++) {
                    if (m_Edit.isOK(m_Edit.m_cArray[i][j])) {
                        selNode2.row = i;
                        flg = true;
                        break;
                    }
                }
                if (flg) break;
            }
            flg = false;
            for (int j = m_nMapLeft; j <= m_nMapRight; j++) {
                for (int i = selNode.row; i <= selNode2.row; i++) {
                    if (m_Edit.isOK(m_Edit.m_cArray[i][j])) {
                        selNode.col = j;
                        selNode2.col = selNode.col;
                        flg = true;
                        break;
                    }
                }
                if (flg) break;
            }
            flg = false;
            for (int j = m_nMapRight; j >= selNode.col; j--) {
                for (int i = selNode.row; i <= selNode2.row; i++) {
                    if (m_Edit.isOK(m_Edit.m_cArray[i][j])) {
                        selNode2.col = j;
                        flg = true;
                        break;
                    }
                }
                if (flg) break;
            }
        } else {  //空地图
            selNode.row = m_nMapTop;
            selNode.col = m_nMapLeft;
            selNode2.row = m_nMapBottom;
            selNode2.col = m_nMapRight;
        }
        selNode.row -= m_nMapTop;
        selNode2.row -= m_nMapTop;
        selNode.col -= m_nMapLeft;
        selNode2.col -= m_nMapLeft;
        m_Edit.selRows = selNode2.row - selNode.row + 1;
        m_Edit.selCols = selNode2.col - selNode.col + 1;
        isFistClick = true;
        mMod = MOD_SELECT;  //块模式
        m_Edit.bt_Cut.setEnabled(true);
        m_Edit.bt_Copy.setEnabled(true);
    }

    private void initView() {
        mMod = MOD_SELECT;  //默认当前模式
        selNode.row = -1;  //此时，尚无选择坐标

        //物件尺寸、顶行信息栏高
        obj_Width = myMaps.m_nWinWidth / 10;
        if (obj_Width > m_PicWidth*2) obj_Width = m_PicWidth*2;
        m_nArenaTop = obj_Width + 2;

        //顶行信息栏内，各矩形框
        rtTop.set(0, 0, myMaps.m_nWinWidth, m_nArenaTop);      //顶行信息栏
        rtF.set( 1             , 1, 1              +obj_Width, obj_Width+1);  //地板
        rtW.set( obj_Width+2   , 1,  obj_Width     +obj_Width, obj_Width+1);  //墙
        rtD.set((obj_Width+2)*2, 1, (obj_Width+2)*2+obj_Width, obj_Width+1);  //目标
        rtB.set((obj_Width+2)*3, 1, (obj_Width+2)*3+obj_Width, obj_Width+1);  //箱子
        rtM.set((obj_Width+2)*4, 1, (obj_Width+2)*4+obj_Width, obj_Width+1);  //人
        rtSize.set(rtM.right+10, 5, myMaps.m_nWinWidth-5, m_nArenaTop-5);  //关卡尺寸

        //皮肤中各物件的矩形框
        rtKW.set(0, 0, 50, 50);         //皮肤中，墙（通用）
        rtKF.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);      //皮肤中，地板
        rtKD.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);      //皮肤中，目标
        rtKB.set(50, 250-myMaps.isSkin_200, 100, 300-myMaps.isSkin_200);    //皮肤中，箱子
        rtKBD.set(50, 300-myMaps.isSkin_200, 100, 350-myMaps.isSkin_200);   //皮肤中，目标上的的箱子
        rtKM.set(100, 250-myMaps.isSkin_200, 150, 300-myMaps.isSkin_200);   //皮肤中，人
        rtKMD.set(100, 300-myMaps.isSkin_200, 150, 350-myMaps.isSkin_200);  //皮肤中，目标上的人
        rtKSel.set(100, 200-myMaps.isSkin_200, 150, 250-myMaps.isSkin_200); //皮肤中，选择框
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
        private static final int MODE_DRAW   = 3;  //连续绘制模式
        private int mMode = MODE_NONE;      //当前模式

        private float mStartDis;  //缩放开始时的手指间距
        private PointF mStartPoint = new PointF();  //触点坐标
        private PointF mid = new PointF();  //手势中心点

        private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            //单触点，长按时触发
            public void onLongPress(MotionEvent e) {
                int x = (int)e.getX(), y = (int) e.getY();
                if (rtM.contains(x, y)) {  //长按仓管员素材，自动求解一下下，方便摆箱子方式求解者
                    if (m_Edit.Normalize2 (m_Edit.m_cArray)) mySolution ();
                } else {
                    if (y < m_nArenaTop) {
                        //块编辑模式，长按素材
                        if (mMod == MOD_SELECT) {
                            int old_obj = cur_Obj;
                            cur_Obj = -1;
                            if (rtF.contains (x, y)) {
                                cur_Obj = 0;  //地板
                            } else if (rtW.contains (x, y)) {
                                cur_Obj = 1;  //素材--墙壁
                            } else if (rtD.contains (x, y)) {
                                cur_Obj = 2;  //素材--目标
                            } else if (rtB.contains (x, y)) {
                                cur_Obj = 3;  //素材--箱子
                            }
                            //不能用仓管员填充，没有选区不能填充
                            if (cur_Obj >= 0 && cur_Obj < 4 && selNode.row >= 0 && (selNode.row != selNode2.row || selNode.col != selNode2.col)) {
                                m_Edit.DoAct (0);  //填充区域或区域勾边
                            }
                            if (cur_Obj < 0) cur_Obj = old_obj;
                        }
                        if (rtSize.contains (x, y)) {  //尺寸--以最小的矩形区域选择地图中的全部素材（地板除外）
                            mMod = MOD_SELECT;
                            mySelectAll ();
                        }
                    } else {
                        isSize = false;  //长按地图区域，右上角显示游标
                        doACT (x, y, false, true);  //仅计算游标
                    }
                    invalidate ();
                }
            }

            //单触点，不滑动，略延迟触发
            public void onShowPress(MotionEvent e) {
                //编辑状态时，用此办法触发连续画
                if ((int) e.getY() > m_nArenaTop) {
                    mMode = MODE_DRAW;
                }
            }

            //单击，抬起时触发
            public boolean onSingleTapUp(MotionEvent e) {
                int x = (int)e.getX(), y = (int) e.getY();
                if (y < m_nArenaTop) {
                    if (rtF.contains(x, y)) {
                        if (mMod != MOD_EDIT) mMod = MOD_EDIT;
                        cur_Obj = 0;  //地板
                    } else if (rtW.contains(x, y)) {
                        if (mMod != MOD_EDIT) mMod = MOD_EDIT;
                        cur_Obj = 1;  //素材--墙壁
                    } else if (rtD.contains(x, y)) {
                        if (mMod != MOD_EDIT) mMod = MOD_EDIT;
                        cur_Obj = 2;  //素材--目标
                    } else if (rtB.contains(x, y)) {
                        if (mMod != MOD_EDIT) mMod = MOD_EDIT;
                        cur_Obj = 3;  //素材--箱子
                    } else if (rtM.contains(x, y)) {
                        if (mMod != MOD_EDIT) mMod = MOD_EDIT;
                        cur_Obj = 4;  //素材--仓管员
                    } else if (rtSize.contains(x, y)) {  //尺寸区域--模式转换
                        if (mMod != MOD_SELECT) {
                            mMod = MOD_SELECT;  //转换到选择状态
                            selNode.row = -1;  //此种转换，尚无选择坐标
                            isFistClick = true;  //准备记录第一坐标点
                        } else mMod = MOD_EDIT;  //转换到编辑状态
                        //设置剪切、复制按钮的状态为不可用
                        if (mMod == MOD_EDIT || selNode.row < 0) {
                            selNode.row = -1;  //此种转换，尚无选择坐标
                            isFistClick = true;  //准备记录第一坐标点
                            m_Edit.bt_Cut.setEnabled(false);
                            m_Edit.bt_Copy.setEnabled(false);
                        }
                    }
                } else if (mMod == MOD_SELECT || mMode != MODE_DRAW ) {
                    doACT((int) e.getX(), (int) e.getY(), false, false);
                    mMode = MODE_NONE;
                }
                return true;
            }

            public boolean onDoubleTap(MotionEvent e) {
                // 双击顶部素材行，打开或关闭识别时的背景图片
                if (e.getRawY() < m_nArenaTop && myMaps.curMapNum == -4 && myMaps.edPict != null) {
                    isShowBkPict = !isShowBkPict;
                    invalidate();
                }
                return true;
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {  //onTouchEvent
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isDrawing = false;
                    mStartPoint.set(event.getX(), event.getY());  //记录触点坐标

                    //单触点按下时，默认首先触发拖动模式
                    mMode = MODE_DRAG;

                    invalidate();
                    break;
                case MotionEvent.ACTION_POINTER_UP:  //只要有触点抬起，即结束拖动或缩放
                case MotionEvent.ACTION_CANCEL:
                    mMode = MODE_NONE;
                case MotionEvent.ACTION_UP:
                    isDrawing = false;
                    isSize = true;  //右上角恢复显示关卡尺寸状态
                    reSetMatrix();
                    m_Edit.getBoxs();  //统计箱子数、目标数

                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mMod == MOD_SELECT) {  //浏览状态
                        if (mMode == MODE_ZOOM) setZoomMatrix(event);  //缩放
                        else if (mMode == MODE_DRAG) setDragMatrix(event);  //拖动
                    } else {  //编辑状态
                        if (mMode == MODE_ZOOM) setZoomMatrix(event);  //缩放
                        else {
                            if (mMode == MODE_DRAG)
                                setDragMatrix(event);  //拖动
                            else if (mMode == MODE_DRAW)
                                doACT((int) event.getX(), (int) event.getY(), true, false);  //单触点略延迟拖动可连续绘制（仅编辑状态）
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
    }

    //计算使用哪个墙壁图片
    private Rect getWall(Rect rt, int r, int c) {

        if (myMaps.isSkin_200 == 200) {
            rt.set(0, 0, 50, 50);
            return rt;
        }

        int bz = 0;

        //看看哪个方向上有“墙”
        if (c > m_nMapLeft && m_Edit.m_cArray[r][c - 1] == '#') bz |= 1; //左
        if (r > m_nMapTop && m_Edit.m_cArray[r - 1][c] == '#') bz |= 2; //上
        if (c < m_nMapRight && m_Edit.m_cArray[r][c + 1] == '#') bz |= 4; //右
        if (r < m_nMapBottom && m_Edit.m_cArray[r + 1][c] == '#') bz |= 8; //下

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

    Rect rt = new Rect();
    Rect rt0 = new Rect();
    char ch;
    String mStr;
    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        setBackgroundColor(Color.BLACK);  //背景色设置

        canvas.save();

        mCurrentMatrix.getValues(values);
        values[Matrix.MTRANS_Y] += m_nArenaTop;
        mMapMatrix.setValues(values);
        m_fTop = values[Matrix.MTRANS_Y];
        m_fLeft = values[Matrix.MTRANS_X];
        m_fScale = values[Matrix.MSCALE_X];
        canvas.setMatrix(mMapMatrix);

        if (isShowBkPict && myMaps.curMapNum == -4 && myMaps.edPict != null) {  // 来自图像识别
            myPaint.setARGB(127, 0, 0, 0);
            canvas.drawBitmap(myMaps.edPict, new Rect(myMaps.edPictLeft, myMaps.edPictTop, myMaps.edPictRight, myMaps.edPictBottom),
                                             new Rect(0, 0, m_nPicWidth, m_nPicHeight), myPaint);
        }

        for (int r = m_nMapTop, i, j; r <= m_nMapBottom; r++) {
            for (int c = m_nMapLeft; c <= m_nMapRight; c++) {
                i = r - m_nMapTop;
                j = c - m_nMapLeft;
                rt.set(m_PicWidth*j, m_PicWidth*i, m_PicWidth*j + m_PicWidth, m_PicWidth*i + m_PicWidth);

                ch = m_Edit.m_cArray[r][c];   //关卡数据

                myPaint.setARGB(255, 0, 0, 0);

                //第一层显示————地板
                if (ch == '#') {
                    canvas.drawBitmap(myMaps.skinBit, rtKF, rt, myPaint);
                } else if (ch != '-' || !isShowBkPict || myMaps.curMapNum != -4 || myMaps.edPict == null) {
                    canvas.drawBitmap(myMaps.skinBit, rtKF, rt, myPaint);
                }

                //第二层显示————目标点
                if (ch == '.' || ch == '*' || ch == '+') {
                    canvas.drawBitmap(myMaps.skinBit, rtKD, rt, myPaint);
                }
                //第三层显示————箱子或人
                switch (ch) {
                    case '#':   //墙壁属于第一层显示
                        rtKW = getWall(rtKW, r, c);  //计算使用哪个“墙”图
						canvas.drawBitmap(myMaps.skinBit, rtKW, rt, myPaint);
                        break;
                    case '$':
                        canvas.drawBitmap(myMaps.skinBit, rtKB, rt, myPaint);
                        break;
                    case '*':
                        canvas.drawBitmap(myMaps.skinBit, rtKBD, rt, myPaint);
                        break;
                    case '@':
                        canvas.drawBitmap(myMaps.skinBit, rtKM, rt, myPaint);
                        break;
                    case '+':
                        canvas.drawBitmap(myMaps.skinBit, rtKMD, rt, myPaint);
                        break;
                } //end switch
                if (ch == '#' && (myMaps.m_Sets[22] & 1) > 0 ||
                    (ch == '-' || ch == '_' || ch == ' ' || ch == '\0') && (myMaps.m_Sets[22] & 2) > 0 ||
                    ch == '.' && (myMaps.m_Sets[22] & 4) > 0 ||
                    (ch == '$' || ch == '*') && (myMaps.m_Sets[22] & 8) > 0 ||
                    (ch == '@' || ch == '+') && (myMaps.m_Sets[22] & 16) > 0) {
                    mStr = String.valueOf(mGetCur2(i, j));  //标尺
                    myPaint.setTextSize(m_PicWidth /3);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt0);
                    myPaint.setColor(myMaps.m_Sets[21]);
                    canvas.drawText(mStr, rt.left + (m_PicWidth-rt0.width())/2, rt.top + (m_PicWidth+rt0.height())/2, myPaint);
                    myPaint.setARGB(255, 0, 0, 0);
                }
            }  //end for j
        } //end for i

        if (mMod == MOD_SELECT && selNode.row >= 0) {  //选择状态
            myPaint.setARGB(100, 200, 0, 200);
            myPaint.setStyle(Paint.Style.FILL);
            rt.set(m_PicWidth*selNode.col, m_PicWidth*selNode.row, m_PicWidth*selNode2.col + m_PicWidth, m_PicWidth*selNode2.row + m_PicWidth);
            if (!isFistClick) {  //第一选择点
                canvas.drawRect(rt, myPaint);
                canvas.drawBitmap(myMaps.skinBit, rtKSel, rt, myPaint);
            } else {  //第二选择点
                canvas.drawRect(rt, myPaint);
            }
        }

        canvas.restore();

        //顶行信息栏
        if (m_nArenaTop > 0) {
            //顶部信息栏矩形
            myPaint.setStyle(Paint.Style.FILL);
            myPaint.setARGB(255, 119, 136, 153);
            canvas.drawRect(rtTop, myPaint);

            //顶部信息栏素材
            myPaint.setARGB(255, 0, 0, 0);
            canvas.drawBitmap(myMaps.skinBit, rtKF,   rtF, myPaint);   //皮肤中，地板
            canvas.drawBitmap(myMaps.skinBit, rtKW,   rtW, myPaint);   //皮肤中，墙（通用的墙）
            canvas.drawBitmap(myMaps.skinBit, rtKD,   rtD, myPaint);   //皮肤中，目标
            canvas.drawBitmap(myMaps.skinBit, rtKB,   rtB, myPaint);   //皮肤中，箱子
            canvas.drawBitmap(myMaps.skinBit, rtKM,   rtM, myPaint);   //皮肤中，人

            //标示选择素材
            if (mMod == MOD_EDIT) {
                rt.set((obj_Width + 2) * cur_Obj, 1, (obj_Width + 2) * cur_Obj + obj_Width, obj_Width + 1);
                canvas.drawBitmap(myMaps.skinBit, rtKSel, rt, myPaint);   //皮肤中，选择框
            }

            //关卡尺寸框，底色，作为区别“常规编辑模式”与“块编辑模式”
            if (mMod == MOD_SELECT) myPaint.setARGB(127, 0, 0, 0);  //块编辑模式的底色
            else myPaint.setARGB(191, 0, 63, 0);  //常规编辑模式的底色

            canvas.drawRect(rtSize, myPaint);
            myPaint.setARGB(255, 255, 255, 255);
            myPaint.setTextSize(obj_Width * 2/5);

            //计算尺寸或游标
            if (isSize) {
                mStr = (m_nMapRight-m_nMapLeft+1)  + "列" + (m_nMapBottom-m_nMapTop+1) + "行" + m_Edit.getBoxs();  //关卡尺寸
            } else {
                if (m_iR < 0 || m_iC < 0 || m_iR > (m_nMapBottom-m_nMapTop) || m_iC > (m_nMapRight-m_nMapLeft)) mStr = " ";
                else mStr = String.valueOf(mGetCur(m_iR, m_iC));  //游标
            }

            //写关卡尺寸、游标
            myPaint.getTextBounds(mStr, 0, mStr.length(), rt);
            canvas.drawText(mStr, rtSize.left + (rtSize.width()-rt.width())/2, (rtSize.height()-rt.height())/2+rt.height(), myPaint);
        }
    }

    //根据位置，自动计算标尺
    private String mGetCur2(int r, int c) {

        StringBuilder s = new StringBuilder();

        int k = c / 26 + 64;
        if (k > 64) s.append((char) (byte) k);

        s.append((char) ((byte) (c % 26 + 65))).append(String.valueOf(1 + r));

        return s.toString();
    }

    //根据位置，自动计算标尺
    private String mGetCur(int r, int c) {

        StringBuilder s = new StringBuilder();

        int k = c / 26 + 64;
        if (k > 64) s.append((char) (byte) k);

        s.append((char) ((byte) (c % 26 + 65))).append(String.valueOf(1 + r));

        s.append(" [ ").append(c+1).append(", ").append(r+1).append(" ]");

        return s.toString();
    }

    //根据点击位置确定行动
    private void doACT(int i, int j, boolean bSontinuous, boolean flg) {  //bSontinuous, 是否连续绘制；flg，是否仅计算游标

        if (j < m_nArenaTop) {
            return;
        } else if (i < m_fLeft || j < m_fTop) {
            m_iC = -1;
            m_iR = -1;
        } else {
			m_iC = ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
			m_iR = ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
        }
        if (flg || m_iR < 0 || m_iR > m_nMapBottom-m_nMapTop || m_iC < 0 || m_iC > m_nMapRight-m_nMapLeft) {
            return;
        }

        if (mMod == MOD_SELECT) {  //选择状态
            if (isFistClick) {  //记录第一选择点
                selNode.row  = m_iR;
                selNode.col  = m_iC;
                selNode2.row = m_iR;
                selNode2.col = m_iC;

                isFistClick  = false;
            } else {  //记录第二选择点
                if (m_iR < selNode.row) selNode.row = m_iR;
                else selNode2.row = m_iR;
                if (m_iC < selNode.col) selNode.col = m_iC;
                else selNode2.col = m_iC;

                isFistClick = true;
            }
            m_Edit.selRows = selNode2.row - selNode.row + 1;
            m_Edit.selCols = selNode2.col - selNode.col + 1;

            //设置剪切、复制按钮的状态为可用
            m_Edit.bt_Cut.setEnabled(true);
            m_Edit.bt_Copy.setEnabled(true);
        } else if (mMod == MOD_EDIT) {  //编辑状态，向关卡数组中写数据
            //编辑时，按关卡四至调整数据在关卡数组中的位置
            //如此处理，方便关卡向四周扩充（扩充时，仅调整四至即可，无需移动数组中的大量数据）
            if (!isDrawing) {  //绘制之前
                if (bSontinuous) m_Edit.DoAct(6);  //连续绘制动作入 UnDo 栈
                else m_Edit.DoAct(3);  //单点绘制动作入 UnDo 栈
            }
            isDrawing = true;  //绘制之后
            if (!bSontinuous || m_iR != m_iR0 || m_iC != m_iC0) {  //非连续绘制；或连续绘制时，坐标改变
                if (myMaps.m_Sets[19] == 1) {
                    switch (m_Objs[cur_Obj]) {
                        case '.':
                            if (m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '$' ||
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '.')
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = '*';
                            else if (m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '@')
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = '+';
                            else
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = '.';
                            break;
                        case '$':
                            if (m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '.' ||
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '+' ||
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '$')
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = '*';
                            else
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = '$';
                            break;
                        case '@':
                            for (int r = m_nMapTop; r <= m_nMapBottom; r++) {  //仓管员只能留存 1 位
                                for (int c = m_nMapLeft; c <= m_nMapRight; c++) {
                                     if (r-m_nMapTop != m_iR || c-m_nMapLeft != m_iC) {
                                        if (m_Edit.m_cArray[r][c] == '+')
                                            m_Edit.m_cArray[r][c] = '.';
                                        else if (m_Edit.m_cArray[r][c] == '@')
                                            m_Edit.m_cArray[r][c] = '-';
                                    }
                                }
                            }
                            if (m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '.' ||
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '@' ||
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] == '*')
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = '+';
                            else
                                m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = '@';
                            break;
                        default:
                            m_Edit.m_cArray[m_iR+m_nMapTop][m_iC+m_nMapLeft] = m_Objs[cur_Obj];
                            break;
                    }
                } else {
                    switch (m_Objs[cur_Obj]) {
                        case '#':
                            if (bSontinuous) {
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '#';
                            } else {
                                if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '#')
                                    m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '-';
                                else m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '#';
                            }
                            break;
                        case '.':
                            if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '$') {
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '*';
                            } else if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '@') {
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '+';
                            } else if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '*') {
                                if (!bSontinuous) m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '$';
                            } else if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '+') {
                                if (!bSontinuous) m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '@';
                            } else if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '.') {
                                if (!bSontinuous) m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '-';
                            } else m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '.';
                            break;
                        case '$':
                            if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '.' ||
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '+') {
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '*';
                            } else if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '*') {
                                if (!bSontinuous) m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '.';
                            } else if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '$') {
                                if (!bSontinuous) m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '-';
                            } else m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '$';
                            break;
                        case '@':
                            for (int r = m_nMapTop; r <= m_nMapBottom; r++) {  //仓管员只能留存 1 位
                                for (int c = m_nMapLeft; c <= m_nMapRight; c++) {
                                    if (m_Edit.m_cArray[r][c] == '+')
                                        m_Edit.m_cArray[r][c] = '.';
                                    else if (m_Edit.m_cArray[r][c] == '@')
                                        m_Edit.m_cArray[r][c] = '-';
                                }
                            }
                            if (m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '.' ||
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] == '*')
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '+';
                            else
                                m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = '@';
                            break;
                        default:
                            m_Edit.m_cArray[m_iR + m_nMapTop][m_iC + m_nMapLeft] = m_Objs[cur_Obj];
                            break;
                    }
                }
                m_iR0 = m_iR;
                m_iC0 = m_iC;
            }
        }

        invalidate();
    }

    //undo、redo 使用
    public void initArena3() {
        //计算关卡图的像素尺寸
        m_nPicWidth = m_PicWidth * (m_nMapRight - m_nMapLeft + 1);
        m_nPicHeight = m_PicWidth * (m_nMapBottom - m_nMapTop + 1);
        mMatrix = getInnerMatrix(mMatrix);  //计算原始变换矩阵
        mMatrix.getValues(values);
        mScale = values[Matrix.MSCALE_X];  //原始缩放倍数
        mCurrentMatrix.getValues(values);
        m_fTop = values[Matrix.MTRANS_Y];  //当前上边界
        m_fLeft = values[Matrix.MSCALE_X];  //当前左边界
        m_fScale = mScale;  //当前缩放倍数
    }

    //调整关卡尺寸时调用舞台初始化
    public void initArena2() {
        //计算关卡图的像素尺寸
        m_nPicWidth = m_PicWidth * (m_nMapRight - m_nMapLeft + 1);
        m_nPicHeight = m_PicWidth * (m_nMapBottom - m_nMapTop + 1);
//        mMatrix = getInnerMatrix(mMatrix);  //计算原始变换矩阵
//        mMatrix.getValues(values);
//        mScale = values[Matrix.MSCALE_X];  //原始缩放倍数
        mMatrix = getInnerMatrix(mMatrix);  //计算原始变换矩阵
        mCurrentMatrix.set(mMatrix);  //同时得到当前变换矩阵
        mMatrix.getValues(values);
        mScale = values[Matrix.MSCALE_X];  //原始缩放倍数
        m_fTop = values[Matrix.MTRANS_Y];  //当前上边界
        m_fLeft = values[Matrix.MSCALE_X];  //当前左边界
        m_fScale = mScale;  //当前缩放倍数
        invalidate();
    }

    //舞台初始化
    public void initArena() {
        //计算关卡图的像素尺寸
		m_nPicWidth = m_PicWidth * (m_nMapRight - m_nMapLeft + 1);
		m_nPicHeight = m_PicWidth * (m_nMapBottom - m_nMapTop + 1);

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
        mMod = MOD_SELECT;
        selNode.row = -1;  //此时，尚无选择坐标
        isFistClick = true;  //准备记录第一坐标点

        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);

        if (w > 0 && h > 0 && (h != old_h || w != old_w)) {
            setArena();  //重置舞台场地
        }
    }

    public void Init(myEditView v) {
        m_Edit = v;
    }
}