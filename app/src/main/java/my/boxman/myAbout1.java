package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class myAbout1 extends Activity {
	
	 AlertDialog isSaveDlg;
	
	 EditText et_Title = null;
	 EditText et_Author = null;
	 EditText et_Comment = null;
	 TextView tv_Count = null;
	 
	 boolean flg = false;

	private String  mMessage;

	private TextWatcher watcher = new TextWatcher(){
		@Override
		public void afterTextChanged(Editable arg0) {
			flg = true;
		}
		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		}   
	 };  

	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//接收数据
		Bundle bundle = this.getIntent ().getExtras ();
		mMessage = bundle.getString ("mMessage");  //关卡数量

	    setContentView(R.layout.about);
		
        //开启标题栏的返回键
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
		setTitle(myMaps.sFile + " - 关于");

		et_Title = (EditText)this.findViewById(R.id.etTitle);
		et_Author = (EditText)this.findViewById(R.id.etAuthor);
		et_Comment = (EditText)this.findViewById(R.id.etComment);
		tv_Count = (TextView)this.findViewById(R.id.tvCount);
		
		et_Title.setText(myMaps.J_Title);
		et_Author.setText(myMaps.J_Author);
		et_Comment.setText(myMaps.J_Comment);
		
		if (myMaps.m_Sets[0] < 3 || myMaps.sFile.equals("最近推过的关卡") || myMaps.sFile.equals("创编关卡") || myMaps.sFile.equals("相似关卡") || myMaps.sFile.equals("关卡查询")) {  //设置内置关卡只读
			et_Title.setCursorVisible(false);      
			et_Title.setFocusable(false);         
			et_Title.setFocusableInTouchMode(false);    
			et_Author.setCursorVisible(false);      
			et_Author.setFocusable(false);         
			et_Author.setFocusableInTouchMode(false);    
			et_Comment.setCursorVisible(false);      
			et_Comment.setFocusable(false);         
			et_Comment.setFocusableInTouchMode(false);    
		}
		
		et_Title.addTextChangedListener(watcher);
		et_Author.addTextChangedListener(watcher);
		et_Comment.addTextChangedListener(watcher);
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		 AlertDialog.Builder builder = new Builder(this, AlertDialog.THEME_HOLO_DARK);
		 builder.setMessage("内容已修改，是否保存？").setCancelable(false)
		 .setNegativeButton("取消", null)
		 .setNeutralButton("否", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	myAbout1.this.finish();
            }
		 })
		 .setPositiveButton("是", new DialogInterface.OnClickListener(){
			 @Override
			 public void onClick(DialogInterface dialog, int which) {
				String str = et_Title.getText().toString().trim();
		    	if (str.length() > 0 && mySQLite.m_SQL.find_Set(str, myMaps.m_Set_id) > 0) {
		    		AlertDialog.Builder builder2 = new Builder(myAbout1.this, AlertDialog.THEME_HOLO_DARK);
		    		builder2.setMessage("关卡集标题(Title)发现重名！\n【" + str + "】").setPositiveButton("确定", null).setCancelable(false).show();
		    	} else {
					myMaps.J_Title = str;
					myMaps.J_Author = et_Author.getText().toString().trim();
					myMaps.J_Comment = et_Comment.getText().toString().trim();
					mySQLite.m_SQL.Update_T_Inf(myMaps.m_Set_id, myMaps.J_Title, myMaps.J_Author, myMaps.J_Comment);
					myMaps.mSets3.get(myMaps.m_Sets[1]).title = str;
					myMaps.sFile = str;
					myAbout1.this.finish();
		    	}
			 }
		 });
		 isSaveDlg = builder.create();
	 }
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
		 //标题栏返回键功能
		 case android.R.id.home:
	 		//内容有变化，提问是否保存
	 		if (flg) isSaveDlg.show();
 			else this.finish();
	 		
			return true;
		 default:
			return super.onOptionsItemSelected(item);
		 }
	}
	 
	@Override 
	protected void onResume() {
		super.onResume();
		tv_Count.setText("关卡完成情况: " + mMessage);
	}

	@Override    
	protected void onDestroy() { 
		setContentView(R.layout.my_grid_view);
		super.onDestroy();
	}
	 
   public boolean onKeyUp(int keyCode, KeyEvent event) {
	 	if (keyCode == KeyEvent.KEYCODE_BACK)
	 		return true;
	 	
	 	return super.onKeyUp(keyCode, event);
   }

   public boolean onKeyDown(int keyCode, KeyEvent event) {
	 	if (keyCode == KeyEvent.KEYCODE_BACK) {
	 		//内容有变化，提问是否保存
	 		if (flg) isSaveDlg.show();
 			else this.finish();
	 		
	 		return true;
	 	}
	 	return super.onKeyDown(keyCode, event);
   }
}
