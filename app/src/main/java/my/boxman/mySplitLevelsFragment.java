package my.boxman;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

//导入时，异步解析关卡，有可能遇到大的文档导致耗时剧增，BoxMan 调用
public class mySplitLevelsFragment extends DialogFragment {

    //接口，向上层传递参数
    interface SplitStatusUpdate {
        void onSplitDone(String inf);
    }

    private WeakReference<SplitStatusUpdate> mStatusUpdate;
    public SplitTask mSplitTask;
    private String mMessage;
    private int myType;  //解析类别
    ArrayList<String> myFiles;  //文档列表

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mStatusUpdate = new WeakReference<SplitStatusUpdate>((SplitStatusUpdate)activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            myType = bundle.getInt("my_Type");
            myFiles = bundle.getStringArrayList("my_Files");
        } else myType = -1;

        setRetainInstance(true);

        mSplitTask = new SplitTask();
        mSplitTask.execute();
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

        if (mMessage == null) mMessage = "解析中...";

        progressDialog.setMessage(mMessage);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return progressDialog;
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        mSplitTask.stopSplit();
    }

    private final class SplitTask extends AsyncTask<Void, String, String> {

        void stopSplit() {
            if (mStatusUpdate != null) {
                SplitStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    if (mSplitTask != null  && !mSplitTask.isCancelled() && mSplitTask.getStatus() == AsyncTask.Status.RUNNING) {
                        mSplitTask.cancel(true);
                        mSplitTask = null;
                    }
                    statusUpdate.onSplitDone("操作被中断，导入可能不完整！");
                }
            }
        }

        @Override
        protected void onPreExecute() {
            myMaps.m_lstMaps.clear();  //关卡列表
        }

        @Override
        protected String doInBackground(Void... params) {

            if (myType < 0 || myFiles == null) return "没有可解析的内容！";

            publishProgress("解析中...");

            int[] num = {0, 0};      //解析出来的关卡数、无效关卡数
            myMaps.m_Nums[0] = 0;    //解析出来的答案数、无效答案数
            myMaps.m_Nums[1] = 0;

            if (myType == 2) {  //关卡集列表
                StringBuilder g_Map = new StringBuilder();      //关卡地图
                StringBuilder g_Title = new StringBuilder();    //标题
                StringBuilder g_Author = new StringBuilder();   //作者
                StringBuilder g_Comment = new StringBuilder();  //"注释"
                StringBuilder sSolution = new StringBuilder();  //答案
                mapNode nd;
                long id;
                String my_Name;
                File file;
                InputStreamReader read;
                BufferedReader bufferedReader;

                boolean flg = false;   //是否 XSB
                boolean flg2 = false;  //是否 Comment
                boolean flg3 = false;  //是否答案
                boolean newSet = false;  //关卡集解析尚未开始

                String line;
                for (int i = 0; i < myFiles.size(); i++) {
                    if (isCancelled()) return "";
                    if (myMaps.m_setName.isItemChecked(i)) {
                        try {
                            my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("关卡扩展/").append(myFiles.get(i)).toString();
                            //若包含导入关卡选项，查看是否有重复关卡集
                            if (myMaps.isXSB) {
                                myMaps.J_Title = myFiles.get(i).substring(0, myFiles.get(i).lastIndexOf("."));    //去掉扩展名
                                long new_Set_id = mySQLite.m_SQL.find_Set(myMaps.J_Title);
                                if (new_Set_id > 0) continue; //关卡集已经登记，跳过
                                else {  //创建关卡集
                                    myMaps.m_Set_id = mySQLite.m_SQL.add_T(3, myMaps.J_Title, "", "");

                                    if (myMaps.m_Set_id > 0) {  //关卡集创建成功，更新界面中的关卡集列表
                                        set_Node nd2 = new set_Node();
                                        nd2.id = myMaps.m_Set_id;
                                        nd2.title = myMaps.J_Title;
                                        myMaps.mSets3.add(nd2);
                                    } else continue; //关卡集创建失败，跳过
                                }
                            }
                            publishProgress("检查文档编码...\n" + myFiles.get(i));
                            file = new File(my_Name);
                            read = new InputStreamReader(new FileInputStream(file), myMaps.getTxtEncode(new FileInputStream(file)));  //考虑到编码格式
                            bufferedReader = new BufferedReader(read);
                            nd = null;
                            while (true) {
                                if (isCancelled()) return "";

                                publishProgress("解析中...\n" + myFiles.get(i) + "\n" + num[0]);

                                line = bufferedReader.readLine();
                                if (line == null || myMaps.isXSB(line)){  //匹配 XSB 行，目前效率最高的判断方法，效率约是最初正则的 10 倍
                                    if (!flg || line == null) {  // XSB 块刚开始，或到文档尾
                                        if (line == null && g_Map.length() <= 0) break;  //到文档尾，且没有解析到 XSB
                                        num[0]++;  //到文档尾时，会虚增一个数
                                        if (newSet) {  //当遇到第一个关卡的XSB后，说明新的关卡集解析已经开始
                                            if (nd == null)
                                                nd = new mapNode(g_Map.toString(), g_Title.toString(), g_Author.toString(), g_Comment.toString());  //关卡节点
                                            if (myMaps.isXSB) {
                                                id = mySQLite.m_SQL.add_L(myMaps.m_Set_id, nd);   //添加的关卡库，关卡所属的关卡集 id: P_id = myMaps.m_Set_id
                                                if (nd.L_CRC_Num < 0 || id <= 0) num[1]++;
                                            }
                                        } else {  //当遇到第一个关卡的XSB后，需要先保存一下关卡集的作者、说明等信息，此时，还没有关卡的XSB读入
                                            if (myMaps.isXSB) {
                                                mySQLite.m_SQL.Update_T_Inf(myMaps.m_Set_id, myMaps.J_Title, g_Author.toString(), g_Comment.toString());
                                            }
                                        }
                                        if (sSolution.length() > 0) {  //有答案尚未保存
                                            mySQLite.m_SQL.inp_Ans(nd, sSolution.toString());  //若导入答案
                                        }
                                        if (line == null) {  //到文档尾，此关卡集解析结束
                                            newSet = false;  //设置“新的关卡集解析尚未开始”标志
                                            num[0]--;        //将虚增的关卡数调整一下
                                            break;
                                        }

                                        newSet = true;  //一个关卡已经开始解析
                                        g_Map     = new StringBuilder();
                                        g_Title   = new StringBuilder();
                                        g_Author  = new StringBuilder();
                                        g_Comment = new StringBuilder();
                                        sSolution = new StringBuilder();
                                        flg3 = false;
                                        flg2 = false;  //强制"注释"块结束，预防“注释”块没写"comment-end:"的情况
                                        flg = true;    //准备读入关卡XSB
                                        nd = null;
                                    }
                                    if (g_Map.length() > 0) g_Map.append('\n');
                                    g_Map.append(line);
                                } else
                                if (!flg2 && line.toLowerCase(Locale.getDefault()).startsWith("title:")){  //匹配 Title，标题
                                    g_Title.append(line.substring(6).trim());
                                    flg = false;  //结束关卡SXB的解析
                                    flg3 = false;
                                } else
                                if (!flg2 && line.toLowerCase(Locale.getDefault()).startsWith("author:")){  //匹配 Author，作者
                                    g_Author.append(line.substring(7).trim());
                                    flg = false;  //结束关卡SXB的解析
                                    flg3 = false;
                                } else
                                if (myMaps.isLurd && line.toLowerCase(Locale.getDefault()).startsWith("solution")){  //匹配 Solution，答案
                                    if (sSolution.length() > 0) {  //有答案尚未保存
                                        if (nd == null)
                                            nd = new mapNode(g_Map.toString(), g_Title.toString(), g_Author.toString(), g_Comment.toString());  //关卡节点}
                                        mySQLite.m_SQL.inp_Ans(nd, sSolution.toString());
                                        sSolution = new StringBuilder();
                                    }
                                    if (line.indexOf(":") >= 0) {
                                        sSolution.append(line.substring(line.indexOf(":")+1).trim());
                                    } else {
                                        sSolution.append(line.substring(line.indexOf(")")+1).trim());
                                    }
                                    flg = false;
                                    flg2 = false;  //结束"注释"块
                                    flg3 = true;   //开始答案行
                                } else
                                if (line.toLowerCase(Locale.getDefault()).startsWith("comment-end:") ||
                                        line.toLowerCase(Locale.getDefault()).startsWith("comment_end:")){  //匹配 Comment-end，"注释"块结束
                                    flg2 = false;  //结束"注释"块
                                } else
                                if (line.toLowerCase(Locale.getDefault()).startsWith("comment:")){  //匹配 Comment，"注释"块开始
                                    flg3 = false;
                                    flg2 = true;  //开始"注释"块
                                    flg = false;  //结束关卡SXB的解析
                                    line = line.substring(8).trim();
                                    if (!line.equals("")) g_Comment.append(line);
                                } else
                                if (!flg2 && (line.indexOf(';') == 0 || line.matches("\\s*"))){  //若非"说明"信息，则跳过注释行和空行
                                    flg = false;  //结束关卡SXB的解析
                                } else
                                if (flg2) {   //"注释"块
                                    if (!g_Comment.toString().isEmpty()) g_Comment.append('\n');
                                    g_Comment.append(line);
                                } else
                                if (flg3) {  //答案行
                                    sSolution.append(line);
                                }
                            }  //end the while

                            read.close();
                        } catch (Exception e) {
                            //对于不规范的文档，直接跳过
                        }
                    }
                }  // end for
            } else if (myType == 1) {  //关卡文档（忽略关卡集方面的信息）
                try {
                    publishProgress("检查文档编码...\n" + myFiles.get(0));
                    String my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("关卡扩展/").append(myFiles.get(0)).toString();
                    File file = new File(my_Name);
                    InputStreamReader read = new InputStreamReader(new FileInputStream(file), myMaps.getTxtEncode(new FileInputStream(file)));  //考虑到编码格式
                    BufferedReader bufferedReader = new BufferedReader(read);

                    StringBuilder g_Map = new StringBuilder();      //关卡地图
                    StringBuilder g_Title = new StringBuilder();    //标题
                    StringBuilder g_Author = new StringBuilder();   //作者
                    StringBuilder g_Comment = new StringBuilder();  //"注释"
                    StringBuilder sSolution = new StringBuilder();  //答案
                    mapNode nd = null;
                    long id;

                    boolean flg = false;   //是否 XSB
                    boolean flg2 = false;  //是否 Comment
                    boolean flg3 = false;  //是否答案

                    String line;
                    while (true) {
                        if (isCancelled()) return "";

                        publishProgress("解析中...\n" + myFiles.get(0) + "\n" + num[0]);

                        line = bufferedReader.readLine();
                        if (line == null || myMaps.isXSB(line)){  //匹配 XSB 行，目前效率最高的判断方法，效率约是最初正则的 10 倍
                            if (!flg || line == null) {  // XSB 块刚开始，或到文档尾
                                if (line == null && g_Map.length() <= 0) break;  //到文档尾，且没有解析到 XSB
                                num[0]++;  //到文档尾时，会虚增一个数
                                if (num[0] > 1) {  //前面有解析过的关卡或到了文档尾，当为 1 时，若遇到文档尾，则说明没有解析到一个关卡
                                    if (nd == null)
                                        nd = new mapNode(g_Map.toString(), g_Title.toString(), g_Author.toString(), g_Comment.toString());  //关卡节点
                                    if (myMaps.isXSB) {
                                        id = mySQLite.m_SQL.add_L(myMaps.m_Set_id, nd);   //添加的关卡库，关卡所属的关卡集 id: P_id = myMaps.m_Set_id
                                        if (nd.L_CRC_Num < 0 || id <= 0) num[1]++;
                                    }
                                }
                                if (sSolution.length() > 0) {  //有答案尚未保存
                                    mySQLite.m_SQL.inp_Ans(nd, sSolution.toString());  //若导入答案
                                }
                                if (line == null) break;  //到文档尾

                                g_Map     = new StringBuilder();
                                g_Title   = new StringBuilder();
                                g_Author  = new StringBuilder();
                                g_Comment = new StringBuilder();
                                sSolution = new StringBuilder();
                                flg3 = false;
                                flg2 = false;  //强制"注释"块结束，预防“注释”块没写"comment-end:"的情况
                                flg = true;    //准备读入关卡XSB
                                nd = null;
                            }
                            if (g_Map.length() > 0) g_Map.append('\n');
                            g_Map.append(line);
                        } else
                        if (!flg2 && line.toLowerCase(Locale.getDefault()).startsWith("title:")){  //匹配 Title，标题
                            g_Title.append(line.substring(6).trim());
                            flg = false;  //结束关卡SXB的解析
                            flg3 = false;
                        } else
                        if (!flg2 && line.toLowerCase(Locale.getDefault()).startsWith("author:")){  //匹配 Author，作者
                            g_Author.append(line.substring(7).trim());
                            flg = false;  //结束关卡SXB的解析
                            flg3 = false;
                        } else
                        if (myMaps.isLurd && line.toLowerCase(Locale.getDefault()).startsWith("solution")){  //匹配 Solution，答案
                            if (sSolution.length() > 0) {  //有答案尚未保存
                                if (nd == null)
                                    nd = new mapNode(g_Map.toString(), g_Title.toString(), g_Author.toString(), g_Comment.toString());  //关卡节点}
                                mySQLite.m_SQL.inp_Ans(nd, sSolution.toString());
                                sSolution = new StringBuilder();
                            }
                            if (line.indexOf(":") >= 0) {
                                sSolution.append(line.substring(line.indexOf(":")+1).trim());
                            } else {
                                sSolution.append(line.substring(line.indexOf(")")+1).trim());
                            }
                            flg = false;
                            flg2 = false;  //结束"注释"块
                            flg3 = true;   //开始答案行
                        } else
                        if (line.toLowerCase(Locale.getDefault()).startsWith("comment-end:") ||
                                line.toLowerCase(Locale.getDefault()).startsWith("comment_end:")){  //匹配 Comment-end，"注释"块结束
                            flg2 = false;  //结束"注释"块
                        } else
                        if (line.toLowerCase(Locale.getDefault()).startsWith("comment:")){  //匹配 Comment，"注释"块开始
                            flg3 = false;
                            flg2 = true;  //开始"注释"块
                            flg = false;  //结束关卡SXB的解析
                            line = line.substring(8).trim();
                            if (!line.equals("")) g_Comment.append(line);
                        } else
                        if (!flg2 && (line.indexOf(';') == 0 || line.matches("\\s*"))){  //若非"说明"信息，则跳过注释行和空行
                            flg = false;  //结束关卡SXB的解析
                        } else
                        if (flg2) {   //"注释"块
                            if (!g_Comment.toString().isEmpty()) g_Comment.append('\n');
                            g_Comment.append(line);
                        } else
                        if (flg3) {  //答案行
                            sSolution.append(line);
                        }
                    }  //end the while

                    read.close();
                } catch (Exception e) {
                }
                if (num[0] > 0) num[0]--;  //修正统计数字
            } else {  //剪切板导入
                try {
                    String[] Arr = (myFiles.get(0) + "\n\n;").split("\r\n|\n\r|\n|\r|\\|");    //将剪切板的内容，按行拆解（为便于解析，人为加一个空行）
                    StringBuilder g_Map = new StringBuilder();      //关卡地图
                    StringBuilder g_Title = new StringBuilder();    //标题
                    StringBuilder g_Author = new StringBuilder();   //作者
                    StringBuilder g_Comment = new StringBuilder();  //"注释"
                    StringBuilder sSolution = new StringBuilder();  //答案
                    mapNode nd = null;
                    long id;

                    boolean flg = false;   //是否 XSB
                    boolean flg2 = false;  //是否 Comment
                    boolean flg3 = false;  //是否答案

                    String line;
                    int k = 0;
                    while (k < Arr.length) {
                        if (isCancelled()) return "";

                        publishProgress("解析中...\n剪切板\n" + num[0]);

                        line = Arr[k++];
                        if (myMaps.isXSB(line) || k == Arr.length-1){  //匹配 XSB 行，目前效率最高的判断方法，效率约是最初正则的 10 倍
                            if (!flg || k == Arr.length-1) {  // XSB 块刚开始，或遇到最后一行（为便于解析，人为加了一个空行）
                                num[0]++;  //到剪切板尾时，会虚增一个数
                                if (num[0] > 1) {  //前面有解析过的关卡或到剪切板尾，当为 1 时，若遇到文档尾，则说明没有解析到一个关卡
                                    if (nd == null)
                                        nd = new mapNode(g_Map.toString(), g_Title.toString(), g_Author.toString(), g_Comment.toString());  //关卡节点
                                    if (myMaps.isXSB) {
                                        id = mySQLite.m_SQL.add_L(myMaps.m_Set_id, nd);   //添加的关卡库，关卡所属的关卡集 id: P_id = myMaps.m_Set_id
                                        if (nd.L_CRC_Num < 0 || id <= 0) num[1]++;
                                    }
                                }
                                if (sSolution.length() > 0) {  //有答案尚未保存
                                    mySQLite.m_SQL.inp_Ans(nd, sSolution.toString());  //若导入答案
                                }
                                g_Map     = new StringBuilder();
                                g_Title   = new StringBuilder();
                                g_Author  = new StringBuilder();
                                g_Comment = new StringBuilder();
                                sSolution = new StringBuilder();
                                flg3 = false;
                                flg2 = false;  //强制"注释"块结束，预防“注释”块没写"comment-end:"的情况
                                flg = true;   //准备读入关卡XSB
                                nd = null;
                            }
                            if (g_Map.length() > 0) g_Map.append('\n');
                            g_Map.append(line);
                        } else
                        if (!flg2 && line.toLowerCase(Locale.getDefault()).startsWith("title:")){  //匹配 Title，标题
                            g_Title.append(line.substring(6).trim());
                            flg = false;  //结束关卡SXB的解析
                            flg3 = false;
                        } else
                        if (!flg2 && line.toLowerCase(Locale.getDefault()).startsWith("author:")){  //匹配 Author，作者
                            g_Author.append(line.substring(7).trim());
                            flg = false;  //结束关卡SXB的解析
                            flg3 = false;
                        } else
                        if (myMaps.isLurd && line.toLowerCase(Locale.getDefault()).startsWith("solution")){  //匹配 Solution，答案
                            if (sSolution.length() > 0) {  //有答案尚未保存
                                if (nd == null)
                                    nd = new mapNode(g_Map.toString(), g_Title.toString(), g_Author.toString(), g_Comment.toString());  //关卡节点
                                mySQLite.m_SQL.inp_Ans(nd, sSolution.toString());
                                sSolution = new StringBuilder();
                            }
                            if (line.indexOf(":") >= 0) {
                                sSolution.append(line.substring(line.indexOf(":")+1).trim());
                            } else {
                                sSolution.append(line.substring(line.indexOf(")")+1).trim());
                            }
                            flg = false;
                            flg2 = false;  //结束"注释"块
                            flg3 = true;   //开始答案行
                        } else
                        if (line.toLowerCase(Locale.getDefault()).startsWith("comment-end:") ||
                                line.toLowerCase(Locale.getDefault()).startsWith("comment_end:")){  //匹配 Comment-end，"注释"块结束
                            flg2 = false;  //结束"注释"块
                        } else
                        if (line.toLowerCase(Locale.getDefault()).startsWith("comment:")){  //匹配 Comment，"注释"块开始
                            flg3 = false;
                            flg2 = true;  //开始"注释"块
                            flg = false;  //结束关卡SXB的解析
                            line = line.substring(8).trim();
                            if (!line.equals("")) g_Comment.append(line);
                        } else
                        if (!flg2 && (line.indexOf(';') == 0 || line.matches("\\s*"))){  //若非"说明"信息，则跳过注释行和空行
                            flg = false;  //结束关卡SXB的解析
                        } else
                        if (flg2) {   //"注释"块
                            if (!g_Comment.toString().isEmpty()) g_Comment.append('\n');
                            g_Comment.append(line);
                        } else
                        if (flg3) {  //答案行
                            sSolution.append(line);
                        }
                    }  //end the while

                } catch (Exception e) {
                }
                if (num[0] > 0) num[0]--;  //修正统计数字
            }

            //导入统计
            StringBuilder str = new StringBuilder();

            if (num[0] == 0) {
                str.append("关卡集重复或无效！");
            } else {

                str.append("关卡数：").append(num[0]).append("\n无效关卡数：").append(num[1]);
                myMaps.m_Nums[2] = num[0];
                myMaps.m_Nums[3] = num[1];
                if (myMaps.isLurd) {
                    str.append("\n\n答案数：").append(myMaps.m_Nums[0]).append("\n导入数：").append(myMaps.m_Nums[0] - myMaps.m_Nums[1]);
                    if (myMaps.m_Nums[1] > 0) {
                        str.append("\n(无效重复超长答案不导入)");
                    }
                }
            }
            return str.toString();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mMessage = progress[0];
            ProgressDialog progressDialog = ((ProgressDialog) mySplitLevelsFragment.this.getDialog());
            if (progressDialog != null) {
                progressDialog.setMessage(mMessage);
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (mStatusUpdate != null) {
                SplitStatusUpdate statusUpdate = mStatusUpdate.get();
                if (statusUpdate != null) {
                    statusUpdate.onSplitDone(result);
                }
            }
        }
    }

}