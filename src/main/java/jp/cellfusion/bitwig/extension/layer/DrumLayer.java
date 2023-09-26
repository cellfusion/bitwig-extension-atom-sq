package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;

public class DrumLayer extends Layer {

    private final AtomSQExtension driver;

    public DrumLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "DRUM_LAYER");

        this.driver = driver;

        // drum pad
        final HardwareButton[] padButtons = driver.getPadButtons();
        for (int i = 0; i < padButtons.length; i++) {
            final int padIndex = i;
            bind(() -> getDrumPadColor(padIndex), padButtons[i]);
        }
    }

    private Color getDrumPadColor(final int padIndex) {
        final DrumPadBank drumPadBank = driver.getDrumPadBank();
        final DrumPad drumPad = drumPadBank.getItemAt(padIndex);
        final boolean padBankExists = drumPadBank.exists().get();
        final boolean isOn = !padBankExists || drumPad.exists().get();

        if (!isOn)
            return null;

        final double darken = 0.7;

        Color drumPadColor;

        final Color sourceDrumPadColor = drumPad.color().get();
        final double red = sourceDrumPadColor.getRed() * darken;
        final double green = sourceDrumPadColor.getGreen() * darken;
        final double blue = sourceDrumPadColor.getBlue() * darken;

        drumPadColor = Color.fromRGB(red, green, blue);

        final int playing = velocityForPlayingNote(padIndex);

        if (playing > 0) {
            return mixColorWithWhite(drumPadColor, playing);
        }

        return drumPadColor;
    }

    private int velocityForPlayingNote(final int padIndex) {
        final PlayingNote[] mPlayingNotes = driver.getPlayingNotes();
        if (mPlayingNotes != null) {
            for (final PlayingNote playingNote : mPlayingNotes) {
                if (playingNote.pitch() == 36 + padIndex) {
                    return playingNote.velocity();
                }
            }
        }

        return 0;
    }


    private Color mixColorWithWhite(final Color color, final int velocity) {
        final float x = velocity / 127.f;
        final double red = color.getRed() * (1 - x) + x;
        final double green = color.getGreen() * (1 - x) + x;
        final double blue = color.getBlue() * (1 - x) + x;

        return Color.fromRGB(red, green, blue);
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
