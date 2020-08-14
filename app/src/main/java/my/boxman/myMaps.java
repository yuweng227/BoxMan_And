package my.boxman;

import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

//此类仅定义公用数据
public class myMaps {

	static boolean isMacroDebug = false;  //单步宏
	static boolean isSelect = false;  //浏览界面是否处于多选模式
	static boolean isRecording = false;  //录制模式
	static int m_nRecording_Bggin;  //正推录制起始点
	static int m_nRecording_Bggin2;  //逆推录制起始点
	static int isSkin_200 = 0;  //皮肤顶部是否减去200
	static boolean isSimpleSkin = false;  //是否简单皮肤350
	static boolean isHengping = false;  //是否使用横屏皮肤

	//公用变量
	static Context ctxDealFile;

	static int m_nWinWidth;  //屏幕尺寸
	static int m_nWinHeight;

	static int m_nMaxSteps = 500000;  //保存答案到 DB 步数限制的
	static int m_nMaxRow = 100;  //地图最大尺寸
	static int m_nMaxCol = 100;
	static int m_nTrun = 0;   //地图旋转角度

	static String sRoot;    //路径根
	static String sPath;    //根目录
	static String sFile;    //关卡集文档名
	static String[] myPathList = {  //关卡截图根目录列表
			"",                         // 默认位置
			"/tencent/qq_images/",      // QQ 图片接收位置
			"/",                        // 自定义 1
			"/",                        // 自定义 2
			"/"                         // 自定义 3
	};

	static String[] sAction;  //动作寄存器缓存
	static boolean m_ActionIsRedy = false;  //状态是否准备就绪
	static boolean m_ActionIsPos = true;  //是否从关卡的当前点执行动作
	static boolean m_ActionIsTrun = true;  //是否按关卡的当前旋转状态执行动作

	static ArrayList<Integer> mArray = new ArrayList<Integer>();   //关卡浏览多选状态模式删除、迁移使用

	static boolean m_StateIsRedy = false;  //状态是否准备就绪
	static ans_Node m_State;  //读入的状态 = new ans_Node()
	static ArrayList<state_Node> mState1 = new ArrayList<state_Node>();  //状态
	static ArrayList<state_Node> mState2 = new ArrayList<state_Node>();  //答案
	static ArrayList<set_Node> mSets0 = new ArrayList<set_Node>();  //关卡集列表 0 -- 入门
	static ArrayList<set_Node> mSets1 = new ArrayList<set_Node>();  //关卡集列表 1 -- 进阶
	static ArrayList<set_Node> mSets2 = new ArrayList<set_Node>();  //关卡集列表 2 -- 塑形
	static ArrayList<set_Node> mSets3 = new ArrayList<set_Node>();  //关卡集列表 3 -- 扩展
	static ArrayList<String> mFile_List = new ArrayList<String>();  //关卡文档
	static ArrayList<String> mFile_List1 = new ArrayList<String>();  //皮肤文档
	static ArrayList<String> mFile_List2 = new ArrayList<String>();  //背景文档
	static ArrayList<mapNode> m_lstMaps = new ArrayList<mapNode>();  //地图列表
	static mapNode curMap, oldMap;  //当前关卡，查找相似关卡时的源
	static int curMapNum;  //编辑关卡时，记录当前状态

	static ListView m_setName;  //查询用关卡集选择框 -- 关卡集选项

	static boolean m_bBianhao = false;  //自动箱子编号
	static boolean m_bBiaochi = false;  //标尺
	static boolean curJi = false;  //是否选择了关卡集，开始选择关卡
	static boolean iskinChange = false;  //皮肤是否有变化
	static boolean isSaveBlock = false;  //关卡编辑中，保存过“区块”
	static boolean isXSB = true;  //是否导入关卡
	static boolean isLurd = false;  //是否导入答案
	static boolean isComment = false;  //是否导入答案的备注
	static int m_Code = 0;  //文档导入关卡的字符编码格式：0 - 自动，1 - GBK，2 - utf-8
	static int[] m_Nums = {0, 0, 0, 0};  //用于统计导入答案数、因重复未能导入数；关卡数、无效关卡数

	static byte[][] Trun8 = { //关卡8方位旋转之n转换算数组
			{0, 3, 2, 1, 4, 5, 6, 7},
			{1, 0, 3, 2, 7, 4, 5, 6},
			{2, 1, 0, 3, 6, 7, 4, 5},
			{3, 2, 1, 0, 5, 6, 7, 4},
			{4, 7, 6, 5, 0, 1, 2, 3},
			{5, 4, 7, 6, 3, 0, 1, 2},
			{6, 5, 4, 7, 2, 3, 0, 1},
			{7, 6, 5, 4, 1, 2, 3, 0}};

	static char[][] DIR = {  //答案8方位旋转之n转换算数组
			{'l', 'u', 'r', 'd', 'L', 'U', 'R', 'D'},
			{'u', 'r', 'd', 'l', 'U', 'R', 'D', 'L'},
			{'r', 'd', 'l', 'u', 'R', 'D', 'L', 'U'},
			{'d', 'l', 'u', 'r', 'D', 'L', 'U', 'R'},
			{'r', 'u', 'l', 'd', 'R', 'U', 'L', 'D'},
			{'d', 'r', 'u', 'l', 'D', 'R', 'U', 'L'},
			{'l', 'd', 'r', 'u', 'L', 'D', 'R', 'U'},
			{'u', 'l', 'd', 'r', 'U', 'L', 'D', 'R'}};

	static String J_Title;  //关卡集"说明"
	static String J_Author;  //关卡集"作者"
	static String J_Comment;  //关卡集"注释"

