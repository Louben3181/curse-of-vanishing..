package fr.claimplugin.commands;

import fr.claimplugin.managers.ClaimManager;
import fr.claimplugin.managers.TeamManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Gère les commandes /claim et /unclaim.
 */
public class ClaimCommand implements CommandExecutor {

    private final ClaimManager claimManager;
    private final TeamManager teamManager;

    // Limite : nombre maximum de chunks qu'un joueur peut claim
    private static final int MAX_CLAIMS = 10;

    public ClaimCommand(ClaimManager claimManager, TeamManager teamManager) {
        this.claimManager = claimManager;
        this.teamManager  = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Ces commandes ne sont utilisables qu'en jeu
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        Chunk chunk = player.getLocation().getChunk();

        // ── /claim ──────────────────────────────────────────────────────────
        if (command.getName().equalsIgnoreCase("claim")) {

            if (claimManager.isClaimed(chunk)) {
                if (claimManager.isOwner(chunk, player)) {
                    player.sendMessage("§eVous possédez déjà ce chunk !");
                } else {
                    player.sendMessage("§cCe chunk appartient déjà à quelqu'un d'autre.");
                }
                return true;
            }

            if (claimManager.countClaims(player) >= MAX_CLAIMS) {
                player.sendMessage("§cVous avez atteint la limite de " + MAX_CLAIMS + " claims.");
                return true;
            }

            claimManager.addClaim(chunk, player);
            claimManager.saveAll();
            player.sendMessage("§aChunk claim avec succès ! (§e" + claimManager.countClaims(player) + "§a/" + MAX_CLAIMS + ")");
            return true;
        }

        // ── /unclaim ─────────────────────────────────────────────────────────
        if (command.getName().equalsIgnoreCase("unclaim")) {

            if (!claimManager.isClaimed(chunk)) {
                player.sendMessage("§cCe chunk n'est pas claim.");
                return true;
            }

            if (!claimManager.isOwner(chunk, player) && !player.hasPermission("claimplugin.admin")) {
                player.sendMessage("§cVous ne possédez pas ce chunk.");
                return true;
            }

            claimManager.removeClaim(chunk);
            claimManager.saveAll();
            player.sendMessage("§aChunk libéré avec succès.");
            return true;
        }

        return false;
    }
}
