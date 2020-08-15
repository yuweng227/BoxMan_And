package my.boxman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

public class myPicListViewAdapter extends BaseAdapter{

	LayoutInflater mLayoutInflater;
	int mWidth = 350, mHeight = 500;

	static class MyPicGridViewHolder{
		public ImageView imageview_thumbnail;
		public TextView textview_test;
	}

	public myPicListViewAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);  
	}
  
    @Override  
    public int getCount() {  
        return myMaps.mFile_List.size();
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
        MyPicGridViewHolder viewHolder;
        if(convertView == null){
            viewHolder = new MyPicGridViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.my_piclist_view_item, null);
            viewHolder.imageview_thumbnail = (ImageView)convertView.findViewById(R.id.m_pic_thum);
            viewHolder.textview_test = (TextView)convertView.findViewById(R.id.m_pic_text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MyPicGridViewHolder)convertView.getTag();
        }

        //先cancelPotentialLoad判断是否有线程正在为imageview加载图片资源，  
        //若有，则看此图片资源（key）是否与现在的图片资源一样，不一样则取消之前的线程（之前的下载线程作废）。  
        if (cancelPotentialLoad(position, viewHolder.imageview_thumbnail)) {
			AsyncLoadImageTask task = new AsyncLoadImageTask(viewHolder.imageview_thumbnail);  
			LoadedDrawable loadedDrawable = new LoadedDrawable(task);  
			viewHolder.imageview_thumbnail.setImageDrawable(loadedDrawable);  
			task.execute(position);
        }

        //图片文档名称
        viewHolder.textview_test.setText(myMaps.mFile_List.get(position));

        return convertView;
    }  
          
	public Bitmap getBitmapFromUrl(int key){

        Bitmap bitmap = myPicListView.picBitmapCaches.get(key);

        if (bitmap == null) {  //生成预览图
        	try {
                bitmap = getThumbnail(myMaps.mFile_List.get(key), mWidth, mHeight);
        	} catch (Exception e) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        	}
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
            myPicListView.picBitmapCaches.put(params[0], bitmap);
            return bitmap;
        }
  
        @Override
        protected void onPostExecute(Bitmap resultBitmap) {
            if (isCancelled()) {
                resultBitmap = null;
            }
            if (imageViewReference != null) {
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

    private Bitmap getThumbnail(String pathName, int width, int height) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(myMaps.sRoot + myMaps.myPathList[myMaps.m_Sets[36]] + pathName, opts);// 图片未加载进内存，但是可以读取长宽
        int oriWidth = opts.outWidth;
        int oriHeight = opts.outHeight;
        opts.inSampleSize = oriWidth / width;
        opts.inSampleSize = opts.inSampleSize > oriHeight / height ? opts.inSampleSize : oriHeight / height;
        opts.inJustDecodeBounds = false;
        Bitmap decodeFile = BitmapFactory.decodeFile(myMaps.sRoot + myMaps.myPathList[myMaps.m_Sets[36]] + pathName, opts);// 图片加载进内存
        Bitmap result = Bitmap.createScaledBitmap(decodeFile, width, height, false);
        decodeFile.recycle();
        return result;
    }

}