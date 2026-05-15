package fr.claimplugin.commands;

import fr.claimplugin.managers.ClaimManager;
import fr.claimplugin.managers.TeamManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. Vérification : Joueur uniquement
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true; // On renvoie true pour éviter d'afficher l'aide console
        }
        
        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();

        // 2. Gestion de la commande /claim
        if (command.getName().equalsIgnoreCase("claim")) {
            executeClaim(player, chunk);
            return true; // IMPORTANT : Toujours true ici
        }

        // 3. Gestion de la commande /unclaim
        if (command.getName().equalsIgnoreCase("unclaim")) {
            executeUnclaim(player, chunk);
            return true; // IMPORTANT : Toujours true ici
        }

        return true; // On renvoie true par défaut pour ne jamais afficher le message d'aide automatique
    }

    private void executeClaim(Player player, Chunk chunk) {
        // Vérifie si déjà claim
        if (claimManager.isClaimed(chunk)) {
            if (claimManager.isOwner(chunk, player)) {
                player.sendMessage("§eVous possédez déjà ce chunk !");
            } else {
                player.sendMessage("§cCe chunk appartient déjà à quelqu'un d'autre.");
            }
            return;
        }

        // Vérifie la limite
        if (claimManager.countClaims(player) >= MAX_CLAIMS) {
            player.sendMessage("§cVous avez atteint la limite de " + MAX_CLAIMS + " claims.");
            return;
        }

        // Ajout du claim
        claimManager.addClaim(chunk, player);
        player.sendMessage("§aChunk claim avec succès ! (§e" + claimManager.countClaims(player) + "§a/" + MAX_CLAIMS + ")");
    }

    private void executeUnclaim(Player player, Chunk chunk) {
        // Vérifie si n'est pas claim
        if (!claimManager.isClaimed(chunk)) {
            player.sendMessage("§cCe chunk n'est pas protégé.");
            return;
        }

        // Vérifie la permission (Propriétaire ou Admin)
        if (!claimManager.isOwner(chunk, player) && !player.hasPermission("claimplugin.admin")) {
            player.sendMessage("§cVous ne possédez pas ce chunk.");
            return;
        }

        // Suppression du claim
        claimManager.removeClaim(chunk);
        player.sendMessage("§aLe chunk a été libéré.");
    }
}
