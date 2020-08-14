package my.boxman;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;
 
 
/**
 * 自定义Toast为解决提示框多次弹出信息时,屏一直不停弹出的问题
 * @author 小谢
 *
 */
public class MyToast {  
     
    /**
     * Show the view or text notification for a short period of time.  This time
     * could be user-definable.  This is the default.
     * @see #setDuration
     */
    public static final int LENGTH_SHORT = 0;
 
    /**
     * Show the view or text notification for a long period of time.  This time
     * could be user-definable.
     * @see #setDuration
     */
    public static final int LENGTH_LONG = 1;
     
     
    private static Toast toast;
     
    private static Handler handler = new Handler();
     
     
    private static Runnable runnable = new Runnable() {
        public void run() {
            toast.cancel();
        }
    };   
        
    /**
     * 弹出一个默认的提示,在一定时间内消失
     * @param context   上下文
     * @param text      提示字符串
     * @param duration  弹出框消失的时间,可选{@link #LENGTH_SHORT}大概2秒,或者{{@link #LENGTH_LONG}大概3.5秒
     */
    public static void showToast(Context context, String text, int duration) {   
        handler.removeCallbacks(runnable);        
        if (toast != null){
            toast.setText(text);
        }else{
            toast = Toast.makeText(context, text, duration);
        }
         
        if(duration == LENGTH_LONG){
            duration = 3000;
        }else{
            duration = 1500;
        }
         
        handler.postDelayed(runnable, duration);
        toast.show();
    }
     
    /**
     * 弹出一个默认的提示,在一定时间内消失
     * @param context   上下文
     * @param stringID      提示字符串的资源ID
     * @param duration  弹出框消失的时间,可选{@link #LENGTH_SHORT}大概2秒,或者{{@link #LENGTH_LONG}大概3.5秒
     */
    public static void showToast(Context context, int stringID, int duration) {
        showToast(context, context.getResources().getString(stringID), duration);
    }   
}