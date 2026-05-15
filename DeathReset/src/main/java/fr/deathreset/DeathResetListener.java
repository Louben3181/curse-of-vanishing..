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
        
        // Empêche les items de tomber au sol
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Sécurité : on vide l'inventaire et l'équipement du joueur immédiatement
        // Cela garantit que TOUT est perdu, même avec le keepInventory
        player.getInventory().clear();
        player.getEquipment().clear();
        
        pendingReset.add(player.getUniqueId());
        player.sendMessage("§c☠ Malédiction : Tout votre équipement a été détruit !");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingReset.remove(player.getUniqueId())) return;

        // Délai de 5 ticks pour s'assurer que le joueur a bien réapparu
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Reset de l'Ender Chest
            player.getEnderChest().clear();
            player.sendMessage("§c☠ Votre Ender Chest a été vidé.");
            
            // Reset des coffres dans ses propres claims
            resetClaimChests(player);
        }, 5L);
    }

    private void resetClaimChests(Player player) {
        // Accès au fichier de configuration du plugin de claim
        File claimsFile = new File(plugin.getDataFolder().getParentFile(), "ClaimPlugin/claims.yml");
        if (!claimsFile.exists()) return;

        FileConfiguration claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
        String playerUUID = player.getUniqueId().toString();
        int totalCleared = 0;

        for (String key : claimsConfig.getKeys(false)) {
            String owner = claimsConfig.getString(key);
            
            // Vérifie si le chunk appartient au joueur mort
            if (playerUUID.equals(owner)) {
                try {
                    // On extrait les coordonnées (Format : monde_x_z)
                    String[] parts = key.split("_");
                    int z = Integer.parseInt(parts[parts.length - 1]);
                    int x = Integer.parseInt(parts[parts.length - 2]);
                    
                    // Reconstitution du nom du monde (gère les mondes avec des underscores)
                    StringBuilder worldName = new StringBuilder();
                    for(int i = 0; i < parts.length - 2; i++) {
                        if(i > 0) worldName.append("_");
                        worldName.append(parts[i]);
                    }

                    World world = Bukkit.getWorld(worldName.toString());
                    if (world != null) {
                        Chunk chunk = world.getChunkAt(x, z);
                        if (!chunk.isLoaded()) chunk.load();

                        // Vidage des coffres de manière optimisée (TileEntities)
                        // Évite de scanner tous les blocs (x16, y256, z16) inutilement
                        for (BlockState state : chunk.getTileEntities()) {
                            if (state instanceof Chest) {
                                ((Chest) state).getInventory().clear();
                                totalCleared++;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore les erreurs de formatage des clés dans le YAML
                }
            }
        }

        if (totalCleared > 0) {
            player.sendMessage("§c☠ " + totalCleared + " coffre(s) ont été vidés dans vos zones.");
        }
    }
}
