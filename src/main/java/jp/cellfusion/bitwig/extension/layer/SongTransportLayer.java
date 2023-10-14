package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;
import jp.cellfusion.bitwig.extension.AtomSQUtils;

public class SongTransportLayer extends Layer {
    private final AtomSQExtension driver;
    private int mTempo = 0;

    public SongTransportLayer(AtomSQExtension driver) {
        super(driver.layers, "SONG_TRANSPORT_LAYER");
        this.driver = driver;

        driver.bindEncoder(this, driver.getMainEncoder(), this::handleEncoder);
        driver.mDisplayButtons[0].isPressed().markInterested();


        driver.transport.tempo().value().markInterested();
        driver.transport.tempo().value().addRawValueObserver(this::onTempoChanged);
    }

    private void handleEncoder(final int increment) {
        if (driver.mDisplayButtons[0].isPressed().get()) {
            if (increment > 0) {
                driver.transport.tempo().value().incRaw(-1);
            } else {
                driver.transport.tempo().value().incRaw(1);
            }
        }
    }

    @Override
    protected void onActivate() {
        super.onActivate();

        updateDisplay();
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();

    }

    private void onTempoChanged(double v) {
        mTempo = (int) Math.round(v);
        updateDisplay();
    }

    private void updateDisplay() {
        AtomSQUtils.writeDisplay(0, "Temp", driver.getMidiOut());
        AtomSQUtils.writeDisplay(3, String.format("%d", mTempo), driver.getMidiOut());

        AtomSQUtils.writeDisplay(1, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(4, "", driver.getMidiOut());

        AtomSQUtils.writeDisplay(2, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(5, "", driver.getMidiOut());


        AtomSQUtils.writeDisplay(6, "Transport(1/2)", driver.getMidiOut());
    }

}
