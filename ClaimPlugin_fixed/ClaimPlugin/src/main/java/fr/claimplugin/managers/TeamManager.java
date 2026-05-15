package fr.claimplugin.managers;

import fr.claimplugin.ClaimPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gère les équipes : création, invitations, membres.
 * Données sauvegardées dans "teams.yml".
 */
public class TeamManager {

    private final ClaimPlugin plugin;
    private final File teamsFile;

    // Map : nom de l'équipe (minuscule) → liste des UUID des membres
    private final Map<String, Set<UUID>> teams = new HashMap<>();

    // Map : UUID du joueur → nom de son équipe (minuscule)
    private final Map<UUID, String> playerTeam = new HashMap<>();

    // Map : UUID du joueur invité → nom de l'équipe
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public TeamManager(ClaimPlugin plugin) {
        this.plugin = plugin;
        // Création du dossier du plugin s'il n'existe pas
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        loadTeams();
    }

    // ─── Vérifications ─────────────────────────────────────────────────────

    public boolean teamExists(String name) {
        return name != null && teams.containsKey(name.toLowerCase());
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
        String teamB = playerTeam.get(b);
        return teamA != null && teamA.equals(teamB);
    }

    public boolean hasPendingInvite(Player player) {
        return pendingInvites.containsKey(player.getUniqueId());
    }

    public String getPendingTeam(Player player) {
        return pendingInvites.get(player.getUniqueId());
    }

    public Set<UUID> getMembers(String teamName) {
        if (teamName == null) return Collections.emptySet();
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
        
        // On sauvegarde immédiatement pour éviter les pertes en cas de crash
        saveAll();
    }

    /** Envoie une invitation à un joueur */
    public void invite(Player target, String teamName) {
        pendingInvites.put(target.getUniqueId(), teamName.toLowerCase());
    }

    /** Le joueur accepte l'invitation en attente */
    public boolean joinTeam(Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = pendingInvites.remove(uuid);
        
        if (teamName == null || !teams.containsKey(teamName)) return false;
        
        teams.get(teamName).add(uuid);
        playerTeam.put(uuid, teamName);
        saveAll();
        return true;
    }

    /** Le joueur quitte son équipe */
    public void leaveTeam(Player player) {
        UUID uuid = player.getUniqueId();
        String teamName = playerTeam.remove(uuid);
        
        if (teamName == null) return;
        
        Set<UUID> members = teams.get(teamName);
        if (members != null) {
            members.remove(uuid);
            // Supprime l'équipe si elle est vide
            if (members.isEmpty()) {
                teams.remove(teamName);
            }
        }
        saveAll();
    }

    // ─── Sauvegarde / Chargement ────────────────────────────────────────────

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
            // Conversion propre des UUID en String pour le YAML
            List<String> uuidStrings = entry.getValue().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            config.set(entry.getKey(), uuidStrings);
        }
        
        try {
            config.save(teamsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§cErreur lors de la sauvegarde de teams.yml : " + e.getMessage());
        }
    }

    private void loadTeams() {
        if (!teamsFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(teamsFile);
        teams.clear();
        playerTeam.clear();

        for (String teamName : config.getKeys(false)) {
            List<String> uuidStrings = config.getStringList(teamName);
            Set<UUID> members = new HashSet<>();
            
            for (String uuidStr : uuidStrings) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    members.add(uuid);
                    playerTeam.put(uuid, teamName.toLowerCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("§cUUID invalide dans teams.yml : " + uuidStr);
                }
            }
            
            if (!members.isEmpty()) {
                teams.put(teamName.toLowerCase(), members);
            }
        }
        plugin.getLogger().info("§a" + teams.size() + " équipe(s) chargée(s).");
    }
}
