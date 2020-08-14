package my.boxman;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author suzhida
 * String压缩/解压
 */
public class StringCompress {

    /**
     * 压缩字符串
     *
     * @param str
     *            压缩的字符串
     * @return 压缩后的字符串
     */
    public static String compress(String str) {

        if (str.isEmpty()) {
            return str;
        }

        try {
            ByteArrayOutputStream bos = null;
            GZIPOutputStream os = null; // 使用默认缓冲区大小创建新的输出流
            byte[] bs = null;
            try {
                bos = new ByteArrayOutputStream();
                os = new GZIPOutputStream(bos);
                os.write(str.getBytes()); // 写入输出流
                os.close();
                bos.close();
                bs = bos.toByteArray();
                return new String(bs, "ISO-8859-1"); // 通过解码字节将缓冲区内容转换为字符串
            } finally {
                bs = null;
                bos = null;
                os = null;
            }
        } catch (Exception ex) {
            return str;
        }
    }

    /**
     * 解压缩字符串
     *
     * @param str
     *            解压缩的字符串
     * @return 解压后的字符串
     */
    public static String decompress(String str) {

        if (str.isEmpty()) {
            return str;
        }

        ByteArrayInputStream bis = null;
        ByteArrayOutputStream bos = null;
        GZIPInputStream is = null;
        byte[] buf = null;
        try {
            bis = new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
            bos = new ByteArrayOutputStream();
            is = new GZIPInputStream(bis); // 使用默认缓冲区大小创建新的输入流
            buf = new byte[1024];
            int len = 0;
            while ((len = is.read(buf)) != -1) { // 将未压缩数据读入字节数组
                // 将指定 byte 数组中从偏移量 off 开始的 len 个字节写入此byte数组输出流
                bos.write(buf, 0, len);
            }
            is.close();
            bis.close();
            bos.close();
            return new String(bos.toByteArray()); // 通过解码字节将缓冲区内容转换为字符串
        } catch (Exception ex) {
            return str;
        } finally {
            bis = null;
            bos = null;
            is = null;
            buf = null;
        }
    }

}
