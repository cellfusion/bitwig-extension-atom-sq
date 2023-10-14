package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;
import jp.cellfusion.bitwig.extension.AtomSQUtils;

public class SongConsoleLayer extends Layer {
    private final AtomSQExtension driver;

    public SongConsoleLayer(AtomSQExtension driver) {
        super(driver.layers, "SONG_CONSOLE_LAYER");

        this.driver = driver;
    }

    @Override
    protected void onActivate() {
        super.onActivate();

        updateDisplay();
    }

    private void updateDisplay() {
        AtomSQUtils.writeDisplay(0, "Console", driver.getMidiOut());
        AtomSQUtils.writeDisplay(3, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(1, "Inspector", driver.getMidiOut());
        AtomSQUtils.writeDisplay(4, "", driver.getMidiOut());
        AtomSQUtils.writeDisplay(2, "Volume", driver.getMidiOut());
        AtomSQUtils.writeDisplay(5, "", driver.getMidiOut());

        AtomSQUtils.writeDisplay(6, "Console(2/2)", driver.getMidiOut());
    }
}
