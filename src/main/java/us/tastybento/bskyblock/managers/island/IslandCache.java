package us.tastybento.bskyblock.managers.island;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

public class IslandCache {
    private BSkyBlock plugin = BSkyBlock.getInstance();
    private BiMap<Location, Island> islandsByLocation;
    /**
     * Every player who is associated with an island is in this map.
     */
    private HashMap<UUID, Island> islandsByUUID;
    // 2D islandGrid of islands, x,z
    private TreeMap<Integer, TreeMap<Integer, Island>> islandGrid = new TreeMap<>();

    public IslandCache() {
        islandsByLocation = HashBiMap.create();
        islandsByUUID = new HashMap<>();
    }

    /**
     * Adds an island to the grid
     * @param island
     */
    public void addIsland(Island island) {
        islandsByLocation.put(island.getCenter(), island);
        islandsByUUID.put(island.getOwner(), island);
        for (UUID member: island.getMemberSet()) {
            islandsByUUID.put(member, island);
        }
        addToGrid(island);
    }

    public void addPlayer(UUID playerUUID, Island teamIsland) {
        islandsByUUID.put(playerUUID, teamIsland);
    }

    /**
     * Adds an island to the grid register
     * @param newIsland
     */
    private void addToGrid(Island newIsland) {
        if (islandGrid.containsKey(newIsland.getMinX())) {
            TreeMap<Integer, Island> zEntry = islandGrid.get(newIsland.getMinX());
            if (zEntry.containsKey(newIsland.getMinZ())) {
                // Island already exists
                Island conflict = islandGrid.get(newIsland.getMinX()).get(newIsland.getMinZ());
                plugin.getLogger().warning("*** Duplicate or overlapping islands! ***");
                plugin.getLogger().warning(
                        "Island at (" + newIsland.getCenter().getBlockX() + ", " + newIsland.getCenter().getBlockZ() + ") conflicts with ("
                                + conflict.getCenter().getBlockX() + ", " + conflict.getCenter().getBlockZ() + ")");
                if (conflict.getOwner() != null) {
                    plugin.getLogger().warning("Accepted island is owned by " + plugin.getPlayers().getName(conflict.getOwner()));
                    plugin.getLogger().warning(conflict.getOwner().toString() + ".yml");
                } else {
                    plugin.getLogger().warning("Accepted island is unowned.");
                }
                if (newIsland.getOwner() != null) {
                    plugin.getLogger().warning("Denied island is owned by " + plugin.getPlayers().getName(newIsland.getOwner()));
                    plugin.getLogger().warning(newIsland.getOwner().toString() + ".yml");
                } else {
                    plugin.getLogger().warning("Denied island is unowned and was just found in the islands folder. Skipping it...");
                }
                plugin.getLogger().warning("Recommend that the denied player file is deleted otherwise weird things can happen.");
                return;
            } else {
                // Add island
                zEntry.put(newIsland.getMinZ(), newIsland);
                islandGrid.put(newIsland.getMinX(), zEntry);
            }
        } else {
            // Add island
            TreeMap<Integer, Island> zEntry = new TreeMap<>();
            zEntry.put(newIsland.getMinZ(), newIsland);
            islandGrid.put(newIsland.getMinX(), zEntry);
        }
    }

    public void clear() {
        islandsByLocation.clear();
        islandsByUUID.clear();
    }

    public Island createIsland(Island island) {
        islandsByLocation.put(island.getCenter(), island);
        if (island.getOwner() != null) {
            islandsByUUID.put(island.getOwner(), island);
        }
        addToGrid(island);
        return island;
    }

    /**
     * Create an island with no owner at location
     * @param location - the location
     */
    public Island createIsland(Location location){
        return createIsland(location, null);
    }

    /**
     * Create an island with owner. Note this does not create the schematic. It just creates the island data object.
     * @param location - the location
     * @param owner - the island owner UUID
     */
    public Island createIsland(Location location, UUID owner){
        Island island = new Island(location, owner, plugin.getSettings().getIslandProtectionRange());
        islandsByLocation.put(location, island);
        if (owner != null) {
            islandsByUUID.put(owner, island);
        }
        addToGrid(island);
        return island;
    }

