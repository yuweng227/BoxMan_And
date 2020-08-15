package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class myExport extends Activity implements myGifMakeFragment.GifMakeStatusUpdate{
	 EditText et_Action = null;
	 CheckBox cb_XSB = null;
	 CheckBox cb_Lurd = null;
	 CheckBox cb_File = null;
	 CheckBox cb_Cur = null;
	 CheckBox cb_Trun = null;
	 Button bt_OK;

	 int m_Gif_Start;  //导出 GIF 的起点
	 boolean is_ANS;  //是否答案
	 String my_Local = "";  //关卡正推现场
	 String my_Loca8 = "";  //关卡正推现场 -- 旋转
	 String my_XSB = "";  //关卡 XSB
	 String my_Lurd = "";  //lurd
	 String my_imPort_YASS = "";  //是否为“导入”或“YASS”动作
	 StringBuilder my_AND;  //答案时的链接字符串

	private myGifMakeFragment mDialog;
	String m_Act;
	int m_nItemSelect;  //对话框中的出item选择前的记忆
	int m_Gif_Mark = 1;  //水印
	int m_Gif_Interval = 300;  //制作GIF动画相关参数：帧间隔（毫秒）、帧方式（逐推、逐移）
	boolean m_Gif_Type = true;
    boolean m_Gif_Skin = false;
	boolean[] my_Rule;   // 仅合成 GIF 使用
	short[] my_BoxNum;   // 仅合成 GIF 使用

	Menu my_Menu = null;

	private static final String TAG_PROGRESS_DIALOG_FRAGMENT = "exp_gif_make_progress_fragment";

	@Override
	 protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.export_view);

		//接收数据
		Bundle bundle = this.getIntent().getExtras();
		my_XSB = bundle.getString("m_XSB");  //关卡 XSB
		my_Lurd = bundle.getString("m_Lurd");  //lurd
		my_Local = bundle.getString("LOCAL");  //接收关卡现场
		is_ANS = bundle.getBoolean("is_ANS");  //是否答案
		my_AND = new StringBuilder("\n");

		if (my_Local == null) {  // 浏览界面的导出
			my_Rule = null;  //需要显示标尺的格子
			my_BoxNum = null;  //迷宫箱子编号（人为）
			m_Gif_Start = 0;  //导出 GIF 的起点
			my_Loca8 = "";  //接收关卡现场 -- 旋转
			my_imPort_YASS = "";  //是否为“导入”或“YASS”动作
		} else {  // 推关卡界面的导出
			my_Rule = bundle.getBooleanArray("my_Rule");  //需要显示标尺的格子
			my_BoxNum = bundle.getShortArray("my_BoxNum");  //迷宫箱子编号（人为）
			m_Gif_Start = bundle.getInt("m_Gif_Start");  //导出 GIF 的起点
			my_Loca8 = bundle.getString("LOCAL8");  //接收关卡现场 -- 旋转
			my_imPort_YASS = bundle.getString("m_InPort_YASS");  //是否为“导入”或“YASS”动作
			if (is_ANS) {
				int len = my_Lurd.length();
				int p = 0;
				for (int k = 0; k < len; k++) {
					if ("LURD".indexOf(my_Lurd.charAt(k)) >= 0) p++;
				}
				if (my_imPort_YASS.toLowerCase().indexOf("yass") >= 0) {
					my_imPort_YASS = "YASS" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				} else if (my_imPort_YASS.toLowerCase().indexOf("导入") >= 0) {
					my_imPort_YASS = "导入" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				} else {
					my_imPort_YASS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				}
				my_AND.append("Solution (moves ").append(len).append(", pushes ").append(p);
				if (myMaps.m_Sets[30] == 1) {  //是否导出答案备注
					my_AND.append(", comment ").append(my_imPort_YASS);
				}
				my_AND.append("): \n");
			}
		}

		 //开启标题栏的返回键
		 ActionBar actionBar = getActionBar();
		 actionBar.setDisplayHomeAsUpEnabled(true);
         actionBar.setDisplayShowHomeEnabled(false);
		 setTitle("导出");

		 //界面初始化
		 et_Action = (EditText)this.findViewById(R.id.ex_edit_text);
		 cb_XSB = (CheckBox) findViewById(R.id.cb_ex_xsb);
		 cb_Lurd = (CheckBox) findViewById(R.id.cb_ex_lurd);
		 cb_File = (CheckBox) findViewById(R.id.cb_ex_File);
		 cb_Cur = (CheckBox) findViewById(R.id.cb_ex_Cur);
		 cb_Trun = (CheckBox) findViewById(R.id.cb_ex_Trun);
		 bt_OK = (Button) findViewById(R.id.bt_ex_OK);

		 et_Action.setTypeface(Typeface.MONOSPACE);  //等宽字符集

		 //不接受编辑
		 et_Action.setCursorVisible(false);
		 et_Action.setFocusable(false);
		 et_Action.setFocusableInTouchMode(false);

		 cb_File.setChecked(false);
		 cb_Cur.setChecked(false);
		 cb_Trun.setEnabled(false);

		if (my_Local == null) {  // 浏览界面的导出
			if (my_Lurd.isEmpty()) {  // 无答案
				et_Action.setText(my_XSB);  //自动加载编辑框
				myMaps.isLurd = false;
				cb_Lurd.setChecked(false);
				cb_Lurd.setEnabled(false);
			} else {  // 有答案
				et_Action.setText(my_XSB + my_AND + my_Lurd);  //自动加载编辑框
				myMaps.isLurd = true;
				cb_Lurd.setChecked(true);
				cb_Lurd.setEnabled(true);
			}
			myMaps.isXSB = true;
			cb_XSB.setChecked(true);
			cb_XSB.setEnabled(false);
		} else {  // 推关卡界面的导出
			cb_XSB.setEnabled(true);
			if (my_Lurd.isEmpty()) {
				et_Action.setText(my_XSB);  //自动加载编辑框
				myMaps.isLurd = false;
				cb_Lurd.setEnabled(false);
				myMaps.isXSB = true;
				cb_XSB.setChecked(true);
			} else {
				et_Action.setText(my_Lurd);  //自动加载编辑框
				myMaps.isXSB = false;
				cb_Lurd.setEnabled(true);
				myMaps.isLurd = true;
				cb_Lurd.setChecked(true);
			}
		}

		final LinearLayout lin = (LinearLayout) findViewById(R.id.ex_is_grid);
		if (my_Local == null) {  // 浏览界面的导出
			lin.setVisibility(View.GONE);
			if (my_Menu != null) my_Menu.setGroupVisible(0, false);
		} else {     // 推关卡界面的导出
			lin.setVisibility(View.VISIBLE);
			if (my_Menu != null) my_Menu.setGroupVisible(0, true);
		}

		cb_XSB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			 @Override
			 public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				 if (my_Lurd.isEmpty()) {
				 	cb_XSB.setChecked(true);
				 } else {
					 myMaps.isXSB = isChecked;
					 if (!isChecked && !cb_Lurd.isChecked()) {
						 cb_Lurd.setChecked(true);
					 }
					 if (myMaps.isXSB) {
						 if (myMaps.isLurd && !my_Lurd.isEmpty()) {
							 et_Action.setText(my_XSB + my_AND + my_Lurd);
						 } else {
							 et_Action.setText(my_XSB);
						 }
					 } else {
						 et_Action.setText(my_Lurd);
					 }
				 }
			 }
		 });

		 cb_Lurd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			 @Override
			 public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				 if (my_Local == null) {  // 浏览界面的导出，能够到这里，肯定是有答案的
					 myMaps.isLurd = isChecked;
					 if (myMaps.isLurd) {
						 et_Action.setText(my_XSB + my_AND + my_Lurd);
					 } else {
						 et_Action.setText(my_XSB);
					 }
				 } else {
					 myMaps.isLurd = isChecked;
					 if (!isChecked && !cb_XSB.isChecked()) {
						 cb_XSB.setChecked(true);
					 }     // 推关卡界面的导出
					 if (myMaps.isXSB) {
						 if (myMaps.isLurd && !my_Lurd.isEmpty()) {
							 et_Action.setText(my_XSB + my_AND + my_Lurd);
						 } else {
							 et_Action.setText(my_XSB);
						 }
					 } else {
						 et_Action.setText(my_Lurd);
					 }
				 }
			 }
		 });

		 cb_Cur.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			 @Override
			 public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				 if (isChecked) {
					 cb_Trun.setEnabled(true);
					 cb_XSB.setEnabled(false);
					 if (!my_Lurd.isEmpty()) cb_Lurd.setEnabled(false);
					 if (cb_Trun.isChecked()) {
						 et_Action.setText(my_Loca8);
					 } else {
						 et_Action.setText(my_Local);
					 }
				 } else {
					 cb_Trun.setEnabled(false);
					 cb_XSB.setEnabled(true);
					 if (!my_Lurd.isEmpty())cb_Lurd.setEnabled(true);
					 if (myMaps.isXSB) {
						 if (myMaps.isLurd && !my_Lurd.isEmpty()) {
							 if (is_ANS) {  //若是答案
								 et_Action.setText(my_XSB + "\nSolution: " + my_Lurd);
							 } else {
								 et_Action.setText(my_XSB + "\n" + my_Lurd);
							 }
						 } else {
							 et_Action.setText(my_XSB);
						 }
					 } else {
						 et_Action.setText(my_Lurd);
					 }
				 }
			 }
		 });

		 cb_Trun.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			 @Override
			 public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				 if (isChecked) {
					 et_Action.setText(my_Loca8);
				 } else {
					 et_Action.setText(my_Local);
				 }
			 }
		 });

		 //执行导出
		 bt_OK.setOnClickListener(new View.OnClickListener() {
			 @Override
			 public void onClick(View v) {
				 if (cb_File.isChecked()) {  //导出到文档
					 //按关卡集和关卡序号生成文档名
					 final String my_Name = new StringBuilder("导出/").append(myMaps.sFile).append("_").append(myMaps.m_lstMaps.indexOf(myMaps.curMap)+1).append(cb_Lurd.isChecked () ? ".txt" : ".xsb").toString();
					 File file = new File(myMaps.sRoot + myMaps.sPath + my_Name);
					 if (file.exists()) {
						 new AlertDialog.Builder(myExport.this, AlertDialog.THEME_HOLO_DARK).setNegativeButton("取消", null)
								 .setPositiveButton("覆写", new DialogInterface.OnClickListener(){
									 @Override
									 public void onClick(DialogInterface arg0, int arg1) {
										 saveFile(et_Action.getText().toString(), my_Name);
									 }})
								 .setTitle("警告")
								 .setMessage("文档已存在，覆写吗？\n" + my_Name).setCancelable(false)
								 .show(); //自设关卡已存在
					 } else {
						 saveFile(et_Action.getText().toString(), my_Name);
					 }
				 } else {  //导出到剪切板
					 myMaps.saveClipper("\n" + et_Action.getText().toString() + "\n");
					 finish();
				 }
			 }
		 });
	 }

	//导出状态到文档
	private void saveFile(String str, String fn) {
		try{
			FileOutputStream fout = new FileOutputStream(myMaps.sRoot + myMaps.sPath + fn);

			fout.write(str.getBytes());

			fout.flush();
			fout.close();

			new AlertDialog.Builder(myExport.this, AlertDialog.THEME_HOLO_DARK).setNegativeButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					})
					.setTitle("成功")
					.setMessage(fn).setCancelable(false)
					.show();
