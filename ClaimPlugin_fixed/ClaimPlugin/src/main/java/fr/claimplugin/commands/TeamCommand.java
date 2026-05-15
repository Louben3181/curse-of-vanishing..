package fr.claimplugin.commands;

import fr.claimplugin.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Gère la commande /team avec ses sous-commandes :
 *   /team create <nom>
 *   /team invite <joueur>
 *   /team join
 *   /team leave
 *   /team list
 */
public class TeamCommand implements CommandExecutor {

    private final TeamManager teamManager;

    public TeamCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("create")) {
            if (args.length < 2) { player.sendMessage("§cUsage : /team create <nom>"); return true; }
            if (teamManager.hasTeam(player)) {
                player.sendMessage("§cVous êtes déjà dans une équipe. Faites /team leave d'abord.");
                return true;
            }
            String name = args[1];
            if (teamManager.teamExists(name)) {
                player.sendMessage("§cUne équipe avec ce nom existe déjà.");
                return true;
            }
            if (name.length() > 16) {
                player.sendMessage("§cLe nom de l'équipe ne peut pas dépasser 16 caractères.");
                return true;
            }
            teamManager.createTeam(name, player);
            teamManager.saveAll();
            player.sendMessage("§aÉquipe §e" + name + " §acréée avec succès !");

        } else if (sub.equals("invite")) {
            if (args.length < 2) { player.sendMessage("§cUsage : /team invite <joueur>"); return true; }
            if (!teamManager.hasTeam(player)) {
                player.sendMessage("§cVous n'êtes dans aucune équipe.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("§cJoueur introuvable ou hors ligne.");
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage("§cVous ne pouvez pas vous inviter vous-même.");
                return true;
            }
            if (teamManager.hasTeam(target)) {
                player.sendMessage("§c" + target.getName() + " est déjà dans une équipe.");
                return true;
            }
            String teamName = teamManager.getTeamName(player);
            teamManager.invite(target, teamName);
            player.sendMessage("§aInvitation envoyée à §e" + target.getName() + "§a.");
            target.sendMessage("§eVous avez été invité dans §a" + teamName + "§e. Tapez §a/team join §epour accepter.");

        } else if (sub.equals("join")) {
            if (!teamManager.hasPendingInvite(player)) {
                player.sendMessage("§cVous n'avez aucune invitation en attente.");
                return true;
            }
            if (teamManager.hasTeam(player)) {
                player.sendMessage("§cVous êtes déjà dans une équipe. Faites /team leave d'abord.");
                return true;
            }
            String teamName = teamManager.getPendingTeam(player);
            teamManager.joinTeam(player);
            teamManager.saveAll();
            player.sendMessage("§aVous avez rejoint l'équipe §e" + teamName + "§a !");
            for (UUID memberUUID : teamManager.getMembers(teamName)) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(player)) {
                    member.sendMessage("§e" + player.getName() + " §aa rejoint votre équipe !");
                }
            }

        } else if (sub.equals("leave")) {
            if (!teamManager.hasTeam(player)) {
                player.sendMessage("§cVous n'êtes dans aucune équipe.");
                return true;
            }
            String teamName = teamManager.getTeamName(player);
            teamManager.leaveTeam(player);
            teamManager.saveAll();
            player.sendMessage("§aVous avez quitté l'équipe §e" + teamName + "§a.");

        } else if (sub.equals("list")) {
            if (!teamManager.hasTeam(player)) {
                player.sendMessage("§cVous n'êtes dans aucune équipe.");
                return true;
            }
            String teamName = teamManager.getTeamName(player);
            Set<UUID> members = teamManager.getMembers(teamName);
            player.sendMessage("§6=== Équipe : §e" + teamName + " §6(" + members.size() + " membre(s)) ===");
            for (UUID uuid : members) {
                Player member = Bukkit.getPlayer(uuid);
                String status = (member != null) ? "§a● " : "§7○ ";
                String name   = (member != null) ? member.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                player.sendMessage(status + (name != null ? name : uuid.toString()));
            }

        } else {
            sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== ClaimPlugin - Équipes ===");
        player.sendMessage("§e/team create <nom> §7— Créer une équipe");
        player.sendMessage("§e/team invite <joueur> §7— Inviter un joueur");
        player.sendMessage("§e/team join §7— Rejoindre une équipe (après invitation)");
        player.sendMessage("§e/team leave §7— Quitter son équipe");
        player.sendMessage("§e/team list §7— Voir les membres");
    }
}
