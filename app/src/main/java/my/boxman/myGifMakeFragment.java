package my.boxman;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import my.boxman.gifencoder.GifEncoder;

public class myGifMakeFragment extends DialogFragment {

    public AsyncGifMakeTask mGifMakeTask;

    private WeakReference<myGifMakeFragment.GifMakeStatusUpdate> mStatusUpdate;

    private int m_Gif_Mark;
    private int m_Gif_Start;
    private boolean m_Type;
    private boolean m_Skin;
    private int m_Interval;
    private String  mMessage, m_Ans;
    private Bitmap my_Skin, my_Bitmap, my_Bitmap11, my_Bitmap12, my_Bitmap21, my_Bitmap13, my_Bitmap31;
    private Canvas my_Canvas, my_Canvas11, my_Canvas12, my_Canvas21, my_Canvas13, my_Canvas31;
    byte[] actArray = null;
    char[][] m_cArray;  //地图
    int m_nStep; //执行的答案步数
    int m_nRow, m_nCol;  //记录仓管员位置
    int m_PicWidth = 16;
    int[] m_Left_Top = {0, 0};  //后续帧图片的位置
    boolean[] my_Rule;
    short[] my_BoxNum0;
    short[][] my_BoxNum;

    GifEncoder gifEncoder = null;

    //接口，向上层传递参数
    interface GifMakeStatusUpdate {
        void onGifMakeDone(String inf);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mStatusUpdate = new WeakReference<GifMakeStatusUpdate>((GifMakeStatusUpdate)activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            m_Gif_Mark = bundle.getInt("m_Gif_Mark");
            m_Gif_Start = bundle.getInt("m_Gif_Start");
            m_Type = bundle.getBoolean("m_Type");
            m_Skin = bundle.getBoolean("m_Skin");
            my_Rule = bundle.getBooleanArray("my_Rule");  //需要显示标尺的格子
            my_BoxNum0 = bundle.getShortArray("my_BoxNum");  //迷宫箱子编号（自动编号与人工编号的关联数组）
            m_Interval = bundle.getInt("m_Interval");
            m_Ans = bundle.getString("m_Ans");
        } else {
            mGifMakeTask.stopGifMake();
        }

        if (m_Interval == 0) m_Type = false;

        setRetainInstance(true);

        if (m_Skin) {  //现场皮肤
            my_Skin = myMaps.skinBit;
            m_PicWidth = 50;
        } else {
            my_Skin = myMaps.skinGif;
            m_PicWidth = 16;
        }

        mGifMakeTask = new AsyncGifMakeTask();
        mGifMakeTask.execute();
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());

        if (mMessage == null) mMessage = "合成中...";

        progressDialog.setMessage(mMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return progressDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mGifMakeTask.stopGifMake();
    }

    private final class AsyncGifMakeTask extends AsyncTask<Void, String, String> {
        Rect rtW = new Rect();
        Rect rtF = new Rect();
        Rect rtD = new Rect();
        Rect rtB = new Rect();
        Rect rtBD = new Rect();
        Rect[] rtM = { new Rect(), new Rect(), new Rect(), new Rect() };
        Rect[] rtMD = { new Rect(), new Rect(), new Rect(), new Rect() };
        Rect rt = new Rect();
        Rect rt0 = new Rect();
        Rect rt1 = new Rect();
        Rect rt2 = new Rect();
        Paint myPaint = new Paint();

