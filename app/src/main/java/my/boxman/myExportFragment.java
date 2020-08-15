package my.boxman;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

//异步导出关卡集，BoxMan 调用
public class myExportFragment extends DialogFragment {

    //接口，向上层传递参数
    interface ExportStatusUpdate {
        void onExportDone(String inf);
    }

    private WeakReference<ExportStatusUpdate> mStatusUpdate;
    public ExportTask mExportTask;
    private String  mMessage, my_Name;
    private StringBuilder mInf;
    private boolean myAns;      //是否导出仅答案关卡
    private boolean myLurd;     //是否导出答案
    private boolean myReWrite;  //遇到重复文档是否覆盖
    private long[] mySets;      //关卡集id列表

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mStatusUpdate = new WeakReference<ExportStatusUpdate>((ExportStatusUpdate)activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            myAns = bundle.getBoolean("my_Ans");
            myLurd = bundle.getBoolean("my_Lurd");
            myReWrite = bundle.getBoolean("my_ReWrite");
            mySets = bundle.getLongArray("my_SetIDs");
        } else {
            myAns = false;
            myLurd = false;
            myReWrite = false;
            mySets = null;
        }

        setRetainInstance(true);

        mExportTask = new ExportTask();
        mExportTask.execute();
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

        if (mMessage == null) mMessage = "导出...";

        progressDialog.setMessage(mMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return progressDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mExportTask.stopExport();
    }

    private final class ExportTask extends AsyncTask<Void, String, String> {

        void stopExport() {
            if (mStatusUpdate != null) {
                ExportStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    if (mExportTask != null  && !mExportTask.isCancelled() && mExportTask.getStatus() == Status.RUNNING) {
                        mExportTask.cancel(true);
                        mExportTask = null;
                    }
                    statusUpdate.onExportDone(mInf.append("\n...Break！").toString());
                }
            }
        }

        @Override
        protected void onPreExecute() {
            myMaps.m_lstMaps.clear();  //关卡列表
        }

        @Override
        protected String doInBackground(Void... params) {

            if (mySets == null) return "没有可导出的内容！";

            int set_Count = 0;
            for (int k = 0; k < mySets.length; k++) {  //计数选中的关卡集个数
                if (mySets[k] > 0) set_Count++;
            }
            if (myAns) set_Count++;
            mInf = new StringBuilder("共选择").append(set_Count).append("个关卡集:");

            for (int k = 0; k < mySets.length; k++) {  //导出选中的关卡集

                if (mySets[k] > 0) {
                    mySQLite.m_SQL.get_Set(mySets[k]);
                    mySQLite.m_SQL.get_Levels(mySets[k]);

                    if (myMaps.m_lstMaps.size() > 0){
                        myMaps.sFile = myMaps.J_Title;
                        my_Name = myMaps.sFile + (myLurd ? ".txt" : ".xsb");  // 导出文档名，不含路径
                        publishProgress("导出...\n" + my_Name);
                        if (isCancelled()) return mInf.append("...Break！").toString();

                        File file = new File(myMaps.sRoot + myMaps.sPath + "导出/" + my_Name);
                        if (!file.exists() || myReWrite) {  // 若没有同名文档或允许覆盖
                            exportSet(my_Name, file.exists());  // 导出
                        } else {  // 若若不允许覆盖同名文档，则跳过
                            mInf.append('\n').append(my_Name).append("...跳过");
                        }
                    }
                }
            }

            if (myAns) {  //导出仅有答案的关卡
                my_Name = "仅有答案的关卡" + (myLurd ? ".txt" : ".xsb");
                publishProgress("导出...\n" + my_Name);
                File file = new File(myMaps.sRoot + myMaps.sPath + "导出/" + my_Name);
                mInf.append('\n').append(my_Name);
                if (!file.exists() || myReWrite) {
                    if (isCancelled()) return mInf.append("...Break！").toString();
                    if (mySQLite.m_SQL.expAnsLevel()) {
                        if (file.exists()) mInf.append("...覆盖");
                        else mInf.append("...OK");
                    } else {
                        mInf.append("...Error");
                    }
                } else {
                    mInf.append("...跳过");
                }
            }

            return mInf.toString();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mMessage = progress[0];
            ProgressDialog progressDialog = ((ProgressDialog) myExportFragment.this.getDialog());
            if (progressDialog != null) {
                progressDialog.setMessage(mMessage);
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (mStatusUpdate != null) {
                ExportStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    statusUpdate.onExportDone(result);
                }
            }
        }
    }

    //导出关卡集
    private void exportSet(String my_Name, boolean flg) {
        try{
            final StringBuilder str = new StringBuilder();

            if (myMaps.J_Author != null && !myMaps.J_Author.trim().isEmpty()){
                str.append("Author: ").append(myMaps.J_Author).append('\n');
            }
            if (myMaps.J_Comment != null && !myMaps.J_Comment.trim().isEmpty()){
                str.append("Comment:\n").append(myMaps.J_Comment).append("\nComment-End:\n");
            }

            for(int k = 0; k < myMaps.m_lstMaps.size(); k++){
                str.append("\n;Level "+ (k+1) + "\n");
                if (myMaps.m_lstMaps.get(k).Title != null && !myMaps.m_lstMaps.get(k).Title.equals("无效关卡")) {
                    str.append(myMaps.m_lstMaps.get(k).Map).append('\n');
                }
                if (myMaps.m_lstMaps.get(k).Title != null && !myMaps.m_lstMaps.get(k).Title.trim().isEmpty()){
                    if (myMaps.m_lstMaps.get(k).Title.equals("无效关卡")) {
                        if (myMaps.m_lstMaps.get(k).Comment != null && !myMaps.m_lstMaps.get(k).Comment.trim().isEmpty()){
                            str.append(myMaps.m_lstMaps.get(k).Comment).append('\n');
                        }
                        continue;
                    }
                    str.append("Title: ").append(myMaps.m_lstMaps.get(k).Title).append('\n');
                }
                if (myMaps.m_lstMaps.get(k).Author != null && !myMaps.m_lstMaps.get(k).Author.trim().isEmpty()){
                    str.append("Author: ").append(myMaps.m_lstMaps.get(k).Author).append('\n');
                }
                if (myMaps.m_lstMaps.get(k).Comment != null && !myMaps.m_lstMaps.get(k).Comment.trim().isEmpty()){
                    str.append("Comment:\n").append(myMaps.m_lstMaps.get(k).Comment);
                    if (myMaps.m_lstMaps.get(k).Comment.charAt(myMaps.m_lstMaps.get(k).Comment.length()-1) != '\n') str.append('\n');
                    str.append("Comment-End:\n");
                }
                if (myLurd) {  //导出答案
                    myMaps.curMap = myMaps.m_lstMaps.get(k);
                    str.append(mySQLite.m_SQL.get_Ans(myMaps.curMap.key));
                }
            }

            FileOutputStream fout = new FileOutputStream(myMaps.sRoot + myMaps.sPath + "导出/" + my_Name);
            fout.write(str.toString().getBytes());
            fout.flush();
            fout.close();
            mInf.append('\n').append(my_Name);
            if (flg) mInf.append("...覆盖");
            else mInf.append("...OK");
        } catch(Exception e) {
            mInf.append('\n').append(my_Name).append("...Error");
        }
    }
}