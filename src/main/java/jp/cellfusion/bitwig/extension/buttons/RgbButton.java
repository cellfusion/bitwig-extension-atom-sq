package jp.cellfusion.bitwig.extension.buttons;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

public interface RgbButton {

    HardwareButton getHwButton();

    MultiStateHardwareLight getLight();

    int getMidiStatus();

    int getMidiDataNr();
}
