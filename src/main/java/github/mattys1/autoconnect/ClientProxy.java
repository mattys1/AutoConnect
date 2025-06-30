package github.mattys1.autoconnect;

import github.mattys1.autoconnect.keybinds.KeyBinder;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
	public static final String REFLECT_NAME = "github.mattys1.autoconnect.ClientProxy";
 
	@Override
	public void preInit(final FMLPreInitializationEvent event) {
		super.preInit(event);

		Log.info("called preinit from client proxy, with event: ", event.toString());
	}

	@Override
	public void init(final FMLInitializationEvent event) {
		super.init(event);

		KeyBinder.registerBinds();
	}
}