	static long m_Set_id;  //增补关卡之关卡集 id
	static int[] m_Sets;  //系统参数设置(0-关卡集组，1-关卡集， 2-关卡预览图标题，3-长按目标点提示关联网点及网口， 4-关卡背景色， 5-仓管员图片方向，6-瞬移，7-轮转方位，8-显示可达提示，9-标尺不随关卡旋转，10-移动速度，11-死锁提示，12-标识重复关卡，13-即景模式，14-仓管员移动方向，15-使用音量键选择关卡，16-显示系统虚拟按键，17-是否允许穿越，18-unDO、ReDo，19-是否采用YASC绘制习惯，20-禁用全屏，21-编辑关卡图时，地图中的标尺字体颜色，22-编辑关卡图时，哪些元素携带标尺，23-单步进退，25-，26-查找相似关卡的默认相似度，27-仓管员转向动画，28-演示时仅推动，29-自动爬阶梯，30-导出答案的注释信息，31-导入关卡为一个时，自动打开，32-逆推时使用正推的目标点，33-每行浏览的图标数，34-每行浏览的图标默认数，35-布局中的图标默认高度，36-识别，37-打开无解关卡时，自动加载最新状态，38-区分奇偶地板格，39-偶位地板格明暗度，40-奇位地板格明暗度)
	static String mMatchNo = "";  //第 n 期比赛
	static String mMatchDate1 = "";  //比赛开始日期
	static String mMatchDate2 = "";  //比赛结束日期
	static String nickname = "";  //提交答案 - nickname
	static String country = "CN";  //提交答案 - country
	static String email = "";  //提交答案 - email
	static String bk_Pic = "使用背景色";  //背景图片文档名
	static String skin_File = "默认皮肤";  //皮肤文档名
	static Bitmap markGif1 = null; //默认的在线推箱子 logo 水印
	static Bitmap markGif2 = null; //自定义水印
	static Bitmap skinGif = null; //GIF皮肤
	static Bitmap skinBit = null; //皮肤
	static Bitmap bkPict = null; //背景图片
	static Bitmap edPict = null;  //关卡编辑界面的背景图片
	static int edPictLeft, edPictTop, edPictRight, edPictBottom;  // 关卡编辑界面的背景图片的四至
	static int edRows, edCols;                                    // 识别时的关卡尺寸
	static android.graphics.Bitmap.Config cfg = Bitmap.Config.ARGB_4444;
	static Paint myPaint = new Paint();
	static Resources res;  //资源

	//加载皮肤
	static void loadSkins() {
		if (myMaps.skinBit != null) myMaps.skinBit.recycle();
		myMaps.skinBit = null;
		try{
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inPreferredConfig = myMaps.cfg;
			opts.inJustDecodeBounds = true;  //仅提取图片宽高
			BitmapFactory.decodeFile(myMaps.sRoot+myMaps.sPath + "皮肤/" + myMaps.skin_File, opts);
			//仅支持 200 x 350 和 200 x 400尺寸皮肤
			isSimpleSkin = false;  //标准皮肤
			isSkin_200 = 0;  //标准皮肤150
			isHengping = false;  //不使用横屏皮肤
			if (opts.outWidth == 200 && opts.outHeight == 150) isSkin_200 = 200;  //皮肤顶部减去200，此为最精简的皮肤
			else if (opts.outWidth == 200 && opts.outHeight == 350) isSimpleSkin = true;  //简单皮肤
			else if (opts.outWidth != 200 || opts.outHeight != 400) throw new Exception();
			opts.inJustDecodeBounds = false;   //提取图片
			myMaps.skinBit = BitmapFactory.decodeFile(myMaps.sRoot+myMaps.sPath + "皮肤/" + myMaps.skin_File, opts);
		} catch (Exception e) {
			isSimpleSkin = false;  //标准皮肤
			isSkin_200 = 0;  //标准皮肤150
			isHengping = false;  //不使用横屏皮肤
			if (!myMaps.skin_File.equals("默认皮肤"))
				MyToast.showToast(myMaps.ctxDealFile, "自定义皮肤无效！", Toast.LENGTH_SHORT);
			InputStream fis = myMaps.res.openRawResource(R.raw.defskin);
			myMaps.skinBit = BitmapFactory.decodeStream(fis);
		}
		System.gc();
	}

	//加载背景
	static void loadBKPic() {
		if (myMaps.bkPict != null) myMaps.bkPict.recycle();
		myMaps.bkPict = null;
		if (myMaps.bk_Pic == null || myMaps.bk_Pic.length() <= 0 || myMaps.bk_Pic.equals("使用背景色")) {
			myMaps.bk_Pic = "使用背景色";
			return;
		}

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
		try{
			opts.inJustDecodeBounds = true;  //仅提取图片宽高
			BitmapFactory.decodeFile(myMaps.sRoot+myMaps.sPath + "背景/" + myMaps.bk_Pic, opts);


			if (opts.outWidth > myMaps.m_nWinWidth || opts.outHeight > myMaps.m_nWinHeight) {
				int k1 = opts.outWidth / myMaps.m_nWinWidth;
				int k2 = opts.outHeight / myMaps.m_nWinHeight;
				opts.inSampleSize = (k1 > k2) ? k1 : k2;
			}
			opts.inJustDecodeBounds = false;   //提取图片
			opts.inDither=false;    /*不进行图片抖动处理*/
			opts.inPreferredConfig=null;  /*设置让解码器以最佳方式解码*/
			/* 下面两个字段需要组合使用 */
			opts.inPurgeable = true;
			opts.inInputShareable = true;
			myMaps.bkPict = BitmapFactory.decodeFile(myMaps.sRoot+myMaps.sPath + "背景/" + myMaps.bk_Pic, opts);
		} catch (Exception e) {
			if (myMaps.bkPict != null && !myMaps.bkPict.isRecycled()) {
				myMaps.bkPict.recycle();
				myMaps.bkPict = null;
			}
			System.gc();
			MyToast.showToast(myMaps.ctxDealFile, "无效的背景图片！", Toast.LENGTH_SHORT);
		}
	}
	//读入剪切板
	static String loadClipper() {
		try {
			android.content.ClipboardManager cmb = (android.content.ClipboardManager) myMaps.ctxDealFile.getSystemService(Context.CLIPBOARD_SERVICE);
			if (cmb.getPrimaryClipDescription().hasMimeType(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN) ||
					cmb.getPrimaryClipDescription().hasMimeType(android.content.ClipDescription.MIMETYPE_TEXT_HTML)) {
				android.content.ClipData cd = cmb.getPrimaryClip();
				android.content.ClipData.Item item = cd.getItemAt(0);
				return item.getText().toString();
			} else return "";
		} catch(Exception e) {
			return "";
		}
	}

	//装填关卡集列表项
	static List<String> getData(ArrayList<Long> mySets) {
		List<String> list = new ArrayList<String>();

		for (int k = 0; k < myMaps.mSets0.size(); k++) {
			list.add(myMaps.mSets0.get(k).title);
			mySets.add(myMaps.mSets0.get(k).id);
		}

		for (int k = 0; k < myMaps.mSets1.size(); k++) {
			list.add(myMaps.mSets1.get(k).title);
			mySets.add(myMaps.mSets1.get(k).id);
		}

		for (int k = 0; k < myMaps.mSets2.size(); k++) {
			list.add(myMaps.mSets2.get(k).title);
			mySets.add(myMaps.mSets2.get(k).id);
		}

		for (int k = 0; k < myMaps.mSets3.size(); k++) {
			list.add(myMaps.mSets3.get(k).title);
			mySets.add(myMaps.mSets3.get(k).id);
		}

		return list;
	}


