package com.komic.komic.translator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;


public final class Util {
    public static void url2bitmap(String urlStr, Consumer<Bitmap> callback) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                Bitmap im = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                callback.accept(im);
            } catch (IOException e) {
                callback.accept(null);
            }
        }).start();
    }
}
