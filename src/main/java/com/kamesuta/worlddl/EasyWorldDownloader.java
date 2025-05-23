package com.kamesuta.worlddl;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class EasyWorldDownloader extends JavaPlugin {
    private String binId;
    private String archivePrefix;
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.00");

    @Override
    public void onEnable() {
        // コンフィグの保存
        saveDefaultConfig();
        
        // コンフィグの読み込み
        FileConfiguration config = getConfig();
        binId = config.getString("bin-id", "mc-world");
        archivePrefix = config.getString("archive-prefix", "world-");
        
        getCommand("download").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return SIZE_FORMAT.format(bytes / Math.pow(1024, exp)) + " " + pre + "B";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("download")) {
            return false;
        }

        sender.sendMessage("§aワールドの圧縮を開始します...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // タイムスタンプ付きのファイル名を生成
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
                String archiveName = archivePrefix + timestamp + ".tar.gz";
                File tempFile = File.createTempFile("worlds", ".tar.gz");
                
                // すべてのワールドをtar.gzに圧縮
                try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(
                        new GzipCompressorOutputStream(new FileOutputStream(tempFile)))) {
                    tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                    
                    for (World world : Bukkit.getWorlds()) {
                        File worldFolder = world.getWorldFolder();
                        sender.sendMessage("§a" + world.getName() + "を圧縮中...");
                        addToTar(tarOut, worldFolder, worldFolder.getName(), sender);
                    }
                }
                
                // filebin.netにアップロード
                String uploadUrl = "https://filebin.net/" + binId + "/" + archiveName;
                String fileSize = formatFileSize(tempFile.length());
                
                // アップロード開始ログ
                String startLog = String.format("[%s] %s %s (%s) のアップロードを開始しました",
                    sender.getName(), archiveName, fileSize, uploadUrl);
                getLogger().info(startLog);
                sender.sendMessage("§aアップロードを開始します...");
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofFile(tempFile.toPath()))
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 201) {
                    String downloadUrl = "https://filebin.net/" + binId + "/" + archiveName;
                    // アップロード成功ログ
                    String successLog = String.format("[%s] %s のアップロードが完了しました: %s",
                        sender.getName(), archiveName, downloadUrl);
                    getLogger().info(successLog);
                    Bukkit.getScheduler().runTask(this, () -> 
                        sender.sendMessage("§aダウンロードURL: " + downloadUrl));
                } else {
                    // アップロード失敗ログ
                    String failLog = String.format("[%s] %s のアップロードに失敗しました: %s",
                        sender.getName(), archiveName, response.body());
                    getLogger().warning(failLog);
                    Bukkit.getScheduler().runTask(this, () -> 
                        sender.sendMessage("§cアップロードに失敗しました: " + response.body()));
                }

                // 一時ファイルを削除
                tempFile.delete();
                
            } catch (Exception e) {
                // エラーログ
                String errorLog = String.format("[%s] ワールドの圧縮中にエラーが発生しました: %s",
                    sender.getName(), e.getMessage());
                getLogger().log(Level.SEVERE, errorLog, e);
                Bukkit.getScheduler().runTask(this, () -> 
                    sender.sendMessage("§cエラーが発生しました: " + e.getMessage()));
            }
        });

        return true;
    }

    private void addToTar(TarArchiveOutputStream tarOut, File file, String entryName, CommandSender sender) throws IOException {
        // session.lockファイルを除外
        if (file.getName().equals("session.lock")) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    try {
                        addToTar(tarOut, child, entryName + "/" + child.getName(), sender);
                    } catch (IOException e) {
                        // ファイルの読み取りに失敗した場合
                        Bukkit.getScheduler().runTask(this, () -> 
                            sender.sendMessage("§e警告: " + entryName + "/" + child.getName() + " の読み取りに失敗しました: " + e.getMessage()));
                    }
                }
            }
        } else {
            try {
                TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
                tarOut.putArchiveEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        tarOut.write(buffer, 0, len);
                    }
                }
                tarOut.closeArchiveEntry();
            } catch (IOException e) {
                // ファイルの読み取りに失敗した場合
                Bukkit.getScheduler().runTask(this, () -> 
                    sender.sendMessage("§e警告: " + entryName + " の読み取りに失敗しました: " + e.getMessage()));
                throw e; // エラーを上位に伝播させる
            }
        }
    }
}
