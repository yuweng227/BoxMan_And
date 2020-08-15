package my.boxman;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//相似查找解耗时，myGridView 调用
public class myFindFragment extends DialogFragment {

    //接口，向上层传递参数
    interface FindStatusUpdate {
        void onFindDone(ArrayList<mapNode> mlMaps);
    }

    private WeakReference<FindStatusUpdate> mStatusUpdate;
    private FindTask mFindTask;
    private String mMessage;
    private ArrayList<mapNode> m_Map_List;
    private int mSimilarity0;      //相似度
    private long[] m_sets;         //关卡集 id 数组
    private boolean m_Ans;         //是否搜索答案库
    private boolean m_Sort;        //搜索后是否按相似度排序
    private boolean m_IgnoreBox;   //搜索后是否按相似度排序

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mStatusUpdate = new WeakReference<FindStatusUpdate>((FindStatusUpdate)activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            m_sets = bundle.getLongArray("mSets");
            mSimilarity0 = bundle.getInt("mSimilarity");
            m_Ans = bundle.getBoolean("mAns");
            m_Sort = bundle.getBoolean("mSort");
            m_IgnoreBox = bundle.getBoolean("mIgnoreBox");
        } else {
            m_sets = null;
            mSimilarity0 = 100;
            m_Ans = false;
            m_Sort = false;
            m_IgnoreBox = false;
        }

        setRetainInstance(true);

        m_Map_List = null;
        mFindTask = new FindTask();
        mFindTask.execute();
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

        if (mMessage == null) mMessage = "查找中...";

        progressDialog.setMessage(mMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return progressDialog;
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        mFindTask.stopFind();
    }

    private final class FindTask extends AsyncTask<String, String, ArrayList<mapNode>> {

        void stopFind() {
            if (mStatusUpdate != null) {
                FindStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    if (mFindTask != null) {
                        if (!mFindTask.isCancelled() && mFindTask.getStatus() == AsyncTask.Status.RUNNING) {
                            mFindTask.cancel(true);
                        }
                        mFindTask = null;
                    }
                    statusUpdate.onFindDone(m_Map_List);
                }
            }
        }

        @Override
        protected void onPreExecute() {
            m_Map_List = new ArrayList<mapNode>();  //相似关卡列表
        }

