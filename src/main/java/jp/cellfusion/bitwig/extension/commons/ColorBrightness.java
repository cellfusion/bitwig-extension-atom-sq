package jp.cellfusion.bitwig.extension.commons;

public enum ColorBrightness {
    DARKENED(0),  //
    BRIGHT(2), //
    DIMMED(1),  //
    SUPERBRIGHT(3); //

    private final int adjust;

    ColorBrightness(final int adjust) {
        this.adjust = adjust;
    }

    public int getAdjust() {
        return adjust;
    }
}
