package jp.cellfusion.bitwig.extension;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.MidiOut;

public class AtomSQUtils {
    public static void writeDisplay(int place, String text, MidiOut midiOut) {
        final SysexBuilder sb = SysexBuilder.fromHex("F0 00 01 06 22 12");
        sb.addByte(place);
        sb.addHex("00 5B 5B 00");
        sb.addString(text, text.length());
        midiOut.sendSysex(sb.terminate());
    }
}
