package my.boxman;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class myEditView extends Activity {

    AlertDialog exitDlg;

    myEditViewMap mMap;  //地图

    //底行按钮
    CheckBox bt_UnDo = null;
    CheckBox bt_ReDo = null;
    CheckBox bt_Cut  = null;
    CheckBox bt_Copy = null;
    CheckBox bt_Paste= null;
    CheckBox bt_Tru  = null;
    CheckBox bt_Save = null;
    CheckBox bt_More = null;

    char[][] m_cArray, m_cSelArray;  //迷宫，选择区域
    int selRows, selCols, selRows2, selCols2;
    int mWhich;
    int m_nItemSelect;  //对话框中的出item选择前的记忆

    LinkedList<ActNode> m_UnDoList;  //unDo 栈
    LinkedList<ActNode> m_ReDoList;  //ReDo 栈
    ActNode ndAct;
    mapNode old_Map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.edit_view);

        hideSystemUI();  // 隐藏系统的那三个按钮

        mMap = (myEditViewMap) this.findViewById(R.id.edMapView);
        mMap.Init(this);

        //按计算的地图尺寸，创建关卡图框架  ==  画布
        m_cSelArray = new char[myMaps.m_nMaxRow][myMaps.m_nMaxCol];
        m_cArray = new char[myMaps.m_nMaxRow*2][myMaps.m_nMaxCol*2];
        for (int i = 0; i < myMaps.m_nMaxRow*2; i++)
            for (int j = 0; j < myMaps.m_nMaxCol*2; j++)
                m_cArray[i][j] = '-';

        old_Map = myMaps.curMap;
        myMaps.isSaveBlock = false;
        initMap();

        Builder dlg0 = new Builder(this);
        dlg0.setTitle("提醒").setMessage("有修改未保存，坚持退出吗？").setCancelable(false).setNegativeButton("否", null)
                .setPositiveButton("是", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                });
        exitDlg = dlg0.create();

        bt_UnDo = (CheckBox) findViewById(R.id.bt_UnDo2);  //撤销
        bt_UnDo.setEnabled(false);
        bt_UnDo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!m_UnDoList.isEmpty()) myUnDo();
            }
        });

        bt_ReDo = (CheckBox) findViewById(R.id.bt_ReDo2);  //重做
        bt_ReDo.setEnabled(false);
        bt_ReDo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!m_ReDoList.isEmpty()) myReDo();
            }
        });

        bt_Cut = (CheckBox) findViewById(R.id.bt_Cut);  //剪切
        bt_Cut.setEnabled(false);
        bt_Cut.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //没有选区时
                if (mMap.selNode.row < 0) {
                    MyToast.showToast(myEditView.this, "还没有选择区块！", Toast.LENGTH_LONG);
                    return;
                }
                ndAct = null;
                ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
                ndAct.Act(1);
                //将选择区域拼接成 XSB 字符串，送入系统“剪切板”
                StringBuilder str = new StringBuilder();
                char ch;
                selRows = mMap.selNode2.row - mMap.selNode.row + 1;
                selCols = mMap.selNode2.col - mMap.selNode.col + 1;

                for (int i = 0; i < selRows; i++) {
                    for (int j = 0; j < selCols; j++) {
                        ch = m_cArray[mMap.selNode.row+i+mMap.m_nMapTop][mMap.selNode.col+j+mMap.m_nMapLeft];
                        if (ch == '#' || ch == '.' || ch == '$' || ch == '*' || ch == '@' || ch == '+')
                            str.append(ch);
                        else str.append('-');
                        m_cArray[mMap.selNode.row+i+mMap.m_nMapTop][mMap.selNode.col+j+mMap.m_nMapLeft] = '-';
                    }
                    if (i < selRows-1) str.append('\n');
                }
                myMaps.saveClipper(str.toString());

                m_UnDoList.offer(ndAct);
                bt_UnDo.setEnabled(true);
                m_ReDoList.clear();
                bt_ReDo.setEnabled(false);
                bt_Save.setEnabled(true);
                bt_Paste.setEnabled(true);
                mMap.invalidate();
            }
        });

        bt_Copy = (CheckBox) findViewById(R.id.bt_Copy);  //复制
        bt_Copy.setEnabled(false);
        bt_Copy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //没有选区时
                if (mMap.selNode.row < 0) {
                    MyToast.showToast(myEditView.this, "还没有选择的区块！", Toast.LENGTH_LONG);
                    return;
                }
                //将选择区域拼接成 XSB 字符串，送入系统“剪切板”
                StringBuilder str = new StringBuilder();
                char ch;
                selRows = mMap.selNode2.row - mMap.selNode.row + 1;
                selCols = mMap.selNode2.col - mMap.selNode.col + 1;

                for (int i = 0; i < selRows; i++) {
                    for (int j = 0; j < selCols; j++) {
                        ch = m_cArray[mMap.selNode.row+i+mMap.m_nMapTop][mMap.selNode.col+j+mMap.m_nMapLeft];
                        if (ch == '#' || ch == '.' || ch == '$' || ch == '*' || ch == '@' || ch == '+')
                            str.append(ch);
                        else str.append('-');
                    }
                    if (i < selRows-1) str.append('\n');
                }
                myMaps.saveClipper(str.toString());
                bt_Paste.setEnabled(true);
            }
        });

        bt_Paste = (CheckBox) findViewById(R.id.bt_Paste);  //粘贴
        bt_Paste.setEnabled(!myMaps.loadClipper().equals(""));  //剪切板中有内容
        bt_Paste.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMap.selNode.row < 0)
                    MyToast.showToast(myEditView.this, "请先给出粘贴位置！", Toast.LENGTH_SHORT);
                else{
                    //解析剪切板中的 XSB，通过 m_cSelArray[][]，粘贴的工作空间
                    String str = myMaps.loadClipper();
                    if (str.equals("")) {
                        MyToast.showToast(myEditView.this, "剪切板中没有找到 XSB！", Toast.LENGTH_SHORT);
                    } else {
                        //从剪切板中解析出 XSB，送入 m_cSelArray[][]，并设置区域尺寸：selRows2、selCols2
                        if (cutXSB(str)) {
                            //先分析是否需要扩展关卡尺寸
                            resetSize();

                            boolean flg = false;  //粘贴区域是否已经绘制
                            for (int i = 0; i < selRows2; i++) {
                                for (int j = 0; j < selCols2; j++) {
                                    if (m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '#' ||
                                            m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '$' ||
                                            m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '*' ||
                                            m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '@' ||
                                            m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '+' ||
                                            m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '.') {
                                        flg = true;
                                        break;
                                    }
                                }
                            }
                            if (flg) {//若粘贴区域已有绘制，则进行覆盖提醒
                                AlertDialog.Builder dlg = new Builder(myEditView.this, AlertDialog.THEME_HOLO_DARK);
                                dlg.setTitle("提醒").setMessage("原有绘制将被覆盖，确定吗？").setNegativeButton("取消", null);
                                dlg.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        selRows = selRows2;
                                        selCols = selCols2;
                                        mMap.selNode2.row = mMap.selNode.row + selRows - 1;
                                        mMap.selNode2.col = mMap.selNode.col + selCols - 1;
                                        mMap.isFistClick = true;

                                        ndAct = null;
                                        ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
                                        ndAct.Act(2);

                                        for (int i = 0; i < selRows2; i++) {  //覆盖粘贴
                                            for (int j = 0; j < selCols2; j++) {
                                                //仓管员只能留存 1 位，若粘贴内容为仓管员，则先进行处理
                                                if (m_cSelArray[i][j] == '@' || m_cSelArray[i][j] == '+') {
                                                    for (int r = mMap.m_nMapTop; r <= mMap.m_nMapBottom; r++) {
                                                        for (int c = mMap.m_nMapLeft; c <= mMap.m_nMapRight; c++) {
                                                            if (m_cArray[r][c] == '+')
                                                                m_cArray[r][c] = '.';
                                                            else if (m_cArray[r][c] == '@')
                                                                m_cArray[r][c] = '-';
                                                        }
                                                    }
                                                }
                                                m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] = m_cSelArray[i][j];
                                            }
                                        }

                                        m_UnDoList.offer(ndAct);
                                        bt_UnDo.setEnabled(true);
                                        m_ReDoList.clear();
                                        bt_ReDo.setEnabled(false);
                                        bt_Save.setEnabled(true);
                                        mMap.invalidate();
                                    }
                                });
                                dlg.setCancelable(false).show();
                            } else {
                                selRows = selRows2;
                                selCols = selCols2;
                                mMap.selNode2.row = mMap.selNode.row + selRows - 1;
                                mMap.selNode2.col = mMap.selNode.col + selCols - 1;
                                mMap.isFistClick = true;

                                ndAct = null;
                                ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
                                ndAct.Act(2);

                                for (int i = 0; i < selRows2; i++) {  //覆盖粘贴
                                    for (int j = 0; j < selCols2; j++) {
                                        //仓管员只能留存 1 位，若粘贴内容为仓管员，则先进行处理
                                        if (m_cSelArray[i][j] == '@' || m_cSelArray[i][j] == '+') {
                                            for (int r = mMap.m_nMapTop; r <= mMap.m_nMapBottom; r++) {
                                                for (int c = mMap.m_nMapLeft; c <= mMap.m_nMapRight; c++) {
                                                    if (m_cArray[r][c] == '+')
                                                        m_cArray[r][c] = '.';
                                                    else if (m_cArray[r][c] == '@')
                                                        m_cArray[r][c] = '-';
                                                }
                                            }
                                        }
                                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] = m_cSelArray[i][j];
                                    }
                                }

                                m_UnDoList.offer(ndAct);
                                bt_UnDo.setEnabled(true);
                                m_ReDoList.clear();
                                bt_ReDo.setEnabled(false);
                                bt_Save.setEnabled(true);
                                mMap.invalidate();
                            }
                        } else MyToast.showToast(myEditView.this, "剪切板中无法解析出 XSB！", Toast.LENGTH_SHORT);
                    }
                }
            }});

        bt_Tru = (CheckBox) findViewById(R.id.bt_tru2);  //变换
        bt_Tru.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMap.selNode.row < 0) mMap.mySelectAll();  //默认选择全部

                String[] m_menu = {
                        "180度",
                        "90度(顺时针)",
                        "90度(逆时针)",
                        "水平翻转",
                        "垂直翻转"
                };

                if (mWhich < 0) mWhich = 1;  //默认"顺90度旋转",

                AlertDialog.Builder builder2 = new Builder(myEditView.this, AlertDialog.THEME_HOLO_DARK);
                builder2.setTitle("变换").setSingleChoiceItems(m_menu, mWhich, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mWhich = which;
                    }
                }).setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        myRotate(mWhich);
                    }
                });
                builder2.setCancelable(false).show();
            }
        });

        bt_Save = (CheckBox) findViewById(R.id.bt_save);  //保存文档到“创建关卡”文件夹下
        bt_Save.setEnabled(false);
        bt_Save.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (saveFile(myMaps.curMap.fileName)) {
                    MyToast.showToast(myEditView.this, "关卡已保存！", Toast.LENGTH_LONG);
                    bt_Save.setEnabled(false);
                } else
                    MyToast.showToast(myEditView.this, "出错了，保存失败！", Toast.LENGTH_SHORT);
            }
        });

        bt_More = (CheckBox) findViewById(R.id.bt_More);  //更多
        bt_More.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                openOptionsMenu();  //菜单
            }
        });
        bt_More.setLongClickable(true);
        bt_More.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MyToast.showToast(myEditView.this, "离开！", Toast.LENGTH_SHORT);
                if (!bt_Save.isEnabled()) finish();
                else exitDlg.show();  //做过编辑修改，提示保存

                return true;
            }
        });

        mWhich = -1;
        IntentFilter filter = new IntentFilter(myAbout2.action);
        registerReceiver(broadcastReceiver, filter);
    }

    //载入关卡数据
    private void LoadLevel() {

        boolean flg = true;

        try {
            if (mMap.m_nMapBottom-mMap.m_nMapTop < 2 || mMap.m_nMapRight-mMap.m_nMapLeft < 2) throw new Exception();

            String[] Arr = null;

            if (myMaps.curMap.Map != null) {
                Arr = myMaps.curMap.Map.split("\r\n|\n\r|\n|\r|\\|");
            }

            for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
                for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                    if (Arr != null && isOK(Arr[i-mMap.m_nMapTop].charAt(j-mMap.m_nMapLeft))) m_cArray[i][j] = Arr[i-mMap.m_nMapTop].charAt(j-mMap.m_nMapLeft);
                    else m_cArray[i][j] = '-';
                }
            }
            flg = false;
        } catch (ArrayIndexOutOfBoundsException ex) {
        } catch (Exception e) { }

        if (flg) {  // 创建新的空关卡
            myMaps.curMap.Map = null;
            myMaps.curMap.Rows = 15;
            myMaps.curMap.Cols = 10;
            mMap.m_nMapBottom = mMap.m_nMapTop + myMaps.curMap.Rows - 1;
            mMap.m_nMapRight  = mMap.m_nMapLeft + myMaps.curMap.Cols - 1;
            for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
                for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                    m_cArray[i][j] = '-';
                }
            }
        }
    }

    //调用之前，myMaps.curMap 的 Rows、Cols、Map 需有定义， 且 Map 中要么为 null，要么为“带过来”的关卡 XSB
    private void initMap() {
        //关卡图在画布上的四至之初始化
        mMap.m_nMapLeft   = myMaps.m_nMaxCol;
        mMap.m_nMapTop    = myMaps.m_nMaxRow;
        mMap.m_nMapRight  = mMap.m_nMapLeft + myMaps.curMap.Cols - 1;
        mMap.m_nMapBottom = mMap.m_nMapTop  + myMaps.curMap.Rows - 1;

        LoadLevel();  //载入关卡
        selRows = 0;
        selCols = 0;
        selRows2 = 0;
        selCols2 = 0;

        mMap.initArena();  //舞台初始化

        if (m_UnDoList != null) m_UnDoList.clear();
        if (m_ReDoList != null) m_ReDoList.clear();
        m_UnDoList = new LinkedList<ActNode>();
        m_ReDoList = new LinkedList<ActNode>();
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
        getMenuInflater().inflate(R.menu.edit, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem mt) {
        switch (mt.getItemId()) {
            case R.id.edit_import:  //导入--XSB/Lurd -- 剪切板
                myImport();  //从剪切板导入 XSB 到 myMaps.curMap
                bt_UnDo.setEnabled(false);
                bt_ReDo.setEnabled(false);
                return true;
            case R.id.edit_export:  //导出--XSB--剪切板
                myExport();
                return true;
            case R.id.edit_setup:  //设置
                String[] m_menu = {
                        "YASC绘制习惯",
                        "系统导航键"
                };
                final boolean[] mChk = {
                        myMaps.m_Sets[19] == 1,   //YASC绘制习惯
                        myMaps.m_Sets[16] == 1,  //系统导航键
                };
                Builder builder2 = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                builder2.setTitle("设置").setMultiChoiceItems(m_menu, mChk, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    }
                }).setNegativeButton("取消", null).setPositiveButton("确定", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //YASC绘制习惯
                        if (mChk[0]) myMaps.m_Sets[19] = 1;
                        else myMaps.m_Sets[19] = 0;

                        //系统导航键
                        if (mChk[1]) {
                            myMaps.m_Sets[16] = 1;
                            showSystemUI();
                        } else {
                            myMaps.m_Sets[16] = 0;
                            hideSystemUI();
                        }

                        BoxMan.saveSets();  //保存设置
                    }
                });
                builder2.setCancelable(false).show();

                return true;
            case R.id.edit_block_save:  //区块另存为...
                myBlockSave(m_cArray);
                return true;
            case R.id.edit_complete:  //提交
                if (bt_Save.isEnabled()) {  //试推前，若需保存
                    if (saveFile(myMaps.curMap.fileName)) {
                        MyToast.showToast(myEditView.this, "关卡已保存！", Toast.LENGTH_LONG);
                        bt_Save.setEnabled(false);
                    } else {
                        MyToast.showToast(myEditView.this, "出错了，关卡未保存！", Toast.LENGTH_LONG);
                    }
                }
                final String[] myList = new String[myMaps.mSets3.size()+1];
                for (int k = 0; k < myMaps.mSets3.size(); k++) {
                    myList[k] = myMaps.mSets3.get(k).title;
                }
                myList[myList.length-1] = myMaps.getNewSetName();  // 末尾，自动加上一个新的关卡集

                if (mWhich < 0 || mWhich >= myList.length) mWhich = 0;
                Builder builder = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                builder.setTitle("提交到").setSingleChoiceItems(myList, mWhich, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mWhich = which;
                    }
                }).setPositiveButton("确定", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 如果，选中的是最后一个新建关卡集
                        if (mWhich == myList.length-1) {
                            try {
                                long new_ID = mySQLite.m_SQL.add_T(3, myList[myList.length-1], "", "");
                                //将“新关卡集”加入列表
                                set_Node nd = new set_Node();
                                nd.id = new_ID;
                                nd.title = myList[myList.length-1];
                                myMaps.mSets3.add(nd);
                            } catch (Exception e) {
                                MyToast.showToast(myEditView.this, "新关卡集创建失败: " + myList[myList.length-1], Toast.LENGTH_SHORT);
                                return;
                            }
                        }

                        ndAct = null;
                        ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
                        ndAct.Act(8);
                        //保存到 DB
                        if (mWhich >= 0 && Normalize2(m_cArray)) {
                            mMap.initArena();

                            //Normalize2()中，已经重新定义 myMaps.curMap 的 Rows、Cols、Map
                            mapNode nd = new mapNode(myMaps.curMap.Map, myMaps.curMap.Title, myMaps.curMap.Author, myMaps.curMap.Comment);
                            long lvl_id = mySQLite.m_SQL.add_L(myMaps.mSets3.get(mWhich).id, nd);
                            if (lvl_id > 0) {
                                MyToast.showToast(myEditView.this, "提交成功！", Toast.LENGTH_SHORT);

                                m_UnDoList.offer(ndAct);
                                bt_UnDo.setEnabled(true);
                                m_ReDoList.clear();
                                bt_ReDo.setEnabled(false);
//                                bt_Save.setEnabled(true);
                            } else
                                MyToast.showToast(myEditView.this, "出错了，提交失败！", Toast.LENGTH_SHORT);
                        }
                    }
                }).setNegativeButton("取消", null);
                builder.setCancelable(false).show();
                return true;
            case R.id.edit_play:  //试推
                if (bt_Save.isEnabled()) {  //试推前，若需保存
                    if (saveFile(myMaps.curMap.fileName)) {
                        MyToast.showToast(myEditView.this, "关卡已保存！", Toast.LENGTH_LONG);
                        bt_Save.setEnabled(false);
                    } else {
                        MyToast.showToast(myEditView.this, "出错了，关卡未保存！", Toast.LENGTH_LONG);
                    }
                }
                old_Map = myMaps.curMap;
                //saveFile()中，已经重新定义 myMaps.curMap 的 Rows、Cols、Map
                try {
                    mapNode nd = new mapNode(myMaps.curMap.Map, myMaps.curMap.Title, myMaps.curMap.Author, myMaps.curMap.Comment);
                    if (nd.L_CRC_Num != -1) {
                        myMaps.curMap = nd;
                        myMaps.curMap.fileName = old_Map.fileName;
                        myMaps.curMap.Level_id = -1;
                        Intent intent1 = new Intent();
                        intent1.setClass(this, myGameView.class);
                        startActivity(intent1);
                    } else {
                        MyToast.showToast(this, "关卡尚不规范！", Toast.LENGTH_SHORT);
                    }
                } catch (Exception e) {
                    MyToast.showToast(this, "请检查关卡是否规范！", Toast.LENGTH_SHORT);
                }
                return true;
            case R.id.edit_normalize:  //关卡标准化
                ndAct = null;
                ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
                ndAct.Act(9);

                Normalize(m_cArray);
                mMap.initArena();

                m_UnDoList.offer(ndAct);
                bt_UnDo.setEnabled(true);
                m_ReDoList.clear();
                bt_ReDo.setEnabled(false);
                bt_Save.setEnabled(true);

                return true;
            case R.id.edit_clear:  //清空地图
                Builder dlg0 = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                dlg0.setCancelable(false).setTitle("确认").setMessage("地图将被清空，确定吗？").setNegativeButton("取消", null);
                dlg0.setPositiveButton("确定", new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ndAct = null;
                        ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, null, null);
                        ndAct.Act(6);

                        for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
                            for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                                m_cArray[i][j] = '-';
                            }
                        }

                        mMap.selNode.row = -1;  //避免显示选择区域块
                        mMap.isFistClick = true;  //准备记录第一坐标点
                        m_UnDoList.offer(ndAct);
                        bt_UnDo.setEnabled(true);
                        m_ReDoList.clear();
                        bt_ReDo.setEnabled(false);
                        bt_Save.setEnabled(true);
                        bt_Cut.setEnabled(false);
                        bt_Copy.setEnabled(false);
                        mMap.invalidate();
                    }
                }).create().show();
                return true;
            case R.id.edit_resize:  //改变关卡尺寸
                ndAct = null;
                ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
                ndAct.Act(4);

                View view = View.inflate(myEditView.this, R.layout.size_dialog, null);
                final Spinner input_left = (Spinner) view.findViewById(R.id.dialog_left);  //左
                final Spinner input_right = (Spinner) view.findViewById(R.id.dialog_right);  //右
                final Spinner input_top = (Spinner) view.findViewById(R.id.dialog_top);  //上
                final Spinner input_bottom = (Spinner) view.findViewById(R.id.dialog_bottom);  //下
                final RadioGroup myGroup = (RadioGroup) view.findViewById(R.id.myGroup);
                final RadioButton rbExtend = (RadioButton) view.findViewById(R.id.rbExtend);  //扩充

                List<String> list = new ArrayList<String>();
                for (int i = 0; i <= 30; i++) {
                    list.add(String.valueOf(i));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent)
                    {
                        return setCentered(super.getView(position, convertView, parent));
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent)
                    {
                        return setCentered(super.getDropDownView(position, convertView, parent));
                    }

                    private View setCentered(View view)
                    {
                        TextView textView = (TextView)view.findViewById(android.R.id.text1);
                        textView.setGravity(Gravity.CENTER);  //居中显示条目
                        return view;
                    }
                };
                input_left.setAdapter(adapter);
                input_right.setAdapter(adapter);
                input_top.setAdapter(adapter);
                input_bottom.setAdapter(adapter);

                input_left.setSelection(0, true);
                input_right.setSelection(0, true);
                input_top.setSelection(0, true);
                input_bottom.setSelection(0, true);

                final int[] mySign = {1};
                myGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if(rbExtend.getId()==checkedId){
                            mySign[0] = 1;
                        } else {
                            mySign[0] = -1;
                        }
                    }
                });

                Builder dlg = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                dlg.setView(view).setCancelable(false).setTitle("当前尺寸：" + (mMap.m_nMapRight-mMap.m_nMapLeft+1) + " × " + (mMap.m_nMapBottom-mMap.m_nMapTop+1)).setNegativeButton("取消", null);
                dlg.setPositiveButton("确定", new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int newLeft, newRight, newTop, newBottom;

                        newLeft = mySign[0] * input_left.getSelectedItemPosition();
                        newRight = mySign[0] * input_right.getSelectedItemPosition();
                        newTop = mySign[0] * input_top.getSelectedItemPosition();
                        newBottom = mySign[0] * input_bottom.getSelectedItemPosition();

                        StringBuilder str = new StringBuilder();

                        if (newLeft + newRight + mMap.m_nMapRight-mMap.m_nMapLeft+1 < 3) {
                            str.append("关卡宽度不能小于3\n");
                        }
                        if (newTop + newBottom + mMap.m_nMapBottom-mMap.m_nMapTop+1 < 3) {
                            str.append("关卡高度不能小于3\n");
                        }
                        if (newLeft + newRight + mMap.m_nMapRight-mMap.m_nMapLeft+1 > myMaps.m_nMaxCol) {
                            str.append("关卡超宽（").append(myMaps.m_nMaxCol).append("）\n");
                        }
                        if (newTop + newBottom + mMap.m_nMapBottom-mMap.m_nMapTop+1 > myMaps.m_nMaxRow) {
                            str.append("关卡超高（").append(myMaps.m_nMaxRow).append("）\n");
                        }
                        if (newLeft > mMap.m_nMapLeft-1) {
                            str.append("左侧空间不足（").append(mMap.m_nMapLeft-1).append("）\n");
                        }
                        if (newTop > mMap.m_nMapTop-1) {
                            str.append("顶部空间不足（").append(mMap.m_nMapTop-1).append("）\n");
                        }
                        if (newRight + mMap.m_nMapRight >= myMaps.m_nMaxCol*2-1) {
                            str.append("右侧空间不足（").append(myMaps.m_nMaxCol*2-1 - mMap.m_nMapRight).append("）\n");
                        }
                        if (newBottom + mMap.m_nMapBottom >= myMaps.m_nMaxRow*2-1) {
                            str.append("底部空间不足（").append(myMaps.m_nMaxRow*2-1 - mMap.m_nMapBottom).append("）\n");
                        }

                        if (str.length() > 0) {
                            Builder dlg2 = new Builder(myEditView.this, AlertDialog.THEME_HOLO_DARK);
                            dlg2.setCancelable(true).setTitle("错误").setPositiveButton("确定", null).setMessage(str);
                            dlg2.setCancelable(false).create().show();
                        } else {
                            for (int i=mMap.m_nMapTop-newTop; i<=mMap.m_nMapBottom+newBottom; i++) {
                                for (int j=mMap.m_nMapLeft-newLeft; j<=mMap.m_nMapRight+newRight; j++) {
                                    if (i<mMap.m_nMapTop || i>mMap.m_nMapBottom || j<mMap.m_nMapLeft || j>mMap.m_nMapRight){
                                        m_cArray[i][j] = '-';
                                    }
                                }
                            }
                            mMap.m_nMapLeft -= newLeft;
                            mMap.m_nMapTop  -= newTop;
                            mMap.m_nMapRight  += newRight;
                            mMap.m_nMapBottom += newBottom;

                            mMap.initArena();  //舞台初始化

                            m_UnDoList.offer(ndAct);
                            bt_UnDo.setEnabled(true);
                            m_ReDoList.clear();
                            bt_ReDo.setEnabled(false);
                            bt_Save.setEnabled(true);
                        }
                        dialog.dismiss();
                    }
                }).create().show();

                return true;
            case R.id.edit_help:  //操作说明
                Intent intent0 = new Intent(this, Help.class);
                //用Bundle携带数据
                Bundle bundle0 = new Bundle();
                bundle0.putInt("m_Num", 2);  //传递参数，指示调用者
                intent0.putExtras(bundle0);

                intent0.setClass(this, Help.class);
                startActivity(intent0);

                return true;
            case R.id.edit_inf:  //关卡作者、标题、说明等资料编辑
                if (myMaps.curMap.Map == null) {
                    MyToast.showToast(myEditView.this, "做好关卡保存后才可以哦！", Toast.LENGTH_SHORT);
                } else {
                    old_Map = myMaps.curMap;
                    Intent intent = new Intent();
                    intent.setClass(myEditView.this, myAbout2.class);
                    startActivity(intent);
                }
                return true;
            case R.id.edit_rule:  //设置标尺
                View view2 = View.inflate(this, R.layout.rule_dialog, null);
                final SeekBar input_color = (SeekBar) view2.findViewById(R.id.dialog_rule_color);  //标尺字体颜色 -- 仅灰度变化就够用了
                final EditText et1 = (EditText) view2.findViewById(R.id.dialog_rule_color1);       //颜色示例1
                final EditText et2 = (EditText) view2.findViewById(R.id.dialog_rule_color2);       //颜色示例2
                final int[] mColor = {myMaps.m_Sets[21] & 0x000000ff};
                et1.setTextColor((mColor[0] << 16) | (mColor[0] << 8) | mColor[0] | 0xff000000);
                et2.setTextColor((mColor[0] << 16) | (mColor[0] << 8) | mColor[0] | 0xff000000);

                input_color.setProgress(mColor[0]);
                input_color.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        mColor[0] = progress;
                        et1.setTextColor((mColor[0] << 16) | (mColor[0] << 8) | mColor[0] | 0xff000000);
                        et2.setTextColor((mColor[0] << 16) | (mColor[0] << 8) | mColor[0] | 0xff000000);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });

                final String [] items = {"墙壁","地板","目标","箱子","仓管员"};
                final boolean checkedItems[] = {(myMaps.m_Sets[22] & 1) > 0, (myMaps.m_Sets[22] & 2) > 0, (myMaps.m_Sets[22] & 4) > 0, (myMaps.m_Sets[22] & 8) > 0, (myMaps.m_Sets[22] & 16) > 0};
                Builder dlg2 = new Builder(this, AlertDialog.THEME_HOLO_DARK);
                dlg2.setView(view2).setCancelable(true);
                dlg2.setTitle("显示标尺的元素").setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        switch (which) {
                            case 0:
                                if (isChecked) myMaps.m_Sets[22] |= 1;
                                else myMaps.m_Sets[22] &= 30;
                                break;
                            case 1:
                                if (isChecked) myMaps.m_Sets[22] |= 2;
                                else myMaps.m_Sets[22] &= 29;
                                break;
                            case 2:
                                if (isChecked) myMaps.m_Sets[22] |= 4;
                                else myMaps.m_Sets[22] &= 27;
                                break;
                            case 3:
                                if (isChecked) myMaps.m_Sets[22] |= 8;
                                else myMaps.m_Sets[22] &= 23;
                                break;
                            case 4:
                                if (isChecked) myMaps.m_Sets[22] |= 16;
                                else myMaps.m_Sets[22] &= 15;
                                break;
                        }
                    }
                }).setNegativeButton("取消", null).setPositiveButton("确定", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            myMaps.m_Sets[21] = (mColor[0] << 16) | (mColor[0] << 8) | mColor[0] | 0xff000000;
                            mMap.invalidate();
                            dialog.dismiss();
                        } catch (Exception e) {
                        }
                    }
                }).setCancelable(false).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(mt);
        }
    }

    // 单边界的尺寸调整
    public void My_ReSize(int m_nSide) {
        ndAct = null;
        ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
        ndAct.Act(4);

        StringBuilder str = new StringBuilder();
        boolean flg = true, flg4 = mMap.m_nMapRight-mMap.m_nMapLeft+1 >= myMaps.m_nMaxCol;
        int newLeft = 0, newRight = 0, newTop = 0, newBottom = 0;

        if (flg4 || 1 > mMap.m_nMapLeft-1) {
            flg = false;
            str.append("左侧空间不足（").append(mMap.m_nMapLeft-1).append("）\n");
        }
        if (flg4 || 1 > mMap.m_nMapTop-1) {
            flg = false;
            str.append("顶部空间不足（").append(mMap.m_nMapTop-1).append("）\n");
        }
        if (flg4 || 1 + mMap.m_nMapRight >= myMaps.m_nMaxCol*2-1) {
            flg = false;
            str.append("右侧空间不足（").append(myMaps.m_nMaxCol*2-1 - mMap.m_nMapRight).append("）\n");
        }
        if (flg4 || 1 + mMap.m_nMapBottom >= myMaps.m_nMaxRow*2-1) {
            flg = false;
            str.append("底部空间不足（").append(myMaps.m_nMaxRow*2-1 - mMap.m_nMapBottom).append("）\n");
        }

        if (flg) {  // 允许调整
            switch (m_nSide) {
                case 0:
                    newLeft = 1;
                    break;
                case 1:
                    newTop = 1;
                    break;
                case 2:
                    newRight = 1;
                    break;
                case 3:
                    newBottom = 1;
                    break;
            }

            for (int i=mMap.m_nMapTop-newTop; i<=mMap.m_nMapBottom+newBottom; i++) {
                for (int j=mMap.m_nMapLeft-newLeft; j<=mMap.m_nMapRight+newRight; j++) {
                    if (i<mMap.m_nMapTop || i>mMap.m_nMapBottom || j<mMap.m_nMapLeft || j>mMap.m_nMapRight){
                        m_cArray[i][j] = '-';
                    }
                }
            }
            mMap.m_nMapLeft -= newLeft;
            mMap.m_nMapTop  -= newTop;
            mMap.m_nMapRight  += newRight;
            mMap.m_nMapBottom += newBottom;

            mMap.initArena();  //舞台初始化

            mMap.selNode.row = -1;
            mMap.isFistClick = true;
            m_UnDoList.offer(ndAct);
            bt_UnDo.setEnabled(true);
            m_ReDoList.clear();
            bt_ReDo.setEnabled(false);
            bt_Save.setEnabled(true);
        } else {
            MyToast.showToast(this, str.toString(), Toast.LENGTH_SHORT);
        }
    }

    //从 myEditViewMap 传来的调用
    public void DoAct(int act) {
        //动作入 UnDo 栈
        //  0: 填充区域或区域勾边
        //  3: 单点绘制
        //  6: 连续绘制
        ActNode nd;
        if (act == 0) {
            nd = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
            m_nItemSelect = 0;
            AlertDialog.Builder dlg = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
            dlg.setTitle("请选择").setSingleChoiceItems(new String[]{"填充", "勾边"}, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_nItemSelect = which;
                }
            }).setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (m_nItemSelect == 0) {  //填充
                        for (int i = mMap.selNode.row; i <= mMap.selNode2.row; i++) {
                            for (int j = mMap.selNode.col; j <= mMap.selNode2.col; j++) {
                                switch (mMap.m_Objs[mMap.cur_Obj]) {
                                    case '.':
                                        if (m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '$' ||
                                                m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '*')
                                            m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '*';
                                        else if (m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '@' ||
                                                m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '+')
                                            m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '+';
                                        else m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '.';
                                        break;
                                    case '$':
                                        if (m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '.' ||
                                                m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '+' ||
                                                m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '*')
                                            m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '*';
                                        else m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '$';
                                        break;
                                    default:
                                        m_cArray[i + mMap.m_nMapTop][j + mMap.m_nMapLeft] = mMap.m_Objs[mMap.cur_Obj];
                                        break;
                                }
                            }
                        }
                    } else {  //勾边
                        for (int j = mMap.selNode.col; j <= mMap.selNode2.col; j++) {
                            switch (mMap.m_Objs[mMap.cur_Obj]) {
                                case '.':
                                    if (m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '$' ||
                                            m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '*')
                                        m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '*';
                                    else if (m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '@' ||
                                            m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '+')
                                        m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '+';
                                    else m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '.';

                                    if (m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '$' ||
                                            m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '*')
                                        m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '*';
                                    else if (m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '@' ||
                                            m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '+')
                                        m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '+';
                                    else m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '.';
                                    break;
                                case '$':
                                    if (m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '.' ||
                                            m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '+' ||
                                            m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '*')
                                        m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '*';
                                    else m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '$';

                                    if (m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '.' ||
                                            m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '+' ||
                                            m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] == '*')
                                        m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '*';
                                    else
                                        m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = '$';
                                    break;
                                default:
                                    m_cArray[mMap.selNode.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = mMap.m_Objs[mMap.cur_Obj];
                                    m_cArray[mMap.selNode2.row + mMap.m_nMapTop][j + mMap.m_nMapLeft] = mMap.m_Objs[mMap.cur_Obj];
                                    break;
                            }
                        }
                        for (int i = mMap.selNode.row+1; i < mMap.selNode2.row; i++) {
                            switch (mMap.m_Objs[mMap.cur_Obj]) {
                                case '.':
                                    if (m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] == '$' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] == '*')
                                        m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] = '*';
                                    else if (m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] == '@' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] == '+')
                                        m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] = '+';
                                    else m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] = '.';

                                    if (m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] == '$' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] == '*')
                                        m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] = '*';
                                    else if (m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] == '@' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] == '+')
                                        m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] = '+';
                                    else m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] = '.';
                                    break;
                                case '$':
                                    if (m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] == '.' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] == '+' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] == '*')
                                        m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] = '*';
                                    else m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] = '$';

                                    if (m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] == '.' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] == '+' ||
                                            m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] == '*')
                                        m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] = '*';
                                    else m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] = '$';
                                    break;
                                default:
                                    m_cArray[i + mMap.m_nMapTop][mMap.selNode.col + mMap.m_nMapLeft] = mMap.m_Objs[mMap.cur_Obj];
                                    m_cArray[i + mMap.m_nMapTop][mMap.selNode2.col + mMap.m_nMapLeft] = mMap.m_Objs[mMap.cur_Obj];
                                    break;
                            }
                        }
                    }
                    mMap.invalidate();
                }
            }).setCancelable(false).create().show();
        } else
            nd = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, mMap.m_iR, mMap.m_iC, null, null, null, null);
        nd.Act(act);
        m_UnDoList.offer(nd);
        bt_UnDo.setEnabled(true);
        m_ReDoList.clear();
        bt_ReDo.setEnabled(false);
        bt_Save.setEnabled(true);
    }

    //UnDo
    private void myUnDo() {
        ActNode nd = m_UnDoList.pollLast(), nd2;

        if (nd.act == 3) {  //单点绘制
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, nd.row, nd.col, null, null, null, null);
            nd2.Act(nd.act);
            m_cArray[nd.row+nd.top][nd.col+nd.left] = nd.ch;
            mMap.selNode.row = -1;  //避免显示选择区域块
            mMap.isFistClick = true;  //准备记录第一坐标点
        } else if (nd.act == 6) {  //连续绘制
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, nd.row, nd.col, null, null, null, null);
            nd2.Act(nd.act);
            nd.setMap();
            mMap.selNode.row = -1;  //避免显示选择区域块
            mMap.isFistClick = true;  //准备记录第一坐标点
        } else if (nd.act == 0 || nd.act == 1 || nd.act == 2 || nd.act == 5 || nd.act == 90) {  //填充、剪切、粘贴、变换
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
            nd2.Act(nd.act);
            nd.setMap();
            if (nd.sel1 != null && nd.sel1.row >= 0) {
                if (nd.act == 90) {
                    selRows = nd.sel2.col - nd.sel1.col + 1;
                    selCols = nd.sel2.row - nd.sel1.row + 1;
                } else {
                    selRows = nd.sel2.row - nd.sel1.row + 1;
                    selCols = nd.sel2.col - nd.sel1.col + 1;
                }
                mMap.selNode.row = nd.sel1.row;
                mMap.selNode.col = nd.sel1.col;
                mMap.selNode2.row = mMap.selNode.row + selRows-1;
                mMap.selNode2.col = mMap.selNode.col + selCols-1;
            }
        } else {  //改变尺寸、标准化、提交、剪切板导入。act = 4、9、8、7
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
            nd2.Act(nd.act);
            nd.setMap();
            mMap.m_nMapLeft = nd.left;
            mMap.m_nMapRight = nd.right;
            mMap.m_nMapTop = nd.top;
            mMap.m_nMapBottom = nd.bottom;
            if (nd.mMtx != null) {
                mMap.mMatrix.set(nd.mMtx);
                mMap.mCurrentMatrix.set(nd.mCurMtx);
            }
            mMap.initArena3();
        }
        if (nd2 != null) {
            m_ReDoList.offer(nd2);
            bt_ReDo.setEnabled(true);
            bt_UnDo.setEnabled(!m_UnDoList.isEmpty());
            bt_Save.setEnabled(true);
            mMap.invalidate();
        }
        mMap.isFistClick = true;
    }

    //ReDo
    private void myReDo() {
        ActNode nd = m_ReDoList.pollLast(), nd2;

        if (nd.act == 3) {  //单点绘制
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, nd.row, nd.col, null, null, null, null);
            nd2.Act(nd.act);
            m_cArray[nd.row + nd.top][nd.col + nd.left] = nd.ch;
            mMap.selNode.row = -1;  //避免显示选择区域块
            mMap.isFistClick = true;  //准备记录第一坐标点
        } else if (nd.act == 6) {  //连续绘制
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, nd.row, nd.col, null, null, null, null);
            nd2.Act(nd.act);
            nd.setMap();
            mMap.selNode.row = -1;  //避免显示选择区域块
            mMap.isFistClick = true;  //准备记录第一坐标点
        } else if (nd.act == 0 || nd.act == 1 || nd.act == 2 || nd.act == 5 || nd.act == 90) {  //填充、剪切、粘贴、变换
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
            nd2.Act(nd.act);
            nd.setMap();
            if (nd.sel1 != null && nd.sel1.row >= 0) {
                if (nd.act == 90) {
                    selCols = nd.sel2.row - nd.sel1.row + 1;
                    selRows = nd.sel2.col - nd.sel1.col + 1;
                } else {
                    selRows = nd.sel2.row - nd.sel1.row + 1;
                    selCols = nd.sel2.col - nd.sel1.col + 1;
                }
                mMap.selNode.row = nd.sel1.row;
                mMap.selNode.col = nd.sel1.col;
                mMap.selNode2.row = mMap.selNode.row + selRows-1;
                mMap.selNode2.col = mMap.selNode.col + selCols-1;
            }
        } else {  //改变尺寸、标准化、提交、剪切板导入。act = 4、9、8、7
            nd2 = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
            nd2.Act(nd.act);
            nd.setMap();
            mMap.m_nMapLeft = nd.left;
            mMap.m_nMapRight = nd.right;
            mMap.m_nMapTop = nd.top;
            mMap.m_nMapBottom = nd.bottom;
            if (nd.mMtx != null) {
                mMap.mMatrix.set(nd.mMtx);
                mMap.mCurrentMatrix.set(nd.mCurMtx);
            }
            mMap.initArena3();
        }
        if (nd2 != null) {
            m_UnDoList.offer(nd2);
            bt_UnDo.setEnabled(true);
            bt_Save.setEnabled(true);
            bt_ReDo.setEnabled(!m_ReDoList.isEmpty());
            mMap.invalidate();
        }
        mMap.isFistClick = true;
    }

    //导出 XSB
    private void myExport() {
        //将关卡地图，送入剪切板。
        StringBuilder str = new StringBuilder();

        //导出全部（有效部分）或选区部分
        if (mMap.selNode.row < 0 || mMap.selNode == mMap.selNode2) {
            str.append(getXSB(m_cArray));  //计算地图有效部分
        } else {  //仅导出选区部分
            for (int i = mMap.selNode.row; i <= mMap.selNode2.row; i++) {
                for (int j = mMap.selNode.col; j <= mMap.selNode2.col; j++) {
                    if (isOK(m_cArray[i+mMap.m_nMapTop][j+mMap.m_nMapLeft])) str.append(m_cArray[i+mMap.m_nMapTop][j+mMap.m_nMapLeft]);
                    else str.append('-');
                }
                str.append('\n');
            }
        }
        str.append("Title: ").append(myMaps.curMap.Title).append('\n');
        str.append("Author: ").append(myMaps.curMap.Author).append('\n');
        str.append("Comment:").append('\n').append(myMaps.curMap.Comment).append('\n');
        str.append("Comment_end:");
        final EditText et = new EditText(this);
        et.setTypeface(Typeface.MONOSPACE);
        et.setText(str);
        new Builder(this).setTitle("剪切板").setView(et).setCancelable(false)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        myMaps.saveClipper(et.getText().toString());
                    }}).create().show();
    }

    //从剪切板导入 XSB
    private void myImport() {
        String str = myMaps.loadClipper();
        if (str.equals("")) {
            MyToast.showToast(this, "没有数据可导入！", Toast.LENGTH_SHORT);
        } else {
            final EditText et = new EditText(this);
            et.setTypeface(Typeface.MONOSPACE);
            et.setText(str);
            new Builder(this).setTitle("剪切板").setView(et).setCancelable(false)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                StringBuilder ss = new StringBuilder();
                                ss.append(et.getText());
                                String fn = myMaps.curMap.fileName;
                                if (myMaps.isLURD(ss.toString())) {  //导入的是答案
                                    if (LurdToXSB(ss.toString())) {
                                        ndAct = null;
                                        ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
                                        ndAct.Act(7);

                                        initMap();

                                        myMaps.curMap.fileName = fn;
                                        m_UnDoList.offer(ndAct);
                                        bt_UnDo.setEnabled(true);
                                        m_ReDoList.clear();
                                        bt_ReDo.setEnabled(false);
                                        bt_Save.setEnabled(true);
                                    } else throw new Exception();
                                } else if (myMaps.loadXSB(ss.toString())) {  //导入 XSB 有效
                                    ndAct = null;
                                    ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, mMap.mMatrix, mMap.mCurrentMatrix, null, null);
                                    ndAct.Act(7);

                                    initMap();

                                    myMaps.curMap.fileName = fn;
                                    m_UnDoList.offer(ndAct);
                                    bt_UnDo.setEnabled(true);
                                    m_ReDoList.clear();
                                    bt_ReDo.setEnabled(false);
                                    bt_Save.setEnabled(true);
                                } else throw new Exception();
                            } catch (Exception e) {
                                MyToast.showToast(myEditView.this, "数据不可用！", Toast.LENGTH_SHORT);
                            }

                        }}).create().show();
        }
    }

    //90度旋转写入
    private void myRot90(final boolean is90) {
        //取区域行列的小者，检查旋转前后的重合区域用
        int mRC = selRows < selCols ? selRows : selCols;

        //计算旋转后的区域尺寸
        selRows2 = selCols;
        selCols2 = selRows;

        //再分析是否需要扩展关卡尺寸，要用到 selRows2、selCols2
        resetSize();

        //检查新区域是否存在绘制，也要用到 selRows2、selCols2
        boolean flg = false;
        for (int i = 0; i < selRows2; i++) {
            for (int j = 0; j < selCols2; j++) {
                if (i < mRC && j < mRC) continue; //重合区域，不需检查
                if (m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '#' ||
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '$' ||
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '*' ||
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '@' ||
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '+' ||
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] == '.') {
                    flg = true;
                    break;
                }
            }
        }
        if (flg) {//旋转会覆盖部分已绘制的区域，则进行覆盖提醒
            AlertDialog.Builder dlg = new Builder(this, AlertDialog.THEME_HOLO_DARK);
            dlg.setTitle("提醒").setMessage("部分原有绘制将被覆盖，确定吗？").setCancelable(false).setNegativeButton("取消", null);
            dlg.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ndAct = null;
                    ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
                    ndAct.Act(90);

                    for (int i = 0; i < selRows; i++) {  //清除原区域
                        for (int j = 0; j < selCols; j++) {
                            m_cSelArray[i][j] = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft];
                            m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] = '-';
                        }
                    }
                    char ch;
                    for (int i = 0; i < selRows2; i++) {  //旋转覆盖写入
                        for (int j = 0; j < selCols2; j++) {
                            if (is90) ch = m_cSelArray[selRows-1-j][i];  //顺 90 度
                            else ch = m_cSelArray[j][selCols-1-i];  //逆 90 度
                            m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] = ch;
                        }
                    }
                    //更改第二选择点坐标
                    mMap.selNode2.row = mMap.selNode.row + selRows2 - 1;
                    mMap.selNode2.col = mMap.selNode.col + selCols2 - 1;
                    selRows = selRows2;
                    selCols = selCols2;

                    m_UnDoList.offer(ndAct);
                    bt_UnDo.setEnabled(true);
                    m_ReDoList.clear();
                    bt_ReDo.setEnabled(false);
                    bt_Save.setEnabled(true);
                    mMap.invalidate();
                }
            });
            dlg.show();
        } else {
            ndAct = null;
            ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
            ndAct.Act(90);

            for (int i = 0; i < selRows; i++) {  //清除原区域
                for (int j = 0; j < selCols; j++) {
                    m_cSelArray[i][j] = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft];
                    m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] = '-';
                }
            }
            char ch;
            for (int i = 0; i < selRows2; i++) {  //旋转写入
                for (int j = 0; j < selCols2; j++) {
                    if (is90) ch = m_cSelArray[selRows-1-j][i];  //顺 90 度
                    else ch = m_cSelArray[j][selCols-1-i];  //逆 90 度
                    m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] = ch;
                }
            }
            //更改第二选择点坐标
            mMap.selNode2.row = mMap.selNode.row + selRows2 - 1;
            mMap.selNode2.col = mMap.selNode.col + selCols2 - 1;
            selRows = selRows2;
            selCols = selCols2;

            m_UnDoList.offer(ndAct);
            bt_UnDo.setEnabled(true);
            m_ReDoList.clear();
            bt_ReDo.setEnabled(false);
            bt_Save.setEnabled(true);
            mMap.invalidate();
        }
    }

    //用答案倒推关卡
    private boolean LurdToXSB(String mStr) {

        try {
            char[][] m_tLevel = new char[myMaps.m_nMaxRow*2][myMaps.m_nMaxCol*2];
            for (int i = 0; i < myMaps.m_nMaxRow*2; i++) {
                for (int j = 0; j < myMaps.m_nMaxCol * 2; j++) {
                    m_tLevel[i][j] = '_';  //暂时假定全部为墙外地板
                }
            }

            //逆推法计算关卡初态
            int row = myMaps.m_nMaxRow, col = myMaps.m_nMaxCol;  //预设仓管员初始位置
            m_tLevel[row][col] = '-';

            int top = row, bottom = row, left = col, right = col;  //关卡四至
            int row2, col2, box_row, box_col;  //记录移动中的位置
            int len = mStr.length();  //答案长度
            for (int k = len-1; k >= 0; k--) {
                switch (mStr.charAt(k)) {
                    case 'r':
                        col2 = col - 1;
                        if (col2 < 1 || m_tLevel[row][col2] == '$' || m_tLevel[row][col2] == '*') {
                            return false;
                        } else {
                            if (m_tLevel[row][col2] == '_') {  //仓管员的新位置是第一次访问
                                m_tLevel[row][col2] = '-';
                            }
                            col = col2;
                            if (left > col) left = col;
                        }
                        break;
                    case 'd':
                        row2 = row - 1;
                        if (row2 < 1 || m_tLevel[row2][col] == '$' || m_tLevel[row2][col] == '*') {
                            return false;
                        } else {
                            if (m_tLevel[row2][col] == '_') {  //仓管员的新位置是第一次访问
                                m_tLevel[row2][col] = '-';
                            }
                            row = row2;
                            if (top > row) top = row;
                        }
                        break;
                    case 'l':
                        col2 = col + 1;
                        if (col2 >= myMaps.m_nMaxCol * 2 || m_tLevel[row][col2] == '$' || m_tLevel[row][col2] == '*') {
                            return false;
                        } else {
                            if (m_tLevel[row][col2] == '_') {  //仓管员的新位置是第一次访问
                                m_tLevel[row][col2] = '-';
                            }
                            col = col2;
                            if (right < col) right = col;
                        }
                        break;
                    case 'u':
                        row2 = row + 1;
                        if (row2 >= myMaps.m_nMaxRow * 2 || m_tLevel[row2][col] == '$' || m_tLevel[row2][col] == '*') {
                            return false;
                        } else {
                            if (m_tLevel[row2][col] == '_') {  //仓管员的新位置是第一次访问
                                m_tLevel[row2][col] = '-';
                            }
                            row = row2;
                            if (bottom < row) bottom = row;
                        }
                        break;
                    case 'R':
                        col2 = col - 1;
                        box_col = col + 1;

                        //界外或遇到矛盾的格子
                        if (col2 < 1 || box_col >= myMaps.m_nMaxCol * 2 || m_tLevel[row][col2] == '$' || m_tLevel[row][col2] == '*' || m_tLevel[row][box_col] == '-') {
                            return false;
                        }

                        if (m_tLevel[row][col2] == '_') {  //仓管员的新位置是第一次访问
                            m_tLevel[row][col2] = '-';
                        }

                        if (m_tLevel[row][box_col] == '_' || m_tLevel[row][box_col] == '*') {  //箱子的位置是第一次访问或箱子在目标点位上
                            m_tLevel[row][box_col] = '.';
                        } else if (m_tLevel[row][box_col] == '$') {
                            m_tLevel[row][box_col] = '-';
                        }
                        if (m_tLevel[row][col] == '.') {  //箱子移到仓管员的位置
                            m_tLevel[row][col] = '*';
                        } else {
                            m_tLevel[row][col] = '$';
                        }

                        col = col2;  //仓管员移到新位置

                        if (left > col) left = col;  //调整关卡四至
                        if (right < box_col) right = box_col;

                        break;
                    case 'D':
                        row2 = row - 1;
                        box_row = row + 1;

                        //界外或遇到矛盾的格子
                        if (row2 < 1 || box_row >= myMaps.m_nMaxRow * 2 || m_tLevel[row2][col] == '$' || m_tLevel[row2][col] == '*' || m_tLevel[box_row][col] == '-') {
                            return false;
                        }

                        if (m_tLevel[row2][col] == '_') {  //仓管员的新位置是第一次访问
                            m_tLevel[row2][col] = '-';
                        }

                        if (m_tLevel[box_row][col] == '_' || m_tLevel[box_row][col] == '*') {  //箱子的位置是第一次访问或箱子在目标点位上
                            m_tLevel[box_row][col] = '.';
                        } else if (m_tLevel[box_row][col] == '$') {
                            m_tLevel[box_row][col] = '-';
                        }
                        if (m_tLevel[row][col] == '.') {  //箱子移到仓管员的位置
                            m_tLevel[row][col] = '*';
                        } else {
                            m_tLevel[row][col] = '$';
                        }

                        row = row2;  //仓管员移到新位置

                        if (top > row) top = row;  //调整关卡四至
                        if (bottom < box_row) bottom = box_row;

                        break;
                    case 'L':
                        col2 = col + 1;
                        box_col = col - 1;

                        //界外或遇到矛盾的格子
                        if (box_col < 1 || col2 >= myMaps.m_nMaxCol * 2 || m_tLevel[row][col2] == '$' || m_tLevel[row][col2] == '*' || m_tLevel[row][box_col] == '-') {
                            return false;
                        }

                        if (m_tLevel[row][col2] == '_') {  //仓管员的新位置是第一次访问
                            m_tLevel[row][col2] = '-';
                        }

                        if (m_tLevel[row][box_col] == '_' || m_tLevel[row][box_col] == '*') {  //箱子的位置是第一次访问或箱子在目标点位上
                            m_tLevel[row][box_col] = '.';
                        } else if (m_tLevel[row][box_col] == '$') {
                            m_tLevel[row][box_col] = '-';
                        }

                        if (m_tLevel[row][col] == '.') {  //箱子移到仓管员的位置
                            m_tLevel[row][col] = '*';
                        } else {
                            m_tLevel[row][col] = '$';
                        }

                        col = col2;  //仓管员移到新位置

                        if (right < col) right = col;  //调整关卡四至
                        if (left > box_col) left = box_col;

                        break;
                    case 'U':
                        row2 = row + 1;
                        box_row = row - 1;

                        //界外或遇到矛盾的格子
                        if (box_row < 1 || row2 >= myMaps.m_nMaxRow * 2 || m_tLevel[row2][col] == '$' || m_tLevel[row2][col] == '*' || m_tLevel[box_row][col] == '-') {
                            return false;
                        }

                        if (m_tLevel[row2][col] == '_') {  //仓管员的新位置是第一次访问
                            m_tLevel[row2][col] = '-';
                        }

                        if (m_tLevel[box_row][col] == '_' || m_tLevel[box_row][col] == '*') {  //箱子的位置是第一次访问或箱子在目标点位上
                            m_tLevel[box_row][col] = '.';
                        } else if (m_tLevel[box_row][col] == '$') {
                            m_tLevel[box_row][col] = '-';
                        }
                        if (m_tLevel[row][col] == '.') {  //箱子移到仓管员的位置
                            m_tLevel[row][col] = '*';
                        } else {
                            m_tLevel[row][col] = '$';
                        }

                        row = row2;  //仓管员移到新位置

                        if (bottom < row) bottom = row;  //调整关卡四至
                        if (top > box_row) top = box_row;
                }
            }

            if (right-left < 2 && bottom-top < 2) return false;

            //仓管员
            if (m_tLevel[row][col] == '.') {
                m_tLevel[row][col] = '+';
            } else {
                m_tLevel[row][col] = '@';
            }

            //关卡标准化
            for (int i = top; i <= bottom; i++) {
                for (int j =left; j <= right; j++) {
                    if (m_tLevel[i][j] != '_' && m_tLevel[i][j] != '#'){
                        if (m_tLevel[i-1][j] == '_') m_tLevel[i-1][j] = '#';
                        if (m_tLevel[i+1][j] == '_') m_tLevel[i+1][j] = '#';
                        if (m_tLevel[i][j-1] == '_') m_tLevel[i][j-1] = '#';
                        if (m_tLevel[i][j+1] == '_') m_tLevel[i][j+1] = '#';
                        if (m_tLevel[i+1][j-1] == '_') m_tLevel[i+1][j-1] = '#';
                        if (m_tLevel[i+1][j+1] == '_') m_tLevel[i+1][j+1] = '#';
                        if (m_tLevel[i-1][j-1] == '_') m_tLevel[i-1][j-1] = '#';
                        if (m_tLevel[i-1][j+1] == '_') m_tLevel[i-1][j+1] = '#';
                    }
                }
            }

            myMaps.curMap = new mapNode(left-1, right+1, top-1, bottom+1, m_tLevel);

            return true;
        } catch(Exception e) {
            return false;
        }
    }

    //左上角尽量不动，旋转区域
    private void myRotate(int n) {
        if (n < 1 || n > 2) {  //1、2 情况特殊，需在调整四至且询问后，才可进入 undo 栈
            ndAct = null;
            ndAct = new ActNode(m_cArray, mMap.m_nMapLeft, mMap.m_nMapRight, mMap.m_nMapTop, mMap.m_nMapBottom, -1, -1, null, null, mMap.selNode, mMap.selNode2);
            ndAct.Act(5);
        }
        selRows = mMap.selNode2.row - mMap.selNode.row + 1;
        selCols = mMap.selNode2.col - mMap.selNode.col + 1;
        char ch;
        switch (n) {
            case 0:  //180度
                for (int i = 0; i < selRows/2; i++) {
                    for (int j = 0; j < selCols; j++) {
                        ch = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft]
                                = m_cArray[mMap.selNode.row + selRows-1 - i + mMap.m_nMapTop][mMap.selNode.col + selCols-1 - j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + selRows-1 - i + mMap.m_nMapTop][mMap.selNode.col + selCols-1 - j + mMap.m_nMapLeft] = ch;
                    }
                }
                if (selRows % 2 == 1) {
                    int i = selRows / 2;
                    for (int j = 0; j < selCols/2; j++) {
                        ch = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft]
                                = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + selCols-1 - j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + selCols-1 - j + mMap.m_nMapLeft] = ch;
                    }
                }
                break;
            case 1:  //顺90度

                myRot90(true);

                break;
            case 2:  //逆90度

                myRot90(false);

                break;
            case 3:  //水平翻转
                for (int i = 0; i < selRows; i++) {
                    for (int j = 0; j < selCols/2; j++) {
                        ch = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft]
                                = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + selCols-1 - j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + selCols-1 - j + mMap.m_nMapLeft] = ch;
                    }
                }
                break;
            case 4:  //垂直翻转
                for (int i = 0; i < selRows/2; i++) {
                    for (int j = 0; j < selCols; j++) {
                        ch = m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft]
                                = m_cArray[mMap.selNode.row + selRows-1 - i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft];
                        m_cArray[mMap.selNode.row + selRows-1 - i + mMap.m_nMapTop][mMap.selNode.col + j + mMap.m_nMapLeft] = ch;
                    }
                }
                break;
        }
        if (n < 1 || n > 2) {
            m_UnDoList.offer(ndAct);
            bt_UnDo.setEnabled(true);
            m_ReDoList.clear();
            bt_ReDo.setEnabled(false);
            bt_Save.setEnabled(true);
            mMap.invalidate();
        }
    }

    //自动调整关卡尺寸，粘贴和变换，现有关卡尺寸内不能满足区域需求时使用
    private void resetSize() {
        //扩展尺寸以便粘贴
        if (mMap.selNode.row + selRows2 + mMap.m_nMapTop > mMap.m_nMapBottom) {  //高超现有尺寸
            if (mMap.m_nMapTop + mMap.selNode.row + selRows2 > myMaps.m_nMaxRow*2-1)  //高超最大尺寸
                selRows2 = myMaps.m_nMaxRow*2-1 - mMap.m_nMapTop - mMap.selNode.row;  //调整剪切板中的 XSB 尺寸的高
            mMap.m_nMapBottom = mMap.m_nMapTop + mMap.selNode.row + selRows2 - 1;
        }
        if (mMap.selNode.col + selCols2 + mMap.m_nMapLeft > mMap.m_nMapRight) {  //宽超现有尺寸
            if (mMap.m_nMapLeft + mMap.selNode.col + selCols2 > myMaps.m_nMaxCol*2-1)  //宽超最大尺寸
                selCols2 = myMaps.m_nMaxCol*2-1 - mMap.m_nMapLeft - mMap.selNode.col;  //调整剪切板中的 XSB 尺寸的宽
            mMap.m_nMapRight = mMap.m_nMapLeft + mMap.selNode.col + selCols2 - 1;
        }

        mMap.initArena2();
    }

    //计数箱子数和目标数，拼接为字符串，供顶行信息栏显示
    public String getBoxs() {
        int m_nBoxs = 0, m_nDsts = 0;  //箱子数、目标数记录
        for (int r = mMap.m_nMapTop; r <= mMap.m_nMapBottom; r++) {
            for (int c = mMap.m_nMapLeft; c <= mMap.m_nMapRight; c++) {
                switch (m_cArray[r][c]){
                    case '$':
                        m_nBoxs++;
                        break;
                    case '.':
                    case '+':
                        m_nDsts++;
                        break;
                    case '*':
                        m_nBoxs++;
                        m_nDsts++;
                        break;
                }
            }
        }
        StringBuilder str = new StringBuilder();
        str.append(" B-").append(m_nBoxs).append(" G-").append(m_nDsts);
        return str.toString();
    }

    //解析剪切板中字符串里的XSB，剪切、复制、粘贴用，如此处理，可接收系统剪切板传来的 XSB 进行粘贴
    private boolean cutXSB(String str) {
        String[] Arr;
        try {
            Arr = str.split("\r\n|\n\r|\n|\r|\\|");

            String[] new_map = new String[myMaps.m_nMaxRow];
            int rows = 0;
            int cols = 0;

            boolean flg = false;
            for (int k = 0; k < Arr.length && k < myMaps.m_nMaxRow; k++) {
                if (myMaps.isXSB(Arr[k])) {
                    flg = true;
                    new_map[rows] = Arr[k];
                    if (cols < Arr[k].length()) cols = Arr[k].length();  //最大宽度
                    rows++;
                } else {  //可以跳过开头的无效行
                    if (flg) break;  //匹配 XSB 行，XSB 行必须连续
                }
            }

            int n;
            char ch;
            for (int i = 0; i < rows; i++) {
                n = new_map[i].length();
                for (int j = 0; j < cols; j++){
                    ch = j < n ? new_map[i].charAt(j) : '-';
                    if (isOK(ch)) m_cSelArray[i][j] = ch;
                    else m_cSelArray[i][j] = '-';
                }
            }
            selRows2 = rows;
            selCols2 = cols;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //保存关卡到文档
    private boolean saveFile(String my_Name) {
        try{
            FileOutputStream fout = new FileOutputStream(myMaps.sRoot+myMaps.sPath+"创编关卡/"+my_Name);

            StringBuilder str = new StringBuilder();

            //关卡XSB
            for (int r = mMap.m_nMapTop; r <= mMap.m_nMapBottom; r++) {
                for (int c = mMap.m_nMapLeft; c <= mMap.m_nMapRight; c++) {
                    str.append(m_cArray[r][c]);
                }
                if (r < mMap.m_nMapBottom) str.append('\n');
            }

            myMaps.curMap.Map = str.toString();
            myMaps.curMap.Rows = mMap.m_nMapBottom-mMap.m_nMapTop + 1;
            myMaps.curMap.Cols = mMap.m_nMapRight-mMap.m_nMapLeft + 1;

            str.append("\nTitle: ").append(myMaps.curMap.Title);
            str.append("\nAuthor: ").append(myMaps.curMap.Author);
            str.append("\nComment:");
            str.append('\n').append(myMaps.curMap.Comment);
            int len = myMaps.curMap.Comment.length()-1;
            if (len >= 0 &&
                    myMaps.curMap.Comment.charAt(len) != '\n' &&
                    myMaps.curMap.Comment.charAt(len) != '\r')
                str.append('\n');
            str.append("Comment_end:");

            fout.write(str.toString().getBytes());

            fout.flush();
            fout.close();

            if (myMaps.curMapNum < -1) {  //新建关卡或创编为新关卡
                myMaps.m_lstMaps.add(myMaps.curMap);
                myMaps.curMapNum = myMaps.m_lstMaps.size() - 1;  //-2，-3为新增，此时，改为继续编辑
            } else {  //编辑关卡
                myMaps.m_lstMaps.set(myMaps.curMapNum, myMaps.curMap);
            }

            return true;
        } catch(Exception e) {
            return false;
        }
    }

    //区块另存为...
    private void myBlockSave(char[][] m_Level) {
        //没有选区时
        if (mMap.selNode.row < 0) {
            MyToast.showToast(this, "请选择一个区块！", Toast.LENGTH_LONG);
            return;
        }

        int r1 = mMap.selNode.row;
        int r2 = mMap.selNode2.row;
        int c1 = mMap.selNode.col;
        int c2 = mMap.selNode2.col;

        if (r2-r1 < 1 || c2-c1 < 1) {
            MyToast.showToast(this, "区块太小！", Toast.LENGTH_LONG);
            return;
        }

        try{
            //区块 XSB
            StringBuilder str = new StringBuilder();
            for (int i = r1; i <= r2; i++) {
                for (int j = c1; j <= c2; j++) {
                    if (isOK(m_Level[i+mMap.m_nMapTop][j+mMap.m_nMapLeft])) str.append(m_Level[i+mMap.m_nMapTop][j+mMap.m_nMapLeft]);
                    else str.append('-');
                }
                str.append('\n');
            }

            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");//设置日期格式
            String fn = "~Block_"+df.format(new Date());  // new Date()获取当前系统时间
            //写入文档
            FileOutputStream fout = new FileOutputStream(myMaps.sRoot+myMaps.sPath+"创编关卡/"+fn+".XSB");

            str.append("Title: " + fn);  //给块一个标题
            fout.write(str.toString().getBytes());

            fout.flush();
            fout.close();

            mapNode nd = new mapNode(c1+mMap.m_nMapLeft, c2+mMap.m_nMapLeft, r1+mMap.m_nMapTop, r2+mMap.m_nMapTop, m_Level);
            nd.Title = fn;
            nd.fileName = fn + ".XSB";
            myMaps.m_lstMaps.add(nd);

            myMaps.isSaveBlock = true;

            AlertDialog.Builder dlg = new Builder(this, AlertDialog.THEME_HOLO_DARK);
            dlg.setTitle("成功").setMessage("标题(Title):\n"+fn).setCancelable(false)
                    .setPositiveButton("确定", null).create().show();
        } catch(Exception e) {
            AlertDialog.Builder dlg = new Builder(this, AlertDialog.THEME_HOLO_DARK);
            dlg.setTitle("错误").setMessage("写错误，保存失败！").setCancelable(false)
                    .setPositiveButton("确定", null).create().show();
        }
    }

    //检查 ch 是否为非空地的 XSB 字符（编辑用的字符）
    public boolean isOK(char ch) {
        if (ch == '#' || ch == '.' || ch == '$' || ch == '*' || ch == '@' || ch == '+')
            return true;
        else return false;
    }

    //导出前计算地图有效部分
    private String getXSB(char[][] mLevel) {
        int r1;
        boolean flg = false;
        for (r1 = mMap.m_nMapTop; r1 <= mMap.m_nMapBottom; r1++) {
            for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                if (isOK(mLevel[r1][j])) {
                    flg = true;
                    break;
                }
            }
            if (flg) break;
        }
        if (r1 > mMap.m_nMapBottom) return "";  //空的地图

        int r2;
        flg = false;
        for (r2 = mMap.m_nMapBottom; r2 > r1; r2--) {
            for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                if (isOK(mLevel[r2][j])) {
                    flg = true;
                    break;
                }
            }
            if (flg) break;
        }

        int c1;
        flg = false;
        for (c1 = mMap.m_nMapLeft; c1 <= mMap.m_nMapRight; c1++) {
            for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
                if (isOK(mLevel[i][c1])) {
                    flg = true;
                    break;
                }
            }
            if (flg) break;
        }

        int c2;
        flg = false;
        for (c2 = mMap.m_nMapRight; c2 > c1; c2--) {
            for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
                if (isOK(mLevel[i][c2])) {
                    flg = true;
                    break;
                }
            }
            if (flg) break;
        }

        StringBuilder str = new StringBuilder();
        for (int i = r1; i <= r2; i++) {
            for (int j = c1; j <= c2; j++){
                if (isOK(mLevel[i][j])) str.append(mLevel[i][j]);
                else str.append('-');
            }
            str.append('\n');
        }
        return str.toString();
    }

    //关卡简单标准化————保留墙外造型（提交前调用）
    public boolean Normalize2(char[][] mLevel) {
        char ch;
        int mr = -1, mc = -1;  //记录仓管员位置
        int nRen = 0;  //箱子数、目标数、仓管员数
        for (int r = mMap.m_nMapTop; r <= mMap.m_nMapBottom; r++) {
            for (int c = mMap.m_nMapLeft; c <= mMap.m_nMapRight; c++) {
                ch = mLevel[r][c];
                switch (ch){
                    case '@':
                    case '+':
                        nRen++;
                        mr = r;
                        mc = c;
                        break;
                }
            }
        }

        if (nRen != 1) {
            AlertDialog.Builder dlg = new Builder(this, AlertDialog.THEME_HOLO_DARK);
            dlg.setTitle("错误").setMessage("仓管员数目不正确！").setCancelable(false)
                    .setPositiveButton("确定", null).create().show();

            return false;
        }

        int[] dr ={-1, 1, 0, 0, -1, 1, -1, 1};	//前四个是可移动方向，后面是四个临角
        int[] dc ={0, 0, 1, -1, -1, -1, 1, 1};
        boolean[][] Mark = new boolean[mMap.m_nMapBottom-mMap.m_nMapTop+1][mMap.m_nMapRight-mMap.m_nMapLeft+1];	//标志数组，表示地图上某一位置Mark1[][]是否访问过。
        int nBox = 0, nDst = 0, F, r, c, r1 = mr, c1 = mc, r2 = mr, c2 = mc;  //关卡图四至修正前准备

        Queue<Integer> P = new LinkedList<Integer>();
        P.offer(mr << 16 | mc);
        Mark[mr-mMap.m_nMapTop][mc-mMap.m_nMapLeft] = true;
        while (!P.isEmpty()) { //走完后，Mark[][]为 1 的，为墙内的可达地板
            F = P.poll();
            mr = F >>> 16;
            mc = F & 0x0000FFFF;

            switch (mLevel[mr][mc]) {
                case '$':
                    nBox++;
                    break;
                case '*':
                    nBox++;
                    nDst++;
                    break;
                case '.':
                case '+':
                    nDst++;
                    break;
            }

            for (int k = 0; k < 4; k++) {//仓管员向四个方向走
                r = mr + dr[k];
                c = mc + dc[k];
                if (r < mMap.m_nMapTop || r > mMap.m_nMapBottom || c < mMap.m_nMapLeft || c > mMap.m_nMapRight ||
                        Mark[r-mMap.m_nMapTop][c-mMap.m_nMapLeft] || mLevel[r][c] == '#') continue; //遇到墙壁或已访问或界外

                //重新计算四至
                if (r1 > r) r1 = r;  //顶
                if (c1 > c) c1 = c;  //左
                if (r2 < r) r2 = r;  //底
                if (c2 < c) c2 = c;  //右

                //做已访问标记
                P.add(r << 16 | c);
                Mark[r-mMap.m_nMapTop][c-mMap.m_nMapLeft] = true;
            }
        }

        if (nBox != nDst || nBox < 1 || nDst < 1) {  //可达区域内的箱子与目标点数不正确
            AlertDialog.Builder dlg = new Builder(this, AlertDialog.THEME_HOLO_DARK);
            dlg.setTitle("警告").setMessage("箱子或目标数不正确！\n箱子数："+nBox+"，目标数："+nDst+"").setCancelable(false)
                    .setPositiveButton("确定", null).create().show();

            return false;
        }

        //补充必要的墙壁并去掉关卡图界外的无效元素
        int r4, c4;
        for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
            for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                if (!Mark[i - mMap.m_nMapTop][j - mMap.m_nMapLeft]) {  //清理非可达区域的无效元素
                    if (mLevel[i][j] == '#' || mLevel[i][j] == '$' || mLevel[i][j] == '*') {
                        if (mLevel[i][j] == '$' || mLevel[i][j] == '*') {
                            mLevel[i][j] = '#';
                        }
                        if (i < r1)  r1 = i;
                        if (j < c1)  c1 = j;
                        if (i > r2)  r2 = i;
                        if (j > c2)  c2 = j;
                    } else {
                        mLevel[i][j] = '-';
                        for (int k = 0; k < 8; k++) {
                            r4 = i + dr[k];
                            c4 = j + dc[k];

                            if (r4 < mMap.m_nMapTop || r4 > mMap.m_nMapBottom || c4 < mMap.m_nMapLeft || c4 > mMap.m_nMapRight) continue;

                            if (Mark[r4-mMap.m_nMapTop][c4-mMap.m_nMapLeft]) {
                                mLevel[i][j] = '#';
                                if (i < r1) r1 = i;
                                if (j < c1) c1 = j;
                                if (i > r2) r2 = i;
                                if (j > c2) c2 = j;
                                break;
                            }
                        }
                    }
                }
            }
        }

        //修正关卡四至
        mMap.m_nMapBottom = r2;
        mMap.m_nMapRight  = c2;
        mMap.m_nMapTop  = r1;
        mMap.m_nMapLeft = c1;

        //将标准化后的关卡数据，记入 myMaps.curMap
        StringBuilder str = new StringBuilder();
        for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
            for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++){
                if (isOK(mLevel[i][j])) str.append(mLevel[i][j]);
                else str.append('-');
            }
            if (i < mMap.m_nMapBottom) str.append('\n');
        }
        myMaps.curMap.Map = str.toString();
        myMaps.curMap.Rows = mMap.m_nMapBottom-mMap.m_nMapTop + 1;
        myMaps.curMap.Cols = mMap.m_nMapRight-mMap.m_nMapLeft + 1;

        return true;
    }

    //精准标准化
    private void Normalize(char[][] mLevel) {
        char ch;
        int mr = -1, mc = -1;  //记录仓管员位置
        int nRen = 0;  //箱子数、目标数、仓管员数

        for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
            for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                ch = mLevel[i][j];
                switch (ch){
                    case '@':
                    case '+':
                        nRen++;
                        mr = i;
                        mc = j;
                        break;
                }
            }
        }

        if (nRen != 1) {
            AlertDialog.Builder dlg = new Builder(this, AlertDialog.THEME_HOLO_DARK);
            dlg.setTitle("错误").setMessage("仓管员数目不正确！").setCancelable(false)
                    .setPositiveButton("确定", null).create().show();

            return;
        }

        //标准化预处理
        if (mMap.m_nMapTop > 0 && mMap.m_nMapBottom-mMap.m_nMapTop+1 < myMaps.m_nMaxRow) {
            mMap.m_nMapTop--;
            for (int k = mMap.m_nMapLeft; k <= mMap.m_nMapRight; k++) {
                mLevel[mMap.m_nMapTop][k] = '#';
            }
        }
        if (mMap.m_nMapBottom < myMaps.m_nMaxRow*2-1 && mMap.m_nMapBottom-mMap.m_nMapTop+1 < myMaps.m_nMaxRow) {
            mMap.m_nMapBottom++;
            for (int k = mMap.m_nMapLeft; k <= mMap.m_nMapRight; k++) {
                mLevel[mMap.m_nMapBottom][k] = '#';
            }
        }
        if (mMap.m_nMapLeft > 0 && mMap.m_nMapRight-mMap.m_nMapLeft+1 < myMaps.m_nMaxCol) {
            mMap.m_nMapLeft--;
            for (int k = mMap.m_nMapTop; k <= mMap.m_nMapBottom; k++) {
                mLevel[k][mMap.m_nMapLeft] = '#';
            }
        }
        if (mMap.m_nMapRight < myMaps.m_nMaxCol*2-1 && mMap.m_nMapRight-mMap.m_nMapLeft+1 < myMaps.m_nMaxCol) {
            mMap.m_nMapRight++;
            for (int k = mMap.m_nMapTop; k <= mMap.m_nMapBottom; k++) {
                mLevel[k][mMap.m_nMapRight] = '#';
            }
        }

        int[] dr ={-1, 1, 0, 0, -1, 1, -1, 1};	//前四个是可移动方向，后面是四个临角
        int[] dc ={0, 0, 1, -1, -1, -1, 1, 1};
        boolean[][] Mark = new boolean[mMap.m_nMapBottom-mMap.m_nMapTop+1][mMap.m_nMapRight-mMap.m_nMapLeft+1];	//是否为可达地板
        int F, r, c, r1 = mr, r2 = mr, c1 = mc, c2 = mc;  //四至修正前准备，以仓管员为中心，向四周扩充

        Queue<Integer> P = new LinkedList<Integer>();
        P.offer(mr << 16 | mc);
        Mark[mr-mMap.m_nMapTop][mc-mMap.m_nMapLeft] = true;  //已访问
        while (!P.isEmpty()) {
            F = P.poll();
            mr = F >>> 16;
            mc = F & 0x0000FFFF;
            for (int k = 0; k < 4; k++) {//仓管员向四个方向走
                r = mr + dr[k];
                c = mc + dc[k];
                if (r < mMap.m_nMapTop || c < mMap.m_nMapLeft || r > mMap.m_nMapBottom || c > mMap.m_nMapRight ||
                        Mark[r-mMap.m_nMapTop][c-mMap.m_nMapLeft] || mLevel[r][c] == '#')
                    continue; //遇到墙壁或已访问或在界外

                //调整四至
                if (r1 > r) r1 = r;  //顶
                if (c1 > c) c1 = c;  //左
                if (r2 < r) r2 = r;  //底
                if (c2 < c) c2 = c;  //右

                P.add(r << 16 | c);
                Mark[r-mMap.m_nMapTop][c-mMap.m_nMapLeft] = true;  //已访问
            }
        }

        //补充必要的墙壁并去掉关卡图界外的所有元素
        int nBox = 0, nDst = 0;
        for (int i = mMap.m_nMapTop; i <= mMap.m_nMapBottom; i++) {
            for (int j = mMap.m_nMapLeft; j <= mMap.m_nMapRight; j++) {
                if (Mark[i - mMap.m_nMapTop][j - mMap.m_nMapLeft]) {  //可达区域
                    //重新计数可达区域内的箱子和目标点
                    switch (mLevel[i][j]) {
                        case '$':
                            nBox++;
                            break;
                        case '*':
                            nBox++;
                            nDst++;
                            break;
                        case '.':
                        case '+':
                            nDst++;
                            break;
                    }
                } else {  //清理非可达区域的无效元素
                    mLevel[i][j] = '-';
                    for (int k = 0; k < 8; k++) {
                        r = i + dr[k];
                        c = j + dc[k];

                        if (r < mMap.m_nMapTop || c < mMap.m_nMapLeft || r > mMap.m_nMapBottom || c > mMap.m_nMapRight) continue;

                        if (Mark[r - mMap.m_nMapTop][c - mMap.m_nMapLeft]) {
                            mLevel[i][j] = '#';
                            break;
                        }
                    }
                }
            }
        }

        //修正关卡四至
        if (r1 > 0 && mMap.m_nMapBottom-mMap.m_nMapTop+1 < myMaps.m_nMaxRow) {
            mMap.m_nMapTop = r1-1;
        }
        if (r2 < myMaps.m_nMaxRow*2-1 && mMap.m_nMapBottom-mMap.m_nMapTop+1 < myMaps.m_nMaxRow) {
            mMap.m_nMapBottom = r2+1;
        }
        if (c1 > 0 && mMap.m_nMapRight-mMap.m_nMapLeft+1 < myMaps.m_nMaxCol) {
            mMap.m_nMapLeft = c1-1;
        }
        if (c2 < myMaps.m_nMaxCol*2-1 && mMap.m_nMapRight-mMap.m_nMapLeft+1 < myMaps.m_nMaxCol) {
            mMap.m_nMapRight = c2+1;
        }

        if (nBox != nDst || nBox < 1 || nDst < 1) {  //可达区域内的箱子与目标点数不正确
            MyToast.showToast(this, "箱子或目标数不正确！\n箱子："+nBox+"，目标："+nDst, Toast.LENGTH_LONG);
        }

        return;
    }

    //接受关卡资料的修改状态
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            bt_Save.setEnabled(true);
        }
    };

    @Override
    protected void onRestart() {
        super.onRestart();
        myMaps.curMap = old_Map;
    }

    @Override
    protected void onDestroy() {
        showSystemUI();  // 显示系统的那三个按钮
        unregisterReceiver(broadcastReceiver);
//        myMaps.curMap = null;
        super.onDestroy();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            return true;

        return super.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (!bt_Save.isEnabled()) finish();
            else exitDlg.show();  //做过编辑修改，提示保存

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

////////////////////////////////////////////////////////////////////////
//辅助类定义
////////////////////////////////////////////////////////////////////////

    //ActNode 节点
    class ActNode {
        char ch;
        int row, col;
        int left, top, right, bottom;
        char[][] map;
        StringBuilder lvl;
        selNode sel1, sel2;
        Matrix mMtx, mCurMtx;
        int act;  //动作：5: 变换，90:90度（选区需要更新）
        //   1：剪切
        //   2：粘贴
        //   3，6：单点绘制，连续绘制
        //   4，9：改变尺寸，标准化
        //   7，8：剪切板导入，提交

        public ActNode(char[][] m, int l, int r, int t, int b, int m_row, int m_col, Matrix mtx0, Matrix mtx1, selNode s1, selNode s2){
            lvl = new StringBuilder();
            map = m;
            left   = l;
            top    = t;
            right  = r;
            bottom = b;
            row = m_row;
            col = m_col;
            sel1 = null;
            sel2 = null;
            mMtx = null;
            mCurMtx = null;
            act = -1;
            sel1 = s1;
            sel2 = s2;

            if (mtx0 != null) {
                mMtx = new Matrix();
                mMtx.set(mtx0);  //原始变换矩阵
            }
            if (mtx1 != null) {
                mCurMtx = new Matrix();
                mCurMtx.set(mtx1);  //当前变换矩阵
            }
        }

        public void Act(int a) {
            act = a;  //行为
            if (act == 3) {  //仅需 undo 单点
                ch = map[row+top][col+left];
                switch (ch) {
                    case '_':
                    case ' ':
                    case '#':
                    case '-':
                    case '.':
                    case '$':
                    case '*':
                    case '@':
                    case '+':
                        break;
                    default:
                        ch = '-';
                }
            } else getMap();  //undo 整体 Map
        }

        //将现场图数据拼接成字符串
        public void getMap() {
            for (int i = top; i <= bottom; i++) {
                for (int j = left; j <= right; j++) {
                    ch = map[i][j];
                    switch (ch) {
                        case '+':
                        case '@':
                        case '#':
                        case '.':
                        case '-':
                        case '$':
                        case '*':
                        case '_':
                        case ' ':
                            break;
                        default:
                            ch = '-';
                            break;
                    }
                    map[i][j] = ch;
                    lvl.append(ch);
                }
                if (i < bottom) lvl.append('\n');
            }
        }

        //undo 恢复现场
        public void setMap() {
            try {
                String[] Arr = lvl.toString().split("\r\n|\n\r|\n|\r|\\|");
                for (int i = top; i <= bottom; i++) {
                    for (int j = left; j <= right; j++) {
                        ch = Arr[i-top].charAt(j-left);
                        switch (ch) {
                            case '+':
                            case '@':
                            case '#':
                            case '.':
                            case '-':
                            case '$':
                            case '*':
                            case '_':
                            case ' ':
                                break;
                            default:
                                ch = '-';
                                break;
                        }
                        map[i][j] = ch;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            } catch (Exception e) {
            }
        }
    }
}