        @Override
        protected ArrayList<mapNode> doInBackground(String... params) {
            try {
                String[] Arr = myMaps.oldMap.Map0.split("\r\n|\n\r|\n|\r|\\|");
                int Rows = Arr.length;
                int Cols = Arr[0].length();

                //检查 XSB 是否规整
                if (Rows < 3 || Cols < 3) throw new Exception();

                for (int r = 1; r < Rows; r++) {
                    if (Arr[r].length() != Cols) throw new Exception();
                }

                //源关卡的 8 次旋转
                char[][][] m_cAry0 = new char[4][Rows][Cols];
                char[][][] m_cAry1 = new char[4][Cols][Rows];
                char ch;
                for (int r = 0; r < Rows; r++) {
                    for (int c = 0; c < Cols; c++) {
                        ch = Arr[r].charAt(c);
                        if (ch == '@' || ch == '_' || ch == ' ') ch = '-';
                        else if (ch == '+') ch = '.';
                        m_cAry0[0][r][c] = ch;                 //0 转
                        m_cAry0[1][Rows-1-r][Cols-1-c] = ch;   //2 转
                        m_cAry0[2][r][Cols-1-c] = ch;          //4 转
                        m_cAry0[3][Rows-1-r][c] = ch;          //6 转
                        m_cAry1[0][c][Rows-1-r] = ch;          //1 转
                        m_cAry1[1][Cols-1-c][r] = ch;          //3 转
                        m_cAry1[2][Cols-1-c][Rows-1-r] = ch;   //5 转
                        m_cAry1[3][c][r] = ch;                 //7 转
                    }
                }

                //搜索关卡表中的相似关卡
                long mCount = -1;  //取得关卡数
                mapNode mMap2;
                String[] Arr2;
                int Rows2, Cols2;
                char[][] m_cAry2;

                int mTotal = Rows * Cols;  //源关卡的格子总数
                int mLeast = (mSimilarity0 == 100 ? mTotal : (int)Math.floor((double)mTotal * mSimilarity0 / 100));  //达到相似要求的最少格子数

                long n = 0;  //搜索进度
                long p_id, l_id2;
                String Map0, Title0, Author0;
                boolean flg;
                Cursor cursor = mySQLite.m_SQL.mSDB.rawQuery("PRAGMA synchronous=OFF", null);
                try {
                    if (m_sets == null) {
                        cursor = mySQLite.m_SQL.mSDB.rawQuery("select * from G_Level", null);
                    } else {
                        mySQLite.m_SQL.new_tmp_Table2();  //创建临时表，用于相似查找和查询
                        for (int k = 0; k < m_sets.length; k++)
                            mySQLite.m_SQL.add_Set_ID(m_sets[k]);

                        cursor = mySQLite.m_SQL.mSDB.rawQuery("select * from G_Level where P_id in (select P_id from id_T)", null);
                    }
                    while (cursor.moveToNext()){
                        if (isCancelled()) break;

                        try {
                            p_id = cursor.getLong(cursor.getColumnIndex("P_id"));
                            l_id2 = cursor.getLong(cursor.getColumnIndex("L_id"));
                            Title0 = cursor.getString(cursor.getColumnIndex("L_Title"));
                            Author0 = cursor.getString(cursor.getColumnIndex("L_Author"));
                            Map0 = cursor.getString(cursor.getColumnIndex("L_thin_XSB"));  //标准化关卡

                            n++;  //搜索进度
                            if (mCount < 0) mCount = cursor.getCount();
                            publishProgress("查找中... " + n + "/" + mCount + '\n' + myMaps.getSetTitle(p_id) + '\n' + Title0 + '\n' + Author0);

                            if (myMaps.oldMap.Level_id == l_id2) continue;

                            Arr2 = Map0.split("\r\n|\n\r|\n|\r|\\|");
                        } catch (Exception e) {
                            continue;
                        }

                        Rows2 = Arr2.length;
                        Cols2 = Arr2[0].length();

                        //检查 XSB 是否规整
                        if (Rows2 < 3 || Cols2 < 3) continue;

                        flg = false;
                        for (int r = 1; r < Rows2; r++) {
                            if (Arr2[r].length() != Cols2) {
                                flg = true;
                                break;
                            }
                        }
                        if (flg) continue;

                        //DB 中的关卡
                        m_cAry2 = new char[Rows2][Cols2];
                        for (int r = 0; r < Rows2; r++) {
                            for (int c = 0; c < Cols2; c++) {
                                ch = Arr2[r].charAt(c);
                                if (ch == '@' || ch == '_' || ch == ' ') ch = '-';
                                else if (ch == '+') ch = '.';

                                m_cAry2[r][c] = ch;
                            }
                        }
                        if (m_Sort) {
                            int mSimilarity1 = 0, mSimilarity2;

                            mSimilarity2 = myCompare(mLeast, m_cAry0[0], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry0[1], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry0[2], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry0[3], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[0], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[1], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[2], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[3], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            if (mSimilarity1 > 0) {  //说明相似率达到要求
                                mMap2 = new mapNode(mSimilarity1,
                                        cursor.getLong(cursor.getColumnIndex("P_id")),
                                        cursor.getLong(cursor.getColumnIndex("L_id")),
                                        cursor.getInt(cursor.getColumnIndex("L_Solved")),
                                        cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
                                        cursor.getString(cursor.getColumnIndex("L_Title")),
                                        cursor.getString(cursor.getColumnIndex("L_Author")),
                                        cursor.getString(cursor.getColumnIndex("L_Comment")),
                                        cursor.getLong(cursor.getColumnIndex("L_Key")),
                                        cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
                                        cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
                                        cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标

                                m_Map_List.add(mMap2);
                            }
                        } else {
                            if (myCompare(mLeast, m_cAry0[0], Rows, Cols, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry0[1], Rows, Cols, m_cAry2, Rows2, Cols2)  > 0||
                                    myCompare(mLeast, m_cAry0[2], Rows, Cols, m_cAry2, Rows2, Cols2)  > 0||
                                    myCompare(mLeast, m_cAry0[3], Rows, Cols, m_cAry2, Rows2, Cols2)  > 0||
                                    myCompare(mLeast, m_cAry1[0], Cols, Rows, m_cAry2, Rows2, Cols2)  > 0||
                                    myCompare(mLeast, m_cAry1[1], Cols, Rows, m_cAry2, Rows2, Cols2)  > 0||
                                    myCompare(mLeast, m_cAry1[2], Cols, Rows, m_cAry2, Rows2, Cols2)  > 0||
                                    myCompare(mLeast, m_cAry1[3], Cols, Rows, m_cAry2, Rows2, Cols2)  > 0) {

                                mMap2 = new mapNode(0,
                                        cursor.getLong(cursor.getColumnIndex("P_id")),
                                        cursor.getLong(cursor.getColumnIndex("L_id")),
                                        cursor.getInt(cursor.getColumnIndex("L_Solved")),
                                        cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
                                        cursor.getString(cursor.getColumnIndex("L_Title")),
                                        cursor.getString(cursor.getColumnIndex("L_Author")),
                                        cursor.getString(cursor.getColumnIndex("L_Comment")),
                                        cursor.getLong(cursor.getColumnIndex("L_Key")),
                                        cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
                                        cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
                                        cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标

                                m_Map_List.add(mMap2);
                            }
                        }
                    }
                } catch (Exception e) {
                } finally {
                    if (cursor != null) cursor.close();
                    mySQLite.m_SQL.del_tmp_Table2();  //清理暂存关卡集 ID 临时表
                }

                if (!m_Ans) throw new Exception();  //若不搜索答案库，用抛出异常结束任务

                try {
                    //搜索答案表中的相似关卡
                    n = 0;
                    mCount = -1;
                    cursor = mySQLite.m_SQL.mSDB.rawQuery("select P_Key, P_Key_Num, L_thin_XSB, S_id from G_State where G_Solution = 1 and P_Key_Num >= 0 and P_Key not in (select L_Key from G_Level) group by P_Key", null);
                    while (cursor.moveToNext()) {
                        if (isCancelled()) break;

                        n++;  //搜索进度
                        if (mCount < 0) mCount = cursor.getCount();
                        publishProgress("查找中... " + n + "/" + mCount + "\n答案库");

                        try {
                            Map0 = cursor.getString(cursor.getColumnIndex("L_thin_XSB"));  //标准化关卡
                            Arr2 = Map0.split("\r\n|\n\r|\n|\r|\\|");
                        } catch (Exception e) {
                            continue;
                        }

                        Rows2 = Arr2.length;
                        Cols2 = Arr2[0].length();

                        //检查 XSB 是否规整
                        if (Rows2 < 3 || Cols2 < 3) continue;

                        flg = false;
                        for (int r = 1; r < Rows2; r++) {
                            if (Arr2[r].length() != Cols2) {
                                flg = true;
                                break;
                            }
                        }
                        if (flg ) continue;

                        //答案库中的关卡
                        m_cAry2 = new char[Rows2][Cols2];
                        for (int r = 0; r < Rows2; r++) {
                            for (int c = 0; c < Cols2; c++) {
                                ch = Arr2[r].charAt(c);
                                if (ch == '@' || ch == '_' || ch == ' ') ch = '-';
                                else if (ch == '+') ch = '.';

                                m_cAry2[r][c] = ch;
                            }
                        }
                        if (m_Sort) {
                            int mSimilarity1 = 0, mSimilarity2;

                            mSimilarity2 = myCompare(mLeast, m_cAry0[0], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry0[1], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry0[2], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry0[3], Rows, Cols, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[0], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[1], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[2], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            mSimilarity2 = myCompare(mLeast, m_cAry1[3], Cols, Rows, m_cAry2, Rows2, Cols2);
                            if (mSimilarity2 > mSimilarity1) mSimilarity1 = mSimilarity2;  //使用关卡节点的序号自段暂存相似率

                            if (mSimilarity1 > 0) {  //说明相似率达到要求
                                mMap2 = new mapNode(mSimilarity1,
                                        -1,  //P_id
                                        cursor.getLong(cursor.getColumnIndex("S_id")),
                                        1,  //是否有答案
                                        cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //关卡 XSB
                                        "",
                                        "",
                                        "",
                                        cursor.getLong(cursor.getColumnIndex("P_Key")),
                                        cursor.getInt(cursor.getColumnIndex("P_Key_Num")),  //第几转
                                        cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
                                        0);  //是否加锁图标
                                m_Map_List.add(mMap2);
                            }
                        } else {
                            if (myCompare(mLeast, m_cAry0[0], Rows, Cols, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry0[1], Rows, Cols, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry0[2], Rows, Cols, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry0[3], Rows, Cols, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry1[0], Cols, Rows, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry1[1], Cols, Rows, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry1[2], Cols, Rows, m_cAry2, Rows2, Cols2) > 0||
                                    myCompare(mLeast, m_cAry1[3], Cols, Rows, m_cAry2, Rows2, Cols2) > 0) {

                                mMap2 = new mapNode(0,
                                        -1,  //P_id
                                        cursor.getLong(cursor.getColumnIndex("S_id")),
                                        1,  //是否有答案
                                        cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //关卡 XSB
                                        "",
                                        "",
                                        "",
                                        cursor.getLong(cursor.getColumnIndex("P_Key")),
                                        cursor.getInt(cursor.getColumnIndex("P_Key_Num")),  //第几转
                                        cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
                                        0);  //是否加锁图标
                                m_Map_List.add(mMap2);
                            }
                        }
                    }
                } catch (Exception e) {
                } finally {
                    if (cursor != null) cursor.close();
                }

            } catch (Exception e) {
            }

            if (m_Sort) {  // 若结果需要排序
                Collections.sort(m_Map_List, new Comparator<mapNode>() {
                    @Override
                    public int compare(mapNode nd1, mapNode nd2) {
                        if (nd1.Num > nd2.Num) return -1;
                        else if (nd1.Num < nd2.Num) return 1;
                        else return 0;
                    }
                });
                return m_Map_List;
            } else {
                return m_Map_List;
            }
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mMessage = progress[0];
            ProgressDialog progressDialog = ((ProgressDialog) myFindFragment.this.getDialog());
            if (progressDialog != null) {
                progressDialog.setMessage(mMessage);
            }
        }
        @Override
        protected void onPostExecute(ArrayList<mapNode> result) {
            if (mStatusUpdate != null) {
                FindStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    statusUpdate.onFindDone(result);
                }
            }
        }

        //计数两个关卡的相同格子数（找到两个关卡的重叠区域并比对，重叠区域需要“晃动”），mLeast -- 相似率要求的最少相同格子数
        private int myCompare(int mLeast, char[][] mAry1, int Rows1,int Cols1, char[][] mAry2, int Rows2,int Cols2) {
            int m, m2, n, n2, dR1, dC1, dR2, dC2;
            char ch1, ch2;

            if (Rows1 < Rows2) m = Rows1;
            else m = Rows2;
            if (Cols1 < Cols2) n = Cols1;
            else n = Cols2;

            //参加对比的格子数不够，不需比对
            if (m * n < mLeast) return 0;

            dR1 = Rows1 - Rows2;
            dC1 = Cols1 - Cols2;
            dR2 = Rows2 - Rows1;
            dC2 = Cols2 - Cols1;

            m2 = Rows1 * Cols1 - mLeast;  //允许的最大不同的格子数
            m = 0;  //记录相同的格子总数的最大值
            if (Rows1 < Rows2) {
                if (Cols1 < Cols2) {
                    for (int i = 0; i <= dR2; i++) {
                        for (int j = 0; j <= dC2; j++) {
                            n = 0;
                            n2 = 0;
                            for (int r = 0; r < Rows1; r++) {
                                for (int c = 0; c < Cols1; c++) {

                                    ch1 = mAry1[r][c];
                                    ch2 = mAry2[r + i][c + j];

                                    if (m_IgnoreBox) {
                                        if (ch1 == '$' || ch1 == '@') ch1 = '-';
                                        else if (ch1 == '*' || ch1 == '+') ch1 = '.';
                                        if (ch2 == '$' || ch2 == '@') ch2 = '-';
                                        else if (ch2 == '*' || ch2 == '+') ch2 = '.';
                                    }
                                    if (ch1 == ch2) n++;
                                    else n2++;

                                    if (m_Sort) {
                                        if (m < n) m = n;
                                    } else {
                                        if (n >= mLeast) return 1;
                                    }
                                    if (n2 > m2) break;
                                }
                                if (n2 > m2) break;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i <= dR2; i++) {
                        for (int j = 0; j <= dC1; j++) {
                            n = 0;
                            n2 = 0;
                            for (int r = 0; r < Rows1; r++) {
                                for (int c = 0; c < Cols2; c++) {

                                    ch1 = mAry1[r][c + j];
                                    ch2 = mAry2[r + i][c];

                                    if (m_IgnoreBox) {
                                        if (ch1 == '$' || ch1 == '@') ch1 = '-';
                                        else if (ch1 == '*' || ch1 == '+') ch1 = '.';
                                        if (ch2 == '$' || ch2 == '@') ch2 = '-';
                                        else if (ch2 == '*' || ch2 == '+') ch2 = '.';
                                    }
                                    if (ch1 == ch2) n++;
                                    else n2++;
//                                    if (mAry1[r][c + j] == mAry2[r + i][c]) n++;
//                                    else n2++;

                                    if (m_Sort) {
                                        if (m < n) m = n;
                                    } else {
                                        if (n >= mLeast) return 1;
                                    }
                                    if (n2 > m2) break;
                                }
                                if (n2 > m2) break;
                            }
                        }
                    }
                }
            } else {
                if (Cols1 < Cols2) {
                    for (int i = 0; i <= dR1; i++) {
                        for (int j = 0; j <= dC2; j++) {
                            n = 0;
                            n2 = 0;
                            for (int r = 0; r < Rows2; r++) {
                                for (int c = 0; c < Cols1; c++) {

                                    ch1 = mAry1[r + i][c];
                                    ch2 = mAry2[r][c + j];

                                    if (m_IgnoreBox) {
                                        if (ch1 == '$' || ch1 == '@') ch1 = '-';
                                        else if (ch1 == '*' || ch1 == '+') ch1 = '.';
                                        if (ch2 == '$' || ch2 == '@') ch2 = '-';
                                        else if (ch2 == '*' || ch2 == '+') ch2 = '.';
                                    }
                                    if (ch1 == ch2) n++;
                                    else n2++;
//                                    if (mAry1[r + i][c] == mAry2[r][c + j]) n++;
//                                    else n2++;

                                    if (m_Sort) {
                                        if (m < n) m = n;
                                    } else {
                                        if (n >= mLeast) return 1;
                                    }
                                    if (n2 > m2) break;
                                }
                                if (n2 > m2) break;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i <= dR1; i++) {
                        for (int j = 0; j <= dC1; j++) {
                            n = 0;
                            n2 = 0;
                            for (int r = 0; r < Rows2; r++) {
                                for (int c = 0; c < Cols2; c++) {

                                    ch1 = mAry1[r + i][c + j];
                                    ch2 = mAry2[r][c];

                                    if (m_IgnoreBox) {
                                        if (ch1 == '$' || ch1 == '@') ch1 = '-';
                                        else if (ch1 == '*' || ch1 == '+') ch1 = '.';
                                        if (ch2 == '$' || ch2 == '@') ch2 = '-';
                                        else if (ch2 == '*' || ch2 == '+') ch2 = '.';
                                    }
                                    if (ch1 == ch2) n++;
                                    else n2++;
//                                    if (mAry1[r + i][c + j] == mAry2[r][c]) n++;
//                                    else n2++;

                                    if (m_Sort) {
                                        if (m < n) m = n;
                                    } else {
                                        if (n >= mLeast) return 1;
                                    }
                                    if (n2 > m2) break;
                                }
                                if (n2 > m2) break;
                            }
                        }
                    }
                }
            }
            if (m_Sort && m >= mLeast) {
                return (int)Math.floor((double) m  * 100 / (Rows1 * Cols1));
            } else {
                return 0;
            }
        }
    }
}