	//根据关卡集 id，得到关卡集名称
	static String getSetTitle(long id) {

		for (int k = 0; k < myMaps.mSets0.size(); k++) {
			if (myMaps.mSets0.get(k).id == id)  return myMaps.mSets0.get(k).title;
		}

		for (int k = 0; k < myMaps.mSets1.size(); k++) {
			if (myMaps.mSets1.get(k).id == id)  return myMaps.mSets1.get(k).title;
		}

		for (int k = 0; k < myMaps.mSets2.size(); k++) {
			if (myMaps.mSets2.get(k).id == id)  return myMaps.mSets2.get(k).title;
		}

		for (int k = 0; k < myMaps.mSets3.size(); k++) {
			if (myMaps.mSets3.get(k).id == id)  return myMaps.mSets3.get(k).title;
		}

		return "";
	}

	//根据关卡集 id，检查是否为扩展关卡集内的关卡
	static boolean isExSet(long id) {
		for (int k = 0; k < myMaps.mSets3.size(); k++) {
			if (myMaps.mSets3.get(k).id == id)  return true;
		}

		return false;
	}

	//送入剪切板
	static void saveClipper(String str) {
		android.content.ClipboardManager cmb = (android.content.ClipboardManager) myMaps.ctxDealFile.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData mClipData = ClipData.newPlainText(null, str);
		cmb.setPrimaryClip(mClipData);
	}

	//通过转换，增强宏功能对全角字符的支持
	static String qj2bj(String src) {
		if (src == null) return src;

		StringBuilder buf = new StringBuilder(src.length());
		char[] ca = src.toCharArray();
		for (int i = 0; i < src.length(); i++) {
			if (ca[i] == '｛') {  //花括号--动作、寄存器
				buf.append('{');
			} else if (ca[i] == '｝') {
				buf.append('}');
			} else if (ca[i] == '（') {  //圆括号--单步后退
				buf.append('(');
			} else if (ca[i] == '）') {
				buf.append(')');
			} else if (ca[i] == '【') {  //方括号--坐标
				buf.append('[');
			} else if (ca[i] == '】') {
				buf.append(']');
			} else if (ca[i] == '〖') {
				buf.append('[');
			} else if (ca[i] == '〗') {
				buf.append(']');
			} else if (ca[i] == '﹝') {
				buf.append('[');
			} else if (ca[i] == '﹞') {
				buf.append(']');
			} else if (ca[i] == '〔') {
				buf.append('[');
			} else if (ca[i] == '〕') {
				buf.append(']');
			} else if (ca[i] == '［') {
				buf.append('[');
			} else if (ca[i] == '］') {
				buf.append(']');
			} else if (ca[i] == '〈') {  //尖括号--行内块
				buf.append('<');
			} else if (ca[i] == '〉') {
				buf.append('>');
			} else if (ca[i] == '＜') {
				buf.append('<');
			} else if (ca[i] == '＞') {
				buf.append('>');
			} else if (ca[i] == '‹') {
				buf.append('<');
			} else if (ca[i] == '›') {
				buf.append('>');
			} else if (ca[i] == '《') {
				buf.append('<');
			} else if (ca[i] == '》') {
				buf.append('>');
			} else if (ca[i] == '«') {
				buf.append('<');
			} else if (ca[i] == '»') {
				buf.append('>');
			} else if (ca[i] == '，') {  //逗号--坐标
				buf.append(',');
			} else if (ca[i] == '；') {  //分号--注释、语句分割
				buf.append(';');
			} else if (ca[i] == '：') {  //冒号--条件成立
				buf.append(':');
			} else if (ca[i] == '∶') {
				buf.append(':');
			} else if (ca[i] == '＝') {  //等号--关卡复位到初态
				buf.append('=');
			} else if (ca[i] == '＋') {  //加号--坐标、人在目标点
				buf.append('+');
			} else if (ca[i] == '－') {  //减号--地板、坐标
				buf.append('-');
			} else if (ca[i] == '－') {
				buf.append('-');
			} else if (ca[i] == '～') {  //波浪线--忽略移位或动作
				buf.append('~');
			} else if (ca[i] == '＾') {  //尖号--调试中断符号
				buf.append('^');
			} else if (ca[i] == '∧') {
				buf.append('^');
			} else if (ca[i] == '＠') {  //@--坐标、人
				buf.append('@');
			} else if (ca[i] == '％') {  //百分号--跳转符号，相当于goto
				buf.append('%');
			} else if (ca[i] == '／') {  //正斜线--条件不成立，即：相当于“否则”、else
				buf.append('/');
			} else if (ca[i] == '！') {  //感叹号--暂时未用
				buf.append('!');
			} else if (ca[i] == '＆') {  //&--暂时未用
				buf.append('&');
			} else if (ca[i] == '＃') {  //#号--墙
				buf.append('#');
			} else if (ca[i] == '＿') {  //下划线--墙外
				buf.append('_');
			} else if (ca[i] == '．') {  //圆点--目标点
				buf.append('.');
			} else if (ca[i] == '·') {
				buf.append('.');
			} else if (ca[i] == '。') {
				buf.append('.');
			} else if (ca[i] == '＄') {  //$--箱子
				buf.append('$');
			} else if (ca[i] == '￥') {
				buf.append('$');
			} else if (ca[i] == '＊') {  //星号--箱子在目标点
				buf.append('*');
			} else if (ca[i] == '※') {
				buf.append('*');
			} else if (ca[i] == '×') {
				buf.append('*');
			} else if (ca[i] == '？') {  //？-条件符号
				buf.append('?');
			} else if (ca[i] == '　') {  //全角空格
				buf.append(' ');
			} else if (ca[i] >= 65281 && ca[i] <= 65374) {  //处理没想到的字符
				buf.append((char) (ca[i] - 65248));
			} else {  //不处理其它字符
				buf.append(ca[i]);
			}
		}
		return buf.toString();
	}

