package com.stickmanitems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class StickmanItems extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String VERSION = "3.0.0";

    private record ItemDef(String id, String name, Material mat, String hex, String lore) {}

    private static final class PhoenixState {
        int phase; // 0 = rising, 1 = diving, 2 = just landed
        int ticks;
        double peak;
        boolean call;
    }

    private final Map<String, ItemDef> items = new LinkedHashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> jetFuel = new HashMap<>();
    private final Map<UUID, PhoenixState> phoenix = new HashMap<>();
    private final Map<UUID, UUID> holds = new HashMap<>();
    private final Map<UUID, UUID> golems = new HashMap<>();
    private final Map<UUID, Integer> fallGuard = new HashMap<>();
    private final Random rnd = new Random();
    private final Set<UUID> noCooldown = new HashSet<>();
    private int hudTick = 0;
    private int auraTick = 0;
    private int gemTick = 0;
    private NamespacedKey ownerKey;
    private NamespacedKey ultKey;
    private final Map<String, ItemDef> gems = new LinkedHashMap<>();
    private final Set<UUID> gemNoCd = new HashSet<>();
    private final Map<UUID, Set<UUID>> trusts = new HashMap<>();
    private final Map<UUID, Long> snowUntil = new HashMap<>();
    private final Map<UUID, Long> nightmareUntil = new HashMap<>();
    private final Map<UUID, Long> witherTouchUntil = new HashMap<>();
    private final Map<UUID, Long> subzeroUntil = new HashMap<>();
    private final Map<UUID, Deque<EntityType>> lastKills = new HashMap<>();
    private final Map<String, Integer> snowHits = new HashMap<>();
    private final List<BarrierState> barriers = new ArrayList<>();
    private final Map<UUID, Integer> abilityCount = new HashMap<>();
    private final Set<UUID> ultReady = new HashSet<>();
    private final List<UUID> followers = new ArrayList<>();
    private final Map<UUID, double[]> charging = new HashMap<>();
    private static final Set<EntityType> NO_SUMMON = EnumSet.of(
            EntityType.WARDEN, EntityType.WITHER, EntityType.ENDER_DRAGON,
            EntityType.ELDER_GUARDIAN, EntityType.GIANT, EntityType.IRON_GOLEM);
    private static final List<String> STICKMAN_SUBS = List.of("givegem", "nocooldowngem", "trust", "summon", "help");
    private final Map<String, ShrineHolder> shrines = new HashMap<>();
    private final Map<UUID, List<ItemStack>> deathKeep = new HashMap<>();
    private final Map<UUID, Integer> boltCharges = new HashMap<>();
    private final Map<UUID, Integer> surgeHits = new HashMap<>();
    private final Map<UUID, Long> zeusUntil = new HashMap<>();
    private boolean inThunderProc = false;
    private final Map<UUID, Long> windBerserkUntil = new HashMap<>();
    private final Map<UUID, Long> blazeUntil = new HashMap<>();
    private final Map<UUID, Long> chargeStart = new HashMap<>();
    private final Set<UUID> evapOn = new HashSet<>();
    private final Map<UUID, Map<Block, Material>> evapSaved = new HashMap<>();
    private final Map<String, UUID> pads = new HashMap<>();
    private final Map<UUID, Long> launchedUntil = new HashMap<>();
    private final Map<UUID, Long> shieldUntil = new HashMap<>();
    private final Map<UUID, Long> meltdownUntil = new HashMap<>();

    private record BarrierData(UUID owner, Location center, Map<Block, Material> originals, long expiry) {}
    private NamespacedKey itemKey;
    private NamespacedKey projKey;

    @Override
    public void onEnable() {
        itemKey = new NamespacedKey(this, "item");
        projKey = new NamespacedKey(this, "proj");
        ownerKey = new NamespacedKey(this, "owner");
        ultKey = new NamespacedKey(this, "ult");
        registerItems();
        registerGems();
        loadShrines();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("stickmanitems").setExecutor(this);
        getCommand("stickmanitems").setTabCompleter(this);
        getCommand("stickman").setExecutor(this);
        getCommand("stickman").setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        getLogger().info("StickmanItems " + VERSION + " enabled.");
    }

    private void def(String id, String name, Material mat, String hex, String lore) {
        items.put(id, new ItemDef(id, name, mat, hex, lore));
    }

    private void registerItems() {
        def("kaboom", "Kaboom Kachow", Material.BOW, "#FF3D00", "Every arrow shot from this bow explodes on impact.");
        def("shakalaka", "Shakalaka Bow", Material.BOW, "#00FF9C", "Each arrow that lands triggers 3 random effects: lightning, explosion or fireball.");
        def("thunderstick", "Thunderstick", Material.NETHERITE_SWORD, "#FFEA00", "Hits strike lightning. Right-click to call lightning where you look.");
        def("vampirefang", "Vampire Fang", Material.NETHERITE_SWORD, "#FF1744", "Heals you for 40% of melee damage. Right-click for a 4-heart heal.");
        def("frostbite", "Frostbite", Material.NETHERITE_SWORD, "#00E5FF", "Hits slow targets and add extra damage. Right-click for a frost nova.");
        def("winddash", "Wind Shard", Material.BREEZE_ROD, "#B3E5FC", "Right-click to dash forward. No cooldown. No fall damage from the dash.");
        def("infernowand", "Inferno Wand", Material.BLAZE_ROD, "#FF9100", "Right-click to launch a fireball. Fireballs do not break blocks.");
        def("groundbreaker", "Groundbreaker", Material.MACE, "#FFB300", "Right-click for a shockwave that damages and knocks back nearby enemies. Breaks no blocks.");
        def("phoenixblade", "Phoenix Blade", Material.MACE, "#FF2D75", "Right-click to leap into the air, then dive and smash. Harder falls hit harder.");
        def("gauntlet", "Control Orb", Material.ENDER_PEARL, "#7C4DFF", "Right-click a mob or player to take control of it. Right-click again to hurl it forward.");
        def("jetpack", "Jetpack", Material.NETHERITE_CHESTPLATE, "#00BCD4", "Right-click to toggle thrust. 10 minutes of fuel. No cooldown.");
        def("grenade", "Grenade", Material.EGG, "#76FF03", "Right-click to throw. Explodes on impact without breaking blocks or hurting you.");
        def("magnetrod", "Magnet Rod", Material.LIGHTNING_ROD, "#D500F9", "Right-click to pull nearby mobs toward you, hard.");
        def("blizzardorb", "Blizzard Orb", Material.PACKED_ICE, "#18FFFF", "Right-click to freeze everything nearby and slow it hard.");
        def("healingcharm", "Healing Charm", Material.GHAST_TEAR, "#FF4081", "Right-click to heal 8 hearts, gain regeneration and remove negative effects.");
        def("shadowcloak", "Shadow Cloak", Material.PHANTOM_MEMBRANE, "#6200EA", "Right-click for 20 seconds of invisibility and Speed II.");
        def("golembell", "Golem Call", Material.GOAT_HORN, "#FFC400", "Blow the horn to summon a friendly iron golem for 90 seconds.");
        def("sonichorn", "Sonic Horn", Material.GOAT_HORN, "#FFAB40", "Right-click for a sonic boom that hits hard and knocks back everything in its path.");
        def("stormheart", "Storm Heart", Material.HEART_OF_THE_SEA, "#2962FF", "Right-click to call lightning on up to 5 nearby mobs.");
        def("phasefruit", "Phase Fruit", Material.CHORUS_FRUIT, "#00E676", "Right-click to blink forward up to 12 blocks, then get Speed II. Never puts you inside a block.");
        def("voidscythe", "Void Slasher", Material.NETHERITE_SWORD, "#651FFF", "Right-click for a wide void slash that hits everything in front of you.");
        def("grapple", "Grapple Hook", Material.LEAD, "#8D6E63", "Right-click to hook a block or mob and pull yourself toward it. Pulls harder.");
        def("starfall", "Starfall Staff", Material.STICK, "#FFF59D", "Right-click to call 5 falling stars onto the spot you look at.");
        def("venom", "Venom Dagger", Material.NETHERITE_SWORD, "#64DD17", "Poisons normal mobs and players. Heals undead mobs. Right-click to lunge.");
        def("glacier", "Phoenix Call", Material.MACE, "#FF6E40", "Right-click to leap, then slam with fire and ice. The higher you fall, the harder it hits.");
        def("tidal", "Tidal Sword", Material.NETHERITE_SWORD, "#0091EA", "Right-click to fire a water orb that blasts everything near its impact.");
        def("reapersigil", "Reaper's Sigil", Material.WITHER_SKELETON_SKULL, "#E040FB", "Withers normal mobs and players and heals you for each hit. Heals undead mobs.");
        def("arccaster", "Arc Caster", Material.BOW, "#E1BEE7", "A bow. Arrows stun or freeze whatever they hit.");
        def("ghostlantern", "Ghost Lantern", Material.SOUL_LANTERN, "#B388FF", "Right-click for 10 seconds of orbiting spirits that zap nearby enemies. Never hits your golems.");
    }

    private void tick() {
        tickPhoenix();
        tickJetpack();
        tickGem();
        tickFallGuard();
        tickGems();
        tickAxolotlRide();
        tickEvaporation();
        if (++hudTick % 5 == 0) updateHud();
    }

    // Action bar sits directly above the hotbar.
    private void updateHud() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String id = idOf(p.getInventory().getItemInMainHand());
            if (id == null) id = idOf(p.getInventory().getItemInOffHand());
            if (id == null) continue;
            if (id.equals("kaboom") || id.equals("shakalaka") || id.equals("arccaster") || id.equals("jetpack")) continue;
            ItemDef d = items.get(id);
            if (d == null) d = gems.get(id);
            if (d == null) continue;
            String title = hexColor(d.hex()) + d.name();
            String text;
            if (noCooldown.contains(p.getUniqueId())) {
                text = title + ChatColor.AQUA + " - NO COOLDOWN";
            } else if (id.equals("jetpack") && jetFuel.containsKey(p.getUniqueId())) {
                double secs = jetFuel.get(p.getUniqueId()) / 20.0;
                text = title + ChatColor.GREEN + " - FUEL " + String.format(Locale.ROOT, "%.1fs", secs);
            } else {
                long left = cdLeft(p, id);
                if (left > 0) {
                    text = title + ChatColor.GRAY + " - " + ChatColor.RED + String.format(Locale.ROOT, "%.1fs", left / 1000.0);
                } else {
                    text = title + ChatColor.GREEN + " - READY";
                }
            }
            p.sendActionBar(text);
        }
    }

    // ---------- Helpers ----------

    private void venomTouch(LivingEntity le) {
        if (isUndead(le)) {
            le.setHealth(Math.min(le.getMaxHealth(), le.getHealth() + 4));
        } else {
            le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
        }
    }

    private void cleanse(Player p) {
        for (PotionEffectType t : List.of(PotionEffectType.WITHER, PotionEffectType.POISON,
                PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.NAUSEA,
                PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS, PotionEffectType.HUNGER)) {
            p.removePotionEffect(t);
        }
    }

    // Ult Ready: 10 ability uses unlock the ultimate for the next cast.
    private void countAbility(Player p) {
        int n = abilityCount.merge(p.getUniqueId(), 1, Integer::sum);
        if (n == 10) {
            p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Ult Ready");
            p.sendTitle(ChatColor.GOLD + "Ult Ready", "", 5, 40, 10);
            ultReady.add(p.getUniqueId());
        }
    }

    private boolean ultGate(Player p) {
        if (ultReady.remove(p.getUniqueId())) {
            abilityCount.remove(p.getUniqueId());
            return true;
        }
        p.sendMessage(ChatColor.RED + "Ultimate locked: " + abilityCount.getOrDefault(p.getUniqueId(), 0) + "/10 ability uses.");
        return false;
    }

    private void addFollower(Entity e) {
        followers.add(e.getUniqueId());
    }

    // Summoned undead and creakings walk back to their owner like a dog.
    private void tickFollowers() {
        Iterator<UUID> it = followers.iterator();
        while (it.hasNext()) {
            Entity mob = Bukkit.getEntity(it.next());
            if (mob == null || !mob.isValid()) { it.remove(); continue; }
            UUID own = ownerOf(mob);
            Player owner = own == null ? null : Bukkit.getPlayer(own);
            if (owner == null || !owner.isOnline()) continue;
            if (mob.getWorld() != owner.getWorld()) {
                mob.teleport(owner.getLocation());
                continue;
            }
            double d = mob.getLocation().distance(owner.getLocation());
            if (d > 25) {
                mob.teleport(owner.getLocation().add(2, 0, 2));
            } else if (d > 4 && mob instanceof Mob m) {
                m.getPathfinder().moveTo(owner.getLocation(), 1.2);
            }
        }
    }

    private EntityType creakingType() {
        try {
            return EntityType.valueOf("CREAKING");
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Entity spawnCreaking(Location loc, EntityType type, UUID owner, double scale, String tag) {
        Entity c = loc.getWorld().spawnEntity(loc, type);
        setOwner(c, owner);
        c.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, tag);
        if (c instanceof LivingEntity le) {
            AttributeInstance sc = le.getAttribute(Attribute.GENERIC_SCALE);
            if (sc != null) sc.setBaseValue(scale);
        }
        addFollower(c);
        return c;
    }

    private String hexColor(String hex) {
        return net.md_5.bungee.api.ChatColor.of(hex).toString();
    }

    private String idOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        PersistentDataContainer c = it.getItemMeta().getPersistentDataContainer();
        return c.get(itemKey, PersistentDataType.STRING);
    }

    private ItemStack make(ItemDef d) {
        ItemStack it = new ItemStack(d.mat());
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(hexColor(d.hex()) + ChatColor.BOLD + d.name());
        m.setLore(List.of(ChatColor.GRAY + d.lore()));
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, d.id());
        it.setItemMeta(m);
        return it;
    }

    private boolean ready(Player p, String id, long ms) {
        if (noCooldown.contains(p.getUniqueId()) || (id.startsWith("g_") && gemNoCd.contains(p.getUniqueId()))) return true;
        long now = System.currentTimeMillis();
        String key = p.getUniqueId() + ":" + id;
        Long expiry = cooldowns.get(key);
        if (expiry != null && now < expiry) {
            p.sendMessage(ChatColor.RED + "On cooldown: " + ((expiry - now) / 1000 + 1) + "s");
            return false;
        }
        cooldowns.put(key, now + ms);
        return true;
    }

    private long cdLeft(Player p, String id) {
        if (noCooldown.contains(p.getUniqueId())) return 0;
        Long expiry = cooldowns.get(p.getUniqueId() + ":" + id);
        if (expiry == null) return 0;
        return Math.max(0, expiry - System.currentTimeMillis());
    }

    private void guard(Player p, int ticks) {
        fallGuard.put(p.getUniqueId(), Math.max(fallGuard.getOrDefault(p.getUniqueId(), 0), ticks));
    }

    private void hit(LivingEntity le, double dmg, Entity src) {
        if (src != null) le.damage(dmg, src);
        else le.damage(dmg);
    }

    private void damageArea(Location c, double r, double dmg, Entity src) {
        for (Entity en : c.getWorld().getNearbyEntities(c, r, r, r)) {
            if (!(en instanceof LivingEntity le) || en.equals(src)) continue;
            if (le.getLocation().distance(c) > r) continue;
            hit(le, dmg, src);
        }
    }

    // Visual explosion that never breaks blocks and never damages the source.
    private void boom(Location c, double r, double dmg, Entity src) {
        World w = c.getWorld();
        w.spawnParticle(Particle.EXPLOSION, c, 2, 0.4, 0.4, 0.4, 0);
        w.spawnParticle(Particle.FLAME, c, 25, r / 2, 0.3, r / 2, 0.05);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        damageArea(c, r, dmg, src);
    }

    private void knockArea(Location c, double r, double up, Entity src) {
        for (Entity en : c.getWorld().getNearbyEntities(c, r, r, r)) {
            if (!(en instanceof LivingEntity le) || en.equals(src)) continue;
            Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0);
            if (out.lengthSquared() == 0) out = new Vector(0, 0, 0.01);
            le.setVelocity(out.normalize().multiply(0.8).setY(up));
        }
    }

    private LivingEntity nearestEnemy(Location loc, double radius, Player exclude) {
        LivingEntity best = null;
        double bestDist = radius;
        for (Entity en : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(en instanceof LivingEntity le) || en.equals(exclude)) continue;
            if (en instanceof Player) continue;
            UUID own = ownerOf(le);
            if (own != null && exclude != null && (own.equals(exclude.getUniqueId()) || isTrusted(own, exclude.getUniqueId()))) continue;
            double d = le.getLocation().distance(loc);
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    private boolean isClear(Location feet) {
        Block body = feet.getBlock();
        Block head = feet.clone().add(0, 1, 0).getBlock();
        Block below = feet.clone().add(0, -1, 0).getBlock();
        return body.isPassable() && head.isPassable() && below.getType().isSolid();
    }

    private Location toLoc(Vector v, World w) {
        return new Location(w, v.getX(), v.getY(), v.getZ());
    }

    // Eye-level ray hitting blocks, as a Location; falls back to max distance.
    private Location aimPoint(Player p, double dist) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        RayTraceResult r = p.getWorld().rayTraceBlocks(eye, dir, dist, FluidCollisionMode.NEVER, true);
        if (r != null && r.getHitPosition() != null) return toLoc(r.getHitPosition(), p.getWorld());
        return eye.clone().add(dir.multiply(dist));
    }

    // ---------- Right-click handling ----------

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && (e.getHand() == null || e.getHand() == EquipmentSlot.HAND)
                && e.getClickedBlock() != null
                && shrines.containsKey(keyOf(e.getClickedBlock().getLocation()))) {
            e.setCancelled(true);
            e.getPlayer().openInventory(shrines.get(keyOf(e.getClickedBlock().getLocation())).inv);
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        String id = idOf(e.getItem());
        if (id == null) return;

        // Bows must still be drawable with right-click
        if (id.equals("kaboom") || id.equals("shakalaka") || id.equals("arccaster")) return;

        e.setUseItemInHand(Event.Result.DENY);
        if (a == Action.RIGHT_CLICK_BLOCK) e.setUseInteractedBlock(Event.Result.DENY);

        if (gems.containsKey(id)) {
            if (e.getHand() == EquipmentSlot.OFF_HAND) handleGemRight(p, id);
            return;
        }

        World w = p.getWorld();
        Location eyeLoc = p.getEyeLocation();
        Vector look = eyeLoc.getDirection().normalize();

        switch (id) {
            case "thunderstick" -> {
                if (!ready(p, id, 5000)) return;
                Location t = aimPoint(p, 40);
                w.strikeLightningEffect(t);
                damageArea(t, 4, 12, p);
            }
            case "vampirefang" -> {
                if (!ready(p, id, 20000)) return;
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 8));
                w.spawnParticle(Particle.HEART, p.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0);
            }
            case "frostbite" -> {
                if (!ready(p, id, 8000)) return;
                w.spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 60, 4, 1, 4, 0.05);
                w.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.4f);
                for (Entity en : w.getNearbyEntities(p.getLocation(), 7, 3, 7)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                    hit(le, 5, p);
                }
            }
            case "winddash" -> {
                p.setVelocity(look.clone().multiply(2.0));
                guard(p, 100);
                w.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1f);
            }
            case "infernowand" -> {
                if (!ready(p, id, 1000)) return;
                Fireball f = p.launchProjectile(Fireball.class);
                f.setYield(0f); // no block damage
                f.setIsIncendiary(true);
            }
            case "groundbreaker" -> {
                if (!ready(p, id, 4000)) return;
                Location loc = p.getLocation();
                w.spawnParticle(Particle.CLOUD, loc, 40, 2, 0.2, 2, 0.05);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
                damageArea(loc, 6, 18, p);
                knockArea(loc, 6, 0.5, p);
            }
            case "phoenixblade" -> {
                if (!ready(p, id, 6000)) return;
                p.setVelocity(new Vector(0, 1.4, 0));
                p.setFallDistance(0f);
                phoenix.put(p.getUniqueId(), new PhoenixState());
                w.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
            }
            case "grenade" -> {
                if (!ready(p, id, 1000)) return;
                Snowball s = p.launchProjectile(Snowball.class);
                s.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "grenade");
            }
            case "tidal" -> {
                if (!ready(p, id, 4000)) return;
                Snowball orb = p.launchProjectile(Snowball.class);
                orb.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "tidal");
                w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);
            }
            case "reapersigil" -> {
                if (!ready(p, id, 12000)) return;
                int hits = 0;
                for (Entity en : w.getNearbyEntities(p.getLocation(), 6, 3, 6)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    if (isUndead(le)) {
                        le.setHealth(Math.min(le.getMaxHealth(), le.getHealth() + 4));
                        continue;
                    }
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                    hit(le, 7, p);
                    hits++;
                }
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + hits * 2));
                w.spawnParticle(Particle.SMOKE, p.getLocation().add(0, 1, 0), 40, 3, 1, 3, 0.02);
                w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
            }
            case "arccaster" -> {
                if (!ready(p, id, 6000)) return;
                List<LivingEntity> chain = new ArrayList<>();
                LivingEntity cur = nearestEnemy(p.getLocation(), 12, p);
                while (cur != null && chain.size() < 4) {
                    chain.add(cur);
                    cur = nextChain(cur, chain);
                }
                for (int i = 0; i < chain.size(); i++) {
                    final LivingEntity target = chain.get(i);
                    final Location from = (i == 0) ? p.getEyeLocation() : chain.get(i - 1).getLocation().add(0, 1, 0);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (!target.isValid()) return;
                        Vector span = target.getLocation().add(0, 1, 0).toVector().subtract(from.toVector());
                        for (int s = 1; s <= 6; s++) {
                            Location pt = from.clone().add(span.clone().multiply(s / 6.0));
                            w.spawnParticle(Particle.CRIT, pt, 3, 0.1, 0.1, 0.1, 0);
                        }
                        w.strikeLightningEffect(target.getLocation());
                        hit(target, 10, p);
                    }, i * 4L);
                }
            }
            case "gauntlet" -> {
                if (holds.containsKey(p.getUniqueId())) {
                    release(p, true);
                    return;
                }
                RayTraceResult ent = w.rayTraceEntities(eyeLoc, look, 12,
                        en -> en instanceof LivingEntity && !en.equals(p));
                if (ent == null || !(ent.getHitEntity() instanceof LivingEntity le)) {
                    p.sendMessage(ChatColor.RED + "No mob in range.");
                    return;
                }
                grab(p, le);
            }
            case "jetpack" -> {
                if (jetFuel.remove(p.getUniqueId()) != null) {
                    p.sendMessage(ChatColor.YELLOW + "Jetpack off.");
                } else {
                    jetFuel.put(p.getUniqueId(), 12000);
                    p.sendMessage(ChatColor.GREEN + "Jetpack on.");
                }
            }
            case "magnetrod" -> {
                if (!ready(p, id, 3000)) return;
                for (Entity en : w.getNearbyEntities(p.getLocation(), 12, 5, 12)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    Vector pull = p.getLocation().toVector().subtract(le.getLocation().toVector());
                    if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(1.8));
                }
            }
            case "blizzardorb" -> {
                if (!ready(p, id, 12000)) return;
                Location c = p.getLocation();
                w.spawnParticle(Particle.SNOWFLAKE, c, 120, 5, 1.5, 5, 0.05);
                w.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                for (Entity en : w.getNearbyEntities(c, 9, 4, 9)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 9));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 2));
                    hit(le, 4, p);
                }
                particleRing(c.clone().add(0, 0.2, 0), 9, Particle.SNOWFLAKE);
            }
            case "healingcharm" -> {
                if (!ready(p, id, 60000)) return;
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 16));
                cleanse(p);
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 2));
                w.spawnParticle(Particle.HEART, p.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0);
            }
            case "shadowcloak" -> {
                if (!ready(p, id, 60000)) return;
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 400, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1));
                p.sendMessage(ChatColor.DARK_GRAY + "Shadow Cloak: 20 seconds of invisibility.");
            }
            case "golembell" -> {
                if (!ready(p, id, 120000)) return;
                UUID old = golems.remove(p.getUniqueId());
                if (old != null) {
                    Entity oldGolem = Bukkit.getEntity(old);
                    if (oldGolem != null) oldGolem.remove();
                }
                IronGolem g = w.spawn(p.getLocation(), IronGolem.class, ig -> ig.setPlayerCreated(true));
                setOwner(g, p.getUniqueId());
                golems.put(p.getUniqueId(), g.getUniqueId());
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (g.isValid()) g.remove();
                    golems.remove(p.getUniqueId(), g.getUniqueId());
                }, 1800L);
                w.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 2f, 0.8f);
                p.sendMessage(ChatColor.GREEN + "Golem summoned. Hit something to send it in.");
            }
            case "sonichorn" -> {
                if (!ready(p, id, 10000)) return;
                w.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 3f, 1f);
                final Vector fwd = look.clone();
                final Location origin = eyeLoc.clone();
                final Set<UUID> hitIds = new HashSet<>();
                for (int step = 1; step <= 20; step++) {
                    final int s = step;
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        Location front = origin.clone().add(fwd.clone().multiply(s * 1.2));
                        w.spawnParticle(Particle.SONIC_BOOM, front, 1, 0, 0, 0, 0);
                        for (Entity en : w.getNearbyEntities(front, 1.8, 1.8, 1.8)) {
                            if (!(en instanceof LivingEntity hitMob) || en.equals(p) || hitIds.contains(en.getUniqueId())) continue;
                            hitIds.add(en.getUniqueId());
                            hit(hitMob, 150, p);
                            hitMob.setVelocity(fwd.clone().multiply(2.4).setY(0.6));
                        }
                    }, step);
                }
            }
            case "stormheart" -> {
                if (!ready(p, id, 20000)) return;
                List<LivingEntity> targets = new ArrayList<>();
                for (Entity en : w.getNearbyEntities(p.getLocation(), 25, 25, 25)) {
                    if (en instanceof LivingEntity le && !(en instanceof Player)) targets.add(le);
                }
                Collections.shuffle(targets, rnd);
                for (int i = 0; i < Math.min(5, targets.size()); i++) {
                    Location tl = targets.get(i).getLocation();
                    w.strikeLightningEffect(tl);
                    hit(targets.get(i), 20, p);
                }
            }
            case "phasefruit" -> {
                if (!ready(p, id, 3000)) return;
                Vector flat = eyeLoc.getDirection().setY(0);
                if (flat.lengthSquared() == 0) return;
                flat.normalize();
                Location chest = p.getLocation().add(0, 1, 0);
                RayTraceResult r = w.rayTraceBlocks(chest, flat, 12, FluidCollisionMode.NEVER, true);
                double maxD = (r != null && r.getHitPosition() != null)
                        ? chest.toVector().distance(r.getHitPosition()) - 0.7
                        : 12;
                Location dest = null;
                for (double d = maxD; d >= 0.5; d -= 0.5) {
                    Location cand = p.getLocation().add(flat.clone().multiply(d));
                    if (isClear(cand)) {
                        dest = cand;
                        break;
                    }
                }
                if (dest == null) {
                    p.sendMessage(ChatColor.RED + "No safe spot to blink to.");
                    return;
                }
                dest.setYaw(p.getLocation().getYaw());
                dest.setPitch(p.getLocation().getPitch());
                w.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.1);
                p.teleport(dest);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
                guard(p, 40);
                w.spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.1);
            }
            case "voidscythe" -> {
                if (!ready(p, id, 1200)) return;
                w.playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.6f);
                Vector forward = p.getLocation().getDirection().setY(0).normalize();
                // Animated slash: 6 frames sweeping left to right.
                for (int f = 0; f <= 5; f++) {
                    final int frame = f;
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        double angle = Math.toRadians(-60 + frame * 24);
                        Vector dirArc = forward.clone().rotateAroundY(angle);
                        for (double r = 1.5; r <= 4.0; r += 0.5) {
                            Location pt = p.getLocation().add(0, 1, 0).add(dirArc.clone().multiply(r));
                            w.spawnParticle(Particle.DRAGON_BREATH, pt, 2, 0.05, 0.05, 0.05, 0.01);
                        }
                    }, f * 1L);
                }
                // Damage everything in a 120-degree cone in front.
                for (Entity en : w.getNearbyEntities(p.getLocation(), 7, 3, 7)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (to.lengthSquared() == 0) continue;
                    if (to.normalize().dot(forward) > 0.4) hit(le, 20, p);
                }
            }
            case "grapple" -> {
                if (!ready(p, id, 1500)) return;
                RayTraceResult blk = w.rayTraceBlocks(eyeLoc, look, 35, FluidCollisionMode.NEVER, true);
                RayTraceResult ent = w.rayTraceEntities(eyeLoc, look, 35);
                Vector target = null;
                if (ent != null && ent.getHitEntity() != null && !ent.getHitEntity().equals(p) && ent.getHitPosition() != null) {
                    target = ent.getHitPosition();
                } else if (blk != null && blk.getHitPosition() != null) {
                    target = blk.getHitPosition();
                }
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "Nothing to hook.");
                    return;
                }
                Location start = eyeLoc.clone().add(look.clone().multiply(0.5));
                Location end = toLoc(target, w);
                // Animated rope: 8 frames extending from the hand to the target.
                for (int f = 1; f <= 8; f++) {
                    final double frac = f / 8.0;
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        Location pt = start.clone().add(end.clone().subtract(start).multiply(frac));
                        w.spawnParticle(Particle.CRIT, pt, 3, 0.05, 0.05, 0.05, 0);
                    }, f * 1L);
                }
                Vector pull = target.clone().subtract(p.getLocation().toVector());
                double dist = pull.length();
                if (dist < 1.5) return;
                p.setVelocity(pull.normalize().multiply(Math.min(2.6, dist * 0.3)).setY(0.6));
                guard(p, 100);
                w.playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 1f);
            }
            case "starfall" -> {
                if (!ready(p, id, 8000)) return;
                Location center = aimPoint(p, 20);
                for (int i = 0; i < 5; i++) {
                    final Location strike = center.clone().add(rand(-3, 3), 0, rand(-3, 3));
                    long base = i * 6L;
                    // Falling star streak
                    for (int k = 0; k < 12; k++) {
                        final int step = k;
                        Bukkit.getScheduler().runTaskLater(this, () ->
                                w.spawnParticle(Particle.END_ROD, strike.clone().add(0, 12 - step * 1.1, 0), 4, 0.1, 0.1, 0.1, 0.02),
                                base + k);
                    }
                    Bukkit.getScheduler().runTaskLater(this, () -> boom(strike, 3, 12, p), base + 12);
                }
            }
            case "venom" -> {
                if (!ready(p, id, 4000)) return;
                p.setVelocity(look.clone().multiply(1.2).setY(0.2));
                guard(p, 30);
                for (Entity en : w.getNearbyEntities(p.getLocation(), 2.5, 2, 2.5)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    venomTouch(le);
                    hit(le, 6, p);
                }
                w.spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
            }
            case "glacier" -> {
                if (!ready(p, id, 8000)) return;
                PhoenixState call = new PhoenixState();
                call.call = true;
                p.setVelocity(new Vector(0, 1.4, 0));
                p.setFallDistance(0f);
                phoenix.put(p.getUniqueId(), call);
                w.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.8f);
                p.sendMessage(ChatColor.GOLD + "Phoenix Call!");
            }
            case "ghostlantern" -> {
                if (!ready(p, id, 30000)) return;
                final BukkitTask[] holder = new BukkitTask[1];
                final int[] t = {0};
                holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
                    if (!p.isOnline() || t[0] > 200) {
                        holder[0].cancel();
                        return;
                    }
                    for (int i = 0; i < 3; i++) {
                        double ang = t[0] * 0.15 + i * 2 * Math.PI / 3;
                        Location orb = p.getLocation().add(Math.cos(ang) * 1.6, 1.2 + Math.sin(t[0] * 0.1 + i) * 0.3, Math.sin(ang) * 1.6);
                        p.getWorld().spawnParticle(Particle.SOUL, orb, 1, 0, 0, 0, 0);
                    }
                    if (t[0] % 20 == 0) {
                        LivingEntity target = nearestEnemy(p.getLocation(), 12, p);
                        if (target != null) {
                            p.getWorld().strikeLightningEffect(target.getLocation());
                            hit(target, 7, p);
                        }
                    }
                    t[0]++;
                }, 0L, 1L);
            }
            default -> { }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (!"gauntlet".equals(idOf(p.getInventory().getItem(e.getHand())))) return;
        e.setCancelled(true);
        if (holds.containsKey(p.getUniqueId())) {
            release(p, true);
        } else if (e.getRightClicked() instanceof LivingEntity le && !le.equals(p)) {
            grab(p, le);
        }
    }

    // ---------- Ticking effects ----------

    private void tickPhoenix() {
        Iterator<Map.Entry<UUID, PhoenixState>> it = phoenix.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PhoenixState> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null || !p.isOnline()) { it.remove(); continue; }
            PhoenixState s = en.getValue();
            s.ticks++;
            if (s.phase == 0) {
                s.peak = Math.max(s.peak, p.getLocation().getY());
                if (s.ticks >= 8 || p.getVelocity().getY() <= 0) {
                    p.setVelocity(new Vector(0, -2.5, 0));
                    s.phase = 1;
                    s.ticks = 0;
                }
            } else if (s.phase == 1) {
                if (s.ticks > 2 && p.isOnGround()) {
                    smash(p, s);
                    s.phase = 2;
                    s.ticks = 0;
                } else if (s.ticks > 400) {
                    it.remove();
                }
            } else if (s.ticks >= 3) {
                it.remove();
            }
        }
    }

    private void smash(Player p, PhoenixState s) {
        Location loc = p.getLocation();
        World w = p.getWorld();
        double drop = Math.max(0, s.peak - loc.getY());
        double dmg = 14 + Math.min(40, drop * 2);
        w.spawnParticle(Particle.EXPLOSION, loc, 1);
        w.spawnParticle(Particle.CLOUD, loc, 60, 3, 0.2, 3, 0.1);
        if (s.call) {
            w.spawnParticle(Particle.FLAME, loc, 80, 3, 0.3, 3, 0.1);
            w.spawnParticle(Particle.SNOWFLAKE, loc, 80, 3, 0.3, 3, 0.1);
        }
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        damageArea(loc, 6, dmg, p);
        knockArea(loc, 6, 0.6, p);
        p.setFallDistance(0f);
        p.sendMessage(ChatColor.GOLD + "Impact: " + String.format(Locale.ROOT, "%.0f", dmg)
                + " damage from " + String.format(Locale.ROOT, "%.1f", drop) + " blocks up.");
    }

    private void tickJetpack() {
        Iterator<Map.Entry<UUID, Integer>> it = jetFuel.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            int fuel = en.getValue();
            if (p == null || !p.isOnline()) { it.remove(); continue; }
            if (fuel <= 0) {
                p.sendMessage(ChatColor.RED + "Jetpack out of fuel.");
                it.remove();
                continue;
            }
            Vector v = p.getVelocity();
            double vy = v.getY() < 0.3 ? 0.35 : v.getY();
            p.setVelocity(new Vector(v.getX(), vy, v.getZ()));
            p.setFallDistance(0f);
            guard(p, 10);
            p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 3, 0.1, 0, 0.1, 0.01);
            en.setValue(fuel - 1);
        }
    }

    private void grab(Player p, LivingEntity mob) {
        holds.put(p.getUniqueId(), mob.getUniqueId());
        if (mob instanceof Player target) guard(target, 60);
        p.sendMessage(ChatColor.GREEN + "Grabbed " + (mob instanceof Player pl ? pl.getName() : "mob")
                + ". Right-click again to hurl it.");
    }

    private void release(Player p, boolean hurl) {
        UUID mobId = holds.remove(p.getUniqueId());
        if (mobId == null) return;
        Entity mob = Bukkit.getEntity(mobId);
        if (hurl && mob != null) {
            Vector look = p.getEyeLocation().getDirection().normalize();
            if (mob instanceof Player target) guard(target, 60);
            mob.setVelocity(look.multiply(2.2).setY(0.3));
            p.sendMessage(ChatColor.YELLOW + "Hurled!");
        } else {
            p.sendMessage(ChatColor.YELLOW + "Released.");
        }
    }

    // Holds the grabbed mob in front of the player by teleporting it each tick.
    private void tickGem() {
        Iterator<Map.Entry<UUID, UUID>> it = holds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            Entity mob = Bukkit.getEntity(en.getValue());
            if (p == null || !p.isOnline() || mob == null || !mob.isValid()) {
                it.remove();
                continue;
            }
            if (!holdsItem(p, "gauntlet")
                    || mob.getWorld() != p.getWorld()
                    || mob.getLocation().distance(p.getLocation()) > 40) {
                it.remove();
                p.sendMessage(ChatColor.YELLOW + "Released.");
                continue;
            }
            Location eye = p.getEyeLocation();
            Location hold = eye.clone().add(eye.getDirection().normalize().multiply(3.0));
            if (mob instanceof Player target) {
                // Players can't be teleported every tick without stuttering, so steer them.
                Vector toHold = hold.clone().subtract(0, 1.6, 0).toVector().subtract(target.getLocation().toVector());
                if (toHold.length() > 1.5) toHold.normalize().multiply(1.5);
                target.setVelocity(toHold.multiply(0.6));
                target.setFallDistance(0f);
                guard(target, 10);
            } else {
                hold.setYaw(p.getLocation().getYaw());
                hold.setPitch(0f);
                mob.teleport(hold);
                mob.setVelocity(new Vector(0, 0, 0));
                mob.setFallDistance(0f);
            }
        }
    }

    private void tickFallGuard() {
        Iterator<Map.Entry<UUID, Integer>> it = fallGuard.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null || !p.isOnline()) { it.remove(); continue; }
            if (!p.isOnGround()) {
                p.setFallDistance(0f);
                continue;
            }
            int left = en.getValue() - 1;
            if (left <= 0) it.remove();
            else en.setValue(left);
        }
    }

    // ---------- Damage / projectile handling ----------

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player attacker
                && e.getEntity() instanceof LivingEntity victim
                && !victim.equals(attacker)) {
            UUID gid = golems.get(attacker.getUniqueId());
            if (gid != null) {
                Entity g = Bukkit.getEntity(gid);
                if (g instanceof IronGolem ig && ig.isValid() && !victim.equals(ig)) ig.setTarget(victim);
            }
        }
        if (!(e.getDamager() instanceof Player p)) return;
        String id = idOf(p.getInventory().getItemInMainHand());
        if (id == null) return;
        switch (id) {
            case "frostbite" -> {
                if (e.getEntity() instanceof LivingEntity le) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    e.setDamage(e.getDamage() + 4);
                }
            }
            case "thunderstick" -> {
                if (e.getEntity() instanceof LivingEntity le) {
                    p.getWorld().strikeLightningEffect(le.getLocation());
                    e.setDamage(e.getDamage() + 6);
                }
            }
            case "vampirefang" -> {
                double heal = e.getDamage() * 0.4;
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + heal));
            }
            case "venom" -> {
                if (e.getEntity() instanceof LivingEntity le) {
                    venomTouch(le);
                    e.setDamage(e.getDamage() + 3);
                }
            }
            default -> { }
        }
    }

    @EventHandler
    public void onWindFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (e.getEntity() instanceof Player p && holdsItem(p, "wind")) {
            e.setCancelled(true);
            return;
        }
        if (e.getEntity() instanceof LivingEntity le) {
            Long until = launchedUntil.get(le.getUniqueId());
            if (until == null) return;
            if (System.currentTimeMillis() < until) {
                e.setDamage(Math.min(e.getDamage(), 8));
            } else {
                launchedUntil.remove(le.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player p)) return;
        if (phoenix.containsKey(p.getUniqueId()) || fallGuard.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player shooterP)) return;
        String id = idOf(e.getBow());
        if (id != null && (id.equals("kaboom") || id.equals("shakalaka") || id.equals("arccaster"))) {
            e.getProjectile().getPersistentDataContainer().set(projKey, PersistentDataType.STRING, id);
            return;
        }
        if (holdsItem(shooterP, "blizzard") && rnd.nextDouble() < 0.30) {
            e.getProjectile().getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "frozen");
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile pr = e.getEntity();
        String tag = pr.getPersistentDataContainer().get(projKey, PersistentDataType.STRING);
        if (tag == null) return;

        Location loc = pr.getLocation();
        World w = loc.getWorld();
        ProjectileSource src = pr.getShooter();
        Entity shooter = src instanceof Entity en ? en : null;

        switch (tag) {
            case "kaboom" -> boom(loc, 4, 12, shooter);
            case "grenade" -> boom(loc, 3.5, 12, shooter);
            case "thunderbolt" -> {
                if (shooter instanceof Player tp) {
                    if (e.getHitEntity() instanceof LivingEntity stunned && !isFriendlyTo(tp, stunned)) {
                        stun(tp, stunned);
                    } else {
                        w.strikeLightningEffect(loc);
                        damageArea(loc, 2, 6, tp);
                    }
                }
            }
            case "meteor" -> boom(loc, 3, 10, shooter);
            case "blazeball" -> boom(loc, 3.5, 14, shooter);
            case "charge1", "charge2", "charge3" -> {
                int lvl = tag.charAt(6) - '0';
                boom(loc, 1.5 + lvl, 4 + lvl * 4, shooter);
            }
            case "gust" -> {
                damageArea(loc, 3, 5, shooter);
                knockArea(loc, 3, 0.7, shooter);
                particleRing(loc.clone().add(0, 0.5, 0), 2.0, Particle.CLOUD);
            }
            case "arccaster" -> {
                if (shooter instanceof Player ap && e.getHitEntity() instanceof LivingEntity arcHit && !isFriendlyTo(ap, arcHit)) {
                    if (rnd.nextBoolean()) {
                        stun(ap, arcHit);
                    } else {
                        arcHit.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 9));
                        ap.sendMessage(ChatColor.AQUA + "Frozen!");
                    }
                }
            }
            case "frozen" -> {
                if (e.getHitEntity() instanceof LivingEntity frozenMob && !isFriendlyTo(shooter, frozenMob)) {
                    frozenMob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 9));
                }
            }
            case "snow" -> {
                if (shooter instanceof Player sp && e.getHitEntity() instanceof LivingEntity snowed
                        && !isFriendlyTo(sp, snowed)) {
                    hit(snowed, 7, sp);
                    Vector kb = pr.getVelocity();
                    if (kb.lengthSquared() > 0) snowed.setVelocity(kb.normalize().multiply(1.2).setY(0.4));
                    String key = sp.getUniqueId() + ":" + snowed.getUniqueId();
                    int count = snowHits.merge(key, 1, Integer::sum);
                    if (count >= 3) {
                        snowed.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 140, 9));
                        snowHits.remove(key);
                        sp.sendMessage(ChatColor.AQUA + "Frozen!");
                    }
                    if (isActive(subzeroUntil, sp)) boom(loc, 2.5, 8, sp);
                }
            }
            case "iceshard" -> {
                damageArea(loc, 2, 6, shooter);
                for (Entity en : w.getNearbyEntities(loc, 2, 2, 2)) {
                    if (en instanceof LivingEntity hitMob && !en.equals(shooter)) {
                        hitMob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    }
                }
                w.spawnParticle(Particle.SNOWFLAKE, loc, 20, 0.5, 0.5, 0.5, 0.05);
            }
            case "tidal" -> {
                boom(loc, 3, 12, shooter);
                w.spawnParticle(Particle.SPLASH, loc, 60, 1.5, 0.5, 1.5, 0.1);
                knockArea(loc, 3, 0.8, shooter);
            }
            case "shakalaka" -> {
                for (int i = 0; i < 3; i++) {
                    Location spot = loc.clone().add(rand(-2, 2), 0, rand(-2, 2));
                    shakalakaRoll(spot, shooter);
                }
            }
            default -> { }
        }
        pr.remove();
    }

    private void shakalakaRoll(Location loc, Entity shooter) {
        World w = loc.getWorld();
        switch (rnd.nextInt(3)) {
            case 0 -> {
                w.strikeLightningEffect(loc);
                damageArea(loc, 3, 9, shooter);
            }
            case 1 -> boom(loc, 3.5, 9, shooter);
            default -> {
                Fireball f = w.spawn(loc.clone().add(0, 8, 0), Fireball.class);
                f.setDirection(new Vector(0, -1, 0));
                f.setYield(0f);
                f.setIsIncendiary(true);
            }
        }
    }

    private boolean holdsItem(Player p, String id) {
        if (gems.containsKey(id)) return id.equals(idOf(p.getInventory().getItemInOffHand()));
        return id.equals(idOf(p.getInventory().getItemInMainHand()))
                || id.equals(idOf(p.getInventory().getItemInOffHand()));
    }

    private void ring(Location c, double radius) {
        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg);
            c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    c.clone().add(Math.cos(rad) * radius, 0.2, Math.sin(rad) * radius), 1, 0, 0, 0, 0);
        }
    }

    // ===================== GEMS =====================

    private void registerGems() {
        gemDef("blizzard", "Blizzard Gem", Material.AMETHYST_SHARD, "#80DEEA",
                "Right-click: Frost Lift (freezes and lifts nearby enemies while arrows rain in). Shift+right-click: Ice Barrier. F: Snow Storm (15s). Shift+F: Subzero (ultimate). Passives: arrows can freeze, immune to snow, lava and magma.");
        gemDef("nightmare", "Nightmare Gem", Material.AMETHYST_SHARD, "#263238",
                "Right-click: Night Terror. Shift+right-click: Nightmare Punishment (5s). F: Abyssal Sight. Shift+F: Giant Creaking (ultimate). Passives: tame bats and creakings with Echo Shards, Echo Location.");
        gemDef("thunder", "Thunder Master Gem", Material.AMETHYST_SHARD, "#FFEB3B", "Right-click: Thunder Strike (lightning at your aim, 12 damage and a stun). Shift+right-click: Chain Lightning (up to 5 foes). F: Overcharge (next 3 hits +10 damage). Shift+F: Zeus (ultimate, 20 seconds of lightning storm). Passives: 20% lightning on hit, immune to lightning, slowness and weakness.");
        gemDef("wind", "Wind Master Gem", Material.AMETHYST_SHARD, "#B2EBF2", "Right-click: Gust (pushes and damages nearby foes). Shift+right-click: Updraft (launch yourself up, no fall damage). F: Tornado (orbits foes within 12 blocks). Sprint+F: Berserk Breeze (ultimate, 30 seconds of gusts on every right-click). Passives: no fall damage, stronger bow, 5% chance to launch players on hit.");
        gemDef("inferno", "Inferno Gem", Material.AMETHYST_SHARD, "#FF6D00",
                "Right-click: Meteor Shower. Shift+right-click: Cozy Campfire. F: Chargeable Fireball (press F to start, F again to release). Shift+F: Evaporation (toggle). Sprint+F: Blaze (ultimate). Passives: fire immunity, fire damage on swords.");
        gemDef("atomic", "Atomic Gem", Material.AMETHYST_SHARD, "#76FF03",
                "Right-click: Atomic Blast (shockwave, heavy damage and knockback). Shift+right-click: Radiation Cloud (15s wither and nausea nearby). F: Fallout Shield (10s, half damage, attackers blinded). Shift+F: Meltdown (ultimate, 20s glow, radiation damage nearby). Passives: immune to wither and poison, slow regeneration.");
        gemDef("undead", "Undead Legion Gem", Material.AMETHYST_SHARD, "#A5D6A7",
                "Right-click: Wither Legion. Shift+right-click: Fallen Recall. F: Wither Touch. Shift+F: Giant Wither Skeleton (ultimate). Passives: heal and extra damage vs undead.");
    }

    private void gemDef(String id, String name, Material mat, String hex, String lore) {
        gems.put(id, new ItemDef(id, name, mat, hex, lore));
    }

    private boolean isActive(Map<UUID, Long> map, Player p) {
        Long until = map.get(p.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private String gemOf(Player p) {
        String off = idOf(p.getInventory().getItemInOffHand());
        if (off != null && gems.containsKey(off)) return off;
        return null;
    }

    private boolean holdsMaterial(Player p, Material m) {
        return p.getInventory().getItemInMainHand().getType() == m
                || p.getInventory().getItemInOffHand().getType() == m;
    }

    private void setOwner(Entity e, UUID owner) {
        e.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
    }

    private UUID ownerOf(Entity e) {
        if (e == null) return null;
        if (e instanceof Player p) return p.getUniqueId();
        if (e instanceof Projectile pr && pr.getShooter() instanceof Player sp) return sp.getUniqueId();
        String s = e.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return s == null ? null : UUID.fromString(s);
    }

    private boolean isTrusted(UUID owner, UUID other) {
        Set<UUID> set = trusts.get(owner);
        return set != null && set.contains(other);
    }

    // True when the source (player, mob or projectile) belongs to someone who owns or trusts the victim.
    private boolean isFriendlyTo(Entity source, Entity victim) {
        UUID owner = ownerOf(source);
        if (owner == null) return false;
        if (!(victim instanceof Player vp)) return false;
        return vp.getUniqueId().equals(owner) || isTrusted(owner, vp.getUniqueId());
    }

    private boolean isUndead(LivingEntity t) {
        return t instanceof AbstractSkeleton || t instanceof Zombie || t instanceof Phantom;
    }

    private void damageArmor(Player p, int amount) {
        ItemStack[] armor = p.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null) continue;
            ItemMeta m = piece.getItemMeta();
            if (m instanceof Damageable dmg) {
                dmg.setDamage(dmg.getDamage() + amount);
                piece.setItemMeta(dmg);
            }
        }
        p.getInventory().setArmorContents(armor);
    }

    private void applyKnownEffects(LivingEntity target, PotionEffect... effects) {
        for (PotionEffect fx : effects) target.addPotionEffect(fx);
    }

    // ----- Right-click (ability 1 = right-click, ability 2 = shift + right-click) -----

    private void handleGemRight(Player p, String gem) {
        countAbility(p);
        boolean shift = p.isSneaking();
        switch (gem) {
            case "blizzard" -> {
                if (shift) blizzardBarrier(p);
                else if (isActive(snowUntil, p)) fireSnow(p);
                else if (isActive(subzeroUntil, p)) tridentRain(p);
                else blizzardLift(p);
            }
            case "thunder" -> {
                if (shift) chainLightning(p);
                else thunderStrike(p);
            }
            case "nightmare" -> {
                if (shift) nightmarePunish(p);
                else nightTerror(p);
            }
            case "undead" -> {
                if (shift) fallenRecall(p);
                else witherLegion(p);
            }
            case "atomic" -> {
                if (shift) radiationCloud(p);
                else atomicBlast(p);
            }
            case "wind" -> {
                if (shift) updraft(p);
                else if (isActive(windBerserkUntil, p)) fireGust(p);
                else gust(p);
            }
            case "inferno" -> {
                if (shift) campfire(p);
                else if (isActive(blazeUntil, p)) fireBlazeBall(p);
                else meteorShower(p);
            }
            default -> { }
        }
    }

    // ----- F key (ability 3 = F, ultimate = shift + F) -----

    @EventHandler
    public void onGemF(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        String gem = gemOf(p);
        if (gem == null) return;
        e.setCancelled(true);
        boolean shift = p.isSneaking();
        boolean sprint = p.isSprinting();
        boolean ult = gem.equals("wind") ? sprint : shift;
        if (ult) {
            if (!ultGate(p)) return;
            switch (gem) {
                case "blizzard" -> subzero(p);
                case "thunder" -> zeus(p);
                case "nightmare" -> giantCreaking(p);
                case "undead" -> giantWither(p);
                case "wind" -> windBerserk(p);
                case "inferno" -> blazeMode(p);
                case "atomic" -> meltdown(p);
                default -> { }
            }
            return;
        }
        countAbility(p);
        switch (gem) {
            case "blizzard" -> snowStorm(p);
            case "thunder" -> surge(p);
            case "nightmare" -> abyssalSight(p);
            case "undead" -> witherTouch(p);
            case "wind" -> tornado(p);
            case "inferno" -> {
                if (sprint) toggleEvaporation(p);
                else chargeFireball(p);
            }
            case "atomic" -> falloutShield(p);
            default -> { }
        }
    }

    // ----- Blizzard -----

    private void blizzardLift(Player p) {
        World w = p.getWorld();
        List<Player> victims = new ArrayList<>();
        for (Entity en : w.getNearbyEntities(p.getLocation(), 8, 4, 8)) {
            if (!(en instanceof Player v)) continue;
            if (isFriendlyTo(p, v)) continue;
            victims.add(v);
        }
        if (victims.isEmpty()) {
            p.sendMessage(ChatColor.RED + "No enemy players nearby.");
            return;
        }
        if (!ready(p, "g_lift", 120000)) return;
        Map<UUID, Double> liftY = new HashMap<>();
        for (Player v : victims) liftY.put(v.getUniqueId(), v.getLocation().getY() + 4);
        w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.8f);

        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            runs[0]++;
            if (runs[0] > 60) {
                holder[0].cancel();
                return;
            }
            for (Player v : victims) {
                if (!v.isOnline()) continue;
                double dy = liftY.get(v.getUniqueId()) - v.getLocation().getY();
                v.setVelocity(new Vector(0, Math.max(-0.5, Math.min(0.5, dy * 0.5)), 0));
                v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 9));
                v.setFallDistance(0f);
                guard(v, 20);
            }
        }, 0L, 1L);

        for (int t = 0; t < 15; t++) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player v : victims) {
                    if (!v.isOnline()) continue;
                    for (int k = 0; k < 2; k++) {
                        double ang = rand(0, Math.PI * 2);
                        Location from = v.getLocation().add(Math.cos(ang) * 8, rand(3, 7), Math.sin(ang) * 8);
                        Vector dir = v.getLocation().add(0, 1, 0).toVector().subtract(from.toVector()).normalize();
                        Arrow ar = w.spawnArrow(from, dir, 1.6f, 0f);
                        ar.setShooter(p);
                        ar.setDamage(1.5);
                    }
                }
            }, t * 4L);
        }
        p.sendMessage(ChatColor.AQUA + "Frost Lift! " + victims.size() + " target(s) frozen in the air.");
    }

    private static final class BarrierState {
        final UUID owner;
        final Map<Block, Material> placed = new HashMap<>();
        long expiry;

        BarrierState(UUID owner, long expiry) {
            this.owner = owner;
            this.expiry = expiry;
        }
    }

    private void blizzardBarrier(Player p) {
        if (!ready(p, "g_barrier", 60000)) return;
        BarrierState bs = new BarrierState(p.getUniqueId(), System.currentTimeMillis() + 15000);
        barriers.add(bs);
        placeRing(bs, p.getLocation());
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.5f, 0.7f);
        p.sendMessage(ChatColor.AQUA + "Ice barrier raised for 15 seconds. It follows you.");
    }

    private void placeRing(BarrierState bs, Location center) {
        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg);
            for (int h = 0; h <= 2; h++) {
                Block b = center.clone().add(Math.cos(rad) * 2.5, h, Math.sin(rad) * 2.5).getBlock();
                if (b.getType().isAir() && !bs.placed.containsKey(b)) {
                    bs.placed.put(b, Material.AIR);
                    b.setType(Material.ICE);
                }
            }
        }
    }

    private void restoreRing(BarrierState bs) {
        for (Map.Entry<Block, Material> en : bs.placed.entrySet()) en.getKey().setType(en.getValue());
        bs.placed.clear();
    }

    private void tickBarriers() {
        long now = System.currentTimeMillis();
        Iterator<BarrierState> it = barriers.iterator();
        while (it.hasNext()) {
            BarrierState bs = it.next();
            Player owner = Bukkit.getPlayer(bs.owner);
            restoreRing(bs);
            if (now >= bs.expiry || owner == null || !owner.isOnline()) {
                it.remove();
                continue;
            }
            placeRing(bs, owner.getLocation());
            for (Player v : Bukkit.getOnlinePlayers()) {
                if (v.getUniqueId().equals(bs.owner) || isTrusted(bs.owner, v.getUniqueId())) continue;
                if (v.getWorld() != owner.getWorld()) continue;
                double dx = v.getLocation().getX() - owner.getLocation().getX();
                double dz = v.getLocation().getZ() - owner.getLocation().getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 1.5 || dist > 3.5) continue;
                Vector out = new Vector(dx, 0, dz);
                v.setVelocity(out.normalize().multiply(1.6).setY(0.35));
                v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
        }
    }

    private void snowStorm(Player p) {
        if (!ready(p, "g_snow", 90000)) return;
        snowUntil.put(p.getUniqueId(), System.currentTimeMillis() + 15000);
        p.sendMessage(ChatColor.AQUA + "Snow Storm active for 15 seconds. Right-click fires snowballs.");
    }

    private void fireSnow(Player p) {
        Vector look = p.getEyeLocation().getDirection().normalize();
        for (int i = -1; i <= 1; i++) {
            Vector v = look.clone().rotateAroundY(Math.toRadians(i * 6)).multiply(1.4);
            Snowball s = p.launchProjectile(Snowball.class, v);
            s.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "snow");
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.8f);
    }

    private void subzero(Player p) {
        if (!ready(p, "g_subzero", 180000)) return;
        subzeroUntil.put(p.getUniqueId(), System.currentTimeMillis() + 60000);
        boolean prev = p.getAllowFlight();
        p.setAllowFlight(true);
        p.setFlying(true);
        p.sendMessage(ChatColor.AQUA + "Subzero: flying for 60 seconds. Right-click to rain tridents.");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            subzeroUntil.remove(p.getUniqueId());
            if (p.isOnline()) {
                p.setFlying(false);
                p.setAllowFlight(prev);
            }
        }, 1200L);
    }

    private void tridentRain(Player p) {
        if (!ready(p, "g_rain", 3000)) return;
        Location aim = aimPoint(p, 40);
        World w = p.getWorld();
        for (int i = 0; i < 10; i++) {
            Location from = aim.clone().add(rand(-3, 3), 12, rand(-3, 3));
            Trident t = w.spawn(from, Trident.class);
            t.setShooter(p);
            t.setVelocity(new Vector(0, -1.8, 0));
        }
        w.playSound(aim, Sound.ITEM_TRIDENT_THUNDER, 2f, 1f);
    }

    private void tickBlizzardPassive() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!holdsItem(p, "blizzard")) continue;
            Block below = p.getLocation().subtract(0, 1, 0).getBlock();
            if (below.getType() == Material.LAVA) below.setType(Material.OBSIDIAN);
        }
    }

    @EventHandler
    public void onBlizzardImmunity(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!holdsItem(p, "blizzard")) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.FREEZE
                || c == EntityDamageEvent.DamageCause.HOT_FLOOR
                || c == EntityDamageEvent.DamageCause.LAVA) {
            e.setCancelled(true);
        }
    }

    // ----- Nightmare -----

    private void nightTerror(Player p) {
        if (!ready(p, "g_terror", 60000)) return;
        int count = 0;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 50, 50, 50)) {
            if (!(en instanceof Player v)) continue;
            if (isFriendlyTo(p, v)) continue;
            v.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 400, 0));
            v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 400, 1));
            v.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 400, 0));
            v.playSound(v.getLocation(), Sound.ENTITY_WARDEN_AMBIENT, 2f, 0.5f);
            v.sendMessage(ChatColor.DARK_PURPLE + "Something is watching you...");
            count++;
        }
        p.sendMessage(ChatColor.DARK_PURPLE + "Night Terror cast on " + count + " player(s).");
    }

    private void nightmarePunish(Player p) {
        if (!ready(p, "g_punish", 60000)) return;
        nightmareUntil.put(p.getUniqueId(), System.currentTimeMillis() + 5000);
        p.sendTitle(ChatColor.DARK_RED + "NIGHTMARE", ChatColor.RED + "Punishment active for 5 seconds", 5, 80, 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_ROAR, 2f, 0.6f);
    }

    private void abyssalSight(Player p) {
        if (!ready(p, "g_abyss", 90000)) return;
        int revealed = 0;
        for (Player v : Bukkit.getOnlinePlayers()) {
            if (v.equals(p) || isFriendlyTo(p, v)) continue;
            int surface = v.getWorld().getHighestBlockYAt(v.getLocation());
            if (v.getLocation().getBlockY() < surface - 4) {
                v.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
                revealed++;
            }
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0));
        p.sendMessage(ChatColor.DARK_PURPLE + "Abyssal Sight: " + revealed + " underground player(s) revealed.");
    }

    private void giantCreaking(Player p) {
        EntityType type = creakingType();
        if (type == null) {
            p.sendMessage(ChatColor.RED + "Creaking is not available on this server version.");
            return;
        }
        if (!ready(p, "g_creak", 180000)) return;
        Entity creak = spawnCreaking(p.getLocation().add(2, 0, 2), type, p.getUniqueId(), 0.8, "nmult");
        p.sendMessage(ChatColor.DARK_PURPLE + "A small creaking follows you. It rots in 120 seconds.");
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            runs[0]++;
            if (!creak.isValid()) {
                holder[0].cancel();
                return;
            }
            if (runs[0] > 24) {
                creak.remove();
                p.sendMessage(ChatColor.DARK_GRAY + "Your creaking has rotted away.");
                holder[0].cancel();
                return;
            }
            creak.getWorld().playSound(creak.getLocation(), Sound.ENTITY_WARDEN_AMBIENT, 1.5f, 0.6f);
        }, 0L, 100L);
    }

    private void tickEcho() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!holdsItem(p, "nightmare")) continue;
            for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 5, 5, 5)) {
                if (!(en instanceof Player hidden)) continue;
                if (hidden.equals(p) || isFriendlyTo(p, hidden)) continue;
                if (!hidden.isSneaking() && !hidden.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
                double d = hidden.getLocation().distance(p.getLocation());
                p.sendMessage(ChatColor.DARK_PURPLE + "[Echo] " + ChatColor.WHITE + "Hidden enemy "
                        + hidden.getName() + " is " + String.format(Locale.ROOT, "%.1f", d) + " blocks away.");
            }
        }
    }

    @EventHandler
    public void onGemTame(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack main = p.getInventory().getItemInMainHand();
        if (main.getType() != Material.ECHO_SHARD || !holdsItem(p, "nightmare")) return;
        Entity target = e.getRightClicked();
        boolean tameable = target instanceof Bat || target.getType().name().equals("CREAKING");
        if (!tameable) return;
        e.setCancelled(true);
        if (ownerOf(target) != null) {
            p.sendMessage(ChatColor.RED + "It's already tamed.");
            return;
        }
        if (main.getAmount() <= 1) p.getInventory().setItemInMainHand(null);
        else main.setAmount(main.getAmount() - 1);
        setOwner(target, p.getUniqueId());
        addFollower(target);
        p.sendMessage(ChatColor.DARK_PURPLE + "Fed an Echo Shard. It will follow you now.");
    }

    // ----- Undead Legion -----

    private void witherLegion(Player p) {
        if (!ready(p, "g_legion", 60000)) return;
        World w = p.getWorld();
        int n = 4 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) {
            Location loc = p.getLocation().add(rand(-3, 3), 0, rand(-3, 3));
            WitherSkeleton ws = w.spawn(loc, WitherSkeleton.class);
            setOwner(ws, p.getUniqueId());
            addFollower(ws);
            ws.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1));
            ws.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 1));
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (ws.isValid()) ws.remove();
            }, 1200L);
        }
        p.sendMessage(ChatColor.DARK_GREEN + "" + n + " buffed wither skeletons summoned for 60 seconds.");
    }

    private void fallenRecall(Player p) {
        Deque<EntityType> kills = lastKills.get(p.getUniqueId());
        if (kills == null || kills.isEmpty()) {
            p.sendMessage(ChatColor.RED + "You have no recent kills to recall.");
            return;
        }
        if (!ready(p, "g_recall", 60000)) return;
        World w = p.getWorld();
        int summoned = 0;
        for (EntityType type : kills) {
            if (!type.isAlive() || NO_SUMMON.contains(type)) continue;
            try {
                Entity m = w.spawnEntity(p.getLocation().add(rand(-3, 3), 0, rand(-3, 3)), type);
                setOwner(m, p.getUniqueId());
                addFollower(m);
                summoned++;
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (m.isValid()) m.remove();
                }, 1200L);
            } catch (IllegalArgumentException ignored) {
                // type can't be spawned on this server
            }
        }
        p.sendMessage(ChatColor.DARK_GREEN + "Recalled " + summoned + " fallen mob(s) for 60 seconds.");
    }

    private void witherTouch(Player p) {
        if (!ready(p, "g_touch", 40000)) return;
        witherTouchUntil.put(p.getUniqueId(), System.currentTimeMillis() + 20000);
        p.sendMessage(ChatColor.DARK_GREEN + "Wither Touch active for 20 seconds.");
    }

    private void giantWither(Player p) {
        if (!ready(p, "g_giant", 180000)) return;
        World w = p.getWorld();
        WitherSkeleton big = w.spawn(p.getLocation().add(2, 0, 2), WitherSkeleton.class);
        setOwner(big, p.getUniqueId());
        big.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, "undeadult");
        big.setCustomName(ChatColor.BLACK + "" + ChatColor.BOLD + "Hollow Colossus");
        big.setCustomNameVisible(true);
        AttributeInstance sc = big.getAttribute(Attribute.GENERIC_SCALE);
        if (sc != null) sc.setBaseValue(0.7);
        AttributeInstance mh = big.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (mh != null) {
            mh.setBaseValue(400);
            big.setHealth(400);
        }
        AttributeInstance ad = big.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (ad != null) {
            ad.setBaseValue(16);
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (big.isValid()) big.remove();
        }, 2400L);
        p.sendMessage(ChatColor.DARK_GREEN + "The Hollow Colossus rises for 120 seconds.");
    }

    @EventHandler
    public void onKillTrack(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player || NO_SUMMON.contains(dead.getType())) return;
        Player killer = dead.getKiller();
        if (killer == null) return;
        Deque<EntityType> q = lastKills.computeIfAbsent(killer.getUniqueId(), k -> new ArrayDeque<>());
        q.addFirst(dead.getType());
        while (q.size() > 3) q.removeLast();
    }

    // ----- Gem damage rules: trust, passives, and effects -----

    @EventHandler
    public void onGemDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity victim = e.getEntity();

        // Trust: your abilities, mobs, projectiles and lightning never hurt you or your trusted players.
        if (isFriendlyTo(damager, victim)) {
            e.setCancelled(true);
            return;
        }
        if ("nmult".equals(victim.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) {
            EntityType ct = creakingType();
            UUID own = ownerOf(victim);
            Location at = victim.getLocation();
            if (ct != null && own != null) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    for (int i = 0; i < 2; i++) {
                        spawnCreaking(at.clone().add(rand(-1.5, 1.5), 0, rand(-1.5, 1.5)), ct, own, 0.4, "nmchild");
                    }
                }, 1L);
            }
        }
        if (!(victim instanceof LivingEntity target)) return;

        if (damager instanceof Player attacker) {
            if (holdsItem(attacker, "undead") && isUndead(target)) {
                e.setDamage(e.getDamage() * 1.5);
                attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + 2));
            }
            if (isActive(witherTouchUntil, attacker)) {
                applyKnownEffects(target,
                        new PotionEffect(PotionEffectType.WITHER, 100 + rnd.nextInt(61), 0),
                        new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
            }
        }

        // Giant wither skeleton hits
        String tag = damager.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING);
        if ("undeadult".equals(tag)) {
            applyKnownEffects(target,
                    new PotionEffect(PotionEffectType.SLOWNESS, 100, 1),
                    new PotionEffect(PotionEffectType.POISON, 100, 0),
                    new PotionEffect(PotionEffectType.NAUSEA, 100, 0),
                    new PotionEffect(PotionEffectType.WITHER, 100, 0));
        }

        // Nightmare Punishment: anyone who hits a guarded user gets punished.
        if (victim instanceof Player guarded && isActive(nightmareUntil, guarded)) {
            LivingEntity attackerLiving = null;
            if (damager instanceof LivingEntity la) attackerLiving = la;
            else if (damager instanceof Projectile pj && pj.getShooter() instanceof LivingEntity sl) attackerLiving = sl;
            if (attackerLiving != null && !attackerLiving.equals(guarded)) {
                applyKnownEffects(attackerLiving,
                        new PotionEffect(PotionEffectType.DARKNESS, 100, 0),
                        new PotionEffect(PotionEffectType.BLINDNESS, 100, 0),
                        new PotionEffect(PotionEffectType.NAUSEA, 100, 0),
                        new PotionEffect(PotionEffectType.WITHER, 100, 0),
                        new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                if (attackerLiving instanceof Player ap) damageArmor(ap, 30);
            }
        }
    }

    @EventHandler
    public void onMinionTarget(EntityTargetLivingEntityEvent e) {
        UUID owner = ownerOf(e.getEntity());
        if (owner == null) return;
        if (!(e.getTarget() instanceof Player targetPlayer)) return;
        if (targetPlayer.getUniqueId().equals(owner) || isTrusted(owner, targetPlayer.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    // ----- Gem timers -----

    private void tickGems() {
        gemTick++;
        tickBlizzardPassive();
        if (gemTick % 5 == 0) tickBarriers();
        if (gemTick % 40 == 0) tickEcho();
        if (gemTick % 10 == 0) tickFollowers();
        if (gemTick % 40 == 0) tickAtomicRegen();
        if (gemTick % 5 == 0) tickThunderAura();
        if (gemTick % 5 == 0) tickGemAuras();
    }

    // ===================== ATOMIC =====================

    private void atomicBlast(Player p) {
        if (!ready(p, "g_atomic", 8000)) return;
        Location c = p.getLocation();
        damageArea(c, 6, 12, p);
        knockArea(c, 6, 0.6, p);
        p.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1.4f);
        for (int r = 2; r <= 6; r += 2) {
            final double radius = r;
            Bukkit.getScheduler().runTaskLater(this, () -> particleRing(c.clone().add(0, 0.3, 0), radius, Particle.HAPPY_VILLAGER), r / 2L);
        }
        p.sendMessage(ChatColor.GREEN + "Atomic Blast!");
    }

    private void radiationCloud(Player p) {
        if (!ready(p, "g_cloud", 30000)) return;
        p.sendMessage(ChatColor.GREEN + "Radiation Cloud for 15 seconds.");
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            runs[0]++;
            if (runs[0] > 15 || !p.isOnline()) {
                holder[0].cancel();
                return;
            }
            Location c = p.getLocation();
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c.clone().add(0, 1, 0), 25, 3, 1, 3, 0);
            for (Entity en : p.getWorld().getNearbyEntities(c, 6, 3, 6)) {
                if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                applyKnownEffects(le,
                        new PotionEffect(PotionEffectType.WITHER, 60, 0),
                        new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
            }
        }, 0L, 20L);
    }

    private void falloutShield(Player p) {
        if (!ready(p, "g_shield", 25000)) return;
        shieldUntil.put(p.getUniqueId(), System.currentTimeMillis() + 10000);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.6f);
        p.sendMessage(ChatColor.GREEN + "Fallout Shield up for 10 seconds.");
    }

    private void meltdown(Player p) {
        if (!ready(p, "g_meltdown", 150000)) return;
        meltdownUntil.put(p.getUniqueId(), System.currentTimeMillis() + 20000);
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 400, 0));
        p.sendTitle(ChatColor.GREEN + "MELTDOWN", ChatColor.WHITE + "Radiation is spreading", 5, 60, 10);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 0.8f);
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            runs[0]++;
            if (runs[0] > 20 || !p.isOnline()) {
                meltdownUntil.remove(p.getUniqueId());
                holder[0].cancel();
                return;
            }
            Location c = p.getLocation();
            particleRing(c.clone().add(0, 0.2, 0), 10, Particle.HAPPY_VILLAGER);
            for (Entity en : p.getWorld().getNearbyEntities(c, 10, 5, 10)) {
                if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                hit(le, 3, p);
                if (le instanceof Player victim) {
                    victim.sendMessage(ChatColor.GREEN + "You are being melted from the radiation");
                }
            }
        }, 0L, 20L);
    }

    @EventHandler
    public void onAtomicDefense(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !isActive(shieldUntil, p) || !holdsItem(p, "atomic")) return;
        e.setDamage(e.getDamage() * 0.5);
    }

    @EventHandler
    public void onAtomicBlindAttackers(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p) || !isActive(shieldUntil, p) || !holdsItem(p, "atomic")) return;
        if (e.getDamager() instanceof LivingEntity attacker && !attacker.equals(p)) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
        }
    }

    @EventHandler
    public void onAtomicImmune(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "atomic")) return;
        if (e.getAction() != EntityPotionEffectEvent.Action.ADDED
                && e.getAction() != EntityPotionEffectEvent.Action.CHANGED) return;
        PotionEffect incoming = e.getNewEffect();
        if (incoming == null) return;
        if (incoming.getType().equals(PotionEffectType.WITHER) || incoming.getType().equals(PotionEffectType.POISON)) {
            e.setCancelled(true);
        }
    }

    // ===================== WIND MASTER =====================

    private void gust(Player p) {
        if (!ready(p, "g_gust", 5000)) return;
        Location c = p.getLocation();
        World w = p.getWorld();
        for (Entity en : w.getNearbyEntities(c, 6, 3, 6)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0);
            if (out.lengthSquared() == 0) out = new Vector(0, 0, 1);
            le.setVelocity(out.normalize().multiply(1.6).setY(0.4));
            hit(le, 4, p);
        }
        for (double r = 2; r <= 6; r += 2) particleRing(c.clone().add(0, 1, 0), r, Particle.CLOUD);
        w.playSound(c, Sound.ENTITY_BREEZE_WIND_BURST, 2f, 1.2f);
        p.sendMessage(ChatColor.AQUA + "Gust!");
    }

    private void tornado(Player p) {
        if (!ready(p, "g_tornado", 20000)) return;
        World w = p.getWorld();
        Location center = p.getLocation();
        List<LivingEntity> victims = new ArrayList<>();
        for (Entity en : w.getNearbyEntities(center, 12, 6, 12)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            if (!(le instanceof Player) && NO_SUMMON.contains(le.getType())) continue;
            victims.add(le);
        }
        if (victims.isEmpty()) {
            p.sendMessage(ChatColor.RED + "No enemies within 12 blocks.");
            return;
        }
        final double[] spin = {0};
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            runs[0]++;
            spin[0] += 0.3;
            if (runs[0] > 100 || !p.isOnline()) {
                holder[0].cancel();
                return;
            }
            for (int i = 0; i < victims.size(); i++) {
                LivingEntity v = victims.get(i);
                if (!v.isValid() || v.isDead()) continue;
                double a = spin[0] + i * (2 * Math.PI / victims.size());
                double h = 2.5 + Math.sin(spin[0] * 2) * 1.2;
                Location orbit = center.clone().add(Math.cos(a) * 4, h, Math.sin(a) * 4);
                if (v instanceof Player pv) {
                    Vector pull = orbit.toVector().subtract(pv.getLocation().toVector());
                    if (pull.length() > 2) pull = pull.normalize().multiply(2);
                    pv.setVelocity(pull.multiply(0.5));
                    guard(pv, 20);
                } else {
                    v.teleport(orbit);
                    v.setVelocity(new Vector(0, 0, 0));
                }
                v.setFallDistance(0f);
            }
            for (int k = 0; k < 3; k++) {
                double a = spin[0] * 1.5 + k * (2 * Math.PI / 3);
                w.spawnParticle(Particle.CLOUD, center.clone().add(Math.cos(a) * 3, (runs[0] % 20) * 0.3, Math.sin(a) * 3), 2, 0.1, 0.1, 0.1, 0.02);
            }
        }, 0L, 1L);
        w.playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 2f, 0.8f);
        p.sendMessage(ChatColor.AQUA + "Tornado: " + victims.size() + " target(s) caught for 5 seconds.");
    }

    private void updraft(Player p) {
        if (!ready(p, "g_updraft", 15000)) return;
        p.setVelocity(new Vector(0, 2.2, 0));
        p.setFallDistance(0f);
        guard(p, 120);
        Location c = p.getLocation();
        particleRing(c.clone().add(0, 0.2, 0), 1.5, Particle.CLOUD);
        p.getWorld().playSound(c, Sound.ENTITY_BREEZE_SHOOT, 2f, 0.8f);
        p.sendMessage(ChatColor.AQUA + "Updraft!");
    }

    private void jumpPad(Player p) {
        if (!ready(p, "g_pad", 30000)) return;
        Block target = p.getTargetBlockExact(6);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Look at a block within 6 blocks.");
            return;
        }
        Block pad = target.getRelative(BlockFace.UP);
        if (!pad.getType().isAir()) {
            p.sendMessage(ChatColor.RED + "No space for the jump pad there.");
            return;
        }
        pad.setType(Material.SLIME_BLOCK);
        String key = keyOf(pad.getLocation());
        pads.put(key, p.getUniqueId());
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (pads.remove(key) != null && pad.getType() == Material.SLIME_BLOCK) pad.setType(Material.AIR);
        }, 600L);
        p.getWorld().playSound(pad.getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 1f, 1f);
        p.sendMessage(ChatColor.GREEN + "Jump pad placed for 30 seconds.");
    }

    @EventHandler
    public void onPadBounce(PlayerMoveEvent e) {
        if (pads.isEmpty()) return;
        Player p = e.getPlayer();
        if (p.getVelocity().getY() >= 0) return;
        Block below = p.getLocation().subtract(0, 0.1, 0).getBlock();
        if (below.getType() != Material.SLIME_BLOCK) return;
        UUID owner = pads.get(keyOf(below.getLocation()));
        if (owner == null || !owner.equals(p.getUniqueId())) return;
        p.setVelocity(new Vector(p.getVelocity().getX(), 1.9, p.getVelocity().getZ()));
        guard(p, 40);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 20, 0.4, 0.1, 0.4, 0.05);
    }

    private void windBurst(Player p) {
        if (!ready(p, "g_wburst", 40000)) return;
        Location eye = p.getEyeLocation();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            Vector dir = new Vector(Math.cos(a), 0.25, Math.sin(a)).normalize();
            spawnWindCharge(eye.clone().add(dir.clone().multiply(1.5)), dir.multiply(1.1));
        }
        particleRing(p.getLocation().add(0, 1, 0), 3, Particle.CLOUD);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 2f, 1f);
    }

    private Entity spawnWindCharge(Location loc, Vector vel) {
        try {
            Entity wc = loc.getWorld().spawnEntity(loc, EntityType.valueOf("WIND_CHARGE"));
            wc.setVelocity(vel);
            return wc;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void windBerserk(Player p) {
        if (!ready(p, "g_breeze", 120000)) return;
        windBerserkUntil.put(p.getUniqueId(), System.currentTimeMillis() + 30000);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1));
        p.sendTitle(ChatColor.AQUA + "BERSERK BREEZE", ChatColor.WHITE + "Gusts on every right-click for 30 seconds", 5, 60, 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 2f, 0.6f);
    }

    private void fireGust(Player p) {
        Vector look = p.getEyeLocation().getDirection().normalize();
        Snowball g = p.launchProjectile(Snowball.class, look.multiply(1.8));
        g.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "gust");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
    }

    @EventHandler
    public void onWindBow(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "wind")) return;
        if (e.getProjectile() instanceof AbstractArrow arrow) {
            arrow.setKnockbackStrength(arrow.getKnockbackStrength() + 2);
            arrow.setDamage(arrow.getDamage() * 1.25 + 1);
        }
    }

    @EventHandler
    public void onWindHitLaunch(EntityDamageByEntityEvent e) {
        if (e.isCancelled() || inThunderProc) return;
        if (!(e.getDamager() instanceof Player attacker) || !holdsItem(attacker, "wind")) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        if (rnd.nextDouble() < 0.05) victim.setVelocity(new Vector(0, 1.3, 0));
    }

    // ===================== INFERNO =====================

    private void meteorShower(Player p) {
        if (!ready(p, "g_meteor", 20000)) return;
        Location aim = aimPoint(p, 25);
        World w = p.getWorld();
        for (int i = 0; i < 12; i++) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!p.isOnline()) return;
                Location from = aim.clone().add(rand(-4, 4), 18, rand(-4, 4));
                Fireball f = w.spawn(from, Fireball.class);
                f.setDirection(new Vector(0, -1, 0));
                f.setYield(0f);
                f.setIsIncendiary(false);
                f.setShooter(p);
                f.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "meteor");
            }, i * 5L);
        }
        p.sendMessage(ChatColor.GOLD + "Meteor Shower!");
    }

    private void campfire(Player p) {
        if (!ready(p, "g_campfire", 40000)) return;
        p.sendMessage(ChatColor.GOLD + "Cozy Campfire lit for 15 seconds.");
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            runs[0]++;
            if (runs[0] > 15 || !p.isOnline()) {
                holder[0].cancel();
                return;
            }
            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
            Location c = p.getLocation().add(0, 1, 0);
            p.getWorld().spawnParticle(Particle.FLAME, c, 4, 0.5, 0.2, 0.5, 0.01);
            p.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.01);
            for (Entity en : p.getWorld().getNearbyEntities(c, 5, 3, 5)) {
                if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                hit(le, 2, p);
            }
        }, 0L, 20L);
    }

    private void blazeMode(Player p) {
        if (!ready(p, "g_blaze", 180000)) return;
        blazeUntil.put(p.getUniqueId(), System.currentTimeMillis() + 30000);
        p.sendTitle(ChatColor.GOLD + "BLAZE", ChatColor.YELLOW + "Explosive fireballs, no cooldown", 5, 60, 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2f, 0.6f);
    }

    private void fireBlazeBall(Player p) {
        Fireball f = p.launchProjectile(Fireball.class);
        f.setYield(0f);
        f.setIsIncendiary(true);
        f.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "blazeball");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.2f);
    }

    private void chargeFireball(Player p) {
        UUID id = p.getUniqueId();
        if (charging.containsKey(id)) {
            releaseCharge(p);
            return;
        }
        if (!ready(p, "g_charge", 3000)) return;
        double[] state = {0, 1}; // [charge percent, direction: 1 rising, -1 draining]
        charging.put(id, state);
        p.sendMessage(ChatColor.YELLOW + "Charging. Press F to release.");
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!charging.containsKey(id) || !p.isOnline()) {
                holder[0].cancel();
                return;
            }
            double[] s = charging.get(id);
            if (s[1] > 0) {
                s[0] += 2;
                if (s[0] >= 100) {
                    s[0] = 100;
                    s[1] = -1;
                }
            } else {
                s[0] -= 1;
                if (s[0] <= 0) {
                    charging.remove(id);
                    p.sendActionBar(ChatColor.RED + "Charge destroyed.");
                    holder[0].cancel();
                    return;
                }
            }
            p.sendActionBar(ChatColor.GOLD + "Charge: " + (int) s[0] + "%");
            Location eye = p.getEyeLocation();
            p.getWorld().spawnParticle(Particle.FLAME, eye.clone().add(eye.getDirection()),
                    2 + (int) (s[0] / 25), 0.2, 0.2, 0.2, 0.01);
        }, 0L, 1L);
    }

    private void releaseCharge(Player p) {
        double[] s = charging.remove(p.getUniqueId());
        if (s == null) return;
        int lvl = s[0] < 34 ? 1 : (s[0] < 67 ? 2 : 3);
        Fireball f = p.launchProjectile(Fireball.class);
        f.setYield(0f);
        f.setIsIncendiary(lvl >= 3);
        f.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "charge" + lvl);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);
        p.sendMessage(ChatColor.GOLD + "Released at " + (int) s[0] + "%.");
    }

    private void toggleEvaporation(Player p) {
        UUID id = p.getUniqueId();
        if (evapOn.remove(id)) {
            restoreWater(id);
            p.sendMessage(ChatColor.GRAY + "Evaporation off. Water restored.");
        } else {
            evapOn.add(id);
            p.sendMessage(ChatColor.AQUA + "Evaporation on. Water around you evaporates.");
        }
    }

    private void restoreWater(UUID id) {
        Map<Block, Material> saved = evapSaved.remove(id);
        if (saved == null) return;
        for (Map.Entry<Block, Material> en : saved.entrySet()) {
            if (en.getKey().getType().isAir()) en.getKey().setType(en.getValue());
        }
    }

    private void tickEvaporation() {
        for (UUID id : evapOn) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            Location c = p.getLocation();
            Map<Block, Material> saved = evapSaved.computeIfAbsent(id, k -> new HashMap<>());
            for (int x = -4; x <= 4; x++) {
                for (int y = -2; y <= 4; y++) {
                    for (int z = -4; z <= 4; z++) {
                        Block b = c.clone().add(x, y, z).getBlock();
                        if (b.getType() == Material.WATER) {
                            saved.put(b, Material.WATER);
                            b.setType(Material.AIR);
                        }
                    }
                }
            }
            p.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 0.1, 0), 4, 0.5, 0.05, 0.5, 0.01);
        }
    }

    @EventHandler
    public void onInfernoFireImmune(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "inferno")) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.FIRE || c == EntityDamageEvent.DamageCause.FIRE_TICK
                || c == EntityDamageEvent.DamageCause.LAVA || c == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInfernoFireAspect(EntityDamageByEntityEvent e) {
        if (e.isCancelled() || !(e.getDamager() instanceof Player p) || !holdsItem(p, "inferno")) return;
        if (!p.getInventory().getItemInMainHand().getType().name().endsWith("_SWORD")) return;
        if (e.getEntity() instanceof LivingEntity le) le.setFireTicks(Math.max(le.getFireTicks(), 160));
    }

    private void tickGemAuras() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (holdsItem(p, "wind")) {
                double base = gemTick * 0.3;
                for (int k = 0; k < 3; k++) {
                    double ang = base + k * 2 * Math.PI / 3;
                    p.getWorld().spawnParticle(Particle.CLOUD,
                            p.getLocation().add(Math.cos(ang) * 1.1, 0.3 + k * 0.6, Math.sin(ang) * 1.1), 1, 0, 0, 0, 0);
                }
            }
            if (holdsItem(p, "inferno")) {
                double a = gemTick * 0.25;
                p.getWorld().spawnParticle(Particle.FLAME,
                        p.getLocation().add(Math.cos(a) * 0.9, 0.5, Math.sin(a) * 0.9), 1, 0, 0.05, 0, 0.01);
                p.getWorld().spawnParticle(Particle.SMOKE,
                        p.getLocation().add(-Math.cos(a) * 0.9, 1.4, -Math.sin(a) * 0.9), 1, 0, 0, 0, 0.01);
            }
        }
    }

    private void tickAtomicRegen() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (holdsItem(p, "atomic") && p.getHealth() < p.getMaxHealth()) {
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1));
            }
        }
    }

    // ===================== AXOLOTL GIANT (mountable) =====================

    @EventHandler
    public void onAxolotlMount(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Axolotl ax)) return;
        if (!"giantaxo".equals(ax.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) return;
        Player p = e.getPlayer();
        e.setCancelled(true);
        UUID owner = ownerOf(ax);
        if (owner == null || !owner.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "That's not your axolotl.");
            return;
        }
        if (!ax.getPassengers().isEmpty()) return;
        ax.addPassenger(p);
        ax.setGravity(false);
        p.sendMessage(ChatColor.AQUA + "Riding the Axolotl Giant. Look where you want to fly. Sneak to get off.");
    }

    @EventHandler
    public void onAxolotlDismount(EntityDismountEvent e) {
        if (e.getDismounted() instanceof Axolotl ax
                && "giantaxo".equals(ax.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) {
            ax.setGravity(true);
        }
    }

    private void tickAxolotlRide() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!(p.getVehicle() instanceof Axolotl ax)) continue;
            if (!"giantaxo".equals(ax.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) continue;
            if (p.isSneaking()) {
                p.leaveVehicle();
                continue;
            }
            ax.setVelocity(p.getLocation().getDirection().normalize().multiply(0.6));
            p.setFallDistance(0f);
        }
    }

    @EventHandler
    public void onEvapQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        evapOn.remove(id);
        restoreWater(id);
    }

    // ===================== THUNDER MASTER =====================

    private void thunderStrike(Player p) {
        if (!ready(p, "g_tstrike", 5000)) return;
        World w = p.getWorld();
        Location from = p.getEyeLocation();
        Location t = aimPoint(p, 30);
        Vector span = t.toVector().subtract(from.toVector());
        for (int k = 1; k <= 6; k++) {
            w.spawnParticle(Particle.CRIT, from.clone().add(span.clone().multiply(k / 6.0)), 3, 0.1, 0.1, 0.1, 0);
        }
        w.strikeLightningEffect(t);
        damageArea(t, 3, 12, p);
        for (Entity en : w.getNearbyEntities(t, 3, 3, 3)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 9));
        }
        w.playSound(t, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);
        p.sendMessage(ChatColor.YELLOW + "Thunder Strike!");
    }

    // Stun for 8 seconds with 3 lightning strikes spread across it.
    private void stun(Player owner, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 9));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 1));
        target.getWorld().strikeLightningEffect(target.getLocation());
        hit(target, 6, owner);
        for (int d : new int[]{30, 90, 150}) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (target.isValid() && !target.isDead()) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    hit(target, 6, owner);
                }
            }, d);
        }
        owner.sendMessage(ChatColor.YELLOW + "Stunned!");
    }

    private void chainLightning(Player p) {
        List<LivingEntity> chain = new ArrayList<>();
        LivingEntity cur = nearestEnemy(p.getLocation(), 12, p);
        while (cur != null && chain.size() < 5) {
            chain.add(cur);
            cur = nextChain(cur, chain);
        }
        if (chain.isEmpty()) {
            p.sendMessage(ChatColor.RED + "No enemies nearby to chain to.");
            return;
        }
        if (!ready(p, "g_chain", 10000)) return;
        for (int i = 0; i < chain.size(); i++) {
            final LivingEntity target = chain.get(i);
            final Location from = (i == 0) ? p.getEyeLocation() : chain.get(i - 1).getLocation().add(0, 1, 0);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!target.isValid()) return;
                Vector span = target.getLocation().add(0, 1, 0).toVector().subtract(from.toVector());
                for (int s = 1; s <= 6; s++) {
                    from.getWorld().spawnParticle(Particle.CRIT, from.clone().add(span.clone().multiply(s / 6.0)), 3, 0.1, 0.1, 0.1, 0);
                }
                target.getWorld().strikeLightningEffect(target.getLocation());
                hit(target, 10, p);
            }, i * 4L);
        }
        p.sendMessage(ChatColor.YELLOW + "Chain Lightning hit " + chain.size() + " target(s).");
    }

    private void surge(Player p) {
        if (!ready(p, "g_surge", 20000)) return;
        surgeHits.put(p.getUniqueId(), 3);
        for (int k = 0; k < 20; k++) {
            final int step = k;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                double ang = Math.toRadians(step * 18);
                double h = 0.2 + step * 0.09;
                Location pt = p.getLocation().add(Math.cos(ang) * 0.8, h, Math.sin(ang) * 0.8);
                p.getWorld().spawnParticle(Particle.CRIT, pt, 3, 0.05, 0.05, 0.05, 0);
            }, k);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.3f);
        p.sendMessage(ChatColor.YELLOW + "Overcharge: your next 3 hits are empowered.");
    }

    private void zeus(Player p) {
        if (!ready(p, "g_zeus", 150000)) return;
        zeusUntil.put(p.getUniqueId(), System.currentTimeMillis() + 20000);
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 1));
        p.sendTitle(ChatColor.GOLD + "ZEUS", ChatColor.YELLOW + "The sky answers you", 5, 60, 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.8f);
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            runs[0]++;
            if (!p.isOnline() || runs[0] > 10) {
                zeusUntil.remove(p.getUniqueId());
                holder[0].cancel();
                return;
            }
            List<LivingEntity> targets = new ArrayList<>();
            for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 20, 10, 20)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) targets.add(le);
            }
            Collections.shuffle(targets, rnd);
            for (int i = 0; i < Math.min(3, targets.size()); i++) {
                LivingEntity t = targets.get(i);
                p.getWorld().strikeLightningEffect(t.getLocation());
                hit(t, 10, p);
            }
            particleRing(p.getLocation().add(0, 1, 0), 2.0, Particle.END_ROD);
        }, 0L, 40L);
    }

    private void particleRing(Location c, double r, Particle pt) {
        for (int deg = 0; deg < 360; deg += 20) {
            double rad = Math.toRadians(deg);
            c.getWorld().spawnParticle(pt, c.clone().add(Math.cos(rad) * r, 0.1, Math.sin(rad) * r), 1, 0, 0, 0, 0);
        }
    }

    // Passive aura for anyone holding the Thunder Master Gem.
    private void tickThunderAura() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!holdsItem(p, "thunder")) continue;
            boolean zeus = isActive(zeusUntil, p);
            double radius = zeus ? 1.2 : 0.7;
            Location base = p.getLocation().add(0, 0.2, 0);
            for (int i = 0; i < (zeus ? 4 : 2); i++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                Location pt = base.clone().add(Math.cos(ang) * radius, rnd.nextDouble() * 2.2, Math.sin(ang) * radius);
                p.getWorld().spawnParticle(Particle.END_ROD, pt, 1, 0, 0, 0, 0);
            }
            if (zeus) p.getWorld().spawnParticle(Particle.CRIT, base.clone().add(0, 1, 0), 4, 0.4, 0.8, 0.4, 0.05);
        }
    }

    @EventHandler
    public void onThunderDamage(EntityDamageByEntityEvent e) {
        if (inThunderProc || e.isCancelled()) return;
        Player attacker = null;
        if (e.getDamager() instanceof Player ap) attacker = ap;
        else if (e.getDamager() instanceof Projectile pj && pj.getShooter() instanceof Player sp) attacker = sp;
        if (attacker == null || !holdsItem(attacker, "thunder")) return;
        if (!(e.getEntity() instanceof LivingEntity victim) || victim.equals(attacker)) return;

        UUID aid = attacker.getUniqueId();
        int charges = surgeHits.getOrDefault(aid, 0);
        if (charges > 0) {
            e.setDamage(e.getDamage() + 10);
            if (charges - 1 <= 0) surgeHits.remove(aid);
            else surgeHits.put(aid, charges - 1);
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.2);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1.5f);
        }

        if (victim instanceof Player && rnd.nextDouble() < 0.20) {
            inThunderProc = true;
            try {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                hit(victim, 6, attacker);
            } finally {
                inThunderProc = false;
            }
        }
    }

    @EventHandler
    public void onThunderLightningImmune(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;
        if (e.getEntity() instanceof Player p && holdsItem(p, "thunder")) e.setCancelled(true);
    }

    @EventHandler
    public void onThunderEffectImmune(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "thunder")) return;
        if (e.getAction() != EntityPotionEffectEvent.Action.ADDED
                && e.getAction() != EntityPotionEffectEvent.Action.CHANGED) return;
        PotionEffect incoming = e.getNewEffect();
        if (incoming == null) return;
        if (incoming.getType().equals(PotionEffectType.SLOWNESS) || incoming.getType().equals(PotionEffectType.WEAKNESS)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onThunderHungerImmune(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "thunder")) return;
        if (e.getFoodLevel() < p.getFoodLevel()) e.setCancelled(true);
    }

    // ===================== CRAFTING, PROTECTION, SHRINES =====================

    private static final class ShrineHolder implements InventoryHolder {
        final Location loc;
        final String key;
        final Inventory inv;

        ShrineHolder(Location loc) {
            this.loc = loc;
            this.key = keyOf(loc);
            this.inv = Bukkit.createInventory(this, 27, ChatColor.GOLD + "Shrine");
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static String keyOf(Location l) {
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    private boolean isOurs(ItemStack it) {
        return idOf(it) != null;
    }

    private boolean isGem(ItemStack it) {
        String id = idOf(it);
        return id != null && gems.containsKey(id);
    }

    private boolean isEmpty(ItemStack it) {
        return it == null || it.getType().isAir();
    }

    private boolean isBundle(ItemStack it) {
        return it != null && it.getType().name().endsWith("BUNDLE");
    }

    // Crafting: none of our items can be used in any recipe.
    @EventHandler
    public void onCraftPrep(PrepareItemCraftEvent e) {
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (isOurs(it)) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (isOurs(it)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // Gems can't be dropped.
    @EventHandler
    public void onGemDrop(PlayerDropItemEvent e) {
        if (!isGem(e.getItemDrop().getItemStack())) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(ChatColor.RED + "Gems can't be dropped.");
    }

    // Gems are kept on death and returned on respawn.
    @EventHandler
    public void onGemDeath(PlayerDeathEvent e) {
        List<ItemStack> keep = new ArrayList<>();
        e.getDrops().removeIf(stack -> {
            if (!isGem(stack)) return false;
            keep.add(stack.clone());
            return true;
        });
        if (!keep.isEmpty()) deathKeep.put(e.getEntity().getUniqueId(), keep);
    }

    @EventHandler
    public void onGemRespawn(PlayerRespawnEvent e) {
        returnKeptGems(e.getPlayer());
    }

    @EventHandler
    public void onGemJoin(PlayerJoinEvent e) {
        returnKeptGems(e.getPlayer());
    }

    private void returnKeptGems(Player p) {
        List<ItemStack> keep = deathKeep.remove(p.getUniqueId());
        if (keep == null) return;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (ItemStack gem : keep) p.getInventory().addItem(gem);
        }, 5L);
    }

    // Gems can't go into chests, barrels, bundles or any other container.
    @EventHandler
    public void onGemContainer(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();

        if (top.getHolder() instanceof ShrineHolder) {
            if (p.isOp()) return;
            boolean placingIn = e.getClickedInventory() != null
                    && e.getClickedInventory().equals(top) && !isEmpty(e.getCursor());
            boolean shiftIn = e.getClickedInventory() != null
                    && !e.getClickedInventory().equals(top)
                    && e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;
            boolean hotbarIn = e.getAction() == InventoryAction.HOTBAR_SWAP;
            if (placingIn || shiftIn || hotbarIn) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + "Only operators can put items in shrines.");
            }
            return;
        }

        boolean intoContainer = !top.equals(p.getInventory());
        if (intoContainer && (isGem(e.getCursor()) || isGem(e.getCurrentItem()))) {
            e.setCancelled(true);
            return;
        }
        if (isGem(e.getCursor()) && isBundle(e.getCurrentItem())) {
            e.setCancelled(true);
            return;
        }
        if (intoContainer && e.getClick() == ClickType.NUMBER_KEY
                && isGem(p.getInventory().getItem(e.getHotbarButton()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onGemDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof ShrineHolder && !p.isOp()) {
            e.setCancelled(true);
            return;
        }
        if (!top.equals(p.getInventory()) && isGem(e.getOldCursor())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onGemHopper(InventoryMoveItemEvent e) {
        if (isGem(e.getItem())) e.setCancelled(true);
    }

    @EventHandler
    public void onGemFrame(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof ItemFrame)) return;
        if (isGem(e.getPlayer().getInventory().getItem(e.getHand()))) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Gems can't be placed in item frames.");
        }
    }

    @EventHandler
    public void onShrineClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof ShrineHolder) saveShrines();
    }

    @EventHandler
    public void onShrineBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        String key = keyOf(b.getLocation());
        ShrineHolder h = shrines.get(key);
        if (h == null) return;
        if (!e.getPlayer().isOp()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Only operators can break shrines.");
            return;
        }
        for (ItemStack it : h.inv.getContents()) {
            if (it != null) b.getWorld().dropItemNaturally(b.getLocation(), it);
        }
        shrines.remove(key);
        saveShrines();
    }

    private void summonShrine(Player p) {
        Block target = p.getTargetBlockExact(5);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Look at a block within 5 blocks.");
            return;
        }
        Block place = target.getRelative(BlockFace.UP);
        if (!place.getType().isAir()) {
            p.sendMessage(ChatColor.RED + "There needs to be empty space above the block you're looking at.");
            return;
        }
        place.setType(Material.LODESTONE);
        ShrineHolder h = new ShrineHolder(place.getLocation());
        shrines.put(h.key, h);
        saveShrines();
        p.sendMessage(ChatColor.GREEN + "Shrine placed. Right-click it to open. Only operators can add items.");
    }

    private void summonAxolotlGiant(Player p) {
        Axolotl ax = p.getWorld().spawn(p.getLocation().add(3, 0, 0), Axolotl.class);
        setOwner(ax, p.getUniqueId());
        ax.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, "giantaxo");
        ax.setPersistent(true);
        AttributeInstance sc = ax.getAttribute(Attribute.GENERIC_SCALE);
        if (sc != null) sc.setBaseValue(4.0);
        ax.setCustomName(ChatColor.AQUA + "Axolotl Giant");
        ax.setCustomNameVisible(true);
        p.sendMessage(ChatColor.AQUA + "Axolotl Giant summoned. (Riding and flying are not in this version yet.)");
    }

    private void loadShrines() {
        ConfigurationSection sec = getConfig().getConfigurationSection("shrines");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String worldName = sec.getString(id + ".world");
            if (worldName == null || Bukkit.getWorld(worldName) == null) continue;
            Location loc = new Location(Bukkit.getWorld(worldName),
                    sec.getInt(id + ".x"), sec.getInt(id + ".y"), sec.getInt(id + ".z"));
            ShrineHolder h = new ShrineHolder(loc);
            List<?> raw = sec.getList(id + ".items");
            if (raw != null) {
                ItemStack[] contents = new ItemStack[27];
                for (int i = 0; i < Math.min(raw.size(), 27); i++) {
                    if (raw.get(i) instanceof ItemStack is && !is.getType().isAir()) contents[i] = is;
                }
                h.inv.setContents(contents);
            }
            shrines.put(h.key, h);
        }
    }

    private void saveShrines() {
        getConfig().set("shrines", null);
        int idx = 0;
        for (ShrineHolder h : shrines.values()) {
            String base = "shrines.s" + (idx++);
            getConfig().set(base + ".world", h.loc.getWorld().getName());
            getConfig().set(base + ".x", h.loc.getBlockX());
            getConfig().set(base + ".y", h.loc.getBlockY());
            getConfig().set(base + ".z", h.loc.getBlockZ());
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack it : h.inv.getContents()) {
                items.add(it == null ? new ItemStack(Material.AIR) : it);
            }
            getConfig().set(base + ".items", items);
        }
        saveConfig();
    }

    @Override
    public void onDisable() {
        for (BarrierState bs : barriers) restoreRing(bs);
        saveShrines();
    }

    // ----- /stickman commands -----

    private boolean handleStickman(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only server operators can use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendStickmanHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "givegem" -> {
                if (args.length < 2) { usage(sender, "/stickman givegem <gem> [player]"); return true; }
                ItemDef g = gems.get(args[1].toLowerCase(Locale.ROOT));
                if (g == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown gem. Gems: " + String.join(", ", gems.keySet()));
                    return true;
                }
                Player target = resolveTarget(sender, args, 2);
                if (target == null) return true;
                target.getInventory().addItem(make(g));
                sender.sendMessage(ChatColor.GREEN + "Gave " + g.name() + " to " + target.getName());
            }
            case "summon" -> {
                if (args.length < 2) { usage(sender, "/stickman summon <shrine|axolotlgiant>"); return true; }
                if (!(sender instanceof Player sp)) { sender.sendMessage(ChatColor.RED + "Only players can summon."); return true; }
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "shrine" -> summonShrine(sp);
                    case "axolotlgiant" -> summonAxolotlGiant(sp);
                    default -> usage(sender, "/stickman summon <shrine|axolotlgiant>");
                }
            }
            case "nocooldowngem" -> {
                if (args.length < 2 || !(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false"))) {
                    usage(sender, "/stickman nocooldowngem <true|false> [player]");
                    return true;
                }
                Player target = resolveTarget(sender, args, 2);
                if (target == null) return true;
                boolean on = args[1].equalsIgnoreCase("true");
                if (on) gemNoCd.add(target.getUniqueId());
                else gemNoCd.remove(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Gem cooldowns " + (on ? "OFF" : "ON") + " for " + target.getName());
            }
            case "trust" -> {
                if (args.length < 2) { usage(sender, "/stickman trust <player>"); return true; }
                if (!(sender instanceof Player owner)) {
                    sender.sendMessage(ChatColor.RED + "Only players can trust others.");
                    return true;
                }
                Player other = Bukkit.getPlayerExact(args[1]);
                if (other == null) { sender.sendMessage(ChatColor.RED + "Player is not online."); return true; }
                if (other.equals(owner)) { sender.sendMessage(ChatColor.RED + "You can't trust yourself."); return true; }
                Set<UUID> set = trusts.computeIfAbsent(owner.getUniqueId(), k -> new HashSet<>());
                if (set.remove(other.getUniqueId())) {
                    owner.sendMessage(ChatColor.YELLOW + "No longer trusting " + other.getName());
                } else {
                    set.add(other.getUniqueId());
                    owner.sendMessage(ChatColor.GREEN + "You now trust " + other.getName() + ". Your abilities and mobs won't hurt them.");
                    other.sendMessage(ChatColor.GREEN + owner.getName() + " trusts you.");
                }
            }
            default -> sendStickmanHelp(sender);
        }
        return true;
    }

    private void sendStickmanHelp(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "StickmanItems gem commands (operators only):");
        s.sendMessage(ChatColor.YELLOW + "/stickman givegem <gem> [player]" + ChatColor.GRAY + " - give a gem");
        s.sendMessage(ChatColor.YELLOW + "/stickman nocooldowngem <true|false> [player]" + ChatColor.GRAY + " - gem cooldowns off/on");
        s.sendMessage(ChatColor.YELLOW + "/stickman trust <player>" + ChatColor.GRAY + " - toggle trust");
        s.sendMessage(ChatColor.GRAY + "Gems: " + String.join(", ", gems.keySet()));
    }

    private List<String> stickmanTab(String[] args) {
        if (args.length == 0) return STICKMAN_SUBS;
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) return STICKMAN_SUBS.stream().filter(s -> s.startsWith(last)).toList();
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("givegem")) {
            return gems.keySet().stream().filter(s -> s.startsWith(last)).toList();
        }
        if (args.length == 2 && sub.equals("nocooldowngem")) return List.of("true", "false");
        if (args.length == 2 && sub.equals("summon")) return List.of("shrine", "axolotlgiant");
        boolean players = (args.length == 2 && sub.equals("trust"))
                || (args.length == 3 && (sub.equals("givegem") || sub.equals("nocooldowngem")));
        if (players) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(last))
                    .toList();
        }
        return List.of();
    }

    private LivingEntity nextChain(LivingEntity from, List<LivingEntity> chain) {
        LivingEntity best = null;
        double bestDist = 8;
        for (Entity en : from.getWorld().getNearbyEntities(from.getLocation(), 8, 8, 8)) {
            if (!(en instanceof LivingEntity le) || en instanceof Player || chain.contains(le)) continue;
            double d = le.getLocation().distance(from.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    private double rand(double a, double b) {
        return a + rnd.nextDouble() * (b - a);
    }

    // ---------- Commands (operators only) ----------

    private static final List<String> SUBCOMMANDS =
            List.of("help", "list", "version", "give", "giveall", "info", "nocd", "cd", "reset", "clear");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("stickman")) return handleStickman(sender, args);
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only server operators can use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> sender.sendMessage(ChatColor.GREEN + "Items: " + String.join(", ", items.keySet()));
            case "version" -> sender.sendMessage(ChatColor.GREEN + "StickmanItems " + VERSION);
            case "give" -> {
                if (args.length < 2) { usage(sender, "/stickmanitems give <item> [player]"); return true; }
                ItemDef d = items.get(args[1].toLowerCase(Locale.ROOT));
                if (d == null) { sender.sendMessage(ChatColor.RED + "Unknown item. Use /stickmanitems list"); return true; }
                Player target = resolveTarget(sender, args, 2);
                if (target == null) return true;
                target.getInventory().addItem(make(d));
                sender.sendMessage(ChatColor.GREEN + "Gave " + d.name() + " to " + target.getName());
            }
            case "giveall" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                for (ItemDef d : items.values()) target.getInventory().addItem(make(d));
                sender.sendMessage(ChatColor.GREEN + "All items given to " + target.getName());
            }
            case "info" -> {
                if (args.length < 2) { usage(sender, "/stickmanitems info <item>"); return true; }
                ItemDef d = items.get(args[1].toLowerCase(Locale.ROOT));
                if (d == null) { sender.sendMessage(ChatColor.RED + "Unknown item. Use /stickmanitems list"); return true; }
                sender.sendMessage(hexColor(d.hex()) + d.name() + ChatColor.GRAY + " (" + d.id() + ", " + d.mat() + ")");
                sender.sendMessage(ChatColor.GRAY + d.lore());
            }
            case "nocd" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                noCooldown.add(target.getUniqueId());
                String prefix = target.getUniqueId() + ":";
                cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
                sender.sendMessage(ChatColor.GREEN + "Cooldowns OFF for " + target.getName());
                if (!sender.equals(target)) target.sendMessage(ChatColor.GREEN + "Your cooldowns are now off.");
            }
            case "cd" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                noCooldown.remove(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Cooldowns ON for " + target.getName());
                if (!sender.equals(target)) target.sendMessage(ChatColor.GREEN + "Your cooldowns are back on.");
            }
            case "reset" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                String prefix = target.getUniqueId() + ":";
                cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
                sender.sendMessage(ChatColor.GREEN + "Cooldowns reset for " + target.getName());
            }
            case "clear" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                var inv = target.getInventory();
                int removed = 0;
                for (int i = 0; i < inv.getSize(); i++) {
                    if (idOf(inv.getItem(i)) != null) {
                        inv.setItem(i, null);
                        removed++;
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Removed " + removed + " StickmanItems from " + target.getName());
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown command. /stickmanitems help");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.isOp()) return List.of();
        if (cmd.getName().equalsIgnoreCase("stickman")) return stickmanTab(args);
        if (args.length == 0) return SUBCOMMANDS;
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(last)).toList();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("give") || sub.equals("info"))) {
            return items.keySet().stream().filter(s -> s.startsWith(last)).toList();
        }
        boolean playerArg = (args.length == 2 && List.of("giveall", "nocd", "cd", "reset", "clear").contains(sub))
                || (args.length == 3 && sub.equals("give"));
        if (playerArg) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(last))
                    .toList();
        }
        return List.of();
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "StickmanItems commands (operators only):");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems give <item> [player]" + ChatColor.GRAY + " - give one item");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems giveall [player]" + ChatColor.GRAY + " - give every item");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems info <item>" + ChatColor.GRAY + " - item details");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems nocd [player]" + ChatColor.GRAY + " - turn cooldowns off");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems cd [player]" + ChatColor.GRAY + " - turn cooldowns back on");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems reset [player]" + ChatColor.GRAY + " - clear cooldowns now");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems clear [player]" + ChatColor.GRAY + " - remove all StickmanItems");
        s.sendMessage(ChatColor.YELLOW + "/stickmanitems list | version");
    }

    private void usage(CommandSender s, String msg) {
        s.sendMessage(ChatColor.RED + "Usage: " + msg);
    }

    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player p = Bukkit.getPlayerExact(args[index]);
            if (p == null) sender.sendMessage(ChatColor.RED + "Player is not online.");
            return p;
        }
        if (sender instanceof Player self) return self;
        sender.sendMessage(ChatColor.RED + "Console must specify an online player.");
        return null;
    }
}
