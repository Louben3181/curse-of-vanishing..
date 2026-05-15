package fr.deathreset;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Gère les 3 fonctionnalités du plugin.
 */
public class DeathResetListener implements Listener {

    private final DeathReset plugin;

    // On stocke l'UUID du joueur mort pour agir au respawn
    // (l'ender chest n'est accessible qu'une fois le joueur respawné)
    private final java.util.Set<UUID> pendingReset = new java.util.HashSet<>();

    public DeathResetListener(DeathReset plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PARTIE 1 — Curse of Vanishing sur TOUT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Quand un joueur craft quelque chose → curse of vanishing sur le résultat
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            ItemStack result = event.getCurrentItem();
            addCurse(result);
        }
    }

    /**
     * Quand un joueur ramasse un item au sol → curse of vanishing
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            addCurse(event.getItem().getItemStack());
        }
    }

    /**
     * Quand un joueur pêche quelque chose → curse of vanishing
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() != null) {
            if (event.getCaught() instanceof org.bukkit.entity.Item item) {
                addCurse(item.getItemStack());
            }
        }
    }

    /**
     * Quand un joueur clique dans un inventaire (coffre, villageois, etc.)
     * pour récupérer un item → curse of vanishing
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // On ne s'intéresse qu'aux clics qui transfèrent un item vers l'inventaire du joueur
        if (event.getClickedInventory() == null) return;
        InventoryType type = event.getView().getTopInventory().getType();

        // Si le joueur clique dans un coffre, un loot, un villageois, etc.
        if (type == InventoryType.CHEST
                || type == InventoryType.BARREL
                || type == InventoryType.SHULKER_BOX
                || type == InventoryType.ENDER_CHEST
                || type == InventoryType.MERCHANT
                || type == InventoryType.HOPPER) {
            addCurse(event.getCurrentItem());
        }
    }

    /**
     * Ajoute Curse of Vanishing à un item (si pas déjà présent et si l'item existe)
     */
    private void addCurse(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        if (item.containsEnchantment(Enchantment.VANISHING_CURSE)) return;

        // Certains items ne peuvent pas être enchantés (eau, bloc de terre, etc.)
        // On utilise unsafe pour forcer l'enchantement sur n'importe quel item
        item.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PARTIE 2 & 3 — Reset à la mort
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Quand un joueur meurt → on marque son UUID pour le reset au respawn
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        pendingReset.add(player.getUniqueId());
    }

    /**
     * Quand le joueur respawn → on vide son ender chest et ses coffres dans ses claims
     * On fait ça au respawn car l'ender chest d'un joueur mort n'est pas accessible directement.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingReset.remove(player.getUniqueId())) return;

        // On décale d'un tick pour que tout soit bien chargé
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resetEnderChest(player);
            resetClaimChests(player);
        }, 1L);
    }

    /**
     * Vide l'ender chest du joueur
     */
    private void resetEnderChest(Player player) {
        Inventory enderChest = player.getEnderChest();
        enderChest.clear();
        player.sendMessage("§c☠ Votre ender chest a été vidée suite à votre mort.");
    }

    /**
     * Vide tous les coffres posés dans les chunks claim du joueur.
     *
     * Méthode : on regarde tous les chunks chargés du monde et on vérifie
     * lesquels appartiennent au joueur (via ClaimPlugin).
     * Pour chaque chunk claim → on scanne tous les blocs coffre et on vide.
     */
    private void resetClaimChests(Player player) {
        // Vérifie si ClaimPlugin est présent
        Plugin claimPluginRaw = Bukkit.getPluginManager().getPlugin("ClaimPlugin");
        if (claimPluginRaw == null) {
            // ClaimPlugin absent → on saute silencieusement
            return;
        }

        // On accède au ClaimManager via réflexion pour ne pas créer de dépendance dure
        try {
            Object claimManager = claimPluginRaw.getClass()
                    .getMethod("getClaimManager")
                    .invoke(claimPluginRaw);

            int resetCount = 0;

            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    // Est-ce que ce chunk appartient au joueur ?
                    boolean isOwner = (boolean) claimManager.getClass()
                            .getMethod("isOwnerByUUID", Chunk.class, UUID.class)
                            .invoke(claimManager, chunk, player.getUniqueId());

                    if (!isOwner) continue;

                    // Scan tous les blocs du chunk à la recherche de coffres
                    resetCount += clearChestsInChunk(chunk);
                }
            }

            if (resetCount > 0) {
                player.sendMessage("§c☠ " + resetCount + " coffre(s) dans vos claims ont été vidés.");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Impossible d'accéder au ClaimManager : " + e.getMessage());
        }
    }

    /**
     * Vide tous les coffres dans un chunk donné.
     * Retourne le nombre de coffres vidés.
     */
    private int clearChestsInChunk(Chunk chunk) {
        int count = 0;
        // Un chunk fait 16x16 blocs, hauteur de -64 à 320 en 1.21
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (block.getState() instanceof Chest chest) {
                        // Coffre double → on vide les deux côtés
                        if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest) {
                            doubleChest.getLeftSide().getInventory().clear();
                            doubleChest.getRightSide().getInventory().clear();
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
