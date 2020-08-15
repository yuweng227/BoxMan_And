package my.boxman;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class mySQLite {

	//将数据库写到手机sd卡中，不再需要SQLiteOpenHelper类了，而是直接通过
	//mSDB = SQLiteDatabase.openOrCreateDatabase(DATABASE_PATH + dbName, null);就可以获得数据库的实例

	public static mySQLite m_SQL;
	private static String DATABASE_PATH = myMaps.sRoot + myMaps.sPath + "DataBase/";  //数据库在SD卡路径
	private static final String dbName = "BoxMan.db";  //数据库的名称
	public SQLiteDatabase mSDB;
	private String my_ANS; //导入答案需要把动作做大小写转换，借用一个全局变量
	public char[][] m_cArr = new char[myMaps.m_nMaxRow][myMaps.m_nMaxCol];

	public static mySQLite getInstance(Context context) {
		initDataBase(context);
		if (m_SQL == null) {
			m_SQL = new mySQLite();
		}
		return m_SQL;
	}

	//初试化数据库
	private static void initDataBase(Context context) {
		boolean dbExist = checkDataBase();
		if (dbExist) {
		} else {
			// 如果不存在，则将raw里的数据存入手机sd卡
			copyDataBase(context);
		}
	}

	//复制数据库到手机指定文件夹下
	private static void copyDataBase(Context context) {
		String databaseFilenames = DATABASE_PATH + dbName;
		File dir = new File(DATABASE_PATH);
		FileOutputStream os = null;
		InputStream is = null;

		// 判断文件夹是否存在，不存在就创建一个
		if (!dir.exists()) {
			dir.mkdirs();
		}
		try {
			// 得到数据库的输出流
			os = new FileOutputStream(databaseFilenames);
			// 得到数据文件的输入流
			is = context.getResources().getAssets().open(dbName);
			byte[] buffer = new byte[8192];
			int count;
			while ((count = is.read(buffer)) != -1) {
				os.write(buffer, 0, count);
				os.flush();
			}
			// 之所以不在这里初始化，是因为这边是静态的方法，而mSDB并没有设置为静态的，也不推荐设为静态的
			// mSDB = SQLiteDatabase.openOrCreateDatabase(DATABASE_PATH + dbName, null);
		} catch (Exception e) {
		} finally {
			try {
				os.close();
				is.close();
			} catch (IOException e) { }
		}
	}

	//判断数据库是否存在
	private static boolean checkDataBase() {
		SQLiteDatabase checkDB = null;
		String databaseFilename = DATABASE_PATH + dbName;
		// 要自己加上try catch方法
		try {
			// 返回最新的数据库
			checkDB = SQLiteDatabase.openDatabase(databaseFilename, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
			// TODO: handle exception
		}

		if (checkDB != null) {
			checkDB.close();
		}
		// 如果checkDB为null，则没有数据库，返回false
		return checkDB == null ? false : true;
	}

	//删除临时表
	public void del_tmp_Table2() {
		String sql = "DROP TABLE id_T";
		try {
			mSDB.execSQL(sql);
		} catch (Exception e) {
            e.printStackTrace ();
		}
	}

	//创建关卡集 ID 临时表，相似查找、查询使用
	public void new_tmp_Table2() {
		String sql = "CREATE TEMPORARY TABLE id_T ( T_id INTEGER PRIMARY KEY AUTOINCREMENT, P_id INTEGER )";
		try {
			mSDB.execSQL(sql);
		} catch (Exception e) {
		    e.printStackTrace ();
		}
	}

	//增加Set id
	public long add_Set_ID(long m_id) {
		ContentValues cv = new ContentValues();

		cv.put("P_id", m_id);

		return mSDB.insert("id_T", null, cv);
	}

//	//创建sha1临时表
//	public void new_sha1_Table() {
//		String sql = "CREATE TEMPORARY TABLE sha1_T ( T_Sha1 BLOB )";
//		mSDB.execSQL(sql);
//		m_SQL.set_tmp_Sha1(null);
//	}

//	//创建sha1临时表
//	public void set_tmp_Sha1(byte[] level_SHA1) {
//	    ContentValues cv = new ContentValues();
//	    cv.put("T_Sha1", level_SHA1);  //关卡SHA1
//		    
//	    mSDB.insert("sha1_T", null, cv);
//	}

	//检查关卡库（DB）版本号，传入 APP 运行需要进行关卡库（DB）升级的版本列表
	public List<Integer> Get_DB_Ver(int[] m_VerArray) {
		List<Integer> Ver_List =  new ArrayList() ;
		String queryStr = "select count(*)  from sqlite_master where type='table' and name = 'BoxManVer'";
		Cursor cursor = mSDB.rawQuery(queryStr, null);
		try {
			if (!cursor.moveToFirst() || cursor.getInt(0) == 0) {  //版本号表尚未建立
				queryStr = "CREATE TABLE BoxManVer ( T_Ver INTEGER NOT NULL DEFAULT 0 )";
				mSDB.execSQL(queryStr);  //创建表
				//需要做所有的升级
				for (int k=0; k<m_VerArray.length; k++) {
					Ver_List.add(m_VerArray[k]);
				}
			} else {
				//取传入版本号之后的版本号
				String where = "T_Ver = ?";
				for (int k = 0; k < m_VerArray.length; k++) {
					cursor = mSDB.query("BoxManVer", null, where, new String[]{Integer.toString(m_VerArray[k])}, null, null, null);
					if (!cursor.moveToNext()) {  //传入的版本号没有找到，则需要先升级
						Ver_List.add(m_VerArray[k]);
					}
				}
			}
		} finally {
			if (cursor != null) cursor.close();
		}
		return Ver_List;
	}

	//检查字段是否存在
//	public boolean isFieldExist(String tableName, String fieldName) {
//		String queryStr = "select sql from sqlite_master where type = 'table' and name = '%s'";
//		queryStr = String.format(queryStr, tableName);
//		Cursor c = mSDB.rawQuery(queryStr, null);
//		String tableCreateSql = null;
//		try {
//			if (c != null && c.moveToFirst()) {
//				tableCreateSql = c.getString(c.getColumnIndex("sql"));
//			}
//		} finally {
//			if (c != null) c.close();
//		}
//		if (tableCreateSql != null && tableCreateSql.contains(fieldName))
//			return true;
//		return false;
//	}

	//添加字段
//	public int addField(String tableName, String fieldName, String typeName) {
//		if (isFieldExist(tableName, fieldName))
//			return -1;
//
//		String queryStr = String.format("ALTER TABLE %s ADD %s %s", tableName, fieldName, typeName);
//
//		try {
//			mSDB.execSQL(queryStr);
//		} catch (Exception e) {
//			return 0;
//		}
//
//		if (isFieldExist(tableName, fieldName))
//			return 1;
//		else
//			return 0;
//	}

	//导入答案
	public void inp_Ans(mapNode ndMap, String ans) {

		if (ndMap == null) return;

		String Comment = "[导入]" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		myMaps.curMap = ndMap;

		int[] step = {0, 0};  //用于记录移动步数、推动步数
		my_ANS = isAnsOK_and_Case(ans, step);  //验证答案并修正大小写

		if (my_ANS.length() > 0) {
			m_SQL.add_S(myMaps.curMap.Level_id, 1, step[0], step[1], 0, 0, -1, -1, my_ANS, "", myMaps.curMap.key, myMaps.curMap.L_CRC_Num, myMaps.curMap.Map0, Comment);
		}
	}

	//取得通关书及关卡总数
	public String count_Level() {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);

		long m_count = 0, m_count0 = 0, m_count1 = 0;
		int m_solved;
		try {
			cursor = mSDB.rawQuery("select L_Solved, count() as L_Count from G_Level group by L_Solved", null);
			while (cursor.moveToNext()) {
				m_solved = cursor.getInt(cursor.getColumnIndex("L_Solved"));
                m_count = cursor.getLong(cursor.getColumnIndex("L_Count"));
				if (m_solved == 0) m_count0 = m_count;
				else m_count1 = m_count;
			}
//			cursor = mSDB.rawQuery("select L_Solved from G_Level", null);
//			if (cursor.moveToLast()) {
//				m_count = cursor.getCount();  //关卡数
//			}
//			cursor = mSDB.rawQuery("select L_Solved from G_Level where L_Solved = 1", null);
//			if (cursor.moveToLast()) {
//				m_count2 = cursor.getCount();  //关卡数
//			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return "( " + m_count1 + " / " + (m_count0 + m_count1) + " )";
//		return "( " + m_count2 + " / " + m_count + " )";
	}

	//取得关卡表中已经删除、但答案表中仍保留有答案的关卡
//	public List<Long> get_Level_From_Ans() {
//		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//		List<Long> Sid_List =  new ArrayList() ;
//
//		long o_key = 0, p_key, s_id;
//		try {
//			//关卡表中已经删除的、但答案表中有答案的关卡
//			cursor = mSDB.rawQuery("select * from G_State where G_Solution = 1 and P_Key_Num >= 0 and P_Key not in (select L_Key from G_Level) order by P_Key", null);
//			while (cursor.moveToNext()) {
//				s_id = cursor.getLong(cursor.getColumnIndex("S_id"));  //答案 id
//				p_key = cursor.getLong(cursor.getColumnIndex("P_Key"));  //关卡 CRC
//				if (o_key != p_key) {
//					Sid_List.add(s_id);
//					o_key = p_key;
//				}
//			}
//		} catch (Exception e) {
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//		return Sid_List;
//	}

	//取得关卡
//	public mapNode get_Level(long n) {
//		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//		String where = "L_id > ? LIMIT 1";
//		String[] whereValue ={ Long.toString(n) };
//
//		try {
//			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
//
//			if (cursor.moveToNext()){
//				mapNode nd = new mapNode(0,
//						cursor.getLong(cursor.getColumnIndex("P_id")),
//						cursor.getLong(cursor.getColumnIndex("L_id")),
//						cursor.getInt(cursor.getColumnIndex("L_Solved")),
//						cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
//						cursor.getString(cursor.getColumnIndex("L_Title")),
//						cursor.getString(cursor.getColumnIndex("L_Author")),
//						cursor.getString(cursor.getColumnIndex("L_Comment")),
//						cursor.getLong(cursor.getColumnIndex("L_Key")),
//						cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
//						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
//						cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标
//				return nd;
//			}
//		} catch (Exception e) {
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//		return null;
//	}

	//取得答案表中的关卡
//	public mapNode get_Level2(long n) {
//		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//		String where = "S_id = ?";
//		String[] whereValue ={ Long.toString(n) };
//
//		try {
//			cursor = mSDB.query("G_State", null, where, whereValue, null, null, null);
//
//			if (cursor.moveToNext()){
//				mapNode nd = new mapNode(0,
//						-1,  //P_id
//						cursor.getLong(cursor.getColumnIndex("S_id")),
//						1,  //是否有答案
//						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //关卡 XSB
//						"",
//						"",
//						"",
//						cursor.getLong(cursor.getColumnIndex("P_Key")),
//						cursor.getInt(cursor.getColumnIndex("P_Key_Num")),  //第几转
//						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
//						0);  //是否加锁图标
//				return nd;
//			}
//		} catch (Exception e) {
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//		return null;
//	}

	//返回关卡集的 id，增补时使用
	public long find_Set(String Title) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "T_Title = ?";

		String[] whereValue ={ Title };

		long m_id = -1;
		try {
			cursor = mSDB.query("G_Set", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) m_id = cursor.getLong(cursor.getColumnIndex("T_id"));
		} catch (Exception e) {
//	        e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_id;
	}

	//返回关卡集的 id，重命名时使用
	public long find_Set(String Title, long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "T_Title = ? AND T_id <> ?";

		String[] whereValue ={ Title, Long.toString(id)};

		long m_id = -1;
		try {
			cursor = mSDB.query("G_Set", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) m_id = cursor.getLong(cursor.getColumnIndex("T_id"));
		} catch (Exception e) {
//	        e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_id;
	}

	//更新G_Set标题
	public void set_T_T(long id, String Title){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = "UPDATE G_Set SET T_Title = ? WHERE T_id = ?";
		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Title, Long.toString(id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//更新关卡集已解关卡和总关卡数
//	public void Set_T_Count(long id, long mSolved, long mTotal){
//		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//		String sql = "UPDATE G_Set SET T_Solved = ?, T_Total = ? WHERE T_id = ?";
//		try {
//			cursor = mSDB.rawQuery(sql, new String[]{ Long.toString(mSolved), Long.toString(mTotal), Long.toString(id) });
//			cursor.moveToNext();
//		} catch (Exception e) {
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//	}

	//更新G_Set标题、作者、说明
	public void Update_T_Inf(long id, String Title, String Author, String Comment){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = "UPDATE G_Set SET T_Title = ?, T_Author = ?, T_Comment = ? WHERE T_id = ?";
		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Title, Author, Comment, Long.toString(id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//返回关卡集的信息
	public void get_Set(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "T_id = ?";
		String[] whereValue ={ Long.toString(id) };

		try {
			cursor = mSDB.query("G_Set", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				myMaps.m_Set_id = id;
				myMaps.J_Title = cursor.getString(cursor.getColumnIndex("T_Title"));
				myMaps.J_Author = cursor.getString(cursor.getColumnIndex("T_Author"));
				myMaps.J_Comment = cursor.getString(cursor.getColumnIndex("T_Comment"));
			}
		} catch (Exception e) {
//	        e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//根据关卡集 id 取得关卡集所在组的名称
//	public int getGroup(long id) {
//		int g = -1;
//		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//		String where = "T_id = ?";
//		String[] whereValue ={ Long.toString(id) };
//
//		try {
//			cursor = mSDB.query("G_Set", null, where, whereValue, null, null, null);
//			if (cursor.moveToNext()) {
//				g = cursor.getInt(cursor.getColumnIndex("T_Group"));
//			}
//		} catch (Exception e) {
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//		return g;
//	}

	//根据id取得关卡集名称
	public String getSetName(long id) {
		String s = "";
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "T_id = ?";
		String[] whereValue ={ Long.toString(id) };

		try {
			cursor = mSDB.query("G_Set", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				s = cursor.getString(cursor.getColumnIndex("T_Title"));
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return s;
	}

	//返回本组（num）的关卡集的列表
	public ArrayList<set_Node> get_GroupList(int num) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "T_Group = ?";
		String[] whereValue ={ Integer.toString(num) };

		ArrayList<set_Node> m_List = new ArrayList<set_Node>();
		try {
			cursor = mSDB.query("G_Set", null, where, whereValue, null, null, null);

			while (cursor.moveToNext()) {
				set_Node nd = new set_Node();
				nd.id = cursor.getLong(cursor.getColumnIndex("T_id"));
				nd.title = cursor.getString(cursor.getColumnIndex("T_Title"));
				m_List.add(nd);
			}
		} catch (Exception e) {
//	        e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_List;
	}

	//增加Set
	public long add_T(int Group, String Title, String Author, String Comment) {
		ContentValues cv = new ContentValues();

		cv.put("T_Group", Group);
		cv.put("T_Title", Title);
		cv.put("T_Author", Author == null ? "" : Author);
		cv.put("T_Comment", Comment == null ? "" : Comment);

		return mSDB.insert("G_Set", null, cv);
	}

	//按关卡的所属关卡集的id，返回关卡所属的关卡集的序号
	public int get_Level_Num(long Pid, long Lid) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ? AND L_NO > 0";
		String where2 = "P_id = ? AND L_NO = 0";
		String orderBy = "L_NO";
		String[] whereValue = { Long.toString(Pid) };

		int m_Num = 0;
		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, orderBy);
			while (cursor.moveToNext()) {
				m_Num++;
				if (Lid == cursor.getLong(cursor.getColumnIndex("L_id"))) return m_Num;
			}
			cursor = mSDB.query("G_Level", null, where2, whereValue, null, null, null);
			while (cursor.moveToNext()) {
				m_Num++;
				if (Lid == cursor.getLong(cursor.getColumnIndex("L_id"))) return m_Num;
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_Num;
	}

	//按关卡id查找关卡，返回关卡所属的关卡集的id
	public long get_Level_Set_id(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "L_id = ?";
		String[] whereValue = { Long.toString(id) };

		long m_id = -1;
		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				m_id = cursor.getLong(cursor.getColumnIndex("P_id"));
			}
		} catch (Exception e) {
			m_id = -1;
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_id;
	}

	//获取关卡最大id
	public long get_Max_id() {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String strSql = "select max(L_id) AS maxId from G_Level";

		long m_id = -1;
		try {
			cursor = mSDB.rawQuery(strSql, null);
			if (cursor.moveToNext()) {
				m_id = cursor.getLong(cursor.getColumnIndex("maxId"));
			}
		} catch (Exception e) {
			m_id = -1;
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_id;
	}

	//按CRC查找关卡，返回关卡id
//	public long find_Level(long key) {
//		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//		String where = "L_Key = ?";
//		String[] whereValue = { Long.toString(key) };
//
//		long m_id = -1;
//		try {
//			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
//			if (cursor.moveToNext()) {
//				m_id = cursor.getLong(cursor.getColumnIndex("L_id"));
//			}
//		} catch (Exception e) {
//			m_id = -1;
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//		return m_id;
//	}

	//按CRC查找重复关卡，返回关卡id
	public long find_Level(long key, long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "L_Key = ? AND L_id != ?";
		String[] whereValue = { Long.toString(key), Long.toString(id) };

		long m_id = -1;
		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				m_id = cursor.getCount();
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_id;
	}

	//查找当前关卡的重复关卡，返回重复关卡id
	public mapNode2 find_Level(mapNode nd) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "L_Key = ? AND L_id != ?";
		String[] whereValue = { Long.toString(nd.key), Long.toString(nd.Level_id) };

		mapNode2 nd2 = null;
		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				//创建重复关卡链表节点
				nd2 = new mapNode2();
				nd2.Num = -1;
				nd2.P_id = cursor.getLong(cursor.getColumnIndex("P_id")); //所属关卡集id
				nd2.Level_id = cursor.getLong(cursor.getColumnIndex("L_id")); //关卡id
				nd2.SetName = getSetName(cursor.getLong(cursor.getColumnIndex("P_id")));  //所在的关卡集名称
				nd2.Title = cursor.getString(cursor.getColumnIndex("L_Title"));
				nd2.Author = cursor.getString(cursor.getColumnIndex("L_Author"));
				nd2.Map = cursor.getString(cursor.getColumnIndex("L_Content"));
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return nd2;
	}

	//按关卡集id，计数其内的关卡数
	public long count_Level(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = " P_id = ? ";
		String[] whereValue = { Long.toString(id) };

		long m_count = 0;
		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				m_count = cursor.getCount();
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_count;
	}

	//按关卡集id，计数其内已解的关卡数
	public long count_Sovled(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = " P_id = ? AND L_Solved > 0 ";
		String[] whereValue = { Long.toString(id) };

		long m_count = 0;
		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				m_count = cursor.getCount();
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return m_count;
	}

	//取得最近推过的关卡列表
	public void get_Recent() {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = " SELECT * FROM G_Level WHERE L_DateTime <> '' ORDER BY L_DateTime DESC LIMIT 60 ";
		try {
			cursor = mSDB.rawQuery(sql, null, null);

			myMaps.m_lstMaps.clear();
			int num = 1;

			while (cursor.moveToNext()){
				mapNode nd = new mapNode(num++,
						cursor.getLong(cursor.getColumnIndex("P_id")),
						cursor.getLong(cursor.getColumnIndex("L_id")),
						cursor.getInt(cursor.getColumnIndex("L_Solved")),
						cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
						cursor.getString(cursor.getColumnIndex("L_Title")),
						cursor.getString(cursor.getColumnIndex("L_Author")),
						cursor.getString(cursor.getColumnIndex("L_Comment")),
						cursor.getLong(cursor.getColumnIndex("L_Key")),
						cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
						cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标
				myMaps.m_lstMaps.add(nd);
			}
		} catch (Exception e) {
//			e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//取得某关卡集中最近推过的关卡列表
	public Long get_Recent(long m_id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = " SELECT L_id FROM G_Level WHERE P_id = ? AND L_DateTime <> '' ORDER BY L_DateTime DESC LIMIT 1 ";
		String[] whereValue = { Long.toString(m_id) };
		long l_id = -1;

		try {
			cursor = mSDB.rawQuery(sql, whereValue, null);

			if (cursor.moveToNext()){
				l_id = cursor.getLong(cursor.getColumnIndex("L_id"));
			}
		} catch (Exception e) {
//			e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
		return l_id;
	}

	//取得关卡列表
	public void get_Levels(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ? AND L_NO > 0";
		String where2 = "P_id = ? AND L_NO = 0";
		String orderBy = "L_NO";
		String[] whereValue = { Long.toString(id) };

		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, orderBy);

			myMaps.m_lstMaps.clear();
			int num = 1;

			while (cursor.moveToNext()){
				mapNode nd = new mapNode(num++,
						id,
						cursor.getLong(cursor.getColumnIndex("L_id")),
						cursor.getInt(cursor.getColumnIndex("L_Solved")),
						cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
						cursor.getString(cursor.getColumnIndex("L_Title")),
						cursor.getString(cursor.getColumnIndex("L_Author")),
						cursor.getString(cursor.getColumnIndex("L_Comment")),
						cursor.getLong(cursor.getColumnIndex("L_Key")),
						cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
						cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标
				myMaps.m_lstMaps.add(nd);
			}
			cursor = mSDB.query("G_Level", null, where2, whereValue, null, null, null);
			while (cursor.moveToNext()){
				mapNode nd = new mapNode(num++,
						id,
						cursor.getLong(cursor.getColumnIndex("L_id")),
						cursor.getInt(cursor.getColumnIndex("L_Solved")),
						cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
						cursor.getString(cursor.getColumnIndex("L_Title")),
						cursor.getString(cursor.getColumnIndex("L_Author")),
						cursor.getString(cursor.getColumnIndex("L_Comment")),
						cursor.getLong(cursor.getColumnIndex("L_Key")),
						cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
						cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标
				myMaps.m_lstMaps.add(nd);
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//取得刚刚添加的关卡到“关卡列表”（仅含一个关卡的列表）
	public void get_Last_Level(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ? AND L_NO = 0";
		String[] whereValue = { Long.toString(id) };

		try {
			myMaps.m_lstMaps.clear();
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			if (cursor.moveToLast()){
				mapNode nd = new mapNode(1,
						id,
						cursor.getLong(cursor.getColumnIndex("L_id")),
						cursor.getInt(cursor.getColumnIndex("L_Solved")),
						cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
						cursor.getString(cursor.getColumnIndex("L_Title")),
						cursor.getString(cursor.getColumnIndex("L_Author")),
						cursor.getString(cursor.getColumnIndex("L_Comment")),
						cursor.getLong(cursor.getColumnIndex("L_Key")),
						cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
						cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标

				myMaps.m_lstMaps.add(nd);
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//取得 id 后面的关卡到“关卡列表”
	public ArrayList<mapNode> get_New_Level(long p_id, long l_id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ? AND L_id > ?";
		String[] whereValue = { Long.toString(p_id), Long.toString(l_id) };
		ArrayList<mapNode> mList = new ArrayList<mapNode>();

		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()){
				mapNode nd = new mapNode(1,
						p_id,
						cursor.getLong(cursor.getColumnIndex("L_id")),
						cursor.getInt(cursor.getColumnIndex("L_Solved")),
						cursor.getString(cursor.getColumnIndex("L_Content")),  //关卡 XSB
						cursor.getString(cursor.getColumnIndex("L_Title")),
						cursor.getString(cursor.getColumnIndex("L_Author")),
						cursor.getString(cursor.getColumnIndex("L_Comment")),
						cursor.getLong(cursor.getColumnIndex("L_Key")),
						cursor.getInt(cursor.getColumnIndex("L_Solution")),  //第几转
						cursor.getString(cursor.getColumnIndex("L_thin_XSB")),  //标准化关卡
						cursor.getInt(cursor.getColumnIndex("L_Locked")));  //是否加锁图标

				mList.add(nd);
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return mList;
	}

//	//计算关卡的SHA1
//	public static byte[] getSha1(String str){
//        if(str==null||str.length()==0) return null;
//        try {
//            MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
//            mdTemp.update(str.getBytes("UTF-8"));
//            return mdTemp.digest();
//        } catch (Exception e) {
//            return null;
//        }
//    }

	//增加Level到指定关卡集(id)
	public long add_L(long id, mapNode nd){
		int sol = count_S(nd.Level_id, nd.key, 1) > 0 ? 1 : 0;  //此关卡是否已有答案
		ContentValues cv = new ContentValues();
		cv.put("P_id", id);  //关卡集 id
		cv.put("L_Solution", nd.L_CRC_Num);
		cv.put("L_Title", nd.Title);
		cv.put("L_Author", nd.Author);
		cv.put("L_Comment", nd.Comment);
		cv.put("L_Content", nd.Map);
		cv.put("L_Key", nd.key);
		cv.put("L_thin_XSB", nd.Map0);
		cv.put("L_Solved", sol);
		cv.put("L_NO", 0);

		return mSDB.insert("G_Level", null, cv);
	}

	//查询关卡标题是否在本关卡集内有重名
//	public long find_Level_T(String Title, long p_id, long id) {
//	    Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//	    String where = "L_Title = ? AND P_id = ? AND L_id <> ?";
//	    
//	    String[] whereValue ={ Title, Long.toString(p_id), Long.toString(id)};
//
//	    long m_id = -1;
//        try {
//        	cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
//		    if (cursor.moveToNext()) m_id = cursor.getLong(cursor.getColumnIndex("L_id"));
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    } finally {
//	        if (cursor != null) cursor.close();
//	    }
//        return m_id;
//	}

	//更新 G_Level 图标加锁列
	public void Update_L_Lock(long id, int lock){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = "UPDATE G_Level SET L_Locked = ? WHERE L_id = ?";
		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Integer.toString(lock), Long.toString(id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//当前日期时间记入关卡库
	public void Set_L_DateTime(long id){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = " UPDATE G_Level SET L_DateTime = ? WHERE L_id = ? ";
		try {
			Date d = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			cursor = mSDB.rawQuery(sql, new String[]{ sdf.format(d), Long.toString(id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//设置关卡序号列
	public void Set_L_NO(long id, int n){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = " UPDATE G_Level SET L_NO = ? WHERE L_id = ? ";
		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Integer.toString(n), Long.toString(id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//登记是否解关，按照关卡 CRC(或关卡 id) 登记，导入及保存答案、删除答案时调用
	public void Set_L_Solved(long key, int mSolve, boolean isID){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql;
		if (isID) sql = " UPDATE G_Level SET L_Solved = ? WHERE L_id = ? ";
		else sql = " UPDATE G_Level SET L_Solved = ? WHERE L_Key = ? ";

		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Integer.toString(mSolve), Long.toString(key) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//设置全部相关关卡的解关状态
//	public void Set_L_Solved_All(){
//		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//		String sql = " UPDATE G_Level SET L_Solved = 1 WHERE L_Key IN ( SELECT P_Key FROM G_State WHERE G_Solution = 1 ) ";
//		try {
//			cursor = mSDB.rawQuery(sql, null);
//			cursor.moveToNext();
//		} catch (Exception e) {
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//	}

	//清空“最近”列表
	public void Clear_L_DateTime(){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = " UPDATE G_Level SET L_DateTime = '' WHERE L_DateTime <> '' ";
		try {
			cursor = mSDB.rawQuery(sql, null);
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//更新G_Level标题、作者、说明
	public void Update_L_inf(long id, String Title, String Author, String Comment){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = "UPDATE G_Level SET L_Title = ?, L_Author = ?, L_Comment = ? WHERE L_id = ?";
		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Title, Author, Comment, Long.toString(id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//计算答案8方向CRC中的最小者
	public long get_minCRC(int CRC_Num, String ans) {
		long minCRC = myMaps.getCRC32(ans);  //计算关卡CRC最小转的答案CRC
		if (CRC_Num == 0) return minCRC;

		StringBuilder str;
		int len = ans.length();

		str = new StringBuilder();
		for (int s = 0; s < len; s++) {
			switch (ans.charAt(s)) {
				case 'l':
					str.append(myMaps.DIR[CRC_Num][0]);
					break;
				case 'u':
					str.append(myMaps.DIR[CRC_Num][1]);
					break;
				case 'r':
					str.append(myMaps.DIR[CRC_Num][2]);
					break;
				case 'd':
					str.append(myMaps.DIR[CRC_Num][3]);
					break;
				case 'L':
					str.append(myMaps.DIR[CRC_Num][4]);
					break;
				case 'U':
					str.append(myMaps.DIR[CRC_Num][5]);
					break;
				case 'R':
					str.append(myMaps.DIR[CRC_Num][6]);
					break;
				case 'D':
					str.append(myMaps.DIR[CRC_Num][7]);
			}
		}
		minCRC = myMaps.getCRC32(str.toString());

		return minCRC;
	}

	//更新状态或答案的注释信息
	public void Update_A_inf(long id, String Comment){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = "UPDATE G_State SET G_DateTime = ? WHERE S_id = ?";
		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Comment, Long.toString(id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//更新状态或答案的 p_id
	public void Update_A_Lid(long old_id, long new_id){
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String sql = "UPDATE G_State SET P_id = ? WHERE P_id = ?";
		try {
			cursor = mSDB.rawQuery(sql, new String[]{ Long.toString(new_id), Long.toString(old_id) });
			cursor.moveToNext();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//增加状态或答案
	public long add_S(long id, int Solution, int Moves, int Pushs, int Moves2, int Pushs2, int Row2, int Col2, String Ans, String bk_Ans, long key, int CRC_Num, String thin_XSB, String Comment) {
		myMaps.m_Nums[0]++;  //统计答案总数

		long m_crc;  //计算CRC最小转关卡的答案CRC

		if (Solution == 1) {  //对状态，略过验证，直接保存即可
			m_crc = get_minCRC(CRC_Num, Ans);  //计算CRC最小转关卡的答案CRC
			if (Ans.length() > myMaps.m_nMaxSteps || m_SQL.count_S(key, Moves, Pushs, m_crc) > 0) {  //查重
				myMaps.m_Nums[1]++;  //统计重复答案（不导入）
				return 0L;
			}
		} else {
			m_crc = myMaps.getCRC32(Ans + bk_Ans);  //计算CRC最小转关卡的答案CRC
			if (Solution == 0) {  //正常的状态保存，yass求解前的自动保存，Solution == 3
				if (m_SQL.count_S(id, Moves, Pushs, Moves2, Pushs2, Row2, Col2, m_crc)) {
					return 0L;  //状态已有过保存，不需要重复保存
				}
			} else Solution = 0;
		}

		//没找到重复答案
		ContentValues cv = new ContentValues();
		cv.put("P_id", id);
		cv.put("G_Solution", Solution);
		cv.put("G_Moves", Moves);
		cv.put("G_Pushs", Pushs);
		cv.put("G_Moves2", Moves2);
		cv.put("G_Pushs2", Pushs2);
		cv.put("G_Row2", Row2);
		cv.put("G_Col2", Col2);
		cv.put("G_Ans", Ans);
		cv.put("G_bk_Ans", bk_Ans);
		cv.put("P_Key", key);
		if (!Comment.isEmpty()) cv.put("G_DateTime", Comment);
		cv.put("S_CRC", m_crc);
		cv.put("P_Key_Num", CRC_Num);
		cv.put("L_thin_XSB", thin_XSB);

		if (Solution == 1) Set_L_Solved(key, 1, false);

		return mSDB.insert("G_State", null, cv);
	}

	//计数保存数  -- 保存答案时，计算有无重复的答案（以关卡CRC、推移步数、答案CRC共4项进行比对）
	public long count_S(long key, int moves, int pushes, long CRC) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_Key = ? AND G_Moves = ? AND G_Pushs = ? AND S_CRC = ? AND G_Solution = 1";
		String[] whereValue = {Long.toString(key), Integer.toString(moves), Integer.toString(pushes), Long.toString(CRC)};

		long num = 0;
		try {
			cursor = mSDB.query("G_State", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) num = cursor.getCount();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return num;
	}

	//计数状态数  -- 检查状态有无重复
	public boolean count_S(long l_id, int moves, int pushes, int moves2, int pushes2, int row2, int col2, long CRC) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ? AND G_Moves = ? AND G_Pushs = ? AND G_Moves2 = ? AND G_Pushs2 = ? AND G_Row2 = ? AND G_Col2 = ? AND S_CRC = ? AND G_Solution = 0";
		String[] whereValue = {Long.toString(l_id), Integer.toString(moves), Integer.toString(pushes), Integer.toString(moves2), Integer.toString(pushes2), Integer.toString(row2), Integer.toString(col2), Long.toString(CRC)};

		long num = 0;
		try {
			cursor = mSDB.query("G_State", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) num = cursor.getCount();
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return num > 0;
	}

	//计数状态数
	public long count_S(long id, long key, int Solution) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where;

		if (Solution == 1)  //答案按关卡CRC计数
			where = "P_Key = ? AND G_Solution = 1";
		else                //状态按关卡id计数
			where = "P_id = ? AND G_Solution = 0";

		long num = 0;
		try {
			if (Solution == 1)  //计数答案
				cursor = mSDB.query("G_State", null, where, new String[]{ Long.toString(key) }, null, null, null);
			else                //计数状态
				cursor = mSDB.query("G_State", null, where, new String[]{ Long.toString(id) }, null, null, null);

			if (cursor.moveToNext()) num = cursor.getCount();
		} catch (Exception e) {
			num = -1;
		} finally {
			if (cursor != null) cursor.close();
		}
		return num;
	}

	//删除关卡集(id)
	public void del_T(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ?";
		String[] whereValue = { Long.toString(id) };

		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			long k;
			while (cursor.moveToNext()) {
				k = cursor.getLong(cursor.getColumnIndex("L_id"));
				mSDB.delete("G_State", "P_id = ? AND G_Solution = 0", new String[]{ Long.toString(k) });  //清理状态表，不删除答案
			}
			mSDB.delete("G_Level", where, whereValue);  //清理关卡表
			where = "T_id = ?" ;
			mSDB.delete("G_Set", where, whereValue);  //删除关卡集
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//删除某个关卡集的答案(id)
	public void del_T_Ans(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ?";
		String[] whereValue = { Long.toString(id) };

		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);

			long k;
			while (cursor.moveToNext()) {
				k = cursor.getLong(cursor.getColumnIndex("L_Key"));

				if (k == 328550106) {  //第一个内置关卡 -- BoxWorld 的第一个关卡
//					mSDB.delete("G_State", "P_Key = ? AND G_Solution = 1 AND S_CRC <> 893232827", new String[]{ Long.toString(k) });  //删除答案
				} else {
					mSDB.delete("G_State", "P_Key = ? AND G_Solution = 1", new String[]{ Long.toString(k) });  //删除答案
					Set_L_Solved(k, 0, false);
				}
			}
		} catch (Exception e) {
			e.printStackTrace ();
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//删除关卡(id)
	public void del_L(long id) {
		String where = "L_id = ?" ;
		String[] whereValue = { Long.toString(id) };
		mSDB.delete("G_State", "P_id = ? AND G_Solution = 0", whereValue);  //清理状态表，不删除答案
		mSDB.delete("G_Level", where, whereValue);
	}

	//删除状态(状态 id)
	public void del_S(long id) {
		String where = "S_id = ?" ;
		String[] whereValue = { Long.toString(id) };
		mSDB.delete("G_State", where, whereValue);
	}

	//删除所有状态(关卡 id)
	public void del_S_ALL(long id) {
		String where = "P_id = ? AND G_Solution = 0" ;
		String[] whereValue = { Long.toString(id) };
		mSDB.delete("G_State", where, whereValue);
	}

	//清理关卡集内所有关卡的状态(关卡集id)
	public void clear_S(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_id = ?";
		String[] whereValue = { Long.toString(id) };

		long ls_id;
		try {
			cursor = mSDB.query("G_Level", null, where, whereValue, null, null, null);
			while (cursor.moveToNext()){
				ls_id = cursor.getLong(cursor.getColumnIndex("L_id"));  //关卡id
				mSDB.delete("G_State", "P_id = ? AND G_Solution != 1", new String[]{ Long.toString(ls_id) });
			}
		} catch (Exception e) {
//	        e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	//按照状态 id 取得状态
	public ans_Node load_State(long id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "S_id = ?";
		String[] whereValue = { Long.toString(id) };
		String ans;

		ans_Node nd = new ans_Node();
		try {
			cursor = mSDB.query("G_State", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				ans = cursor.getString(cursor.getColumnIndex("G_Ans"));
				nd.r = cursor.getInt(cursor.getColumnIndex("G_Row2"));
				nd.c = cursor.getInt(cursor.getColumnIndex("G_Col2"));
				nd.solution = cursor.getInt(cursor.getColumnIndex("G_Solution"));
				nd.ans = (nd.solution == 1 ? myMaps.getANS(ans, cursor.getInt(cursor.getColumnIndex("P_Key_Num")), myMaps.curMap.L_CRC_Num) : ans);
				nd.bk_ans = cursor.getString(cursor.getColumnIndex("G_bk_Ans"));
				nd.time = cursor.getString(cursor.getColumnIndex("G_DateTime"));
			}
		} catch (Exception e) {
//	        e.printStackTrace();
		} finally {
			if (cursor != null) cursor.close();
		}
		return nd;
	}

	//导出仅有答案的关卡
	public boolean expAnsLevel() {
		boolean flg = true;
		String my_Name;
		Cursor cursor;
		cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);

		try{
			my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("导出/").append("仅有答案的关卡").append(myMaps.isLurd ? ".txt" : ".xsb").toString();

			FileOutputStream fout = new FileOutputStream(my_Name);
			StringBuilder str = new StringBuilder();

			str.append("Title: ").append("仅有答案的关卡").append('\n');
			str.append("Comment:\n").append("仅有答案，没有（被删除）原始关卡的自由关卡").append("\nComment-End:\n");
			fout.write(str.toString().getBytes());

			long key, old_key = 0, n = 0;
			String level, ans;
			cursor = mySQLite.m_SQL.mSDB.rawQuery("select * from G_State where G_Solution = 1 and P_Key_Num >= 0 and P_Key not in (select L_Key from G_Level) order by P_Key", null);
			while (cursor.moveToNext()) {
				//关卡标准化
				try{
					key = cursor.getLong(cursor.getColumnIndex("P_Key"));
					ans = cursor.getString(cursor.getColumnIndex("G_Ans"));
				 }catch (Exception e) {
					continue;
				}

				str = new StringBuilder();
				if (old_key != key) {  //XSB
					n++;
					level = cursor.getString(cursor.getColumnIndex("L_thin_XSB"));
					str.append("\n;Level ").append(n).append("\n").append(level).append("\n");
				}

				//Lurd
				if (myMaps.isLurd) {
					str.append("Solution (moves ").append(cursor.getString(cursor.getColumnIndex("G_Moves")))
							.append(", pushes ").append(cursor.getString(cursor.getColumnIndex("G_Pushs")));
					if (myMaps.isComment) {  //是否导出答案备注
						str.append(", comment ").append(cursor.getString(cursor.getColumnIndex("G_DateTime")));
					}
					str.append("): \n").append(ans).append('\n');
				}

				fout.write(str.toString().getBytes());
				old_key = key;
			}

			fout.flush();
			fout.close();

		}catch(Exception e){
			flg = false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return flg;
	}

	//导出全部关卡信息
//	public boolean exp_Inf_ALL() {
//		String my_Name;
//		boolean flg = false;
//		Cursor cursor;
//		cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
//
//		try{
//			my_Name = new StringBuilder(myMaps.sRoot).append(myMaps.sPath).append("Inf_ALL.txt").toString();
//
//			FileOutputStream fout = new FileOutputStream(my_Name);
//			StringBuilder str;
//
//			String where = "T_id <= 33", s2;
//			cursor = mSDB.query("G_Set", null, where, null, null, null, null);
//			while (cursor.moveToNext()) {
//				str = new StringBuilder("~~~Set~~~\n");
//				//inf
//				s2 = cursor.getString(cursor.getColumnIndex("T_Comment"));
////				str.append("Title:").append(cursor.getString(cursor.getColumnIndex("T_Title")).substring(3)).append('\n')
//				str.append("Title:").append(cursor.getString(cursor.getColumnIndex("T_Title"))).append('\n')
//						.append("Author:").append(cursor.getString(cursor.getColumnIndex("T_Author"))).append('\n');
//				if (s2.replaceAll("[\n\t\r ]", "").trim().isEmpty()) {
//					str.append("Comment:\nComment_End:\n\n");
//				} else {
//					str.append("Comment:\n").append(s2);
//					if (s2.charAt(s2.length() - 1) == '\n') {
//						str.append("Comment_End:\n\n");
//					} else {
//						str.append("\nComment_End:\n\n");
//					}
//				}
//
//				fout.write(str.toString().getBytes());
//			}
//
//			where = "P_id <= 33";
//			cursor = mSDB.query("G_Level", null, where, null, null, null, null);
//			while (cursor.moveToNext()) {
//				str = new StringBuilder("~~~Level~~~\n");
//				s2 = cursor.getString(cursor.getColumnIndex("L_Comment"));
//				//inf
//				str.append("L_Key:").append(cursor.getString(cursor.getColumnIndex("L_Key"))).append('\n')
//						.append("Title:").append(cursor.getString(cursor.getColumnIndex("L_Title"))).append('\n')
//						.append("Author:").append(cursor.getString(cursor.getColumnIndex("L_Author"))).append('\n');
//				if (s2.replaceAll("[\n\t\r ]", "").trim().isEmpty()) {
//					str.append("Comment:\nComment_End:\n\n");
//				} else {
//					str.append("Comment:\n").append(s2);
//					if (s2.charAt(s2.length()-1) == '\n') {
//						str.append("Comment_End:\n\n");
//					} else {
//						str.append("\nComment_End:\n\n");
//					}
//				}
//
//				fout.write(str.toString().getBytes());
//			}
//
//			fout.flush();
//			fout.close();
//			flg = true;
//		}catch(Exception e){
//		} finally {
//			if (cursor != null) cursor.close();
//		}
//		return flg;
//	}

	//读取答案
	public String get_Ans(long key) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);

		String where = "P_Key = ? AND G_Solution = 1";     //按关卡CRC列出所有可能的答案
		String[] whereValue = { Long.toString(key) };

		StringBuilder str = new StringBuilder();
		String ans;
		try {
			cursor = mSDB.query("G_State", null, where, whereValue, null, null, null);
			while (cursor.moveToNext()) {
				//计算关卡n转答案
				ans = myMaps.getANS(cursor.getString(cursor.getColumnIndex("G_Ans")), cursor.getInt(cursor.getColumnIndex("P_Key_Num")), myMaps.curMap.L_CRC_Num);
				if (isAnsOK(ans)){  //试推验证答案
					str.append("Solution (moves ").append(cursor.getString(cursor.getColumnIndex("G_Moves")))
							.append(", pushes ").append(cursor.getString(cursor.getColumnIndex("G_Pushs")));
					if (myMaps.isComment) {  //是否导出答案备注
						str.append(", comment ").append(cursor.getString(cursor.getColumnIndex("G_DateTime")));
					}
					str.append("): \n").append(ans.replaceAll("[^LURDlurd]", "")).append('\n');
				}
			}

		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return str.toString();
	}

	// 检查该答案是否为快手保留答案（第一个内置关卡，至少需保留 1 个答案）
	public boolean isCanDeleteAns(long s_id) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);

		String where = "S_id = ?";
		String[] whereValue = { Long.toString(s_id) };

		long p_key;
		try {
			cursor = mSDB.query("G_State", null, where, whereValue, null, null, null);
			if (cursor.moveToNext()) {
				p_key = cursor.getLong(cursor.getColumnIndex("P_Key"));

				// 不是第一个内置关卡，或多于 1 个答案，就允许删除
				if (p_key != 328550106 || count_S(-1, p_key, 1) > 1) {
					return false;
				} else {
					return true;
				}
			}

		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}

		// 遇到错误时，默认不允许删除
		return true;
	}

	//取得指定关卡(id)的状态列表
	public boolean load_StateList(long id, long key) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_Key = ?";                                 //以CRC查询，解关可能有非本关卡保存的状态及答案
		String orderBy = "G_Moves, G_Pushs";
		String[] whereValue = { Long.toString(key) };

		myMaps.mState1.clear();
		myMaps.mState2.clear();
		try {
			cursor = mSDB.query("G_State", null, where, whereValue, null, null, orderBy);
			StringBuilder str, str2;
			int moves, pushs;

			while (cursor.moveToNext()){
				str = new StringBuilder();
				str2 = new StringBuilder();

				if (id == cursor.getLong(cursor.getColumnIndex("P_id")) ||          //本关卡保存的状态及答案，直接读取
						(cursor.getInt(cursor.getColumnIndex("G_Solution")) == 1 && isAnsOK(myMaps.getANS(cursor.getString(cursor.getColumnIndex("G_Ans")), cursor.getInt(cursor.getColumnIndex("P_Key_Num")), myMaps.curMap.L_CRC_Num)))) {    //非本关卡保存的答案，做试推验证cursor.getString(cursor.getColumnIndex("G_Ans"))

					moves = cursor.getInt(cursor.getColumnIndex("G_Moves"));
					pushs = cursor.getInt(cursor.getColumnIndex("G_Pushs"));
					str.append("移动: ").append(moves).append(", 推动: ").append(pushs);
					str2.append(cursor.getString(cursor.getColumnIndex("G_DateTime")));
					if (cursor.getInt(cursor.getColumnIndex("G_Solution")) == 0) {  //答案不需要逆推步数
						str.append(" [ 移: ").append(cursor.getInt(cursor.getColumnIndex("G_Moves2")))
								.append(", 拉: ").append(cursor.getInt(cursor.getColumnIndex("G_Pushs2"))).append(" ]");
					}
					state_Node ans = new state_Node();
					ans.id = cursor.getLong(cursor.getColumnIndex("S_id"));
					ans.pid = cursor.getLong(cursor.getColumnIndex("P_id"));
					ans.pkey = cursor.getLong(cursor.getColumnIndex("P_Key"));
					ans.moves = moves;
					ans.pushs = pushs;
					ans.inf = str.toString();
					ans.time = str2.toString();

					if (cursor.getInt(cursor.getColumnIndex("G_Solution")) == 0)
						myMaps.mState1.add(ans);  //状态
					else
						myMaps.mState2.add(ans);   //答案
				}
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return (myMaps.mState1.size() > 0 || myMaps.mState2.size() > 0);
	}

	//取得相似关卡(id)的答案列表
	public boolean load_SolitionList(long key) {
		Cursor cursor = mSDB.rawQuery("PRAGMA synchronous=OFF", null);
		String where = "P_Key = ? and G_Solution = 1";                                 //以CRC查询，解关可能有非本关卡保存的状态及答案
		String orderBy = "G_Moves, G_Pushs";
		String[] whereValue = { Long.toString(key) };

		myMaps.mState1.clear();
		myMaps.mState2.clear();
		try {
			cursor = mSDB.query("G_State", null, where, whereValue, null, null, orderBy);
			StringBuilder str, str2;

			while (cursor.moveToNext()){
				str = new StringBuilder();
				str2 = new StringBuilder();

				if ((isAnsOK(myMaps.getANS(cursor.getString(cursor.getColumnIndex("G_Ans")), cursor.getInt(cursor.getColumnIndex("P_Key_Num")), myMaps.curMap.L_CRC_Num)))) {    //非本关卡保存的答案，做试推验证cursor.getString(cursor.getColumnIndex("G_Ans"))

					str.append("移动: ").append(cursor.getInt(cursor.getColumnIndex("G_Moves")))
							.append(", 推动: ").append(cursor.getInt(cursor.getColumnIndex("G_Pushs")));
					str2.append(cursor.getString(cursor.getColumnIndex("G_DateTime")));

					state_Node ans = new state_Node();
					ans.id = cursor.getLong(cursor.getColumnIndex("S_id"));
					ans.pid = cursor.getLong(cursor.getColumnIndex("P_id"));
					ans.pkey = cursor.getLong(cursor.getColumnIndex("P_Key"));
					ans.inf = str.toString();
					ans.time = str2.toString();

					myMaps.mState2.add(ans);   //答案
				}
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) cursor.close();
		}
		return (myMaps.mState2.size() > 0);
	}

	//关卡是否 OK，以及试推前准备
	int nDstOK;
	int nDstNum;
	int nBoxNum;
	int nRow;
	int nCol;
	public boolean isLevelOK() {
		nDstOK = 0;
		nDstNum = 0;
		nBoxNum = 0;
		nRow = -1;
		nCol = -1;
		try{
			//小于3行3列的地图，地图无效
			if(myMaps.curMap.Rows < 3 || myMaps.curMap.Cols < 3) throw new Exception();

			String[] Arr = myMaps.curMap.Map.split("\r\n|\n\r|\n|\\|");

			char ch;
			for (int i = 0; i < myMaps.curMap.Rows; i++)
				for (int j = 0; j < myMaps.curMap.Cols; j++){
					ch = Arr[i].charAt(j);
					switch (ch){
						case '#':
						case '_':
						case '-':
							m_cArr[i][j] = ch;
							break;
						case '*':
							nBoxNum++;
							nDstOK++;
							nDstNum++;
							m_cArr[i][j] = ch;
							break;
						case '+':
							nRow = i;
							nCol = j;
							nDstNum++;
							m_cArr[i][j] = ch;
							break;
						case '.':
							nDstNum++;
							m_cArr[i][j] = ch;
							break;
						case '$':
							nBoxNum++;
							m_cArr[i][j] = ch;
							break;
						case '@':
							nRow = i;
							nCol = j;
							m_cArr[i][j] = ch;
							break;
						default: m_cArr[i][j] = '_';
					}
				}
		} catch (Exception e) {
			return false;   //读取地图出错，地图无效
		}

		//没有仓管员、箱子数与目标数不符，地图无效
		if (nRow < 0 || nCol < 0 || nRow >= myMaps.curMap.Rows|| nCol >= myMaps.curMap.Cols|| nBoxNum != nDstNum) return false;

		return true;
	}

	//在 myMaps.curMap 上假推，进行答案验证并转换大小写
	int[] dr = {0,  0, -1, 0, 1,  0, -2, 0, 2};
	int[] dc = {0, -1,  0, 1, 0, -2,  0, 2, 0};	//人和箱子四个方向调整值：l,u,r,d，为省脑筋，前面多放一个 0
	int i, j, i2, j2;
	boolean pushed;
	byte dir;
	int len;
	public boolean isAnsOK(String ans) {

		if (!isLevelOK()) return false;

		len = ans.length();
		pushed = false;
		for (int k = 0; k < len; k++){
			switch (ans.charAt(k)){
				case 'l':
					dir = 1;
					break;
				case 'u':
					dir = 2;
					break;
				case 'r':
					dir = 3;
					break;
				case 'd':
					dir = 4;
					break;
				case 'L':
					dir = 5;
					break;
				case 'U':
					dir = 6;
					break;
				case 'R':
					dir = 7;
					break;
				case 'D':
					dir = 8;
					break;
				default: continue;  //无效动作
			}

			if (dir < 5){
				i = nRow + dr[dir];
				j = nCol + dc[dir];
				//界外
				if (i < 0 || j < 0 || i >= myMaps.curMap.Rows || j > myMaps.curMap.Cols) return false;
				//不能动
				if (m_cArr[i][j] != '-' && m_cArr[i][j] != '.') return false;
				i2 = -1;
				j2 = -1;
			} else {
				i = nRow + dr[dir-4];
				j = nCol + dc[dir-4];
				i2 = nRow + dr[dir];
				j2 = nCol + dc[dir];
				//界外
				if (i < 0 || j < 0 || i2 < 0 || j2 < 0 || i >= myMaps.curMap.Rows || j >= myMaps.curMap.Cols || i2 >= myMaps.curMap.Rows || j2 >= myMaps.curMap.Cols) return false;
				//不能推
				if (m_cArr[i2][j2] != '-' && m_cArr[i2][j2] != '.' || m_cArr[i][j] != '$' && m_cArr[i][j] != '*') return false;
				pushed = true;
			}

			if ( dir > 4){ //若为“推”
				//先动箱子
				if (m_cArr[i2][j2] == '-')
					m_cArr[i2][j2] = '$';
				else {
					m_cArr[i2][j2] = '*';
					nDstOK++;
				}
				//再动人
				if (m_cArr[i][j] == '$')
					m_cArr[i][j] = '@';
				else {
					m_cArr[i][j] = '+';
					nDstOK--;
				}
			} else { //若为移动
				if (m_cArr[i][j] == '-')
					m_cArr[i][j] = '@';
				else {
					m_cArr[i][j] = '+';
				}
			}
			if (m_cArr[nRow][nCol] == '@')
				m_cArr[nRow][nCol] = '-';
			else
				m_cArr[nRow][nCol] = '.';

			nRow = i;
			nCol = j;
		}  //end for

		//有“推”的动作，且完成目标
		return (pushed && nDstNum == nDstOK);
	}

	//批量导入时，在 myMaps.curMap 上假推，进行答案验证并转换大小写
	char[] arr_char = {'l', 'u', 'r', 'd', 'L', 'U', 'R', 'D'};
	StringBuilder sb;
	public String isAnsOK_and_Case(String ans, int[] step) {

		if (!isLevelOK()) return "";

		sb = new StringBuilder();

		len = ans.length();
		pushed = false;
		for (int k = 0; k < len; k++){
			switch (ans.charAt(k)){
				case 'l':
					dir = 1;
					break;
				case 'u':
					dir = 2;
					break;
				case 'r':
					dir = 3;
					break;
				case 'd':
					dir = 4;
					break;
				case 'L':
					dir = 5;
					break;
				case 'U':
					dir = 6;
					break;
				case 'R':
					dir = 7;
					break;
				case 'D':
					dir = 8;
					break;
				default:
					continue;
			}

			//检查并修正动作
			if (dir < 5){
				i = nRow + dr[dir];
				j = nCol + dc[dir];
				i2 = -1;
				j2 = -1;

				//界外
				if (i < 0 || j < 0 || i >= myMaps.curMap.Rows || j >= myMaps.curMap.Cols) return "";

				//若 [i, j] 是箱子，修正动作
				if (m_cArr[i][j] == '$' || m_cArr[i][j] == '*'){
					dir += 4;
					i = nRow + dr[dir-4];
					j = nCol + dc[dir-4];
					i2 = nRow + dr[dir];
					j2 = nCol + dc[dir];

					if (i2 < 0 || j2 < 0 || i2 >= myMaps.curMap.Rows || j2 >= myMaps.curMap.Cols || m_cArr[i2][j2] != '.' && m_cArr[i2][j2] != '-')
						return "";  //不能推
				}
			} else {
				i = nRow + dr[dir-4];
				j = nCol + dc[dir-4];
				i2 = nRow + dr[dir];
				j2 = nCol + dc[dir];

				//界外
				if (i < 0 || j < 0 || i >= myMaps.curMap.Rows || j >= myMaps.curMap.Cols) return "";

				//若 [i, j] 是地板，修正动作
				if (m_cArr[i][j] == '.' || m_cArr[i][j] == '-'){
					dir -= 4;
					i2 = -1;
					j2 = -1;
				} else
				if (m_cArr[i][j] != '$' && m_cArr[i][j] != '*' || m_cArr[i2][j2] != '.' && m_cArr[i2][j2] != '-')
					return "";  //无效动作
			}

			//有效动作
			sb.append(arr_char[dir-1]);
			step[0]++;

			if ( dir > 4) { //若为“推”
				step[1]++;
				if (!pushed) pushed = true;
				//先动箱子
				if (m_cArr[i2][j2] == '-')
					m_cArr[i2][j2] = '$';
				else {
					m_cArr[i2][j2] = '*';
					nDstOK++;
				}

				//再动人
				if (m_cArr[i][j] == '$')
					m_cArr[i][j] = '@';
				else {
					m_cArr[i][j] = '+';
					nDstOK--;
				}
			} else { //若为移动
				if (m_cArr[i][j] == '-')
					m_cArr[i][j] = '@';
				else {
					m_cArr[i][j] = '+';
				}
			}
			if (m_cArr[nRow][nCol] == '@')
				m_cArr[nRow][nCol] = '-';
			else
				m_cArr[nRow][nCol] = '.';

			nRow = i;
			nCol = j;
		}

		//有“推”的动作，完成目标，答案才有效
		if (!pushed || nDstNum != nDstOK) return "";
		else return sb.toString();
	}

	public void closeDataBase() {
		if (mSDB != null) {
			mSDB.close();
		}
	}

	public boolean openDataBase() {
		mSDB = SQLiteDatabase.openOrCreateDatabase(DATABASE_PATH + dbName, null);
		if (mSDB == null) return false;
		else return true;
	}

}


////////////////////////////////////////////////////////////////////////
//辅助类定义
////////////////////////////////////////////////////////////////////////

class list_Node{  //提交列表
	int id;         //次序
	String date;    //日期
	String name;    //姓名
	String country; //国籍
	String moves;   //移动步数
	String pushs;   //推动步数
}

class state_Node{  //状态列表
	long id;
	long pid;
	long pkey;
	int moves;   //移动步数
	int pushs;   //推动步数
	String inf;
	String time;
}

class ans_Node{  //答案列表
	int r;
	int c;
	int solution;
	String ans;
	String bk_ans;
	String time;
}

class set_Node{  //关卡集列表
	long id;
	String title;
}

