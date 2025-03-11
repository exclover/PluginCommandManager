# Profesyonel Komut Yönetim Sistemi

Bu profesyonel komut yönetim sistemi, Minecraft plugin geliştirme için endüstri standartlarına uygun tasarlanmış kapsamlı bir çözümdür. 

## Öne Çıkan Özellikler

### Builder Deseni Uygulaması
- Akıcı arayüz (Fluent Interface) ile daha okunabilir kod
- Zincirleme metot çağrıları ile kolay komut yapılandırması
- İsteğe bağlı özellikler için esnek ayarlama

### Komut Grubu Desteği
- İlişkili komutları gruplandırma imkanı
- Grup tabanlı işlemler (tüm grup komutlarını kaldırma gibi)
- Grup üyeliğini dinamik olarak yönetme

### Konfigürasyon Entegrasyonu
- YAML dosyalarından komut yapılandırması yükleme
- Dinamik sınıf yükleme ile esnek yapı
- Komut yapılandırmalarını çalışma zamanında değiştirebilme

### Gelişmiş Hata Yönetimi
- Her seviyede ayrıntılı hata kontrolü
- Anlamlı log mesajları
- Zincirleme hatalardan geri kazanma mekanizmaları

---

## Kullanım Örneği

### 1. Basit Komut Kaydı
```java
// Ana sınıfta
private CommandManager commandManager;

@Override
public void onEnable() {
    commandManager = new CommandManager(this);
    
    // Builder kullanarak komut oluşturma ve kaydetme
    commandManager.command("example", new PlayerCommand(this))
        .description("Örnek bir komut")
        .usage("/example [param]")
        .permission("example.use")
        .permissionMessage("\u00a7cBu komutu kullanma izniniz yok!")
        .aliases("ex", "demo", "test")
        .tabCompleter(new ExampleTabCompleter())
        .group("utilityCommands")
        .register();
}

@Override
public void onDisable() {
    if (commandManager != null) {
        commandManager.unregisterAllCommands();
    }
}
```

### 2. Konfigürasyondan Komut Yükleme
```yaml
# config.yml dosyası
commands:
  example:
    name: example
    executor: org.exclover.example.commands.PlayerCommand
    description: Örnek bir komut
    usage: /example [param]
    permission: example.use
    permissionMessage: §cBu komutu kullanma izniniz yok!
    aliases:
      - ex
      - demo
      - test
    tabCompleter: org.exclover.example.commands.ExampleTabCompleter
    group: utilityCommands
```
```java
// Ana sınıfta
@Override
public void onEnable() {
    saveDefaultConfig();
    
    commandManager = new CommandManager(this);
    int loadedCount = commandManager.loadCommandsFromConfig(getConfig().getConfigurationSection("commands"));
    getLogger().info(loadedCount + " komut config dosyasından yüklendi.");
}
```

### 3. Grup İşlemleri
```java
// Belirli bir gruptaki tüm komutları göster
List<PluginCommand> utilityCommands = commandManager.getCommandsByGroup("utilityCommands");
for (PluginCommand cmd : utilityCommands) {
    getLogger().info(cmd.getName() + " - " + cmd.getDescription());
}

// Sadece belirli bir grubun komutlarını devre dışı bırak
int disabledCount = commandManager.unregisterCommandGroup("adminCommands");
getLogger().info(disabledCount + " admin komutu devre dışı bırakıldı.");
```

---

Bu profesyonel komut yönetim sistemi, kodunuzu daha okunabilir, bakımı kolay ve ölçeklenebilir hale getirecek ve geniş ölçekli plugin projeleri için idealdir. 
Tasarım desenleri, konfigürasyon entegrasyonu ve grup tabanlı yapı ile modüler plugin mimarisi oluşturmanıza olanak tanır.

