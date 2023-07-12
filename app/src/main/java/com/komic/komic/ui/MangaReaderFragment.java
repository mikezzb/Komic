package com.komic.komic.ui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.komic.komic.MainActivity;
import com.komic.komic.R;
import com.komic.komic.adapters.MangaImageAdapter;
import com.komic.komic.mangascraper.Chapter;
import com.komic.komic.mangascraper.ChapterMangaDex;
import com.komic.komic.translator.MLKitTextRecognizer;
import com.komic.komic.util.Store;

public class MangaReaderFragment extends Fragment {
    private Store store;

    private ProgressBar mProgressBar;
    private String[] mImageUrls;
    private Chapter mChapter;
    private ViewPager mViewPager;
    private MangaImageAdapter mAdapter;
    private Boolean prevActionIsDown = false;
    private View mBottomBar;
    private View mToolbar;

    // buttons
    private Button mTranslateBtn;
    private Button mL2RBtn;
    private Button mR2LBtn;
    private Button mBrightnessBtn;
    // page num
    private SeekBar mSeekBar;
    private SeekBar mBrightnessBar;
    private TextView mPageNum;
    private ImageButton mNavBtn;
    private TextView mNavText;
    private int currentPage;
    private int totalPages;
    private MainActivity mainActivity;

    private enum PagerDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        VERTICAL_SCROLL
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }


    private void setCurrentPage(int pageNum) {
        currentPage = pageNum;
        setPageNumText(pageNum);
        // set seekbar
        mSeekBar.setProgress(currentPage);
    }

    private void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        // set seekbar
        mSeekBar.setMax(totalPages);
    }

    private void setPageNumText(int pageNum) {
        mPageNum.setText(pageNum + "/" + totalPages);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.manga_reader, container, false);
        mProgressBar = view.findViewById(R.id.manga_reader_progress);
        mViewPager = view.findViewById(R.id.reader_view_pager);
        mPageNum = view.findViewById(R.id.reader_page_num);
        mBrightnessBar = view.findViewById(R.id.brightness_seekbar);
        mBrightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setViewBrightness(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekBar = view.findViewById(R.id.reader_seekbar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // fake change the text only, do NOT switch image cuz it's inefficient
                setPageNumText(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int finalValue = seekBar.getProgress();
                setCurrentPage(finalValue);
                mViewPager.setCurrentItem(finalValue - 1);
            }
        });
        mViewPager.setOffscreenPageLimit(MangaImageAdapter.OFFSCREEN_PAGE_LIMIT);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setCurrentPage(position + 1);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        mBottomBar = view.findViewById(R.id.reader_bottom_bar);
        mToolbar = view.findViewById(R.id.reader_toolbar);
        // buttons
        mTranslateBtn = view.findViewById(R.id.reader_translate_btn);
        mTranslateBtn.setOnClickListener(this::onTranslateClicked);
        mL2RBtn = view.findViewById(R.id.reader_l2r_btn);
        mL2RBtn.setOnClickListener(v -> setPagerDirection(PagerDirection.LEFT_TO_RIGHT));
        mR2LBtn = view.findViewById(R.id.reader_r2l_btn);
        mR2LBtn.setOnClickListener(v -> setPagerDirection(PagerDirection.RIGHT_TO_LEFT));
        mBrightnessBtn = view.findViewById(R.id.reader_brightness_btn);
        mBrightnessBtn.setOnClickListener(v -> {
            if (mBrightnessBar.getVisibility() == View.VISIBLE) {
                mBrightnessBar.setVisibility(View.INVISIBLE);
            } else {
                mBrightnessBar.setVisibility(View.VISIBLE);
            }
        });

        // args
        Bundle args = getArguments();
        if (args != null) {
            String data = args.getString("chapter");
            mChapter = ChapterMangaDex.fromString(data);
        }

        // toolbar
        mNavBtn = view.findViewById(R.id.reader_nav_btn);
        mNavText = view.findViewById(R.id.reader_nav_title);
        mNavText.setText("Chapter " + mChapter.name);
        mNavBtn.setOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        });

        return view;
    }

    private int getCurrerntPosition() {
        return currentPage - 1;
    }

    private void onTranslateClicked(View v) {
        String viewTag = "page_" + getCurrerntPosition();
        View currentPageView = mViewPager.findViewWithTag(viewTag);

        // get current image view
        if (currentPageView == null) return;
        ImageView imageView = currentPageView.findViewById(R.id.reader_image_view);
        if (imageView == null) return;
        mProgressBar.setVisibility(View.VISIBLE);
        Bitmap im = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        // translate
        MLKitTextRecognizer recognizer = new MLKitTextRecognizer();
        recognizer.recognizeFromBitmap(im, processedIm -> {
            getActivity().runOnUiThread(() -> {
                imageView.setImageBitmap(processedIm);
                mProgressBar.setVisibility(View.GONE);
            });
        });
    }

    private void setPagerDirection(PagerDirection dir) {
        switch (dir) {
            case RIGHT_TO_LEFT:
                int degree = 180;
                mViewPager.setRotationY(degree);
                mAdapter.setScaleX(-1);
                break;
            case LEFT_TO_RIGHT:
                mViewPager.setRotationY(0);
                mAdapter.setScaleX(1);
                break;
            case VERTICAL_SCROLL:
                mViewPager.setRotationY(90);
                break;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new FetchImagesTask(mChapter).execute();
    }

    private class FetchImagesTask extends AsyncTask<Void, Void, String[]> {
        private final Chapter chapter;

        public FetchImagesTask(Chapter chapter) {
            this.chapter = chapter;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            String[] imageUrls = chapter.getChapterPages();
            return imageUrls;
        }

        @Override
        protected void onPostExecute(String[] imageUrls) {
            super.onPostExecute(imageUrls);
            if (imageUrls == null) {
                Snackbar.make(mViewPager, "Error", Snackbar.LENGTH_SHORT).show();
                return;
            }
            mImageUrls = imageUrls;
            mProgressBar.setVisibility(View.GONE);
            // set views
            // set onClick handler of the view pager
            mAdapter = new MangaImageAdapter(getContext(), imageUrls);
            mViewPager.setAdapter(mAdapter);
            mViewPager.setOnTouchListener((v, e) -> {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        prevActionIsDown = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Handle touch movement
                        prevActionIsDown = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (prevActionIsDown) {
                            // i.e. a down and up, instead of moving, show the toolbar
                            toggleToolbars();
                        }
                        prevActionIsDown = false;
                        // Handle touch release
                        break;
                }
                return false;
            });
            setTotalPages(imageUrls.length);
            setCurrentPage(1);

        }
    }

    private void toggleToolbars() {
        Boolean isVisible = mToolbar.getVisibility() == View.VISIBLE;
        if (isVisible) {
            // make it invisible
            mToolbar.setVisibility(View.INVISIBLE);
            mBottomBar.setVisibility(View.INVISIBLE);
            mBrightnessBar.setVisibility(View.INVISIBLE);
        } else {
            // set animation
            int height = 50;
            ObjectAnimator animator = ObjectAnimator.ofFloat(mToolbar, "translationY", -height, 0);
            animator.setDuration(100);
            animator.start();
            // make it visible
            mToolbar.setVisibility(View.VISIBLE);
            mBottomBar.setVisibility(View.VISIBLE);
        }
    }

    public void setViewBrightness(int brightness) {
        if (mainActivity == null) return;

        /* NOTE: Not changing system bright because it requires extra permission (This setting is for DEMO video only)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            boolean canWriteSettings = Settings.System.canWrite(getContext());
            if (!canWriteSettings) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                startActivity(intent);
            }
        }

        android.provider.Settings.System.putInt(getActivity().getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        android.provider.Settings.System.putInt(getActivity().getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness);
         */

        // set inner view brightness (NOT working in emulator since it CANNOT show brightness)
        WindowManager.LayoutParams layout = getActivity().getWindow().getAttributes();
        layout.screenBrightness = brightness / 255f;
        getActivity().getWindow().setAttributes(layout);
    }


}