package us.tastybento.bskyblock.database.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.annotations.Expose;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.adapters.Adapter;
import us.tastybento.bskyblock.database.objects.adapters.FlagSerializer;
import us.tastybento.bskyblock.managers.RanksManager;
import us.tastybento.bskyblock.util.Pair;
import us.tastybento.bskyblock.util.Util;

/**
 * Stores all the info about an island
 * Managed by IslandsManager
 * Responsible for team information as well.
 *
 * @author Tastybento
 * @author Poslovitch
 */
public class Island implements DataObject {

    @Expose
    private String uniqueId = UUID.randomUUID().toString();

    //// Island ////
    // The center of the island itself
    @Expose
    private Location center;

    // Island range
    @Expose
    private int range;

    // Coordinates of the island area
    @Expose
    private int minX;
    @Expose
    private int minZ;

    // Coordinates of minimum protected area
    @Expose
    private int minProtectedX;
    @Expose
    private int minProtectedZ;

    // Protection size
    @Expose
    private int protectionRange;

    // World the island started in. This may be different from the island location
    @Expose
    private World world;

    // Display name
    @Expose
    private String name = "";

    // Time parameters
    @Expose
    private long createdDate;
    @Expose
    private long updatedDate;

    //// Team ////
    @Expose
    private UUID owner;

    @Expose
    private Map<UUID, Integer> members = new HashMap<>();

    //// State ////
    @Expose
    private boolean locked = false;
    @Expose
    private boolean spawn = false;
    @Expose
    private boolean purgeProtected = false;

    //// Protection flags ////
    @Adapter(FlagSerializer.class)
    @Expose
    private Map<Flag, Integer> flags = new HashMap<>();

    @Expose
    private int levelHandicap;
    @Expose
    private Location spawnPoint;

    public Island() {}
    public Island(Location location, UUID owner, int protectionRange) {
        setOwner(owner);
        createdDate = System.currentTimeMillis();
        updatedDate = System.currentTimeMillis();
        world = location.getWorld();
        center = location;
        range = BSkyBlock.getInstance().getSettings().getIslandDistance();
        minX = center.getBlockX() - range;
        minZ = center.getBlockZ() - range;
        this.protectionRange = protectionRange;
        minProtectedX = center.getBlockX() - protectionRange;
        minProtectedZ = center.getBlockZ() - protectionRange;
    }

    /**
     * Adds a team member. If player is on banned list, they will be removed from it.
     * @param playerUUID - the player's UUID
     */
    public void addMember(UUID playerUUID) {
        if (playerUUID != null) {
            members.put(playerUUID, RanksManager.MEMBER_RANK);
        }
    }
    /**
     * Adds target to a list of banned players for this island. May be blocked by the event being cancelled.
     * If the player is a member, coop or trustee, they will be removed from those lists.
     * @param targetUUID - the target's UUID
     * @return true if successfully added
     */
    public boolean addToBanList(UUID targetUUID) {
        if (targetUUID != null) {
            members.put(targetUUID, RanksManager.BANNED_RANK);
        }
        return true;
    }

    /**
     * @return the banned
     */
    public Set<UUID> getBanned() {
        Set<UUID> result = new HashSet<>();
        for (Entry<UUID, Integer> member: members.entrySet()) {
            if (member.getValue() <= RanksManager.BANNED_RANK) {
                result.add(member.getKey());
            }
        }
        return result;
    }

    /**
     * @return the center Location
     */
    public Location getCenter(){
        return center;
    }

    /**
     * @return the date when the island was created
     */
    public long getCreatedDate(){
        return createdDate;
    }
    /**
     * Gets the rank needed to bypass this Island Guard flag
     * @param flag
     * @return the rank needed to bypass this flag. Players must have at least this rank to bypass this flag.
     */
    public int getFlag(Flag flag){
        if(flags.containsKey(flag)) {
            return flags.get(flag);
        } else {
            flags.put(flag, RanksManager.MEMBER_RANK);
            return RanksManager.MEMBER_RANK;
        }
    }

    /**
     * @return the flags
     */
    public Map<Flag, Integer> getFlags() {
        return flags;
    }

    /**
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * @return true if the island is locked, otherwise false
     */
    public boolean getLocked(){
        return locked;
    }

    /**
     * @return the members
     */
    public Map<UUID, Integer> getMembers() {
        return members;
    }

    /**
     * @return the members of the island (owner included)
     */
    public ImmutableSet<UUID> getMemberSet(){
        Builder<UUID> result = new ImmutableSet.Builder<>();

        for (Entry<UUID, Integer> member: members.entrySet()) {
            if (member.getValue() >= RanksManager.MEMBER_RANK) {
                result.add(member.getKey());
            }
        }
        return result.build();
    }

    /**
     * @return the minProtectedX
     */
    public final int getMinProtectedX() {
        return minProtectedX;
    }

