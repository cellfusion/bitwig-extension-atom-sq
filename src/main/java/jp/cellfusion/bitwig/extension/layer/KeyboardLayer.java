package jp.cellfusion.bitwig.extension.layer;


import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;

public class KeyboardLayer extends Layer {
    public KeyboardLayer(AtomSQExtension driver) {
        super(driver.getLayers(), "KEYBOARD_LAYER");
    }
}
