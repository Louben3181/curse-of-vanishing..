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
 */
public class ClaimManager {

    private final ClaimPlugin plugin;
    private final File claimsFile;

    // Map : clé du chunk (monde_x_z) → UUID du propriétaire
    private final Map<String, UUID> claims = new HashMap<>();

    public ClaimManager(ClaimPlugin plugin) {
        this.plugin = plugin;
        
        // Sécurité : Création du dossier du plugin s'il n'existe pas
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
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

    /** Version par UUID (très utile pour DeathReset) */
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
        saveAll(); // Sauvegarde automatique
    }

    /** Supprime le claim d'un chunk */
    public void removeClaim(Chunk chunk) {
        claims.remove(key(chunk));
        saveAll(); // Sauvegarde automatique
    }

    /** Nombre de claims d'un joueur */
    public long countClaims(Player player) {
        UUID uuid = player.getUniqueId();
        return claims.values().stream()
                .filter(owner -> owner.equals(uuid))
                .count();
    }

    // ─── Sauvegarde / Chargement ────────────────────────────────────────────

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<String, UUID> entry : claims.entrySet()) {
            config.set(entry.getKey(), entry.getValue().toString());
        }
        
        try {
            config.save(claimsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§cImpossible de sauvegarder claims.yml : " + e.getMessage());
        }
    }

    private void loadClaims() {
        if (!claimsFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(claimsFile);
        claims.clear();

        for (String chunkKey : config.getKeys(false)) {
            String uuidString = config.getString(chunkKey);
            if (uuidString == null) continue;

            try {
                claims.put(chunkKey, UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§cFormat d'UUID invalide dans claims.yml pour le chunk : " + chunkKey);
            }
        }
        plugin.getLogger().info("§a" + claims.size() + " claim(s) chargé(s).");
    }
}
