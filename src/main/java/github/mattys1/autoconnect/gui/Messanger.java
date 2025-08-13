package github.mattys1.autoconnect.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.Optional;
import java.util.function.Function;

public class Messanger {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static ScaledResolution resolution = new ScaledResolution(mc);
    private static ScheduledTask scheduled = new ScheduledTask(() -> {}, 0); // this looks bad

    private static float centeredAt(String text, float x) {
        return x - (float) mc.fontRenderer.getStringWidth(text) / 2;
    }

    public static void renderTask(long tickCount) {
        scheduled.execute(tickCount);
    }

    public static void updateResolution(ScaledResolution res) {
        resolution = res;
    }

    public static void writeAboveHotbar(String text, int color) {
        writeAboveHotbar(text, color, 1);
    }

    public static void writeAboveHotbar(String text, int color, int fadeOutAfter) {
        scheduled = new ScheduledTask(() ->  {
            mc.fontRenderer.drawString(
                    text,
                    centeredAt(text, (float) resolution.getScaledWidth() / 2),
                    (float) resolution.getScaledHeight() - 50,
                    color,
                    true
            );
        }, fadeOutAfter);
    }
}
