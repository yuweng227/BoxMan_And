package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;

public class mySubmitList extends Activity {

	ArrayList<list_Node> mList1 = new ArrayList<list_Node>();  //主关提交列表
	ArrayList<list_Node> mList2 = new ArrayList<list_Node>();  //主关提交列表

	String[] submit_groups = { "主关", "副关" };
	int[] mTotal = {0, 0};  //参赛人数
    int g_Pos;  
    int c_Pos;
    int min_Moves, min_Pushs, min_Moves2, min_Pushs2;

	ExpandableListView submit_expView;
	MyExpandableListView submit_Adapter;

	private static String url = "http://sokoban.ws/api/competition/submission/";
	private ProgressDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_main);
        
        //设置标题栏标题，并开启标题栏的返回键
		setTitle("提交列表");
		
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));

		submit_expView = (ExpandableListView) findViewById(R.id.submit_explist);
		submit_Adapter = new MyExpandableListView();
		submit_expView.setAdapter(submit_Adapter);
        
        //设置item点击的监听器
		submit_expView.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
            	//记住点击位置
            	g_Pos = groupPosition;
            	c_Pos = childPosition;

            	return true;
            }
        });

		submit_expView.setOnItemLongClickListener(new OnItemLongClickListener()
        {  
            @Override  
            public boolean onItemLongClick(AdapterView<?> parent, View childView, int flatPos, long id) {  
            	
            	 long packedPosition = submit_expView.getExpandableListPosition(flatPos);
                 g_Pos = ExpandableListView.getPackedPositionGroup(packedPosition);
                 c_Pos = ExpandableListView.getPackedPositionChild(packedPosition);

                 return true;  
            }             
        }); 
        registerForContextMenu(submit_expView);

		dialog = new ProgressDialog(this);
		dialog.setMessage("下载中...");
		dialog.setCancelable(true);
		new Thread(new mySubmitList.MyThread()).start();
		dialog.show();

    }

	//比赛关卡下载完毕，关闭进度条
	private Handler handler = new Handler() {
		// 在Handler中获取消息，重写handleMessage()方法
		@Override
		public void handleMessage(Message msg) {
			dialog.dismiss();

			if (msg.what == 1 && (mList1.size() > 0 || mList2.size() > 0)) {
				if (mList1.size() > 0) submit_expView.expandGroup(0);
				if (mList2.size() > 0) submit_expView.expandGroup(1);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(mySubmitList.this, AlertDialog.THEME_HOLO_DARK);
				builder.setTitle("错误").setMessage(msg.obj.toString()).setPositiveButton("确定", null);
				builder.setCancelable(false).create().show();
			}
			submit_Adapter.notifyDataSetChanged();
		}
	};

	//使用独立进程下载提交列表
	public class MyThread implements Runnable {
		@Override
		public void run() {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			HttpGet httpGet2 = new HttpGet(url + "?t=extra");
			try {
				String inf;
				Message msg = Message.obtain();
				msg.obj = "";
				mTotal[0] = 0;
				mTotal[1] = 0;

				HttpResponse httpResponse = httpClient.execute(httpGet);
				int code = httpResponse.getStatusLine().getStatusCode();
				if (code == 200) {
					String temp = EntityUtils.toString(httpResponse.getEntity());
					msg.what = 1;
					msg.obj = myJson(temp, 0);  //解析列表
				} else {
					msg.what = 0;
					msg.obj = "下载主关列表遇到网络错误：" + code;
				}
				httpResponse = httpClient.execute(httpGet2);
				code = httpResponse.getStatusLine().getStatusCode();
				if (code == 200) {
					String temp = EntityUtils.toString(httpResponse.getEntity());
					msg.what = 1;
					inf = myJson(temp, 1);  //解析列表
					if (msg.obj.equals("")) {
						msg.obj = inf;
					} else {
						msg.obj = msg.obj + "\n" + inf;
					}
				} else {
					msg.what = 0;
					inf = "下载副关列表遇到网络错误：" + code;
					if (msg.obj.equals("")) {
						msg.obj = inf;
					} else {
						msg.obj = msg.obj + "\n" + inf;
					}
				}
				handler.sendMessage(msg);
			} catch (Exception e) {
				Message msg = Message.obtain();
				msg.what = 0;
				msg.obj = "网络错误：000";
				handler.sendMessage(msg);
			}
		}
	}


	//解析比赛关卡（使用 json-simple-1.1.jar，比较轻量级）
	private String myJson(String str, int num) {
		String inf = "解析列表遇到错误！";
		ArrayList<list_Node> mList = mList1;

		JSONParser parser = new JSONParser();
		try {
			if (num == 1) mList = mList2;  //提交列表
			mList.clear();

			JSONObject obj = (JSONObject)parser.parse(str);

			// 解析出列表
			str = obj.get("submission").toString();
			Object obj2 = JSONValue.parse(str);
			JSONArray array = (JSONArray)obj2;

			list_Node nd;
			int n1 = Integer.MAX_VALUE, n2 = Integer.MAX_VALUE, n3 = Integer.MAX_VALUE, n4 = Integer.MAX_VALUE;
			if (num == 0) {
				min_Moves = -1;
				min_Pushs = -1;
			} else {
				min_Moves2 = -1;
				min_Pushs2 = -1;
			}
			for (int k = 0; k < array.size(); k++) {
				obj = (JSONObject) array.get(k);

				nd = new list_Node();
				nd.id = k+1;
				nd.date = obj.get("time").toString();
				nd.name = obj.get("name").toString();
				nd.country = obj.get("country").toString();
				nd.moves = obj.get("move").toString();
				nd.pushs = obj.get("push").toString();

				if (n1 > Integer.valueOf(nd.moves)) {
					n1 = Integer.valueOf(nd.moves);
					n3 = Integer.valueOf(nd.pushs);
					if (num == 0) min_Moves = k+1;
					else min_Moves2 = k+1;
				} else if (n1 == Integer.valueOf(nd.moves)) {
					if (n3 > Integer.valueOf(nd.pushs)) {
						n1 = Integer.valueOf(nd.moves);
						n3 = Integer.valueOf(nd.pushs);
						if (num == 0) min_Moves = k+1;
						else min_Moves2 = k+1;
					}
				}

				if (n2 > Integer.valueOf(nd.pushs)) {
					n2 = Integer.valueOf(nd.pushs);
					n4 = Integer.valueOf(nd.moves);
					if (num == 0) min_Pushs = k+1;
					else min_Pushs2 = k+1;
				} else if (n2 == Integer.valueOf(nd.pushs)) {
					if (n4 > Integer.valueOf(nd.moves)) {
						n2 = Integer.valueOf(nd.pushs);
						n4 = Integer.valueOf(nd.moves);
						if (num == 0) min_Pushs = k+1;
						else min_Pushs2 = k+1;
					}
				}

				mList.add(nd);
			}

			//统计参赛人数
			ArrayList<String> list  =   new  ArrayList<String>();
			for (int k = 0; k < mList.size(); k++) {
				if(!list.contains(mList.get(k).name.trim().toLowerCase())){
					list.add(mList.get(k).name.trim().toLowerCase());
				}
			}
			mTotal[num] = list.size();

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return inf;
	}

	//为ExpandableListView自定义适配器
	class MyExpandableListView extends BaseExpandableListAdapter {
	
		//返回一级列表的个数
		@Override
		public int getGroupCount() {
			return submit_groups.length;
		}
	
		//返回各二级列表的个数
		@Override
		public int getChildrenCount(int groupPosition) {
			if (groupPosition == 0)
				return mList1.size();
			else
				return mList2.size();
		}
	
		//返回一级列表的单个item（返回的是对象）
		@Override
		public Object getGroup(int groupPosition) {
			return submit_groups[groupPosition];
		}
	
		//返回二级列表中的单个item（返回的是对象）
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			if (groupPosition == 0)
				return mList1.get(childPosition);
			else
				return mList2.get(childPosition);
		}
	
		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
	
		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
	
		//每个item的id是否是固定，一般为true
		@Override
		public boolean hasStableIds() {
			return true;
		}
	
		//【重要】填充一级列表
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
	
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.submit_groups, null);
			}
			TextView ts_group = (TextView) convertView.findViewById(R.id.submit_expGroup);
			ts_group.setText(submit_groups[groupPosition] + " 【" + mTotal[groupPosition] + "人参赛】");
			return convertView;
		}
	
		//【重要】填充二级列表
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
	
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.submit_child, null);
			}
	
			TextView ts_child = (TextView) convertView.findViewById(R.id.submit_expChild);
			TextView ts_child2 = (TextView) convertView.findViewById(R.id.submit_expChild2);
			TextView ts_child3 = (TextView) convertView.findViewById(R.id.submit_expChild3);
			TextView ts_child4 = (TextView) convertView.findViewById(R.id.submit_expChild4);
			TextView ts_child5 = (TextView) convertView.findViewById(R.id.submit_expChild5);
			TextView ts_child6 = (TextView) convertView.findViewById(R.id.submit_expChild6);

			if (groupPosition == 0) {
				ts_child.setText(String.valueOf(mList1.get(childPosition).id));
				ts_child2.setText(mList1.get(childPosition).date.substring(5, 11));
				ts_child3.setText(mList1.get(childPosition).name);
				ts_child4.setText(mList1.get(childPosition).country);
				ts_child5.setText(String.valueOf(mList1.get(childPosition).moves));
				ts_child6.setText(String.valueOf(mList1.get(childPosition).pushs));
				if (min_Moves == mList1.get(childPosition).id) ts_child5.setTextColor(0xff00ff00);
				else ts_child5.setTextColor(0xffbbbbbb);
				if (min_Pushs == mList1.get(childPosition).id) ts_child6.setTextColor(0xff0000ff);
				else ts_child6.setTextColor(0xffbbbbbb);
			} else {
				ts_child.setText(String.valueOf(mList2.get(childPosition).id));
				ts_child2.setText(mList2.get(childPosition).date.substring(5, 11));
				ts_child3.setText(mList2.get(childPosition).name);
				ts_child4.setText(mList2.get(childPosition).country);
				ts_child5.setText(String.valueOf(mList2.get(childPosition).moves));
				ts_child6.setText(String.valueOf(mList2.get(childPosition).pushs));
				if (min_Moves2 == mList2.get(childPosition).id) ts_child5.setTextColor(0xff00ff00);
				else ts_child5.setTextColor(0xffbbbbbb);
				if (min_Pushs2 == mList2.get(childPosition).id) ts_child6.setTextColor(0xff0000ff);
				else ts_child6.setTextColor(0xffbbbbbb);
			}
	
			return convertView;
		}
	
		//二级列表中的item是否能够被选中
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

	public boolean onOptionsItemSelected(MenuItem mt) {
	    switch (mt.getItemId()) {
		//菜单栏返回键功能
		case android.R.id.home:
			this.finish();

			return true;
       default:
       		return super.onOptionsItemSelected(mt);
       }
	}
}