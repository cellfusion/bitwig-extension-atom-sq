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
    private int currentHits = 0;
    private String selectedContentType = "";
    private String selectedElement = "";
    private String categoryElement = "";
    private String creatorElement = "";
    private String tagElement = "";
    private String fileTypeElement = "";
    private String deviceElement = "";
    private String[] contentTypeNames;
    private String currentContentType = "";


    public BrowserLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "BROWSR_LAYER");

        browser = driver.getBrowser();
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


        // deviceItem
        deviceItem = (CursorBrowserFilterItem) browser.deviceColumn().createCursorItem();
        deviceItem.hitCount().markInterested();
        deviceItem.name().addValueObserver(v -> {
            deviceElement = v.trim();
            currentHits = deviceItem.hitCount().get();
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

        // select
        driver.bindEncoder(this, driver.getMainEncoder(), this::handleEncoder);
        driver.bindEncoder(this, driver.getEncoder(0), this::scrollContentType);
        driver.bindEncoder(this, driver.getEncoder(1), this::scrollFileType);
        driver.bindEncoder(this, driver.getEncoder(2), this::scrollDevice);
        driver.bindEncoder(this, driver.getEncoder(3), this::scrollCategory);
        driver.bindEncoder(this, driver.getEncoder(4), this::scrollTag);
        driver.bindEncoder(this, driver.getEncoder(5), this::scrollCreator);
    }


    private void scrollContentType(final int increment) {
        final int v = browser.selectedContentTypeIndex().get();
        browser.selectedContentTypeIndex().set(v + increment);
    }

    private void scrollFileType(final int increment) {
        if (increment > 0) {
            fileTypeItem.selectNext();
        } else {
            fileTypeItem.selectPrevious();
        }
    }

    private void scrollDevice(final int increment) {
        if (increment > 0) {
            deviceItem.selectNext();
        } else {
            deviceItem.selectPrevious();
        }
    }

    private void scrollCategory(final int increment) {
        if (increment > 0) {
            categoryItem.selectNext();
        } else {
            categoryItem.selectPrevious();
        }
    }

    private void scrollTag(final int increment) {
        if (increment > 0) {
            tagItem.selectNext();
        } else {
            tagItem.selectPrevious();
        }
    }

    private void scrollCreator(final int increment) {
        if (increment > 0) {
            creatorItem.selectNext();
        } else {
            creatorItem.selectPrevious();
        }
    }

    private void handleEncoder(final int increment) {
        if (increment > 0) {
            browser.selectNextFile();
        } else {
            browser.selectPreviousFile();
        }
    }

    private void contentTypeNamesChanged(String[] names) {
        contentTypeNames = names;
    }

    private void browserValueChanged(boolean exists) {
        setIsActive(exists);
    }

    private void selectedChanged(int selected) {
        if (selected < contentTypeNames.length) {
            currentContentType = contentTypeNames[selected];
            final boolean selectionExists = resultCursorItem.exists().get();
        }
    }

    public void shiftPressAction(final boolean down) {
        if (browser.exists().get()) {
            browser.cancel();
        } else {
            if (cursorDevice.exists().get()) {
                cursorDevice.replaceDeviceInsertionPoint().browse();
            } else {
                cursorTrack.endOfDeviceChainInsertionPoint().browse();
            }
        }
    }

    public void pressAction(final boolean down) {
        if (down || !browser.exists().get()) {
            return;
        }

        browser.commit();
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
