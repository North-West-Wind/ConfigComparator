package ml.northwestwind.configcomparator.events;

import ml.northwestwind.configcomparator.ConfigComparator;
import ml.northwestwind.configcomparator.network.Communicator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ConfigComparator.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonEvents {
    @SubscribeEvent
    public static void commonSetup(final FMLCommonSetupEvent event) {
        Communicator.registerPackets();
    }
}
