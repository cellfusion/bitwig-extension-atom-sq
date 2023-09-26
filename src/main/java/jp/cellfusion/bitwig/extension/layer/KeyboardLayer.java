package jp.cellfusion.bitwig.extension.layer;


import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;
import jp.cellfusion.bitwig.extension.KeyboardMode;

public class KeyboardLayer extends Layer {
    private final AtomSQExtension driver;

    private final KeyboardMode keyboardMode = KeyboardMode.Keyboard;

    public KeyboardLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "KEYBOARD_LAYER");

        this.driver = driver;

        // keyboard Pad
        final HardwareButton[] padButtons = driver.getPadButtons();
        for (int i = 0; i < padButtons.length; i++) {
            final int padIndex = i;
            bind(() -> getPadColor(padIndex), padButtons[i]);
        }
    }

    /**
     * パッドの色を取得する
     * ルートごとに色を変える
     *
     * @param padIndex
     * @return
     */
    private Color getPadColor(int padIndex) {
        final double darken = 0.7;

        Color padColor;

        if (keyboardMode == KeyboardMode.Keyboard) {
            padColor = driver.getCursorTrack().color().get();
        } else {
            padColor = Color.blackColor();
        }

        return padColor;

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

    @Override
    protected void onActivate() {
        super.onActivate();
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }

}
