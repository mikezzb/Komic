package com.komic.komic.translator;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextTranslator {
    private final String sourceLanguage;
    private final String targetLanguage;
    private Translator translator;
    private boolean ready = false;
    private final List<PendingJob> pendingJobs = new ArrayList<>();

    private class PendingJob {
        String text;
        Consumer<String> callback;

        PendingJob(String text, Consumer<String> callback) {
            this.text = text;
            this.callback = callback;
        }
    }

    public TextTranslator(String sourceLanguage, String targetLanguage) {
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        init();
    }

    public TextTranslator() {
        this(TranslateLanguage.JAPANESE, TranslateLanguage.CHINESE);
    }

    private void init() {
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLanguage)
                        .setTargetLanguage(targetLanguage)
                        .build();
        translator = Translation.getClient(options);
        translator.downloadModelIfNeeded().addOnSuccessListener(r -> onReady());
    }

    private void onReady() {
        ready = true;
        // handle pending jobs
        for (PendingJob job : pendingJobs) {
            translate(job.text, job.callback);
        }
    }

    public void translate(String text, Consumer<String> callback) {
        if (text == null) {
            callback.accept(null);
            return;
        }
        if (!ready) {
            pendingJobs.add(new PendingJob(text, callback));
            return;
        }
        Task<String> translationTask = translator.translate(text);
        translationTask
                .addOnSuccessListener(translatedText -> {
                    callback.accept(translatedText);
                })
                .addOnFailureListener(error -> {
                    callback.accept(null);
                });
    }

    public void close() {
        translator.close();
    }
}