	//是否为“宏”指令，此时，全角字符已经做过转换
	static boolean isMacro(String[] str) {
		String regex = "[=:~;@%.*#$&!<>\\{\\}\\?\\/\\(\\)\\+\\^]";  //＝：∶～；＠％．。·＊※×＃＄￥＆！【】〖〗﹝﹞〔〕［］〈〉＜＞‹›《》«»｛｝？／（）＋＾∧
		Matcher m;
		for (int k = 0; k < str.length; k++) {
			m = Pattern.compile(regex).matcher(str[k]);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	//从文档读入“宏”
	static String readMacroFile(String fn) {
		String my_Name;
		try {
			my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("宏/").append(fn).toString();

			File file = new File(my_Name);
//			InputStreamReader read = new InputStreamReader(new FileInputStream(file), myMaps.getFileEncode(file));  //考虑到编码格式
			InputStreamReader read = new InputStreamReader(new FileInputStream(file), myMaps.getTxtEncode(new FileInputStream(file)));  //考虑到编码格式

			BufferedReader bufferedReader = new BufferedReader(read);

			StringBuilder str = new StringBuilder();
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				str.append(line).append('\n');
			}

			return str.toString();
		} catch (Exception e) {
			MyToast.showToast(ctxDealFile, "无效的宏文档！", Toast.LENGTH_SHORT);
		}
		return "";
	}

	//取得"宏/"文件夹下的文档列表，最新文档列在最前面
	static void mMacroList() {
		File targetDir = new File(myMaps.sRoot + myMaps.sPath + "宏/");
		myMaps.mFile_List.clear();
		if (!targetDir.exists()) targetDir.mkdirs();  //创建"宏/"文件夹
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

	//取得截图列表
	static void edPicList(String fn) {
		File targetDir = new File(fn);
		myMaps.mFile_List.clear();
		if (targetDir.exists()) {
			File[] fs = targetDir.listFiles();
			Arrays.sort(fs, new Comparator< File>(){            // 截图文档列表按创建时间排序
				public int compare(File f1, File f2) {
					long diff = f1.lastModified() - f2.lastModified();
					if (diff > 0)
						return -1;
					else if (diff == 0)
						return 0;
					else
						return 1;
				}
				public boolean equals(Object obj) {
					return true;
				}
			});
			for (int i = 0; i < fs.length; i++) {
				int dot = fs[i].getName().lastIndexOf('.');
				if ((dot > -1) && (dot < (fs[i].length()))) {
					String prefix = fs[i].getName().substring(fs[i].getName().lastIndexOf(".") + 1);
					if (prefix.equalsIgnoreCase("jpg") || prefix.equalsIgnoreCase("bmp") || prefix.equalsIgnoreCase("png"))
						myMaps.mFile_List.add(fs[i].getName());
				}
			}
//			String[] filelist = targetDir.list();
//			Arrays.sort(filelist, String.CASE_INSENSITIVE_ORDER);
//			for (int i = 0; i < filelist.length; i++) {
//				int dot = filelist[i].lastIndexOf('.');
//				if ((dot > -1) && (dot < (filelist[i].length()))) {
//					String prefix = filelist[i].substring(filelist[i].lastIndexOf(".") + 1);
//					if (prefix.equalsIgnoreCase("jpg") || prefix.equalsIgnoreCase("bmp") || prefix.equalsIgnoreCase("png"))
//						myMaps.mFile_List.add(filelist[i]);
//				}
//			}
		}
	}

	//"导入/"下的文档列表
	static void newSetList(){
		File targetDir = new File(sRoot+sPath + "导入/");
		mFile_List.clear();
		if (!targetDir.exists()) targetDir.mkdirs();  //创建"导入/"文件夹
		else {
			String[] filelist = targetDir.list();
			Arrays.sort(filelist, String.CASE_INSENSITIVE_ORDER);
			for (int i = 0; i < filelist.length; i++) {
				int dot = filelist[i].lastIndexOf('.');
				if ((dot >-1) && (dot < (filelist[i].length()))){
					String prefix=filelist[i].substring(filelist[i].lastIndexOf(".")+1);
					if (prefix.equalsIgnoreCase("xsb") ||
							prefix.equalsIgnoreCase("txt") ||
							prefix.equalsIgnoreCase("sok") ||
							prefix.equalsIgnoreCase("txz"))
						mFile_List.add(filelist[i]);
				}
			}
		}
	}

//	//快手全部关卡集列表
//	static void allSetList(){
//		mFile_List.clear();
//		mArray2.clear();
//
//		for (int i = 0; i < myMaps.mSets0.size(); i++) {
//			mFile_List.add(myMaps.mSets0.get(i).title);
//			myMaps.mArray2.add(myMaps.mSets0.get(i).id);
//		}
//		for (int i = 0; i < myMaps.mSets1.size(); i++) {
//			mFile_List.add(myMaps.mSets1.get(i).title);
//			myMaps.mArray2.add(myMaps.mSets1.get(i).id);
//		}
//		for (int i = 0; i < myMaps.mSets2.size(); i++) {
//			mFile_List.add(myMaps.mSets2.get(i).title);
//			myMaps.mArray2.add(myMaps.mSets2.get(i).id);
//		}
//		for (int i = 0; i < myMaps.mSets3.size(); i++) {
//			mFile_List.add(myMaps.mSets3.get(i).title);
//			myMaps.mArray2.add(myMaps.mSets3.get(i).id);
//		}
//		mFile_List.add("答案库中的自由关卡");
//	}

	//解析字符串中的XSB
	static boolean loadXSB(String str) {
		String[] Arr;
		try {
			Arr = str.split("\r\n|\n\r|\n|\r|\\|");

			String[] new_map = new String[m_nMaxRow];
			int row = 0;
			int col = 0;

			boolean flg = false;
			int p = 0;
			for (int k = 0; k < Arr.length && k < m_nMaxRow; k++) {
				if (isXSB(Arr[k])) {
					flg = true;
					new_map[row] = Arr[k];
					if (col < Arr[k].length()) col = Arr[k].length();  //最大宽度
					if (col > m_nMaxCol) col = m_nMaxCol;  //最大宽度
					row++;
				} else {  //可以跳过开头的无效行
					if (flg) {
						p = k;
						break;  //匹配 XSB 行，XSB 行必须连续
					}
				}
			}

			boolean flg2 = false;
			StringBuilder g_Title = new StringBuilder();  //标题
			StringBuilder g_Author = new StringBuilder();  //作者
			StringBuilder g_Comment = new StringBuilder();  //"注释"
			for (int k = p; k < Arr.length && k < m_nMaxRow; k++) {
				if (Arr[k].trim().toLowerCase(Locale.getDefault()).startsWith("title:")) {  //匹配 Title，标题
					g_Title.append(Arr[k].substring(Arr[k].indexOf(":")+1).trim());
				} else if (Arr[k].trim().toLowerCase(Locale.getDefault()).startsWith("author:")) {  //匹配 Author，作者
					g_Author.append(Arr[k].substring(Arr[k].indexOf(":")+1).trim());
				} else if (Arr[k].trim().toLowerCase(Locale.getDefault()).startsWith("comment:")) {  //匹配 Comment，"注释"块开始
					if (!Arr[k].substring(Arr[k].indexOf(":")+1).trim().equals(""))
						g_Comment.append(Arr[k].substring(Arr[k].indexOf(":")+1).trim()).append('\n');
					flg2 = true;
				} else if (Arr[k].trim().toLowerCase(Locale.getDefault()).startsWith("comment-end:")) {
					flg2 = false;
				} else if (Arr[k].trim().toLowerCase(Locale.getDefault()).startsWith("comment_end:")) {
					flg2 = false;
				} else if (Arr[k].trim().toLowerCase(Locale.getDefault()).startsWith("solution:")) {
					flg2 = false;
				} else {
					if (flg2) g_Comment.append(Arr[k]).append('\n');
				}
			}

			curMap = new mapNode(row, col, new_map, g_Title.toString(), g_Author.toString(), g_Comment.toString());

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	//解析字符串中的第一个动作串（Lurd）并返回，flg = -1：仅逆推 lurd；flg = 1：仅正推 lurd；flg = 0：正逆推 lurd
	static String loadLURD(String str, int flg) {
		int index = str.toLowerCase(Locale.getDefault()).indexOf("solution");
		if (index >= 0) {
			str = str.substring(index + 8);
			str = str.substring(str.indexOf(")") + 1);
			str = str.substring(str.indexOf(":") + 1);
		} else {
			index = str.toLowerCase(Locale.getDefault()).indexOf("comment-end:");
			if (index >= 0) {
				str = str.substring(index + 12);
			} else {
				index = str.toLowerCase(Locale.getDefault()).indexOf("comment_end:");
				if (index >= 0) {
					str = str.substring(index + 12);
				} else {
					index = str.toLowerCase(Locale.getDefault()).indexOf("comment:");
					if (index >= 0) {
						return "";
					} else {
						index = str.toLowerCase(Locale.getDefault()).indexOf("author:");
						int index2 = str.toLowerCase(Locale.getDefault()).indexOf("title:");
						index = (index > index2 ? index : index2);
						if (index >= 0) {
							str = str.substring(index);
							index2 = str.toLowerCase(Locale.getDefault()).indexOf("\n");
							if (index2 >= 0) {
								str = str.substring(index2);
							} else {
								index2 = str.toLowerCase(Locale.getDefault()).indexOf("\r");
								if (index2 >= 0) {
									str = str.substring(index2);
								}
							}
						}
					}
				}
			}
		}
		//返回“串”中的第一个动作段
		try {
			index = 0;
			while ("lurdLURD[".indexOf(str.charAt(index)) < 0) index++;
			int index2 = index;
			try {
				while ("lurdLURD[0123456789,-] \t\n\r".indexOf(str.charAt(index2)) >= 0) index2++;
//				index2++;
			} catch (Exception e) {
				index2 = str.length();
			}
			str = str.substring(index, index2);

			index = str.indexOf("[");
			if (flg == 0) return str;  //返回正、逆推动作串
			else if (flg < 0) {  //仅返回逆推动作串
				if (index < 0) return str;  //没找到，则把全部动作视为逆推动作
				else return str.substring(index);
			} else {  //仅返回正推动作串
				if (index < 0) return str;  //没找到，则把全部动作视为正推动作
				else return str.substring(0, index);
			}
		} catch (Exception e) {
			return "";
		}
	}

	//解析创编中的关卡
	static void read_DirBuilder(){

		myMaps.m_lstMaps.clear();  //关卡集链表

		myMaps.m_Sets[0] = 3;
		myMaps.sFile = "创编关卡";
		myMaps.J_Title = "创编关卡";
		myMaps.J_Author = "";
		myMaps.J_Comment = "";
		myMaps.m_Set_id = -1;

		File myDir = new File(myMaps.sRoot+myMaps.sPath + "创编关卡/");  //创编的关卡文件夹
		if (!myDir.exists()) myDir.mkdirs();

		//取得"创编关卡"文件夹下的全部关卡
		String[] filelist = myDir.list();
		Arrays.sort(filelist, String.CASE_INSENSITIVE_ORDER);

		InputStream fin;
		String my_Name;
		for (int i = 0; i < filelist.length; i++) {
			int dot = filelist[i].lastIndexOf('.');
			if ((dot > -1) && (dot < (filelist[i].length()))){
				String prefix = filelist[i].substring(filelist[i].lastIndexOf(".")+1);
				if (prefix.equalsIgnoreCase("xsb") ||
						prefix.equalsIgnoreCase("txt") ||
						prefix.equalsIgnoreCase("sok") ||
						prefix.equalsIgnoreCase("txz")) {

					//依次读入各文档中的关卡
					try{
						my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("创编关卡/").append(filelist[i]).toString();
						fin = new FileInputStream(my_Name);

						int len = fin.available();
						byte[] Buf = new byte[len];
						fin.read(Buf);
						fin.close();

						if (myMaps.loadXSB(new String(Buf))) {
							myMaps.curMap.fileName = filelist[i];  //关卡文档名
							myMaps.m_lstMaps.add(myMaps.curMap);  //加入列表
						}
					} catch(Exception e) {
//						myMaps.curJi = false;
					}
				}
			}
		}
	}

	//判断是否为有效的 XSB 行，目前效率最高的判断方法
	static boolean isXSB(String str){
		if (str == null || str.matches("\\s*")) return false;  //排除空行的可能 || str.trim().isEmpty()

		int n = str.length();
		for(int k = 0; k < n; k++) { //判断是否全部由有效字符组成
			if (" _#-.$*@+".indexOf(str.charAt(k)) < 0){
				return false;
			}
		}
		return true;
	}

	//检查字符串是否为动作串（Lurd）
	static boolean isLURD(String str) {

		if (str == null || str.trim().isEmpty()) return false;  //排除空串的可能

		int n = str.length();
		for(int k = 0; k < n; k++) { //判断是否全部由有效字符组成
			if ("lurdLURD \t\n\r".indexOf(str.charAt(k)) < 0){
				return false;
			}
		}
		return true;
	}

	//检查字符串是否为逆推动作串（Lurd）
	static boolean isLURD2(String str) {

		if (str == null) return false;  //排除空串的可能 || str.trim().isEmpty()

		int n, m = str.indexOf(';');
		if (m < 0) {
			n = str.length();
		} else{
			n = m;
		}
		for(int k = 0; k < n; k++) { //判断是否全部由有效字符组成
			if ("lurdLURD[0123456789,-] \t\n\r".indexOf(str.charAt(k)) < 0){
				return false;
			}
		}
		return true;
	}

	//取得关卡现场
	static String getLocale(char[][] tm_cArray) {
		StringBuilder s = new StringBuilder();

		//关卡正推现场XSB
		for (int i = 0; i < tm_cArray.length; i++) {
			for (int j = 0; j < tm_cArray[0].length; j++) {
				s.append(tm_cArray[i][j]);
			}
			if (i < tm_cArray.length-1) s.append('\n');
		}
		return s.toString();
	}

//	//获取文件编码格式.
//	public static String getFileEncode(File fileName) {
//	    if (myMaps.isUTF8) return "UTF-8";
//
//		String charsetName = "GBK";
//		try {
//			BufferedInputStream bin = new BufferedInputStream(new FileInputStream(fileName));
//			int p = (bin.read() << 8) + bin.read();
//			//其中的 0xefbb、0xfffe、0xfeff、0x5c75这些都是这个文件的前面两个字节的16进制数
//			switch (p) {
//				case 0xefbb:
//					charsetName = "UTF-8";
//					break;
//				case 0xfffe:
//					charsetName = "Unicode";
//					break;
//				case 0xfeff:
//					charsetName = "UTF-16BE";
//					break;
//				case 0x5c75:
//					charsetName = "ANSI|ASCII" ;
//					break ;
//				default:
//					charsetName = "GBK";
//			}
//		} catch ( Exception e ) { }
//
//		return charsetName;
//	}

	static String getTxtEncode(FileInputStream in) {

		if (myMaps.m_Code == 2) return "UTF-8";
		else if (myMaps.m_Code == 1) return "GBK";

		String charsetName = Charset.defaultCharset().name();

		UnicodeInputStream uin = new UnicodeInputStream(in, charsetName);

		try {
			if("UTF-8".equals(uin.getEncoding())){
				uin.close();
				return "UTF-8";
			}
			uin.close();

			byte[] head = new byte[3];
			in.read(head);
			if (head[0] == -1 && head[1] == -2 )
				charsetName = "UTF-16";
			if (head[0] == -2 && head[1] == -1 )
				charsetName = "Unicode";
			//带BOM
			if(head[0]==-17 && head[1]==-69 && head[2] ==-65)
				charsetName = "UTF-8";
			if("Unicode".equals(charsetName))
				charsetName = "UTF-16";
		} catch ( Exception e ) { }

		return charsetName;
	}

	//计算关卡或答案的CRC32
	static long getCRC32(String str) {
		if(str==null||str.length()==0) return 0;
		try {
			CRC32 crc32 = new CRC32();
			crc32.update(str.getBytes("UTF-8"));
			return crc32.getValue();
		} catch (Exception e) {
			return 0;
		}
	}

	//生成不重复的新关卡集名称
	static String getNewSetName() {
		String name = "新建关卡集_";
		int n = 1, k, len = mSets3.size();

		while (true) {
			// 检查新关卡集的名称有没有重复
			k = 0;
			for (; k < len; k++) {
				if (mSets3.get(k).title.equals(name + (n))) {
					break;
				}
			}
			// 直到新关卡集的名称有没有重复
			if (k == len) {
				break;
			}
			n++;
		}

		return name+n;
	}

	//根据答案的旋转数计算出关卡某旋转的答案
	static String getANS(String str, int ans_num, int level_num) {
		if (ans_num == level_num) return str;

		StringBuilder ans = new StringBuilder();

		int len = str.length();
		for (int k = 0; k < len; k++) {
			switch (str.charAt(k)) {
				case 'l':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 0 ]);
					break;
				case 'u':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 1 ]);
					break;
				case 'r':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 2 ]);
					break;
				case 'd':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 3 ]);
					break;
				case 'L':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 4 ]);
					break;
				case 'U':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 5 ]);
					break;
				case 'R':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 6 ]);
					break;
				case 'D':
					ans.append(DIR[ Trun8[ans_num][level_num] ][ 7 ]);
			}
		}
		return ans.toString();
	}

	//列表框 Adapter
	public static class MyAdapter extends BaseAdapter {
		private Context context;
		private ArrayList<String> data;

		public MyAdapter(Context context,ArrayList<String> data){
			this.context = context;
			this.data = data;
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
			tv.setTextSize(16);
			tv.setText(data.get(position));
			//判断position位置是否被选中，改变颜色
			if(m_setName.isItemChecked(position)) {
				tv.setBackgroundColor(Color.parseColor("#0088aa"));
			} else {
				tv.setBackgroundColor(Color.parseColor("#363636"));
			}
			return tv;
		}
	}


}

