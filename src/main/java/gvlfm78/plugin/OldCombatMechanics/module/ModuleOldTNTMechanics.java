package gvlfm78.plugin.OldCombatMechanics.module;

import gvlfm78.plugin.OldCombatMechanics.OCMMain;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ModuleOldTNTMechanics extends Module {

    public ModuleOldTNTMechanics(OCMMain plugin) {
        super(plugin, "old-tnt-mechanics");
    }

    private Location center(Location location) {
        return new Location(location.getWorld(), location.getBlockX() + 0.5, location.getBlockY(), location.getBlockZ() + 0.5);
    }

    private void spawnTNT(Location location) {
        TNTPrimed tnt = location.getWorld().spawn(center(location), TNTPrimed.class);
        tnt.setVelocity(new Vector(0.0, 0.25, 0.0));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTNTDispense(BlockDispenseEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.TNT) {
            return;
        }

        e.setCancelled(true);
        Location location = new Location(
                e.getBlock().getLocation().getWorld(),
                e.getVelocity().getX(),
                e.getVelocity().getY(),
                e.getVelocity().getZ()
        );

        spawnTNT(location);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTNTIgnite(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getPlayer() == null) {
            return;
        }

        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.TNT) {
            return;
        }

        if (e.getItem() == null || (e.getItem().getType() != Material.FLINT_AND_STEEL && e.getItem().getType() != Material.FIREBALL)) {
            return;
        }

        e.setCancelled(true);
        e.getClickedBlock().setType(Material.AIR);
        spawnTNT(e.getClickedBlock().getLocation());

        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = e.getItem();

        if (item.getType() == Material.FLINT_AND_STEEL) {
            item.setDurability((short) (item.getDurability() + 1));
            if (item.getDurability() > item.getType().getMaxDurability()) {
                item.setType(Material.AIR);
            }
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }
}