        void stopGifMake() {
            if (gifEncoder != null) gifEncoder.finish();

            if (mStatusUpdate != null) {
                GifMakeStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    if (mGifMakeTask != null && !mGifMakeTask.isCancelled () && mGifMakeTask.getStatus () == Status.RUNNING) {
                        mGifMakeTask.cancel (true);
                        mGifMakeTask = null;
                    }
                    statusUpdate.onGifMakeDone("放弃合成！");
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //地图初始化
            try {
                myPaint.setStyle (Paint.Style.FILL);
                if (m_Skin) {  //现场皮肤
                    rtF.set(0, m_PicWidth*5-myMaps.isSkin_200, m_PicWidth, m_PicWidth*6-myMaps.isSkin_200);  //地板
                    rtD.set(0, m_PicWidth*6-myMaps.isSkin_200, m_PicWidth, m_PicWidth*7-myMaps.isSkin_200);  //目标
                    rtB.set(m_PicWidth, m_PicWidth*5-myMaps.isSkin_200, m_PicWidth*2, m_PicWidth*6-myMaps.isSkin_200);   //箱子
                    rtBD.set(m_PicWidth, m_PicWidth*6-myMaps.isSkin_200, m_PicWidth*2, m_PicWidth*7-myMaps.isSkin_200);  //目标上的的箱子
                    if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200) {  //简单皮肤
                        rtM[1].set(m_PicWidth*2, m_PicWidth*5-myMaps.isSkin_200, m_PicWidth*3, m_PicWidth*6-myMaps.isSkin_200);
                        rtMD[1].set(m_PicWidth*2, m_PicWidth*6-myMaps.isSkin_200, m_PicWidth*3, m_PicWidth*7-myMaps.isSkin_200);
                        rtM[0] = rtM[1];
                        rtM[2] = rtM[1];
                        rtM[3] = rtM[1];
                        rtMD[0] = rtMD[1];
                        rtMD[2] = rtMD[1];
                        rtMD[3] = rtMD[1];
                    } else {
                        rtM[0].set(0, m_PicWidth*7, m_PicWidth, m_PicWidth*8);  //左
                        rtM[1].set(m_PicWidth*2, m_PicWidth*5, m_PicWidth*3, m_PicWidth*6);  //上
                        rtM[2].set(m_PicWidth*3, m_PicWidth*5, m_PicWidth*4, m_PicWidth*6);  //右
                        rtM[3].set(m_PicWidth*2, m_PicWidth*7, m_PicWidth*3, m_PicWidth*8);  //下

                        rtMD[0].set(m_PicWidth, m_PicWidth*7, m_PicWidth*2, m_PicWidth*8);   //左
                        rtMD[1].set(m_PicWidth*2, m_PicWidth*6, m_PicWidth*3, m_PicWidth*7);   //上
                        rtMD[2].set(m_PicWidth*3, m_PicWidth*6, m_PicWidth*4, m_PicWidth*7);   //右
                        rtMD[3].set(m_PicWidth*3, m_PicWidth*7, m_PicWidth*4, m_PicWidth*8);   //下
                    }
                } else {
                    rtW.set(m_PicWidth * 3, 0, m_PicWidth * 4, m_PicWidth);                //墙壁
                    rtF.set(0, 0, m_PicWidth, m_PicWidth);                                    //地板
                    rtD.set(0, m_PicWidth, m_PicWidth, m_PicWidth * 2);                      //目标
                    rtB.set(m_PicWidth, 0, m_PicWidth * 2, m_PicWidth);                         //箱子
                    rtBD.set(m_PicWidth, m_PicWidth, m_PicWidth * 2, m_PicWidth * 2);         //目标上的的箱子
                    rtM[1].set(m_PicWidth * 2, 0, m_PicWidth * 3, m_PicWidth);                  //人
                    rtMD[1].set(m_PicWidth * 2, m_PicWidth, m_PicWidth * 3, m_PicWidth * 2);  //目标上的人
                }
                m_nRow = -1;
                m_nCol = -1;
                m_cArray = new char[myMaps.curMap.Rows][myMaps.curMap.Cols];
                my_Bitmap = Bitmap.createBitmap(m_PicWidth * myMaps.curMap.Cols, m_PicWidth * myMaps.curMap.Rows, Bitmap.Config.ARGB_4444);
                my_Bitmap11 = Bitmap.createBitmap(m_PicWidth, m_PicWidth, Bitmap.Config.ARGB_4444);
                my_Bitmap12 = Bitmap.createBitmap(m_PicWidth, m_PicWidth * 2, Bitmap.Config.ARGB_4444);
                my_Bitmap21 = Bitmap.createBitmap(m_PicWidth * 2, m_PicWidth, Bitmap.Config.ARGB_4444);
                my_Bitmap13 = Bitmap.createBitmap(m_PicWidth, m_PicWidth * 3, Bitmap.Config.ARGB_4444);
                my_Bitmap31 = Bitmap.createBitmap(m_PicWidth * 3, m_PicWidth, Bitmap.Config.ARGB_4444);
                my_Canvas = new Canvas(my_Bitmap);
                my_Canvas11 = new Canvas(my_Bitmap11);
                my_Canvas12 = new Canvas(my_Bitmap12);
                my_Canvas21 = new Canvas(my_Bitmap21);
                my_Canvas13 = new Canvas(my_Bitmap13);
                my_Canvas31 = new Canvas(my_Bitmap31);
                my_BoxNum = new short[myMaps.curMap.Rows][myMaps.curMap.Cols];

                String[] Arr = myMaps.curMap.Map.split("\r\n|\n\r|\n|\r|\\|");
                char ch;
                int n = 0;
                for (int i = 0; i < myMaps.curMap.Rows; i++) {
                    for (int j = 0; j < myMaps.curMap.Cols; j++) {
                        ch = Arr[i].charAt(j);
                        my_BoxNum[i][j] = -1;  //为方便处理，将全部没有人工箱子编号的格子，赋值为 -1
                        switch (ch) {
                            case '+':
                            case '@':
                                m_nRow = i;
                                m_nCol = j;
                            case '#':
                            case '-':
                            case '.':
                                m_cArray[i][j] = ch;
                                break;
                            case '$':
                            case '*':
                                m_cArray[i][j] = ch;
                                //利用自动编号，转换到人工编号
                                if (m_Skin && my_BoxNum0 != null)
                                    my_BoxNum[i][j] = my_BoxNum0[n++];
                                break;
                            default:
                                m_cArray[i][j] = '_';
                        }
                    }
                }
                if (m_nRow < 0 || m_nRow >= myMaps.curMap.Rows || m_nCol < 0 || m_nCol >= myMaps.curMap.Cols) mGifMakeTask.stopGifMake();
            } catch (Throwable ex) {
                mGifMakeTask.stopGifMake();
            }

        }

