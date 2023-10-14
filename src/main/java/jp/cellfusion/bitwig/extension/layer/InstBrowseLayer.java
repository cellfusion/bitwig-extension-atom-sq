package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extensions.framework.BooleanObject;
import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;

public class InstBrowseLayer extends Layer {
    private final AtomSQExtension driver;
    private final BrowserLayer browserLayer;
    private final BooleanObject mDeviceToggle;
    private final BooleanObject mParameterToggle;

    public InstBrowseLayer(AtomSQExtension driver) {
        super(driver.layers, "INST_BROWSE_LAYER");

        browserLayer = new BrowserLayer(driver);

        bindPressed(driver.mDisplayButtons[0], pressed -> {
            if (driver.getCursorDevice().exists().get()) {
                if (driver.getShiftDown().get()) {
                    driver.cursorDevice.replaceDeviceInsertionPoint().browse();
                } else {
                    driver.cursorDevice.afterDeviceInsertionPoint().browse();
                }
            } else {
                driver.deviceBank.browseToInsertDevice(0);
            }
        });

        driver.cursorDevice.name().markInterested();
        driver.cursorDevice.name().addValueObserver((x) -> {
            updateDisplay();
        });
        driver.cursorDevice.position().markInterested();

        driver.cursorRemoteControlsPage.pageCount().markInterested();
        driver.cursorRemoteControlsPage.pageCount().addValueObserver((x) -> {
            updateDisplay();
        });
        driver.cursorRemoteControlsPage.selectedPageIndex().markInterested();
        driver.cursorRemoteControlsPage.selectedPageIndex().addValueObserver((x) -> {
            updateDisplay();
        });

        driver.bindEncoder(this, driver.getMainEncoder(), this::handleEncoder);

        // TODO 排他的にする
        mDeviceToggle = new BooleanObject();
        bindToggle(driver.mDisplayButtons[2], mDeviceToggle::toggle, mDeviceToggle);
        mParameterToggle = new BooleanObject();
        bindToggle(driver.mDisplayButtons[3], mParameterToggle::toggle, mParameterToggle);
        bindIsPressed(driver.mDisplayButtons[3], this);

        // device enabled
        driver.cursorDevice.isEnabled().markInterested();
        bindToggle(driver.mDisplayButtons[4], () -> driver.cursorDevice.isEnabled().toggle(), driver.cursorDevice.isEnabled());


        this.driver = driver;
    }

    private void handleEncoder(final int increment) {
        if (mDeviceToggle.getAsBoolean()) {
            if (increment < 0) {
                driver.cursorDevice.selectNext();
            } else {
                driver.cursorDevice.selectPrevious();
            }
        } else if (mParameterToggle.getAsBoolean()) {
            if (increment < 0) {
                driver.cursorRemoteControlsPage.selectNext();
            } else {
                driver.cursorRemoteControlsPage.selectPrevious();
            }
        }
    }

    @Override
    protected void onActivate() {
        super.onActivate();

        updateDisplay();
    }

    private void updateDisplay() {
        int deviceIndex = driver.cursorDevice.position().get() + 1;
        int deviceCount = driver.deviceBank.getSizeOfBank();

        driver.writeDisplay(0, "Browser");
        driver.writeDisplay(3, "");
        driver.writeDisplay(1, "Show");
        driver.writeDisplay(4, "Plugin");
        driver.writeDisplay(2, "Device");
        driver.writeDisplay(5, String.format("(%d/%d)", deviceIndex, deviceCount));

        driver.writeDisplay(6, "Browser(1/2)");
        driver.writeDisplay(7, driver.cursorDevice.name().get());

        int pageIndex = driver.cursorRemoteControlsPage.selectedPageIndex().get() + 1;
        int pageCount = driver.cursorRemoteControlsPage.pageCount().get();

        driver.writeDisplay(8, "Parameter");
        driver.writeDisplay(11, String.format("(%d/%d)", pageIndex, pageCount));
        driver.writeDisplay(9, "enabled");
        driver.writeDisplay(12, "");
        driver.writeDisplay(10, "");
        driver.writeDisplay(13, "");
    }
}
