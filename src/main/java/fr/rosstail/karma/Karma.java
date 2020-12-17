package fr.rosstail.karma;


import java.io.File;
import java.io.IOException;
import java.sql.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class and methods of the plugin
 */
public class Karma extends JavaPlugin implements Listener {

    public Connection connection;
    public String host, database, username, password;
    public int port;

    @Override
    public void onLoad() {
        if (Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            new WGPreps().worldGuardHook();
        }
    }

    public void onEnable() {

        this.saveDefaultConfig();

        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {

                Bukkit.getPluginManager().registerEvents(this, this);

            } else {
                throw new RuntimeException("Could not find PlaceholderAPI!! Plugin can not work without it!");
            }
        }

        if (this.getConfig().getBoolean("mysql.active")) {
            prepareConnection();
        } else {
            this.createPlayerDataFolder();
        }

        this.createLangFiles();
        Bukkit.getPluginManager().registerEvents(new PlayerConnect(this), this);
        Bukkit.getPluginManager().registerEvents(new KillEvents(this), this);
        Bukkit.getPluginManager().registerEvents(new HitEvents(this), this);
        this.getCommand("karma").setExecutor(new KarmaCommand(this));
    }

    private void prepareConnection() {
        host = this.getConfig().getString("mysql.host");
        database = this.getConfig().getString("mysql.database");
        username = this.getConfig().getString("mysql.username");
        password = this.getConfig().getString("mysql.password");
        port = this.getConfig().getInt("mysql.port");
        try {
            openConnection();
            setTableToDataBase();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database, this.username, this.password);
        }
    }


    public void setTableToDataBase() {
        String sql = "CREATE TABLE IF NOT EXISTS Karma ( UUID varchar(40) PRIMARY KEY UNIQUE NOT NULL,\n" +
                " NickName varchar(16) NOT NULL,\n" +
                " Karma double,\n" +
                " Tier varchar(50),\n" +
                " Last_Attack bigint(20));";
        try {
            if (connection != null && !connection.isClosed()) {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the folder for player's datas
     */
    public void createPlayerDataFolder() {
        File folder = new File(this.getDataFolder(), "playerdata/");
        if (!folder.exists()) {
            String message = this.getConfig().getString("messages.creating-playerdata-folder");
            if (message != null) {
                message = ChatColor.translateAlternateColorCodes('&', message);

                getServer().getConsoleSender().sendMessage(message);
            }
            folder.mkdir();
        }
    }

    /**
     * Create the subfolder and files for languages
     */
    public void createLangFiles() {
        try {
            FileResourcesUtils.main("lang", this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
