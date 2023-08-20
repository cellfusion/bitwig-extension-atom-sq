package jp.cellfusion.bitwig.extension;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class AtomSQExtension extends ControllerExtension
{
   private final static int CC_ENCODER_1 = 0x0E;
   private final static int CC_ALPHABET_1 = 0x18;
   private final static int CC_STOP = 0x55;
   private final static int CC_PLAY = 0x56;
   private final static int CC_REC = 0x57;
   private final static int CC_METRONOME = 0x58;

   private final static int CC_SHIFT = 0x1F;
   private final static int CC_UP = 0x57;
   private final static int CC_DOWN = 0x59;
   private final static int CC_LEFT = 0x5A;
   private final static int CC_RIGHT = 0x66;

   private final static int ENCODER_NUM = 8;

   private static final Color WHITE = Color.fromRGB(1, 1, 1);

   private static final Color BLACK = Color.fromRGB(0, 0, 0);

   private static final Color RED = Color.fromRGB(1, 0, 0);

   private static final Color DIM_RED = Color.fromRGB(0.3, 0.0, 0.0);

   private static final Color GREEN = Color.fromRGB(0, 1, 0);

   private static final Color ORANGE = Color.fromRGB(1, 1, 0);

   private static final Color BLUE = Color.fromRGB(0, 0, 1);

   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mCursorRemoteControls;
   private HardwareSurface mHardwareSurface;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private HardwareButton mShiftButton;
   private HardwareButton mUpButton;
   private HardwareButton mDownButton;
   private HardwareButton mLeftButton;
   private HardwareButton mRightButton;

   private RelativeHardwareKnob[] mEncoders = new RelativeHardwareKnob[ENCODER_NUM];

   private final Layers mLayers = new Layers(this)
   {
      @Override
      protected void activeLayersChanged()
      {
         super.activeLayersChanged();
      }
   };

   private Layer mBaseLayer;

   protected AtomSQExtension(final AtomSQExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiIn = host.getMidiInPort(0);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);
      mCursorTrack.arm().markInterested();

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
      // For now just show a popup notification for verification that it is no longer running.
      getHost().showPopupNotification("Atom SQ Exited");
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
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

      // Pads

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

   private void initLayers()
   {
      mBaseLayer = new Layer(mLayers, "Base");

      initBaseLayer();

      DebugUtilities.createDebugLayer(mLayers, mHardwareSurface).activate();
   }

   private void initBaseLayer()
   {
      // nav
      mBaseLayer.bindToggle(mUpButton, mCursorTrack.selectPreviousAction(), mCursorTrack.hasPrevious());
      mBaseLayer.bindToggle(mDownButton, mCursorTrack.selectNextAction(), mCursorTrack.hasNext());
      mBaseLayer.bindToggle(mLeftButton, mCursorDevice.selectPreviousAction(), mCursorDevice.hasPrevious());
      mBaseLayer.bindToggle(mRightButton, mCursorDevice.selectNextAction(), mCursorDevice.hasNext());

      // encoder
      for (int i = 0; i < ENCODER_NUM; i++)
      {
         final Parameter parameter = mCursorRemoteControls.getParameter(i);
         final RelativeHardwareKnob encoder = mEncoders[i];

         mBaseLayer.bind(encoder, parameter);
      }

      mBaseLayer.activate();
   }

}