//查重用关卡节点
class mapNode2{
	int Num;  //关卡序号
	String SetName;  //关卡集名称
	String Title;  //关卡标题
	String Author;  //关卡作者
	String Map;   //地图
	long Level_id;  //关卡 id
	long P_id;  //关卡所在关卡集的 id
}

//关卡节点
class mapNode{
	int Num;  //关卡序号
	int Rows;   //地图尺寸
	int Cols;
	int Trun;
	long P_id;  //关卡所属的关卡集的 id
	long Level_id;  //关卡在数据库中的 id
	boolean Solved;  //是否已解
	boolean Lock;  //是否加锁图标
	boolean Select;  //是否被选中
	String fileName;  //编辑关卡时的关卡文档名
	String Author;  //作者
	String Title;  //标题
	String Comment;  //描述
	String Map;   //关卡
	String Map0;   //标准化 XSB
	int L_CRC_Num;  //计算关卡第几转（最小CRC在以本关卡CRC为开头的序列中的序号）
	long key;  //关卡 CRC

	//从 DB 中加载时调用
	public mapNode(int n, long Pid, long id, int fs, String m, String t, String a, String c, long k, int ln, String thinXSB, int lock){
		Num = n;  //关卡序号
		Trun = 0;
		Lock = (lock == 1);
		P_id = Pid;
		Level_id = id;
		fileName = null;
		Author = a;
		Title = t;
		Comment = c;
		Solved = (fs == 1);
		Select = false;
		Map = m;
		L_CRC_Num = ln;
		Map0 = thinXSB;
		String[] Arr;
		try{
			Arr = Map.split("\r\n|\n\r|\n|\r|\\|");
			Rows = Arr.length;  //地图高
			Cols = Arr[0].length();  //地图宽
			key = k;
		} catch (Exception e) {  //无效关卡
			Map = "--";
			Rows = 1;
			Cols = 2;
			key = 0;
			Map0 = "";
			L_CRC_Num = -1;
		}
	}

