package github.mattys1.autoconnect.connection;


import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class InventoryManager {
    public final Item connectionItem;
    private final InventoryPlayer inventory;

    public boolean haveEnoughFor(int connectionLength) {
        return inventory.mainInventory.stream()
                .filter(stack -> stack.getItem().equals(connectionItem))
                .mapToInt(ItemStack::getCount)
                .sum() >= connectionLength;
    }

    public int getConnectionItemCount() {
        return inventory.mainInventory.stream()
                .filter(stack -> stack.getItem().equals(connectionItem))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    public InventoryManager(InventoryPlayer inventory) {
        this.inventory = inventory;
        connectionItem = inventory.getCurrentItem().getItem();
    }

}
