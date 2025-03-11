
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.exclover.coinflip.CoinFlip;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Profesyonel Komut Yönetim Sistemi.
 * <p>
 * Bu sınıf plugin'in komut yapısını yönetmek için gelişmiş özellikler sunar:
 * - Çoklu komut desteği
 * - Dinamik komut kaydı ve kaldırma
 * - Alias (alternatif ad) yönetimi
 * - İzin yönetimi
 * - Tab completion desteği
 * - Komut grupları desteği
 * - Konfigürasyon entegrasyonu
 * </p>
 * 
 * @author exclover
 * @version 2.1.0
 */
public class CommandManager {
    private final CoinFlip plugin;
    private final Map<String, PluginCommand> registeredCommands;
    private final Map<String, Set<String>> commandGroups;
    private CommandMap bukkitCommandMap;

    /**
     * CommandManager sınıfının yapıcı metodu
     *
     * @param plugin Komut kaydedilecek plugin
     */
    public CommandManager(CoinFlip plugin) {
        this.plugin = plugin;
        this.registeredCommands = new HashMap<>();
        this.commandGroups = new HashMap<>();
        this.bukkitCommandMap = initCommandMap();
    }

    /**
     * Yeni bir komut inşa etmek için builder sınıfı
     */
    public class CommandBuilder {
        private String name;
        private CommandExecutor executor;
        private TabCompleter tabCompleter;
        private String description = "";
        private String usage;
        private String permission;
        private String permissionMessage;
        private List<String> aliases = new ArrayList<>();
        private String group;

        /**
         * CommandBuilder yapıcı metodu
         *
         * @param name Ana komut adı
         * @param executor Komut yürütücüsü
         */
        public CommandBuilder(String name, CommandExecutor executor) {
            this.name = name;
            this.executor = executor;
            this.usage = "/" + name;
        }

        /**
         * Komut açıklaması belirle
         *
         * @param description Komut açıklaması
         * @return Builder örneği (zincir metodlar için)
         */
        public CommandBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Komut kullanım şekli belirle
         *
         * @param usage Komut kullanımı
         * @return Builder örneği
         */
        public CommandBuilder usage(String usage) {
            this.usage = usage;
            return this;
        }

        /**
         * Komut için gerekli izin belirle
         *
         * @param permission İzin adı
         * @return Builder örneği
         */
        public CommandBuilder permission(String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * İzni olmayanlara gösterilecek mesaj
         *
         * @param permissionMessage İzin mesajı
         * @return Builder örneği
         */
        public CommandBuilder permissionMessage(String permissionMessage) {
            this.permissionMessage = permissionMessage;
            return this;
        }

        /**
         * Alternatif komut adları belirle
         *
         * @param aliases Alternatif adlar
         * @return Builder örneği
         */
        public CommandBuilder aliases(String... aliases) {
            this.aliases = Arrays.asList(aliases);
            return this;
        }

        /**
         * Tab tamamlayıcı belirle
         *
         * @param tabCompleter Tab tamamlayıcı
         * @return Builder örneği
         */
        public CommandBuilder tabCompleter(TabCompleter tabCompleter) {
            this.tabCompleter = tabCompleter;
            return this;
        }

        /**
         * Komut grubu belirle
         *
         * @param group Grup adı
         * @return Builder örneği
         */
        public CommandBuilder group(String group) {
            this.group = group;
            return this;
        }

        /**
         * Komutu oluştur ve kaydet
         *
         * @return Kayıt başarılı ise true
         */
        public boolean register() {
            try {
                // CommandMap kontrolü
                if (bukkitCommandMap == null) {
                    bukkitCommandMap = initCommandMap();
                    if (bukkitCommandMap == null) {
                        throw new IllegalStateException("CommandMap bulunamadı.");
                    }
                }

                // Programatik olarak komut oluştur
                final Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);
                PluginCommand command = constructor.newInstance(name, plugin);
                
                // Komut özelliklerini ayarla
                command.setExecutor(executor);
                command.setDescription(description);
                command.setUsage(usage);
                
                if (permission != null && !permission.isEmpty()) {
                    command.setPermission(permission);
                }
                
                if (permissionMessage != null && !permissionMessage.isEmpty()) {
                    command.setPermissionMessage(permissionMessage);
                }
                
                if (!aliases.isEmpty()) {
                    command.setAliases(aliases);
                }
                
                if (tabCompleter != null) {
                    command.setTabCompleter(tabCompleter);
                }
                
                // Komutu kaydet
                bukkitCommandMap.register(plugin.getName().toLowerCase(), command);
                registeredCommands.put(name.toLowerCase(), command);
                
                // Komut grubuna ekle
                if (group != null && !group.isEmpty()) {
                    addCommandToGroup(name, group);
                }
                
                plugin.getLogger().info(String.format("'%s' komutu başarıyla kaydedildi. %s", 
                    name, 
                    !aliases.isEmpty() ? "Alternatifler: " + String.join(", ", aliases) : ""
                ));
                
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Komut kaydı yapılamadı: " + name, e);
                return false;
            }
        }
    }

