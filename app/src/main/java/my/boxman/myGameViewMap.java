package my.boxman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.TimeZone;

public class myGameViewMap extends View {

    public int d_Moves;  //动画时，移动步数
    public int m_iR, m_iC;  //单击的节点坐标
    int m_iR2, m_iC2;  //长按的节点坐标
    static int ladderDir = 0, ladderDir2 = 0;  //阶梯方向
    LinkedList<Ladder> mLadder = new LinkedList<Ladder>(), mLadder2 = new LinkedList<Ladder>();  //阶梯
    public int Box_Row0, Box_Col0, click_Box_Row, click_Box_Col, click_Box_Row2, click_Box_Col2;  //记录正推箱子移动之前的坐标、（正逆推）点击的箱子
    public int m_Count[] = {0, 0, 0, 0, 0, 0};  //计数（正逆推）“箱子数、目标数、完成数”
    private myGameView m_Game;  //父控件指针，以便使用父控件的功能

    Paint myPaint = new Paint();
    boolean m_lShowDst2 = false; //是否在“逆推时使用正推目标点”状态下，采用另外的方式提示（双目标点提示模式）
    boolean m_lShowAnsInf = false; //是否允许在开始推关卡之前显示答案信息
    boolean m_lEven; //是否偶半位
    boolean m_lWallTop; //是否画墙顶
    boolean m_bBoxTo; //是否在提示箱子可达位置状态
    boolean m_bBoxTo2;
    boolean m_bManTo; //是否在提示人可达位置状态
    boolean m_bManTo2;
    boolean m_boxCanMove; //是否在提示可动箱子状态
    boolean m_boxCanMove2;
    boolean m_boxCanCome; //是否在提示可推过来的箱子状态
    boolean m_boxCanCome2;
    boolean m_boxNoMoved; //是否在提示未动的箱子状态
    boolean m_boxNoUsed; //是否在提示未使用地板的状态
    boolean m_lChangeBK; //是否显示更换背景按钮
    selNode selNode, selNode2; //计数区域对角点，计数区域内的各类箱子数量
    char mClickObj;  //点击的物件（箱子、空地、墙壁等）

    public int m_nArenaTop;  //舞台 Top 距屏幕顶的距离
    int w_bkPic, h_bkPic, w_bkNum, h_bkNum;  //（舞台用）背景图片的宽、高；及其平铺时的横、纵个数
    int m_nPicWidth, m_nPicHeight, m_nRows, m_nCols;  //关卡图的像素尺寸
    int m_PicWidth = 50;   //素材尺寸，即关卡图每个格子的像素尺寸
    Matrix mMatrix = new Matrix();  //图片原始变换矩阵
    Matrix mCurrentMatrix = new Matrix();  //当前变换矩阵
    Matrix mMapMatrix = new Matrix();  //当前变换矩阵
    float m_fTop, m_fLeft, m_fScale, mScale;  //关卡图的当前上边界、左边界、缩放倍数；原始缩放倍数
    float[] values = new float[9];

    //上一关、下一关按钮
    private Rect m_rNext;
    private Rect m_rPre;
    private Bitmap bitNext;
    private Bitmap bitPre;
    private Rect m_rTrans;  //可以触发“选择宏”的区域
    private Rect m_rPre_Skin;  //可以触发“上一个皮肤”的区域
    private Rect m_rNext_Skin;  //可以触发“下一个皮肤”的区域
    private Rect m_rPre_BK;  //可以触发“上一个背景”的区域
    private Rect m_rNext_BK;  //可以触发“下一个背景”的区域
    private Rect m_rColor_BK;  //可以触发“背景色”的区域
    private Rect m_rProgress_Bar;  //可以触发“进度条”的区域
    private Rect m_rNext_Speed;  //可以触发“下一速度值”的区域
    private Rect m_rChangeBK;  //可以触发更改“背景”设置的区域
    private Rect m_rRecording;  //可以触发更改关闭“录制”设置的区域

    //UnDo、ReDo按钮，全屏时使用
    private Rect m_rUnDo;
    private Rect m_rReDo;
    private Bitmap bitUnDo;
    private Bitmap bitReDo;
    private Bitmap bitPreBK;
    private Bitmap bitNextBK;
    private Bitmap bitPreSkin;
    private Bitmap bitNextSkin;
    private Bitmap bitColorBK;
    int curButton = 0;

    private Bitmap bitInvalid;  // 无效关卡图片

    public ArrayList<Integer> myMacro = new ArrayList<Integer>();
    public String myMacroInf = "";

    //奇偶格位明暗度调整
    boolean m_lParityBrightnessShade = false;
    private Rect mrBrightnessShade, mrBrightnessShade2;

    //进度条
    boolean m_lGoto = false, m_lGoto2 = false;
    public int stLeft, stTop, stBottom, stRight, curMoves, curMoves2;
    private Rect mrProgressBar;

