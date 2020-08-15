package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class myAboutRecgo extends Activity {
	 private static TextView tv_help = null;

	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.help);
		
        //开启标题栏的返回键
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title));

		tv_help = (TextView)this.findViewById(R.id.tvHelp);
		
		String s =
		 "图像识别，是利用关卡截图，自动识别出其 XSB，属于创编关卡的辅助功能。\n\n" +
		 "一般步骤：\n\n" +
		 "  1、边框定位：\n" +
		 "     边框线上带有圆形“指示灯”，粉红色为“点亮”状态，表示该边线被选中，长按可以“点亮”（当没有选择底行的 XSB 元素时，点击也可以点击“点亮”）。底行的“上下左右”按钮对被选中的边线进行微调定位，也可以在“指示灯”附近双击进行微调；“长按”指示灯（此时，底行的“仓管员”元素会闪烁）可以直接拖动当前边线；边框线采用了“黑白”双色线条，对位的最佳效果是白线“压边”。\n" +
		 "  2、确定横向格子数：\n" +
		 "     当边框确定好后，就可以利用顶行的“增”、“减”按钮调整关卡水平方向的格子数。\n" +
		 "  3、识别：\n" +
		 "     当格子对准图片元素后，先点选底行的 XSB“元素”，然后点击图片中的相应格子，快手会自动启动“识别”。\n" +
		 "  4、识别设置：\n" +
		 "     当时识别效率差时，可以通过顶行的“度”按钮，调整识别“相似度”进行一定程度的改善。\n" +
		 "  5、编辑：\n" +
		 "     对于少量的关卡元素，也可以直接手动编辑而不做自动识别，可以通过点击顶行的“识别”或“编辑”菜单项进行模式切换。在“编辑”模式下，先点选底行的 XSB“元素”，然后点击“格子”即可。特别的，长按底行的 XSB“元素”，会有更多功能，具体不做详述。";

		tv_help.setText(s);
	 }
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
		 //标题栏返回键功能
		 case android.R.id.home:
			this.finish();
			return true;
		 default:
			return super.onOptionsItemSelected(item);
		 }
	}
		
	 @Override    
	 protected void onDestroy() { 
		 setContentView(R.layout.main);
		 super.onDestroy();
	 }

}
