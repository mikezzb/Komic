package com.komic.komic.translator;

import android.graphics.Bitmap;

import java.util.function.Consumer;

public interface MangaRecognizer {
    void recognizeFromBitmap(Bitmap im, Consumer<Bitmap> callback);
}
