package my.boxman;

/*  特别感谢！！！
	下面两个函数由 anian 老师完成，使推箱子界面能够实现真正的“全屏”（隐藏或显示新型手机底部的三个导航按钮）
	private void hideSystemUI() { ... }
 	private void showSystemUI() { ... }
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import my.boxman.jsoko.DiagonalLock;
import my.boxman.jsoko.FreezeLock;
import my.boxman.jsoko.IntStack;

public class myGameView extends Activity {

    AsyncCountBoxsTask mTask;
    public RunMicroTask mMicroTask;

    AlertDialog AotoNextDlg;
    AlertDialog exitDlg;
    AlertDialog exitDlg2;
    AlertDialog exitDlg3;
    AlertDialog lockDlg;

    RefreshHandler1 myTimer1;
    RefreshHandler2 myTimer2;
    RefreshHandler3 myTimer3;
    RefreshHandler4 myTimer4;

    myGameViewMap mMap;  //地图
    myPathfinder mPathfinder;  //探路者

    //底行按钮
    CheckBox bt_UnDo = null;
    CheckBox bt_ReDo = null;
    CheckBox bt_More = null;
    CheckBox bt_IM = null;
    CheckBox bt_Sel = null;
    CheckBox bt_TR = null;
    CheckBox bt_BK = null;

    //正推，目标数、完成数、仓管员初始位置
    public int m_nDstNum;
    int m_nDstOK;
    public int m_nRow, m_nCol;
    //逆推，目标数、完成数、仓管员初始位置
    int m_nDstNum2;
    int m_nDstOK2;
    int m_nRow2;
    int m_nCol2;
    int m_nRow0;  //逆推时，记录仓管员初始占位，周转用
    int m_nCol0;
    int m_iR9, m_iC9, m_iR10, m_iC10; //正逆推时，箱子停止的位置，判断死锁移动时使用
    int b_nRow = -1, b_nCol = -1, oldDir; //动画中的箱子
    final int m_iSleep[] = {50, 17, 10, 5, 2}; //移动速度
    final String m_sSleep[] = {"最快", "较快", "中速", "较慢", "最慢"}; //移动速度

    LinkedList<Byte> m_lstMovedHistory = new LinkedList<Byte>();  //临时栈，记录当前点前的动作，MacroDebug 会用的到

    LinkedList<Byte> m_lstMovUnDo;  //unDo 栈
    LinkedList<Byte> m_lstMovReDo;  //reDo 栈

    LinkedList<Byte> m_lstMovUnDo2;  //逆推 unDo 栈
    LinkedList<Byte> m_lstMovReDo2;  //逆推 reDo 栈

    char[][] mArray9;  //检查死锁用的临时地图（空地图），正推根据目标数识别死锁时，也会用到
    myPathfinder mPF;  //检查死锁用的探路者，正推根据目标数识别死锁时，也会用到
    boolean[][] mark15, mark16, mark7, mark8, mark11, mark12, mark44;  //15、为逆推死点（不可推动的点）；16、为正推网锁点（不可推动的点）；7、为正推箱子初位；8、为正推初态地板；11、为哪个箱子能动或未动过；12、为正推未被使用过的地板；44、是否显示标尺
    short[][] mark14;  //14、为逆推可推的点（其它为推死的点）
    byte[][] mark41;  //网锁标志

    int m_Gif_Start = 0;  //导出 GIF 的起点
    int m_nStep, m_nLastSteps = -1;  //执行"推"、"移"的步数
    boolean m_bYanshi, m_bYanshi2;  //是否演示
    String m_imPort_YASS;  //是否做过动作“导入”或“YASS”过动作
    int m_iStep[] = new int[4];    //记录"推"、"移"的步数
//    boolean m_Path_Formating;  //是否正在格式化路径（仅yass求解返回后使用）
    boolean m_bACT_ERROR;  //执行动作时是否遇到错误
    boolean m_bACT_IgnoreCase;  //执行动作时是否忽略大小写
    boolean m_bMoved;  //有新动作
    boolean m_bBusing;  //忙中
    boolean m_bPush;  //推
    boolean m_bNetLock;  //网锁

    //四邻：左、右、上、下
    final byte[] dr4 = {0, 0, -1, 1};
    final byte[] dc4 = {-1, 1, 0, 0};

    short[][] m_iBoxNum;  //迷宫箱子编号（人为）
    short[][] m_iBoxNum2;  //迷宫箱子自动（固定）编号
    char[][] m_cArray0;  //迷宫初态
    public char[][] m_cArray;  //迷宫
    char[][] bk_cArray;  //逆推迷宫

    char[][] ls_bk_cArray;  //导入时用的临时逆推迷宫

    byte[][] m_selArray;  //迷宫选择
    byte[][] bk_selArray;  //逆推迷宫选择

    DiagonalLock closedDiagonalLock;
    public FreezeLock freezeDeadlock;
    public byte[][] m_Freeze;  //检查冻结时使用的临时数字

    int m_nRow3;  //逆推过关时用，记录仓管员正推地图之初始占位
    int m_nCol3;
    int m_nRow4;  //导入用，记录仓管员逆推地图之初始占位
    int m_nCol4;
    int m_nItemSelect;  //对话框中的出item选择前的记忆

    class RefreshHandler1 extends Handler {

        public void handleMessage(Message msg) {
            UpData1(msg.what);
        }

        public void sleep(int m) {
            removeMessages(0);
            sendMessageDelayed(obtainMessage(1), m);
        }
    }

    class RefreshHandler2 extends Handler {
        public void handleMessage(Message msg) {
            UpData2(msg.what);
        }

        public void sleep(int m) {
            removeMessages(0);
            sendMessageDelayed(obtainMessage(1), m);
        }
    }

    class RefreshHandler3 extends Handler {
        public void handleMessage(Message msg) {
            UpData3(msg.what);
        }

        public void sleep(int m) {
            removeMessages(0);
            sendMessageDelayed(obtainMessage(1), m);
        }
    }

    class RefreshHandler4 extends Handler {
        public void handleMessage(Message msg) {
            UpData4(msg.what);
        }

        public void sleep(int m) {
            removeMessages(0);
            sendMessageDelayed(obtainMessage(1), m);
        }
    }

    //计算仓管员旋转“角度”
    private int getRotate(int mDir1, int mDir2) {
        int mRT = 0;

        //若仅仓管员的移动或为正推
        if (b_nRow < 0 || (!bt_BK.isChecked() && myMaps.m_Sets[18] > 0)) {
            switch (mDir1) {
                case 1:
                    switch (mDir2) {
                        case 2:  //90
                            mRT = -2080;
                            break;
                        case 3:  //180
                            mRT = -10170;
                            break;
                        case 4:  //-90
                            mRT = -1080;
                            break;
                    }
                    break;
                case 2:
                    switch (mDir2) {
                        case 1:  //-90
                            mRT = -1080;
                            break;
                        case 3:  //90
                            mRT = -2080;
                            break;
                        case 4:  //180
                            mRT = -10170;
                            break;
                    }
                    break;
                case 3:
                    switch (mDir2) {
                        case 1:  //180
                            mRT = -10170;
                            break;
                        case 2:  //-90
                            mRT = -1080;
                            break;
                        case 4:  //90
                            mRT = -2080;
                            break;
                    }
                    break;
                case 4:
                    switch (mDir2) {
                        case 1:  //90
                            mRT = -2080;
                            break;
                        case 2:  //180
                            mRT = -10170;
                            break;
                        case 3:  //-90
                            mRT = -1080;
                            break;
                    }
                    break;
            }
        } else {  //正推的redo及逆推之带箱子移动
            switch (mDir1) {
                case 1:
                    switch (mDir2) {
                        case 4:  //90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -1080;
                            else mRT = -2080;
                            break;
                        case 1:  //180
                            if (!bt_BK.isChecked()) mRT = -10170;
                            break;
                        case 2:  //-90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -2080;
                            else mRT = -1080;
                            break;
                    }
                    break;
                case 2:
                    switch (mDir2) {
                        case 3:  //-90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -2080;
                            else mRT = -1080;
                            break;
                        case 1:  //90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -1080;
                            else mRT = -2080;
                            break;
                        case 2:  //180
                            if (!bt_BK.isChecked()) mRT = -10170;
                            break;
                    }
                    break;
                case 3:
                    switch (mDir2) {
                        case 3:  //180
                            if (!bt_BK.isChecked()) mRT = -10170;
                            break;
                        case 4:  //-90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -2080;
                            else mRT = -1080;
                            break;
                        case 2:  //90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -1080;
                            else mRT = -2080;
                            break;
                    }
                    break;
                case 4:
                    switch (mDir2) {
                        case 3:  //90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -1080;
                            else mRT = -2080;
                            break;
                        case 4:  //180
                            if (!bt_BK.isChecked()) mRT = -10170;
                            break;
                        case 1:  //-90
                            if (bt_BK.isChecked() && myMaps.m_Sets[18] < 0) mRT = -2080;
                            else mRT = -1080;
                            break;
                    }
                    break;
            }
        }
        return mRT;
    }

    int[] YanshiSpeed = {0, 100, 200, 500, 1000};

    //redo -- 正推
    private void UpData1(int i) {
        if ((i != 1 || m_nStep <= 0 || m_lstMovReDo.isEmpty()) && mMap.d_Moves >= mMap.m_PicWidth) {
            if (myClearance()) {
                if (m_bMoved) {
                    if (myMaps.curMap.Level_id > 0) {
                        saveAns(1);  //常规关卡的解关答案的保存
                        AotoNextDlg.show();
                    } else {
                        MyToast.showToast(this, "正推通关！", Toast.LENGTH_SHORT);
                        saveAns(0);  //试推时，答案视作状态保存
                    }
                } else MyToast.showToast(this, "正推通关！", Toast.LENGTH_SHORT);
            } else if (myMeet()) {  //正逆相合
                if (m_bMoved) {
                    zhengniHE();  //合并正逆合答案并保存
                    saveAns2();
                    if (myMaps.curMap.Level_id < 0)
                        MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
                } else MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            }
            m_bBusing = false;
            m_bYanshi = false;
            m_bYanshi2 = false;
            return;
        }

        if (bt_IM.isChecked()) {  //瞬移
            if (m_bYanshi) {
                do {
                    reDo1();
                } while (m_nStep > 0 && (m_bPush && m_lstMovReDo.getLast() == m_Dir || !m_bPush && m_lstMovReDo.getLast() < 5));
                mMap.invalidate();

                if (m_nStep > 0) {
                    myTimer1.sleep(YanshiSpeed[myMaps.m_Sets[10]]);
                    m_bPush = false;
                }
            } else {
                while (m_nStep > 0) reDo1();
                mMap.invalidate();
            }
            if (myClearance()) {
                m_bYanshi = false;
                if (m_bMoved) {
                    if (myMaps.curMap.Level_id > 0) {
                        saveAns(1);  //常规关卡的解关答案的保存
                        AotoNextDlg.show();
                    } else {
                        MyToast.showToast(this, "正推通关！", Toast.LENGTH_SHORT);
                        saveAns(0);  //试推时，答案视作状态保存
                    }
                } else
                    MyToast.showToast(this, "正推通关！", Toast.LENGTH_SHORT);
            } else if (myMeet()) {  //正逆相合
                m_bYanshi = false;
                if (m_bMoved) {
                    zhengniHE();  //合并正逆合答案并保存
                    saveAns2();
                    if (myMaps.curMap.Level_id < 0)
                        MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
                } else MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            }
            m_bBusing = false;
        } else {
            if (mMap.d_Moves < mMap.m_PicWidth) {  //精细移动
                if (mMap.d_Moves >= 0) mMap.d_Moves += m_iSleep[myMaps.m_Sets[10]];
                else mMap.d_Moves += (myMaps.m_Sets[10] > 3 ? 10 : 30);
                if (mMap.d_Moves >= -10000 && mMap.d_Moves < -2080 || mMap.d_Moves >= -2000 && mMap.d_Moves < -1080 || mMap.d_Moves >= -1000 && mMap.d_Moves < 0)  //转向已经完成
                    mMap.d_Moves = 0;
            } else {  //初始移动
                b_nRow = -1;
                b_nCol = -1;
                oldDir = myMaps.m_Sets[5];
                if (m_bYanshi && myMaps.m_Sets[28] == 1) {
                    do {
                        reDo1();
                    } while (m_nStep > 0 && !m_bPush);
                    m_bPush = false;
                } else {
                    reDo1();
                }
                myMaps.m_Sets[18] = 1;  //ReDo
                if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200 || myMaps.m_Sets[27] == 0 || myMaps.m_Sets[10] < 3){
                    mMap.d_Moves = 0;
                } else {
                    if (!m_bACT_ERROR) {
                        mMap.d_Moves = getRotate(oldDir, myMaps.m_Sets[14]);
                    }
                }
            }
            mMap.invalidate();
            myTimer1.sleep(1);
        }

        //正推死锁判断
        if (m_nStep == 0 && myMaps.m_Sets[11] == 1 && mMap.d_Moves >= mMap.m_PicWidth && myLock(m_iR9, m_iC9)) {
            lockDlg.show();
        }
    }

    //undo -- 正推
    private void UpData2(int i) {
        if ((i != 1 || m_nStep <= 0 || m_lstMovUnDo.isEmpty()) && mMap.d_Moves >= mMap.m_PicWidth) {
            if (myClearance())
                MyToast.showToast(this, "正推通关！", Toast.LENGTH_SHORT);
            else if (myMeet())  //正逆相合
                MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            m_bBusing = false;
            m_bYanshi2 = false;
            return;
        }

        if (bt_IM.isChecked()) {
            while (m_nStep > 0) unDo1();
            mMap.invalidate();
            if (myClearance()) {
                m_bYanshi2 = false;
                MyToast.showToast(this, "正推通关！", Toast.LENGTH_SHORT);
            } else if (myMeet()) {  //正逆相合
                m_bYanshi2 = false;
                MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            }
            m_bBusing = false;
        } else {
            if (mMap.d_Moves < mMap.m_PicWidth) {  //精细移动
                if (mMap.d_Moves >= 0) mMap.d_Moves += m_iSleep[myMaps.m_Sets[10]];
                else mMap.d_Moves += (myMaps.m_Sets[10] > 3 ? 10 : 30);
                if (mMap.d_Moves >= -10000 && mMap.d_Moves < -2080 || mMap.d_Moves >= -2000 && mMap.d_Moves < -1080 || mMap.d_Moves >= -1000 && mMap.d_Moves < 0)
                    mMap.d_Moves = 0;
            } else {  //初始移动
                b_nRow = -1;
                b_nCol = -1;
                oldDir = myMaps.m_Sets[5];
                unDo1();
                myMaps.m_Sets[18] = -1;  //UnDo
                if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200 || myMaps.m_Sets[27] == 0 || myMaps.m_Sets[10] < 3)
                    mMap.d_Moves = 0;
                else mMap.d_Moves = getRotate(oldDir, myMaps.m_Sets[14]);
            }
            mMap.invalidate();
            myTimer2.sleep(1);
        }
    }

    //redo -- 逆推
    private void UpData3(int i) {
        if ((i != 1 || m_nStep <= 0 || m_lstMovReDo2.isEmpty()) && mMap.d_Moves >= mMap.m_PicWidth) {
            if (myClearance2()) {  //逆推通关
                if (m_bMoved) {
                    zhengniHE2();  //逆推答案转为正推答案并保存
                    saveAns2();
                    if (myMaps.curMap.Level_id < 0)
                        MyToast.showToast(this, "逆推通关！", Toast.LENGTH_SHORT);
                } else MyToast.showToast(this, "逆推通关！", Toast.LENGTH_SHORT);
            } else if (myMeet()) {  //正逆相合
                if (m_bMoved) {
                    zhengniHE();  //合并正逆合答案并保存
                    saveAns2();
                    if (myMaps.curMap.Level_id < 0)
                        MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
                } else MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            }
            m_bBusing = false;
            return;
        }

        if (bt_IM.isChecked()) {
            if (m_bYanshi2) {
                do {
                    reDo2();
                } while (m_nStep > 0 && (m_bPush && m_lstMovReDo2.getLast() == m_Dir || !m_bPush && m_lstMovReDo2.getLast() < 5));
                mMap.invalidate();
                if (m_nStep > 0) {
                    myTimer3.sleep(YanshiSpeed[myMaps.m_Sets[10]]);
                    m_bPush = false;
                }
            } else {
                while (m_nStep > 0) reDo2();
                mMap.invalidate();
            }
            if (myClearance2()) {  //逆推通关
                if (m_bMoved) {
                    zhengniHE2();  //逆推答案转为正推答案并保存
                    saveAns2();
                    if (myMaps.curMap.Level_id < 0)
                        MyToast.showToast(this, "逆推通关！", Toast.LENGTH_SHORT);
                } else MyToast.showToast(this, "逆推通关！", Toast.LENGTH_SHORT);
            } else if (myMeet()) {  //正逆相合
                if (m_bMoved) {
                    zhengniHE();  //合并正逆合答案并保存
                    saveAns2();
                    if (myMaps.curMap.Level_id < 0)
                        MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
                } else MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            }
            m_bBusing = false;
        } else {
            if (mMap.d_Moves < mMap.m_PicWidth) {  //精细移动
                if (mMap.d_Moves >= 0) mMap.d_Moves += m_iSleep[myMaps.m_Sets[10]];
                else mMap.d_Moves += (myMaps.m_Sets[10] > 3 ? 10 : 30);
                if (mMap.d_Moves >= -10000 && mMap.d_Moves < -2080 || mMap.d_Moves >= -2000 && mMap.d_Moves < -1080 || mMap.d_Moves >= -1000 && mMap.d_Moves < 0)
                    mMap.d_Moves = 0;
            } else {  //初始移动
                b_nRow = -1;
                b_nCol = -1;
                oldDir = myMaps.m_Sets[5];
                reDo2();
                myMaps.m_Sets[18] = 1;  //ReDo
                if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200 || myMaps.m_Sets[27] == 0 || myMaps.m_Sets[10] < 3) {
                    mMap.d_Moves = 0;
                } else {
                    if (!m_bACT_ERROR) {
                        mMap.d_Moves = getRotate(oldDir, myMaps.m_Sets[14]);
                    }
                }
            }
            mMap.invalidate();
            myTimer3.sleep(1);
        }

        //逆推死锁判断
        if (m_nStep == 0 && myMaps.m_Sets[11] == 1 && mMap.d_Moves >= mMap.m_PicWidth && myLock2(m_iR10, m_iC10)) {
            lockDlg.show();
        }
    }

    //undo -- 逆推
    private void UpData4(int i) {
        if ((i != 1 || m_nStep <= 0 || m_lstMovUnDo2.isEmpty()) && mMap.d_Moves >= mMap.m_PicWidth) {
            if (myClearance2()) //逆推通关
                MyToast.showToast(this, "逆推通关！", Toast.LENGTH_SHORT);
            else if (myMeet())  //正逆相合
                MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            m_bBusing = false;
            return;
        }

        if (bt_IM.isChecked()) {
            while (m_nStep > 0) unDo2();
            mMap.invalidate();
            if (myClearance2()) //逆推通关
                MyToast.showToast(this, "逆推通关！", Toast.LENGTH_SHORT);
            else if (myMeet())  //正逆相合
                MyToast.showToast(this, "正逆相合！", Toast.LENGTH_SHORT);
            m_bBusing = false;
        } else {
            if (mMap.d_Moves < mMap.m_PicWidth) {  //精细移动
                if (mMap.d_Moves >= 0) mMap.d_Moves += m_iSleep[myMaps.m_Sets[10]];
                else mMap.d_Moves += (myMaps.m_Sets[10] > 3 ? 10 : 30);
                if (mMap.d_Moves >= -10000 && mMap.d_Moves < -2080 || mMap.d_Moves >= -2000 && mMap.d_Moves < -1080 || mMap.d_Moves >= -1000 && mMap.d_Moves < 0)
                    mMap.d_Moves = 0;
            } else {  //初始移动
                b_nRow = -1;
                b_nCol = -1;
                oldDir = myMaps.m_Sets[5];
                unDo2();
                myMaps.m_Sets[18] = -1;  //UnDo
                if (myMaps.isSimpleSkin || myMaps.isSkin_200 == 200 || myMaps.m_Sets[27] == 0 || myMaps.m_Sets[10] < 3)
                    mMap.d_Moves = 0;
                else mMap.d_Moves = getRotate(oldDir, myMaps.m_Sets[14]);
            }
            mMap.invalidate();
            myTimer4.sleep(1);
        }
    }

    int i, j, i2, j2;
    byte m_Dir;
    //reDo1，移动/推动 1 步 ---- 正推
    int[] dr_reDo1 = {0, 0, -1, 0, 1, 0, -2, 0, 2};
    int[] dc_reDo1 = {0, -1, 0, 1, 0, -2, 0, 2, 0};    //人和箱子四个方向调整值：l,u,r,d，为省脑筋，前面多放一个 0

    public void reDo1() {
        if (mMap.m_lGoto) {  //进度条
            int len = m_lstMovUnDo.size();
            int len2 = m_lstMovUnDo.size() + m_lstMovReDo.size();
            if (len2 > 0) {
                mMap.curMoves = (int) (((double) len / len2) * (mMap.stRight - mMap.stLeft));
            } else mMap.curMoves = 0;
        }
        if (m_lstMovReDo.isEmpty()) {
            m_bBusing = false;
            m_nStep = 0;
            return;
        }
        m_bBusing = true;
        m_iR9 = -1;  //决定是否有必要进行死锁判断
        m_Dir = m_lstMovReDo.pollLast();

        if (m_Dir < 1 || m_Dir > 8) {
            m_bBusing = false;
            m_lstMovReDo.clear();
            m_nStep = 0;
            m_bACT_ERROR = true;  //执行动作时遇到错误
            return;
        }
        m_bPush = (m_Dir > 4);   //推
        if (mMap.m_bBoxTo) mMap.m_bBoxTo = false; //关闭箱子可达位置提示状态
        if (mMap.m_bManTo) mMap.m_bManTo = false; //关闭可达位置提示状态
        if (mMap.m_boxCanMove) mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
        if (mMap.m_boxNoMoved) mMap.m_boxNoMoved = false;  //关闭未动箱子提示状态
        if (mMap.m_boxNoUsed) mMap.m_boxNoUsed = false;  //关闭未使用地板提示状态
        if (mMap.m_boxCanCome) mMap.m_boxCanCome = false;  //关闭可推过来的箱子提示状态
        if (mMap.m_boxCanCome) mMap.m_boxCanCome = false;  //关闭可推过来箱子提示状态

        if (m_Dir < 5) {
            myMaps.m_Sets[14] = m_Dir;  //动画移动方向
            myMaps.m_Sets[5] = m_Dir;  //仓管员图片方向
            i = m_nRow + dr_reDo1[m_Dir];
            j = m_nCol + dc_reDo1[m_Dir];
            i2 = -1;
            j2 = -1;
        } else {
            myMaps.m_Sets[14] = m_Dir - 4;  //动画移动方向
            myMaps.m_Sets[5] = m_Dir - 4;  //仓管员图片方向
            i = m_nRow + dr_reDo1[m_Dir - 4];
            j = m_nCol + dc_reDo1[m_Dir - 4];
            i2 = m_nRow + dr_reDo1[m_Dir];
            j2 = m_nCol + dc_reDo1[m_Dir];
        }

        if (m_bACT_IgnoreCase) {  //如果允许忽略大小写
            if (m_Dir > 4) { //若为“推”
                if (i >= 0 && j >= 0 && i < m_cArray.length && j < m_cArray[0].length &&
                        (m_cArray[i][j] == '-' || m_cArray[i][j] == '.')) {  //“推”到“地板“

                    m_Dir = (byte) (m_Dir - 4);

                    i = m_nRow + dr_reDo1[m_Dir];
                    j = m_nCol + dc_reDo1[m_Dir];
                    i2 = -1;
                    j2 = -1;
                }
            } else {  //若为移动
                if (i >= 0 && j >= 0 && i < m_cArray.length && j < m_cArray[0].length &&
                        (m_cArray[i][j] == '$' || m_cArray[i][j] == '*')) {  //“移动”到“箱子”

                    m_Dir = (byte) (m_Dir + 4);

                    i = m_nRow + dr_reDo1[m_Dir - 4];
                    j = m_nCol + dc_reDo1[m_Dir - 4];
                    i2 = m_nRow + dr_reDo1[m_Dir];
                    j2 = m_nCol + dc_reDo1[m_Dir];
                }
            }
        }

        if (m_Dir > 4) { //若为“推”    =============================================
            if (i < 0 || j < 0 || i2 < 0 || j2 < 0 ||
                    i >= m_cArray.length || j >= m_cArray[0].length || i2 >= m_cArray.length || j2 >= m_cArray[0].length ||
                    m_cArray[i2][j2] != '-' && m_cArray[i2][j2] != '.' ||
                    m_cArray[i][j] != '$' && m_cArray[i][j] != '*') {
                m_lstMovReDo.clear();
                m_bBusing = false;
                m_nStep = 0;
                m_bACT_ERROR = true;  //执行动作时遇到错误
                return; //界外，或不合理
            }
            if (mMap.Box_Row0 < 0) {  //记录箱子移动前的位置，检查网锁时使用
                mMap.Box_Row0 = i;
                mMap.Box_Col0 = j;
            }
            //先动箱子
            if (m_cArray[i2][j2] == '-')
                m_cArray[i2][j2] = '$';
            else {
                m_cArray[i2][j2] = '*';
                m_nDstOK++;
            }
            //箱子编号
            m_iBoxNum[i2][j2] = m_iBoxNum[i][j];
            m_iBoxNum[i][j] = -1;
            m_iBoxNum2[i2][j2] = m_iBoxNum2[i][j];
            m_iBoxNum2[i][j] = -1;
            //再动人
            if (m_cArray[i][j] == '$')
                m_cArray[i][j] = '@';
            else {
                m_cArray[i][j] = '+';
                m_nDstOK--;
            }
            if (m_cArray[m_nRow][m_nCol] == '@')
                m_cArray[m_nRow][m_nCol] = '-';
            else
                m_cArray[m_nRow][m_nCol] = '.';

            m_nRow = i;
            m_nCol = j;
            m_iR9 = i2;  //决定是否有必要进行死锁判断
            m_iC9 = j2;
            b_nRow = i2;  //动画移动时使用
            b_nCol = j2;

            //动作有效，入栈
            m_lstMovUnDo.offer(m_Dir);
            m_iStep[0]++;  //推动步数
        } else { //若为移动          =============================================
            if (i < 0 || j < 0 || i >= m_cArray.length || j > m_cArray[0].length ||
                    m_cArray[i][j] != '-' && m_cArray[i][j] != '.') {
                m_lstMovReDo.clear();
                m_bBusing = false;
                m_nStep = 0;
                m_bACT_ERROR = true;  //执行动作时遇到错误
                return; //界外，或不合理
            }
            if (m_cArray[i][j] == '-')
                m_cArray[i][j] = '@';
            else {
                m_cArray[i][j] = '+';
            }
            if (m_cArray[m_nRow][m_nCol] == '@')
                m_cArray[m_nRow][m_nCol] = '-';
            else
                m_cArray[m_nRow][m_nCol] = '.';

            m_nRow = i;
            m_nCol = j;

            //动作有效，入栈
            m_lstMovUnDo.offer(m_Dir);
        }   //=============================================

        m_iStep[1]++;  //移动步数
        m_nStep--;
        mMap.m_iR = m_nRow;
        mMap.m_iC = m_nCol;
    }

    //unDo1，撤销 1 步 ---- 正推
    byte[] dr_unDo1 = {0, 0, 1, 0, -1, 0, -1, 0, 1};
    byte[] dc_unDo1 = {0, 1, 0, -1, 0, -1, 0, 1, 0};    //人和箱子四个方向调整值：l,u,r,d，为省脑筋，前面多放一个 0

    public void unDo1() {
        if (mMap.m_lGoto) {  //进度条
            int len = m_lstMovUnDo.size();
            int len2 = m_lstMovUnDo.size() + m_lstMovReDo.size();
            if (len2 > 0) {
                mMap.curMoves = (int) (((double) len / len2) * (mMap.stRight - mMap.stLeft));
            } else mMap.curMoves = 0;
        }
        if (m_lstMovUnDo.isEmpty()) {
            m_bBusing = false;
            return;
        }
        m_bBusing = true;
        m_Dir = m_lstMovUnDo.pollLast();

        m_bPush = (m_Dir > 4);    //推
        if (mMap.m_bBoxTo) mMap.m_bBoxTo = false; //关闭箱子可达位置提示状态
        if (mMap.m_bManTo) mMap.m_bManTo = false; //关闭可达位置提示状态
        if (mMap.m_boxCanMove) mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
        if (mMap.m_boxNoMoved) mMap.m_boxNoMoved = false;  //关闭未动箱子提示状态
        if (mMap.m_boxNoUsed) mMap.m_boxNoUsed = false;  //关闭未使用地板提示状态
        if (mMap.m_boxCanCome) mMap.m_boxCanCome = false;  //关闭可推过来的箱子提示状态
        if (mMap.m_boxCanCome) mMap.m_boxCanCome = false;  //关闭可推过来箱子提示状态

        switch (m_Dir) {
            case 1:
                myMaps.m_Sets[14] = 3;  //动画移动方向
                myMaps.m_Sets[5] = 3;   //仓管员图片方向
                break;
            case 2:
                myMaps.m_Sets[14] = 4;
                myMaps.m_Sets[5] = 4;
                break;
            case 3:
                myMaps.m_Sets[14] = 1;
                myMaps.m_Sets[5] = 1;
                break;
            case 4:
                myMaps.m_Sets[14] = 2;
                myMaps.m_Sets[5] = 2;
                break;
            case 5:
                myMaps.m_Sets[14] = 3;
                myMaps.m_Sets[5] = 1;
                break;
            case 6:
                myMaps.m_Sets[14] = 4;
                myMaps.m_Sets[5] = 2;
                break;
            case 7:
                myMaps.m_Sets[14] = 1;
                myMaps.m_Sets[5] = 3;
                break;
            case 8:
                myMaps.m_Sets[14] = 2;
                myMaps.m_Sets[5] = 4;
                break;
        }
        if (m_Dir < 5) {
            i = m_nRow + dr_unDo1[m_Dir];
            j = m_nCol + dc_unDo1[m_Dir];
            i2 = -1;
            j2 = -1;
        } else {
            i = m_nRow + dr_unDo1[m_Dir - 4];
            j = m_nCol + dc_unDo1[m_Dir - 4];
            i2 = m_nRow + dr_unDo1[m_Dir];
            j2 = m_nCol + dc_unDo1[m_Dir];
        }

        if (m_Dir > 4) { //若为“推” ==》 “拉”   =============================================
            if ((m_cArray[i][j] == '-' || m_cArray[i][j] == '.') && //人可以动
                    (m_cArray[i2][j2] == '$' || m_cArray[i2][j2] == '*')) { // 确认有箱子，避免演示答案与关卡不符
                //先动人
                if (m_cArray[i][j] == '-')
                    m_cArray[i][j] = '@';
                else {
                    m_cArray[i][j] = '+';
                }
                //再动箱子
                if (m_cArray[m_nRow][m_nCol] == '@')
                    m_cArray[m_nRow][m_nCol] = '$';
                else {
                    m_cArray[m_nRow][m_nCol] = '*';
                    m_nDstOK++;
                }
                if (m_cArray[i2][j2] == '$')
                    m_cArray[i2][j2] = '-';
                else {
                    m_cArray[i2][j2] = '.';
                    m_nDstOK--;
                }
                //箱子编号
                m_iBoxNum[m_nRow][m_nCol] = m_iBoxNum[i2][j2];
                m_iBoxNum[i2][j2] = -1;
                m_iBoxNum2[m_nRow][m_nCol] = m_iBoxNum2[i2][j2];
                m_iBoxNum2[i2][j2] = -1;

                b_nRow = m_nRow;  //动画移动时使用
                b_nCol = m_nCol;
                m_nRow = i;
                m_nCol = j;

                //动作有效，入栈
                m_lstMovReDo.offer(m_Dir);
                m_iStep[0]--;  //推动步数
            }
        } else { //若为移动          =============================================
            if (m_cArray[i][j] == '-' || m_cArray[i][j] == '.') { //可以动
                if (m_cArray[i][j] == '-')
                    m_cArray[i][j] = '@';
                else {
                    m_cArray[i][j] = '+';
                }
                if (m_cArray[m_nRow][m_nCol] == '@')
                    m_cArray[m_nRow][m_nCol] = '-';
                else
                    m_cArray[m_nRow][m_nCol] = '.';

                m_nRow = i;
                m_nCol = j;

                //动作有效，入栈
                m_lstMovReDo.offer(m_Dir);
            }
        }   //=============================================

        m_iStep[1]--;  //移动步数
        if (m_iStep[1] < m_Gif_Start) m_Gif_Start = 0;  //导出 GIF 片段的开始点复位
        m_nStep--;
        mMap.m_iR = m_nRow;
        mMap.m_iC = m_nCol;
    }

    //reDo2，移动/推动 1 步 ---- 逆推
    int[] dr_reDo2 = {0, 0, -1, 0, 1, 0, 1, 0, -1};
    int[] dc_reDo2 = {0, -1, 0, 1, 0, 1, 0, -1, 0};    //reDo2人和箱子四个方向调整值：l,u,r,d，为省脑筋，前面多放一个 0

    public void reDo2() {
        if (mMap.m_lGoto2) {  //进度条
            int len = m_lstMovUnDo2.size();
            int len2 = m_lstMovUnDo2.size() + m_lstMovReDo2.size();
            if (len2 > 0) {
                mMap.curMoves2 = (int) (((double) len / len2) * (mMap.stRight - mMap.stLeft));
            } else mMap.curMoves2 = 0;
        }
        if (m_lstMovReDo2.isEmpty()) {
            m_bBusing = false;
            m_nStep = 0;
            return;
        }
        m_bBusing = true;
        m_iR10 = -1;  //决定是否有必要进行死锁判断
        m_Dir = m_lstMovReDo2.pollLast();

        if (m_Dir < 1 || m_Dir > 8) {
            m_bBusing = false;
            m_lstMovReDo2.clear();
            m_nStep = 0;
            m_bACT_ERROR = true;  //执行动作时遇到错误
            return;
        }
        m_bPush = (m_Dir > 4);   //推
        if (mMap.m_bBoxTo2) mMap.m_bBoxTo2 = false; //关闭箱子可达位置提示状态
        if (mMap.m_bManTo2) mMap.m_bManTo2 = false; //关闭可达位置提示状态
        if (mMap.m_boxCanMove2) mMap.m_boxCanMove2 = false;  //关闭可动箱子提示状态
        if (mMap.m_boxCanCome2) mMap.m_boxCanCome2 = false;  //关闭可推过来的箱子提示状态
        if (mMap.m_boxCanCome2) mMap.m_boxCanCome2 = false;  //关闭可推过来箱子提示状态

        switch (m_Dir) {
            case 1:
                myMaps.m_Sets[14] = 1;  //动画移动方向
                myMaps.m_Sets[5] = 1;   //仓管员图片方向
                break;
            case 2:
                myMaps.m_Sets[14] = 2;
                myMaps.m_Sets[5] = 2;
                break;
            case 3:
                myMaps.m_Sets[14] = 3;
                myMaps.m_Sets[5] = 3;
                break;
            case 4:
                myMaps.m_Sets[14] = 4;
                myMaps.m_Sets[5] = 4;
                break;
            case 5:
                myMaps.m_Sets[14] = 1;
                myMaps.m_Sets[5] = 3;
                break;
            case 6:
                myMaps.m_Sets[14] = 2;
                myMaps.m_Sets[5] = 4;
                break;
            case 7:
                myMaps.m_Sets[14] = 3;
                myMaps.m_Sets[5] = 1;
                break;
            case 8:
                myMaps.m_Sets[14] = 4;
                myMaps.m_Sets[5] = 2;
                break;
        }
        if (m_Dir < 5) {
            i = m_nRow2 + dr_reDo2[m_Dir];
            j = m_nCol2 + dc_reDo2[m_Dir];
            i2 = -1;
            j2 = -1;
        } else {
            i = m_nRow2 + dr_reDo2[m_Dir - 4];
            j = m_nCol2 + dc_reDo2[m_Dir - 4];
            i2 = m_nRow2 + dr_reDo2[m_Dir];
            j2 = m_nCol2 + dc_reDo2[m_Dir];
        }

        if (m_bACT_IgnoreCase) {  //如果允许忽略大小写
            if (m_Dir > 4) { //若为“推”
                if (i >= 0 && j >= 0 && i < bk_cArray.length && j < bk_cArray[0].length &&
                        (bk_cArray[i2][j2] == '-' || bk_cArray[i2][j2] == '.')) {  //“推”到“地板“

                    m_Dir = (byte) (m_Dir - 4);

                    i = m_nRow2 + dr_reDo2[m_Dir];
                    j = m_nCol2 + dc_reDo2[m_Dir];
                    i2 = -1;
                    j2 = -1;
                }
            } else {  //若为移动
                if (i >= 0 && j >= 0 && i < bk_cArray.length && j < bk_cArray[0].length &&
                        (bk_cArray[m_nRow2 + dr_reDo2[m_Dir]][m_nCol2 + dc_reDo2[m_Dir]] == '$' || bk_cArray[m_nRow2 + dr_reDo2[m_Dir]][m_nCol2 + dc_reDo2[m_Dir]] == '*')) {  //“看”到“箱子”

                    m_Dir = (byte) (m_Dir + 4);

                    i = m_nRow2 + dr_reDo2[m_Dir - 4];
                    j = m_nCol2 + dc_reDo2[m_Dir - 4];
                    i2 = m_nRow2 + dr_reDo2[m_Dir];
                    j2 = m_nCol2 + dc_reDo2[m_Dir];
                }
            }
        }

        if (m_Dir > 4) { //若为“拉” ==》 “推”   =============================================
            if (i < 0 || j < 0 || i2 < 0 || j2 < 0 ||
                    i >= bk_cArray.length || j >= bk_cArray[0].length || i2 >= bk_cArray.length || j2 >= bk_cArray[0].length ||
                    bk_cArray[i][j] != '-' && bk_cArray[i][j] != '.' ||
                    bk_cArray[i2][j2] != '$' && bk_cArray[i2][j2] != '*') {
                m_lstMovReDo2.clear();
                m_bBusing = false;
                m_nStep = 0;
                m_bACT_ERROR = true;  //执行动作时遇到错误
                return; //界外，或不合理
            }
            //先动人
            if (bk_cArray[i][j] == '-')
                bk_cArray[i][j] = '@';
            else {
                bk_cArray[i][j] = '+';
            }
            //再动箱子
            if (bk_cArray[m_nRow2][m_nCol2] == '@')
                bk_cArray[m_nRow2][m_nCol2] = '$';
            else {
                bk_cArray[m_nRow2][m_nCol2] = '*';
                m_nDstOK2++;
            }
            if (bk_cArray[i2][j2] == '$')
                bk_cArray[i2][j2] = '-';
            else {
                bk_cArray[i2][j2] = '.';
                m_nDstOK2--;
            }

            m_iR10 = m_nRow2;  //决定是否有必要进行死锁判断
            m_iC10 = m_nCol2;
            b_nRow = m_nRow2;  //动画移动时使用
            b_nCol = m_nCol2;
            m_nRow2 = i;
            m_nCol2 = j;

            //动作有效，入栈
            m_lstMovUnDo2.offer(m_Dir);
            m_iStep[2]++;  //推动步数
        } else { //若为移动          =============================================
            if (i < 0 || j < 0 || i >= bk_cArray.length || j > bk_cArray[0].length ||
                    bk_cArray[i][j] != '-' && bk_cArray[i][j] != '.') {
                m_lstMovReDo2.clear();
                m_bBusing = false;
                m_nStep = 0;
                m_bACT_ERROR = true;  //执行动作时遇到错误
                return; //界外，或不合理
            }
            if (bk_cArray[i][j] == '-')
                bk_cArray[i][j] = '@';
            else {
                bk_cArray[i][j] = '+';
            }
            if (bk_cArray[m_nRow2][m_nCol2] == '@')
                bk_cArray[m_nRow2][m_nCol2] = '-';
            else
                bk_cArray[m_nRow2][m_nCol2] = '.';

            m_nRow2 = i;
            m_nCol2 = j;

            //动作有效，入栈
            m_lstMovUnDo2.offer(m_Dir);
        }   //=============================================

        m_iStep[3]++;  //移动步数
        m_nStep--;
        mMap.m_iR = m_nRow2;
        mMap.m_iC = m_nCol2;
    }

    //unDo2，撤销 1 步 ---- 逆推
    int[] dr_unDo2 = {0, 0, 1, 0, -1, 0, 2, 0, -2};
    int[] dc_unDo2 = {0, 1, 0, -1, 0, 2, 0, -2, 0};    //人和箱子四个方向调整值：l,u,r,d，为省脑筋，前面多放一个 0

    public void unDo2() {
        if (mMap.m_lGoto2) {  //进度条
            int len = m_lstMovUnDo2.size();
            int len2 = m_lstMovUnDo2.size() + m_lstMovReDo2.size();
            if (len2 > 0) {
                mMap.curMoves2 = (int) (((double) len / len2) * (mMap.stRight - mMap.stLeft));
            } else mMap.curMoves2 = 0;
        }
        if (m_lstMovUnDo2.isEmpty()) {
            m_bBusing = false;
            return;
        }
        m_bBusing = true;
        m_Dir = m_lstMovUnDo2.pollLast();

        m_bPush = (m_Dir > 4);   //推
        if (mMap.m_bBoxTo2) mMap.m_bBoxTo2 = false; //关闭箱子可达位置提示状态
        if (mMap.m_bManTo2) mMap.m_bManTo2 = false; //关闭可达位置提示状态
        if (mMap.m_boxCanMove2) mMap.m_boxCanMove2 = false;  //关闭可动箱子提示状态
        if (mMap.m_boxCanCome2) mMap.m_boxCanCome2 = false;  //关闭可推过来的箱子提示状态
        if (mMap.m_boxCanCome2) mMap.m_boxCanCome2 = false;  //关闭可推过来箱子提示状态

        switch (m_Dir) {
            case 1:
                myMaps.m_Sets[14] = 3;  //动画移动方向
                myMaps.m_Sets[5] = 3;   //仓管员图片方向
                break;
            case 2:
                myMaps.m_Sets[14] = 4;
                myMaps.m_Sets[5] = 4;
                break;
            case 3:
                myMaps.m_Sets[14] = 1;
                myMaps.m_Sets[5] = 1;
                break;
            case 4:
                myMaps.m_Sets[14] = 2;
                myMaps.m_Sets[5] = 2;
                break;
            case 5:
                myMaps.m_Sets[14] = 3;
                myMaps.m_Sets[5] = 3;
                break;
            case 6:
                myMaps.m_Sets[14] = 4;
                myMaps.m_Sets[5] = 4;
                break;
            case 7:
                myMaps.m_Sets[14] = 1;
                myMaps.m_Sets[5] = 1;
                break;
            case 8:
                myMaps.m_Sets[14] = 2;
                myMaps.m_Sets[5] = 2;
                break;
        }
        if (m_Dir < 5) {
            i = m_nRow2 + dr_unDo2[m_Dir];
            j = m_nCol2 + dc_unDo2[m_Dir];
            i2 = -1;
            j2 = -1;
        } else {
            i = m_nRow2 + dr_unDo2[m_Dir - 4];
            j = m_nCol2 + dc_unDo2[m_Dir - 4];
            i2 = m_nRow2 + dr_unDo2[m_Dir];
            j2 = m_nCol2 + dc_unDo2[m_Dir];
        }

        if (m_Dir > 4) { //若为“拉” ==》 “推”    =============================================
            if ((bk_cArray[i2][j2] == '-' || bk_cArray[i2][j2] == '.') && //可以推
                    (bk_cArray[i][j] == '$' || bk_cArray[i][j] == '*')) { // 确认是箱子，避免演示答案与关卡不符
                //先动箱子
                if (bk_cArray[i2][j2] == '-')
                    bk_cArray[i2][j2] = '$';
                else {
                    bk_cArray[i2][j2] = '*';
                    m_nDstOK2++;
                }
                //再动人
                if (bk_cArray[i][j] == '$')
                    bk_cArray[i][j] = '@';
                else {
                    bk_cArray[i][j] = '+';
                    m_nDstOK2--;
                }
                if (bk_cArray[m_nRow2][m_nCol2] == '@')
                    bk_cArray[m_nRow2][m_nCol2] = '-';
                else
                    bk_cArray[m_nRow2][m_nCol2] = '.';

                m_nRow2 = i;
                m_nCol2 = j;
                b_nRow = i2;  //动画移动时使用
                b_nCol = j2;

                //动作有效，入栈
                m_lstMovReDo2.offer(m_Dir);
                m_iStep[2]--;  //推动步数
            }
        } else { //若为移动          =============================================
            if (bk_cArray[i][j] == '-' || bk_cArray[i][j] == '.') { //可以动
                if (bk_cArray[i][j] == '-')
                    bk_cArray[i][j] = '@';
                else {
                    bk_cArray[i][j] = '+';
                }
                if (bk_cArray[m_nRow2][m_nCol2] == '@')
                    bk_cArray[m_nRow2][m_nCol2] = '-';
                else
                    bk_cArray[m_nRow2][m_nCol2] = '.';

                m_nRow2 = i;
                m_nCol2 = j;

                //动作有效，入栈
                m_lstMovReDo2.offer(m_Dir);
            }
        }   //=============================================

        m_iStep[3]--;  //移动步数
        m_nStep--;
        mMap.m_iR = m_nRow2;
        mMap.m_iC = m_nCol2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.game_view);

        hideSystemUI();  // 隐藏系统的那三个按钮

        myTimer1 = new RefreshHandler1();
        myTimer2 = new RefreshHandler2();
        myTimer3 = new RefreshHandler3();
        myTimer4 = new RefreshHandler4();

        mMap = (myGameViewMap) this.findViewById(R.id.mxMapView);
        mMap.Init(this);

        Builder dlg = new Builder(this);
        dlg.setTitle("恭喜过关！").setMessage("是否自动打开下一个未解关卡？")
                .setCancelable(false).setNegativeButton("否", null)
                .setPositiveButton("是", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        int k = myMaps.m_lstMaps.indexOf(myMaps.curMap) + 1;
                        int len = myMaps.m_lstMaps.size();
                        while (k < len && myMaps.m_lstMaps.get(k).Solved) {
                            k++;
                        }
                        if (k < len) {
                            myMaps.curMap = null;
                            myMaps.curMap = myMaps.m_lstMaps.get(k);
                            myMaps.m_nTrun = myMaps.curMap.Trun;
                            bt_BK.setChecked(false);  //默认正推模式
                            bt_TR.setChecked(false);
                            bt_TR.setText(myMaps.m_nTrun + " 转");
                            initMap();
                            ls_bk_cArray = bk_cArray;
                        } else {
                            MyToast.showToast(myGameView.this, "后面没有未解关卡！", Toast.LENGTH_SHORT);
                        }
                    }
                });
        AotoNextDlg = dlg.create();

        Builder dlg0 = new Builder(this);
        dlg0.setTitle("退出").setMessage("有状态未保存，坚持退出吗？").setCancelable(false).setNegativeButton("否", null)
                .setPositiveButton("是", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        myStop();
                        finish();
                    }
                });
        exitDlg = dlg0.create();

        //上一关卡对话框
        Builder dlg1 = new Builder(this);
        dlg1.setTitle("更换关卡").setMessage("有状态未保存，坚持更换吗？").setCancelable(false).setNegativeButton("否", null)
                .setPositiveButton("是", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        int n1 = myMaps.m_lstMaps.indexOf(myMaps.curMap);

                        if (n1 > 0) {
                            myPre(n1-1);
                        } else {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        }
                    }
                });
        exitDlg2 = dlg1.create();

        //下一关卡对话框
        Builder dlg2 = new Builder(this);
        dlg2.setTitle("更换关卡").setMessage("有状态未保存，坚持更换吗？").setCancelable(false).setNegativeButton("否", null)
                .setPositiveButton("是", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        int n2 = myMaps.m_lstMaps.indexOf(myMaps.curMap);

                        if (n2 >= 0 && n2 + 1  < myMaps.m_lstMaps.size()) {
                            myNext(n2+1);
                        } else {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        }
                    }
                });
        exitDlg3 = dlg2.create();

        //死锁提示框
        Builder dlg4 = new Builder(this);
        dlg4.setTitle("死锁移动").setMessage("这一步造成关卡死锁，继续吗？")
                .setCancelable(false).setNegativeButton("继续", null)
                .setPositiveButton("撤销移动", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        bt_UnDo.setChecked(!bt_UnDo.isChecked());
                    }
                });
        lockDlg = dlg4.create();

        bt_UnDo = (CheckBox) findViewById(R.id.bt_UnDo);  //后退
        bt_UnDo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StopMicro();  //停止“宏”功能运行

                bt_Sel.setChecked(false);  //关闭计数状态
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_bNetLock = false;  //取消网型提示
                m_nStep = 0;
                m_bYanshi = false;
                m_bYanshi2 = false;
                mMap.m_lChangeBK = false;  //是否显示更换背景按钮
                m_bACT_ERROR = false;  //执行动作时是否遇到错误
                mMap.invalidate();
                if (m_bBusing) return;

                if (bt_BK.isChecked()) {  //逆推
                    if (m_lstMovUnDo2.isEmpty()) {
                        MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                    } else {
                        if (myMaps.m_Sets[23] == 1) m_nStep = 1;
                        else m_nStep = getStep(m_lstMovUnDo2);
                        UpData4(1);
                    }
                } else {            //正推
                    if (myMaps.isMacroDebug) {
                        levelReset(false);  //正推关卡复位
                        if (myMaps.m_Sets[25] == 1) {
                            load_Level(true);  //加载即景正推目标点
                        }
                        if (myMaps.m_ActionIsPos) {  //若执行“宏”选择了从当前点执行，则先将地图回复到当前点局面
                            Iterator myItr = m_lstMovedHistory.iterator();
                            m_lstMovReDo.clear();
                            m_lstMovUnDo.clear();
                            while (myItr.hasNext()) {
                                m_lstMovReDo.offer((Byte)myItr.next());
                                m_nStep = 1;
                                reDo1();
                            }
                            mMap.invalidate();
                        }
                        mMap.curMoves = 0;

                        m_nMacro_Row = m_nRow;
                        m_nMacro_Col = m_nCol;
                        int len = mMap.myMacro.size();
                        if (len > 1) {
                            mMap.myMacro.remove(len - 1);
                            for (int k = 0; k < len - 2; k++) {
                                myDo_Block(mMap.myMacro.get(k).intValue(), mMap.myMacro.get(k).intValue(), true, false);
                            }
                            m_lstMovReDo.clear();
                            mMap.invalidate();
                        } else {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        }
                    } else {
                        if (m_lstMovUnDo.isEmpty()) {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        } else {
                            if (myMaps.m_Sets[23] == 1) m_nStep = 1;
                            else  {
                                if (m_nLastSteps >= 0 && m_nLastSteps <= m_lstMovUnDo.size()) m_nStep = m_lstMovUnDo.size() - m_nLastSteps;
                                else m_nStep = getStep2(m_lstMovUnDo);
                            }
                            m_nLastSteps = -1;
                            UpData2(1);
                        }
                    }
                }
                m_bBusing = false;
            }
        });
        bt_UnDo.setLongClickable(true);
        bt_UnDo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                StopMicro();  //停止“宏”功能运行
                bt_Sel.setChecked(false);  //关闭计数状态
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_bNetLock = false;  //取消网型提示
                m_nStep = 0;
                m_bYanshi = false;
                m_bYanshi2 = false;
                mMap.m_lChangeBK = false;  //是否显示更换背景按钮
                m_bACT_ERROR = false;  //执行动作时是否遇到错误
                mMap.d_Moves = mMap.m_PicWidth;
                mMap.invalidate();

                //先合并 undo 和 redo 序列，关卡复位后，将合并后的动作序列送入 redu 序列
                if (bt_BK.isChecked()) {  //逆推
                    MyToast.showToast(myGameView.this, "重新开始！", Toast.LENGTH_SHORT);
                    levelReset(true);  //逆推关卡复位
                    if (myMaps.m_Sets[13] == 1)
                        load_BK_Level(true);  //加载即景逆推目标点

                } else {  //正推
                    MyToast.showToast(myGameView.this, "重新开始！", Toast.LENGTH_SHORT);
                    levelReset(false);  //正推关卡复位
                    if (myMaps.m_Sets[25] == 1) {
                        load_Level(true);  //加载即景正推目标点
                    }

                    if (myMaps.m_ActionIsPos && myMaps.isMacroDebug) {  //若执行“宏”选择了从当前点执行，则先将地图回复到当前点局面
                        Iterator myItr = m_lstMovedHistory.iterator();
                        m_lstMovReDo.clear();
                        m_lstMovUnDo.clear();
                        while (myItr.hasNext()) {
                            m_lstMovReDo.offer((Byte)myItr.next());
                            m_nStep = 1;
                            reDo1();
                        }
                    }
                    mMap.myMacro.clear();
                    mMap.myMacro.add(0);  //准备从第 0 行开始
                    mMap.myMacroInf = "";
                }
                mMap.curMoves = 0;
                m_bBusing = false;
                mMap.invalidate();
                return true;
            }
        });
        bt_ReDo = (CheckBox) findViewById(R.id.bt_ReDo);  //前进
        bt_ReDo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StopMicro();  //停止“宏”功能运行

                bt_Sel.setChecked(false);  //关闭计数状态
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_bNetLock = false;  //取消网型提示
                m_nStep = 0;
                m_bYanshi = false;
                m_bYanshi2 = false;
                mMap.m_lChangeBK = false;  //是否显示更换背景按钮
                m_bACT_ERROR = false;  //执行动作时是否遇到错误
                mMap.invalidate();
                if (m_bBusing) return;
                if (bt_BK.isChecked()) {  //逆推
                    if (m_lstMovReDo2.isEmpty()) {
                        MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                    } else {
                        if (myMaps.m_Sets[23] == 1) m_nStep = 1;
                        else m_nStep = getStep2(m_lstMovReDo2);
                        UpData3(1);
                    }
                } else {            //正推
                    if (myMaps.isMacroDebug) {
                        if (mMap.myMacro.get(mMap.myMacro.size() - 1) < myMaps.sAction.length) {
                            myDo_Block(mMap.myMacro.get(mMap.myMacro.size() - 1), mMap.myMacro.get(mMap.myMacro.size() - 1), false, false);
                            m_lstMovReDo.clear();

                            mMap.invalidate();
                        } else {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        }
                    } else {
                        if (m_lstMovReDo.isEmpty()) {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        } else {
                            if (myMaps.m_Sets[23] == 1) m_nStep = 1;
                            else m_nStep = getStep(m_lstMovReDo);
                            mMap.Box_Row0 = -1;  //记录箱子移动前的位置
                            m_nLastSteps = m_lstMovUnDo.size();
                            UpData1(1);
                        }
                    }
                }
                m_bBusing = false;
            }
        });
        bt_ReDo.setLongClickable(true);
        bt_ReDo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                StopMicro();  //停止“宏”功能运行

                bt_Sel.setChecked(false);  //关闭计数状态
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_bNetLock = false;  //取消网型提示
                m_nStep = 0;
                m_bYanshi = false;
                m_bYanshi2 = false;
                mMap.m_lChangeBK = false;  //是否显示更换背景按钮
                m_bACT_ERROR = false;  //执行动作时是否遇到错误
                mMap.invalidate();

                if (bt_BK.isChecked()) {  //逆推
                    if (m_lstMovReDo2.isEmpty()) {
                        MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                    } else {
                        MyToast.showToast(myGameView.this, "进至尾！", Toast.LENGTH_SHORT);
                        if (m_lstMovUnDo2.isEmpty()) goHome();
                        m_nStep = m_lstMovReDo2.size();
                        m_bYanshi = false;
                        m_bYanshi2 = true;
                        mMap.curMoves = 0;
                        UpData3(1);
                    }
                } else {            //正推
                    if (myMaps.isMacroDebug) {
                        if (mMap.myMacro.get(mMap.myMacro.size() - 1) < myMaps.sAction.length) {
                            myDo_Block(mMap.myMacro.get(mMap.myMacro.size() - 1), myMaps.sAction.length - 1, false, false);
                            m_lstMovReDo.clear();

                            mMap.invalidate();
                        } else {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        }
                    } else {
                        if (m_lstMovReDo.isEmpty()) {
                            MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                        } else {
                            MyToast.showToast(myGameView.this, "正向演示！", Toast.LENGTH_SHORT);
                            m_nStep = m_lstMovReDo.size();
                            mMap.Box_Row0 = -1;  //记录箱子移动前的位置
                            m_bYanshi = true;
                            m_bYanshi2 = false;
                            mMap.curMoves = 0;
                            m_nLastSteps = -1;
                            UpData1(1);
                        }
                    }
                }
                m_bBusing = false;
                return true;
            }
        });
        bt_IM = (CheckBox) findViewById(R.id.cb_IM);  //瞬移
        bt_IM.setChecked(myMaps.m_Sets[6] == 1);  //上次瞬移开关状态
        if (myMaps.m_Sets[6] == 1)
            bt_IM.setBackgroundColor(0xff445566);
        else
            bt_IM.setBackgroundColor(0xff778899);
        bt_IM.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMap.d_Moves = mMap.m_PicWidth;
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                if (isChecked) {
                    buttonView.setBackgroundColor(0xff445566);
                    myMaps.m_Sets[6] = 1;  //记录瞬移开关状态，下次进入使用
                } else {
                    buttonView.setBackgroundColor(0xff778899);
                    myMaps.m_Sets[6] = 0;
                }
            }
        });
        bt_BK = (CheckBox) findViewById(R.id.cb_BK);  //逆推
        bt_BK.setChecked(false);  //默认正推模式
        bt_BK.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StopMicro();  //停止“宏”功能运行

                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                myMaps.isRecording = false;  //关闭录制模式
                mMap.d_Moves = mMap.m_PicWidth;
                m_bBusing = false;
                m_nStep = 0;   //结束所有推、移
                m_bYanshi = false;
                m_bYanshi2 = false;
                m_bACT_ERROR = false;  //执行动作时是否遇到错误
                if (isChecked) {
                    load_BK_Level(myMaps.m_Sets[13] == 1);  //加载逆推目标点（区分是否即景）
                    buttonView.setBackgroundColor(0xff445566);
                    if (m_nRow2 < 0 || m_nCol2 < 0) {  //首次进入逆推模式，需要确定仓管员位置
                        mMap.m_iR = m_nRow2;
                        mMap.m_iC = m_nCol2;
                        MyToast.showToast(myGameView.this, "需要给出仓管员的位置！", Toast.LENGTH_SHORT);
                    }
                } else {
                    load_Level(myMaps.m_Sets[25] == 1);  //加载正推目标点（区分是否即景）
                    buttonView.setBackgroundColor(0xff778899);
                    mMap.m_iR = m_nRow;
                    mMap.m_iC = m_nCol;
                }
                m_bNetLock = false;  //取消网型提示
                mMap.invalidate();
            }
        });
        bt_Sel = (CheckBox) findViewById(R.id.cb_Sel);  //计数
        bt_Sel.setChecked(false);  //默认
        bt_Sel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StopMicro();  //停止“宏”功能运行

                mMap.d_Moves = mMap.m_PicWidth;
                m_nStep = 0;
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_bYanshi = false;
                m_bYanshi2 = false;
                m_bBusing = false;
                //关闭进度条、奇偶位明暗度调整条
                mMap.m_lGoto = false;                   // 正推“跳至”
                mMap.m_lGoto2 = false;                  // 逆推“跳至”
                mMap.m_lParityBrightnessShade = false;  // 奇偶位明暗度调整

                //计数控制
                mMap.m_Count[0] = 0;  //正推箱子数
                mMap.m_Count[1] = 0;  //正推目标数
                mMap.m_Count[2] = 0;  //正推完成数
                mMap.m_Count[3] = 0;  //逆推箱子数
                mMap.m_Count[4] = 0;  //逆推目标数
                mMap.m_Count[5] = 0;  //逆推完成数
                for (int i = 0; i < m_cArray.length; i++)
                    for (int j = 0; j < m_cArray[0].length; j++) {
                        m_selArray[i][j] = 0;
                        bk_selArray[i][j] = 0;
                    }

                if (isChecked) {
                    buttonView.setBackgroundColor(0xff445566);
                    mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
                    mMap.m_boxNoMoved = false;  //关闭未动箱子提示状态
                    mMap.m_boxCanMove2 = false;  //关闭可动箱子提示状态
                    mMap.m_boxNoUsed = false;  //关闭未使用地板提示状态
                } else {
                    buttonView.setBackgroundColor(0xff778899);
                }
                m_bNetLock = false;  //取消网型提示
                mMap.m_lChangeBK = false;  //是否显示更换背景按钮
                mMap.invalidate();
            }
        });
        bt_TR = (CheckBox) findViewById(R.id.cb_TR);  //旋转地图
        bt_TR.setChecked(false);
        myMaps.m_nTrun = myMaps.curMap.Trun;
        bt_TR.setText(myMaps.m_nTrun + " 转");
        bt_TR.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StopMicro();

                myMaps.m_nTrun = (myMaps.m_nTrun + 1) % 8;

                myMaps.curMap.Trun = myMaps.m_nTrun;
                bt_TR.setText(myMaps.m_nTrun + " 转");
                mMap.initArena();
                mMap.invalidate();
                if (myMaps.m_nTrun == 0)
                    MyToast.showToast(myGameView.this, "第 0 转", Toast.LENGTH_SHORT);
            }
        });
        bt_TR.setLongClickable(true);
        bt_TR.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                StopMicro();

                myMaps.m_nTrun = 0;

                myMaps.curMap.Trun = myMaps.m_nTrun;
                bt_TR.setText(myMaps.m_nTrun + " 转");
                mMap.initArena();
                mMap.invalidate();
                if (myMaps.m_nTrun == 0)
                    MyToast.showToast(myGameView.this, "第 0 转", Toast.LENGTH_SHORT);

                return true;
            }
        });
        bt_More = (CheckBox) findViewById(R.id.cb_More);  //更多
        bt_More.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StopMicro();  //停止“宏”功能运行

                bt_Sel.setChecked(false);  //关闭计数状态
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_bNetLock = false;  //取消网型提示
                mMap.m_lChangeBK = false;  //是否显示更换背景按钮
                m_bACT_ERROR = false;  //执行动作时是否遇到错误
                mMap.invalidate();
                mMap.d_Moves = mMap.m_PicWidth;
                m_nStep = 0;
                m_bYanshi = false;
                m_bYanshi2 = false;
                m_bBusing = false;
                openOptionsMenu();  //菜单
            }
        });
        bt_More.setLongClickable(true);
        bt_More.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MyToast.showToast(myGameView.this, "可以离开了！", Toast.LENGTH_SHORT);
                StopMicro();  //停止“宏”功能运行

                bt_Sel.setChecked(false);  //关闭计数状态
                if (mMap.m_lShowAnsInf) mMap.m_lShowAnsInf = false; //有了动作后，自动关闭答案信息的显示
                m_bNetLock = false;  //取消网型提示
                mMap.m_lChangeBK = false;  //是否显示更换背景按钮
                m_bACT_ERROR = false;  //执行动作时是否遇到错误
                mMap.d_Moves = mMap.m_PicWidth;
                m_nStep = 0;
                m_bYanshi = false;
                m_bYanshi2 = false;
                m_bBusing = false;

                mMap.invalidate();

                if (m_bMoved && (m_lstMovUnDo.size() > 0 || m_lstMovUnDo2.size() > 0)) {
                    exitDlg.show();
                } else {
                    myStop();
                    finish();
                }

                return true;
            }
        });

        skinList();  //取得皮肤列表
        bkPicList();  //取得背景列表
        //按计算的地图尺寸，创建地图框架
        myMaps.m_nTrun = myMaps.curMap.Trun;
        myMaps.isHengping = false;  //不使用横屏皮肤
        m_iR9 = -1;
        m_iC9 = -1;
        m_iR10 = -1;
        m_iC10 = -1;
        initMap();
        m_bNetLock = false;  //取消网型提示

        closedDiagonalLock = new DiagonalLock(this);
        freezeDeadlock = new FreezeLock(this);
    }

    //逆推undo至首后，有可能更改了仓管员的位置，与redo不符，如此可调整到与redo相符的位置
    private void goHome() {
        if (m_nRow2 != m_nRow0 || m_nCol2 != m_nCol0) {  //若，仓管员的位置有更改
            if (m_nRow2 > -1) {  //非首次给仓管员定位
                if (bk_cArray[m_nRow2][m_nCol2] == '@')
                    bk_cArray[m_nRow2][m_nCol2] = '-';
                else
                    bk_cArray[m_nRow2][m_nCol2] = '.';
            }

            if (bk_cArray[m_nRow0][m_nCol0] == '-')
                bk_cArray[m_nRow0][m_nCol0] = '@';
            else
                bk_cArray[m_nRow0][m_nCol0] = '+';

            m_nRow2 = m_nRow0;
            m_nCol2 = m_nCol0;

            mMap.invalidate();
        }
    }

    private void myPre(int n) {
        bt_BK.setChecked(false);  //默认正推模式
        bt_Sel.setChecked(false);  //计数默认
        myMaps.curMap = null;
        myMaps.curMap = myMaps.m_lstMaps.get(n);
        myMaps.m_nTrun = myMaps.curMap.Trun;
        bt_TR.setText(myMaps.m_nTrun + " 转");
        initMap();
        ls_bk_cArray = bk_cArray;
    }

    private void myNext(int n) {
        bt_BK.setChecked(false);  //默认正推模式
        bt_Sel.setChecked(false);  //计数默认
        myMaps.curMap = null;
        myMaps.curMap = myMaps.m_lstMaps.get(n);
        myMaps.m_nTrun = myMaps.curMap.Trun;
        bt_TR.setText(myMaps.m_nTrun + " 转");
        initMap();
        ls_bk_cArray = bk_cArray;
    }

    //加载正推关卡目标点，flg 是否即景
    private void load_Level(boolean flg) {
        bt_Sel.setChecked(false);  //关闭计数状态
        //复位进度条
        mMap.curMoves = 0;

        char[][] ls_cArray;

        if (flg) ls_cArray = bk_cArray;  //即景正推模式且尚未正逆相合
        else ls_cArray = m_cArray0;  //标准正推模式

        m_nDstOK = 0;
        char ch;
        boolean flg2;
        for (int i = 0; i < myMaps.curMap.Rows; i++) {
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                //扫描新现场目标点
                flg2 = false;
                ch = ls_cArray[i][j];
                switch (ch) {
                    case '*':  //一定是目标点
                        flg2 = true;
                        break;
                    case '$':  //即景时，是目标点
                        if (flg) flg2 = true;
                        break;
                    case '.':  //非即景时，是目标点
                    case '+':
                        if (!flg) flg2 = true;
                        break;
                }
                //调整现场目标点
                ch = m_cArray[i][j];
                switch (ch) {
                    case '-':
                        if (flg2) m_cArray[i][j] = '.';
                        break;
                    case '$':
                        if (flg2) m_cArray[i][j] = '*';
                        break;
                    case '@':
                        if (flg2) m_cArray[i][j] = '+';
                        break;
                    case '.':
                        if (!flg2) m_cArray[i][j] = '-';
                        break;
                    case '*':
                        if (!flg2) m_cArray[i][j] = '$';
                        break;
                    case '+':
                        if (!flg2) m_cArray[i][j] = '@';
                        break;
                }
                if (m_cArray[i][j] == '*') m_nDstOK++;
            }
        }
    }

    //加载逆推关卡目标点，flg 标识是否即景
    private void load_BK_Level(boolean flg) {
        bt_Sel.setChecked(false);  //关闭计数状态
        //复位进度条
        mMap.curMoves2 = 0;

        char[][] ls_cArray;

        if (flg) ls_cArray = m_cArray;  //即景逆推模式
        else ls_cArray = m_cArray0;  //标准逆推模式

        m_nDstOK2 = 0;
        char ch;
        boolean flg2;
        for (int i = 0; i < myMaps.curMap.Rows; i++) {
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                //扫描新现场目标点
                flg2 = false;
                ch = ls_cArray[i][j];
                switch (ch) {
                    case '*':
                    case '$':
                        flg2 = true;
                        break;
                }
                //调整现场目标点
                ch = bk_cArray[i][j];
                switch (ch) {
                    case '-':
                        if (flg2) bk_cArray[i][j] = '.';
                        break;
                    case '$':
                        if (flg2) bk_cArray[i][j] = '*';
                        break;
                    case '@':
                        if (flg2) bk_cArray[i][j] = '+';
                        break;
                    case '.':
                        if (!flg2) bk_cArray[i][j] = '-';
                        break;
                    case '*':
                        if (!flg2) bk_cArray[i][j] = '$';
                        break;
                    case '+':
                        if (!flg2) bk_cArray[i][j] = '@';
                        break;
                }
                if (bk_cArray[i][j] == '*') m_nDstOK2++;
            }
        }
    }

    //标准正逆推关卡复位，用 flg 区分正逆推
    private void levelReset(boolean flg) {
        if (flg) {
            m_nStep = m_lstMovUnDo2.size();
            while (m_nStep > 0) unDo2();

            mMap.m_bBoxTo2 = false; //关闭箱子可达位置提示状态
            mMap.m_bManTo2 = false; //关闭可达位置提示状态
            mMap.m_boxCanMove2 = false;  //关闭可动箱子提示状态
            mMap.m_boxCanCome2 = false; //是否在提示可推过来的箱子状态

            mMap.m_iR = m_nRow2;
            mMap.m_iC = m_nCol2;
        } else {
            m_nStep = m_lstMovUnDo.size();
            while (m_nStep > 0) unDo1();

//            //宏调试时，若“从当前点”调试执行会需要如此
//            if (myMaps.m_ActionIsPos && myMaps.isMacroDebug) {
//                m_lstMovReDo = m_lstMovedHistory;
//                m_nStep = m_lstMovReDo.size();
//                while (m_nStep > 0) reDo1();
//            }

            mMap.m_bBoxTo = false; //关闭箱子可达位置提示状态
            mMap.m_bManTo = false; //关闭可达位置提示状态
            mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
            mMap.m_boxNoMoved = false;  //关闭未动箱子提示状态
            mMap.m_boxNoUsed = false;  //关闭未使用地板提示状态
            mMap.m_boxCanCome = false; //是否在提示可推过来的箱子状态

            mMap.m_iR = m_nRow;
            mMap.m_iC = m_nCol;
        }

        m_nStep = 0;

        m_bBusing = false;
        m_bMoved = false;
        myMaps.m_StateIsRedy = false;
        myMaps.m_Sets[14] = 2;
        myMaps.m_Sets[5] = 2;
    }

    //自动箱子编号及计数有效的箱子和目标点
    private void mBoxNum(char[][] level, int mr, int mc) {
        int nRows = level.length;
        int nCols = level[0].length;

        boolean[][] Mark = new boolean[nRows][nCols];    //标志数组，表示地图上某一位置Mark1[i][j]是否访问过。
        int F, mr2, mc2;

        int[] dr = {-1, 1, 0, 0, -1, 1, -1, 1};    //前四个是可移动方向，后面是四个临角
        int[] dc = {0, 0, 1, -1, -1, -1, 1, 1};

        Queue<Integer> P = new LinkedList<Integer>();
        P.offer(mr << 16 | mc);
        Mark[mr][mc] = true;
        while (!P.isEmpty()) { //走完后，Mark[][]为 1 的，为墙内
            F = P.poll();
            mr = F >>> 16;
            mc = F & 0x0000FFFF;
            for (int k = 0; k < 4; k++) {//仓管员向四个方向走
                mr2 = mr + dr[k];
                mc2 = mc + dc[k];
                if (mr2 < 0 || mr2 >= nRows || mc2 < 0 || mc2 >= nCols ||  //出界
                        Mark[mr2][mc2] || level[mr2][mc2] == '_' || level[mr2][mc2] == '#')
                    continue;  //已访问或遇到墙

                P.add(mr2 << 16 | mc2);
                Mark[mr2][mc2] = true;  //标记为已访问
            }
        }

        //箱子自动编号，统计目标点及完成数
        short box_Num = 0;
        m_nDstNum = 0;
        m_nDstOK = 0;
        m_nDstNum2 = 0;
        m_nDstOK2 = 0;
        for (int i = 0; i < nRows; i++) {
            for (j = 0; j < nCols; j++) {
                switch (level[i][j]) {
                    case '$':
                        if (Mark[i][j]) {
                            box_Num++;
                            m_iBoxNum2[i][j] = box_Num;
                        }
                        m_nDstNum2++;
                        break;
                    case '*':
                        if (Mark[i][j]) {
                            box_Num++;
                            m_iBoxNum2[i][j] = box_Num;
                        }
                        m_nDstOK++;
                        m_nDstNum++;
                        m_nDstOK2++;
                        m_nDstNum2++;
                        break;
                    case '.':
                    case '+':
                        m_nDstNum++;
                        break;
                }
            }
        }

    }

    private void newGame() {
        //本次游戏初始化（正逆推）
        m_bBusing = true;
        m_nRow = -1;
        m_nCol = -1;
        m_nRow2 = -1;
        m_nCol2 = -1;
        m_nRow0 = -1;
        m_nCol0 = -1;
        m_nRow4 = -1;
        m_nCol4 = -1;

        if (m_lstMovUnDo != null) m_lstMovUnDo.clear();
        if (m_lstMovReDo != null) m_lstMovReDo.clear();
        if (m_lstMovUnDo2 != null) m_lstMovUnDo2.clear();
        if (m_lstMovReDo2 != null) m_lstMovReDo2.clear();
        m_lstMovUnDo = new LinkedList<Byte>();
        m_lstMovReDo = new LinkedList<Byte>();
        m_lstMovUnDo2 = new LinkedList<Byte>();
        m_lstMovReDo2 = new LinkedList<Byte>();

        try {
            m_iBoxNum = new short[myMaps.curMap.Rows][myMaps.curMap.Cols];
            m_iBoxNum2 = new short[myMaps.curMap.Rows][myMaps.curMap.Cols];
            m_Freeze = new byte[myMaps.curMap.Rows][myMaps.curMap.Cols];
            m_cArray = new char[myMaps.curMap.Rows][myMaps.curMap.Cols];
            m_cArray0 = new char[myMaps.curMap.Rows][myMaps.curMap.Cols];
            bk_cArray = new char[myMaps.curMap.Rows][myMaps.curMap.Cols];
            m_selArray = new byte[myMaps.curMap.Rows][myMaps.curMap.Cols];
            bk_selArray = new byte[myMaps.curMap.Rows][myMaps.curMap.Cols];
            ls_bk_cArray = bk_cArray;

            mark7 = new boolean[myMaps.curMap.Rows][myMaps.curMap.Cols];
            mark8 = new boolean[myMaps.curMap.Rows][myMaps.curMap.Cols];
            mark11 = new boolean[myMaps.curMap.Rows][myMaps.curMap.Cols];
            mark12 = new boolean[myMaps.curMap.Rows][myMaps.curMap.Cols];
            mark44 = new boolean[myMaps.curMap.Rows][myMaps.curMap.Cols];
            mark41 = new byte[myMaps.curMap.Rows][myMaps.curMap.Cols];

            String[] Arr = myMaps.curMap.Map.split("\r\n|\n\r|\n|\r|\\|");
            char ch;
            for (int i = 0; i < myMaps.curMap.Rows; i++) {
                for (int j = 0; j < myMaps.curMap.Cols; j++) {
                    m_iBoxNum[i][j] = -1;
                    m_iBoxNum2[i][j] = -1;
                    ch = Arr[i].charAt(j);
                    switch (ch) {
                        case '#':
                        case '_':
                            m_cArray[i][j] = ch;
                            m_cArray0[i][j] = ch;
                            bk_cArray[i][j] = ch;
                            break;
                        case '-':
                            m_cArray[i][j] = ch;
                            m_cArray0[i][j] = ch;
                            bk_cArray[i][j] = ch;
                            mark8[i][j] = true;
                            break;
                        case '*':
                            m_cArray[i][j] = ch;
                            m_cArray0[i][j] = ch;
                            bk_cArray[i][j] = ch;
                            mark7[i][j] = true;
                            break;
                        case '+':
                            m_nRow = i;
                            m_nCol = j;
                            m_cArray[i][j] = ch;
                            m_cArray0[i][j] = ch;
                            bk_cArray[i][j] = '$';
                            break;
                        case '.':
                            m_nDstNum++;
                            m_cArray[i][j] = ch;
                            m_cArray0[i][j] = ch;
                            bk_cArray[i][j] = '$';
                            mark8[i][j] = true;
                            break;
                        case '$':
                            m_cArray[i][j] = ch;
                            m_cArray0[i][j] = ch;
                            bk_cArray[i][j] = '.';
                            mark7[i][j] = true;
                            break;
                        case '@':
                            m_nRow = i;
                            m_nCol = j;
                            m_cArray[i][j] = ch;
                            m_cArray0[i][j] = ch;
                            bk_cArray[i][j] = '-';
                    }
                }
            }

            mark14 = null;
            mark15 = null;
            mark16 = null;
            mArray9 = null;

            //箱子自动编号只针对人的活动范围内的箱子，手动编号针对“全部”箱子（包括墙外的箱子）
            mBoxNum(m_cArray, m_nRow, m_nCol);

            mPathfinder = new myPathfinder(myMaps.curMap.Rows, myMaps.curMap.Cols);

            m_iStep[0] = 0;      //记录正推"推"、"移"的步数
            m_iStep[1] = 0;
            m_Gif_Start = 0;  //导出 GIF 片段的开始点复位
            m_iStep[2] = 0;      //记录逆推"推"、"移"的步数
            m_iStep[3] = 0;
            m_nStep = 0;

            mMap.m_iR = m_nRow;
            mMap.m_iC = m_nCol;
            m_nRow3 = m_nRow;
            m_nCol3 = m_nCol;

            mMap.m_bBoxTo = false; //关闭箱子可达位置提示状态
            mMap.m_bBoxTo2 = false;
            mMap.m_bManTo = false; //关闭可达位置提示状态
            mMap.m_bManTo2 = false;
            mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
            mMap.m_boxCanMove2 = false;
            mMap.m_boxNoMoved = false;  //关闭未动箱子提示状态
            mMap.m_boxNoUsed = false;  //关闭未使用地板提示状态
            mMap.m_boxCanCome = false; //是否在提示可推过来的箱子状态
            mMap.m_boxCanCome2 = false;
            //关闭进度条、奇偶位明暗度调整条
            mMap.m_lGoto = false;                   // 正推“跳至”
            mMap.m_lGoto2 = false;                  // 逆推“跳至”
            mMap.m_lParityBrightnessShade = false;  // 奇偶位明暗度调整
            myMaps.m_bBiaochi = false;
            myMaps.m_bBianhao = false;

            m_bBusing = false;
            m_bMoved = false;
            m_bYanshi = false;
            m_bYanshi2 = false;
            myMaps.m_StateIsRedy = false;
            m_imPort_YASS = "";
            myMaps.m_Sets[14] = 2;
            myMaps.m_Sets[5] = 2;
            m_nLastSteps = -1;
        } catch (Throwable ex) {
            myStop();
            myMaps.curMap.Title = "无效关卡";
            myMaps.curMap.Map = "--";
            myMaps.curMap.Rows = 1;
            myMaps.curMap.Cols = 2;
//            finish();
        }
    }

    private void initMap() {
        if (mTask != null) {
            if (!mTask.isCancelled() && mTask.getStatus() == AsyncTask.Status.RUNNING) {
                mTask.cancel(true);
            }
            mTask = null;
        }

        StopMicro();

        newGame();
        mMap.m_lChangeBK = false;  //是否显示更换背景按钮
        myMaps.isRecording = false;  //关闭录制模式
        mMap.d_Moves = mMap.m_PicWidth;
        myMaps.m_Sets[25] = 0;  //加载新的关卡时，关闭即景正推
        levelReset(false);  //因为有了即景正推，计算静态死锁点中有正逆推的临时转换，影响正推箱子目标的复位，故此处特别进行正推关卡复位

        //舞台初始化
        mMap.initArena();

        //记录关卡打开时间，便于遍历“最近打开的关卡”
        mySQLite.m_SQL.Set_L_DateTime(myMaps.curMap.Level_id);

        //取得状态及答案列表
        mySQLite.m_SQL.load_StateList(myMaps.curMap.Level_id, myMaps.curMap.key);

        if (myMaps.mState2.size() > 0) {  // 若有答案
            myMaps.m_State = mySQLite.m_SQL.load_State(myMaps.mState2.get(0).id);
            if (myMaps.m_State.ans.length() > 0) formatPath(myMaps.m_State.ans, false);
        } else if (myMaps.m_Sets[37] == 1 && myMaps.mState1.size() > 0) {  // 若无答案，自动加载最新状态
            // 按保存时间排序，第一个为最新状态
            Collections.sort(myMaps.mState1, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    return ((state_Node) o2).time.compareTo(((state_Node) o1).time);
                }
            });
            myMaps.m_State = mySQLite.m_SQL.load_State(myMaps.mState1.get(0).id);
            m_nLastSteps = -1;
            int len = myMaps.m_State.ans.length();
            if (len > 0) {
                formatPath(myMaps.m_State.ans, false);
                if (myMaps.m_State.time.toLowerCase().indexOf("yass") >= 0) {
                    m_imPort_YASS = "[YASS]";
                } else if (myMaps.m_State.time.toLowerCase().indexOf("导入") >= 0) {
                    m_imPort_YASS = "[导入]";
                } else {
                    m_imPort_YASS = "";
                }
                len = m_lstMovReDo.size();
                for (int k = 0; k < len; k++) reDo1();
                m_bBusing = false;
            }

            len = myMaps.m_State.bk_ans.length();
            if (len > 0) {
                try {
                    m_nRow0 = myMaps.m_State.r;
                    m_nCol0 = myMaps.m_State.c;
                    m_nRow2 = m_nRow0;
                    m_nCol2 = m_nCol0;
                    bk_cArray[m_nRow2][m_nCol2] = (bk_cArray[m_nRow2][m_nCol2] == '-' ? '@' : '+');
                    formatPath(myMaps.m_State.bk_ans, true);
                    len = m_lstMovReDo2.size();
                    for (int k = 0; k < len; k++) reDo2();
                    m_bBusing = false;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    m_nRow0 = -1;
                    m_nCol0 = -1;
                    m_nRow2 = m_nRow0;
                    m_nCol2 = m_nCol0;
                    m_lstMovReDo2.clear();
                }
            }
        }

        //异步进程计算逆推的死锁点，比较耗时
        mTask = new AsyncCountBoxsTask(this);
        mTask.execute(myMaps.curMap.Rows, myMaps.curMap.Cols);

        m_bACT_ERROR = false;  //执行动作时是否遇到错误
        myMaps.isMacroDebug = false;  //单步宏
        mMap.m_lShowAnsInf = false; //是否允许在开始推关卡之前显示答案信息
    }

    //是否已经访问过
    private boolean isVisited(byte[][] m_Mrk, int mR, int mC) {
        try {
            return m_Mrk[mR][mC] > 0;
        } catch (ArrayIndexOutOfBoundsException ex) {
        } catch (Throwable ex) {
        }
        return true;
    }

    //是否是箱子、墙壁、墙外、界外
    private boolean isWall_Box(char[][] m_Arr, int mR, int mC) {
        try {
            if (m_Arr[mR][mC] == '#' || m_Arr[mR][mC] == '_' || m_Arr[mR][mC] == '$' || m_Arr[mR][mC] == '*')
                return true;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return true;
        } catch (Throwable ex) {
            return true;
        }
        return false;
    }

    //是否是墙壁、墙外
    private boolean isWall(char[][] m_Arr, int mR, int mC) {
        try {
            if (m_Arr[mR][mC] == '#' || m_Arr[mR][mC] == '_') return true;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return true;
        } catch (Throwable ex) {
            return true;
        }
        return false;
    }

    //箱子是否不在目标点上
    private boolean isBox(char[][] m_Arr, int mR, int mC) {
        try {
            if (m_Arr[mR][mC] == '$') return true;
        } catch (ArrayIndexOutOfBoundsException ex) {
        } catch (Throwable ex) {
        }
        return false;
    }

    //箱子是否在目标点上
    private boolean isBox_Goal(char[][] m_Arr, int mR, int mC) {
        try {
            if (m_Arr[mR][mC] == '*') return true;
        } catch (ArrayIndexOutOfBoundsException ex) {
        } catch (Throwable ex) {
        }
        return false;
    }

    //是否是通道或仓管员
    private boolean isFloor2(char[][] m_Arr, int mR, int mC) {
        try {
            if (m_Arr[mR][mC] == '-' || m_Arr[mR][mC] == '.' || m_Arr[mR][mC] == '@' || m_Arr[mR][mC] == '+')
                return true;
        } catch (ArrayIndexOutOfBoundsException ex) {
        } catch (Throwable ex) {
        }
        return false;
    }

    //是否是地板
    private boolean isFloor1(char[][] m_Arr, int mR, int mC) {
        try {
            if (m_Arr[mR][mC] == '-' || m_Arr[mR][mC] == '@') return true;
        } catch (ArrayIndexOutOfBoundsException ex) {
        } catch (Throwable ex) {
        }
        return false;
    }

    //检查区域内箱子数是否超标，超标即视为死锁（此理论暂时仅用于逆推中，理论和算法均待验）
    private boolean isLock_Count(char[][] m_Arr, short[][] m_mark, int mR, int mC, int mRow, int mCol) {
        for (int i = 0; i < myMaps.curMap.Rows; i++) {
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                mPathfinder.mark1[i][j] = false;
            }
        }

        mArray9[mR][mC] = '$';
        mPF.boxReachable(true, mR, mC, mRow, mCol);  //计算箱子范围
        mArray9[mR][mC] = '-';

        //排查可达点的四邻（用循环取代递归）
        boolean is_ALL_OK = true;  //区域内的箱子是否全部在目标点上
        int n = 0, n2 = 0;  //记录区域内的箱子数、邻域死点上的箱子（此时是死点上的目标点）
        int i1, j1;
        int p = 0, tail = 0;
        mPathfinder.pt[0] = mRow << 16 | mCol;
        mPathfinder.mark1[mPathfinder.pt[0] >>> 16][mPathfinder.pt[0] & 0x0000ffff] = true;  //临时使用，记录格子是否被检查过了
        for (; p <= tail; ) {
            for (; p <= tail; ) {
                for (int k = 0; 4 > k; k++) {
                    try {
                        i1 = (mPathfinder.pt[p] >>> 16) + dr4[k];
                        j1 = (mPathfinder.pt[p] & 0x0000ffff) + dc4[k];
                        if (!mPF.mark4[i1][j1]) continue;  //对于非可达范围，不检查
                        if (m_mark[mR][mC] == m_mark[i1][j1] && !mPathfinder.mark1[i1][j1]) {  //仅在区域内没有检查过的格子
                            tail++;
                            mPathfinder.pt[tail] = i1 << 16 | j1;
                            mPathfinder.mark1[i1][j1] = true;

                            //计数区域内的箱子
                            if (!mark15[i1][j1] && (m_Arr[i1][j1] == '$' || m_Arr[i1][j1] == '*'))
                                n++;
                            //死点上的目标点（）
                            if (mark15[i1][j1] && m_Arr[i1][j1] == '.') n--;

                            //检测到了不在目标点上的箱子
                            if (is_ALL_OK && m_Arr[i1][j1] == '$') is_ALL_OK = false;
                        }
                        //死点上的目标点（一般在邻域--不属于一个区域，若存在较远的，可能调整不过来）
                        if (mark15[i1][j1] && !mPathfinder.mark1[i1][j1] && m_Arr[i1][j1] == '.')
                            n2++;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                    } catch (Throwable ex) {
                    }
                }
                p++;
            }
        }

        //全部箱子均在目标点上，则不为死锁
        if (is_ALL_OK) return false;

        //区域内的箱子数是否多于允许数
        return (n > m_mark[mR][mC] + n2);
    }

    //正推目标点不够用之死锁
    private boolean isLock_Goal(char[][] m_Arr, int bRow, int bCol, int mRow, int mCol) {
        //检查死锁用的探路者 mPF
        mArray9[bRow][bCol] = '$';

        mPF.boxReachable(false, bRow, bCol, mRow, mCol);  //计算箱子可达点

        //若箱子的打开范围内目标点数不够用了，则死锁
        int n1 = 0, n2 = 0;
        for (int r = 0; r < myMaps.curMap.Rows; r++) {
            for (int c = 0; c < myMaps.curMap.Cols; c++) {
                if (mPF.mark3[r][c]) {  //可达
                    //在目标点上的箱子，就不用数了
                    if (m_Arr[r][c] == '$') n1++;
                    if (m_Arr[r][c] == '.' || m_Arr[r][c] == '+') n2++;
                }
            }
        }

        mArray9[bRow][bCol] = '-';

        return n1 > n2;
    }

     //逆推“网”型死锁
    private boolean isLock_Net2(char[][] m_Arr, int b_new_Row, int b_new_Col) {
        int mRows = m_Arr.length, mCols = m_Arr[0].length;

        //访问标志复位
        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < mCols; j++) {
                mark41[i][j] = 0;
            }
        }

        Queue<Integer> Q = new LinkedList<Integer>();

        int P, r = b_new_Row, c = b_new_Col, r1, c1, r2, c2;

        Q.offer(r << 16 | c);  //初始位置入队列，待查其四邻
        mark41[r][c] = 1;
        boolean flg = false;  //网内是否包含没有归位的目标点

        while (!Q.isEmpty()) {
            P = Q.poll();//出队列
            r = P >>> 16;
            c = P & 0x0000ffff;

            flg |= (!isBox_Goal(m_Arr, r, c));  //网内是否包含没有归位的目标点

            for (int k = 0; k < 4; k++) {
                r1 = r + dr4[k];
                c1 = c + dc4[k];
                r2 = r + dr4[k] * 2;
                c2 = c + dc4[k] * 2;

                if (isVisited(mark41, r2, c2) || isWall(m_Arr, r2, c2) || isWall(m_Arr, r1, c1)) {  //箱子的一侧临墙
                    continue;
                } else if (isFloor2(m_Arr, r2, c2)) {  //有网口（应该是'.'、'-'、'+'、'@'）
                    return false;
                } else {  //没有访问过的箱子（应该是'$'、'*'）
                    Q.offer(r2 << 16 | c2);
                    mark41[r2][c2] = 1;
                }
            }
        }

        return flg;
    }

    //计算网点、网口
    public void m_Net_Inf(char[][] m_Arr, int b_Row, int b_Col) {

        int mRows = m_Arr.length, mCols = m_Arr[0].length;
        int P, r = b_Row, c = b_Col, r1, c1, r2, c2;

        //访问标志复位
        for (int i = 0; i < mRows; i++) {
            for (int j = 0; j < mCols; j++) {
                mark41[i][j] = 0;
            }
        }

        Queue<Integer> Q = new LinkedList<Integer>();

        Q.offer(r << 16 | c);  //初始位置入队列，待查其四邻
        mark41[r][c] = 1;

        while (!Q.isEmpty()) {
            P = Q.poll();//出队列
            r = P >>> 16;
            c = P & 0x0000ffff;

            for (int k = 0; k < 4; k++) {
                r1 = r + dr4[k];
                c1 = c + dc4[k];
                r2 = r + dr4[k] * 2;
                c2 = c + dc4[k] * 2;

                if (isVisited(mark41, r2, c2) || isWall(m_Arr, r2, c2) || isWall(m_Arr, r1, c1)) {  //箱子的一侧临墙
                    continue;
                } else if (isFloor1(m_Arr, r2, c2) || isBox(m_Arr, r2, c2)) {  //网口
                    mark41[r2][c2] = 2;
                } else {  //没有访问过的格子（应该是'.'或'*'，不会是'+'）
                    Q.offer(r2 << 16 | c2);
                    mark41[r2][c2] = 1;  //网点
                }
            }
        }
    }

    //正推死锁识别
    private boolean myLock(int bRow, int bCol) {
        if (m_iR9 > -1) {
            if (mArray9 != null && isLock_Goal(m_cArray, bRow, bCol, m_nRow, m_nCol)) {
                lockDlg.setMessage("这一步造成关卡死锁，继续吗？\n（点位不足）");
                return true;
            } else if (freezeDeadlock.isDeadlock(bRow, bCol)) {
                lockDlg.setMessage("这一步造成关卡死锁，继续吗？\n（僵位冻结）");
                return true;
            } else if (closedDiagonalLock.isDeadlock(bRow * myMaps.curMap.Cols + bCol)) {
                lockDlg.setMessage("这一步造成关卡死锁，继续吗？\n（闭锁对角）");
                return true;
            }
        }
        return false;
    }

    //逆推死锁识别
    private boolean myLock2(int bRow, int bCol) {
        if (m_iR10 > -1 && mark14 != null) {
            if (mArray9 != null && isLock_Count(bk_cArray, mark14, bRow, bCol, m_nRow2, m_nCol2)) {
                lockDlg.setMessage("这一步造成关卡死锁，继续吗？\n（点位不足）");
                return true;
            } else if (isLock_Net2(bk_cArray, bRow, bCol)) {  //“网”型死锁
                lockDlg.setMessage("这一步造成关卡死锁，继续吗？\n（网位互锁）");
                return true;
            }
        }
        return false;
    }

    //设置进度条
    private void mySetProgressBar() {
        //进度条
        m_nLastSteps = -1;
        if (bt_BK.isChecked()) {  //逆推
            int len = m_lstMovUnDo2.size();
            int len2 = m_lstMovUnDo2.size() + m_lstMovReDo2.size();
            if (len2 > 0) {
                mMap.curMoves2 = (int) (((double) len / len2) * (mMap.stRight - mMap.stLeft));
                mMap.m_lGoto2 = true;
            } else {
                MyToast.showToast(myGameView.this, "尚无动作可用！", Toast.LENGTH_SHORT);
            }
        } else {  //正推
            int len = m_lstMovUnDo.size();
            int len2 = m_lstMovUnDo.size() + m_lstMovReDo.size();
            if (len2 > 0) {
                mMap.curMoves = (int) (((double) len / len2) * (mMap.stRight - mMap.stLeft));
                mMap.m_lGoto = true;
            } else {
                MyToast.showToast(myGameView.this, "尚无动作可用！", Toast.LENGTH_SHORT);
            }
        }
        mMap.invalidate();
    }

    //取得"皮肤/"文件夹下的文档列表
    private void skinList() {
        File targetDir = new File(myMaps.sRoot + myMaps.sPath + "皮肤/");
        myMaps.mFile_List1.clear();
        myMaps.mFile_List1.add("默认皮肤");
        if (!targetDir.exists()) targetDir.mkdirs();  //创建"皮肤/"文件夹
        else {
            String[] filelist = targetDir.list();
            Arrays.sort(filelist, String.CASE_INSENSITIVE_ORDER);
            for (int i = 0; i < filelist.length; i++) {
                int dot = filelist[i].lastIndexOf('.');
                if ((dot > -1) && (dot < (filelist[i].length()))) {
                    String prefix = filelist[i].substring(filelist[i].lastIndexOf(".") + 1);
                    if (prefix.equalsIgnoreCase("png"))
                        myMaps.mFile_List1.add(filelist[i]);
                }
            }
        }
    }

    //取得"皮肤/"文件夹下的文档列表
    private void bkPicList() {
        File targetDir = new File(myMaps.sRoot + myMaps.sPath + "背景/");
        myMaps.mFile_List2.clear();
        myMaps.mFile_List2.add("使用背景色");
        if (!targetDir.exists()) targetDir.mkdirs();  //创建"皮肤/"文件夹
        else {
            String[] filelist = targetDir.list();
            Arrays.sort(filelist, String.CASE_INSENSITIVE_ORDER);
            for (int i = 0; i < filelist.length; i++) {
                int dot = filelist[i].lastIndexOf('.');
                if ((dot > -1) && (dot < (filelist[i].length()))) {
                    String prefix = filelist[i].substring(filelist[i].lastIndexOf(".") + 1);
                    if (prefix.equalsIgnoreCase("jpg") || prefix.equalsIgnoreCase("bmp") || prefix.equalsIgnoreCase("png"))
                        myMaps.mFile_List2.add(filelist[i]);
                }
            }
        }
    }

    //正推通关
    private boolean myClearance() {
        //判断是否过关
        if ((myMaps.m_Sets[25] == 0 || m_iStep[2] <= 0) && m_nDstOK == m_nDstNum && m_iStep[0] > 0)
            return true;
        return false;
    }

    //逆推通关
    private boolean myClearance2() {
        //判断是否过关，逆推时，需要仓管员最后能回到正推地图中仓管员的初始位置
        if ((myMaps.m_Sets[13] == 0 || m_iStep[0] <= 0) && m_nDstOK2 == m_nDstNum2 && m_iStep[2] > 0 && mPathfinder.manTo2(true, bk_cArray, -1, -1, m_nRow2, m_nCol2, m_nRow3, m_nCol3))
            return true;
        return false;
    }

    //判断正逆推是否已经相合（正逆推箱子位置对应，人的位置互通）
    private boolean myMeet() {
        boolean flg = true;
        if (m_iStep[2] < 1 || m_bYanshi || m_bYanshi2)
            return false;

        for (int i = 0; i < m_cArray.length; i++) {
            for (int j = 0; j < m_cArray[0].length; j++) {
                if ((m_cArray[i][j] == '$' || m_cArray[i][j] == '*') && bk_cArray[i][j] != '$' && bk_cArray[i][j] != '*') {
                    return false;
                }
            }
        }
        if (m_nRow2 != m_nRow || m_nCol2 != m_nCol) {
            if (bt_BK.isChecked())  //逆推
                flg = mPathfinder.manTo2(true, bk_cArray, -1, -1, m_nRow2, m_nCol2, m_nRow, m_nCol);
            else
                flg = mPathfinder.manTo2(false, m_cArray, -1, -1, m_nRow, m_nCol, m_nRow2, m_nCol2);

        }
        if (flg) {
            m_nStep = 0;
            if (bt_BK.isChecked()) { //逆推
                mMap.m_iR = m_nRow2;
                mMap.m_iC = m_nCol2;
            } else {
                mMap.m_iR = m_nRow;
                mMap.m_iC = m_nCol;
            }
            myMaps.m_Sets[25] = 0;  //正逆相合后，即可取消即景正推，否则影响正推答案演示
            load_Level(false);  //加载正常正推目标点（取消即景）
        }
        return flg;
    }

    public void DoEvent(int act) {
        mMap.m_bBoxTo = false; //关闭箱子可达位置提示状态
        mMap.m_bBoxTo2 = false;
        mMap.m_bManTo = false; //关闭可达位置提示状态
        mMap.m_bManTo2 = false;
        mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
        mMap.m_boxCanMove2 = false;
        m_bNetLock = false;  //取消网型提示
        mMap.invalidate();
        switch (act) {
            case 0:  //即景模式转换
                if (bt_BK.isChecked()) {
                    if (myMaps.m_Sets[13] == 0) {
                        myMaps.m_Sets[13] = 1;
                        load_BK_Level(true);  //加载即景逆推关卡目标点
                    } else {
                        myMaps.m_Sets[13] = 0;
                        load_BK_Level(false);  //加载标准逆推关卡目标点
                    }
                } else {
                    if (myMaps.m_Sets[25] == 0) {
                        myMaps.m_Sets[25] = 1;
                        load_Level(true);  //加载即景正推目标点
                    } else {
                        myMaps.m_Sets[25] = 0;
                        load_Level(false);  //加载标准正推目标点
                    }
                }
                mMap.invalidate();
                break;
            case 1:  //推、移  -- 正推
                mMap.Box_Row0 = -1;  //记录箱子移动前的位置
                m_nLastSteps = m_lstMovUnDo.size();
                UpData1(1);
                break;
            case 2:  //推、移  -- 逆推
                UpData3(1);
                break;
            case 3:  //undo 长按
                bt_UnDo.onKeyLongPress(0, null);
                break;
            case 4:  //redo 长按
                bt_ReDo.onKeyLongPress(0, null);
                break;
            case 5:  //undo
                bt_UnDo.setChecked(!bt_UnDo.isChecked());
                break;
            case 6:  //redo
                bt_ReDo.setChecked(!bt_ReDo.isChecked());
                break;
            case 7:  //上一关
                StopMicro();
                int n1 = myMaps.m_lstMaps.indexOf(myMaps.curMap);

                if (n1 > 0) {
                    if (m_bMoved) exitDlg2.show();
                    else myPre(n1-1);
                } else {
                    MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                }
                break;
            case 8:  //下一关
                StopMicro();
                int n2 = myMaps.m_lstMaps.indexOf(myMaps.curMap);

                if (n2 >= 0 && n2+1 < myMaps.m_lstMaps.size()) {
                    if (m_bMoved) exitDlg3.show();
                    else myNext(n2+1);
                } else {
                    MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
                }
                break;
            case 9:  //将录制的动作送入剪切板，并打开“导入”窗口

                setACT(true);  //录制有效

                myMaps.m_ActionIsRedy = false;

                if (bt_BK.isChecked())
                    myMaps.m_nRecording_Bggin2 = m_lstMovUnDo2.size();  //逆推录制起始点
                else
                    myMaps.m_nRecording_Bggin = m_lstMovUnDo.size();  //正推录制起始点

                Intent intent2 = new Intent(this, myActGMView.class);
                //用Bundle携带数据
                Bundle bundle = new Bundle();
                bundle.putBoolean("is_BK", bt_BK.isChecked());  //传递参数：是否逆推
                bundle.putString("LOCAL", myMaps.getLocale(m_cArray));  //关卡现场地图数据
                intent2.putExtras(bundle);

                intent2.setClass(myGameView.this, myActGMView.class);
                startActivity(intent2);

                break;
            case 10:  //宏调试（单步执行）时，结束调试，否则加载并执行宏
                if (myMaps.isMacroDebug) {
                    final CheckBox isBack = new CheckBox(this);
                    isBack.setText("关卡回到该“宏”打开前的状态");
                    isBack.setChecked(true);
                    new Builder(myGameView.this, AlertDialog.THEME_HOLO_DARK).setTitle("关闭调试")
                        .setView(isBack)
                        .setPositiveButton("确定", new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (isBack.isChecked()) {  //回到宏打开前的关卡状态
                                    levelReset(false);  //正推关卡复位
                                    if (myMaps.m_Sets[25] == 1) {
                                        load_Level(true);  //加载即景正推目标点
                                    }
                                }
                                myMaps.isMacroDebug = false;
                                mMap.invalidate();
                            }
                        })
                        .setNegativeButton("取消", null).setCancelable(false)
                        .show();
                } else {
                    StopMicro();
                    mLoad_Do_Macro();
                }
                break;
            case 11:  //进度条
                if (myMaps.isMacroDebug) {
                    MyToast.showToast(myGameView.this, "“宏”调试时，不支持此功能！", Toast.LENGTH_SHORT);
                } else {
                    if (bt_BK.isChecked()) {  // 逆推
                        if (mMap.m_lGoto2) {
                            mMap.m_lGoto2 = false;
                        } else {
                            mySetProgressBar();
                        }
                    } else {  // 正推
                        if (mMap.m_lGoto) {
                            mMap.m_lGoto = false;
                        } else {
                            mySetProgressBar();
                        }
                    }
                }
                break;
            case 12:  //“奇偶格模式”开关
                if (myMaps.m_Sets[38] == 1) {
                    myMaps.m_Sets[38] = 0;
                    MyToast.showToast(myGameView.this, "奇偶格模式 - 关", Toast.LENGTH_SHORT);
                } else {
                    myMaps.m_Sets[38] = 1;
                    MyToast.showToast(myGameView.this, "奇偶格模式 - 开", Toast.LENGTH_SHORT);
                }
                break;
        }
    }

    //将动作送入导入窗口
    private void setACT(boolean flg) {
        StringBuilder s1 = new StringBuilder();  //已做动作
        StringBuilder s2 = new StringBuilder();  //后续动作
        try {  //将已做动作、后续动作保存到系统缓存文件中
            char[] Move = {'l', 'u', 'r', 'd', 'L', 'U', 'R', 'D'};
            Byte t;
            Iterator myItr;
            if (bt_BK.isChecked()) {  //逆推
                if (!m_lstMovUnDo2.isEmpty()) {
                    myItr = m_lstMovUnDo2.iterator();
                    while (myItr.hasNext()) {
                        t = (Byte)myItr.next();
                        s1.append(Move[t - 1]);
                    }
                }
                if (myMaps.isRecording && flg) {  //录制模式并且录制有效
                    if (!m_lstMovUnDo2.isEmpty() && myMaps.m_nRecording_Bggin2 < m_lstMovUnDo2.size()) {
                        myItr = m_lstMovUnDo2.iterator();
                        for (int k = 0; k < myMaps.m_nRecording_Bggin2; k++) {
                            if (myItr.hasNext()) myItr.next();
                            else break;
                        }
                        while (myItr.hasNext()) {
                            t = (Byte)myItr.next();
                            s2.append(Move[t - 1]);
                        }
                    }
                } else {  //非录制模式
                    if (!m_lstMovReDo2.isEmpty()) {
                        myItr = m_lstMovReDo2.descendingIterator();
                        while (myItr.hasNext()) {
                            t = (Byte)myItr.next();
                            s2.append(Move[t - 1]);
                        }
                    }
                }
            } else {  //正推
                if (!m_lstMovUnDo.isEmpty()) {
                    myItr = m_lstMovUnDo.iterator();
                    while (myItr.hasNext()) {
                        t = (Byte)myItr.next();
                        s1.append(Move[t - 1]);
                    }
                }
                if (myMaps.isRecording && flg) {  //录制模式并且录制有效
                    if (!m_lstMovUnDo.isEmpty() && myMaps.m_nRecording_Bggin < m_lstMovUnDo.size()) {
                        myItr = m_lstMovUnDo.iterator();
                        for (int k = 0; k < myMaps.m_nRecording_Bggin; k++) {
                            if (myItr.hasNext()) myItr.next();
                            else break;
                        }
                        while (myItr.hasNext()) {
                            t = (Byte)myItr.next();
                            s2.append(Move[t - 1]);
                        }
                    }
                } else {  //非录制模式
                    if (!m_lstMovReDo.isEmpty()) {
                        myItr = m_lstMovReDo.descendingIterator();
                        while (myItr.hasNext()) {
                            t = (Byte)myItr.next();
                            s2.append(Move[t - 1]);
                        }
                    }
                }
            }
            ////传递参数：将 undo、redo 动作序列送入临时寄存器
            SharedPreferences.Editor editor = getSharedPreferences("BoxMan", Context.MODE_PRIVATE).edit();
            editor.putString("act1", s1.toString());  //寄存：已做动作
            editor.putString("act2", s2.toString());  //寄存：后续动作（录制模式时，为录制的动作）
            editor.commit();
        } catch (Throwable ex) {
        }
    }

    //解析正推 reDo 动作节点 -- 每推一个箱子为一个动作
    private int getStep(LinkedList<Byte> m_lstMove) {
        if (m_nDstNum == 1) return m_lstMove.size();

        int len = m_lstMove.size();

        int[] boxRC = {1000, 1000};
        int i = 0, j = 0;
        byte mDir;

        //寻找动作节点
        int n = 0, k = 0;  //应该停在第几个动作上
        boolean flg = false;

        Iterator descItr = m_lstMove.descendingIterator();
        while (descItr.hasNext()) {
            mDir = (Byte)descItr.next();
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

    //解析正推 unDo 动作节点 -- 每推一个箱子为一个动作
    private int getStep2(LinkedList<Byte> m_lstMove) {
        if (m_nDstNum == 1) return m_lstMove.size();

        int len = m_lstMove.size();

        int[] boxRC = {1000, 1000};
        int i = 0, j = 0;
        byte mDir;

        //划分动作节点
        int n = 0, k = 0;
        boolean flg = false;
        Iterator descItr = m_lstMove.descendingIterator();
        while (descItr.hasNext()) {
            mDir = (Byte)descItr.next();
            k++;
            switch (mDir) {
                case 1:
                    j--;
                    break;
                case 2:
                    i--;
                    break;
                case 3:
                    j++;
                    break;
                case 4:
                    i++;
                    break;
                case 5:
                    if (boxRC[0] != i || boxRC[1] != j + 1) {
                        if (flg) return n;  //第二个箱子
                        flg = true;         //第一个箱子
                    }
                    n = k;                  //第一个箱子的最后位置
                    boxRC[0] = i;
                    boxRC[1] = j;
                    j--;
                    break;
                case 6:
                    if (boxRC[0] != i + 1 || boxRC[1] != j) {
                        if (flg) return n;  //第二个箱子
                        flg = true;         //第一个箱子
                    }
                    n = k;                  //第一个箱子的最后位置
                    boxRC[0] = i;
                    boxRC[1] = j;
                    i--;
                    break;
                case 7:
                    if (boxRC[0] != i || boxRC[1] != j - 1) {
                        if (flg) return n;  //第二个箱子
                        flg = true;         //第一个箱子
                    }
                    n = k;                  //第一个箱子的最后位置
                    boxRC[0] = i;
                    boxRC[1] = j;
                    j++;
                    break;
                case 8:
                    if (boxRC[0] != i - 1 || boxRC[1] != j) {
                        if (flg) return n;  //第二个箱子
                        flg = true;         //第一个箱子
                    }
                    n = k;                  //第一个箱子的最后位置
                    boxRC[0] = i;
                    boxRC[1] = j;
                    i++;
                    break;
            }
        }
        if (flg) return n;  //最后一个动作不是推，但前面有推的动作时
        return len;
    }

    //逆推答案转为正推答案并保存
    private void zhengniHE2() {

        //在逆推状态下，得到人到正推中的人的原始位置的路径，如此，逆推才算真正通关
        if (m_nRow2 != m_nRow3 || m_nCol2 != m_nCol3)
            FindPath(m_nRow3, m_nCol3, true);

        //将上面的路径转到逆推的 unDO 中，以便统一复制到正推的 reDO 中
        int len = m_lstMovReDo2.size();
        for (int k = 0; k < len; k++) reDo2();
        m_bBusing = false;

        //去掉逆推开始的空移位，使人停在箱子旁边
        byte t;
        int s = 0;
        len = m_lstMovUnDo2.size();
        for (int k = 0; k < len; k++) {
            t = m_lstMovUnDo2.get(k);
            if (t < 5) s++;
            else break;
        }

        //清正推 ReDo、UnDo
        len = m_lstMovUnDo.size();
        for (int k = 0; k < len; k++) unDo1();
        m_lstMovReDo.clear();
        m_lstMovUnDo.clear();  //保险一下
        m_iStep[0] = 0;
        m_iStep[1] = 0;

        //答案复制到正推的 reDO 中
        Byte mDir;
        Iterator myItr = m_lstMovUnDo2.iterator();

        for (int k = 0; k < s; k++) {
            if (myItr.hasNext()) myItr.next();
            else break;
        }
        while (myItr.hasNext()) {
            mDir = (Byte)myItr.next();
            switch (mDir) {
                case -1:
                case 1:
                    m_lstMovReDo.offer((byte) 3);
                    break;
                case -2:
                case 2:
                    m_lstMovReDo.offer((byte) 4);
                    break;
                case -3:
                case 3:
                    m_lstMovReDo.offer((byte) 1);
                    break;
                case -4:
                case 4:
                    m_lstMovReDo.offer((byte) 2);
                    break;
                case -5:
                case 5:
                    m_lstMovReDo.offer((byte) 7);
                    break;
                case -6:
                case 6:
                    m_lstMovReDo.offer((byte) 8);
                    break;
                case -7:
                case 7:
                    m_lstMovReDo.offer((byte) 5);
                    break;
                case -8:
                case 8:
                    m_lstMovReDo.offer((byte) 6);
                    break;
            }
        }
        mMap.invalidate();
    }

    //合并正逆合答案并保存
    private void zhengniHE() {

        if (m_nRow2 != m_nRow || m_nCol2 != m_nCol) {
            FindPath(m_nRow2, m_nCol2, false);
        } else {
            m_lstMovReDo.clear();
        }

        byte t;
        int s = 0;
        int len = m_lstMovUnDo2.size();
        for (int k = 0; k < len; k++) {
            t = m_lstMovUnDo2.get(k);
            if (t < 5) {
                s++;
            } else break;
        }

        //合成答案到 m_lstMovReDo
        Byte mDir;
        len = m_lstMovUnDo2.size();
        Iterator myItr = m_lstMovUnDo2.descendingIterator();
        while (myItr.hasNext()) {
            mDir = (Byte)myItr.next();
            if (--len < s) break;
            switch (mDir) {
                case -1:
                case 1:
                    m_lstMovReDo.offerFirst((byte) 3);
                    break;
                case -2:
                case 2:
                    m_lstMovReDo.offerFirst((byte) 4);
                    break;
                case -3:
                case 3:
                    m_lstMovReDo.offerFirst((byte) 1);
                    break;
                case -4:
                case 4:
                    m_lstMovReDo.offerFirst((byte) 2);
                    break;
                case -5:
                case 5:
                    m_lstMovReDo.offerFirst((byte) 7);
                    break;
                case -6:
                case 6:
                    m_lstMovReDo.offerFirst((byte) 8);
                    break;
                case -7:
                case 7:
                    m_lstMovReDo.offerFirst((byte) 5);
                    break;
                case -8:
                case 8:
                    m_lstMovReDo.offerFirst((byte) 6);
                    break;
            }
        }
        mMap.invalidate();
    }

    //保存答案或状态到文档
    private void saveToFile(final String str, String fn) {
        //按关卡集和关卡序号生成文档名
        try{
            FileOutputStream fout = new FileOutputStream(myMaps.sRoot + myMaps.sPath + fn);

            fout.write(str.getBytes());

            fout.flush();
            fout.close();
            MyToast.showToast(this, "保存成功！" , Toast.LENGTH_SHORT);
        } catch (Exception e){
            MyToast.showToast(this, "出错了，保存失败！", Toast.LENGTH_SHORT);
        }
    }

    //保存状态或答案到DB
    private void saveAns(int m_Solution) {
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        byte t;
        Iterator myItr;

        //解关答案
        char[] Move = {'l', 'u', 'r', 'd', 'L', 'U', 'R', 'D'};
        if (!m_lstMovUnDo.isEmpty()) {
            myItr = m_lstMovUnDo.iterator();
            while (myItr.hasNext()) {
                t = (Byte)myItr.next();
                s1.append(Move[t - 1]);
            }
        }

        if (m_Solution == 0 && !m_lstMovUnDo2.isEmpty()) {
            myItr = m_lstMovUnDo2.iterator();
            while (myItr.hasNext()) {
                t = (Byte)myItr.next();
                s2.append(Move[t - 1]);
            }
        }

        if (m_imPort_YASS.toLowerCase().indexOf("yass") >= 0) {
            m_imPort_YASS = "[YASS]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        } else if (m_imPort_YASS.toLowerCase().indexOf("导入") >= 0) {
            m_imPort_YASS = "[导入]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        } else {
            m_imPort_YASS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }

        if (m_Solution == 0 && (m_lstMovUnDo.size() > myMaps.m_nMaxSteps || m_lstMovUnDo2.size() > myMaps.m_nMaxSteps) || m_Solution == 1 && m_lstMovUnDo.size() > myMaps.m_nMaxSteps) {  //答案或状态太长，保存到文档
            final StringBuilder str = new StringBuilder();
            //关卡初态
            str.append(myMaps.curMap.Map).append("\nTitle: ").append(myMaps.curMap.Title).append("\nAuthor: ").append(myMaps.curMap.Author);
            str.append("\nComment:\n").append(myMaps.curMap.Comment).append("\nComment-End:\n");

            if (m_Solution == 0) {  //状态
                str.append(s1).append("\n[").append(m_nRow0).append(", ").append(m_nCol0).append("]").append(s2);
            } else {  //答案
                str.append("Solution (moves ").append(m_iStep[1]).append(", pushes " ).append(m_iStep[0]).append(": ");
                str.append(m_imPort_YASS);
                str.append(s1);
            }

            final String my_Name = new StringBuilder("导入/").append(myMaps.sFile).append("_").append(myMaps.m_lstMaps.indexOf(myMaps.curMap)+1).toString();
            File file;
            int n = 1;
            file = new File(myMaps.sRoot + myMaps.sPath + my_Name + "(" + n + ").txt");
            while (file.exists()) {  //生成不重复的文件名
                n++;
                file = new File(myMaps.sRoot + myMaps.sPath + my_Name + "(" + n + ").txt");
            }
            final int finalN = n;
            new Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("注意").setMessage("答案或状态太长，保存到文档！\n" + my_Name + "(" + n + ").txt").setCancelable(false)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        saveToFile(str.toString(), my_Name + "(" + finalN + ").txt");
                    }}).create().show();

        } else {  //正常，保存到 DB
            long hh = mySQLite.m_SQL.add_S(myMaps.curMap.Level_id,
                    m_Solution,
                    m_iStep[1],
                    m_iStep[0],
                    m_Solution == 0 ? m_iStep[3] : 0,
                    m_Solution == 0 ? m_iStep[2] : 0,
                    m_Solution == 0 ? m_nRow0 : -1,
                    m_Solution == 0 ? m_nCol0 : -1,
                    s1.toString(),
                    s2.toString(),
                    myMaps.curMap.key,
                    m_Solution == 0 ? -1 : myMaps.curMap.L_CRC_Num,
                    m_Solution == 0 ? "" : myMaps.curMap.Map0,
                    m_imPort_YASS);

            m_bMoved = false;
            if (hh > 0) {
                if (m_Solution == 1) {
                    myMaps.curMap.Solved |= (m_Solution == 1);
                    MyToast.showToast(this, "答案已保存！", Toast.LENGTH_LONG);
                    state_Node ans = new state_Node();
                    ans.id = hh;
                    ans.pid = myMaps.curMap.Level_id;
                    ans.pkey = myMaps.curMap.key;
                    ans.moves = m_iStep[1];
                    ans.pushs = m_iStep[0];
                    ans.inf = "移动: " + m_iStep[1] + ", 推动: " + m_iStep[0];
                    ans.time = m_imPort_YASS;
                    myMaps.mState2.add(ans);
                    mMap.invalidate();
                } else {  //保存的是状态
                    MyToast.showToast(this, "状态已保存！", Toast.LENGTH_LONG);
                }
            } else if (hh == 0) {
                if (m_Solution == 1) {
                    myMaps.curMap.Solved |= (m_Solution == 1);
                    MyToast.showToast(this, "答案有重复，未再保存！\n移动：" + (m_iStep[1] + m_lstMovReDo.size()) + "，推动：" + (m_iStep[0] + m_iStep[2]), Toast.LENGTH_LONG);
                } else {
                    MyToast.showToast(this, "有重复，已重排！", Toast.LENGTH_LONG);
                }
            } else {
                myMaps.curMap.Solved |= (m_Solution == 1);
                MyToast.showToast(this, "出错了，保存失败！", Toast.LENGTH_LONG);
            }
        }
    }

    //保存逆推通关或正逆合答案到DB
    private void saveAns2() {

        //保存答案
        StringBuilder s1 = new StringBuilder();
        byte t;
        Iterator myItr;

        //解关答案，以标准格式保存，不以快手内部格式保存，增强通用性
        char[] Move = {'l', 'u', 'r', 'd', 'L', 'U', 'R', 'D'};
        if (!m_lstMovUnDo.isEmpty()) {
            myItr = m_lstMovUnDo.iterator();
            while (myItr.hasNext()) {
                t = (Byte)myItr.next();
                s1.append(Move[t - 1]);
            }
        }
        if (!m_lstMovReDo.isEmpty()) {
            myItr = m_lstMovReDo.descendingIterator();
            while (myItr.hasNext()) {
                t = (Byte)myItr.next();
                s1.append(Move[t - 1]);
            }
        }

        if (m_imPort_YASS.toLowerCase().indexOf("yass") >= 0) {
            m_imPort_YASS = "[YASS]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        } else if (m_imPort_YASS.toLowerCase().indexOf("导入") >= 0) {
            m_imPort_YASS = "[导入]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        } else {
            m_imPort_YASS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }

        if (m_lstMovUnDo.size() + m_lstMovReDo.size() > myMaps.m_nMaxSteps) {  //答案太长，保存到文档
            final StringBuilder str = new StringBuilder();
            //关卡初态
            str.append(myMaps.curMap.Map).append("\nTitle: ").append(myMaps.curMap.Title).append("\nAuthor: ").append(myMaps.curMap.Author);
            str.append("\nComment:\n").append(myMaps.curMap.Comment).append("\nComment-End:\n");

            str.append("Solution (moves ").append(m_iStep[1] + m_lstMovReDo.size()).append(", pushes " ).append(m_iStep[0] + m_iStep[2]).append(": ");
            str.append(m_imPort_YASS);
            str.append(s1);

            File targetDir = new File(myMaps.sRoot+myMaps.sPath + "超长答案/");
            if (!targetDir.exists()) targetDir.mkdirs();  //创建文件夹

            final String my_Name = new StringBuilder("超长答案/").append(myMaps.sFile).append("_").append(myMaps.m_lstMaps.indexOf(myMaps.curMap)+1).toString();
            File file;
            int n = 1;
            file = new File(myMaps.sRoot + myMaps.sPath + my_Name + "(" + n + ").txt");
            while (file.exists()) {  //生成不重复的文件名
                n++;
                file = new File(myMaps.sRoot + myMaps.sPath + my_Name + "(" + n + ").txt");
            }
            final int finalN = n;
            new Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("注意").setMessage("答案太长，保存到文档！\n" + my_Name + "(" + n + ").txt").setCancelable(false)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            saveToFile(str.toString(), my_Name + "(" + finalN + ").txt");
                        }}).create().show();

        } else {  //正常，保存到 DB
            long hh = mySQLite.m_SQL.add_S(myMaps.curMap.Level_id,
                    myMaps.curMap.Level_id > 0 ? 1 : 0,  //试推时，以状态形式保存
                    m_iStep[1] + m_lstMovReDo.size(),
                    m_iStep[0] + m_iStep[2],
                    0,
                    0,
                    -1,
                    -1,
                    s1.toString(),
                    "",
                    myMaps.curMap.key,
                    myMaps.curMap.L_CRC_Num,
                    myMaps.curMap.Map0,
                    m_imPort_YASS);

            myMaps.curMap.Solved = true;
            m_bMoved = false;
            if (myMaps.curMap.Level_id > 0) {
                if (hh > 0) {
                    state_Node ans = new state_Node();
                    ans.id = hh;
                    ans.pid = myMaps.curMap.Level_id;
                    ans.pkey = myMaps.curMap.key;
                    ans.inf = "移动: " + m_iStep[1] + ", 推动: " + m_iStep[0];
                    ans.time = m_imPort_YASS;
                    myMaps.mState2.add(ans);
                    Builder builder = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                    builder.setTitle("正逆相合或通关！")
                            .setMessage("答案成功保存！\n(可用进退键观看通关演示)")
                            .setPositiveButton("确定", null);
                    builder.setCancelable(false).show();
                } else if (hh == 0) {
                    Builder builder = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                    builder.setTitle("正逆相合或通关！")
                            .setMessage("答案有重复！\n移动：" + (m_iStep[1] + m_lstMovReDo.size()) + "，推动：" + (m_iStep[0] + m_iStep[2]) + "，\n本次未做保存！\n(可用进退键观看通关演示)")
                            .setPositiveButton("确定", null);
                    builder.setCancelable(false).show();
                } else {
                    Builder builder = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                    builder.setTitle("正逆相合或通关！")
                            .setMessage("DB写错误，答案未能保存，请利用剪切板手动保存到其它地方！\n(可用进退键观看通关演示)")
                            .setPositiveButton("确定", null);
                    builder.setCancelable(false).show();
                }
            }
        }
    }

    //接收打开的状态
    private void OpenState() {
        m_nLastSteps = -1;
        myMaps.m_StateIsRedy = false;
        try {
            levelReset(false);  //正推复位
            myMaps.m_Sets[25] = 0;  //求解后，关闭即景正推
            load_Level(false);  //加载即景正推目标点

            int len = myMaps.m_State.ans.length();
            if (len > 0) {
                formatPath(myMaps.m_State.ans, false);
                if (myMaps.m_State.time.toLowerCase().indexOf("yass") >= 0) {
                    m_imPort_YASS = "[YASS]";
                } else if (myMaps.m_State.time.toLowerCase().indexOf("导入") >= 0) {
                    m_imPort_YASS = "[导入]";
                } else {
                    m_imPort_YASS = "";
                }
                if (myMaps.m_State.solution == 0) {  //答案，停在开始位置；状态，停在结束位置
                    len = m_lstMovReDo.size();
                    for (int k = 0; k < len; k++) reDo1();
                    m_bBusing = false;
                } else
                    MyToast.showToast(this, "答案加载成功！", Toast.LENGTH_SHORT);
            }

            len = myMaps.m_State.bk_ans.length();
            if (len > 0) {
                try {
                    levelReset(true);  //逆推复位
                    try {
                        if (bk_cArray[m_nRow2][m_nCol2] == '@') bk_cArray[m_nRow2][m_nCol2] = '-';
                        else if (bk_cArray[m_nRow2][m_nCol2] == '+') bk_cArray[m_nRow2][m_nCol2] = '.';
                    } catch (ArrayIndexOutOfBoundsException ex) { }
                    m_nRow0 = myMaps.m_State.r;
                    m_nCol0 = myMaps.m_State.c;
                    m_nRow2 = m_nRow0;
                    m_nCol2 = m_nCol0;
                    bk_cArray[m_nRow2][m_nCol2] = (bk_cArray[m_nRow2][m_nCol2] == '-' ? '@' : '+');
                    formatPath(myMaps.m_State.bk_ans, true);
                    len = m_lstMovReDo2.size();
                    for (int k = 0; k < len; k++) reDo2();
                    m_bBusing = false;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    m_nRow0 = -1;
                    m_nCol0 = -1;
                    m_nRow2 = m_nRow0;
                    m_nCol2 = m_nCol0;
                    m_lstMovReDo2.clear();
                }
            }
            bt_BK.setChecked(false);  //强制回到正推界面
            mMap.invalidate();
        } catch (Throwable ex) {
        }
    }

    //重回前台时，防止其它 APP 有改变，再次隐藏它们
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && myMaps.m_Sets[16] == 0) {
            hideSystemUI();
        }
    }

    //可能 openoptionsmenu() 有一个bug，当它被调用时，隐藏的导航栏、状态栏会显示出来，为此，需要再次隐藏它们
    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        if (myMaps.m_Sets[16] == 0) hideSystemUI();
    }

    @Override
    public void openOptionsMenu() {
        Configuration config = getResources().getConfiguration();
        if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) > Configuration.SCREENLAYOUT_SIZE_LARGE) {
            int originalScreenLayout = config.screenLayout;
            config.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE;
            super.openOptionsMenu();
            config.screenLayout = originalScreenLayout;
        } else {
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player, menu);
        if (myMaps.m_Sets[17] == 0) {  //是否允许穿越
            menu.getItem(0).setChecked(false);
        } else {
            menu.getItem(0).setChecked(true);
        }
        if (myMaps.m_Sets[23] == 0) {  //单步进退
            menu.getItem(1).setChecked(false);
        } else {
            menu.getItem(1).setChecked(true);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem mt) {

        switch (mt.getItemId()) {
            case R.id.player_help:  //操作说明
                Intent intent0 = new Intent(this, Help.class);
                //用Bundle携带数据
                Bundle bundle0 = new Bundle();
                bundle0.putInt("m_Num", 1);  //传递参数，指示调用者
                intent0.putExtras(bundle0);

                intent0.setClass(this, Help.class);
                startActivity(intent0);

                return true;
            case R.id.player_about:  //关卡描述
                Intent intent = new Intent();
                intent.setClass(myGameView.this, myAbout2.class);
                startActivity(intent);

                return true;
            case R.id.player_IN:  //导入
                setACT(false);  //非录制模式

                myMaps.m_ActionIsRedy = false;

                if (bt_BK.isChecked())
                    myMaps.m_nRecording_Bggin2 = m_lstMovUnDo2.size();  //逆推录制起始点
                else
                    myMaps.m_nRecording_Bggin = m_lstMovUnDo.size();  //正推录制起始点

                Intent intent2 = new Intent(this, myActGMView.class);
                //用Bundle携带数据
                Bundle bundle = new Bundle();
                bundle.putBoolean("is_BK", bt_BK.isChecked());  //传递参数：是否逆推
                bundle.putString("LOCAL", myMaps.getLocale(m_cArray));  //关卡现场地图数据
                intent2.putExtras(bundle);

                intent2.setClass(myGameView.this, myActGMView.class);
                startActivity(intent2);

                return true;
            case R.id.player_load:  //打开状态
                if (myMaps.isMacroDebug) {
                    final CheckBox isBack = new CheckBox(this);
                    isBack.setText("关卡回到该“宏”打开前的状态");
                    isBack.setChecked(true);
                    new Builder(myGameView.this, AlertDialog.THEME_HOLO_DARK).setTitle("关闭调试")
                            .setView(isBack)
                            .setPositiveButton("确定", new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (isBack.isChecked()) {  //回到宏打开前的关卡状态
                                        levelReset(false);  //正推关卡复位
                                        if (myMaps.m_Sets[25] == 1) {
                                            load_Level(true);  //加载即景正推目标点
                                        }
                                    }
                                    myMaps.isMacroDebug = false;
                                    mMap.invalidate();
                                    myOpenState();  //启动打开状态对话框
                                }
                            })
                            .setNegativeButton("取消", null).setCancelable(false)
                            .show();
                } else myOpenState();  //启动打开状态对话框

                return true;
            case R.id.player_save:  //保存状态
                if (m_lstMovUnDo.size() > 0 || m_lstMovUnDo2.size() > 0) {
                    if ((myMaps.m_Sets[25] == 0 || m_iStep[2] <= 0) && m_nDstOK == m_nDstNum && m_iStep[0] > 0 && myMaps.curMap.Level_id > 0)  //箱子 == 目标 && 动过箱子，非试推状态
                        saveAns(1);
                    else
                        saveAns(0);
                } else {
                    MyToast.showToast(this, "没什么可保存的！", Toast.LENGTH_SHORT);
                }

                return true;
            case R.id.player_ReStart:  //重新开始
                Builder builder0 = new Builder(myGameView.this);
                builder0.setMessage("重新开始，确定吗？").setNegativeButton("取消", null)
                        .setPositiveButton("确定", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            mMap.d_Moves = mMap.m_PicWidth;

                            //先合并 undo 和 redo 序列，关卡复位后，将合并后的动作序列送入 redu 序列
                            if (bt_BK.isChecked()) {  //逆推
                                MyToast.showToast(myGameView.this, "重新开始！", Toast.LENGTH_SHORT);
                                levelReset(true);  //逆推关卡复位
                                if (myMaps.m_Sets[13] == 1)
                                    load_BK_Level(true);  //加载即景逆推目标点

                            } else {  //正推
                                MyToast.showToast(myGameView.this, "重新开始！", Toast.LENGTH_SHORT);
                                levelReset(false);  //正推关卡复位
                                if (myMaps.m_Sets[25] == 1)
                                    load_Level(true);  //加载即景正推目标点

                                if (myMaps.isMacroDebug) {
                                    mMap.myMacro.clear();
                                    mMap.myMacro.add(0);  //首次执行宏时，准备从第 0 行开始
                                }
                            }
                            mMap.curMoves = 0;
                            mMap.invalidate();
                            }
                        });
                builder0.setCancelable(false).show();

                return true;
            case R.id.player_Home:  //后退至首
                m_bYanshi = false;
                m_bYanshi2 = false;
                if (bt_BK.isChecked()) {  //逆推
                    if (m_lstMovUnDo2.isEmpty()) {
                        MyToast.showToast(this, "没有了！", Toast.LENGTH_SHORT);
                        m_bBusing = false;
                    } else {
                        m_nStep = m_lstMovUnDo2.size();
                        UpData4(1);
                    }
                } else {            //正推
                    if (m_lstMovUnDo.isEmpty()) {
                        MyToast.showToast(this, "没有了！", Toast.LENGTH_SHORT);
                        m_bBusing = false;
                    } else {
                        m_nStep = m_lstMovUnDo.size();
                        UpData2(1);
                    }
                }

                return true;
            case R.id.player_End:  //前进至尾
                m_bYanshi = false;
                m_bYanshi2 = false;
                if (bt_BK.isChecked()) {  //逆推
                    if (m_lstMovReDo2.isEmpty()) {
                        MyToast.showToast(this, "没有了！", Toast.LENGTH_SHORT);
                        m_bBusing = false;
                    } else {
                        if (m_lstMovUnDo2.isEmpty()) goHome();
                        m_nStep = m_lstMovReDo2.size();
                        UpData3(1);
                    }
                } else {            //正推
                    if (m_lstMovReDo.isEmpty()) {
                        MyToast.showToast(this, "没有了！", Toast.LENGTH_SHORT);
                        m_bBusing = false;
                    } else {
                        m_nStep = m_lstMovReDo.size();
                        mMap.Box_Row0 = -1;  //记录箱子移动前的位置
                        m_nLastSteps = -1;
                        UpData1(1);
                    }
                }

                return true;
            case R.id.player_Yass_Solver:  //YASS求解
                if (!bt_BK.isChecked()) {
                    mySolution();  //YASS求解
                } else {
                    MyToast.showToast(this, "逆推时，无此功能！", Toast.LENGTH_SHORT);
                }

                return true;
            case R.id.player_Setup1:  //场景设置
                String[] m_menu2 = {
                        "速度设置",
                        "更换皮肤",
                        "更换背景",
                        "奇偶格位明暗度"
                };
                Builder builder2 = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                builder2.setTitle("设置").setSingleChoiceItems(m_menu2, -1, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:   //速度设置
                                Builder builder4 = new Builder(myGameView.this, AlertDialog.THEME_HOLO_DARK);
                                builder4.setTitle("移动速度").setSingleChoiceItems(m_sSleep, myMaps.m_Sets[10], new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        myMaps.m_Sets[10] = which;
                                    }
                                }).setPositiveButton("确定", new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        BoxMan.saveSets();  //保存设置
                                    }
                                });
                                builder4.setCancelable(false).show();
                                break;
                            case 1:   //更换皮肤
                                skinList();
                                m_nItemSelect = 0;
                                if (myMaps.mFile_List1.size() > 0) {
                                    for (int k = 0; k < myMaps.mFile_List1.size(); k++) {
                                        if (myMaps.mFile_List1.get(k).equals(myMaps.skin_File)) {
                                            m_nItemSelect = k;
                                            break;
                                        }
                                    }
                                    Builder builder = new Builder(myGameView.this, AlertDialog.THEME_HOLO_DARK);
                                    builder.setTitle("皮肤").setSingleChoiceItems(myMaps.mFile_List1.toArray(new String[myMaps.mFile_List1.size()]), m_nItemSelect, new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            myMaps.skin_File = myMaps.mFile_List1.get(which);  //选择的文档
                                            myMaps.loadSkins();
                                            mMap.invalidate();
                                        }
                                    }).setPositiveButton("确定", new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            myMaps.iskinChange = true;
                                            dialog.dismiss();
                                        }
                                    }).setNegativeButton("取消", new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            myMaps.iskinChange = false;
                                            myMaps.skin_File = myMaps.mFile_List1.get(m_nItemSelect);  //选择的文档
                                            myMaps.loadSkins();
                                            mMap.invalidate();
                                            dialog.dismiss();
                                        }
                                    });
                                    builder.setCancelable(false).show();
                                } else
                                    MyToast.showToast(myGameView.this, "没找到皮肤图片文档！", Toast.LENGTH_SHORT);
                                break;
                            case 2:   //设置背景图片
                                bkPicList();
                                if (myMaps.mFile_List2.size() > 0) {
                                    m_nItemSelect = 0;
                                    for (int k = 0; k < myMaps.mFile_List2.size(); k++) {
                                        if (myMaps.mFile_List2.get(k).equals(myMaps.bk_Pic)) {
                                            m_nItemSelect = k;
                                            break;
                                        }
                                    }
                                    Builder builder = new Builder(myGameView.this, AlertDialog.THEME_HOLO_DARK);
                                    builder.setTitle("背景图片").setSingleChoiceItems(myMaps.mFile_List2.toArray(new String[myMaps.mFile_List2.size()]), m_nItemSelect, new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            myMaps.bk_Pic = myMaps.mFile_List2.get(which);  //选择的文档
                                            myMaps.loadBKPic();

                                            if (which == 0) {     // 使用背景色
                                                setColorBK();
                                            } else if (myMaps.bkPict != null) {    // 使用背景图片
                                                mMap.w_bkPic = myMaps.bkPict.getWidth();
                                                mMap.h_bkPic = myMaps.bkPict.getHeight();
                                                mMap.w_bkNum = myMaps.m_nWinWidth / mMap.w_bkPic + 1;
                                                mMap.h_bkNum = myMaps.m_nWinHeight / mMap.h_bkPic + 1;
                                            }
                                            mMap.invalidate();
                                        }
                                    }).setPositiveButton("确定", new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).setNegativeButton("取消", new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int n) {
                                            myMaps.bk_Pic = myMaps.mFile_List2.get(m_nItemSelect);  //选择的文档
                                            myMaps.loadBKPic();
                                            if (m_nItemSelect > 0 && myMaps.bkPict != null) {
                                                mMap.w_bkPic = myMaps.bkPict.getWidth();
                                                mMap.h_bkPic = myMaps.bkPict.getHeight();
                                                mMap.w_bkNum = myMaps.m_nWinWidth / mMap.w_bkPic + 1;
                                                mMap.h_bkNum = myMaps.m_nWinHeight / mMap.h_bkPic + 1;
                                            }
                                            mMap.invalidate();
                                            dialog.dismiss();
                                        }
                                    });
                                    builder.setCancelable(false).show();
                                } else {
                                    MyToast.showToast(myGameView.this, "没找到图片文档", Toast.LENGTH_SHORT);
                                }
                                break;
                            case 3:   //奇偶格位明暗度条
                                dialog.dismiss();
                                mMap.m_lParityBrightnessShade = true;
                                mMap.invalidate();
                                break;
                        }  //end switch
                    }
                }).setPositiveButton("返回", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BoxMan.saveSets();  //保存设置
                    }
                });
                builder2.setCancelable(false).show();

                return true;
            case R.id.player_Setup2:  //开关选项
                String[] m_menu = {
                        "自动箱子编号",
                        "标尺",
                        "标尺不随关卡旋转",
                        "区分奇偶地板格",
                        "死锁嗅探",
                        "可达提示",
                        "仓管员转向动画",
                        "长按点位提示关联网",
                        "自动加载最新状态",
                        "逆推时使用正推目标点",
                        "允许穿越",
                        "单步进退",
                        "进度条",
                        "自动爬阶梯",
                        "系统导航键",
                        "禁用全屏",
                        "演示时仅推动",
                        "音量键选择关卡"
                };
                final boolean[] mChk = {
                        myMaps.m_bBianhao,       //自动箱子编号
                        myMaps.m_bBiaochi,       //显示标尺
                        myMaps.m_Sets[9] == 1,   //标尺不随关卡旋转
                        myMaps.m_Sets[38] == 1,   //区分奇偶地板格
                        myMaps.m_Sets[11] == 1,  //死锁提示
                        myMaps.m_Sets[8] == 1,   //显示可达提示
                        myMaps.m_Sets[27] == 1,  //仓管员转向动画
                        myMaps.m_Sets[3] == 1,   //长按目标点提示关联网点及网口
                        myMaps.m_Sets[37] == 1,   //关卡初态，长按仓管员，切换是否“自动加载最新状态”
                        myMaps.m_Sets[32] == 1,  //禁用逆推目标点
                        myMaps.m_Sets[17] == 1,  //允许穿越
                        myMaps.m_Sets[23] == 1,  //单步进退
                        !bt_BK.isChecked() && mMap.m_lGoto || bt_BK.isChecked() && mMap.m_lGoto2,  //进度条
                        myMaps.m_Sets[29] == 1,  //自动爬阶梯
                        myMaps.m_Sets[16] == 1,  //显示系统虚拟按键
                        myMaps.m_Sets[20] == 1,  //禁用全屏
                        myMaps.m_Sets[28] == 1,  //演示时仅推动
                        myMaps.m_Sets[15] == 1   //音量键选择关卡
                };

                Builder builder = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                builder.setTitle("开关选项").setMultiChoiceItems(m_menu, mChk, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    }
                }).setNegativeButton("取消", null).setPositiveButton("确定", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //自动箱子编号
                        if (myMaps.m_bBianhao != mChk[0]) {
                            myMaps.m_bBianhao = mChk[0];
                            mMap.invalidate();
                        }

                        //显示标尺
                        if (myMaps.m_bBiaochi != mChk[1]) {
                            myMaps.m_bBiaochi = mChk[1];
                            mMap.invalidate();
                        }

                        //标尺不随关卡旋转
                        if (mChk[2]) myMaps.m_Sets[9] = 1;
                        else myMaps.m_Sets[9] = 0;

                        //区分奇偶地板格
                        if (mChk[3]) myMaps.m_Sets[38] = 1;
                        else myMaps.m_Sets[38] = 0;

                        //死锁提示
                        if (mChk[4]) myMaps.m_Sets[11] = 1;
                        else myMaps.m_Sets[11] = 0;

                        //显示可达提示
                        if (mChk[5]) {
                            myMaps.m_Sets[8] = 1;
                        } else {
                            myMaps.m_Sets[8] = 0;
                            mMap.m_bBoxTo = false;
                            mMap.m_bBoxTo2 = false;
                            mMap.m_boxCanCome = false;
                            mMap.m_boxCanCome2 = false;
                            mMap.m_boxCanMove = false;
                            mMap.m_boxCanMove2 = false;
                        }

                        //仓管员转向动画
                        if (mChk[6]) myMaps.m_Sets[27] = 1;
                        else myMaps.m_Sets[27] = 0;

                        //长按目标点提示关联网点及网口
                        if (mChk[7]) myMaps.m_Sets[3] = 1;
                        else myMaps.m_Sets[3] = 0;

                        //是否“自动加载最新状态”
                        if (mChk[8]) myMaps.m_Sets[37] = 1;
                        else myMaps.m_Sets[37] = 0;

                        //禁用逆推目标点
                        if (mChk[9]) myMaps.m_Sets[32] = 1;
                        else myMaps.m_Sets[32] = 0;

                        //是否允许穿越
                        if (mChk[10]) {
                            myMaps.m_Sets[17] = 1;
                            mMap.m_bManTo = false;
                            mMap.m_bManTo2 = false;
                            mMap.m_bBoxTo = false;
                            mMap.m_bBoxTo2 = false;
                            mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
                            mMap.m_boxCanMove2 = false;  //关闭可动箱子提示状态
                            mMap.invalidate();
                        } else {
                            myMaps.m_Sets[17] = 0;
                            mMap.m_bManTo = false;
                            mMap.m_bManTo2 = false;
                            mMap.m_bBoxTo = false;
                            mMap.m_bBoxTo2 = false;
                            mMap.m_boxCanMove = false;  //关闭可动箱子提示状态
                            mMap.m_boxCanMove2 = false;  //关闭可动箱子提示状态
                            mMap.invalidate();
                        }

                        //是否单步进退
                        if (mChk[11]) myMaps.m_Sets[23] = 1;
                        else myMaps.m_Sets[23] = 0;

                        //是否显示进度条
                        if (mChk[12]) {
                            if (bt_BK.isChecked()) {  //逆推
                                mMap.m_lGoto2 = true;
                            } else {
                                mMap.m_lGoto = true;
                            }
                        } else {
                            if (bt_BK.isChecked()) {  //逆推
                                mMap.m_lGoto2 = false;
                            } else {
                                mMap.m_lGoto = false;
                            }
                        }

                        //自动爬阶梯
                        if (mChk[13]) myMaps.m_Sets[29] = 1;
                        else myMaps.m_Sets[29] = 0;

                        //显示系统导航键
                        if (mChk[14]) {
                            myMaps.m_Sets[16] = 1;
                            showSystemUI();
                        } else {
                            myMaps.m_Sets[16] = 0;
                            hideSystemUI();
                        }

                        //禁用全屏
                        if (mChk[15]) myMaps.m_Sets[20] = 1;
                        else myMaps.m_Sets[20] = 0;

                        //演示时仅推动
                        if (mChk[16]) myMaps.m_Sets[28] = 1;
                        else myMaps.m_Sets[28] = 0;

                        //使用音量键选择关卡
                        if (mChk[17]) myMaps.m_Sets[15] = 1;
                        else myMaps.m_Sets[15] = 0;

                        BoxMan.saveSets();  //保存设置
                    }
                });
                builder.setCancelable(false).show();

                return true;
            case R.id.player_EX:  //导出
                StringBuilder s_XSB = new StringBuilder();  //关卡初态
                StringBuilder s_XSB1 = new StringBuilder();  //关卡正推现场
                StringBuilder s_XSB8 = new StringBuilder();  //关卡正推现场 -- 旋转
                StringBuilder s_Lurd = new StringBuilder();  //Lurd
                boolean isANS = (m_nDstOK == m_nDstNum);

                //关卡初态
                s_XSB.append(myMaps.curMap.Map).append("\nTitle: ").append(myMaps.curMap.Title).append("\nAuthor: ").append(myMaps.curMap.Author);
                if (!myMaps.curMap.Comment.trim().isEmpty()) {
                    s_XSB.append("\nComment:\n").append(myMaps.curMap.Comment).append("\nComment-End:");
                }

                //关卡正推现场
                for (int i = 0; i < myMaps.curMap.Rows; i++) {
                    for (int j = 0; j < myMaps.curMap.Cols; j++) {
                        s_XSB1.append(m_cArray[i][j]);
                    }
                    if (i < myMaps.curMap.Rows - 1) s_XSB1.append('\n');
                }

                //关卡正推现场 -- 旋转
                if (myMaps.m_nTrun % 2 == 0) {
                    for (int i = 0; i < myMaps.curMap.Rows; i++) {
                        for (int j = 0; j < myMaps.curMap.Cols; j++) {
                            switch (myMaps.m_nTrun) {
                                case 0:
                                    s_XSB8.append(m_cArray[i][j]);
                                    break;
                                case 2:
                                    s_XSB8.append(m_cArray[myMaps.curMap.Rows - 1 - i][myMaps.curMap.Cols - 1 - j]);
                                    break;
                                case 4:
                                    s_XSB8.append(m_cArray[i][myMaps.curMap.Cols - 1 - j]);
                                    break;
                                case 6:
                                    s_XSB8.append(m_cArray[myMaps.curMap.Rows - 1 - i][j]);
                                    break;
                            }
                        }
                        if (i < myMaps.curMap.Rows - 1) s_XSB8.append('\n');
                    }
                } else {
                    for (int j = 0; j < myMaps.curMap.Cols; j++) {
                        for (int i = 0; i < myMaps.curMap.Rows; i++) {
                            switch (myMaps.m_nTrun) {
                                case 1:
                                    s_XSB8.append(m_cArray[myMaps.curMap.Rows - 1 - i][j]);
                                    break;
                                case 3:
                                    s_XSB8.append(m_cArray[i][myMaps.curMap.Cols - 1 - j]);
                                    break;
                                case 5:
                                    s_XSB8.append(m_cArray[myMaps.curMap.Rows - 1 - i][myMaps.curMap.Cols - 1 - j]);
                                    break;
                                case 7:
                                    s_XSB8.append(m_cArray[i][j]);
                                    break;
                            }
                        }
                        if (j < myMaps.curMap.Cols - 1) s_XSB8.append('\n');
                    }
                }

                char[] Move = {'l', 'u', 'r', 'd', 'L', 'U', 'R', 'D'};
                byte t;
                Iterator myItr;

                //解关答案
                if (!m_lstMovUnDo.isEmpty()) {
                    myItr = m_lstMovUnDo.iterator();
                    while (myItr.hasNext()) {
                        t = (Byte)myItr.next();
                        s_Lurd.append(Move[t - 1]);
                    }
                }

                if (!isANS) {  //若正推已经是答案，则不再导出逆推动作
                    if (!m_lstMovUnDo2.isEmpty()) {
                        if (s_Lurd.length() > 0) s_Lurd.append('\n');
                        s_Lurd.append('[').append(m_nCol0).append(',').append(m_nRow0).append(']');  //（x, y）-- 先列后行
                        myItr = m_lstMovUnDo2.iterator();
                        while (myItr.hasNext()) {
                            t = (Byte)myItr.next();
                            s_Lurd.append(Move[t - 1]);
                        }
                    }
                }

                boolean[] my_Rule = new boolean[myMaps.curMap.Cols * myMaps.curMap.Rows];
                short[] my_BoxNum = new short[m_nDstNum];  //按箱子数定义一个数字，记录“自动箱子编号”，以方便转换“人工箱子编号”
                for (int i = 0; i < myMaps.curMap.Rows; i++) {
                    for (int j = 0; j < myMaps.curMap.Cols; j++) {
                        my_Rule[myMaps.curMap.Cols*i+j] = mark44[i][j];
                        if (m_iBoxNum2[i][j] > 0 && (m_cArray[i][j] == '$' || m_cArray[i][j] == '*')) {
                            my_BoxNum[m_iBoxNum2[i][j]-1] = myMaps.m_bBianhao ? m_iBoxNum2[i][j] : m_iBoxNum[i][j];   //自动箱子编号与人工箱子编号建立关联
                        }
                    }
                }


                Intent intent3 = new Intent(this, myExport.class);
                //用Bundle携带数据
                Bundle bundle2 = new Bundle();
                bundle2.putInt("m_Gif_Start", m_Gif_Start);  //导出 GIF 的起点
                bundle2.putBoolean("is_ANS", isANS);  //是否答案
                bundle2.putBooleanArray("my_Rule", my_Rule);  //需要显示标尺的格子
                bundle2.putShortArray("my_BoxNum", my_BoxNum);  //迷宫箱子编号（人为）
                bundle2.putString("LOCAL", s_XSB1.toString());  //关卡正推现场
                bundle2.putString("LOCAL8", s_XSB8.toString());  //关卡正推现场 -- 旋转
                bundle2.putString("m_XSB", s_XSB.toString());  //关卡初态
                bundle2.putString("m_Lurd", s_Lurd.toString());  //Lurd
                bundle2.putString("m_InPort_YASS", m_imPort_YASS);  //是否为“导入”或“YASS”动作
                intent3.putExtras(bundle2);

                intent3.setClass(myGameView.this, myExport.class);
                startActivity(intent3);

                return true;
            default:
                return super.onOptionsItemSelected(mt);
        }
    }

    // 设置背景色
    public void setColorBK() {
        View view2 = View.inflate(this, R.layout.color_dialog, null);
        final View vw = view2.findViewById(R.id.dialog_bk_color);                   //颜色示例
        final SeekBar color_R = (SeekBar) view2.findViewById(R.id.dialog_color_R);  //标尺字体颜色 -- 红
        final SeekBar color_G = (SeekBar) view2.findViewById(R.id.dialog_color_G);  //标尺字体颜色 -- 绿
        final SeekBar color_B = (SeekBar) view2.findViewById(R.id.dialog_color_B);  //标尺字体颜色 -- 蓝

        final int[] mColor = {
                (myMaps.m_Sets[4] & 0x00FF0000) >> 16,
                (myMaps.m_Sets[4] & 0x0000FF00) >> 8,
                myMaps.m_Sets[4] & 0x000000FF };

        vw.setBackgroundColor(myMaps.m_Sets[4] | 0xff000000);

        color_R.setProgress(mColor[0]);
        color_G.setProgress(mColor[1]);
        color_B.setProgress(mColor[2]);

        color_R.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mColor[0] = progress;
                vw.setBackgroundColor(mColor[0] << 16 | mColor[1] << 8 | mColor[2] | 0xff000000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        color_G.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mColor[1] = progress;
                vw.setBackgroundColor(mColor[0] << 16 | mColor[1] << 8 | mColor[2] | 0xff000000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        color_B.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mColor[2] = progress;
                vw.setBackgroundColor(mColor[0] << 16 | mColor[1] << 8 | mColor[2] | 0xff000000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        new Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("设置背景色:")
                .setView(view2)
                .setPositiveButton("确定", new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        myMaps.m_Sets[4] = mColor[0] << 16 | mColor[1] << 8 | mColor[2] | 0xff000000;
                        myMaps.bk_Pic = "使用背景色";
                        mMap.invalidate();
                    }
                })
                .setNegativeButton("取消", null).setCancelable(false)
                .show();
    }

    //接收 YASS 返回值
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {  //求解返回
            if (resultCode == RESULT_OK) {
                formatPath(data.getStringExtra("SOLUTION"), false);
                MyToast.showToast(this, "答案已经载！", Toast.LENGTH_SHORT);
                m_imPort_YASS = "[YASS]";
            } else {
                MyToast.showToast(this, "未能完成求解！", Toast.LENGTH_SHORT);
            }
        }
    }

    // YASS 求解
    protected void mySolution() {
        try {
            //拼接正逆天动作，进行查重和保存
            char[] Move = {'l', 'u', 'r', 'd', 'L', 'U', 'R', 'D'};
            StringBuilder s1 = new StringBuilder();
            StringBuilder s2 = new StringBuilder();
            Iterator myItr;
            if (!m_lstMovUnDo.isEmpty()) {
                myItr = m_lstMovUnDo.iterator();
                while (myItr.hasNext()) {
                    s1.append(Move[(Byte)myItr.next() - 1]);
                }
            }

            if (!m_lstMovUnDo2.isEmpty()) {
                s2.append("[").append(m_nRow0).append(", ").append(m_nCol0).append("]");
                myItr = m_lstMovUnDo2.iterator();
                while (myItr.hasNext()) {
                    s2.append(Move[(Byte)myItr.next() - 1]);
                }
            }

            //自动保存一下当前状态（自动查重），避免yass闪退造成丢失
            if ((m_iStep[1] > 0 || m_iStep[3] > 0) && mySQLite.m_SQL.count_S(myMaps.curMap.Level_id, m_iStep[1], m_iStep[0], m_iStep[3], m_iStep[2], m_nRow0, m_nCol0, myMaps.getCRC32(s1.toString()+s2.toString())) <= 0) {
                if (m_imPort_YASS.toLowerCase().indexOf("yass") >= 0) {
                    m_imPort_YASS = "[YASS]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                } else if (m_imPort_YASS.toLowerCase().indexOf("导入") >= 0) {
                    m_imPort_YASS = "[导入]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                } else {
                    m_imPort_YASS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                }

                long hh = mySQLite.m_SQL.add_S(myMaps.curMap.Level_id,
                        3,
                        m_iStep[1],
                        m_iStep[0],
                        m_iStep[3],
                        m_iStep[2],
                        m_nRow0,
                        m_nCol0,
                        s1.toString(),
                        s2.toString(),
                        myMaps.curMap.key,
                        -1,
                        "",
                        m_imPort_YASS);

                m_bMoved = false;
                if (hh > 0) {
                    MyToast.showToast(this, "状态已保存！", Toast.LENGTH_SHORT);
                }
            }

            Intent intent3 = new Intent(Intent.ACTION_MAIN);
            intent3.addCategory(Intent.CATEGORY_LAUNCHER);
            ComponentName name = new ComponentName("net.sourceforge.sokobanyasc.joriswit.yass", "yass.YASSActivity");
            intent3.setComponent(name);
            String actName = intent3.getAction();
            intent3.setAction("nl.joriswit.sokosolver.SOLVE");
//            intent3.putExtra("m_Caller", 1);
            intent3.putExtra("LEVEL", myMaps.getLocale(m_cArray));
            startActivityForResult(intent3, 1);
            intent3.setAction(actName);
        } catch (Exception e) {
            MyToast.showToast(this, "没有找到求解器！", Toast.LENGTH_SHORT);
        }
    }

    //启动打开状态对话框
    private void myOpenState() {
        mySQLite.m_SQL.load_StateList(myMaps.curMap.Level_id, myMaps.curMap.key);
        //仅修正本关卡是否解关（按关卡 id）
        if (myMaps.curMap.Num > 0) {  //试推时，不做修正
            myMaps.curMap.Solved = (myMaps.mState2.size() > 0); //修正关卡预览图之是否有答案
            mySQLite.m_SQL.Set_L_Solved(myMaps.curMap.Level_id, myMaps.curMap.Solved ? 1 : 0, true);
        }

        // 状态按保存时间排序，最后保存的在最前面
        Collections.sort(myMaps.mState1, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((state_Node) o2).time.compareTo(((state_Node) o1).time);
            }
        });
        myMaps.m_StateIsRedy = false;
        Intent intent1 = new Intent();
        intent1.setClass(this, myStateBrow.class);
        myStateBrow.my_Sort = 0;  // 每次，默认移动优先排序答案
        startActivity(intent1);
    }

    int m_nMacro_Row, m_nMacro_Col;  //“宏”功能中，用于记忆仓管员坐标

    @Override
    protected void onStart() {
        super.onStart();

        mMap.m_lGoto = false;                   // 正推“跳至”
        mMap.m_lGoto2 = false;                  // 逆推“跳至”
        mMap.m_lParityBrightnessShade = false;  // 奇偶位明暗度调整
        if (myMaps.m_StateIsRedy) {  //打开状态回来
//            long exitTime = System.currentTimeMillis();
            OpenState();  //处理打开的状态
//            MyToast.showToast(this, ""+(System.currentTimeMillis() - exitTime), Toast.LENGTH_LONG);
        } else if (myMaps.m_ActionIsRedy) {  //导入动作
            if (bt_BK.isChecked()) {  //逆推，不支持“宏”，但是，可能以坐标开始
                //先提取并检查动作中的仓管员坐标
                int index = myMaps.sAction[0].indexOf("[");
                int index2 = myMaps.sAction[0].indexOf("]");

                int[] m_iPlayer2 = {-1, -1};  //记忆导入的坐标
                if (index >= 0 && index2 >= 0) {  //解去除仓管员坐标，待用
                    try {
                        String s = myMaps.sAction[0].substring(index + 1, index2);
                        String[] Arr = s.split(",");
                        m_iPlayer2[0] = Integer.parseInt(Arr[1]);  // y -- 行
                        m_iPlayer2[1] = Integer.parseInt(Arr[0]);  // x -- 列

                        //检查导入的坐标是否有效（须在图内且可以放置仓管员 -- 墙内、不是箱子）
                        if (m_iPlayer2[0] < 0 || m_iPlayer2[1] < 0 || m_iPlayer2[0] >= myMaps.curMap.Rows || m_iPlayer2[1] >= myMaps.curMap.Cols ||
                                bk_cArray[m_iPlayer2[0]][m_iPlayer2[1]] == '#' || bk_cArray[m_iPlayer2[0]][m_iPlayer2[1]] == '_' ||
                                bk_cArray[m_iPlayer2[0]][m_iPlayer2[1]] == '$' || bk_cArray[m_iPlayer2[0]][m_iPlayer2[1]] == '*')
                            throw new Exception();  //解析出来的坐标有错误

                    } catch (ArrayIndexOutOfBoundsException ex) {
                        m_iPlayer2[0] = -1;  // y -- 行
                        m_iPlayer2[1] = -1;  // x -- 列
                    } catch (Throwable ex) {
                        m_iPlayer2[0] = -1;  // y -- 行
                        m_iPlayer2[1] = -1;  // x -- 列
                    }
                }

                //执行动作前，仓管员的定位
                boolean flg = false;  //执行动作前，仓管员是否已经有了坐标，暂时默认没有
                if (myMaps.m_ActionIsPos) {  //若为从当前点执行动作，默认使用关卡中的坐标
                    //当关卡中没有仓管员时，尝试使用导入的坐标
                    if (m_nRow2 < 0 || m_nCol2 < 0 || m_nRow2 >= myMaps.curMap.Rows || m_nCol2 >= myMaps.curMap.Cols) {
                        //若导入坐标有效
                        if (m_iPlayer2[0] >= 0 && m_iPlayer2[1] >= 0 && m_iPlayer2[0] < myMaps.curMap.Rows && m_iPlayer2[1] < myMaps.curMap.Cols) {
                            m_nRow2 = m_iPlayer2[0];
                            m_nCol2 = m_iPlayer2[1];
                            m_nRow0 = m_iPlayer2[0];
                            m_nCol0 = m_iPlayer2[1];

                            //此时一定能够放置仓管员，不能放置的情况，前面已经排除
                            if (bk_cArray[m_nRow2][m_nCol2] == '.')
                                bk_cArray[m_nRow2][m_nCol2] = '+';
                            else
                                bk_cArray[m_nRow2][m_nCol2] = '@';

                            flg = true;  //仓管员使用导入的坐标
                        }
                    } else {  //关卡中有仓管员
                        flg = true;  //默认使用关卡中的坐标
                    }
                } else {  //若不是从当前点执行动作，默认使用导入的坐标
                    //导入坐标有效
                    if (m_iPlayer2[0] >= 0 && m_iPlayer2[1] >= 0 && m_iPlayer2[0] < myMaps.curMap.Rows && m_iPlayer2[1] < myMaps.curMap.Cols) {
                        m_nRow2 = m_iPlayer2[0];
                        m_nCol2 = m_iPlayer2[1];
                        m_nRow0 = m_iPlayer2[0];
                        m_nCol0 = m_iPlayer2[1];

                        //此时一定能够放置仓管员，不能放置的情况，前面已经排除
                        if (bk_cArray[m_nRow2][m_nCol2] == '.')
                            bk_cArray[m_nRow2][m_nCol2] = '+';
                        else
                            bk_cArray[m_nRow2][m_nCol2] = '@';

                        flg = true;  //默认使用导入的坐标
                    } else {  //导入坐标无效（包括导入中没有坐标、图外和不能放置仓管员的情况），尝试使用关卡中的坐标
                        if (m_nRow2 >= 0 && m_nCol2 >= 0 && m_nRow2 < myMaps.curMap.Rows && m_nCol2 < myMaps.curMap.Cols) {
                            flg = true;  //使用关卡中的坐标
                        }
                    }
                }

                if (flg) {  //坐标有效，可以执行动作
                    //不是从当前点开始执行，则逆推关卡复位
                    if (!myMaps.m_ActionIsPos) {
                        levelReset(true);
                        if (myMaps.m_Sets[13] == 1) {
                            load_BK_Level(true);  //加载即景逆推目标点
                        }
                    }
                    doACT(myMaps.sAction[0]);  //执行导入的动作
                } else {  //不能为仓管员定位，导入作废
                    MyToast.showToast(this, "没有仓管员或其位置无效！", Toast.LENGTH_SHORT);
                }
            } else {  //正推， -- 执行动作(可能包含“宏”)
                if (myMaps.isMacro(myMaps.sAction)) {
                    //宏是否从当前点执行，以首行首字符为准（需要保存 undo 序列，MacroDebug 会用的到）
                    if (!myMaps.sAction[0].isEmpty() && myMaps.sAction[0].charAt(0) == '=') {
                        myMaps.m_ActionIsPos = false;
                    } else {
                        myMaps.m_ActionIsPos = true;
                    }
                    if (myMaps.m_ActionIsPos) {
                        m_lstMovedHistory.clear();
                        Iterator myItr = m_lstMovUnDo.descendingIterator();
                        while (myItr.hasNext()) {
                            m_lstMovedHistory.offer((Byte)myItr.next());
                        }
                    }
                }

                if (!myMaps.m_ActionIsPos) {
                    levelReset(false);  //正推关卡复位
                    if (myMaps.m_Sets[25] == 1) {
                        load_Level(true);  //加载即景正推目标点
                    }
                }

                //正推时，“宏”功能之初，记忆的仓管员坐标
                m_nMacro_Row = m_nRow;
                m_nMacro_Col = m_nCol;
                mMap.myMacro.clear();
                mMap.myMacro.add(0);  //首次执行宏时，准备从第 0 行开始
                mMap.myMacroInf = "";

                myMaps.isMacroDebug = false;
//                myDo_Block(0, myMaps.sAction.length - 1, false, false);     //这里延迟较长
                //异步执行“宏”任务
                StopMicro();
                mMicroTask = new RunMicroTask(myGameView.this);
                mMicroTask.execute(0, myMaps.sAction.length - 1);
            }
            mMap.invalidate();
            m_bBusing = false;
            myMaps.m_ActionIsRedy = false;

            if (m_imPort_YASS.toLowerCase().indexOf("yass") < 0 && m_imPort_YASS.indexOf("导入") < 0) {
                m_imPort_YASS = "[导入]";
            }
        }  //导入动作处理结束
    }

    private void mLoad_Do_Macro() {
        if(bt_BK.isChecked()) {  //逆推
            MyToast.showToast(this, "逆推不支持宏功能！", Toast.LENGTH_SHORT);
        } else {
            m_nItemSelect = -1;
            //关闭进度条、奇偶位明暗度调整条
            mMap.m_lGoto = false;                   // 正推“跳至”
            mMap.m_lParityBrightnessShade = false;  // 奇偶位明暗度调整
            myMaps.mMacroList();
            if (myMaps.mFile_List.size() > 0) {
                new Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("选择：宏").setCancelable(false)
                        .setSingleChoiceItems(myMaps.mFile_List.toArray(new String[myMaps.mFile_List.size()]), -1, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            m_nItemSelect = which;
                        }
                    }).setNegativeButton("取消", null).setPositiveButton("确定", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dlg, int arg1) {
                    if (m_nItemSelect > -1) {
                        myMaps.isMacroDebug = false;
                        myMaps.sAction = myMaps.readMacroFile(myMaps.mFile_List.get(m_nItemSelect)).split("\n|\r|\n\r|\r\n|\\|");

                        //提前去掉注释行和两端的空格及制表符，方便以后处理
                        int len = myMaps.sAction.length;
                        int w, w1, w2;
                        String str;
                        for (int k = 0; k < len; k++) {
                            if (myMaps.sAction[k].trim().isEmpty()) {
                                myMaps.sAction[k] = "";
                                continue;
                            } else {
                                str = myMaps.qj2bj(myMaps.sAction[k]).trim();  //全角转换成半角
                            }
                            w = str.indexOf('<');
                            if (w >= 0) {
                                w1 = str.indexOf(';');
                                if (w1 >= 0 && w1 < w) w = w1;  //若注释符号在行内块之前
                                else {
                                    w2 = str.indexOf('>');
                                    w = str.indexOf(';', w2);  //行内块后面的注释符号
                                }
                            } else {
                                w = str.indexOf(';');
                            }
                            if (w > 0) {
                                myMaps.sAction[k] = str.substring(0, w).replaceAll("[\t]", " ").trim();
                            } else if (w == 0) {
                                myMaps.sAction[k] = "";
                            } else {
                                myMaps.sAction[k] = str;
                            }
                        }

                        //宏是否从当前点执行，以首行首字符为准（需要保存 undo 序列，MacroDebug 会用的到）
                        if (!myMaps.sAction[0].isEmpty() && myMaps.sAction[0].charAt(0) == '=') {
                            myMaps.m_ActionIsPos = false;
                        } else {
                            myMaps.m_ActionIsPos = true;
                        }
                        if (myMaps.m_ActionIsPos) {
                            m_lstMovedHistory.clear();
                            Iterator myItr = m_lstMovUnDo.descendingIterator();
                            while (myItr.hasNext()) {
                                m_lstMovedHistory.offer((Byte)myItr.next());
                            }
                        }
                        if (!myMaps.m_ActionIsPos) {
                            levelReset(false);  //正推关卡复位
                            if (myMaps.m_Sets[25] == 1) {
                                load_Level(true);  //加载即景正推目标点
                            }
                        }

                        //正推时，“宏”功能之初，记忆的仓管员坐标
                        m_nMacro_Row = m_nRow;
                        m_nMacro_Col = m_nCol;
                        mMap.myMacro.clear();
                        mMap.myMacro.add(0);  //从第 0 行开始执行
                        myMaps.isMacroDebug = false;
                        StopMicro();
                        mMicroTask = new RunMicroTask(myGameView.this);
                        mMicroTask.execute(0, myMaps.sAction.length - 1);
                        m_bBusing = false;

                        mMap.invalidate();
                        if (!m_lstMovReDo.isEmpty()) {
                            m_lstMovReDo.clear();
                        }
                        dlg.dismiss();
                }}
                }).show();
            } else {
                MyToast.showToast(this, "没有可读取的“宏”文档！", Toast.LENGTH_SHORT);
            }
        }
    }

    //执行导入的动作
    String[][] DIR = {  //答案8方位旋转之n转换算数组
            {"l", "u", "r", "d", "L", "U", "R", "D"},
            {"d", "l", "u", "r", "D", "L", "U", "R"},
            {"r", "d", "l", "u", "R", "D", "L", "U"},
            {"u", "r", "d", "l", "U", "R", "D", "L"},
            {"r", "u", "l", "d", "R", "U", "L", "D"},
            {"d", "r", "u", "l", "D", "R", "U", "L"},
            {"l", "d", "r", "u", "L", "D", "R", "U"},
            {"u", "l", "d", "r", "U", "L", "D", "R"}};
    private void doACT(String myACT){
        if (myMaps.m_ActionIsTrun) {  //根据关卡的旋转状态，转换动作串

            Pattern p = Pattern.compile("l|L|r|R|u|U|d|D");
            Matcher m = p.matcher(myACT);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                if (m.group().equals("l")) m.appendReplacement(sb, DIR[myMaps.m_nTrun][0]);
                else if (m.group().equals("u"))
                    m.appendReplacement(sb, DIR[myMaps.m_nTrun][1]);
                else if (m.group().equals("r"))
                    m.appendReplacement(sb, DIR[myMaps.m_nTrun][2]);
                else if (m.group().equals("d"))
                    m.appendReplacement(sb, DIR[myMaps.m_nTrun][3]);
                else if (m.group().equals("L"))
                    m.appendReplacement(sb, DIR[myMaps.m_nTrun][4]);
                else if (m.group().equals("U"))
                    m.appendReplacement(sb, DIR[myMaps.m_nTrun][5]);
                else if (m.group().equals("R"))
                    m.appendReplacement(sb, DIR[myMaps.m_nTrun][6]);
                else if (m.group().equals("D"))
                    m.appendReplacement(sb, DIR[myMaps.m_nTrun][7]);
            }
            m.appendTail(sb);
            formatPath(sb.toString(), bt_BK.isChecked());  //解析动作并入栈
        } else {  //关卡无旋转，无需转换动作串
            formatPath(myACT, bt_BK.isChecked());  //解析动作并入栈
        }
        int len;
        m_bACT_ERROR = false;
        if (bt_BK.isChecked()) {  //逆推
            len = m_lstMovReDo2.size();
            if (len > 0) {
                for (int k = 0; k < len; k++) reDo2();
            }
        } else {  //正推
            len = m_lstMovReDo.size();
            if (len > 0) {
                for (int k = 0; k < len; k++) reDo1();
            }
        }
    }

    //保存寄存器
    private void saveAct(String name, String value) {
        try {
            SharedPreferences.Editor editor = getSharedPreferences("BoxMan", Context.MODE_PRIVATE).edit();
            editor.putString(name, value);
            editor.commit();
        } catch (Throwable ex) { }
    }

    //读入寄存器
    private String loadAct(String name) {
        try {
            SharedPreferences sp = getSharedPreferences("BoxMan", Context.MODE_PRIVATE);
            return sp.getString(name, "");
        } catch (Throwable ex) {
            return "";
        }
    }

    //解析坐标：cs（-1 -- 是否为“行内条件”中的坐标；0 -- 绝对坐标；1 -- 相对仓管员坐标；2 -- 相对上次登记的坐标）
    private char my_Get_Position(String str, int cs) {
        int m = 0, n = 0;
        char chr = '!';  //设置一个默认的字符（关卡中不可能有的字符）

        boolean inLine = cs < 0;  //是否为“行内条件”中的坐标

        if (inLine) {  //“行内条件”中的坐标
            if (str.charAt(0) == '@') {
                cs = 1;
            } else if (str.charAt(0) == '+') {
                cs = 2;
            } else {
                cs = 0;
            }
        }

        boolean bMoves = str.indexOf('~') < 0;  //不包含“~”时，要移动仓管员

        if (str.indexOf(',') < 0) {  //没有包含逗号分隔的坐标
            if (cs > 0) {  //相对坐标时，必须包含逗号分隔的两个数字
                chr = ' ';
            } else {  //绝对坐标时，可能是“标尺”形式的坐标值
                try {
                    m = Integer.valueOf(str.replaceAll("[^0-9]", " ").trim()) - 1;

                    str = str.replaceAll("[^a-zA-Z]", " ").toLowerCase(Locale.getDefault()).trim();

                    if (str.isEmpty() || str.length() > 2) {
                        chr = ' ';
                    } else {
                        if (str.length() == 1) {
                            n = (int) str.charAt(0) - 'a';
                        } else {
                            int i = (int) str.charAt(0) - 'a';
                            int j = (int) str.charAt(1) - 'a';
                            n = (i + 1) * 26 + j;
                        }
                    }
                } catch (Throwable ex) {
                    chr = ' ';
                }
            }
        } else {  //逗号分隔的两个数字
            try {
                String[] arr = str.replaceAll("[@~\\+\\[\\]]", " ").split(",");

                m = Integer.valueOf(arr[1].trim());  // y -- 行
                n = Integer.valueOf(arr[0].trim());  // x -- 列

            } catch (ArrayIndexOutOfBoundsException ex) {
                chr = ' ';
            } catch (Throwable ex) {
                chr = ' ';
            }
        }

        if (chr != ' ') {  //坐标有效
            int r2, c2;
            switch (cs) {
                case 1:  //相对仓管员的坐标
                    r2 = m_nRow + m;
                    c2 = m_nCol + n;
                    break;
                case 2:  //相对上次登记的坐标
                    r2 = m_nMacro_Row + m;
                    c2 = m_nMacro_Col + n;
                    break;
                default:  //绝对坐标
                    r2 = m;
                    c2 = n;
            }
            if (r2 < 0 || c2 < 0 || r2 >= myMaps.curMap.Rows || c2 >= myMaps.curMap.Cols) {
                chr = ' ';  //坐标值无效
            } else {
                if (!inLine) {  //非行内条件中的坐标，需要登记、移动仓管员
                    m_nMacro_Row = r2;
                    m_nMacro_Col = c2;
                    mMap.myMacroInf = "  >>>  [ " + mMap.mGetCur(r2, c2) + " ]";
                    if (bMoves) {  //坐标不需登记（行内条件语句中的坐标）时，一定不移动仓管员的
                        if (FindPath(m_nMacro_Row, m_nMacro_Col, false)) {
                            int len = m_lstMovReDo.size();
                            for (int k = 0; k < len; k++) {
                                reDo1();
                            }
                        }
                    }
                } else {  //为行内条件中的坐标时，不登记、不移动，但返回坐标内的“元素”字符
                    chr = m_cArray[r2][c2];
                    mMap.myMacroInf = "  >>>  [ " + mMap.mGetCur(r2, c2) + " ] = " + chr;
                }
            }
        }
        return chr;
    }

    //解析数字
    private int my_Get_Num(String str, int def) {
        int num;

        try {
            num = Integer.valueOf(str.trim());
        } catch (Throwable ex) {
            num = def;  //缺省值
        }

        return num;
    }

    //执行“块”循环，从 mBegin 执行到 mEnd 行
    boolean isLooping = false;  //是否正在循环，为是否中断循环所使用
    int loopBegin = -1, loopEnd = -1;
    private int myDo_Loop(int nLine) {
        //计算循环次数
        int n = my_Get_Num(myMaps.sAction[nLine].replaceAll("[\\*]", " "), -1);
        if (n < 0) return nLine+1;  //遇到无效的循环起点，则忽略此行

        //检查循环结束行
        int k = nLine+1;
        while (k < myMaps.sAction.length) {  //查找循环结束点
            if (myMaps.sAction[k].isEmpty()) {
                k++;
                continue;  //忽略空行
            }

            if (myMaps.sAction[k].charAt(0) == '*') {  //遇到下一个循环符号
                int n2 = my_Get_Num(myMaps.sAction[k].replaceAll("[\\*]", " "), -1);
                if (n2 < 0) {  //遇到本循环体的结束点
                    k++;
                }
                break;
            }
            k++;
        }
        isLooping = true;
        loopBegin = nLine+1;  //循环块的起止位置，用以控制跳转语句，防止跳转到循环块外，即：只允许块内的短跳转，特别的，允许块外向块内的跳转
        loopEnd = k-1;
        for (int s = 0; s < n; s++) {  //循环执行 n 次
            myDo_Block(loopBegin, loopEnd, false, true);  //执行循环“块”
            if (!isLooping) break;  //循环块内，遇到中断符号“^”
        }
        if (isLooping) isLooping = false;  //循环正常结束
        loopBegin = -1;
        loopEnd = -1;

        return k;
    }

    //计算跳转位置
    private int myGet_GoTo(String mStr) {
        int k, n;
        char ch;
        String str = mStr.replaceAll("%", " ").trim();

        if (loopBegin >= 0 && str.equals("*")) {  //遇到 %*，为跳出循环指令
            isLooping = false;  //结束循环标志
            return -2;
        }

        //先计算出跳转标号（数字）
        n = my_Get_Num(str, -1);

        //遇到错误跳转语句
        if (n < 0) return -1;

        //寻找跳转位置
        int len;
        if (loopBegin >= 0) {  //正在循环块内执行跳转，否则，loopBegin == -1
            k = loopBegin;
            len = loopEnd;
        } else {
            k = 0;
            len = myMaps.sAction.length;
        }
        while (k < len) {
            if (myMaps.sAction[k].isEmpty()) {
                k++;
                continue;  //忽略空行和分号开始的行
            }

            ch = myMaps.sAction[k].charAt(0);

            if (ch == ':') {  //标记行
                if (n == my_Get_Num(myMaps.sAction[k].replaceAll(":", " "), -1)) {
                    return k + 1;  //找到标记
                }
            }
            k++;
        }
        return -1;  //没找到跳转位置
    }

    //执行“宏”语句中的“块”，从 mBegin 执行到 mEnd 行
    private void myDo_Block(int mBegin, int mEnd, boolean isUnDo, boolean isLoop) {  //是否 undo 撤销，是否 loop 循环
        int n;
        char ch;

        int step = mBegin;
        while (step <= mEnd && step < myMaps.sAction.length) {  //顺次执行“宏”中的各行
            mMap.myMacroInf = "";  //宏指令运行中，有关坐标的信息
            if (myMaps.sAction[step].isEmpty()) {  //忽略空行和分号开始的行
                step++;
                if (!isUnDo && !isLoop && mMap.myMacro.get(mMap.myMacro.size()-1) < myMaps.sAction.length) {
                    mMap.myMacro.add(step);
                }
                continue;
            }

            ch = myMaps.sAction[step].charAt(0);  //读取行首字符，作为“宏”命令

            m_bBusing = false;
            if (ch == '*') {  //“块”循环
                step = myDo_Loop(step);  //返回循环体后面的行号
            } else {  //单行
                n = myDo_Line(myMaps.sAction[step], isUnDo, isLoop);  //只有行内条件语句中有“跳转”指令时，n 才有可能是跳转位置，其它均单行执行且返回 -1
                if (n == -1) {
                    step++;
                } else if (n < -1) {  //强制循环停止动作
                    return;
                } else {
                    step = n;  //行内条件语句得到的跳转位置
                }
            }

            if (!bt_BK.isChecked()) {  //正推
                if (!isUnDo && !isLoop && mMap.myMacro.get(mMap.myMacro.size() - 1) < myMaps.sAction.length) {
                    mMap.myMacro.add(step);
                }
                if (!isUnDo && !isLoop && myMaps.isMacroDebug) break;  //断点生效
            }
        }
    }

    //执行“宏”语句中的“一行”
    private int myDo_Line(String myACT, boolean isUnDo, boolean isLoop) {
        if (myACT.isEmpty()) return -1;  //空语句

        String str;
        String[] myArr;
        int n, m;
        char ch;

        //解析出“宏”命令字符
        ch = myACT.charAt(0);

        m_bACT_IgnoreCase = false;  //默认区分动作字符的大小写
        m_bACT_ERROR = false;  //执行动作时是否遇到错误
        if (ch == '{') {  //“寄存器”动作或常规动作
            try {
                if (myACT.indexOf('=') >= 0) {  //变量赋值语句
                    myArr = myACT.replaceAll("[\\{\\}]", " ").split("=");
                    if (myArr.length > 1) {
                        str = myArr[0].trim();  //寄存器号
                        n = my_Get_Num(str, 0);  //若为寄存器号
                        str = myArr[1].trim();  //常规动作串
                        if (n > 9 && n < 100 && myMaps.isLURD(str)) {  //暂存动作串到寄存器中，避开玩家手动寄存的动作
                            saveAct("reg" + n, str);
                        }
                    }
                } else {
                    m_bACT_IgnoreCase = (myACT.indexOf('~') >= 0);  //是否忽略动作字符的大小写
                    myArr = myACT.replaceAll("~", " ").split("\\{|\\}");
                    if (myArr.length > 1) {
                        str = myArr[1].trim();  //默认为常规动作串

                        n = my_Get_Num(str, 0);  //若为寄存器号
                        if (n > 0 && n < 100) {  //读取寄存器中的动作串
                            str = loadAct("reg" + n);
                        }
                    } else {
                        str = "";
                    }
                    if (myArr.length > 2) {  //若包含重复次数
                        n = my_Get_Num(myArr[2].trim(), 1);  //重复次数

                        if (n == 0) {  //近似无限重复
                            n = Integer.MAX_VALUE;
                        }

                        if (n > 1) {  //按次数重复
                            for (int k = 0; k < n; k++) {
                                doACT(str);
                                if (m_bACT_ERROR) {
                                    break;  //执行动作时遇到错误
                                }
                            }
                        } else {  //执行一次
                            doACT(str);
                        }
                    } else {  //缺少后面的“}”时，没有重复次数，默认执行一次
                        doACT(str);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
            } catch (Throwable ex) { }
        } else if (ch == '?') {  //执行：行内“分支”

            myArr = myACT.split("\\?|:|\\/");

            if (myArr.length == 3 || myArr.length == 4) {

                String[] myArr2 = myArr[1].split("=");

                if (myArr2.length == 2) {
                    char ch2 = my_Get_Position(myArr2[0].trim(), -1);  //取得坐标上的字符

                    mMap.myMacroInf = mMap.myMacroInf + " ▲ " + myACT;

                    if (myArr2[1].trim().indexOf(ch2) >= 0) {  //是否满足条件
                        return myDo_Line(myArr[2].trim(), isUnDo, isLoop);  //执行第 3 部分
                    } else {
                        if (myArr.length == 4) {
                            return myDo_Line(myArr[3].trim(), isUnDo, isLoop);  //执行第 4 部分
                        }
                    }
                }
            }
        } else if (ch == '(') {  //后退步数
            try {
                myArr = myACT.split("\\(|\\)");
                n = my_Get_Num(myArr[1].trim(), -1);
                for (int k = 0; k < n; k++) unDo1();
            } catch (Throwable ex) { }
        } else if (ch == '@') {  //相对当前的坐标调整
            my_Get_Position(myACT, 1);
        } else if (ch == '+') {  //相对上次的坐标调整
            my_Get_Position(myACT, 2);
        } else if (ch == '[') {  //坐标
            my_Get_Position(myACT, 0);
        } else if (ch == '%') {  //跳转语句，有可能是 ？ 语句过来的
            return myGet_GoTo(myACT);  //计算跳转位置
        } else if (ch == '^') {  //断点、进入宏调试模式
            if (!isUnDo && !isLoop) {
                myMaps.isMacroDebug = true;
            }
        } else if (ch == '<') {  //行内块标记
            n = myACT.indexOf('>');
            if (n >= 0) {  //取得重复次数
                m = my_Get_Num(myACT.substring(n).replaceAll(">~", " ").trim(), 1);
                myArr = myACT.substring(0, n).replaceAll("<", " ").split(";");
            } else {
                m = 1;
                myArr = myACT.replaceAll("<", " ").split(";");
            }
            int len = myArr.length;
            for (int t = 0; t < m; t++) {  //重复行内块
                for (int k = 0; k < len; k++) {  //顺次执行行内各“宏”语句
                    n = myDo_Line(myArr[k].trim(), isUnDo, isLoop);
                    if (n >= 0) return n;  //包含跳转语句
                }
            }
        } else if (ch == ':') {  //标记号，对应于 GoTo
        } else {  //视为常规动作 Lurd
            doACT(myACT);
        }
        return -1;
    }

    //停止“宏”功能运行
    public void StopMicro() {
        if (mMicroTask != null) {
            if (!mMicroTask.isCancelled() && mMicroTask.getStatus() == AsyncTask.Status.RUNNING) {
                mMicroTask.cancel(true);
            }
            mMicroTask = null;
        }
    }

    @Override
    protected void onDestroy() {
        showSystemUI();  // 显示系统的那三个按钮
        try {
            BoxMan.saveSets();  //保存设置
        } catch (Throwable ex) { }

        myStop();

        super.onDestroy();
    }

    //结束推箱子界面前的处理
    private void myStop() {
        if (mTask != null) {
            if (!mTask.isCancelled() && mTask.getStatus() == AsyncTask.Status.RUNNING) {
                mTask.cancel(true);
            }
            mTask = null;
        }
        StopMicro();
        if (myTimer1 != null) {
            myTimer1.removeCallbacksAndMessages(null);
            myTimer1 = null;
        }
        if (myTimer2 != null) {
            myTimer2.removeCallbacksAndMessages(null);
            myTimer2 = null;
        }
        if (myTimer3 != null) {
            myTimer3.removeCallbacksAndMessages(null);
            myTimer3 = null;
        }
        if (myTimer4 != null) {
            myTimer4.removeCallbacksAndMessages(null);
            myTimer4 = null;
        }

        mMap.d_Moves = mMap.m_PicWidth;
    }

    //为消除音量键的按键音
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (myMaps.m_Sets[15] == 1 && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
            return true;

        return super.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (!myMaps.isRecording) {
                if (m_bMoved && (m_lstMovUnDo.size() > 0 || m_lstMovUnDo2.size() > 0)) {
                    exitDlg.show();
                } else {
                    myStop();
                    finish();
                }
                return true;
            } else {  //开启录制时，返回导入窗口
                //传递参数
                setACT(false);  //录制被取消

                myMaps.m_ActionIsRedy = false;

                if (bt_BK.isChecked())
                    myMaps.m_nRecording_Bggin2 = m_lstMovUnDo2.size();  //逆推录制起始点
                else
                    myMaps.m_nRecording_Bggin = m_lstMovUnDo.size();  //正推录制起始点

                Intent intent2 = new Intent(this, myActGMView.class);
                //用Bundle携带数据
                Bundle bundle = new Bundle();
                bundle.putBoolean("is_BK", bt_BK.isChecked());  //传递参数：是否逆推
//                bundle.putString("LOCAL", myMaps.getLocale(m_cArray));  //关卡现场地图数据
                intent2.putExtras(bundle);

                intent2.setClass(myGameView.this, myActGMView.class);
                startActivity(intent2);
            }
            return true;
        } else if (myMaps.m_Sets[15] == 1 && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            int n1 = myMaps.m_lstMaps.indexOf(myMaps.curMap);
            if (n1 > 0) {
                //指向上一关卡
                if (m_bMoved) exitDlg2.show();
                else myPre(n1-1);
            } else {
                MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
            }
            return true;
        } else if (myMaps.m_Sets[15] == 1 && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            int n2 = myMaps.m_lstMaps.indexOf(myMaps.curMap);
            if (n2 >= 0 && n2+1 < myMaps.m_lstMaps.size()) {
                //指向下一关卡
                if (m_bMoved) exitDlg3.show();
                else myNext(n2+1);
            } else {
                MyToast.showToast(myGameView.this, "没有了！", Toast.LENGTH_SHORT);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void hideSystemUI() {
        if (myMaps.m_Sets[16] == 1) return;

        int myFlags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |         //隐藏导航栏或操作栏
                      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |        //隐藏状态栏和导航栏时的沉浸模式和保持交互性
                      View.SYSTEM_UI_FLAG_FULLSCREEN |              //全屏显示
                      View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        getWindow().getDecorView().setSystemUiVisibility(myFlags);     //隐藏状态栏和导航栏时的沉浸模式和保持交互性
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    //设置自定义状态栏、菜单栏是否可见
    public void setMenuVisible() {
        LinearLayout llayout = (LinearLayout) findViewById(R.id.main_bottom);
        if (llayout.getVisibility() == View.VISIBLE) {
            llayout.setVisibility(View.GONE);
            mMap.m_nArenaTop = 0;
        } else {
            llayout.setVisibility(View.VISIBLE);
            mMap.m_nArenaTop = myMaps.m_nWinHeight * 27 / 640;
        }
        mMap.invalidate();
    }

    //计算可以动的箱子
    public void boxNoMoved() {

        if (bt_BK.isChecked()) return;  //此功能仅在正推状态使用

        int row, col, dir;

        row = m_nRow3;
        col = m_nCol3;
        mMap.m_boxNoMoved = true;
        mMap.m_boxNoUsed = true;
        dir = 0;

        for (int i = 0; i < myMaps.curMap.Rows; i++)
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                if (mark7[i][j]) mark11[i][j] = false;  //记录哪个箱子没有移动过
                else mark11[i][j] = true;
                if (mark8[i][j]) mark12[i][j] = false;  //记录哪个地板未使用过
                else mark12[i][j] = true;
            }

        byte mDir;
        int t = -1;
        Iterator myItr = m_lstMovUnDo.iterator();
        while (myItr.hasNext()) {
            mDir = (Byte)myItr.next();
            switch (mDir) {
                case 1:
                case 5:
                    col--;
                    t = 0;
                    break;
                case 2:
                case 6:
                    row--;
                    t = 2;
                    break;
                case 3:
                case 7:
                    col++;
                    t = 1;
                    break;
                case 4:
                case 8:
                    row++;
                    t = 3;
                    break;
            }

            if (mDir > 4 && t >= 0 && !mark11[row - dr4[t] * dir][col - dc4[t] * dir]) {
                mark11[row - dr4[t] * dir][col - dc4[t] * dir] = true;
                mark12[row - dr4[t] * dir][col - dc4[t] * dir] = true;  //地板被使用
            }
            mark12[row][col] = true;  //地板被使用
        }
    }

    //计算可以推过来的箱子
    public void boxCanCome(int row, int col) {
        boolean[][] mark, mark9;
        char[][] level;
        int nRow, nCol;

        if (bt_BK.isChecked()) {  //逆推
            level = bk_cArray;
            mark = mPathfinder.mark4;  //用于记录箱子的可达点
            mark9 = mark12;  //用于记录可来的箱子
            mMap.m_boxCanCome2 = true;
            nRow = m_nRow2;
            nCol = m_nCol2;
        } else {
            level = m_cArray;
            mark = mPathfinder.mark3;
            mark9 = mark11;
            mMap.m_boxCanCome = true;
            nRow = m_nRow;
            nCol = m_nCol;
        }

        for (int i = 0; i < myMaps.curMap.Rows; i++) {
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                mark9[i][j] = false;
            }
        }

        //遍历箱子，检查是否可达该点
        for (int i = 0; i < myMaps.curMap.Rows; i++) {
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                if (level[i][j] == '$' || level[i][j] == '*') {
                    mPathfinder.FindBlock(bt_BK.isChecked(), level, i, j);
                    mPathfinder.boxReachable(bt_BK.isChecked(), i, j, nRow, nCol);
                    mark9[i][j] = mark[row][col];
                }
            }
        }
        mark9[row][col] = true;  //标示所按的空地
    }

    //计算可以动的箱子
    public void boxCanMove() {
        char[][] level;
        boolean[][] mark, mark9;
        boolean flg = bt_BK.isChecked();

        if (flg) {  //逆推
            level = bk_cArray;
            mark = mPathfinder.mark2;
            mark9 = mark12;  //记录哪个箱子能动
            mMap.m_boxCanMove2 = true;
        } else {
            level = m_cArray;
            mark = mPathfinder.mark1;
            mark9 = mark11;
            mMap.m_boxCanMove = true;
        }

        for (int i = 0; i < myMaps.curMap.Rows; i++)
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                mark9[i][j] = false;
            }

        for (int i = 0; i < myMaps.curMap.Rows; i++) {
            for (int j = 0; j < myMaps.curMap.Cols; j++) {
                if (level[i][j] == '$' || level[i][j] == '*') {
                    for (int k = 0; 4 > k; k++) {
                        try {
                            if (flg) {  //逆推
                                if (mark[i + dr4[k]][j + dc4[k]] && (level[i + dr4[k] * 2][j + dc4[k] * 2] == '-' || level[i + dr4[k] * 2][j + dc4[k] * 2] == '.' || level[i + dr4[k] * 2][j + dc4[k] * 2] == '@' || level[i + dr4[k] * 2][j + dc4[k] * 2] == '+')) {
                                    mark9[i][j] = true;
                                    break;
                                }
                            } else {
                                if (mark[i + dr4[k]][j + dc4[k]] && (level[i - dr4[k]][j - dc4[k]] == '-' || level[i - dr4[k]][j - dc4[k]] == '.' || level[i - dr4[k]][j - dc4[k]] == '@' || level[i - dr4[k]][j - dc4[k]] == '+')) {
                                    mark9[i][j] = true;
                                    break;
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException ex) {
                        } catch (Throwable ex) {
                        }
                    }
                }
            }
        }
    }

    public boolean FindPath(int k, int l, boolean flg) {
        LinkedList<Byte> path;  //得到逆序的路径
        LinkedList<Byte> pathByteList;

        if (flg) {  //逆推
            path = mPathfinder.manTo(true, bk_cArray, m_nRow2, m_nCol2, k, l);
            pathByteList = m_lstMovReDo2;
        } else {
            path = mPathfinder.manTo(false, m_cArray, m_nRow, m_nCol, k, l);
            pathByteList = m_lstMovReDo;
        }

        if (!path.isEmpty()) {
            pathByteList.clear();
            while (!path.isEmpty()) {
                pathByteList.offer(path.removeFirst());
            }
            return true;
        } else return false;
    }

    //格式化路径
    private void formatPath(String strPath, boolean flg) {
//        m_Path_Formating = true;
        int Len = strPath.length();

        if (Len > 0) {
            LinkedList<Byte> pathByteList = flg ? m_lstMovReDo2 : m_lstMovReDo;
            pathByteList.clear();
            for (int t = 0; t < Len; t++) {
                switch (strPath.charAt(t)) {
                    case 'l':
                        pathByteList.offerFirst((byte)1);
                        break;
                    case 'u':
                        pathByteList.offerFirst((byte)2);
                        break;
                    case 'r':
                        pathByteList.offerFirst((byte)3);
                        break;
                    case 'd':
                        pathByteList.offerFirst((byte)4);
                        break;
                    case 'L':
                        pathByteList.offerFirst((byte)5);
                        break;
                    case 'U':
                        pathByteList.offerFirst((byte)6);
                        break;
                    case 'R':
                        pathByteList.offerFirst((byte)7);
                        break;
                    case 'D':
                        pathByteList.offerFirst((byte)8);
                        break;
                }
            }
        }
//        m_Path_Formating = false;
    }

    //计数逆推区域箱子数的异步任务和关卡正推初态中陷于网锁的箱子（这些箱子正推时不可动）
    private class AsyncCountBoxsTask extends AsyncTask<Integer, Void, short[][]> {

        boolean m_bNoSolution = false;  //关卡是否无解
        boolean[][] mk15, mk16;  //15：记录逆推死点（正推初态中被冻结的箱子）；16 - 记录正推死点（正推初态中陷于网锁的箱子）
        short[][] mk14;  //记录逆推中的区域箱子数
        byte[][] mk0;
        char[][] mArray;   //空地图，逆推死点（正推初态中被冻结的箱子）被变成了墙壁
        char[][] mArray0;  //地图副本
        byte[][] freezeBoxs;  // 记录该位置的箱子是否已经检查过了
        IntStack intStack;
        int m_Rows, m_Cols;

        private final WeakReference<myGameView> mViewReference;

        public AsyncCountBoxsTask(myGameView mView) {
            super();
            mViewReference = new WeakReference<myGameView>(mView);
        }

        @Override
        protected void onPreExecute() {
            mark14 = null;
            mark15 = null;
            mark16 = null;
            mArray9 = null;
            m_bNoSolution = false;
            bt_More.setTextColor(0xffcc0000);

            super.onPreExecute();
        }

        @Override
        protected short[][] doInBackground(Integer... params) {
            try {
                //计算逆推的预知死锁点，比较耗时，将在异步进程中调用此方法
                m_Rows = params[0];
                m_Cols = params[1];

                int mRow = m_nRow;
                int mCol = m_nCol;

                if (isCancelled()) return null;

                mk0  = new byte[m_Rows][m_Cols];
                mk15 = new boolean[m_Rows][m_Cols];
                mk16 = new boolean[m_Rows][m_Cols];
                mk14 = new short[m_Rows][m_Cols];
                freezeBoxs = new byte[m_Rows][m_Cols];
                intStack = new IntStack(200);  // 登记“Z型”冻结的箱子的位置，每个“Z型”线，最多箱子数

                mArray = new char[m_Rows][m_Cols];   // 临时地图，正推根据目标数识别死锁时，也会用到
                mArray0 = new char[m_Rows][m_Cols];  // 地图副本，建立副本的目的，是避免异步进程正在计算的时候，玩家若切换了关卡，会造成 APP 出错崩溃（数组索引可能出现越界）

                // 生成临时关卡图
                for (int i = 0; i < m_Rows; i++) {
                    for (int j = 0; j < m_Cols; j++) {
                        if (isCancelled()) return null;
                        mArray0[i][j] = m_cArray0[i][j];  // 从关卡的初态数组取得关卡数据
                        // 临时地图，只有墙壁和地板
                        if (mArray0[i][j] == '#' || mArray0[i][j] == '_') mArray[i][j] = '#';
                        else mArray[i][j] = '-';
                    }
                }

                setBlock4();  // 登记全部的“四块”冻结

                // 检查并登记其“Z型”冻结
                int pos, r, c;
                for (int i = 0; i < m_Rows; i++) {
                    for (int j = 0; j < m_Cols; j++) {
                        if (isCancelled()) return null;
                        // 若遇到没有检查过的箱子，检查并登记其“Z型”冻结情况
                        if ((mArray0[i][j] == '$' || mArray0[i][j] == '*') && freezeBoxs[i][j] == 0) {
                            if (isZ_Freeze(i, j)) {    // 若被“Z型”冻结，处理被冻结的箱子群
                                while (!intStack.isEmpty()) {
                                    pos = intStack.remove();
                                    r = pos >>> 16;
                                    c = pos & 0x0000ffff;
                                    freezeBoxs[r][c] = 2;    // 冻结登记
                                }
                            }
                        }
                    }
                }

                // 把正推初态中被冻结的箱子设置为逆推死点，同时，在逆推时，不允许移动这些箱子
                for (int i = 0; i < m_Rows; i++) {
                    for (int j = 0; j < m_Cols; j++) {
                        if (isCancelled()) return null;
                        if (isBox2(i, j) && freezeBoxs[i][j] > 0) {
                            if (mArray0[i][j] == '$' && !m_bNoSolution) m_bNoSolution = true;  // 关卡是否无解
                            mk15[i][j] = true;
                            mArray0[i][j] = '#';
                            mArray[i][j] = '#';
                        }
                    }
                }

                // 检查正推初态形成网锁的箱子，这些箱子在真正正推时，不允许移动，同时，也属于逆推死锁数据测算的准备
                for (int i = 0; i < m_Rows; i++) {
                    for (int j = 0; j < m_Cols; j++) {
                        if (isCancelled()) return null;

                        if (mArray0[i][j] == '*' && mArray[i][j] != '#' && checkNet(mArray0, i, j)) {
                            mk15[i][j] = true;
                            mk16[i][j] = true;
                            mArray0[i][j] = '#';
                            mArray[i][j] = '#';
                        }
                    }
                }

                publishProgress();
                mark15 = mk15;  //真正逆推时，不允许拉动的箱子（即那些正推初态中，不能移动的箱子 -- 正推时，初态本就死锁的箱子）
                mark16 = mk16;  //真正正推时，不允许推动的箱子（即那些正推初态中，不能移动的箱子 -- 形成网锁的箱子）

                //正推法，测算逆推区域箱子数（区域的点位数）
                mPF = new myPathfinder(m_Rows, m_Cols);  //检查死锁之探路者，正推根据目标数识别死锁时，也会用到
                mPF.FindBlock(false, mArray, mRow, mCol);   //计算临时地图的割点，块
                for (int i = 0; i < m_Rows; i++) {
                    for (int j = 0; j < m_Cols; j++) {
                        if (isCancelled()) return null;
                        //把正推中的箱子逐个放到临时（空）地图里，进行探查测算
                        if (mArray0[i][j] == '$' || mArray0[i][j] == '*') {
                            mArray[i][j] = '$';
                            mPF.boxReachable(false, i, j, mRow, mCol);  //探查可达点

                            //累计每个格子中，有多少个箱子可达，可达数相同的相邻格子，自动组成一个区域
                            for (r = 0; r < m_Rows; r++) {
                                for (c = 0; c < m_Cols; c++) {
                                    if (isCancelled()) return null;
                                    if (mPF.mark3[r][c]) mk14[r][c]++;  //不能到达的点，是不需计数的
                                }
                            }
                            mArray[i][j] = '-';
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                return null;
            } catch (OutOfMemoryError ex) {
                return null;
            } catch (Throwable ex) {
                return null;
            }
            return mk14;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (m_bNoSolution) {  //若为无解的关卡，则提醒
                Builder dlg = new Builder(myGameView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setTitle("提醒").setMessage("这是一个无解的关卡！")
                        .setPositiveButton("确定", null).setCancelable(false).create().show();
                m_bNoSolution = false;
            }

            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(short[][] mk) {
            if (isCancelled()) {
                mark14 = null;
                mark15 = null;
                mark16 = null;
                mArray9 = null;
                mk = null;
            }

            if (mViewReference != null) {
                mark14 = mk14;  //逆推的区域箱子数
                mArray9 = mArray;  //空地图，正推根据目标数识别死锁时，也会用到

//                System.out.println("===========================");
//                for (int i = 0; i < m_Rows; i++) {
//                    for (int j = 0; j < m_Cols; j++) {
//                        if (m_cArray[i][j] == '#') System.out.print('#');
//                        else if (mark14[i][j] > 0) System.out.print(mark14[i][j]);
//                        else System.out.print('-');
//                    }
//                    System.out.println();
//                }
            }
            super.onPostExecute(mk);

            bt_More.setTextColor(0xffffffff);
        }

        // 是否遇到墙壁、墙外或界外
        private boolean isWall(int row, int col) {
            return  row < 0 || col < 0 || row >= m_Rows || col >= m_Cols ||
                    mArray0[row][col] == '#' || mArray0[row][col] == '_' ||
                    freezeBoxs[row][col] == 4;  // 在“Z型”冻结检查时，“四块”视作墙壁
        }

        // 是否通道
        private boolean isPass(int row, int col) {
            if (row < 0 || col < 0 || row >= m_Rows || col >= m_Cols) return false;
            return  mArray0[row][col] == '-' || mArray0[row][col] == '.' ||
                    mArray0[row][col] == '@' || mArray0[row][col] == '+';
        }

        // 是否箱子或墙壁
        private boolean isBoxOrWall(int row, int col) {
            if (row < 0 || col < 0 || row >= m_Rows || col >= m_Cols ||
                    mArray0[row][col] == '#' || mArray0[row][col] == '_' ||
                    mArray0[row][col] == '$' || mArray0[row][col] == '*') return true;
            return false;
        }

        // 是否箱子
        private boolean isBox2(int row, int col) {
            if (row < 0 || col < 0 || row >= m_Rows || col >= m_Cols) return false;
            return mArray0[row][col] == '$'|| mArray0[row][col] == '*';
        }

        // 登记全部的“四块”冻结
        private void setBlock4() {
            for (int i = 0; i < m_Rows; i++) {
                for (int j = 0; j < m_Cols; j++) {
                    if (isBoxOrWall(i, j) && isBoxOrWall(i, j-1) && isBoxOrWall(i-1, j) && isBoxOrWall(i-1, j-1)) {
                        if (isBox2(i, j)) freezeBoxs[i][j] = 4;
                        if (isBox2(i, j-1)) freezeBoxs[i][j-1] = 4;
                        if (isBox2(i-1, j)) freezeBoxs[i-1][j] = 4;
                        if (isBox2(i-1, j-1)) freezeBoxs[i-1][j-1] = 4;
                    }
                    else freezeBoxs[i][j] = 0;
                }
            }
        }

        // 是否引发了“Z型”死锁
        private final int[][] dA = {
                { 0, -1, -1, 0 },  // 先左后上
                { 0, -1,  1, 0 },  // 先左后下
                { 0,  1, -1, 0 },  // 先右后上
                { 0,  1,  1, 0 }   // 先右后下
        };
        private final int[][] dB = {
                {  1, 0, 0,  1 },  // 先下后右
                { -1, 0, 0,  1 },  // 先上后右
                {  1, 0, 0, -1 },  // 先下后左
                { -1, 0, 0, -1 }   // 先上后左
        };
        private boolean isZ_Freeze(int row, int col) {
            intStack.clear();
            if (isZ(row, col, dA[0]) && isZ(row, col, dB[0])) return true;  // 左上 - 下右
            intStack.clear();
            if (isZ(row, col, dA[1]) && isZ(row, col, dB[1])) return true;  // 左下 - 上右
            intStack.clear();
            if (isZ(row, col, dA[2]) && isZ(row, col, dB[2])) return true;  // 右上 - 下左
            intStack.clear();
            if (isZ(row, col, dA[3]) && isZ(row, col, dB[3])) return true;  // 右下 - 上左

            return false;
        }

        // 是否形成单方向上的“Z型”冻结
        private boolean isZ(int row, int col, int[] dDir) {
            int r = row, c = col;
            boolean flg = true;

            while (true) {
                intStack.add(r << 16 | c);                                     // 缓存被检查的箱子

                if (flg) { r += dDir[0]; c += dDir[1]; }
                else     { r += dDir[2]; c += dDir[3]; }

                if (isPass(r, c)) break;                                       // 遇到通道，没有冻结
                if (isWall(r, c)) return true;                                 // 遇到墙壁、“四块”、或界外
                flg = !flg;                                                    // 折转
            }
            return false;
        }

        // 检查并标识陷于网锁的箱子
        private boolean checkNet(char[][] m_Arr, int b_new_Row, int b_new_Col) {
            int mRows = m_Arr.length, mCols = m_Arr[0].length;

            // 访问标志复位
            for (int i = 0; i < mRows; i++) {
                for (int j = 0; j < mCols; j++) {
                    mk0[i][j] = 0;
                }
            }

            Queue<Integer> Q = new LinkedList<Integer>();
            Queue<Integer> Q2 = new LinkedList<Integer>();

            int P, r = b_new_Row, c = b_new_Col, r1, c1, r2, c2;

            Q.offer(r << 16 | c);  // 初始位置入队列，待查其四邻
            mk0[r][c] = 1;

            while (!Q.isEmpty()) {
                P = Q.poll();   // 出队列
                r = P >>> 16;
                c = P & 0x0000ffff;

                for (int k = 0; k < 4; k++) {
                    r1 = r + dr4[k];
                    c1 = c + dc4[k];
                    r2 = r + dr4[k] * 2;
                    c2 = c + dc4[k] * 2;

                    if (isVisited(mk0, r2, c2) || isWall(r2, c2) || isWall(r1, c1)) {  // 箱子的一侧临墙，已经包含了墙外和界外的处理
                        continue;
                    } else if (m_Arr[r2][c2] == '-' || m_Arr[r2][c2] == '.' || m_Arr[r2][c2] == '@' || m_Arr[r2][c2] == '+' || m_Arr[r2][c2] == '$') {  // 网口
                        return false;
                    } else {
                        Q.offer(r2 << 16 | c2);
                        Q2.offer(r2 << 16 | c2);
                        mk0[r2][c2] = 1;
                    }
                }
            }

            // 此时，以形成网锁，将本位之外的箱子做“网锁”标识
            while (!Q2.isEmpty()) {
                P = Q2.poll();//出队列
                r = P >>> 16;
                c = P & 0x0000ffff;
                mk15[r][c] = true;
                mk16[r][c] = true;
                m_Arr[r][c] = '#';
                mArray[r][c] = '#';
            }

            return true;
        }

    }

    //运行“宏”的异步任务
    private class RunMicroTask extends AsyncTask<Integer, Void, Void> {

        private final WeakReference<myGameView> mViewReference2;
        private long myTime, myTime0;  //计算两次刷新时间间隔及总耗时
        private final int[] mySpeed = {0, 200, 300, 500, 1000};

        public RunMicroTask(myGameView mView) {
            super();
            mViewReference2 = new WeakReference<myGameView>(mView);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            myTime0 = System.currentTimeMillis();  //开始时间
            myTime  = myTime0;
        }

        @SuppressLint("WrongThread")
        @Override
        protected Void doInBackground(Integer... params) {
            try {
                int mBegin     = params[0];
                int mEnd       = params[1];

                int n;
                char ch;

                int step = mBegin;
                while (step <= mEnd && step < myMaps.sAction.length) {  //顺次执行“宏”中的各行
                    if (isCancelled()) return null;

                    mMap.myMacroInf = "";  //宏指令运行中，有关坐标的信息
                    if (myMaps.sAction[step].isEmpty()) {  //忽略空行和分号开始的行
                        step++;
                        if (mMap.myMacro.get(mMap.myMacro.size()-1) < myMaps.sAction.length) {
                            mMap.myMacro.add(step);
                        }
                        continue;
                    }

                    ch = myMaps.sAction[step].charAt(0);  //读取行首字符，作为“宏”命令

                    m_bBusing = false;
                    if (ch == '*') {  //“块”循环
                        step = myDo_Loop(step);  //返回循环体后面的行号
                    } else {  //单行
                        n = myDo_Line(myMaps.sAction[step], false, false);  //只有行内条件语句中有“跳转”指令时，n 才有可能是跳转位置，其它均单行执行且返回 -1
                        if (n == -1) {
                            step++;
                        } else if (n < -1) {  //强制循环停止动作
                            return null;
                        } else {
                            step = n;  //行内条件语句得到的跳转位置
                        }
                    }

                    if (!bt_BK.isChecked()) {  //正推
                        if (mMap.myMacro.get(mMap.myMacro.size() - 1) < myMaps.sAction.length) {
                            mMap.myMacro.add(step);
                        }
                        if (myMaps.isMacroDebug) break;  //断点生效
                    }
                    if (System.currentTimeMillis() - myTime > mySpeed[myMaps.m_Sets[10]]) {  //刷新间隔
                        myTime = System.currentTimeMillis();
                        publishProgress();
                    }
                }
                return null;
            } catch (ArrayIndexOutOfBoundsException ex) {
            } catch (OutOfMemoryError ex) {
            } catch (Throwable ex) {
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            if (mViewReference2 != null) mMap.invalidate();
        }

        @Override
        protected void onPostExecute(Void rt) {
            if (mViewReference2 != null) {
                mMap.invalidate();
                m_bBusing = false;
                myMaps.m_ActionIsRedy = false;
                MyToast.showToast(myGameView.this, "耗时 " + ((myTime - myTime0) / 1000) + " 秒", Toast.LENGTH_SHORT);
                if (!m_lstMovReDo.isEmpty()) {  //仅正推
                    m_lstMovReDo.clear();
                }
                StopMicro();
            }
            super.onPostExecute(rt);
        }
    }
}
