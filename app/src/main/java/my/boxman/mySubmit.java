package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class mySubmit extends Activity {
	private EditText m_id = null;
	private EditText m_email = null;
	private Spinner m_country = null;
	private Button Bt_OK = null;
	private Button Bt_Cancel = null;

	private String result = ""; // 声明Post返回值的的字符串

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
	int m_Sel_id;

	//装填国家列表项
	private List<String> getData(String[][] m_arr) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < m_arr.length; i++) {
			list.add(m_arr[i][1]);
		}
		return list;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.submit);

		final EditText m_id = (EditText) this.findViewById(R.id.submit_id);                //提交用户 id
		final EditText m_email = (EditText) this.findViewById(R.id.submit_email);          //提交 邮箱
		final Spinner m_country = (Spinner) this.findViewById(R.id.submit_country);        //提交 国家或地区
		final Button bt_OK = (Button) this.findViewById(R.id.submit_OK);                   //提交按钮
		final Button bt_Cancel = (Button) this.findViewById(R.id.submit_Cancel);           //返回按钮

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

		m_Sel_id = 0;
		for (int k = 0; k < m_menu.length; k++) {
			if (m_menu[k][0].equals(myMaps.country)) {
				m_Sel_id = k;
				break;
			}
		}
		m_country.setAdapter(adapter);
		m_country.setSelection(m_Sel_id, true);

		bt_OK.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				myMaps.nickname = m_id.getText().toString().trim();
				myMaps.email = m_email.getText().toString().trim();
				myMaps.country = m_menu[m_country.getSelectedItemPosition()][0];
				new Thread(new mySubmit.MyThread()).start();
			}
		});

		bt_Cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

        //设置标题栏标题，并开启标题栏的返回键
		setTitle("比赛答案提交");
		
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);

    }

	//使用独立进程提交答案
	public class MyThread implements Runnable {
		@Override
		public void run() {
			send();
		}
	}

	//提交信息
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (msg.what == 1) {
				//比赛答案提交列表
				Intent intent3 = new Intent ();
				intent3.setClass (mySubmit.this, mySubmitList.class);
				startActivity (intent3);
				finish();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder (mySubmit.this, AlertDialog.THEME_HOLO_DARK);
				builder.setTitle("错误").setMessage (msg.obj.toString ()).setPositiveButton ("确定", null);
				builder.setCancelable (false).create ().show ();
			}
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
					msg.obj = "提交成功！";
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
			Message msg = Message.obtain();
			msg.what = 0;
			msg.obj = "网络或读写错误！";
			handler.sendMessage(msg);
		} catch (IOException e) {
			Message msg = Message.obtain();
			msg.what = 0;
			msg.obj = "网络或读写错误！";
			handler.sendMessage(msg);
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