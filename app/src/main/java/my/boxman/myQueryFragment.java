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

//查询解耗时，BoxMan 调用
public class myQueryFragment extends DialogFragment {

    //接口，向上层传递参数
    interface FindStatusUpdate {
        void onQueryDone(ArrayList<mapNode> mlMaps);
    }

    private WeakReference<FindStatusUpdate> mStatusUpdate;
    private FindTask mFindTask;
    private String mMessage;
    private ArrayList<mapNode> m_Map_List;
    private String mTitle, mAuthor;  //标题，作者
    private boolean m_Ans;     //是否搜索答案库
    private int mBoxs, mCols, mRows, mBoxs2, mCols2, mRows2;  //箱子数，列数，行数
    private long[] m_sets;     //关卡集 id 数组

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
            m_Ans = bundle.getBoolean("mAns");
            mTitle = bundle.getString("mTitle").toUpperCase();
            mAuthor = bundle.getString("mAauthor").toUpperCase();
            mBoxs = bundle.getInt("mBoxs");
            mCols = bundle.getInt("mCols");
            mRows = bundle.getInt("mRows");
            mBoxs2 = bundle.getInt("mBoxs2");
            mCols2 = bundle.getInt("mCols2");
            mRows2 = bundle.getInt("mRows2");
        } else {
            m_sets = null;
            mTitle = "";
            mAuthor = "";
            mBoxs = 0;
            m_Ans = false;
            mCols = 0;
            mRows = 0;
            mBoxs2 = 0;
            mCols2 = 0;
            mRows2 = 0;
        }

        if (mTitle.isEmpty()) myMaps.J_Title =  "关卡查询";
        else myMaps.J_Title = mTitle;
        myMaps.J_Author = mAuthor;
        myMaps.J_Comment = "";
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

        if (mMessage == null) mMessage = "查询中...";

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
                        if (!mFindTask.isCancelled() && mFindTask.getStatus() == Status.RUNNING) {
                            mFindTask.cancel(true);
                        }
                        mFindTask = null;
                    }
                    statusUpdate.onQueryDone(m_Map_List);
                }
            }
        }

        @Override
        protected void onPreExecute() {
            m_Map_List = new ArrayList<mapNode>();  //关卡列表
        }

        @Override
        protected ArrayList<mapNode> doInBackground(String... params) {
            //搜索关卡表中的相似关卡
            long mCount = -1;  //取得关卡数
            mapNode mMap2;
            String[] Arr2;
            int Rows2, Cols2;

            long n = 0;  //搜索进度
            boolean flg;
            long p_id;
            String Map0, Title0, Author0;

            Cursor cursor = mySQLite.m_SQL.mSDB.rawQuery("PRAGMA synchronous=OFF", null);
            try {
                if (m_sets == null) {
                    cursor = mySQLite.m_SQL.mSDB.query("G_Level", null, null, null, null, null, null);
                } else {
                    mySQLite.m_SQL.new_tmp_Table2();  //创建临时表，用于相似查找和查询
                    for (int k = 0; k < m_sets.length; k++)
                        mySQLite.m_SQL.add_Set_ID(m_sets[k]);

                    cursor = mySQLite.m_SQL.mSDB.rawQuery("select * from G_Level where P_id in (select P_id from id_T)", null);
                }
                while (cursor.moveToNext()){
                    if (isCancelled()) return m_Map_List;

                    int m = 0;  //统计箱子数
                    try {
                        p_id = cursor.getLong(cursor.getColumnIndex("P_id"));
                        Title0 = cursor.getString(cursor.getColumnIndex("L_Title"));
                        Author0 = cursor.getString(cursor.getColumnIndex("L_Author"));
                        Map0 = cursor.getString(cursor.getColumnIndex("L_thin_XSB"));  //标准化关卡

                        n++;  //搜索进度
                        if (mCount < 0) mCount = cursor.getCount();
                        publishProgress("查询中... " + n + "/" + mCount + '\n' + myMaps.getSetTitle(p_id) + '\n' + Title0 + '\n' + Author0);

                        if (Title0.equals("无效关卡")) {
                            Rows2 = 0;
                            Cols2 = 0;
                            Author0 = "";
                        } else {
                            Arr2 = Map0.split("\r\n|\n\r|\n|\r|\\|");
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

                            //DB 中的关卡
                            char ch;
                            for (int r = 0; r < Rows2; r++) {
                                for (int c = 0; c < Cols2; c++) {
                                    ch = Arr2[r].charAt(c);
                                    if (ch == '$' || ch == '*') m++;
                                }
                            }                        }
                    } catch (Exception e) {
                        continue;
                    }

                    if ((mTitle.isEmpty() || Title0.toUpperCase().indexOf(mTitle) >= 0) &&
                            (mAuthor.isEmpty() || Author0.toUpperCase().indexOf(mAuthor) >= 0) &&
                            ((mBoxs == 0 && mBoxs2 == 0 || mBoxs2 == 0 && mBoxs <= m) || (mBoxs == 0 && mBoxs2 >= m) || (mBoxs <= m && mBoxs2 >= m)) &&
                            ((mCols == 0 && mCols2 == 0 || mCols2 == 0 && mCols <= Cols2) || (mCols == 0 && mCols2 >= Cols2) || (mCols <= Cols2 && mCols2 >= Cols2)) &&
                            ((mRows == 0 && mRows2 == 0 || mRows2 == 0 && mRows <= Rows2) || (mRows == 0 && mRows2 >= Rows2) || (mRows <= Rows2 && mRows2 >= Rows2))) {

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
            } catch (Exception e) {
            } finally {
                if (cursor != null) cursor.close();
                mySQLite.m_SQL.del_tmp_Table2();  //清理暂存关卡集 ID 临时表
            }


            try {
                if (!m_Ans || mBoxs <= 0 && mCols <= 0 && mRows <= 0) throw new Exception();  //若不搜索答案库，用抛出异常结束任务

                //搜索答案表中的相似关卡
                n = 0;
                mCount = -1;
                cursor = mySQLite.m_SQL.mSDB.rawQuery("select P_Key, P_Key_Num, L_thin_XSB, S_id from G_State where G_Solution = 1 and P_Key_Num >= 0 and P_Key not in (select L_Key from G_Level) group by P_Key", null);
                while (cursor.moveToNext()) {
                    if (isCancelled()) break;

                    n++;  //搜索进度
                    if (mCount < 0) mCount = cursor.getCount();
                    publishProgress("查询中... " + n + "/" + mCount + "\n答案库" + "\n查询答案库时，会忽略关卡的标题及作者信息。");

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

                    char ch;
                    int m = 0;  //统计箱子数
                    for (int r = 0; r < Rows2; r++) {
                        for (int c = 0; c < Cols2; c++) {
                            ch = Arr2[r].charAt(c);
                            if (ch == '$' || ch == '*') m++;
                        }
                    }

                    if (((mBoxs == 0 && mBoxs2 == 0 || mBoxs2 == 0 && mBoxs <= m) || (mBoxs == 0 && mBoxs2 >= m) || (mBoxs <= m && mBoxs2 >= m)) &&
                        ((mCols == 0 && mCols2 == 0 || mCols2 == 0 && mCols <= Cols2) || (mCols == 0 && mCols2 >= Cols2) || (mCols <= Cols2 && mCols2 >= Cols2)) &&
                        ((mRows == 0 && mRows2 == 0 || mRows2 == 0 && mRows <= Rows2) || (mRows == 0 && mRows2 >= Rows2) || (mRows <= Rows2 && mRows2 >= Rows2))) {

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
            } catch (Exception e) {
            } finally {
                if (cursor != null) cursor.close();
            }

            return m_Map_List;
        }
        @Override
        protected void onProgressUpdate(String... progress) {
            mMessage = progress[0];
            ProgressDialog progressDialog = ((ProgressDialog) myQueryFragment.this.getDialog());
            if (progressDialog != null) {
                progressDialog.setMessage(mMessage);
            }
        }
        @Override
        protected void onPostExecute(ArrayList<mapNode> result) {
            if (mStatusUpdate != null) {
                FindStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    statusUpdate.onQueryDone(result);
                }
            }
        }
    }
}