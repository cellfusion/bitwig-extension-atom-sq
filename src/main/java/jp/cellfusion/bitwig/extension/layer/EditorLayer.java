package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extensions.framework.Layer;
import jp.cellfusion.bitwig.extension.AtomSQExtension;

public class EditorLayer extends Layer {
    private final AtomSQExtension driver;

    public EditorLayer(AtomSQExtension driver) {
        super(driver.layers, "EDITOR_LAYER");

        this.driver = driver;
    }

    @Override
    protected void onActivate() {
        super.onActivate();
    }
}
