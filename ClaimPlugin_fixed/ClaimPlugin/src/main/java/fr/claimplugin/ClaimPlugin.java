package fr.claimplugin;

import fr.claimplugin.commands.ClaimCommand;
import fr.claimplugin.commands.TeamCommand;
import fr.claimplugin.managers.ClaimManager;
import fr.claimplugin.managers.TeamManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe principale du plugin — c'est le "point de départ".
 */
public class ClaimPlugin extends JavaPlugin {

    private static ClaimPlugin instance;
    private ClaimManager claimManager;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;

        // Crée les fichiers de sauvegarde (config.yml, etc.)
        saveDefaultConfig();

        // Initialise les managers
        this.claimManager = new ClaimManager(this);
        this.teamManager = new TeamManager(this);

        // Enregistrement sécurisé des commandes
        registerCommands();

        // Enregistre le listener pour protéger les blocs
        getServer().getPluginManager().registerEvents(new ClaimListener(claimManager, teamManager), this);

        getLogger().info("§aClaimPlugin activé avec succès !");
    }

    private void registerCommands() {
        // Cette méthode évite les erreurs si une commande manque dans le plugin.yml
        
        ClaimCommand claimExec = new ClaimCommand(claimManager, teamManager);
        TeamCommand teamExec = new TeamCommand(teamManager);

        registerCommand("claim", claimExec);
        registerCommand("unclaim", claimExec);
        registerCommand("team", teamExec);
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        } else {
            getLogger().warning("§cLa commande /" + name + " est absente du plugin.yml !");
        }
    }

    @Override
    public void onDisable() {
        // Sauvegarde impérative des données
        if (claimManager != null) claimManager.saveAll();
        if (teamManager != null) teamManager.saveAll();
        
        getLogger().info("§eClaimPlugin désactivé — Toutes les données ont été sauvegardées.");
    }

    // --- Getters ---

    public static ClaimPlugin getInstance() {
        return instance;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
