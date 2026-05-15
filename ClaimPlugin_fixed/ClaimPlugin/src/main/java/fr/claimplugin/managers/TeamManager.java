package fr.claimplugin.managers;

import fr.claimplugin.ClaimPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les équipes : création, invitations, membres.
 * Données sauvegardées dans "teams.yml".
 */
public class TeamManager {

    private final ClaimPlugin plugin;
    private final File teamsFile;

    // Map : nom de l'équipe → liste des UUID des membres
    private final Map<String, Set<UUID>> teams = new HashMap<>();

    // Map : UUID du joueur → nom de son équipe
    private final Map<UUID, String> playerTeam = new HashMap<>();

    // Map : UUID du joueur invité → nom de l'équipe
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public TeamManager(ClaimPlugin plugin) {
        this.plugin = plugin;
        this.teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        loadTeams();
    }

    // ─── Vérifications ─────────────────────────────────────────────────────

    public boolean teamExists(String name) {
        return teams.containsKey(name.toLowerCase());
    }

    public boolean hasTeam(Player player) {
        return playerTeam.containsKey(player.getUniqueId());
    }

    public String getTeamName(Player player) {
        return playerTeam.get(player.getUniqueId());
    }

    /** Est-ce que deux joueurs sont dans la même équipe ? */
    public boolean sameTeam(UUID a, UUID b) {
        String teamA = playerTeam.get(a);
        return teamA != null && teamA.equals(playerTeam.get(b));
    }

    public boolean hasPendingInvite(Player player) {
        return pendingInvites.containsKey(player.getUniqueId());
    }

    public String getPendingTeam(Player player) {
        return pendingInvites.get(player.getUniqueId());
    }

    public Set<UUID> getMembers(String teamName) {
        return teams.getOrDefault(teamName.toLowerCase(), Collections.emptySet());
    }

    // ─── Actions ───────────────────────────────────────────────────────────

    /** Crée une équipe avec le joueur comme premier membre */
    public void createTeam(String name, Player creator) {
        String key = name.toLowerCase();
        Set<UUID> members = new HashSet<>();
        members.add(creator.getUniqueId());
        teams.put(key, members);
        playerTeam.put(creator.getUniqueId(), key);
    }

    /** Envoie une invitation à un joueur */
    public void invite(Player target, String teamName) {
        pendingInvites.put(target.getUniqueId(), teamName.toLowerCase());
    }

    /** Le joueur accepte l'invitation en attente */
    public void joinTeam(Player player) {
        String teamName = pendingInvites.remove(player.getUniqueId());
        if (teamName == null) return;
        teams.computeIfAbsent(teamName, k -> new HashSet<>()).add(player.getUniqueId());
        playerTeam.put(player.getUniqueId(), teamName);
    }

    /** Le joueur quitte son équipe */
    public void leaveTeam(Player player) {
        String teamName = playerTeam.remove(player.getUniqueId());
        if (teamName == null) return;
        Set<UUID> members = teams.get(teamName);
        if (members != null) {
            members.remove(player.getUniqueId());
            // Supprime l'équipe si elle est vide
            if (members.isEmpty()) teams.remove(teamName);
        }
    }

    // ─── Sauvegarde / Chargement ────────────────────────────────────────────

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
            List<String> uuids = new ArrayList<>();
            for (UUID uuid : entry.getValue()) uuids.add(uuid.toString());
            config.set(entry.getKey(), uuids);
        }
        try {
            config.save(teamsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder teams.yml : " + e.getMessage());
        }
    }

    private void loadTeams() {
        if (!teamsFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(teamsFile);
        for (String teamName : config.getKeys(false)) {
            Set<UUID> members = new HashSet<>();
            for (String uuidStr : config.getStringList(teamName)) {
                UUID uuid = UUID.fromString(uuidStr);
                members.add(uuid);
                playerTeam.put(uuid, teamName);
            }
            teams.put(teamName, members);
        }
        plugin.getLogger().info(teams.size() + " équipe(s) chargée(s).");
    }
}
