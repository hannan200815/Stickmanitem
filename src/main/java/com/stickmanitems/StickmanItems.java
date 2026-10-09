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
        def("kaboom", "Kaboom Kachow", Material.BOW, "Every arrow shot from this bow explodes on impact.");
        def("thunderstick", "Thunderstick", Material.NETHERITE_SWORD, "Hits strike lightning. Right-click to call lightning where you look.");
        def("vampirefang", "Vampire Fang", Material.TRIDENT, "Heals you for 40% of melee damage. Right-click for a 4-heart heal.");
        def("frostbite", "Frostbite", Material.DIAMOND_SWORD, "Hits slow targets and add extra damage. Right-click for a frost nova.");
        def("winddash", "Wind Shard", Material.BREEZE_ROD, "Right-click to dash forward. No cooldown.");
        def("infernowand", "Inferno Wand", Material.BLAZE_ROD, "Right-click to launch a fireball.");
        def("groundbreaker", "Groundbreaker", Material.MACE, "Right-click for a shockwave that damages and knocks back nearby enemies. Breaks no blocks.");
        def("shakalaka", "Shakalaka Bow", Material.BOW, "Each arrow that lands triggers 3 random effects: lightning, explosion, fireball, or a friendly wolf.");
        def("phoenixblade", "Phoenix Blade", Material.NETHERITE_AXE, "Right-click to leap into the air, then dive and smash everything below you.");
        def("mobgem", "Crosshair Gem", Material.EMERALD, "Right-click a mob to make it follow your crosshair. Right-click again or in the air to release.");
        def("jetpack", "Jetpack", Material.IRON_CHESTPLATE, "Right-click to toggle thrust. 15 seconds of fuel, 20 second recharge.");
        def("grenade", "Grenade", Material.FIRE_CHARGE, "Right-click to throw. Explodes on impact without breaking blocks.");
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
                w.spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 80, 3, 1, 3, 0.05);
                for (Entity en : w.getNearbyEntities(p.getLocation(), 6, 3, 6)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                }
            }
            case "healingcharm" -> {
                if (!ready(p, id, 60000)) return;
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 8));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 1));
                w.spawnParticle(Particle.HEART, p.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0);
            }
            case "shadowcloak" -> {
                if (!ready(p, id, 60000)) return;
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0));
            }
            case "golembell" -> {
                if (!ready(p, id, 120000)) return;
                IronGolem g = w.spawn(p.getLocation(), IronGolem.class, ig -> ig.setPlayerCreated(true));
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (g.isValid()) g.remove();
                }, 1800L);
                w.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1f);
            }
            case "sonichorn" -> {
                if (!ready(p, id, 8000)) return;
                Location loc = p.getLocation();
                w.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_0, 2f, 1f);
                for (Entity en : w.getNearbyEntities(loc, 6, 3, 6)) {
                    if (en.equals(p)) continue;
                    Vector push = en.getLocation().toVector().subtract(loc.toVector());
                    if (push.lengthSquared() == 0) continue;
                    en.setVelocity(push.normalize().multiply(1.2).setY(0.4));
                }
            }
            case "stormheart" -> {
                if (!ready(p, id, 20000)) return;
                List<LivingEntity> targets = new ArrayList<>();
                for (Entity en : w.getNearbyEntities(p.getLocation(), 15, 15, 15)) {
                    if (en instanceof LivingEntity le && !(en instanceof Player) && !en.equals(p)) targets.add(le);
                }
                Collections.shuffle(targets, rnd);
                for (int i = 0; i < Math.min(3, targets.size()); i++) {
                    w.strikeLightning(targets.get(i).getLocation());
                }
            }
            case "speedboots" -> {
                if (!ready(p, id, 30000)) return;
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
            }
            case "berserkember" -> {
                if (!ready(p, id, 60000)) return;
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
            }
            case "phasefruit" -> {
                if (!ready(p, id, 3000)) return;
                Vector dir = p.getEyeLocation().getDirection().normalize();
                RayTraceResult r = w.rayTraceBlocks(p.getEyeLocation(), dir, 8);
                Location dest = (r != null && r.getHitPosition() != null)
                        ? toLoc(r.getHitPosition().subtract(dir.clone().multiply(0.6)), w)
                        : p.getEyeLocation().add(dir.multiply(8));
                dest.setYaw(p.getLocation().getYaw());
                dest.setPitch(p.getLocation().getPitch());
                dest.subtract(0, 1.62, 0);
                p.teleport(dest);
                w.spawnParticle(Particle.PORTAL, p.getLocation(), 30, 0.3, 0.5, 0.3, 0.1);
            }
            default -> { }
        }
    }
        @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!"mobgem".equals(idOf(p.getInventory().getItemInMainHand()))) return;
        e.setCancelled(true);
        if (!(e.getRightClicked() instanceof LivingEntity) || e.getRightClicked() instanceof Player) return;
        UUID current = gemLocks.get(p.getUniqueId());
        if (current != null && current.equals(e.getRightClicked().getUniqueId())) {
            gemLocks.remove(p.getUniqueId());
            p.sendMessage(ChatColor.YELLOW + "Crosshair Gem released.");
        } else {
            gemLocks.put(p.getUniqueId(), e.getRightClicked().getUniqueId());
            p.sendMessage(ChatColor.GREEN + "Mob locked. It will follow your crosshair.");
        }
    }

    private void tickPhoenix() {
        Iterator<Map.Entry<UUID, PhoenixState>> it = phoenix.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PhoenixState> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null || !p.isOnline()) { it.remove(); continue; }
            PhoenixState s = en.getValue();
            s.ticks++;
            if (s.phase == 0) {
                if (s.ticks >= 8 || p.getVelocity().getY() <= 0) {
                    p.setVelocity(new Vector(0, -2.5, 0));
                    s.phase = 1;
                    s.ticks = 0;
                }
            } else if (s.phase == 1) {
                if (s.ticks > 2 && p.isOnGround()) {
                    smash(p);
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

    private void smash(Player p) {
        Location loc = p.getLocation();
        World w = p.getWorld();
        w.spawnParticle(Particle.EXPLOSION, loc, 1);
        w.spawnParticle(Particle.CLOUD, loc, 60, 3, 0.2, 3, 0.1);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        for (Entity en : w.getNearbyEntities(loc, 4, 2, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
            le.damage(10, p);
            le.setVelocity(new Vector(0, 0.6, 0));
        }
        p.setFallDistance(0f);
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
            p.setFallDistance(0f);
            Vector v = p.getVelocity();
            if (v.getY() < 0.45) p.setVelocity(v.setY(0.45));
            p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 3, 0.1, 0, 0.1, 0.01);
            en.setValue(fuel - 1);
        }
    }

    private void tickGem() {
        Iterator<Map.Entry<UUID, UUID>> it = gemLocks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            Entity mob = Bukkit.getEntity(en.getValue());
            if (p == null || !p.isOnline() || mob == null || !mob.isValid()
                    || mob.getWorld() != p.getWorld()
                    || mob.getLocation().distance(p.getLocation()) > 40) {
                it.remove();
                continue;
            }
            Location eye = p.getEyeLocation();
            Location target = eye.clone().add(eye.getDirection().multiply(6));
            Vector diff = target.toVector().subtract(mob.getLocation().toVector());
            double d = diff.length();
            if (d > 0.5) mob.setVelocity(diff.normalize().multiply(Math.min(d * 0.35, 1.2)));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        String id = idOf(p.getInventory().getItemInMainHand());
        if (id == null) return;
        switch (id) {
            case "frostbite" -> {
                if (e.getEntity() instanceof LivingEntity le) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    e.setDamage(e.getDamage() + 2);
                }
            }
            case "thunderstick" -> {
                if (e.getEntity() instanceof LivingEntity le) {
                    p.getWorld().strikeLightning(le.getLocation());
                }
            }
            case "vampirefang" -> {
                double heal = e.getDamage() * 0.4;
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + heal));
            }
            default -> { }
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL
                && e.getEntity() instanceof Player p
                && phoenix.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        String id = idOf(e.getBow());
        if (id == null) return;
        if (id.equals("kaboom") || id.equals("shakalaka")) {
            e.getProjectile().getPersistentDataContainer().set(projKey, PersistentDataType.STRING, id);
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
            case "kaboom" -> w.createExplosion(loc, 3f, false, false, shooter);
            case "grenade" -> w.createExplosion(loc, 2.5f, false, false, shooter);
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
        switch (rnd.nextInt(4)) {
            case 0 -> w.strikeLightning(loc);
            case 1 -> w.createExplosion(loc, 2.5f, false, false, shooter);
            case 2 -> {
                Fireball f = w.spawn(loc.clone().add(0, 8, 0), Fireball.class);
                f.setDirection(new Vector(0, -1, 0));
            }
            default -> w.spawn(loc, Wolf.class, wf -> {
                wf.setTamed(true);
                if (shooter instanceof Player owner) {
                    wf.setOwner(owner);
                    LivingEntity target = nearestPlayer(loc, owner);
                    if (target != null) wf.setTarget(target);
                }
            });
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only server operators can use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "/stickmanitems give <item> [player]");
            sender.sendMessage(ChatColor.GOLD + "/stickmanitems giveall [player]");
            sender.sendMessage(ChatColor.GOLD + "/stickmanitems list | version");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> sender.sendMessage(ChatColor.GREEN + "Items: " + String.join(", ", items.keySet()));
            case "version" -> sender.sendMessage(ChatColor.GREEN + "StickmanItems " + VERSION);
            case "give" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /stickmanitems give <item> [player]");
                    return true;
                }
                ItemDef d = items.get(args[1].toLowerCase(Locale.ROOT));
                if (d == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown item. Use /stickmanitems list");
                    return true;
                }
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
            default -> sender.sendMessage(ChatColor.RED + "Unknown command. /stickmanitems help");
        }
        return true;
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
