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

public class DeathResetListener implements Listener {

    private final DeathReset plugin;
    private final Set<UUID> pendingReset = new HashSet<>();

    public DeathResetListener(DeathReset plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // 1. Simulation de Curse of Vanishing globale
        // On vide tout ce qui est au sol et dans l'inventaire
        event.getDrops().clear();
        event.setDroppedExp(0);
        player.getInventory().clear();
        player.getEquipment().clear();
        
        pendingReset.add(player.getUniqueId());
        player.sendMessage("§c☠ Malédiction : Tout votre équipement a été pulvérisé !");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingReset.remove(player.getUniqueId())) return;

        // Délai de 5 ticks pour s'assurer que le joueur est bien réapparu
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 2. Reset de l'Ender Chest
            player.getEnderChest().clear();
            player.sendMessage("§c☠ Votre Ender Chest a été vidé.");
            
            // 3. Reset des coffres dans les claims
            clearPlayerClaims(player);
        }, 5L);
    }

    private void clearPlayerClaims(Player player) {
        // On récupère l'instance de ClaimPlugin de manière propre
        ClaimPlugin claimPlugin = (ClaimPlugin) Bukkit.getPluginManager().getPlugin("ClaimPlugin");
        
        if (claimPlugin == null || !claimPlugin.isEnabled()) {
            plugin.getLogger().severe("Erreur : ClaimPlugin est introuvable ou désactivé !");
            return;
        }

        ClaimManager claimManager = claimPlugin.getClaimManager();
        UUID playerUUID = player.getUniqueId();
        int chestCount = 0;

        // On parcourt uniquement les chunks chargés pour éviter de faire ramer le serveur
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                
                // On utilise directement le manager du plugin de claim (Beaucoup plus rapide)
                UUID ownerUUID = claimManager.getOwner(chunk);
                
                if (ownerUUID != null && ownerUUID.equals(playerUUID)) {
                    chestCount += clearChestsInChunk(chunk);
                }
            }
        }

        if (chestCount > 0) {
            player.sendMessage("§c☠ " + chestCount + " coffre(s) ont été vidés dans vos territoires.");
        }
    }

    private int clearChestsInChunk(Chunk chunk) {
        int count = 0;
        // On ne boucle que sur les blocs spéciaux (coffres, fours, etc.) du chunk
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Chest) {
                ((Chest) state).getInventory().clear();
                count++;
            }
        }
        return count;
    }
}
