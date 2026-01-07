# RTP-nao

![Version](https://img.shields.io/badge/version-1.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Paper-green.svg)
![Java](https://img.shields.io/badge/java-21+-orange.svg)

Paper向けに最適化された、シンプルで高性能なランダムテレポートプラグインです。

A simple and optimized random teleport plugin for Paper.

---

## 📖 機能 / Features

- **🚀 ランダムテレポート / Random Teleport**
  - 安全な場所へ瞬時にテレポート。
  - Teleport to a random safe location.
- **🛡️ 安全チェック / Safety Check**
  - 水や溶岩などの危険な場所を自動的に回避。
  - Automatically avoids dangerous blocks like water and lava.
- **⏳ 待機時間 / Warmup delay**
  - テレポート前の移動制限。
  - Requires players to stay still before teleporting.
- **💤 クールタイム / Cooldown**
  - 連続使用の制限。
  - Prevents frequent use of the command.
- **🌍 マルチワールド対応 / Multi-world support**
  - 許可するワールドを自由に変更可能。
  - Specify allowed worlds in the config.

## 💻 コマンド / Commands

| コマンド / Command | 説明 / Description | 権限 / Permission |
|:---|:---|:---|
| `/rtp` | ランダムテレポートを実行<br>Execute RTP | `rtpnao.use` |
| `/rtp reload` | 設定を再読み込み<br>Reload config | `rtpnao.reload` |

## 🔑 権限 / Permissions

| 権限 / Permission | 説明 / Description | デフォルト / Default |
|:---|:---|:---|
| `rtpnao.use` | `/rtp` の使用を許可<br>Allows usage of RTP command | 全員 / Everyone |
| `rtpnao.reload` | 設定のリロードを許可<br>Allows reloading the config | OP |
| `rtpnao.bypass.cooldown` | クールタイムを無視<br>Bypass cooldown | OP |
| `rtpnao.bypass.warmup` | 待機時間を無視<br>Bypass warmup delay | OP |

## ⚙️ 設定 / Configuration (config.yml)

```yaml
# テレポートの範囲設定 (radius)
# 待機時間 (warmup)
# クールタイム (cooldown)
# 許可ワールド (allowed-worlds)
# すべてのメッセージ (messages)
```

メッセージも含め、すべての項目を `config.yml` から自由にカスタマイズ可能です。

All settings and messages can be customized via `config.yml`.
