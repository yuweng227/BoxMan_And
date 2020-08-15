package my.boxman;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class myAbout extends Activity {
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
		
		String s = "《推箱快手》\n\n" +
			   "  版本：9.93\n\n" +
			   "  作者：愉翁    QQ：252804303\n\n" +
			   "  策划：anian、愉翁\n\n" +
			   "  特别感谢：anian老师和杨超教授及众多热心箱友，anian老师在游戏设计的全过程中，" +
				"给出了大量的指导性意见，并进行了繁重的开发期测试，尤其在“割点”寻径及“穿越”寻径等算法方面，" +
				"更是得到了两位老师不遗余力的支持和帮助，许多算法都是直接移植于杨超教授的“SokoPlayer HTML5”。" +
				"另一方面，anian老师也为游戏提供了几乎全部的关卡集原始档案，还有少数关卡选自“http://sokoban.cn/”，" +
				"在游戏的公测期间，也收到了众多箱友的宝贵建议，这些建议包含了便捷操作、界面调整、功能增减以及错误修复等诸多方面。" +
				"可以说，离开了anian老师和杨超教授的倾心指导以及众位箱友热心支持，本游戏不会这么顺利地完成编写。" +
				"还有，从网上淘到“衣旧”网友编写的一个“ini文件工具类”，用在了系统配置的存取；" +
				"“闭口对角”死锁的检测代码和 XSB 的图像自动识别代码，移植于德国 Matthias Meger 大师的 JSoko；" +
				"以及 Kevin Weiner, FM Software 的 GIF 合成代码。" +
				"特此鸣谢！同时，也祝各位箱友都能很快的晋升到推箱子群中的快手之列！\n\n" +
			   "  2018年冬";

		tv_help.setText(s);
	 }
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
		 //标题栏返回键功能
		 case android.R.id.home:
//			Intent intent = new Intent(this, BoxMan.class);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(intent);
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
