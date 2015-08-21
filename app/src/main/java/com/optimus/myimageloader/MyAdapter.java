package com.optimus.myimageloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * Created by RenTianzhu on 2015/8/21.
 */
public class MyAdapter extends BaseAdapter {

    private LayoutInflater inflater = null;
    private Context context;
    public MyAdapter(Context context){
        this.context = context;
        inflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return Images.imageThumbUrls.length;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView==null){
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_grid_view,null);
            holder.imageView = (ImageView)convertView.findViewById(R.id.image_view_for_grid);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }
        holder.imageView.setImageResource(R.mipmap.ic_launcher);
        String url = Images.imageThumbUrls[position];
        holder.imageView.setTag(url);
        ImageLoader.getInstance(context).loadImage(holder.imageView,url);
        return convertView;
    }
    static class ViewHolder{
        private ImageView imageView;
    }
}
