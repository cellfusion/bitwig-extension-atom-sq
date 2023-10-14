package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;
import jp.cellfusion.bitwig.extension.AtomSQUtils;
import jp.cellfusion.bitwig.extension.types.SongLayerMode;

public class SongLayer extends Layer {
    private final AtomSQExtension driver;
    private SongLayerMode mode = SongLayerMode.Transport;

    public SongLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "SONG_LAYER");

        this.driver = driver;

        bindPressed(this.driver.mLeftButton, this::previousMode);
        bindPressed(this.driver.mRightButton, this::nextMode);
    }

    @Override
    protected void onActivate() {
        super.onActivate();

        updateDisplay();
    }

    private void nextMode() {
        switch (mode) {
            case Transport:
                mode = SongLayerMode.Console;
                break;
            case Console:
                mode = SongLayerMode.Arranger;
                break;
            case Arranger:
                mode = SongLayerMode.Effects;
                break;
            case Effects:
                mode = SongLayerMode.Transport;
                break;
        }
        updateDisplay();
    }

    private void previousMode() {
        switch (mode) {
            case Transport:
                mode = SongLayerMode.Effects;
                break;
            case Console:
                mode = SongLayerMode.Transport;
                break;
            case Arranger:
                mode = SongLayerMode.Console;
                break;
            case Effects:
                mode = SongLayerMode.Arranger;
                break;
        }
        updateDisplay();
    }

    public void updateDisplay() {
        switch (mode) {
            case Transport:
                updateTransportDisplay();
                break;
            case Console:
                updateConsoleDisplay();
                break;
            case Arranger:
                updateArrangerDisplay();
                break;
            case Effects:
                updateEffectsDisplay();
                break;
        }
    }

    private void updateEffectsDisplay() {
        AtomSQUtils.writeDisplay(6, "Effects(4/4)", driver.getMidiOut());
    }

    private void updateArrangerDisplay() {

        AtomSQUtils.writeDisplay(6, "Arranger(3/4)", driver.getMidiOut());
    }

    private void updateConsoleDisplay() {
        AtomSQUtils.writeDisplay(0, "Console", driver.getMidiOut());
        AtomSQUtils.writeDisplay(1, "Inspector", driver.getMidiOut());
        AtomSQUtils.writeDisplay(2, "Volume", driver.getMidiOut());

        AtomSQUtils.writeDisplay(6, "Console(2/4)", driver.getMidiOut());
    }

    private void updateTransportDisplay() {
        AtomSQUtils.writeDisplay(0, "Temp", driver.getMidiOut());
        AtomSQUtils.writeDisplay(1, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(2, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(3, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(4, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(5, "", driver.getMidiOut());

        AtomSQUtils.writeDisplay(6, "Transport(1/4)", driver.getMidiOut());
    }

    public void setMode(SongLayerMode mode) {
        this.mode = mode;
    }
}
