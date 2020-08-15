package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class myRecogView extends Activity {

    AlertDialog exitDlg;

    Menu myMenu = null;

    myRecogViewMap mMap;

    char[][] m_cArray, bk_cArray;  //地图，备份地图
    int m_nRows, m_nCols;
    int m_nBoxNum, DstNum;

    int isActNum = -1;
    boolean isInAction = false;

    private ProgressDialog dialog;

    Button bt_Floor;
    Button bt_Wall;
    Button bt_Box;
    Button bt_BoxGoal;
    Button bt_Goal;
    Button bt_Player;
    Button bt_Color;
    Button bt_Left;
    Button bt_Right;
    Button bt_Up;
    Button bt_Down;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recog_view);

        //开启标题栏的返回键
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        setTitle("");

        mMap = (myRecogViewMap) this.findViewById(R.id.myRecogMap);
        mMap.Init(this);

        initMap();

        bt_Floor = (Button) findViewById(R.id.bt_floor);  //地板
        bt_Floor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nObj != 0) {
                    mMap.m_nObj = 0;
                    setColor();
                    bt_Floor.setBackgroundColor(0x9f0000ff);
                } else {
                    mMap.m_nObj = -1;
                    bt_Floor.setBackgroundColor(0xff334455);
                }
            }
        });
        bt_Floor.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] m_menu = {
                        "清空地图"
                };
                isActNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearXSB();
                        myCount();
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, isActNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isActNum = which;
                    }
                }).setCancelable(false).create().show();
                mMap.invalidate();
                return true;
            }
        });

        bt_Wall = (Button) findViewById(R.id.bt_wall);  //墙壁
        bt_Wall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nObj != 1) {
                    mMap.m_nObj = 1;
                    setColor();
                    bt_Wall.setBackgroundColor(0x9f0000ff);
                } else {
                    mMap.m_nObj = -1;
                    bt_Wall.setBackgroundColor(0xff334455);
                }
            }
        });
        bt_Wall.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] m_menu = {
                        "清理墙壁"
                };
                isActNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('#', true);
                        myCount();
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, isActNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isActNum = which;
                    }
                }).setCancelable(false).create().show();
                mMap.invalidate();
                return true;
            }
        });

        bt_Box = (Button) findViewById(R.id.bt_box);  //箱子
        bt_Box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nObj != 2) {
                    mMap.m_nObj = 2;
                    setColor();
                    bt_Box.setBackgroundColor(0x9f0000ff);
                } else {
                    mMap.m_nObj = -1;
                    bt_Box.setBackgroundColor(0xff334455);
                }
            }
        });
        bt_Box.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] m_menu = {
                        "清理纯箱子",
                        "清理所有箱子"
                };
                isActNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('$', isActNum == 1);
                        myCount();
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, isActNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isActNum = which;
                    }
                }).setCancelable(false).create().show();
                return true;
            }
        });

        bt_BoxGoal = (Button) findViewById(R.id.bt_boxgoal);  //目标点上的箱子
        bt_BoxGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nObj != 3) {
                    mMap.m_nObj = 3;
                    setColor();
                    bt_BoxGoal.setBackgroundColor(0x9f0000ff);
                } else {
                    mMap.m_nObj = -1;
                    bt_BoxGoal.setBackgroundColor(0xff334455);
                }
            }
        });
        bt_BoxGoal.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] m_menu = {
                        "清理目标点箱子",
                        "保留目标点清理箱子"
                };
                isActNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('*', isActNum == 0);
                        myCount();
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, isActNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isActNum = which;
                    }
                }).setCancelable(false).create().show();
                return true;
            }
        });

        bt_Goal = (Button) findViewById(R.id.bt_goal);  //目标点
        bt_Goal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nObj != 4) {
                    mMap.m_nObj = 4;
                    setColor();
                    bt_Goal.setBackgroundColor(0x9f0000ff);
                } else {
                    mMap.m_nObj = -1;
                    bt_Goal.setBackgroundColor(0xff334455);
                }
            }
        });
        bt_Goal.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] m_menu = {
                        "清理纯目标点",
                        "清理所有目标点"
                };
                isActNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('.', isActNum == 1);
                        myCount();
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, isActNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isActNum = which;
                    }
                }).setCancelable(false).create().show();
                return true;
            }
        });

        bt_Player = (Button) findViewById(R.id.bt_player);  //仓管员
        bt_Player.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nObj != 5) {
                    mMap.m_nObj = 5;
                    setColor();
                    bt_Player.setBackgroundColor(0x9f0000ff);
                } else {
                    mMap.m_nObj = -1;
                    bt_Player.setBackgroundColor(0xff334455);
                }
            }
        });

        mMap.m_nObj = -1;   // 默认，没有选择物件（XSB元素）

        bt_Color = (Button) findViewById(R.id.bt_color);  //网格线颜色
        bt_Color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.m_nLine_Color++;
                mMap.m_nLine_Color %= 3;
                if (mMap.m_nLine_Color == 1) {            // 网格线为白色
                    bt_Color.setBackgroundColor(0x9fffffff);
                    bt_Color.setTextColor(0xff000000);
                    bt_Color.setText("白");
                } else if (mMap.m_nLine_Color == 2) {     // 网格线为黑色
                    bt_Color.setTextColor(0xffffffff);
                    bt_Color.setBackgroundColor(0x9f000000);
                    bt_Color.setText("黑");
                } else {                                  // 默认，网格线为紫色
                    bt_Color.setBackgroundColor(0x9fff00ff);
                    bt_Color.setTextColor(0xffffffff);
                    bt_Color.setText("紫");
                }
                if (myMenu != null) {
                    if (mMap.m_nLine_Color == 2) {
                        myMenu.getItem(3).setTitle("-.5");
                        myMenu.getItem(4).setTitle("+.5");
                    } else if (mMap.m_nLine_Color == 1) {
                        myMenu.getItem(3).setTitle("-3");
                        myMenu.getItem(4).setTitle("+3");
                    } else {
                        myMenu.getItem(3).setTitle("-1");
                        myMenu.getItem(4).setTitle("+1");
                    }
                }
                mMap.invalidate();
            }
        });
        mMap.m_nLine_Color = 0;

        bt_Left = (Button) findViewById(R.id.bt_left);  //网格线左移
        bt_Left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nMapLeft > 0) {
                    mMap.m_nMapLeft--;
                    if (mMap.cur_Rect.top >= 0) {  //焦点框
                        mMap.cur_Rect.left--;
                        mMap.cur_Rect.right--;
                    }
                    mMap.invalidate();
                }
            }
        });

        bt_Right = (Button) findViewById(R.id.bt_right);  //网格线右移
        bt_Right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nMapLeft < mMap.m_nPicWidth - mMap.m_nWidth) {
                    mMap.m_nMapLeft++;
                    if (mMap.cur_Rect.top >= 0) {  //焦点框
                        mMap.cur_Rect.left++;
                        mMap.cur_Rect.right++;
                    }
                    mMap.invalidate();
                }
            }
        });

        bt_Up = (Button) findViewById(R.id.bt_up);  //网格线上移
        bt_Up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nMapTop > 0) {
                    mMap.m_nMapTop--;
                    if (mMap.cur_Rect.top >= 0) {  //焦点框
                        mMap.cur_Rect.top--;
                        mMap.cur_Rect.bottom--;
                    }
                    mMap.invalidate();
                }
            }
        });

        bt_Down = (Button) findViewById(R.id.bt_down);  //网格线下移
        bt_Down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.m_nMapTop < mMap.m_nPicHeight - mMap.m_nWidth) {
                    mMap.m_nMapTop++;
                    if (mMap.cur_Rect.top >= 0) {  //焦点框
                        mMap.cur_Rect.top++;
                        mMap.cur_Rect.bottom++;
                    }
                    mMap.invalidate();
                }
            }
        });

        Builder dlg0 = new Builder(this);
        dlg0.setTitle("提醒").setMessage("有识别内容，应用吗？").setCancelable(false).setNegativeButton("取消", null)
                .setPositiveButton("送入编辑", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //若关卡标题不空，则以关卡标题为本，命名新关卡标题，否则，以关卡集名及其序号为本命名关卡标题（此时，关卡标题与关卡文档名相同）
                        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");//设置日期格式

                        //为关卡生成关卡文档名称（含有原关卡集及其关卡序号信息）
                        String newTitle = "Recog_"+df.format(new Date());  // new Date()获取当前系统时间
                        String level = getXSB();

                        myMaps.curMap = new mapNode(m_nRows, m_nCols, level.split("\n"), newTitle, "", "");

                        //取得当前关卡文档名
                        myMaps.sFile = "创编关卡";
                        myMaps.curMap.fileName = newTitle + ".XSB";
                        myMaps.curMapNum = -4;  //识别关卡编辑
                        Intent intent2 = new Intent();
                        intent2.setClass(myRecogView.this, myEditView.class);
                        startActivity(intent2);
                    }
                })
                .setNeutralButton("丢弃", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                });
        exitDlg = dlg0.create();

        m_nBoxNum = 0;
        DstNum = 0;
    }

    // 识别
    public void doAction() {
        if (isInAction || mMap.cur_Rect.top < 0) return;
        isInAction = true;
        dialog = new ProgressDialog(myRecogView.this);
        dialog.setMessage("分析中...\n" + "最大允许色差: " + mMap.toleranceValueColor + "\n最多误差像素: " + mMap.toleranceValueDifferentColor);
        dialog.setCancelable(false);
        new Thread(new MyThread()).start();
        dialog.show();
    }

    //识别进度条
    final char[] myXSB = { '#', '$', '*', '.', '-' };
    private Handler handler = new Handler() {
        // 在Handler中获取消息，重写handleMessage()方法
        @Override
        public void handleMessage(Message msg) {
            if (dialog != null) dialog.dismiss();

            if (msg.what == 1) {  // 识别成功
                myBackup();
                for(Point p : mMap.curPoints) {
                    setXSB(Math.round((p.x-mMap.m_nMapLeft)/mMap.m_nWidth), Math.round((p.y-mMap.m_nMapTop)/mMap.m_nWidth), myXSB[isActNum]);
                }
                myCount();
                mMap.invalidate();
            }
            isInAction = false;
        }
    };


    // 还原到上一次识别出的地图
    public void myRestore() {
        char ch;
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxRow; j++) {
                ch = m_cArray[i][j];
                m_cArray[i][j] = bk_cArray[i][j];
                bk_cArray[i][j] = ch;
            }
        }
    }

    // 备份一次识别出的地图，以备撤销使用（只允许撤销一次）
    public void myBackup() {
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxRow; j++) {
                bk_cArray[i][j] = m_cArray[i][j];
            }
        }
    }

    // 计数箱子和目标点
    public void myCount() {
        m_nBoxNum = 0;
        DstNum = 0;
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxRow; j++) {
                if (m_cArray[i][j] == '$') {
                    m_nBoxNum++;
                } else if (m_cArray[i][j] == '*') {
                    m_nBoxNum++;
                    DstNum++;
                } else if (m_cArray[i][j] == '.' || m_cArray[i][j] == '+') {
                    DstNum++;
                }
            }
        }
    }

    //将识别出来的XSB转换为字符串
    private String getXSB() {

        StringBuilder str = new StringBuilder();
        m_nRows = 0;
        m_nCols = 0;

        // 关卡最小化
        int mTop = 0, mLeft = 0, mBottom = myMaps.m_nMaxRow-1, mRight = myMaps.m_nMaxCol-1;
        for (int k = 0, t; k < myMaps.m_nMaxRow; k++) {
            t = 0;
            while (t < myMaps.m_nMaxCol && m_cArray[k][t] == '-') t++;
            if (t == myMaps.m_nMaxCol) mTop++;
            else break;
        }
        for (int k = myMaps.m_nMaxRow-1, t; k > mTop; k--) {
            t = 0;
            while (t < myMaps.m_nMaxCol && m_cArray[k][t] == '-') t++;
            if (t == myMaps.m_nMaxCol) mBottom--;
            else break;
        }
        if (mBottom - mTop < 2) return "";

        for (int k = 0, t; k < myMaps.m_nMaxCol; k++) {
            t = mTop;
            while (t <= mBottom && m_cArray[t][k] == '-') t++;
            if (t > mBottom) mLeft++;
            else break;
        }
        for (int k = myMaps.m_nMaxCol-1, t; k > mLeft; k--) {
            t = mTop;
            while (t <= mBottom && m_cArray[t][k] == '-') t++;
            if (t > mBottom) mRight--;
            else break;
        }
        if (mRight - mLeft < 2) return "";

        for (int i = mTop; i <= mBottom; i++) {
            for (int j = mLeft; j <= mRight; j++) {
                str.append(m_cArray[i][j]);
            }
            str.append('\n');
        }

        m_nRows = mBottom - mTop  + 1;
        m_nCols = mRight  - mLeft + 1;
        return str.toString();
    }

    //手动编辑XSB
    private void setXSB(int x, int y, char ch) {
        if (x < 0 || y < 0 || x >= myMaps.m_nMaxCol || y >= myMaps.m_nMaxRow) {
            return;
        }
        m_cArray[y][x] = ch;
    }

    //清除所选的XSB元素
    private void removeXSB(char ch, boolean isAll) {
        myBackup();
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxRow; j++) {
                if (ch == '.') {
                    if (m_cArray[i][j] == '.') m_cArray[i][j] = '-';
                    //考虑是否“所有目标点”选项
                    if (isAll) {
                        if (m_cArray[i][j] == '*') {
                            m_cArray[i][j] = '$';
                        } else if (m_cArray[i][j] == '+') {
                            m_cArray[i][j] = '@';
                        }
                    }
                } else  if (ch == '$') {
                    if (m_cArray[i][j] == '$') m_cArray[i][j] = '-';
                    //考虑是否“所有箱子”选项
                    if (isAll && m_cArray[i][j] == '*') m_cArray[i][j] = '.';
                } else  if (ch == '*') {
                    //考虑是否“保留目标点”选项
                    if (isAll) {
                        if (m_cArray[i][j] == '*') m_cArray[i][j] = '-';
                    } else {
                        if (m_cArray[i][j] == '*') m_cArray[i][j] = '.';
                    }
                } else {
                    if (m_cArray[i][j] == ch) m_cArray[i][j] = '-';
                }
            }
        }
    }

    //清除所有的元素XSB
    private void clearXSB() {
        myBackup();
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxRow; j++) {
                if (m_cArray[i][j] != '-') {
                    m_cArray[i][j] = '-';
                }
            }
        }
        isInAction = false;
    }

    //恢复所有元素按钮为未选状态的颜色
    private void setColor() {
        bt_Floor.setBackgroundColor(0xff334455);
        bt_Wall.setBackgroundColor(0xff334455);
        bt_Box.setBackgroundColor(0xff334455);
        bt_BoxGoal.setBackgroundColor(0xff334455);
        bt_Goal.setBackgroundColor(0xff334455);
        bt_Player.setBackgroundColor(0xff334455);
    }

    // 是否有识别的XSB
    private boolean isRecog() {
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxRow; j++) {
                if (m_cArray[i][j] != '-') {
                    return true;
                }
            }
        }
        return false;
    }

    //使用独立进程识别地图
    public class MyThread implements Runnable {
        @Override
        public void run() {
            Message msg = Message.obtain();
            try {
                mMap.curPoints = mMap.findSubimages();
                msg.what = 1;
                handler.sendMessage(msg);
            } catch (Exception e) {
                msg.what = 0;
                handler.sendMessage(msg);
            }
        }
    }

    //地图数组初始化
    private void initMap() {
        // 默认尺寸：100 * 100
        m_cArray = new char[myMaps.m_nMaxRow][myMaps.m_nMaxCol];
        bk_cArray = new char[myMaps.m_nMaxRow][myMaps.m_nMaxCol];
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxCol; j++) {
                m_cArray[i][j] = '-';
                bk_cArray[i][j] = '-';
            }
        }
        mMap.initArena();  //舞台初始化
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recog, menu);
        myMenu = menu;
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem mt) {
        final NumberKeyListener getNumber = new NumberKeyListener() {
            @Override
            protected char[] getAcceptedChars() {
                return new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
            }

            @Override
            public int getInputType() {
                return InputType.TYPE_CLASS_PHONE;
            }
        };
        switch (mt.getItemId()) {
            //菜单栏返回键功能
            case android.R.id.home:
                if (!isRecog()) finish();
                else exitDlg.show();  //有过识别动作，提示保存
                return true;
            case R.id.recog_about:  // 关于
                Intent intent1 = new Intent();
                intent1.setClass(this, myAboutRecgo.class);
                startActivity(intent1);
                return true;
            case R.id.recog_cut_left_top:  // 设置为左上角: m_nMapTop, m_nMapLeft
                mMap.cur_Rect.top = -1;   // 取消点击的格子
                mMap.m_nMapLeft = (int) (Math.abs(mMap.m_fLeft) / mMap.m_fScale);
                mMap.m_nMapTop  = (int) (Math.abs(mMap.m_fTop)  / mMap.m_fScale);
                mMap.invalidate();
                return true;
            case R.id.recog_shrink:  // 格子变小
                mMap.cur_Rect.top = -1;   // 取消点击的格子
                if (mMap.m_nLine_Color == 2) {
                    mMap.m_nWidth -= 0.5;
                } else if (mMap.m_nLine_Color == 1) {
                    mMap.m_nWidth -= 3.0;
                } else {
                    mMap.m_nWidth -= 1.0;
                }
                if (mMap.m_nWidth < 10) mMap.m_nWidth = 10;

                mMap.setPR();
                mMap.invalidate();
                return true;
            case R.id.recog_extend:  // 格子变大
                mMap.cur_Rect.top = -1;   // 取消点击的格子
                if (mMap.m_nLine_Color == 2) {
                    mMap.m_nWidth += 0.5;
                } else if (mMap.m_nLine_Color == 1) {
                    mMap.m_nWidth += 3.0;
                } else {
                    mMap.m_nWidth += 1.0;
                }

                if (mMap.m_nWidth > 200) mMap.m_nWidth = 200;

                mMap.setPR();
                mMap.invalidate();
                return true;
            case R.id.recog_restore:  // 取消本次识别
                myRestore();
                myCount();
                mMap.invalidate();
                return true;
            case R.id.recog_complete:  // 开始识别
                if (mMap.cur_Rect.top < 0) {
                    MyToast.showToast(this, "请给出要识别的位置！", Toast.LENGTH_LONG);
                    return true;
                }
                String[] m_menu = {
                        "墙壁",
                        "箱子",
                        "箱子在目标点",
                        "目标点",
                        "地板"
                };
                isActNum = -1;
                View view3 = View.inflate(this, R.layout.recog_dialog, null);
                final EditText input1 = (EditText) view3.findViewById(R.id.recog_toleranceValueColor);  //色差
                final EditText input2 = (EditText) view3.findViewById(R.id.recog_toleranceValueDifferentColor);  //误差点数
                Builder dlg2 = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                dlg2.setView(view3).setCancelable(false);

                input1.setKeyListener(getNumber);
                input2.setKeyListener(getNumber);

                final int mm = mMap.toleranceValueColor;
                final int nn = mMap.toleranceValueDifferentColor;

                input1.setText(""+mm);
                input2.setText(""+nn);

                dlg2.setTitle("识别选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int m, n;
                        try {
                            m = Integer.valueOf(input1.getText().toString().trim());
                            n = Integer.valueOf(input2.getText().toString().trim());
                            if (m >= 0 && m <= 600) {
                                mMap.toleranceValueColor = m;
                            }
                            if (n >= 0 && n <= 60) {
                                mMap.toleranceValueDifferentColor = n;
                            }
                            if (isActNum >= 0) doAction();
                        } catch (Throwable ex) { }
                    }
                }).setSingleChoiceItems(m_menu, isActNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isActNum = which;
                    }
                }).setCancelable(false).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(mt);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            return true;

        return super.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (!isRecog()) finish();
            else exitDlg.show();  //有过识别动作，提示保存

            return true;
        }
        return super.onKeyDown(keyCode, event);
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
}