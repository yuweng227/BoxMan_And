package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class myAbout2 extends Activity {

	 public static final String action = "jason.broadcast.action";

	 AlertDialog isSaveDlg;
		
	 EditText et_Title = null;
	 EditText et_Author = null;
	 EditText et_Comment = null;
	 TextView tv_Count = null;
	 ImageView iv_Map = null;
	 ImageView iv_Map2 = null;
	 
	 private boolean flg = false;

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
		
		setContentView(R.layout.about);
		
        //开启标题栏的返回键
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
		setTitle(myMaps.sFile + "：" + (myMaps.curMap.Num < 0 ? "关卡资料" : (myMaps.m_lstMaps.indexOf(myMaps.curMap)+1) + " - 关于"));

		et_Title = (EditText)this.findViewById(R.id.etTitle);
		et_Author = (EditText)this.findViewById(R.id.etAuthor);
		et_Comment = (EditText)this.findViewById(R.id.etComment);
		tv_Count = (TextView)this.findViewById(R.id.tvCount);
		iv_Map = (ImageView)this.findViewById(R.id.ivMap);
		iv_Map2 = (ImageView)this.findViewById(R.id.ivMap2);
		
		et_Title.setText(myMaps.curMap.Title);
		et_Author.setText(myMaps.curMap.Author);
		et_Comment.setText(myMaps.curMap.Comment);

		mapNode2 nd = null;
		if (!myMaps.curMap.Map.equals("--")) nd = mySQLite.m_SQL.find_Level(myMaps.curMap);
		int m_Level_Num;
		StringBuilder s = new StringBuilder();
        m_Level_Num = mySQLite.m_SQL.get_Level_Num(myMaps.curMap.P_id, myMaps.curMap.Level_id);  //源关卡在所属关卡集中的序号
		if (m_Level_Num > 0) {
		    s.append("所属关卡集：").append(myMaps.getSetTitle(myMaps.curMap.P_id)).append("\n关卡序号： " + m_Level_Num);
		}
		if (nd == null) {
			tv_Count.setText(s);
			iv_Map = null;
		} else {
			m_Level_Num = mySQLite.m_SQL.get_Level_Num(nd.P_id, nd.Level_id);  //源关卡在所属关卡集中的序号
			s.append("\n\n★疑似与关卡集【").append(nd.SetName).append("】中\n第 " + m_Level_Num + " 关卡（前图）重复\n");
			s.append("标题: ").append(nd.Title).append('\n');
			s.append("作者: ").append(nd.Author);
			tv_Count.setText(s);
			iv_Map.setImageBitmap(getBitmap(nd.Map, false));  //疑似重复关卡图
			iv_Map2.setImageBitmap(getBitmap(myMaps.curMap.Map, true));  //当前关卡图
		}
		
		if (myMaps.m_Sets[0] < 3 || myMaps.sFile.equals("相似关卡")) {  //设置内置关卡只读
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
				myAbout2.this.finish();
            }
		 })
		 .setPositiveButton("是", new DialogInterface.OnClickListener(){
			 @Override
			 public void onClick(DialogInterface dialog, int which) {
				 myMaps.curMap.Title = et_Title.getText().toString().trim();
				 myMaps.curMap.Author = et_Author.getText().toString().trim();
				 myMaps.curMap.Comment = et_Comment.getText().toString().trim();
				 mySQLite.m_SQL.Update_L_inf(myMaps.curMap.Level_id, myMaps.curMap.Title, myMaps.curMap.Author, myMaps.curMap.Comment);

				 myAbout2.this.finish();
			 }
		 });
		 isSaveDlg = builder.create();
	 }
	 
	Rect rt = new Rect();
	Rect rt1 = new Rect();
	int m_Width = 25, Rows, Cols;
	String[] Arr;
	public Bitmap getBitmap(String Map, boolean flg){  
        Bitmap bitmap;

    	try {
			if(Map.equals("--"))
				throw new Exception();
    		Arr = Map.split("\r\n|\n\r|\n|\\|");
    		Rows = Arr.length;
    		Cols = Arr[0].length();
    		int height = m_Width * Rows;
    		int	width  = m_Width * Cols;

            bitmap = Bitmap.createBitmap(width, height, myMaps.cfg);  //为每个关卡创建图标
            Canvas cvs = new Canvas(bitmap);
            
			//画关卡图
    		rt.top = 0;
    		for (int i = 0; i < Rows; i++) {
    		   rt.bottom = rt.top + m_Width;
    		   rt.left = 0;
       		   Cols = Arr[i].length();
    		   for (int j = 0; j < Cols; j++) {
    			   rt.right = rt.left + m_Width;
    			   switch (Arr[i].charAt(j)){
    			   case '_':
    				   	break;
    			   case '#':
    		    	   	rt1.set(0, 0, 50, 50);
    		    	   	cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
    			       	break;
				   case '-':
					   rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   break;
				   case '.':
					   rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   break;
				   case '$':
					   rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   rt1.set(50, 250-myMaps.isSkin_200, 100, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   break;
				   case '*':
					   rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   rt1.set(50, 300-myMaps.isSkin_200, 100, 350-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   break;
				   case '@':
					   rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   rt1.set(100, 250-myMaps.isSkin_200, 150, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   break;
				   case '+':
					   rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
					   rt1.set(100, 300-myMaps.isSkin_200, 150, 350-myMaps.isSkin_200);
					   cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
    			   } //end switch
    			   rt.left = rt.left + m_Width;
    		   	}  //end for j
    		   	rt.top = rt.top + m_Width;
    		} //end for i
    	} catch (Exception e) {
            bitmap = Bitmap.createBitmap(200, 200, myMaps.cfg);
			Canvas cvs99 = new Canvas(bitmap);
			Drawable dw99 = myMaps.res.getDrawable(R.drawable.defbit);
			dw99.setBounds(0, 0, 200, 200);
			dw99.draw(cvs99);
    	}
    	bitmap = myGridViewAdapter.getBitmapThumbnail(bitmap, myMaps.m_nWinWidth < myMaps.m_nWinHeight ? myMaps.m_nWinWidth/2 : myMaps.m_nWinHeight/2, flg);
        return bitmap;  
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
			if (flg) {  //内容有变化，提问是否保存
				if (myMaps.curMap.Level_id < 0) {  //创编关卡，关卡资料更改时，向父窗体发送消息，由父窗体负责这些资料的保存（保存的关卡文档）
					myMaps.curMap.Title = "" + et_Title.getText().toString().trim();
					myMaps.curMap.Author = "" + et_Author.getText().toString().trim();
					myMaps.curMap.Comment = "" + et_Comment.getText().toString().trim();
					Intent intent = new Intent(action);
					sendBroadcast(intent);
					myAbout2.this.finish();
				} else isSaveDlg.show();  //其它情况下，提问后，直接写 DB 保存
			}
 			else this.finish();
	 		 
	 		return true;
	 	}
	 	return super.onKeyDown(keyCode, event);
	 }
}
