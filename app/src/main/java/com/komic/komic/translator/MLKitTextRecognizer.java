package com.komic.komic.translator;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.NonNull;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class MLKitTextRecognizer implements MangaRecognizer {

    private final String sourceLanguage;
    private final String targetLanguage;

    TextRecognizerOptions options = new TextRecognizerOptions.Builder().build();
    TextRecognizer recognizer = TextRecognition.getClient(options);
    TextTranslator translator;

    public MLKitTextRecognizer() {
        this(TranslateLanguage.ENGLISH, TranslateLanguage.CHINESE);
    }

    public MLKitTextRecognizer(String sourceLanguage, String targetLanguage) {
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        translator = new TextTranslator(sourceLanguage, targetLanguage);
    }

    public void recognizeFromUrl(String url, Consumer<Bitmap> callback) {
        Util.url2bitmap(url, im -> {
            recognizeFromBitmap(im, callback);
        });
    }

    public void recognizeFromBitmap(Bitmap im, Consumer<Bitmap> callback) {
        new Thread(() -> {
            // Convert bitmap to InputImage
            InputImage image = InputImage.fromBitmap(im, 0);
            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        translateTextBlocks(text, textBlocks -> {
                            if (textBlocks == null) {
                                callback.accept(null);
                                return;
                            }
                            Bitmap processedIm = renderTranslatedText(im, textBlocks);
                            callback.accept(processedIm);
                        });
                    })
                    .addOnFailureListener(e -> {
                        callback.accept(null);
                    });
        }).start();
    }

    private Bitmap renderTranslatedText(Bitmap im,
                                        List<TranslationTextBlock> translatedTextBlocks) {
        Bitmap imCopy = im.copy(im.getConfig(), true);
        Canvas canvas = new Canvas(imCopy);
        Rect boundingBox;
        // use paint to convert original text
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        // background paint
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);


        for (TranslationTextBlock block : translatedTextBlocks) {
            // get bounds of the detected textarea
            boundingBox = block.textBlock.getBoundingBox();
            textPaint.setTextSize(block.translationFontSize);

            // use white background to cover original text
            canvas.drawRect(boundingBox, bgPaint);

            // Create a StaticLayout object to draw the wrapped text
            StaticLayout layout = new StaticLayout(block.getText(), textPaint, Math.max(boundingBox.width(), 80), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            canvas.save();
            canvas.translate(boundingBox.left, boundingBox.top);
            layout.draw(canvas);
            canvas.restore();
        }
        return imCopy;
    }

    public void translateTextBlocks(Text text, Consumer<List<TranslationTextBlock>> callback) {
        new Thread(() -> {
            List<TranslationTextBlock> textBlocks = extractTextBlocks(text);
            // translate text blocks
            CountDownLatch latch = new CountDownLatch(textBlocks.size());
            for (TranslationTextBlock block : textBlocks) {
                translator.translate(block.textBlock.getText(),
                        translatedText -> {
                            block.setTranslation(translatedText);
                            latch.countDown();
                        });
            }
            // Wait for all callbacks to complete
            try {
                // Wait for all callbacks to complete
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            callback.accept(textBlocks);
        }).start();
    }

    private List<TranslationTextBlock> extractTextBlocks(Text text) {
        List<Text.TextBlock> textBlocks = text.getTextBlocks();
        List<TranslationTextBlock> translationTextBlocks = new ArrayList<>();
        for (Text.TextBlock block : textBlocks) {
            translationTextBlocks.add(new TranslationTextBlock(block));
        }
        return translationTextBlocks;
    }

    public class TranslationTextBlock {
        public final Text.TextBlock textBlock;
        public String translatedText = null;
        public String text;
        public float translationFontSize;

        public TranslationTextBlock(@NonNull Text.TextBlock textBlock) {
            this.textBlock = textBlock;
            text = textBlock.getText();
        }

        public void setTranslation(String s) {
            if (s == null) return;
            translatedText = s;
            estimateTranslationFontSize();
        }

        public String getText() {
            if (translatedText != null) return translatedText;
            return textBlock.getText();
        }

        private void estimateTranslationFontSize() {
            // compute the Japanese char font size
            Text.Line firstLine = textBlock.getLines().get(0);
            if (firstLine == null) return;
            Rect frame = firstLine.getBoundingBox();
            if (frame == null) return;
            float fontSize = frame.bottom - frame.top;
            // convert the Japanese font size to translated font size
            switch (targetLanguage) {
                case "en":
                    // approx formula
                    translationFontSize = fontSize * (float) (1.1 + 0.1 * text.length() / translatedText.length());
                    break;
                default:
                    translationFontSize = fontSize;
            }
            // cap fontsize min max
            translationFontSize = Math.max(Math.min(translationFontSize, 22), 32);
        }
    }

}
