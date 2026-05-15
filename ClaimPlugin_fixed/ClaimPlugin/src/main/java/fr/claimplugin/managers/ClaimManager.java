package fr.claimplugin.managers;

import fr.claimplugin.ClaimPlugin;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les claims : qui possède quel chunk.
 * Les données sont sauvegardées dans "claims.yml".
 *
 * Un chunk est identifié par : "monde_x_z"  (ex: "world_10_-3")
 */
public class ClaimManager {

    private final ClaimPlugin plugin;
    private final File claimsFile;
    private FileConfiguration claimsConfig;

    // Map : clé du chunk → UUID du propriétaire
    private final Map<String, UUID> claims = new HashMap<>();

    public ClaimManager(ClaimPlugin plugin) {
        this.plugin = plugin;
        this.claimsFile = new File(plugin.getDataFolder(), "claims.yml");
        loadClaims();
    }

    // ─── Clé unique pour identifier un chunk ───────────────────────────────
    private String key(Chunk chunk) {
        return chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }

    // ─── Vérifications ─────────────────────────────────────────────────────

    /** Est-ce que ce chunk est déjà claim ? */
    public boolean isClaimed(Chunk chunk) {
        return claims.containsKey(key(chunk));
    }

    /** Est-ce que ce joueur possède ce chunk ? */
    public boolean isOwner(Chunk chunk, Player player) {
        UUID owner = claims.get(key(chunk));
        return owner != null && owner.equals(player.getUniqueId());
    }

    /** Version par UUID (utilisée par DeathReset) */
    public boolean isOwnerByUUID(Chunk chunk, UUID uuid) {
        UUID owner = claims.get(key(chunk));
        return owner != null && owner.equals(uuid);
    }

    /** Qui possède ce chunk ? (null si personne) */
    public UUID getOwner(Chunk chunk) {
        return claims.get(key(chunk));
    }

    // ─── Actions ───────────────────────────────────────────────────────────

    /** Claim un chunk pour un joueur */
    public void addClaim(Chunk chunk, Player player) {
        claims.put(key(chunk), player.getUniqueId());
    }

    /** Supprime le claim d'un chunk */
    public void removeClaim(Chunk chunk) {
        claims.remove(key(chunk));
    }

    /** Nombre de claims d'un joueur */
    public long countClaims(Player player) {
        return claims.values().stream()
                .filter(uuid -> uuid.equals(player.getUniqueId()))
                .count();
    }

    // ─── Sauvegarde / Chargement ────────────────────────────────────────────

    public void saveAll() {
        claimsConfig = new YamlConfiguration();
        for (Map.Entry<String, UUID> entry : claims.entrySet()) {
            claimsConfig.set(entry.getKey(), entry.getValue().toString());
        }
        try {
            claimsConfig.save(claimsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder claims.yml : " + e.getMessage());
        }
    }

    private void loadClaims() {
        if (!claimsFile.exists()) return;
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
        for (String key : claimsConfig.getKeys(false)) {
            claims.put(key, UUID.fromString(claimsConfig.getString(key)));
        }
        plugin.getLogger().info(claims.size() + " claim(s) chargé(s).");
    }
}