//			MyToast.showToast(this, "导出成功！\n" + fn, Toast.LENGTH_SHORT);
		} catch (Exception e){
			MyToast.showToast(this, "出错了，导出失败！", Toast.LENGTH_SHORT);
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		setContentView(R.layout.game_view);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.export, menu);
		my_Menu = menu;

		if (my_Local == null) {  // 浏览界面的导出
			menu.setGroupVisible(0, false);
		} else {     // 推关卡界面的导出
			menu.setGroupVisible(0, true);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem mt) {
		switch (mt.getItemId()) {
			//菜单栏返回键功能
			case android.R.id.home:
				this.finish();
				return true;
			case R.id.exp_gif_make:  //导出为动画
				int n = my_Lurd.indexOf ('\n');
				if (n >= 0) m_Act = my_Lurd.substring(0, n);
				else {
					n = my_Lurd.indexOf ('[');
					if (n >= 0) m_Act = my_Lurd.substring(0, n);
					else m_Act = my_Lurd;
				}

				int l = m_Act.length ();
				if (l <= 0 || l < m_Gif_Start)  {
					MyToast.showToast(this, "没有制作动画的正推动作！", Toast.LENGTH_SHORT);
					return true;  //没有正推动作，直接返回
				}

				File targetDir = new File(myMaps.sRoot+myMaps.sPath + "GIF/");
				if (!targetDir.exists()) targetDir.mkdirs();  //创建自定义GIF文件夹

				final String[] m_menu2 = {  //帧间隔（毫秒）
						"自动", "100", "200", "300", "500", "1000", "2000"
				};

				View view3 = View.inflate(this, R.layout.gif_set_dialog, null);
				final CheckBox m_gif_act_type = (CheckBox) view3.findViewById(R.id.gif_act_type);         //帧类型
                final CheckBox m_gif_act_skin = (CheckBox) view3.findViewById(R.id.gif_act_skin);         //现场皮肤
				final RadioButton gif_mark_1 = (RadioButton) view3.findViewById(R.id.gif_mark_1);          //无水印
				final RadioButton gif_mark_2 = (RadioButton) view3.findViewById(R.id.gif_mark_2);          //默认水印
				final RadioButton gif_mark_3 = (RadioButton) view3.findViewById(R.id.gif_mark_3);          //自定义水印

                m_gif_act_skin.setChecked (m_Gif_Skin);
                m_gif_act_skin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        m_Gif_Skin = isChecked;
                    }
                });

                m_gif_act_type.setChecked (m_Gif_Type);
				m_gif_act_type.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						m_Gif_Type = isChecked;
					}
				});

				if (m_Gif_Mark == 0) gif_mark_1.setChecked (true);
				else if (m_Gif_Mark == 2) gif_mark_3.setChecked (true);
				else gif_mark_2.setChecked (true);

				m_nItemSelect = 3;
				if (m_Gif_Interval == 0) {
					m_nItemSelect = 0;
					m_gif_act_type.setEnabled(false);
				} else {
					for (int k = 1; k < m_menu2.length; k++) {
						if (Integer.valueOf(m_menu2[k]) == m_Gif_Interval) m_nItemSelect = k;
					}
					m_gif_act_type.setEnabled(true);
				}

				new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle ("帧间隔")
						.setView (view3)
						.setSingleChoiceItems (m_menu2, m_nItemSelect, new DialogInterface.OnClickListener () {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == 0) {
									m_Gif_Interval = 0;
									m_gif_act_type.setEnabled(false);
								} else {
									m_Gif_Interval = Integer.valueOf(m_menu2[which]);
									m_gif_act_type.setEnabled(true);
								}
							}
						}).setNegativeButton ("取消", null)
						.setPositiveButton ("制作", new DialogInterface.OnClickListener () {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss ();
								if (mDialog == null) {
									mDialog = new myGifMakeFragment ();
									if (gif_mark_1.isChecked ()) m_Gif_Mark = 0;
									else if (gif_mark_3.isChecked ()) m_Gif_Mark = 2;
									else m_Gif_Mark = 1;

									//异步合成 GIF
									Bundle bundle = new Bundle ();
									bundle.putInt("m_Gif_Mark", m_Gif_Mark);  //水印
									bundle.putInt("m_Gif_Start", m_Gif_Start);  //导出 GIF 的起点
									bundle.putBoolean ("m_Type", m_Gif_Type);
                                    bundle.putBoolean ("m_Skin", m_Gif_Skin);
									bundle.putBooleanArray("my_Rule", my_Rule);  //需要显示标尺的格子
									bundle.putShortArray("my_BoxNum", my_BoxNum);  //迷宫箱子编号（人为）
									bundle.putInt ("m_Interval", m_Gif_Interval);
									bundle.putString ("m_Ans", m_Act);
									mDialog.setArguments (bundle);
									mDialog.show (getFragmentManager (), TAG_PROGRESS_DIALOG_FRAGMENT);
								}
							}
						}).setCancelable(false).show();

				return true;
			default:
				return super.onOptionsItemSelected(mt);
		}
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
	 	if (keyCode == KeyEvent.KEYCODE_BACK)
	 		return true;
	 	
	 	return super.onKeyUp(keyCode, event);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
	 	if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
	 		 
	 		return true;
	 	}
	 	return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onGifMakeDone(String inf) {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		builder.setTitle("信息").setMessage(inf).setPositiveButton("确定", null);
		builder.setCancelable(false).create().show();
	}
}
