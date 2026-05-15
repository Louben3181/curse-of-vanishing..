package fr.deathreset;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
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
        event.getDrops().clear();
        event.setDroppedExp(0);
        pendingReset.add(player.getUniqueId());
        player.sendMessage("§c☠ Vous êtes mort — tous vos items ont disparu !");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingReset.remove(player.getUniqueId())) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getEnderChest().clear();
            player.sendMessage("§c☠ Votre ender chest a été vidée.");
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

                count += clearChestsInChunk(chunk);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Clé claims invalide : " + key);
            }
        }

        if (count > 0) {
            player.sendMessage("§c☠ " + count + " coffre(s) dans vos claims ont été vidés.");
        }
    }

    private int clearChestsInChunk(Chunk chunk) {
        int count = 0;
        World world = chunk.getWorld();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getState() instanceof Chest) {
                        Chest chest = (Chest) block.getState();
                        if (chest.getInventory().getHolder() instanceof DoubleChest) {
                            DoubleChest dc = (DoubleChest) chest.getInventory().getHolder();
                            dc.getLeftSide().getInventory().clear();
                            dc.getRightSide().getInventory().clear();
                        } else {
                            chest.getInventory().clear();
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
