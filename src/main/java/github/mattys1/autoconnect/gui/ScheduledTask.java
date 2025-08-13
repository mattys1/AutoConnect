package github.mattys1.autoconnect.gui;

import net.minecraft.client.Minecraft;

public class ScheduledTask {
    private final Runnable task;
    private int taskDuration;
    private long previousTickCount = 0;

    public ScheduledTask(final Runnable oTask, final int duration) {
        task = oTask;
        taskDuration = duration;
    }

    public void execute(long tickCount) {
        if(taskDuration > 0) {
            task.run();

            if(previousTickCount < tickCount) {
                taskDuration--;
                previousTickCount = tickCount;
            }
        }
    }
}
