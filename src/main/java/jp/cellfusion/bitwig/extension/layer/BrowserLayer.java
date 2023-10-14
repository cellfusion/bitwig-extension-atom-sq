package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;

public class BrowserLayer extends Layer {
    private final AtomSQExtension driver;
    private final PopupBrowser browser;
    private final PinnableCursorDevice cursorDevice;
    private final CursorTrack cursorTrack;
    private final CursorBrowserResultItem resultCursorItem;
    private final CursorBrowserFilterItem deviceItem;
    private final CursorBrowserFilterItem fileTypeItem;
    private final CursorBrowserFilterItem categoryItem;
    private final CursorBrowserFilterItem creatorItem;
    private final CursorBrowserFilterItem tagItem;
    private final CursorBrowserFilterItem locationItem;
    private int currentHits = 0;
    private String selectedContentType = "";
    private String locationElement = "";
    private String selectedElement = "";
    private String categoryElement = "";
    private String creatorElement = "";
    private String tagElement = "";
    private String fileTypeElement = "";
    private String deviceElement = "";
    private String[] contentTypeNames;


    public BrowserLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "BROWSR_LAYER");

        browser = driver.browser;
        cursorDevice = driver.getCursorDevice();
        cursorTrack = driver.getCursorTrack();
        cursorDevice.exists().markInterested();

        this.driver = driver;

        browser.exists().addValueObserver(this::browserValueChanged);
        browser.contentTypeNames().addValueObserver(this::contentTypeNamesChanged);

        // contentType
        browser.selectedContentTypeIndex().markInterested();
        browser.selectedContentTypeName().addValueObserver(content -> {
            selectedContentType = content;
        });

        locationItem = (CursorBrowserFilterItem) browser.locationColumn().createCursorItem();
        locationItem.hitCount().markInterested();
        locationItem.name().addValueObserver(v -> {
            locationElement = v.trim();
            currentHits = locationItem.hitCount().get();
        });

        fileTypeItem = (CursorBrowserFilterItem) browser.fileTypeColumn().createCursorItem();
        fileTypeItem.hitCount().markInterested();
        fileTypeItem.name().addValueObserver(v -> {
            fileTypeElement = v.trim();
            currentHits = fileTypeItem.hitCount().get();
        });

        categoryItem = (CursorBrowserFilterItem) browser.categoryColumn().createCursorItem();
        categoryItem.hitCount().markInterested();
        categoryItem.name().addValueObserver(v -> {
            categoryElement = v.trim();
            currentHits = categoryItem.hitCount().get();
        });

        creatorItem = (CursorBrowserFilterItem) browser.creatorColumn().createCursorItem();
        creatorItem.hitCount().markInterested();
        creatorItem.name().addValueObserver(v -> {
            creatorElement = v.trim();
            currentHits = creatorItem.hitCount().get();
        });

        deviceItem = (CursorBrowserFilterItem) browser.deviceColumn().createCursorItem();
        deviceItem.hitCount().markInterested();
        deviceItem.name().addValueObserver(v -> {
            deviceElement = v.trim();
            currentHits = deviceItem.hitCount().get();
        });

        tagItem = (CursorBrowserFilterItem) browser.tagColumn().createCursorItem();
        tagItem.hitCount().markInterested();
        tagItem.name().addValueObserver(v -> {
            tagElement = v.trim();
            currentHits = tagItem.hitCount().get();
        });

        resultCursorItem = (CursorBrowserResultItem) browser.resultsColumn().createCursorItem();
        resultCursorItem.name().addValueObserver(v -> {
            selectedElement = v;
        });

        bindPressed(driver.mDisplayButtons[0], pressed -> {
            if (driver.getShiftDown().get()) {
                browser.cancel();
            } else {
                browser.commit();
            }
        });
        driver.mShiftButton.isPressed().addValueObserver(pressed -> {
            updateDisplay();
        });

        // select
        driver.bindEncoder(this, driver.getMainEncoder(), this::handleEncoder);
    }

    private void updateDisplay() {
        driver.writeDisplay(0, driver.getShiftDown().get() ? "Cancel" : "OK");

    }


    private void scrollContentType(final int increment) {
        final int v = browser.selectedContentTypeIndex().get();
        browser.selectedContentTypeIndex().set(v + increment);
    }

    private void handleEncoder(final int increment) {
        // B Button Pressed
        if (driver.getAlphabetButton(1).isPressed().get()) {
            if (increment > 0) {
                locationItem.selectNext();
            } else {
                locationItem.selectPrevious();
            }

            // C Button Pressed
        } else if (driver.getAlphabetButton(2).isPressed().get()) {
            if (increment > 0) {
                fileTypeItem.selectNext();
            } else {
                fileTypeItem.selectPrevious();
            }
            // D Button Pressed
        } else if (driver.getAlphabetButton(3).isPressed().get()) {
            if (increment > 0) {
                categoryItem.selectNext();
            } else {
                categoryItem.selectPrevious();
            }
            // E Button Pressed
        } else if (driver.getAlphabetButton(4).isPressed().get()) {
            if (increment > 0) {
                creatorItem.selectNext();
            } else {
                creatorItem.selectPrevious();
            }
            // F Button Pressed
        } else if (driver.getAlphabetButton(5).isPressed().get()) {
            if (increment > 0) {
                deviceItem.selectNext();
            } else {
                deviceItem.selectPrevious();
            }
            // G Button Pressed
        } else if (driver.getAlphabetButton(6).isPressed().get()) {
            if (increment > 0) {
                tagItem.selectNext();
            } else {
                tagItem.selectPrevious();
            }
        } else {
            if (increment > 0) {
                browser.selectNextFile();
            } else {
                browser.selectPreviousFile();
            }
        }
    }

    private void contentTypeNamesChanged(String[] names) {
        contentTypeNames = names;
    }

    private void browserValueChanged(boolean exists) {
        setIsActive(exists);
    }

    @Override
    protected void onActivate() {
        super.onActivate();
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }
}
