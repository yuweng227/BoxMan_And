package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.text.SimpleDateFormat;
import java.util.Date;

public class myRecogView extends Activity {

    MyHandler myTimer;

    AlertDialog exitDlg;

    Menu myMenu = null;

    myRecogViewMap mMap;

    char[][] m_cArray, bk_cArray;  //地图，备份地图m_nRows, m_nCols,
    int m_nBoxNum, DstNum;

    int actNum = 0;              // 定时器功能选择
    int selNum = -1;             // 当前的选择条目
    boolean isInAction = false;  // 是否正在识别

    Button bt_Floor;
    Button bt_Wall;
    Button bt_Box;
    Button bt_BoxGoal;
    Button bt_Goal;
    Button bt_Player;
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

        myTimer = new MyHandler();

        initMap();

        bt_Floor = (Button) findViewById(R.id.bt_floor);  //地板
        bt_Floor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(0);
            }
        });
        bt_Floor.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.m_nObj = -1;
                setColor(0);
                String[] m_menu = {
                        "清空地图"
                };
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearXSB();
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, 0, null).setCancelable(false).create().show();
                return true;
            }
        });

        bt_Wall = (Button) findViewById(R.id.bt_wall);  //墙壁
        bt_Wall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(1);
            }
        });
        bt_Wall.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.m_nObj = -1;
                setColor(1);
                String[] m_menu = {
                        "清理墙壁"
                };
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('#', true);
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, 0, null).setCancelable(false).create().show();
                return true;
            }
        });

        bt_Box = (Button) findViewById(R.id.bt_box);  //箱子
        bt_Box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(2);
            }
        });
        bt_Box.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.m_nObj = -1;
                setColor(2);
                String[] m_menu = {
                        "清理纯箱子",
                        "清理所有箱子"
                };
                selNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('$', selNum == 1);
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, selNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selNum = which;
                    }
                }).setCancelable(false).create().show();
                return true;
            }
        });

        bt_BoxGoal = (Button) findViewById(R.id.bt_boxgoal);  //目标点上的箱子
        bt_BoxGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(3);
            }
        });
        bt_BoxGoal.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.m_nObj = -1;
                setColor(3);
                String[] m_menu = {
                        "清理目标点箱子",
                        "保留目标点清理箱子"
                };
                selNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('*', selNum == 0);
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, selNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selNum = which;
                    }
                }).setCancelable(false).create().show();
                return true;
            }
        });

        bt_Goal = (Button) findViewById(R.id.bt_goal);  //目标点
        bt_Goal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(4);
            }
        });
        bt_Goal.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.m_nObj = -1;
                setColor(4);
                String[] m_menu = {
                        "清理纯目标点",
                        "清理所有目标点"
                };
                selNum = 0;
                Builder dlg = new Builder(myRecogView.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setCancelable(false);

                dlg.setTitle("选项").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeXSB('.', selNum == 1);
                        mMap.invalidate();
                    }
                }).setSingleChoiceItems(m_menu, selNum, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selNum = which;
                    }
                }).setCancelable(false).create().show();
                return true;
            }
        });

        bt_Player = (Button) findViewById(R.id.bt_player);  //仓管员
        bt_Player.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(5);
            }
        });

        mMap.m_nObj = -1;   // 默认，没有选择物件（XSB元素）

        mMap.isLeftTop = true;  // 默认：左、上边的指示灯点亮

        bt_Left = (Button) findViewById(R.id.bt_left);  //网格线左移
        bt_Left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actNum > 0) {    // 取消连续调整
                    actNum = 0;      // 取消定时器功能
                    return;
                }
                mMap.cur_Rect.top = -1;   // 取消焦点格子
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapLeft > 1) {
                        mMap.m_nMapLeft--;
                        mMap.invalidate();
                    }
                } else {
                    if (mMap.m_nMapRight-50 > mMap.m_nMapLeft) {
                        mMap.m_nMapRight--;
                        mMap.invalidate();
                    }
                }
            }
        });
        bt_Left.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.cur_Rect.top = -1;  // 取消焦点框
                actNum = 1;              // 定时器功能选择 -- 连续左移
                UpData(1);            // 启动定时器
                return false;
            }
        });

        bt_Right = (Button) findViewById(R.id.bt_right);  //网格线右移
        bt_Right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actNum > 0) {    // 取消连续调整
                    actNum = 0;      // 取消定时器功能
                    return;
                }
                mMap.cur_Rect.top = -1;   // 取消焦点格子
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapLeft+50 < mMap.m_nMapRight) {
                        mMap.m_nMapLeft++;
                        mMap.invalidate();
                    }
                } else {
                    if (mMap.m_nMapRight+1 < mMap.m_nPicWidth-1) {
                        mMap.m_nMapRight++;
                        mMap.invalidate();
                    }
                }
            }
        });
        bt_Right.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.cur_Rect.top = -1;  // 取消焦点框
                actNum = 2;              // 定时器功能选择 -- 连续右移
                UpData(1);            // 启动定时器
                return false;
            }
        });

        bt_Up = (Button) findViewById(R.id.bt_up);  //网格线上移
        bt_Up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actNum > 0) {    // 取消连续调整
                    actNum = 0;      // 取消定时器功能
                    return;
                }
                mMap.cur_Rect.top = -1;   // 取消焦点格子
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapTop > 1) {
                        mMap.m_nMapTop--;
                        mMap.invalidate();
                    }
                } else {
                    if (mMap.m_nMapBottom-50 > mMap.m_nMapTop) {
                        mMap.m_nMapBottom--;
                        mMap.invalidate();
                    }
                }
            }
        });
        bt_Up.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.cur_Rect.top = -1;  // 取消焦点框
                actNum = 3;              // 定时器功能选择 -- 连续上移
                UpData(1);            // 启动定时器
                return false;
            }
        });

        bt_Down = (Button) findViewById(R.id.bt_down);  //网格线下移
        bt_Down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actNum > 0) {    // 取消连续调整
                    actNum = 0;      // 取消定时器功能
                    return;
                }
                mMap.cur_Rect.top = -1;   // 取消焦点格子
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapTop+50 < mMap.m_nMapBottom) {
                        mMap.m_nMapTop++;
                        mMap.invalidate();
                    }
                } else {
                    if (mMap.m_nMapBottom+1 < mMap.m_nPicHeight-1) {
                        mMap.m_nMapBottom++;
                        mMap.invalidate();
                    }
                }
            }
        });
        bt_Down.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mMap.cur_Rect.top = -1;  // 取消焦点框
                actNum = 4;              // 定时器功能选择 -- 连续下移
                UpData(1);            // 启动定时器
                return false;
            }
        });

        Builder dlg0 = new Builder(this);
        dlg0.setTitle("请选择:").setCancelable(false).setNegativeButton("取消", null)
                .setPositiveButton("进入编辑", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //若关卡标题不空，则以关卡标题为本，命名新关卡标题，否则，以关卡集名及其序号为本命名关卡标题（此时，关卡标题与关卡文档名相同）
                        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");//设置日期格式

                        //为关卡生成关卡文档名称（含有原关卡集及其关卡序号信息）
                        final String newTitle = "Recog_"+df.format(new Date());  // new Date()获取当前系统时间
                        final String level = getXSB();

                        if (level == null) {
                            myMaps.curMap.Map = null;
                        } else {
                            myMaps.curMap = new mapNode(mMap.m_nRows, mMap.m_nCols, level.split("\n"), newTitle, "", "");
                        }
                        //取得当前关卡文档名
                        myMaps.sFile = "创编关卡";
                        myMaps.curMap.fileName = newTitle + ".XSB";
                        if (mMap.m_nRows < 3 || mMap.m_nCols < 3) {
                            myMaps.curMapNum = -5;  //识别关卡编辑
                        } else {
                            myMaps.curMapNum = -4;  //识别关卡编辑
                        }
                        // 计算截图尺寸
                        myMaps.edPictLeft = mMap.m_nMapLeft;
                        myMaps.edPictTop  = mMap.m_nMapTop;
                        myMaps.edPictRight = mMap.m_nMapRight;
                        myMaps.edPictBottom = (int) (mMap.m_nMapTop + mMap.m_nRows * mMap.m_nWidth);
                        myMaps.edRows = mMap.m_nRows;
                        myMaps.edCols = mMap.m_nCols;
                        Intent intent2 = new Intent();
                        intent2.setClass(myRecogView.this, myEditView.class);
                        startActivity(intent2);
                    }
                })
                .setNeutralButton("退出", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                });
        exitDlg = dlg0.create();

        m_nBoxNum = 0;
        DstNum = 0;
    }

    // 统一定时器处理
    public void UpData(int p) {
        if (p != 1) {
            return;
        }

        switch (actNum) {
            case 1:                       // 长按“左”，微调左右边线
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapLeft > 3) {
                        mMap.m_nMapLeft -= 3;
                    } else {
                        mMap.m_nMapLeft = 0;
                    }
                } else {
                    if (mMap.m_nMapRight-100 > mMap.m_nMapLeft) {
                        mMap.m_nMapRight -= 3;
                    }
                }
                mMap.invalidate();
                myTimer.sleep(20);
                break;
            case 2:                       // 长按“右”，微调左右边线
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapLeft+100 < mMap.m_nMapRight) {
                        mMap.m_nMapLeft += 3;
                    }
                } else {
                    mMap.m_nMapRight += 3;
                    if (mMap.m_nMapRight > mMap.m_nPicWidth-1) {
                        mMap.m_nMapRight = mMap.m_nPicWidth-1;
                    }
                }
                mMap.invalidate();
                myTimer.sleep(20);
                break;
            case 3:                       // 长按“上”，微调上下边线
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapTop > 3) {
                        mMap.m_nMapTop -= 3;
                    } else {
                        mMap.m_nMapTop = 0;
                    }
                } else {
                    if (mMap.m_nMapBottom-100 > mMap.m_nMapTop) {
                        mMap.m_nMapBottom -= 3;
                    }
                }
                mMap.invalidate();
                myTimer.sleep(20);
                break;
            case 4:                       // 长按“下”，微调上下边线
                if (mMap.isLeftTop) {
                    if (mMap.m_nMapTop+100 < mMap.m_nMapBottom) {
                        mMap.m_nMapTop += 3;
                    }
                } else {
                    mMap.m_nMapBottom += 3;
                    if (mMap.m_nMapBottom > mMap.m_nPicHeight-1) {
                        mMap.m_nMapBottom = mMap.m_nPicHeight-1;
                    }
                }
                mMap.invalidate();
                myTimer.sleep(20);
                break;
            case 5:                       // 长按指示灯时，闪烁
                mMap.isLamp = !mMap.isLamp;
                mMap.invalidate();
                myTimer.sleep(500);
            break;
        }
    }

    // 自动识别
    public void doAction() {
        if (isInAction || mMap.cur_Rect.top < 0) return;

        isInAction = true;     // 识别中标记

        mMap.curPoints = mMap.findSubimages();

        // 应用识别结果
        for(int k = 0; k < mMap.curPoints.size(); k++) {
            setXSB(mMap.curPoints.get(k) & 0xffff, mMap.curPoints.get(k) >>> 16, mMap.myXSB[mMap.m_nObj]);
        }

        isInAction = false;     // 识别结束

        mMap.invalidate();
    }

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
        for (int i = 0; i < mMap.m_nRows; i++) {
            for (int j = 0; j < mMap.m_nCols; j++) {
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

        if (mMap.m_nRows < 3 || mMap.m_nCols < 3) {
            return null;
        }

        for (int i = 0; i <= mMap.m_nRows; i++) {
            for (int j = 0; j <= mMap.m_nCols; j++) {
                str.append(m_cArray[i][j]);
            }
            str.append('\n');
        }

        return str.toString();
    }

    //置XSB
    private void setXSB(int r, int c, char ch) {
        if (c < 0 || r < 0 || c >= myMaps.m_nMaxCol || r >= myMaps.m_nMaxRow) {
            return;
        }
        m_cArray[r][c] = ch;
    }

    //清除特定的XSB元素
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

    //清除所有的XSB元素
    private void clearXSB() {
        myBackup();
        for (int i = 0; i < myMaps.m_nMaxRow; i++) {
            for (int j = 0; j < myMaps.m_nMaxRow; j++) {
                if (m_cArray[i][j] != '-') {
                    m_cArray[i][j] = '-';
                }
            }
        }
    }

    //元素按钮的状态颜色
    public void setColor(int n) {
        bt_Floor.setBackgroundColor(0xff334455);
        bt_Wall.setBackgroundColor(0xff334455);
        bt_Box.setBackgroundColor(0xff334455);
        bt_BoxGoal.setBackgroundColor(0xff334455);
        bt_Goal.setBackgroundColor(0xff334455);
        bt_Player.setBackgroundColor(0xff334455);

        int m_Color;
        if (mMap.isRecog) {  // 识别模式的高亮颜色
            m_Color = 0x9f0000ff;
        } else {             // 编辑模式的高亮颜色
            m_Color = 0x9fff3300;
        }

        if (mMap.m_nObj == n) {
            mMap.m_nObj = -1;
        } else {
            mMap.m_nObj = n;
            switch (n) {
                case 0:
                    bt_Floor.setBackgroundColor(m_Color);
                    break;
                case 1:
                    bt_Wall.setBackgroundColor(m_Color);
                    break;
                case 2:
                    bt_Box.setBackgroundColor(m_Color);
                    break;
                case 3:
                    bt_BoxGoal.setBackgroundColor(m_Color);
                    break;
                case 4:
                    bt_Goal.setBackgroundColor(m_Color);
                    break;
                case 5:
                    bt_Player.setBackgroundColor(m_Color);
                    break;
            }
        }
    }

    // 定时器进程
    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            UpData(msg.what);
        }

        public void sleep(int m) {
            removeMessages(0);
            sendMessageDelayed(obtainMessage(1), m);
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
        switch (mt.getItemId()) {
            //菜单栏返回键功能
            case android.R.id.home:
                if (mMap.m_nCols < 3 || mMap.m_nRows < 3) finish();
                else exitDlg.show();  //有过识别动作，提示保存
                return true;
            case R.id.recog_about:  // 关于
                Intent intent0 = new Intent(this, Help.class);
                //用Bundle携带数据
                Bundle bundle0 = new Bundle();
                bundle0.putInt("m_Num", 6);  //传递参数，指示调用者
                intent0.putExtras(bundle0);

                intent0.setClass(this, Help.class);
                startActivity(intent0);

                return true;
            case R.id.recog_shrink:  // 减少格子数
                mMap.cur_Rect.top = -1;  // 取消焦点框
                if (mMap.m_nRows > 3 && mMap.m_nCols > 3) {
                    mMap.m_nCols--;
                    mMap.invalidate();
                    mMap.m_nWidth = (float) (mMap.m_nMapRight - mMap.m_nMapLeft + 1) / mMap.m_nCols;
                    mMap.m_nRows = (int) ((mMap.m_nMapBottom - mMap.m_nMapTop + 1) / mMap.m_nWidth);
                }
                return true;
            case R.id.recog_extend:  // 增加格子数
                mMap.cur_Rect.top = -1;  // 取消焦点框
                mMap.m_nWidth = (float) (mMap.m_nMapRight - mMap.m_nMapLeft + 1) / mMap.m_nCols;
                if (mMap.m_nCols < 100 && (int)mMap.m_nWidth > 8) {
                    mMap.m_nCols++;
                    mMap.invalidate();
                    mMap.m_nWidth = (float) (mMap.m_nMapRight - mMap.m_nMapLeft + 1) / mMap.m_nCols;
                    mMap.m_nRows = (int) ((mMap.m_nMapBottom - mMap.m_nMapTop + 1) / mMap.m_nWidth);
                }
                return true;
            case R.id.recog_restore:  // 取消本次识别
                myRestore();
                mMap.invalidate();
                return true;
            case R.id.recog_similarity:  // 设置相似度
                View view3 = View.inflate(this, R.layout.recog_dialog, null);
                final RadioGroup rg = (RadioGroup) view3.findViewById(R.id.recog_similarity);  //相似度
                final RadioButton rb5 = (RadioButton) view3.findViewById(R.id.rb_5);
                final RadioButton rb6 = (RadioButton) view3.findViewById(R.id.rb_6);
                final RadioButton rb7 = (RadioButton) view3.findViewById(R.id.rb_7);
                final RadioButton rb8 = (RadioButton) view3.findViewById(R.id.rb_8);
                final RadioButton rb9 = (RadioButton) view3.findViewById(R.id.rb_9);
                Builder dlg2 = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                dlg2.setView(view3).setCancelable(false);

                final RadioButton[] my_RBS = {rb5, rb6, rb7, rb8, rb9};
                setBT_Color(my_RBS[mMap.mSimilarity-5], true);

                rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        for (int k = 0; k < 5; k++) {
                            setBT_Color(my_RBS[k], false);
                        }
                        if (checkedId == R.id.rb_5) mMap.mSimilarity = 5;
                        else if (checkedId == R.id.rb_6) mMap.mSimilarity = 6;
                        else if (checkedId == R.id.rb_8) mMap.mSimilarity = 8;
                        else if (checkedId == R.id.rb_9) mMap.mSimilarity = 9;
                        else mMap.mSimilarity = 7;

                        setBT_Color(my_RBS[mMap.mSimilarity-5], true);
                    }
                });
                dlg2.setTitle("识别设置").setPositiveButton("确定", null).setCancelable(false).create().show();
                return true;
            case R.id.recog_complete:  // 开始识别
                setColor(-1);  // 取消底行 XSB 元素高亮
                if (mt.getTitle().equals("识别")) {
                    mt.setTitle("编辑");
                    mMap.isRecog = false;
                } else {
                    mt.setTitle("识别");
                    mMap.isRecog = true;
                }
                 return true;
            default:
                return super.onOptionsItemSelected(mt);
        }
    }

    private void setBT_Color(RadioButton bt, boolean flg) {
        if (flg) {
            bt.setBackgroundColor(0xffffffff);
            bt.setTextColor(0xff000000);
        } else {
            bt.setBackgroundColor(0xff000000);
            bt.setTextColor(0xffffffff);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            return true;

        return super.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (mMap.m_nCols < 3 || mMap.m_nRows < 3) finish();
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