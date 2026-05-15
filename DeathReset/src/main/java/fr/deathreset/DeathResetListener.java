package fr.deathreset;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathResetListener implements Listener {

    private final DeathReset plugin;
    private final Set<UUID> pendingReset = new HashSet<>();

    public DeathResetListener(DeathReset plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // 1. On vide les drops au sol (effet Curse of Vanishing global)
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // 2. On vide l'inventaire du joueur AVANT qu'il ne respawn 
        // (Sécurité totale contre le keepInventory ou autres plugins)
        player.getInventory().clear();
        player.getEquipment().clear(); // Vide armure et main gauche
        
        pendingReset.add(player.getUniqueId());
        player.sendMessage("§c☠ TOUT votre inventaire a été pulvérisé !");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingReset.remove(player.getUniqueId())) return;

        // Délai de 2 ticks pour laisser le temps au joueur de réapparaître
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 3. Reset de l'Ender Chest
            player.getEnderChest().clear();
            player.sendMessage("§c☠ Votre Ender Chest a été vidé.");
            
            // 4. Reset des coffres dans les claims (Méthode optimisée)
            resetClaimChests(player);
        }, 2L);
    }

    private void resetClaimChests(Player player) {
        File claimsFile = new File(plugin.getDataFolder().getParentFile(), "ClaimPlugin/claims.yml");
        if (!claimsFile.exists()) return;

        FileConfiguration claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
        int count = 0;

        for (String key : claimsConfig.getKeys(false)) {
            String ownerUUID = claimsConfig.getString(key);
            if (!player.getUniqueId().toString().equals(ownerUUID)) continue;

            String[] parts = key.split("_");
            if (parts.length < 3) continue;

            try {
                int z = Integer.parseInt(parts[parts.length - 1]);
                int x = Integer.parseInt(parts[parts.length - 2]);
                String worldName = key.substring(0, key.lastIndexOf("_" + x + "_" + z));

                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                Chunk chunk = world.getChunkAt(x, z);
                if (!chunk.isLoaded()) chunk.load();

                // On utilise getTileEntities pour ne pas faire crash le serveur
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Chest) {
                        ((Chest) state).getInventory().clear();
                        count++;
                    }
                }
            } catch (Exception e) {
                // Ignore les erreurs de parsing
            }
        }

        if (count > 0) {
            player.sendMessage("§c☠ " + count + " coffre(s) de vos claims ont été vidés.");
        }
    }
}
