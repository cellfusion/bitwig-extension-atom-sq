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
    private String[] contentTypeNames;
    private String currentContentType = "";

    private boolean browsingInitiated = false;
    private boolean isSelecting = false;

    public BrowserLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "BROWSR_LAYER");

        browser = driver.getHost().createPopupBrowser();
        cursorDevice = driver.getCursorDevice();
        cursorTrack = driver.getCursorTrack();
        cursorDevice.exists().markInterested();

        this.driver = driver;

        browser.exists().markInterested();
        browser.exists().addValueObserver(this::browserValueChanged);
        browser.contentTypeNames().addValueObserver(this::contentTypeNamesChanged);
        browser.selectedContentTypeIndex().markInterested();
        browser.selectedContentTypeIndex().addValueObserver(this::selectedChanged);

        resultCursorItem = (CursorBrowserResultItem) browser.resultsColumn().createCursorItem();

        final RelativeHardwareKnob mainEncoder = driver.getMainEncoder();
        driver.bindEncoder(this, mainEncoder, this::handleEncoder);
    }

    private void handleEncoder(int dir) {
        driver.debugLog("BrowserLayer", "encoderAction: " + dir + ", shift: " + driver.getShiftDown().get());
        if (driver.getShiftDown().get()) {
            final int index = browser.selectedContentTypeIndex().get();
            if (index >= 0 && index < contentTypeNames.length) {
                int next = index + dir;
                next = next < 0 ? contentTypeNames.length - 1 : (next >= contentTypeNames.length) ? 0 : next;
                browser.selectedContentTypeIndex().set(next);
            }
        } else {
            browser.shouldAudition().set(false);
            isSelecting = true;
            if (dir > 0) {
                resultCursorItem.selectNext();
            } else {
                resultCursorItem.selectPrevious();
            }
        }
    }

    private void contentTypeNamesChanged(String[] names) {
        contentTypeNames = names;
    }

    private void browserValueChanged(boolean exists) {
        setIsActive(exists);
        if (browsingInitiated) {
            browser.shouldAudition().set(false);
            isSelecting = true;
        }

        if (!exists) {
            browsingInitiated = false;
        } else {
            isSelecting = true;
        }
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
                browsingInitiated = true;
                cursorDevice.replaceDeviceInsertionPoint().browse();
            } else {
                browsingInitiated = true;
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
