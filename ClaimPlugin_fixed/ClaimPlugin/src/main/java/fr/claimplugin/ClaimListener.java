package fr.claimplugin;

import fr.claimplugin.managers.ClaimManager;
import fr.claimplugin.managers.TeamManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Écoute les actions des joueurs et bloque celles
 * qui ne sont pas autorisées dans un chunk claim.
 *
 * Règle : seul le propriétaire ET ses coéquipiers peuvent
 *         construire/casser dans un chunk claim.
 */
public class ClaimListener implements Listener {

    private final ClaimManager claimManager;
    private final TeamManager  teamManager;

    public ClaimListener(ClaimManager claimManager, TeamManager teamManager) {
        this.claimManager = claimManager;
        this.teamManager  = teamManager;
    }

    // ─── Casser un bloc ────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Chunk  chunk  = event.getBlock().getChunk();

        if (isAllowed(player, chunk)) return;

        event.setCancelled(true);
        player.sendMessage("§cCe chunk est protégé — vous ne pouvez pas casser de blocs ici.");
    }

    // ─── Poser un bloc ─────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Chunk  chunk  = event.getBlock().getChunk();

        if (isAllowed(player, chunk)) return;

        event.setCancelled(true);
        player.sendMessage("§cCe chunk est protégé — vous ne pouvez pas poser de blocs ici.");
    }

    // ─── Interagir (coffres, portes, leviers…) ─────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Chunk  chunk  = event.getClickedBlock().getChunk();

        if (isAllowed(player, chunk)) return;

        event.setCancelled(true);
        player.sendMessage("§cCe chunk est protégé — vous ne pouvez pas interagir ici.");
    }

    // ─── Logique d'autorisation ────────────────────────────────────────────

    /**
     * Retourne true si le joueur est autorisé à agir dans ce chunk.
     * Autorisé si :
     *  - Le chunk n'est pas claim
     *  - Le joueur est le propriétaire
     *  - Le joueur est admin (permission claimplugin.admin)
     *  - Le joueur est dans la même équipe que le propriétaire
     */
    private boolean isAllowed(Player player, Chunk chunk) {
        if (!claimManager.isClaimed(chunk))   return true;
        if (player.hasPermission("claimplugin.admin")) return true;

        UUID owner = claimManager.getOwner(chunk);
        if (owner == null) return true;
        if (owner.equals(player.getUniqueId())) return true; // c'est le proprio

        // Même équipe que le proprio ?
        return teamManager.sameTeam(player.getUniqueId(), owner);
    }
}
