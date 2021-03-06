/**
 * 
 */
package us.tastybento.bskyblock.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;

/**
 * Enables changing of obsidian back into lava
 * @author tastybento
 *
 */
public class ObsidianToLava implements Listener {

    private BSkyBlock plugin;


    /**
     * @param plugin
     */
    public ObsidianToLava(BSkyBlock plugin) {
        this.plugin = plugin;
    }


    /**
     * Enables changing of obsidian back into lava
     * 
     * @param e - event
     * @return false if obsidian not scooped, true if scooped
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public boolean onPlayerInteract(final PlayerInteractEvent e) {
        if (!plugin.getSettings().isAllowObsidianScooping() 
                || !plugin.getIslandWorldManager().inWorld(e.getPlayer().getLocation())
                || !e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)
                || !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || !(e.getItem() != null && e.getItem().getType().equals(Material.BUCKET))
                || !(e.getClickedBlock() != null && e.getClickedBlock().getType().equals(Material.OBSIDIAN))) {
            return false;
        }
        User user = User.getInstance(e.getPlayer());
        if (plugin.getIslands().playerIsOnIsland(user)) {
            // Look around to see if this is a lone obsidian block
            Block b = e.getClickedBlock();
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        Material testBlock = b.getWorld().getBlockAt(b.getX() + x, b.getY() + y, b.getZ() + z).getType();
                        if ((x != 0 || y != 0 || z != 0) && testBlock.equals(Material.OBSIDIAN)) {
                            // Do nothing special
                            return false;

                        }
                    }
                }
            }
            user.sendMessage("general.tips.changing-ob-to-lava");
            e.getItem().setType(Material.LAVA_BUCKET);
            e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1F, 1F);
            e.getClickedBlock().setType(Material.AIR);
            e.setCancelled(true);
            return true;
        }

        return false;
    }
}
