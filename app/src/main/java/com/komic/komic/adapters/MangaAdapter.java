package com.komic.komic.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.bumptech.glide.Glide;
import com.komic.komic.R;
import com.komic.komic.mangascraper.Manga;
import com.komic.komic.mangascraper.MangaMangaDex;
import com.komic.komic.ui.MangaDetailFragment;

import java.util.List;

public class MangaAdapter extends Adapter<MangaAdapter.MangaViewHolder> {
    private final Context context;
    private final LayoutInflater mInflater;
    private final List<Manga> mMangaList;

    class MangaViewHolder extends RecyclerView.ViewHolder {

        ImageView mangaCover;
        TextView mangaTitle;

        final MangaAdapter mAdapter;

        public MangaViewHolder(View itemView, MangaAdapter adapter) {
            super(itemView);
            mangaCover = itemView.findViewById(R.id.manga_list_image);
            mangaTitle = itemView.findViewById(R.id.manga_list_title);
            this.mAdapter = adapter;

            mangaCover.setOnClickListener(v -> {
                // parse chapter detail and enter chapter detail page
                int position = getLayoutPosition();
                Manga manga = mMangaList.get(position);
                // pass bundle
                Bundle args = new Bundle();
                args.putString("manga", ((MangaMangaDex) manga).toString());
                // navigate
                MangaDetailFragment fragment = new MangaDetailFragment();
                fragment.setArguments(args);
                AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.add(R.id.container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            });
        }
    }

    public MangaAdapter(Context context, List<Manga> mangaList) {
        this.mMangaList = mangaList;
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.manga_listitem, parent, false);
        return new MangaViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {
        Manga manga = mMangaList.get(position);
        Uri coverImg = Uri.parse(manga.thumbnail);
        Glide.with(context)
                .load(coverImg)
                .into(holder.mangaCover);
        // holder.mangaCover.setImageURI(coverImg);
        holder.mangaTitle.setText(manga.title);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mMangaList.size();
    }
}