package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class myStateBrow extends Activity implements myGifMakeFragment.GifMakeStatusUpdate{
	AlertDialog DelDlg;
	AlertDialog DelDlgAll;

	private myGifMakeFragment mDialog3;
	private static final String TAG_PROGRESS_DIALOG_FRAGMENT = "gif_make_progress_fragment";

	String[] s_groups = { "状态", "答案" };
	String[] s_sort = { "  【移动优先】", "  【推动优先】" };
	static int my_Sort = 0;  // 答案列表的默认排序 -- 移动优先
	Comparator comp = new SortComparator();

    int g_Pos;
    int c_Pos;
	int m_Sel_id2;
	long m_Sel_id;
    int m_nItemSelect;  //对话框中的出item选择前的记忆

	private String result = ""; // 声明Post返回值的的字符串

	int m_Gif_Mark = 1;  //水印
	int m_Gif_Interval = 300;  //制作GIF动画相关参数：帧间隔（毫秒）、帧方式（逐推、逐移）
	boolean m_Gif_Type = true;
	boolean m_Gif_Skin = false;

	ExpandableListView s_expView;
	MyExpandableListView s_Adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.s_main);
        
        //设置标题栏标题，并开启标题栏的返回键
		setTitle("关卡状态"); 
		
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
            	g_Pos = groupPosition;
            	c_Pos = childPosition;
            	
    	    	//取得保存状态的 id
            	if (c_Pos > -1) {
	    			if (g_Pos == 0)
	    				m_Sel_id = myMaps.mState1.get(childPosition).id;
	    			else
	    				m_Sel_id = myMaps.mState2.get(childPosition).id;
	    			
	    			myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
	    			set_State();
	    			
            	} else m_Sel_id = (long)-1;
    			
            	return true;
            }
        });
		
		s_expView.setOnItemLongClickListener(new OnItemLongClickListener()  
        {  
            @Override  
            public boolean onItemLongClick(AdapterView<?> parent, View childView, int flatPos, long id) {  
            	
            	 long packedPosition = s_expView.getExpandableListPosition(flatPos);
                 g_Pos = ExpandableListView.getPackedPositionGroup(packedPosition);
                 c_Pos = ExpandableListView.getPackedPositionChild(packedPosition);

				if (c_Pos < 0) {
					if (g_Pos == 1 && myMaps.mState2.size() > 1) {  // 答案多于1个，且长按了答案分组项
						if (my_Sort == 0) {    // 置推动优先
							my_Sort = 1;
						} else {               // 置移动优先
							my_Sort = 0;
						}
						Collections.sort(myMaps.mState2, comp);
						s_Adapter.notifyDataSetInvalidated();
					}
				} else {
						if (g_Pos == 0)
							m_Sel_id = myMaps.mState1.get(c_Pos).id;
						else
							m_Sel_id = myMaps.mState2.get(c_Pos).id;

						s_expView.showContextMenu();
				}
                 return true;  
            }             
        }); 
        registerForContextMenu(s_expView);
        
		if (myMaps.mState1.size() > 0) s_expView.expandGroup(0);
		if (myMaps.mState2.size() > 0) s_expView.expandGroup(1);

		AlertDialog.Builder dlg = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		dlg.setMessage("删除此状态或答案，确认吗？")
		   .setCancelable(false).setNegativeButton("取消", null)
		   .setPositiveButton("确定", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					if (mySQLite.m_SQL.isCanDeleteAns(m_Sel_id)) {  //第一个内置关卡，至少需保留 1 个答案
						MyToast.showToast(myStateBrow.this, "第一个内置关卡，至少需保留 1 个答案！", Toast.LENGTH_SHORT);
					} else {
						mySQLite.m_SQL.del_S(m_Sel_id);
						if (g_Pos == 0)
							myMaps.mState1.remove(c_Pos);
						else
							myMaps.mState2.remove(c_Pos);

						myMaps.curMap.Solved = (myMaps.mState2.size() > 0);
						if (g_Pos == 1 && !myMaps.curMap.Solved) { //若此CRC关卡的答案个数为0，则设置涉及到的全部关卡为无答案
							mySQLite.m_SQL.Set_L_Solved(myMaps.curMap.key, 0, false);
						}
						s_Adapter.notifyDataSetChanged();
					}
				}});
		DelDlg = dlg.create();
		
		AlertDialog.Builder dlg2 = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		dlg2.setMessage("删除所有的状态，确定吗？")
		   .setCancelable(false).setNegativeButton("取消", null)
		   .setPositiveButton("确定", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {

					mySQLite.m_SQL.del_S_ALL(myMaps.mState1.get(c_Pos).pid);
					myMaps.mState1.clear();

					s_Adapter.notifyDataSetChanged();
				}});
		DelDlgAll = dlg2.create();

    }

	//接收 YASS 返回值
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 1) {  //优化返回
			if (resultCode == RESULT_OK) {
				final String str = data.getStringExtra("SOLUTION");
				final String time = "[YASS]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				AlertDialog.Builder dlg0 = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
				dlg0.setTitle("成功").setMessage("优化成功，结果保存吗？").setCancelable(false).setNegativeButton("放弃", null)
					.setNeutralButton("带走", new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							myMaps.m_State.ans = str;
							myMaps.m_State.bk_ans = "";
							set_State();
						}
					})
					.setPositiveButton("保存", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							int len = str.length();
							int p = 0, p2 = 0;
							for (int k = 0; k < len; k++) {
								switch (str.charAt(k)) {
									case 'L':
									case 'U':
									case 'R':
									case 'D':
										p++;
									case 'l':
									case 'u':
									case 'r':
									case 'd':
										p2++;
								}
							}
							long hh = mySQLite.m_SQL.add_S(myMaps.curMap.Level_id,
									1,
									p2,
									p,
									0,
									0,
									-1,
									-1,
									str,  //答案
									"",
									myMaps.curMap.key,
									myMaps.curMap.L_CRC_Num,
									myMaps.curMap.Map0,
									time);
							if (hh > 0) {
								myMaps.curMap.Solved = true;
								mySQLite.m_SQL.Set_L_Solved(myMaps.curMap.Level_id, 1, true);
								state_Node ans = new state_Node();
								ans.id = hh;
								ans.pid = myMaps.curMap.Level_id;
								ans.pkey = myMaps.curMap.key;
								ans.inf = "移动: " + p2 + ", 推动: " + p;
								ans.time = time;
								myMaps.mState2.add(ans);
								s_Adapter.notifyDataSetChanged();
								if (!s_expView.isGroupExpanded(1)) {
									s_expView.expandGroup(1);
								}
							} else {
								MyToast.showToast(myStateBrow.this, "答案有重复！", Toast.LENGTH_SHORT);
							}
						}
					});
				dlg0.show();
			} else {
				MyToast.showToast(this, "未能完成优化！", Toast.LENGTH_SHORT);
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		menu.add(0, 1, 0, "打开");
		menu.add(0, 2, 1, "YASS优化");
		menu.add(0, 3, 2, "导出到文档: XSB+Lurd");
		menu.add(0, 4, 3, "导出到剪切板: XSB+Lurd");
		menu.add(0, 5, 4, "导出到剪切板: Lurd");
		menu.add(0, 6, 5, "导出到剪切板: 正推 Lurd");
		menu.add(0, 7, 6, "导出到剪切板: 逆推 Lurd");
		menu.add(0, 8, 7, "注释标签");
		menu.add(0, 9, 8, "删除");
		menu.add(0, 10, 9, "删除全部状态");
		menu.add(0, 11, 10, "提交答案（sokoban.cn）");
		menu.add(0, 12, 11, "制作 GIF 演示动画");

		menu.getItem(0).setVisible(false);    // 打开
		menu.getItem(1).setVisible(false);    // YASS优化
		menu.getItem(2).setVisible(false);    // 导出到文档: XSB+Lurd
		menu.getItem(3).setVisible(false);    // 导出到剪切板: XSB+Lurd
		menu.getItem(4).setVisible(false);    // 导出到剪切板: Lurd
		menu.getItem(5).setVisible(false);    // 导出到剪切板: 正推 Lurd
		menu.getItem(6).setVisible(false);    // 导出到剪切板: 逆推 Lurd
		menu.getItem(7).setVisible(false);    // 注释标签
		menu.getItem(8).setVisible(false);    // 删除
		menu.getItem(9).setVisible(false);    // 删除全部状态
		menu.getItem(10).setVisible(false);   // 提交答案（sokoban.cn）
		menu.getItem(11).setVisible(false);   // 制作 GIF 演示动画

		if (g_Pos == 0) {
			menu.getItem(0).setVisible(true);    // 打开
			menu.getItem(2).setVisible(true);    // 导出到文档: XSB+Lurd
			menu.getItem(3).setVisible(true);    // 导出到剪切板: XSB+Lurd
			menu.getItem(4).setVisible(true);    // 导出到剪切板: Lurd
			menu.getItem(5).setVisible(true);    // 导出到剪切板: 正推 Lurd
			menu.getItem(6).setVisible(true);    // 导出到剪切板: 逆推 Lurd
			menu.getItem(7).setVisible(true);    // 注释标签
			menu.getItem(8).setVisible(true);    // 删除
			menu.getItem(9).setVisible(true);    // 删除全部状态
		} else {
			menu.getItem(0).setVisible(true);    // 打开
			menu.getItem(1).setVisible(true);    // YASS优化
			menu.getItem(2).setVisible(true);    // 导出到文档: XSB+Lurd
			menu.getItem(3).setVisible(true);    // 导出到剪切板: XSB+Lurd
			menu.getItem(4).setVisible(true);    // 导出到剪切板: Lurd
			menu.getItem(7).setVisible(true);    // 注释标签
			menu.getItem(8).setVisible(true);    // 删除
			menu.getItem(10).setVisible(true);   // 提交答案（sokoban.cn）
			menu.getItem(11).setVisible(true);   // 制作 GIF 演示动画
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (c_Pos >= 0) {
			switch (item.getItemId()){
			case 1:  //打开
				myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
				set_State();
				break;
			case 2:  //YASS 优化
				myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
				StringBuilder str = new StringBuilder();
				int len = myMaps.m_State.ans.length();
				if (len > 0) {
					try {
						Intent intent3 = new Intent(Intent.ACTION_MAIN);
						intent3.addCategory(Intent.CATEGORY_LAUNCHER);
						ComponentName name = new ComponentName("net.sourceforge.sokobanyasc.joriswit.yass", "yass.YASSActivity");
						intent3.setComponent(name);
						String actName = intent3.getAction();
						intent3.setAction("nl.joriswit.sokosolver.OPTIMIZE");
						intent3.putExtra("LEVEL", myMaps.curMap.Map);
						str.append(myMaps.m_State.ans.replaceAll("[K,k,,]", ""));
						intent3.putExtra("SOLUTION", str.toString());
						startActivityForResult(intent3, 1);
						intent3.setAction(actName);
					}catch (Exception e){
						MyToast.showToast(this, "没有找到求解器！", Toast.LENGTH_SHORT);
					}
				} else {
					MyToast.showToast(this, "答案是空的！", Toast.LENGTH_SHORT);
				}
				break;
			case 3:  //导出到文档: XSB+Lurd
				myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
				saveAnsToFile(m_Sel_id);
				break;
			case 4:  //导出到剪切板: XSB+Lurd
				myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
				myExport2(m_Sel_id);
				break;
			case 5:  //导出到剪切板: Lurd
				myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
				myExport();
				break;
			case 6:  //导出到剪切板: 正推 Lurd
				myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
				myExport3();
				break;
			case 7:  //导出到剪切板: 逆推 Lurd
				myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
				myExport4();
				break;
			case 8:  //注释
				final EditText et = new EditText(this);

				if (g_Pos == 0) {
					if (myMaps.mState1.get(c_Pos).time.toLowerCase().indexOf("yass") >= 0 || myMaps.mState1.get(c_Pos).time.toLowerCase().indexOf("导入") >= 0) {
						MyToast.showToast(myStateBrow.this, "只读！", Toast.LENGTH_SHORT);
						break;
					}
					et.setText(myMaps.mState1.get(c_Pos).time);
				} else {
					if (myMaps.mState2.get(c_Pos).time.toLowerCase().indexOf("yass") >= 0 || myMaps.mState2.get(c_Pos).time.toLowerCase().indexOf("导入") >= 0) {
						MyToast.showToast(myStateBrow.this, "只读！", Toast.LENGTH_SHORT);
						break;
					}
					et.setText(myMaps.mState2.get(c_Pos).time);
				}

				new AlertDialog.Builder(myStateBrow.this, AlertDialog.THEME_HOLO_DARK).setTitle("注释")
						.setView(et)
						.setPositiveButton("确定", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								String inf = et.getText().toString().trim();

								if (g_Pos == 0)
									myMaps.mState1.get(c_Pos).time = inf;
								else
									myMaps.mState2.get(c_Pos).time = inf;

								try {
									mySQLite.m_SQL.Update_A_inf(m_Sel_id, inf);
								} catch (Exception e) {
									MyToast.showToast(myStateBrow.this, "出错了，注释未能保存！", Toast.LENGTH_SHORT);
								}
							}
						})
						.setNegativeButton("取消", null).setCancelable(false)
						.show();

				break;
			case 9:  //删除
				DelDlg.show();
				break;
			case 10:  //删除全部状态
				DelDlgAll.show();
				break;
			case 11:  //提交答案
				final String[][] m_menu = {  //国家
						{"00", "*** Other ***"},
						{"CN", "中国"},
						{"AF", "Afghanistan"},
						{"AL", "Albania"},
						{"DZ", "Algeria"},
						{"AS", "American Samoa"},
						{"AD", "Andorra"},
						{"AO", "Angola"},
						{"AI", "Anguilla"},
						{"AQ", "Antarctica"},
						{"AG", "Antigua and Barbuda"},
						{"AR", "Argentina"},
						{"AM", "Armenia"},
						{"AW", "Aruba"},
						{"AU", "Australia"},
						{"AT", "Austria"},
						{"AZ", "Azerbaijan"},
						{"BS", "Bahamas"},
						{"BH", "Bahrain"},
						{"BD", "Bangladesh"},
						{"BB", "Barbados"},
						{"BY", "Belarus"},
						{"BE", "Belgium"},
						{"BZ", "Belize"},
						{"BJ", "Benin"},
						{"BM", "Bermuda"},
						{"BT", "Bhutan"},
						{"BO", "Bolivia"},
						{"BA", "Bosnia and Herzegovina"},
						{"BW", "Botswana"},
						{"BV", "Bouvet Island"},
						{"BR", "Brazil"},
						{"IO", "British Indian Ocean Territory"},
						{"VG", "British Virgin Islands"},
						{"BN", "Brunei Darussalam"},
						{"BG", "Bulgaria"},
						{"BF", "Burkina Faso"},
						{"MM", "Burma"},
						{"BI", "Burundi"},
						{"KH", "Cambodia"},
						{"CM", "Cameroon"},
						{"CA", "Canada"},
						{"CV", "Cape Verde"},
						{"KY", "Cayman Islands"},
						{"CF", "Central African Republic"},
						{"TD", "Chad"},
						{"CL", "Chile"},
						{"CN", "China"},
						{"CX", "Christmas Island"},
						{"CC", "Cocos (Keeling) Islands"},
						{"CO", "Colombia"},
						{"KM", "Comoros"},
						{"CD", "Congo, Democratic Republic of the"},
						{"CG", "Congo, Republic of the"},
						{"CK", "Cook Islands"},
						{"CR", "Costa Rica"},
						{"CI", "Cote d'Ivoire (Ivory Coast)"},
						{"HR", "Croatia"},
						{"CU", "Cuba"},
						{"CY", "Cyprus"},
						{"CZ", "Czech Republic"},
						{"DK", "Denmark"},
						{"DJ", "Djibouti"},
						{"DM", "Dominica"},
						{"DO", "Dominican Republic"},
						{"TP", "East Timor"},
						{"EC", "Ecuador"},
						{"EG", "Egypt"},
						{"SV", "El Salvador"},
						{"GQ", "Equatorial Guinea"},
						{"ER", "Eritrea"},
						{"EE", "Estonia"},
						{"ET", "Ethiopia"},
						{"FK", "Falkland Islands (Islas Malvinas)"},
						{"FO", "Faroe Islands"},
						{"FJ", "Fiji"},
						{"FI", "Finland"},
						{"FR", "France"},
						{"GF", "French Guiana"},
						{"PF", "French Polynesia"},
						{"GA", "Gabon"},
						{"GM", "Gambia"},
						{"GE", "Georgia"},
						{"DE", "Germany"},
						{"GH", "Ghana"},
						{"GI", "Gibraltar"},
						{"GR", "Greece"},
						{"GL", "Greenland"},
						{"GD", "Grenada"},
						{"GP", "Guadeloupe"},
						{"GU", "Guam"},
						{"GT", "Guatemala"},
						{"GG", "Guernsey"},
						{"GN", "Guinea"},
						{"GW", "Guinea-Bissau"},
						{"GY", "Guyana"},
						{"HT", "Haiti"},
						{"VA", "Holy See (Vatican City)"},
						{"HN", "Honduras"},
						{"HK", "Hong Kong"},
						{"HU", "Hungary"},
						{"IS", "Iceland"},
						{"IN", "India"},
						{"ID", "Indonesia"},
						{"IR", "Iran"},
						{"IQ", "Iraq"},
						{"IE", "Ireland"},
						{"IL", "Israel"},
						{"IT", "Italy"},
						{"JM", "Jamaica"},
						{"JP", "Japan"},
						{"JE", "Jersey"},
						{"JO", "Jordan"},
						{"KZ", "Kazakhstan"},
						{"KE", "Kenya"},
						{"KI", "Kiribati"},
						{"KP", "Korea, North"},
						{"KR", "Korea, South"},
						{"KW", "Kuwait"},
						{"KG", "Kyrgyzstan"},
						{"LA", "Laos"},
						{"LV", "Latvia"},
						{"LB", "Lebanon"},
						{"LS", "Lesotho"},
						{"LR", "Liberia"},
						{"LY", "Libya"},
						{"LI", "Liechtenstein"},
						{"LT", "Lithuania"},
						{"LU", "Luxembourg"},
						{"MO", "Macao"},
						{"MK", "Macedonia, The Former Yugoslav Republic of"},
						{"MG", "Madagascar"},
						{"MW", "Malawi"},
						{"MY", "Malaysia"},
						{"MV", "Maldives"},
						{"ML", "Mali"},
						{"MT", "Malta"},
						{"IM", "Man, Isle of"},
						{"MH", "Marshall Islands"},
						{"MQ", "Martinique"},
						{"MR", "Mauritania"},
						{"MU", "Mauritius"},
						{"YT", "Mayotte"},
						{"MX", "Mexico"},
						{"FM", "Micronesia, Federated States of"},
						{"MD", "Moldova"},
						{"MC", "Monaco"},
						{"MN", "Mongolia"},
						{"ME", "Montenegro"},
						{"MS", "Montserrat"},
						{"MA", "Morocco"},
						{"MZ", "Mozambique"},
						{"NA", "Namibia"},
						{"NR", "Nauru"},
						{"NP", "Nepal"},
						{"NL", "Netherlands"},
						{"AN", "Netherlands Antilles"},
						{"NC", "New Caledonia"},
						{"NZ", "New Zealand"},
						{"NI", "Nicaragua"},
						{"NE", "Niger"},
						{"NG", "Nigeria"},
						{"NU", "Niue"},
						{"NF", "Norfolk Island"},
						{"MP", "Northern Mariana Islands"},
						{"NO", "Norway"},
						{"OM", "Oman"},
						{"PK", "Pakistan"},
						{"PW", "Palau"},
						{"PS", "Palestinian Territory, Occupied"},
						{"PA", "Panama"},
						{"PG", "Papua New Guinea"},
						{"PY", "Paraguay"},
						{"PE", "Peru"},
						{"PH", "Philippines"},
						{"PN", "Pitcairn Islands"},
						{"PL", "Poland"},
						{"PT", "Portugal"},
						{"PR", "Puerto Rico"},
						{"QA", "Qatar"},
						{"RE", "Réunion"},
						{"RO", "Romania"},
						{"RU", "Russia"},
						{"RW", "Rwanda"},
						{"SH", "Saint Helena"},
						{"KN", "Saint Kitts and Nevis"},
						{"LC", "Saint Lucia"},
						{"PM", "Saint Pierre and Miquelon"},
						{"VC", "Saint Vincent and the Grenadines"},
						{"WS", "Samoa"},
						{"SM", "San Marino"},
						{"ST", "São Tomé and Príncipe"},
						{"SA", "Saudi Arabia"},
						{"SN", "Senegal"},
						{"RS", "Serbia"},
						{"SC", "Seychelles"},
						{"SL", "Sierra Leone"},
						{"SG", "Singapore"},
						{"SK", "Slovakia"},
						{"SI", "Slovenia"},
						{"SB", "Solomon Islands"},
						{"SO", "Somalia"},
						{"ZA", "South Africa"},
						{"GS", "South Georgia and the South Sandwich Islands"},
						{"ES", "Spain"},
						{"LK", "Sri Lanka"},
						{"SD", "Sudan"},
						{"SR", "Suriname"},
						{"SJ", "Svalbard"},
						{"SZ", "Swaziland"},
						{"SE", "Sweden"},
						{"CH", "Switzerland"},
						{"SY", "Syria"},
						{"TW", "Taiwan"},
						{"TJ", "Tajikistan"},
						{"TZ", "Tanzania"},
						{"TH", "Thailand"},
						{"TG", "Togo"},
						{"TK", "Tokelau"},
						{"TO", "Tonga"},
						{"TT", "Trinidad and Tobago"},
						{"TN", "Tunisia"},
						{"TR", "Turkey"},
						{"TM", "Turkmenistan"},
						{"TC", "Turks and Caicos Islands"},
						{"TV", "Tuvalu"},
						{"UG", "Uganda"},
						{"UA", "Ukraine"},
						{"AE", "United Arab Emirates"},
						{"UK", "United Kingdom"},
						{"US", "United States"},
						{"UY", "Uruguay"},
						{"UZ", "Uzbekistan"},
						{"VU", "Vanuatu"},
						{"VE", "Venezuela"},
						{"VN", "Vietnam"},
						{"VI", "Virgin Islands"},
						{"WF", "Wallis and Futuna"},
						{"EH", "Western Sahara"},
						{"YE", "Yemen"},
						{"ZM", "Zambia"},
						{"ZW", "Zimbabwe"}
				};

				View view2 = View.inflate(this, R.layout.submit_dialog, null);
				final EditText m_id = (EditText) view2.findViewById(R.id.submit_id);                //提交用户 id
				final EditText m_email = (EditText) view2.findViewById(R.id.submit_email);          //提交 邮箱
				final Spinner m_country = (Spinner) view2.findViewById(R.id.submit_country);        //提交 国家或地区

				m_id.setText(myMaps.nickname);
				m_email.setText(myMaps.email);

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getData(m_menu)) {
					@Override
					public View getView(int position, View convertView, ViewGroup parent)
					{
						return setCentered(super.getView(position, convertView, parent));
					}

					@Override
					public View getDropDownView(int position, View convertView, ViewGroup parent)
					{
						return setCentered(super.getDropDownView(position, convertView, parent));
					}

					private View setCentered(View view)
					{
						TextView textView = (TextView)view.findViewById(android.R.id.text1);
						textView.setGravity(Gravity.LEFT);  //居左显示条目
						return view;
					}
				};

				m_Sel_id2 = 0;
				for (int k = 0; k < m_menu.length; k++) {
					if (m_menu[k][0].equals(myMaps.country)) {
						m_Sel_id2 = k;
						break;
					}
				}
				m_country.setAdapter(adapter);
				m_country.setSelection(m_Sel_id2, true);

				AlertDialog.Builder builder4 = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
				builder4.setTitle("提交").setView(view2).setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						myMaps.nickname = m_id.getText().toString().trim();
						myMaps.email = m_email.getText().toString().trim();
						myMaps.country = m_menu[m_country.getSelectedItemPosition()][0];
						myMaps.m_State = mySQLite.m_SQL.load_State(m_Sel_id);
						new Thread(new MyThread()).start();
					}});
				builder4.setCancelable(false).show();
				break;
			case 12:  //生成 GIF 演示动画
