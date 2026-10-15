package com.stickmanitems;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.entity.Monster;
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
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Mob;
import org.bukkit.Color;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Item;
import org.bukkit.event.EventPriority;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import java.util.logging.Level;
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
    private CreaturesModule creatures;

    private static final String VERSION = "4.0.0";

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
    private final Map<UUID, Set<UUID>> voidHits = new HashMap<>();
    private final Map<UUID, Deque<EntityType>> unoStore = new HashMap<>();

    private static final Map<String, String> ABILITY_NAMES = Map.ofEntries(
            Map.entry("g_lift", "Frost Lift"), Map.entry("g_barrier", "Ice Barrier"), Map.entry("g_snow", "Snow Storm"),
            Map.entry("g_subzero", "Subzero"), Map.entry("g_rain", "Trident Rain"), Map.entry("g_terror", "Night Terror"),
            Map.entry("g_punish", "Nightmare Punishment"), Map.entry("g_abyss", "Abyssal Sight"), Map.entry("g_creak", "Nightmare Phantom"),
            Map.entry("g_legion", "Wither Legion"), Map.entry("g_recall", "Fallen Recall"), Map.entry("g_touch", "Wither Touch"),
            Map.entry("g_giant", "Hollow Colossus"), Map.entry("g_tstrike", "Thunder Strike"), Map.entry("g_chain", "Chain Lightning"),
            Map.entry("g_surge", "Overcharge"), Map.entry("g_zeus", "Zeus"), Map.entry("g_gust", "Gust"), Map.entry("g_updraft", "Updraft"),
            Map.entry("g_tornado", "Tornado"), Map.entry("g_breeze", "Berserk Breeze"), Map.entry("g_meteor", "Meteor Shower"),
            Map.entry("g_campfire", "Cozy Campfire"), Map.entry("g_charge", "Chargeable Fireball"), Map.entry("g_blaze", "Blaze"),
            Map.entry("g_atomic", "Atomic Blast"), Map.entry("g_cloud", "Radiation Cloud"), Map.entry("g_shield", "Fallout Shield"),
            Map.entry("g_meltdown", "Meltdown"), Map.entry("g_sonic", "Sonic Wave"), Map.entry("g_echo", "Echo Pulse"),
            Map.entry("g_fist", "Warden's Fist"), Map.entry("g_wardenult", "Warden Monster"), Map.entry("g_yin", "Yin Strike"),
            Map.entry("g_yang", "Yang Strike"), Map.entry("g_balance", "Balance"), Map.entry("g_taijitu", "Taijitu"), Map.entry("reaper_soul", "Soul Drain"), Map.entry("reaper_mark", "Death Mark"),
            Map.entry("g_pad", "Jump Pad"), Map.entry("g_wburst", "Wind Burst"), Map.entry("g_pulse", "Void Pulse"),
            Map.entry("g_step", "Shadow Step"), Map.entry("g_umbral", "Umbral Shield"), Map.entry("g_eclipse", "Total Eclipse"),
            Map.entry("g_soldiers", "Summon Soldiers"), Map.entry("g_rally", "Rally"), Map.entry("g_curse", "Curse of Iron"),
            Map.entry("g_darklegion", "Dark Legion"));

    private static final Map<String, List<String>> GEM_ABILITIES = Map.ofEntries(
            Map.entry("thunder", List.of("g_tstrike", "g_chain", "g_surge", "g_zeus")),
            Map.entry("wind", List.of("g_gust", "g_updraft", "g_tornado", "g_breeze")),
            Map.entry("inferno", List.of("g_meteor", "g_campfire", "g_charge", "g_blaze")),
            Map.entry("blizzard", List.of("g_lift", "g_barrier", "g_snow", "g_subzero")),
            Map.entry("nightmare", List.of("g_terror", "g_punish", "g_abyss", "g_creak")),
            Map.entry("undead", List.of("g_legion", "g_recall", "g_touch", "g_giant")),
            Map.entry("atomic", List.of("g_atomic", "g_cloud", "g_shield", "g_meltdown")),
            Map.entry("warden", List.of("g_sonic", "g_echo", "g_fist", "g_wardenult")),
            Map.entry("yinyang", List.of("g_yin", "g_yang", "g_balance", "g_taijitu")),
            Map.entry("eclipse", List.of("g_pulse", "g_step", "g_umbral", "g_eclipse")),
            Map.entry("tide", List.of("g_wave", "g_whirl", "g_riptide", "g_kraken")),
            Map.entry("stonefist", List.of("g_quake", "g_armor", "g_boulder", "g_titan")),
            Map.entry("spectral", List.of("g_haunt", "g_phase", "g_wail", "g_reaper")),
            Map.entry("verdant", List.of("g_vine", "g_bloom", "g_thorn", "g_forest")),
            Map.entry("gravity", List.of("g_pull", "g_crush", "g_float", "g_singularity")));
    private static final Set<EntityType> NO_SUMMON = EnumSet.of(
            EntityType.WARDEN, EntityType.WITHER, EntityType.ENDER_DRAGON,
            EntityType.ELDER_GUARDIAN, EntityType.GIANT, EntityType.IRON_GOLEM);
    private static final List<String> STICKMAN_SUBS = List.of("givegem", "nocooldowngem", "trust", "summon", "help", "cooldowns", "gems", "reload", "clearcd", "heal", "feed", "followers", "cleargems", "trustlist", "dismount", "killmounts", "time", "weather", "stats", "menu", "killfollowers", "lightning", "bring", "tp", "fly", "invis", "crowncheck", "debug");
    private final Map<String, ShrineHolder> shrines = new HashMap<>();
    private final Map<UUID, List<ItemStack>> deathKeep = new HashMap<>();
    private final Map<UUID, Integer> boltCharges = new HashMap<>();
    private final Map<UUID, Integer> surgeHits = new HashMap<>();
    private final Map<UUID, Long> zeusUntil = new HashMap<>();
    private boolean inThunderProc = false;
    private final Map<UUID, Long> windBerserkUntil = new HashMap<>();
    private final Map<UUID, Long> blazeUntil = new HashMap<>();
    private final Set<UUID> evapOn = new HashSet<>();
    private final Map<UUID, Map<Block, Material>> evapSaved = new HashMap<>();
    private final Map<String, UUID> pads = new HashMap<>();
    private final Map<UUID, Long> launchedUntil = new HashMap<>();
    private final Map<UUID, Long> shieldUntil = new HashMap<>();
    private final Map<UUID, Long> meltdownUntil = new HashMap<>();

    private NamespacedKey itemKey;
    private NamespacedKey projKey;

    @Override
    public void onEnable() {
        loadVanillaCooldowns();
        if (getCommand("stickcooldowns") != null) {
            getCommand("stickcooldowns").setExecutor((sender, cmd, label, args) -> {
                cooldownCommand(sender, args);
                return true;
            });
        }
        if (getCommand("stickcooldowns") != null) {
            getCommand("stickcooldowns").setTabCompleter((sender, cmd, alias, args) -> cooldownTabs(args));
            getLogger().info("/stickcooldowns is registered.");
        }
        startGambleTimer();
        creatures = new CreaturesModule();
        creatures.start(this);
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

    /** Like def(), but the material is looked up by name, so a material missing from this server falls back to a Stick. */
    private void def(String id, String name, String matName, String hex, String lore) {
        Material m = Material.matchMaterial(matName);
        def(id, name, m == null ? Material.STICK : m, hex, lore);
    }

    private void def(String id, String name, Material mat, String hex, String lore) {
        items.put(id, new ItemDef(id, name, mat, hex, lore));
        modelIds.put(id, 1000 + items.size());
    }

    private void giveOrDrop(Player p, ItemStack it) {
        for (ItemStack rest : p.getInventory().addItem(it).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rest);
        }
    }

    private boolean hasGem(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (isGem(it)) return true;
        }
        return false;
    }

    private void registerItems() {
        def("thunderstick", "Thunderstick", Material.NETHERITE_SWORD, "#FFEA00", "Hits strike lightning. Right-click to call lightning where you look. Passive: Storm Touch: 10% chance on hit to call lightning for 3 damage.");
        def("vampirefang", "Vampire Fang", Material.NETHERITE_SWORD, "#FF1744", "Heals you for 40% of melee damage. Right-click for a 4-heart heal. Passive: Blood Drain: heals you 1 heart on hit.");
        def("frostbite", "Frostbite", Material.NETHERITE_SWORD, "#00E5FF", "Hits slow targets and add extra damage. Right-click for a frost nova. Passive: Frost Bite: slows the target for 1 second on hit.");
        def("winddash", "Wind Shard", Material.BREEZE_ROD, "#B3E5FC", "Right-click to dash forward. No cooldown. No fall damage from the dash. Passive: Gust: knocks the target back on hit.");
        def("infernowand", "Inferno Wand", Material.BLAZE_ROD, "#FF9100", "Right-click to launch a fireball. Fireballs do not break blocks. Passive: Ember: sets the target on fire for 2 seconds on hit.");
        def("groundbreaker", "Groundbreaker", Material.MACE, "#FFB300", "Right-click for a shockwave that damages and knocks back nearby enemies. Breaks no blocks. Passive: Seismic Rhythm: every 4th hit deals 4 extra damage.");
        def("phoenixblade", "Phoenix Blade", Material.MACE, "#FF2D75", "Right-click to leap into the air, then dive and smash. Harder falls hit harder. Passive: Rebirth: heals you 1 heart on hit while below half health.");
        def("gauntlet", "Control Orb", Material.PRISMARINE_SHARD, "#7C4DFF", "Right-click a mob or player to take control of it. Right-click again to hurl it forward. Passive: Orb Control: 1 extra damage and a 1-second slow on hit.");
        def("grenade", "Grenade", Material.EGG, "#76FF03", "Ability 1 (right-click): Throw a grenade. It explodes on impact, then explodes again one second later, both at half a TNT minecart's power. Passive: Pin Pull. Each melee hit deals 1 extra damage and gives the target 1 second of nausea.");
        def("magnetrod", "Magnet Rod", Material.MACE, "#D500F9", "Right-click to pull nearby mobs toward you, hard. Passive: Magnetism: pulls the target a block toward you on hit.");
        def("blizzardorb", "Blizzard Orb", Material.SNOWBALL, "#18FFFF", "Right-click to freeze everything nearby and slow it hard. Passive: Blizzard Chill: slows the target for 2 seconds on hit.");
        def("healingcharm", "Healing Charm", Material.GHAST_TEAR, "#FF4081", "Right-click to heal 8 hearts, gain regeneration and remove negative effects. Passive: Charm Heal: heals you half a heart on hit.");
        def("shadowcloak", "Shadow Cloak", Material.PHANTOM_MEMBRANE, "#6200EA", "Right-click for 20 seconds of invisibility and Speed II. Passive: Shroud: 1 second of invisibility on hit.");
        def("golembell", "Golem Call", Material.GOAT_HORN, "#FFC400", "Blow the horn to summon a friendly iron golem for 90 seconds. Passive: Iron Call: 10% chance on hit to knock the target back.");
        def("sonichorn", "Sonic Horn", Material.GOAT_HORN, "#FFAB40", "Right-click for a sonic boom that hits hard and knocks back everything in its path. Passive: Sonic Echo: 2 damage to foes within 2 blocks of the target on hit.");
        def("stormheart", "Storm Heart", Material.HEART_OF_THE_SEA, "#2962FF", "Right-click to call lightning on up to 5 nearby mobs. Passive: Storm Surge: 10% chance on hit to call lightning for 3 damage.");
        def("phasefruit", "Phase Fruit", Material.CHORUS_FRUIT, "#00E676", "Right-click to blink forward up to 12 blocks, then get Speed II. Never puts you inside a block. Passive: Phase Strike: 1 second of speed on hit.");
        def("voidscythe", "Void Slasher", Material.NETHERITE_SWORD, "#651FFF", "Right-click for a wide void slash that hits everything in front of you. Passive: Void Drain: 1 extra damage on hit, and you heal half a heart.");
        def("grapple", "Grapple Hook", Material.LEAD, "#8D6E63", "Right-click to hook a block or mob and pull yourself toward it. Pulls harder. Passive: Hook Pull: pulls the target a block toward you on hit.");
        def("starfall", "Starfall Staff", Material.STICK, "#FFF59D", "Right-click to call 5 falling stars onto the spot you look at. Passive: Starlight: 10% chance on hit to deal 4 extra damage.");
        def("venom", "Venom Dagger", Material.NETHERITE_SWORD, "#64DD17", "Poisons normal mobs and players. Heals undead mobs. Right-click to lunge. Passive: Venom Stack: poisons the target for 2 seconds on hit.");
        def("glacier", "Phoenix Call", Material.MACE, "#FF6E40", "Right-click to leap, then slam with fire and ice. The higher you fall, the harder it hits. Passive: Glacial Hold: slows the target for 2 seconds on hit.");
        def("tidal", "Tidal Sword", Material.NETHERITE_SWORD, "#0091EA", "Right-click to fire a water orb that blasts everything near its impact. Passive: Undertow: slows the target for 1 second on hit.");
        def("reapersigil", "Reaper's Sigil", Material.WITHER_SKELETON_SKULL, "#2962FF", "Right-click: Blue Wither Skull, a blue skull that withers and damages whatever it hits. Shift + right-click: Soul Drain, 8 damage to nearby enemies and heals you per hit. Sprint + right-click: Death Mark, marks the nearest enemy for 10 seconds for +50% damage. Offhand: lifesteal, you heal the damage you deal to players. Passive: Reaper's Mark: 2 extra damage on hit to targets below half health.");
        def("shadowdagger", "Shadow Dagger", Material.NETHERITE_SWORD, "#7E57C2", "Right-click to teleport behind the nearest enemy and strike for 10 damage. Passive: Backstab: 3 extra damage when you hit a foe from behind.");
        def("solarlance", "Solar Lance", Material.NETHERITE_SWORD, "#FFD180", "Right-click for an instant beam that hits everything in a 25-block line for 14 damage. Passive: Solar Flare: sets the target on fire for 3 seconds on hit.");
        def("gravitywell", "Gravity Well", Material.MAGMA_CREAM, "#AB47BC", "Right-click to place a gravity well for 6 seconds. It pulls and damages nearby mobs. Passive: Gravity Tug: pulls the target a block toward you on hit.");
        def("aegistotem", "Aegis Totem", Material.TOTEM_OF_UNDYING, "#66BB6A", "Right-click for 15 seconds of Resistance and Regeneration for you and your trusted players nearby. Passive: Aegis: 1 heart of absorption on hit.");
        def("unoreverse", "Uno Reverse Blade", Material.NETHERITE_SWORD, "#00E5FF", "Saves every projectile that hits you (up to 30). Right-click to release them back, newest first. Passive: Desperate Reverse: 2 extra damage on hit while below half health.");
        def("chronohourglass", "Chrono Hourglass", Material.CLOCK, "#FFD700", "Right-click to freeze every enemy within 10 blocks for 4 seconds. Trusted players and your mobs are not affected. Passive: Rewind: 10% chance on hit to heal 1 heart.");
        def("bloodstone", "Bloodstone Mace", Material.MACE, "#B71C1C", "Hits heal you for 10% of the damage dealt. Right-click for a blood nova that damages and slows nearby enemies. Passive: Blood Pact: heals you 1 heart on hit.");
        def("quakehammer", "Quake Hammer", Material.MACE, "#8D6E63", "Right-click to slam the ground. Everything within 6 blocks takes 18 damage and is thrown into the air. Passive: Aftershock: 15% chance on hit to knock the target up.");
        def("venomsickle", "Venom Sickle", Material.NETHERITE_SWORD, "#33691E", "Right-click to release a poison cloud for 8 seconds. Poisons normal mobs and players, heals undead. Passive: Sickle Venom: poisons the target for 3 seconds on hit.");
        def("crystalpick", "Crystal Pick", Material.NETHERITE_PICKAXE, "#E1F5FE", "Right-click to fire 5 crystal shards in a spread. Each does 7 damage in a small blast. Passive: Crystal Edge: 2 extra damage on hit.");
        def("rewindcompass", "Rewind Compass", Material.RECOVERY_COMPASS, "#00BFA5", "Right-click to teleport back to where you were 10 seconds ago. 30-second cooldown. Passive: Waypoint: 1 extra damage on hit.");
        def("spoutshell", "Nautilus Spout", Material.NAUTILUS_SHELL, "#0288D1", "Right-click to raise a water spout where you aim. Enemies near it are launched 2.5 blocks up and take 8 damage. 12-second cooldown. Passive: Spout Spray: knocks the target up slightly on hit.");
        def("honeybomb", "Honey Bomb", Material.HONEY_BOTTLE, "#FFB300", "Right-click to throw a honey bomb. On impact enemies nearby are heavily slowed and take 4 damage. 6-second cooldown. Passive: Sticky Honey: slows the target for 1 second on hit.");
        def("cloudbottle", "Cloud Bottle", Material.GLASS_BOTTLE, "#B3E5FC", "Right-click to conjure a cloud that lifts you about 8 blocks and lowers you gently back down. 20-second cooldown. Passive: Cloud Step: 1 second of Slow Falling on hit.");
        def("meteor", "Meteor", Material.DRAGON_BREATH, "#E040FB", "Right-click to call 10 to 20 dragon fireballs down on random spots around where you aim. 90-second cooldown. Passive: Ember Dust: sets the target on fire for 1 second on hit.");
        def("emberblade", "Ember Blade", Material.NETHERITE_SWORD, "#FF7043", "Ability 1 (right-click): Ember slash hits every enemy in a wide arc in front of you for 8 damage and sets them on fire for 4 seconds. 5-second cooldown. Passive: Ember Trail: sets the target on fire for 3 seconds on hit.");
        def("thornrapier", "Thorn Rapier", Material.NETHERITE_SWORD, "#66BB6A", "Ability 1 (right-click): Lunge forward and stab the first enemy you reach for 10 damage and 5 seconds of Poison. 6-second cooldown. Passive: Thorn Poison: poisons the target for 3 seconds on hit.");
        def("moonsplitter", "Moonsplitter", Material.NETHERITE_SWORD, "#B39DDB", "Ability 1 (right-click): Crescent slash hits every enemy within 5 blocks for 9 damage and knocks them back. 7-second cooldown. Passive: Moon Edge: 2 extra damage on hit at night.");
        def("frostfang", "Frostfang Greatsword", Material.NETHERITE_SWORD, "#81D4FA", "Ability 1 (right-click): Frost wave in a cone 8 blocks long deals 6 damage and slows enemies for 5 seconds. 9-second cooldown. Passive: Frostbite Edge: slows the target for 2 seconds on hit.");
        def("voidcutter", "Void Cutter", Material.NETHERITE_SWORD, "#7E57C2", "Ability 1 (right-click): Blink 6 blocks forward and slash everything within 2.5 blocks of where you land for 10 damage. 8-second cooldown. Passive: Rift Cut: 15% chance on hit to deal 4 extra damage.");
        def("bloodletter", "Bloodletter", Material.NETHERITE_SWORD, "#B71C1C", "Ability 1 (right-click): Spend 2 hearts to deal 12 damage to every enemy within 3 blocks. You need more than 3 hearts to use it. 10-second cooldown. Passive: Hemophilia: the target bleeds 1 damage one second after the hit.");
        def("sunder", "Sunder Blade", Material.NETHERITE_SWORD, "#8D6E63", "Ability 1 (right-click): Slam the ground, dealing 7 damage to enemies within 4 blocks and knocking them back. 6-second cooldown. Passive: Quakebreak: knocks the target back a block on hit.");
        def("soulreaver", "Soulreaver", Material.NETHERITE_SWORD, "#4A148C", "Ability 1 (right-click): Deals 6 hearts to every enemy within 4 blocks and heals you for half of the total, up to 6 hearts. 12-second cooldown. Passive: Soul Siphon: heals you 1 heart on hit.");
        def("stormfang", "Stormfang", Material.NETHERITE_SWORD, "#29B6F6", "Ability 1 (right-click): Chain lightning strikes the 3 nearest enemies within 8 blocks for 6 damage each. 9-second cooldown. Passive: Storm Chain: 10% chance on hit to strike lightning for 3 damage.");
        def("venomtooth", "Venomtooth", Material.NETHERITE_SWORD, "#7CB342", "Ability 1 (right-click): Releases a poison cloud around you for 6 seconds. Enemies within 3 blocks are poisoned and take damage each second. 14-second cooldown. Passive: Venom Bite: poisons the target for 2 seconds on hit.");
        def("bladestorm", "Bladestorm", Material.NETHERITE_SWORD, "#90A4AE", "Ability 1 (right-click): Spins twice, hitting every enemy within 3 blocks for 5 damage each time. 8-second cooldown. Passive: Whirl Cut: 1 extra damage on hit.");
        def("kaboom", "Kaboom Kachow", Material.BOW, "#FF3D00", "Every arrow shot from this bow explodes on impact. Passive: Kaboom Spark: 10% chance on hit to deal 4 extra damage.");
        def("shakalaka", "Shakalaka Bow", Material.BOW, "#00FF9C", "Each arrow that lands triggers 3 random effects: lightning, explosion or fireball. Passive: Shaka Rush: 1 second of speed on hit.");
        def("arccaster", "Arc Caster", Material.BOW, "#E1BEE7", "A bow. Arrows stun or freeze whatever they hit. Passive: Arc Charge: 15% chance on hit to strike lightning for 3 damage.");
        def("hyperion", "Hyperion Blade", Material.NETHERITE_SWORD, "#00E5FF", "Ability 1 (right-click): Hyperion Burst. Hits everything within 4 blocks for 9 damage. 8-second cooldown. Passive: Life Ward. Each melee hit gives you 2 hearts of absorption.");
        def("mistral", "Mistral Spear", "NETHERITE_SPEAR", "#B2DFDB", "Ability 1 (right-click): Mistral Lunge. Dash forward and hit the first foe for 9 damage. 6-second cooldown. Passive: Wind Cut. Each melee hit deals 1 extra damage.");
        def("obsidianfist", "Obsidian Fist", Material.MACE, "#3E2723", "Ability 1 (right-click): Obsidian Quake. Everything within 4 blocks takes 4 damage and is slowed for 2 seconds. 9-second cooldown. Passive: Hardened. Each melee hit slows the target for 1 second.");
        def("starshard", "Starshard Scepter", Material.GLOWSTONE_DUST, "#FFF176", "Ability 1 (right-click): Star Mark. Marks everything within 10 blocks, making it glow for 5 seconds, and deals 3 damage to foes within 3 blocks. 8-second cooldown. Passive: Constellation. 10% chance on hit to deal 5 extra damage.");
        def("cryptbone", "Crypt Bone", Material.BONE_MEAL, "#CFD8DC", "Ability 1 (right-click): Bone Burst. Hits everything within 3 blocks for 6 damage. 7-second cooldown. Passive: Bone Shatter. Each melee hit deals 2 extra damage.");
        def("tempesthalberd", "Tempest Halberd", Material.NETHERITE_AXE, "#00838F", "Ability 1 (right-click): Tempest Spin. Hits everything within 4 blocks for 6 damage and launches it. 8-second cooldown. Passive: Storm Edge. 10% chance on hit to call lightning for 2 damage.");
        def("bloodmoon", "Bloodmoon Scythe", Material.NETHERITE_SWORD, "#B71C1C", "Ability 1 (right-click): Blood Moon. Restores 2 hearts and gives regeneration for 10 seconds. 10-second cooldown. Passive: Blood Thirst. At night, each melee hit heals you 1 heart.");
        def("prismblade", "Prism Blade", Material.DIAMOND_SWORD, "#E040FB", "Ability 1 (right-click): Prism Beam. A beam 10 blocks long deals 6 damage to the first foe it hits. 6-second cooldown. Passive: Refraction. 15% chance on hit to deal 3 extra damage.");
        def("voidfang", "Voidfang Dagger", Material.NETHERITE_SWORD, "#311B92", "Ability 1 (right-click): Void Lunge. Dash forward and hit the first foe for 8 damage. 5-second cooldown. Passive: Void Bite. Each melee hit deals 1 extra damage.");
        def("solstice", "Solstice Hammer", Material.MACE, "#FFB300", "Ability 1 (right-click): Solstice Slam. Hits everything within 4.5 blocks for 11 damage and launches it. 9-second cooldown. Passive: Sunburst. Each melee hit sets the target on fire for 2 seconds.");
        def("glacierhalberd", "Glacier Halberd", Material.NETHERITE_AXE, "#B3E5FC", "Ability 1 (right-click): Glacier Nova. Everything within 5 blocks takes 4 damage and is heavily slowed for 3 seconds. 10-second cooldown. Passive: Permafrost. Each melee hit slows the target for 1 second.");
        def("embershort", "Ember Shortspear", "NETHERITE_SPEAR", "#FF5722", "Ability 1 (right-click): Ember Stab. A cone 5 blocks long deals 8 damage. 6-second cooldown. Passive: Smoulder Pierce. Each melee hit sets the target on fire for 2 seconds.");
        def("runescythe", "Rune Scythe", Material.NETHERITE_SWORD, "#7B1FA2", "Ability 1 (right-click): Rune Sweep. A cone 6 blocks long deals 7 damage and slows everything in it for 4 seconds. 7-second cooldown. Passive: Rune Mark. Each melee hit makes the target glow for 2 seconds.");
        def("thundercudgel", "Thunder Cudgel", Material.GOLDEN_AXE, "#FFEB3B", "Ability 1 (right-click): Thunder Chain. Lightning strikes the 2 nearest foes within 10 blocks for 7 damage each. 8-second cooldown. Passive: Thunder Hammer. 10% chance on hit to call lightning for 3 damage.");
        def("duskblade", "Dusk Blade", Material.NETHERITE_SWORD, "#37474F", "Ability 1 (right-click): Dusk Veil. Everything within 4 blocks takes 4 damage and gets Darkness for 3 seconds. 9-second cooldown. Passive: Dusk Edge. At night, each melee hit deals 1 extra damage.");
        def("ironbulwark", "Iron Bulwark", Material.NETHERITE_SWORD, "#CFD8DC", "Ability 1 (right-click): Bulwark Mend. Restores 3 hearts. 11-second cooldown. Passive: Bulwark. Each melee hit gives you 1 heart of absorption.");
        def("gamblersfavourite", "Gambler's Favourite", Material.EMERALD, "#FFD700", "Right-click to receive a random ability item for 3 minutes. Single use.");
        def("crazyrolls", "CrazyRolls", Material.NETHER_STAR, "#FF00FF", "Ability 1 (right-click): Fires one random ability from every single-ability weapon and every gem. Passive: Everything. Each melee hit triggers a random weapon passive and a random gem passive.");
        def("boomhammer", "Boom Hammer", Material.MACE, "#795548", "Ability 1 (right-click): Thunder Slam. Leap upward, then crash down after 1 second, hitting everything within 5 blocks for 14 damage and launching it. 9-second cooldown. Passive: 25% chance on a melee hit to launch the target into the air.");
        def("spearlance", "Spear of Dawn", "NETHERITE_SPEAR", "#FFCA28", "Ability 1 (right-click): Sunlance. A line of light 8 blocks long strikes every foe along it for 6 damage and makes them glow. 8-second cooldown. Passive: Bleed. Each melee hit makes the target bleed 1 damage a second for 3 seconds.");
        def("duskscythe", "Dusk Scythe", Material.NETHERITE_SWORD, "#6A1B9A", "Ability 1 (right-click): Reaping Arc. Pulls every foe in a 6-block cone toward you, then deals 4 damage to them. 6-second cooldown. Passive: Lifesteal. Each melee hit heals you 1 heart.");
        def("thornwhip", "Thornwhip", Material.NETHERITE_SWORD, "#2E7D32", "Ability 1 (right-click): Lash Chain. Hooks the first foe in your aim within 8 blocks, dealing 6 damage and pulling them to you. 7-second cooldown. Passive: Thorns. Each melee hit slows the target for 2 seconds.");
        def("frostglaive", "Frost Glaive", Material.NETHERITE_SWORD, "#B3E5FC", "Ability 1 (right-click): Frost Zone. Every foe within 4 blocks takes 2 damage and is heavily slowed for 3 seconds. 7-second cooldown. Passive: Frostbite. Each melee hit puts out the target's fire and deals 1 extra damage.");
        def("ironhalberd", "Iron Halberd", Material.NETHERITE_AXE, "#90A4AE", "Ability 1 (right-click): Cleaving Spin. Spin for 1 second, hitting foes within 3 blocks four times for 3 damage each. 5-second cooldown. Passive: Sweep. Each melee hit also strikes one other foe within 2 blocks of the target for 2 damage.");
        def("tidecaller", "Tidecaller Spear", "NETHERITE_SPEAR", "#26C6DA", "Ability 1 (right-click): Maelstrom. Makes a pull point 6 blocks ahead that drags foes within 4 blocks toward it and deals 5 damage. 8-second cooldown. Passive: Tidal Surge. Each melee hit knocks the target back.");
        def("bloodsickle", "Blood Sickle", Material.NETHERITE_SWORD, "#B71C1C", "Ability 1 (right-click): Hemorrhage. The nearest foe within 5 blocks bleeds 3 damage over 3 seconds, and you heal 1 heart per hit. 7-second cooldown. Passive: Blood Bond. Each melee hit on a player heals you 1 heart.");
        def("nagina", "Shadow Naginata", Material.NETHERITE_SWORD, "#263238", "Ability 1 (right-click): Shadow Slash. Dash 4 blocks forward, hitting everything in the path for 7 damage and blinding it for 2 seconds. 8-second cooldown. Passive: Veiled. Each melee hit makes you invisible for 1 second.");
        def("chainflail", "Chain Flail", Material.NETHERITE_SWORD, "#78909C", "Ability 1 (right-click): Chain Whirl. Spin for 2 seconds, hitting foes within 3 blocks four times for 2 damage each. 9-second cooldown. Passive: Hook. 20% chance on a melee hit to pull the target toward you.");
        def("lanternstaff", "Lantern Staff", Material.BLAZE_ROD, "#FFB300", "Ability 1 (right-click): Lantern Beam. Fires a beam of light along your aim that deals 9 damage to the first foe it hits. 4-second cooldown. Passive: Lamplight. Each melee hit makes the target glow for 3 seconds and deals 1 extra damage.");
        def("stonepike", "Stone Pike", Material.NETHERITE_SWORD, "#9E9E9E", "Ability 1 (right-click): Bedrock Stab. Plunge into the first foe in your aim within 3 blocks for 9 damage and slow them heavily for 1.5 seconds. 6-second cooldown. Passive: Stonehide. Each melee hit gives you 2 hearts of absorption.");
        def("cleaver", "Obsidian Cleaver", Material.NETHERITE_AXE, "#311B92", "Ability 1 (right-click): Shatter Slash. A cone 4 blocks long deals 10 damage and gives everything in it Weakness for 3 seconds. 7-second cooldown. Passive: Brittle Bite. Each melee hit gives the target Weakness for 2 seconds.");
        def("wardenclaw", "Warden's Claw", Material.NETHERITE_SWORD, "#0D47A1", "Ability 1 (right-click): Sonic Claw. A sonic wave 12 blocks long deals 6 damage to everything along it. 10-second cooldown. Passive: Tremor. Each melee hit knocks the target back a little.");
        def("frostlance", "Frost Lance", Material.NETHERITE_SWORD, "#81D4FA", "Ability 1 (right-click): Glacier Lance. A line of ice 6 blocks long deals 5 damage to everything along it and heavily slows it for 2 seconds. 8-second cooldown. Passive: Cold Snap. Each melee hit has a 20% chance to root the target for 1 second.");
        def("emberpike", "Ember Pike", "NETHERITE_SPEAR", "#FF6D00", "Ability 1 (right-click): Phoenix Thrust. Dash forward, and where you land a fire burst hits everything within 2.5 blocks for 8 damage and sets it alight for 4 seconds. 6.5-second cooldown. Passive: Ember Trail. Each melee hit sets the target on fire for 3 seconds.");
        def("stormhalberd", "Storm Halberd", Material.NETHERITE_AXE, "#FFEB3B", "Ability 1 (right-click): Storm Cleave. Calls lightning on up to 3 random foes within 6 blocks for 4 damage each. 9-second cooldown. Passive: Static. Each melee hit has a 15% chance to call lightning onto the target for 3 damage.");
        def("hexscythe", "Hexed Scythe", Material.NETHERITE_SWORD, "#4A148C", "Ability 1 (right-click): Hex Curse. The nearest foe within 6 blocks takes 3 damage and gets Wither and Nausea for 3 seconds. 14-second cooldown. Passive: Withering Touch. Each melee hit gives the target Wither for 2 seconds.");
        def("windpike", "Wind Pike", "NETHERITE_SPEAR", "#E0F7FA", "Ability 1 (right-click): Gust Vault. Leap up and forward, then a gust slams down after 0.9 seconds, pushing everything within 3 blocks away for 3 damage. 8-second cooldown. Passive: Tailwind. Each melee hit gives you Speed I for 2 seconds.");
        def("ironmaul", "Ironbound Maul", Material.NETHERITE_AXE, "#BCAAA4", "Ability 1 (right-click): Seismic Dash. Leap forward, then crash down after 0.75 seconds, hitting everything within 3 blocks for 12 damage. 10-second cooldown. Passive: Heavy Hit. Every 5th melee hit deals 6 extra damage.");
        def("ghostglaive", "Ghost Glaive", Material.NETHERITE_SWORD, "#EEEEEE", "Ability 1 (right-click): Phantom Step. Become invisible for 3 seconds. Your next melee hit in that time deals 6 extra damage. 9-second cooldown. Passive: Haunting. Each melee hit makes the target glow for 3 seconds.");
        def("dawnhalberd", "Dawn Halberd", Material.NETHERITE_SWORD, "#FFF59D", "Ability 1 (right-click): Dawn Radiance. Restore 3 hearts and gain fire resistance for 5 seconds. 12-second cooldown. Passive: Dawnbringer. Melee hits deal 3 extra damage to undead mobs.");
        def("tidallance", "Tidal Lance", "NETHERITE_SPEAR", "#0097A7", "Ability 1 (right-click): Tidal Hook. Pulls you behind the first foe within 8 blocks and hits them for 5 damage. 6-second cooldown. Passive: Undertow. Each melee hit slows the target for 1 second.");
        def("bloomstaff", "Bloom Staff", Material.STICK, "#66BB6A", "Ability 1 (right-click): Bloom Bloom. Restore 3 hearts and remove poison, wither, slowness, blindness and nausea. 9-second cooldown. Passive: Verdant Blessing. Each melee hit gives you Regeneration for 2 seconds.");
        def("moonblade", "Moonlit Blade", Material.NETHERITE_SWORD, "#9FA8DA", "Ability 1 (right-click): Lunar Blink. Teleport up to 5 blocks forward through open space. 10-second cooldown. Passive: Moonlight. At night, melee hits deal 2 extra damage.");
        def("echohammer", "Echo Warhammer", Material.MACE, "#8E24AA", "Ability 1 (right-click): Echo Shout. Two shockwaves hit everything within 4 blocks: 5 damage, then 3 damage a second later. 7-second cooldown. Passive: Resonance. Each melee hit echoes 2 damage to the nearest other foe within 3 blocks.");
        def("gravelglaive", "Gravel Glaive", Material.NETHERITE_SWORD, "#616161", "Ability 1 (right-click): Gravel Well. A well forms 5 blocks ahead for 1.5 seconds, pulling foes within 4 blocks toward its centre and dealing 2 damage each second. 9-second cooldown. Passive: Heavy Stone. Each melee hit deals 1 extra damage and slows the target for 1 second.");
        def("eclipsescythe", "Eclipse Scythe", Material.NETHERITE_SWORD, "#311B92", "Ability 1 (right-click): Umbral Veil. Everything within 5 blocks takes 2 damage and gets Darkness for 4 seconds. 11-second cooldown. Passive: Umbral Edge. Each melee hit gives the target Darkness for 2 seconds.");
        def("stormstaff", "Storm Staff", Material.BLAZE_ROD, "#FFD600", "Ability 1 (right-click): Storm Pulse. Calls lightning on the foe closest to your aim within 16 blocks for 10 damage, and 3 damage to foes within 2 blocks of it. 9-second cooldown. Passive: Charged. Every 4th melee hit chains lightning to the nearest other foe within 4 blocks for 3 damage.");
        def("soulsickle", "Soul Sickle", Material.NETHERITE_SWORD, "#7E57C2", "Ability 1 (right-click): Soul Harvest. Drains 2 damage from up to 5 foes within 6 blocks and gives you absorption for each one drained. 15-second cooldown. Passive: Soul Link. Killing a foe with a melee hit heals you 2 hearts.");
        def("emberglaive", "Ember Glaive", Material.NETHERITE_AXE, "#FF3D00", "Ability 1 (right-click): Ember Crescent. A fiery cone 6 blocks long deals 8 damage and sets everything in it on fire for 4 seconds. 7-second cooldown. Passive: Smolder. Each melee hit sets the target on fire for 2 seconds.");
        def("wardenspear", "Warden's Spear", "NETHERITE_SPEAR", "#1A237E", "Ability 1 (right-click): Echolocation. Everything within 12 blocks glows for 6 seconds, and foes within 3 blocks take 3 damage. 11-second cooldown. Passive: Dark Resonance. Each melee hit gives the target Darkness for 2 seconds.");
        def("hexbone", "Hex Bone", Material.BONE, "#CFD8DC", "Right-click to curse the ground around you for 15 seconds. Enemies within 6 blocks take 3 damage a second and are slowed. 25-second cooldown. Passive: Hex Touch: 1 extra damage and 1 second of Weakness on hit.");
        def("cursedcoin", "Cursed Coin", Material.GOLD_NUGGET, "#FFB74D", "Right-click to flip a cursed coin. It can heal you, blind nearby enemies, or wail so loudly that nearby enemies are slowed. 15-second cooldown. Passive: Cursed Luck: 10% chance on hit to give you Luck for 5 seconds.");
        def("spectralquill", "Spectral Quill", Material.FEATHER, "#E1F5FE", "Right-click to write a curse on the nearest enemy within 15 blocks. It takes 12 damage and gains Wither and Darkness. 12-second cooldown. Passive: Ink Curse: blinds the target for 1 second on hit.");
        def("rottenoffering", "Rotten Offering", Material.ROTTEN_FLESH, "#558B2F", "Right-click to raise 3 zombie servants for 60 seconds. They follow you and attack whatever you attack. 60-second cooldown. Passive: Rotten Bite: poisons the target for 2 seconds on hit.");
        def("eyeofthecrypt", "Eye of the Crypt", Material.SPIDER_EYE, "#7E57C2", "Right-click to reveal every invisible or hidden creature within 20 blocks for 8 seconds. 20-second cooldown. Passive: Crypt Sight: the target glows for 3 seconds on hit.");
        def("ghostlantern", "Ghost Lantern", Material.GLOW_INK_SAC, "#B388FF", "Right-click for 10 seconds of orbiting spirits that zap nearby enemies. Never hits your golems. Passive: Lantern Glow: the target glows for 2 seconds on hit.");
    }

    private void tick() {
        try {
            tickInner();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Tick error", t);
        }
    }

    private void tickInner() {
        tickPhoenix();
        tickJetpack();
        tickGem();
        tickFallGuard();
        tickGems();
        tickAxolotlRide();
        tickEvaporation();
        tickParrotRide();
        tickGiantRide();
        if (++hudTick % 5 == 0) updateHud();
    }

    // Action bar sits directly above the hotbar.
    private void updateHud() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            double[] cs = charging.get(p.getUniqueId());
            if (cs != null) {
                p.sendActionBar(ChatColor.GOLD + "Charge: " + (int) cs[0] + "%  (press F to release)");
                continue;
            }
            String gem = gemOf(p);
            if (gem != null && gems.containsKey(gem)) {
                p.sendActionBar(gemTable(p, gem));
                continue;
            }
            String id = idOf(p.getInventory().getItemInMainHand());
            if (id == null) id = idOf(p.getInventory().getItemInOffHand());
            if (id == null) continue;
            if (id.equals("kaboom") || id.equals("shakalaka") || id.equals("arccaster") || id.equals("jetpack")) continue;
            ItemDef d = items.get(id);
            if (d == null) continue;
            String title = hexColor(d.hex()) + d.name();
            String text;
            if (noCooldown.contains(p.getUniqueId())) {
                text = title + ChatColor.AQUA + " - NO COOLDOWN";
            } else {
                long left = cdLeft(p, id);
                text = left > 0
                        ? title + ChatColor.GRAY + " - " + ChatColor.RED + String.format(Locale.ROOT, "%.1fs", left / 1000.0)
                        : title + ChatColor.GREEN + " - READY";
            }
            p.sendActionBar(text);
        }
    }

    private String gemTable(Player p, String gem) {
        ItemDef d = gems.get(gem);
        StringBuilder sb = new StringBuilder(hexColor(d.hex()) + ChatColor.BOLD + d.name());
        int slot = 0;
        for (String ab : GEM_ABILITIES.getOrDefault(gem, List.of())) {
            slot++;
            long left = cdLeft(p, ab);
            sb.append(ChatColor.GRAY).append(" | ").append(left > 0 ? ChatColor.RED : ChatColor.GREEN).append("A").append(slot);
            if (left > 0) sb.append(ChatColor.RED).append(" ").append((left / 1000) + 1).append("s");
        }
        return sb.toString();
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
    }

    private boolean ultGate(Player p) {
        return true;
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

    // Stops any follower from holding its owner or trusted players as a target, including sonic and vibration AI.
    private void tickFollowerTargets() {
        for (UUID fid : followers) {
            Entity f = Bukkit.getEntity(fid);
            if (!(f instanceof Mob m) || !m.isValid()) continue;
            UUID own = ownerOf(m);
            if (own == null) continue;
            LivingEntity t = m.getTarget();
            if (t instanceof Player tp && (tp.getUniqueId().equals(own) || isTrusted(own, tp.getUniqueId()))) {
                m.setTarget(null);
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

    private void burstFor(Player p, String id) {
        Color c = colorFor(id);
        for (Map.Entry<String, List<String>> en : GEM_ABILITIES.entrySet()) {
            if (en.getValue().contains(id) && gems.containsKey(en.getKey())) {
                c = colorFor(en.getKey());
                break;
            }
        }
        Location at = p.getLocation().add(0, 1, 0);
        p.getWorld().spawnParticle(Particle.DUST, at, 60, 0.8, 0.9, 0.8, 0, new Particle.DustOptions(c, 1.6f));
        p.getWorld().spawnParticle(Particle.END_ROD, at, 25, 0.8, 0.9, 0.8, 0.08);
        for (int deg = 0; deg < 360; deg += 30) {
            double r = Math.toRadians(deg);
            p.getWorld().spawnParticle(Particle.DUST, at.clone().add(Math.cos(r) * 1.5, -0.8, Math.sin(r) * 1.5),
                    2, 0, 0, 0, 0, new Particle.DustOptions(c, 1.2f));
        }
    }

    /** Bold, italic name where each letter fades from the item's colour to white. */
    private String styledName(String name, String hex) {
        int[] c = {Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16)};
        StringBuilder sb = new StringBuilder();
        int n = Math.max(1, name.length() - 1);
        for (int i = 0; i < name.length(); i++) {
            double t = (double) i / n;
            int r = (int) Math.round(c[0] + (255 - c[0]) * t);
            int g = (int) Math.round(c[1] + (255 - c[1]) * t);
            int b = (int) Math.round(c[2] + (255 - c[2]) * t);
            sb.append(hexColor(String.format("#%02X%02X%02X", r, g, b))).append(ChatColor.BOLD).append(ChatColor.ITALIC).append(name.charAt(i));
        }
        return sb.toString();
    }

    private String hexColor(String hex) {
        return net.md_5.bungee.api.ChatColor.of(hex).toString();
    }

    private String idOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        PersistentDataContainer c = it.getItemMeta().getPersistentDataContainer();
        return c.get(itemKey, PersistentDataType.STRING);
    }

    // Bliss SMP style lore: grey description, gold activation line, bold headings, bullet passives.
    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            if (cur.length() > 0 && cur.length() + word.length() + 1 > width) {
                lines.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private static final Map<String, String> ITEM_PASSIVES = Map.ofEntries(
            Map.entry("thunderstick", "Melee hits strike lightning and add 6 damage."),
            Map.entry("vampirefang", "Heals you for 40% of the melee damage you deal."),
            Map.entry("frostbite", "Melee hits slow the target and add 4 damage."),
            Map.entry("venom", "Melee hits poison normal mobs and players, and heal undead mobs."),
            Map.entry("reapersigil", "Offhand: lifesteal, you heal the damage you deal to players. Passive: Harvest, every kill heals you 4 hearts while it is in your offhand."),
            Map.entry("bloodstone", "Melee hits heal you for 10% of the damage dealt."),
            Map.entry("arccaster", "Each arrow stuns or freezes the target it hits."),
            Map.entry("unoreverse", "Saves every projectile that hits you while you hold it, up to 30."),
            Map.entry("winddash", "No fall damage from the dash."));

    private static final Set<String> BOWS = Set.of("kaboom", "shakalaka", "arccaster");


    private String keybindName(String label) {
        return switch (label) {
            case "Right-click" -> "Right-click";
            case "Shift+right-click" -> "Shift + Right-click";
            case "Sprint+F" -> "Sprint + F";
            case "Shift+F" -> "Shift + F";
            case "F" -> "F";
            default -> label;
        };
    }

    private List<String> loreFor(ItemDef d) {
        List<String> out = new ArrayList<>();
        if (gems.containsKey(d.id())) {
            String text = d.lore();
            java.util.regex.Matcher mt = java.util.regex.Pattern.compile(
                    "(Right-click|Shift\\+right-click|Sprint\\+F|Shift\\+F|F|Passives|The Light Before the Dark):").matcher(text);
            List<String[]> segs = new ArrayList<>();
            int last = -1;
            String lastLabel = null;
            while (mt.find()) {
                if (lastLabel != null) segs.add(new String[]{lastLabel, text.substring(last, mt.start()).trim()});
                lastLabel = mt.group(1);
                last = mt.end();
            }
            if (lastLabel != null) segs.add(new String[]{lastLabel, text.substring(last).trim()});

            int abilityNo = 0;
            String ultKey = null;
            String ultText = null;
            for (String[] seg : segs) {
                String label = seg[0];
                if (label.equals("Passives") || label.equals("The Light Before the Dark")) continue;
                if (seg[1].contains("ultimate")) {
                    ultKey = label;
                    ultText = seg[1];
                    continue;
                }
                abilityNo++;
                out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ability " + abilityNo + " (" + keybindName(label) + ")");
                for (String ln : wrapText(seg[1], 40)) out.add(ChatColor.GRAY + ln);
                out.add("");
            }

            out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Passive");
            for (String[] seg : segs) {
                if (seg[0].equals("Passives")) {
                    for (String part : seg[1].split("\\. ")) {
                        String t = part.trim().replaceAll("\\.$", "");
                        if (!t.isEmpty()) out.add(ChatColor.GRAY + "- " + t);
                    }
                } else if (seg[0].equals("The Light Before the Dark")) {
                    out.add(ChatColor.GRAY + "- The Light Before the Dark: " + seg[1].replaceAll("\\.$", ""));
                }
            }
            out.add("");

            if (ultText != null) {
                out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ult (" + keybindName(ultKey) + ")");
                for (String ln : wrapText(ultText, 40)) out.add(ChatColor.GRAY + ln);
            }
            out.add("");
            out.add(ChatColor.YELLOW + "Works from your main or offhand. Cooldowns show above the hotbar.");
            return out;
        }
        if (d.id().equals("cursedcrown")) {
            out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ability 1 (Right-click)");
            for (String ln : wrapText("Summons 4 soldiers that follow you and fight whatever you attack. Up to 8 at once, for 2 minutes.", 40)) out.add(ChatColor.GRAY + ln);
            out.add("");
            out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ability 2 (Shift + Right-click)");
            for (String ln : wrapText("Rally: teleports all your soldiers to you and gives them Strength II.", 40)) out.add(ChatColor.GRAY + ln);
            out.add("");
            out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ability 3 (F)");
            for (String ln : wrapText("Curse of Iron: enemies within 8 blocks get Weakness II and Slowness II.", 40)) out.add(ChatColor.GRAY + ln);
            out.add("");
            out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Passive");
            out.add(ChatColor.GRAY + "- Your soldiers deal 25% more damage.");
            out.add(ChatColor.GRAY + "- You take 10% more damage while holding or wearing the crown.");
            out.add(ChatColor.GRAY + "- Sprinting leaves a yellow trail.");
            out.add("");
            out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ult (Shift + F)");
            for (String ln : wrapText("Dark Legion: summons 8 wither skeleton soldiers for 3 minutes and gives you Resistance II.", 40)) out.add(ChatColor.GRAY + ln);
            out.add("");
            out.add(ChatColor.YELLOW + "Works from your main hand, offhand, or head.");
            return out;
        }
        String keybind = BOWS.contains(d.id()) ? "Shoot" : "Right-click";
        String desc = d.lore().replaceAll("\\s*[0-9]+-second cooldown\\.", "").trim();
        out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Ability 1 (" + keybind + ")");
        for (String ln : wrapText(desc, 40)) out.add(ChatColor.GRAY + ln);
        out.add("");
        String passive = ITEM_PASSIVES.get(d.id());
        if (passive != null) {
            out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Passive");
            for (String ln : wrapText(passive, 40)) out.add(ChatColor.GRAY + ln);
            out.add("");
        }
        out.add(ChatColor.YELLOW + "Works from your main hand or offhand.");
        return out;
    }

    private ItemStack make(ItemDef d) {
        ItemStack it = new ItemStack(d.mat());
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(styledName(d.name(), d.hex()));
        Integer model = modelIds.get(d.id());
        if (model != null) m.setCustomModelData(model);
        m.setLore(loreFor(d));
        m.setEnchantmentGlintOverride(true);
        if (d.id().equals("cursedcrown")) {
            m.setUnbreakable(true);
            m.addEnchant(Enchantment.PROTECTION, 10, true);
            m.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        }
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, d.id());
        it.setItemMeta(m);
        return it;
    }

    // ---------- Gambler's Favourite ----------

    private record GambleGrant(String id, long expires) {}
    private final Map<UUID, List<GambleGrant>> gambleGrants = new HashMap<>();

    private void gamble(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else p.getInventory().setItemInMainHand(null);
        String pick = ABILITY_ITEM_IDS[rnd.nextInt(ABILITY_ITEM_IDS.length)];
        ItemDef d = items.get(pick);
        if (d == null) return;
        for (ItemStack left : p.getInventory().addItem(make(d)).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
        gambleGrants.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>())
                .add(new GambleGrant(pick, System.currentTimeMillis() + 180_000L));
        p.sendMessage(ChatColor.GOLD + "You got " + d.name() + " for 3 minutes.");
    }

    private void startGambleTimer() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, List<GambleGrant>> en : gambleGrants.entrySet()) {
                Player p = Bukkit.getPlayer(en.getKey());
                if (p == null) continue;
                en.getValue().removeIf(g -> {
                    if (now < g.expires()) return false;
                    ItemStack[] contents = p.getInventory().getContents();
                    for (int i = 0; i < contents.length; i++) {
                        if (g.id().equals(idOf(contents[i]))) p.getInventory().setItem(i, null);
                    }
                    return true;
                });
            }
        }, 20L, 20L);
    }

    /** True while an ability item is cooling down, so the item can be used normally (for example a spear). */
    private boolean abilityCoolingDown(Player p, String id) {
        if (noCooldown.contains(p.getUniqueId())) return false;
        Long expiry = cooldowns.get(p.getUniqueId() + ":" + id);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    private boolean ready(Player p, String id, long ms) {
        if (noCooldown.contains(p.getUniqueId()) || (id.startsWith("g_") && gemNoCd.contains(p.getUniqueId()))) return true;
        String path = "cooldowns." + id;
        if (!getConfig().contains(path)) getConfig().set(path, ms / 1000.0);
        long cdMs = Math.round(getConfig().getDouble(path, ms / 1000.0) * 1000);
        long now = System.currentTimeMillis();
        String key = p.getUniqueId() + ":" + id;
        Long expiry = cooldowns.get(key);
        if (expiry != null && now < expiry) {
            dbg(p, id + " is on cooldown");
            return false;
        }
        cooldowns.put(key, now + cdMs);
        dbg(p, id + " fired (cooldown " + (cdMs / 1000) + "s)");
        String name = ABILITY_NAMES.getOrDefault(id, items.containsKey(id) ? items.get(id).name() : id);
        burstFor(p, id);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.4f);
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
        double out = dmg * ABILITY_DAMAGE_MULT;
        if (src instanceof Player sp && gemOf(sp) != null) {
            out *= getConfig().getDouble("damage.gem-multiplier", 1.5);
        }
        if (src != null) le.damage(out, src);
        else le.damage(out);
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
        w.createExplosion(c, 2.0f, false, false);
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
            if (en instanceof Player tp && exclude != null
                    && (tp.getUniqueId().equals(exclude.getUniqueId()) || isTrusted(exclude.getUniqueId(), tp.getUniqueId()))) continue;
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
        try {
            onInteractInner(e);
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Click error", t);
            e.getPlayer().sendMessage(ChatColor.RED + "Click error: " + t);
        }
    }

    private void onInteractInner(PlayerInteractEvent e) {
        dbg(e.getPlayer(), "click action=" + e.getAction() + " hand=" + e.getHand() + " item=" + idOf(e.getItem()) + " block=" + (e.getClickedBlock() == null ? "none" : e.getClickedBlock().getType().name()));
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && (e.getHand() == null || e.getHand() == EquipmentSlot.HAND)
                && e.getClickedBlock() != null
                && shrines.containsKey(keyOf(e.getClickedBlock().getLocation()))) {
            e.setCancelled(true);
            e.getPlayer().openInventory(shrines.get(keyOf(e.getClickedBlock().getLocation())).inv);
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && e.getClickedBlock().getType().isInteractable()) {
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && idOf(e.getItem()) != null) {
            e.setUseItemInHand(Event.Result.DENY);
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        String id = idOf(e.getItem());
        if (id == null) return;

        // Bows must still be drawable with right-click
        if (id.equals("kaboom") || id.equals("shakalaka") || id.equals("arccaster")) return;

        if (!gems.containsKey(id) && abilityCoolingDown(p, id)) return;
        e.setUseItemInHand(Event.Result.DENY);
        if (a == Action.RIGHT_CLICK_BLOCK) e.setUseInteractedBlock(Event.Result.DENY);

        if (gems.containsKey(id)) {
            handleGemRight(p, id);
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
            case "shadowdagger" -> {
                if (!ready(p, id, 8000)) return;
                LivingEntity target = nearestEnemy(p.getLocation(), 12, p);
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "No enemy within 12 blocks.");
                    return;
                }
                Location from = p.getLocation().clone();
                Location behind = target.getLocation().clone().add(target.getLocation().getDirection().setY(0).normalize().multiply(-1.5));
                behind.setDirection(target.getLocation().toVector().subtract(behind.toVector()));
                p.teleport(behind);
                hit(target, 10, p);
                for (int k = 0; k <= 8; k++) {
                    Location tp = from.clone().add(behind.clone().toVector().subtract(from.toVector()).multiply(k / 8.0));
                    w.spawnParticle(Particle.DUST, tp.add(0, 1, 0), 3, 0.1, 0.2, 0.1, 0, new Particle.DustOptions(colorFor(id), 1.2f));
                }
                w.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.4f);
            }
            case "solarlance" -> {
                if (!ready(p, id, 4000)) return;
                Set<UUID> beamHit = new HashSet<>();
                for (double d = 1; d <= 25; d += 0.5) {
                    Location pt = eyeLoc.clone().add(look.clone().multiply(d));
                    w.spawnParticle(Particle.DUST, pt, 2, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(colorFor(id), 1.5f));
                    for (Entity en : w.getNearbyEntities(pt, 1, 1, 1)) {
                        if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le) || beamHit.contains(le.getUniqueId())) continue;
                        beamHit.add(le.getUniqueId());
                        hit(le, 30, p);
                    }
                }
                w.playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 1.8f);
            }
            case "gravitywell" -> {
                if (!ready(p, id, 20000)) return;
                Location center = aimPoint(p, 20);
                final int[] runs = {0};
                final BukkitTask[] holder = new BukkitTask[1];
                holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
                    runs[0]++;
                    if (runs[0] > 12 || !p.isOnline()) {
                        holder[0].cancel();
                        return;
                    }
                    for (Entity en : w.getNearbyEntities(center, 6, 6, 6)) {
                        if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                        Vector pull = center.toVector().subtract(le.getLocation().toVector());
                        if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(0.6));
                        if (runs[0] % 2 == 0) hit(le, 2, p);
                    }
                    particleRing(center.clone().add(0, 0.2, 0), 2 + (runs[0] % 4), Particle.DRAGON_BREATH);
                }, 0L, 10L);
                w.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 1f, 0.8f);
            }
            case "aegistotem" -> {
                if (!ready(p, id, 40000)) return;
                final int[] runs = {0};
                final BukkitTask[] holder = new BukkitTask[1];
                holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
                    runs[0]++;
                    if (runs[0] > 15 || !p.isOnline()) {
                        holder[0].cancel();
                        return;
                    }
                    for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 8, 4, 8)) {
                        if (!(en instanceof Player ally)) continue;
                        if (!ally.equals(p) && !isTrusted(p.getUniqueId(), ally.getUniqueId())) continue;
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0));
                    }
                    particleRing(p.getLocation().add(0, 0.2, 0), 3, Particle.TOTEM_OF_UNDYING);
                }, 0L, 20L);
                w.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
            }
            case "unoreverse" -> releaseProjectile(p);
            case "chronohourglass" -> {
                if (!ready(p, id, 30000)) return;
                int frozen = 0;
                for (Entity en : w.getNearbyEntities(p.getLocation(), 10, 10, 10)) {
                    if (en.equals(p)) continue;
                    UUID own = ownerOf(en);
                    if (own != null && (own.equals(p.getUniqueId()) || isTrusted(p.getUniqueId(), own))) continue;
                    if (en instanceof Player pl && isTrusted(p.getUniqueId(), pl.getUniqueId())) continue;
                    if (en instanceof Mob m) {
                        m.setAI(false);
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            if (m.isValid()) m.setAI(true);
                        }, 80L);
                        frozen++;
                    } else if (en instanceof LivingEntity le) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 9));
                        frozen++;
                    }
                }
                particleRing(p.getLocation().add(0, 0.2, 0), 10, Particle.END_ROD);
                w.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 2f, 0.5f);
                p.sendMessage(ChatColor.GOLD + "Time stops. " + frozen + " frozen.");
            }
            case "bloodstone" -> {
                if (!ready(p, id, 8000)) return;
                Location c = p.getLocation();
                for (Entity en : w.getNearbyEntities(c, 5, 3, 5)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 8, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
                particleRing(c.clone().add(0, 0.2, 0), 5, Particle.DUST);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_CRIT, 2f, 0.6f);
            }
            case "quakehammer" -> {
                if (!ready(p, id, 12000)) return;
                Location c = p.getLocation();
                w.spawnParticle(Particle.CLOUD, c, 80, 4, 0.3, 4, 0.1);
                w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
                for (Entity en : w.getNearbyEntities(c, 6, 3, 6)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 18, p);
                    le.setVelocity(new Vector(0, 0.9, 0));
                }
                for (double r = 2; r <= 6; r += 2) particleRing(c.clone().add(0, 0.2, 0), r, Particle.CLOUD);
            }
            case "venomsickle" -> {
                if (!ready(p, id, 15000)) return;
                final Location cloud = p.getLocation();
                final int[] runs = {0};
                final BukkitTask[] holder = new BukkitTask[1];
                holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
                    runs[0]++;
                    if (runs[0] > 8) {
                        holder[0].cancel();
                        return;
                    }
                    particleRing(cloud.clone().add(0, 0.2, 0), 4, Particle.COMPOSTER);
                    for (Entity en : cloud.getWorld().getNearbyEntities(cloud, 4, 3, 4)) {
                        if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                        venomTouch(le);
                        hit(le, 2, p);
                    }
                }, 0L, 20L);
            }
            case "crystalpick" -> {
                if (!ready(p, id, 6000)) return;
                Vector shardDir = eyeLoc.getDirection().normalize();
                for (int i = -2; i <= 2; i++) {
                    Vector v = shardDir.clone().rotateAroundY(Math.toRadians(i * 5)).multiply(1.8);
                    Snowball shard = p.launchProjectile(Snowball.class, v);
                    shard.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "crystal");
                }
                w.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1f);
            }
            case "rewindcompass" -> {
                Deque<Location> q = posHistory.get(p.getUniqueId());
                if (q == null || q.isEmpty()) {
                    p.sendMessage(ChatColor.RED + "No history yet. Wait a few seconds.");
                    return;
                }
                if (!ready(p, id, 30000)) return;
                Location dest = q.peekFirst().clone();
                dest.setYaw(p.getLocation().getYaw());
                dest.setPitch(p.getLocation().getPitch());
                w.spawnParticle(Particle.REVERSE_PORTAL, p.getLocation().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.1);
                p.teleport(dest);
                w.spawnParticle(Particle.REVERSE_PORTAL, dest.clone().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.1);
                w.playSound(dest, Sound.BLOCK_PORTAL_TRAVEL, 1f, 1.5f);
            }
            case "spoutshell" -> {
                if (!ready(p, id, 12000)) return;
                Location aim = aimPoint(p, 15);
                for (int k = 0; k < 15; k++) {
                    final int step = k;
                    Bukkit.getScheduler().runTaskLater(this, () ->
                            w.spawnParticle(Particle.SPLASH, aim.clone().add(0, step * 0.6, 0), 12, 0.3, 0.2, 0.3, 0.05), k * 2L);
                }
                for (Entity en : w.getNearbyEntities(aim, 3, 4, 3)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 8, p);
                    le.setVelocity(new Vector(0, 2.5, 0));
                }
                w.playSound(aim, Sound.ENTITY_GENERIC_SPLASH, 2f, 0.8f);
            }
            case "honeybomb" -> {
                if (!ready(p, id, 6000)) return;
                Snowball bomb = p.launchProjectile(Snowball.class);
                bomb.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "honey");
                w.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.5f);
            }
            case "cloudbottle" -> {
                if (!ready(p, id, 20000)) return;
                p.setVelocity(new Vector(0, 1.7, 0));
                p.setFallDistance(0f);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0));
                guard(p, 200);
                w.spawnParticle(Particle.CLOUD, p.getLocation(), 60, 0.8, 0.2, 0.8, 0.05);
                w.playSound(p.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1.4f);
            }
            case "meteor" -> {
                if (!ready(p, id, 90000)) return;
                Location aim = aimPoint(p, 40);
                int count = 10 + rnd.nextInt(11);
                int spawned = 0;
                for (int i = 0; i < count; i++) {
                    Location spot = aim.clone().add(rand(-12, 12), 0, rand(-12, 12));
                    try {
                        DragonFireball fb = w.spawn(spot.clone().add(0, 25, 0), DragonFireball.class);
                        fb.setShooter(p);
                        fb.setVelocity(new Vector(0, -1.4, 0));
                        meteorOwners.put(p.getUniqueId(), System.currentTimeMillis());
                        spawned++;
                    } catch (Exception ex) {
                        p.sendMessage(ChatColor.RED + "Meteor could not spawn a fireball: " + ex.getClass().getSimpleName());
                        break;
                    }
                }
                w.playSound(aim, Sound.ENTITY_ENDER_DRAGON_SHOOT, 2f, 1f);
                p.sendMessage(ChatColor.LIGHT_PURPLE + "Meteor: " + spawned + " dragon fireballs incoming.");
            }
            case "hexbone" -> {
                if (!ready(p, id, 25000)) return;
                Location c = p.getLocation();
                w.playSound(c, Sound.AMBIENT_CAVE, 2f, 0.7f);
                final int[] runs = {0};
                final BukkitTask[] holder = new BukkitTask[1];
                holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
                    runs[0]++;
                    if (runs[0] > 15 || !p.isOnline()) {
                        holder[0].cancel();
                        return;
                    }
                    for (Entity en : c.getWorld().getNearbyEntities(c, 6, 3, 6)) {
                        if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                        hit(le, 3, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1));
                    }
                    particleRing(c.clone().add(0, 0.2, 0), 6, Particle.SOUL);
                }, 0L, 20L);
            }
            case "cursedcoin" -> {
                if (!ready(p, id, 15000)) return;
                Location c = p.getLocation();
                int flip = rnd.nextInt(3);
                if (flip == 0) {
                    p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 6));
                    p.sendMessage(ChatColor.GREEN + "The coin lands heads. You are healed.");
                } else if (flip == 1) {
                    for (Entity en : w.getNearbyEntities(c, 8, 4, 8)) {
                        if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                    }
                    p.sendMessage(ChatColor.DARK_GRAY + "The coin lands tails. Nearby enemies are blinded.");
                } else {
                    for (Entity en : w.getNearbyEntities(c, 8, 4, 8)) {
                        if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                    }
                    p.sendMessage(ChatColor.DARK_PURPLE + "The coin wails. Nearby enemies are slowed.");
                }
                w.playSound(c, Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.6f);
            }
            case "spectralquill" -> {
                LivingEntity target = nearestEnemy(p.getLocation(), 15, p);
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "No enemy within 15 blocks.");
                    return;
                }
                if (!ready(p, id, 12000)) return;
                hit(target, 12, p);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0));
                target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
                w.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1.5f, 0.5f);
            }
            case "rottenoffering" -> {
                if (!ready(p, id, 60000)) return;
                for (int i = 0; i < 3; i++) {
                    Zombie z = w.spawn(p.getLocation().add(rand(-2, 2), 0, rand(-2, 2)), Zombie.class);
                    setOwner(z, p.getUniqueId());
                    z.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, "servant");
                    addFollower(z);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (z.isValid()) z.remove();
                    }, 1200L);
                }
                w.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2f, 0.6f);
            }
            case "eyeofthecrypt" -> {
                if (!ready(p, id, 20000)) return;
                Location c = p.getLocation();
                int revealed = 0;
                for (Entity en : w.getNearbyEntities(c, 20, 20, 20)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p)) continue;
                    if (!le.isInvisible() && !le.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0));
                    revealed++;
                }
                w.playSound(c, Sound.AMBIENT_CAVE, 2f, 1f);
                p.sendMessage(ChatColor.DARK_PURPLE + "Eye of the Crypt revealed " + revealed + " hidden creature(s).");
            }
            case "tidal" -> {
                if (!ready(p, id, 4000)) return;
                Snowball orb = p.launchProjectile(Snowball.class);
                orb.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "tidal");
                w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);
            }
            case "reapersigil" -> {
                if (p.isSneaking()) {
                    soulDrain(p);
                    return;
                }
                if (p.isSprinting()) {
                    deathMark(p);
                    return;
                }
                if (!ready(p, id, 6000)) return;
                WitherSkull skull = p.launchProjectile(WitherSkull.class);
                if (skull instanceof Fireball fb) fb.setYield(0f);
                skull.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "reaperskull");
                w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
            }
            case "emberblade" -> {
                Vector fwd = p.getLocation().getDirection().setY(0);
                if (fwd.lengthSquared() < 0.01) return;
                fwd = fwd.normalize();
                if (!ready(p, id, 5000)) return;
                for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.5) continue;
                    hit(le, 8, p);
                    le.setFireTicks(80);
                }
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(fwd.clone().multiply(2)).add(0, 1, 0), 30, 1.5, 0.5, 1.5, 0.05);
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
            }
            case "thornrapier" -> {
                if (!ready(p, id, 6000)) return;
                final Player lp = p;
                Vector dir = p.getLocation().getDirection().normalize();
                p.setVelocity(dir.multiply(1.2).setY(0.1));
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    for (Entity en : lp.getWorld().getNearbyEntities(lp.getLocation(), 2.5, 2.5, 2.5)) {
                        if (!(en instanceof LivingEntity le) || en.equals(lp) || isFriendlyTo(lp, le)) continue;
                        hit(le, 10, lp);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                        break;
                    }
                }, 6L);
            }
            case "moonsplitter" -> {
                if (!ready(p, id, 7000)) return;
                for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 5, 3, 5)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 9, p);
                    Vector out = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0.4);
                    if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(0.9));
                }
                p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1, 0), 20, 3, 0.5, 3, 0);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
            }
            case "frostfang" -> {
                Vector fwd = p.getLocation().getDirection().setY(0);
                if (fwd.lengthSquared() < 0.01) return;
                fwd = fwd.normalize();
                if (!ready(p, id, 9000)) return;
                for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 8, 3, 8)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.6) continue;
                    hit(le, 6, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                }
                p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(fwd.clone().multiply(4)).add(0, 1, 0), 60, 2.5, 0.8, 2.5, 0.05);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
            }
            case "voidcutter" -> {
                if (!ready(p, id, 8000)) return;
                Vector fwd = p.getLocation().getDirection().normalize();
                Location start = p.getLocation().clone();
                Location dest = start.clone().add(fwd.clone().multiply(6));
                if (!isClear(dest)) dest = start;
                p.getWorld().spawnParticle(Particle.PORTAL, start.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.5);
                p.teleport(dest);
                for (Entity en : p.getWorld().getNearbyEntities(dest, 2.5, 2.5, 2.5)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 10, p);
                }
                p.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
            }
            case "bloodletter" -> {
                if (p.getHealth() <= 6) {
                    p.sendMessage(ChatColor.RED + "You need more than 3 hearts to use Bloodletter.");
                    return;
                }
                if (!ready(p, id, 10000)) return;
                p.setHealth(p.getHealth() - 4);
                for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 3, 2, 3)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 12, p);
                }
                p.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, p.getLocation().add(0, 1, 0), 20, 1, 0.8, 1, 0.1);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.7f);
            }
            case "sunder" -> {
                if (!ready(p, id, 6000)) return;
                Location c = p.getLocation().clone();
                for (Entity en : p.getWorld().getNearbyEntities(c, 4, 2, 4)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 7, p);
                    Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0.6);
                    if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(0.7).setY(0.6));
                }
                p.getWorld().spawnParticle(Particle.EXPLOSION, c, 3, 1, 0.2, 1, 0);
                p.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
            }
            case "crazyrolls" -> crazyRolls(p);
            case "gamblersfavourite" -> gamble(p);
            case "voidfang" -> gearDash(p, "voidfang", 2.0, 8, 5000);
            case "solstice" -> gearBurst(p, "solstice", 4.5, 11, 1.3, 9000);
            case "glacierhalberd" -> gearAoe(p, "glacierhalberd", 5, PotionEffectType.SLOWNESS, 60, 3, 4, 10000);
            case "embershort" -> gearCone(p, "embershort", 5, 8, 0, 6000);
            case "runescythe" -> gearCone(p, "runescythe", 6, 7, 80, 7000);
            case "thundercudgel" -> gearChain(p, "thundercudgel", 2, 10, 7, 8000);
            case "duskblade" -> gearAoe(p, "duskblade", 4, PotionEffectType.DARKNESS, 60, 0, 4, 9000);
            case "ironbulwark" -> gearMend(p, "ironbulwark", 3, 0, 11000);
            case "hyperion" -> gearBurst(p, "hyperion", 4, 9, 0.8, 8000);
            case "mistral" -> gearDash(p, "mistral", 1.8, 9, 6000);
            case "obsidianfist" -> gearAoe(p, "obsidianfist", 4, PotionEffectType.SLOWNESS, 40, 2, 4, 9000);
            case "starshard" -> gearAoe(p, "starshard", 10, PotionEffectType.GLOWING, 100, 0, 3, 8000);
            case "cryptbone" -> gearBurst(p, "cryptbone", 3, 6, 0.2, 7000);
            case "tempesthalberd" -> gearBurst(p, "tempesthalberd", 4, 6, 1.4, 8000);
            case "bloodmoon" -> gearMend(p, "bloodmoon", 2, 200, 10000);
            case "prismblade" -> gearBeam(p, "prismblade", 10, 6, 6000);
            case "boomhammer" -> thunderSlam(p);
            case "spearlance" -> sunlance(p);
            case "duskscythe" -> reapingArc(p);
            case "thornwhip" -> lashChain(p);
            case "frostglaive" -> frostZone(p);
            case "ironhalberd" -> cleavingSpin(p);
            case "tidecaller" -> maelstrom(p);
            case "bloodsickle" -> hemorrhage(p);
            case "nagina" -> shadowSlash(p);
            case "chainflail" -> chainWhirl(p);
            case "lanternstaff" -> gearBeam(p, "lanternstaff", 25, 9, 4000);
            case "stonepike" -> bedrockStab(p);
            case "cleaver" -> shatterSlash(p);
            case "wardenclaw" -> sonicClaw(p);
            case "frostlance" -> glacierLance(p);
            case "emberpike" -> emberThrust(p);
            case "stormhalberd" -> stormCleave(p);
            case "hexscythe" -> hexCurse(p);
            case "windpike" -> gustVault(p);
            case "ironmaul" -> seismicDash(p);
            case "ghostglaive" -> phantomStep(p);
            case "dawnhalberd" -> dawnRadiance(p);
            case "tidallance" -> tidalHookPull(p);
            case "bloomstaff" -> bloomBloom(p);
            case "moonblade" -> lunarBlink(p);
            case "echohammer" -> echoShout(p);
            case "gravelglaive" -> gravelWell(p);
            case "eclipsescythe" -> umbralVeil(p);
            case "stormstaff" -> stormPulse(p);
            case "soulsickle" -> soulHarvest(p);
            case "emberglaive" -> emberCrescent(p);
            case "wardenspear" -> sonarLance(p);
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
            case "soulreaver" -> {
                if (!ready(p, id, 12000)) return;
                int drained = 0;
                for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 4, 3, 4)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    hit(le, 12, p);
                    drained++;
                }
                if (drained > 0) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + Math.min(12, drained * 6)));
                p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 40, 2, 1, 2, 0.05);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 1.2f);
            }
            case "stormfang" -> {
                if (!ready(p, id, 9000)) return;
                java.util.List<LivingEntity> foes = new java.util.ArrayList<>();
                for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 8, 4, 8)) {
                    if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) foes.add(le);
                }
                foes.sort(java.util.Comparator.comparingDouble(le -> le.getLocation().distanceSquared(p.getLocation())));
                for (int i = 0; i < Math.min(3, foes.size()); i++) {
                    LivingEntity target = foes.get(i);
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    hit(target, 6, p);
                }
            }
            case "venomtooth" -> {
                if (!ready(p, id, 14000)) return;
                final Player vp = p;
                final Location cloud = p.getLocation().clone();
                for (int t = 0; t < 6; t++) {
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        for (Entity en : cloud.getWorld().getNearbyEntities(cloud, 3, 2, 3)) {
                            if (!(en instanceof LivingEntity le) || en.equals(vp) || isFriendlyTo(vp, le)) continue;
                            le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                            hit(le, 1, vp);
                        }
                        cloud.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, cloud.clone().add(0, 0.5, 0), 25, 2.5, 0.4, 2.5, 0.01);
                    }, t * 20L);
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1f, 0.8f);
            }
            case "bladestorm" -> {
                if (!ready(p, id, 8000)) return;
                final Player bp = p;
                for (int pulse = 0; pulse < 2; pulse++) {
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        for (Entity en : bp.getWorld().getNearbyEntities(bp.getLocation(), 3, 2, 3)) {
                            if (!(en instanceof LivingEntity le) || en.equals(bp) || isFriendlyTo(bp, le)) continue;
                            hit(le, 5, bp);
                        }
                        bp.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bp.getLocation().add(0, 1, 0), 15, 2, 0.5, 2, 0);
                    }, pulse * 10L);
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.9f);
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
                if (!ready(p, id, 1000)) return;
                Vector fwd = p.getLocation().getDirection().setY(0);
                if (fwd.lengthSquared() == 0) fwd = new Vector(0, 0, 1);
                final Vector f = fwd.normalize();
                final Location origin = p.getLocation().clone();
                int hits = 0;
                for (Entity en : w.getNearbyEntities(origin, 8, 4, 8)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                    Vector to = le.getLocation().toVector().subtract(origin.toVector()).setY(0);
                    if (to.lengthSquared() == 0) continue;
                    if (to.normalize().dot(f) > 0.2) {
                        le.damage(20, p);
                        le.setVelocity(f.clone().multiply(0.6).setY(0.3));
                        hits++;
                    }
                }
                for (int k = 0; k < 6; k++) {
                    final int step = k;
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        double ang = Math.toRadians(-80 + step * 32);
                        Vector dir = f.clone().rotateAroundY(ang);
                        for (double r = 2; r <= 8; r += 1.0) {
                            w.spawnParticle(Particle.DRAGON_BREATH, origin.clone().add(0, 1, 0).add(dir.clone().multiply(r)), 3, 0.05, 0.05, 0.05, 0.01);
                        }
                    }, k);
                }
                w.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
                p.sendMessage(ChatColor.DARK_PURPLE + "Void Slasher hit " + hits + " target(s).");
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
            case "grenade" -> {
                boom(loc, 3.5, 12, shooter);
                Bukkit.getScheduler().runTaskLater(this, () -> boom(loc, 3.5, 12, shooter), 20L);
            }
            case "kaboom" -> boom(loc, 4, 12, shooter);
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
            case "shakalaka" -> {
                for (int i = 0; i < 3; i++) {
                    Location spot = loc.clone().add(rand(-2, 2), 0, rand(-2, 2));
                    shakalakaRoll(spot, shooter);
                }
            }
            case "meteor" -> boom(loc, 3, 10, shooter);
            case "blazeball" -> boom(loc, 3.5, 14, shooter);
            case "charge1", "charge2", "charge3" -> {
                int lvl = tag.charAt(6) - '0';
                boom(loc, 1.5 + lvl, 4 + lvl * 4, shooter);
            }
            case "subzero" -> {
                damageArea(loc, 3, 10, shooter);
                w.spawnParticle(Particle.CLOUD, loc, 30, 1, 1, 1, 0.05);
            }
            case "honey" -> {
                for (Entity en : w.getNearbyEntities(loc, 3, 3, 3)) {
                    if (!(en instanceof LivingEntity le) || en.equals(shooter) || isFriendlyTo(shooter, le)) continue;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                    hit(le, 4, shooter);
                }
                w.spawnParticle(Particle.DUST, loc, 40, 1, 0.6, 1, 0, new Particle.DustOptions(Color.fromRGB(0xFFB300), 1.6f));
            }
            case "crystal" -> {
                damageArea(loc, 2, 14, shooter);
                w.spawnParticle(Particle.END_ROD, loc, 12, 0.3, 0.3, 0.3, 0.05);
            }
            case "voidslash" -> {
                damageArea(loc, 3, 14, shooter);
                knockArea(loc, 3, 0.5, shooter);
                w.spawnParticle(Particle.DRAGON_BREATH, loc, 40, 1, 1, 1, 0.05);
            }
            case "reaperskull" -> {
                damageArea(loc, 2.5, 12, shooter);
                for (Entity en : w.getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
                    if (en instanceof LivingEntity wl && !en.equals(shooter)) {
                        wl.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                    }
                }
                w.spawnParticle(Particle.SMOKE, loc, 30, 0.8, 0.8, 0.8, 0.05);
            }
            case "gust" -> {
                damageArea(loc, 3, 5, shooter);
                knockArea(loc, 3, 0.7, shooter);
                particleRing(loc.clone().add(0, 0.5, 0), 2.0, Particle.CLOUD);
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
        if (id.equals("cursedcrown") && id.equals(idOf(p.getInventory().getHelmet()))) return true;
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
                "Right-click: Night Terror. Shift+right-click: Nightmare Punishment (5s). F: Abyssal Sight. Shift+F: Nightmare Phantom (ultimate). Passives: tame bats and creakings with Echo Shards, Echo Location.");
        gemDef("thunder", "Thunder Master Gem", Material.AMETHYST_SHARD, "#FFEB3B", "Right-click: Thunder Strike (lightning at your aim, 12 damage and a stun). Shift+right-click: Chain Lightning (up to 5 foes). F: Overcharge (next 3 hits +10 damage). Shift+F: Zeus (ultimate, 20 seconds of lightning storm). Passives: 20% lightning on hit, immune to lightning, slowness and weakness.");
        gemDef("wind", "Wind Master Gem", Material.AMETHYST_SHARD, "#B2EBF2", "Right-click: Gust (pushes and damages nearby foes). Shift+right-click: Updraft (launch yourself up, no fall damage). F: Tornado (orbits foes within 12 blocks). Sprint+F: Berserk Breeze (ultimate, 30 seconds of gusts on every right-click). Passives: no fall damage, stronger bow, 5% chance to launch players on hit.");
        gemDef("inferno", "Inferno Gem", Material.AMETHYST_SHARD, "#FF6D00",
                "Right-click: Meteor Shower. Shift+right-click: Cozy Campfire. F: Chargeable Fireball (press F to start charging, F again to release, charge rises to 100% then drains). Shift+F: Blaze (ultimate, explosive fireballs with no cooldown for 30 seconds). Sprint+F: Evaporation (toggle, water around you turns to air). Passives: fire immunity, fire damage on swords.");
        gemDef("atomic", "Atomic Gem", Material.AMETHYST_SHARD, "#76FF03",
                "Right-click: Atomic Blast (shockwave, heavy damage and knockback). Shift+right-click: Radiation Cloud (15s wither and nausea nearby). F: Fallout Shield (10s, half damage, attackers blinded). Shift+F: Meltdown (ultimate, 20s glow, radiation damage nearby). Passives: immune to wither and poison, slow regeneration.");
        gemDef("warden", "Warden Keeper Gem", Material.AMETHYST_SHARD, "#0D47A1", "Right-click: Sonic Wave (a cone sonic boom). Shift+right-click: Echo Pulse (reveals enemies). F: Warden's Fist (warden-strength slam). Shift+F: Warden Monster (ultimate, a warden ally for 5 minutes). Passives: immune to fire, lava, falls, poison, wither, blindness, darkness, slowness and nausea.");
        gemDef("yinyang", "Yin Yang Gem", Material.AMETHYST_SHARD, "#EEEEEE", "Right-click: Yin Strike (heal you, damage nearby). Shift+right-click: Yang Strike (burn nearby, hurt you). F: Balance (swap health with nearest enemy for 5 seconds). Shift+F: Taijitu (ultimate, 60-second ring that pushes enemies and heals allies). Passives: half of damage taken heals you back, slow regeneration.");
        gemDef("eclipse", "Solar Eclipse Gem", Material.AMETHYST_SHARD, "#4A148C", "Right-click: Eclipse Pulse (12 damage, blind, slow and weaken nearby). Shift+right-click: Shadow Lunge (teleport behind the nearest enemy, 15 damage). F: Eclipse Ward (10 hearts of absorption, attackers blinded, 25% of their damage reflected). Shift+F: Total Eclipse (ultimate, 30 seconds of darkness, invisibility and Strength II). Passives: blocks darkness and blindness, takes 15% less damage at night, dark aura. The Light Before the Dark: below 5 hearts you get Resistance, a black campfire aura and slow healing.");
        gemDef("undead", "Undead Legion Gem", Material.AMETHYST_SHARD, "#A5D6A7",
                "Right-click: Wither Legion. Shift+right-click: Fallen Recall. F: Wither Touch. Shift+F: Giant Wither Skeleton (ultimate). Passives: heal and extra damage vs undead.");
        gemDef("tide", "Tidal Gem", Material.AMETHYST_SHARD, "#0097A7", "Right-click: Tidal Wave (8 damage and a push in a cone). Shift+right-click: Whirlpool (pulls and slows nearby foes). F: Riptide Dash (dash forward and hit the first foe for 6). Shift+F: Kraken Call (3 lightning strikes for 10 damage each). Passive: Soaked: slows the target for 1 second on hit.");
        gemDef("stonefist", "Stone Fist Gem", Material.AMETHYST_SHARD, "#8D6E63", "Right-click: Quake Stomp (6 damage and a knock-up around you). Shift+right-click: Stone Armor (4 hearts of absorption). F: Boulder Throw (10 damage to the first foe you look at). Shift+F: Titan Form (20 hearts of absorption and Strength II). Passive: Quake Knuckles: 1 extra damage on hit.");
        gemDef("spectral", "Spectral Gem", Material.AMETHYST_SHARD, "#B39DDB", "Right-click: Haunt Pulse (5 damage and glowing around you). Shift+right-click: Phase Walk (invisible and fast). F: Banshee Wail (blindness and nausea nearby). Shift+F: Reaper's Harvest (10 damage to up to 5 foes, healing you 1 heart per hit). Passive: Phantom Touch: the target glows for 2 seconds on hit.");
        gemDef("verdant", "Verdant Gem", Material.AMETHYST_SHARD, "#66BB6A", "Right-click: Vine Snare (4 damage and roots foes in front). Shift+right-click: Bloom Heal (4 hearts and regeneration). F: Thorn Burst (6 damage to foes within 4 blocks). Shift+F: Living Forest (regeneration, absorption and speed). Passive: Living Bark: you regenerate for 2 seconds on hit.");
        gemDef("gravity", "Gravity Gem", Material.AMETHYST_SHARD, "#5C6BC0", "Right-click: Gravity Pull (drags foes within 8 blocks toward you). Shift+right-click: Gravity Crush (8 damage and slow within 4 blocks). F: Feather Float (slow falling). Shift+F: Singularity (pulls and damages foes within 10 blocks for 3 seconds). Passive: Crushing Weight: slows the target for 1 second on hit.");
    }

    private void gemDef(String id, String name, Material mat, String hex, String lore) {
        gems.put(id, new ItemDef(id, name, mat, hex, lore));
        modelIds.put(id, 2000 + gems.size());
    }

    private boolean isActive(Map<UUID, Long> map, Player p) {
        Long until = map.get(p.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private String gemOf(Player p) {
        String main = idOf(p.getInventory().getItemInMainHand());
        if (main != null && gems.containsKey(main)) return main;
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

    private boolean handHasItem(PlayerInteractEntityEvent e) {
        ItemStack hand = e.getPlayer().getInventory().getItem(e.getHand());
        return hand != null && !hand.getType().isAir();
    }

    private boolean isMount(Entity en) {
        String t = en.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING);
        return "giantmob".equals(t) || "giantparrot".equals(t) || "giantaxo".equals(t);
    }

    @EventHandler
    public void onMountNoSuffocate(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.SUFFOCATION) return;
        Entity v = e.getEntity();
        if (isMount(v) || (v instanceof Player pl && pl.getVehicle() != null && isMount(pl.getVehicle()))) {
            e.setCancelled(true);
        }
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
            case "eclipse" -> {
                if (shift) shadowStep(p);
                else voidPulse(p);
            }
            case "warden" -> {
                if (shift) echoPulse(p);
                else sonicWave(p);
            }
            case "yinyang" -> {
                if (shift) yangStrike(p);
                else yinStrike(p);
            }
            case "tide" -> {
                if (shift) whirlpool(p);
                else tidalWave(p);
            }
            case "stonefist" -> {
                if (shift) stoneArmor(p);
                else quakeStomp(p);
            }
            case "spectral" -> {
                if (shift) phaseWalk(p);
                else hauntPulse(p);
            }
            case "verdant" -> {
                if (shift) bloomHeal(p);
                else vineSnare(p);
            }
            case "gravity" -> {
                if (shift) gravityCrush(p);
                else gravityPull(p);
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
        try {
            onGemFInner(e);
            String g = gemOf(p);
            if (g != null && GEM_ABILITIES.containsKey(g) && !charging.containsKey(p.getUniqueId())) {
                boolean ult = g.equals("wind") ? p.isSprinting() : p.isSneaking();
                List<String> ab = GEM_ABILITIES.get(g);
                long left = cdLeft(p, ult ? ab.get(3) : ab.get(2));
                if (left > 0) p.sendTitle("", ChatColor.RED + "Cooldown " + ((left / 1000) + 1) + "s", 0, 25, 5);
            }
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "F key error", t);
            p.sendMessage(ChatColor.RED + "F key error: " + t);
        }
    }

    private void onGemFInner(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        String gem = gemOf(p);
        if (gem == null) return;
        e.setCancelled(true);
        boolean shift = p.isSneaking();
        boolean sprint = p.isSprinting();
        boolean ult = gem.equals("wind") ? sprint : shift;
        dbg(p, "F press gem=" + gem + " ult=" + ult + " sneaking=" + shift + " sprinting=" + sprint);
        if (ult) {
            if (!ultGate(p)) return;
            switch (gem) {
                case "blizzard" -> subzero(p);
                case "thunder" -> zeus(p);
                case "nightmare" -> giantCreaking(p);
                case "undead" -> giantWither(p);
                case "wind" -> windBerserk(p);
                case "inferno" -> blazeMode(p);
                case "eclipse" -> totalEclipse(p);
                case "warden" -> wardenMonster(p);
                case "yinyang" -> taijitu(p);
                case "tide" -> krakenCall(p);
                case "stonefist" -> titanForm(p);
                case "spectral" -> reaperHarvest(p);
                case "verdant" -> livingForest(p);
                case "gravity" -> singularity(p);
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
            case "eclipse" -> umbralShield(p);
            case "warden" -> wardenFist(p);
            case "yinyang" -> balance(p);
            case "tide" -> riptide(p);
            case "stonefist" -> boulderThrow(p);
            case "spectral" -> bansheeWail(p);
            case "verdant" -> thornBurst(p);
            case "gravity" -> featherFloat(p);
            case "atomic" -> falloutShield(p);
            default -> { }
        }
    }

    // ----- Tide -----

    private void tidalWave(Player p) {
        if (!ready(p, "g_wave", 4000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        fwd = fwd.normalize();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 8, 3, 8)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.5) continue;
            hit(le, 8, p);
            le.setVelocity(fwd.clone().multiply(0.8).setY(0.3));
        }
        p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation().add(fwd.clone().multiply(3)).add(0, 1, 0), 60, 2, 1, 2, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 0.8f);
    }

    private void whirlpool(Player p) {
        if (!ready(p, "g_whirl", 8000)) return;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector pull = p.getLocation().toVector().subtract(le.getLocation().toVector()).setY(0.2);
            if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(1.0));
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
        }
        p.getWorld().spawnParticle(Particle.BUBBLE_POP, p.getLocation().add(0, 1, 0), 50, 3, 1, 3, 0.2);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SWIM, 1f, 0.8f);
    }

    private void riptide(Player p) {
        if (!ready(p, "g_riptide", 6000)) return;
        Vector dir = p.getLocation().getDirection().normalize();
        p.setVelocity(dir.clone().multiply(1.6).setY(0.1));
        final Player rp = p;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Entity en : rp.getWorld().getNearbyEntities(rp.getLocation(), 2.5, 2.5, 2.5)) {
                if (!(en instanceof LivingEntity le) || en.equals(rp) || isFriendlyTo(rp, le)) continue;
                hit(le, 6, rp);
                break;
            }
        }, 8L);
    }

    private void krakenCall(Player p) {
        if (!ready(p, "g_kraken", 180000)) return;
        List<LivingEntity> foes = new ArrayList<>();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 12, 6, 12)) {
            if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) foes.add(le);
        }
        Collections.shuffle(foes);
        int strikes = Math.min(3, foes.size());
        for (int i = 0; i < strikes; i++) {
            LivingEntity t = foes.get(i);
            t.getWorld().strikeLightningEffect(t.getLocation());
            hit(t, 10, p);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.8f);
        p.sendMessage(ChatColor.AQUA + "Kraken Call: " + strikes + " strike(s).");
    }

    // ----- Stone Fist -----

    private void quakeStomp(Player p) {
        if (!ready(p, "g_quake", 6000)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, 4, 2, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 6, p);
            le.setVelocity(new Vector(0, 0.7, 0));
        }
        c.getWorld().spawnParticle(Particle.EXPLOSION, c, 3, 1, 0.2, 1, 0);
        c.getWorld().playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 1.5f, 0.6f);
    }

    private void stoneArmor(Player p) {
        if (!ready(p, "g_armor", 15000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1.5f, 0.6f);
        p.sendMessage(ChatColor.GRAY + "Stone Armor: 4 hearts of absorption for 10 seconds.");
    }

    private void boulderThrow(Player p) {
        if (!ready(p, "g_boulder", 7000)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        for (double d = 1; d <= 12; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            boolean done = false;
            for (Entity en : point.getWorld().getNearbyEntities(point, 0.8, 0.8, 0.8)) {
                if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                hit(le, 10, p);
                done = true;
                break;
            }
            if (done) break;
        }
        p.getWorld().spawnParticle(Particle.CRIT, eye.clone().add(dir.clone().multiply(4)), 20, 0.5, 0.5, 0.5, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.7f);
    }

    private void titanForm(Player p) {
        if (!ready(p, "g_titan", 120000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 9));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.7f);
        p.sendMessage(ChatColor.GOLD + "Titan Form: 20 hearts of absorption and Strength II for 15 seconds.");
    }

    // ----- Spectral -----

    private void hauntPulse(Player p) {
        if (!ready(p, "g_haunt", 5000)) return;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 5, 3, 5)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 5, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
        }
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 40, 2.5, 1, 2.5, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1f, 0.8f);
    }

    private void phaseWalk(Player p) {
        if (!ready(p, "g_phase", 12000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1));
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.5);
        p.sendMessage(ChatColor.DARK_GRAY + "Phase Walk: invisible and fast for 4 seconds.");
    }

    private void bansheeWail(Player p) {
        if (!ready(p, "g_wail", 10000)) return;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 8, 4, 8)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1f, 0.7f);
    }

    private void reaperHarvest(Player p) {
        if (!ready(p, "g_reaper", 60000)) return;
        int hits = 0;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (hits >= 5) break;
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 10, p);
            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
            hits++;
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
        p.sendMessage(ChatColor.DARK_PURPLE + "Reaper's Harvest: " + hits + " foe(s).");
    }

    // ----- Verdant -----

    private void vineSnare(Player p) {
        if (!ready(p, "g_vine", 8000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        fwd = fwd.normalize();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.6) continue;
            hit(le, 4, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9));
        }
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(fwd.clone().multiply(3)).add(0, 1, 0), 30, 2, 1, 2, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 0.8f);
    }

    private void bloomHeal(Player p) {
        if (!ready(p, "g_bloom", 15000)) return;
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 8));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 1.2f);
    }

    private void thornBurst(Player p) {
        if (!ready(p, "g_thorn", 8000)) return;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 4, 2, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 6, p);
        }
        p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 40, 2, 1, 2, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BAMBOO_BREAK, 1f, 0.8f);
    }

    private void livingForest(Player p) {
        if (!ready(p, "g_forest", 150000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GRASS_BREAK, 2f, 0.6f);
        p.sendMessage(ChatColor.GREEN + "Living Forest: regeneration, absorption and speed for 15 seconds.");
    }

    // ----- Gravity -----

    private void gravityPull(Player p) {
        if (!ready(p, "g_pull", 6000)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, 8, 4, 8)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector pull = c.toVector().subtract(le.getLocation().toVector()).setY(0.2);
            if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(1.2));
        }
        c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(0, 1, 0), 60, 3, 1, 3, 0.4);
        c.getWorld().playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
    }

    private void gravityCrush(Player p) {
        if (!ready(p, "g_crush", 9000)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, 4, 2, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 8, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
        }
        c.getWorld().spawnParticle(Particle.EXPLOSION, c, 2, 1, 0.2, 1, 0);
        c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);
    }

    private void featherFloat(Player p) {
        if (!ready(p, "g_float", 15000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0));
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 30, 0.6, 0.3, 0.6, 0.02);
        p.sendMessage(ChatColor.GRAY + "Feather Float: slow falling for 10 seconds.");
    }

    private void singularity(Player p) {
        if (!ready(p, "g_singularity", 150000)) return;
        final Location c = p.getLocation().clone();
        final int[] ticks = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            ticks[0]++;
            if (ticks[0] > 60 || !p.isOnline()) {
                holder[0].cancel();
                return;
            }
            for (Entity en : c.getWorld().getNearbyEntities(c, 10, 6, 10)) {
                if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                Vector pull = c.toVector().subtract(le.getLocation().toVector());
                if (pull.lengthSquared() > 0.5) le.setVelocity(pull.normalize().multiply(0.5));
                if (ticks[0] % 10 == 0) hit(le, 2, p);
            }
            c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(0, 1, 0), 30, 2, 1, 2, 0.5);
        }, 0L, 1L);
        p.sendMessage(ChatColor.DARK_PURPLE + "Singularity: pulling foes in for 3 seconds.");
    }

    // ----- Melee and staff abilities (no bows or crossbows) -----

    private void gearBurst(Player p, String id, double radius, double dmg, double kb, long cd) {
        if (!ready(p, id, cd)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, radius, 2.5, radius)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, dmg, p);
            Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0.4);
            if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(kb));
        }
        c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 40, radius / 2, 0.5, radius / 2, 0.1);
        c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
    }

    private void gearCone(Player p, String id, double range, double dmg, int slowTicks, long cd) {
        if (!ready(p, id, cd)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        fwd = fwd.normalize();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), range, 3, range)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.55) continue;
            hit(le, dmg, p);
            if (slowTicks > 0) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, 1));
        }
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(fwd.clone().multiply(2)).add(0, 1, 0), 12, 1.5, 0.5, 1.5, 0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
    }

    private void gearDash(Player p, String id, double speed, double dmg, long cd) {
        if (!ready(p, id, cd)) return;
        Vector dir = p.getLocation().getDirection().normalize();
        p.setVelocity(dir.clone().multiply(speed).setY(0.1));
        final Player dp = p;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Entity en : dp.getWorld().getNearbyEntities(dp.getLocation(), 2.5, 2.5, 2.5)) {
                if (!(en instanceof LivingEntity le) || en.equals(dp) || isFriendlyTo(dp, le)) continue;
                hit(le, dmg, dp);
                break;
            }
        }, 6L);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.2f);
    }

    private void gearAoe(Player p, String id, double radius, PotionEffectType type, int ticks, int amp, double dmg, long cd) {
        if (!ready(p, id, cd)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, radius, 3, radius)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            if (dmg > 0) hit(le, dmg, p);
            if (type != null) le.addPotionEffect(new PotionEffect(type, ticks, amp));
        }
        c.getWorld().spawnParticle(Particle.WITCH, c.clone().add(0, 1, 0), 40, radius / 2, 0.5, radius / 2, 0.02);
        c.getWorld().playSound(c, Sound.ENTITY_WITCH_AMBIENT, 1f, 0.9f);
    }

    private void gearChain(Player p, String id, int count, double range, double dmg, long cd) {
        if (!ready(p, id, cd)) return;
        List<LivingEntity> foes = new ArrayList<>();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), range, 4, range)) {
            if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) foes.add(le);
        }
        foes.sort(java.util.Comparator.comparingDouble(le -> le.getLocation().distanceSquared(p.getLocation())));
        int n = Math.min(count, foes.size());
        for (int i = 0; i < n; i++) {
            LivingEntity t = foes.get(i);
            t.getWorld().strikeLightningEffect(t.getLocation());
            hit(t, dmg, p);
        }
        if (n == 0) p.sendMessage(ChatColor.GRAY + "No foe in range.");
    }

    private void gearPull(Player p, String id, double radius, double strength, long cd) {
        if (!ready(p, id, cd)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, radius, 4, radius)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector pull = c.toVector().subtract(le.getLocation().toVector()).setY(0.2);
            if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(strength));
        }
        c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(0, 1, 0), 60, 3, 1, 3, 0.4);
        c.getWorld().playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
    }

    private void gearBuff(Player p, String id, PotionEffectType type, int ticks, int amp, long cd) {
        if (!ready(p, id, cd)) return;
        p.addPotionEffect(new PotionEffect(type, ticks, amp));
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.5);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
    }

    private void gearMend(Player p, String id, double hearts, int regenTicks, long cd) {
        if (!ready(p, id, cd)) return;
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + hearts * 2));
        if (regenTicks > 0) p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenTicks, 1));
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void gearBeam(Player p, String id, double range, double dmg, long cd) {
        if (!ready(p, id, cd)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        LivingEntity target = null;
        double reach = range;
        for (double d = 1; d <= range && target == null; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            for (Entity en : point.getWorld().getNearbyEntities(point, 0.7, 0.7, 0.7)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) {
                    target = le;
                    reach = d;
                    break;
                }
            }
        }
        for (double d = 0.5; d <= reach; d += 0.5) {
            p.getWorld().spawnParticle(Particle.END_ROD, eye.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0);
        }
        if (target != null) hit(target, dmg, p);
        else p.sendMessage(ChatColor.GRAY + "The beam found no foe.");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.6f);
    }

    // ----- Unique weapon abilities and passives (batch 1) -----

    private boolean inPassive = false;

    static final double ABILITY_DAMAGE_MULT = 1.25;

    private static final String[] ABILITY_ITEM_IDS = {"unoreverse", "boomhammer", "spearlance", "duskscythe", "thornwhip", "frostglaive", "ironhalberd", "tidecaller", "bloodsickle", "nagina", "chainflail", "lanternstaff", "stonepike", "cleaver", "wardenclaw", "frostlance", "emberpike", "stormhalberd", "hexscythe", "windpike", "ironmaul", "ghostglaive", "dawnhalberd", "tidallance", "bloomstaff", "moonblade", "echohammer", "gravelglaive", "eclipsescythe", "stormstaff", "soulsickle", "emberglaive", "wardenspear", "hyperion", "mistral", "obsidianfist", "starshard", "cryptbone", "tempesthalberd", "bloodmoon", "prismblade", "voidfang", "solstice", "glacierhalberd", "embershort", "runescythe", "thundercudgel", "duskblade", "ironbulwark"};
    private static final String[] GEM_PASSIVE_IDS = {"tide", "stonefist", "spectral", "verdant", "gravity", "blizzard", "nightmare", "thunder", "wind", "inferno", "atomic", "warden", "yinyang", "eclipse", "undead"};
    private static final java.util.List<String> NEW_GEM_PASSIVES = java.util.List.of("tide", "stonefist", "spectral", "verdant", "gravity");

    /** Runs the ability of any single-ability weapon by its id. Used by CrazyRolls. */
    private void runAbilityFor(Player p, String id) {
        switch (id) {
            case "unoreverse" -> releaseProjectile(p);
            case "boomhammer" -> thunderSlam(p);
            case "spearlance" -> sunlance(p);
            case "duskscythe" -> reapingArc(p);
            case "thornwhip" -> lashChain(p);
            case "frostglaive" -> frostZone(p);
            case "ironhalberd" -> cleavingSpin(p);
            case "tidecaller" -> maelstrom(p);
            case "bloodsickle" -> hemorrhage(p);
            case "nagina" -> shadowSlash(p);
            case "chainflail" -> chainWhirl(p);
            case "lanternstaff" -> gearBeam(p, "lanternstaff", 25, 9, 4000);
            case "stonepike" -> bedrockStab(p);
            case "cleaver" -> shatterSlash(p);
            case "wardenclaw" -> sonicClaw(p);
            case "frostlance" -> glacierLance(p);
            case "emberpike" -> emberThrust(p);
            case "stormhalberd" -> stormCleave(p);
            case "hexscythe" -> hexCurse(p);
            case "windpike" -> gustVault(p);
            case "ironmaul" -> seismicDash(p);
            case "ghostglaive" -> phantomStep(p);
            case "dawnhalberd" -> dawnRadiance(p);
            case "tidallance" -> tidalHookPull(p);
            case "bloomstaff" -> bloomBloom(p);
            case "moonblade" -> lunarBlink(p);
            case "echohammer" -> echoShout(p);
            case "gravelglaive" -> gravelWell(p);
            case "eclipsescythe" -> umbralVeil(p);
            case "stormstaff" -> stormPulse(p);
            case "soulsickle" -> soulHarvest(p);
            case "emberglaive" -> emberCrescent(p);
            case "wardenspear" -> sonarLance(p);
            default -> { }
        }
    }

    private void applyGemPassive(Player p, LivingEntity le, String gem) {
        switch (gem) {
            case "tide" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
            case "stonefist" -> hit(le, 1, p);
            case "spectral" -> le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0));
            case "verdant" -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0));
            case "gravity" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
            case "blizzard" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
            case "nightmare" -> le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
            case "thunder" -> {
                if (rnd.nextDouble() < 0.1) {
                    le.getWorld().strikeLightningEffect(le.getLocation());
                    hit(le, 2, p);
                }
            }
            case "wind" -> le.setVelocity(new Vector(0, 0.5, 0));
            case "inferno" -> le.setFireTicks(40);
            case "atomic" -> hit(le, 1, p);
            case "warden" -> le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
            case "yinyang" -> p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1));
            case "eclipse" -> le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 0));
            case "undead" -> le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
            default -> { }
        }
    }


    private static final String[] RANDOM_PASSIVES = {"boomhammer", "spearlance", "duskscythe", "thornwhip",
            "frostglaive", "ironhalberd", "tidecaller", "bloodsickle", "frostlance", "stormhalberd", "hexscythe",
            "ironmaul", "ghostglaive", "dawnhalberd", "moonblade", "soulsickle", "emberpike", "windpike", "tidallance",
            "gravelglaive", "echohammer", "eclipsescythe", "stormstaff", "emberglaive", "wardenspear", "bloomstaff",
            "cleaver", "wardenclaw", "stonepike", "chainflail", "nagina", "lanternstaff", "hyperion", "mistral", "obsidianfist", "starshard", "cryptbone", "tempesthalberd", "bloodmoon", "prismblade", "voidfang", "solstice", "glacierhalberd", "embershort", "runescythe", "thundercudgel", "duskblade", "ironbulwark"};

    private final Map<UUID, Integer> chargeCount = new HashMap<>();
    private final Map<UUID, Integer> comboCount = new HashMap<>();

    private final Map<UUID, Integer> hitCount = new HashMap<>();
    private final Map<UUID, Long> phantomUntil = new HashMap<>();

    private boolean undeadMob(LivingEntity le) {
        EntityType t = le.getType();
        return t == EntityType.ZOMBIE || t == EntityType.ZOMBIE_VILLAGER || t == EntityType.HUSK || t == EntityType.DROWNED
                || t == EntityType.SKELETON || t == EntityType.STRAY || t == EntityType.WITHER_SKELETON || t == EntityType.PHANTOM;
    }

    /** CrazyRolls: one random ability from the batch-one arsenal per right-click. */
    private void crazyRolls(Player p) {
        int pick = rnd.nextInt(ABILITY_ITEM_IDS.length + GEM_PASSIVE_IDS.length);
        if (pick < ABILITY_ITEM_IDS.length) runAbilityFor(p, ABILITY_ITEM_IDS[pick]);
        else handleGemRight(p, GEM_PASSIVE_IDS[pick - ABILITY_ITEM_IDS.length]);
    }

    private void thunderSlam(Player p) {
        if (!ready(p, "boomhammer", 9000)) return;
        p.setVelocity(new Vector(0, 1.0, 0));
        final Player sp = p;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Location c = sp.getLocation().clone();
            for (Entity en : c.getWorld().getNearbyEntities(c, 5, 3, 5)) {
                if (!(en instanceof LivingEntity le) || en.equals(sp) || isFriendlyTo(sp, le)) continue;
                hit(le, 14, sp);
                Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0.6);
                if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(1.2));
            }
            c.getWorld().spawnParticle(Particle.EXPLOSION, c, 3, 1, 0.2, 1, 0);
            c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        }, 20L);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1f, 0.8f);
    }

    private void sunlance(Player p) {
        if (!ready(p, "spearlance", 8000)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Set<LivingEntity> struck = new HashSet<>();
        for (double d = 0.5; d <= 8; d += 0.5) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            p.getWorld().spawnParticle(Particle.END_ROD, pt, 1, 0, 0, 0, 0);
            for (Entity en : pt.getWorld().getNearbyEntities(pt, 1, 1, 1)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le) && struck.add(le)) {
                    hit(le, 6, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                }
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.6f);
    }

    private void reapingArc(Player p) {
        if (!ready(p, "duskscythe", 6000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        fwd = fwd.normalize();
        List<LivingEntity> victims = new ArrayList<>();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.5) continue;
            victims.add(le);
        }
        for (LivingEntity v : victims) {
            Vector pull = p.getLocation().toVector().subtract(v.getLocation().toVector()).setY(0.1);
            if (pull.lengthSquared() > 0) v.setVelocity(pull.normalize().multiply(1.1));
        }
        final Player sp = p;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (LivingEntity v : victims) {
                if (v.isValid() && !v.isDead()) hit(v, 4, sp);
            }
        }, 8L);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.4f);
    }

    private void lashChain(Player p) {
        if (!ready(p, "thornwhip", 7000)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        LivingEntity target = null;
        for (double d = 0.5; d <= 8 && target == null; d += 0.5) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            for (Entity en : pt.getWorld().getNearbyEntities(pt, 0.8, 0.8, 0.8)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) {
                    target = le;
                    break;
                }
            }
        }
        if (target == null) {
            p.sendMessage(ChatColor.GRAY + "Lash Chain found no foe.");
            return;
        }
        hit(target, 6, p);
        Vector pull = p.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.2);
        if (pull.lengthSquared() > 0) target.setVelocity(pull.normalize().multiply(1.3));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.7f);
    }

    private void frostZone(Player p) {
        if (!ready(p, "frostglaive", 7000)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, 4, 3, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 2, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3));
        }
        c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 60, 3, 0.8, 3, 0.05);
        c.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
    }

    private void cleavingSpin(Player p) {
        if (!ready(p, "ironhalberd", 5000)) return;
        final Player sp = p;
        for (int i = 0; i < 4; i++) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!sp.isOnline()) return;
                Location c = sp.getLocation().clone();
                for (Entity en : c.getWorld().getNearbyEntities(c, 3, 2, 3)) {
                    if (!(en instanceof LivingEntity le) || en.equals(sp) || isFriendlyTo(sp, le)) continue;
                    hit(le, 3, sp);
                }
                c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(0, 1, 0), 10, 2, 0.5, 2, 0);
            }, i * 5L);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.9f);
    }

    private void maelstrom(Player p) {
        if (!ready(p, "tidecaller", 8000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        Location point = p.getLocation().clone().add(fwd.normalize().multiply(6));
        for (Entity en : point.getWorld().getNearbyEntities(point, 4, 3, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 5, p);
            Vector pull = point.toVector().subtract(le.getLocation().toVector()).setY(0.1);
            if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(1.0));
        }
        point.getWorld().spawnParticle(Particle.BUBBLE_POP, point.clone().add(0, 1, 0), 80, 3, 1, 3, 0.2);
        point.getWorld().playSound(point, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.7f);
    }

    private void hemorrhage(Player p) {
        if (!ready(p, "bloodsickle", 7000)) return;
        LivingEntity target = null;
        double best = 5;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 5, 3, 5)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            double d = le.getLocation().distance(p.getLocation());
            if (d < best) {
                best = d;
                target = le;
            }
        }
        if (target == null) {
            p.sendMessage(ChatColor.GRAY + "Hemorrhage found no foe.");
            return;
        }
        final LivingEntity victim = target;
        final Player sp = p;
        for (int i = 1; i <= 3; i++) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!victim.isValid() || victim.isDead()) return;
                victim.damage(1);
                if (sp.isOnline()) sp.setHealth(Math.min(sp.getMaxHealth(), sp.getHealth() + 2));
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
            }, i * 20L);
        }
    }

    /** One passive per weapon, on melee hits only. */
    /** Weapons hit harder: a melee hit with any held weapon deals 50% more damage. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponDamage(EntityDamageByEntityEvent e) {
        if (inPassive || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player p)) return;
        if (idOf(p.getInventory().getItemInMainHand()) == null) return;
        e.setDamage(e.getDamage() * 1.5);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeaponPassive(EntityDamageByEntityEvent e) {
        if (inPassive || (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) return;
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity le)) return;
        if (le.equals(p) || isFriendlyTo(p, le)) return;
        String id = idOf(p.getInventory().getItemInMainHand());
        if (id == null) return;
        inPassive = true;
        try {
            String passive = id.equals("crazyrolls") ? RANDOM_PASSIVES[rnd.nextInt(RANDOM_PASSIVES.length)] : id;
            switch (passive) {
                case "boomhammer" -> {
                    if (rnd.nextDouble() < 0.25) le.setVelocity(new Vector(0, 0.8, 0));
                }
                case "spearlance" -> {
                    for (int i = 1; i <= 3; i++) {
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            if (le.isValid() && !le.isDead()) le.damage(1);
                        }, i * 20L);
                    }
                }
                case "duskscythe" -> p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                case "thornwhip" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                case "frostglaive" -> {
                    le.setFireTicks(0);
                    hit(le, 1, p);
                }
                case "ironhalberd" -> {
                    for (Entity en : le.getWorld().getNearbyEntities(le.getLocation(), 2, 2, 2)) {
                        if (en instanceof LivingEntity other && !en.equals(le) && !en.equals(p) && !isFriendlyTo(p, other)) {
                            hit(other, 2, p);
                            break;
                        }
                    }
                }
                case "tidecaller" -> {
                    Vector out = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0.1);
                    if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(0.8));
                }
                case "bloodsickle" -> {
                    if (le instanceof Player) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                }
                case "frostlance" -> {
                    if (rnd.nextDouble() < 0.2) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 9));
                }
                case "stormhalberd" -> {
                    if (rnd.nextDouble() < 0.15) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        hit(le, 3, p);
                    }
                }
                case "hexscythe" -> le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                case "ironmaul" -> {
                    int count = hitCount.merge(p.getUniqueId(), 1, Integer::sum);
                    if (count % 5 == 0) hit(le, 6, p);
                }
                case "ghostglaive" -> {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                    Long until = phantomUntil.get(p.getUniqueId());
                    if (until != null && System.currentTimeMillis() < until) {
                        phantomUntil.remove(p.getUniqueId());
                        hit(le, 6, p);
                    }
                }
                case "dawnhalberd" -> {
                    if (undeadMob(le)) hit(le, 3, p);
                }
                case "moonblade" -> {
                    long time = p.getWorld().getTime();
                    if (time >= 13000 && time < 23000) hit(le, 2, p);
                }
                case "soulsickle" -> Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (le.isDead() && p.isOnline()) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 4));
                }, 1L);
                case "emberpike" -> le.setFireTicks(60);
                case "windpike" -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0));
                case "tidallance" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2));
                case "gravelglaive" -> {
                    hit(le, 1, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
                }
                case "echohammer" -> {
                    LivingEntity nearest = null;
                    double best = 3;
                    for (Entity en : le.getWorld().getNearbyEntities(le.getLocation(), 3, 3, 3)) {
                        if (en instanceof LivingEntity other && !en.equals(le) && !en.equals(p) && !isFriendlyTo(p, other)) {
                            double d = other.getLocation().distance(le.getLocation());
                            if (d < best) {
                                best = d;
                                nearest = other;
                            }
                        }
                    }
                    if (nearest != null) hit(nearest, 2, p);
                }
                case "eclipsescythe" -> le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                case "stormstaff" -> {
                    int count = chargeCount.merge(p.getUniqueId(), 1, Integer::sum);
                    if (count % 4 == 0) {
                        LivingEntity chain = null;
                        double best = 4;
                        for (Entity en : le.getWorld().getNearbyEntities(le.getLocation(), 4, 3, 4)) {
                            if (en instanceof LivingEntity other && !en.equals(le) && !en.equals(p) && !isFriendlyTo(p, other)) {
                                double d = other.getLocation().distance(le.getLocation());
                                if (d < best) {
                                    best = d;
                                    chain = other;
                                }
                            }
                        }
                        if (chain != null) {
                            chain.getWorld().strikeLightningEffect(chain.getLocation());
                            hit(chain, 3, p);
                        }
                    }
                }
                case "emberglaive" -> le.setFireTicks(40);
                case "wardenspear" -> le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                case "bloomstaff" -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0));
                case "cleaver" -> le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0));
                case "wardenclaw" -> {
                    Vector push = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0.1);
                    if (push.lengthSquared() > 0) le.setVelocity(push.normalize().multiply(0.6));
                }
                case "stonepike" -> p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0));
                case "chainflail" -> {
                    if (rnd.nextDouble() < 0.2) {
                        Vector pull = p.getLocation().toVector().subtract(le.getLocation().toVector()).setY(0.1);
                        if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(1.2));
                    }
                }
                case "nagina" -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0));
                case "lanternstaff" -> {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                    hit(le, 1, p);
                }
                case "thunderstick" -> {
                    if (rnd.nextDouble() < 0.1) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        hit(le, 3, p);
                    }
                }
                case "vampirefang" -> p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                case "frostbite" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
                case "winddash" -> {
                    Vector gust = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0.2);
                    if (gust.lengthSquared() > 0) le.setVelocity(gust.normalize().multiply(0.9));
                }
                case "infernowand" -> le.setFireTicks(40);
                case "groundbreaker" -> {
                    int combo = comboCount.merge(p.getUniqueId(), 1, Integer::sum);
                    if (combo % 4 == 0) hit(le, 4, p);
                }
                case "phoenixblade" -> {
                    if (p.getHealth() < p.getMaxHealth() / 2) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                }
                case "gauntlet" -> {
                    hit(le, 1, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
                }
                case "voidscythe" -> {
                    hit(le, 1, p);
                    p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1));
                }
                case "venom" -> le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                case "tidal" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
                case "shadowdagger" -> {
                    if (le.getLocation().getDirection().dot(p.getLocation().getDirection()) > 0.5) hit(le, 3, p);
                }
                case "solarlance" -> le.setFireTicks(60);
                case "unoreverse" -> {
                    if (p.getHealth() < p.getMaxHealth() / 2) hit(le, 2, p);
                }
                case "bloodstone" -> p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                case "quakehammer" -> {
                    if (rnd.nextDouble() < 0.15) le.setVelocity(new Vector(0, 0.6, 0));
                }
                case "glacier" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                case "starfall" -> {
                    if (rnd.nextDouble() < 0.1) hit(le, 4, p);
                }
                case "venomsickle" -> le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                case "crystalpick" -> hit(le, 2, p);
                case "voidcutter" -> {
                    if (rnd.nextDouble() < 0.15) hit(le, 4, p);
                }
                case "bloodletter" -> Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (le.isValid() && !le.isDead()) le.damage(1);
                }, 20L);
                case "sunder" -> {
                    Vector away = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0.2);
                    if (away.lengthSquared() > 0) le.setVelocity(away.normalize().multiply(1.0));
                }
                case "reapersigil" -> {
                    if (le.getHealth() < le.getMaxHealth() / 2) hit(le, 2, p);
                }
                case "shadowcloak" -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0));
                case "golembell" -> {
                    if (rnd.nextDouble() < 0.1) {
                        Vector back = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0.2);
                        if (back.lengthSquared() > 0) le.setVelocity(back.normalize().multiply(1.2));
                    }
                }
                case "sonichorn" -> {
                    for (Entity en : le.getWorld().getNearbyEntities(le.getLocation(), 2, 2, 2)) {
                        if (en instanceof LivingEntity other && !en.equals(le) && !en.equals(p) && !isFriendlyTo(p, other)) hit(other, 2, p);
                    }
                }
                case "stormheart" -> {
                    if (rnd.nextDouble() < 0.1) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        hit(le, 3, p);
                    }
                }
                case "phasefruit" -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 0));
                case "grapple" -> {
                    Vector pull = p.getLocation().toVector().subtract(le.getLocation().toVector()).setY(0.1);
                    if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(0.8));
                }
                case "aegistotem" -> p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 0));
                case "chronohourglass" -> {
                    if (rnd.nextDouble() < 0.1) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                }
                case "rewindcompass" -> hit(le, 1, p);
                case "spoutshell" -> le.setVelocity(new Vector(0, 0.4, 0));
                case "honeybomb" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
                case "cloudbottle" -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
                case "meteor" -> le.setFireTicks(20);
                case "hexbone" -> {
                    hit(le, 1, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 0));
                }
                case "healingcharm" -> p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1));
                case "ghostlantern" -> le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0));
                case "kaboom" -> {
                    if (rnd.nextDouble() < 0.1) hit(le, 4, p);
                }
                case "shakalaka" -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 0));
                case "arccaster" -> {
                    if (rnd.nextDouble() < 0.15) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        hit(le, 3, p);
                    }
                }
                case "cursedcoin" -> {
                    if (rnd.nextDouble() < 0.1) p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 100, 0));
                }
                case "spectralquill" -> le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                case "rottenoffering" -> le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                case "eyeofthecrypt" -> le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                case "magnetrod" -> {
                    Vector pull = p.getLocation().toVector().subtract(le.getLocation().toVector()).setY(0.1);
                    if (pull.lengthSquared() > 0) le.setVelocity(pull.normalize().multiply(0.8));
                }
                case "blizzardorb" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                case "grenade" -> {
                    hit(le, 1, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20, 0));
                }
                case "gravitywell" -> {
                    Vector tug = p.getLocation().toVector().subtract(le.getLocation().toVector()).setY(0.1);
                    if (tug.lengthSquared() > 0) le.setVelocity(tug.normalize().multiply(0.8));
                }
                case "emberblade" -> le.setFireTicks(60);
                case "thornrapier" -> le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                case "moonsplitter" -> {
                    long time = p.getWorld().getTime();
                    if (time >= 13000 && time < 23000) hit(le, 2, p);
                }
                case "frostfang" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                case "soulreaver" -> p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                case "stormfang" -> {
                    if (rnd.nextDouble() < 0.1) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        hit(le, 3, p);
                    }
                }
                case "venomtooth" -> le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                case "bladestorm" -> hit(le, 1, p);
                case "hyperion" -> p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 0));
                case "mistral" -> hit(le, 1, p);
                case "obsidianfist" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
                case "starshard" -> {
                    if (rnd.nextDouble() < 0.1) hit(le, 5, p);
                }
                case "cryptbone" -> hit(le, 2, p);
                case "tempesthalberd" -> {
                    if (rnd.nextDouble() < 0.1) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        hit(le, 2, p);
                    }
                }
                case "bloodmoon" -> {
                    long t = p.getWorld().getTime();
                    if (t >= 13000 && t < 23000) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
                }
                case "prismblade" -> {
                    if (rnd.nextDouble() < 0.15) hit(le, 3, p);
                }
                case "voidfang" -> hit(le, 1, p);
                case "solstice" -> le.setFireTicks(40);
                case "glacierhalberd" -> le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0));
                case "embershort" -> le.setFireTicks(40);
                case "runescythe" -> le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0));
                case "thundercudgel" -> {
                    if (rnd.nextDouble() < 0.1) {
                        le.getWorld().strikeLightningEffect(le.getLocation());
                        hit(le, 3, p);
                    }
                }
                case "duskblade" -> {
                    long t = p.getWorld().getTime();
                    if (t >= 13000 && t < 23000) hit(le, 1, p);
                }
                case "ironbulwark" -> p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20, 0));
                default -> { }
            }
            String gemHeld = gemOf(p);
            if (gemHeld != null && NEW_GEM_PASSIVES.contains(gemHeld)) applyGemPassive(p, le, gemHeld);
            if (id.equals("crazyrolls")) applyGemPassive(p, le, GEM_PASSIVE_IDS[rnd.nextInt(GEM_PASSIVE_IDS.length)]);
        } finally {
            inPassive = false;
        }
    }

    // ----- Batch 2 abilities -----

    private void glacierLance(Player p) {
        if (!ready(p, "frostlance", 8000)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Set<LivingEntity> struck = new HashSet<>();
        for (double d = 0.5; d <= 6; d += 0.5) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, pt, 2, 0.1, 0.1, 0.1, 0);
            for (Entity en : pt.getWorld().getNearbyEntities(pt, 1, 1, 1)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le) && struck.add(le)) {
                    hit(le, 5, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9));
                }
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.4f);
    }

    private void stormCleave(Player p) {
        if (!ready(p, "stormhalberd", 9000)) return;
        List<LivingEntity> foes = new ArrayList<>();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) foes.add(le);
        }
        Collections.shuffle(foes);
        int n = Math.min(3, foes.size());
        for (int i = 0; i < n; i++) {
            LivingEntity t = foes.get(i);
            t.getWorld().strikeLightningEffect(t.getLocation());
            hit(t, 4, p);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.1f);
    }

    private void hexCurse(Player p) {
        if (!ready(p, "hexscythe", 14000)) return;
        LivingEntity target = null;
        double best = 6;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            double d = le.getLocation().distance(p.getLocation());
            if (d < best) {
                best = d;
                target = le;
            }
        }
        if (target == null) {
            p.sendMessage(ChatColor.GRAY + "Hex Curse found no foe.");
            return;
        }
        hit(target, 3, p);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1f, 0.6f);
    }

    private void seismicDash(Player p) {
        if (!ready(p, "ironmaul", 10000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        p.setVelocity(fwd.normalize().multiply(1.6).setY(0.35));
        final Player sp = p;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Location c = sp.getLocation().clone();
            for (Entity en : c.getWorld().getNearbyEntities(c, 3, 2, 3)) {
                if (!(en instanceof LivingEntity le) || en.equals(sp) || isFriendlyTo(sp, le)) continue;
                hit(le, 12, sp);
            }
            c.getWorld().spawnParticle(Particle.EXPLOSION, c, 2, 0.6, 0.1, 0.6, 0);
            c.getWorld().playSound(c, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.6f);
        }, 15L);
    }

    private void phantomStep(Player p) {
        if (!ready(p, "ghostglaive", 9000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0));
        phantomUntil.put(p.getUniqueId(), System.currentTimeMillis() + 3000L);
        p.getWorld().spawnParticle(Particle.SMOKE, p.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.02);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.6f);
    }

    private void dawnRadiance(Player p) {
        if (!ready(p, "dawnhalberd", 12000)) return;
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 6));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0));
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.6f);
    }

    private void lunarBlink(Player p) {
        if (!ready(p, "moonblade", 10000)) return;
        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) return;
        dir = dir.normalize();
        Location from = p.getLocation().clone();
        Location dest = from.clone();
        for (double d = 1; d <= 5; d += 1) {
            Location next = from.clone().add(dir.clone().multiply(d));
            if (!next.getBlock().isPassable() || !next.clone().add(0, 1, 0).getBlock().isPassable()) break;
            dest = next;
        }
        dest.setYaw(from.getYaw());
        dest.setPitch(from.getPitch());
        p.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.3);
        p.teleport(dest);
        p.getWorld().spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.3);
        p.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
    }

    private void soulHarvest(Player p) {
        if (!ready(p, "soulsickle", 15000)) return;
        int drained = 0;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (drained >= 5) break;
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 2, p);
            drained++;
        }
        if (drained > 0) p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, Math.min(4, drained - 1)));
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 40, 2, 1, 2, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 1.3f);
    }

    // ----- Batch 3 abilities -----

    private void emberThrust(Player p) {
        if (!ready(p, "emberpike", 6500)) return;
        Vector dir = p.getEyeLocation().getDirection().normalize();
        p.setVelocity(dir.clone().multiply(1.2).setY(0.1));
        final Player sp = p;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Location c = sp.getLocation().clone();
            for (Entity en : c.getWorld().getNearbyEntities(c, 2.5, 2.5, 2.5)) {
                if (!(en instanceof LivingEntity le) || en.equals(sp) || isFriendlyTo(sp, le)) continue;
                hit(le, 8, sp);
                le.setFireTicks(80);
            }
            c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 1, 0), 50, 2, 0.8, 2, 0.05);
            c.getWorld().playSound(c, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.7f);
        }, 8L);
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 30, 0.4, 0.4, 0.4, 0.05);
    }

    private void highJump(Player p) {
        if (!ready(p, "windpike-jump", 6000)) return;
        p.setVelocity(new Vector(0, 2.1, 0));
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 30, 0.5, 0.2, 0.5, 0.05);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!p.isOnline() || ++ticks > 60) {
                    cancel();
                    return;
                }
                p.setFallDistance(0f);
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void gustVault(Player p) {
        if (p.isSneaking()) {
            highJump(p);
            return;
        }
        if (!ready(p, "windpike", 8000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        p.setVelocity(fwd.normalize().multiply(1.1).setY(0.9));
        final Player sp = p;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Location c = sp.getLocation().clone();
            for (Entity en : c.getWorld().getNearbyEntities(c, 3, 2, 3)) {
                if (!(en instanceof LivingEntity le) || en.equals(sp) || isFriendlyTo(sp, le)) continue;
                hit(le, 3, sp);
                Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0.4);
                if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(1.2));
            }
            c.getWorld().spawnParticle(Particle.CLOUD, c, 40, 2, 0.5, 2, 0.05);
            c.getWorld().playSound(c, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1f);
        }, 18L);
    }

    private void tidalHookPull(Player p) {
        if (!ready(p, "tidallance", 6000)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        LivingEntity target = null;
        for (double d = 0.5; d <= 8 && target == null; d += 0.5) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            for (Entity en : pt.getWorld().getNearbyEntities(pt, 0.8, 0.8, 0.8)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le)) {
                    target = le;
                    break;
                }
            }
        }
        if (target == null) {
            p.sendMessage(ChatColor.GRAY + "Tidal Hook found no foe.");
            return;
        }
        Location behind = target.getLocation().clone().subtract(dir.clone().multiply(1.5));
        if (!behind.getBlock().isPassable()) {
            p.sendMessage(ChatColor.GRAY + "No room behind that foe.");
            return;
        }
        behind.setYaw(p.getLocation().getYaw());
        behind.setPitch(p.getLocation().getPitch());
        p.teleport(behind);
        hit(target, 5, p);
        p.getWorld().spawnParticle(Particle.BUBBLE_POP, behind.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
        p.getWorld().playSound(behind, Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.6f);
    }

    private void gravelWell(Player p) {
        if (!ready(p, "gravelglaive", 9000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        final Location center = p.getLocation().clone().add(fwd.normalize().multiply(5));
        final int[] ticks = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            ticks[0]++;
            if (ticks[0] > 30) {
                holder[0].cancel();
                return;
            }
            for (Entity en : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
                if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                Vector pull = center.toVector().subtract(le.getLocation().toVector()).setY(0.1);
                if (pull.lengthSquared() > 0.5) le.setVelocity(pull.normalize().multiply(0.4));
                if (ticks[0] % 20 == 0) hit(le, 2, p);
            }
            center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(0, 1, 0), 20, 2, 0.5, 2, 0.3);
        }, 0L, 1L);
        p.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK, 1f, 0.6f);
    }

    private void echoShout(Player p) {
        if (!ready(p, "echohammer", 7000)) return;
        final Location c = p.getLocation().clone();
        echoPulse(p, c, 5);
        Bukkit.getScheduler().runTaskLater(this, () -> echoPulse(p, c, 3), 20L);
        p.getWorld().playSound(c, Sound.BLOCK_BELL_USE, 1.2f, 0.8f);
    }

    private void echoPulse(Player p, Location c, double dmg) {
        for (Entity en : c.getWorld().getNearbyEntities(c, 4, 2.5, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, dmg, p);
            Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0.3);
            if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(0.9));
        }
        c.getWorld().spawnParticle(Particle.NOTE, c.clone().add(0, 1.5, 0), 20, 2, 0.5, 2, 1);
    }

    private void umbralVeil(Player p) {
        if (!ready(p, "eclipsescythe", 11000)) return;
        Location c = p.getLocation().clone();
        for (Entity en : c.getWorld().getNearbyEntities(c, 5, 3, 5)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 2, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0));
        }
        c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 1, 0), 80, 3, 1, 3, 0.02);
        c.getWorld().playSound(c, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.5f);
    }

    private void stormPulse(Player p) {
        if (!ready(p, "stormstaff", 9000)) return;
        Vector dir = p.getEyeLocation().getDirection().normalize();
        LivingEntity best = null;
        double bestDot = 0.9;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 16, 8, 16)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().add(0, 1, 0).toVector().subtract(p.getEyeLocation().toVector());
            if (to.lengthSquared() == 0) continue;
            double dot = to.clone().normalize().dot(dir);
            if (dot > bestDot) {
                bestDot = dot;
                best = le;
            }
        }
        if (best == null) {
            p.sendMessage(ChatColor.GRAY + "Storm Pulse found no foe in your aim.");
            return;
        }
        best.getWorld().strikeLightningEffect(best.getLocation());
        hit(best, 10, p);
        for (Entity en : best.getWorld().getNearbyEntities(best.getLocation(), 2, 2, 2)) {
            if (!(en instanceof LivingEntity other) || en.equals(best) || en.equals(p) || isFriendlyTo(p, other)) continue;
            hit(other, 3, p);
        }
    }

    private void emberCrescent(Player p) {
        if (!ready(p, "emberglaive", 7000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        fwd = fwd.normalize();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 6, 3, 6)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.55) continue;
            hit(le, 8, p);
            le.setFireTicks(80);
        }
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(fwd.clone().multiply(3)).add(0, 1, 0), 40, 2, 0.8, 2, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.9f);
    }

    // ----- Batch 4 abilities -----

    private void sonarLance(Player p) {
        if (!ready(p, "wardenspear", 11000)) return;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 12, 6, 12)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0));
            if (le.getLocation().distance(p.getLocation()) <= 3) hit(le, 3, p);
        }
        p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_LISTENING, 1.2f, 1f);
    }

    private void bloomBloom(Player p) {
        if (!ready(p, "bloomstaff", 9000)) return;
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 6));
        p.removePotionEffect(PotionEffectType.POISON);
        p.removePotionEffect(PotionEffectType.WITHER);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.NAUSEA);
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 60, 1.2, 1, 1.2, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AZALEA_LEAVES_PLACE, 1f, 1.2f);
    }

    private void shatterSlash(Player p) {
        if (!ready(p, "cleaver", 7000)) return;
        Vector fwd = p.getLocation().getDirection().setY(0);
        if (fwd.lengthSquared() < 0.01) return;
        fwd = fwd.normalize();
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 4, 3, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() == 0 || to.clone().normalize().dot(fwd) < 0.55) continue;
            hit(le, 10, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
        }
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(fwd.clone().multiply(2)).add(0, 1, 0), 15, 1.5, 0.5, 1.5, 0);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_DEEPSLATE_BREAK, 1f, 0.8f);
    }

    private void sonicClaw(Player p) {
        if (!ready(p, "wardenclaw", 10000)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Set<LivingEntity> hitSet = new HashSet<>();
        for (double d = 1; d <= 12; d += 1) {
            Location pt = eye.clone().add(dir.clone().multiply(d));
            p.getWorld().spawnParticle(Particle.SONIC_BOOM, pt, 1, 0, 0, 0, 0);
            for (Entity en : pt.getWorld().getNearbyEntities(pt, 1, 1, 1)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le) && hitSet.add(le)) {
                    hit(le, 6, p);
                }
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1f);
    }

    private void bedrockStab(Player p) {
        if (!ready(p, "stonepike", 6000)) return;
        Vector fwd = p.getEyeLocation().getDirection().normalize();
        LivingEntity target = null;
        double best = 0.7;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 3, 3, 3)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            Vector to = le.getLocation().add(0, 1, 0).toVector().subtract(p.getEyeLocation().toVector());
            if (to.lengthSquared() == 0) continue;
            double dot = to.clone().normalize().dot(fwd);
            if (dot > best) {
                best = dot;
                target = le;
            }
        }
        if (target == null) {
            p.sendMessage(ChatColor.GRAY + "Bedrock Stab found no foe in reach.");
            return;
        }
        hit(target, 9, p);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 9));
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.2);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.6f);
    }

    private void chainWhirl(Player p) {
        if (!ready(p, "chainflail", 9000)) return;
        final Player sp = p;
        for (int i = 0; i < 4; i++) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!sp.isOnline()) return;
                Location c = sp.getLocation().clone();
                for (Entity en : c.getWorld().getNearbyEntities(c, 3, 2, 3)) {
                    if (!(en instanceof LivingEntity le) || en.equals(sp) || isFriendlyTo(sp, le)) continue;
                    hit(le, 2, sp);
                }
                c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(0, 1, 0), 12, 2, 0.5, 2, 0);
            }, i * 10L);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1.2f);
    }

    private void shadowSlash(Player p) {
        if (!ready(p, "nagina", 8000)) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) return;
        dir = dir.normalize();
        Set<LivingEntity> struck = new HashSet<>();
        for (double d = 0.5; d <= 4; d += 0.5) {
            Location pt = p.getLocation().clone().add(dir.clone().multiply(d)).add(0, 1, 0);
            p.getWorld().spawnParticle(Particle.SMOKE, pt, 4, 0.2, 0.3, 0.2, 0.02);
            for (Entity en : pt.getWorld().getNearbyEntities(pt, 1, 1.5, 1)) {
                if (en instanceof LivingEntity le && !en.equals(p) && !isFriendlyTo(p, le) && struck.add(le)) {
                    hit(le, 7, p);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                }
            }
        }
        p.setVelocity(dir.clone().multiply(0.8).setY(0.1));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1f, 0.8f);
    }

    // ----- Cooldowns for any Minecraft item (/stickcooldowns) -----

    private final Map<Material, Long> vanillaCooldownMs = new HashMap<>();
    private final Map<UUID, Map<Material, Long>> vanillaUntil = new HashMap<>();

    private void loadVanillaCooldowns() {
        vanillaCooldownMs.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("vanilla-cooldowns");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            Material m = Material.matchMaterial(k);
            if (m != null) vanillaCooldownMs.put(m, Math.round(sec.getDouble(k) * 1000));
        }
    }

    /** Returns true (and tells the player) if this plain Minecraft item is still cooling down. Starts the cooldown otherwise. */
    private boolean vanillaOnCooldown(Player p, ItemStack it) {
        if (it == null || it.getType().isAir() || idOf(it) != null) return false;
        Long cd = vanillaCooldownMs.get(it.getType());
        if (cd == null || cd <= 0 || p.hasPermission("stickcooldowns.bypass")) return false;
        long now = System.currentTimeMillis();
        Map<Material, Long> mine = vanillaUntil.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long until = mine.getOrDefault(it.getType(), 0L);
        if (until > now) {
            p.sendMessage(ChatColor.RED + prettyMaterial(it.getType()) + " is on cooldown for " + ((until - now + 999) / 1000) + "s.");
            return true;
        }
        mine.put(it.getType(), now + cd);
        p.setCooldown(it.getType(), (int) Math.max(1, cd / 50));
        return false;
    }

    private List<String> cooldownTabs(String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>(List.of("list", "help", "spear"));
            for (Material m : Material.values()) {
                if (m.isItem() && !m.isLegacy()) out.add(m.name().toLowerCase(Locale.ROOT));
            }
            return filterTabs(out, args[0]);
        }
        if (args.length == 2) return filterTabs(List.of("0", "5", "10", "15", "20", "30", "60", "120"), args[1]);
        return List.of();
    }

    private static List<String> filterTabs(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }

    private static String prettyMaterial(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String w : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVanillaUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (vanillaOnCooldown(e.getPlayer(), e.getItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVanillaMelee(EntityDamageByEntityEvent e) {
        if (inPassive || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player p)) return;
        if (vanillaOnCooldown(p, p.getInventory().getItemInMainHand())) e.setCancelled(true);
    }

    /** /stickcooldowns <item> <seconds> | <item> 0 (clears) | list */
    private void cooldownCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("stickcooldowns.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "/stickcooldowns <item> <seconds>" + ChatColor.GRAY + " - set a cooldown (0 clears it)");
            sender.sendMessage(ChatColor.GOLD + "/stickcooldowns list" + ChatColor.GRAY + " - show set cooldowns");
            return;
        }
        if (args[0].equalsIgnoreCase("list")) {
            if (vanillaCooldownMs.isEmpty()) sender.sendMessage(ChatColor.GRAY + "No item cooldowns are set.");
            for (Map.Entry<Material, Long> en : vanillaCooldownMs.entrySet()) {
                sender.sendMessage(ChatColor.GRAY + " - " + prettyMaterial(en.getKey()) + ": " + (en.getValue() / 1000.0) + "s");
            }
            return;
        }
        Material m = args[0].equalsIgnoreCase("spear") ? Material.matchMaterial("NETHERITE_SPEAR") : Material.matchMaterial(args[0].toUpperCase(Locale.ROOT));
        if (m == null || !m.isItem()) {
            sender.sendMessage(ChatColor.RED + "Unknown item: " + args[0]);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /stickcooldowns " + args[0] + " <seconds>");
            return;
        }
        double secs;
        try {
            secs = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Seconds must be a number.");
            return;
        }
        if (secs < 0) {
            sender.sendMessage(ChatColor.RED + "Seconds cannot be negative.");
            return;
        }
        if (secs == 0) {
            vanillaCooldownMs.remove(m);
            getConfig().set("vanilla-cooldowns." + m.name(), null);
            sender.sendMessage(ChatColor.GREEN + prettyMaterial(m) + " has no cooldown now.");
        } else {
            vanillaCooldownMs.put(m, Math.round(secs * 1000));
            getConfig().set("vanilla-cooldowns." + m.name(), secs);
            sender.sendMessage(ChatColor.GREEN + prettyMaterial(m) + " now has a " + secs + "s cooldown.");
        }
        saveConfig();
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
        holder[0] = gemTimer(p, () -> {
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
        Vector horiz = p.getEyeLocation().getDirection().setY(0);
        if (horiz.lengthSquared() == 0) return;
        if (!ready(p, "g_barrier", 60000)) return;
        horiz.normalize();
        Vector perp = new Vector(-horiz.getZ(), 0, horiz.getX());
        Location center = p.getLocation().add(horiz.clone().multiply(4)).add(0, 4, 0);
        BarrierState bs = new BarrierState(p.getUniqueId(), System.currentTimeMillis() + 15000);
        for (int u = -4; u <= 4; u++) {
            for (int v = -4; v <= 4; v++) {
                if (u * u + v * v > 16) continue;
                Block b = center.clone().add(perp.clone().multiply(u)).add(0, v, 0).getBlock();
                if (b.getType().isAir()) {
                    bs.placed.put(b, Material.AIR);
                    b.setType(Material.ICE);
                }
            }
        }
        barriers.add(bs);
        for (int deg = 0; deg < 360; deg += 15) {
            double rad = Math.toRadians(deg);
            Location rim = center.clone().add(perp.clone().multiply(Math.cos(rad) * 4.6)).add(0, Math.sin(rad) * 4.6, 0);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, rim, 2, 0.1, 0.1, 0.1, 0.01);
            p.getWorld().spawnParticle(Particle.END_ROD, rim, 1, 0, 0, 0, 0);
        }
        p.getWorld().playSound(center, Sound.BLOCK_GLASS_PLACE, 1.5f, 0.7f);
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
            if (now >= bs.expiry) {
                restoreRing(bs);
                it.remove();
                continue;
            }
            for (Player v : Bukkit.getOnlinePlayers()) {
                if (v.getUniqueId().equals(bs.owner) || isTrusted(bs.owner, v.getUniqueId())) continue;
                Location vl = v.getLocation().add(0, 1, 0);
                for (Block b : bs.placed.keySet()) {
                    Location bl = b.getLocation().add(0.5, 0.5, 0.5);
                    if (bl.distance(vl) < 1.4) {
                        Vector out = vl.toVector().subtract(bl.toVector()).setY(0);
                        if (out.lengthSquared() > 0) v.setVelocity(out.normalize().multiply(1.2).setY(0.3));
                        v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        break;
                    }
                }
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
        if (!ready(p, "g_subzero", 300000)) return;
        subzeroUntil.put(p.getUniqueId(), System.currentTimeMillis() + 60000);
        final boolean prevFly = p.getAllowFlight();
        p.setAllowFlight(true);
        p.setFlying(true);
        p.sendMessage(ChatColor.AQUA + "Subzero: flying for 60 seconds. Right-click to rain tridents.");
        final BukkitTask[] h = new BukkitTask[1];
        h[0] = gemTimer(p, () -> {
            if (!p.isOnline() || !isActive(subzeroUntil, p)) {
                subzeroUntil.remove(p.getUniqueId());
                if (p.isOnline()) {
                    p.setFlying(false);
                    p.setAllowFlight(prevFly);
                }
                h[0].cancel();
                return;
            }
            if (!p.isFlying()) p.setFlying(true);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 0.2, 0), 6, 0.4, 0.1, 0.4, 0.02);
        }, 0L, 5L);
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
            t.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            t.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "subzero");
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
        if (!ready(p, "g_creak", 180000)) return;
        Entity phantom = spawnCreaking(p.getLocation().add(2, 0, 2), EntityType.PHANTOM, p.getUniqueId(), 2.0, "nmult");
        phantom.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Nightmare Phantom");
        phantom.setCustomNameVisible(true);
        p.sendMessage(ChatColor.DARK_PURPLE + "A Nightmare Phantom follows you for 120 seconds.");
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = gemTimer(p, () -> {
            runs[0]++;
            if (!phantom.isValid()) {
                holder[0].cancel();
                return;
            }
            if (runs[0] > 24) {
                phantom.remove();
                p.sendMessage(ChatColor.DARK_GRAY + "The Nightmare Phantom has faded.");
                holder[0].cancel();
                return;
            }
            phantom.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 2f, 0.5f);
            phantom.getWorld().spawnParticle(Particle.SMOKE, phantom.getLocation().add(0, 1, 0), 15, 0.6, 0.6, 0.6, 0.02);
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
                hidden.getWorld().spawnParticle(Particle.SCULK_SOUL, hidden.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
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
        int n = 3 + rnd.nextInt(5);
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
        if (!ready(p, "g_giant", 300000)) return;
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
            EntityType ct = EntityType.PHANTOM;
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
        Player owner = null;
        if (damager instanceof Player op) owner = op;
        else if (damager instanceof Projectile shot && shot.getShooter() instanceof Player sp) owner = sp;
        if (owner != null) {
            for (UUID fid : followers) {
                Entity f = Bukkit.getEntity(fid);
                if (f instanceof Mob m && m.isValid() && owner.getUniqueId().equals(ownerOf(f))) m.setTarget(target);
            }
        }

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

    private Color colorFor(String id) {
        if (id.equals("reaperskull")) return Color.fromRGB(0x2962FF);
        ItemDef d = items.get(id);
        if (d == null) d = gems.get(id);
        if (d == null) return Color.WHITE;
        return Color.fromRGB(Integer.parseInt(d.hex().substring(1), 16));
    }

    // Every projectile we fire leaves a trail in its item's colour.
    private void tickTrails() {
        for (World w : Bukkit.getWorlds()) {
            for (Projectile pr : w.getEntitiesByClass(Projectile.class)) {
                String tag = pr.getPersistentDataContainer().get(projKey, PersistentDataType.STRING);
                if (tag == null) continue;
                w.spawnParticle(Particle.DUST, pr.getLocation(), 3, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(colorFor(tag), 1.2f));
                if (tag.equals("voidslash")) voidSlashTick(pr);
            }
        }
    }

    private void voidSlashTick(Projectile pr) {
        Set<UUID> already = voidHits.computeIfAbsent(pr.getUniqueId(), k -> new HashSet<>());
        Entity shooter = pr.getShooter() instanceof Entity se ? se : null;
        for (Entity en : pr.getNearbyEntities(2, 2, 2)) {
            if (!(en instanceof LivingEntity le) || en.equals(shooter) || already.contains(le.getUniqueId())) continue;
            if (isFriendlyTo(shooter, le)) continue;
            already.add(le.getUniqueId());
            hit(le, 8, shooter);
        }
        if (!pr.isValid()) voidHits.remove(pr.getUniqueId());
    }

    // Hitting something with one of our items sends out a spark in that item's colour.
    @EventHandler
    public void onItemSpark(EntityDamageByEntityEvent e) {
        if (e.isCancelled() || !(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        String id = idOf(p.getInventory().getItemInMainHand());
        if (id == null) return;
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 14, 0.3, 0.4, 0.3, 0,
                new Particle.DustOptions(colorFor(id), 1.4f));
    }

    private void tickGems() {
        gemTick++;
        if (gemTick % 2 == 0) tickTrails();
        if (gemTick % 4 == 0) tickGemAuraAll();
        if (gemTick % 20 == 0) tickPosHistory();
        tickBlizzardPassive();
        if (gemTick % 5 == 0) tickBarriers();
        if (gemTick % 40 == 0) tickEcho();
        if (gemTick % 10 == 0) tickFollowers();
        if (gemTick % 40 == 0) tickAtomicRegen();
        if (gemTick % 40 == 0) tickYinyangRegen();
        if (gemTick % 10 == 0) tickFollowerTargets();
        if (gemTick % 5 == 0) tickThunderAura();
        if (gemTick % 5 == 0) tickGemAuras();
        if (gemTick % 5 == 0) tickEclipseAura();
        if (gemTick % 5 == 0) cleanupGemTasks();
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
        holder[0] = gemTimer(p, () -> {
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
        if (!ready(p, "g_meltdown", 300000)) return;
        meltdownUntil.put(p.getUniqueId(), System.currentTimeMillis() + 20000);
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 400, 0));
        p.sendTitle(ChatColor.GREEN + "MELTDOWN", ChatColor.WHITE + "Radiation is spreading", 5, 60, 10);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 0.8f);
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = gemTimer(p, () -> {
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
                    if (runs[0] % 5 == 1) victim.sendMessage(ChatColor.GREEN + "You are being melted from the radiation");
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
            le.setVelocity(out.normalize().multiply(2.0).setY(0.5));
            hit(le, 6, p);
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
        holder[0] = gemTimer(p, () -> {
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
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0));
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
        if (!ready(p, "g_breeze", 300000)) return;
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

    private final Map<UUID, Long> meteorOwners = new HashMap<>();

    /** Which projectile tag each crossbow or bow gives its shots. */


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMeteorSelfHarm(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Long cast = meteorOwners.get(victim.getUniqueId());
        if (cast == null || System.currentTimeMillis() - cast > 20000L) return;
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause != EntityDamageEvent.DamageCause.MAGIC
                && cause != EntityDamageEvent.DamageCause.PROJECTILE
                && cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        if (e instanceof EntityDamageByEntityEvent de
                && !(de.getDamager() instanceof DragonFireball)
                && !(de.getDamager() instanceof Fireball)) return;
        e.setCancelled(true);
    }

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
        holder[0] = gemTimer(p, () -> {
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
        if (!ready(p, "g_blaze", 300000)) return;
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
        holder[0] = gemTimer(p, () -> {
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

    // ===================== WARDEN KEEPER =====================

    private void sonicWave(Player p) {
        if (!ready(p, "g_sonic", 10000)) return;
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector fwd = eye.getDirection().normalize();
        w.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 3f, 1f);
        Set<UUID> hitIds = new HashSet<>();
        for (int step = 1; step <= 20; step++) {
            final int s = step;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Location front = eye.clone().add(fwd.clone().multiply(s * 1.2));
                w.spawnParticle(Particle.SONIC_BOOM, front, 1, 0, 0, 0, 0);
                for (Entity en : w.getNearbyEntities(front, 1.8, 1.8, 1.8)) {
                    if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le) || hitIds.contains(en.getUniqueId())) continue;
                    hitIds.add(en.getUniqueId());
                    hit(le, 40, p);
                    le.setVelocity(fwd.clone().multiply(1.6).setY(0.5));
                }
            }, step);
        }
    }

    private void echoPulse(Player p) {
        if (!ready(p, "g_echo", 20000)) return;
        int n = 0;
        for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 20, 20, 20)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
            hit(le, 2, p);
            n++;
        }
        particleRing(p.getLocation().add(0, 0.2, 0), 6, Particle.SCULK_SOUL);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 1f);
        p.sendMessage(ChatColor.DARK_AQUA + "Echo Pulse revealed " + n + " target(s).");
    }

    private void wardenFist(Player p) {
        if (!ready(p, "g_fist", 15000)) return;
        World w = p.getWorld();
        Vector fwd = p.getLocation().getDirection().setY(0).normalize();
        Location spot = p.getLocation().add(fwd.clone().multiply(3));
        for (Entity en : w.getNearbyEntities(spot, 4, 3, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 30, p);
            le.setVelocity(fwd.clone().multiply(0.8).setY(0.8));
        }
        particleRing(spot.clone().add(0, 0.2, 0), 3, Particle.CLOUD);
        particleRing(spot.clone().add(0, 0.2, 0), 1.5, Particle.SCULK_SOUL);
        w.playSound(spot, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2f, 1f);
    }

    private void wardenMonster(Player p) {
        if (!ready(p, "g_wardenult", 300000)) return;
        World w = p.getWorld();
        Warden warden = w.spawn(p.getLocation().add(2, 0, 2), Warden.class);
        setOwner(warden, p.getUniqueId());
        warden.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, "wardenult");
        warden.setCustomName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Warden Monster");
        warden.setCustomNameVisible(true);
        addFollower(warden);
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = gemTimer(p, () -> {
            runs[0]++;
            if (!warden.isValid() || runs[0] > 600 || !p.isOnline()) {
                if (warden.isValid()) warden.remove();
                holder[0].cancel();
                return;
            }
            Location c = warden.getLocation().add(0, 1, 0);
            w.spawnParticle(Particle.SCULK_SOUL, c, 4, 0.4, 0.4, 0.4, 0.01);
            if (runs[0] % 4 == 0) {
                LivingEntity target = nearestEnemy(warden.getLocation(), 16, p);
                if (target != null) {
                    Vector span = target.getLocation().add(0, 1, 0).toVector().subtract(c.toVector());
                    for (int k = 1; k <= 6; k++) {
                        w.spawnParticle(Particle.SONIC_BOOM, c.clone().add(span.clone().multiply(k / 6.0)), 1, 0, 0, 0, 0);
                    }
                    hit(target, 20, p);
                    w.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
                }
            }
        }, 0L, 10L);
        p.sendMessage(ChatColor.DARK_AQUA + "Warden Monster summoned for 5 minutes.");
    }

    @EventHandler
    public void onWardenImmuneDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "warden")) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.FIRE || c == EntityDamageEvent.DamageCause.FIRE_TICK
                || c == EntityDamageEvent.DamageCause.LAVA || c == EntityDamageEvent.DamageCause.HOT_FLOOR
                || c == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWardenImmuneEffects(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "warden")) return;
        if (e.getAction() != EntityPotionEffectEvent.Action.ADDED
                && e.getAction() != EntityPotionEffectEvent.Action.CHANGED) return;
        PotionEffect in = e.getNewEffect();
        if (in == null) return;
        PotionEffectType t = in.getType();
        if (t.equals(PotionEffectType.POISON) || t.equals(PotionEffectType.WITHER)
                || t.equals(PotionEffectType.BLINDNESS) || t.equals(PotionEffectType.DARKNESS)
                || t.equals(PotionEffectType.SLOWNESS) || t.equals(PotionEffectType.NAUSEA)) {
            e.setCancelled(true);
        }
    }

    // ===================== YIN YANG =====================

    private void yinYangRing(Location c, double r) {
        for (int deg = 0; deg < 360; deg += 15) {
            double rad = Math.toRadians(deg);
            Color col = deg < 180 ? Color.BLACK : Color.WHITE;
            c.getWorld().spawnParticle(Particle.DUST, c.clone().add(Math.cos(rad) * r, 0.2, Math.sin(rad) * r),
                    1, 0, 0, 0, 0, new Particle.DustOptions(col, 1.3f));
        }
    }

    private void yinStrike(Player p) {
        if (!ready(p, "g_yin", 8000)) return;
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 8));
        Location c = p.getLocation();
        for (Entity en : c.getWorld().getNearbyEntities(c, 4, 3, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 8, p);
        }
        yinYangRing(c.clone().add(0, 0.2, 0), 4);
        p.sendMessage(ChatColor.WHITE + "Yin Strike!");
    }

    private void yangStrike(Player p) {
        if (!ready(p, "g_yang", 8000)) return;
        p.damage(4);
        Location c = p.getLocation();
        for (Entity en : c.getWorld().getNearbyEntities(c, 4, 3, 4)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 8, p);
            le.setFireTicks(Math.max(le.getFireTicks(), 80));
        }
        particleRing(c.clone().add(0, 0.2, 0), 4, Particle.FLAME);
        p.sendMessage(ChatColor.RED + "Yang Strike!");
    }

    private void balance(Player p) {
        if (!ready(p, "g_balance", 30000)) return;
        LivingEntity target = nearestEnemy(p.getLocation(), 10, p);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "No enemy within 10 blocks.");
            return;
        }
        double mine = p.getHealth();
        double theirs = target.getHealth();
        p.setHealth(Math.min(p.getMaxHealth(), theirs));
        target.setHealth(Math.min(target.getMaxHealth(), mine));
        p.sendMessage(ChatColor.GRAY + "Balance! Health swapped for 5 seconds.");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline()) p.setHealth(Math.min(p.getMaxHealth(), mine));
            if (target.isValid()) target.setHealth(Math.min(target.getMaxHealth(), theirs));
        }, 100L);
    }

    private void taijitu(Player p) {
        if (!ready(p, "g_taijitu", 300000)) return;
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = gemTimer(p, () -> {
            runs[0]++;
            if (runs[0] > 120 || !p.isOnline()) {
                holder[0].cancel();
                return;
            }
            Location c = p.getLocation();
            yinYangRing(c.clone().add(0, 0.2, 0), 6);
            for (Entity en : c.getWorld().getNearbyEntities(c, 6, 3, 6)) {
                if (en.equals(p)) continue;
                if (en instanceof Player pl && (pl.equals(p) || isTrusted(p.getUniqueId(), pl.getUniqueId()))) {
                    pl.setHealth(Math.min(pl.getMaxHealth(), pl.getHealth() + 1));
                    continue;
                }
                if (!(en instanceof LivingEntity le) || isFriendlyTo(p, le)) continue;
                Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0);
                if (out.lengthSquared() > 0) le.setVelocity(out.normalize().multiply(0.8).setY(0.2));
            }
        }, 0L, 10L);
        p.sendMessage(ChatColor.WHITE + "Taijitu: 60 seconds of balance.");
    }

    @EventHandler
    public void onYinHalfHeal(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "yinyang")) return;
        double heal = e.getFinalDamage() * 0.5;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline() && !p.isDead()) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + heal));
        }, 1L);
    }

    private void tickYinyangRegen() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (holdsItem(p, "yinyang") && p.getHealth() < p.getMaxHealth()) {
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1));
            }
        }
    }

    // ===================== UNO REVERSE BLADE =====================

    private String prettyType(EntityType t) {
        return t.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    @EventHandler
    public void onUnoSave(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !(e.getDamager() instanceof Projectile pr)) return;
        if (!"unoreverse".equals(idOf(victim.getInventory().getItemInMainHand()))
                && !"unoreverse".equals(idOf(victim.getInventory().getItemInOffHand()))) return;
        Deque<EntityType> store = unoStore.computeIfAbsent(victim.getUniqueId(), k -> new ArrayDeque<>());
        if (store.size() >= 30) {
            victim.sendMessage(ChatColor.RED + "You reached the projectile save limit (30). You can't save more projectiles.");
            return;
        }
        store.addLast(pr.getType());
        victim.sendMessage(ChatColor.AQUA + "Saved " + prettyType(pr.getType()) + " (" + store.size() + "/30)");
    }

    private void releaseProjectile(Player p) {
        Deque<EntityType> store = unoStore.get(p.getUniqueId());
        if (store == null || store.isEmpty()) {
            p.sendMessage(ChatColor.RED + "No saved projectiles.");
            return;
        }
        EntityType type = store.pollLast();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Entity shot;
        try {
            shot = p.getWorld().spawnEntity(eye.clone().add(dir.clone().multiply(1.2)), type);
        } catch (IllegalArgumentException ex) {
            p.sendMessage(ChatColor.RED + "Can't release that projectile.");
            return;
        }
        if (shot instanceof Projectile pr) pr.setShooter(p);
        shot.setVelocity(dir.clone().multiply(2.0));
        p.getWorld().playSound(eye, Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1.2f);
        p.sendMessage(ChatColor.AQUA + "Reversed! Released " + prettyType(type) + " (" + store.size() + " left).");
    }

    // ===================== SOLAR ECLIPSE =====================

    private final Map<UUID, Long> umbralUntil = new HashMap<>();
    private final Map<UUID, UUID> deathMarkTarget = new HashMap<>();
    private final Map<UUID, Long> deathMarkUntil = new HashMap<>();
    private final Map<UUID, Deque<Location>> posHistory = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> gemTasks = new HashMap<>();
    private final Set<UUID> debugOn = new HashSet<>();
    private final Map<String, Integer> modelIds = new HashMap<>();

    private void dbg(Player p, String msg) {
        if (debugOn.contains(p.getUniqueId())) p.sendMessage(ChatColor.DARK_GRAY + "[dbg] " + msg);
    }

    // Repeating gem ability task, tracked so it stops as soon as the player no longer holds a gem or the crown.
    private BukkitTask gemTimer(Player p, Runnable r, long delay, long period) {
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(this, r, delay, period);
        gemTasks.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(t);
        return t;
    }

    private void cleanupGemTasks() {
        Iterator<Map.Entry<UUID, List<BukkitTask>>> it = gemTasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<BukkitTask>> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            boolean holding = p != null && (gemOf(p) != null || holdsItem(p, "cursedcrown"));
            if (!holding) {
                for (BukkitTask t : en.getValue()) t.cancel();
                it.remove();
            } else {
                en.getValue().removeIf(BukkitTask::isCancelled);
            }
        }
    }
    private boolean reflectBusy = false;

    private void tickPosHistory() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Deque<Location> q = posHistory.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
            q.addLast(p.getLocation().clone());
            while (q.size() > 10) q.pollFirst();
        }
    }

    // Yellow trail for anyone sprinting while wearing or holding the Cursed Crown.
    private void tickGemAuraAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String g = gemOf(p);
            if (g == null || !gems.containsKey(g)) continue;
            Color c = colorFor(g);
            double base = gemTick * 0.15;
            for (int k = 0; k < 3; k++) {
                double a = base + k * 2 * Math.PI / 3;
                double h = 0.4 + ((gemTick + k * 7) % 20) * 0.08;
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(Math.cos(a) * 1.0, h, Math.sin(a) * 1.0),
                        1, 0, 0, 0, 0, new Particle.DustOptions(c, 1.3f));
            }
        }
    }

    private void tickCrownTrail() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isSprinting()) continue;
            boolean crown = holdsItem(p, "cursedcrown") || "cursedcrown".equals(idOf(p.getInventory().getHelmet()));
            if (!crown) continue;
            Vector back = p.getLocation().getDirection().setY(0);
            if (back.lengthSquared() > 0) back.normalize().multiply(-0.6);
            Location at = p.getLocation().add(back).add(0, 0.3, 0);
            p.getWorld().spawnParticle(Particle.DUST, at, 4, 0.25, 0.2, 0.25, 0, new Particle.DustOptions(Color.YELLOW, 1.4f));
        }
    }

    private void voidPulse(Player p) {
        if (!ready(p, "g_pulse", 10000)) return;
        Location c = p.getLocation();
        for (Entity en : c.getWorld().getNearbyEntities(c, 10, 5, 10)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 12, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
        }
        for (int r = 2; r <= 10; r += 4) {
            final double rr = r;
            Bukkit.getScheduler().runTaskLater(this, () -> particleRing(c.clone().add(0, 0.2, 0), rr, Particle.SMOKE), r / 2L);
        }
        c.getWorld().playSound(c, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
    }

    private void shadowStep(Player p) {
        LivingEntity target = nearestEnemy(p.getLocation(), 20, p);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "No enemy within 20 blocks.");
            return;
        }
        if (!ready(p, "g_step", 10000)) return;
        Location from = p.getLocation().clone();
        Location behind = target.getLocation().clone().add(target.getLocation().getDirection().setY(0).normalize().multiply(-1.5));
        behind.setDirection(target.getLocation().toVector().subtract(behind.toVector()));
        p.teleport(behind);
        hit(target, 15, p);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        for (int k = 0; k <= 10; k++) {
            Location tp = from.clone().add(behind.clone().toVector().subtract(from.toVector()).multiply(k / 10.0));
            p.getWorld().spawnParticle(Particle.SMOKE, tp.add(0, 1, 0), 4, 0.1, 0.2, 0.1, 0);
        }
    }

    private void umbralShield(Player p) {
        if (!ready(p, "g_umbral", 25000)) return;
        umbralUntil.put(p.getUniqueId(), System.currentTimeMillis() + 15000);
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 4));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.4f);
        p.sendMessage(ChatColor.DARK_PURPLE + "Eclipse Ward: 10 hearts and reflection for 15 seconds.");
    }

    private void totalEclipse(Player p) {
        if (!ready(p, "g_eclipse", 300000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 600, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 1));
        final Location center = p.getLocation().clone();
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = gemTimer(p, () -> {
            runs[0]++;
            if (runs[0] > 15 || !p.isOnline()) {
                holder[0].cancel();
                return;
            }
            particleRing(center.clone().add(0, 0.2, 0), 12, Particle.SMOKE);
            for (Entity en : center.getWorld().getNearbyEntities(center, 12, 6, 12)) {
                if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                hit(le, 6, p);
            }
        }, 0L, 40L);
        p.sendMessage(ChatColor.DARK_PURPLE + "Total Eclipse: 30 seconds of darkness.");
    }

    @EventHandler
    public void onUmbralBlind(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p) || !isActive(umbralUntil, p)) return;
        if (e.getDamager() instanceof LivingEntity attacker && !attacker.equals(p)) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            if (!reflectBusy) {
                reflectBusy = true;
                try {
                    hit(attacker, e.getDamage() * 0.25, p);
                } finally {
                    reflectBusy = false;
                }
            }
        }
    }

    @EventHandler
    public void onEclipseImmune(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "eclipse")) return;
        if (e.getAction() != EntityPotionEffectEvent.Action.ADDED && e.getAction() != EntityPotionEffectEvent.Action.CHANGED) return;
        PotionEffect in = e.getNewEffect();
        if (in != null && (in.getType().equals(PotionEffectType.DARKNESS) || in.getType().equals(PotionEffectType.BLINDNESS))) {
            e.setCancelled(true);
            p.getWorld().spawnParticle(Particle.SMOKE, p.getLocation().add(0, 1, 0), 10, 0.4, 0.6, 0.4, 0.02);
        }
    }

    private void tickEclipseAura() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!holdsItem(p, "eclipse")) continue;
            double a = gemTick * 0.2;
            for (int k = 0; k < 2; k++) {
                double ang = a + k * Math.PI;
                p.getWorld().spawnParticle(Particle.SMOKE,
                        p.getLocation().add(Math.cos(ang) * 0.9, 0.4 + k * 0.9, Math.sin(ang) * 0.9), 1, 0, 0, 0, 0.01);
            }
            // The Light Before the Dark: below 5 hearts, Resistance, black campfire aura and slow healing.
            if (p.getHealth() < 10) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0));
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 0.25));
                Location base = p.getLocation().add(0, 0.2, 0);
                for (int k = 0; k < 4; k++) {
                    double ang = gemTick * 0.3 + k * Math.PI / 2;
                    p.getWorld().spawnParticle(Particle.DUST, base.clone().add(Math.cos(ang) * 0.6, 0.2, Math.sin(ang) * 0.6),
                            2, 0.05, 0.1, 0.05, 0, new Particle.DustOptions(Color.BLACK, 1.5f));
                }
                p.getWorld().spawnParticle(Particle.SMOKE, base.clone().add(0, 0.6, 0), 3, 0.2, 0.3, 0.2, 0.01);
                p.getWorld().spawnParticle(Particle.FLAME, base.clone().add(0, 0.2, 0), 2, 0.2, 0.05, 0.2, 0.005);
            }
        }
    }

    @EventHandler
    public void onEclipseNight(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !holdsItem(p, "eclipse")) return;
        long t = p.getWorld().getTime();
        if (t >= 13000 && t < 23000) e.setDamage(e.getDamage() * 0.85);
    }

    // ===================== CURSED CROWN =====================

    private static final List<EntityType> SOLDIER_TYPES = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER_SKELETON);

    private int ownedSoldiers(Player p) {
        int n = 0;
        for (UUID fid : followers) {
            Entity f = Bukkit.getEntity(fid);
            if (f != null && f.isValid() && p.getUniqueId().equals(ownerOf(f))
                    && "soldier".equals(f.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) {
                n++;
            }
        }
        return n;
    }

    private void spawnSoldier(Player p, EntityType t, Location loc, long lifeTicks) {
        Entity s;
        try {
            s = p.getWorld().spawnEntity(loc, t);
        } catch (IllegalArgumentException ex) {
            return;
        }
        setOwner(s, p.getUniqueId());
        s.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, "soldier");
        if (s instanceof LivingEntity ls) {
            ls.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) lifeTicks, 0));
        }
        addFollower(s);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (s.isValid()) s.remove();
        }, lifeTicks);
    }

    private void summonSoldiers(Player p) {
        int have = ownedSoldiers(p);
        if (have >= 8) {
            p.sendMessage(ChatColor.RED + "You already have 8 soldiers.");
            return;
        }
        if (!ready(p, "g_soldiers", 60000)) return;
        int n = Math.min(3 + rnd.nextInt(5), 8 - have);
        for (int i = 0; i < n; i++) {
            EntityType t = SOLDIER_TYPES.get(rnd.nextInt(SOLDIER_TYPES.size()));
            spawnSoldier(p, t, p.getLocation().add(rand(-3, 3), 0, rand(-3, 3)), 2400L);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2f, 0.8f);
        p.sendMessage(ChatColor.GOLD + "" + n + " soldier(s) rise for 2 minutes.");
    }

    private void rally(Player p) {
        if (!ready(p, "g_rally", 45000)) return;
        int n = 0;
        for (UUID fid : followers) {
            Entity f = Bukkit.getEntity(fid);
            if (f == null || !f.isValid() || !p.getUniqueId().equals(ownerOf(f))) continue;
            if (!"soldier".equals(f.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) continue;
            f.teleport(p.getLocation().add(rand(-2, 2), 0, rand(-2, 2)));
            if (f instanceof LivingEntity ls) ls.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 1));
            n++;
        }
        p.getWorld().playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 2f, 1f);
        p.sendMessage(ChatColor.GOLD + "Rally! " + n + " soldier(s) called to you.");
    }

    private void curseOfIron(Player p) {
        if (!ready(p, "g_curse", 30000)) return;
        Location c = p.getLocation();
        for (Entity en : c.getWorld().getNearbyEntities(c, 8, 4, 8)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1));
        }
        particleRing(c.clone().add(0, 0.2, 0), 8, Particle.SMOKE);
        c.getWorld().playSound(c, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.7f);
    }

    private void darkLegion(Player p) {
        if (!ready(p, "g_darklegion", 300000)) return;
        int legion = 3 + rnd.nextInt(5);
        for (int i = 0; i < legion; i++) {
            spawnSoldier(p, EntityType.WITHER_SKELETON, p.getLocation().add(rand(-4, 4), 0, rand(-4, 4)), 3600L);
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 3600, 1));
        p.sendTitle(ChatColor.DARK_PURPLE + "DARK LEGION", ChatColor.GRAY + "Eight wither soldiers rise", 5, 60, 10);
    }

    @EventHandler
    public void onSoldierDamage(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        if ("soldier".equals(d.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) {
            e.setDamage(e.getDamage() * 1.25);
        }
    }

    @EventHandler
    public void onCrownCurse(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && holdsItem(p, "cursedcrown")) {
            e.setDamage(e.getDamage() * 1.1);
        }
    }

    // ===================== REAPER SIGIL =====================

    private void soulDrain(Player p) {
        if (!ready(p, "reaper_soul", 12000)) return;
        World w = p.getWorld();
        Location c = p.getLocation();
        int hit = 0;
        for (Entity en : w.getNearbyEntities(c, 6, 3, 6)) {
            if (!(en instanceof LivingEntity le) || en.equals(p) || isFriendlyTo(p, le)) continue;
            hit(le, 8, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            if (hit < 4) p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 2));
            hit++;
        }
        particleRing(c.clone().add(0, 0.2, 0), 6, Particle.SOUL);
        w.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 1f);
        p.sendMessage(ChatColor.DARK_AQUA + "Soul Drain hit " + hit + " target(s).");
    }

    private void deathMark(Player p) {
        LivingEntity target = nearestEnemy(p.getLocation(), 20, p);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "No enemy within 20 blocks.");
            return;
        }
        if (!ready(p, "reaper_mark", 25000)) return;
        deathMarkTarget.put(p.getUniqueId(), target.getUniqueId());
        deathMarkUntil.put(p.getUniqueId(), System.currentTimeMillis() + 10000);
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
        p.sendMessage(ChatColor.DARK_RED + "Death Mark set on a " + target.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ') + " for 10 seconds.");
    }

    // Offhand lifesteal: you heal by the health the victim loses.
    @EventHandler
    public void onReaperLifesteal(EntityDamageByEntityEvent e) {
        if (e.isCancelled() || !(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof Player)) return;
        if (!"reapersigil".equals(idOf(p.getInventory().getItemInOffHand()))) return;
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + e.getFinalDamage()));
    }

    @EventHandler
    public void onDeathMarkDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity v)) return;
        UUID target = deathMarkTarget.get(p.getUniqueId());
        if (target != null && target.equals(v.getUniqueId()) && isActive(deathMarkUntil, p)) {
            e.setDamage(e.getDamage() * 1.5);
        }
    }

    // Passive Harvest: every kill heals 4 hearts while the sigil is in your offhand.
    @EventHandler
    public void onReaperHarvest(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || !"reapersigil".equals(idOf(killer.getInventory().getItemInOffHand()))) return;
        killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + 8));
    }

    // ===================== GUI MENU =====================

    private static final class MenuHolder implements InventoryHolder {
        final String type;
        final int page;
        final Map<Integer, String> actions = new HashMap<>();
        Inventory inv;

        MenuHolder(String type, int page) {
            this.type = type;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private ItemStack icon(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }

    private void fillMenu(Inventory inv) {
        ItemStack pane = icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }
    }

    private void openMain(Player p) {
        MenuHolder h = new MenuHolder("main", 0);
        h.inv = Bukkit.createInventory(h, 27, ChatColor.GOLD + "" + ChatColor.BOLD + "StickmanItems");
        h.inv.setItem(10, icon(Material.CHEST, ChatColor.GREEN + "" + ChatColor.BOLD + "All Items", ChatColor.GRAY + "Browse and get every item."));
        h.inv.setItem(12, icon(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "All Gems", ChatColor.GRAY + "Get a gem (one at a time)."));
        h.inv.setItem(14, icon(Material.COMMAND_BLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + "Commands", ChatColor.GRAY + "Quick command buttons."));
        h.inv.setItem(16, icon(Material.CLOCK, ChatColor.YELLOW + "" + ChatColor.BOLD + "Cooldown Editor", ChatColor.GRAY + "Change cooldowns live."));
        h.inv.setItem(22, icon(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Close"));
        fillMenu(h.inv);
        p.openInventory(h.inv);
    }

    private void openItems(Player p, int page) {
        List<ItemDef> list = new ArrayList<>(items.values());
        int pages = Math.max(1, (list.size() + 44) / 45);
        MenuHolder h = new MenuHolder("items", page);
        h.inv = Bukkit.createInventory(h, 54, ChatColor.GREEN + "Items " + (page + 1) + "/" + pages);
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= list.size()) break;
            h.inv.setItem(i, make(list.get(idx)));
        }
        if (page > 0) h.inv.setItem(45, icon(Material.ARROW, ChatColor.YELLOW + "Previous page"));
        h.inv.setItem(47, icon(Material.HOPPER, ChatColor.GREEN + "Give every item"));
        h.inv.setItem(49, icon(Material.BARRIER, ChatColor.RED + "Back"));
        if ((page + 1) * 45 < list.size()) h.inv.setItem(53, icon(Material.ARROW, ChatColor.YELLOW + "Next page"));
        fillMenu(h.inv);
        p.openInventory(h.inv);
    }

    private void openGems(Player p) {
        MenuHolder h = new MenuHolder("gems", 0);
        h.inv = Bukkit.createInventory(h, 54, ChatColor.LIGHT_PURPLE + "Gems");
        int i = 0;
        for (ItemDef d : gems.values()) {
            if (i >= 45) break;
            h.inv.setItem(i++, make(d));
        }
        h.inv.setItem(49, icon(Material.BARRIER, ChatColor.RED + "Back"));
        h.inv.setItem(53, icon(Material.LAVA_BUCKET, ChatColor.RED + "Remove my gems"));
        fillMenu(h.inv);
        p.openInventory(h.inv);
    }

    private void button(MenuHolder h, int slot, Material m, String name, String command, String... lore) {
        h.inv.setItem(slot, icon(m, name, lore));
        if (command != null) h.actions.put(slot, command);
    }

    private void openCommands(Player p) {
        MenuHolder h = new MenuHolder("commands", 0);
        h.inv = Bukkit.createInventory(h, 36, ChatColor.AQUA + "Commands");
        button(h, 0, Material.GOLDEN_APPLE, ChatColor.GREEN + "Heal", "stickman heal");
        button(h, 1, Material.COOKED_BEEF, ChatColor.GREEN + "Feed", "stickman feed");
        button(h, 2, Material.CLOCK, ChatColor.YELLOW + "Clear my cooldowns", "stickman clearcd");
        button(h, 3, Material.LAVA_BUCKET, ChatColor.RED + "Remove my gems", "stickman cleargems");
        button(h, 4, Material.SADDLE, ChatColor.GOLD + "Kill my giant mounts", "stickman killmounts");
        button(h, 5, Material.LEAD, ChatColor.GOLD + "Dismount", "stickman dismount");
        button(h, 6, Material.SUNFLOWER, ChatColor.YELLOW + "Set day", "stickman time day");
        button(h, 7, Material.CLOCK, ChatColor.BLUE + "Set night", "stickman time night");
        button(h, 8, Material.WHITE_WOOL, ChatColor.WHITE + "Clear weather", "stickman weather clear");
        button(h, 9, Material.WATER_BUCKET, ChatColor.AQUA + "Rain", "stickman weather rain");
        button(h, 10, Material.LIGHTNING_ROD, ChatColor.YELLOW + "Thunder", "stickman weather thunder");
        button(h, 11, Material.BOOK, ChatColor.WHITE + "Stats", "stickman stats");
        button(h, 12, Material.AMETHYST_CLUSTER, ChatColor.LIGHT_PURPLE + "Summon shrine (look at a block)", "stickman summon shrine");
        button(h, 13, Material.PARROT_SPAWN_EGG, ChatColor.GREEN + "Summon giant parrot", "stickman summon giantparrot");
        button(h, 14, Material.AXOLOTL_BUCKET, ChatColor.LIGHT_PURPLE + "Summon axolotl giant", "stickman summon axolotlgiant");
        button(h, 15, Material.RAVAGER_SPAWN_EGG, ChatColor.GOLD + "Summon giant ravager", "stickman summon giantmob ravager");
        button(h, 16, Material.HORSE_SPAWN_EGG, ChatColor.GOLD + "Summon giant horse", "stickman summon giantmob horse");
        button(h, 17, Material.GHAST_SPAWN_EGG, ChatColor.GOLD + "Summon giant ghast", "stickman summon giantmob ghast");
        button(h, 18, Material.WRITABLE_BOOK, ChatColor.WHITE + "Trust list", "stickman trustlist");
        button(h, 19, Material.NETHERITE_SWORD, ChatColor.WHITE + "Follower count", "stickman followers");
        button(h, 20, Material.BELL, ChatColor.YELLOW + "Reload config.yml", "stickman reload");
        button(h, 21, Material.DIAMOND, ChatColor.AQUA + "Gem cooldowns OFF", "stickman nocooldowngem true");
        button(h, 22, Material.COAL, ChatColor.GRAY + "Gem cooldowns ON", "stickman nocooldowngem false");
        button(h, 23, Material.ENDER_PEARL, ChatColor.AQUA + "Item cooldowns OFF", "stickmanitems nocd");
        button(h, 24, Material.REDSTONE, ChatColor.RED + "Item cooldowns ON", "stickmanitems cd");
        button(h, 25, Material.CLOCK, ChatColor.YELLOW + "Cooldown editor", null);
        button(h, 31, Material.BARRIER, ChatColor.RED + "Back", null);
        fillMenu(h.inv);
        p.openInventory(h.inv);
    }

    private void openCooldowns(Player p, int page) {
        ConfigurationSection sec = getConfig().getConfigurationSection("cooldowns");
        List<String> keys = sec == null ? new ArrayList<>() : new ArrayList<>(sec.getKeys(false));
        Collections.sort(keys);
        int pages = Math.max(1, (keys.size() + 44) / 45);
        MenuHolder h = new MenuHolder("cooldowns", page);
        h.inv = Bukkit.createInventory(h, 54, ChatColor.YELLOW + "Cooldowns " + (page + 1) + "/" + pages);
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= keys.size()) break;
            String key = keys.get(idx);
            String name = ABILITY_NAMES.getOrDefault(key, items.containsKey(key) ? items.get(key).name() : key);
            double secs = getConfig().getDouble("cooldowns." + key, 0);
            h.inv.setItem(i, icon(Material.PAPER, ChatColor.YELLOW + name,
                    ChatColor.GRAY + "Current: " + ChatColor.WHITE + secs + "s",
                    ChatColor.GREEN + "Left click: +5s  " + ChatColor.RED + "Right click: -5s",
                    ChatColor.GRAY + "Shift: +/-30s"));
        }
        if (keys.isEmpty()) {
            h.inv.setItem(22, icon(Material.BOOK, ChatColor.GRAY + "No cooldowns yet",
                    ChatColor.GRAY + "Use an ability once and it appears here."));
        }
        if (page > 0) h.inv.setItem(45, icon(Material.ARROW, ChatColor.YELLOW + "Previous page"));
        h.inv.setItem(49, icon(Material.BARRIER, ChatColor.RED + "Back"));
        if ((page + 1) * 45 < keys.size()) h.inv.setItem(53, icon(Material.ARROW, ChatColor.YELLOW + "Next page"));
        fillMenu(h.inv);
        p.openInventory(h.inv);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMenuClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder mh)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)) return;
        if (!p.isOp()) {
            p.closeInventory();
            return;
        }
        int slot = e.getSlot();
        switch (mh.type) {
            case "main" -> {
                switch (slot) {
                    case 10 -> openItems(p, 0);
                    case 12 -> openGems(p);
                    case 14 -> openCommands(p);
                    case 16 -> openCooldowns(p, 0);
                    case 22 -> p.closeInventory();
                    default -> { }
                }
            }
            case "items" -> {
                List<ItemDef> list = new ArrayList<>(items.values());
                if (slot < 45) {
                    int idx = mh.page * 45 + slot;
                    if (idx < list.size()) {
                        giveOrDrop(p, make(list.get(idx)));
                        p.sendMessage(ChatColor.GREEN + "Got " + list.get(idx).name());
                    }
                } else if (slot == 45 && mh.page > 0) {
                    openItems(p, mh.page - 1);
                } else if (slot == 47) {
                    for (ItemDef d : items.values()) giveOrDrop(p, make(d));
                    p.sendMessage(ChatColor.GREEN + "Got every item.");
                } else if (slot == 49) {
                    openMain(p);
                } else if (slot == 53 && (mh.page + 1) * 45 < list.size()) {
                    openItems(p, mh.page + 1);
                }
            }
            case "gems" -> {
                List<ItemDef> list = new ArrayList<>(gems.values());
                if (slot < 45 && slot < list.size()) {
                    p.performCommand("stickman givegem " + list.get(slot).id());
                } else if (slot == 49) {
                    openMain(p);
                } else if (slot == 53) {
                    p.performCommand("stickman cleargems");
                }
            }
            case "commands" -> {
                String cmd = mh.actions.get(slot);
                if (cmd != null) {
                    p.performCommand(cmd);
                } else if (slot == 25) {
                    openCooldowns(p, 0);
                } else if (slot == 31) {
                    openMain(p);
                }
            }
            case "cooldowns" -> {
                ConfigurationSection sec = getConfig().getConfigurationSection("cooldowns");
                List<String> keys = sec == null ? new ArrayList<>() : new ArrayList<>(sec.getKeys(false));
                Collections.sort(keys);
                if (slot < 45) {
                    int idx = mh.page * 45 + slot;
                    if (idx < keys.size()) {
                        String key = keys.get(idx);
                        double step = e.isShiftClick() ? 30 : 5;
                        double delta = e.isRightClick() ? -step : step;
                        double cur = getConfig().getDouble("cooldowns." + key, 0);
                        getConfig().set("cooldowns." + key, Math.max(1, cur + delta));
                        saveConfig();
                        openCooldowns(p, mh.page);
                    }
                } else if (slot == 45 && mh.page > 0) {
                    openCooldowns(p, mh.page - 1);
                } else if (slot == 49) {
                    openMain(p);
                } else if (slot == 53 && (mh.page + 1) * 45 < keys.size()) {
                    openCooldowns(p, mh.page + 1);
                }
            }
            default -> { }
        }
    }

    // ===================== GIANT RIDEABLE MOBS =====================

    private static final Set<EntityType> FLYERS = Set.of(EntityType.PARROT, EntityType.BAT, EntityType.PHANTOM,
            EntityType.GHAST, EntityType.BLAZE, EntityType.ALLAY, EntityType.VEX, EntityType.ENDER_DRAGON,
            EntityType.BEE, EntityType.WITHER);

    private boolean summonGiantMob(Player p, String typeName) {
        EntityType t;
        try {
            t = EntityType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            p.sendMessage(ChatColor.RED + "Unknown mob type: " + typeName);
            return false;
        }
        Entity g;
        try {
            g = p.getWorld().spawnEntity(p.getLocation().add(3, 0, 0), t);
        } catch (IllegalArgumentException ex) {
            p.sendMessage(ChatColor.RED + "That mob can't be spawned: " + typeName);
            return false;
        }
        if (!(g instanceof LivingEntity gl)) {
            g.remove();
            p.sendMessage(ChatColor.RED + "That isn't a living mob: " + typeName);
            return false;
        }
        setOwner(g, p.getUniqueId());
        g.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, "giantmob");
        AttributeInstance sc = gl.getAttribute(Attribute.GENERIC_SCALE);
        if (sc != null) sc.setBaseValue(3.0);
        AttributeInstance hp = gl.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(400);
            gl.setHealth(400);
        }
        gl.setPersistent(true);
        gl.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Giant " + t.name().replace('_', ' '));
        gl.setCustomNameVisible(true);
        addFollower(g);
        p.sendMessage(ChatColor.GOLD + "Giant " + t.name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + " summoned. Right-click it to ride. Sneak to get off.");
        return true;
    }

    @EventHandler
    public void onGiantMount(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof LivingEntity mount)) return;
        if (!"giantmob".equals(mount.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) return;
        Player p = e.getPlayer();
        if (handHasItem(e)) return;
        e.setCancelled(true);
        if (!mount.getPassengers().isEmpty()) return;
        mount.addPassenger(p);
        if (FLYERS.contains(mount.getType())) mount.setGravity(false);
        p.sendMessage(ChatColor.GOLD + "Mounted. Look where you want to go.");
    }

    @EventHandler
    public void onGiantDismount(EntityDismountEvent e) {
        if (e.getDismounted() instanceof LivingEntity mount
                && "giantmob".equals(mount.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) {
            mount.setGravity(true);
        }
    }

    private void tickGiantRide() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!(p.getVehicle() instanceof LivingEntity mount)) continue;
            if (!"giantmob".equals(mount.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) continue;
            if (p.isSneaking()) {
                p.leaveVehicle();
                continue;
            }
            Vector dir = p.getLocation().getDirection().normalize();
            if (FLYERS.contains(mount.getType())) {
                mount.setVelocity(dir.multiply(1.05));
            } else {
                Vector h = dir.clone().setY(0);
                if (h.lengthSquared() > 0) h.normalize().multiply(0.5);
                mount.setVelocity(new Vector(h.getX(), mount.getVelocity().getY(), h.getZ()));
            }
            mount.setRotation(p.getYaw(), 0f);
            p.setFallDistance(0f);
        }
    }

    // ===================== GIANT PARROT =====================

    private void summonGiantParrot(Player p) {
        Parrot parrot = p.getWorld().spawn(p.getLocation().add(3, 0, 0), Parrot.class);
        setOwner(parrot, p.getUniqueId());
        parrot.getPersistentDataContainer().set(ultKey, PersistentDataType.STRING, "giantparrot");
        parrot.setPersistent(true);
        AttributeInstance sc = parrot.getAttribute(Attribute.GENERIC_SCALE);
        if (sc != null) sc.setBaseValue(3.0);
        AttributeInstance php = parrot.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (php != null) {
            php.setBaseValue(300);
            parrot.setHealth(300);
        }
        parrot.setCustomName(ChatColor.GREEN + "" + ChatColor.BOLD + "Giant Parrot");
        parrot.setCustomNameVisible(true);
        p.sendMessage(ChatColor.GREEN + "Giant Parrot summoned. Right-click it to ride. Sneak to get off.");
    }

    @EventHandler
    public void onParrotMount(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Parrot par)) return;
        if (!"giantparrot".equals(par.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) return;
        Player p = e.getPlayer();
        if (handHasItem(e)) return;
        e.setCancelled(true);
        if (!par.getPassengers().isEmpty()) return;
        par.addPassenger(p);
        par.setGravity(false);
        p.sendMessage(ChatColor.GREEN + "Flying. Look where you want to go.");
    }

    @EventHandler
    public void onParrotDismount(EntityDismountEvent e) {
        if (e.getDismounted() instanceof Parrot par
                && "giantparrot".equals(par.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) {
            par.setGravity(true);
        }
    }

    private void tickParrotRide() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!(p.getVehicle() instanceof Parrot par)) continue;
            if (!"giantparrot".equals(par.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) continue;
            if (p.isSneaking()) {
                p.leaveVehicle();
                continue;
            }
            par.setVelocity(p.getLocation().getDirection().normalize().multiply(1.05));
            par.setRotation(p.getYaw(), p.getPitch());
            p.setFallDistance(0f);
        }
    }

    // ===================== BLOODSTONE =====================

    @EventHandler
    public void onBloodstoneLifesteal(EntityDamageByEntityEvent e) {
        if (e.isCancelled() || !(e.getDamager() instanceof Player p)) return;
        if (!"bloodstone".equals(idOf(p.getInventory().getItemInMainHand()))) return;
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + e.getDamage() * 0.10));
    }

    // ===================== AXOLOTL GIANT (mountable) =====================

    @EventHandler
    public void onAxolotlMount(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Axolotl ax)) return;
        if (!"giantaxo".equals(ax.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) return;
        Player p = e.getPlayer();
        if (handHasItem(e)) return;
        e.setCancelled(true);
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
        Bukkit.getScheduler().runTaskLater(this, () -> {
            w.strikeLightningEffect(t);
            damageArea(t, 3, 12, p);
            particleRing(t.clone().add(0, 0.2, 0), 3, Particle.CRIT);
        }, 10L);
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
        if (!ready(p, "g_zeus", 300000)) return;
        zeusUntil.put(p.getUniqueId(), System.currentTimeMillis() + 20000);
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 1));
        p.sendTitle(ChatColor.GOLD + "ZEUS", ChatColor.YELLOW + "The sky answers you", 5, 60, 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.8f);
        final int[] runs = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = gemTimer(p, () -> {
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
            Location at = c.clone().add(Math.cos(rad) * r, 0.1, Math.sin(rad) * r);
            if (pt == Particle.DUST) {
                c.getWorld().spawnParticle(pt, at, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x8B0000), 1.3f));
            } else {
                c.getWorld().spawnParticle(pt, at, 1, 0, 0, 0, 0);
            }
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
            LivingEntity chainTo = nearestEnemy(victim.getLocation(), 6, attacker);
            if (chainTo != null) {
                inThunderProc = true;
                try {
                    victim.getWorld().strikeLightningEffect(chainTo.getLocation());
                    hit(chainTo, 5, attacker);
                } finally {
                    inThunderProc = false;
                }
            }
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

        boolean intoContainer = !(top.getType() == InventoryType.PLAYER || top.getType() == InventoryType.CRAFTING);
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
        if (!(top.getType() == InventoryType.PLAYER || top.getType() == InventoryType.CRAFTING) && isGem(e.getOldCursor())) {
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
        if (creatures != null) creatures.stop();
        saveConfig();
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
                if (hasGem(target)) {
                    Item extra = target.getWorld().dropItem(target.getLocation(), make(g));
                    extra.remove();
                    target.sendMessage(ChatColor.RED + "You already hold a gem. The extra gem was removed.");
                    sender.sendMessage(ChatColor.YELLOW + target.getName() + " already has a gem; the new one was removed.");
                } else {
                    giveOrDrop(target, make(g));
                }
                sender.sendMessage(ChatColor.GREEN + "Gave " + g.name() + " to " + target.getName());
            }
            case "summon" -> {
                if (args.length < 2) { usage(sender, "/stickman summon <shrine|axolotlgiant>"); return true; }
                if (!(sender instanceof Player sp)) { sender.sendMessage(ChatColor.RED + "Only players can summon."); return true; }
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "shrine" -> summonShrine(sp);
                    case "axolotlgiant" -> summonAxolotlGiant(sp);
                    case "giantparrot" -> summonGiantParrot(sp);
                    case "giantmob" -> summonGiantMob(sp, args.length > 2 ? args[2] : "horse");
                    case "cutemob" -> summonGiantMob(sp, "panda");
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
            case "cooldowns" -> {
                if (!(sender instanceof Player cp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                for (Map.Entry<String, List<String>> en : GEM_ABILITIES.entrySet()) {
                    StringBuilder sb = new StringBuilder(hexColor(gems.get(en.getKey()).hex()) + gems.get(en.getKey()).name());
                    int slot = 0;
                    for (String ab : en.getValue()) {
                        slot++;
                        long left = cdLeft(cp, ab);
                        sb.append(ChatColor.GRAY).append(" | ").append(left > 0 ? ChatColor.RED : ChatColor.GREEN).append("A").append(slot);
                        if (left > 0) sb.append(ChatColor.RED).append(" ").append((left / 1000) + 1).append("s");
                    }
                    sender.sendMessage(sb.toString());
                }
            }
            case "gems" -> sender.sendMessage(ChatColor.GOLD + "Gems: " + String.join(", ", gems.keySet()));
            case "menu" -> {
                if (!(sender instanceof Player mp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                openMain(mp);
            }
            case "reload" -> {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "config.yml reloaded.");
            }
            case "clearcd" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                String prefix = target.getUniqueId() + ":";
                cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
                sender.sendMessage(ChatColor.GREEN + "All cooldowns cleared for " + target.getName());
            }
            case "heal" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                target.setHealth(target.getMaxHealth());
                sender.sendMessage(ChatColor.GREEN + "Healed " + target.getName());
            }
            case "feed" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                target.setFoodLevel(20);
                target.setSaturation(20f);
                sender.sendMessage(ChatColor.GREEN + "Fed " + target.getName());
            }
            case "followers" -> {
                if (!(sender instanceof Player fp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                long count = followers.stream().filter(fid -> {
                    Entity f = Bukkit.getEntity(fid);
                    return f != null && f.isValid() && fp.getUniqueId().equals(ownerOf(f));
                }).count();
                sender.sendMessage(ChatColor.GREEN + "You have " + count + " follower(s).");
            }
            case "cleargems" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                var inv = target.getInventory();
                int removed = 0;
                for (int i = 0; i < inv.getSize(); i++) {
                    if (isGem(inv.getItem(i))) {
                        inv.setItem(i, null);
                        removed++;
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Removed " + removed + " gem(s) from " + target.getName());
            }
            case "trustlist" -> {
                if (!(sender instanceof Player tp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                Set<UUID> set = trusts.getOrDefault(tp.getUniqueId(), Set.of());
                if (set.isEmpty()) { sender.sendMessage(ChatColor.GRAY + "You trust nobody."); return true; }
                StringBuilder names = new StringBuilder();
                for (UUID u : set) names.append(Bukkit.getOfflinePlayer(u).getName()).append(' ');
                sender.sendMessage(ChatColor.GREEN + "Trusted: " + names.toString().trim());
            }
            case "dismount" -> {
                if (!(sender instanceof Player dp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (dp.getVehicle() == null) sender.sendMessage(ChatColor.GRAY + "You aren't riding anything.");
                else {
                    dp.leaveVehicle();
                    sender.sendMessage(ChatColor.GREEN + "Dismounted.");
                }
            }
            case "killmounts" -> {
                if (!(sender instanceof Player kp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                int killed = 0;
                Iterator<UUID> it = followers.iterator();
                while (it.hasNext()) {
                    Entity f = Bukkit.getEntity(it.next());
                    if (f == null) { it.remove(); continue; }
                    if (!kp.getUniqueId().equals(ownerOf(f))) continue;
                    if (!"giantmob".equals(f.getPersistentDataContainer().get(ultKey, PersistentDataType.STRING))) continue;
                    f.remove();
                    it.remove();
                    killed++;
                }
                sender.sendMessage(ChatColor.GREEN + "Removed " + killed + " giant mount(s).");
            }
            case "time" -> {
                if (args.length < 2 || !(sender instanceof Player tp)) { usage(sender, "/stickman time <day|night>"); return true; }
                long t = args[1].equalsIgnoreCase("night") ? 13000L : 1000L;
                tp.getWorld().setTime(t);
                sender.sendMessage(ChatColor.GREEN + "Time set to " + args[1].toLowerCase(Locale.ROOT) + ".");
            }
            case "weather" -> {
                if (args.length < 2 || !(sender instanceof Player wp)) { usage(sender, "/stickman weather <clear|rain|thunder>"); return true; }
                World ww = wp.getWorld();
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "clear" -> { ww.setStorm(false); ww.setThundering(false); }
                    case "rain" -> { ww.setStorm(true); ww.setThundering(false); }
                    case "thunder" -> { ww.setStorm(true); ww.setThundering(true); }
                    default -> { usage(sender, "/stickman weather <clear|rain|thunder>"); return true; }
                }
                sender.sendMessage(ChatColor.GREEN + "Weather set to " + args[1].toLowerCase(Locale.ROOT) + ".");
            }
            case "stats" -> {
                if (!(sender instanceof Player sp2)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                long owned = followers.stream().filter(fid -> {
                    Entity f = Bukkit.getEntity(fid);
                    return f != null && sp2.getUniqueId().equals(ownerOf(f));
                }).count();
                sender.sendMessage(ChatColor.GOLD + "Stats for " + sp2.getName() + ":");
                sender.sendMessage(ChatColor.GRAY + "Holding a gem: " + (hasGem(sp2) ? "yes" : "no"));
                sender.sendMessage(ChatColor.GRAY + "Followers: " + owned);
                sender.sendMessage(ChatColor.GRAY + "Trusted players: " + trusts.getOrDefault(sp2.getUniqueId(), Set.of()).size());
                sender.sendMessage(ChatColor.GRAY + "Saved projectiles: " + unoStore.getOrDefault(sp2.getUniqueId(), new ArrayDeque<>()).size());
            }
            case "killfollowers" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                int removed = 0;
                Iterator<UUID> it = followers.iterator();
                while (it.hasNext()) {
                    Entity f = Bukkit.getEntity(it.next());
                    if (f == null) { it.remove(); continue; }
                    if (!target.getUniqueId().equals(ownerOf(f))) continue;
                    f.remove();
                    it.remove();
                    removed++;
                }
                sender.sendMessage(ChatColor.GREEN + "Removed " + removed + " follower(s) of " + target.getName());
            }
            case "lightning" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                target.getWorld().strikeLightningEffect(target.getLocation());
                hit(target, 6, sender instanceof Player lp ? lp : null);
                sender.sendMessage(ChatColor.YELLOW + "Lightning on " + target.getName());
            }
            case "bring" -> {
                if (!(sender instanceof Player bp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                target.teleport(bp.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Brought " + target.getName() + " to you.");
            }
            case "tp" -> {
                if (!(sender instanceof Player tpp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                tpp.teleport(target.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
            }
            case "fly" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                boolean on = !target.getAllowFlight();
                target.setAllowFlight(on);
                target.setFlying(on);
                sender.sendMessage(ChatColor.GREEN + "Flight " + (on ? "ON" : "OFF") + " for " + target.getName());
            }
            case "invis" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                if (target.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    target.removePotionEffect(PotionEffectType.INVISIBILITY);
                    sender.sendMessage(ChatColor.GRAY + "Invisibility off for " + target.getName());
                } else {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 0, false, false));
                    sender.sendMessage(ChatColor.GRAY + "Invisibility on for " + target.getName());
                }
            }
            case "debug" -> {
                if (!(sender instanceof Player dp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (debugOn.remove(dp.getUniqueId())) sender.sendMessage(ChatColor.GRAY + "Debug off.");
                else {
                    debugOn.add(dp.getUniqueId());
                    sender.sendMessage(ChatColor.GRAY + "Debug on. Press F or click an item and send me these lines.");
                }
            }
            case "crowncheck" -> {
                if (!(sender instanceof Player cp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                ItemStack helmet = cp.getInventory().getHelmet();
                sender.sendMessage(ChatColor.GOLD + "Cursed Crown check:");
                sender.sendMessage(ChatColor.GRAY + "Helmet slot: " + (helmet == null ? "empty" : helmet.getType().name()));
                sender.sendMessage(ChatColor.GRAY + "Helmet id: " + idOf(helmet));
                sender.sendMessage(ChatColor.GRAY + "Main hand id: " + idOf(cp.getInventory().getItemInMainHand()));
                sender.sendMessage(ChatColor.GRAY + "Offhand id: " + idOf(cp.getInventory().getItemInOffHand()));
                sender.sendMessage(ChatColor.GRAY + "holdsItem(cursedcrown): " + holdsItem(cp, "cursedcrown"));
                sender.sendMessage(ChatColor.GRAY + "Soldiers owned: " + ownedSoldiers(cp));
                sender.sendMessage(ChatColor.GRAY + "Soldier cooldown left: " + (cdLeft(cp, "g_soldiers") / 1000) + "s");
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
        s.sendMessage(ChatColor.YELLOW + "/stickman menu  (opens the full GUI)");
        s.sendMessage(ChatColor.YELLOW + "/stickman killfollowers|lightning|bring|tp|fly|invis [player]");
        s.sendMessage(ChatColor.YELLOW + "/stickman cooldowns | gems | reload | followers | trustlist");
        s.sendMessage(ChatColor.YELLOW + "/stickman clearcd|heal|feed|cleargems [player]");
        s.sendMessage(ChatColor.YELLOW + "/stickman summon giantmob <type> | dismount | killmounts | time day|night | weather clear|rain|thunder | stats");
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
        if (args.length == 2 && sub.equals("summon")) return List.of("shrine", "axolotlgiant", "giantparrot", "giantmob", "cutemob");
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
                giveOrDrop(target, make(d));
                sender.sendMessage(ChatColor.GREEN + "Gave " + d.name() + " to " + target.getName());
            }
            case "giveall" -> {
                Player target = resolveTarget(sender, args, 1);
                if (target == null) return true;
                for (ItemDef d : items.values()) giveOrDrop(target, make(d));
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

    // ===== Creatures module (formerly StickmanCreatures) =====
    public static class CreaturesModule implements Listener, CommandExecutor, TabCompleter {
        private StickmanItems host;
        void start(StickmanItems h) {
            host = h;
            ownerKey = new NamespacedKey(host, "creature-owner");
            tamerKey = new NamespacedKey(host, "creature-tamer");
            creatureKey = new NamespacedKey(host, "creature-type");
            registerTamerRecipe();
            host.getServer().getPluginManager().registerEvents(this, host);
            if (host.getCommand("stickmancreatures") != null) {
                host.getCommand("stickmancreatures").setExecutor(this);
                host.getCommand("stickmancreatures").setTabCompleter(this);
            } else {
                host.getLogger().severe("Command stickmancreatures is missing from plugin.yml. Creature commands are off.");
            }
            Bukkit.getScheduler().runTaskTimer(host, this::tick, 10L, 10L);
            Bukkit.getScheduler().runTaskTimer(host, this::tickTamed, 10L, 10L);
            Bukkit.getScheduler().runTaskTimer(host, this::tickAura, 10L, 10L);
            host.getLogger().info("StickmanCreatures loaded " + CREATURES.size() + " creatures.");
        }
        void stop() {
            for (UUID owner : new ArrayList<>(companions.keySet())) dismiss(owner);
        }

    
        static final long HIT_COOLDOWN_MS = 1500L;
        static final double CREATURE_DAMAGE_MULT = 3.0;
        static final double AURA_RANGE = 50.0;
        static final long ATTACKER_MEMORY_MS = 10000L;
        static final double MEDIUM_SCALE = 1.4;
    
        record Creature(String id, String name, EntityType type, String color, String ability, boolean fast) {}
    
        record Hostile(UUID attacker, long time) {}
    
        static final class Companion {
            final UUID owner;
            final Creature def;
            final UUID entityId;
            long lastHit;
    
            Companion(UUID owner, Creature def, UUID entityId) {
                this.owner = owner;
                this.def = def;
                this.entityId = entityId;
            }
        }
    
        static final List<Creature> CREATURES = List.of(
                new Creature("sparkit", "Sparkit", EntityType.AXOLOTL, "#FFEB3B", "LIGHTNING", true),
                new Creature("cindermutt", "Cindermutt", EntityType.WOLF, "#FF6D00", "FIRE", false),
                new Creature("frostlet", "Frostlet", EntityType.FOX, "#81D4FA", "FREEZE", false),
                new Creature("stingle", "Stingle", EntityType.BEE, "#FDD835", "POISON", false),
                new Creature("thornhop", "Thornhop", EntityType.RABBIT, "#66BB6A", "KNOCK", true),
                new Creature("gusty", "Gusty", EntityType.PARROT, "#4DB6AC", "LEVITATE", false),
                new Creature("boulder", "Boulder", EntityType.PANDA, "#8D6E63", "STUN", false),
                new Creature("shellfin", "Shellfin", EntityType.TURTLE, "#A5D6A7", "SHIELD_OWNER", false),
                new Creature("lunar", "Lunar", EntityType.CAT, "#B39DDB", "BLIND", true),
                new Creature("glimmer", "Glimmer", EntityType.ALLAY, "#80DEEA", "HEAL_OWNER", false),
                new Creature("dune", "Dune", EntityType.CAMEL, "#D7CCC8", "BLIND", false),
                new Creature("cobalt", "Cobalt", EntityType.DOLPHIN, "#42A5F5", "TIDAL", true),
                new Creature("hush", "Hush", EntityType.SNIFFER, "#A1887F", "REVEAL", false),
                new Creature("emberail", "Emberail", EntityType.STRIDER, "#FF3D00", "LAVA", false),
                new Creature("tumbleroll", "Tumbleroll", EntityType.ARMADILLO, "#BCAAA4", "KNOCK", false),
                new Creature("gustwisp", "Gustwisp", EntityType.BREEZE, "#B2EBF2", "WIND", false),
                new Creature("hornback", "Hornback", EntityType.GOAT, "#EEEEEE", "STUN", false),
                new Creature("fleecebolt", "Fleecebolt", EntityType.SHEEP, "#FFF59D", "LIGHTNING", false),
                new Creature("peckbrood", "Peckbrood", EntityType.CHICKEN, "#FFCC80", "WIND", false),
                new Creature("hogrush", "Hogrush", EntityType.PIG, "#F48FB1", "SPEED_OWNER", true),
                new Creature("spitback", "Spitback", EntityType.LLAMA, "#CE93D8", "NAUSEA", false),
                new Creature("hopper", "Hopper", EntityType.FROG, "#9CCC65", "LEVITATE", true),
                new Creature("shadowpaw", "Shadowpaw", EntityType.OCELOT, "#616161", "POISON", false),
                new Creature("mooncow", "Mooncow", EntityType.COW, "#E1F5FE", "HEAL_OWNER", false),
                new Creature("gigglepaw", "Gigglepaw", EntityType.PANDA, "#FFAB91", "NAUSEA", false),
                new Creature("honeyscorch", "Honeyscorch", EntityType.BEE, "#FFB300", "DRAIN", false),
                new Creature("wavebolt", "Wavebolt", EntityType.DOLPHIN, "#26C6DA", "PUSH", true),
                new Creature("ghostpup", "Ghostpup", EntityType.WOLF, "#E0E0E0", "REVEAL", false),
                new Creature("frostpaw", "Frostpaw", EntityType.CAT, "#B3E5FC", "FREEZE", false),
                new Creature("dustroller", "Dustroller", EntityType.CAMEL, "#E6AE6B", "DRAIN", false));
    
        static final Map<String, String> ABILITY_TEXT = Map.ofEntries(
                Map.entry("LIGHTNING", "Calls a lightning strike onto the target for 6 damage."),
                Map.entry("FIRE", "Sets the target on fire for 5 seconds and deals 4 damage."),
                Map.entry("LAVA", "Sets the target on fire for 8 seconds and deals 4 damage."),
                Map.entry("FREEZE", "Slows the target for 4 seconds and deals 3 damage."),
                Map.entry("POISON", "Poisons the target for 6 seconds and deals 2 damage."),
                Map.entry("KNOCK", "Knocks the target back and deals 3 damage."),
                Map.entry("TIDAL", "Sweeps the target away with a wave and deals 4 damage."),
                Map.entry("PUSH", "Shoves the target hard away and deals 2 damage."),
                Map.entry("LEVITATE", "Lifts the target into the air for 1 second and deals 3 damage."),
                Map.entry("STUN", "Heavily slows the target for 1.5 seconds and deals 5 damage."),
                Map.entry("BLIND", "Blinds the target for 3 seconds and deals 2 damage."),
                Map.entry("NAUSEA", "Makes the target nauseous for 4 seconds and deals 2 damage."),
                Map.entry("REVEAL", "Makes the target glow for 6 seconds and deals 3 damage."),
                Map.entry("WIND", "Blasts everything near the target with a gust for 2 damage."),
                Map.entry("DRAIN", "Deals 4 damage and heals you for 2 hearts."),
                Map.entry("HEAL_OWNER", "Deals 2 damage and heals you for 2 hearts."),
                Map.entry("SHIELD_OWNER", "Deals 2 damage and gives you 2 hearts of absorption for 10 seconds."),
                Map.entry("SPEED_OWNER", "Deals 2 damage and gives you Speed II for 3 seconds."));
    
        private final Map<UUID, Companion> companions = new HashMap<>();
        private final Map<UUID, Hostile> lastAttackers = new HashMap<>();
        private NamespacedKey ownerKey;
        private NamespacedKey tamerKey;
        private NamespacedKey creatureKey;
        private final Map<UUID, UUID> tamedOwner = new HashMap<>();
        private final Map<UUID, Hostile> ownerTargets = new HashMap<>();
        private final Map<UUID, Long> tamedHitAt = new HashMap<>();
    
        // ---------- Helpers ----------
    
        private Creature find(String id) {
            for (Creature c : CREATURES) if (c.id().equalsIgnoreCase(id)) return c;
            return null;
        }
    
        private String colored(Creature c) {
            return net.md_5.bungee.api.ChatColor.of(c.color()) + "" + ChatColor.BOLD + c.name();
        }
    
        private void dismiss(UUID owner) {
            Companion c = companions.remove(owner);
            if (c == null) return;
            Entity e = Bukkit.getEntity(c.entityId);
            if (e != null && e.isValid()) e.remove();
        }
    
        private void summon(Player owner, Creature def) {
            dismiss(owner.getUniqueId());
            Location spawn = owner.getLocation().clone().add(2, 0, 2);
            Entity e = spawn.getWorld().spawnEntity(spawn, def.type());
            if (!(e instanceof LivingEntity le)) {
                e.remove();
                owner.sendMessage(ChatColor.RED + "That creature could not be spawned.");
                return;
            }
            le.getPersistentDataContainer().set(creatureKey, PersistentDataType.STRING, def.id());
            applyVariant(le, def.id());
            le.setInvulnerable(true);
            le.setCustomName(colored(def));
            le.setCustomNameVisible(true);
            le.setPersistent(true);
            if (le instanceof Mob m) m.setRemoveWhenFarAway(false);
    
            AttributeInstance scale = le.getAttribute(Attribute.GENERIC_SCALE);
            if (scale != null) scale.setBaseValue(MEDIUM_SCALE);
    
            AttributeInstance maxHp = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHp != null) maxHp.setBaseValue(1000);
            le.setHealth(1000);
    
            if (def.fast()) {
                AttributeInstance speed = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.35);
            }
    
            le.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            companions.put(owner.getUniqueId(), new Companion(owner.getUniqueId(), def, le.getUniqueId()));
            owner.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Summoned " + ChatColor.RESET + colored(def)
                    + ChatColor.GREEN + ". " + ChatColor.GRAY + ABILITY_TEXT.get(def.ability()));
        }
    
        private void damage(LivingEntity target, double amount, Player owner) {
            target.damage(amount * CREATURE_DAMAGE_MULT, owner);
        }
    
        private void knock(LivingEntity target, Location from, double horizontal, double up) {
            Vector out = target.getLocation().toVector().subtract(from.toVector()).setY(0);
            if (out.lengthSquared() < 0.01) out = new Vector(0.01, 0, 0);
            target.setVelocity(out.normalize().multiply(horizontal).setY(up));
        }
    
        private void healOwner(Player owner, double hearts) {
            owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + hearts));
        }
    
        /** Each creature's single ability, fired when it lands a hit. */
        private void useAbility(String ability, LivingEntity target, Player owner) {
            Location at = target.getLocation();
            switch (ability) {
                case "LIGHTNING" -> {
                    at.getWorld().strikeLightningEffect(at);
                    damage(target, 6, owner);
                }
                case "FIRE" -> {
                    target.setFireTicks(100);
                    damage(target, 4, owner);
                }
                case "LAVA" -> {
                    target.setFireTicks(160);
                    damage(target, 4, owner);
                }
                case "FREEZE" -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                    damage(target, 3, owner);
                }
                case "POISON" -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 0));
                    damage(target, 2, owner);
                }
                case "KNOCK" -> {
                    damage(target, 3, owner);
                    knock(target, owner.getLocation(), 0.9, 0.5);
                }
                case "TIDAL" -> {
                    damage(target, 4, owner);
                    knock(target, owner.getLocation(), 1.4, 0.2);
                }
                case "PUSH" -> {
                    damage(target, 2, owner);
                    knock(target, owner.getLocation(), 1.8, 0.4);
                }
                case "LEVITATE" -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 0));
                    damage(target, 3, owner);
                }
                case "STUN" -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 4));
                    damage(target, 5, owner);
                }
                case "BLIND" -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    damage(target, 2, owner);
                }
                case "NAUSEA" -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
                    damage(target, 2, owner);
                }
                case "REVEAL" -> {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0));
                    damage(target, 3, owner);
                }
                case "WIND" -> {
                    for (Entity en : at.getWorld().getNearbyEntities(at, 3, 2, 3)) {
                        if (!(en instanceof LivingEntity le) || en.equals(owner)) continue;
                        damage(le, 2, owner);
                        knock(le, at, 1.0, 0.3);
                    }
                }
                case "DRAIN" -> {
                    damage(target, 4, owner);
                    healOwner(owner, 2);
                }
                case "HEAL_OWNER" -> {
                    damage(target, 2, owner);
                    healOwner(owner, 2);
                }
                case "SHIELD_OWNER" -> {
                    damage(target, 2, owner);
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                }
                case "SPEED_OWNER" -> {
                    damage(target, 2, owner);
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
                }
                default -> damage(target, 3, owner);
            }
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.4, 0.5, 0.4, 0.1);
        }
    
        private LivingEntity findTarget(Player owner) {
            Hostile h = lastAttackers.get(owner.getUniqueId());
            if (h != null && System.currentTimeMillis() - h.time() < ATTACKER_MEMORY_MS) {
                Player attacker = Bukkit.getPlayer(h.attacker());
                if (attacker != null && attacker.isOnline() && attacker.getWorld() == owner.getWorld()
                        && attacker.getLocation().distance(owner.getLocation()) <= 16) {
                    return attacker;
                }
            }
            LivingEntity best = null;
            double bestDist = 10;
            for (Entity en : owner.getNearbyEntities(10, 4, 10)) {
                if (!(en instanceof Monster m)) continue;
                double d = m.getLocation().distance(owner.getLocation());
                if (d < bestDist) {
                    bestDist = d;
                    best = m;
                }
            }
            return best;
        }
    
        // ---------- Companion loop ----------
    
        private void tick() {
            for (Companion c : new ArrayList<>(companions.values())) {
                Player owner = Bukkit.getPlayer(c.owner);
                if (owner == null || !owner.isOnline()) {
                    dismiss(c.owner);
                    continue;
                }
                Entity ent = Bukkit.getEntity(c.entityId);
                if (!(ent instanceof LivingEntity le) || !le.isValid() || le.isDead()) {
                    companions.remove(c.owner);
                    continue;
                }
                if (le.getWorld() != owner.getWorld() || le.getLocation().distance(owner.getLocation()) > 20) {
                    le.teleport(owner.getLocation().clone().add(2, 0, 2));
                    continue;
                }
                LivingEntity target = findTarget(owner);
                if (target == null) {
                    if (le instanceof Mob m && le.getLocation().distance(owner.getLocation()) > 4) {
                        m.getPathfinder().moveTo(owner.getLocation());
                    }
                    continue;
                }
                if (le instanceof Mob m) m.getPathfinder().moveTo(target.getLocation());
                if (le.getLocation().distance(target.getLocation()) <= 2.5
                        && System.currentTimeMillis() - c.lastHit >= HIT_COOLDOWN_MS) {
                    c.lastHit = System.currentTimeMillis();
                    useAbility(c.def.ability(), target, owner);
                }
            }
        }
    
        // ---------- Events ----------
    
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onDamage(EntityDamageByEntityEvent e) {
            Player attacker = null;
            if (e.getDamager() instanceof Player dp) attacker = dp;
            else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player sp) attacker = sp;
    
            if (e.getEntity() instanceof Player victim && attacker != null && !attacker.equals(victim)) {
                lastAttackers.put(victim.getUniqueId(), new Hostile(attacker.getUniqueId(), System.currentTimeMillis()));
            }
            if (attacker != null && e.getEntity() instanceof LivingEntity tv && !(tv instanceof Player)) {
                ownerTargets.put(attacker.getUniqueId(), new Hostile(tv.getUniqueId(), System.currentTimeMillis()));
            }
            // Owners cannot hurt their own creature.
            String victimOwner = e.getEntity().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (victimOwner != null && attacker != null && victimOwner.equals(attacker.getUniqueId().toString())) {
                e.setCancelled(true);
                return;
            }
            // A creature cannot hurt its own owner.
            String attackerOwner = e.getDamager().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (attackerOwner != null && e.getEntity() instanceof Player v && attackerOwner.equals(v.getUniqueId().toString())) {
                e.setCancelled(true);
            }
        }
    
        @EventHandler
        public void onCreatureDeath(EntityDeathEvent e) {
            tamedOwner.remove(e.getEntity().getUniqueId());
            String ownerTag = e.getEntity().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (ownerTag == null) return;
            e.getDrops().clear();
            e.setDroppedExp(0);
            UUID ownerId = UUID.fromString(ownerTag);
            companions.remove(ownerId);
            Player owner = Bukkit.getPlayer(ownerId);
            if (owner != null) owner.sendMessage(ChatColor.RED + "Your creature fainted. Summon it again with /creature summon.");
        }
    
        // ===== Creature menu =====
    
        static final class MenuHolder implements InventoryHolder {
            Inventory inv;
    
            @Override
            public Inventory getInventory() {
                return inv;
            }
        }
    
        private void openMenu(Player p) {
            MenuHolder holder = new MenuHolder();
            Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Creatures");
            holder.inv = inv;
            for (int i = 0; i < CREATURES.size() && i < 45; i++) {
                Creature c = CREATURES.get(i);
                Material icon = Material.matchMaterial(c.type().name() + "_SPAWN_EGG");
                ItemStack it = new ItemStack(icon == null ? Material.BARRIER : icon);
                ItemMeta meta = it.getItemMeta();
                meta.setDisplayName(net.md_5.bungee.api.ChatColor.of(c.color()) + "" + ChatColor.BOLD + c.name());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Medium size" + (c.fast() ? ", fast" : ""));
                lore.add(ChatColor.GRAY + "Ability: " + ChatColor.WHITE + ABILITY_TEXT.getOrDefault(c.ability(), c.ability()));
                lore.add(ChatColor.GREEN + "Click to summon");
                meta.setLore(lore);
                it.setItemMeta(meta);
                inv.setItem(i, it);
            }
            ItemStack dismiss = new ItemStack(Material.BARRIER);
            ItemMeta dm = dismiss.getItemMeta();
            dm.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Dismiss your creature");
            dismiss.setItemMeta(dm);
            inv.setItem(49, dismiss);
            p.openInventory(inv);
        }
    
        @EventHandler(priority = EventPriority.HIGH)
        public void onMenuClick(InventoryClickEvent e) {
            if (!(e.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (!p.hasPermission("stickmancreatures.use")) {
                p.closeInventory();
                return;
            }
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= 54) return;
            if (slot < CREATURES.size() && slot < 45) {
                summon(p, CREATURES.get(slot));
                p.closeInventory();
            } else if (slot == 49) {
                dismiss(p.getUniqueId());
                p.sendMessage(ChatColor.GRAY + "Your creature was returned.");
                p.closeInventory();
            }
        }
    
        // ===== Taming =====

        private void registerTamerRecipe() {
            try {
                ItemStack tamer = new ItemStack(Material.NAME_TAG);
                ItemMeta meta = tamer.getItemMeta();
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Creature Tamer");
                meta.setLore(List.of(ChatColor.GRAY + "Right-click a creature to tame it.",
                        ChatColor.GRAY + "Tamed creatures follow you and fight for you."));
                meta.getPersistentDataContainer().set(tamerKey, PersistentDataType.BYTE, (byte) 1);
                tamer.setItemMeta(meta);
                ShapedRecipe r = new ShapedRecipe(new NamespacedKey(host, "creature-tamer-recipe"), tamer);
                r.shape("DND", "NSN", "DND");
                r.setIngredient('D', Material.DIAMOND);
                r.setIngredient('N', Material.NETHERITE_INGOT);
                r.setIngredient('S', Material.NETHER_STAR);
                Bukkit.addRecipe(r);
            } catch (IllegalStateException ex) {
                host.getLogger().warning("Creature Tamer recipe is already registered.");
            }
        }

        private void applyVariant(LivingEntity le, String creatureId) {
            if ("sparkit".equals(creatureId) && le instanceof Axolotl ax) ax.setVariant(Axolotl.Variant.CYAN);
        }

        private Creature creatureOf(LivingEntity le) {
            String cid = le.getPersistentDataContainer().get(creatureKey, PersistentDataType.STRING);
            if (cid != null) for (Creature c : CREATURES) if (c.id().equals(cid)) return c;
            return defFor(le.getType());
        }

        private boolean isTamer(ItemStack it) {
            return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(tamerKey, PersistentDataType.BYTE);
        }

        private boolean isCreatureType(EntityType t) {
            for (Creature c : CREATURES) if (c.type() == t) return true;
            return false;
        }

        /** A golden apple heals a creature by 250 health. */
        @EventHandler(priority = EventPriority.HIGH)
        public void onFeedCreature(PlayerInteractEntityEvent e) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            Player p = e.getPlayer();
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() != Material.GOLDEN_APPLE) return;
            if (!(e.getRightClicked() instanceof LivingEntity le) || le instanceof Player) return;
            if (!isCreatureType(le.getType())) return;
            e.setCancelled(true);
            AttributeInstance max = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxHp = max == null ? le.getHealth() : max.getValue();
            if (le.getHealth() >= maxHp) {
                p.sendMessage(ChatColor.GRAY + "That creature is already at full health.");
                return;
            }
            le.setHealth(Math.min(maxHp, le.getHealth() + 250));
            if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
            else p.getInventory().setItemInMainHand(null);
            p.sendMessage(ChatColor.GOLD + "Healed 250 health. Health: " + Math.round(le.getHealth()) + "/" + Math.round(maxHp));
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onTame(PlayerInteractEntityEvent e) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            Player p = e.getPlayer();
            if (!isTamer(p.getInventory().getItemInMainHand())) return;
            e.setCancelled(true);
            if (!(e.getRightClicked() instanceof LivingEntity le) || le instanceof Player) return;
            if (!isCreatureType(le.getType())) {
                p.sendMessage(ChatColor.RED + "Only creatures can be tamed with the Creature Tamer.");
                return;
            }
            String owner = le.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (owner != null) {
                p.sendMessage(owner.equals(p.getUniqueId().toString()) ? ChatColor.GRAY + "That creature is already yours." : ChatColor.RED + "That creature is already tamed by someone else.");
                return;
            }
            tame(le, p);
        }

        private void tame(LivingEntity le, Player p) {
            le.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, p.getUniqueId().toString());
            le.setPersistent(true);
            le.setInvulnerable(true);
            if (le instanceof Mob m) m.setRemoveWhenFarAway(false);
            tamedOwner.put(le.getUniqueId(), p.getUniqueId());
            Creature td = creatureOf(le);
            if (td != null) {
                le.getPersistentDataContainer().set(creatureKey, PersistentDataType.STRING, td.id());
                applyVariant(le, td.id());
                le.setCustomName(colored(td));
            }
            le.setCustomNameVisible(true);
            p.sendMessage(ChatColor.GREEN + "Tamed. It will follow you, attack whoever attacks you, and attack whoever you attack.");
        }

        private void spawnWild(Player p, Creature def) {
            Location at = p.getLocation().clone().add(2, 0, 2);
            Entity e = at.getWorld().spawnEntity(at, def.type());
            if (!(e instanceof LivingEntity le)) { e.remove(); return; }
            le.getPersistentDataContainer().set(creatureKey, PersistentDataType.STRING, def.id());
            applyVariant(le, def.id());
            le.setInvulnerable(true);
            le.setCustomName(colored(def));
            le.setCustomNameVisible(true);
            le.setPersistent(true);
            if (le instanceof Mob m) m.setRemoveWhenFarAway(false);
            AttributeInstance sc = le.getAttribute(Attribute.GENERIC_SCALE);
            if (sc != null) sc.setBaseValue(MEDIUM_SCALE);
            AttributeInstance hp = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hp != null) hp.setBaseValue(1000);
            le.setHealth(1000);
            p.sendMessage(ChatColor.GREEN + "Spawned a wild " + def.name() + ". Right-click it with a Creature Tamer.");
        }

        /** Tamed creatures behave exactly like summoned ones: follow the owner, fight what attacks the owner, and use their ability. */
        private void tickTamed() {
            for (Map.Entry<UUID, UUID> en : new ArrayList<>(tamedOwner.entrySet())) {
                Entity ent = Bukkit.getEntity(en.getKey());
                if (!(ent instanceof LivingEntity le) || !le.isValid() || le.isDead()) {
                    tamedOwner.remove(en.getKey());
                    continue;
                }
                Player owner = Bukkit.getPlayer(en.getValue());
                if (owner == null || !owner.isOnline()) continue;
                if (le.getWorld() != owner.getWorld() || le.getLocation().distance(owner.getLocation()) > 20) {
                    le.teleport(owner.getLocation().clone().add(2, 0, 2));
                    continue;
                }
                LivingEntity target = findTarget(owner);
                if (target == null) {
                    if (le instanceof Mob m && le.getLocation().distance(owner.getLocation()) > 4) {
                        m.getPathfinder().moveTo(owner.getLocation());
                    }
                    continue;
                }
                if (le instanceof Mob m) m.getPathfinder().moveTo(target.getLocation());
                Creature def = creatureOf(le);
                long now = System.currentTimeMillis();
                if (def != null && le.getLocation().distance(target.getLocation()) <= 2.5
                        && now - tamedHitAt.getOrDefault(le.getUniqueId(), 0L) >= HIT_COOLDOWN_MS) {
                    tamedHitAt.put(le.getUniqueId(), now);
                    useAbility(def.ability(), target, owner);
                }
            }
        }

        private LivingEntity tamedTarget(Player owner, LivingEntity pet) {
            long now = System.currentTimeMillis();
            Hostile hit = lastAttackers.get(owner.getUniqueId());
            if (hit != null && now - hit.time() < ATTACKER_MEMORY_MS) {
                Player attacker = Bukkit.getPlayer(hit.attacker());
                if (attacker != null && attacker.isOnline() && attacker.getWorld() == pet.getWorld()
                        && attacker.getLocation().distance(owner.getLocation()) <= 30) return attacker;
            }
            Hostile mine = ownerTargets.get(owner.getUniqueId());
            if (mine != null && now - mine.time() < ATTACKER_MEMORY_MS) {
                Entity victim = Bukkit.getEntity(mine.attacker());
                if (victim instanceof LivingEntity lv && lv.isValid() && !lv.isDead() && lv.getWorld() == pet.getWorld()
                        && !tamedOwner.containsKey(lv.getUniqueId())) return lv;
            }
            LivingEntity best = null;
            double bestD = 10;
            for (Entity en : owner.getNearbyEntities(10, 4, 10)) {
                if (!(en instanceof Monster mob)) continue;
                double d = mob.getLocation().distance(owner.getLocation());
                if (d < bestD) {
                    bestD = d;
                    best = mob;
                }
            }
            return best;
        }

        // ===== Aura: every creature within 50 blocks of its owner gives the owner a passive =====

        private PotionEffectType auraEffect(String ability) {
            return switch (ability) {
                case "LIGHTNING", "TIDAL", "PUSH" -> PotionEffectType.STRENGTH;
                case "FIRE", "LAVA" -> PotionEffectType.FIRE_RESISTANCE;
                case "FREEZE", "STUN" -> PotionEffectType.RESISTANCE;
                case "POISON", "DRAIN", "HEAL_OWNER" -> PotionEffectType.REGENERATION;
                case "KNOCK", "LEVITATE" -> PotionEffectType.JUMP_BOOST;
                case "BLIND", "REVEAL" -> PotionEffectType.NIGHT_VISION;
                case "WIND", "SPEED_OWNER" -> PotionEffectType.SPEED;
                case "SHIELD_OWNER" -> PotionEffectType.ABSORPTION;
                default -> PotionEffectType.LUCK;
            };
        }

        private Creature defFor(EntityType t) {
            for (Creature c : CREATURES) if (c.type() == t) return c;
            return null;
        }

        private void tickAura() {
            for (Companion c : new ArrayList<>(companions.values())) {
                Player owner = Bukkit.getPlayer(c.owner);
                Entity ent = Bukkit.getEntity(c.entityId);
                if (owner == null || !owner.isOnline() || ent == null || !ent.isValid()) continue;
                if (ent.getWorld() == owner.getWorld() && ent.getLocation().distance(owner.getLocation()) <= AURA_RANGE) {
                    owner.addPotionEffect(new PotionEffect(auraEffect(c.def.ability()), 60, 0));
                }
            }
            for (Map.Entry<UUID, UUID> en : new ArrayList<>(tamedOwner.entrySet())) {
                Player owner = Bukkit.getPlayer(en.getValue());
                Entity ent = Bukkit.getEntity(en.getKey());
                if (owner == null || !owner.isOnline() || ent == null || !ent.isValid()) continue;
                Creature def = defFor(ent.getType());
                if (def != null && ent.getWorld() == owner.getWorld() && ent.getLocation().distance(owner.getLocation()) <= AURA_RANGE) {
                    owner.addPotionEffect(new PotionEffect(auraEffect(def.ability()), 60, 0));
                }
            }
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            dismiss(e.getPlayer().getUniqueId());
        }
    
        // ---------- Commands ----------
    
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 0) {
                help(sender);
                return true;
            }
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "help" -> help(sender);
                case "menu" -> {
                    if (!(sender instanceof Player mp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                    if (!mp.hasPermission("stickmancreatures.use")) { mp.sendMessage(ChatColor.RED + "You cannot summon creatures."); return true; }
                    openMenu(mp);
                }
                case "spawnwild" -> {
                    if (!sender.hasPermission("stickmancreatures.admin")) { sender.sendMessage(ChatColor.RED + "You do not have permission."); return true; }
                    if (!(sender instanceof Player sp)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                    if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /stickmancreatures spawnwild <id>"); return true; }
                    Creature wild = null;
                    for (Creature c : CREATURES) if (c.id().equalsIgnoreCase(args[1])) wild = c;
                    if (wild == null) { sender.sendMessage(ChatColor.RED + "Unknown creature: " + args[1]); return true; }
                    spawnWild(sp, wild);
                }
                case "list" -> {
                    sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "StickmanCreatures (" + CREATURES.size() + ")");
                    for (Creature c : CREATURES) {
                        sender.sendMessage(ChatColor.GRAY + " - " + colored(c) + ChatColor.GRAY + " (" + c.id() + ") "
                                + (c.fast() ? "fast" : ""));
                    }
                }
                case "info" -> {
                    if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /creature info <id>"); return true; }
                    Creature c = find(args[1]);
                    if (c == null) { sender.sendMessage(ChatColor.RED + "Unknown creature."); return true; }
                    sender.sendMessage(colored(c) + ChatColor.GRAY + " is a medium " + c.type().name().toLowerCase(Locale.ROOT)
                            + (c.fast() ? ", fast," : ",") + " with one ability:");
                    sender.sendMessage(ChatColor.YELLOW + ABILITY_TEXT.get(c.ability()));
                }
                case "summon" -> {
                    if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /creature summon <id> [player]"); return true; }
                    Creature c = find(args[1]);
                    if (c == null) { sender.sendMessage(ChatColor.RED + "Unknown creature."); return true; }
                    Player target;
                    if (args.length >= 3) {
                        if (!sender.hasPermission("stickmancreatures.admin")) { sender.sendMessage(ChatColor.RED + "You cannot summon for other players."); return true; }
                        target = Bukkit.getPlayerExact(args[2]);
                        if (target == null) { sender.sendMessage(ChatColor.RED + "That player is not online."); return true; }
                    } else if (sender instanceof Player p) {
                        if (!p.hasPermission("stickmancreatures.use")) { p.sendMessage(ChatColor.RED + "You cannot summon creatures."); return true; }
                        target = p;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Give a player name from the console.");
                        return true;
                    }
                    summon(target, c);
                }
                case "dismiss" -> {
                    if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                    dismiss(p.getUniqueId());
                    p.sendMessage(ChatColor.GRAY + "Your creature was returned.");
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Try /creature help");
            }
            return true;
        }
    
        private void help(CommandSender s) {
            s.sendMessage(ChatColor.GOLD + "StickmanCreatures");
            s.sendMessage(ChatColor.YELLOW + "/stickmancreatures menu" + ChatColor.GRAY + " - open the creature menu");
            s.sendMessage(ChatColor.YELLOW + "/creature list" + ChatColor.GRAY + " - all 30 creatures");
            s.sendMessage(ChatColor.YELLOW + "/creature info <id>" + ChatColor.GRAY + " - ability and details");
            s.sendMessage(ChatColor.YELLOW + "/creature summon <id> [player]" + ChatColor.GRAY + " - bring a creature out");
            s.sendMessage(ChatColor.YELLOW + "/creature dismiss" + ChatColor.GRAY + " - send your creature away");
        }
    
        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
            if (args.length == 1) {
                return List.of("list", "info", "summon", "dismiss", "help").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("summon"))) {
                return CREATURES.stream().map(Creature::id)
                        .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
            }
            return List.of();
        }
    
    }
}
