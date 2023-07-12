package com.komic.komic.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.komic.komic.R;
import com.komic.komic.adapters.MangaAdapter;
import com.komic.komic.mangascraper.Manga;
import com.komic.komic.mangascraper.MangaParser;
import com.komic.komic.mangascraper.ParserMangaDex;
import com.komic.komic.util.Store;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements SearchView.OnQueryTextListener {
    private MangaAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefresh;
    private View view;
    private Toolbar mToolbar;

    private List<Manga> mMangaList = new ArrayList<>();
    private String currentQuery = null;
    private static Store store;
    private ListMode currentMode = ListMode.POPULAR;
    private ListMode savedMode = null;
    private String savedQuery;
    private Menu menu;
    private int pageNum = 0;
    MangaParser parser = null;

    private enum ListMode {
        POPULAR("Popular"),
        RECENT("Recent"),
        BOOKMARK("Bookmark"),
        DOWNLOAD("Download"),
        SEARCH("SEARCH");

        private final String stringValue;

        ListMode(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);
        // restore saved states
        if (savedInstanceState != null) {
            savedQuery = savedInstanceState.getString("currentQuery");
            savedMode = (ListMode) savedInstanceState.getSerializable("mode");
        }

        mRecyclerView = view.findViewById(R.id.manga_list);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1) && newState==RecyclerView.SCROLL_STATE_IDLE) {
                    if (currentMode == ListMode.SEARCH || currentMode == ListMode.POPULAR) {
                        new FetchMangaTask(currentQuery).execute();
                    }
                }
            }
        });

        mProgressBar = view.findViewById(R.id.manga_list_progress);
        mSwipeRefresh = view.findViewById(R.id.home_swipe);
        mSwipeRefresh.setOnRefreshListener(() -> {
            fetchManga(currentQuery);
        });


        mToolbar = view.findViewById(R.id.home_search_view);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        setHasOptionsMenu(true);


        // load store
        store = Store.getInstance(getContext());
        return view;
    }

    private void fetchManga(String query) {
        pageNum = 0;
        currentQuery = query;
        new FetchMangaTask(query).execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_popular:
                switchListMode(ListMode.POPULAR);
                return true;
            case R.id.filter_recent:
                switchListMode(ListMode.RECENT);
                return true;
            case R.id.filter_bookmark:
                switchListMode(ListMode.BOOKMARK);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void switchListMode(ListMode mode) {
        currentMode = mode;
        switch (mode) {
            case POPULAR:
                fetchManga(null);
                break;
            case RECENT:
                switchMangaList(store.getMangaHistory(), false);
                break;
            case BOOKMARK:
                switchMangaList(store.getBookmarkedManga(), false);
                break;
        }
        mToolbar.setTitle(mode.stringValue);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        fetchManga(query);
        currentMode = ListMode.SEARCH;
        mToolbar.setTitle(query);
        return false;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", currentQuery);
        outState.putSerializable("mode", currentMode);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            savedQuery = savedInstanceState.getString("query");
            savedMode = (ListMode) savedInstanceState.getSerializable("mode");
        }
    }


    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedMode != null) {
            // recover
            if (savedMode == ListMode.SEARCH) {
                onQueryTextSubmit(savedQuery);
            } else {
                switchListMode(savedMode);
            }
            savedMode = null;
            savedQuery = null;
        } else {
            switchListMode(ListMode.POPULAR);
        }
    }

    private class FetchMangaTask extends AsyncTask<Void, Void, List<Manga>> {
        private final String query;

        public FetchMangaTask(String query) {
            this.query = query;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }


        @Override
        protected List<Manga> doInBackground(Void... voids) {
            if (parser == null) {
                parser = ParserMangaDex.getInstance("en");
            }
            List<Manga> mangaList;
            if (pageNum > 0) {
                mangaList = parser.nextPage();
            } else {
                mangaList = parser.searchManga(query);
            }
            return mangaList;
        }

        @Override
        protected void onPostExecute(List<Manga> mangaList) {
            super.onPostExecute(mangaList);
            if (mangaList == null) {
                Snackbar.make(view, "Error", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (pageNum == 0) {
                switchMangaList(mangaList, true);
            } else {
                mSwipeRefresh.setEnabled(true);
                mMangaList.addAll(mangaList);
                mAdapter.notifyDataSetChanged();
            }
            pageNum++;
            mProgressBar.setVisibility(View.GONE);
            mSwipeRefresh.setRefreshing(false);
        }
    }

    private void switchMangaList(List<Manga> mangaList, boolean enableRefresh) {
        pageNum = 0;
        mSwipeRefresh.setEnabled(enableRefresh);
        this.mMangaList = mangaList;
        mAdapter = new MangaAdapter(getActivity(), mMangaList);
        mRecyclerView.setAdapter(mAdapter);
    }
}
