package github.mattys1.autoconnect.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Messanger {
    private static final Minecraft mc = Minecraft.getMinecraft();
//    private static final int resX = mc.displayWidth;
//    private static final int resY = mc.displayHeight;
    private static float centeredAt(String text, float x) {
        return x - (float) mc.fontRenderer.getStringWidth(text) / 2;
    }

    public static void writeAboveHotbar(String text, int color, ScaledResolution resolution) {
        mc.fontRenderer.drawString(
                text,
                centeredAt(text, (float) resolution.getScaledWidth() / 2),
                (float) resolution.getScaledHeight() - 50,
                color,
                true
        );
    }
}
