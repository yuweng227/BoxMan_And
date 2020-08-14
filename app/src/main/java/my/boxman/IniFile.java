package my.boxman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ini文件工具类
 * 
 * @author 衣旧  2014-11-13
 *	
 */
public class IniFile {
	
	/**
	 * 点节
	 * 
	 * @author liucf
	 *
	 */
	public class Section {

		private String name;

		private Map<String, Object> values = new LinkedHashMap<String, Object>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void set(String key, Object value) {
			values.put(key, value);
		}

		public Object get(String key) {
			return values.get(key);
		}

		public Map<String, Object> getValues() {
			return values;
		}
	}
	
	/**
	 * 换行符
	 */
	private String line_separator = null;
	
	/**
	 * 编码
	 */
	private String charSet = "UTF-8";
	
	private Map<String, Section> sections = new LinkedHashMap<String, Section>();

	/**
	 * 指定换行符
	 * 
	 * @param line_separator
	 */
	public void setLineSeparator(String line_separator){
		this.line_separator = line_separator;
	}
	
	/**
	 * 指定编码
	 * 
	 * @param charSet
	 */
	public void setCharSet(String charSet){
		this.charSet = charSet;
	}
	
	/**
	 * 设置值
	 * 
	 * @param section
	 * 			节点
	 * @param key
	 * 			属性名
	 * @param value
	 * 			属性值
	 */
	public void set(String section, String key, Object value) {
		Section sectionObject = sections.get(section);
		if (sectionObject == null)
			sectionObject = new Section();
		sectionObject.name = section;
		sectionObject.set(key, value);
		sections.put(section, sectionObject);
	}

	/**
	 * 获取节点
	 * 
	 * @param section
	 * 			节点名称
	 * @return
	 */
	public Section get(String section){
		return sections.get(section);
	}
	
	/**
	 * 获取值
	 * 
	 * @param section
	 * 			节点名称
	 * @param key
	 * 			属性名称
	 * @return
	 */
	public Object get(String section, String key) {
		return get(section, key, null);
	}

	/**
	 * 获取值
	 * 
	 * @param section
	 * 			节点名称
	 * @param key
	 * 			属性名称
	 * @param defaultValue
	 * 			如果为空返回默认值
	 * @return
	 */
	public Object get(String section, String key, String defaultValue) {
		Section sectionObject = sections.get(section);
		if (sectionObject != null) {
			Object value = sectionObject.get(key);
			if (value == null || value.toString().trim().equals(""))
				return defaultValue;
			return value;
		}
		return defaultValue;
	}
	
	/**
	 * 删除节点
	 * 
	 * @param section
	 * 			节点名称
	 */
	public void remove(String section){
		sections.remove(section);
	}
	
	/**
	 * 删除属性
	 * 
	 * @param section
	 * 			节点名称
	 * @param key
	 * 			属性名称
	 */
	public void remove(String section,String key){
		Section sectionObject = sections.get(section);
		if(sectionObject!=null)sectionObject.getValues().remove(key);
	}
	

	/**
	 * 当前操作的文件对像
	 */
	private File file = null;
	
	public IniFile(){
		
	}
	
	public IniFile(File file) {
		this.file = file;
		initFromFile(file);
	}

	public IniFile(InputStream inputStream) {
		initFromInputStream(inputStream);
	}
	
	/**
	 * 加载一个ini文件
	 * 
	 * @param file
	 */
	public void load(File file){
		this.file = file;
		initFromFile(file);
	}
	
	/**
	 * 加载一个输入流
	 * 
	 * @param inputStream
	 */
	public void load(InputStream inputStream){
		initFromInputStream(inputStream);
	}
	
	/**
	 * 写到输出流中
	 * 
	 * @param outputStream
	 */
	public void save(OutputStream outputStream){
		BufferedWriter bufferedWriter;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream,charSet));
			saveConfig(bufferedWriter);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 保存到文件
	 * 
	 * @param file
	 */
	public void save(File file){
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
			saveConfig(bufferedWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 保存到当前文件
	 */
	public void save(){
		save(this.file);
	}

	/**
	 * 从输入流初始化IniFile
	 * 
	 * @param inputStream
	 */
	private void initFromInputStream(InputStream inputStream) {
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream,charSet));
			toIniFile(bufferedReader);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从文件初始化IniFile
	 * 
	 * @param file
	 */
	private void initFromFile(File file) {
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			toIniFile(bufferedReader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从BufferedReader 初始化IniFile
	 * 
	 * @param bufferedReader
	 */
	private void toIniFile(BufferedReader bufferedReader) {
		String strLine;
		Section section = null;
		Pattern p = Pattern.compile("^\\[.*\\]$");
		try {
			while ((strLine = bufferedReader.readLine()) != null) {
				if (p.matcher((strLine)).matches()) {
					strLine = strLine.trim();
					section = new Section();
					section.name = strLine.substring(1, strLine.length() - 1);
					sections.put(section.name, section);
				} else {
					String[] keyValue = strLine.split("=");
					if (keyValue.length == 2) {
						section.set(keyValue[0], keyValue[1]);
					}
				}
			}
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 保存Ini文件
	 * 
	 * @param bufferedWriter
	 */
	private void saveConfig(BufferedWriter bufferedWriter){
		try {
			boolean line_spe = true;
			if(line_separator == null || line_separator.trim().equals("")) line_spe = false;
			for (Section section : sections.values()) {
				bufferedWriter.write("["+section.getName()+"]");
				if(line_spe)
					bufferedWriter.write(line_separator);
				else
					bufferedWriter.newLine();
				for (Map.Entry<String, Object> entry : section.getValues().entrySet()) {
					bufferedWriter.write(entry.getKey());
					bufferedWriter.write("=");
					bufferedWriter.write(entry.getValue().toString());
					if(line_spe)
						bufferedWriter.write(line_separator);
					else
						bufferedWriter.newLine();
				}
			}
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}