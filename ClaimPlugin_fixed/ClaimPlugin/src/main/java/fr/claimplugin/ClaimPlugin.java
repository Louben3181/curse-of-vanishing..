package fr.claimplugin;

import fr.claimplugin.commands.ClaimCommand;
import fr.claimplugin.commands.TeamCommand;
import fr.claimplugin.managers.ClaimManager;
import fr.claimplugin.managers.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe principale du plugin — c'est le "point de départ".
 * Elle s'exécute quand le serveur charge le plugin.
 */
public class ClaimPlugin extends JavaPlugin {

    // Les managers gèrent les données (claims et équipes)
    private ClaimManager claimManager;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        // Crée les dossiers/fichiers de sauvegarde si besoin
        saveDefaultConfig();

        // Initialise les managers
        claimManager = new ClaimManager(this);
        teamManager  = new TeamManager(this);

        // Enregistre les commandes /claim, /unclaim et /team
        getCommand("claim").setExecutor(new ClaimCommand(claimManager, teamManager));
        getCommand("unclaim").setExecutor(new ClaimCommand(claimManager, teamManager));
        getCommand("team").setExecutor(new TeamCommand(teamManager));

        // Enregistre le listener qui bloque les actions dans les claims
        getServer().getPluginManager().registerEvents(new ClaimListener(claimManager, teamManager), this);

        getLogger().info("ClaimPlugin activé !");
    }

    @Override
    public void onDisable() {
        // Sauvegarde les données quand le serveur s'arrête
        if (claimManager != null) claimManager.saveAll();
        if (teamManager  != null) teamManager.saveAll();
        getLogger().info("ClaimPlugin désactivé — données sauvegardées.");
    }

    /** Permet aux autres plugins d'accéder au ClaimManager */
    public ClaimManager getClaimManager() {
        return claimManager;
    }

    /** Permet aux autres plugins d'accéder au TeamManager */
    public TeamManager getTeamManager() {
        return teamManager;
    }
}
