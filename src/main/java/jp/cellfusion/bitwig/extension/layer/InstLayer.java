package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;

public class InstLayer extends Layer {
    private final AtomSQExtension driver;

    public InstLayer(AtomSQExtension driver) {
        super(driver.layers, "INST_LAYER");

        this.driver = driver;
    }

    @Override
    protected void onActivate() {
        super.onActivate();
    }
}
