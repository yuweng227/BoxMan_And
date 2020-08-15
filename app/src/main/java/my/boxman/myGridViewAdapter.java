package my.boxman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class myGridViewAdapter extends BaseAdapter{  
  
	LayoutInflater mLayoutInflater;

	public int m_PicWidth;
    public int m_changeItem = -1;
          
	static class MyGridViewHolder{
		public ImageView imageview_thumbnail;  
		public TextView textview_test;  
		public TextView textview_test2;
	}
          
	public myGridViewAdapter(Context context) {  
        mLayoutInflater = LayoutInflater.from(context);  
	}
  
    @Override  
    public int getCount() {  
        return myMaps.m_lstMaps.size();  
    }  
  
    @Override  
    public Object getItem(int arg0) {  
        return null;  
    }  
  
    @Override  
    public long getItemId(int position) {  
        return 0;  
    }
  
    @Override  
    public View getView(int position, View convertView, ViewGroup parent) {
        MyGridViewHolder viewHolder;
        if(convertView == null){
            viewHolder = new MyGridViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.my_grid_view_item, null);  
            viewHolder.imageview_thumbnail = (ImageView)convertView.findViewById(R.id.m_image_thum);  
            viewHolder.textview_test = (TextView)convertView.findViewById(R.id.m_image_text);
            viewHolder.textview_test2 = (TextView)convertView.findViewById(R.id.m_image_text2);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (MyGridViewHolder)convertView.getTag();
        }
        if (myMaps.m_Sets[2] == 0) {
            viewHolder.textview_test.setVisibility(View.VISIBLE);
            viewHolder.textview_test2.setVisibility(View.GONE);
        } else {
            viewHolder.textview_test.setVisibility(View.GONE);
            viewHolder.textview_test2.setVisibility(View.VISIBLE);
        }
         
        //先cancelPotentialLoad判断是否有线程正在为imageview加载图片资源，  
        //若有，则看此图片资源（key）是否与现在的图片资源一样，不一样则取消之前的线程（之前的下载线程作废）。  
        if (cancelPotentialLoad(position, viewHolder.imageview_thumbnail)) {
			AsyncLoadImageTask task = new AsyncLoadImageTask(viewHolder.imageview_thumbnail);  
			LoadedDrawable loadedDrawable = new LoadedDrawable(task);  
			viewHolder.imageview_thumbnail.setImageDrawable(loadedDrawable);  
			task.execute(position);
        }

        //关卡序号、标题、作者等信息
        viewHolder.textview_test.setText("" + (position + 1));
        viewHolder.textview_test2.setText("序号：" + (position + 1) + "\n标题：" + myMaps.m_lstMaps.get(position).Title + "\n作者：" + myMaps.m_lstMaps.get(position).Author);

        //绿色标示有答案关卡
        if (myMaps.m_lstMaps.get(position).Solved) {
            viewHolder.textview_test.setBackgroundColor(0xff004700);
            viewHolder.textview_test2.setBackgroundColor(0xff004700);
        } else {
            viewHolder.textview_test.setBackgroundColor(0xff151515);
            viewHolder.textview_test2.setBackgroundColor(0xff151515);
        }

        //黄色标示被选中的关卡
        if (myMaps.isSelect && myMaps.m_lstMaps.get(position).Select) {
            viewHolder.textview_test.setBackgroundColor(0xffa06300);
            viewHolder.textview_test2.setBackgroundColor(0xffa06300);
        }

        return convertView;
    }  
          
	Rect rt = new Rect();
	Rect rt1 = new Rect();
	int m_Width = 25;
	mapNode nd;
	String[] Arr;
	public Bitmap getBitmapFromUrl(int key){

        Bitmap bitmap = myGridView.gridviewBitmapCaches.get(key);

        if (myMaps.m_lstMaps.get(key).Lock || m_changeItem == key) {
            if (m_changeItem == key) {  //“锁”图标的处理
                m_changeItem = -1;
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                System.gc();
                bitmap = null;
            }
            if (myMaps.m_lstMaps.get(key).Lock) {
                bitmap = Bitmap.createBitmap(m_PicWidth, m_PicWidth, myMaps.cfg);  //皮肤图片（50*50像素的小图，横4纵7排列）
                Canvas cvs99 = new Canvas(bitmap);
                Drawable dw99 = myMaps.res.getDrawable(R.drawable.lock);
                dw99.setBounds(0, 0, m_PicWidth, m_PicWidth);
                dw99.draw(cvs99);
            }
        }

        if (bitmap == null) {  //生成预览图
        	try {
	    		nd = myMaps.m_lstMaps.get(key);
				if(nd.Map.equals("--"))
				    throw new Exception();

                Arr = nd.Map.split("\r\n|\n\r|\n|\\|");
                int height = m_Width * nd.Rows;
                int width = m_Width * nd.Cols;

                bitmap = Bitmap.createBitmap(width, height, myMaps.cfg);  //为每个关卡创建图标
                Canvas cvs = new Canvas(bitmap);

                //画关卡图
                rt.top = 0;
                for (int i = 0; i < nd.Rows; i++) {
                    rt.bottom = rt.top + m_Width;
                    rt.left = 0;
                    for (int j = 0; j < nd.Cols; j++) {
                        rt.right = rt.left + m_Width;
                        switch (Arr[i].charAt(j)) {
                            case '_':
                                break;
                            case '#':
                                rt1.set(0, 0, 50, 50);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                break;
                            case '-':
                                rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                break;
                            case '.':
                                rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                break;
                            case '$':
                                rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                rt1.set(50, 250-myMaps.isSkin_200, 100, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                break;
                            case '*':
                                rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                rt1.set(50, 300-myMaps.isSkin_200, 100, 350-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                break;
                            case '@':
                                rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                rt1.set(100, 250-myMaps.isSkin_200, 150, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                break;
                            case '+':
                                rt1.set(0, 250-myMaps.isSkin_200, 50, 300-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                rt1.set(0, 300-myMaps.isSkin_200, 50, 350-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                                rt1.set(100, 300-myMaps.isSkin_200, 150, 350-myMaps.isSkin_200);
                                cvs.drawBitmap(myMaps.skinBit, rt1, rt, null);
                        } //end switch
                        rt.left = rt.left + m_Width;
                    }  //end for j
                    rt.top = rt.top + m_Width;
                } //end for i
        	} catch (Exception e) {
                bitmap = Bitmap.createBitmap(m_PicWidth, m_PicWidth, myMaps.cfg);  //皮肤图片（50*50像素的小图，横4纵7排列）
    			Canvas cvs99 = new Canvas(bitmap);
    			Drawable dw99 = myMaps.res.getDrawable(R.drawable.defbit);
    			dw99.setBounds(0, 0, m_PicWidth, m_PicWidth);
    			dw99.draw(cvs99);
        	}
        	bitmap = getBitmapThumbnail(bitmap, m_PicWidth, myMaps.m_Sets[12] == 1 && mySQLite.m_SQL.find_Level(myMaps.m_lstMaps.get(key).key, myMaps.m_lstMaps.get(key).Level_id) > -1 && !myMaps.sFile.equals("创编关卡"));
        }
        return bitmap;
    }  

    //加载图片的异步任务          
    private class AsyncLoadImageTask extends AsyncTask<Integer, Void, Bitmap>{
        private Integer key;
        private final WeakReference<ImageView> imageViewReference;  
         
        public AsyncLoadImageTask(ImageView imageview) {  
            super();  
            imageViewReference = new WeakReference<ImageView>(imageview);
        }
  
        @Override
        protected Bitmap doInBackground(Integer... params) {
            this.key = params[0];
            Bitmap bitmap = getBitmapFromUrl(params[0]);
            myGridView.gridviewBitmapCaches.put(params[0], bitmap);
            return bitmap;
        }
  
        @Override
        protected void onPostExecute(Bitmap resultBitmap) {
            if(isCancelled()){
                resultBitmap = null;
            }
            if(imageViewReference != null){
                ImageView imageview = imageViewReference.get();
                AsyncLoadImageTask loadImageTask = getAsyncLoadImageTask(imageview);
                if (this == loadImageTask) {
                    imageview.setImageBitmap(resultBitmap);
                    imageview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
            }
            super.onPostExecute(resultBitmap);
        }                                                          
    }  
          
    private boolean cancelPotentialLoad(Integer key,ImageView imageview){
        AsyncLoadImageTask loadImageTask = getAsyncLoadImageTask(imageview);  
  
        if (loadImageTask != null) {  
            Integer bitmapUrl = loadImageTask.key;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(key))) {  
                loadImageTask.cancel(true);                          
            } else {  
                // 相同的key已经在加载中.  
                return false;  
            }  
        }  
        return true;  
    }  
          
    //当 loadImageTask.cancel(true)被执行的时候，则AsyncLoadImageTask 就会被取消，  
    //当AsyncLoadImageTask 任务执行到onPostExecute的时候，如果这个任务加载到了图片，  
    //它也会把这个bitmap设为null了。  
    private AsyncLoadImageTask getAsyncLoadImageTask(ImageView imageview){  
        if (imageview != null) {  
        	Drawable drawable = imageview.getDrawable();  
            if (drawable instanceof LoadedDrawable) {  
                LoadedDrawable loadedDrawable = (LoadedDrawable)drawable;  
                return loadedDrawable.getLoadImageTask();  
            }  
        }  
        return null;  
    }  
  
    //该类功能为：记录imageview加载任务并且为imageview设置默认的drawable  
    public static class LoadedDrawable extends ColorDrawable{  
        private final WeakReference<AsyncLoadImageTask> loadImageTaskReference;  
  
        public LoadedDrawable(AsyncLoadImageTask loadImageTask) {  
            super(Color.TRANSPARENT);  
            loadImageTaskReference = new WeakReference<AsyncLoadImageTask>(loadImageTask);  
        }  
  
        public AsyncLoadImageTask getLoadImageTask() {  
            return loadImageTaskReference.get();  
        }  
    }
    
    //压缩图片尺寸（缩略图）
    public static Bitmap getBitmapThumbnail(Bitmap bmp, int width, boolean flg){  //, int height
        Bitmap bitmap = null;  
        if(bmp != null ){  
            int bmpWidth = bmp.getWidth();
            int bmpHeight = bmp.getHeight();
            if(width != 0){   //&& height !=0
                Matrix matrix = new Matrix();
                float scaleWidth = ((float) width / bmpWidth);
                float scaleHeight = scaleWidth;//((float) height / bmpHeight);
                matrix.postScale(scaleWidth, scaleHeight);
                bitmap = Bitmap.createBitmap(bmp, 0, 0, bmpWidth, bmpHeight, matrix, true);
				if (bmp != null) bmp.recycle();
            }else{  
                bitmap = bmp;
            }
            if (flg){  //对重复关卡预览图加红框
            	Canvas cv = new Canvas( bitmap );
            	myMaps.myPaint.setARGB(127, 255, 0, 0);
            	myMaps.myPaint.setStyle(Paint.Style.STROKE);
            	myMaps.myPaint.setStrokeWidth(15);
            	cv.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), myMaps.myPaint);
            }
        }  
        return bitmap;  
    }

}