    /**
     * Deletes an island from the database. Does not remove blocks
     * @param island
     */
    public void deleteIslandFromCache(Island island) {
        if (!islandsByLocation.remove(island.getCenter(), island)) {
            plugin.getLogger().severe("Could not remove island from cache!");
        }
        Iterator<Entry<UUID, Island>> it = islandsByUUID.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Island> en = it.next();
            if (en.getValue().equals(island)) {
                it.remove();
            }
        }
        // Remove from grid
        if (island != null) {
            int x = island.getMinX();
            int z = island.getMinZ();
            if (islandGrid.containsKey(x)) {
                TreeMap<Integer, Island> zEntry = islandGrid.get(x);
                if (zEntry.containsKey(z)) {
                    // Island exists - delete it
                    zEntry.remove(z);
                    islandGrid.put(x, zEntry);
                }
            }
        }
    }

    public Island get(Location location) {
        return islandsByLocation.get(location);
    }

    public Island get(UUID uuid) {
        return islandsByUUID.get(uuid);
    }

    /**
     * Gets the island for this player. If they are in a team, the team island is returned
     * @param uuid - UUID
     * @return Island
     */
    public Island getIsland(UUID uuid){
        return islandsByUUID.get(uuid);
    }

    /**
     * Returns the island at the x,z location or null if there is none.
     * This includes the full island space, not just the protected area.
     *
     * @param x - x coordinate
     * @param z - z coordinate
     * @return Island or null
     */
    public Island getIslandAt(int x, int z) {
        Entry<Integer, TreeMap<Integer, Island>> en = islandGrid.floorEntry(x);
        if (en != null) {
            Entry<Integer, Island> ent = en.getValue().floorEntry(z);
            if (ent != null) {
                // Check if in the island range
                Island island = ent.getValue();
                if (island.inIslandSpace(x, z)) {
                    return island;
                }
            }
        }
        return null;
    }

    /**
     * Returns the island at the location or null if there is none.
     * This includes the full island space, not just the protected area
     *
     * @param location - the location
     * @return Island object
     */
    public Island getIslandAt(Location location) {
        if (location == null) {
            return null;
        }
        // World check
        if (!Util.inWorld(location)) {
            return null;
        }
        return getIslandAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Returns the player's island location.
     * Returns an island location OR a team island location
     *
     * @param playerUUID - the player's UUID
     * @return Location of player's island or null if one does not exist
     */
    public Location getIslandLocation(UUID playerUUID) {
        if (hasIsland(playerUUID)) {
            return getIsland(playerUUID).getCenter();
        }
        return null;
    }

    /**
     * Get name of the island owned by owner
     * @param owner - the island owner
     * @return Returns the name of owner's island, or the owner's name if there is none.
     */
    public String getIslandName(UUID owner) {
        String result = plugin.getPlayers().getName(owner);
        if (islandsByUUID.containsKey(owner)) {
            Island island = islandsByUUID.get(owner);
            if (island.getName() != null && !island.getName().isEmpty()) {
                result = island.getName();
            }
        }
        return ChatColor.translateAlternateColorCodes('&', result) + ChatColor.RESET;
    }

    public Collection<Island> getIslands() {
        return Collections.unmodifiableCollection(islandsByLocation.values());
    }

    public Set<UUID> getMembers(UUID playerUUID) {
        Island island = islandsByUUID.get(playerUUID);
        if (island != null) {
            return island.getMemberSet();
        }
        return new HashSet<>(0);
    }

    public UUID getTeamLeader(UUID playerUUID) {
        if (islandsByUUID.containsKey(playerUUID)) {
            return islandsByUUID.get(playerUUID).getOwner();
        }
        return null;
    }

    /**
     * @param playerUUID - the player's UUID
     * @return true if player has island and owns it
     */
    public boolean hasIsland(UUID playerUUID) {
        return (islandsByUUID.containsKey(playerUUID) && islandsByUUID.get(playerUUID).getOwner() != null 
                && (islandsByUUID.get(playerUUID).getOwner().equals(playerUUID))) ? true : false;
    }

    public void removePlayer(UUID playerUUID) {
        Island island = islandsByUUID.get(playerUUID);
        if (island != null) {
            if (island.getOwner() != null && island.getOwner().equals(playerUUID)) {
                // Clear ownership and members
                island.getMembers().clear();
                island.setOwner(null);
            }
            island.removeMember(playerUUID);
        }
        islandsByUUID.remove(playerUUID);

    }

    public void setIslandName(UUID owner, String name) {
        if (islandsByUUID.containsKey(owner)) {
            Island island = islandsByUUID.get(owner);
            island.setName(name);
        }

    }

    public int size() {
        return islandsByLocation.size();
    }
    
}
