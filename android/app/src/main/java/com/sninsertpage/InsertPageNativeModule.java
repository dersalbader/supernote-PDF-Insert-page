package com.sninsertpage;

import android.util.Log;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageTree;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class InsertPageNativeModule extends ReactContextBaseJavaModule {

    private static final String TAG = "InsertPageNative";

    InsertPageNativeModule(ReactApplicationContext context) {
        super(context);
    }

    @NonNull
    @Override
    public String getName() {
        return "InsertPageNative";
    }

    private File backupFile() {
        return new File(getReactApplicationContext().getFilesDir(), "undo_backup.pdf");
    }

    private void copyFile(File source, File dest) throws java.io.IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    private void backupBeforeEdit(String filePath) throws java.io.IOException {
        Log.d(TAG, "backupBeforeEdit: starting copy to internal storage");
        copyFile(new File(filePath), backupFile());
        Log.d(TAG, "backupBeforeEdit: copy done");
    }

    @ReactMethod
    public void undoLastAction(String filePath, Promise promise) {
        Log.d(TAG, "undoLastAction: called, filePath=" + filePath);
        try {
            File backup = backupFile();
            Log.d(TAG, "undoLastAction: backup exists=" + backup.exists());
            if (!backup.exists()) {
                promise.reject("NO_UNDO_AVAILABLE", "No previous action to undo.");
                return;
            }
            copyFile(backup, new File(filePath));
            backup.delete();
            Log.d(TAG, "undoLastAction: restore complete, resolving");
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "undoLastAction: FAILED", e);
            promise.reject("UNDO_FAILED", e.getMessage(), e);
        }
    }

    @ReactMethod
    public void insertBlankPageAfter(String filePath, int pageIndex, Promise promise) {
        Log.d(TAG, "insertBlankPageAfter: called, filePath=" + filePath + " pageIndex=" + pageIndex);
        PDDocument document = null;
        try {
            File file = new File(filePath);
            Log.d(TAG, "insertBlankPageAfter: loading document...");
            document = PDDocument.load(file);
            Log.d(TAG, "insertBlankPageAfter: document loaded");

            PDPageTree pages = document.getPages();
            Log.d(TAG, "insertBlankPageAfter: pageCount=" + pages.getCount());

            if (pageIndex < 0 || pageIndex >= pages.getCount()) {
                Log.d(TAG, "insertBlankPageAfter: BAD_PAGE_INDEX");
                promise.reject("BAD_PAGE_INDEX", "Page index out of range: " + pageIndex);
                return;
            }

            PDPage existingPage = pages.get(pageIndex);
            PDPage newPage = new PDPage(existingPage.getMediaBox());
            pages.insertAfter(newPage, existingPage);
            Log.d(TAG, "insertBlankPageAfter: page inserted in memory");

            drawDotGrid(document, newPage);
            Log.d(TAG, "insertBlankPageAfter: dot grid drawn");

            backupBeforeEdit(filePath);

            Log.d(TAG, "insertBlankPageAfter: saving document...");
            document.save(file);
            Log.d(TAG, "insertBlankPageAfter: document saved, resolving promise");
            promise.resolve(true);
            Log.d(TAG, "insertBlankPageAfter: promise resolved");
        } catch (Exception e) {
            Log.e(TAG, "insertBlankPageAfter: FAILED", e);
            promise.reject("INSERT_PAGE_FAILED", e.getMessage(), e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                    Log.d(TAG, "insertBlankPageAfter: document closed");
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void drawDotGrid(PDDocument document, PDPage page) throws java.io.IOException {
        PDRectangle box = page.getMediaBox();
        float spacing = 14.17f; // ~5mm in PDF points (72 pt/inch, 25.4mm/inch)
        float dotSize = 2.2f;
        float margin = spacing;

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            cs.setNonStrokingColor(0.45f, 0.45f, 0.45f);
            for (float x = margin; x < box.getWidth() - margin; x += spacing) {
                for (float y = margin; y < box.getHeight() - margin; y += spacing) {
                    cs.addRect(x - dotSize / 2f, y - dotSize / 2f, dotSize, dotSize);
                }
            }
            cs.fill();
        }
    }

    @ReactMethod
    public void deletePageAt(String filePath, int pageIndex, Promise promise) {
        Log.d(TAG, "deletePageAt: called, filePath=" + filePath + " pageIndex=" + pageIndex);
        PDDocument document = null;
        try {
            File file = new File(filePath);
            Log.d(TAG, "deletePageAt: loading document...");
            document = PDDocument.load(file);
            Log.d(TAG, "deletePageAt: document loaded");

            PDPageTree pages = document.getPages();
            Log.d(TAG, "deletePageAt: pageCount=" + pages.getCount());

            if (pageIndex < 0 || pageIndex >= pages.getCount()) {
                Log.d(TAG, "deletePageAt: BAD_PAGE_INDEX");
                promise.reject("BAD_PAGE_INDEX", "Page index out of range: " + pageIndex);
                return;
            }

            if (pages.getCount() <= 1) {
                Log.d(TAG, "deletePageAt: CANNOT_DELETE_LAST_PAGE");
                promise.reject("CANNOT_DELETE_LAST_PAGE", "Cannot delete the only page in the document.");
                return;
            }

            backupBeforeEdit(filePath);
            document.removePage(pageIndex);
            Log.d(TAG, "deletePageAt: page removed in memory");

            Log.d(TAG, "deletePageAt: saving document...");
            document.save(file);
            Log.d(TAG, "deletePageAt: document saved, resolving promise");
            promise.resolve(true);
            Log.d(TAG, "deletePageAt: promise resolved");
        } catch (Exception e) {
            Log.e(TAG, "deletePageAt: FAILED", e);
            promise.reject("DELETE_PAGE_FAILED", e.getMessage(), e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                    Log.d(TAG, "deletePageAt: document closed");
                } catch (Exception ignored) {
                }
            }
        }
    }

    @ReactMethod
    public void insertPdfAfter(String targetFilePath, int pageIndex, String sourceFilePath, Promise promise) {
        Log.d(TAG, "insertPdfAfter: called, target=" + targetFilePath + " pageIndex=" + pageIndex + " source=" + sourceFilePath);
        PDDocument targetDoc = null;
        PDDocument sourceDoc = null;
        try {
            File targetFile = new File(targetFilePath);
            Log.d(TAG, "insertPdfAfter: loading target...");
            targetDoc = PDDocument.load(targetFile);
            Log.d(TAG, "insertPdfAfter: target loaded, pageCount=" + targetDoc.getPages().getCount());

            Log.d(TAG, "insertPdfAfter: loading source...");
            sourceDoc = PDDocument.load(new File(sourceFilePath));
            int sourceCount = sourceDoc.getPages().getCount();
            Log.d(TAG, "insertPdfAfter: source loaded, pageCount=" + sourceCount);

            PDPageTree pages = targetDoc.getPages();

            if (pageIndex < 0 || pageIndex >= pages.getCount()) {
                Log.d(TAG, "insertPdfAfter: BAD_PAGE_INDEX");
                promise.reject("BAD_PAGE_INDEX", "Page index out of range: " + pageIndex);
                return;
            }

            int originalCount = pages.getCount();

            java.util.List<PDPage> importedPages = new java.util.ArrayList<>();
            for (PDPage sourcePage : sourceDoc.getPages()) {
                PDPage imported = targetDoc.importPage(sourcePage);
                importedPages.add(imported);
            }
            Log.d(TAG, "insertPdfAfter: imported " + importedPages.size() + " pages at end");

            for (int i = 0; i < importedPages.size(); i++) {
                pages.remove(originalCount);
            }
            Log.d(TAG, "insertPdfAfter: removed imported pages from tail, pageCount now=" + pages.getCount());

            PDPage anchor = pages.get(pageIndex);
            for (PDPage imported : importedPages) {
                pages.insertAfter(imported, anchor);
                anchor = imported;
            }
            Log.d(TAG, "insertPdfAfter: reinserted after index " + pageIndex + ", new pageCount=" + pages.getCount());

            backupBeforeEdit(targetFilePath);

            Log.d(TAG, "insertPdfAfter: saving document...");
            targetDoc.save(targetFile);
            Log.d(TAG, "insertPdfAfter: document saved, resolving promise");
            promise.resolve(importedPages.size());
            Log.d(TAG, "insertPdfAfter: promise resolved");
        } catch (Exception e) {
            Log.e(TAG, "insertPdfAfter: FAILED", e);
            promise.reject("INSERT_PDF_FAILED", e.getMessage(), e);
        } finally {
            if (sourceDoc != null) {
                try {
                    sourceDoc.close();
                    Log.d(TAG, "insertPdfAfter: source closed");
                } catch (Exception ignored) {
                }
            }
            if (targetDoc != null) {
                try {
                    targetDoc.close();
                    Log.d(TAG, "insertPdfAfter: target closed");
                } catch (Exception ignored) {
                }
            }
        }
    }
}
