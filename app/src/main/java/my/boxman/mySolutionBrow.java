package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class mySolutionBrow extends Activity {
	AlertDialog mDlg;
	
	String[] s_groups = { "答案" };
    int c_Pos;
	long m_Sel_id;

	ExpandableListView s_expView;
	MyExpandableListView s_Adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.s_main);
        
        //设置标题栏标题，并开启标题栏的返回键
		setTitle("相似关卡");
		
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));
        
        s_expView = (ExpandableListView) findViewById(R.id.s_explist);
        s_Adapter = new MyExpandableListView();
        s_expView.setAdapter(s_Adapter);
        
        //设置item点击的监听器
		s_expView.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
            	//记住点击位置
            	c_Pos = childPosition;
            	
    	    	//取得答案的 id
            	if (c_Pos > -1) {
	    			m_Sel_id = myMaps.mState2.get(childPosition).id;
            	} else m_Sel_id = (long)-1;
    			
            	return true;
            }
        });
		
		s_expView.setOnItemLongClickListener(new OnItemLongClickListener()  
        {  
            @Override  
            public boolean onItemLongClick(AdapterView<?> parent, View childView, int flatPos, long id) {  
            	
            	 long packedPosition = s_expView.getExpandableListPosition(flatPos);
                 c_Pos = ExpandableListView.getPackedPositionChild(packedPosition);

    			 if (c_Pos != -1) {
					m_Sel_id = myMaps.mState2.get(c_Pos).id;
					s_expView.showContextMenu();
				 }
                 
                 return true;  
            }             
        }); 
        registerForContextMenu(s_expView);

		s_expView.expandGroup(0);

		AlertDialog.Builder dlg3 = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		dlg3.setNegativeButton("取消", null)
		   .setPositiveButton("覆写", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					String my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("导出/").append(myMaps.sFile).append("_").append(myMaps.m_lstMaps.indexOf(myMaps.curMap)+1).append(".txt").toString();
					if (writeStateFile(my_Name))
						MyToast.showToast(mySolutionBrow.this, "导出完成！\n" + my_Name.substring(my_Name.indexOf("导出/")), Toast.LENGTH_SHORT);
					else
						MyToast.showToast(mySolutionBrow.this, "导出时遇到错误！", Toast.LENGTH_SHORT);
				}});
		mDlg = dlg3.create();
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
//		menu.add(0, 3, 0, "导出到文档: XSB+Lurd");
//		menu.add(0, 4, 0, "导出到剪切板: XSB+Lurd");
		menu.add(0, 5, 0, "导出到剪切板: Lurd");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (c_Pos >= 0) {
			switch (item.getItemId()) {
//				case 3:  //导出到文档: XSB+Lurd
//					myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
//
//					File targetDir = new File(myMaps.sRoot+myMaps.sPath + "导出/");
//					if (!targetDir.exists()) targetDir.mkdirs();  //创建文件夹
//
//					String my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("导出/").append(myMaps.sFile).append("_").append(myMaps.m_lstMaps.indexOf(myMaps.curMap) + 1).append(".txt").toString();
//					File file = new File(my_Name);
//					if (file.exists()) {
//						mDlg.setMessage("文档已存在，覆写吗？\n" + my_Name.substring(my_Name.indexOf("导出/")));
//						mDlg.show(); //自设关卡已存在
//					} else {
//						if (writeStateFile(my_Name))
//							MyToast.showToast(this, "导出完成！\n" + my_Name.substring(my_Name.indexOf("导出/")), Toast.LENGTH_SHORT);
//						else
//							MyToast.showToast(this, "导出时遇到错误！", Toast.LENGTH_SHORT);
//					}
//
//					break;
//				case 4:  //导出到剪切板: XSB+Lurd
//					myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
//					myExport2();
//					break;
				case 5:  //导出到剪切板: Lurd
					myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
					myExport();
					break;
			}
		}
		return true;
	}
	
	//为ExpandableListView自定义适配器
	class MyExpandableListView extends BaseExpandableListAdapter {
	
		//返回一级列表的个数
		@Override
		public int getGroupCount() {
			return s_groups.length;
		}
	
		//返回各二级列表的个数
		@Override
		public int getChildrenCount(int groupPosition) {
			return myMaps.mState2.size();
		}
	
		//返回一级列表的单个item（返回的是对象）
		@Override
		public Object getGroup(int groupPosition) {
			return s_groups[groupPosition];
		}
	
		//返回二级列表中的单个item（返回的是对象）
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return myMaps.mState2.get(childPosition);
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
				convertView = getLayoutInflater().inflate(R.layout.s_groups, null);
			}
			TextView ts_group = (TextView) convertView.findViewById(R.id.s_expGroup);
			ts_group.setText(s_groups[groupPosition]);
			return convertView;
		}
	
		//【重要】填充二级列表
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
	
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.s_child, null);
			}
	
			TextView ts_child = (TextView) convertView.findViewById(R.id.s_expChild);
			TextView ts_child2 = (TextView) convertView.findViewById(R.id.s_expChild2);

			ts_child.setText(myMaps.mState2.get(childPosition).inf);
			ts_child2.setText(myMaps.mState2.get(childPosition).time);

			return convertView;
		}
	
		//二级列表中的item是否能够被选中
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

	//导出到文档
	private boolean writeStateFile(String my_Name) {
		try{   
			FileOutputStream fout = new FileOutputStream(my_Name); 
			
			fout.write(myMaps.curMap.Map.getBytes());
			StringBuilder s = new StringBuilder();
			s.append(myMaps.curMap.Map);
			fout.write(s.toString().getBytes());

			int len = myMaps.m_State.ans.length();
			byte t;
			if (len > 0) {
				fout.write('\n');
				fout.write("Solution: ".getBytes());
				for (int k = 0; k < len; k++) {
					t = (byte) myMaps.m_State.ans.charAt(k);
					if ("lurdLURD".indexOf(t) >= 0)
						fout.write(t);
				}
			}

	        fout.flush();
	        fout.close();
		}catch(Exception e){
			return false;
		}
		return true;
	}

	//导出到剪切板: XSB+Lurd
	private void myExport2() {
		StringBuilder str = new StringBuilder();
		try{
			
			str.append(myMaps.curMap.Map).append("\nSolution: ").append(myMaps.m_State.ans);

			final EditText et = new EditText(mySolutionBrow.this);
			et.setText(str.toString());
			new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("剪切板：XSB+Lurd").setView(et).setCancelable(false)
				.setNegativeButton("取消", null)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						myMaps.saveClipper(et.getText().toString());
					}
				}).create().show();
	  	}catch(Exception e){
	  	}
	}

	//导出到剪切板: Lurd
	private void myExport() {
		StringBuilder str = new StringBuilder();
		try{
			str.append(myMaps.m_State.ans);

			final EditText et = new EditText(mySolutionBrow.this);
			et.setText(str.toString());
			new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("剪切板：Lurd").setView(et).setCancelable(false)
				.setNegativeButton("取消", null)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						myMaps.saveClipper(et.getText().toString());
					}
				}).create().show();
	  	}catch(Exception e){
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