package github.mattys1.autoconnect.connection;


import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.swing.text.html.Option;
import java.util.Optional;

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

    private InventoryManager(InventoryPlayer inventory, Item conItem) {
        this.inventory = inventory;
        connectionItem = conItem;
    }

    public static Optional<InventoryManager> create(InventoryPlayer inventory) {
        final Item item = inventory.getCurrentItem().getItem();

        if(!(item instanceof ItemBlock)) { // TODO: this should also check for tile entities;
            return Optional.empty();
        };

        return Optional.of(new InventoryManager(inventory, item));
    }
}
