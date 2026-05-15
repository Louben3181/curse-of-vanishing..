package fr.deathreset;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * DeathReset — Plugin principal.
 *
 * Ce plugin fait 3 choses automatiquement :
 *  1. Chaque item crafté/récupéré/droppé reçoit Curse of Vanishing
 *  2. Quand un joueur meurt → son ender chest est vidé
 *  3. Quand un joueur meurt → tous les coffres dans ses claims sont vidés
 *     (nécessite ClaimPlugin dans le même serveur)
 */
public class DeathReset extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Un seul listener gère tout
        getServer().getPluginManager().registerEvents(new DeathResetListener(this), this);

        getLogger().info("DeathReset activé !");
        getLogger().info("→ Curse of Vanishing automatique : ON");
        getLogger().info("→ Reset ender chest à la mort    : ON");
        getLogger().info("→ Reset coffres claims à la mort : ON");
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathReset désactivé.");
    }
}
