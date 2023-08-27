package jp.cellfusion.bitwig.extension;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.framework.values.Midi;

public enum CcAssignment {
    PLAY(0x6D),
    STOP(0x6F),
    RECORD(0x6B),
    TAPMETRO(0x69),

    MODE_SONG(0x20),
    MODE_INST(0x21),
    MODE_EDITOR(0x22),
    MODE_USER(0x23),

    SHIFT(0x1F),

    ARROW_UP(0x57),
    ARROW_DOWN(0x59),
    ARROW_LEFT(0x5A),
    ARROW_RIGHT(0x66),

    MAIN_ENCODER(0x1D),

    LEFT(0x2A),
    RIGHT(0x2B),

    PITCH(0x40, 0xB0),

    PLUS(0x00, 0x90),
    MINUS(0x01, 0x90);

    private int ccNr;
    private int channel;

    CcAssignment(final int ccNr, final int channel) {
        this.ccNr = ccNr;
        this.channel = channel;
    }

    CcAssignment(final int ccNr) {
        this.ccNr = ccNr;
        this.channel = 0;
    }

    public int getCcNr() {
        return ccNr;
    }

    public void setCcNr(final int ccNr) {
        this.ccNr = ccNr;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(final int channel) {
        this.channel = channel;
    }

    public HardwareActionMatcher createActionMatcher(final MidiIn midiIn, final int matchvalue) {
        return midiIn.createCCActionMatcher(channel, ccNr, matchvalue);
    }

    public int getType() {
        return Midi.CC | channel;
    }
}
