package com.komic.komic.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.komic.komic.R;
import com.komic.komic.mangascraper.Chapter;
import com.komic.komic.mangascraper.ChapterMangaDex;
import com.komic.komic.mangascraper.Manga;
import com.komic.komic.ui.MangaReaderFragment;
import com.komic.komic.util.Store;

import java.util.List;

public class MangaChapterAdapter extends Adapter<MangaChapterAdapter.MangaChapterViewHolder> {
    private final Manga manga;
    private final LayoutInflater mInflater;
    private final List<Chapter> chaptersList;
    private final Store store;

    class MangaChapterViewHolder extends RecyclerView.ViewHolder {
        TextView chapterItem;
        final MangaChapterAdapter mAdapter;

        public MangaChapterViewHolder(View itemView, MangaChapterAdapter adapter) {
            super(itemView);
            chapterItem = itemView.findViewById(R.id.chapter_item_button);
            this.mAdapter = adapter;

            chapterItem.setOnClickListener(v -> {
                // enter manga reader
                int position = getLayoutPosition();
                Chapter chapter = chaptersList.get(position);
                AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                navigateToChapter(chapter, manga, store, activity);
            });
        }
    }

    public static void navigateToChapter(Chapter chapter, Manga manga, Store store, AppCompatActivity activity) {
        Bundle args = new Bundle();
        args.putString("chapter", ((ChapterMangaDex) chapter).toString());
        // save reading history
        store.addMangaHistory(manga.id, chapter.toString(), manga.toString());
        // navigation
        MangaReaderFragment fragment = new MangaReaderFragment();
        fragment.setArguments(args);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public MangaChapterAdapter(Context context, List<Chapter> chaptersList, Manga manga) {
        this.manga = manga;
        store = Store.getInstance(context);
        mInflater = LayoutInflater.from(context);
        this.chaptersList = chaptersList;
    }

    @NonNull
    @Override
    public MangaChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.chapter_item, parent, false);
        return new MangaChapterViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull MangaChapterViewHolder holder, int position) {
        // set the view

        Chapter chapter = chaptersList.get(position);
        holder.chapterItem.setText(chapter.name);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return chaptersList.size();
    }
}