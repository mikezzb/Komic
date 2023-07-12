package com.komic.komic.adapters;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.komic.komic.R;

public class MangaImageAdapter extends PagerAdapter {
    public static final int OFFSCREEN_PAGE_LIMIT = 3;
    private final SparseArray<View> instantiatedViews = new SparseArray<>();


    private final Context context;
    private final String[] imageUrls;
    private int scale = 1;

    public MangaImageAdapter(Context context, String[] imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }


    public void setScaleX(int scale) {
        this.scale = scale;
        for (int i = 0; i < instantiatedViews.size(); i++) {
            int key = instantiatedViews.keyAt(i);
            View view = instantiatedViews.get(key);
            ImageView imageView = view.findViewById(R.id.reader_image_view);
            imageView.setScaleX(scale);
        }
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.manga_reader_image, container, false);
        view.setTag("page_" + position);
        ImageView imageView = view.findViewById(R.id.reader_image_view);
        if (scale != 1) {
            imageView.setScaleX(scale);
        }
        Glide.with(context)
                .load(imageUrls[position])
                .into(imageView);
        container.addView(view);
        instantiatedViews.put(position, view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return imageUrls.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}