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
		 "图像识别，是利用关卡截图，以“半自动”的方式，识别出其 XSB，是关卡编辑的辅助功能。\n\n" +
		 "一般步骤：\n\n" +
		 "  1、定位：\n" +
		 "     确定图片中关卡“左上角”的位置，方法是：先适当缩放图片，再通过移动图片，使图中关卡的“左上角”对准屏幕可用区域的左上角，然后按顶行的“┏”按钮。之后，还可以利用“底行”的“上下左右”按钮进行微调。\n" +
		 "  2、确定“网格”大小：\n" +
		 "     利用顶行的“━”和“╋”按钮调整即可（底行的“颜色”按钮，可以改变每次的调整量）。之后，也可以利用“底行”的“上下左右”按钮进行整体移位微调。\n" +
		 "  3、识别：\n" +
		 "     先选中一个格子，然后，按“识别”按钮。\n" +
		 "  4、手动编辑：\n" +
		 "     利用底行左边的 XSB“元素”按钮，可以对识别区域进行手动编辑（调整）。单击“按钮”为选择或取消选择 XSB“元素”（以按钮的背景色指示是否选择），特别的，长按某些按钮，可以清除识别区中的对应元素，具体操作不做详述。\n" +
		 "  5、关于识别率：\n" +
		 "     可以按顶行的“设置”按钮，调整识别允许的“色差”和“误差像素数”，来改善识别效果。";

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
