package com.stickmanitems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class StickmanItems extends JavaPlugin implements Listener, CommandExecutor {

    private static final String VERSION = "3.0.0";

    private record ItemDef(String id, String name, Material mat, String lore) {}

    private static final class PhoenixState {
        int phase; // 0 = rising, 1 = diving, 2 = just landed
        int ticks;
    }

    private final Map<String, ItemDef> items = new LinkedHashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> jetFuel = new HashMap<>();
    private final Map<UUID, PhoenixState> phoenix = new HashMap<>();
    private final Map<UUID, UUID> gemLocks = new HashMap<>();
    private final Random rnd = new Random();
    private NamespacedKey itemKey;
    private NamespacedKey projKey;

    @Override
    public void onEnable() {
        itemKey = new NamespacedKey(this, "item");
        projKey = new NamespacedKey(this, "proj");
        registerItems();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("stickmanitems").setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        getLogger().info("StickmanItems " + VERSION + " enabled.");
    }

    private void def(String id, String name, Material mat, String lore) {
        items.put(id, new ItemDef(id, name, mat, lore));
    }

    private void registerItems() {
        // Original items (updated)
        def("kaboom", "Kaboom Kachow", Material.BOW, "Every arrow shot from this bow explodes on impact.");
        def("thunderstick", "Thunderstick", Material.NETHERITE_SWORD, "Hits strike lightning. Right-click to call lightning where you look.");
        def("vampirefang", "Vampire Fang", Material.TRIDENT, "Heals you for 40% of melee damage. Right-click for a 4-heart heal.");
        def("frostbite", "Frostbite", Material.DIAMOND_SWORD, "Hits slow targets and add extra damage. Right-click for a frost nova.");
        def("winddash", "Wind Shard", Material.BREEZE_ROD, "Right-click to dash forward. No cooldown.");
        def("infernowand", "Inferno Wand", Material.BLAZE_ROD, "Right-click to launch a fireball.");
        def("groundbreaker", "Groundbreaker", Material.MACE, "Right-click for a shockwave that damages and knocks back nearby enemies. Breaks no blocks.");
        // Requested new items
        def("shakalaka", "Shakalaka Bow", Material.BOW, "Each arrow that lands triggers 3 random effects: lightning, explosion, fireball, or a friendly wolf.");
        def("phoenixblade", "Phoenix Blade", Material.NETHERITE_AXE, "Right-click to leap into the air, then dive and smash everything below you.");
        def("mobgem", "Crosshair Gem", Material.EMERALD, "Right-click a mob to make it follow your crosshair. Right-click again or in the air to release.");
        def("jetpack", "Jetpack", Material.IRON_CHESTPLATE, "Right-click to toggle thrust. 15 seconds of fuel, 20 second recharge.");
        def("grenade", "Grenade", Material.FIRE_CHARGE, "Right-click to throw. Explodes on impact without breaking blocks.");
        // Extra items
        def("magnetrod", "Magnet Rod", Material.LIGHTNING_ROD, "Right-click to pull nearby mobs toward you.");
        def("blizzardorb", "Blizzard Orb", Material.PACKED_ICE, "Right-click to freeze nearby mobs and players.");
        def("healingcharm", "Healing Charm", Material.GHAST_TEAR, "Right-click to heal 4 hearts and gain regeneration.");
        def("shadowcloak", "Shadow Cloak", Material.PHANTOM_MEMBRANE, "Right-click to become invisible for 10 seconds.");
        def("golembell", "Golem Bell", Material.BELL, "Right-click to summon a friendly iron golem for 90 seconds.");
        def("sonichorn", "Sonic Horn", Material.GOAT_HORN, "Right-click to blast nearby entities away from you.");
        def("stormheart", "Storm Heart", Material.HEART_OF_THE_SEA, "Right-click to call lightning on up to 3 nearby mobs.");
        def("speedboots", "Speed Boots", Material.LEATHER_BOOTS, "Right-click for Speed III for 10 seconds.");
        def("berserkember", "Berserk Ember", Material.NETHER_STAR, "Right-click for Strength II and Resistance for 10 seconds.");
        def("phasefruit", "Phase Fruit", Material.CHORUS_FRUIT, "Right-click to blink forward up to 8 blocks.");
    }

    private void tick() {
        tickPhoenix();
        tickJetpack();
        tickGem();
    }

    // ---------- Item helpers ----------

    private String idOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        PersistentDataContainer c = it.getItemMeta().getPersistentDataContainer();
        return c.get(itemKey, PersistentDataType.STRING);
    }

    private ItemStack make(ItemDef d) {
        ItemStack it = new ItemStack(d.mat());
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.GOLD + d.name());
        m.setLore(List.of(ChatColor.GRAY + d.lore()));
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, d.id());
        it.setItemMeta(m);
        return it;
    }

    private boolean ready(Player p, String id, long ms) {
        long now = System.currentTimeMillis();
        String key = p.getUniqueId() + ":" + id;
        Long last = cooldowns.get(key);
        if (last != null && now - last < ms) {
            p.sendMessage(ChatColor.RED + "On cooldown: " + ((ms - (now - last)) / 1000 + 1) + "s");
            return false;
        }
        cooldowns.put(key, now);
        return true;
    }

    private double rand(double a, double b) {
        return a + rnd.nextDouble() * (b - a);
    }

    private Location toLoc(Vector v, World w) {
        return new Location(w, v.getX(), v.getY(), v.getZ());
    }

    private LivingEntity nearestPlayer(Location loc, Player exclude) {
        Player best = null;
        double bestDist = 40;
        for (Player pl : loc.getWorld().getPlayers()) {
            if (pl.equals(exclude)) continue;
            double d = pl.getLocation().distance(loc);
            if (d < bestDist) {
                bestDist = d;
                best = pl;
            }
        }
        return best;
    }

    // ---------- Right-click handling ----------

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        String id = idOf(e.getItem());
        if (id == null) return;

        // Bows must still be drawable with right-click
        if (id.equals("kaboom") || id.equals("shakalaka")) return;

        e.setUseItemInHand(Event.Result.DENY);
        if (a == Action.RIGHT_CLICK_BLOCK) e.setUseInteractedBlock(Event.Result.DENY);

        World w = p.getWorld();
        switch (id) {
            case "thunderstick" -> {
                if (!ready(p, id, 5000)) return;
                RayTraceResult r = w.rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 40);
                Location t = (r != null && r.getHitPosition() != null)
                        ? toLoc(r.getHitPosition(), w)
                        : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(40));
                w.strikeLightning(t);
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
                for (Entity en : w.getNearbyEntities(p.getLocation(), 5, 3, 5)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                    le.damage(3, p);
                }
            }
            case "winddash" -> {
                Vector dir = p.getEyeLocation().getDirection().normalize();
                p.setVelocity(dir.multiply(2.2));
                p.setFallDistance(0f);
                w.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1f);
            }
            case "infernowand" -> {
                if (!ready(p, id, 1000)) return;
                Fireball f = p.launchProjectile(Fireball.class);
                f.setYield(1.0f);
                f.setIsIncendiary(true);
            }
            case "groundbreaker" -> {
                if (!ready(p, id, 4000)) return;
                Location loc = p.getLocation();
                w.spawnParticle(Particle.CLOUD, loc, 40, 2, 0.2, 2, 0.05);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
                for (Entity en : w.getNearbyEntities(loc, 4, 2, 4)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    le.damage(8, p);
                    le.setVelocity(new Vector(0, 0.8, 0));
                }
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
            case "mobgem" -> {
                if (gemLocks.remove(p.getUniqueId()) != null) {
                    p.sendMessage(ChatColor.YELLOW + "Crosshair Gem released.");
                } else {
                    p.sendMessage(ChatColor.YELLOW + "Right-click a mob to lock it onto your crosshair.");
                }
            }
            case "jetpack" -> {
                if (jetFuel.remove(p.getUniqueId()) != null) {
                    p.sendMessage(ChatColor.YELLOW + "Jetpack off.");
                } else {
                    if (!ready(p, id, 20000)) return;
                    jetFuel.put(p.getUniqueId(), 300);
                    p.sendMessage(ChatColor.GREEN + "Jetpack on.");
                }
            }
            case "magnetrod" -> {
                if (!ready(p, id, 3000)) return;
                for (Entity en : w.getNearbyEntities(p.getLocation(), 8, 4, 8)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    Vector pull = p.getLocation().toVector().subtract(le.getLocation().toVector());
                    if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(0.9));
                }
            }
            case "blizzardorb" -> {
                if (!ready(p, id, 15000)) return;
