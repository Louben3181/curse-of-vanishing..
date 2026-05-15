package fr.deathreset;

import fr.claimplugin.managers.ClaimManager; // On importe ton manager !
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

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
        
        // Effet Curse of Vanishing Global : on vide TOUT
        event.getDrops().clear();
        event.setDroppedExp(0);
        player.getInventory().clear();
        player.getEquipment().clear();
        
        pendingReset.add(player.getUniqueId());
        player.sendMessage("§c☠ Vos objets ont été détruits par la malédiction !");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingReset.remove(player.getUniqueId())) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Reset Ender Chest
            player.getEnderChest().clear();
            player.sendMessage("§c☠ Votre Ender Chest a été réinitialisé.");
            
            // Reset des Claims via le ClaimManager de ton autre plugin
            clearPlayerClaims(player);
        }, 5L);
    }

    private void clearPlayerClaims(Player player) {
        // On récupère l'instance de ton ClaimPlugin
        Plugin claimPlugin = Bukkit.getPluginManager().getPlugin("ClaimPlugin");
        if (claimPlugin == null || !claimPlugin.isEnabled()) {
            plugin.getLogger().severe("ClaimPlugin est introuvable ! Impossible de vider les coffres.");
            return;
        }

        // On va scanner les chunks chargés pour trouver ceux appartenant au joueur
        // (Note: C'est la méthode la plus sûre si tu ne veux pas modifier le ClaimManager)
        int count = 0;
        UUID deadPlayerUUID = player.getUniqueId();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                // On vérifie si le chunk appartient au mort via ton système de claim
                if (isOwner(deadPlayerUUID, chunk)) {
                    count += clearChestsInChunk(chunk);
                }
            }
        }

        if (count > 0) {
            player.sendMessage("§c☠ " + count + " coffre(s) dans vos zones protégées ont été vidés.");
        }
    }

    // Utilise la logique de ton ClaimListener pour vérifier le proprio
    private boolean isOwner(UUID uuid, Chunk chunk) {
        try {
            // On tente de lire le fichier de config de ton plugin de claim de façon dynamique
            java.io.File file = new java.io.File("plugins/ClaimPlugin/claims.yml");
            if (!file.exists()) return false;
            org.bukkit.configuration.file.FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            
            // Le format dans ton code semble être : monde_x_z: uuid
            String key = chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
            return uuid.toString().equals(config.getString(key));
        } catch (Exception e) {
            return false;
        }
    }

    private int clearChestsInChunk(Chunk chunk) {
        int count = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Chest) {
                ((Chest) state).getInventory().clear();
                count++;
            }
        }
        return count;
    }
}