        @Override
        protected String doInBackground(Void... params) {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");//设置日期格式
            String fn = df.format(new Date ())+".gif";  // new Date()获取当前系统时间

            try {

                actArray = formatPath(m_Ans);  //格式化答案

                gifEncoder = new GifEncoder ();
                gifEncoder.start (myMaps.sRoot + myMaps.sPath + "GIF/" + fn);

                m_nStep = 0;
                while (m_nStep < m_Gif_Start){  //定位到动画开始点（推关卡时，长按“仓管员”可以选择动画开始点，然后，可导出 GIF 片段）
                    myMove2();
                }

                gifEncoder.getSkinPixels(my_Skin);

                gifEncoder.setDelay (1000);  //第一帧的帧间隔

                int m = 0;
                gifEncoder.addFrame(myDraw());  //添加动画第一帧
                while (m_nStep < actArray.length) {
                    if (isCancelled()) return "放弃合成！";
                    if (m_Type) {      //仅关键帧
                        gifEncoder.setDelay (0);  //前半帧延时
                        gifEncoder.addFrame (myDraw2(false), m_Left_Top[0], m_Left_Top[1]);  //移动前，先擦除原位置上的元素
                        do {
                            myMove2();
                        } while (m_nStep < actArray.length && (actArray[m_nStep] < 5 && actArray[m_nStep - 1] < 5 || actArray[m_nStep] > 4 && actArray[m_nStep] == actArray[m_nStep - 1]));
                        if (m_nStep > actArray.length-1) gifEncoder.setDelay (m_Interval+2000);  //结束帧延时
                        else gifEncoder.setDelay (m_Interval);  //后半帧延时
                        gifEncoder.addFrame (myDraw2(true), m_Left_Top[0], m_Left_Top[1]);  //添加动画后续帧（移动后的现场）
                    } else {           //全部帧
                        if (m <= 0) m = getStep(actArray, m_nStep);
                        if (m_Interval == 0) {
                            if (m_nStep >= actArray.length-1) gifEncoder.setDelay (3000);  //结束帧延时
                            else if (m == 1) gifEncoder.setDelay (2000);  //关键帧延时
                            else {
                                if (actArray[m_nStep] < 5) gifEncoder.setDelay(50);  //帧延时
                                else gifEncoder.setDelay(100);  //帧延时
                            }
                        } else {
                            if (m_nStep >= actArray.length - 1) gifEncoder.setDelay(m_Interval + 2000);  //结束帧延时
                            else gifEncoder.setDelay(m_Interval);  //帧延时
                        }
                        m--;
                        gifEncoder.addFrame (myMove(), m_Left_Top[0], m_Left_Top[1]);  //添加动画后续帧
                    }
                    publishProgress ("合成中..." + (m_nStep) + "/" + actArray.length);
                }

                gifEncoder.finish();

            } catch (Exception e) { }

            return "制作完成！\n" + "GIF/" + fn;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mMessage = progress[0];
            ProgressDialog progressDialog = ((ProgressDialog) myGifMakeFragment.this.getDialog());
            if (progressDialog != null) {
                progressDialog.setMessage(mMessage);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (mStatusUpdate != null) {
                GifMakeStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    statusUpdate.onGifMakeDone(result);
                }
            }
        }

