package jp.cellfusion.bitwig.extension;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.BooleanObject;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.util.NoteInputUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AtomSQExtension extends ControllerExtension
{
   private final static int CC_ENCODER_1 = 0x0E;
   private final static int CC_ALPHABET_1 = 0x00;
   private final static int CC_PAD_1 = 0x24;
   private final static int CC_DISPLAY_1 = 0x24;
   private final static int CC_STOP_UNDO = 0x6F;
   private final static int CC_PLAY_LOOP_TOGGLE = 0x6D;
   private final static int CC_RECORD_SAVE = 0x6B;
   private final static int CC_CLICK_COUNT_IN = 0x69;

   private final static int CC_SONG = 0x20;
   private final static int CC_INST = 0x21;
   private final static int CC_EDITOR = 0x22;
   private final static int CC_USER = 0x23;

   private final static int CC_SHIFT = 0x1F;
   private final static int CC_UP = 0x57;
   private final static int CC_DOWN = 0x59;
   private final static int CC_LEFT = 0x5A;
   private final static int CC_RIGHT = 0x66;

   private final static int ENCODER_NUM = 8;
   private final static int PAD_NUM = 32;
   private final static int ALPHABET_NUM = 8;
   private final static int DISPLAY_NUM = 6;

   private static final Color WHITE = Color.fromRGB(1, 1, 1);

   private static final Color BLACK = Color.fromRGB(0, 0, 0);

   private static final Color RED = Color.fromRGB(1, 0, 0);

   private static final Color DIM_RED = Color.fromRGB(0.3, 0.0, 0.0);

   private static final Color GREEN = Color.fromRGB(0, 1, 0);

   private static final Color ORANGE = Color.fromRGB(1, 1, 0);

   private static final Color BLUE = Color.fromRGB(0, 0, 1);
   private static final int LAUNCHER_SCENES = 16;

   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mCursorRemoteControls;
   private HardwareSurface mHardwareSurface;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private HardwareButton mShiftButton, mUpButton, mDownButton, mLeftButton, mRightButton, mClickCountInButton, mRecordSaveButton, mPlayLoopButton, mStopUndoButton, mSongButton, mInstButton, mEditorButton, mUserButton;

   private final RelativeHardwareKnob[] mEncoders = new RelativeHardwareKnob[ENCODER_NUM];
   private final HardwareButton[] mPadButtons = new HardwareButton[PAD_NUM];

   private final HardwareButton[] mAlphabetButtons = new HardwareButton[ALPHABET_NUM];
   private final HardwareButton[] mDisplayButtons = new HardwareButton[DISPLAY_NUM];

   private final MultiStateHardwareLight[] mPadLights = new MultiStateHardwareLight[PAD_NUM];

   private final Layers mLayers = new Layers(this)
   {
      @Override
      protected void activeLayersChanged()
      {
         super.activeLayersChanged();
      }
   };

   private Application mApplication;
   private Layer mSongLayer;
   private Layer mInstrumentLayer;
   private Layer mEditorLayer;
   private boolean mShift;
   private Transport mTransport;
   private NoteInput mNoteInput;
   private PlayingNote[] mPlayingNotes;
   private DrumPadBank mDrumPadBank;
   private PinnableCursorClip mCursorClip;
   private int mPlayingStep;
   private final int[] mStepData = new int[16];
   private int mCurrentPadForSteps;
   private SceneBank mSceneBank;


   protected AtomSQExtension(final AtomSQExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      mApplication = host.createApplication();

      mMidiIn = host.getMidiInPort(0);
      mMidiOut = host.getMidiOutPort(0);

      mNoteInput = mMidiIn.createNoteInput("Pads", getNoteInputMask());
      mNoteInput.setShouldConsumeEvents(true);

      mCursorTrack = host.createCursorTrack(0, LAUNCHER_SCENES);
      mCursorTrack.arm().markInterested();
      mSceneBank = host.createSceneBank(LAUNCHER_SCENES);
      for (int s = 0; s < LAUNCHER_SCENES; s++) {
         final ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(s);
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

      mCursorDevice = mCursorTrack.createCursorDevice("ATOM_SQ", "Atom SQ", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(ENCODER_NUM);
      mCursorRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, ENCODER_NUM);

      for (int i = 0; i < ENCODER_NUM; i++) {
         final RemoteControl parameter = mCursorRemoteControls.getParameter(i);
         parameter.setIndication(true);
         parameter.markInterested();
         parameter.exists().markInterested();

         parameter.name().markInterested();
         parameter.name().addValueObserver(newValue -> {
//            mShouldFlushSysex = true;
            getHost().requestFlush();
         });
      }

      mTransport = host.createTransport();
      mTransport.isPlaying().markInterested();
      mTransport.getPosition().markInterested();

      mCursorClip = mCursorTrack.createLauncherCursorClip(16, 1);
      mCursorClip.color().markInterested();
      mCursorClip.clipLauncherSlot().color().markInterested();
      mCursorClip.clipLauncherSlot().isPlaying().markInterested();
      mCursorClip.clipLauncherSlot().isRecording().markInterested();
      mCursorClip.clipLauncherSlot().isPlaybackQueued().markInterested();
      mCursorClip.clipLauncherSlot().isRecordingQueued().markInterested();
      mCursorClip.clipLauncherSlot().hasContent().markInterested();
      mCursorClip.getLoopLength().markInterested();
      mCursorClip.getLoopStart().markInterested();
      mCursorClip.playingStep().addValueObserver(s -> mPlayingStep = s, -1);
      mCursorClip.scrollToKey(36);
      mCursorClip.addNoteStepObserver(d -> {
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
      mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);

      mDrumPadBank = mCursorDevice.createDrumPadBank(PAD_NUM);
      mDrumPadBank.exists().markInterested();

      mCursorTrack.color().markInterested();

      createHardwareSurface();

      initLayers();

      // Turn on Native Mode
      mMidiOut.sendMidi(0x8f, 0, 127);

      // For now just show a popup notification for verification that it is running.
      host.showPopupNotification("Atom SQ Initialized");
   }



   @Override
   public void exit()
   {
      // Turn off Native Mode
      mMidiOut.sendMidi(0x8f, 0, 0);

      // For now just show a popup notification for verification that it is no longer running.
      getHost().showPopupNotification("Atom SQ Exited");
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   private void setIsShiftPressed(final boolean value)
   {
      if (value != mShift)
      {
         mShift = value;
         mLayers.setGlobalSensitivity(value ? 0.1 : 1);
      }
   }

   private void createHardwareSurface()
   {
      final ControllerHost host = getHost();
      final HardwareSurface surface = host.createHardwareSurface();
      mHardwareSurface = surface;

      mShiftButton = createToggleButton("shift", CC_SHIFT, ORANGE);
      mShiftButton.setLabel("Shift");

      // NAV section
      mUpButton = createToggleButton("up", CC_UP, ORANGE);
      mUpButton.setLabel("Up");
      mDownButton = createToggleButton("down", CC_DOWN, ORANGE);
      mDownButton.setLabel("Down");
      mLeftButton = createToggleButton("left", CC_LEFT, ORANGE);
      mLeftButton.setLabel("Left");
      mRightButton = createToggleButton("right", CC_RIGHT, ORANGE);
      mRightButton.setLabel("Right");

      // Mode section
      mSongButton = createToggleButton("song", CC_SONG, BLUE);
      mSongButton.setLabel("Song");
      mInstButton = createToggleButton("inst", CC_INST, BLUE);
      mInstButton.setLabel("Inst");
      mEditorButton = createToggleButton("editor", CC_EDITOR, BLUE);
      mEditorButton.setLabel("Editor");
      mUserButton = createToggleButton("user", CC_USER, BLUE);
      mUserButton.setLabel("User");


      // TRANS section
      mClickCountInButton = createToggleButton("click_count_in", CC_CLICK_COUNT_IN, BLUE);
      mClickCountInButton.setLabel("Click\nCount in");
      mRecordSaveButton = createToggleButton("record_save", CC_RECORD_SAVE, RED);
      mRecordSaveButton.setLabel("Record\nSave");
      mPlayLoopButton = createToggleButton("play_loop", CC_PLAY_LOOP_TOGGLE, GREEN);
      mPlayLoopButton.setLabel("Play\nLoop");
      mStopUndoButton = createToggleButton("stop_undo", CC_STOP_UNDO, ORANGE);
      mStopUndoButton.setLabel("Stop\nUndo");


      // Alphabet section
      for (int i = 0; i < ALPHABET_NUM; i++) {
         HardwareButton button = createButton("alphabet" + (i + 1), CC_ALPHABET_1 + i);
         button.setLabel("Alphabet " + (i + 1));

         mAlphabetButtons[i] = button;
      }

      // Display section
      for (int i = 0; i < DISPLAY_NUM; i++) {
         HardwareButton button = createToggleButton("display" + (i + 1), CC_DISPLAY_1 + i, ORANGE);
         button.setLabel("Display " + (i + 1));

         mDisplayButtons[i] = button;
      }

      // Pads
      for (int i = 0; i < PAD_NUM; i++) {
         final DrumPad drumPad = mDrumPadBank.getItemAt(i);
         drumPad.exists().markInterested();
         drumPad.color().markInterested();

         createPadButton(i);
      }

      // Encoder
      for (int i = 0; i < ENCODER_NUM; i++) {
         createEncoder(i);
      }

      initHardwareLayout();
   }

   private void initHardwareLayout()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("shift").setBounds(12.25, 175.25, 12.0, 9.0);
      surface.hardwareElementWithId("up").setBounds(178.25, 21.75, 14.0, 10.0);
      surface.hardwareElementWithId("down").setBounds(178.25, 37.0, 14.0, 10.0);
      surface.hardwareElementWithId("left").setBounds(178.25, 52.0, 14.0, 10.0);
      surface.hardwareElementWithId("right").setBounds(178.25, 67.25, 14.0, 10.0);
   }

   private void createEncoder(int i) {
      final String id = "encoder" + (i + 1);

      final RelativeHardwareKnob knob = mHardwareSurface.createRelativeHardwareKnob(id);
      knob.setAdjustValueMatcher(
              mMidiIn.createRelativeSignedBitCCValueMatcher(0, CC_ENCODER_1 + i, 50));
      knob.isUpdatingTargetValue().markInterested();
      knob.setLabel(id);
      knob.setIndexInGroup(i);
      knob.setLabelPosition(RelativePosition.ABOVE);
      mEncoders[i] = knob;
   }

   private HardwareButton createToggleButton(
           final String id,
           final int controlNumber,
           final Color onLightColor)
   {
      final HardwareButton button = createButton(id, controlNumber);
      final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight(id + "_light");

      final Color offColor = Color.mix(onLightColor, Color.blackColor(), 0.5);

      light.setStateToVisualStateFunction(
              isOn -> isOn ? HardwareLightVisualState.createForColor(onLightColor, Color.blackColor())
                      : HardwareLightVisualState.createForColor(offColor, Color.blackColor()));

      button.setBackgroundLight(light);

      light.isOn().onUpdateHardware(value -> {
         mMidiOut.sendMidi(0xB0, controlNumber, value ? 127 : 0);
      });

      return button;
   }

   private HardwareButton createButton(final String id, final int controlNumber)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton(id);
      final MidiExpressions midiExpressions = getHost().midiExpressions();

      button.pressedAction().setActionMatcher(mMidiIn
              .createActionMatcher(midiExpressions.createIsCCExpression(0, controlNumber) + " && data2 > 0"));
      button.releasedAction().setActionMatcher(mMidiIn.createCCActionMatcher(0, controlNumber, 0));
      button.setLabelColor(BLACK);

      return button;
   }

   private void createPadButton(final int index) {
      final HardwareButton pad = mHardwareSurface.createHardwareButton("pad" + (index + 1));
      pad.setLabel("Pad " + (index + 1));
      pad.setLabelColor(BLACK);

      final int note = CC_PAD_1 + index;
      pad.pressedAction().setPressureActionMatcher(mMidiIn.createNoteOnVelocityValueMatcher(0, note));
      pad.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, note));

      mPadButtons[index] = pad;

      final MultiStateHardwareLight light = mHardwareSurface
              .createMultiStateHardwareLight("pad_light" + (index + 1));

      light.state().onUpdateHardware(new LightStateSender(0x90, CC_PAD_1 + index));

      light.setColorToStateFunction(RGBLightState::new);

      pad.setBackgroundLight(light);

      mPadLights[index] = light;
   }

   private void initLayers()
   {
      mSongLayer = new Layer(mLayers, "Song");
      mInstrumentLayer = new Layer(mLayers, "Instrument");
      mEditorLayer = new Layer(mLayers, "Editor");

      initSongLayer();
      initInstrumentLayer();
      initEditorLayer();

      DebugUtilities.createDebugLayer(mLayers, mHardwareSurface).activate();
   }

   private void initEditorLayer() {

      // Lock
      //mEditorLayer.bindPressed(mDisplayButtons[0], () -> mCursorDevice);
      // < Device
      //mEditorLayer.bindPressed(mDisplayButtons[1], () -> mCursorTrack.mute().toggle());
      // Device >
      //mEditorLayer.bindPressed(mDisplayButtons[2], () -> mCursorTrack.arm().toggle());
      // On/Off
      mEditorLayer.bindPressed(mDisplayButtons[3], () -> mCursorDevice.isEnabled().toggle());
      // Previous Parameter Page
      mEditorLayer.bindToggle(mDisplayButtons[4], () -> mCursorRemoteControls.selectPrevious(), mCursorRemoteControls.hasPrevious());
      // Next Parameter Page
      mEditorLayer.bindToggle(mDisplayButtons[5], () -> mCursorRemoteControls.selectNext(), mCursorRemoteControls.hasNext());
   }

   private void initInstrumentLayer() {
      final BooleanObject fullLevelIsOn = new BooleanObject();

      // Velocity
      mInstrumentLayer.bindToggle(mDisplayButtons[5], () -> {
         fullLevelIsOn.toggle();

         mNoteInput.setVelocityTranslationTable(
                 fullLevelIsOn.getAsBoolean() ? NoteInputUtils.FULL_VELOCITY : NoteInputUtils.NORMAL_VELOCITY);
      }, fullLevelIsOn);

      for (int i = 0; i < PAD_NUM; i++) {
         final HardwareButton padButton = mPadButtons[i];

         final int padIndex = i;

         // TODO mode
         mInstrumentLayer.bindPressed(padButton, () -> {
            mCursorClip.scrollToKey(36 + padIndex);
            mCurrentPadForSteps = padIndex;
         });

         mInstrumentLayer.bind(() -> getDrumPadColor(padIndex), padButton);
      }
   }

   private void initSongLayer()
   {
      mSongLayer.bindIsPressed(mShiftButton, this::setIsShiftPressed);
      mSongLayer.bindToggle(mClickCountInButton, mTransport.isMetronomeEnabled());

      mSongLayer.bindToggle(mPlayLoopButton, () -> {
         if (mShift) mTransport.isArrangerLoopEnabled().toggle();
         else mTransport.play();
      }, mTransport.isPlaying());

      mSongLayer.bindToggle(mStopUndoButton, () -> {
         if (mShift) mApplication.undo();
         else mTransport.stop();
      }, () -> !mTransport.isPlaying().get());

      mSongLayer.bindToggle(mRecordSaveButton, () -> {
         if (mShift) save();
         else mTransport.isArrangerRecordEnabled().toggle();
      }, mTransport.isArrangerRecordEnabled());

      // layer
      // TODO switch layer
//      mSongLayer.bind(() -> mSongLayer.isActive() ? BLUE : null, mSongButton);
      mSongLayer.bindToggle(mInstButton, mInstrumentLayer);
      mSongLayer.bindToggle(mEditorButton, mEditorLayer);

      // nav
      mSongLayer.bindToggle(mUpButton, mCursorTrack.selectPreviousAction(), mCursorTrack.hasPrevious());
      mSongLayer.bindToggle(mDownButton, mCursorTrack.selectNextAction(), mCursorTrack.hasNext());
      mSongLayer.bindToggle(mLeftButton, mCursorDevice.selectPreviousAction(), mCursorDevice.hasPrevious());
      mSongLayer.bindToggle(mRightButton, mCursorDevice.selectNextAction(), mCursorDevice.hasNext());

      mSongLayer.bindPressed(mDisplayButtons[0], () -> mCursorTrack.solo().toggle());
//      mSongLayer.bind(() -> mCursorTrack.solo().get() ? ORANGE : null, mDisplayButtons[0]);

      mSongLayer.bindPressed(mDisplayButtons[1], () -> mCursorTrack.mute().toggle());
//      mSongLayer.bind(() -> mCursorTrack.mute().get() ? ORANGE : null, mDisplayButtons[1]);

      mSongLayer.bindPressed(mDisplayButtons[2], () -> mCursorTrack.arm().toggle());
//      mSongLayer.bind(() -> mCursorTrack.arm().get() ? ORANGE : null, mDisplayButtons[2]);

      // TODO Clip start
      // mSongLayer.bindPressed(mDisplayButtons[3], () -> mCursorTrack);
      // TODO Scene clip start
      // mSongLayer.bindPressed(mDisplayButtons[4], () -> mCursorTrack.arm().toggle());
      // TODO Stop clip stop
      // mSongLayer.bindPressed(mDisplayButtons[5], () -> mCursorTrack.arm().toggle());

      // encoder
      for (int i = 0; i < ENCODER_NUM; i++)
      {
         final Parameter parameter = mCursorRemoteControls.getParameter(i);
         final RelativeHardwareKnob encoder = mEncoders[i];

         mSongLayer.bind(encoder, parameter);
      }

      // pads
      // TODO 1-16 select instrument
      for (int i = 0; i < 16; i++) {
         final HardwareButton padButton = mPadButtons[i];

         final int padIndex = i;

      }


      // 17-32 select clip
      for (int i = 0; i < 16; i++) {
         final HardwareButton padButton = mPadButtons[i + 16];

         final ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(i);
         mSongLayer.bindPressed(padButton, () -> {
            slot.select();
            slot.launch();
         });
         mSongLayer.bind(() -> slot.hasContent().get() ? getClipColor(slot) : null, padButton);
      }

      mSongLayer.activate();
   }

   private Color getClipColor(final ClipLauncherSlot s) {
      if (s.isRecordingQueued().get()) {
         return Color.mix(RED, BLACK, getTransportPulse(1.0, 1));
      } else if (s.hasContent().get()) {
         if (s.isPlaybackQueued().get()) {
            return Color.mix(s.color().get(), WHITE, 1 - getTransportPulse(4.0, 1));
         } else if (s.isRecording().get()) {
            return RED;
         } else if (s.isPlaying().get() && mTransport.isPlaying().get()) {
            return Color.mix(s.color().get(), WHITE, 1 - getTransportPulse(1.0, 1));
         }

         return s.color().get();
      } else if (mCursorTrack.arm().get()) {
         return Color.mix(BLACK, RED, 0.1f);
      }

      return BLACK;
   }

   private float getTransportPulse(final double multiplier, final double amount) {
      final double p = mTransport.getPosition().get() * multiplier;
      return (float) ((0.5 + 0.5 * Math.cos(p * 2 * Math.PI)) * amount);
   }

   private void save()
   {
      final Action saveAction = mApplication.getAction("Save");
      if (saveAction != null)
      {
         saveAction.invoke();
      }
   }

   private Color getDrumPadColor(final int padIndex) {
      final DrumPad drumPad = mDrumPadBank.getItemAt(padIndex);
      final boolean padBankExists = mDrumPadBank.exists().get();
      final boolean isOn = !padBankExists || drumPad.exists().get();

      if (!isOn)
         return null;

      final double darken = 0.7;

      Color drumPadColor;

      if (!padBankExists) {
         drumPadColor = mCursorTrack.color().get();
      } else {
         final Color sourceDrumPadColor = drumPad.color().get();
         final double red = sourceDrumPadColor.getRed() * darken;
         final double green = sourceDrumPadColor.getGreen() * darken;
         final double blue = sourceDrumPadColor.getBlue() * darken;

         drumPadColor = Color.fromRGB(red, green, blue);
      }

      final int playing = velocityForPlayingNote(padIndex);

      if (playing > 0) {
         return mixColorWithWhite(drumPadColor, playing);
      }

      return drumPadColor;
   }

   private int velocityForPlayingNote(final int padIndex) {
      if (mPlayingNotes != null) {
         for (final PlayingNote playingNote : mPlayingNotes) {
            if (playingNote.pitch() == 36 + padIndex) {
               return playingNote.velocity();
            }
         }
      }

      return 0;
   }

   private Color mixColorWithWhite(final Color color, final int velocity) {
      final float x = velocity / 127.f;
      final double red = color.getRed() * (1 - x) + x;
      final double green = color.getGreen() * (1 - x) + x;
      final double blue = color.getBlue() * (1 - x) + x;

      return Color.fromRGB(red, green, blue);
   }

   private String[] getNoteInputMask() {
      final List<String> masks = new ArrayList<>();
      masks.add("80????"); // Note On
      masks.add("90????"); // Note Off
      masks.add("D???"); // Channel Pressure
      return masks.toArray(String[]::new);
   }

   private class LightStateSender implements Consumer<RGBLightState> {
      protected LightStateSender(final int statusStart, final int data1) {
         super();
         mStatusStart = statusStart;
         mData1 = data1;
      }

      @Override
      public void accept(final RGBLightState state) {
         mValues[0] = state != null ? (state.isOn() ? 127 : 0) : 0;
         mValues[1] = state != null ? state.getRed() : 0;
         mValues[2] = state != null ? state.getGreen() : 0;
         mValues[3] = state != null ? state.getBlue() : 0;

         for (int i = 0; i < 4; i++) {
            if (mValues[i] != mLastSent[i]) {
               mMidiOut.sendMidi(mStatusStart + i, mData1, mValues[i]);
               mLastSent[i] = mValues[i];
            }
         }
      }

      private final int mStatusStart, mData1;

      private final int[] mLastSent = {-1, -1, -1, -1};

      private final int[] mValues = new int[4];
   }
}
