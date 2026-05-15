package fr.deathreset;

import fr.claimplugin.ClaimPlugin;
import fr.claimplugin.managers.ClaimManager;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener gérant la "Malédiction de Mort" :
 * 1. Vide l'inventaire et les drops au sol.
 * 2. Vide l'Ender Chest au respawn.
 * 3. Vide les coffres situés dans les claims du joueur.
 */
public class DeathResetListener implements Listener {

    private final DeathReset plugin;
    private final Set<UUID> pendingReset = new HashSet<>();

    public DeathResetListener(DeathReset plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // --- ÉTAPE 1 : Destruction de l'inventaire ---
        // On supprime les drops qui tomberaient au sol
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // On force le vidage de l'inventaire et de l'équipement
        player.getInventory().clear();
        player.getEquipment().clear();
        
        // On marque le joueur pour le reset suite au respawn
        pendingReset.add(player.getUniqueId());
        
        player.sendMessage("§c☠ Malédiction : Votre inventaire a été pulvérisé !");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!pendingReset.remove(uuid)) return;

        // Délai court (5 ticks) pour laisser le temps au joueur de réapparaître
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            
            // --- ÉTAPE 2 : Reset de l'Ender Chest ---
            player.getEnderChest().clear();
            player.sendMessage("§c☠ Votre Ender Chest a été réinitialisé.");
            
            // --- ÉTAPE 3 : Reset des coffres dans les Claims ---
            clearPlayerClaims(player);
            
        }, 5L);
    }

    private void clearPlayerClaims(Player player) {
        // On récupère l'instance du plugin de Claim
        ClaimPlugin claimPlugin = (ClaimPlugin) Bukkit.getPluginManager().getPlugin("ClaimPlugin");
        
        if (claimPlugin == null || !claimPlugin.isEnabled()) {
            plugin.getLogger().severe("ClaimPlugin introuvable ! Impossible de vider les coffres des claims.");
            return;
        }

        ClaimManager claimManager = claimPlugin.getClaimManager();
        UUID playerUUID = player.getUniqueId();
        int totalChestsCleared = 0;

        // On parcourt les mondes et les chunks chargés
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                
                // On vérifie si le chunk appartient au joueur via le Manager du ClaimPlugin
                if (claimManager.isOwnerByUUID(chunk, playerUUID)) {
                    totalChestsCleared += clearChestsInChunk(chunk);
                }
            }
        }

        if (totalChestsCleared > 0) {
            player.sendMessage("§c☠ " + totalChestsCleared + " coffre(s) ont été vidés dans vos territoires.");
        }
    }

    /**
     * Parcourt un chunk et vide tous les inventaires de coffres trouvés.
     */
    private int clearChestsInChunk(Chunk chunk) {
        int count = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Chest chest) {
                chest.getInventory().clear();
                count++;
            }
        }
        return count;
    }
}