        private Bitmap myDraw() {
            for (int i = 0; i < myMaps.curMap.Rows; i++) {
                for (int j = 0; j < myMaps.curMap.Cols; j++) {
                    rt.left = m_PicWidth * j;
                    rt.top = m_PicWidth * i;
                    rt.right = rt.left + m_PicWidth;
                    rt.bottom = rt.top + m_PicWidth;

                    switch (m_cArray[i][j]) {
                        case '#':
                            if (m_Skin) rtW = getWall(rtW, i, j);  //现场皮肤，计算使用哪个“墙”图
                            my_Canvas.drawBitmap(my_Skin, rtW, rt, myPaint);
                            break;
                        case '-':
                            my_Canvas.drawBitmap(my_Skin, rtF, rt, myPaint);
                            break;
                        case '.':
                            if (m_Skin) my_Canvas.drawBitmap(my_Skin, rtF, rt, myPaint);  //现场皮肤先画地板
                            my_Canvas.drawBitmap(my_Skin, rtD, rt, myPaint);
                            break;
                        case '$':
                            if (m_Skin) my_Canvas.drawBitmap(my_Skin, rtF, rt, myPaint);  //现场皮肤先画地板
                            my_Canvas.drawBitmap(my_Skin, rtB, rt, myPaint);
                            break;
                        case '*':
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas.drawBitmap(my_Skin, rtF, rt, myPaint);
                                my_Canvas.drawBitmap(my_Skin, rtD, rt, myPaint);
                            }
                            my_Canvas.drawBitmap(my_Skin, rtBD, rt, myPaint);
                            break;
                        case '@':
                            if (m_Skin) my_Canvas.drawBitmap(my_Skin, rtF, rt, myPaint);  //现场皮肤先画地板
                            my_Canvas.drawBitmap(my_Skin, rtM[1], rt, myPaint);
                            break;
                        case '+':
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas.drawBitmap(my_Skin, rtF, rt, myPaint);
                                my_Canvas.drawBitmap(my_Skin, rtD, rt, myPaint);
                            }
                            my_Canvas.drawBitmap(my_Skin, rtMD[1], rt, myPaint);
                            break;
                    } //end switch

                    if (m_Skin && my_Rule != null) {  //若使用了现场皮肤
                        if (my_BoxNum[i][j] >= 0)
                            drawBoxNum(my_Canvas, rt, my_BoxNum[i][j]);        //若有箱子编号
                        else if (my_Rule[i * myMaps.curMap.Cols + j])
                            drawRule(my_Canvas, rt, i, j);     //若有标尺
                    }
                }
            }
            if (m_Skin) {
                if (m_Gif_Mark == 1 && myMaps.markGif1 != null) my_Canvas.drawBitmap(myMaps.markGif1, 8, my_Bitmap.getHeight()-myMaps.markGif1.getHeight()-4, myPaint);
                else if (m_Gif_Mark == 2 && myMaps.markGif2 != null) my_Canvas.drawBitmap(myMaps.markGif2, 8, my_Bitmap.getHeight()-myMaps.markGif2.getHeight()-4, myPaint);
            } else {
                if (m_Gif_Mark > 0) {
                    myPaint.setTextSize (m_PicWidth-4);
                    myPaint.setARGB (255, 255, 255, 255);
                    myPaint.setStrokeWidth (1);
                    //选择默认水印或自定义水印时没有玩家id（提交比赛答案的id）
                    if (m_Gif_Mark == 1 || myMaps.nickname.trim ().isEmpty ()) my_Canvas.drawText ("92017135-Q群", 2, my_Bitmap.getHeight () - 4, myPaint);
                    else my_Canvas.drawText ("by " + myMaps.nickname, 2, my_Bitmap.getHeight () - 4, myPaint);
                }
            }
            return my_Bitmap;
        }

        //仅关键帧时，以小图片为范围，画上移动后的现场（不需要透明色，分两步：一步是擦除移动前的原位置元素，第二步是后移动后的现场）
        private Bitmap myDraw2(boolean isMoved) {
            Bitmap img = null;
            byte m_Dir = isMoved ? actArray[m_nStep-1] : actArray[m_nStep];
            switch (m_Dir) {
                case 1:
                case 2:
                case 3:
                case 4:
                    img = my_Bitmap11;                                        //单格子图片
                    rt.set (0, 0, m_PicWidth, m_PicWidth);
                    if (isMoved) {                                           //移动后图片
                        if (m_cArray[m_nRow][m_nCol] == '@') {
                            if (m_Skin) {   //现场皮肤先画地板、目标的
                                my_Canvas11.drawBitmap(my_Skin, rtF, rt, myPaint);
                                my_Canvas11.drawBitmap(my_Skin, rtM[m_Dir-1], rt, myPaint);
                            } else my_Canvas11.drawBitmap(my_Skin, rtM[1], rt, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas11.drawBitmap(my_Skin, rtF, rt, myPaint);
                                my_Canvas11.drawBitmap(my_Skin, rtD, rt, myPaint);
                                my_Canvas11.drawBitmap(my_Skin, rtMD[m_Dir-1], rt, myPaint);
                            }else my_Canvas11.drawBitmap(my_Skin, rtMD[1], rt, myPaint);
                        }
                    } else {                                                 //移动前图片
                        if (m_cArray[m_nRow][m_nCol] == '@') my_Canvas11.drawBitmap(my_Skin, rtF, rt, myPaint);
                        else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas11.drawBitmap(my_Skin, rtF, rt, myPaint);
                            my_Canvas11.drawBitmap(my_Skin, rtD, rt, myPaint);
                        }
                        if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                            drawRule(my_Canvas11, rt, m_nRow, m_nCol);     //若有标尺
                        }
                    }
                    m_Left_Top[0] = m_PicWidth * m_nCol;  //帧的绘制位置
                    m_Left_Top[1] = m_PicWidth * m_nRow;
                    break;
                case 5:  //左推
                    img = my_Bitmap21;                                           //左右格子图片
                    rt1.set (0, 0, m_PicWidth, m_PicWidth);                       //左格子
                    rt2.set (m_PicWidth, 0, m_PicWidth*2, m_PicWidth);           //右格子

                    if (isMoved) {  //移动后图片
                        //右格子
                        if (m_cArray[m_nRow][m_nCol] == '@') {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtM[0], rt2, myPaint);
                            } else my_Canvas21.drawBitmap(my_Skin, rtM[1], rt2, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtD, rt2, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtMD[0], rt2, myPaint);
                            } else my_Canvas21.drawBitmap(my_Skin, rtMD[1], rt2, myPaint);
                        }

                        //左格子
                        if (m_cArray[m_nRow][m_nCol-1] == '$') {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtB, rt1, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            }
                            my_Canvas21.drawBitmap(my_Skin, rtBD, rt1, myPaint);
                        }
                        if (m_Skin && my_Rule != null && my_BoxNum[m_nRow][m_nCol-1] >= 0) {  //若使用了现场皮肤
                            drawBoxNum(my_Canvas21, rt1, my_BoxNum[m_nRow][m_nCol-1]);     //若有箱子编号
                        }
                    } else {  //移动前图片
                        //右格子
                        if (m_cArray[m_nRow][m_nCol] == '@') {
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        } else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }

                        //左格子
                        if (m_cArray[m_nRow][m_nCol-1] == '$') {
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                        } else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtD, rt1, myPaint);
                        }
                        if (m_Skin && my_Rule != null) {  //若使用了现场皮肤
                            if (my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) drawRule(my_Canvas21, rt2, m_nRow, m_nCol);     //若有标尺
                            if (my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol-1]) drawRule(my_Canvas21, rt1, m_nRow, m_nCol-1);     //若有标尺
                        }
                    }
                    m_Left_Top[0] = m_PicWidth * (m_nCol-1);  //帧坐标
                    m_Left_Top[1] = m_PicWidth * m_nRow;

                    break;
                case 7:  //右推
                    img = my_Bitmap21;                                           //左右格子图片
                    rt1.set (0, 0, m_PicWidth, m_PicWidth);                       //左格子
                    rt2.set (m_PicWidth, 0, m_PicWidth*2, m_PicWidth);           //右格子

                    if (isMoved) {  //移动后图片
                        //左格子
                        if (m_cArray[m_nRow][m_nCol] == '@') {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtM[2], rt1, myPaint);
                            } else my_Canvas21.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtD, rt1, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtMD[2], rt1, myPaint);
                            } else my_Canvas21.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                        }

                        //右格子
                        if (m_cArray[m_nRow][m_nCol+1] == '$') {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtB, rt2, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                                my_Canvas21.drawBitmap(my_Skin, rtD, rt2, myPaint);
                            }
                            my_Canvas21.drawBitmap(my_Skin, rtBD, rt2, myPaint);
                        }
                        if (m_Skin && my_Rule != null && my_BoxNum[m_nRow][m_nCol+1] >= 0) {  //若使用了现场皮肤
                            drawBoxNum(my_Canvas21, rt2, my_BoxNum[m_nRow][m_nCol+1]);     //若有箱子编号
                        }
                    } else {  //移动前图片
                        //左格子
                        if (m_cArray[m_nRow][m_nCol] == '@') my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                        else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtD, rt1, myPaint);
                        }

                        //右格子
                        if (m_cArray[m_nRow][m_nCol+1] == '$') my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas21.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }
                        if (m_Skin && my_Rule != null) {  //若使用了现场皮肤
                            if (my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) drawRule(my_Canvas21, rt1, m_nRow, m_nCol);     //若有标尺
                            if (my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol+1]) drawRule(my_Canvas21, rt2, m_nRow, m_nCol+1);     //若有标尺
                        }
                    }
                    m_Left_Top[0] = m_PicWidth * m_nCol;  //帧坐标
                    m_Left_Top[1] = m_PicWidth * m_nRow;
                    break;
                case 6:  //上推
                    img = my_Bitmap12;                                           //上下格子图片
                    rt1.set (0, 0, m_PicWidth, m_PicWidth);                       //上格子
                    rt2.set (0, m_PicWidth, m_PicWidth, m_PicWidth*2);         //下格子

                    if (isMoved) {  //移动后图片
                        //下格子
                        if (m_cArray[m_nRow][m_nCol] == '@') {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtM[1], rt2, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                                my_Canvas12.drawBitmap(my_Skin, rtD, rt2, myPaint);
                            }
                            my_Canvas12.drawBitmap(my_Skin, rtMD[1], rt2, myPaint);
                        }

                        //上格子
                        if (m_cArray[m_nRow-1][m_nCol] == '$') {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtB, rt1, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                                my_Canvas12.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            }
                            my_Canvas12.drawBitmap(my_Skin, rtBD, rt1, myPaint);
                        }
                        if (m_Skin && my_Rule != null && my_BoxNum[m_nRow-1][m_nCol] >= 0) {  //若使用了现场皮肤
                            drawBoxNum(my_Canvas12, rt1, my_BoxNum[m_nRow-1][m_nCol]);     //若有箱子编号
                        }
                    } else {  //移动前图片
                        //下格子
                        if (m_cArray[m_nRow][m_nCol] == '@') my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }

                        //上格子
                        if (m_cArray[m_nRow-1][m_nCol] == '$') my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                        else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtD, rt1, myPaint);
                        }
                        if (m_Skin && my_Rule != null) {  //若使用了现场皮肤
                            if (my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) drawRule(my_Canvas12, rt2, m_nRow, m_nCol);     //若有标尺
                            if (my_Rule[(m_nRow-1) * myMaps.curMap.Cols + m_nCol]) drawRule(my_Canvas12, rt1, m_nRow-1, m_nCol);     //若有标尺
                        }
                    }
                    m_Left_Top[0] = m_PicWidth * m_nCol;  //帧坐标
                    m_Left_Top[1] = m_PicWidth * (m_nRow-1);
                    break;
                case 8:  //下推
                    img = my_Bitmap12;                                           //上下格子图片
                    rt1.set (0, 0, m_PicWidth, m_PicWidth);                       //上格子
                    rt2.set (0, m_PicWidth, m_PicWidth, m_PicWidth*2);         //下格子

                    if (isMoved) {  //移动后图片
                        //上格子
                        if (m_cArray[m_nRow][m_nCol] == '@') {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                                my_Canvas12.drawBitmap(my_Skin, rtM[3], rt1, myPaint);
                            } else my_Canvas12.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                                my_Canvas12.drawBitmap(my_Skin, rtD, rt1, myPaint);
                                my_Canvas12.drawBitmap(my_Skin, rtMD[3], rt1, myPaint);
                            } else my_Canvas12.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                        }

                        //下格子
                        if (m_cArray[m_nRow+1][m_nCol] == '$') {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtB, rt2, myPaint);
                        } else {
                            if (m_Skin) {  //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                                my_Canvas12.drawBitmap(my_Skin, rtD, rt2, myPaint);
                            }
                            my_Canvas12.drawBitmap(my_Skin, rtBD, rt2, myPaint);
                        }
                        if (m_Skin && my_Rule != null && my_BoxNum[m_nRow+1][m_nCol] >= 0) {  //若使用了现场皮肤
                            drawBoxNum(my_Canvas12, rt2, my_BoxNum[m_nRow+1][m_nCol]);     //若有箱子编号
                        }
                    } else {  //移动前图片
                        //上格子
                        if (m_cArray[m_nRow][m_nCol] == '@') my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                        else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtD, rt1, myPaint);
                        }

                        //下格子
                        if (m_cArray[m_nRow+1][m_nCol] == '$') my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        else {
                            if (m_Skin)   //现场皮肤先画地板、目标的
                                my_Canvas12.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }
                        if (m_Skin && my_Rule != null) {  //若使用了现场皮肤
                            if (my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) drawRule(my_Canvas12, rt1, m_nRow, m_nCol);     //若有标尺
                            if (my_Rule[(m_nRow+1) * myMaps.curMap.Cols + m_nCol]) drawRule(my_Canvas12, rt2, m_nRow+1, m_nCol);     //若有标尺
                        }
                    }
                    m_Left_Top[0] = m_PicWidth * m_nCol;  //帧坐标
                    m_Left_Top[1] = m_PicWidth * m_nRow;
                    break;
            }
            return img;
        }

        //合成所有帧时，移动一步后的图片
        private Bitmap myMove() {
            Bitmap img = null;
            switch (actArray[m_nStep]) {
                case 1:  //左
                    img = my_Bitmap21;

                    rt.set (m_PicWidth, 0, m_PicWidth*2, m_PicWidth);  //仓管员原位置
                    rt1.set (0, 0, m_PicWidth, m_PicWidth);              //仓管员新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas21.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas21.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas21, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_nCol--;
                    m_Left_Top[0] = m_PicWidth * m_nCol;
                    m_Left_Top[1] = m_PicWidth * m_nRow;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin) {  //现场皮肤先画地板
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtM[0], rt1, myPaint);
                        } else {
                            my_Canvas21.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                        }
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtMD[0], rt1, myPaint);
                        } else {
                            my_Canvas21.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                        }
                    }
                    break;
                case 3:  //右
                    img = my_Bitmap21;

                    rt.set (0, 0, m_PicWidth, m_PicWidth);              //仓管员原位置
                    rt1.set (m_PicWidth, 0, m_PicWidth*2, m_PicWidth);  //仓管员新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas21.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas21.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas21, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_Left_Top[0] = m_PicWidth * m_nCol;
                    m_Left_Top[1] = m_PicWidth * m_nRow;
                    m_nCol++;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin) {  //现场皮肤先画地板
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtM[2], rt1, myPaint);
                        } else my_Canvas21.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas21.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            my_Canvas21.drawBitmap(my_Skin, rtMD[2], rt1, myPaint);
                        } else my_Canvas21.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                    }
                    break;
                case 2:  //上
                    img = my_Bitmap12;

                    rt.set (0, m_PicWidth, m_PicWidth, m_PicWidth*2);  //仓管员原位置
                    rt1.set (0, 0, m_PicWidth, m_PicWidth);               //仓管员新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas12.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas12.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas12.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas12, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_nRow--;
                    m_Left_Top[0] = m_PicWidth * m_nCol;
                    m_Left_Top[1] = m_PicWidth * m_nRow;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                        my_Canvas12.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtD, rt1, myPaint);
                        }
                        my_Canvas12.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                    }
                    break;
                case 4:  //下
                    img = my_Bitmap12;

                    rt.set (0, 0, m_PicWidth, m_PicWidth);                 //仓管员原位置
                    rt1.set (0, m_PicWidth, m_PicWidth, m_PicWidth*2);  //仓管员新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas12.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas12.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas12.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas12, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_Left_Top[0] = m_PicWidth * m_nCol;
                    m_Left_Top[1] = m_PicWidth * m_nRow;
                    m_nRow++;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin) {  //现场皮肤先画地板
                            my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtM[3], rt1, myPaint);
                        } else my_Canvas12.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas12.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            my_Canvas12.drawBitmap(my_Skin, rtMD[3], rt1, myPaint);
                        } else my_Canvas12.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                    }
                    break;
                case 5:  //左推
                    img = my_Bitmap31;

                    rt.set (m_PicWidth*2, 0, m_PicWidth*3, m_PicWidth);   //仓管员原位置
                    rt1.set (m_PicWidth, 0, m_PicWidth*2, m_PicWidth);          //仓管员新位置
                    rt2.set (0, 0, m_PicWidth, m_PicWidth);                       //箱子新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas31.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas31.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas31, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_nCol--;
                    my_BoxNum[m_nRow][m_nCol-1] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;
                    m_Left_Top[0] = m_PicWidth * (m_nCol-1);
                    m_Left_Top[1] = m_PicWidth * (m_nRow);

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin) {  //现场皮肤先画地板
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtM[0], rt1, myPaint);
                        } else my_Canvas31.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtMD[0], rt1, myPaint);
                        } else my_Canvas31.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                    }
                    if (m_cArray[m_nRow][m_nCol-1] == '-') {
                        m_cArray[m_nRow][m_nCol-1] = '$';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        my_Canvas31.drawBitmap(my_Skin, rtB, rt2, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol-1] = '*';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }
                        my_Canvas31.drawBitmap(my_Skin, rtBD, rt2, myPaint);
                    }
                    if (m_Skin && my_Rule != null && my_BoxNum[m_nRow][m_nCol-1] >= 0) {  //若使用了现场皮肤
                        drawBoxNum(my_Canvas31, rt2, my_BoxNum[m_nRow][m_nCol-1]);     //若有箱子编号
                    }
                    break;
                case 7:  //右推
                    img = my_Bitmap31;

                    rt.set (0, 0, m_PicWidth, m_PicWidth);                       //仓管员原位置
                    rt1.set (m_PicWidth, 0, m_PicWidth*2, m_PicWidth);          //仓管员新位置
                    rt2.set (m_PicWidth*2, 0, m_PicWidth*3, m_PicWidth);   //箱子新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas31.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas31.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas31, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_Left_Top[0] = m_PicWidth * m_nCol;
                    m_Left_Top[1] = m_PicWidth * m_nRow;
                    m_nCol++;
                    my_BoxNum[m_nRow][m_nCol+1] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin) {  //现场皮肤先画地板
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtM[2], rt1, myPaint);
                        } else my_Canvas31.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtMD[2], rt1, myPaint);
                        } else my_Canvas31.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                    }
                    if (m_cArray[m_nRow][m_nCol+1] == '-') {
                        m_cArray[m_nRow][m_nCol+1] = '$';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        my_Canvas31.drawBitmap(my_Skin, rtB, rt2, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol+1] = '*';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas31.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas31.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }
                        my_Canvas31.drawBitmap(my_Skin, rtBD, rt2, myPaint);
                    }
                    if (m_Skin && my_Rule != null && my_BoxNum[m_nRow][m_nCol+1] >= 0) {  //若使用了现场皮肤
                        drawBoxNum(my_Canvas31, rt2, my_BoxNum[m_nRow][m_nCol+1]);     //若有箱子编号
                    }
                    break;
                case 6:  //上推
                    img = my_Bitmap13;

                    rt.set (0, m_PicWidth*2, m_PicWidth, m_PicWidth*3);  //仓管员原位置
                    rt1.set (0, m_PicWidth, m_PicWidth, m_PicWidth*2);         //仓管员新位置
                    rt2.set (0, 0, m_PicWidth, m_PicWidth);                        //箱子新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas13.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas13.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas13, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_nRow--;
                    my_BoxNum[m_nRow-1][m_nCol] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;
                    m_Left_Top[0] = m_PicWidth * (m_nCol);
                    m_Left_Top[1] = m_PicWidth * (m_nRow-1);

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt1, myPaint);
                        my_Canvas13.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas13.drawBitmap(my_Skin, rtD, rt1, myPaint);
                        }
                        my_Canvas13.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                    }
                    if (m_cArray[m_nRow-1][m_nCol] == '-') {
                        m_cArray[m_nRow-1][m_nCol] = '$';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        my_Canvas13.drawBitmap(my_Skin, rtB, rt2, myPaint);
                    } else {
                        m_cArray[m_nRow-1][m_nCol] = '*';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas13.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }
                        my_Canvas13.drawBitmap(my_Skin, rtBD, rt2, myPaint);
                    }
                    if (m_Skin && my_Rule != null && my_BoxNum[m_nRow-1][m_nCol] >= 0) {  //若使用了现场皮肤
                        drawBoxNum(my_Canvas13, rt2, my_BoxNum[m_nRow-1][m_nCol]);     //若有箱子编号
                    }
                    break;
                case 8:  //下推
                    img = my_Bitmap13;

                    rt.set (0, 0, m_PicWidth, m_PicWidth);                        //仓管员原位置
                    rt1.set (0, m_PicWidth, m_PicWidth, m_PicWidth*2);         //仓管员新位置
                    rt2.set (0, m_PicWidth*2, m_PicWidth, m_PicWidth*3);  //箱子新位置

                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                        my_Canvas13.drawBitmap(my_Skin, rtF, rt, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt, myPaint);
                        my_Canvas13.drawBitmap(my_Skin, rtD, rt, myPaint);
                    }

                    if (m_Skin && my_Rule != null && my_Rule[m_nRow * myMaps.curMap.Cols + m_nCol]) {  //若使用了现场皮肤
                        drawRule(my_Canvas13, rt, m_nRow, m_nCol);     //若有标尺
                    }

                    m_Left_Top[0] = m_PicWidth * m_nCol;
                    m_Left_Top[1] = m_PicWidth * m_nRow;
                    m_nRow++;
                    my_BoxNum[m_nRow+1][m_nCol] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                        if (m_Skin) {  //现场皮肤先画地板
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas13.drawBitmap(my_Skin, rtM[3], rt1, myPaint);
                        } else my_Canvas13.drawBitmap(my_Skin, rtM[1], rt1, myPaint);
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt1, myPaint);
                            my_Canvas13.drawBitmap(my_Skin, rtD, rt1, myPaint);
                            my_Canvas13.drawBitmap(my_Skin, rtMD[3], rt1, myPaint);
                        } else my_Canvas13.drawBitmap(my_Skin, rtMD[1], rt1, myPaint);
                    }
                    if (m_cArray[m_nRow+1][m_nCol] == '-') {
                        m_cArray[m_nRow+1][m_nCol] = '$';
                        if (m_Skin)   //现场皮肤先画地板
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt2, myPaint);
                        my_Canvas13.drawBitmap(my_Skin, rtB, rt2, myPaint);
                    } else {
                        m_cArray[m_nRow+1][m_nCol] = '*';
                        if (m_Skin) {  //现场皮肤先画地板、目标点
                            my_Canvas13.drawBitmap(my_Skin, rtF, rt2, myPaint);
                            my_Canvas13.drawBitmap(my_Skin, rtD, rt2, myPaint);
                        }
                        my_Canvas13.drawBitmap(my_Skin, rtBD, rt2, myPaint);
                    }
                    if (m_Skin && my_Rule != null && my_BoxNum[m_nRow+1][m_nCol] >= 0) {  //若使用了现场皮肤
                        drawBoxNum(my_Canvas13, rt2, my_BoxNum[m_nRow+1][m_nCol]);     //若有箱子编号
                    }
                    break;
            }
            m_nStep++;
            return img;
        }

        //仅修改移动后的地图 xsb
        private void myMove2() {
            switch (actArray[m_nStep]) {
                case 1:  //左
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nCol--;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    break;
                case 3:  //右
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nCol++;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    break;
                case 2:  //上
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nRow--;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    break;
                case 4:  //下
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nRow++;

                    if (m_cArray[m_nRow][m_nCol] == '-') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    break;
                case 5:  //左推
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nCol--;
                    my_BoxNum[m_nRow][m_nCol-1] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    if (m_cArray[m_nRow][m_nCol-1] == '-') {
                        m_cArray[m_nRow][m_nCol-1] = '$';
                    } else {
                        m_cArray[m_nRow][m_nCol-1] = '*';
                    }
                    break;
                case 7:  //右推
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nCol++;
                    my_BoxNum[m_nRow][m_nCol+1] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    if (m_cArray[m_nRow][m_nCol+1] == '-') {
                        m_cArray[m_nRow][m_nCol+1] = '$';
                    } else {
                        m_cArray[m_nRow][m_nCol+1] = '*';
                    }
                    break;
                case 6:  //上推
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nRow--;
                    my_BoxNum[m_nRow-1][m_nCol] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    if (m_cArray[m_nRow-1][m_nCol] == '-') {
                        m_cArray[m_nRow-1][m_nCol] = '$';
                    } else {
                        m_cArray[m_nRow-1][m_nCol] = '*';
                    }
                    break;
                case 8:  //下推
                    if (m_cArray[m_nRow][m_nCol] == '@') {
                        m_cArray[m_nRow][m_nCol] = '-';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '.';
                    }

                    m_nRow++;
                    my_BoxNum[m_nRow+1][m_nCol] = my_BoxNum[m_nRow][m_nCol];
                    my_BoxNum[m_nRow][m_nCol] = -1;

                    if (m_cArray[m_nRow][m_nCol] == '$') {
                        m_cArray[m_nRow][m_nCol] = '@';
                    } else {
                        m_cArray[m_nRow][m_nCol] = '+';
                    }
                    if (m_cArray[m_nRow+1][m_nCol] == '-') {
                        m_cArray[m_nRow+1][m_nCol] = '$';
                    } else {
                        m_cArray[m_nRow+1][m_nCol] = '*';
                    }
                    break;
            }
            m_nStep++;
        }

        //计算使用哪个墙壁图片
        private Rect getWall(Rect rt, int r, int c) {

            if (myMaps.isSkin_200 == 200) {
                rt.set(0, 0, 50, 50);
                return rt;
            }

            int bz = 0;

            //看看哪个方向上有“墙”
            if (c > 0 && m_cArray[r][c - 1] == '#') bz |= 1; //左
            if (r > 0 && m_cArray[r - 1][c] == '#') bz |= 2; //上
            if (c < m_cArray[0].length - 1 && m_cArray[r][c + 1] == '#') bz |= 4; //右
            if (r < m_cArray.length - 1 && m_cArray[r + 1][c] == '#') bz |= 8; //下

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

        //计算箱子编号
        private String mGetBoxNum(int num) {
            //人为编号：A -- Z, A0--A9 ～ Z0--Z9, 共26 + 260 = 286个编号
            if (num > 25) {  // 超过26，使用字母 + 数字
                int n0 = ((num - 25) % 10 + 9) % 10, n1 = (num - 26) / 10;  // 使字母后面的数字错从 0 到 9（否则是1、2、3...9、0）
                return new StringBuilder().append((char) ((byte) (n1 % 26 + 65))).append(String.valueOf(n0)).toString();
            } else {  // 26以内，使用26个单字母
                return new StringBuilder().append((char) ((byte) (num + 65))).toString();
            }
        }

        //画箱子编号
        private void drawBoxNum(Canvas canvas, Rect rt, int num) {
            String str = String.valueOf(myMaps.m_bBianhao ? num : mGetBoxNum(num));
            myPaint.setTextSize(m_PicWidth / 2);
            myPaint.getTextBounds(str, 0, str.length(), rt0);  //文字框
            myPaint.setARGB(255, 255, 255, 255);
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setStrokeWidth(3);
            canvas.drawText(str, rt.left + (m_PicWidth - rt0.width()) / 2, rt.bottom - (m_PicWidth - rt0.height()) / 2, myPaint);
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStrokeWidth(2);
            canvas.drawText(str, rt.left + (m_PicWidth - rt0.width()) / 2, rt.bottom - (m_PicWidth - rt0.height()) / 2, myPaint);
        }

        //根据位置，计算标尺
        private String mGetRule(int r, int c) {

            StringBuilder s = new StringBuilder();

            int k = c / 26 + 64;
            if (k > 64) s.append((char) (byte) k);

            s.append((char) ((byte) (c % 26 + 65))).append(String.valueOf(1 + r));

            return s.toString();
        }

        //画标尺
        private void drawRule(Canvas canvas, Rect rt, int r, int c) {
            String mStr = mGetRule(r, c);
            myPaint.setTextSize(m_PicWidth / 2);
            myPaint.getTextBounds(mStr, 0, mStr.length(), rt0);  //标尺文字框
            myPaint.setARGB(255, 0, 0, 0);
            myPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            myPaint.setStrokeWidth(3);
            canvas.drawText(mStr, rt.left + (m_PicWidth - rt0.width()) / 2, rt.bottom - (m_PicWidth - rt0.height()) / 2, myPaint);
            myPaint.setARGB(255, 255, 255, 255);
            myPaint.setStrokeWidth(1);
            canvas.drawText(mStr, rt.left + (m_PicWidth - rt0.width()) / 2, rt.bottom - (m_PicWidth - rt0.height()) / 2, myPaint);
        }

        //格式化路径
        private byte[] formatPath(String strPath) {
            int Len = strPath.length();
            if (Len > 0) {
                byte[] actArray = new byte[Len];

                for (int t = 0; t < Len; t++) {
                    switch (strPath.charAt(t)) {
                        case 'l':
                            actArray[t] = 1;
                            break;
                        case 'u':
                            actArray[t] = 2;
                            break;
                        case 'r':
                            actArray[t] = 3;
                            break;
                        case 'd':
                            actArray[t] = 4;
                            break;
                        case 'L':
                            actArray[t] = 5;
                            break;
                        case 'U':
                            actArray[t] = 6;
                            break;
                        case 'R':
                            actArray[t] = 7;
                            break;
                        case 'D':
                            actArray[t] = 8;
                            break;
                    }
                }
                return actArray;
            }
            return null;
        }

        //解析动作节点 -- 每推一个箱子为一个动作
        private int getStep(byte[] actArray, int mCur) {
            int len = actArray.length;

            int[] boxRC = {1000, 1000};
            int i = 0, j = 0;
            byte mDir;

            //寻找动作节点
            int n = 0, k = 0;;  //应该停在第几个动作上
            boolean flg = false;

            for (int t = mCur; t < len; t++) {
                mDir = actArray[t];
                k++;
                switch (mDir) {
                    case 1:  //左移
                        j--;
                        break;
                    case 2:  //上移
                        i--;
                        break;
                    case 3:  //右移
                        j++;
                        break;
                    case 4:  //下移
                        i++;
                        break;
                    case 5:  //左推
                        j--;

                        if (boxRC[0] != i || boxRC[1] != j) {
                            if (flg) return n;  //第二个箱子
                            flg = true;         //第一个箱子
                        }
                        n = k;                  //第一个箱子的最后位置

                        boxRC[0] = i;
                        boxRC[1] = j - 1;
                        break;
                    case 6:  //上推
                        i--;

                        if (boxRC[0] != i || boxRC[1] != j) {
                            if (flg) return n;  //第二个箱子
                            flg = true;         //第一个箱子
                        }
                        n = k;                  //第一个箱子的最后位置

                        boxRC[0] = i - 1;
                        boxRC[1] = j;
                        break;
                    case 7:  //右推
                        j++;

                        if (boxRC[0] != i || boxRC[1] != j) {
                            if (flg) return n;  //第二个箱子
                            flg = true;         //第一个箱子
                        }
                        n = k;                  //第一个箱子的最后位置

                        boxRC[0] = i;
                        boxRC[1] = j + 1;
                        break;
                    case 8:  //下推
                        i++;

                        if (boxRC[0] != i || boxRC[1] != j) {
                            if (flg) return n;  //第二个箱子
                            flg = true;         //第一个箱子
                        }
                        n = k;                  //第一个箱子的最后位置

                        boxRC[0] = i + 1;
                        boxRC[1] = j;
                        break;
                }
            }
            if (flg) return n;  //最后一个动作不是推，但前面有推的动作时
            return len;  //剩余的全部动作
        }

    }
}