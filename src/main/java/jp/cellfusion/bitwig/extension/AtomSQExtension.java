package jp.cellfusion.bitwig.extension;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;

public class AtomSQExtension extends ControllerExtension
{
   private final static int CC_ENCODER_1 = 0x0E;
   private final static int CC_ALPHABET_1 = 0x18;
   private final static int CC_STOP = 0x55;
   private final static int CC_PLAY = 0x56;
   private final static int CC_REC = 0x57;
   private final static int CC_METRONOME = 0x58;

   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mCursorRemoteControls;
   private RelativeHardwareKnob[] mKnobs;
   private HardwareSurface mHardwareSurface;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;

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
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      mHardwareSurface = getHost().createHardwareSurface();

      mKnobs = new RelativeHardwareKnob[8];

      for (int i = 0; i < mKnobs.length; ++i)
      {
         final RemoteControl parameter = mCursorRemoteControls.getParameter(i);
         parameter.setIndication(true);
         parameter.markInterested();
         parameter.exists().markInterested();

         parameter.name().markInterested();
         parameter.name().addValueObserver(newValue -> {
//               mShouldFlushSysex = true;
            getHost().requestFlush();
         });

         final String id = "encoder" + (i + 1);

         final RelativeHardwareKnob knob = mHardwareSurface.createRelativeHardwareKnob(id);
         final AbsoluteHardwareValueMatcher absoluteCCValueMatcher = mMidiIn.createAbsoluteCCValueMatcher(0,
                 CC_ENCODER_1 + i);
         knob.setAdjustValueMatcher(
                 mMidiIn.createRelative2sComplementValueMatcher(absoluteCCValueMatcher, 127));
         knob.isUpdatingTargetValue().markInterested();
         knob.setLabel(id);
         knob.setIndexInGroup(i);
         knob.setLabelPosition(RelativePosition.ABOVE);
         mKnobs[i] = knob;
//         mainLayer.bind(knob, parameter);
      }

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
      // TODO Send any updates you need here.
   }



}
