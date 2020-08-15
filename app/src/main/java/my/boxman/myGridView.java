package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class myGridView extends Activity implements OnScrollListener, myFindFragment.FindStatusUpdate {
	Menu MyMenu = null;
	ContextMenu MyContMenu;
	GridView mGridView = null;
	ListView mListView = null;
	ItemClickListener mItemClickListener = null;
	ItemLongClickListener mItemLongClickListener = null;

	//缓存 GridView 中每个 Item 的图片
	public static SparseArray <Bitmap> gridviewBitmapCaches = new SparseArray <Bitmap>();
	int m_Num, mWhich;
	int childPos;
	View my_View = null;

	myGridViewAdapter adapter = null;

	ListView m_Similarity;     //相似度选项
	myMaps.MyAdapter mAdapter;
	MyAdapter2 mAdapter2;
	ArrayList<Long> mySets;   //关卡集id

	private myFindFragment mDialog;
	private static final String TAG_PROGRESS_DIALOG_FRAGMENT = "find_progress_fragment";

	public long currentTime = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

		setContentView(R.layout.my_grid_view);

		//设置标题栏标题为关卡集名
		setTitle(myMaps.sFile);  // + " （" + myMaps.m_lstMaps.size() + "）"

		//开启标题栏的返回键
		ActionBar actionBar = getActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);

		adapter = new myGridViewAdapter(this);
		mItemClickListener = new ItemClickListener();
		mItemLongClickListener = new ItemLongClickListener();
		mGridView = (GridView)findViewById(R.id.m_gridView);
		mListView = (ListView)findViewById(R.id.m_listview);
		updateLayout();

		registerForContextMenu(mGridView);

		// 通过屏幕宽度（PX），计算地图素材尺寸
		DisplayMetrics metric = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		adapter.m_PicWidth = metric.widthPixels / 6;  //按每行放 6 个预览图标（间距占去一个图标宽度，实际显示5个图标）

		recycleBitmapCaches(0, myMaps.m_lstMaps.size());
		myMaps.curMapNum = -1;  //默认为非关卡编辑状态
		myMaps.isSelect = false;  //是否多选模式
	}

	//释放图片的函数
	private void recycleBitmapCaches(int fromPosition,int toPosition){
		Bitmap delBitmap;
		for(int del=fromPosition;del<toPosition;del++){
			delBitmap = gridviewBitmapCaches.get(del);
			if(delBitmap != null){
				gridviewBitmapCaches.remove(del);
				try {
					delBitmap.recycle();
				} catch (Exception e){
				}
			}
		}
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	//当AdapterView被单击(触摸屏或者键盘)，则返回的Item单击事件
	class ItemClickListener implements OnItemClickListener {
		public void onItemClick(AdapterView<?> arg0,//The AdapterView where the click happened
								View arg1,//The view within the AdapterView that was clicked
								int arg2,//The position of the view in the adapter
								long arg3) {//The row id of the item that was clicked

			//防止点击过快
			if (!myMaps.isSelect && System.currentTimeMillis() - currentTime < 2000) return;
			currentTime = System.currentTimeMillis();

			m_Num = arg2;
			if (myMaps.sFile.equals("相似关卡")) {  //相似关卡的浏览时
				myMaps.iskinChange = false;
				myMaps.curMap = myMaps.m_lstMaps.get(arg2);
				Intent intent1 = new Intent();
				intent1.setClass(myGridView.this, myFindView.class);
				startActivity(intent1);
			} else if (myMaps.m_lstMaps.get(arg2).Level_id > 0) {  //正常推关卡时
				if (myMaps.isSelect) {
					myMaps.m_lstMaps.get(arg2).Select = !myMaps.m_lstMaps.get(arg2).Select;
					if (myMaps.m_Sets[2] == 0) {  //仅刷新被单击的条目，避免闪烁
						adapter.getView(arg2, arg1, mGridView).invalidate();
					} else {
						adapter.getView(arg2, arg1, mListView).invalidate();
					}
				} else {
					myMaps.iskinChange = false;
					myMaps.curMap = myMaps.m_lstMaps.get(arg2);
					//遇到无效关卡，暂时提示后退出，下一步，直接显示关卡的备注信息（解析到无效关卡时，会将关卡的所有资料放到该关卡的“备注”中）
					if (myMaps.curMap.Map.equals("--")) {
						Intent intent = new Intent();
						intent.setClass(myGridView.this, myAbout2.class);
						startActivity(intent);
					} else {
						Intent intent1 = new Intent();
						intent1.setClass(myGridView.this, myGameView.class);
						startActivity(intent1);
					}
				}
			} else {  //编辑关卡
				if (myMaps.isSelect) {
					myMaps.m_lstMaps.get(arg2).Select = !myMaps.m_lstMaps.get(arg2).Select;
					if (myMaps.m_Sets[2] == 0) {  //仅刷新被单击的条目，避免闪烁
						adapter.getView(arg2, arg1, mGridView).invalidate();
					} else {
						adapter.getView(arg2, arg1, mListView).invalidate();
					}
				} else {
					mapNode nd = myMaps.m_lstMaps.get(arg2);
					myMaps.curMap = new mapNode(nd.Rows, nd.Cols, nd.Map.split("\r\n|\n\r|\n|\r|\\|"), nd.Title, nd.Author, nd.Comment);
					//取得当前关卡文档名
					myMaps.curMap.fileName = nd.fileName;
					myMaps.curMapNum = arg2;  //继续编辑
					Intent intent2 = new Intent();
					intent2.setClass(myGridView.this, myEditView.class);
					startActivity(intent2);
				}
			}
		}
	}

	//当AdapterView长按事件
	class ItemLongClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,int arg2, long arg3){ //arg2代表长按的位置

			my_View = arg1;
			m_Num = arg2;
			mGridView.showContextMenu();

			MyContMenu.getItem(1).setTitle("改编为新关卡");
			MyContMenu.getItem(2).setTitle("图标加锁");
			MyContMenu.getItem(0).setVisible(false);
			MyContMenu.getItem(1).setVisible(false);
			MyContMenu.getItem(2).setVisible(false);
			MyContMenu.getItem(3).setVisible(false);
			MyContMenu.getItem(4).setVisible(false);
			MyContMenu.getItem(5).setVisible(false);
			MyContMenu.getItem(6).setVisible(false);
			MyContMenu.getItem(7).setVisible(false);
			MyContMenu.getItem(8).setVisible(false);
			MyContMenu.getItem(9).setVisible(false);
			MyContMenu.getItem(10).setVisible(false);
			MyContMenu.getItem(11).setVisible(false);

			if (myMaps.sFile.equals("相似关卡")) {
				MyContMenu.getItem(0).setVisible(true);
			} else if (myMaps.sFile.equals("关卡查询")) {
				MyContMenu.getItem(0).setVisible(true);
				MyContMenu.getItem(1).setVisible(true);
				MyContMenu.getItem(9).setVisible(true);
			}  else if (myMaps.sFile.equals("创编关卡")) {
				MyContMenu.getItem(1).setVisible(true);  //改为“编辑”
				MyContMenu.getItem(1).setTitle("编辑");
				MyContMenu.getItem(8).setVisible(true);
				MyContMenu.getItem(9).setVisible(true);
			} else {
				if (myMaps.m_lstMaps.get(arg2).L_CRC_Num < 0) {  //遇到无效关卡时
					MyContMenu.getItem(1).setVisible(true);
					MyContMenu.getItem(8).setVisible(true);
				} else {
					MyContMenu.getItem(0).setVisible(true);
					MyContMenu.getItem(1).setVisible(true);
					MyContMenu.getItem(5).setVisible(true);
					MyContMenu.getItem(9).setVisible(true);
					if (myMaps.isExSet(myMaps.m_lstMaps.get(arg2).P_id)) {  //若为扩展关卡集内的关卡
						if (myMaps.m_lstMaps.get(arg2).Lock) MyContMenu.getItem(2).setTitle("图标解锁");
						MyContMenu.getItem(2).setVisible(true);  //图标加锁、解锁切换
						MyContMenu.getItem(3).setVisible(true);
						MyContMenu.getItem(4).setVisible(true);
						MyContMenu.getItem(6).setVisible(true);
						MyContMenu.getItem(7).setVisible(true);
						MyContMenu.getItem(8).setVisible(true);
					}
					if (myMaps.sFile.equals("最近推过的关卡")) {
						MyContMenu.getItem(3).setVisible(false);
						MyContMenu.getItem(5).setVisible(false);
						MyContMenu.getItem(6).setVisible(false);
						MyContMenu.getItem(7).setVisible(false);
						MyContMenu.getItem(8).setVisible(false);
					}
				}
			}
			if (myMaps.isSelect) {  //是否多选模式
				MyContMenu.getItem(10).setVisible(true);  //连续选择...
				MyContMenu.getItem(11).setVisible(true);  //反选
			} else {
				MyContMenu.getItem(10).setVisible(false);
				MyContMenu.getItem(11).setVisible(false);
			}

			return true;
		}
	}

	//设置菜单项是否可见
	private void setMenu(Menu menu) {
		if (myMaps.isSelect) {  //是否多选模式
			menu.getItem(3).setChecked(true);
		} else {
			menu.getItem(3).setChecked(false);
		}
		if (myMaps.m_Sets[2] == 0) {  //是否显示关卡标题
			menu.getItem(4).setChecked(false);
		} else {
			menu.getItem(4).setChecked(true);
		}
		if (myMaps.m_Sets[12] == 0) {  //是否标识重复关卡
			menu.getItem(5).setChecked(false);
		} else {
			menu.getItem(5).setChecked(true);
		}
		if (myMaps.sFile.equals("最近推过的关卡")) {  //最近推过的关卡允许“清空列表”菜单项
			menu.getItem(8).setVisible(true);
		} else {
			menu.getItem(8).setVisible(false);
		}
		menu.getItem(3).setVisible(false);  //是否多选模式
		if (myMaps.sFile.equals("创编关卡")) {
			menu.getItem(5).setVisible(false);  //是否标识重复关卡
			menu.getItem(6).setVisible(false);  //打开首个未解关卡
			menu.getItem(9).setVisible(true);  //新建关卡
			menu.getItem(10).setVisible(true);  //批量删除
		} else {
			menu.getItem(5).setVisible(true);
			menu.getItem(6).setVisible(true);
			menu.getItem(9).setVisible(false);
			if (myMaps.m_Sets[0] == 3) {  //是否显示“批量删除”
				menu.getItem(10).setVisible(true);
			} else {
				menu.getItem(10).setVisible(false);
			}
		}
		if (myMaps.m_Sets[0] == 3 || myMaps.sFile.equals("创编关卡")) {  //扩展关卡集
			menu.getItem(3).setVisible(true);  //是否多选模式
		}
		if (myMaps.sFile.equals("最近推过的关卡") || myMaps.sFile.equals("关卡查询") || myMaps.sFile.equals("创编关卡")) {
			menu.getItem(7).setVisible(false);  //上次推的关卡
		} else {
			menu.getItem(7).setVisible(true);  //上次推的关卡
		}
		menu.getItem(11).setVisible(false);
		if (myMaps.sFile.equals("关卡查询")) {  //关卡查询，允许“保存为关卡集”菜单项
			menu.getItem(3).setVisible(false);  //是否多选模式
			menu.getItem(10).setVisible(false);  //批量删除
			menu.getItem(11).setVisible(true);  //保存到关卡集
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.levels, menu);

		MyMenu = menu;

		setMenu(MyMenu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem mt) {
		switch (mt.getItemId()) {
			//标题栏返回键功能
			case android.R.id.home:
				this.finish();
				return true;
			case R.id.levels_top:
				if (myMaps.m_Sets[2] == 0) {
					mGridView.setSelection(0);
				} else {
					mListView.setSelection(0);
				}
				return true;
			case R.id.levels_bottom:
				if (myMaps.m_Sets[2] == 0) {
					mGridView.setSelection(mGridView.getCount()-1);
				} else {
					mListView.setSelection(mListView.getCount()-1);
				}
				return true;
			case R.id.levels_goto:
				View view = View.inflate(this, R.layout.goto_dialog, null);
				final EditText input_steps = (EditText) view.findViewById(R.id.dialog_steps);  //位置
				AlertDialog.Builder dlg = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
				dlg.setView(view).setCancelable(true);
				dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface di, int keyCode, KeyEvent event) {
						if(keyCode == KeyEvent.KEYCODE_ENTER){
							int n = -1;
							try {
								n = Integer.parseInt(input_steps.getText().toString());
								if (n < 1) n = 1;
								else {
									if (myMaps.m_Sets[2] == 0) {
										if (n > mGridView.getCount()) n = mGridView.getCount();
									} else {
										if (n > mListView.getCount()) n = mListView.getCount();
									}
								}
							} catch (Exception e) { }
							if (n > 0) {
								if (myMaps.m_Sets[2] == 0) {
									mGridView.setSelection(n-1);
								} else {
									mListView.setSelection(n-1);
								}
							}
							di.dismiss();
							return true;
						}
						return false;
					}
				});
				dlg.setTitle("跳至").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int n = -1;
						try {
							n = Integer.parseInt(input_steps.getText().toString());
							if (n < 1) n = 1;
							else {
								if (myMaps.m_Sets[2] == 0) {
									if (n > mGridView.getCount()) n = mGridView.getCount();
								} else {
									if (n > mListView.getCount()) n = mListView.getCount();
								}
							}
						} catch (Exception e) { }
						if (n > 0) {
							if (myMaps.m_Sets[2] == 0) {
								mGridView.setSelection(n-1);
							} else {
								mListView.setSelection(n-1);
							}
						}
						dialog.dismiss();
					}
				}).setCancelable(false).create().show();
				return true;
			case R.id.levels_select:  //多选模式
				if (myMaps.isSelect) {
					myMaps.isSelect = false;
					mt.setChecked(false);
				} else {
					myMaps.isSelect = true;
					mt.setChecked(true);
				}
				adapter.notifyDataSetChanged();
				return true;
			case R.id.levels_showtitle:  //显示关卡标题
				if (myMaps.m_Sets[2] == 1) {
					myMaps.m_Sets[2] = 0;
					mt.setChecked(false);
				} else {
					myMaps.m_Sets[2] = 1;
					mt.setChecked(true);
				}
				updateLayout();
				mGridView.invalidateViews();
				return true;
			case R.id.levels_showdup:
				//标识重复关卡
				if (myMaps.m_Sets[12] == 1) {
					myMaps.m_Sets[12] = 0;
					mt.setChecked(false);
				} else {
					myMaps.m_Sets[12] = 1;
					mt.setChecked(true);
				}
				recycleBitmapCaches(0, myMaps.m_lstMaps.size());
				mGridView.invalidateViews();
				return true;
			case R.id.levels_first_nosolved:  //打开首个未解关卡
				int k = 0;
				int len = myMaps.m_lstMaps.size();
				while (k < len && myMaps.m_lstMaps.get(k).Solved) {
					k++;
				}
				if (k < len) {
					myMaps.iskinChange = false;
					myMaps.curMap = myMaps.m_lstMaps.get(k);
					Intent intent1 = new Intent();
					intent1.setClass(myGridView.this, myGameView.class);
					startActivity(intent1);
				} else {
					MyToast.showToast(this, "关卡已经全部解开！", Toast.LENGTH_SHORT);
				}
				return true;
			case R.id.levels_recent:  //打开上次推的关卡
				long m_id = mySQLite.m_SQL.get_Recent(myMaps.m_lstMaps.get(0).P_id);

				int k2 = 0;
				if (m_id > 0) {
					int len2 = myMaps.m_lstMaps.size();
					while (k2 < len2) {
						if (myMaps.m_lstMaps.get(k2).Level_id == m_id) break;
						k2++;
					}
					if (k2 >= len2) k2 = 0;  //默认打开第一个关卡
				}

				myMaps.iskinChange = false;
				myMaps.curMap = myMaps.m_lstMaps.get(k2);
				Intent intent3 = new Intent();
				intent3.setClass(myGridView.this, myGameView.class);
				startActivity(intent3);

				return true;
			case R.id.levels_clear:  //清空列表
				AlertDialog.Builder builder = new Builder(this, AlertDialog.THEME_HOLO_DARK);
				builder.setTitle("确认")
						.setMessage("清空列表，确定吗？")
						.setPositiveButton("确定", new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								mySQLite.m_SQL.Clear_L_DateTime();
								myMaps.m_lstMaps.clear();
								recycleBitmapCaches(0, myMaps.m_lstMaps.size());
								adapter.notifyDataSetChanged();
							}}).setCancelable(false).create().show();

				return true;
			case R.id.levels_builder:  //新建关卡
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");//设置日期格式
				String fn = "NewLevel_"+df.format(new Date())+".XSB";  // new Date()获取当前系统时间
				myMaps.curMap = new mapNode(12, 8, null, fn, "", "");
				myMaps.curMap.fileName = fn;
				myMaps.curMapNum = -2;  //为“新建关卡”状态，此时，列表中没有关卡图标
				Intent intent2 = new Intent();
				intent2.setClass(this, myEditView.class);
				startActivity(intent2);
				return true;
			case R.id.levels_delete_more:  //批量删除
				if (myMaps.m_lstMaps.size() > 0) {
					View view3 = View.inflate(myGridView.this, R.layout.del_dialog, null);
					final EditText input1 = (EditText) view3.findViewById(R.id.del_begin);  //开始
					final EditText input2 = (EditText) view3.findViewById(R.id.del_end);  //结束
					AlertDialog.Builder dlg2 = new AlertDialog.Builder(myGridView.this, AlertDialog.THEME_HOLO_DARK);
					dlg2.setView(view3).setCancelable(false);

					dlg2.setTitle("删除范围").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							int m, n;
							try {
								m = Integer.valueOf(input1.getText().toString().trim());
								n = Integer.valueOf(input2.getText().toString().trim());
							} catch (Throwable ex) {
								m = 1;
								n = 0;
							}

							myMaps.curMap = null;
							recycleBitmapCaches(0, myMaps.m_lstMaps.size());

							if (myMaps.sFile.equals("创编关卡")) {
								File file;
								int len = myMaps.m_lstMaps.size();
								for (int k = n; k >= m; k--) {
									try {
										if (k <= len) {
											file = new File(new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("创编关卡/").append(myMaps.m_lstMaps.get(k-1).fileName).toString());
											if (file.exists() && file.isFile()) file.delete();
											myMaps.m_lstMaps.remove(k-1);
										}
									} catch (Exception e) {
									}
								}
							} else {
								int len = myMaps.m_lstMaps.size();
								for (int k = n; k >= m; k--) {
									try {
										if (k <= len) {
											mySQLite.m_SQL.del_L(myMaps.m_lstMaps.get(k-1).Level_id);
											myMaps.m_lstMaps.remove(k-1);
										}
									} catch (Exception e) { }
								}
							}
							dialog.dismiss();
							adapter.notifyDataSetChanged();
						}
					}).setCancelable(false).create().show();
				}
				return true;
			case R.id.levels_to_newset:
				mWhich = 0;
				String[] myList = new String[myMaps.mSets3.size()];
				for (int i = 0; i < myMaps.mSets3.size(); i++) {
					myList[i] = myMaps.mSets3.get(i).title;
				}

				new Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("选择关卡集")
						.setSingleChoiceItems(myList, 0, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mWhich = which;
							}}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//保存到 DB
						if (mWhich >= 0) {
							long m_S_id = myMaps.mSets3.get(mWhich).id;
							try {
								for (int k= 0; k < myMaps.m_lstMaps.size(); k++) {
									mySQLite.m_SQL.add_L(m_S_id, myMaps.m_lstMaps.get(k));
								}
								MyToast.showToast(myGridView.this, "保存成功！", Toast.LENGTH_SHORT);
							} catch (Exception e) {
								MyToast.showToast(myGridView.this, "未知原因，保存失败！", Toast.LENGTH_SHORT);
							}

						}}}).setNegativeButton("取消", null).setNeutralButton("新建关卡集", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//保存为关卡集
						final EditText et = new EditText(myGridView.this);
						et.setText("");
						et.setMaxLines(1);
						et.setSelection(et.getText().length());

						new AlertDialog.Builder(myGridView.this, AlertDialog.THEME_HOLO_DARK).setTitle("关卡集名称").setCancelable(false)
								.setView(et).setOnKeyListener(new DialogInterface.OnKeyListener() {
							@Override
							public boolean onKey(DialogInterface di, int keyCode, KeyEvent event) {
								if(keyCode == KeyEvent.KEYCODE_ENTER){
									String input;
									try {
										input = et.getText().toString().trim();

										if (input.equals("")) {
											MyToast.showToast(myGridView.this, "无效的关卡集名称！" + input, Toast.LENGTH_SHORT);
										} else {
											if (mySQLite.m_SQL.find_Set(input, myMaps.mSets3.get(childPos).id) > 0) {
												MyToast.showToast(myGridView.this, "此名称已经存在！\n" + input, Toast.LENGTH_SHORT);
												et.setText(input);
												et.setSelection(input.length());
											} else {
												if (saveToNewSet(input)) {
													MyToast.showToast(myGridView.this, "保存成功！\n" + input, Toast.LENGTH_SHORT);
												} else {
													MyToast.showToast(myGridView.this, "未知原因，保存失败！", Toast.LENGTH_SHORT);
												}
											}
										}
									} catch (Exception e) {
										MyToast.showToast(myGridView.this, "请检查输入的名称是否合法！", Toast.LENGTH_SHORT);
									}
								}
								return false;
							}
						}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								String input;
								try {
									input = et.getText().toString().trim();

									if (input.equals("")) {
										MyToast.showToast(myGridView.this, "无效的关卡集名称！\n" + input, Toast.LENGTH_SHORT);
									} else {
										if (myMaps.mSets3.size() > 0 && mySQLite.m_SQL.find_Set(input, myMaps.mSets3.get(childPos).id) > 0) {
											MyToast.showToast(myGridView.this, "此名称已经存在！\n" + input, Toast.LENGTH_SHORT);
											et.setText(input);
											et.setSelection(input.length());
										} else {
											if (saveToNewSet(input)) {
												MyToast.showToast(myGridView.this, "保存成功！\n" + input, Toast.LENGTH_SHORT);
											} else {
												MyToast.showToast(myGridView.this, "未知原因，保存失败！", Toast.LENGTH_SHORT);
											}
										}
									}
								} catch (Exception e) {
									MyToast.showToast(myGridView.this, "请检查输入的名称是否合法！", Toast.LENGTH_SHORT);
								}
							}})
								.setNegativeButton("取消", null)
								.show();
					}
				}).setCancelable(false).create().show();


				return true;
			case R.id.levels_about:
				//关卡集描述
				Intent intent1 = new Intent();
				intent1.setClass(this, myAbout1.class);
				startActivity(intent1);
				return true;
			default:
				return super.onOptionsItemSelected(mt);
		}
	}

	@Override
	public void onFindDone(ArrayList<mapNode> mlMaps) {
		if (mlMaps != null && mlMaps.size() > 0) {
			int n = myMaps.m_lstMaps.size();
			myMaps.curMap = null;
			myMaps.m_lstMaps.clear();
			recycleBitmapCaches(0, n);
			myMaps.sFile = "相似关卡";
			myMaps.m_Set_id = -1;
			myMaps.J_Title = myMaps.sFile;
			myMaps.J_Author = "";
			myMaps.J_Comment = "";
			myMaps.m_lstMaps = mlMaps;
			setTitle(myMaps.sFile);
			MyMenu.getItem(5).setVisible(false);
			MyMenu.getItem(7).setVisible(false);
			MyMenu.getItem(9).setVisible(false);
			mGridView.setSelection(0);
			mGridView.invalidateViews();
			adapter.notifyDataSetChanged();
		} else {
			MyToast.showToast(this, "没有发现相似的关卡！", Toast.LENGTH_SHORT);
		}

		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	protected void onDestroy() {
		myMaps.curJi = false;
		super.onDestroy();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (myMaps.sFile.equals("创编关卡") || myMaps.iskinChange) {
			recycleBitmapCaches(0, myMaps.m_lstMaps.size());
			myMaps.isSaveBlock = false;
			myMaps.iskinChange = false;
		}
		myMaps.curMap = null;
		setTitle(myMaps.sFile);
		if (myMaps.m_Sets[2] == 0) {  //刷新
			mGridView.invalidateViews();
		} else {
			mListView.invalidateViews();
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.add(0, 1, 0, "打开");
		menu.add(0, 2, 1, "改编为新关卡");   //编辑
		menu.add(0, 3, 2, "图标加锁");       //图标解锁
		menu.add(0, 4, 3, "迁出关卡至...");
		menu.add(0, 5, 4, "复制关卡到...");
		menu.add(0, 6, 5, "移动到...");
		menu.add(0, 7, 6, "前移");
		menu.add(0, 8, 7, "后移");
		menu.add(0, 9, 8, "删除");
		menu.add(0, 10, 9, "查找相似关卡");
		menu.add(0, 11, 10, "连续选择...");
		menu.add(0, 12, 11, "反选");

		MyContMenu = menu;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case 1:  //打开
				if (myMaps.sFile.equals("相似关卡")) {  //相似关卡的浏览时
					myMaps.iskinChange = false;
					myMaps.curMap = myMaps.m_lstMaps.get(m_Num);
					Intent intent1 = new Intent();
					intent1.setClass(myGridView.this, myFindView.class);
					startActivity(intent1);
				} else {
					myMaps.iskinChange = false;
					myMaps.curMap = myMaps.m_lstMaps.get(m_Num);
					Intent intent1 = new Intent();
					intent1.setClass(this, myGameView.class);
					startActivity(intent1);
				}
				break;
			case 2:  //改编为新关卡或继续编辑
				MyMenu.getItem(10).setVisible(false);

				mapNode nd = myMaps.m_lstMaps.get(m_Num);  //当前长按的关卡
                boolean flg9 = true;  //是否有效关卡

				if (nd.Level_id > 0) {  //为“改编为新关卡”状态时
					//为关卡生成关卡文档名称（含有原关卡集及其关卡序号信息）
					String newTitle;

					//若关卡标题不空，则以关卡标题为本，命名新关卡标题，否则，以关卡集名及其序号为本命名关卡标题（此时，关卡标题与关卡文档名相同）
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");//设置日期格式
					String fn = "NewLevel_"+df.format(new Date())+".XSB";  // new Date()获取当前系统时间

					//生成关卡新的标题
					if (nd.Title.trim().equals(""))
						newTitle = new StringBuilder(myMaps.sFile).append("_").append(m_Num + 1).toString();
					else
						newTitle = new StringBuilder(nd.Title.trim()).toString();

					recycleBitmapCaches(0, myMaps.m_lstMaps.size());
					myMaps.read_DirBuilder();  //重新加载“创编关卡”文件夹中的关卡列表

					//调整本界面的菜单项
					MyMenu.getItem(4).setVisible(false);
					MyMenu.getItem(5).setVisible(false);
					MyMenu.getItem(7).setVisible(true);
					MyMenu.getItem(8).setVisible(true);

					myMaps.curMapNum = -3;  //设置“改编为新关卡”状态，此时，列表中没有关卡图标
					nd.fileName = fn;  //关卡保存时的文档名称
                    if (nd.Map.equals("--")) {   //无效关卡
                        myMaps.loadXSB(nd.Comment);
                        flg9 = false;
                    }
					nd.Title = newTitle;  //修改关卡标题
					nd.Comment = "";  //关卡说明
				} else {
					myMaps.curMapNum = m_Num;  //为“继续编辑”状态,此时，列表中有关卡图标
				}

				//设置当前编辑关卡
                if (flg9) myMaps.curMap = new mapNode(nd.Rows, nd.Cols, nd.Map.split("\r\n|\n\r|\n|\r|\\|"), nd.Title, nd.Author, nd.Comment);
				//取得当前关卡文档名
				myMaps.curMap.fileName = nd.fileName;

				Intent intent2 = new Intent();
				intent2.setClass(this, myEditView.class);
				startActivity(intent2);

				break;
			case 3:  //图标锁
				if (myMaps.m_lstMaps.get(m_Num).P_id < 0) break;  //相似查找中，答案表中的关卡

				if (myMaps.m_lstMaps.get(m_Num).Lock)
					myMaps.m_lstMaps.get(m_Num).Lock = false;
				else
					myMaps.m_lstMaps.get(m_Num).Lock = true;

				mySQLite.m_SQL.Update_L_Lock(myMaps.m_lstMaps.get(m_Num).Level_id, myMaps.m_lstMaps.get(m_Num).Lock ? 1 : 0);
				adapter.m_changeItem = m_Num;
				adapter.notifyDataSetChanged();
				break;
			case 4:  //迁移关卡到...
				String str;
				myMaps.mArray.clear();
				myMaps.curMap = null;
				if (myMaps.isSelect && myMaps.m_lstMaps.get(m_Num).Select) {  //多选模式下，长按被选中的关卡，多关卡迁移
					for (int k = 0; k < myMaps.m_lstMaps.size(); k++) {
						if (myMaps.m_lstMaps.get(k).Select) {
							myMaps.mArray.add(k);
						}
					}
					str = "共 " + myMaps.mArray.size() + " 个关卡迁至";
				} else {  //单关卡迁移
					myMaps.mArray.add(m_Num);
					str = m_Num + " 号关卡迁至";
				}

				mWhich = 0;
				String[] myList = new String[myMaps.mSets3.size()];
				for (int k = 0; k < myMaps.mSets3.size(); k++) {
					myList[k] = myMaps.mSets3.get(k).title;
				}

				new Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle(str)
						.setSingleChoiceItems(myList, 0, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mWhich = which;
							}}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//保存到 DB
						if (mWhich >= 0) {
							long m_S_id = myMaps.mSets3.get(mWhich).id, new_id;
							long m_S_id2 = mySQLite.m_SQL.get_Level_Set_id(myMaps.m_lstMaps.get(m_Num).Level_id);
							if (m_S_id == m_S_id2)
								MyToast.showToast(myGridView.this, "请选择新的关卡集！", Toast.LENGTH_SHORT);
							else {
								try {
									int len = myMaps.m_lstMaps.size();
									for (int k= 0; k < myMaps.mArray.size(); k++) {
										new_id = mySQLite.m_SQL.add_L(m_S_id, myMaps.m_lstMaps.get(myMaps.mArray.get(k).intValue() - k));
										mySQLite.m_SQL.Update_A_Lid(myMaps.m_lstMaps.get(myMaps.mArray.get(k).intValue() - k).Level_id, new_id);  //将关卡的状态带过去
										mySQLite.m_SQL.del_L(myMaps.m_lstMaps.get(myMaps.mArray.get(k).intValue() - k).Level_id);
										myMaps.m_lstMaps.remove(myMaps.mArray.get(k).intValue() - k);
									}
									recycleBitmapCaches(0, len);
									adapter.notifyDataSetChanged();
									MyToast.showToast(myGridView.this, "迁出成功！", Toast.LENGTH_SHORT);
								} catch (Exception e) {
									MyToast.showToast(myGridView.this, "未知原因，迁出失败！", Toast.LENGTH_SHORT);
								}
							}
						}}}).setNegativeButton("取消", null).setCancelable(false).create().show();

				break;
			case 5:  //复制关卡到...
				String str2;
				myMaps.mArray.clear();
				myMaps.curMap = null;
				if (myMaps.isSelect && myMaps.m_lstMaps.get(m_Num).Select) {  //多选模式下，长按被选中的关卡，多关卡迁移
					for (int k = 0; k < myMaps.m_lstMaps.size(); k++) {
						if (myMaps.m_lstMaps.get(k).Select) {
							myMaps.mArray.add(k);
						}
					}
					str2 = "共 " + myMaps.mArray.size() + " 个关卡复制到";
				} else {  //单关卡迁移
					myMaps.mArray.add(m_Num);
					str2 = m_Num + " 号关卡复制到";
				}

				mWhich = 0;
				String[] myList2 = new String[myMaps.mSets3.size()];
				for (int k = 0; k < myMaps.mSets3.size(); k++) {
					myList2[k] = myMaps.mSets3.get(k).title;
				}

				new Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle(str2)
						.setSingleChoiceItems(myList2, 0, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mWhich = which;
							}}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//保存到 DB
						if (mWhich >= 0) {
							long m_S_id = myMaps.mSets3.get(mWhich).id, new_id;
							long m_S_id2 = mySQLite.m_SQL.get_Level_Set_id(myMaps.m_lstMaps.get(m_Num).Level_id);
							if (m_S_id == m_S_id2)
								MyToast.showToast(myGridView.this, "请选择新的关卡集！", Toast.LENGTH_SHORT);
							else {
								try {
									for (int k= 0; k < myMaps.mArray.size(); k++) {
										new_id = mySQLite.m_SQL.add_L(m_S_id, myMaps.m_lstMaps.get(k));
										mySQLite.m_SQL.Update_A_Lid(myMaps.m_lstMaps.get(k).Level_id, new_id);  //将关卡的状态带过去
									}
									MyToast.showToast(myGridView.this, "复制成功！", Toast.LENGTH_SHORT);
								} catch (Exception e) {
									MyToast.showToast(myGridView.this, "未知原因，复制失败！", Toast.LENGTH_SHORT);
								}
							}
						}}}).setNegativeButton("取消", null).setCancelable(false).create().show();

				break;
			case 6:  //移动到...
				myMaps.curMap = null;
				View view = View.inflate(this, R.layout.goto_dialog, null);
				final EditText input_steps = (EditText) view.findViewById(R.id.dialog_steps);  //位置
				AlertDialog.Builder dlg = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
				dlg.setView(view).setCancelable(true);
				dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface di, int keyCode, KeyEvent event) {
						if(keyCode == KeyEvent.KEYCODE_ENTER){
							try {
								int n = Integer.parseInt(input_steps.getText().toString());
								if (n > mGridView.getCount()) n = mGridView.getCount();
								if (n > 0 && n-1 != m_Num) {
									try {
										swap(myMaps.m_lstMaps, m_Num, n-1);
										if (n-1 < m_Num) {
											recycleBitmapCaches(n-1, m_Num+1);
										} else {
											recycleBitmapCaches(m_Num, n+1);
										}
										updateNO();  //关卡顺序改变
										adapter.notifyDataSetChanged();
										mGridView.setSelection(n-1);
									} catch (Exception e) {
										MyToast.showToast(myGridView.this, "未知原因，移动失败！", Toast.LENGTH_SHORT);
									}
								}
							} catch (Exception e) { }
							di.dismiss();
							return true;
						}
						return false;
					}
				});
				dlg.setMessage("移动范围：1--"+myMaps.m_lstMaps.size()+"\n（大于 "+myMaps.m_lstMaps.size()+ " 时则移到尾部）").setTitle("将 "+(m_Num+1)+" 号关卡移到").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int n = Integer.parseInt(input_steps.getText().toString());
							if (n > mGridView.getCount()) n = mGridView.getCount();
							if (n > 0 && n-1 != m_Num) {
								try {
									swap(myMaps.m_lstMaps, m_Num, n-1);
									if (n-1 < m_Num) {
										recycleBitmapCaches(n-1, m_Num+1);
									} else {
										recycleBitmapCaches(m_Num, n+1);
									}
									updateNO();  //关卡顺序改变
									adapter.notifyDataSetChanged();
									mGridView.setSelection(n-1);
								} catch (Exception e) {
									MyToast.showToast(myGridView.this, "未知原因，移动失败！", Toast.LENGTH_SHORT);
								}
							}
						} catch (Exception e) { }
						dialog.dismiss();
					}
				}).setCancelable(false).create().show();
				break;
			case 7:  //前移
				myMaps.curMap = null;
				myMaps.mArray.clear();
				boolean flg = true;  //被选中的关卡是否集中在一起
				if (myMaps.isSelect && myMaps.m_lstMaps.get(m_Num).Select) {  //多选模式下，长按被选中的关卡，多关卡迁动
					for (int k = 0; k < myMaps.m_lstMaps.size(); k++) {
						if (myMaps.m_lstMaps.get(k).Select) {
							if (flg && myMaps.mArray.size() > 0 && myMaps.mArray.get(myMaps.mArray.size()-1).intValue()+1 < k) {
								flg = false;  //分散不集中
							}
							myMaps.mArray.add(k);
						}
					}
				} else {  //单关卡移动
					myMaps.mArray.add(m_Num);  //但关卡时，flg 默认为集中状态
				}
				int toNum, p;
				if (!flg) {  //关卡多且分散
					toNum = myMaps.mArray.get(0).intValue()+1;
					p = 1;
				} else {  //单个关卡或关卡已经集中在一起，且前面有空位
					toNum = myMaps.mArray.get(0).intValue()-1;
					p = 0;
				}
				//有空位可移动
				if (toNum >= 0) {
					for (int k = p; k < myMaps.mArray.size(); k++) {
						swap(myMaps.m_lstMaps, myMaps.mArray.get(k).intValue(), toNum);
						toNum++;
					}
					updateNO();  //关卡顺序改变
					recycleBitmapCaches(myMaps.mArray.get(0).intValue() - 1, myMaps.mArray.get(myMaps.mArray.size() - 1).intValue() + 1);
					adapter.notifyDataSetChanged();
				}
				break;
			case 8:  //后移
				myMaps.curMap = null;
				myMaps.mArray.clear();
				flg = true;  //被选中的关卡是否集中在一起
				if (myMaps.isSelect && myMaps.m_lstMaps.get(m_Num).Select) {  //多选模式下，长按被选中的关卡，多关卡迁动
					for (int k = myMaps.m_lstMaps.size()-1; k >= 0; k--) {
						if (myMaps.m_lstMaps.get(k).Select) {
							if (flg && myMaps.mArray.size() > 0 && myMaps.mArray.get(myMaps.mArray.size()-1).intValue()-1 > k) {
								flg = false;  //分散不集中
							}
							myMaps.mArray.add(k);
						}
					}
				} else {  //单关卡移动
					myMaps.mArray.add(m_Num);  //但关卡时，flg 默认为集中状态
				}
				if (!flg) {  //关卡多且分散
					toNum = myMaps.mArray.get(0).intValue()-1;
					p = 1;
				} else {  //单个关卡或关卡已经集中在一起，且后面有空位
					toNum = myMaps.mArray.get(0).intValue()+1;
					p = 0;
				}
				//有空位可移动
				if (toNum < myMaps.m_lstMaps.size()) {
					for (int k = p; k < myMaps.mArray.size(); k++) {
						swap(myMaps.m_lstMaps, myMaps.mArray.get(k).intValue(), toNum);
						toNum--;
					}
					updateNO();  //关卡顺序改变
					recycleBitmapCaches(myMaps.mArray.get(myMaps.mArray.size()-1).intValue(), myMaps.mArray.get(0).intValue() + 2);
					adapter.notifyDataSetChanged();
				}
				break;
			case 9:  //删除
				String str3;
				myMaps.mArray.clear();
				if (myMaps.isSelect && myMaps.m_lstMaps.get(m_Num).Select) {  //多选模式下，长按被选中的关卡，多关卡迁移
					for (int k = myMaps.m_lstMaps.size()-1; k >= 0; k--) {
						if (myMaps.m_lstMaps.get(k).Select) {
							myMaps.mArray.add(k);
						}
					}
					str3 = "共有 " + myMaps.mArray.size() + " 个关卡将被删除，\n确认吗？";
				} else {  //单关卡迁移
					myMaps.mArray.add(m_Num);
					str3 = (m_Num+1) + " 号关卡将被删除，确认吗？";
				}

				new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setMessage(str3)
						.setCancelable(false).setNegativeButton("取消", null)
						.setPositiveButton("确定", new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								File file;
								myMaps.curMap = null;
								int len = myMaps.m_lstMaps.size();
								for (int k = 0; k < myMaps.mArray.size(); k++) {
									if (myMaps.sFile.equals("创编关卡")) {
										file = new File(new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("创编关卡/").append(myMaps.m_lstMaps.get(myMaps.mArray.get(k).intValue()).fileName).toString());
										if (file.exists() && file.isFile()) file.delete();
									} else {
										mySQLite.m_SQL.del_L(myMaps.m_lstMaps.get(myMaps.mArray.get(k).intValue()).Level_id);
									}
									myMaps.m_lstMaps.remove(myMaps.mArray.get(k).intValue());
								}
								recycleBitmapCaches(0, len);
								adapter.notifyDataSetChanged();

							}}).create().show();

				break;
			case 10:  //查找相似关卡
				//需参照：myFindView.myCompare()中的定义
				final String[] m_menu4 = {  //相似度
						"100",
						"95",
						"90",
						"85",
						"80",
						"75",
						"66",
						"50"
				};

				View view2 = View.inflate(this, R.layout.find_dialog, null);
				final CheckBox m_All = (CheckBox) view2.findViewById(R.id.find_all);                //全选
				final CheckBox m_Ans = (CheckBox) view2.findViewById(R.id.find_ans);                //搜索答案库
				final CheckBox m_Sort = (CheckBox) view2.findViewById(R.id.find_sort);              //排序
				myMaps.m_setName = (ListView) view2.findViewById(R.id.find_setname);                //关卡集选项
				m_Similarity = (ListView) view2.findViewById(R.id.find_similarity);                 //相似度选项

				mySets = new ArrayList<Long>();                                                            //关卡集 id
				mAdapter = new myMaps.MyAdapter(this, (ArrayList)myMaps.getData(mySets));
				myMaps.m_setName.setAdapter(mAdapter);                                                     //关卡集
				myMaps.m_setName.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);                             //开启多选模式
				for (int k = 0; k < myMaps.m_setName.getCount(); k++) {
					myMaps.m_setName.setItemChecked(k, true);
				}
				myMaps.m_setName.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						mySets.set(position, -mySets.get(position));

						if (mySets.get(position) > 0) myMaps.m_setName.setItemChecked(position, true);
						else myMaps.m_setName.setItemChecked(position, false);

						//点击item后，通知adapter重新加载view
						mAdapter.notifyDataSetChanged();
					}
				});

				m_All.setChecked(true);                                                               //是否全选
				m_All.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						for (int k = 0; k < myMaps.m_setName.getCount(); k++) {
							myMaps.m_setName.setItemChecked(k, isChecked);
							if (isChecked) {
								if (mySets.get(k) < 0) mySets.set(k, -mySets.get(k));
							} else {
								if (mySets.get(k) > 0) mySets.set(k, -mySets.get(k));
							}
						}
					}
				});

				mAdapter2 = new MyAdapter2(this, (ArrayList)getData2(m_menu4));
				m_Similarity.setAdapter(mAdapter2);                                                   //相似度
				m_Similarity.setChoiceMode(ListView.CHOICE_MODE_SINGLE);                              //开启单选模式
				if (myMaps.m_Sets[26] < 0 || myMaps.m_Sets[26] > 7) myMaps.m_Sets[26] = 0;
				m_Similarity.setItemChecked(myMaps.m_Sets[26], true);
				m_Similarity.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						myMaps.m_Sets[26] = position;
						mAdapter2.notifyDataSetChanged();
					}
				});

				AlertDialog.Builder builder4 = new Builder(this, AlertDialog.THEME_HOLO_DARK);
				builder4.setTitle("搜索相似关卡").setView(view2).setNegativeButton("取消", null).setPositiveButton("开始", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
							int len = 0;
							for (int k = 0; k < mySets.size(); k++) {
								if (mySets.get(k) > 0) len++;
							}
							if (len > 0 || m_Ans.isChecked()) {  //选择了关卡集
								long[] m_sets = null;

								if (len != mySets.size()) {  //全选时，数组为 null
									m_sets = new long[len];
									int i = 0;
									for (int k = 0; k < mySets.size(); k++) {
										if (mySets.get(k) > 0) m_sets[i++] = mySets.get(k);
									}
								}

                                if (mDialog == null) {
                                    mDialog = new myFindFragment();
                                    Bundle bundle = new Bundle();
                                    bundle.putLongArray("mSets", m_sets);
                                    bundle.putInt("mSimilarity", Integer.valueOf(m_menu4[myMaps.m_Sets[26]]));
                                    bundle.putBoolean("mAns", m_Ans.isChecked());
                                    bundle.putBoolean("mSort", m_Sort.isChecked());
                                    myMaps.curMap = myMaps.m_lstMaps.get(m_Num);
                                    //指向源关卡
                                    if (myMaps.sFile.equals("创编关卡")) {
                                        myMaps.oldMap = new mapNode(0, -1, myMaps.curMap.Rows, myMaps.curMap.Cols, myMaps.curMap.Map, myMaps.curMap.Title, myMaps.curMap.Author, myMaps.curMap.Comment, myMaps.curMap.Map);
                                        myMaps.oldMap.Map0 = myMaps.oldMap.Map;
                                        myMaps.oldMap.Num = m_Num + 1;  //关卡序号
                                    } else {
                                        myMaps.oldMap = new mapNode(myMaps.curMap.Level_id, myMaps.curMap.P_id, myMaps.curMap.Rows, myMaps.curMap.Cols, myMaps.curMap.Map, myMaps.curMap.Title, myMaps.curMap.Author, myMaps.curMap.Comment, myMaps.curMap.Map0);
                                    }
                                    mDialog.setArguments(bundle);
                                    mDialog.show(getFragmentManager(), TAG_PROGRESS_DIALOG_FRAGMENT);
                                    MyMenu.getItem(3).setVisible(false);  //是否多选模式
                                    MyMenu.getItem(10).setVisible(false);  //批量删除
                                    MyMenu.getItem(11).setVisible(true);   //保存到新的关卡集
                                }
						}
					}});
				builder4.setCancelable(false).show();

				break;
			case 11:  //连续选择...
				final NumberKeyListener getNumber = new NumberKeyListener() {
					@Override
					protected char[] getAcceptedChars() {
						return new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
					}

					@Override
					public int getInputType() {
						return InputType.TYPE_CLASS_PHONE;
					}
				};
				final EditText et = new EditText(this);
				et.setKeyListener(getNumber);

				et.setText("");
				final int[] num = {m_Num+1, m_Num+1};
				new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("连选至")
						.setView(et)
						.setMessage("请输入一个关卡序号。")
						.setPositiveButton("确定", new DialogInterface.OnClickListener () {
							public void onClick(DialogInterface dialog, int which) {
								try {
									int n = Integer.valueOf(et.getText().toString());
									if (n < 1) n = 1;
									else if (n > myMaps.m_lstMaps.size ()) n = myMaps.m_lstMaps.size ();
									if (n < num[0]) {
										num[0] = n;
									} else if (n > num[1]) {
										num[1] = n;
									}
                                    for (int k = num[0]; k <= num[1]; k++) {
                                        myMaps.m_lstMaps.get(k-1).Select = true;
                                    }
                                    if (myMaps.m_Sets[2] == 0) {  //刷新
                                        mGridView.invalidateViews();
                                    } else {
                                        mListView.invalidateViews();
                                    }
                                    adapter.notifyDataSetChanged();
								} catch (Throwable ex) {
									MyToast.showToast(myGridView.this, "关卡序号不正确！", Toast.LENGTH_SHORT);
								}
							}
						})
						.setNegativeButton("取消", null).setCancelable(false)
						.show();

				break;
			case 12:  //反选
				for (int k = 0; k < myMaps.m_lstMaps.size (); k++) {
					myMaps.m_lstMaps.get(k).Select = !myMaps.m_lstMaps.get(k).Select;
				}
				if (myMaps.m_Sets[2] == 0) {  //刷新
					mGridView.invalidateViews();
				} else {
					mListView.invalidateViews();
				}
				adapter.notifyDataSetChanged();
				break;
		}
		return true;
	}

	//装填相似度列表项
	private List<String> getData2(String[] m_arr) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < m_arr.length; i++) {
			list.add(m_arr[i]);
		}
		return list;
	}

	//更新关卡顺序号
	private void updateNO() {
		int len = myMaps.m_lstMaps.size();
		for(int k=0; k<len; k++) {
			mySQLite.m_SQL.Set_L_NO(myMaps.m_lstMaps.get(k).Level_id, k+1);
		}
	}

	//保存为新关卡集
	private boolean saveToNewSet(String name) {
		try {
			//创建新的关卡集
			long new_ID = mySQLite.m_SQL.add_T(3, name, "", "");
			//将新关卡集加入列表
			set_Node nd = new set_Node();
			nd.id = new_ID;
			nd.title = name;
			myMaps.mSets3.add(1, nd);
			//复制关卡
			for (int k= 0; k < myMaps.m_lstMaps.size(); k++) {
				mySQLite.m_SQL.add_L(new_ID, myMaps.m_lstMaps.get(k));
			}
			return true;
		} catch (Exception e) { }
		return false;
	}

	private void updateLayout() {
		int n = mGridView.getFirstVisiblePosition();
		if (myMaps.m_Sets[2] == 0) {
			mGridView.setVisibility(View.VISIBLE);
			mGridView.setAdapter(adapter);
			mGridView.setOnScrollListener(this);
			mGridView.setOnItemClickListener(mItemClickListener);
			mGridView.setOnItemLongClickListener(mItemLongClickListener);
			mListView.setVisibility(View.GONE);
			mGridView.setSelection(n);
		} else {
			mListView.setVisibility(View.VISIBLE);
			mListView.setAdapter(adapter);
			mListView.setOnScrollListener(this);
			mListView.setOnItemClickListener(mItemClickListener);
			mListView.setOnItemLongClickListener(mItemLongClickListener);
			mGridView.setVisibility(View.GONE);
			mListView.setSelection(n);
		}
	}

	public static <T> void swap(List<T> list, int oldPosition, int newPosition){
		if(null == list){
			throw new IllegalStateException("The list can not be empty...");
		}

		// 向前移动，前面的元素需要向后移动
		if(oldPosition < newPosition){
			for(int i = oldPosition; i < newPosition; i++){
				Collections.swap(list, i, i + 1);
			}
		}

		// 向后移动，后面的元素需要向前移动
		if(oldPosition > newPosition){
			for(int i = oldPosition; i > newPosition; i--){
				Collections.swap(list, i, i - 1);
			}
		}
	}

	//列表框 Adapter
	public class MyAdapter2 extends BaseAdapter {
		private Context context;
		private ArrayList<String>data;

		public MyAdapter2(Context context,ArrayList<String> data){
			this.context=context;
			this.data=data;
		}

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = new TextView(context);
			tv.setText(data.get(position));
			//判断position位置是否被选中，改变颜色
			if(m_Similarity.isItemChecked(position)) {
				tv.setBackgroundColor(Color.parseColor("#0088aa"));
			} else {
				tv.setBackgroundColor(Color.parseColor("#363636"));
			}
			return tv;
		}
	}
}