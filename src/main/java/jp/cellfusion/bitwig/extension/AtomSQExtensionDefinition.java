package jp.cellfusion.bitwig.extension;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class AtomSQExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("c9c0337f-1a36-4264-b8e5-38a330cda6b0");
   
   public AtomSQExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "Atom SQ";
   }
   
   @Override
   public String getAuthor()
   {
      return "cellfusion";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor()
   {
      return "PreSonus";
   }
   
   @Override
   public String getHardwareModel()
   {
      return "Atom SQ";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 18;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      list.add(new String[]{"ATM SQ"}, new String[]{"ATM SQ"});
      list.add(new String[]{"MIDIIN2 (ATM SQ)"}, new String[]{"MIDIOUT2 (ATM SQ)"});
   }

   @Override
   public AtomSQExtension createInstance(final ControllerHost host)
   {
      return new AtomSQExtension(this, host);
   }
}
