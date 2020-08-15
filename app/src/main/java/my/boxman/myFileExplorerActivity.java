package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class myFileExplorerActivity extends Activity {
	AlertDialog exitDlg;

	ListView listView;
	TextView textView;
	//记录当前父文件夹
	File currentParent;
	//记录当前路经下的所有文件
	File[] currentFiles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.line_list);
		listView = (ListView)findViewById(R.id.filelist);
		textView = (TextView)findViewById(R.id.tv_file_path);

		//开启标题栏的返回键
		ActionBar actionBar = getActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);

		//获取系统SD卡目录
		File root;
		if (!myMaps.myPathList[2].isEmpty()) {
			File targetDir = new File(myMaps.sRoot+myMaps.myPathList[2]);
			if (targetDir.exists()) {
				root = new File(myMaps.sRoot+myMaps.myPathList[2]);
			} else {
				root = new File(myMaps.sRoot);
			}
		} else {
			root = new File(myMaps.sRoot);
		}

		if (root.exists()) {
			currentParent = root;
			currentFiles = root.listFiles();
			//使用当前目录下的全部文件，来填充ListView
			inflateListView(currentFiles);
		} else {
			MyToast.showToast(this, "系统错误，无法执行该操作！", Toast.LENGTH_SHORT);
			finish();
		}

		setTitle("自定义位置");

		//为ListView的列表项单击事件绑定监听器
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//用户单击了文件，直接返回
				if (currentFiles[position].isFile()) return;
				//获取单击文件夹下的所有文件
				File[] tmp = currentFiles[position].listFiles();
				if (tmp != null) {
					//获取用户单击的列表项对应的文件夹，设为当前父文件夹
					currentParent = currentFiles[position];
					//保存当前父文件夹内的所有文件和文件夹
					currentFiles = tmp;
					//再次更新ListView
					inflateListView(currentFiles);
				}
			}
		});

		AlertDialog.Builder dlg0 = new AlertDialog.Builder(this);
		dlg0.setTitle("提醒").setMessage("退出浏览，确定吗？").setCancelable(false).setNegativeButton("取消", null)
				.setPositiveButton("确定", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		exitDlg = dlg0.create();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.filelist, menu);

		return true;
	}


	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			return true;

		return super.onKeyUp(keyCode, event);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			String str = textView.getText().toString();
			if (str.isEmpty()) exitDlg.show();
			else myParent();

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onOptionsItemSelected(MenuItem mt) {
		switch (mt.getItemId()) {
			//标题栏返回键功能
			case android.R.id.home:
				this.finish();
				return true;
			case R.id.filelist_ok:  // 完成
				// 保存自定义位置
				String str = textView.getText().toString();
//				if (!str.isEmpty()) {
					myMaps.myPathList[myMaps.m_Sets[36]] = str + '/';
//					myMaps.m_Sets[36] = 2;
					Intent intent = new Intent();
					setResult(999, intent);
//				}
				this.finish();
				return true;
			case R.id.filelist_parent:  // 上一级
				myParent();
				return true;
			default:
				return super.onOptionsItemSelected(mt);
		}
	}


	private void myParent() {
		try {
			if (!currentParent.getCanonicalPath().equals(myMaps.sRoot)) {
				//获取上一目录
				currentParent = currentParent.getParentFile();
				//列出当前目录下的所有文件
				currentFiles = currentParent.listFiles();
				//再次更新ListView
				inflateListView(currentFiles);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void inflateListView(File[] files) {
		//创建List集合，元素是Map
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		String fn;
		int dot;

		Collections.sort(Arrays.asList(files), new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if (o1.isDirectory() && o2.isFile())
					return -1;
				if (o1.isFile() && o2.isDirectory())
					return 1;
				return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			}
		});

		for (int i = 0; i < files.length; i++ ) {
			Map<String, Object> listItem = new HashMap<String, Object>();
			//如果当前File是文件夹，使用文件夹图标，其它使用文件图标
			if (files[i].isDirectory()) {
				listItem.put("icon", R.drawable.folder);
			} else {
				fn = files[i].getName();
				dot = fn.lastIndexOf('.');
				if ((dot > -1) && (dot < fn.length())) {
					String prefix = fn.substring(fn.lastIndexOf(".") + 1);
					if (!(prefix.equalsIgnoreCase("jpg") || prefix.equalsIgnoreCase("bmp") || prefix.equalsIgnoreCase("png"))){
						continue;
					}
				}
				listItem.put("icon", R.drawable.file);
			}
			listItem.put("file", files[i].getName());
			//添加List项
			listItems.add(listItem);
		}


		//创建
		SimpleAdapter simpleAdapter = new SimpleAdapter(this
				,listItems ,R.layout.line
				,new String[]{"icon", "file"}
				,new int[] {R.id.icon, R.id.file_name});
		//为ListView设置Adapter
		listView.setAdapter(simpleAdapter);
		try {
			textView.setText(currentParent.getCanonicalPath().replace(myMaps.sRoot, ""));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}