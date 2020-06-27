package fr.rosstail.karma;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.io.File;


/**
 * Changes the attacker karma when attacking entities
 */
public class HitEvents implements Listener {
    private final Karma plugin;
    private final File langFile;
    private final YamlConfiguration configLang;
    private final AdaptMessage adaptMessage;

    private Player attacker;
    private Player victim;
    private double damage;
    private String message;

    HitEvents(Karma plugin) {
        super();
        this.plugin = plugin;
        this.langFile = new File(plugin.getDataFolder(),
            "lang/" + plugin.getConfig().getString("general.lang") + ".yml");
        this.configLang = YamlConfiguration.loadConfiguration(langFile);
        this.adaptMessage = new AdaptMessage(plugin);
    }

    /**
     * Changes karma when player attack another entity (animal or monster)
     *
     * @param event
     */
    @EventHandler public void onEntityHurt(EntityDamageByEntityEvent event) {
        double reward;
        double attackerKarma;
        double attackerModifiedKarma;
        LivingEntity livingEntity;
        String livingEntityName;
        attacker = null;
        if (!(event.getEntity() instanceof LivingEntity && event.getFinalDamage() >= 1d
            && ((LivingEntity) event.getEntity()).getHealth() - event.getFinalDamage() > 0)) {
            return;
        }

        damage = event.getFinalDamage();
        livingEntity = (LivingEntity) event.getEntity();
        livingEntityName = livingEntity.toString().replaceAll("Craft", "");
        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            } else {
                return;
            }
        } else if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else {
            return;
        }
        DataHandler playerData = DataHandler.gets(attacker, plugin);

        if (attacker.hasMetadata("NPC") || !playerData.getTime()) {
            return;
        }

        if (livingEntity instanceof Player && attacker != null) {
            victim = ((Player) livingEntity).getPlayer();
            onPlayerHurt();
            return;
        }

        reward = plugin.getConfig().getDouble("entities." + livingEntityName + ".hit-karma-reward");

        if (!(reward == 0 || attacker == null)) {
            attackerKarma = playerData.getPlayerKarma();

            if (Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard") && plugin
                .getConfig().getBoolean("general.use-worldguard")) {

                WGPreps wgPreps = new WGPreps();
                double mult = wgPreps.checkMultipleKarmaFlags(attacker);
                reward = reward * mult;
            }

            attackerModifiedKarma = attackerKarma + reward;

            playerData.setKarmaToPlayer(attackerModifiedKarma);
        }

        message = plugin.getConfig().getString("entities." + livingEntityName + ".hit-message");

        if (attacker != null) {
            adaptMessage.entityHitMessage(message, attacker, reward);
        }
    }

    /**
     * Launch When a player is hurt by another player.
     */
    public void onPlayerHurt() {
        DataHandler attackerData = DataHandler.gets(attacker, plugin);
        DataHandler victimData = DataHandler.gets(victim, plugin);

        double attackerInitialKarma = attackerData.getPlayerKarma();
        double victimKarma = victimData.getPlayerKarma();

        if (!(!(victim.getName().equals(attacker.getName())) && damage >= 1d)) {
            return;
        }
        double arg1 = plugin.getConfig().getDouble("pvp.hit-reward-variables.1");
        String arg2Str = plugin.getConfig().getString("pvp.hit-reward-variables.2");
        double arg2 = 0;
        double arg3 = plugin.getConfig().getDouble("pvp.hit-reward-variables.3");
        double arg4 = plugin.getConfig().getDouble("pvp.hit-reward-variables.4");

        if (arg2Str != null) {
            if (arg2Str.equalsIgnoreCase("<VICTIM_KARMA>")) {
                if (!isVictimNPC()) {
                    arg2 = victimKarma;
                } else if (isVictimNPCHaveKarma()){
                    arg2 = victim.getMetadata("Karma").get(0).asDouble();
                } else {
                    return;
                }
            } else {
                try {
                    arg2 = Double.parseDouble(arg2Str);
                } catch (NumberFormatException e) {
                    System.out.println("Player" + victim.getName() + " has wrong Karma value.");
                    return;
                }
            }
        }

        double formula = arg1 * (arg2 + arg3) / arg4;

        if (Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard") && plugin
            .getConfig().getBoolean("general.use-worldguard")) {
            WGPreps wgPreps = new WGPreps();
            double mult = wgPreps.checkMultipleKarmaFlags(attacker);
            formula = formula * mult;
        }

        double attackerNewKarma = attackerInitialKarma + formula;

        if (plugin.getConfig().getBoolean("pvp.crime-time.enable") && !(attacker.hasMetadata("NPC")
            || victim.hasMetadata("NPC"))) {
            long timeStamp = System.currentTimeMillis();
            long delay = plugin.getConfig().getLong("pvp.crime-time.delay");

            double attackStart = attackerData.getPlayerLastAttack();
            double attackEnd = attackerData.getPlayerLastAttack() + delay * 1000;
            double victimStart = victimData.getPlayerLastAttack();
            double victimEnd = victimData.getPlayerLastAttack() + delay * 1000;

            if (attackStart != 0L
                && victimStart != 0L) {
                if ((timeStamp >= attackStart && timeStamp <= attackEnd)
                    || timeStamp > victimEnd) {
                    attackerData.setLastAttackToPlayer();
                } else {
                    if (!doesDefendChangeKarma(attackerInitialKarma, attackerNewKarma)) {
                        message = configLang.getString("self-defending-off");
                        adaptMessage.message(null, attacker, 0, message);
                        return;
                    }
                    message = configLang.getString("self-defending-on");
                    adaptMessage.message(null, attacker, 0, message);
                }
            } else if (attackStart == 0L) {
                attackerData.setLastAttackToPlayer();
            } else if (victimStart != 0L) {
                if (timeStamp >= victimStart && timeStamp <= victimEnd) {
                    if (!doesDefendChangeKarma(attackerInitialKarma, attackerNewKarma)) {
                        message = configLang.getString("self-defending-off");
                        adaptMessage.message(null, attacker, 0, message);
                        return;
                    }
                    message = configLang.getString("self-defending-on");
                    adaptMessage.message(null, attacker, 0, message);
                } else {
                    attackerData.setLastAttackToPlayer();
                }
            }

        }

        attackerData.setKarmaToPlayer(attackerNewKarma);

        message = null;
        if (attackerNewKarma > attackerInitialKarma) {
            message = plugin.getConfig().getString("pvp.hit-message-on-karma-increase");
        } else if (attackerNewKarma < attackerInitialKarma) {
            message = plugin.getConfig().getString("pvp.hit-message-on-karma-decrease");
        }
        if (message != null) {
            adaptMessage.playerHitMessage(message, attacker, victim, attackerInitialKarma);
        }
    }

    private boolean isVictimNPC() {
        return victim.hasMetadata("NPC");
    }

    private boolean isVictimNPCHaveKarma() {
        return victim.hasMetadata("Karma") && victim.getMetadata("Karma").get(0) != null;
    }

    private boolean doesDefendChangeKarma(double attackerInitialKarma, double attackerNewKarma) {
        if (attackerNewKarma > attackerInitialKarma) {
            return plugin.getConfig().getBoolean("pvp.crime-time.active-on-up");
        } else if (attackerNewKarma == attackerInitialKarma) {
            return plugin.getConfig().getBoolean("pvp.crime-time.active-on-still");
        } else {
            return plugin.getConfig().getBoolean("pvp.crime-time.active-on-down");
        }
    }
}
