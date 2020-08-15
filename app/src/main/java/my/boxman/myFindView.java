package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;

public class myFindView extends Activity {

    myFindViewMap mMap;  //地图
    Menu myMenu = null;

    char[][] m_cArray1;  //源关卡 -- 全貌
    char[][] m_cArray2;  //相似关卡 -- 全貌
    char[][] m_cArray3;  //源关卡 -- 标准化
    char[][] m_cArray4;  //相似关卡 -- 标准化

    boolean m_Level;  //当前显示的关卡是否为源关卡

    String m_Set_Pos1;  //源关卡所属关卡集位置信息
    String m_Set_Pos2;  //相似关卡所属关卡集位置信息
    int Rows1, Cols1, Rows2, Cols2, Rows3, Cols3, Rows4, Cols4;  //关卡原貌及标准化关卡的原始尺寸

    int mTrun;  //源关卡 n 转相似度最高
    int mSimilarity;  //精准相似度设定
    int[][] mSelect;  //相似区域，用于绘制关卡图中的相似区域方框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.find_view);

        //设置标题栏标题为关卡集名
        setTitle(myMaps.sFile);

        //开启标题栏的返回键
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);

        mMap = (myFindViewMap) this.findViewById(R.id.findMapView);
        mMap.Init(this);

        mSelect = new int[2][4];  //记录两个关卡的相似区域

        getLevel_inf();  //取得两个关卡，及其位置信息
    }

    //取得关卡相关资料
    private void getLevel_inf() {

        try {
            //重新计算比较精确的相似度  ****************
            String[] Arr = myMaps.oldMap.Map0.split("\r\n|\n\r|\n|\r|\\|");
            Rows1 = Arr.length;
            Cols1 = Arr[0].length();

            //源关卡的 8 次旋转
            char[][][] m_cAry0 = new char[4][Rows1][Cols1];
            char[][][] m_cAry1 = new char[4][Cols1][Rows1];
            char ch;
            for (int r = 0; r < Rows1; r++) {
                for (int c = 0; c < Cols1; c++) {
                    ch = Arr[r].charAt(c);
                    if (ch == '_' || ch == ' ') ch = '-';
                    m_cAry0[0][r][c] = ch;                  //0 转
                    m_cAry1[0][c][Rows1-1-r] = ch;          //1 转
                    m_cAry0[1][Rows1-1-r][Cols1-1-c] = ch;  //2 转
                    m_cAry1[1][Cols1-1-c][r] = ch;          //3 转
                    m_cAry0[2][r][Cols1-1-c] = ch;          //4 转
                    m_cAry1[2][Cols1-1-c][Rows1-1-r] = ch;  //5 转
                    m_cAry0[3][Rows1-1-r][c] = ch;          //6 转
                    m_cAry1[3][c][r] = ch;                  //7 转
                }
            }

            //相似关卡的标准化 XSB  ********************
            Arr = myMaps.curMap.Map0.split("\r\n|\n\r|\n|\r|\\|");
            Rows2 = Arr.length;
            Cols2 = Arr[0].length();

            //相似关卡的 0 转
            char[][] m_cAry2 = new char[Rows2][Cols2];
            for (int r = 0; r < Rows2; r++) {
                for (int c = 0; c < Cols2; c++) {
                    ch = Arr[r].charAt(c);
                    if (ch == '@' || ch == '_' || ch == ' ') ch = '-';
                    else if (ch == '+') ch = '.';

                    m_cAry2[r][c] = ch;
                }
            }

            //计算两个关卡的精确相似度及相似区域  ********************
            mSimilarity = 0;
            int[][] sel = new int[2][4];  //记录两个关卡的相似区域
            int mSimilarity0 = myCompare(m_cAry0[0], Rows1, Cols1, m_cAry2, Rows2, Cols2, sel);  //计算两个关卡的精确相似度、同时记录相似区域
            if (mSimilarity < mSimilarity0) {
                mTrun = 0;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }
            mSimilarity0 = myCompare(m_cAry0[1], Rows1, Cols1, m_cAry2, Rows2, Cols2, sel);
            if (mSimilarity < mSimilarity0) {
                mTrun = 2;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }
            mSimilarity0 = myCompare(m_cAry0[2], Rows1, Cols1, m_cAry2, Rows2, Cols2, sel);
            if (mSimilarity < mSimilarity0) {
                mTrun = 4;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }
            mSimilarity0 = myCompare(m_cAry0[3], Rows1, Cols1, m_cAry2, Rows2, Cols2, sel);
            if (mSimilarity < mSimilarity0) {
                mTrun = 6;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }
            mSimilarity0 = myCompare(m_cAry1[0], Cols1, Rows1, m_cAry2, Rows2, Cols2, sel);
            if (mSimilarity < mSimilarity0) {
                mTrun = 1;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }
            mSimilarity0 = myCompare(m_cAry1[1], Cols1, Rows1, m_cAry2, Rows2, Cols2, sel);
            if (mSimilarity < mSimilarity0) {
                mTrun = 3;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }
            mSimilarity0 = myCompare(m_cAry1[2], Cols1, Rows1, m_cAry2, Rows2, Cols2, sel);
            if (mSimilarity < mSimilarity0) {
                mTrun = 5;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }
            mSimilarity0 = myCompare(m_cAry1[3], Cols1, Rows1, m_cAry2, Rows2, Cols2, sel);
            if (mSimilarity < mSimilarity0) {
                mTrun = 7;
                mSimilarity = mSimilarity0;

                //相似区域的坐标调整到 0 转坐标
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        mSelect[i][j] = sel[i][j];  //mSelect[2][4] 用于绘制关卡图中的相似区域方框
                    }
                }
            }

            //源关卡  ******************
            if (myMaps.oldMap.P_id >= 0) {  //创编关卡，不需要记录打开时间
                mySQLite.m_SQL.Set_L_DateTime(myMaps.oldMap.Level_id);  //记录源关卡打开时间
            }

            String m_Set_Name1;
            if (myMaps.oldMap.P_id < 0) {
                m_Set_Pos1 = "关卡集：创编关卡，序号：" + myMaps.oldMap.Num;  //源关卡所属关卡集位置信息
            } else {
                m_Set_Name1 = myMaps.getSetTitle(myMaps.oldMap.P_id);  //源关卡所属关卡集
                int m_Level_Num1 = mySQLite.m_SQL.get_Level_Num(myMaps.oldMap.P_id, myMaps.oldMap.Level_id);  //源关卡在所属关卡集中的序号
                m_Set_Pos1 = "关卡集：" + m_Set_Name1 + "，序号：" + m_Level_Num1;  //源关卡所属关卡集位置信息
            }

            //源关卡全貌
            Arr = myMaps.oldMap.Map.split("\r\n|\n\r|\n|\r|\\|");
            Rows1 = Arr.length;
            Cols1 = Arr[0].length();

            if (mTrun % 2 == 0) m_cArray1 = new char[Rows1][Cols1];
            else m_cArray1 = new char[Cols1][Rows1];
            for (int r = 0; r < Rows1; r++) {
                for (int c = 0; c < Cols1; c++) {
                    switch (mTrun) {
                        case 0:
                            m_cArray1[r][c] = Arr[r].charAt(c);
                            break;
                        case 1:
                            m_cArray1[c][Rows1-1-r] = Arr[r].charAt(c);
                            break;
                        case 2:
                            m_cArray1[Rows1-1-r][Cols1-1-c] = Arr[r].charAt(c);
                            break;
                        case 3:
                            m_cArray1[Cols1-1-c][r] = Arr[r].charAt(c);
                            break;
                        case 4:
                            m_cArray1[r][Cols1-1-c] = Arr[r].charAt(c);
                            break;
                        case 5:
                            m_cArray1[Cols1-1-c][Rows1-1-r] = Arr[r].charAt(c);
                            break;
                        case 6:
                            m_cArray1[Rows1-1-r][c] = Arr[r].charAt(c);
                            break;
                        case 7:
                            m_cArray1[c][r] = Arr[r].charAt(c);
                            break;
                    }
                }
            }
            if (mTrun % 2 == 1) {
                int n = Rows1;
                Rows1 = Cols1;
                Cols1 = n;
            }

            //原关卡标准化
            switch (mTrun) {
                case 0:
                    m_cArray3 = m_cAry0[0];
                    break;
                case 1:
                    m_cArray3 = m_cAry1[0];
                    break;
                case 2:
                    m_cArray3 = m_cAry0[1];
                    break;
                case 3:
                    m_cArray3 = m_cAry1[1];
                    break;
                case 4:
                    m_cArray3 = m_cAry0[2];
                    break;
                case 5:
                    m_cArray3 = m_cAry1[2];
                    break;
                case 6:
                    m_cArray3 = m_cAry0[3];
                    break;
                case 7:
                    m_cArray3 = m_cAry1[3];
                    break;
            }
            Rows3 = m_cArray3.length;
            Cols3 = m_cArray3[0].length;

            //相似关卡  *******************
            if (myMaps.oldMap.P_id >= 0) {  //答案表中的关卡，不需要记录打开时间
                mySQLite.m_SQL.Set_L_DateTime(myMaps.curMap.Level_id);  //记录相似关卡打开时间
            }

            String m_Set_Name2 = mySQLite.m_SQL.getSetName(myMaps.curMap.P_id);  //相似关卡所属关卡集
            if (myMaps.curMap.P_id >= 0) {
                int m_Level_Num2 = mySQLite.m_SQL.get_Level_Num(myMaps.curMap.P_id, myMaps.curMap.Level_id);  //相似关卡在所属关卡集中的序号
                m_Set_Pos2 = "关卡集：" + m_Set_Name2 + "，序号：" + m_Level_Num2;  //相似关卡所属关卡集位置信息
            } else {  //答案表中的关卡，没有序号
                m_Set_Pos2 = "关卡集：无，自由关卡";
            }

            //相似关卡全貌
            Arr = myMaps.curMap.Map.split("\r\n|\n\r|\n|\r|\\|");
            Rows2 = Arr.length;
            Cols2 = Arr[0].length();

            m_cArray2 = new char[Rows2][Cols2];
            for (int r = 0; r < Rows2; r++) {
                for (int c = 0; c < Cols2; c++) {
                    m_cArray2[r][c] = Arr[r].charAt(c);
                }
            }

            //标准化 XSB -- 可看到相似区域
            Arr = myMaps.curMap.Map0.split("\r\n|\n\r|\n|\r|\\|");
            Rows4 = Arr.length;
            Cols4 = Arr[0].length();

            m_cArray4 = new char[Rows4][Cols4];
            for (int r = 0; r < Rows4; r++) {
                for (int c = 0; c < Cols4; c++) {
                    m_cArray4[r][c] = Arr[r].charAt(c);
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            finish();
            MyToast.showToast(this, "关卡数据不完整！", Toast.LENGTH_SHORT);
        } catch (Exception e) {
            finish();
            MyToast.showToast(this, "关卡数据不完整！", Toast.LENGTH_SHORT);
        }
        mMap.m_Level_All = false;
        if (myMenu != null) myMenu.getItem(5).setChecked(mMap.m_Level_All);
        if (mMap.m_Level_All) {  //关卡全貌
            if (m_Level) {
                mMap.m_cArray = m_cArray1;  //源关卡 -- 全貌
                mMap.m_nRows = Rows1;
                mMap.m_nCols = Cols1;
            } else {
                mMap.m_cArray = m_cArray2;  //相似关卡 -- 全貌
                mMap.m_nRows = Rows2;
                mMap.m_nCols = Cols2;
            }
        } else {   //瘦关卡
            if (m_Level) {
                mMap.m_cArray = m_cArray3;  //源关卡 -- 瘦
                mMap.m_nRows = Rows3;
                mMap.m_nCols = Cols3;
            } else {
                mMap.m_cArray = m_cArray4;  //相似关卡 -- 瘦
                mMap.m_nRows = Rows4;
                mMap.m_nCols = Cols4;
            }
        }
        myMaps.m_nTrun = 0;
        m_Level = false;  //默认显示相似关卡

        //舞台初始化
        mMap.initArena();
    }

    //计算精准相似度
    private int myCompare(char[][] mAry1, int Rows1,int Cols1, char[][] mAry2, int Rows2,int Cols2, int[][] Sel) {
        int m, n, dR1, dC1, dR2, dC2;
        int mLeast_Similarity[] = {100, 95, 90, 85, 80, 75, 66, 50};  //需参照：myGridView.onContextItemSelected()中的定义

        if (Rows1 < Rows2) m = Rows1;
        else m = Rows2;
        if (Cols1 < Cols2) n = Cols1;
        else n = Cols2;

        //参加对比的格子不够数，不需比较
        if ((int)Math.ceil((double) m * n  * 100 / (Rows1 * Cols1)) < mLeast_Similarity[myMaps.m_Sets[26]]) return 0;

        dR1 = Rows1 - Rows2;
        dC1 = Cols1 - Cols2;
        dR2 = Rows2 - Rows1;
        dC2 = Cols2 - Cols1;

        m = 0;  //记录相同的格子总数的最大值
        char ch;
        if (Rows1 < Rows2) {
            if (Cols1 < Cols2) {
                for (int i = 0; i <= dR2; i++) {
                    for (int j = 0; j <= dC2; j++) {
                        n = 0;
                        for (int r = 0; r < Rows1; r++) {
                            for (int c = 0; c < Cols1; c++) {
                                ch = mAry1[r][c];
                                if (ch == '@') ch = '-';
                                else if (ch == '+') ch = '.';
                                if (ch == mAry2[r + i][c + j]) n++;
                            }
                        }
                        if (m < n) {
                            m = n;
                            Sel[0][0] = 0;
                            Sel[0][1] = 0;
                            Sel[0][2] = Rows1 - 1;
                            Sel[0][3] = Cols1 - 1;
                            Sel[1][0] = i;
                            Sel[1][1] = j;
                            Sel[1][2] = i + Rows1 - 1;
                            Sel[1][3] = j + Cols1 - 1;
                        }
                    }
                }
            } else {
                for (int i = 0; i <= dR2; i++) {
                    for (int j = 0; j <= dC1; j++) {
                        n = 0;
                        for (int r = 0; r < Rows1; r++) {
                            for (int c = 0; c < Cols2; c++) {
                                ch = mAry1[r][c + j];
                                if (ch == '@') ch = '-';
                                else if (ch == '+') ch = '.';
                                if (ch == mAry2[r + i][c]) n++;
                            }
                        }
                        if (m < n) {
                            m = n;
                            Sel[0][0] = 0;
                            Sel[0][1] = j;
                            Sel[0][2] = Rows1 - 1;
                            Sel[0][3] = j + Cols2 - 1;
                            Sel[1][0] = i;
                            Sel[1][1] = 0;
                            Sel[1][2] = i + Rows1 - 1;
                            Sel[1][3] = Cols2 - 1;
                        }
                    }
                }
            }
        } else {
            if (Cols1 < Cols2) {
                for (int i = 0; i <= dR1; i++) {
                    for (int j = 0; j <= dC2; j++) {
                        n = 0;
                        for (int r = 0; r < Rows2; r++) {
                            for (int c = 0; c < Cols1; c++) {
                                ch = mAry1[r + i][c];
                                if (ch == '@') ch = '-';
                                else if (ch == '+') ch = '.';
                                if (ch == mAry2[r][c + j]) n++;
                            }
                        }
                        if (m < n) {
                            m = n;
                            Sel[0][0] = i;
                            Sel[0][1] = 0;
                            Sel[0][2] = i + Rows2 - 1;
                            Sel[0][3] = Cols1 - 1;
                            Sel[1][0] = 0;
                            Sel[1][1] = j;
                            Sel[1][2] = Rows2 - 1;
                            Sel[1][3] = j + Cols1 - 1;
                        }
                    }
                }
            } else {
                for (int i = 0; i <= dR1; i++) {
                    for (int j = 0; j <= dC1; j++) {
                        n = 0;
                        for (int r = 0; r < Rows2; r++) {
                            for (int c = 0; c < Cols2; c++) {
                                ch = mAry1[r + i][c + j];
                                if (ch == '@') ch = '-';
                                else if (ch == '+') ch = '.';
                                if (ch == mAry2[r][c]) n++;
                            }
                        }
                        if (m < n) {
                            m = n;
                            Sel[0][0] = i;
                            Sel[0][1] = j;
                            Sel[0][2] = i + Rows2 - 1;
                            Sel[0][3] = j + Cols2 - 1;
                            Sel[1][0] = 0;
                            Sel[1][1] = 0;
                            Sel[1][2] = Rows2 - 1;
                            Sel[1][3] = Cols2 - 1;
                        }
                    }
                }
            }
        }

        return (int)Math.floor((double) m  * 100 / (Rows1 * Cols1));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.find, menu);

        myMenu = menu;
        myMenu.getItem(5).setChecked(mMap.m_Level_All);

        return true;
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

    public boolean onOptionsItemSelected(MenuItem mt) {
        switch (mt.getItemId()) {
            case android.R.id.home:  //标题栏返回键功能
                this.finish();

                return true;
            case R.id.find_pre:  //上一关卡
                int n1 = myMaps.m_lstMaps.indexOf(myMaps.curMap);
                if (n1 > 0) {
                    myLoadLevel(n1-1);
                }

                return true;
            case R.id.find_next:  //下一关卡
                int n2 = myMaps.m_lstMaps.indexOf(myMaps.curMap);
                if (n2 >= 0 && n2+1 < myMaps.m_lstMaps.size()) {
                    myLoadLevel(n2+1);
                }

                return true;
            case R.id.find_level:  //源关卡与相似关卡切换
                myLevel();

                return true;
            case R.id.find_trun:  //旋转关卡
                myTrun();

                return true;
            case R.id.find_solution:  //答案
                if (!m_Level && myMaps.curMap.Solved) {  //相似关卡，且有答案
                    mySQLite.m_SQL.load_SolitionList(myMaps.curMap.key);  //加载答案列表
                    myMaps.curMap.Solved = (myMaps.mState2.size() > 0); //修正相似关卡预览图之是否有答案

                    Collections.sort(myMaps.mState1, new Comparator() {
                        @Override
                        public int compare(Object o1, Object o2) {
                            return ((state_Node) o2).time.compareTo(((state_Node) o1).time);
                        }
                    });
                    myMaps.m_StateIsRedy = false;
                    Intent intent1 = new Intent();
                    intent1.setClass(this, mySolutionBrow.class);
                    startActivity(intent1);
                } else {
                    if (myMaps.curMap.Solved)
                        MyToast.showToast(this, "请先切换到相似关卡！", Toast.LENGTH_SHORT);
                    else
                        MyToast.showToast(this, "相似关卡也没有答案！", Toast.LENGTH_SHORT);
                }
                return true;
            case R.id.find_level_all:  //关卡全貌
                mMap.m_Level_All = !mMap.m_Level_All;
                mt.setChecked(mMap.m_Level_All);
                if (mMap.m_Level_All) {  //关卡全貌
                    if (m_Level) {
                        mMap.m_cArray = m_cArray1;  //源关卡 -- 全貌
                        mMap.m_nRows = Rows1;
                        mMap.m_nCols = Cols1;
                    } else {
                        mMap.m_cArray = m_cArray2;  //相似关卡 -- 全貌
                        mMap.m_nRows = Rows2;
                        mMap.m_nCols = Cols2;
                    }
                } else {   //瘦关卡
                    if (m_Level) {
                        mMap.m_cArray = m_cArray3;  //源关卡 -- 瘦
                        mMap.m_nRows = Rows3;
                        mMap.m_nCols = Cols3;
                    } else {
                        mMap.m_cArray = m_cArray4;  //相似关卡 -- 瘦
                        mMap.m_nRows = Rows4;
                        mMap.m_nCols = Cols4;
                    }
                }
                mMap.initArena();
                mMap.invalidate();

                return true;
            case R.id.find_about:  //关卡信息
                Intent intent1 = new Intent();
                intent1.setClass(this, myAbout2.class);
                startActivity(intent1);

                return true;
            default:
                return super.onOptionsItemSelected(mt);
        }
    }

    //为消除音量键的按键音
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (myMaps.m_Sets[15] == 1 && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
            return true;

        return super.onKeyUp(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            finish();
            return true;
        } else if (myMaps.m_Sets[15] == 1 && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            int n1 = myMaps.m_lstMaps.indexOf(myMaps.curMap);
            if (n1 > 0) {
                //指向上一关卡
                myLoadLevel(n1-1);
            }
            return true;
        } else if (myMaps.m_Sets[15] == 1 && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            int n2 = myMaps.m_lstMaps.indexOf(myMaps.curMap);
            if (n2 >= 0 && n2+1 < myMaps.m_lstMaps.size()) {
                //指向下一关卡
                myLoadLevel(n2+1);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //切换源关卡与相似关卡
    public void myLevel() {
        m_Level = !m_Level;

        if (m_Level) {
            if (mMap.m_Level_All) {  //关卡全貌
                mMap.m_cArray = m_cArray1;  //源关卡 -- 全貌
                mMap.m_nRows = Rows1;
                mMap.m_nCols = Cols1;
            } else {
                mMap.m_cArray = m_cArray3;  //源关卡 -- 标准化
                mMap.m_nRows = Rows3;
                mMap.m_nCols = Cols3;
            }
        } else {
            if (mMap.m_Level_All) {  //关卡全貌
                mMap.m_cArray = m_cArray2;  //相似关卡 -- 全貌
                mMap.m_nRows = Rows2;
                mMap.m_nCols = Cols2;
            } else {
                mMap.m_cArray = m_cArray4;  //相似关卡 -- 标准化
                mMap.m_nRows = Rows4;
                mMap.m_nCols = Cols4;
            }
        }

        //舞台初始化
        mMap.initArena();
        mMap.invalidate();
    }

    //旋转关卡
    public void myTrun() {
        myMaps.m_nTrun = (myMaps.m_nTrun + 1) % 2;

        mMap.initArena();
        mMap.invalidate();
    }

    //加载新的相似关卡
    private void myLoadLevel(int n) {
        //指向下一关卡
        myMaps.curMap = null;
        myMaps.curMap = myMaps.m_lstMaps.get(n);

        getLevel_inf();  //取得两个关卡，及其位置信息
    }
}
