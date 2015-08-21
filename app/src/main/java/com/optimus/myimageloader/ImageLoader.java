package com.optimus.myimageloader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import libcore.io.DiskLruCache;

/**
 * Created by RenTianzhu on 2015/8/21.
 */
public class ImageLoader {
    private static ImageLoader imageLoader = null;
    private LruCache mLruCache;
    private DiskLruCache mDiskLruCache;
    private static Context context;


    private ImageLoader(){
        int maxMemory = (int)Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory/4;
        mLruCache= new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        try {
            // 获取图片缓存路径
            File cacheDir = getDiskCacheDir(context, "bitmap");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            // 创建DiskLruCache实例，初始化缓存数据
            mDiskLruCache = DiskLruCache
                    .open(cacheDir, getAppVersion(context), 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static synchronized  ImageLoader getInstance(Context context){
        ImageLoader.context = context;
        if(imageLoader == null) {
            imageLoader = new ImageLoader();
        }
        return imageLoader;
    }
    /*
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if((String)msg.obj == mImageView.getTag())
            mImageView.setImageBitmap((Bitmap)mLruCache.get((String)msg.obj));
        }
    };
    */

    public void loadImage(final ImageView imageView,final String url){
        Bitmap bitmap;
        if(mLruCache.get(url)==null) {
            bitmap = getBitmapFromDiskCache(url);
            if (bitmap == null) {
                Log.e("null","null");
                LoadImageAsyncTask loadImageAsyncTask = new LoadImageAsyncTask(imageView);
                loadImageAsyncTask.execute(url);
            } else {
                imageView.setImageBitmap(bitmap);
            }
        }else{
                imageView.setImageBitmap((Bitmap) mLruCache.get(url));
            }
        }


    private class LoadImageAsyncTask extends AsyncTask<String,Void,Bitmap>{

        private ImageView imageView;
        private String mUrl;
        public LoadImageAsyncTask(ImageView imageView){
            this.imageView = imageView;
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            mUrl = params[0];
            Bitmap bitmap = null;
            bitmap = httpLoadImage(mUrl);
            //bitmap = addBitmapToDiskCache(mUrl);
            //Log.e("Bitmap",bitmap.toString());
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
           if(bitmap!=null && imageView.getTag()==mUrl){
               imageView.setImageBitmap(bitmap);
               mLruCache.put(mUrl, bitmap);
               addBitmapToDiskCache(mUrl,bitmap);
           }
        }
    }

    private Bitmap httpLoadImage(String url) {
        Bitmap bitmap = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL realUrl = new URL(url);
            try {
                httpURLConnection = (HttpURLConnection) realUrl.openConnection();
                InputStream is = httpURLConnection.getInputStream();
                //addBitmapToDiskCache(url, is);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                httpURLConnection.disconnect();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private Bitmap getBitmapFromDiskCache(String url){
        Bitmap bitmap = null;
        InputStream is = null;
        String key = hashKeyForDisk(url);
        try {
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
            if (snapShot != null) {
                is = snapShot.getInputStream(0);
                //Log.e("SIZE",((Integer)is.read()).toString());
                bitmap = BitmapFactory.decodeStream(is);
                Log.e("size",((Integer)bitmap.getByteCount()).toString());
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if(is!=null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    private void addBitmapToDiskCache(String urlString, Bitmap bitmap) {

        final String key = hashKeyForDisk(urlString);
        BufferedOutputStream out = null;
        try {
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            OutputStream outputStream = editor.newOutputStream(0);
            //Log.e("in",((Integer)in.read()).toString());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
          //  int b;
            //while ((b = outputStream.read()) != -1) {
              //  Log.e("aaaaaaa", "aaaaaaa");
              // out.write(b);
           // }
            editor.commit();
            //mDiskLruCache.flush();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    private int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 使用MD5算法对传入的key进行加密并返回。
     */
    private String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

}
