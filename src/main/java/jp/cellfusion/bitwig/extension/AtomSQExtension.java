package jp.cellfusion.bitwig.extension;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import jp.cellfusion.bitwig.extension.buttons.RgbButton;
import jp.cellfusion.bitwig.extension.layer.BrowserLayer;
import jp.cellfusion.bitwig.extension.layer.DrumLayer;
import jp.cellfusion.bitwig.extension.layer.KeyboardLayer;
import jp.cellfusion.bitwig.extension.layer.SongLayer;
import jp.cellfusion.bitwig.extension.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class AtomSQExtension extends ControllerExtension {

    private static final int[] ALPHABET_CC_MAPPING = new int[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    private static final int[] DISPLAY_BUTTON_CC_MAPPING = new int[]{0x24, 0x25, 0x26, 0x27, 0x28, 0x29};
    private static final int[] ENCODER_CC_MAPPING = new int[]{0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15};
    private static final int[] PAD_CC_MAPPING = new int[]{0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40, 0x41, 0x42, 0x43};

    private static final Color WHITE = Color.fromRGB(1, 1, 1);

    private static final Color BLACK = Color.fromRGB(0, 0, 0);

    private static final Color RED = Color.fromRGB(1, 0, 0);

    private static final Color DIM_RED = Color.fromRGB(0.3, 0.0, 0.0);

    private static final Color GREEN = Color.fromRGB(0, 1, 0);

    private static final Color ORANGE = Color.fromRGB(1, 1, 0);

    private static final Color BLUE = Color.fromRGB(0, 0, 1);
    private static final int LAUNCHER_SCENES = 16;

    private CursorTrack cursorTrack;
    private PinnableCursorDevice cursorDevice;
    private CursorRemoteControlsPage cursorRemoteControlsPage;
    private HardwareSurface hardwareSurface;

    private Application mApplication;
    private final BooleanValueObject shiftDown = new BooleanValueObject();
    public Transport transport;
    private PinnableCursorDevice primaryDevice;
    private NoteInput mNoteInput;
    private PlayingNote[] mPlayingNotes;
    private DrumPadBank drumPadBank;
    private PinnableCursorClip launcherCursorClip;
    private int mPlayingStep;
    private final int[] mStepData = new int[16];
    private int mCurrentPadForSteps;
    private SceneBank mSceneBank;
    private ControllerHost host;

    private PopupBrowser browser;
    private DeviceBank deviceBank;

    // encoder
    private RelativeHardwareKnob mainEncoder;
    public final RelativeHardwareKnob[] mEncoders = new RelativeHardwareKnob[ENCODER_CC_MAPPING.length];

    // midi
    public MidiIn midiIn;
    public MidiOut midiOut;

    // buttons
    public HardwareButton mShiftButton, mUpButton, mDownButton, mLeftButton, mRightButton, mClickCountInButton, mRecordSaveButton, mPlayLoopButton, mStopUndoButton, mSongButton, mInstButton, mEditorButton, mUserButton;
    public final HardwareButton[] mPadButtons = new HardwareButton[PAD_CC_MAPPING.length];
    public final HardwareButton[] mAlphabetButtons = new HardwareButton[ALPHABET_CC_MAPPING.length];
    public final HardwareButton[] mDisplayButtons = new HardwareButton[DISPLAY_BUTTON_CC_MAPPING.length];
    public final MultiStateHardwareLight[] mPadLights = new MultiStateHardwareLight[PAD_CC_MAPPING.length];

    // layers
    public Layers layers;
    private Layer mBaseLayer;
    private BrowserLayer browserLayer;
    private Layer shiftLayer;
    private DrumLayer mDrumLayer;
    private KeyboardLayer mKeyboardLayer;
    private SongLayer mSongLayer;


    public Layers getLayers() {
        return layers;
    }

    public PinnableCursorDevice getCursorDevice() {
        return cursorDevice;
    }

    public RelativeHardwareKnob getMainEncoder() {
        return mainEncoder;
    }

    public RelativeHardwareKnob getEncoder(int index) {
        assert index >= 0 && index < mEncoders.length;
        return mEncoders[index];
    }

    public HardwareButton getAlphabetButton(int index) {
        assert index >= 0 && index < mAlphabetButtons.length;
        return mAlphabetButtons[index];
    }

    public HardwareButton[] getAlphabetButtons() {
        return mAlphabetButtons;
    }

    public HardwareButton[] getPadButtons() {
        return mPadButtons;
    }

    public PlayingNote[] getPlayingNotes() {
        return mPlayingNotes;
    }

    public BooleanValueObject getShiftDown() {
        return shiftDown;
    }

    public PinnableCursorDevice getPrimaryDevice() {
        return primaryDevice;
    }


    protected AtomSQExtension(final AtomSQExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        host = getHost();
        LogUtil.init(host);
        LogUtil.println("Atom SQ Initialized");
        mApplication = host.createApplication();
        layers = new Layers(this);

        midiIn = host.getMidiInPort(0);
        midiOut = host.getMidiOutPort(0);

        hardwareSurface = host.createHardwareSurface();

        // Turn on Native Mode
        midiOut.sendMidi(0x8f, 0, 127);

        mNoteInput = midiIn.createNoteInput("Pads", getNoteInputMask());
        mNoteInput.setShouldConsumeEvents(true);

        cursorTrack = host.createCursorTrack(0, LAUNCHER_SCENES);
        cursorTrack.arm().markInterested();

        mSceneBank = host.createSceneBank(LAUNCHER_SCENES);
        for (int s = 0; s < LAUNCHER_SCENES; s++) {
            final ClipLauncherSlot slot = cursorTrack.clipLauncherSlotBank().getItemAt(s);
            slot.color().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.isRecordingQueued().markInterested();
            slot.hasContent().markInterested();

            final Scene scene = mSceneBank.getScene(s);
            scene.color().markInterested();
            scene.exists().markInterested();
        }

        deviceBank = cursorTrack.createDeviceBank(4);

        cursorDevice = cursorTrack.createCursorDevice();
        cursorDevice.exists().markInterested();

        cursorRemoteControlsPage = cursorDevice.createCursorRemoteControlsPage(ENCODER_CC_MAPPING.length);
        cursorRemoteControlsPage.setHardwareLayout(HardwareControlType.ENCODER, ENCODER_CC_MAPPING.length);

        for (int i = 0; i < ENCODER_CC_MAPPING.length; i++) {
            final RemoteControl parameter = cursorRemoteControlsPage.getParameter(i);
            parameter.setIndication(true);
            parameter.markInterested();
            parameter.exists().markInterested();

            parameter.name().markInterested();
            parameter.name().addValueObserver(newValue -> {
                getHost().requestFlush();
            });
        }

        transport = host.createTransport();
        transport.isPlaying().markInterested();
        transport.getPosition().markInterested();

        launcherCursorClip = cursorTrack.createLauncherCursorClip(16, 1);
        launcherCursorClip.color().markInterested();
        launcherCursorClip.clipLauncherSlot().color().markInterested();
        launcherCursorClip.clipLauncherSlot().isPlaying().markInterested();
        launcherCursorClip.clipLauncherSlot().isRecording().markInterested();
        launcherCursorClip.clipLauncherSlot().isPlaybackQueued().markInterested();
        launcherCursorClip.clipLauncherSlot().isRecordingQueued().markInterested();
        launcherCursorClip.clipLauncherSlot().hasContent().markInterested();
        launcherCursorClip.getLoopLength().markInterested();
        launcherCursorClip.getLoopStart().markInterested();
        launcherCursorClip.playingStep().addValueObserver(s -> mPlayingStep = s, -1);
        launcherCursorClip.scrollToKey(36);
        launcherCursorClip.addNoteStepObserver(d -> {
            final int x = d.x();
            final int y = d.y();

            if (y == 0 && x >= 0 && x < mStepData.length) {
                final NoteStep.State state = d.state();

                if (state == NoteStep.State.NoteOn)
                    mStepData[x] = 2;
                else if (state == NoteStep.State.NoteSustain)
                    mStepData[x] = 1;
                else
                    mStepData[x] = 0;
            }
        });
        cursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);

        drumPadBank = cursorDevice.createDrumPadBank(PAD_CC_MAPPING.length);
        drumPadBank.exists().markInterested();

        cursorTrack.color().markInterested();

        setUpHardware();

        mBaseLayer = new Layer(layers, "BASE");
        shiftLayer = new Layer(layers, "SHIFT");
        bindEncoder(mBaseLayer, mainEncoder, this::mainEncoderAction);
        bindEncoder(shiftLayer, mainEncoder, this::mainEncoderShiftAction);

        initBrowserSection();

        mDrumLayer = new DrumLayer(this);
        mKeyboardLayer = new KeyboardLayer(this);

        // TODO primary device が drum machine だったら drum layer を表示する
        primaryDevice = cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        primaryDevice.exists().markInterested();
        primaryDevice.hasDrumPads().markInterested();
        primaryDevice.hasDrumPads().addValueObserver(v -> {
            if (v) {
                mDrumLayer.activate();
                mKeyboardLayer.deactivate();
            } else {
                mDrumLayer.deactivate();
                mKeyboardLayer.activate();
            }
        });

        initLayers();

        // init display
        AtomSQUtils.writeDisplay(6, "Bitwig Studio", midiOut);
        AtomSQUtils.writeDisplay(7, "ATOM SQ", midiOut);

        // For now just show a popup notification for verification that it is running.
        host.showPopupNotification("Atom SQ Initialized");
    }

    private void initBrowserSection() {
        browser = host.createPopupBrowser();
        browser.exists().markInterested();

        browserLayer = new BrowserLayer(this);

        mBaseLayer.bindPressed(mAlphabetButtons[0], pressed -> {
            if (browser.exists().get()) {
                browser.commit();
            } else {
                if (cursorDevice.exists().get()) {
                    cursorDevice.afterDeviceInsertionPoint().browse();
                } else {
                    deviceBank.browseToInsertDevice(0);
                }
            }
        });
        shiftLayer.bindPressed(mAlphabetButtons[0], pressed -> {
            if (browser.exists().get()) {
                browser.cancel();
            } else {
                if (cursorDevice.exists().get()) {
                    cursorDevice.replaceDeviceInsertionPoint().browse();
                } else {
                    deviceBank.browseToInsertDevice(0);
                }
            }
        });
    }

    // track select
    private void mainEncoderAction(final int dir) {
        if (dir > 0) {
            cursorTrack.selectNext();
        } else {
            cursorTrack.selectPrevious();
        }
    }

    private void mainEncoderShiftAction(final int dir) {
    }


    @Override
    public void exit() {
        // Turn off Native Mode
        midiOut.sendMidi(0x8f, 0, 0);

        // For now just show a popup notification for verification that it is no longer running.
        getHost().showPopupNotification("Atom SQ Exited");
    }

    @Override
    public void flush() {
        hardwareSurface.updateHardware();
    }

    private void handleShift(final boolean pressed) {
        shiftDown.set(pressed);
        if (pressed) {
            shiftLayer.activate();
        } else {
            shiftLayer.deactivate();
        }
        layers.setGlobalSensitivity(pressed ? 0.1 : 1);
    }

    private void setUpHardware() {
        mShiftButton = createToggleButton("shift", CcAssignment.SHIFT.getCcNr(), ORANGE);
        mShiftButton.setLabel("Shift");
        mShiftButton.isPressed().addValueObserver(this::handleShift);

        // NAV section
        mUpButton = createToggleButton("up", CcAssignment.ARROW_UP.getCcNr(), ORANGE);
        mUpButton.setLabel("Up");
        mDownButton = createToggleButton("down", CcAssignment.ARROW_DOWN.getCcNr(), ORANGE);
        mDownButton.setLabel("Down");
        mLeftButton = createToggleButton("left", CcAssignment.LEFT.getCcNr(), ORANGE);
        mLeftButton.setLabel("Left");
        mRightButton = createToggleButton("right", CcAssignment.RIGHT.getCcNr(), ORANGE);
        mRightButton.setLabel("Right");

        // Mode section
        mSongButton = createToggleButton("song", CcAssignment.MODE_SONG.getCcNr(), BLUE);
        mSongButton.setLabel("Song");
        mInstButton = createToggleButton("inst", CcAssignment.MODE_INST.getCcNr(), BLUE);
        mInstButton.setLabel("Inst");
        mEditorButton = createToggleButton("editor", CcAssignment.MODE_EDITOR.getCcNr(), BLUE);
        mEditorButton.setLabel("Editor");
        mUserButton = createToggleButton("user", CcAssignment.MODE_USER.getCcNr(), BLUE);
        mUserButton.setLabel("User");


        // TRANS section
        mClickCountInButton = createToggleButton("click_count_in", CcAssignment.TAPMETRO.getCcNr(), BLUE);
        mClickCountInButton.setLabel("Click\nCount in");
        mRecordSaveButton = createToggleButton("record_save", CcAssignment.RECORD.getCcNr(), RED);
        mRecordSaveButton.setLabel("Record\nSave");
        mPlayLoopButton = createToggleButton("play_loop", CcAssignment.PLAY.getCcNr(), GREEN);
        mPlayLoopButton.setLabel("Play\nLoop");
        mStopUndoButton = createToggleButton("stop_undo", CcAssignment.STOP.getCcNr(), ORANGE);
        mStopUndoButton.setLabel("Stop\nUndo");


        // Alphabet section
        for (int i = 0; i < ALPHABET_CC_MAPPING.length; i++) {
            HardwareButton button = createToggleButton("alphabet" + (i + 1), ALPHABET_CC_MAPPING[i], ORANGE);
            button.isPressed().markInterested();
            button.setLabel("Alphabet " + (i + 1));

            mAlphabetButtons[i] = button;
        }

        // Display section
        for (int i = 0; i < DISPLAY_BUTTON_CC_MAPPING.length; i++) {
            HardwareButton button = createToggleButton("display" + (i + 1), DISPLAY_BUTTON_CC_MAPPING[i], ORANGE);
            button.setLabel("Display " + (i + 1));

            mDisplayButtons[i] = button;
        }

        // Pads
        for (int i = 0; i < PAD_CC_MAPPING.length; i++) {
            final DrumPad drumPad = drumPadBank.getItemAt(i);
            drumPad.exists().markInterested();
            drumPad.color().markInterested();

            createPadButton(i, PAD_CC_MAPPING[i]);
        }

        // Encoder
        mainEncoder = createMainEncoder(CcAssignment.MAIN_ENCODER.getCcNr());
        for (int i = 0; i < ENCODER_CC_MAPPING.length; i++) {
            final RelativeHardwareKnob encoder = createEncoder(ENCODER_CC_MAPPING[i]);
            mEncoders[i] = encoder;
        }

        initHardwareLayout();
    }

    private void initHardwareLayout() {
        final HardwareSurface surface = hardwareSurface;
        surface.hardwareElementWithId("shift").setBounds(12.25, 175.25, 12.0, 9.0);
        surface.hardwareElementWithId("up").setBounds(178.25, 21.75, 14.0, 10.0);
        surface.hardwareElementWithId("down").setBounds(178.25, 37.0, 14.0, 10.0);
        surface.hardwareElementWithId("left").setBounds(178.25, 52.0, 14.0, 10.0);
        surface.hardwareElementWithId("right").setBounds(178.25, 67.25, 14.0, 10.0);
    }

    private RelativeHardwareKnob createMainEncoder(final int ccNumber) {
        final String id = "MAIN_ENCODER_" + ccNumber;
        final RelativeHardwareKnob encoder = hardwareSurface.createRelativeHardwareKnob(id);
        final RelativeHardwareValueMatcher stepUpMatcher = midiIn.createRelativeValueMatcher(
                "(status == 176 && data1 == " + ccNumber + " && data2 > 64)", 1);
        final RelativeHardwareValueMatcher stepDownMatcher = midiIn.createRelativeValueMatcher(
                "(status == 176 && data1 == " + ccNumber + " && data2 < 63)", -1);
        final RelativeHardwareValueMatcher matcher = host.createOrRelativeHardwareValueMatcher(stepDownMatcher,
                stepUpMatcher);
        encoder.setAdjustValueMatcher(matcher);
        encoder.isUpdatingTargetValue().markInterested();
        encoder.setLabel(id);
        //encoder.setLabelPosition(RelativePosition.ABOVE);
        encoder.setStepSize(1);

        return encoder;
    }

    private RelativeHardwareKnob createEncoder(final int ccNumber) {
        final String id = "ENCODER_" + ccNumber;

        final RelativeHardwareKnob encoder = hardwareSurface.createRelativeHardwareKnob(id);
        encoder.setAdjustValueMatcher(midiIn.createRelativeSignedBitCCValueMatcher(0, ccNumber, 50));
        encoder.isUpdatingTargetValue().markInterested();
        encoder.setLabel(id);
        encoder.setLabelPosition(RelativePosition.ABOVE);

        return encoder;
    }

    private HardwareButton createToggleButton(
            final String id,
            final int controlNumber,
            final Color onLightColor) {
        final HardwareButton button = createButton(id, controlNumber);
        final OnOffHardwareLight light = hardwareSurface.createOnOffHardwareLight(id + "_light");

        final Color offColor = Color.mix(onLightColor, Color.blackColor(), 0.5);

        light.setStateToVisualStateFunction(
                isOn -> isOn ? HardwareLightVisualState.createForColor(onLightColor, Color.blackColor())
                        : HardwareLightVisualState.createForColor(offColor, Color.blackColor()));

        button.setBackgroundLight(light);

        light.isOn().onUpdateHardware(value -> {
            midiOut.sendMidi(0xB0, controlNumber, value ? 127 : 0);
        });

        return button;
    }

    private HardwareButton createButton(final String id, final int controlNumber) {
        final HardwareButton button = hardwareSurface.createHardwareButton(id);
        final MidiExpressions midiExpressions = getHost().midiExpressions();

        button.pressedAction().setActionMatcher(midiIn
                .createActionMatcher(midiExpressions.createIsCCExpression(0, controlNumber) + " && data2 > 0"));
        button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, controlNumber, 0));
        button.setLabelColor(BLACK);

        return button;
    }

    private void createPadButton(final int index, final int ccNumber) {
        final HardwareButton pad = hardwareSurface.createHardwareButton("pad" + (index + 1));
        pad.setLabel("Pad " + (index + 1));
        pad.setLabelColor(BLACK);

        pad.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, ccNumber));
        pad.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, ccNumber));

        mPadButtons[index] = pad;

        final MultiStateHardwareLight light = hardwareSurface
                .createMultiStateHardwareLight("pad_light" + (index + 1));

        light.state().onUpdateHardware(new LightStateSender(0x90, ccNumber));

        light.setColorToStateFunction(RgbLightState::new);

        pad.setBackgroundLight(light);

        mPadLights[index] = light;
    }

    private void initLayers() {
        initBaseLayer();

        // TODO Song/Inst/Editor/User Layer の切り替え
        mSongLayer = new SongLayer(this);

        mBaseLayer.bindToggle(mSongButton, mSongLayer::activate, mSongLayer::isActive);

        // 初期状態は Song Layer
        mSongLayer.activate();

        DebugUtilities.createDebugLayer(layers, hardwareSurface).activate();
    }

    private void initBaseLayer() {

        // transport
        mBaseLayer.bindToggle(mClickCountInButton, transport.isMetronomeEnabled());
        mBaseLayer.bindToggle(mPlayLoopButton, () -> {
            if (shiftDown.get()) transport.isArrangerLoopEnabled().toggle();
            else transport.play();
        }, transport.isPlaying());

        mBaseLayer.bindToggle(mStopUndoButton, () -> {
            if (shiftDown.get()) mApplication.undo();
            else transport.stop();
        }, () -> !transport.isPlaying().get());

        mBaseLayer.bindToggle(mRecordSaveButton, () -> {
            if (shiftDown.get()) save();
            else transport.isArrangerRecordEnabled().toggle();
        }, transport.isArrangerRecordEnabled());

        // nav
        mBaseLayer.bindToggle(mUpButton, cursorTrack.selectPreviousAction(), cursorTrack.hasPrevious());
        mBaseLayer.bindToggle(mDownButton, cursorTrack.selectNextAction(), cursorTrack.hasNext());
        mBaseLayer.bindToggle(mLeftButton, cursorDevice.selectPreviousAction(), cursorDevice.hasPrevious());
        mBaseLayer.bindToggle(mRightButton, cursorDevice.selectNextAction(), cursorDevice.hasNext());

        // encoder
        for (int i = 0; i < ENCODER_CC_MAPPING.length; i++) {
            final Parameter parameter = cursorRemoteControlsPage.getParameter(i);
            final RelativeHardwareKnob encoder = mEncoders[i];

            mBaseLayer.bind(encoder, parameter);
        }


        mBaseLayer.activate();
    }

    private Color getClipColor(final ClipLauncherSlot s) {
        if (s.isRecordingQueued().get()) {
            return Color.mix(RED, BLACK, getTransportPulse(1.0, 1));
        } else if (s.hasContent().get()) {
            if (s.isPlaybackQueued().get()) {
                return Color.mix(s.color().get(), WHITE, 1 - getTransportPulse(4.0, 1));
            } else if (s.isRecording().get()) {
                return RED;
            } else if (s.isPlaying().get() && transport.isPlaying().get()) {
                return Color.mix(s.color().get(), WHITE, 1 - getTransportPulse(1.0, 1));
            }

            return s.color().get();
        } else if (cursorTrack.arm().get()) {
            return Color.mix(BLACK, RED, 0.1f);
        }

        return BLACK;
    }

    private float getTransportPulse(final double multiplier, final double amount) {
        final double p = transport.getPosition().get() * multiplier;
        return (float) ((0.5 + 0.5 * Math.cos(p * 2 * Math.PI)) * amount);
    }

    private void save() {
        final Action saveAction = mApplication.getAction("Save");
        if (saveAction != null) {
            saveAction.invoke();
        }
    }

    private String[] getNoteInputMask() {
        final List<String> masks = new ArrayList<>();
        masks.add("80????"); // Note On
        masks.add("90????"); // Note Off
        masks.add("D???"); // Channel Pressure
        return masks.toArray(String[]::new);
    }

    public CursorTrack getCursorTrack() {
        return cursorTrack;
    }

    public void bindEncoder(final Layer layer, final RelativeHardwareKnob encoder, final IntConsumer action) {
        final HardwareActionBindable incAction = host.createAction(() -> action.accept(1), () -> "+");
        final HardwareActionBindable decAction = host.createAction(() -> action.accept(-1), () -> "-");
        layer.bind(encoder, host.createRelativeHardwareControlStepTarget(incAction, decAction));
    }

    public void debugLog(final String label, final String message) {
        AtomSQUtils.writeDisplay(6, label, midiOut);
        AtomSQUtils.writeDisplay(7, message, midiOut);
    }

    public PopupBrowser getBrowser() {
        return browser;
    }

    public MidiIn getMidiIn() {
        return midiIn;
    }

    public MidiOut getMidiOut() {
        return midiOut;
    }

    public HardwareSurface getSurface() {
        return hardwareSurface;
    }

    public DrumPadBank getDrumPadBank() {
        return drumPadBank;
    }

    public void updatePadLed(final RgbButton button) {
        final RgbLed state = (RgbLed) button.getLight().state().currentValue();
        if (state != null) {
            midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), state.getColor());
        } else {
            midiOut.sendMidi(button.getMidiStatus(), button.getMidiDataNr(), 0);
        }
    }


    private class LightStateSender implements Consumer<RgbLightState> {
        protected LightStateSender(final int statusStart, final int data1) {
            super();
            mStatusStart = statusStart;
            mData1 = data1;
        }

        @Override
        public void accept(final RgbLightState state) {
            mValues[0] = state != null ? (state.isOn() ? 127 : 0) : 0;
            mValues[1] = state != null ? state.getRed() : 0;
            mValues[2] = state != null ? state.getGreen() : 0;
            mValues[3] = state != null ? state.getBlue() : 0;

            for (int i = 0; i < 4; i++) {
                if (mValues[i] != mLastSent[i]) {
                    midiOut.sendMidi(mStatusStart + i, mData1, mValues[i]);
                    mLastSent[i] = mValues[i];
                }
            }
        }

        private final int mStatusStart, mData1;

        private final int[] mLastSent = {-1, -1, -1, -1};

        private final int[] mValues = new int[4];
    }
}
