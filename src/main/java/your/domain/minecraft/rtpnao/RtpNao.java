package your.domain.minecraft.rtpnao; // パッケージ名を変更

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class RtpNao extends JavaPlugin implements CommandExecutor, Listener, TabCompleter {

    // 最後にテレポートした時間 (Cooldown用)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // 待機中のタスク (Warmup用)
    private final Map<UUID, BukkitTask> warmupTasks = new HashMap<>();

    @Override
    public void onEnable() {
        // 設定ファイルの保存と読み込み
        saveDefaultConfig();

        // コマンドとイベントの登録
        if (getCommand("rtp") != null) {
            getCommand("rtp").setExecutor(this);
            getCommand("rtp").setTabCompleter(this);
        }
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("RTP-nao has been enabled!");
    }

    @Override
    public void onDisable() {
        // リロードや停止時にタスクが残らないようにキャンセル
        warmupTasks.values().forEach(BukkitTask::cancel);
        warmupTasks.clear();
    }

    // --- コマンド実行時の処理 ---
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. リロードコマンド (/rtp reload)
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rtpnao.reload")) {
                sender.sendMessage(color("&c権限がありません。"));
                return true;
            }
            reloadConfig();
            sender.sendMessage(color("&a[RTP-nao] Config reloaded!"));
            return true;
        }

        // 2. プレイヤー確認
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can use this command."));
            return true;
        }

        // 3. 権限確認
        if (!player.hasPermission("rtpnao.use")) {
            sender.sendMessage(color("&c権限がありません。"));
            return true;
        }

        // 4. ワールド確認
        List<String> allowedWorlds = getConfig().getStringList("allowed-worlds");
        if (!allowedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(msg("invalid-world"));
            return true;
        }

        // 5. 二重実行の防止
        if (warmupTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(color("&c現在テレポート準備中です！動かないでください。"));
            return true;
        }

        // 6. クールタイム確認
        if (!player.hasPermission("rtpnao.bypass.cooldown")) {
            long cooldownTime = getConfig().getInt("cooldown") * 1000L;
            if (cooldowns.containsKey(player.getUniqueId())) {
                long lastUsed = cooldowns.get(player.getUniqueId());
                long timePassed = System.currentTimeMillis() - lastUsed;

                if (timePassed < cooldownTime) {
                    long timeLeft = (cooldownTime - timePassed) / 1000;
                    player.sendMessage(msg("cooldown", "%time%", String.valueOf(timeLeft)));
                    return true;
                }
            }
        }

        // === RTPロジック開始 ===
        int warmupSeconds = getConfig().getInt("warmup");

        // 裏で計算を開始 (非同期・再帰呼び出し)
        // 20回まで試行
        CompletableFuture<Location> searchFuture = findSafeLocationAsync(player.getWorld(), 20);

        // 権限持ち または 待機時間0 の場合は即時実行
        if (player.hasPermission("rtpnao.bypass.warmup") || warmupSeconds <= 0) {
            executeTeleport(player, searchFuture);
        } else {
            startWarmup(player, warmupSeconds, searchFuture);
        }

        return true;
    }

    // --- タブ補完 (入力補助) ---
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("rtpnao.reload")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }

    // --- 待機処理 (Warmup) ---
    private void startWarmup(Player player, int seconds, CompletableFuture<Location> searchFuture) {
        player.sendMessage(msg("warmup-start", "%time%", String.valueOf(seconds)));

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                // プレイヤーがログアウトしていたらキャンセル
                if (!player.isOnline()) {
                    warmupTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                // カウントダウン終了
                if (timeLeft <= 0) {
                    warmupTasks.remove(player.getUniqueId());
                    this.cancel();
                    executeTeleport(player, searchFuture);
                    return;
                }

                // タイトル表示
                Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1200), Duration.ofMillis(200));
                Title title = Title.title(Component.empty(), color("&eテレポートまで &c" + timeLeft + " &e秒"), times);
                player.showTitle(title);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);

                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L); // 1秒間隔

        warmupTasks.put(player.getUniqueId(), task);
    }

    // --- テレポート実行 ---
    private void executeTeleport(Player player, CompletableFuture<Location> searchFuture) {
        if (!searchFuture.isDone()) {
            player.sendMessage(color("&e最適な場所を最終調整しています..."));
        }

        searchFuture.thenAccept(location -> {
            if (location != null) {
                // Paper API の teleportAsync を使用して安全にテレポート
                player.teleportAsync(location).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(msg("success"));
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    } else {
                        player.sendMessage(color("&cテレポートに失敗しました。"));
                    }
                });
            } else {
                player.sendMessage(msg("not-safe"));
            }
        });
    }

    // --- 移動検知 (キャンセル処理) ---
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // 監視対象でなければ無視
        if (!warmupTasks.containsKey(player.getUniqueId())) return;

        // 視点移動のみは許可
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // 動いたのでキャンセル
        BukkitTask task = warmupTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendMessage(msg("warmup-cancel"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.clearTitle();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BukkitTask task = warmupTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
    }

    // --- 安全な場所の検索 (非同期対応・修正版) ---
    private CompletableFuture<Location> findSafeLocationAsync(World world, int attempts) {
        if (attempts <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        int maxRadius = getConfig().getInt("radius.max", 5000);
        int minRadius = getConfig().getInt("radius.min", 500);

        // 座標生成 (ThreadLocalRandomを使用)
        int x = generateRandomCoord(maxRadius, minRadius);
        int z = generateRandomCoord(maxRadius, minRadius);

        // チャンクを非同期でロードしてから、安全確認を行う
        // Paper APIの getChunkAtAsync は完了時にメインスレッドに戻してくれる場合が多いが、
        // 念のためブロック操作は安全に行う必要がある。
        return world.getChunkAtAsync(x >> 4, z >> 4).thenCompose(chunk -> {
            // ブロック情報はメインスレッドで取得する必要がある
            // getChunkAtAsyncの完了コールバックはメインスレッドで実行される
            int y = world.getHighestBlockYAt(x, z);
            Block block = world.getBlockAt(x, y - 1, z);

            if (isSafe(block.getType())) {
                return CompletableFuture.completedFuture(new Location(world, x + 0.5, y + 1, z + 0.5));
            } else {
                // 失敗したら再帰呼び出し (試行回数を減らす)
                return findSafeLocationAsync(world, attempts - 1);
            }
        });
    }

    private int generateRandomCoord(int max, int min) {
        // 範囲チェック
        if (min >= max) min = 0;
        
        // min ～ max の範囲でランダムな値を生成
        int range = max - min;
        int coord = java.util.concurrent.ThreadLocalRandom.current().nextInt(range + 1) + min;
        
        // 50%の確率で負の値にする (全方位に対応)
        if (java.util.concurrent.ThreadLocalRandom.current().nextBoolean()) {
            coord = -coord;
        }
        return coord;
    }

    private boolean isSafe(Material type) {
        // 危険なブロックを除外
        return type != Material.WATER && type != Material.LAVA &&
                type != Material.MAGMA_BLOCK && type != Material.CACTUS && !type.isAir();
    }

    // --- 便利メソッド ---
    private Component msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String text = getConfig().getString("messages." + key, "");
        return color(prefix + text);
    }

    private Component msg(String key, String placeholder, String value) {
        String prefix = getConfig().getString("messages.prefix", "");
        String text = getConfig().getString("messages." + key, "");
        if (text == null) return color("&cMessage Missing: " + key);
        return color((prefix + text).replace(placeholder, value));
    }

    private Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}