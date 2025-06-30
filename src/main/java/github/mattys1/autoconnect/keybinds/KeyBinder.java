package github.mattys1.autoconnect.keybinds;

import java.util.Map;

import github.mattys1.autoconnect.Log;
import github.mattys1.autoconnect.Reference;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjglx.input.Keyboard;

public class KeyBinder {
	public static final Map<KeyBinds, KeyBinding> bindings = Map.of(
		KeyBinds.BEGIN_CONNECTION, new KeyBinding("Begin connection", Keyboard.KEY_B, Reference.MOD_NAME),
		KeyBinds.CONFIRM_CONNECTION, new KeyBinding("Confirm connection", Keyboard.KEY_C, Reference.MOD_NAME),
		KeyBinds.CANCEL_CONNECTION, new KeyBinding("Cancel connection", Keyboard.KEY_U, Reference.MOD_NAME)
	);

	public static void registerBinds() {
		Log.info("registering binds", bindings.toString());

		for(final KeyBinding binding : bindings.values()) {
			ClientRegistry.registerKeyBinding(binding);
		}
	}
}