	//编辑的新建和导入调用
	public mapNode(int rs, int cs, String[] m, String t, String a, String c){
		//编辑关卡时，关卡序号、关卡id、关卡CRC 统一设置为 -1，关卡id小于0用于区别试推状态
		Num = -1;  //关卡序号
		Level_id = -1;  //关卡 id
		key = -1;  //关卡 CRC
		P_id = -1;
		Lock = false;
		fileName = null;
		Author = a;
		Title = t;
		Comment = c;
		Rows = rs;
		Cols = cs;
		if (m == null) Map = null;
		else {
			StringBuilder str = new StringBuilder();
			int n;
			char ch;
			for (int i = 0; i < Rows; i++) {
				n = m[i].length();
				for (int j = 0; j < Cols; j++){
					ch = j < n ? m[i].charAt(j) : '-';
					switch (ch){
						case '#':
						case '_':
						case '-':
						case '.':
						case '$':
						case '*':
						case '@':
						case '+':
							break;
						default:
							ch = '-';
					}
					str.append(ch);
				}
				if (i < Rows-1) str.append('\n');
			}
			Map = str.toString();
		}
	}

	//编辑的新建和导入调用
	public mapNode(int l, int r, int t, int b, char[][] m){
		//编辑关卡时，关卡序号、关卡id、关卡CRC 统一设置为 -1，关卡id小于0用于区别试推状态
		Num = -1;  //关卡序号
		Level_id = -1;  //关卡 id
		key = -1;  //关卡 CRC
		P_id = -1;
		Lock = false;
		fileName = null;
		Author = "";
		Title = "";
		Comment = "";
		Rows = b-t+1;
		Cols = r-l+1;

		StringBuilder str = new StringBuilder();
		for (int i = t; i <= b; i++) {
			for (int j = l; j <= r; j++){
				str.append(m[i][j]);
			}
			if (i < b) str.append('\n');
		}
		Map = str.toString();
	}

