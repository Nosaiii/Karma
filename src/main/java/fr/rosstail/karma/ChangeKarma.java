package fr.rosstail.karma;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class ChangeKarma {

    Karma karma;
    public void checkKarmaLimit(Player player) {
        System.out.println(player.getName());
        File file = new File(karma.getDataFolder(), "playerdata/" + player.getUniqueId() + ".yml");
        System.out.println("THIS IS A TEST");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        System.out.println("THIS IS A TEST 1");
        if (configuration.getInt("karma") > karma.getConfig().getInt("karma.maximum-karma")) {
            System.out.println("THIS IS A TEST 1A");
            setKarmaToMinimum(player, file, configuration);
        }
        else if (configuration.getInt("karma") < karma.getConfig().getInt("karma.minimum-karma")) {
            System.out.println("THIS IS A TEST 1B");
            setKarmaToMaximum(player, file, configuration);
        }
    }

    public void setKarmaToMinimum(Player player, File file, YamlConfiguration configuration) {
        try {
            configuration.set("karma", karma.getConfig().getInt("karma.maximum-karma"));
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(player.getName() +
                " has a karma higher than maximum, now set to maximum karma defined in config.yml");
    }

    public void setKarmaToMaximum(Player player, File file, YamlConfiguration configuration) {
        try {
            configuration.set("karma", karma.getConfig().getInt("karma.minimum-karma"));
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(player.getName() +
                " has a karma higher than maximum, now set to minimum karma defined in config.yml");
    }
}