    /**
     * @return the minProtectedZ
     */
    public final int getMinProtectedZ() {
        return minProtectedZ;
    }

    /**
     * @return the minX
     */
    public final int getMinX() {
        return minX;
    }

    /**
     * @return the minZ
     */
    public final int getMinZ() {
        return minZ;
    }

    /**
     * @return the island display name or the owner's name if none is set
     */
    public String getName() {
        return name;
    }

    /**
     * @return the owner (team leader)
     */
    public UUID getOwner(){
        return owner;
    }

    /**
     * @return the protectionRange
     */
    public int getProtectionRange() {
        return protectionRange;
    }

    /**
     * @return true if the island is protected from the Purge, otherwise false
     */
    public boolean getPurgeProtected(){
        return purgeProtected;
    }

    /**
     * @return the island range
     */
    public int getRange(){
        return range;
    }

    /**
     * Get the rank of user for this island
     * @param user - the User
     * @return rank integer
     */
    public int getRank(User user) {
        return members.getOrDefault(user.getUniqueId(), RanksManager.VISITOR_RANK);
    }

    /**
     * @return true if the island is the spawn otherwise false
     */
    public boolean getSpawn(){
        return spawn;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * @param material
     * @return count of how many tile entities of type mat are on the island at last count. Counts are done when a player places
     * a tile entity.
     */
    public int getTileEntityCount(Material material, World world) {
        int result = 0;
        for (int x = getMinProtectedX() /16; x <= (getMinProtectedX() + getProtectionRange() - 1)/16; x++) {
            for (int z = getMinProtectedZ() /16; z <= (getMinProtectedZ() + getProtectionRange() - 1)/16; z++) {
                for (BlockState holder : world.getChunkAt(x, z).getTileEntities()) {
                    if (onIsland(holder.getLocation())) {
                        if (holder.getType() == material) {
                            result++;
                        } else if (material.equals(Material.REDSTONE_COMPARATOR_OFF)) {
                            if (holder.getType().equals(Material.REDSTONE_COMPARATOR_ON)) {
                                result++;
                            }
                        } else if (material.equals(Material.FURNACE)) {
                            if (holder.getType().equals(Material.BURNING_FURNACE)) {
                                result++;
                            }
                        } else if (material.toString().endsWith("BANNER")) {
                            if (holder.getType().toString().endsWith("BANNER")) {
                                result++;
                            }
                        } else if (material.equals(Material.WALL_SIGN) || material.equals(Material.SIGN_POST)) {
                            if (holder.getType().equals(Material.WALL_SIGN) || holder.getType().equals(Material.SIGN_POST)) {
                                result++;
                            }
                        }
                    }
                }
                for (Entity holder : world.getChunkAt(x, z).getEntities()) {
                    if (holder.getType().toString().equals(material.toString()) && onIsland(holder.getLocation())) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @return the date when the island was updated (team member connection, etc...)
     */
    public long getUpdatedDate(){
        return updatedDate;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the x coordinate of the island center
     */
    public int getX(){
        return center.getBlockX();
    }

    /**
     * @return the y coordinate of the island center
     */
    public int getY(){
        return center.getBlockY();
    }

    /**
     * @return the z coordinate of the island center
     */
    public int getZ(){
        return center.getBlockZ();
    }

    /**
     * Checks if coords are in the island space
     * @param x - x coordinate
     * @param z - z coordinate
     * @return true if in the island space
     */
    public boolean inIslandSpace(int x, int z) {
         return x >= minX && x < minX + range*2 && z >= minZ && z < minZ + range*2;
    }

    public boolean inIslandSpace(Location location) {
        if (Util.inWorld(location)) {
            return inIslandSpace(location.getBlockX(), location.getBlockZ());
        }
        return false;
    }

    /**
     * Checks if the coords are in island space
     * @param blockCoord
     * @return true or false
     */
    public boolean inIslandSpace(Pair<Integer, Integer> blockCoord) {
        return inIslandSpace(blockCoord.x, blockCoord.z);
    }

    /**
     * Check if the flag is allowed or not
     * For flags that are for the island in general and not related to rank
     * @param flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(Flag flag) {
        return getFlag(flag) >= 0;
    }

    /**
     * Check if a user is allowed to bypass the flag or not
     * @param user - the User - user
     * @param flag - flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(User user, Flag flag) {
        return getRank(user) >= getFlag(flag);
    }

    /**
     * Check if banned
     * @param targetUUID - the target's UUID
     * @return Returns true if target is banned on this island
     */
    public boolean isBanned(UUID targetUUID) {
        return members.containsKey(targetUUID) && members.get(targetUUID).equals(RanksManager.BANNED_RANK);
    }

    /**
     * @return true if island is locked, false if not
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @return spawn
     */
    public boolean isSpawn() {
        return spawn;
    }

    /**
     * Checks if a location is within this island's protected area
     *
     * @param target
     * @return true if it is, false if not
     */
    public boolean onIsland(Location target) {
        if (center != null && center.getWorld() != null) {
            return target.getBlockX() >= minProtectedX && target.getBlockX() < (minProtectedX + protectionRange * 2)
                    && target.getBlockZ() >= minProtectedZ && target.getBlockZ() < (minProtectedZ + protectionRange * 2);
        }

        return false;
    }

    /**
     * Removes target from the banned list. May be cancelled by unban event.
     * @param targetUUID - the target's UUID
     * @return true if successful, otherwise false.
     */
    public boolean removeFromBanList(UUID targetUUID) {
        members.remove(targetUUID);
        return true;
    }

    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
    }

    /**
     * @param center the center to set
     */
    public void setCenter(Location center) {
        this.center = center;
    }

    /**
     * @param createdDate - the createdDate to sets
     */
    public void setCreatedDate(long createdDate){
        this.createdDate = createdDate;
    }

    /**
     * Set the Island Guard flag rank
     * @param flag
     * @param value - rank value. If the flag applies to the island, a positive number = true, negative = false
     */
    public void setFlag(Flag flag, int value){
        flags.put(flag, value);
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(Map<Flag, Integer> flags) {
        this.flags = flags;
    }

    /**
     * Resets the flags to their default as set in config.yml for this island
     */
    public void setFlagsDefaults(){
        /*for(SettingsFlag flag : SettingsFlag.values()){
            this.flags.put(flag, Settings.defaultIslandSettings.get(flag));
        }*/ //TODO default flags
    }

    /**
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
    }

    /**
     * Locks/Unlocks the island.
     * @param locked - the lock state to set
     */
    public void setLocked(boolean locked){
        this.locked = locked;
    }

    /**
     * @param members the members to set
     */
    public void setMembers(Map<UUID, Integer> members) {
        this.members = members;
    }

    /**
     * @param minProtectedX the minProtectedX to set
     */
    public final void setMinProtectedX(int minProtectedX) {
        this.minProtectedX = minProtectedX;
    }

    /**
     * @param minProtectedZ the minProtectedZ to set
     */
    public final void setMinProtectedZ(int minProtectedZ) {
        this.minProtectedZ = minProtectedZ;
    }

    /**
     * @param minX the minX to set
     */
    public final void setMinX(int minX) {
        this.minX = minX;
    }

    /**
     * @param minZ the minZ to set
     */
    public final void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    /**
     * @param name - the display name to set
     *               Set to null to remove the display name
     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Sets the owner of the island.
     * @param owner - the island owner - the owner/team leader to set
     */
    public void setOwner(UUID owner){
        this.owner = owner;
        if (owner == null) {
            return;
        }
        // Defensive code: demote any previous owner
        for (Entry<UUID, Integer> en : members.entrySet()) {
            if (en.getValue().equals(RanksManager.OWNER_RANK)) {
                en.setValue(RanksManager.MEMBER_RANK);
            }
        }
        members.put(owner, RanksManager.OWNER_RANK);
    }

    /**
     * @param protectionRange the protectionRange to set
     */
    public void setProtectionRange(int protectionRange) {
        this.protectionRange = protectionRange;
    }

    /**
     * @param purgeProtected - if the island is protected from the Purge
     */
    public void setPurgeProtected(boolean purgeProtected){
        this.purgeProtected = purgeProtected;
    }

    /**
     * @param range - the range to set
     */
    public void setRange(int range){
        this.range = range;
    }

    /**
     * Set user's rank to an arbitrary rank value
     * @param user - the User
     * @param rank
     */
    public void setRank(User user, int rank) {
        if (user.getUniqueId() != null) {
            members.put(user.getUniqueId(), rank);
        }
    }

    /**
     * @param ranks the ranks to set
     */
    public void setRanks(Map<UUID, Integer> ranks) {
        members = ranks;
    }

    /**
     * @param isSpawn - if the island is the spawn
     */
    public void setSpawn(boolean isSpawn){
        spawn = isSpawn;
    }

    /**
     * Resets the flags to their default as set in config.yml for the spawn
     */
    public void setSpawnFlagsDefaults(){
        /*for(SettingsFlag flag : SettingsFlag.values()){
            this.flags.put(flag, Settings.defaultSpawnSettings.get(flag));
        }*/ //TODO default flags
    }

    public void setSpawnPoint(Location location) {
        spawnPoint = location;

    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @param updatedDate - the updatedDate to sets
     */
    public void setUpdatedDate(long updatedDate){
        this.updatedDate = updatedDate;
    }

    /**
     * @param world the world to set
     */
    public void setWorld(World world) {
        this.world = world;
    }
}