	//查找相似关卡时，记录源关卡
	public mapNode(long id, long pid, int rs, int cs, String m, String t, String a, String c, String m0){
		//编辑关卡时，关卡序号、关卡id、关卡CRC 统一设置为 -1，关卡id小于0用于区别试推状态
		Num = -1;  //关卡序号
		key = -1;  //关卡 CRC
		fileName = null;
		Lock = false;
		Level_id = id;
		P_id = pid;
		Author = a;
		Title = t;
		Comment = c;
		Rows = rs;
		Cols = cs;
		Map = m;
		Map0 = m0;
	}

	//常规导入或编辑提交时调用
	public mapNode(String m, String t, String a, String c){
		Num = -1;  //关卡序号
		Lock = false;
		P_id = -1;
		Level_id = 0;
		fileName = null;
		Author = a;
		Title = t;
		Comment = c;
		Solved = false;

		String[] arr = m.split("\r\n|\n\r|\n|\r|\\|");
		int rs = arr.length, cs = 0;
		for (int k = 0; k < rs; k++) {
			if (cs < arr[k].length()) {
				cs = arr[k].length();
			}
		}
		Rows = rs;
		Cols = cs;

		if (rs > myMaps.m_nMaxRow || cs > myMaps.m_nMaxCol || rs < 3 || cs < 3 || !mapNormalize(arr)){  //无效关卡
			Map = "--";
			Map0 = "";
			Title = "无效关卡";
			Author = "";
			if (c.trim().isEmpty()) {
				Comment = m + "\nTitle: " + t + "\nAuthor: " + a;  //无效关卡的所有信息，记录在关卡的“说明”中
			} else {
				Comment = m + "\nTitle: " + t + "\nAuthor: " + a + "\nComment: \n" + c + "\nComment_End: ";  //无效关卡的所有信息，记录在关卡的“说明”中
			}
			if (rs < 3 || cs < 3) {
				Rows = 1;
				Cols = 2;
			}
			L_CRC_Num = -1;
			key = 0;
		}
	}

	int[] dr ={-1, 1, 0, 0, -1, 1, -1, 1};	//前四个是可移动方向，后面是四个临角
	int[] dc ={0, 0, 1, -1, -1, -1, 1, 1};
	boolean[][] Mark;

