/**
 * Copyright (c) 2012-2013, Michael Yang 杨福海 (www.yangfuhai.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tsz.afinal;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.tsz.afinal.bitmap.core.BitmapCommonUtils;
import net.tsz.afinal.bitmap.core.BitmapDisplayConfig;
import net.tsz.afinal.bitmap.core.BitmapProcess;
import net.tsz.afinal.bitmap.core.BitmapCache;
import net.tsz.afinal.bitmap.display.Displayer;
import net.tsz.afinal.bitmap.display.SimpleDisplayer;
import net.tsz.afinal.bitmap.download.Downloader;
import net.tsz.afinal.bitmap.download.SimpleHttpDownloader;

public class FinalBitmap {
	

	private FinalBitmapConfig mConfig;
	private BitmapCache mImageCache;

	private boolean mExitTasksEarly = false;
	private boolean mPauseWork = false;
	private final Object mPauseWorkLock = new Object();
	private Context mContext;
	
	private ExecutorService bitmapLoadAndDisplayExecutor;

	////////////////////////// config method start////////////////////////////////////
	public FinalBitmap(Context context) {
		mContext = context;
		mConfig = new FinalBitmapConfig(context);
		
		configDiskCachePath(BitmapCommonUtils.getDiskCacheDir(context, "afinalCache"));//配置缓存路径
		configDisplayer(new SimpleDisplayer());//配置显示器
		configDownlader(new SimpleHttpDownloader());//配置下载器
	}
	
	/**
	 * 设置图片正在加载的时候显示的图片
	 * @param bitmap
	 */
	public FinalBitmap configLoadingImage(Bitmap bitmap) {
		mConfig.defaultDisplayConfig.setLoadingBitmap(bitmap);
		return this;
	}

	/**
	 * 设置图片正在加载的时候显示的图片
	 * @param bitmap
	 */
	public FinalBitmap configLoadingImage(int resId) {
		mConfig.defaultDisplayConfig.setLoadingBitmap(BitmapFactory.decodeResource(mContext.getResources(), resId));
		return this;
	}
	
	/**
	 * 设置图片加载失败时候显示的图片
	 * @param bitmap
	 */
	public FinalBitmap configLoadfailImage(Bitmap bitmap) {
		mConfig.defaultDisplayConfig.setLoadfailBitmap(bitmap);
		return this;
	}
	
	/**
	 * 设置图片加载失败时候显示的图片
	 * @param resId
	 */
	public FinalBitmap configLoadfailImage(int resId) {
		mConfig.defaultDisplayConfig.setLoadfailBitmap(BitmapFactory.decodeResource(mContext.getResources(), resId));
		return this;
	}
	
	/**
	 * 配置磁盘缓存路径
	 * @param strPath
	 * @return
	 */
	public FinalBitmap configDiskCachePath(String strPath){
		if(!TextUtils.isEmpty(strPath)){
			mConfig.cachePath = strPath;
		}
		return this;
	}
	
	/**
	 * 配置磁盘缓存路径
	 * @param strPath
	 * @return
	 */
	public FinalBitmap configDiskCachePath(File pathFile){
		if(pathFile!=null)
			configDiskCachePath(pathFile.getAbsolutePath());
		return this;
	}
	
	/**
	 * 配置默认图片的小的高度
	 * @param bitmapHeight
	 */
	public FinalBitmap configBitmapMaxHeight(int bitmapHeight){
		mConfig.defaultDisplayConfig.setBitmapHeight(bitmapHeight);
		return this;
	}
	
	/**
	 * 配置默认图片的小的宽度
	 * @param bitmapHeight
	 */
	public FinalBitmap configBitmapMaxWidth(int bitmapWidth){
		mConfig.defaultDisplayConfig.setBitmapWidth(bitmapWidth);
		return this;
	}
	
	/**
	 * 设置下载器，比如通过ftp或者其他协议去网络读取图片的时候可以设置这项
	 * @param downlader
	 * @return
	 */
	public FinalBitmap configDownlader(Downloader downlader){
		mConfig.downloader = downlader;
		return this;
	}
	
	/**
	 * 设置显示器，比如在显示的过程中显示动画等
	 * @param displayer
	 * @return
	 */
	public FinalBitmap configDisplayer(Displayer displayer){
		mConfig.displayer = displayer;
		return this;
	}
	
	/**
	 * 配置内存缓存大小 大于2MB以上有效
	 * @param size 缓存大小
	 */
	public FinalBitmap configMemoryCacheSize(int size){
		mConfig.memCacheSize = size;
		return this;
	}
	
	/**
	 * 设置应缓存的在APK总内存的百分比，优先级大于configMemoryCacheSize
	 * @param percent 百分比，值的范围是在 0.05 到 0.8之间
	 */
	public FinalBitmap configMemoryCachePercent(float percent){
		mConfig.memCacheSizePercent = percent;
		return this;
	}
	
	/**
	 * 设置磁盘缓存大小 5MB 以上有效
	 * @param size
	 */
	public FinalBitmap configDiskCacheSize(int size){
		mConfig.diskCacheSize = size;
		return this;
	} 
	
	/**
	 * 配置原始图片缓存大小（非压缩缓存）
	 * @param size
	 */
	public FinalBitmap configOriginalDiskCacheSize(int size){
		mConfig.diskCacheSize = size;
		return this;
	}
	
	/**
	 * 设置加载图片的线程并发数量
	 * @param size
	 */
	public FinalBitmap configBitmapLoadThreadSize(int size){
		if(size >= 1)
			mConfig.poolSize = size;
		return this;
	}
	
	
	public FinalBitmap init(){
		
		mConfig.init();
		
		BitmapCache.ImageCacheParams imageCacheParams = new BitmapCache.ImageCacheParams(mConfig.cachePath);
		if(mConfig.memCacheSizePercent>0.05 && mConfig.memCacheSizePercent<0.8){
			imageCacheParams.setMemCacheSizePercent(mContext, mConfig.memCacheSizePercent);
		}else{
			if(mConfig.memCacheSize > 1024 * 1024 * 2){
				imageCacheParams.setMemCacheSize(mConfig.memCacheSize);	
			}else{
				//设置默认的内存缓存大小
				imageCacheParams.setMemCacheSizePercent(mContext, 0.3f);
			}
		}
		if(mConfig.diskCacheSize > 1024 * 1024 * 5)
			imageCacheParams.setDiskCacheSize(mConfig.diskCacheSize);
		mImageCache = new BitmapCache(imageCacheParams);
		
		bitmapLoadAndDisplayExecutor = Executors.newFixedThreadPool(mConfig.poolSize,new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				// 设置线程的优先级别，让线程先后顺序执行（级别越高，抢到cpu执行的时间越多）
				t.setPriority(Thread.NORM_PRIORITY - 1);
				return t;
			}
		});
		
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_INIT_DISK_CACHE);
		
		return this;
	}
	
	////////////////////////// config method end////////////////////////////////////
	
	public void display( ImageView imageView,String uri){
		doDisplay(imageView,uri,null);
	}

	

	public void display(ImageView imageView,String uri,int imageWidth,int imageHeight){
		BitmapDisplayConfig displayConfig = getDisplayConfig();
		displayConfig.setBitmapHeight(imageHeight);
		displayConfig.setBitmapWidth(imageWidth);
		
		doDisplay(imageView,uri,displayConfig);
	}
	
	public void display(ImageView imageView,String uri,Bitmap loadingBitmap){
		BitmapDisplayConfig displayConfig = getDisplayConfig();
		displayConfig.setLoadingBitmap(loadingBitmap);
		
		doDisplay(imageView,uri,displayConfig);
	}
	
	
	public void display(ImageView imageView,String uri,Bitmap loadingBitmap,Bitmap laodfailBitmap){
		BitmapDisplayConfig displayConfig = getDisplayConfig();
		displayConfig.setLoadingBitmap(loadingBitmap);
		displayConfig.setLoadfailBitmap(laodfailBitmap);
		
		doDisplay(imageView,uri,displayConfig);
	}
	
	public void display(ImageView imageView,String uri,int imageWidth,int imageHeight,Bitmap loadingBitmap,Bitmap laodfailBitmap){
		BitmapDisplayConfig displayConfig = getDisplayConfig();
		displayConfig.setBitmapHeight(imageHeight);
		displayConfig.setBitmapWidth(imageWidth);
		displayConfig.setLoadingBitmap(loadingBitmap);
		displayConfig.setLoadfailBitmap(laodfailBitmap);
		
		doDisplay(imageView,uri,displayConfig);
	}
	
	
	public void display( ImageView imageView,String uri,BitmapDisplayConfig config){
		doDisplay(imageView,uri,config);
	}
	
	
	private void doDisplay(ImageView imageView, String uri, BitmapDisplayConfig displayConfig) {
		if (TextUtils.isEmpty(uri) || imageView == null) {
			return;
		}
		
		if(displayConfig == null)
			displayConfig = mConfig.defaultDisplayConfig;
	
		Bitmap bitmap = null;
	
		if (mImageCache != null) {
			bitmap = mImageCache.getBitmapFromMemCache(uri);
		}
	
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
			
		}else if (checkImageTask(uri, imageView)) {
			final BitmapLoadAndDisplayTask task = new BitmapLoadAndDisplayTask(imageView,uri,displayConfig);
			//设置默认图片
			final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), displayConfig.getLoadingBitmap(), task);
	        imageView.setImageDrawable(asyncDrawable);
	
	        bitmapLoadAndDisplayExecutor.submit(task);
	    }
	}
	
	
	private BitmapDisplayConfig getDisplayConfig(){
		BitmapDisplayConfig config = new BitmapDisplayConfig();
		config.setAnimation(mConfig.defaultDisplayConfig.getAnimation());
		config.setAnimationType(mConfig.defaultDisplayConfig.getAnimationType());
		config.setBitmapHeight(mConfig.defaultDisplayConfig.getBitmapHeight());
		config.setBitmapWidth(mConfig.defaultDisplayConfig.getBitmapWidth());
		config.setLoadfailBitmap(mConfig.defaultDisplayConfig.getLoadfailBitmap());
		config.setLoadingBitmap(mConfig.defaultDisplayConfig.getLoadingBitmap());
		return config;
	}


	private void initDiskCacheInternal() {
		if (mImageCache != null) {
			mImageCache.initDiskCache();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.initHttpDiskCache();
		}
	}

	private void clearCacheInternal() {
		if (mImageCache != null) {
			mImageCache.clearCache();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.clearCacheInternal();
		}
	}

	private void flushCacheInternal() {
		if (mImageCache != null) {
			mImageCache.flush();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.flushCacheInternal();
		}
	}

	private void closeCacheInternal() {
		if (mImageCache != null) {
			mImageCache.close();
			mImageCache = null;
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.clearCacheInternal();
		}
	}

	/**
	 * 网络加载bitmap
	 * @param data
	 * @return
	 */
	private Bitmap processBitmap(String uri,BitmapDisplayConfig config) {
		if (mConfig != null && mConfig.bitmapProcess != null) {
			return mConfig.bitmapProcess.processBitmap(uri,config);
		}
		return null;
	}

	/**
	 * 清除缓存
	 */
	public void clearCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLEAR);
	}

	/**
	 * 刷新缓存
	 */
	public void flushCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_FLUSH);
	}

	/**
	 * 关闭缓存
	 */
	public void closeCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLOSE);
	}

	/**
	 * 退出正在加载的线程，程序退出的时候调用词方法
	 * @param exitTasksEarly
	 */
	public void exitTasksEarly(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
		if(exitTasksEarly)
			pauseWork(false);//让暂停的线程结束
	}

	/**
	 * 暂停正在加载的线程，监听listview或者gridview正在滑动的时候条用词方法
	 * @param pauseWork true停止暂停线程，false继续线程
	 */
	public void pauseWork(boolean pauseWork) {
		synchronized (mPauseWorkLock) {
			mPauseWork = pauseWork;
			if (!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

	 private static class AsyncDrawable extends BitmapDrawable {
	        private final WeakReference<BitmapLoadAndDisplayTask> bitmapWorkerTaskReference;

	        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapLoadAndDisplayTask bitmapWorkerTask) {
	            super(res, bitmap);
	            bitmapWorkerTaskReference = new WeakReference<BitmapLoadAndDisplayTask>(bitmapWorkerTask);
	        }

	        public BitmapLoadAndDisplayTask getBitmapWorkerTask() {
	            return bitmapWorkerTaskReference.get();
	        }
	  }
	 
	    
	   /**
	    * 获取imageview中正在执行的任务
	    * @param imageView
	    * @return
	    */
	    private static BitmapLoadAndDisplayTask getBitmapWorkerTask(ImageView imageView) {
	        if (imageView != null) {
	            final Drawable drawable = imageView.getDrawable();
	            if (drawable instanceof AsyncDrawable) {
	                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
	                return asyncDrawable.getBitmapWorkerTask();
	            }
	        }
	        return null;
	    }

	    /**
	     * 检测 imageView中是否已经有线程在运行
	     * @param data
	     * @param imageView
	     * @return true 没有 false 有线程在运行了
	     */
	    public static boolean checkImageTask(String data, ImageView imageView) {
	        final BitmapLoadAndDisplayTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
	        if (bitmapWorkerTask != null) {
	            final String bitmapData = bitmapWorkerTask.uriData;
	            return bitmapData == null || !bitmapData.equals(data);
	        }
	        return true;
	    }

	/**
	 * @title bitmap下载显示的线程
	 * @description 负责下载（或从sdcard加载）bitmap 并显示在imageview上
	 * @company 探索者网络工作室(www.tsz.net)
	 * @author michael Young (www.YangFuhai.com)
	 * @version 1.0
	 * @created 2012-10-28
	 */
	private class BitmapLoadAndDisplayTask implements Runnable {
		private final String uriData;
		private final WeakReference<ImageView> imageViewReference;
		private final BitmapDisplayConfig bitmapDisplayConfig;

		private Handler mhander = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0://加载成功
					final Bitmap bm = mImageCache.getBitmapFromMemCache(uriData.toString());
					final ImageView iv = getAttachedImageView();
					if (iv != null && bm != null) {
						mConfig.displayer.loadCompletedisplay(iv, bm,bitmapDisplayConfig);
					}
					break;
				case 1: //加载失败
					final Bitmap failBitmap = bitmapDisplayConfig.getLoadfailBitmap();
					final ImageView failIv = getAttachedImageView();
					if (failIv != null && failBitmap != null) {
						mConfig.displayer.loadFailDisplay(failIv, failBitmap);
					}
					break;
				default:
					break;
				}
			}
		};

		public BitmapLoadAndDisplayTask(ImageView imageView, String uriData,BitmapDisplayConfig displayConfig) {
			this.imageViewReference = new WeakReference<ImageView>(imageView);
			this.uriData = uriData;
			this.bitmapDisplayConfig = displayConfig;
		}

		@Override
		public void run() {

			synchronized (mPauseWorkLock) {
				while (mPauseWork) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
					}
				}
			}

			Bitmap bitmap = null;
			
			if ( mImageCache != null && getAttachedImageView()!=null &&  !mExitTasksEarly) {
				bitmap = mImageCache.getBitmapFromDiskCache(uriData);
			}
			
			if (bitmap == null && getAttachedImageView()!=null && !mExitTasksEarly) {
				bitmap = processBitmap(uriData,bitmapDisplayConfig);
			}
			
			if ( bitmap != null && mImageCache != null) {
				mImageCache.addBitmapToCache(uriData, bitmap);
				mhander.sendEmptyMessage(0);
			}else{
				mhander.sendEmptyMessage(1);
			}
			
		}
		
		 private ImageView getAttachedImageView() {
	            final ImageView imageView = imageViewReference.get();
	            final BitmapLoadAndDisplayTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

	            if (this == bitmapWorkerTask) {
	                return imageView;
	            }

	            return null;
	        }
		
	}
	

	/**
	 * @title 缓存操作的异步任务
	 * @description 操作缓存
	 * @company 探索者网络工作室(www.tsz.net)
	 * @author michael Young (www.YangFuhai.com)
	 * @version 1.0
	 * @created 2012-10-28
	 */
	private class CacheExecutecTask extends AsyncTask<Object, Void, Void> {
		public static final int MESSAGE_CLEAR = 0;
		public static final int MESSAGE_INIT_DISK_CACHE = 1;
		public static final int MESSAGE_FLUSH = 2;
		public static final int MESSAGE_CLOSE = 3;
		@Override
		protected Void doInBackground(Object... params) {
			switch ((Integer) params[0]) {
			case MESSAGE_CLEAR:
				clearCacheInternal();
				break;
			case MESSAGE_INIT_DISK_CACHE:
				initDiskCacheInternal();
				break;
			case MESSAGE_FLUSH:
				flushCacheInternal();
				break;
			case MESSAGE_CLOSE:
				closeCacheInternal();
				break;
			}
			return null;
		}
	}
	
	
	/**
	 * @title 配置信息
	 * @description FinalBitmap的配置信息
	 * @company 探索者网络工作室(www.tsz.net)
	 * @author michael Young (www.YangFuhai.com)
	 * @version 1.0
	 * @created 2012-10-28
	 */
	private class FinalBitmapConfig {

		public String cachePath;

		 public Displayer displayer;
		 public Downloader downloader;
		 public BitmapProcess bitmapProcess;
		 public BitmapDisplayConfig defaultDisplayConfig;
		 public float memCacheSizePercent;//缓存百分比，android系统分配给每个apk内存的大小
		 public int memCacheSize;//内存缓存百分比
		 public int diskCacheSize;//磁盘百分比
		 public int poolSize = 3;//默认的线程池线程并发数量
		 public int originalDiskCache = 20 * 1024 * 1024;//20MB
		 
		
		 public FinalBitmapConfig(Context context) {
				defaultDisplayConfig = new BitmapDisplayConfig();
				
				defaultDisplayConfig.setAnimation(null);
				defaultDisplayConfig.setAnimationType(BitmapDisplayConfig.AnimationType.fadeIn);
				
				//设置图片的显示最大尺寸（为屏幕的大小,默认为屏幕宽度的1/3）
				DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
				int defaultWidth = (int)Math.floor(displayMetrics.widthPixels/3);
				defaultDisplayConfig.setBitmapHeight(defaultWidth);
				defaultDisplayConfig.setBitmapWidth(defaultWidth);
				
		}

		 public void init() {
			if(downloader==null)
				downloader = new SimpleHttpDownloader();
			
			if(displayer==null)
				displayer = new SimpleDisplayer();
			
			bitmapProcess = new BitmapProcess(downloader,cachePath,originalDiskCache);
		}

	}
	
}
