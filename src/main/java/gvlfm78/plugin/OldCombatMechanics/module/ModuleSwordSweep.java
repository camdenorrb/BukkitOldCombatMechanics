package gvlfm78.plugin.OldCombatMechanics.module;

import gvlfm78.plugin.OldCombatMechanics.OCMMain;
import gvlfm78.plugin.OldCombatMechanics.utilities.Messenger;
import gvlfm78.plugin.OldCombatMechanics.utilities.damage.ToolDamage;
import gvlfm78.plugin.OldCombatMechanics.utilities.packet.PacketAdapter;
import gvlfm78.plugin.OldCombatMechanics.utilities.packet.PacketEvent;
import gvlfm78.plugin.OldCombatMechanics.utilities.packet.PacketManager;
import gvlfm78.plugin.OldCombatMechanics.utilities.reflection.sweep.SweepPacketDetector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * A module to disable the sweep attack.
 */
public class ModuleSwordSweep extends Module {

    private BukkitRunnable task;
    private List<Location> sweepLocations = new ArrayList<>();
    private EntityDamageEvent.DamageCause sweepDamageCause;
    private ParticleListener particleListener;

    public ModuleSwordSweep(OCMMain plugin){
        super(plugin, "disable-sword-sweep");

        this.particleListener = new ParticleListener();

        try{
            // Will be available from some 1.11 version onwards
            sweepDamageCause = EntityDamageEvent.DamageCause.valueOf("ENTITY_SWEEP_ATTACK");
        } catch(IllegalArgumentException e){
            sweepDamageCause = null;
        }


        // inject all players at startup, so the plugin still works properly after a reload
        OCMMain.getInstance().addEnableListener(() -> {
            for(Player player : Bukkit.getOnlinePlayers()){
                PacketManager.getInstance().addListener(particleListener, player);
            }
        });
    }

    @Override
    public void reload(){
        // we didn't set anything up in the first place
        if(sweepDamageCause != null){
            return;
        }

        if(task != null){
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run(){
                sweepLocations.clear();
            }
        };

        task.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent e){
        // always attach the listener, it checks internally
        PacketManager.getInstance().addListener(particleListener, e.getPlayer());
    }

    //Changed from HIGHEST to LOWEST to support DamageIndicator plugin
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamaged(EntityDamageByEntityEvent e){
        Entity damager = e.getDamager();
        World world = damager.getWorld();

        if(!isEnabled(world)) return;

        if(!(damager instanceof Player)) return;

        if(sweepDamageCause != null){
            if(e.getCause() == sweepDamageCause){
                e.setCancelled(true);
                debug("Sweep cancelled", damager);
            }
            // sweep attack detected or not, we do not need to fall back to the guessing implementation
            return;
        }

        Player attacker = (Player) e.getDamager();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        if(isHoldingSword(weapon.getType()))
            onSwordAttack(e, attacker, weapon);
    }

    private void onSwordAttack(EntityDamageByEntityEvent e, Player attacker, ItemStack weapon){
        //Disable sword sweep
        Location attackerLocation = attacker.getLocation();

        int level = 0;

        try{ //In a try catch for servers that haven't updated
            level = weapon.getEnchantmentLevel(Enchantment.SWEEPING_EDGE);
        } catch(NoSuchFieldError ignored){
        }

        float damage = ToolDamage.getDamage(weapon.getType()) * level / (level + 1) + 1;

        if(e.getDamage() == damage){
            // Possibly a sword-sweep attack
            if(sweepLocations.contains(attackerLocation)){
                debug("Cancelling sweep...", attacker);
                e.setCancelled(true);
            }
        } else {
            sweepLocations.add(attackerLocation);
        }
    }

    private boolean isHoldingSword(Material mat){
        return mat.toString().endsWith("_SWORD");
    }

    /**
     * Hides sweep particles.
     */
    private class ParticleListener extends PacketAdapter {

        private boolean disabledDueToError;

        @Override
        public void onPacketSend(PacketEvent packetEvent){
            if(disabledDueToError || !isEnabled(packetEvent.getPlayer().getWorld())){
                return;
            }

            try{
                if(SweepPacketDetector.getInstance().isSweepPacket(packetEvent.getPacket())){
                    packetEvent.setCancelled(true);
                }
            } catch(Exception e){
                disabledDueToError = true;
                Messenger.warn(
                        e,
                        "Error detecting sweep packets. Please report it along with the following exception " +
                                "on github." +
                                "Sweep cancellation should still work, but particles might show up."
                );
            }
        }
    }
}