	//地图标准化，包括：简单标准化 -- 保留关卡的墙外造型；精准标准化 -- 不保留关卡的墙外造型，同时计算 CRC 等
	public boolean mapNormalize(String[] Arr) {
		char[][] sMapArray = new char[Rows][Cols];

		int mr = -1, mc = -1;
		int nLen, nRen = 0;
		char ch;
		for (int i = 0; i < Rows; i++) {
			nLen = Arr[i].length();
			for (int j = 0; j < Cols; j++){
				if (j < nLen) ch = Arr[i].charAt(j);
				else ch = '-';

				switch (ch){
					case '#':
					case '.':
					case '$':
					case '*':
						break;
					case '@':
						nRen++;
						mr = i;
						mc = j;
						break;
					case '+':
						nRen++;
						mr = i;
						mc = j;
						break;
					default:
						ch = '-';
				}
				sMapArray[i][j] = ch;
			}
		}

		if (nRen != 1) {  //对仓管员 <> 1
			return false;
		}

		Mark = new boolean[Rows][Cols];	//标志数组，表示地图上某一位置Mark1[i][j]是否访问过。
		int F, mr2, mc2, left = mc, top = mr, right = mc, bottom = mr, nBox = 0, nDst = 0;

		Queue<Integer> P = new LinkedList<Integer>();
		P.offer(mr << 16 | mc);
		Mark[mr][mc] = true;
		while (!P.isEmpty()) { //走完后，Mark[][]为 1 的，为墙内
			F = P.poll();
			mr = F >>> 16;
			mc = F & 0x0000FFFF;
			switch (sMapArray[mr][mc]) {
				case '$':
					nBox++;
					break;
				case '*':
					nBox++;
					nDst++;
					break;
				case '.':
				case '+':
					nDst++;
					break;
			}
			for (int k = 0; k < 4; k++) {//仓管员向四个方向走
				mr2 = mr + dr[k];
				mc2 = mc + dc[k];
				if (mr2 < 0 || mr2 >= Rows || mc2 < 0 || mc2 >= Cols ||  //出界
						Mark[mr2][mc2] || sMapArray[mr2][mc2] == '#') continue;  //已访问或遇到墙

				//调整四至
				if (left > mc2) left = mc2;
				if (top > mr2) top = mr2;
				if (right < mc2) right = mc2;
				if (bottom < mr2) bottom = mr2;

				P.add(mr2 << 16 | mc2);
				Mark[mr2][mc2] = true;  //标记为已访问
			}
		}

		if (nBox != nDst || nBox < 1 || nDst < 1) {  //可达区域内的箱子与目标点数不正确
			return false;
		}

		//标准化后的尺寸（八转）
		int nRows = bottom-top+1+2;
		int nCols = right-left+1+2;
		char[][] aMap0 = new char[nRows][nCols];
		char[][] aMap1 = new char[nCols][nRows];
		char[][] aMap2 = new char[nRows][nCols];
		char[][] aMap3 = new char[nCols][nRows];
		char[][] aMap4 = new char[nRows][nCols];
		char[][] aMap5 = new char[nCols][nRows];
		char[][] aMap6 = new char[nRows][nCols];
		char[][] aMap7 = new char[nCols][nRows];

		//整理关卡元素
		for (int i = 0; i < Rows; i++) {
			for (int j = 0; j < Cols; j++) {
				ch = sMapArray[i][j];
				if (Mark[i][j]) {  //墙内
					if (!(ch == '-' || ch == '.' || ch == '$' || ch == '*' || ch == '@' || ch == '+')) {  //无效元素
						ch = '-';
						sMapArray[i][j] = ch;
					}
				} else {  //墙外造型
					if (ch == '*' || ch == '$') {
						ch = '#';
						sMapArray[i][j] = ch;
					} else if (!(ch == '#' || ch == '_')) {  //无效元素
						ch = '_';
						sMapArray[i][j] = ch;
					}
				}
				if (i >= top && i <= bottom && j >= left && j <= right){  //“四至”范围内
					if (Mark[i][j]) aMap0[i-top+1][j-left+1] = ch;  //标准化关卡的有效元素（暂时空出四周）
					else aMap0[i-top+1][j-left+1] = '_';
				}
			}
		}

		// 关卡最小化
		int mTop = 0, mLeft = 0, mBottom = Rows-1, mRight = Cols-1;
		for (int k = 0, t; k < Rows; k++) {
			t = 0;
			while (t < Cols && sMapArray[k][t] == '_') t++;
			if (t == Cols) mTop++;
			else break;
		}
		for (int k = Rows-1, t; k > mTop; k--) {
			t = 0;
			while (t < Cols && sMapArray[k][t] == '_') t++;
			if (t == Cols) mBottom--;
			else break;
		}
		if (mBottom - mTop < 2) return false;

		for (int k = 0, t; k < Cols; k++) {
			t = mTop;
			while (t <= mBottom && sMapArray[t][k] == '_') t++;
			if (t > mBottom) mLeft++;
			else break;
		}
		for (int k = Cols-1, t; k > mLeft; k--) {
			t = mTop;
			while (t <= mBottom && sMapArray[t][k] == '_') t++;
			if (t > mBottom) mRight--;
			else break;
		}
		if (mRight - mLeft < 2) return false;

		StringBuilder s1 = new StringBuilder();  //保持关卡原貌，对墙外造型部分不再清理
		for (int i = mTop; i <= mBottom; i++) {
			for (int j = mLeft; j <= mRight; j++) {
				s1.append(sMapArray[i][j]);
			}
			if (i < Rows-1) s1.append('\n');
		}
		Map = s1.toString();  //关卡原貌，已做简单标准化（保留墙外造型）
		Rows = mBottom-mTop+1;
		Cols = mRight-mLeft+1;

		//标准化关卡的四周填充 '_'
		for (int i = 0; i < nRows; i++) {
			for (int j = 0; j < nCols; j++) {
				if (i == 0 || j == 0 || i == nRows-1 || j == nCols-1) aMap0[i][j] = '_';
			}
		}

		//标准化
		for (int i = 1; i < nRows-1; i++) {
			for (int j = 1; j < nCols-1; j++) {
				if (aMap0[i][j] != '_' && aMap0[i][j] != '#'){  //探查内部有效元素的八个方位，是否可以安排墙壁
					if (aMap0[i-1][j] == '_') aMap0[i-1][j] = '#';
					if (aMap0[i+1][j] == '_') aMap0[i+1][j] = '#';
					if (aMap0[i][j-1] == '_') aMap0[i][j-1] = '#';
					if (aMap0[i][j+1] == '_') aMap0[i][j+1] = '#';
					if (aMap0[i+1][j-1] == '_') aMap0[i+1][j-1] = '#';
					if (aMap0[i+1][j+1] == '_') aMap0[i+1][j+1] = '#';
					if (aMap0[i-1][j-1] == '_') aMap0[i-1][j-1] = '#';
					if (aMap0[i-1][j+1] == '_') aMap0[i-1][j+1] = '#';
				}
			}
		}

		//标准化后的八转：关卡先顺时针旋转（得到：0转、1转、2转、3转），4转为0转的左右镜像，4转再顺时针旋转（得到：4转、5转、6转、7转）
		for (int i = 0; i < nRows; i++) {
			for (int j = 0; j < nCols; j++) {
				aMap1[j][nRows-1-i] = aMap0[i][j];
				aMap2[nRows-1-i][nCols-1-j] = aMap0[i][j];
				aMap3[nCols-1-j][i] = aMap0[i][j];
				aMap4[i][nCols-1-j] = aMap0[i][j];
				aMap5[nCols-1-j][nRows-1-i] = aMap0[i][j];
				aMap6[nRows-1-i][j] = aMap0[i][j];
				aMap7[j][i] = aMap0[i][j];
			}
		}

		StringBuilder s20 = new StringBuilder();  // 0 转
		StringBuilder s21 = new StringBuilder();  // 1 转
		StringBuilder s22 = new StringBuilder();  // 2 转
		StringBuilder s23 = new StringBuilder();  // 3 转
		StringBuilder s24 = new StringBuilder();  // 4 转
		StringBuilder s25 = new StringBuilder();  // 5 转
		StringBuilder s26 = new StringBuilder();  // 6 转
		StringBuilder s27 = new StringBuilder();  // 7 转

		for (int i = 0; i < nRows; i++) {
			for (int j = 0; j < nCols; j++) {
				s20.append(aMap0[i][j]);
				s22.append(aMap2[i][j]);
				s24.append(aMap4[i][j]);
				s26.append(aMap6[i][j]);
			}
			if(i < nRows-1) {
				s20.append('\n');
				s22.append('\n');
				s24.append('\n');
				s26.append('\n');
			}
		}
		for (int i = 0; i < nCols; i++) {
			for (int j = 0; j < nRows; j++) {
				s21.append(aMap1[i][j]);
				s23.append(aMap3[i][j]);
				s25.append(aMap5[i][j]);
				s27.append(aMap7[i][j]);
			}
			if(i < nCols-1) {
				s21.append('\n');
				s23.append('\n');
				s25.append('\n');
				s27.append('\n');
			}
		}

		//计算八转的最小 CRC，及其处于八转的第几转
		long[] key8 = { 0, 0, 0, 0, 0, 0, 0, 0 };
		key8[1] = myMaps.getCRC32(s21.toString());
		key8[2] = myMaps.getCRC32(s22.toString());
		key8[3] = myMaps.getCRC32(s23.toString());
		key8[4] = myMaps.getCRC32(s24.toString());
		key8[5] = myMaps.getCRC32(s25.toString());
		key8[6] = myMaps.getCRC32(s26.toString());
		key8[7] = myMaps.getCRC32(s27.toString());
		Map0 = s20.toString();  //精准标准化后的关卡
		key = myMaps.getCRC32(Map0);
		L_CRC_Num = 0;

		for (int i = 1; i < 8; i++){
			if (key > key8[i]){
				key = key8[i];
				L_CRC_Num = i;
			}
		}

		return true;
	}
}  //end class

//选择用节点
class selNode {
	int row, col;
}
