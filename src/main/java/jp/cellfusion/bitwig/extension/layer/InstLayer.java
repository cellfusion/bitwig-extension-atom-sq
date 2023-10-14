package jp.cellfusion.bitwig.extension.layer;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.LayerGroup;
import jp.cellfusion.bitwig.extension.AtomSQExtension;

public class InstLayer extends Layer {
    private final AtomSQExtension driver;
    private final LayerGroup mLayerGroup;
    private final InstBrowseLayer mBrowseLayer;

    public InstLayer(AtomSQExtension driver) {
        super(driver.layers, "INST_LAYER");

        mBrowseLayer = new InstBrowseLayer(driver);
        mLayerGroup = new LayerGroup(mBrowseLayer);

        // encoder
        for (int i = 0; i < driver.mEncoders.length; i++) {
            final Parameter parameter = driver.cursorRemoteControlsPage.getParameter(i);
            final RelativeHardwareKnob encoder = driver.mEncoders[i];

            bind(encoder, parameter);
        }

        this.driver = driver;
    }

    @Override
    protected void onActivate() {
        super.onActivate();

        mBrowseLayer.activate();
    }
}
