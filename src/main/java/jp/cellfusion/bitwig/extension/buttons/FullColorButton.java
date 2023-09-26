package jp.cellfusion.bitwig.extension.buttons;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import jp.cellfusion.bitwig.extension.AtomSQExtension;
import jp.cellfusion.bitwig.extension.RgbLed;

public class FullColorButton implements RgbButton {
    private final HardwareButton hwButton;
    private final MultiStateHardwareLight light;

    public FullColorButton(final String id, final int controlNumber, final AtomSQExtension driver) {
        final HardwareSurface surface = driver.getSurface();
        final MidiIn midiIn = driver.getMidiIn();

        hwButton = surface.createHardwareButton(id);
        hwButton.pressedAction()
                .setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, controlNumber));
        hwButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, controlNumber));

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
