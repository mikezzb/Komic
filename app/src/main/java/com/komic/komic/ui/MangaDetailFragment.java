package com.komic.komic.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.komic.komic.R;
import com.komic.komic.adapters.MangaChapterAdapter;
import com.komic.komic.mangascraper.Chapter;
import com.komic.komic.mangascraper.ChapterMangaDex;
import com.komic.komic.mangascraper.Manga;
import com.komic.komic.mangascraper.MangaMangaDex;
import com.komic.komic.util.Store;

import java.util.List;

public class MangaDetailFragment extends Fragment {

    private Manga manga;
    private List<Chapter> mMangaChapters;
    // Views
    private ImageView mCoverImage;
    private ProgressBar mProgressBar;
    private TextView mTitle;
    private TextView mAuthor;
    private TextView mDescription;
    private TextView mNumChapters;
    private MangaChapterAdapter mAdapter;
    private RecyclerView mRecyclerView;
    // buttons
    private ImageButton mBackButton;
    private ImageButton mBookmarkButton;
    private Button mContinue;

    private Store store;
    private boolean bookmarked = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.manga_detail, container, false);
        // get views
        mProgressBar = view.findViewById(R.id.manga_detail_progress);
        mCoverImage = view.findViewById(R.id.detail_cover);
        mTitle = view.findViewById(R.id.detail_title);
        mAuthor = view.findViewById(R.id.detail_author);
        mDescription = view.findViewById(R.id.detail_description);
        mNumChapters = view.findViewById(R.id.detail_num_chapters);
        mRecyclerView = view.findViewById(R.id.detail_chapters_container);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5));
        mBackButton = view.findViewById(R.id.detail_back_button);
        mBookmarkButton = view.findViewById(R.id.detail_bookmark_button);
        // set button actions
        mBackButton.setOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        });
        mBookmarkButton.setOnClickListener(v -> {
            toggleBookmark(false);
        });
        // get bundle
        Bundle args = getArguments();
        if (args != null) {
            String data = args.getString("manga");
            manga = MangaMangaDex.fromString(data);
        }

        // get store
        store = Store.getInstance(getContext());
        Store.MangaPreference prefs = store.getMangaPref(manga.id);
        if (prefs != null) {
            if (prefs.bookmarked) {
                toggleBookmark(true);
            }
            if (prefs.lastReadChapter != null) {
                Chapter chapter = ChapterMangaDex.fromString(prefs.lastReadChapter);
                mContinue = view.findViewById(R.id.detail_continue);
                mContinue.setVisibility(View.VISIBLE);
                mContinue.setOnClickListener(v -> {
                    MangaChapterAdapter.navigateToChapter(chapter, manga, store, (AppCompatActivity) getContext());
                });
            }
        }

        return view;
    }

    private void toggleBookmark(Boolean skipSaving) {
        bookmarked = !bookmarked;
        Drawable icon;
        if (bookmarked) {
            icon = getResources().getDrawable(R.drawable.baseline_favorite_24);
        } else {
            icon = getResources().getDrawable(R.drawable.baseline_favorite_border_24);
        }
        mBookmarkButton.setImageDrawable(icon);
        if (!skipSaving) {
            store.updateMangaPref(manga.id, "bookmarked", bookmarked, manga.toString());
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            String data = savedInstanceState.getString("manga");
            manga = MangaMangaDex.fromString(data);
        }
        if (manga == null) return;
        // set the views
        mTitle.setText(manga.title);
        mAuthor.setText(manga.author);
        mDescription.setText(manga.description);
        // cover iamge
        Uri coverImg = Uri.parse(manga.thumbnail);
        Glide.with(getContext())
                .load(coverImg)
                .into(mCoverImage);
        // fetch chapters
        new FetchChaptersTask(manga).execute();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (manga != null) {
            String data = manga.toString();
            outState.putString("manga", data);
        }
    }

    private class FetchChaptersTask extends AsyncTask<Void, Void, List<Chapter>> {
        private final Manga mManga;

        public FetchChaptersTask(Manga manga) {
            mManga = manga;
        }

        @Override
        protected List<Chapter> doInBackground(Void... voids) {
            List<Chapter> chapters = mManga.getChapters();
            return chapters;
        }

        @Override
        protected void onPostExecute(List<Chapter> chapters) {
            super.onPostExecute(chapters);
            mProgressBar.setVisibility(View.GONE);
            mMangaChapters = chapters;

            if (chapters == null) {
                Snackbar.make(mTitle, "Error", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Render the chapters
            mNumChapters.setText(getNumChaptersString(chapters.size()));
            mAdapter = new MangaChapterAdapter(getActivity(), mMangaChapters, manga);
            mRecyclerView.setAdapter(mAdapter);

        }
    }

    private static String getNumChaptersString(int size) {
        return size + " Chapters";
    }
}