    public myGameViewMap(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myGameViewMap(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(new TouchListener());
        initView();
    }

    public myGameViewMap(Context context) {
        super(context);

        setOnTouchListener(new TouchListener());
        initView();
    }

    private void initView() {
        selNode = new selNode();
        selNode2 = new selNode();

        //准备顶行信息栏
        m_nArenaTop = myMaps.m_nWinHeight * 27 / 640;  //信息栏高度

        m_rPre = new Rect();
        m_rNext = new Rect();
        m_rPre.set(0, 0, m_nArenaTop * 4 / 3, m_nArenaTop); //上一关
        m_rNext.set(myMaps.m_nWinWidth - m_nArenaTop * 4 / 3, 0, myMaps.m_nWinWidth, m_nArenaTop); //下一关

        m_rRecording = new Rect();
        m_rTrans = new Rect();
        m_rProgress_Bar = new Rect();
        m_rNext_Speed = new Rect();
        m_rChangeBK = new Rect();

        int w0 = m_rPre.right+8;
        int w = ((m_rNext.left-8) - w0) / 4;
        m_rTrans.set(w0, 0, w0+w, m_nArenaTop-2); //选择宏
        m_rProgress_Bar.set(w0+w, 0, w0+w*2-8, m_nArenaTop-2); //进度条
        m_rNext_Speed.set(w0+w*2, 0, w0+w*3-8, m_nArenaTop-2); //下一速度值
        m_rChangeBK.set(w0+w*3, 0, m_rNext.left-8, m_nArenaTop-2); //切换更改背景模式的区域
        ss = sp2px(myMaps.ctxDealFile, 16);
        m_rRecording.set(0, m_nArenaTop+8, ss*4+ss/2, m_nArenaTop+ss+ss/2+8); //关闭“录制模式的区域

        bitPre = Bitmap.createBitmap(m_nArenaTop * 4 / 3, m_nArenaTop, myMaps.cfg); //上一关按钮图片
        Canvas cvs01 = new Canvas(bitPre);
        Drawable dw01 = myMaps.res.getDrawable(R.drawable.prebtn);
        dw01.setBounds(0, 0, m_nArenaTop * 4 / 3, m_nArenaTop);
        dw01.draw(cvs01);
        bitNext = Bitmap.createBitmap(m_nArenaTop * 4 / 3, m_nArenaTop, myMaps.cfg); //下一关按钮图片
        Canvas cvs02 = new Canvas(bitNext);
        Drawable dw02 = myMaps.res.getDrawable(R.drawable.nextbtn);
        dw02.setBounds(0, 0, m_nArenaTop * 4 / 3, m_nArenaTop);
        dw02.draw(cvs02);

        m_rUnDo = new Rect();
        m_rReDo = new Rect();
        m_rUnDo.set(m_nArenaTop, myMaps.m_nWinHeight-m_nArenaTop*3, m_nArenaTop*3, myMaps.m_nWinHeight-m_nArenaTop); //UnDo
        m_rReDo.set(m_rUnDo.right+m_nArenaTop, m_rUnDo.top, m_rUnDo.right+m_nArenaTop*3, m_rUnDo.bottom); //ReDo
        bitUnDo = Bitmap.createBitmap(m_nArenaTop*2, m_nArenaTop*2, myMaps.cfg); //UnDo按钮图片
        Canvas cvs03 = new Canvas(bitUnDo);
        Drawable dw03 = myMaps.res.getDrawable(R.drawable.undobtn);
        dw03.setBounds(0, 0, m_nArenaTop*2, m_nArenaTop*2);
        dw03.draw(cvs03);
        bitReDo = Bitmap.createBitmap(m_nArenaTop*2, m_nArenaTop*2, myMaps.cfg); //ReDo按钮图片
        Canvas cvs04 = new Canvas(bitReDo);
        Drawable dw04 = myMaps.res.getDrawable(R.drawable.redobtn);
        dw04.setBounds(0, 0, m_nArenaTop*2, m_nArenaTop*2);
        dw04.draw(cvs04);

        bitInvalid = Bitmap.createBitmap(50, 50, myMaps.cfg); //无效关卡图片
        Canvas cvs00 = new Canvas(bitInvalid);
        Drawable dw00 = myMaps.res.getDrawable(R.drawable.defbit);
        dw00.setBounds(0, 0, 50, 50);
        dw00.draw(cvs00);

        m_rPre_BK = new Rect();
        m_rNext_BK = new Rect();
        m_rColor_BK = new Rect();
        m_rPre_Skin = new Rect();
        m_rNext_Skin = new Rect();
        m_rPre_BK.set(myMaps.m_nWinWidth-m_nArenaTop*4, m_nArenaTop*3/2, myMaps.m_nWinWidth-m_nArenaTop*2, m_nArenaTop*2+m_nArenaTop*3/2); //上一个背景
        m_rColor_BK.set(m_rPre_BK.left, m_rPre_BK.bottom, m_rPre_BK.right, m_rPre_BK.bottom+m_nArenaTop*2); //背景色
        m_rNext_BK.set(m_rPre_BK.left, m_rColor_BK.bottom, m_rPre_BK.right, m_rColor_BK.bottom+m_nArenaTop*2); //下一个背景
        m_rPre_Skin.set(m_rPre_BK.left-m_nArenaTop*2, m_rPre_BK.top+m_nArenaTop*2, m_rPre_BK.left, m_rPre_BK.top+m_nArenaTop*4); //上一个皮肤
        m_rNext_Skin.set(m_rPre_BK.right, m_rColor_BK.top, myMaps.m_nWinWidth, m_rColor_BK.bottom); //下一个皮肤

        bitPreBK = Bitmap.createBitmap(m_nArenaTop*2, m_nArenaTop*2, myMaps.cfg); //上一个背景按钮图片
        Canvas cvs05 = new Canvas(bitPreBK);
        Drawable dw05 = myMaps.res.getDrawable(R.drawable.prebk);
        dw05.setBounds(0, 0, m_nArenaTop*2, m_nArenaTop*2);
        dw05.draw(cvs05);
        bitNextBK = Bitmap.createBitmap(m_nArenaTop*2, m_nArenaTop*2, myMaps.cfg); //下一个背景按钮图片
        Canvas cvs06 = new Canvas(bitNextBK);
        Drawable dw06 = myMaps.res.getDrawable(R.drawable.nextbk);
        dw06.setBounds(0, 0, m_nArenaTop*2, m_nArenaTop*2);
        dw06.draw(cvs06);
        bitColorBK = Bitmap.createBitmap(m_nArenaTop*2, m_nArenaTop*2, myMaps.cfg); //背景色
        Canvas cvs09 = new Canvas(bitColorBK);
        Drawable dw09 = myMaps.res.getDrawable(R.drawable.bk_color);
        dw09.setBounds(0, 0, m_nArenaTop*2, m_nArenaTop*2);
        dw09.draw(cvs09);
        bitPreSkin = Bitmap.createBitmap(m_nArenaTop*2, m_nArenaTop*2, myMaps.cfg); //上一个皮肤按钮图片
        Canvas cvs07 = new Canvas(bitPreSkin);
        Drawable dw07 = myMaps.res.getDrawable(R.drawable.preskin);
        dw07.setBounds(0, 0, m_nArenaTop*2, m_nArenaTop*2);
        dw07.draw(cvs07);
        bitNextSkin = Bitmap.createBitmap(m_nArenaTop*2, m_nArenaTop*2, myMaps.cfg); //下一个皮肤按钮图片
        Canvas cvs08 = new Canvas(bitNextSkin);
        Drawable dw08 = myMaps.res.getDrawable(R.drawable.nextskin);
        dw08.setBounds(0, 0, m_nArenaTop*2, m_nArenaTop*2);
        dw08.draw(cvs08);
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
        private static final int MODE_NONE = 0;
        private static final int MODE_DRAG = 1;  //拖动模式
        private static final int MODE_ZOOM = 2;  //缩放模式
        private int mMode = MODE_NONE;      //当前模式

        float mMaxScale = 5;   //最大缩放级别
        private float mStartDis;  //缩放开始时的手指间距
        private PointF mStartPoint = new PointF(), mClickPoint = new PointF();  //第一触点，相对及绝对坐标
        private PointF mid = new PointF();  //手势中心点

        private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            //单触点，不滑动，略延迟触发
            public void onShowPress(MotionEvent e) {
                if (m_lShowAnsInf) m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_Game.m_bNetLock = false;  //取消网型提示
                if (m_rUnDo.contains((int) e.getX(), (int) e.getY())) {
                    curButton = 1;  //启动悬停按钮的拖动
                    mMode = MODE_DRAG;
                    mStartPoint.set(e.getX(), e.getY());  //相对坐标
                } else if (m_rReDo.contains((int) e.getX(), (int) e.getY())) {
                    curButton = 2;  //启动悬停按钮的拖动
                    mMode = MODE_DRAG;
                    mStartPoint.set(e.getX(), e.getY());  //相对坐标
                } else curButton = 0;  //停止悬停按钮的拖动
            }

            public void onLongPress(MotionEvent e) {
                if (m_lShowAnsInf) m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                //触发长按
                if (m_Game.bt_Sel.isChecked()) return;  //计数状态，直接返回

                m_Game.m_bNetLock = false;  //取消网型提示
                m_bManTo = false;  //关闭人的可达点状态
                m_bManTo2 = false;
                m_bBoxTo = false;  //关闭箱子可达位置提示状态状态
                m_bBoxTo2 = false;
                m_boxNoMoved = false;  //关闭未动箱子提示状态
                m_boxNoUsed = false;  //关闭未使用地板提示状态
                m_boxCanCome = false;  //是否在提示可推过来的箱子状态
                m_boxCanCome2 = false;
                m_boxCanMove = false;  //关闭可动箱子提示状态
                m_boxCanMove2 = false;
                m_Game.m_bACT_ERROR = false;  //执行动作时是否遇到错误
                if (m_nArenaTop <= 0 && m_rUnDo.contains((int) e.getX(), (int) e.getY())) {  //长按 undo
                    m_Game.m_bNetLock = false;  //取消网型提示
                    m_Game.DoEvent(3);  //undo
                } else if (m_nArenaTop <= 0 && m_rReDo.contains((int) e.getX(), (int) e.getY())) {  //长按redo
                    m_Game.m_bNetLock = false;  //取消网型提示
                    m_Game.DoEvent(4);  //redo
                } else if (m_rTrans.contains((int) e.getX(), (int) e.getY())) {
                    m_Game.DoEvent(12);  //“奇偶格位显示”开关
                } else if (m_rProgress_Bar.contains((int) e.getX(), (int) e.getY())) {  //即景模式转换
                    if (m_Game.mMicroTask != null) return;
                    m_Game.m_nStep = 0;
                    m_Game.m_bYanshi = false;
                    m_Game.m_bYanshi2 = false;
                    m_Game.DoEvent(0);   //即景模式转换
                } else if (m_rNext_Speed.contains((int) e.getX(), (int) e.getY())) {  //“自动箱子编号”开关
                    myMaps.m_bBianhao = !myMaps.m_bBianhao;
                } else if (m_rChangeBK.contains((int) e.getX(), (int) e.getY())) {  //“标尺”开关
                    myMaps.m_bBiaochi = !myMaps.m_bBiaochi;
                } else if (!m_Game.bt_BK.isChecked() && (mClickObj == '*' || myMaps.m_Sets[3] == 1 && mClickObj == '.')) {  //长按点位上箱子，显示关联网点、网口
                    m_Game.m_Net_Inf(m_Game.m_cArray, m_iR, m_iC);  //计算网点、网口
                    m_Game.m_bNetLock = true;
                } else  if (mClickObj == '$') {  //长按非点位上的箱子，显示哪些箱子可以被推动
                    if (m_Game.mMicroTask != null) return;
                    if (m_Game.bt_BK.isChecked() && m_Game.m_nRow0 < 0) return;  //逆推的仓管员尚未定位时，不计算
                    if (m_Game.bt_BK.isChecked()) {
                        m_Game.mPathfinder.manReachable(true, m_Game.bk_cArray, m_Game.m_nRow2, m_Game.m_nCol2);
                    } else {
                        m_Game.mPathfinder.manReachable(false, m_Game.m_cArray, m_Game.m_nRow, m_Game.m_nCol);
                    }
                    m_bManTo = false;  //关闭人的可达点状态
                    m_bManTo2 = false;
                    m_Game.boxCanMove();
                } else if (mClickObj == '-' || mClickObj == '.' && (myMaps.m_Sets[3] == 0 || m_Game.bt_BK.isChecked())) {  //长按空地，显示哪些箱子可以到这里
                    if (m_Game.mMicroTask != null) return;
                    m_iR2 = m_iR;
                    m_iC2 = m_iC;
                    m_Game.boxCanCome(m_iR, m_iC);
                    m_bManTo = false;  //关闭人的可达点状态
                    m_bManTo2 = false;
                    m_boxCanMove = false;  //关闭可动箱子提示状态
                    m_boxCanMove2 = false;
                } else if (mClickObj == '@' || mClickObj == '+') {  //长按仓管员，正推，选择导出 GIF 的开始点；逆推，切换“禁用逆推目标点”选项开关
                    if (m_Game.bt_BK.isChecked()) {
                        if (myMaps.m_Sets[32] == 1) {
                            myMaps.m_Sets[32] = 0;
                            MyToast.showToast(m_Game, "启用逆推目标点！", Toast.LENGTH_SHORT);
                        } else {
                            myMaps.m_Sets[32] = 1;
                            MyToast.showToast(m_Game, "使用正推目标点！", Toast.LENGTH_SHORT);
                        }
                    } else {
                        if (m_Game.m_iStep[1] == 0) {  // 在关卡初态，即尚未任何动作
                            if (myMaps.m_Sets[37] == 1) {
                                myMaps.m_Sets[37] = 0;
                                MyToast.showToast(m_Game, "自动加载最新状态 -- 关！", Toast.LENGTH_SHORT);
                            } else {
                                myMaps.m_Sets[37] = 1;
                                MyToast.showToast(m_Game, "自动加载最新状态 -- 开！", Toast.LENGTH_SHORT);
                            }
                        } else {
                            if (m_Game.m_Gif_Start == m_Game.m_iStep[1]) m_Game.m_Gif_Start = 0;
                            else m_Game.m_Gif_Start = m_Game.m_iStep[1];
                        }
                    }
                } else if (mClickObj == '#') {  //长按墙壁，显示哪些箱子没有动过
                    if (m_Game.mMicroTask != null) return;
                    if (!m_Game.bt_Sel.isChecked()) {
                        if (m_Game.bt_BK.isChecked()) {  //是否做逆推的双目标点提示
                            if (myMaps.m_Sets[32] == 1) m_lShowDst2 = !m_lShowDst2;
                        } else if (myMaps.curMap.Num > 0 && myMaps.mState2.size() > 0 && m_Game.m_iStep[1] == 0) {
                            m_lShowAnsInf = !m_lShowAnsInf;  //是否允许在开始推关卡之前显示答案信息
                            invalidate();
                        } else m_Game.boxNoMoved();
                    }
                }
                invalidate();
            }

            public boolean onDoubleTap(MotionEvent e) {
                if (m_lShowAnsInf) m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_Game.m_bNetLock = false;  //取消网型提示
                m_Game.m_bACT_ERROR = false;  //执行动作时是否遇到错误
                //当手指快速第二次按下触发,此时必须是单指模式才允许执行doubleTap
                if (mMode == MODE_DRAG &&
                        Math.abs(e.getRawX() - mClickPoint.x) < 50f &&  //双击范围控制
                        Math.abs(e.getRawY() - mClickPoint.y) < 50f) {

                    if (e.getRawY() < m_nArenaTop || m_lChangeBK || m_nArenaTop <= 0 && (m_rUnDo.contains((int) e.getX(), (int) e.getY()) || m_rReDo.contains((int) e.getX(), (int) e.getY()))) {
                        return true;
                    }

                    boolean flg;
                    if (mClickObj == '$' || mClickObj == '*') {  //箱子编号
                        if (myMaps.m_bBianhao) {  //自动箱子编号状态打开时，不使用人工箱子编号
                        } else {  //箱子使用人工编号
                            if (m_Game.m_iBoxNum[m_iR][m_iC] < 0) {
                                for (short k = 0; k < 286; k++) {
                                    flg = false;
                                    for (int i = 0; i < myMaps.curMap.Rows; i++) {
                                        for (int j = 0; j < myMaps.curMap.Cols; j++) {
                                            if (m_Game.m_iBoxNum[i][j] == k) {
                                                flg = true;
                                                break;
                                            }
                                        }
                                        if (flg) break;
                                    }
                                    if (!flg) {
                                        m_Game.m_iBoxNum[m_iR][m_iC] = k;
                                        break;
                                    }
                                }
                            } else {
                                m_Game.m_iBoxNum[m_iR][m_iC] = -1;
                            }
                        }
                    } else if (myMaps.m_bBiaochi) {  //在标尺显示状态，双击墙壁、空地、仓管员或墙外
                        int[] RC = get_iR_iC((int)e.getX(), (int)e.getY());
                        if (RC[1] >= 0 && RC[1] <= m_Game.m_cArray.length && RC[0] >= 0 && RC[0] < m_Game.m_cArray[0].length) {
                            m_Game.mark44[RC[1]][RC[0]] = !m_Game.mark44[RC[1]][RC[0]];  //切换标尺显示标志
                        }
                    } else if (mClickObj == '#' || mClickObj == '_') {  //双击箱子、墙、墙外
                        if (myMaps.m_Sets[20] == 0) m_Game.setMenuVisible();  //全屏
                    } else if (mClickObj == '@' || mClickObj == '+') {  //双击仓管员
                        if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200) {  //简单皮肤，横竖屏切换
                            myMaps.isHengping = !myMaps.isHengping;
                        }
                    }
                }
                return true;
            }

//			public boolean onSingleTapConfirmed(MotionEvent e) {
//				return true;
//			}

            public boolean onSingleTapUp(MotionEvent e) {
                if (m_lShowAnsInf) m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_Game.m_bACT_ERROR = false;  //执行动作时是否遇到错误
                if (m_nArenaTop > 0 && m_rChangeBK.contains((int) e.getX(), (int) e.getY())) {  //切换更换背景皮肤模式
                    m_lChangeBK = !m_lChangeBK;
                    return true;
                } else if (myMaps.isRecording && m_rRecording.contains((int) e.getX(), (int) e.getY())) {  //录制模式时
                    m_Game.DoEvent(9);   //关闭录制模式，将录制的动作送入剪切板，并打开“导入”窗口
                    return true;
                } else if (m_rTrans.contains((int) e.getX(), (int) e.getY())) {  //宏调试（单步执行）时，结束调试，否则加载并执行宏
                    m_Game.DoEvent(10);  //加载并执行宏
                    return true;
                } else if (m_rProgress_Bar.contains((int) e.getX(), (int) e.getY())) {   //进度条
                    if (m_Game.mMicroTask == null) m_Game.DoEvent(11);
                    return true;
                } else if (m_rNext_Speed.contains((int) e.getX(), (int) e.getY())) {  //逐级加速
                    if (myMaps.m_Sets[10] > 0) myMaps.m_Sets[10]--;
                    else myMaps.m_Sets[10] = 4;
                    MyToast.showToast(myMaps.ctxDealFile, "速度：" + m_Game.m_sSleep[myMaps.m_Sets[10]], Toast.LENGTH_SHORT);
                    return true;
                } else if (m_rPre.contains((int) e.getX(), (int) e.getY())) {
                    m_Game.DoEvent(7);   //上一关
                    return true;
                } else if (m_rNext.contains((int) e.getX(), (int) e.getY())) {
                    m_Game.DoEvent(8);   //下一关
                    return true;
                } else if (m_nArenaTop <= 0 && m_rUnDo.contains((int) e.getX(), (int) e.getY())) {
                    m_Game.DoEvent(5);  //undo
                    return true;
                } else if (m_nArenaTop <= 0 && m_rReDo.contains((int) e.getX(), (int) e.getY())) {
                    m_Game.DoEvent(6);  //redo
                    return true;
                } else if (m_lChangeBK && m_rPre_BK.contains((int) e.getX(), (int) e.getY())) {  //上一背景
                    if (myMaps.mFile_List2.size() > 0) {
                        int m_nPic = 0;
                        for (int k = 0; k < myMaps.mFile_List2.size(); k++) {
                            if (myMaps.mFile_List2.get(k).equals(myMaps.bk_Pic)) {
                                m_nPic = k;
                                break;
                            }
                        }
                        m_nPic--;
                        if (m_nPic < 0) m_nPic = myMaps.mFile_List2.size()-1;
                        myMaps.bk_Pic = myMaps.mFile_List2.get(m_nPic);  //选择的文档
                        myMaps.loadBKPic();

                        //计算（舞台用）背景图片的尺寸，以及平铺时的水平、垂直数量
                        if (m_nPic > 0 && myMaps.bkPict != null) {
                            w_bkPic = myMaps.bkPict.getWidth();
                            h_bkPic = myMaps.bkPict.getHeight();
                            w_bkNum = myMaps.m_nWinWidth / w_bkPic + 1;
                            h_bkNum = myMaps.m_nWinHeight / h_bkPic + 1;
                        }
                    }
                    return true;
                } else if (m_lChangeBK && m_rNext_BK.contains((int) e.getX(), (int) e.getY())) {  //下一背景
                    if (myMaps.mFile_List2.size() > 0) {
                        int m_nPic = 0;
                        for (int k = 0; k < myMaps.mFile_List2.size(); k++) {
                            if (myMaps.mFile_List2.get(k).equals(myMaps.bk_Pic)) {
                                m_nPic = k;
                                break;
                            }
                        }
                        m_nPic++;
                        m_nPic = m_nPic % myMaps.mFile_List2.size();
                        myMaps.bk_Pic = myMaps.mFile_List2.get(m_nPic);  //选择的文档
                        myMaps.loadBKPic();

                        //计算（舞台用）背景图片的尺寸，以及平铺时的水平、垂直数量
                        if (m_nPic > 0 && myMaps.bkPict != null) {
                            w_bkPic = myMaps.bkPict.getWidth();
                            h_bkPic = myMaps.bkPict.getHeight();
                            w_bkNum = myMaps.m_nWinWidth / w_bkPic + 1;
                            h_bkNum = myMaps.m_nWinHeight / h_bkPic + 1;
                        }
                    }
                    return true;
                } else if (m_lChangeBK && m_rColor_BK.contains((int) e.getX(), (int) e.getY())) {  //背景色
                    m_Game.setColorBK();
                    invalidate();
                    return true;
                } else if (m_lChangeBK && m_rPre_Skin.contains((int)e.getX(), (int)e.getY())) {  //上一个皮肤
                    int m_nSkin = -1;
                    if (myMaps.mFile_List1.size() > 0) {
                        for (int k = 0; k < myMaps.mFile_List1.size(); k++) {
                            if (myMaps.mFile_List1.get(k).equals(myMaps.skin_File)) {
                                m_nSkin = k;
                                break;
                            }
                        }
                        m_nSkin--;
                        if (m_nSkin < 0) m_nSkin = myMaps.mFile_List1.size()-1;

                        myMaps.skin_File = myMaps.mFile_List1.get(m_nSkin);  //皮肤文档

                        MyToast.showToast(myMaps.ctxDealFile, myMaps.skin_File, Toast.LENGTH_SHORT);

                        myMaps.loadSkins();
                        invalidate();
                        myMaps.iskinChange = true;
                    }
                    return true;
                } if (m_lChangeBK && m_rNext_Skin.contains((int)e.getX(), (int)e.getY())) {  //下一个皮肤
                    int m_nSkin = -1;
                    if (myMaps.mFile_List1.size() > 0) {
                        for (int k = 0; k < myMaps.mFile_List1.size(); k++) {
                            if (myMaps.mFile_List1.get(k).equals(myMaps.skin_File)) {
                                m_nSkin = k;
                                break;
                            }
                        }
                        m_nSkin++;
                        m_nSkin = m_nSkin % myMaps.mFile_List1.size();

                        myMaps.skin_File = myMaps.mFile_List1.get(m_nSkin);  //皮肤文档

                        MyToast.showToast(myMaps.ctxDealFile, myMaps.skin_File, Toast.LENGTH_SHORT);

                        myMaps.loadSkins();
                        invalidate();
                        myMaps.iskinChange = true;
                    }
                    return true;
                }

                if (m_Game.mMicroTask == null) {
                    m_lChangeBK = false;
                    if (mMode == MODE_NONE)
                        doACT((int) e.getX(), (int) e.getY(), true);
                }
                return true;
            }

        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {  //onTouchEvent
            if ((int) event.getY() >= m_nArenaTop && myMaps.curMap.Title.equals("无效关卡")) {  // 无效关卡时，不执行任何命令
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: //触发点击，记录被点击的物件（箱子、墙壁、人等）
                    doACT((int) event.getX(), (int) event.getY(), false);
                    mMode = MODE_DRAG;
                    mStartPoint.set(event.getX(), event.getY());  //相对坐标
                    mClickPoint.set(event.getRawX(), event.getRawY());  //触点的屏幕坐标，控制双击的范围和单击的抖动时使用

                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    curButton = 0;
                    reSetMatrix();
                    invalidate();
                    mMode = MODE_NONE;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mMode == MODE_ZOOM) setZoomMatrix(event);
                    else if (mMode == MODE_DRAG) {
                        if (curButton == 1) setDrag1(event);
                        else if (curButton == 2) setDrag2(event);
                        else setDragMatrix(event);
                    }
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

        //设置undo按钮的拖拽状
        public void setDrag1(MotionEvent event) {
            //计算移动距离
            int dx = (int)(event.getX() - mStartPoint.x);
            int dy = (int)(event.getY() - mStartPoint.y);

            mStartPoint.set(event.getX(), event.getY());

            //在当前基础上移动
            int l = m_rUnDo.left+dx;
            int t = m_rUnDo.top+dy;
            int r = m_rUnDo.right+dx;
            int b = m_rUnDo.bottom+dy;
            if (l > 0 && t > 0 && r < getWidth() && b < getHeight())
                m_rUnDo.set(l, t, r, b);

            invalidate();
        }

        //设置redo按钮的拖拽状
        public void setDrag2(MotionEvent event) {
            //计算移动距离
            int dx = (int)(event.getX() - mStartPoint.x);
            int dy = (int)(event.getY() - mStartPoint.y);

            mStartPoint.set(event.getX(), event.getY());

            //在当前基础上移动
            int l = m_rReDo.left+dx;
            int t = m_rReDo.top+dy;
            int r = m_rReDo.right+dx;
            int b = m_rReDo.bottom+dy;

            if (l > 0 && t > 0 && r < getWidth() && b < getHeight())
                m_rReDo.set(l, t, r, b);

            invalidate();
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

        //计算两指间的距离
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
            m_lWallTop = false;
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
        if (c > 0 && m_Game.m_cArray[r][c - 1] == '#') bz |= dir[myMaps.m_nTrun][0]; //左
        if (r > 0 && m_Game.m_cArray[r - 1][c] == '#') bz |= dir[myMaps.m_nTrun][1]; //上
        if (c < m_Game.m_cArray[0].length - 1 && m_Game.m_cArray[r][c + 1] == '#') bz |= dir[myMaps.m_nTrun][2]; //右
        if (r < m_Game.m_cArray.length - 1 && m_Game.m_cArray[r + 1][c] == '#') bz |= dir[myMaps.m_nTrun][3]; //下

        m_lWallTop = ((bz == dir[myMaps.m_nTrun][4] ||
                bz == dir[myMaps.m_nTrun][5] ||
                bz == dir[myMaps.m_nTrun][6] || bz == 15)
                && c > 0 && r > 0 && m_Game.m_cArray[r - 1][c - 1] == '#'); //是否需要画墙顶

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

    //移动动画的位置计算
    boolean box_Goal[] = {false, false};  //箱子移动前后位置是否为目标点
    void getRect(int i, int j, Rect rt) {
        char[][] mArray;

        if (m_Game.bt_BK.isChecked()) mArray = m_Game.bk_cArray;
        else mArray = m_Game.m_cArray;

        //根据箱子的移动位置，计算所用的图片（是否使用在目标点上的图片）
        switch (myMaps.m_Sets[14]) {
            case 1:
                if (m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] > 0 ||
                    !m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] < 0) {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i][j+1] == '.') box_Goal[0] = true;
                    else box_Goal[0] = false;
                } else {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i][j+1] == '+') box_Goal[0] = true;
                    else box_Goal[0] = false;
                }
                break;
            case 2:
                if (m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] > 0 ||
                    !m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] < 0) {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i+1][j] == '.') box_Goal[0] = true;
                    else box_Goal[0] = false;
                } else {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i+1][j] == '+') box_Goal[0] = true;
                    else box_Goal[0] = false;
                }
                break;
            case 3:
                if (m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] > 0 ||
                    !m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] < 0) {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i][j-1] == '.') box_Goal[0] = true;
                    else box_Goal[0] = false;
                } else {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i][j-1] == '+') box_Goal[0] = true;
                    else box_Goal[0] = false;
                }
                break;
            case 4:
                if (m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] > 0 ||
                    !m_Game.bt_BK.isChecked() && myMaps.m_Sets[18] < 0) {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i-1][j] == '.') box_Goal[0] = true;
                    else box_Goal[0] = false;
                } else {
                    if (mArray[i][j] == '*') box_Goal[1] = true;
                    else box_Goal[1] = false;
                    if (mArray[i-1][j] == '+') box_Goal[0] = true;
                    else box_Goal[0] = false;
                }
                break;
        }

        //动画移动的理论方向与距离
        switch (myMaps.m_Sets[14]) {
            case 1:
                d_M_Row = 0;
                if (d_Moves <= 0) d_M_Col = m_PicWidth;
                else if (d_Moves < m_PicWidth) d_M_Col = m_PicWidth - d_Moves;
                else d_M_Col = 0;
                break;
            case 2:
                d_M_Col = 0;
                if (d_Moves <= 0) d_M_Row = m_PicWidth;
                else if (d_Moves < m_PicWidth) d_M_Row = m_PicWidth - d_Moves;
                else d_M_Col = 0;
                break;
            case 3:
                d_M_Row = 0;
                if (d_Moves <= 0) d_M_Col = -m_PicWidth;
                else if (d_Moves < m_PicWidth) d_M_Col = d_Moves - m_PicWidth;
                else d_M_Col = 0;
                break;
            default:
                d_M_Col = 0;
                if (d_Moves <= 0) d_M_Row = -m_PicWidth;
                else if (d_Moves < m_PicWidth) d_M_Row = d_Moves - m_PicWidth;
                else d_M_Col = 0;
                break;
        }
        //根据关卡的第 n 转，计算实际移动到的位置
        switch (myMaps.m_nTrun) {
            case 0:
                rt.left = m_PicWidth * j + d_M_Col;
                rt.top = m_PicWidth * i + d_M_Row;
                break;
            case 1:
                rt.left = m_PicWidth * (m_nRows - 1 - i) - d_M_Row;
                rt.top = m_PicWidth * j + d_M_Col;
                break;
            case 2:
                rt.left = m_PicWidth * (m_nCols - 1 - j) - d_M_Col;
                rt.top = m_PicWidth * (m_nRows - 1 - i) - d_M_Row;
                break;
            case 3:
                rt.left = m_PicWidth * i + d_M_Row;
                rt.top = m_PicWidth * (m_nCols - 1 - j) - d_M_Col;
                break;
            case 4:
                rt.left = m_PicWidth * (m_nCols - 1 - j) - d_M_Col;
                rt.top = m_PicWidth * i + d_M_Row;
                break;
            case 5:
                rt.left = m_PicWidth * (m_nRows - 1 - i) - d_M_Row;
                rt.top = m_PicWidth * (m_nCols - 1 - j) - d_M_Col;
                break;
            case 6:
                rt.left = m_PicWidth * j + d_M_Col;
                rt.top = m_PicWidth * (m_nRows - 1 - i) - d_M_Row;
                break;
            case 7:
                rt.left = m_PicWidth * i + d_M_Row;
                rt.top = m_PicWidth * j + d_M_Col;
        }
        rt.right = rt.left + m_PicWidth;
        rt.bottom = rt.top + m_PicWidth;
    }

    //绘制游戏画面
    Rect rt = new Rect();
    Rect rt1 = new Rect();
    Rect rt2 = new Rect();
    Rect rt3 = new Rect();
    Rect rt4 = new Rect();
    Rect rt5 = new Rect();
    Rect rt6 = new Rect();
    Rect rt7 = new Rect();
    Rect rt8 = new Rect();
    char ch;
    int ww, hh, ss, cur;
    String mStr;
    int d_M_Row, d_M_Col;

    byte[][] PlayDir = { //仓管员4方向对关卡8方位旋转之转换算数组
            {0, 1, 2, 3, 4},  //0 转之左、上、右、下
            {0, 2, 3, 4, 1},
            {0, 3, 4, 1, 2},
            {0, 4, 1, 2, 3},
            {0, 3, 2, 1, 4},
            {0, 4, 3, 2, 1},
            {0, 1, 4, 3, 2},
            {0, 2, 1, 4, 3}};