//				MyToast.showToast(myStateBrow.this, "这里不支持标尺与箱子编号！", Toast.LENGTH_SHORT);
				File targetDir = new File(myMaps.sRoot+myMaps.sPath + "GIF/");
				if (!targetDir.exists()) targetDir.mkdirs();  //创建自定义GIF文件夹

                final String[] m_menu2 = {  //帧间隔（毫秒）
                    "自动", "100", "200", "300", "500", "1000", "2000"
                };

				View view3 = View.inflate(this, R.layout.gif_set_dialog, null);
				final CheckBox m_gif_act_type = (CheckBox) view3.findViewById(R.id.gif_act_type);         //帧类型
				final CheckBox m_gif_act_skin = (CheckBox) view3.findViewById(R.id.gif_act_skin);          //现场皮肤
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
                                if (mDialog3 == null) {
                                    mDialog3 = new myGifMakeFragment ();
									if (gif_mark_1.isChecked ()) m_Gif_Mark = 0;
									else if (gif_mark_3.isChecked ()) m_Gif_Mark = 2;
									else m_Gif_Mark = 1;

									//异步合成 GIF
                                    Bundle bundle = new Bundle ();
                                    myMaps.m_State = mySQLite.m_SQL.load_State (m_Sel_id);
									bundle.putInt("m_Gif_Mark", m_Gif_Mark);  //水印
									bundle.putInt("m_Gif_Start", 0);  //GIF 的起点
                                    bundle.putBoolean ("m_Type", m_Gif_Type);
                                    bundle.putBoolean ("m_Skin", m_Gif_Skin);
									bundle.putBooleanArray("my_Rule", null);  //需要显示标尺的格子，答案动画不支持标尺与箱子编号的显示
									bundle.putShortArray("my_BoxNum", null);  //人工箱子编号数组，答案动画不支持标尺与箱子编号的显示
									bundle.putInt ("m_Interval", m_Gif_Interval);
                                    bundle.putString ("m_Ans", myMaps.m_State.ans.replaceAll ("[^lurdLURD]", ""));
                                    mDialog3.setArguments (bundle);
                                    mDialog3.show (getFragmentManager (), TAG_PROGRESS_DIALOG_FRAGMENT);
                                }
                            }
                        }).setCancelable(false).show();

                break;
 			}
		}
		return true;
	}

	@Override
	public void onGifMakeDone(String inf) {  //异步合成 Gif 返回
		if (mDialog3 != null) {
			mDialog3.dismiss();
			mDialog3 = null;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		builder.setTitle("信息").setMessage(inf).setPositiveButton("确定", null);
		builder.setCancelable(false).create().show();
	}

	//使用独立进程提交答案
	public class MyThread implements Runnable {
		@Override
		public void run() {
			send();
		}
	}

	//装填国家列表项
	private List<String> getData(String[][] m_arr) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < m_arr.length; i++) {
			list.add(m_arr[i][1]);
		}
		return list;
	}

	//提交信息
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			String s;
			final int code = msg.what;

			if (msg.what == 0) {
				s = "错误";
			} else {
				s = "已提交";
			}
			AlertDialog.Builder builder = new AlertDialog.Builder (myStateBrow.this, AlertDialog.THEME_HOLO_DARK);
			builder.setTitle (s).setMessage (msg.obj.toString ()).setPositiveButton ("确定", new DialogInterface.OnClickListener () {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					if (code == 1) {
						//比赛答案提交列表
						Intent intent3 = new Intent ();
						intent3.setClass (myStateBrow.this, mySubmitList.class);
						startActivity (intent3);
					}
				}
			});
			builder.setCancelable (false).create ().show ();
		}
	};

	//提交答案
	public void send() {
		String target = "http://sokoban.cn/submit_result.php";	//要提交的目标地址
		URL url;
		try {
			url = new URL(target);
			HttpURLConnection urlConn = (HttpURLConnection) url.openConnection(); // 创建一个HTTP连接
			urlConn.setRequestMethod("POST"); // 指定使用POST请求方式
			urlConn.setDoInput(true); // 向连接中写入数据
			urlConn.setDoOutput(true); // 从连接中读取数据
			urlConn.setUseCaches(false); // 禁止缓存
			urlConn.setInstanceFollowRedirects(true);	//自动执行HTTP重定向
			urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // 设置内容类型
			DataOutputStream out = new DataOutputStream(urlConn.getOutputStream()); // 获取输出流
			String param = "nickname="
					+ URLEncoder.encode(myMaps.nickname, "GBK")  // 强制使用接受服务器的字符编码上传各个参数GB2312
					+ "&country="
					+ URLEncoder.encode(myMaps.country, "GBK")
					+ "&email="
					+ URLEncoder.encode(myMaps.email, "GBK")
					+ "&lurd="
					+ URLEncoder.encode(myMaps.m_State.ans, "GBK");	//连接要提交的数据
			out.writeBytes(param);//将要传递的数据写入数据输出流
			out.flush();	//输出缓存
			out.close();	//关闭数据输出流
			// 判断是否响应成功
			int responseCode = urlConn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStreamReader in = new InputStreamReader(urlConn.getInputStream()); // 获得读取的内容
				BufferedReader buffer = new BufferedReader(in); // 获取输入流对象
				String inputLine;
				while ((inputLine = buffer.readLine()) != null) {
					result += inputLine + "\n";
				}
				in.close();	//关闭字符输入流
				Message msg = Message.obtain();

				if (result.toLowerCase().indexOf("correct (for ") >= 0) {
					msg.what = 1;
					msg.obj = "成功，恭喜过关！";
				} else if (result.toLowerCase().indexOf("not correct") >= 0) {
					msg.what = 2;
					msg.obj = "答案不正确！";
				} else if (result.toLowerCase().indexOf("competition has ended") >= 0) {
					msg.what = 3;
					msg.obj = "比赛已过期，请关注下一期！";
				} else if (result.toLowerCase().indexOf("not begin yet") >= 0) {
					msg.what = 4;
					msg.obj = "比赛尚未开始，请耐心等待！";
				} else if (result.toLowerCase().indexOf("name cannot be empty") >= 0) {
					msg.what = 5;
					msg.obj = "姓名不能空着！";
				} else {
					msg.what = 6;
					msg.obj = "未知情况！";
				}
				handler.sendMessage(msg);
			} else {
				Message msg = Message.obtain();
				msg.what = 0;
				msg.obj = "网络错误，请与网站管理员或程序员联系！";
				handler.sendMessage(msg);
			}
			urlConn.disconnect();	//断开连接
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			if (groupPosition == 0)
				return myMaps.mState1.size();
			else
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
			if (groupPosition == 0)
				return myMaps.mState1.get(childPosition);
			else
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
			if (groupPosition == 1) {
				ts_group.setText(s_groups[groupPosition] + s_sort[my_Sort]);
			} else {
				ts_group.setText(s_groups[groupPosition]);
			}
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

			if (groupPosition == 0) {
				ts_child.setText(myMaps.mState1.get(childPosition).inf);
				ts_child2.setText(myMaps.mState1.get(childPosition).time);
			} else {
				ts_child.setText(myMaps.mState2.get(childPosition).inf);
				ts_child2.setText(myMaps.mState2.get(childPosition).time);
			}
	
			return convertView;
		}
	
		//二级列表中的item是否能够被选中
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}
	 
	void set_State() {
		myMaps.m_StateIsRedy = true;
        finish();
	}

	//取得"推箱快手/"文件夹下的“.txt”文档列表
	private void mTxtList() {
		File targetDir = new File(myMaps.sRoot + myMaps.sPath + "款/");
		myMaps.mFile_List.clear();
		if (!targetDir.exists()) targetDir.mkdirs();  //创建"导入/"文件夹
		else {
			String[] filelist = targetDir.list();
			Arrays.sort(filelist, String.CASE_INSENSITIVE_ORDER);
			for (int i = 0; i < filelist.length; i++) {
				int dot = filelist[i].lastIndexOf('.');
				if ((dot > -1) && (dot < (filelist[i].length()))) {
					String prefix = filelist[i].substring(filelist[i].lastIndexOf(".") + 1);
					if (prefix.equalsIgnoreCase("txt"))
						myMaps.mFile_List.add(filelist[i]);
				}
			}
		}
	}

	//保存“答案”到文档
	private void saveAnsToFile(final long my_id) {
		final String fn = new StringBuilder(myMaps.sFile).append("_").append(myMaps.m_lstMaps.indexOf(myMaps.curMap)+1).append(".txt").toString();

		final EditText et = new EditText(this);
		et.setBackgroundColor(0xff444444);
		et.setText(fn);

		mTxtList();
		myMaps.mFile_List.add(0, "自动名称");

		new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("文档名").setView(et)
				.setSingleChoiceItems(myMaps.mFile_List.toArray(new String[myMaps.mFile_List.size()]), -1, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which > 0) {
							et.setText(myMaps.mFile_List.get(which));
						} else {
							et.setText(fn);
						}
					}
				}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					File targetDir = new File(myMaps.sRoot + myMaps.sPath + "导出/");
					myMaps.mFile_List.clear();
					if (!targetDir.exists()) targetDir.mkdirs();  //创建"导出/"文件夹

					String str = et.getText().toString().trim();
					String prefix = str.substring(str.lastIndexOf(".") + 1);
					if (!prefix.equalsIgnoreCase("txt")) {
						str = str + ".txt";
					}
					final String my_Name = str;
					File file = new File(myMaps.sRoot + myMaps.sPath + "导出/" + my_Name);
					if (file.exists()) {
						new AlertDialog.Builder(myStateBrow.this, AlertDialog.THEME_HOLO_DARK).setMessage("文档已存在，覆写吗？\n导出/" + my_Name)
								.setNegativeButton("取消", null).setPositiveButton("覆写", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								if (writeStateFile(my_Name, my_id)) {
									MyToast.showToast(myStateBrow.this, "导出成功！", Toast.LENGTH_SHORT);
								} else {
									MyToast.showToast(myStateBrow.this, "出错了，导出失败！", Toast.LENGTH_SHORT);
								}
							}}).setCancelable(false).show();
					} else {
						if (writeStateFile(my_Name, my_id)) {
							MyToast.showToast(myStateBrow.this, "导出成功！", Toast.LENGTH_SHORT);
						} else {
							MyToast.showToast(myStateBrow.this, "出错了，导出失败！", Toast.LENGTH_SHORT);
						}
					}
				} catch (Exception e) {
					MyToast.showToast(myStateBrow.this, "出错了，导出失败！", Toast.LENGTH_SHORT);
				}
			}
		}).setNegativeButton("取消", null).setCancelable(false).show();
	}

	//导出状态
	private boolean writeStateFile(String my_Name, long myID) {
		try{   
			FileOutputStream fout = new FileOutputStream(myMaps.sRoot+myMaps.sPath+"导出/"+my_Name);
			
			fout.write(myMaps.curMap.Map.getBytes());
			StringBuilder s = new StringBuilder();
			s.append("\nTitle: ").append(myMaps.curMap.Title).append("\nAuthor: ").append(myMaps.curMap.Author);
			if (!myMaps.curMap.Comment.trim().isEmpty()) {
				s.append("\nComment:\n").append(myMaps.curMap.Comment).append("\nComment-End:");
			}
			fout.write(s.toString().getBytes());

			if (myID < 0) {  //导出全部答案
				int len0 = myMaps.mState2.size();
				for (int t = 0; t < len0; t++) {
					myMaps.m_State = mySQLite.m_SQL.load_State(myMaps.mState2.get(t).id);
					String strAns = myMaps.m_State.ans.replaceAll("[^lurdLURD]", "");
					int len = strAns.length();
					if (len > 0) {
						fout.write('\n');
						int p = 0;
						for (int k = 0; k < len; k++) {
							if ("LURD".indexOf(strAns.charAt(k)) >= 0) p++;
						}
						fout.write(("Solution (moves " + len + ", pushes " + p + (myMaps.m_Sets[30] == 1 ? myMaps.m_State.time : "") + "): \n").getBytes());
						fout.write(strAns.getBytes());
					}
				}
			} else {  //导出单个状态或答案   && !myMaps.m_State.time.isEmpty()
				myMaps.m_State = mySQLite.m_SQL.load_State(myID);
				String strAns = myMaps.m_State.ans.replaceAll("[^lurdLURD]", "");
				int len = strAns.length();
				byte t;
				if (len > 0) {
					fout.write('\n');
					if (myMaps.m_State.solution == 1) {  //若是答案
						int p = 0;
						for (int k = 0; k < len; k++) {
							if ("LURD".indexOf(strAns.charAt(k)) >= 0) p++;
						}
						fout.write(("Solution (moves " + len + ", pushes " + p + (myMaps.m_Sets[30] == 1 ? myMaps.m_State.time : "") + "): \n").getBytes());
					}
					fout.write(strAns.getBytes());
				}

				if (myMaps.m_State.solution != 1) {  //若不是答案，还要加上逆推动作
					len = myMaps.m_State.bk_ans.length();
					if (len > 0) {  //逆推仓管员坐标，（x，y）-- 先列后行
						fout.write('\n');
						fout.write('[');
						fout.write(Integer.toString(myMaps.m_State.c).getBytes("UTF-8"));
						fout.write(',');
						fout.write(Integer.toString(myMaps.m_State.r).getBytes("UTF-8"));
						fout.write(']');
						for (int k = 0; k < len; k++) {
							t = (byte) myMaps.m_State.bk_ans.charAt(k);
							if ("lurdLURD".indexOf(t) >= 0)
								fout.write(t);
						}
					}
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
	private void myExport2(long myID) {
		StringBuilder str = new StringBuilder();
		try{
			
			str.append(myMaps.curMap.Map);

			if (myID < 0) {  //导出全部答案
				int len0 = myMaps.mState2.size();
				for (int t = 0; t < len0; t++) {
					myMaps.m_State = mySQLite.m_SQL.load_State(myMaps.mState2.get(t).id);
					String strAns = myMaps.m_State.ans.replaceAll("[^lurdLURD]", "");
					int len = strAns.length();
					if (len > 0) {
						str.append('\n');
						if (myMaps.m_State.solution == 1) {  //若是答案
							int p = 0;
							for (int k = 0; k < len; k++) {
								if ("LURD".indexOf(strAns.charAt(k)) >= 0) p++;
							}
							str.append("Solution (moves ").append(len).append(", pushes ").append(p);
							if (myMaps.m_Sets[30] == 1) {  //是否导出答案备注
								str.append(", comment ").append(myMaps.m_State.time);
							}
							str.append("): \n");
						}
						str.append(strAns);
					}
				}
			} else {  //导出单个状态或答案
				myMaps.m_State = mySQLite.m_SQL.load_State(myID);
				String strAns = myMaps.m_State.ans.replaceAll("[^lurdLURD]", "");
				int len = strAns.length();
				if (len > 0) {
					str.append('\n');
					if (myMaps.m_State.solution == 1) {  //若是答案
						int p = 0;
						for (int k = 0; k < len; k++) {
							if ("LURD".indexOf(strAns.charAt(k)) >= 0) p++;
						}
						str.append("Solution (moves ").append(len).append(", pushes ").append(p);
						if (myMaps.m_Sets[30] == 1) {  //是否导出答案备注
							str.append(", comment ").append(myMaps.m_State.time);
						}
						str.append("): \n");
					}
					str.append(strAns);
				}

				if (myMaps.m_State.solution != 1) {  //若不是答案，还要加上逆推动作
					len = myMaps.m_State.bk_ans.length();
					if (len > 0) {  //逆推仓管员坐标，（x，y）-- 先列后行
						str.append('\n');
						str.append('[');
						str.append(myMaps.m_State.c);
						str.append(',');
						str.append(myMaps.m_State.r);
						str.append(']');
						str.append(myMaps.m_State.bk_ans.replaceAll("[^lurdLURD]", ""));
					}
				}
			}
			final EditText et = new EditText(myStateBrow.this);
			et.setTypeface(Typeface.MONOSPACE);
			et.setCursorVisible(false);
			et.setFocusable(false);
			et.setFocusableInTouchMode(false);
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
			int len = myMaps.m_State.ans.length();
			if (len > 0) {
				str.append(myMaps.m_State.ans.replaceAll("[^lurdLURD]", ""));
			}

			len = myMaps.m_State.bk_ans.length();
			if (len > 0) {  //逆推仓管员坐标，（x，y）-- 先列后行
				str.append("\n[");
				str.append(myMaps.m_State.c);
				str.append(',');
				str.append(myMaps.m_State.r);
				str.append(']');
				str.append(myMaps.m_State.bk_ans.replaceAll("[^lurdLURD]", ""));
			}
			final EditText et = new EditText(myStateBrow.this);
			et.setTypeface(Typeface.MONOSPACE);
			et.setCursorVisible(false);
			et.setFocusable(false);
			et.setFocusableInTouchMode(false);
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

	//导出到剪切板: 正推 Lurd
	private void myExport3() {
		StringBuilder str = new StringBuilder();
		try{
			int len = myMaps.m_State.ans.length();
			if (len > 0) {
				str.append(myMaps.m_State.ans.replaceAll("[^lurdLURD]", ""));
			}

			final EditText et = new EditText(myStateBrow.this);
			et.setTypeface(Typeface.MONOSPACE);
			et.setCursorVisible(false);
			et.setFocusable(false);
			et.setFocusableInTouchMode(false);
			et.setText(str.toString());
			new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("剪切板：正推 Lurd").setView(et).setCancelable(false)
				.setNegativeButton("取消", null)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						myMaps.saveClipper(et.getText().toString());
					}}).create().show();
	  	}catch(Exception e){
	  	}
	}

	//导出到剪切板: 逆推 Lurd
	private void myExport4() {
		StringBuilder str = new StringBuilder();
		try{
			int len = myMaps.m_State.bk_ans.length();
			if (len > 0) {  //逆推仓管员坐标，（x，y）-- 先列后行
				str.append('[');
				str.append(myMaps.m_State.c);
				str.append(',');
				str.append(myMaps.m_State.r);
				str.append(']');
				str.append(myMaps.m_State.bk_ans.replaceAll("[^lurdLURD]", ""));
			}

			final EditText et = new EditText(myStateBrow.this);
			et.setTypeface(Typeface.MONOSPACE);
			et.setCursorVisible(false);
			et.setFocusable(false);
			et.setFocusableInTouchMode(false);
			et.setText(str.toString());
			new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK).setTitle("剪切板：逆推 Lurd").setView(et).setCancelable(false)
				.setNegativeButton("取消", null)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						myMaps.saveClipper(et.getText().toString());
					}}).create().show();
	  	}catch(Exception e){
	  	}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.state, menu);
		if (myMaps.m_Sets[30] == 0) {  //导出答案的注释信息
			menu.getItem(2).setChecked(false);
		} else {
			menu.getItem(2).setChecked(true);
		}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem mt) {
	    switch (mt.getItemId()) {
		//菜单栏返回键功能
		case android.R.id.home:
			this.finish();

			return true;
		case R.id.st_ex_all_ans_file:  //导出全部答案到文档
			saveAnsToFile(-1);
			return true;
		case R.id.st_ex_all_ans:  //导出全部答案到剪切板
			myExport2(-1);
			return true;
		case R.id.st_ex_ans_comment:  //导出答案的注释信息
			if (myMaps.m_Sets[30] == 1) {
				myMaps.m_Sets[30] = 0;
				mt.setChecked(false);
			} else {
				myMaps.m_Sets[30] = 1;
				mt.setChecked(true);
			}
			return true;
		case R.id.st_ex_submit_list:  //比赛答案提交列表
			//比赛答案提交列表
			Intent intent3 = new Intent();
			intent3.setClass(this, mySubmitList.class);
			startActivity(intent3);
			return true;
       default:
       		return super.onOptionsItemSelected(mt);
       }
	}
}

class SortComparator implements Comparator {
	@Override
	public int compare(Object lhs, Object rhs) {
		state_Node a = (state_Node) lhs;
		state_Node b = (state_Node) rhs;
		if (myStateBrow.my_Sort == 0) {
			if (a.moves == b.moves) return (a.pushs - b.pushs);
			else return (a.moves - b.moves);
		} else {
			if (a.pushs == b.pushs) return (a.moves - b.moves);
			else return (a.pushs - b.pushs);
		}
	}
}
