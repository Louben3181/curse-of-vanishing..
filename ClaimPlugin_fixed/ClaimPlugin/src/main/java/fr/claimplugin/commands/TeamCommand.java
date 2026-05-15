package fr.claimplugin.commands;

import fr.claimplugin.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Gère la commande /team avec ses sous-commandes.
 */
public class TeamCommand implements CommandExecutor {

    private final TeamManager teamManager;

    public TeamCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Vérification : Joueur uniquement
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        // Si aucun argument : afficher l'aide
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "join"   -> handleJoin(player);
            case "leave"  -> handleLeave(player);
            case "list"   -> handleList(player);
            default       -> sendHelp(player);
        }

        return true; // On renvoie toujours true pour éviter le message d'aide automatique de Bukkit
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage : /team create <nom>");
            return;
        }
        if (teamManager.hasTeam(player)) {
            player.sendMessage("§cVous êtes déjà dans une équipe. Faites /team leave d'abord.");
            return;
        }
        
        String name = args[1];
        // Protection contre les noms d'équipe bizarres
        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            player.sendMessage("§cLe nom de l'équipe ne doit contenir que des lettres et des chiffres.");
            return;
        }
        if (name.length() > 16) {
            player.sendMessage("§cLe nom de l'équipe ne peut pas dépasser 16 caractères.");
            return;
        }
        if (teamManager.teamExists(name)) {
            player.sendMessage("§cCe nom d'équipe est déjà utilisé.");
            return;
        }

        teamManager.createTeam(name, player);
        player.sendMessage("§aÉquipe §e" + name + " §acréée avec succès !");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage : /team invite <joueur>");
            return;
        }
        if (!teamManager.hasTeam(player)) {
            player.sendMessage("§cVous n'êtes dans aucune équipe.");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJoueur introuvable.");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage("§cVous ne pouvez pas vous inviter vous-même.");
            return;
        }
        if (teamManager.hasTeam(target)) {
            player.sendMessage("§cCe joueur est déjà dans une équipe.");
            return;
        }

        String teamName = teamManager.getTeamName(player);
        teamManager.invite(target, teamName);
        player.sendMessage("§aInvitation envoyée à §e" + target.getName() + "§a.");
        target.sendMessage("§6[Team] §e" + player.getName() + " §7vous invite dans l'équipe §a" + teamName + "§7.");
        target.sendMessage("§7Tapez §a/team join §7pour accepter.");
    }

    private void handleJoin(Player player) {
        if (teamManager.hasTeam(player)) {
            player.sendMessage("§cVous avez déjà une équipe.");
            return;
        }
        if (!teamManager.hasPendingInvite(player)) {
            player.sendMessage("§cVous n'avez reçu aucune invitation.");
            return;
        }

        String teamName = teamManager.getPendingTeam(player);
        if (teamManager.joinTeam(player)) {
            player.sendMessage("§aBienvenue dans l'équipe §e" + teamName + "§a !");
            // Notifier les alliés
            for (UUID memberUUID : teamManager.getMembers(teamName)) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(player)) {
                    member.sendMessage("§e" + player.getName() + " §aa rejoint l'équipe !");
                }
            }
        }
    }

    private void handleLeave(Player player) {
        if (!teamManager.hasTeam(player)) {
            player.sendMessage("§cVous n'avez pas d'équipe à quitter.");
            return;
        }
        String teamName = teamManager.getTeamName(player);
        teamManager.leaveTeam(player);
        player.sendMessage("§aVous avez quitté l'équipe §e" + teamName + "§a.");
    }

    private void handleList(Player player) {
        if (!teamManager.hasTeam(player)) {
            player.sendMessage("§cVous n'avez pas d'équipe.");
            return;
        }
        String teamName = teamManager.getTeamName(player);
        Set<UUID> members = teamManager.getMembers(teamName);

        player.sendMessage("§6--- Équipe : §e" + teamName + " §6---");
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            String status = (p != null && p.isOnline()) ? "§a● " : "§7○ ";
            String name = (p != null) ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            player.sendMessage(status + "§f" + (name != null ? name : "Inconnu"));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(" ");
        player.sendMessage("§6§lSISTÈME D'ÉQUIPES");
        player.sendMessage("§e/team create <nom> §7- Créer");
        player.sendMessage("§e/team invite <joueur> §7- Inviter");
        player.sendMessage("§e/team join §7- Accepter");
        player.sendMessage("§e/team leave §7- Quitter");
        player.sendMessage("§e/team list §7- Voir les membres");
        player.sendMessage(" ");
    }
}
