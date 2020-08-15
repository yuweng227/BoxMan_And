package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

public class myPicListView extends Activity implements OnScrollListener {
	GridView mGridView = null;
	ItemClickListener mItemClickListener = null;

	//缓存 GridView 中每个 Item 的图片
	public static SparseArray <Bitmap> picBitmapCaches = new SparseArray <Bitmap>();

	myPicListViewAdapter adapter = null;

	public long currentTime = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

		setContentView(R.layout.my_piclist_view);

		//开启标题栏的返回键
		ActionBar actionBar = getActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);

		adapter = new myPicListViewAdapter(this);
		mItemClickListener = new ItemClickListener();
		mGridView = (GridView)findViewById(R.id.m_piclistView);

		mGridView.setAdapter(adapter);
		mGridView.setOnScrollListener(this);
		mGridView.setOnItemClickListener(mItemClickListener);

		//设置标题栏标题为图片路径
		setTitle(myMaps.myPathList[myMaps.m_Sets[36]]);

		recycleBitmapCaches(0, myMaps.mFile_List.size());
		adapter.notifyDataSetChanged();
	}

	//释放图片的函数
	private void recycleBitmapCaches(int fromPosition,int toPosition){
		Bitmap delBitmap;
		for(int del=fromPosition;del<toPosition;del++){
			delBitmap = picBitmapCaches.get(del);
			if(delBitmap != null){
				picBitmapCaches.remove(del);
				try {
					delBitmap.recycle();
				} catch (Exception e){
				}
			}
		}
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		try {  // 卷动时，删除屏幕之外的图标，防止 OOM
	        recycleBitmapCaches(0,firstVisibleItem);
	        recycleBitmapCaches(firstVisibleItem+visibleItemCount, totalItemCount);
		} catch (Exception e) { }
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
			if (System.currentTimeMillis() - currentTime < 2000) return;
			currentTime = System.currentTimeMillis();

			loadEDPic(myMaps.mFile_List.get(arg2));

			if (myMaps.edPict != null) {
				Intent intent1 = new Intent();
				intent1.setClass(myPicListView.this, myRecogView.class);
				startActivity(intent1);
			} else {
				MyToast.showToast(myPicListView.this, "图片文档读取出错或权限不够！", Toast.LENGTH_SHORT);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.piclist, menu);

		return true;
	}

    public boolean onOptionsItemSelected(MenuItem mt) {
		switch (mt.getItemId()) {
			//标题栏返回键功能
			case android.R.id.home:
				this.finish();
				return true;
			case R.id.pic_path:  // 浏览位置
				String[] m_menu = {
						"快手默认位置",
						"QQ 图片接收文件夹",
						"自定义"
				};
				int m = myMaps.m_Sets[36];
				if (m < 0 || m > 2) m = -1;
				AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
				builder.setTitle("截图位置").setSingleChoiceItems(m_menu, m, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								if ((which == 0 || which == 1) && which != myMaps.m_Sets[36]) {
									recycleBitmapCaches(0, myMaps.mFile_List.size());
									myMaps.m_Sets[36] = which;
									myMaps.edPicList(myMaps.sRoot + myMaps.myPathList[myMaps.m_Sets[36]]);
									setTitle(myMaps.myPathList[myMaps.m_Sets[36]]);
									adapter.notifyDataSetChanged();
								} else if (which == 2) {
									Intent intent1 = new Intent();
									intent1.setClass(myPicListView.this, myFileExplorerActivity.class);
									startActivityForResult(intent1, 999);
								}
							}
						}).setPositiveButton("取消", null);
				builder.setCancelable(false).show();

				return true;
			default:
				return super.onOptionsItemSelected(mt);
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 999) {
			recycleBitmapCaches(0, myMaps.mFile_List.size());
			myMaps.edPicList(myMaps.sRoot + myMaps.myPathList[myMaps.m_Sets[36]]);
			setTitle(myMaps.myPathList[myMaps.m_Sets[36]]);
			adapter.notifyDataSetChanged();
        }
    }

	//加载关卡截图
	private static void loadEDPic(String fn) {
		if (myMaps.edPict != null) myMaps.edPict.recycle();
		myMaps.edPict = null;

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		try{
			opts.inJustDecodeBounds = false;   //提取图片
			opts.inDither=false;    /*不进行图片抖动处理*/
			opts.inPreferredConfig=null;  /*设置让解码器以最佳方式解码*/
			/* 下面两个字段需要组合使用 */
			opts.inPurgeable = true;
			opts.inInputShareable = true;
			myMaps.edPict = BitmapFactory.decodeFile(myMaps.sRoot + myMaps.myPathList[myMaps.m_Sets[36]] + fn, opts);
		} catch (Exception e) {
			if (myMaps.edPict != null && !myMaps.edPict.isRecycled()) {
				myMaps.edPict.recycle();
				myMaps.edPict = null;
			}
			System.gc();
			MyToast.showToast(myMaps.ctxDealFile, "图片加载失败！", Toast.LENGTH_SHORT);
		}
	}

}