//    String[] myWeek = {
//            "","日","一","二","三","四","五","六"
//    };

    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (myMaps.curMap == null) return;

        // 显示背景色或背景图片
        if (myMaps.bk_Pic == null || myMaps.bk_Pic.length() <= 0 || myMaps.bk_Pic.equals("使用背景色")) {
            setBackgroundColor(myMaps.m_Sets[4]);  //设置背景色
        } else {
            if (myMaps.bkPict != null) {
                for (int i = 0; i <= w_bkNum; i++) {
                    for (int j = 0; j <= h_bkNum; j++)
                        canvas.drawBitmap(myMaps.bkPict, w_bkPic * i, h_bkPic * j, null);
                }
            }
        }

        // 在背景上显示当前时间
//        Calendar cal = Calendar.getInstance();
//        cal.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
//
//        String week = myWeek[cal.get(Calendar.DAY_OF_WEEK)];
//        String hour;
//        if (cal.get(Calendar.AM_PM) == 0)
//            hour = String.valueOf(cal.get(Calendar.HOUR));
//        else
//            hour = String.valueOf(cal.get(Calendar.HOUR)+12);
//        String minute = String.valueOf(cal.get(Calendar.MINUTE));
//        String second = String.valueOf(cal.get(Calendar.SECOND));
//
//        String my_time = hour + ":" + minute + ":" + second + " 周" + week;
//
//        myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
//        myPaint.setTextSize(30);
//        myPaint.setStrokeWidth(3);
//        myPaint.setARGB(255, 255, 255, 255);
//        canvas.drawText(my_time, 10, 30, myPaint);
//        myPaint.setStrokeWidth(1);
//        myPaint.setARGB(255, 0, 0, 0);
//        canvas.drawText(my_time, 10, 30, myPaint);

        // 显示地图
        canvas.save();
        mCurrentMatrix.getValues(values);
        values[Matrix.MTRANS_Y] += m_nArenaTop;
        mMapMatrix.setValues(values);
        m_fTop = values[Matrix.MTRANS_Y];
        m_fLeft = values[Matrix.MTRANS_X];
        m_fScale = values[Matrix.MSCALE_X];
        canvas.setMatrix(mMapMatrix);

        for (int i = 0; i < m_nRows; i++) {
            for (int j = 0; j < m_nCols; j++) {
                switch (myMaps.m_nTrun) {
                    case 0:
                        rt.left = m_PicWidth * j;
                        rt.top = m_PicWidth * i;
                        break;
                    case 1:
                        rt.left = m_PicWidth * (m_nRows - 1 - i);
                        rt.top = m_PicWidth * j;
                        break;
                    case 2:
                        rt.left = m_PicWidth * (m_nCols - 1 - j);
                        rt.top = m_PicWidth * (m_nRows - 1 - i);
                        break;
                    case 3:
                        rt.left = m_PicWidth * i;
                        rt.top = m_PicWidth * (m_nCols - 1 - j);
                        break;
                    case 4:
                        rt.left = m_PicWidth * (m_nCols - 1 - j);
                        rt.top = m_PicWidth * i;
                        break;
                    case 5:
                        rt.left = m_PicWidth * (m_nRows - 1 - i);
                        rt.top = m_PicWidth * (m_nCols - 1 - j);
                        break;
                    case 6:
                        rt.left = m_PicWidth * j;
                        rt.top = m_PicWidth * (m_nRows - 1 - i);
                        break;
                    case 7:
                        rt.left = m_PicWidth * i;
                        rt.top = m_PicWidth * j;
                }

                rt.right = rt.left + m_PicWidth;
                rt.bottom = rt.top + m_PicWidth;

                if (myMaps.m_Sets[13] == 1) {  //即景模式
                    if (m_Game.bt_BK.isChecked()) {
                        ch = m_Game.bk_cArray[i][j];  //逆推迷宫
                        if (m_Game.m_cArray[i][j] == '$' || m_Game.m_cArray[i][j] == '*'){
                            switch (ch) {
                                case '-':
                                    ch = '.';
                                    break;
                                case '$':
                                    ch = '*';
                                    break;
                                case '@':
                                    ch = '+';
                            }
                        } else {
                            switch (ch) {
                                case '.':
                                    ch = '-';
                                    break;
                                case '*':
                                    ch = '$';
                                    break;
                                case '+':
                                    ch = '@';
                            }
                        }
                    } else {
                        ch = m_Game.m_cArray[i][j];  //正推迷宫
                        if (m_Game.bk_cArray[i][j] == '$' || m_Game.bk_cArray[i][j] == '*'){
                            switch (ch) {
                                case '-':
                                    ch = '.';
                                    break;
                                case '$':
                                    ch = '*';
                                    break;
                                case '@':
                                    ch = '+';
                            }
                        } else {
                            switch (ch) {
                                case '.':
                                    ch = '-';
                                    break;
                                case '*':
                                    ch = '$';
                                    break;
                                case '+':
                                    ch = '@';
                            }
                        }
                    }
                } else {   //非即景模式
                    if (m_Game.bt_BK.isChecked()) {
                        ch = m_Game.bk_cArray[i][j];  //逆推迷宫
                        if (myMaps.m_Sets[32] == 1) {  //逆推时使用正推的目标点
                            switch (m_Game.m_cArray[i][j]) {
                                case '.':
                                case '*':
                                case '+':
                                    switch (ch) {
                                        case '-':
                                            ch = '.';
                                            break;
                                        case '$':
                                            ch = '*';
                                            break;
                                        case '@':
                                            ch = '+';
                                    }
                                    break;
                                case '-':
                                case '$':
                                case '@':
                                    switch (ch) {
                                        case '.':
                                            ch = '-';
                                            break;
                                        case '*':
                                            ch = '$';
                                            break;
                                        case '+':
                                            ch = '@';
                                    }
                            }
                        }
                    } else {
                        ch = m_Game.m_cArray[i][j];   //正推迷宫
                    }
                }

                myPaint.setARGB(255, 0, 0, 0);
                //第一、二层显示————地板、逆推水印
                if (ch != '_' && ch != '#') {
                    rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);  //地板————第一层
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                    if (myMaps.m_Sets[38] == 1) {  // 区分奇偶格位显示
                        if ((i + j) % 2 == 1) {    // 奇格位
                            myPaint.setARGB(myMaps.m_Sets[40] * 5, 0, 0, 0);
                        } else {                   // 偶格位
                            myPaint.setARGB(myMaps.m_Sets[39] * 5, 255, 255, 255);
                        }
                        myPaint.setStyle(Paint.Style.FILL);
                        canvas.drawRect(rt, myPaint);
                    }
                    myPaint.setARGB(255, 0, 0, 0);
                    if (m_Game.bt_BK.isChecked()) {  //逆推水印————第二层
                        rt1.set(50, 200-myMaps.isSkin_200, 100, 250-myMaps.isSkin_200);
                        canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                    }
                    if (ch == '-' && myMaps.curMap.Title.equals("无效关卡")) {
                        canvas.drawBitmap(bitInvalid, rt.left, rt.top, myPaint);
                        continue;
                    }
                }
                //第三层显示————目标点
                if (ch == '.' || ch == '*' || ch == '+') {
                    rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                }

                //第四层显示————箱子或人
                switch (ch) {
                    case '#':   //墙壁属于第一层显示
                        if (myMaps.m_bBiaochi) { //标尺开关
                            rt1.set(0, 0, 50, 50);
                            canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                            myPaint.setARGB(127, 63, 63, 63);
                        } else {
                            rt1 = getWall(rt1, i, j);  //计算使用哪个“墙”图
                            if (myMaps.isSkin_200 == 200 && myMaps.isHengping) {
                                canvas.scale(-1, 1, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                                canvas.rotate(-90, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                                canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);  //new Rect(rt.right, rt.top, rt.left, rt.bottom)
                                canvas.rotate(90, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                                canvas.scale(-1, 1, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                            } else canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                            if (m_lWallTop && !m_Game.bt_Sel.isChecked()) { //是否画墙顶
                                rt2.set(0, 200, 50, 250);
                                switch (myMaps.m_nTrun) {
                                    case 0:
                                        rt3.set(rt.left - m_PicWidth / 2, rt.top - m_PicWidth / 2, rt.right - m_PicWidth / 2, rt.bottom - m_PicWidth / 2);
                                        break;
                                    case 1:
                                        rt3.set(rt.left + m_PicWidth / 2, rt.top - m_PicWidth / 2, rt.right + m_PicWidth / 2, rt.bottom - m_PicWidth / 2);
                                        break;
                                    case 2:
                                        rt3.set(rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2, rt.right + m_PicWidth / 2, rt.bottom + m_PicWidth / 2);
                                        break;
                                    case 3:
                                        rt3.set(rt.left - m_PicWidth / 2, rt.top + m_PicWidth / 2, rt.right - m_PicWidth / 2, rt.bottom + m_PicWidth / 2);
                                        break;
                                    case 4:
                                        rt3.set(rt.left + m_PicWidth / 2, rt.top - m_PicWidth / 2, rt.right + m_PicWidth / 2, rt.bottom - m_PicWidth / 2);
                                        break;
                                    case 5:
                                        rt3.set(rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2, rt.right + m_PicWidth / 2, rt.bottom + m_PicWidth / 2);
                                        break;
                                    case 6:
                                        rt3.set(rt.left - m_PicWidth / 2, rt.top + m_PicWidth / 2, rt.right - m_PicWidth / 2, rt.bottom + m_PicWidth / 2);
                                        break;
                                    case 7:
                                        rt3.set(rt.left - m_PicWidth / 2, rt.top - m_PicWidth / 2, rt.right - m_PicWidth / 2, rt.bottom - m_PicWidth / 2);
                                }
                                canvas.drawBitmap(myMaps.skinBit, rt2, rt3, myPaint);
                            }
                        }
                        break;
                    case '$':
                        if (d_Moves >= m_PicWidth || m_Game.b_nRow != i || m_Game.b_nCol != j) {
                            rt1.set(50, 250-myMaps.isSkin_200, 100, 300-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                            if (myMaps.m_bBianhao) {  //箱子使用自动编号
                                boxNum(canvas, rt, m_Game.m_iBoxNum2[i][j]);
                            } else {
                                boxNum(canvas, rt, m_Game.m_iBoxNum[i][j]);
                            }
                        }
                        break;
                    case '*':
                        if (d_Moves >= m_PicWidth || m_Game.b_nRow != i || m_Game.b_nCol != j) {
                            rt1.set(50, 300-myMaps.isSkin_200, 100, 350-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                            if (myMaps.m_bBianhao) {  //箱子使用自动编号
                                boxNum(canvas, rt, m_Game.m_iBoxNum2[i][j]);
                            } else {
                                boxNum(canvas, rt, m_Game.m_iBoxNum[i][j]);
                            }
                        }
                        break;
                    case '@':
                        if (d_Moves >= m_PicWidth) {
                            if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200) {  //简单皮肤
                                if (myMaps.isHengping) {
                                    rt1.set(150, 250-myMaps.isSkin_200, 200, 300-myMaps.isSkin_200);  //使用横屏皮肤
                                }
                                else {
                                    rt1.set(100, 250-myMaps.isSkin_200, 150, 300-myMaps.isSkin_200);  //不使用横屏皮肤
                                }
                            } else {  //标准皮肤
                                switch (PlayDir[myMaps.m_nTrun][myMaps.m_Sets[5]]) {
                                    case 1:  //左
                                        rt1.set(0, 350, 50, 400);
                                        break;
                                    case 3:  //右
                                        rt1.set(150, 250, 200, 300);
                                        break;
                                    case 4:  //下
                                        rt1.set(100, 350, 150, 400);
                                        break;
                                    default:  //上
                                        rt1.set(100, 250, 150, 300);
                                        break;
                                }
                            }
                            canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        }
                        break;
                    case '+':
                        if (d_Moves >= m_PicWidth) {
                            if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200) {  //简单皮肤
                                if (myMaps.isHengping) {
                                    rt1.set(150, 300-myMaps.isSkin_200, 200, 350-myMaps.isSkin_200);  //使用横屏皮肤
                                } else {
                                    rt1.set(100, 300-myMaps.isSkin_200, 150, 350-myMaps.isSkin_200);  //不使用横屏皮肤
                                }
                            } else {  //标准皮肤
                                switch (PlayDir[myMaps.m_nTrun][myMaps.m_Sets[5]]) {
                                    case 1:  //左
                                        rt1.set(50, 350, 100, 400);
                                        break;
                                    case 3:  //右
                                        rt1.set(150, 300, 200, 350);
                                        break;
                                    case 4:  //下
                                        rt1.set(150, 350, 200, 400);
                                        break;
                                    default:  //上
                                        rt1.set(100, 300, 150, 350);
                                        break;
                                }
                            }
                            canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        }
                        break;
                } //end switch

                if (myMaps.m_bBiaochi && (i == 0 || j == 0 || i == m_nRows-1 || j == m_nCols-1 || m_Game.mark44[i][j] && (!m_Game.bt_BK.isChecked() && m_Game.m_cArray[i][j] != '$' && m_Game.m_cArray[i][j] != '*'|| m_Game.bt_BK.isChecked() && m_Game.bk_cArray[i][j] != '$' && m_Game.bk_cArray[i][j] != '*'))) {   //标尺开关
                    if (i == 0 || j == 0 || i == m_nRows-1 || j == m_nCols-1) {
                        myPaint.setTextSize(m_PicWidth / 4);
                    } else {
                        myPaint.setTextSize(m_PicWidth / 2);
                    }
                    mStr = mGetCur(i, j);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //标尺文字框
                    myPaint.setARGB(255, 0, 0, 0);
                    myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    myPaint.setStrokeWidth(3);
                    canvas.drawText(mStr, rt.left + (m_PicWidth - rt8.width()) / 2, rt.bottom - (m_PicWidth - rt8.height()) / 2, myPaint);
                    myPaint.setARGB(255, 255, 255, 255);
                    myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    myPaint.setStrokeWidth(1);
                    canvas.drawText(mStr, rt.left + (m_PicWidth - rt8.width()) / 2, rt.bottom - (m_PicWidth - rt8.height()) / 2, myPaint);
                }

                myPaint.setARGB(255, 0, 0, 0);

                //第五层显示————各类提示、标示等
                if (m_Game.bt_BK.isChecked()) {  //逆推
                    if (m_bBoxTo2 && m_Game.mPathfinder.mark4[i][j]) { //箱子可达位置提示
                        if ((myMaps.m_Sets[8] == 1 || (m_iR == i && m_iC == j)) && (myMaps.m_Sets[24] == 0 || m_Game.mark14[i][j] > 0)) {
                            rt1.set(150, 200-myMaps.isSkin_200, 175, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                    }
                    if (myMaps.m_Sets[8] == 1) {  //显示可达位置提示
                        if (m_boxCanMove2 && m_Game.mark12[i][j] || m_boxCanCome2 && m_Game.mark12[i][j]) { //可动/来箱子提示
                           rt1.set(150, 200-myMaps.isSkin_200, 175, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                        if (m_bManTo2 && m_Game.mPathfinder.mark2[i][j]) { //人可达位置提示
                            rt1.set(150, 200-myMaps.isSkin_200, 175, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                        if ((m_boxCanMove2 || m_bManTo2 || m_bBoxTo2) && m_Game.mPathfinder.mark6[i][j]) { //穿越点提示
                            rt1.set(175, 200-myMaps.isSkin_200, 200, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                    }
                    if (m_Game.m_bNetLock) {
                        if (m_Game.mark41[i][j] == 1) {  //网锁位
                            rt1.set(150, 225-myMaps.isSkin_200, 175, 250-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        } else if (m_Game.mark41[i][j] > 1) {  //网口位
                            rt1.set(175, 225-myMaps.isSkin_200, 200, 250-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                    }
                    if (m_Game.m_nRow3 == i && m_Game.m_nCol3 == j) {//正推地图中，仓管员的初始位置
                        myPaint.setARGB(150, 200, 0, 200);
                        myPaint.setStyle(Paint.Style.STROKE);
                        myPaint.setStrokeWidth(3);
                        canvas.drawCircle(rt.left + 25, rt.top + 25, 10, myPaint);
                    }
                    //当“逆推时使用正推目标点”时，对逆推目标点做简单提示
                    if (m_lShowDst2 && myMaps.m_Sets[32] == 1 && (m_Game.bk_cArray[i][j] == '.' || m_Game.bk_cArray[i][j] == '*' || m_Game.bk_cArray[i][j] == '+')) {
                        myPaint.setARGB(127, 255, 255, 0);
                        myPaint.setStyle(Paint.Style.FILL);
                        rt1.set(rt.left+20, rt.top+20, rt.right-20, rt.bottom-20);
                        canvas.drawRect(rt1, myPaint);
                        myPaint.setARGB(223, 255, 255, 255);
                        rt1.set(rt.left+22, rt.top+22, rt.right-22, rt.bottom-22);
                        canvas.drawRect(rt1, myPaint);
                    }
                } else {  //正推
                    if (m_bBoxTo && m_Game.mPathfinder.mark3[i][j]) { //箱子可达位置提示
                        if (myMaps.m_Sets[8] == 1 || (m_iR == i && m_iC == j)) {
                            rt1.set(150, 200-myMaps.isSkin_200, 175, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                    }
                    if (myMaps.m_Sets[8] == 1) {  //显示可达位置提示
                        if (m_boxCanMove && m_Game.mark11[i][j] || m_boxCanCome && m_Game.mark11[i][j]) { //可动/来箱子提示
                            rt1.set(150, 200-myMaps.isSkin_200, 175, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                        if (m_bManTo && m_Game.mPathfinder.mark1[i][j]) { //人可达位置提示
                            rt1.set(150, 200-myMaps.isSkin_200, 175, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                        if ((m_boxCanMove || m_bManTo || m_bBoxTo) && m_Game.mPathfinder.mark5[i][j]) { //穿越点提示
                            rt1.set(175, 200-myMaps.isSkin_200, 200, 225-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                    }
                    if (m_boxNoMoved && !m_Game.mark11[i][j] ||
                        m_boxNoUsed && !m_Game.mark12[i][j] && (ch == '-' || ch == '.')) {  //显示未动过的箱子提示或地板未被使用过
                        myPaint.setColor(Color.WHITE);                    //设置画笔颜色
                        myPaint.setStrokeWidth((float) 5.0);              //设置线宽
                        canvas.drawLine(rt.left+10, rt.top+10, rt.right-10, rt.bottom-10, myPaint);
                        canvas.drawLine(rt.right-10, rt.top+10, rt.left+10, rt.bottom-10, myPaint);
                    }
                    if (m_Game.m_bNetLock) {
                        if (m_Game.mark41[i][j] == 1) {  //网锁位
                            rt1.set(150, 225-myMaps.isSkin_200, 175, 250-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        } else if (m_Game.mark41[i][j] > 1) {  //网口位
                            rt1.set(175, 225-myMaps.isSkin_200, 200, 250-myMaps.isSkin_200);
                            canvas.drawBitmap(myMaps.skinBit, rt1, new Rect(rt.left+13, rt.top+13, rt.right-12, rt.bottom-12), myPaint);
                        }
                    }
                }
                if (m_Game.bt_Sel.isChecked()) { //计数状态
                    myPaint.setARGB(100, 200, 0, 200);
                    myPaint.setStyle(Paint.Style.FILL);
                    if (m_Game.bt_BK.isChecked()) {  //逆推
                        if (m_Game.bk_selArray[i][j] == 1) canvas.drawRect(rt, myPaint);
                        if (selNode2.r2 < 0 && m_iR == i && m_iC == j) {
                            rt1.set(100, 200-myMaps.isSkin_200, 150, 250-myMaps.isSkin_200);
                            canvas.drawRect(rt, myPaint);
                            canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        }
                    } else {
                        if (m_Game.m_selArray[i][j] == 1) canvas.drawRect(rt, myPaint);
                        if (selNode.r2 < 0 && m_iR == i && m_iC == j) {
                            rt1.set(100, 200-myMaps.isSkin_200, 150, 250-myMaps.isSkin_200);
                            canvas.drawRect(rt, myPaint);
                            canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                        }
                    }
                } else if (myMaps.m_bBiaochi && ch != '_' && m_iR == i && m_iC == j) {
                    rt1.set(100, 200-myMaps.isSkin_200, 150, 250-myMaps.isSkin_200);
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                }

            }  //end for j
        } //end for i

        //精细移动动画
        if (!m_Game.m_bACT_ERROR && (d_Moves < m_PicWidth || m_Game.m_nStep > 0)) {  //有移动
            if (m_Game.b_nRow >= 0) {  //移动的箱子
                getRect(m_Game.b_nRow, m_Game.b_nCol, rt);

                if (d_Moves*2 >= m_PicWidth) {
                    if (box_Goal[1]) rt1.set(50, 300-myMaps.isSkin_200, 100, 350-myMaps.isSkin_200);
                    else rt1.set(50, 250-myMaps.isSkin_200, 100, 300-myMaps.isSkin_200);
                } else {
                    if (box_Goal[0]) rt1.set(50, 300-myMaps.isSkin_200, 100, 350-myMaps.isSkin_200);
                    else rt1.set(50, 250-myMaps.isSkin_200, 100, 300-myMaps.isSkin_200);
                }
                canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                if (myMaps.m_bBianhao) {  //箱子使用自动编号
                    boxNum(canvas, rt, m_Game.m_iBoxNum2[m_Game.b_nRow][m_Game.b_nCol]);
                } else {
                    boxNum(canvas, rt, m_Game.m_iBoxNum[m_Game.b_nRow][m_Game.b_nCol]);
                }
            }

            //移动的仓管员
            if (m_Game.bt_BK.isChecked()) getRect(m_Game.m_nRow2, m_Game.m_nCol2, rt);
            else getRect(m_Game.m_nRow, m_Game.m_nCol, rt);

            if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200) {  //简单皮肤
                if (myMaps.isHengping) rt1.set(150, 250-myMaps.isSkin_200, 200, 300-myMaps.isSkin_200);  //使用横屏皮肤
                else rt1.set(100, 250-myMaps.isSkin_200, 150, 300-myMaps.isSkin_200);  //不使用横屏皮肤
            } else {  //标准皮肤
                switch (PlayDir[myMaps.m_nTrun][myMaps.m_Sets[5]]) {
                    case 1:  //左
                        rt1.set(0, 350, 50, 400);
                        break;
                    case 3:  //右
                        rt1.set(150, 250, 200, 300);
                        break;
                    case 4:  //下
                        rt1.set(100, 350, 150, 400);
                        break;
                    default:  //上
                        rt1.set(100, 250, 150, 300);
                        break;
                }
            }
            byte h = 1;
            if (myMaps.m_nTrun > 3) h *= -1;
            if (d_Moves < 0) {  //转弯
                if (d_Moves < -10000) {  //180
                    canvas.rotate(-(10000+d_Moves)*h, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                    canvas.rotate((10000+d_Moves)*h, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                } else if (d_Moves < -2000) {  //90
                    canvas.rotate((2000+d_Moves)*h, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                    canvas.rotate(-(2000+d_Moves)*h, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                } else {  //-90
                    canvas.rotate(-(1000+d_Moves)*h, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                    canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
                    canvas.rotate((1000+d_Moves)*h, rt.left + m_PicWidth / 2, rt.top + m_PicWidth / 2);
                }
            } else canvas.drawBitmap(myMaps.skinBit, rt1, rt, myPaint);
        }

        canvas.restore();

        //进度条
        if (m_lGoto && !m_Game.bt_BK.isChecked() || m_lGoto2 && m_Game.bt_BK.isChecked()) {
            if (m_Game.bt_BK.isChecked()) cur = curMoves2;
            else cur = curMoves;

            myPaint.setARGB(223, 255, 255, 255);
            myPaint.setStrokeWidth(3);
            canvas.drawLine(stLeft, stTop+20, stRight, stTop+20, myPaint);
            myPaint.setARGB(255, 0, 150, 0);
            canvas.drawLine(stLeft, stTop+20, stLeft + cur, stTop+20, myPaint);
            myPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(stLeft + cur, stTop + 20, 15, myPaint);
        }

        //奇偶格位明暗度调整条
        if (m_lParityBrightnessShade) {
            ss = sp2px(myMaps.ctxDealFile, 16);
            myPaint.setARGB(255, 255, 255, 255);  //设置字体颜色
            canvas.drawText("偶位格: ", 20, m_nArenaTop + mrBrightnessShade.top - ss / 2, myPaint);
            canvas.drawText("奇位格: ", 20, m_nArenaTop + mrBrightnessShade2.top - ss / 2, myPaint);
            myPaint.setARGB(223, 255, 255, 255);
            canvas.drawRect(mrBrightnessShade, myPaint);
            canvas.drawRect(mrBrightnessShade2, myPaint);
            myPaint.setARGB(255, 0, 0, 150);
            canvas.drawRect(mrBrightnessShade.left, mrBrightnessShade.top, mrBrightnessShade.left + (myMaps.m_Sets[39]+1) * 40, mrBrightnessShade.bottom, myPaint);
            canvas.drawRect(mrBrightnessShade2.left, mrBrightnessShade2.top, mrBrightnessShade2.left + (myMaps.m_Sets[40]+1) * 40, mrBrightnessShade2.bottom, myPaint);
            myPaint.setARGB(255, 255, 255, 255);  //设置字体颜色
            canvas.drawText("" + myMaps.m_Sets[39], mrBrightnessShade.right + 15, m_nArenaTop + mrBrightnessShade.top - ss / 2, myPaint);
            canvas.drawText("" + myMaps.m_Sets[40], mrBrightnessShade.right + 15, m_nArenaTop + mrBrightnessShade2.top - ss / 2, myPaint);

            int c = m_nArenaTop + mrBrightnessShade.top + 15;
            myPaint.setARGB(127, 255, 0, 255);  //设置字体颜色
            canvas.drawRect(mrBrightnessShade.right + 90, mrBrightnessShade.top, mrBrightnessShade.right + ss + 150, mrBrightnessShade2.bottom, myPaint);
            myPaint.setARGB(255, 255, 255, 255);  //设置字体颜色
            canvas.drawText("默", mrBrightnessShade.right + 120, c - ss / 2, myPaint);
            canvas.drawText("认", mrBrightnessShade.right + 120, c + ss / 2 + 30, myPaint);
        }

        //全屏时，在底部显示 undo、redo 两个按钮
        if (m_nArenaTop == 0) {
            myPaint.setARGB(127, 0, 0, 0);
            myPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(m_rUnDo, myPaint);
            canvas.drawRect(m_rReDo, myPaint);
            myPaint.setARGB(255, 0, 0, 0);
            canvas.drawBitmap(bitUnDo, m_rUnDo.left, m_rUnDo.top, null);   //画按钮
            canvas.drawBitmap(bitReDo, m_rReDo.left, m_rReDo.top, null);   //画按钮
        }

        //更换背景的两个按钮
        if (m_lChangeBK) {
            myPaint.setARGB(255, 0, 0, 0);
            canvas.drawBitmap(bitPreBK, m_rPre_BK.left, m_rPre_BK.top, null);   //画按钮
            canvas.drawBitmap(bitNextBK, m_rNext_BK.left, m_rNext_BK.top, null);   //画按钮
            canvas.drawBitmap(bitColorBK, m_rColor_BK.left, m_rColor_BK.top, null);   //画按钮
            canvas.drawBitmap(bitPreSkin, m_rPre_Skin.left, m_rPre_Skin.top, null);   //画按钮
            canvas.drawBitmap(bitNextSkin, m_rNext_Skin.left, m_rNext_Skin.top, null);   //画按钮
        }

        //单步宏
        if (myMaps.isMacroDebug && !m_Game.bt_BK.isChecked()) {
            ss = sp2px(myMaps.ctxDealFile, 16);
            myPaint.setTextSize(ss);  //设置字体大小
            myPaint.setStyle(Paint.Style.FILL);
            myPaint.setStrokeWidth(1);
            int p = myMacro.size()-1;
            if (myMacro.size() > 1) {
                myPaint.setARGB(255, 191, 191, 191);  //设置字体颜色
                canvas.drawText((myMacro.get(p-1)+1) + ": " + myMaps.sAction[myMacro.get(p-1)] + myMacroInf, 0, m_nArenaTop + ss, myPaint);
            }
            if (myMacro.get(p) < myMaps.sAction.length) {
                myPaint.setARGB(255, 255, 255, 255);  //设置字体颜色
                canvas.drawText((myMacro.get(p)+1) + ": " + myMaps.sAction[myMacro.get(p)], 0, m_nArenaTop + (ss + 4) * 2, myPaint);
                int t = myMacro.get(p)+1;
                if (t < myMaps.sAction.length) {
                    canvas.drawText((t+1) + ": " + myMaps.sAction[t], 0, m_nArenaTop + (ss + 4) * 3, myPaint);
                }
                if (!myMaps.sAction[myMacro.get(p)].isEmpty() && myMaps.sAction[myMacro.get(p)].charAt(0) == '*'){
                    for (int k = t+1; k < myMaps.sAction.length; k++) {
                        canvas.drawText((k+1) + ": " + myMaps.sAction[k], 0, m_nArenaTop + (ss + 4) * (k-p+2), myPaint);
                        if (!myMaps.sAction[k].isEmpty() && myMaps.sAction[k].charAt(0) == '*') {
                            break;
                        }
                    }
                }
            }
        }

        //顶行信息栏
        if (m_nArenaTop > 0) {
            myPaint.setARGB(255, 60, 70, 80);
            myPaint.setStyle(Paint.Style.FILL);
            rt1.set(0, 0, getWidth(), m_nArenaTop);
            canvas.drawRect(rt1, myPaint);
            canvas.drawBitmap(bitPre, m_rPre.left, m_rPre.top, null);   //画上一关按钮
            canvas.drawBitmap(bitNext, m_rNext.left, m_rNext.top, null);  //画下一关按钮

            myPaint.setARGB(67, 255, 255, 255);

            ss = sp2px(myMaps.ctxDealFile, 12);
            myPaint.setTextSize(ss);
            myPaint.getTextBounds("888888888", 0, 7, rt6);  //关卡序号框、游标框
            myPaint.getTextBounds("888888888", 0, 9, rt7);  //推移步数框
            ss = sp2px(myMaps.ctxDealFile, 9);
            myPaint.setTextSize(ss);
            myPaint.getTextBounds("关", 0, 1, rt5);  //文字框

            hh = (m_nArenaTop - rt6.height()) / 4;  //框的外部 panding
            ww = getWidth()/2;  //顶行信息栏中点

            myPaint.setARGB(255, 80, 100, 110);
            rt2.set(ww-rt7.width()-hh/2, hh*3/4, ww-hh/2, m_nArenaTop-hh*3/4);
            canvas.drawRect(rt2, myPaint);
            rt3.set(ww+rt5.width()+hh, hh*3/4, ww+rt5.width()+rt7.width()+hh, m_nArenaTop-hh*3/4);
            canvas.drawRect(rt3, myPaint);
            if (myMaps.m_Sets[12] == 1 && mySQLite.m_SQL.find_Level(myMaps.curMap.key, myMaps.curMap.Level_id) > -1) myPaint.setARGB(255, 127, 0, 0);
            else myPaint.setARGB(255, 80, 100, 110);
            rt4.set(ww+rt7.width()+rt5.width()*2+hh*2, hh*3/4, ww+rt7.width()+rt5.width()*2+rt6.width()+hh*2, m_nArenaTop-hh*3/4);
            canvas.drawRect(rt4, myPaint);
            if (myMaps.curMap.Solved) myPaint.setARGB(136, 0, 71, 0);
            else myPaint.setARGB(255, 80, 100, 110);
            rt1.set(ww-rt7.width()-rt5.width()-rt6.width()-hh*3/2, hh*3/4, ww-rt7.width()-rt5.width()-hh*3/2, m_nArenaTop-hh*3/4);
            canvas.drawRect(rt1, myPaint);

            myPaint.setARGB(255, 255, 255, 255);
            canvas.drawText("关", rt1.left-rt5.width()-hh/2, rt5.height()+hh/2, myPaint);
            canvas.drawText("卡", rt1.left-rt5.width()-hh/2, rt5.height()*2+hh, myPaint);
            if (m_Game.bt_Sel.isChecked()) { //计数状态
                canvas.drawText("目", rt2.left-rt5.width()-hh/2, rt5.height()+hh/2, myPaint);
                canvas.drawText("标", rt2.left-rt5.width()-hh/2, rt5.height()*2+hh, myPaint);
                canvas.drawText("箱", ww+hh/2, rt5.height()+hh/2, myPaint);
                canvas.drawText("子", ww+hh/2, rt5.height()*2+hh, myPaint);
                canvas.drawText("完", rt4.left-rt5.width()-hh/2, rt5.height()+hh/2, myPaint);
                canvas.drawText("成", rt4.left-rt5.width()-hh/2, rt5.height()*2+hh, myPaint);
            } else {
                canvas.drawText("移", rt2.left-rt5.width()-hh/2, rt5.height()+hh/2, myPaint);
                canvas.drawText("动", rt2.left-rt5.width()-hh/2, rt5.height()*2+hh, myPaint);
                canvas.drawText("推", ww+hh/2, rt5.height()+hh/2, myPaint);
                canvas.drawText("动", ww+hh/2, rt5.height()*2+hh, myPaint);
                canvas.drawText("游", rt4.left-rt5.width()-hh/2, rt5.height()+hh/2, myPaint);
                canvas.drawText("标", rt4.left-rt5.width()-hh/2, rt5.height()*2+hh, myPaint);
            }
            myPaint.setARGB(255, 255, 255, 255);
            ss = sp2px(myMaps.ctxDealFile, 12);
            myPaint.setTextSize(ss);
            mStr = String.valueOf(myMaps.m_lstMaps.indexOf(myMaps.curMap)+1);
            myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //关卡序号框
            canvas.drawText(mStr, rt1.left + (rt6.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
            if (m_Game.bt_Sel.isChecked()) { //计数状态
                if (m_Game.bt_BK.isChecked()) {  //逆推
                    mStr = String.valueOf(m_Count[4]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //目标数框
                    canvas.drawText(mStr, rt2.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                    mStr = String.valueOf(m_Count[3]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //箱子数框
                    canvas.drawText(mStr, rt3.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                    mStr = String.valueOf(m_Count[5]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //完成数框
                    canvas.drawText(mStr, rt4.left + (rt6.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                } else {  //正推
                    mStr = String.valueOf(m_Count[1]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //目标数框
                    canvas.drawText(mStr, rt2.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                    mStr = String.valueOf(m_Count[0]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //箱子数框
                    canvas.drawText(mStr, rt3.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                    mStr = String.valueOf(m_Count[2]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //完成数框
                    canvas.drawText(mStr, rt4.left + (rt6.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                }
            } else {
                if (m_Game.bt_BK.isChecked()) {  //逆推
                    mStr = String.valueOf(m_Game.m_iStep[3]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //移动数框
                    canvas.drawText(mStr, rt2.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                    mStr = String.valueOf(m_Game.m_iStep[2]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //推动数框
                    canvas.drawText(mStr, rt3.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                } else {  //正推
                    mStr = String.valueOf(m_Game.m_iStep[1]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //移动数框
                    canvas.drawText(mStr, rt2.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                    mStr = String.valueOf(m_Game.m_iStep[0]);
                    myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //推动数框
                    canvas.drawText(mStr, rt3.left + (rt7.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
                }
                if (m_iR < 0 || m_iC < 0 || m_iR >= myMaps.curMap.Rows || m_iC >= myMaps.curMap.Cols)
                    mStr = " ";
                else
                    mStr = String.valueOf(mGetCur(m_iR, m_iC));
                myPaint.getTextBounds(mStr, 0, mStr.length(), rt8);  //游标框
                if (m_lEven) {
                    myPaint.setARGB(255, 255, 255, 255);
                } else {
                    myPaint.setARGB(255, 0, 255, 255);
                }
                canvas.drawText(mStr, rt4.left + (rt6.width()-rt8.width())/2, rt8.height()+hh*2, myPaint);
            }
        }
        ss = sp2px(myMaps.ctxDealFile, 16);
        myPaint.setTextSize(ss);
        if (myMaps.m_Sets[13] == 1) {  //即景模式
            rt.set(getWidth()-ss*2-ss/2, m_nArenaTop, getWidth(), m_nArenaTop+ss+ss/2);
            myPaint.setARGB(127, 0, 0, 0);
            myPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(rt, myPaint);
            myPaint.setARGB(255, 255, 255, 255);
            canvas.drawText("即景", getWidth()-ss*2-4, m_nArenaTop+ss+4, myPaint);
        }


        if (!m_Game.bt_BK.isChecked() && m_Game.m_Gif_Start > 0) {
            mStr = "GIF 开始点：" + m_Game.m_Gif_Start;
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStrokeWidth(5);
            canvas.drawText(mStr, (getWidth()-ss*5)/2, m_nArenaTop+ss+4, myPaint);
            myPaint.setARGB(255, 255, 255, 255);
            myPaint.setStrokeWidth(1);
            canvas.drawText(mStr, (getWidth()-ss*5)/2, m_nArenaTop+ss+4, myPaint);
        }

        if (myMaps.isRecording) {
            myPaint.setARGB(127, 0, 0, 0);
            myPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(m_rRecording, myPaint);
            myPaint.setARGB(255, 255, 0, 0);
            canvas.drawText("结束录制", m_rRecording.left+4, m_rRecording.top+ss+4, myPaint);
        }

        if (m_lShowAnsInf) {  //显示答案信息
            myPaint.setARGB(255, 255, 255, 255);
            myPaint.setStyle(Paint.Style.FILL);
            myPaint.setStrokeWidth(1);
            for (int k = 0; k < myMaps.mState2.size(); k++) {
                mStr = myMaps.mState2.get(k).inf.replace("移动: ", "").replace(", 推动: ", "/");
                canvas.drawText(mStr + ", " + myMaps.mState2.get(k).time, 0, m_nArenaTop+ss*(k+1)+4, myPaint);
            }
        }
    }

    //箱子编号的计算
    char[] myNum = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    public String mGetBoxNum(int num) {
        if (myMaps.m_bBianhao) {  //箱子使用自动编号
            String low = Integer.toString(num);
            if (num > 5999) {
                return "";
            } else if (num > 99) {
                int h = num / 100;
                return new StringBuilder().append(myNum[h]).append(low.substring(low.length()-2)).toString();
            } else {
                return low;
            }
        } else {  //人为编号：A -- Z, A0--A9 ～ Z0--Z9, 共26 + 260 = 286个编号
            if (num > 25) {  // 超过26，使用字母 + 数字
                int n0 = ((num - 25) % 10 + 9) % 10, n1 = (num - 26) / 10;  // 使字母后面的数字错从 0 到 9（否则是1、2、3...9、0）
                return new StringBuilder().append((char) ((byte) (n1 % 26 + 65))).append(String.valueOf(n0)).toString();
            } else {  // 26以内，使用26个单字母
                return new StringBuilder().append((char) ((byte) (num + 65))).toString();
            }
        }
    }

    private boolean isBox(char[][] lvl, int rr, int cc) {
        try {
            return (lvl[rr][cc] == '$' || lvl[rr][cc] == '*');
        } catch (ArrayIndexOutOfBoundsException ex) { } catch (Throwable ex) { }
        return false;
    }

    //是否寻求阶梯
    private boolean isCanMoved(boolean isBK, int f_Row, int f_Col) {
        boolean[][] mark = isBK ? m_Game.mPathfinder.mark4 : m_Game.mPathfinder.mark3;
        try {
            return mark[f_Row][f_Col];
        } catch (ArrayIndexOutOfBoundsException ex) { } catch (Throwable ex) { }
        return false;
    }

    //是否寻求阶梯
    private int isLadder(boolean isBK, int f_Row, int f_Col) {
        boolean[][] mark = isBK ? m_Game.mPathfinder.mark4 : m_Game.mPathfinder.mark3;
        char[][] m_Level = isBK ? m_Game.bk_cArray : m_Game.m_cArray;
        int dirR, dirC, dir;
        try {
            if (!isBox(m_Level,f_Row - 2,f_Col - 2) && isBox(m_Level,f_Row - 1,f_Col - 1) && mark[f_Row - 1][f_Col - 1] && (isCanMoved(isBK, f_Row,f_Col - 1) || isCanMoved(isBK, f_Row - 1,f_Col) || isCanMoved(isBK, f_Row - 2,f_Col - 1) || isCanMoved(isBK, f_Row - 1,f_Col - 2))) {
                f_Row--;   //首箱子在左上
                f_Col--;
                dirR = 1;  //向下
                dirC = 1;  //向右
                dir =  3;  //右下阶梯
            } else if (!isBox(m_Level,f_Row + 2,f_Col + 2) && isBox(m_Level,f_Row + 1,f_Col + 1) && mark[f_Row + 1][f_Col + 1] && (isCanMoved(isBK, f_Row,f_Col + 1) || isCanMoved(isBK, f_Row + 1,f_Col) || isCanMoved(isBK, f_Row + 2,f_Col + 1) || isCanMoved(isBK, f_Row + 1,f_Col + 2))) {
                f_Row++;    //首箱子在右下
                f_Col++;
                dirR = -1;  //向上
                dirC = -1;  //向左
                dir =  1;   //左上阶梯
            } else if (!isBox(m_Level,f_Row - 2,f_Col + 2) && isBox(m_Level,f_Row - 1,f_Col + 1) && mark[f_Row - 1][f_Col + 1] && (isCanMoved(isBK, f_Row,f_Col + 1) || isCanMoved(isBK, f_Row - 1,f_Col) || isCanMoved(isBK, f_Row - 2,f_Col + 1) || isCanMoved(isBK, f_Row - 1,f_Col + 2))) {
                f_Row--;    //首箱子在右上
                f_Col++;
                dirR = 1;   //向下
                dirC = -1;  //向左
                dir =  4;   //左下阶梯
            } else if (!isBox(m_Level,f_Row + 2,f_Col - 2) && isBox(m_Level,f_Row + 1,f_Col - 1) && mark[f_Row + 1][f_Col - 1] && (isCanMoved(isBK, f_Row,f_Col - 1) || isCanMoved(isBK, f_Row + 1,f_Col) || isCanMoved(isBK, f_Row + 2,f_Col - 1) || isCanMoved(isBK, f_Row + 1,f_Col - 2))) {
                f_Row++;    //首箱子在左下
                f_Col--;
                dirR = -1;  //向上
                dirC = 1;   //向右
                dir =  2;   //右上阶梯
            } else {
                return 0;  //无有效阶梯形成
            }

            m_Game.mPathfinder.FindBlock(isBK, m_Level, f_Row, f_Col);   //计算割点，块
            m_Game.mPathfinder.boxReachable(isBK, f_Row, f_Col, isBK ? m_Game.m_nRow2 : m_Game.m_nRow, isBK ? m_Game.m_nCol2 : m_Game.m_nCol);  //计算箱子可达点

            for (int i = 0; i < m_nRows; i++) {
                for (int j = 0; j < m_nCols; j++) {
                    if (i == f_Row && (j == f_Col+1 || j == f_Col-1) || j == f_Col && (i == f_Row+1 || i == f_Row-1)) continue;
                    mark[i][j] = false;
                }
            }

            int r = f_Row, c = f_Col;
            LinkedList<Ladder> m_Ladder = isBK ? mLadder2 : mLadder;
            while (true) {
                if (isBox(m_Level, r, c)) {
                    m_Ladder.offer(new Ladder(r, c));
                    mark[r][c] = true;
                } else break;
                r = r + dirR;
                c = c + dirC;
            }
            return dir;
        } catch (ArrayIndexOutOfBoundsException ex) { } catch (Throwable ex) { }

        return 0;  //无有效阶梯形成
    }

    //根据关卡旋转的状态，计算刷新显示的坐标
    private int[] get_iR_iC(int i, int j) {
        int[] RC = {0, 0};
        switch (myMaps.m_nTrun) {
            case 0:
                RC[0] = ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[1] = ((int) ((j - m_fTop ) / m_fScale)) / m_PicWidth;
                break;
            case 1:
                RC[1] = m_Game.m_cArray.length - 1 - ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[0] = ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
                break;
            case 2:
                RC[0] = m_Game.m_cArray[0].length - 1 - ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[1] = m_Game.m_cArray.length - 1 - ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
                break;
            case 3:
                RC[1] = ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[0] = m_Game.m_cArray[0].length - 1 - ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
                break;
            case 4:
                RC[0] = m_Game.m_cArray[0].length - 1 - ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[1] = ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
                break;
            case 5:
                RC[1] = m_Game.m_cArray.length - 1 - ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[0] = m_Game.m_cArray[0].length - 1 - ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
                break;
            case 6:
                RC[0] = ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[1] = m_Game.m_cArray.length - 1 - ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
                break;
            case 7:
                RC[1] = ((int) ((i - m_fLeft) / m_fScale)) / m_PicWidth;
                RC[0] = ((int) ((j - m_fTop) / m_fScale)) / m_PicWidth;
        }
        return RC;
    }

    //画箱子编号
    private void boxNum(Canvas canvas, Rect rt, int num) {
        if (num < 0 || m_Game.bt_BK.isChecked()) return;  //逆推不支持箱子编号

        if (myMaps.m_bBianhao) {
            myPaint.setTextSize(m_PicWidth / 3);
        } else {
            myPaint.setTextSize(m_PicWidth / 2);
        }
        String str;
        str = String.valueOf(mGetBoxNum(num));
        myPaint.getTextBounds(str, 0, str.length(), rt8);  //文字框
        myPaint.setARGB(255, 255, 255, 255);
        myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        myPaint.setStrokeWidth(3);
        canvas.drawText(str, rt.left + (m_PicWidth - rt8.width()) / 2, rt.bottom - (m_PicWidth - rt8.height()) / 2, myPaint);
        myPaint.setARGB(255, 0, 0, 0);
        myPaint.setStrokeWidth(2);
        canvas.drawText(str, rt.left + (m_PicWidth - rt8.width()) / 2, rt.bottom - (m_PicWidth - rt8.height()) / 2, myPaint);
    }

    private int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    //根据点击位置确定行动
    private void doACT(int i, int j, boolean isAct) {

        m_Game.m_bACT_ERROR = false;  //执行动作时是否遇到错误

        m_boxCanMove = false;  //关闭可动箱子提示状态
        m_boxCanMove2 = false;
        m_boxNoMoved = false;  //关闭未动箱子提示状态
        m_boxNoUsed = false;  //关闭未使用地板提示状态

        if (m_Game.m_bBusing || myMaps.isMacroDebug) {
            mClickObj = '_';
            return;
        }
        if ((m_lGoto || m_lGoto2) && mrProgressBar.contains(i, j)) {  //进度条内
            m_Game.m_bNetLock = false;  //取消网型提示
            if (m_Game.bt_BK.isChecked()) {  //逆推
                if (m_lGoto2) {
                    curMoves2 = i - stLeft;
                    int len1 = m_Game.m_lstMovUnDo2.size();
                    int len2 = m_Game.m_lstMovReDo2.size();
                    int n = (int) ((double) curMoves2 / (stRight - stLeft) * (len1+len2));  //计算 goto 点
                    if (n > len1) {
                        for (int k = len1; k < n; k++) m_Game.reDo2();
                        m_Game.m_bBusing = false;
                    } else if (n < len1) {
                        for (int k = n; k < len1; k++) m_Game.unDo2();
                        m_Game.m_bBusing = false;
                    }
                }
            } else {
                if (m_lGoto) {
                    curMoves = i - stLeft;
                    int len1 = m_Game.m_lstMovUnDo.size();
                    int len2 = m_Game.m_lstMovReDo.size();
                    int n = (int) ((double) curMoves / (stRight - stLeft) * (len1+len2));  //计算 goto 点
                    if (n > len1) {
                        for (int k = len1; k < n; k++) m_Game.reDo1();
                        m_Game.m_bBusing = false;
                    } else if (n < len1) {
                        for (int k = n; k < len1; k++) m_Game.unDo1();
                        m_Game.m_bBusing = false;
                    }
                }
            }
            return;
        } else if (m_lParityBrightnessShade) {  // 奇偶位明暗度调整条
            int v;
            ss = sp2px(myMaps.ctxDealFile, 16);
            if (mrBrightnessShade.contains(i, j)) {         // 偶位明暗度调整条
                v = (i - mrBrightnessShade.left) / 40;
                myMaps.m_Sets[39] = v;
            } else if (mrBrightnessShade2.contains(i, j)) {  // 奇位明暗度调整条
                v = (i - mrBrightnessShade2.left) / 40;
                myMaps.m_Sets[40] = v;
            } else if (i > mrBrightnessShade.right + 90 && i < mrBrightnessShade.right + ss + 150 && j > mrBrightnessShade.top && j < mrBrightnessShade2.bottom) {
                myMaps.m_Sets[39] = 0;
                myMaps.m_Sets[40] = 5;
            } else if (i < mrBrightnessShade.left && j > mrBrightnessShade.top && j < mrBrightnessShade.bottom) {
                if (i > mrBrightnessShade.left-30) myMaps.m_Sets[39] = 0;
            } else if (i < mrBrightnessShade.left && j > mrBrightnessShade2.top && j < mrBrightnessShade2.bottom) {
                if (i > mrBrightnessShade.left-30) myMaps.m_Sets[40] = 0;
            } else if (i > mrBrightnessShade.right && j > mrBrightnessShade.top && j < mrBrightnessShade.bottom) {
                if (i < mrBrightnessShade.right+30) myMaps.m_Sets[39] = 15;
            } else if (i > mrBrightnessShade.right && j > mrBrightnessShade2.top && j < mrBrightnessShade2.bottom) {
                if (i < mrBrightnessShade.right+30) myMaps.m_Sets[40] = 15;
            }

            if (j < mrBrightnessShade.top-30 || j > mrBrightnessShade2.bottom+30) {
                m_lParityBrightnessShade = false;
            } else {
                return;
            }
        } else if (j < m_nArenaTop) {
            mClickObj = '_';  //顶行信息栏内
            return;
        } else if (i < m_fLeft || j < m_fTop) {  //界外
            m_iC = -1;
            m_iR = -1;
        } else {
            int[] RC = get_iR_iC(i, j);
            m_iC = RC[0];
            m_iR = RC[1];
        }
        if (m_iR < 0 || m_iR >= m_Game.m_cArray.length || m_iC < 0 || m_iC >= m_Game.m_cArray[0].length) {
            mClickObj = '_';
            return;
        }

        if (m_Game.bt_BK.isChecked()) mClickObj = m_Game.bk_cArray[m_iR][m_iC];
        else mClickObj = m_Game.m_cArray[m_iR][m_iC];
        if (!isAct) return;

        m_Game.m_nStep = 0;
        m_Game.m_bYanshi = false;
        m_Game.m_bYanshi2 = false;

        if (m_Game.bt_Sel.isChecked()) { //计数状态
            if (m_Game.bt_BK.isChecked()) {  //逆推
                selNode2.setPT(m_Game.bk_selArray, m_iR, m_iC);
                m_Count[3] = 0;  //逆推箱子数
                m_Count[4] = 0;  //逆推目标数
                m_Count[5] = 0;  //逆推完成数
                for (int r = 0; r < m_nRows; r++)
                    for (int c = 0; c < m_nCols; c++) {
                        switch (m_Game.bk_cArray[r][c]) {
                            case '$':
                                m_Count[3] += m_Game.bk_selArray[r][c];
                                break;
                            case '*':
                                m_Count[3] += m_Game.bk_selArray[r][c];
                                m_Count[4] += m_Game.bk_selArray[r][c];
                                m_Count[5] += m_Game.bk_selArray[r][c];
                                break;
                            case '.':
                            case '+':
                                m_Count[4] += m_Game.bk_selArray[r][c];
                                break;
                        }
                    }
            } else {  //正推
                selNode.setPT(m_Game.m_selArray, m_iR, m_iC);
                m_Count[0] = 0;  //正推箱子数
                m_Count[1] = 0;  //正推目标数
                m_Count[2] = 0;  //正推完成数
                for (int r = 0; r < m_nRows; r++)
                    for (int c = 0; c < m_nCols; c++) {
                        switch (m_Game.m_cArray[r][c]) {
                            case '$':
                                m_Count[0] += m_Game.m_selArray[r][c];
                                break;
                            case '*':
                                m_Count[0] += m_Game.m_selArray[r][c];
                                m_Count[1] += m_Game.m_selArray[r][c];
                                m_Count[2] += m_Game.m_selArray[r][c];
                                break;
                            case '.':
                            case '+':
                                m_Count[1] += m_Game.m_selArray[r][c];
                                break;
                        }
                    }
            }
        } else {  //正常状态
            m_Game.m_bNetLock = false;  //取消网型提示
            //逆推模式下，尚未动过箱子，且不在可达提示状态，点击空位是指定位置给仓管员
            if (m_Game.bt_BK.isChecked() && m_Game.m_lstMovUnDo2.size() == 0) {
                if (m_Game.bk_cArray[m_iR][m_iC] == '-' || m_Game.bk_cArray[m_iR][m_iC] == '.') {
                    if (!m_bBoxTo2) {  //未在可达提示状态，新的逆推开始前，记录仓管员初始位置
                        if (m_Game.m_nRow2 > -1) {  //非首次给仓管员定位，先清理前次定位
                            if (m_Game.bk_cArray[m_Game.m_nRow2][m_Game.m_nCol2] == '@')
                                m_Game.bk_cArray[m_Game.m_nRow2][m_Game.m_nCol2] = '-';
                            else
                                m_Game.bk_cArray[m_Game.m_nRow2][m_Game.m_nCol2] = '.';
                        }

                        //最新初始定位
                        if (m_Game.bk_cArray[m_iR][m_iC] == '-')
                            m_Game.bk_cArray[m_iR][m_iC] = '@';
                        else
                            m_Game.bk_cArray[m_iR][m_iC] = '+';

                        m_Game.m_nRow2 = m_iR;
                        m_Game.m_nCol2 = m_iC;
                        m_Game.m_nRow0 = m_Game.m_nRow2;
                        m_Game.m_nCol0 = m_Game.m_nCol2;

                        invalidate();

                        MyToast.showToast(m_Game, "拉动箱子则逆推开始！", Toast.LENGTH_SHORT);
                        return;
                    }
                } else if (m_Game.m_nRow2 < 0) {  //仓管员没有定位之前，点箱子是无效的
                    return;
                }
            }

            if (m_Game.bt_BK.isChecked()) {  //逆推
                ch = m_Game.bk_cArray[m_iR][m_iC];
                if (ch == '$' || ch == '*') { //点按了箱子，计算可达位置并显示
                    if (m_boxCanCome2 && m_Game.mark12[m_iR][m_iC]) {  //在可来提示状态
                        m_Game.mPathfinder.FindBlock(true, m_Game.bk_cArray, m_iR, m_iC);   //计算割点，块
                        m_Game.mPathfinder.boxTo(true, m_iR, m_iC, m_iR2, m_iC2, m_Game.m_nRow2, m_Game.m_nCol2, m_Game.m_lstMovReDo2);
                        m_Game.m_nStep = m_Game.m_lstMovReDo2.size();
                        m_Game.m_bMoved = true;  //有新动作，标示现场（包括答案）是否保存
                        m_Game.DoEvent(2);
                    } else if (m_bBoxTo2 && m_Game.mPathfinder.mark4[m_iR][m_iC]) {  //若在箱子可达位置提示状态又点了该箱子 && click_Box_Row2 == m_iR && click_Box_Col2 == m_iC
                        if (mLadder2.size() > 0) mLadder2.clear();
                        m_bBoxTo2 = false; //取消箱子可达位置提示状态标志
                    } else if (myMaps.m_Sets[29] == 1 && m_bBoxTo2 && (ladderDir2 = isLadder(true, m_iR, m_iC)) > 0) {  //是否触发爬阶梯（点击阶梯中的第二个箱子）
                    } else {
                        click_Box_Row2 = m_iR;
                        click_Box_Col2 = m_iC;
                        if (myMaps.m_Sets[11] == 1 && m_Game.mark15 != null && m_Game.mark15[m_iR][m_iC]) {
                            MyToast.showToast(myMaps.ctxDealFile, "这是一个被冻结的箱子！", Toast.LENGTH_SHORT);
                            for (int r = 0; r < m_nRows; r++) {
                                for (int c = 0; c < m_nCols; c++) {
                                    m_Game.mPathfinder.mark4[r][c] = false;
                                }
                            }
                            m_Game.mPathfinder.mark4[m_iR][m_iC] = true;
                        } else {
                            if (mLadder2.size() > 0) mLadder2.clear();
                            m_Game.mPathfinder.FindBlock(true, m_Game.bk_cArray, m_iR, m_iC);   //计算割点，块
                            m_Game.mPathfinder.boxReachable(true, m_iR, m_iC, m_Game.m_nRow2, m_Game.m_nCol2);  //计算箱子可达点
                        }
                        m_bBoxTo2 = true;  //置箱子可达位置提示状态标志
                        m_bManTo2 = false;
                    }
                } else { //end 点按了箱子
                    if (ch == '-' || ch == '.' || //点按了空地
                            (m_bBoxTo2 && (ch == '@' || ch == '+'))) { //提示状态下点了人
                        if (m_bBoxTo2) {//若在箱子可达位置提示状态
                            if (m_Game.mPathfinder.mark4[m_iR][m_iC] && (myMaps.m_Sets[24] == 0 || m_Game.mark14[m_iR][m_iC] > 0)) { //若为箱子的可达位置，自动推箱子的该位置
                                m_Game.mPathfinder.boxTo(true, click_Box_Row2, click_Box_Col2, m_iR, m_iC, m_Game.m_nRow2, m_Game.m_nCol2, m_Game.m_lstMovReDo2);
                                if (m_Game.m_lstMovReDo2.size() > 0) {
                                    if (mLadder2.size() > 0) {  //加上爬阶梯路径
                                        byte dir = m_Game.m_lstMovReDo2.getFirst();  //最后一个动作
                                        switch (dir) {
                                            case 5:  //左
                                                if (ladderDir2 == 1) {                      //左上阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 2) {               //右上阶梯
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 3) {               //右下阶梯
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                }
                                                break;
                                            case 6:  //上
                                                if (ladderDir2 == 1) {                      //左上阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 2) {               //右上阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 3) {               //右下阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                }
                                                break;
                                            case 7:  //右
                                                if (ladderDir2 == 1) {                      //左上阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 2) {               //右上阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 3) {               //右下阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                }
                                                break;
                                            case 8:  //下
                                                if (ladderDir2 == 1) {                      //左上阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 2) {               //右上阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else if (ladderDir2 == 3) {               //右下阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder2.removeFirst();
                                                    while (!mLadder2.isEmpty()) {
                                                        m_Game.m_lstMovReDo2.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo2.offerFirst(dir);
                                                        mLadder2.removeFirst();
                                                    }
                                                }
                                                break;
                                        }
                                    }
                                }
                                m_Game.m_nStep = m_Game.m_lstMovReDo2.size();
                                m_Game.m_bMoved = true;  //有新动作，标示现场（包括答案）是否保存
                                m_Game.DoEvent(2);
                            } else if (ch == '@' || ch == '+') {//点按了人
                                m_bManTo2 = true;
                                m_bBoxTo2 = false;
                                m_Game.mPathfinder.manReachable(true, m_Game.bk_cArray, m_Game.m_nRow2, m_Game.m_nCol2);
                            }
                        } else { //若仓管员可达，则自动移动
                            if (m_Game.FindPath(m_iR, m_iC, true)) {
                                m_Game.m_nStep = m_Game.m_lstMovReDo2.size();
                                m_Game.DoEvent(2);
                            }
                        }
                    } else {  //end 点按了空地
                        if (ch == '@' || ch == '+') {//点按了人
                            if (m_bManTo2) {
                                m_bManTo2 = false;
                            } else {
                                m_bManTo2 = true;
                                m_Game.mPathfinder.manReachable(true, m_Game.bk_cArray, m_Game.m_nRow2, m_Game.m_nCol2);
                            }
                        } else if (ch == '#' || ch == '_') {//点按了墙壁
                            m_bBoxTo2 = false;
                            m_bManTo2 = false;
                        }
                    }
                }
                m_boxCanCome2 = false;
            } else {   //end 逆推；begin 正推
                ch = m_Game.m_cArray[m_iR][m_iC];
                if (ch == '$' || ch == '*') {  //点按了箱子，计算可达位置并显示
                    if (m_boxCanCome && m_Game.mark11[m_iR][m_iC]) {  //在可来提示状态
                        m_Game.mPathfinder.FindBlock(false, m_Game.m_cArray, m_iR, m_iC);   //计算割点，块
                        m_Game.mPathfinder.boxTo(false, m_iR, m_iC, m_iR2, m_iC2, m_Game.m_nRow, m_Game.m_nCol, m_Game.m_lstMovReDo);
                        m_Game.m_nStep = m_Game.m_lstMovReDo.size();
                        m_Game.m_bMoved = true;  //有新动作，标示现场（包括答案）是否保存
                        m_Game.DoEvent(1);
                    } else if (m_bBoxTo && m_Game.mPathfinder.mark3[m_iR][m_iC]) {//若在箱子可达位置提示状态又点了该箱子 && click_Box_Row == m_iR && click_Box_Col == m_iC
                        if (mLadder.size() > 0) mLadder.clear();
                        m_bBoxTo = false;    //取消箱子可达位置提示状态标志
                    } else if (myMaps.m_Sets[29] == 1 && m_bBoxTo && (ladderDir = isLadder(false, m_iR, m_iC)) > 0) {  //是否触发爬阶梯（点击阶梯中的第二个箱子）
                    } else {
                        if (mLadder.size() > 0) mLadder.clear();
                        click_Box_Row = m_iR;
                        click_Box_Col = m_iC;
                        if (myMaps.m_Sets[11] == 1 && m_Game.mark16 != null && m_Game.mark16[m_iR][m_iC]) {
                            MyToast.showToast(myMaps.ctxDealFile, "这是一个陷于网锁的箱子！", Toast.LENGTH_SHORT);
                            for (int r = 0; r < m_nRows; r++) {
                                for (int c = 0; c < m_nCols; c++) {
                                    m_Game.mPathfinder.mark3[r][c] = false;
                                }
                            }
                            m_Game.mPathfinder.mark3[m_iR][m_iC] = true;
                        } else {
                            m_Game.mPathfinder.FindBlock(false, m_Game.m_cArray, m_iR, m_iC);   //计算割点，块
                            m_Game.mPathfinder.boxReachable(false, m_iR, m_iC, m_Game.m_nRow, m_Game.m_nCol);  //计算箱子可达点
                        }
                        m_bBoxTo = true;  //置箱子可达位置提示状态标志
                        m_bManTo = false;
                    }
                } else {  //end 点按了箱子
                    if (ch == '-' || ch == '.' || //点按了空地
                            (m_bBoxTo && (ch == '@' || ch == '+'))) { //提示状态下点了人
                        if (m_bBoxTo) {//若在箱子可达位置提示状态
                            if (m_Game.mPathfinder.mark3[m_iR][m_iC]) { //若为箱子的可达位置，自动推箱子的该位置
                                m_Game.mPathfinder.boxTo(false, click_Box_Row, click_Box_Col, m_iR, m_iC, m_Game.m_nRow, m_Game.m_nCol, m_Game.m_lstMovReDo);
                                if (m_Game.m_lstMovReDo.size() > 0) {
                                    if (mLadder.size() > 0) {  //加上爬阶梯路径
                                        byte dir = m_Game.m_lstMovReDo.getFirst();  //最后一个动作
                                        switch (dir) {
                                            case 5:  //左
                                                if (ladderDir == 1) {                      //左上阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 2) {               //右上阶梯
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 3) {               //右下阶梯
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                }
                                                break;
                                            case 6:  //上
                                                if (ladderDir == 1) {                      //左上阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 2) {               //右上阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 3) {               //右下阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                }
                                                break;
                                            case 7:  //右
                                                if (ladderDir == 1) {                      //左上阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 2) {               //右上阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 3) {               //右下阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)4);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                }
                                                break;
                                            case 8:  //下
                                                if (ladderDir == 1) {                      //左上阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 2) {               //右上阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)2);
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else if (ladderDir == 3) {               //右下阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)3);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                } else {                                   //左下阶梯
                                                    mLadder.removeFirst();
                                                    while (!mLadder.isEmpty()) {
                                                        m_Game.m_lstMovReDo.offerFirst((byte)1);
                                                        m_Game.m_lstMovReDo.offerFirst(dir);
                                                        mLadder.removeFirst();
                                                    }
                                                }
                                                break;
                                        }
                                    }
                                }
                                m_Game.m_nStep = m_Game.m_lstMovReDo.size();
                                m_Game.m_bMoved = true;  //有新动作，标示现场（包括答案）是否保存
                                m_Game.DoEvent(1);
                            } else if (ch == '@' || ch == '+') {//点按了人
                                m_bManTo = true;
                                m_bBoxTo = false;
                                m_Game.mPathfinder.manReachable(false, m_Game.m_cArray, m_Game.m_nRow, m_Game.m_nCol);
                            }
                        } else { //若仓管员可达，则自动移动
                            if (m_Game.FindPath(m_iR, m_iC, false)) {
                                m_Game.m_nStep = m_Game.m_lstMovReDo.size();
                                m_Game.DoEvent(1);
                            }
                        }
                    } else {  //end 点按了空地
                        if (ch == '@' || ch == '+') {//点按了人
                            if (m_bManTo) {
                                m_bManTo = false;
                            } else {
                                m_bManTo = true;
                                m_Game.mPathfinder.manReachable(false, m_Game.m_cArray, m_Game.m_nRow, m_Game.m_nCol);
                            }
                        } else if (ch == '#' || ch == '_') {//点按了墙壁
                            m_bBoxTo = false;
                            m_bManTo = false;
                        }
                    }
                }
                m_boxCanCome = false;
            }  //end 正推
        }
        invalidate();
    }

    //舞台初始化
    public void initArena() {
        //计算关卡图的像素尺寸
        m_nRows = myMaps.curMap.Rows;
        m_nCols = myMaps.curMap.Cols;
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

        //进度条
        stLeft = 40;
        stTop = getHeight()-120;
        stRight = getWidth()-40;
        stBottom = stTop+60;
        mrProgressBar = new Rect(stLeft, stTop, stRight, stBottom);

        // 明暗度调整条
        mrBrightnessShade  = new Rect(220, m_nArenaTop + 50,  859, m_nArenaTop + 120);
        mrBrightnessShade2 = new Rect(220, m_nArenaTop + 150, 859, m_nArenaTop + 220);

        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);

        if (w > 0 && h > 0 && (h != old_h || w != old_w)) {
            setArena();  //重置舞台场地
        }
    }

    //根据位置，自动计算标尺
    public String mGetCur(int r, int c) {

        StringBuilder s = new StringBuilder();

        if (myMaps.m_Sets[9] == 1) {  //标尺不随关卡旋转
            int n;
            switch (myMaps.m_nTrun) {
                case 1:
                    n = r;
                    r = c;
                    c = m_Game.m_cArray.length - 1 - n;
                    break;
                case 2:
                    r = m_Game.m_cArray.length - 1 - r;
                    c = m_Game.m_cArray[0].length - 1 - c;
                    break;
                case 3:
                    n = r;
                    r = m_Game.m_cArray[0].length - 1 - c;
                    c = n;
                    break;
                case 4:
                    c = m_Game.m_cArray[0].length - 1 - c;
                    break;
                case 5:
                    n = r;
                    r = m_Game.m_cArray[0].length - 1 - c;
                    c = m_Game.m_cArray.length - 1 - n;
                    break;
                case 6:
                    r = m_Game.m_cArray.length - 1 - r;
                    break;
                case 7:
                    n = r;
                    r = c;
                    c = n;
            }
        }

        int k = c / 26 + 64;
        if (k > 64) s.append((char) (byte) k);

        s.append((char) ((byte) (c % 26 + 65))).append(String.valueOf(1 + r));

        // 格子的奇偶性
        m_lEven = ((r + c) % 2 == 0);

        return s.toString();
    }

    public void Init(myGameView v) {
        m_Game = v;
    }


    ////////////////////////////////////////////////////////////////////////
    //辅助类定义
    ////////////////////////////////////////////////////////////////////////

    //阶梯节点
    class Ladder {
        int r, c;

        public Ladder(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    //计数节点，计数选区内的箱子和目标用
    class selNode {
        int r1, r2, c1, c2;

        public selNode(){
            r1 = 0;
            r2 = 0;
            c1 = 0;
            c2 = 0;
        }

        void setPT(byte[][] m_selArray, int r, int c) {
            if (r2 >= 0) {
                r1 = r;
                c1 = c;
                r2 = -1;
                c2 = -1;
            } else {
                if (r < r1) {
                    r2 = r1;
                    r1 = r;
                } else {
                    r2 = r;
                }
                if (c < c1) {
                    c2 = c1;
                    c1 = c;
                } else {
                    c2 = c;
                }
            }
            for (int i = r1; i <= r2; i++)
                for (int j = c1; j <= c2; j++)
                    m_selArray[i][j] = 1;

        }
    }
}