    /**
     * Yeni bir komut oluşturmak için builder başlat
     *
     * @param name Komut adı
     * @param executor Komut yürütücüsü
     * @return CommandBuilder örneği
     */
    public CommandBuilder command(String name, CommandExecutor executor) {
        return new CommandBuilder(name, executor);
    }

    /**
     * Konfigürasyon dosyasından komutları yükle
     *
     * @param configSection Komutlar için konfigürasyon bölümü
     * @return Yüklenen komut sayısı
     */
    public int loadCommandsFromConfig(ConfigurationSection configSection) {
        if (configSection == null) {
            return 0;
        }

        int loadedCommands = 0;
        for (String commandKey : configSection.getKeys(false)) {
            ConfigurationSection cmdSection = configSection.getConfigurationSection(commandKey);
            if (cmdSection == null) continue;

            String name = cmdSection.getString("name", commandKey);
            String executorClass = cmdSection.getString("executor");
            if (executorClass == null) {
                plugin.getLogger().warning("Komut için executor belirtilmemiş: " + name);
                continue;
            }

            try {
                // Executor sınıfını dinamik olarak yükle
                Class<?> clazz = Class.forName(executorClass);
                if (!CommandExecutor.class.isAssignableFrom(clazz)) {
                    plugin.getLogger().warning(executorClass + " sınıfı CommandExecutor arayüzünü uygulamıyor.");
                    continue;
                }

                CommandExecutor executor = (CommandExecutor) clazz.getDeclaredConstructor(CoinFlip.class).newInstance(plugin);
                
                // Builder ile komutu oluştur
                CommandBuilder builder = command(name, executor)
                    .description(cmdSection.getString("description", ""))
                    .usage(cmdSection.getString("usage", "/" + name))
                    .permission(cmdSection.getString("permission", ""))
                    .permissionMessage(cmdSection.getString("permissionMessage", ""));
                
                // Aliases
                if (cmdSection.contains("aliases")) {
                    List<String> aliases = cmdSection.getStringList("aliases");
                    builder.aliases(aliases.toArray(new String[0]));
                }
                
                // Tab Completer
                String tabCompleterClass = cmdSection.getString("tabCompleter");
                if (tabCompleterClass != null) {
                    Class<?> tcClass = Class.forName(tabCompleterClass);
                    if (TabCompleter.class.isAssignableFrom(tcClass)) {
                        TabCompleter tabCompleter = (TabCompleter) tcClass.getDeclaredConstructor(CoinFlip.class).newInstance(plugin);
                        builder.tabCompleter(tabCompleter);
                    }
                }
                
                // Grup
                if (cmdSection.contains("group")) {
                    builder.group(cmdSection.getString("group"));
                }
                
                // Komutu kaydet
                if (builder.register()) {
                    loadedCommands++;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Komut yüklenirken hata oluştu: " + name, e);
            }
        }
        
        return loadedCommands;
    }

    /**
     * Belirli bir komutu CommandMap'ten kaldır
     *
     * @param commandName Kaldırılacak komut adı
     * @return Kaldırma işleminin başarılı olup olmadığı
     */
    public boolean unregisterCommand(String commandName) {
        PluginCommand command = registeredCommands.get(commandName.toLowerCase());
        if (command == null) {
            plugin.getLogger().warning("Kaldırılacak komut bulunamadı: " + commandName);
            return false;
        }

        try {
            // CommandMap kontrolü
            if (bukkitCommandMap == null) {
                bukkitCommandMap = initCommandMap();
                if (bukkitCommandMap == null) {
                    throw new IllegalStateException("CommandMap bulunamadı.");
                }
            }
            
            // knownCommands map'ine erişim sağla
            Field knownCommandsField = getKnownCommandsField(bukkitCommandMap);
            if (knownCommandsField == null) {
                throw new IllegalStateException("knownCommands field'ına erişilemedi.");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(bukkitCommandMap);
            
            // Ana komut ve namespace'li halini kaldır
            knownCommands.remove(command.getName().toLowerCase());
            knownCommands.remove(plugin.getName().toLowerCase() + ":" + command.getName().toLowerCase());
            
            // Aliases'ları da kaldır
            if (command.getAliases() != null) {
                for (String alias : command.getAliases()) {
                    knownCommands.remove(alias.toLowerCase());
                    knownCommands.remove(plugin.getName().toLowerCase() + ":" + alias.toLowerCase());
                }
            }
            
            // Komut gruplarından kaldır
            for (Set<String> commands : commandGroups.values()) {
                commands.remove(commandName.toLowerCase());
            }
            
            // Kayıtlı komutlardan kaldır
            registeredCommands.remove(commandName.toLowerCase());
            
            plugin.getLogger().info("'" + commandName + "' komutu ve tüm alternatifleri başarıyla kaldırıldı.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Komut kaldırılamadı: " + commandName, e);
            return false;
        }
    }

    /**
     * Belirli bir gruptaki tüm komutları kaldır
     *
     * @param groupName Grup adı
     * @return Kaldırılan komut sayısı
     */
    public int unregisterCommandGroup(String groupName) {
        Set<String> commands = commandGroups.get(groupName.toLowerCase());
        if (commands == null || commands.isEmpty()) {
            return 0;
        }
        
        List<String> commandsToRemove = new ArrayList<>(commands);
        int count = 0;
        
        for (String commandName : commandsToRemove) {
            if (unregisterCommand(commandName)) {
                count++;
            }
        }
        
        commandGroups.remove(groupName.toLowerCase());
        return count;
    }

    /**
     * Tüm kayıtlı komutları kaldır
     *
     * @return Kaldırılan komut sayısı
     */
    public int unregisterAllCommands() {
        List<String> commandsToRemove = new ArrayList<>(registeredCommands.keySet());
        int count = 0;
        
        for (String commandName : commandsToRemove) {
            if (unregisterCommand(commandName)) {
                count++;
            }
        }
        
        commandGroups.clear();
        return count;
    }

    /**
     * Komutu bir gruba ekle
     *
     * @param commandName Komut adı
     * @param groupName Grup adı
     */
    public void addCommandToGroup(String commandName, String groupName) {
        Set<String> commands = commandGroups.computeIfAbsent(
            groupName.toLowerCase(), k -> new HashSet<>()
        );
        commands.add(commandName.toLowerCase());
    }

    /**
     * Komutu bir gruptan çıkar
     *
     * @param commandName Komut adı
     * @param groupName Grup adı
     * @return Başarılı ise true
     */
    public boolean removeCommandFromGroup(String commandName, String groupName) {
        Set<String> commands = commandGroups.get(groupName.toLowerCase());
        if (commands == null) {
            return false;
        }
        return commands.remove(commandName.toLowerCase());
    }

    /**
     * Belirli bir gruptaki tüm komutları al
     *
     * @param groupName Grup adı
     * @return Komutlar listesi
     */
    public List<PluginCommand> getCommandsByGroup(String groupName) {
        Set<String> commandNames = commandGroups.get(groupName.toLowerCase());
        if (commandNames == null || commandNames.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<PluginCommand> groupCommands = new ArrayList<>();
        for (String name : commandNames) {
            PluginCommand cmd = registeredCommands.get(name);
            if (cmd != null) {
                groupCommands.add(cmd);
            }
        }
        
        return groupCommands;
    }

    /**
     * CommandMap'i reflection ile başlat
     *
     * @return Sunucunun CommandMap nesnesi, başarısız olursa null
     */
    private CommandMap initCommandMap() {
        try {
            Server server = Bukkit.getServer();
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(server);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "CommandMap alınamadı", e);
            return null;
        }
    }

    /**
     * KnownCommands field'ına erişim sağla
     *
     * @param commandMap CommandMap nesnesi
     * @return knownCommands field'ı, başarısız olursa null
     */
    private Field getKnownCommandsField(CommandMap commandMap) {
        try {
            // Önce SimpleCommandMap sınıfında knownCommands field'ını ara
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            return knownCommandsField;
        } catch (NoSuchFieldException e) {
            // Bulamazsa üst sınıflarda ara
            Class<?> clazz = commandMap.getClass().getSuperclass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField("knownCommands");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ex) {
                    clazz = clazz.getSuperclass();
                } catch (Exception ex) {
                    break;
                }
            }
            
            plugin.getLogger().log(Level.SEVERE, "knownCommands field'ı bulunamadı", e);
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "knownCommands field'ına erişilemedi", e);
            return null;
        }
    }

    /**
     * Kayıtlı komutu döndürür
     *
     * @param commandName Komut adı
     * @return Kayıtlı PluginCommand nesnesi, yoksa null
     */
    public PluginCommand getCommand(String commandName) {
        return registeredCommands.get(commandName.toLowerCase());
    }
    
    /**
     * Tüm kayıtlı komutları döndürür
     *
     * @return Kayıtlı komutlar listesi
     */
    public List<PluginCommand> getAllCommands() {
        return new ArrayList<>(registeredCommands.values());
    }
    
    /**
     * Belirli bir komutun kayıtlı olup olmadığını kontrol eder
     *
     * @param commandName Kontrol edilecek komut adı
     * @return Komutun kayıtlı olup olmadığı
     */
    public boolean isCommandRegistered(String commandName) {
        return registeredCommands.containsKey(commandName.toLowerCase());
    }
    
    /**
     * Tüm komut gruplarını döndürür
     *
     * @return Grup adlarını içeren bir Set
     */
    public Set<String> getAllGroups() {
        return new HashSet<>(commandGroups.keySet());
    }
}
