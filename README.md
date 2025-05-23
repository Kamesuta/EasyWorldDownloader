# EasyWorldDownloader

Minecraftサーバーのワールドを簡単にダウンロードできるプラグインです。

## 機能

- `/download`コマンドで現在のワールドをtar.gz形式で圧縮
- [filebin.net](https://filebin.net/)を使用してファイルをアップロード
- ログイン時にダウンロード方法をアナウンス
- 複数のワールドを1つのアーカイブにまとめて保存

## インストール方法

1. [Releases](https://github.com/Kamesuta/EasyWorldDownloader/releases)から最新のjarファイルをダウンロード
2. サーバーの`plugins`フォルダにjarファイルを配置
3. サーバーを再起動

## 設定

`plugins/EasyWorldDownloader/config.yml`を編集して設定を変更できます：

```yaml
# filebin.netのBinID
bin-id: "mc-world"

# アーカイブファイル名のプレフィックス
archive-prefix: "world-"

# ログイン時のアナウンスメッセージ
login-message: |-
  ----------------------------------------------
  
    ようこそ！バニラサーバーへ！
  
    遊んだあとは、§e/download§r と打つことで
    ワールドをダウンロードできますよ！
  
  ----------------------------------------------
```

## コマンド

- `/download` - 現在のワールドをダウンロード用に圧縮してアップロードします

## 権限

- `easyworlddownloader.download` - `/download`コマンドを実行する権限（デフォルト: true）

## 注意事項

- ワールドのサイズによっては圧縮に時間がかかる場合があります
- filebin.netの制限により、大きなファイルはアップロードできない可能性があります
- ロックされているファイル（例：`session.lock`）は自動的にスキップされます

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。詳細は[LICENSE](LICENSE)ファイルを参照してください。 