package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.LayerGroup;
import jp.cellfusion.bitwig.extension.AtomSQExtension;
import jp.cellfusion.bitwig.extension.types.SongLayerMode;

public class SongLayer extends Layer {
    private final AtomSQExtension driver;
    private final SongTransportLayer mTransportlayer;
    private final SongConsoleLayer mConsoleLayer;
    private final LayerGroup mLayerGroup;
    private SongLayerMode mode = SongLayerMode.Transport;

    public SongLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "SONG_LAYER");

        mTransportlayer = new SongTransportLayer(driver);
        mConsoleLayer = new SongConsoleLayer(driver);
        mLayerGroup = new LayerGroup(mTransportlayer, mConsoleLayer);

        this.driver = driver;

        bindPressed(this.driver.mNavLeftButton, this::previousMode);
        bindPressed(this.driver.mNavRightButton, this::nextMode);
    }

    @Override
    protected void onActivate() {
        super.onActivate();

        mTransportlayer.activate();
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();

        mTransportlayer.deactivate();
        mConsoleLayer.deactivate();
    }

    private void changeMode(SongLayerMode mode) {
        this.mode = mode;

        switch (mode) {
            case Transport -> mTransportlayer.activate();
            case Console -> mConsoleLayer.activate();
        }
    }

    private void nextMode() {
        switch (mode) {
            case Transport -> changeMode(SongLayerMode.Console);
            case Console -> changeMode(SongLayerMode.Transport);
        }
    }

    private void previousMode() {
        switch (mode) {
            case Transport -> changeMode(SongLayerMode.Console);
            case Console -> changeMode(SongLayerMode.Transport);
        }
    }
}
