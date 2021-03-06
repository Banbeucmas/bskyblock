package us.tastybento.bskyblock.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.PlayersManager;

public class JoinLeaveListener implements Listener {

    private BSkyBlock plugin;
    private PlayersManager players;

    /**
     * @param plugin - BSkyBlock plugin object
     */
    public JoinLeaveListener(BSkyBlock plugin) {
        this.plugin = plugin;
        players = plugin.getPlayers();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        User user = User.getInstance(event.getPlayer());
        if (user.getUniqueId() == null) {
            return;
        }
        UUID playerUUID = user.getUniqueId();
        if (plugin.getPlayers().isKnown(playerUUID)) {
            // Load player
            players.addPlayer(playerUUID);
            // Reset resets if the admin changes it to or from unlimited
            if (plugin.getSettings().getResetLimit() < players.getResetsLeft(playerUUID)  || (plugin.getSettings().getResetLimit() >= 0 && players.getResetsLeft(playerUUID) < 0)) {
                players.setResetsLeft(playerUUID, plugin.getSettings().getResetLimit());
            }
            // Set the player's name (it may have changed), but only if it isn't empty
            if (!user.getName().isEmpty()) {
                players.setPlayerName(user);
                players.save(playerUUID);
            } else {
                plugin.getLogger().warning("Player that just logged in has no name! " + playerUUID.toString());
            }
            if (plugin.getSettings().isRemoveMobsOnLogin()) {
                plugin.getIslands().removeMobs(user.getLocation());
            }

            // Check if they logged in to a locked island and expel them or if they are banned
            Island currentIsland = plugin.getIslands().getIslandAt(user.getLocation()).orElse(null);
            if (currentIsland != null && (currentIsland.isLocked() || plugin.getPlayers().isBanned(currentIsland.getOwner(),user.getUniqueId()))) {
                if (!currentIsland.getMemberSet().contains(playerUUID) && !user.hasPermission(Constants.PERMPREFIX + "mod.bypassprotect")) {
                    user.sendMessage("locked.islandlocked");
                    plugin.getIslands().homeTeleport(user.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        players.removeOnlinePlayer(event.getPlayer().getUniqueId());
        User.removePlayer(event.getPlayer());
    }
}
