package jp.cellfusion.bitwig.extension.buttons;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import jp.cellfusion.bitwig.extension.AtomSQExtension;
import jp.cellfusion.bitwig.extension.RgbLed;

public class PadButton implements RgbButton {

    private static final int PAD_NOTE_OFFSET = 0x24;
    private final HardwareButton hwButton;

    private final int index;
    private final MultiStateHardwareLight light;

    public PadButton(final int index, final AtomSQExtension driver) {
        final HardwareSurface surface = driver.getSurface();
        final MidiIn midiIn = driver.getMidiIn();
        this.index = index;

        final String id = "PAD_" + (index + 1);
        hwButton = surface.createHardwareButton(id);
        hwButton.pressedAction()
                .setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, PAD_NOTE_OFFSET + index));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, PAD_NOTE_OFFSET + index));

        light = surface.createMultiStateHardwareLight(id + "-light");
        light.state().setValue(RgbLed.OFF);
        light.state().onUpdateHardware(hwState -> driver.updatePadLed(this));
        hwButton.setBackgroundLight(light);
    }

    @Override
    public HardwareButton getHwButton() {
        return hwButton;
    }

    @Override
    public MultiStateHardwareLight getLight() {
        return light;
    }

    @Override
    public int getMidiStatus() {
        return 0;
    }

    @Override
    public int getMidiDataNr() {
        return 0;
    }
}
