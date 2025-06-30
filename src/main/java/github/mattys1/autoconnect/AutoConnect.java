package github.mattys1.autoconnect;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
public class AutoConnect {
	@SidedProxy(serverSide = CommonProxy.REFLECT_NAME, clientSide = ClientProxy.REFLECT_NAME)
	public static CommonProxy proxy;

	public static EventHandler handler = new EventHandler();

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
		proxy.preInit(event);

        MinecraftForge.EVENT_BUS.register(handler);
    }

	@Mod.EventHandler
	public void init(final FMLInitializationEvent event) {
		proxy.init(event);
	}
}
