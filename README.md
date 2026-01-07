# RTP-nao

Paper向けに最適化されたシンプルなランダムテレポートプラグインです。 / A simple and optimized random teleport plugin for Paper.

## 機能 / Features
- **ランダムテレポート / Random Teleport**: `/rtp` で安全な場所へランダムにテレポートします。 / Teleport to a random safe location.
- **安全チェック / Safety Check**: 水や溶岩などの危険な場所を自動的に避けます。 / Automatically avoids dangerous blocks like water and lava.
- **待機時間 / Warmup delay**: テレポート前に一定時間動かずに待機する必要があります。 / Requires players to stay still before teleporting.
- **クールタイム / Cooldown**: 連続使用を制限します。 / Prevents frequent use of the command.
- **マルチワールド対応 / Multi-world support**: 設定ファイルで許可するワールドを指定できます。 / Specify allowed worlds in the config.
- **権限管理 / Permissions**: 詳細な権限設定が可能です。 / Detailed permission nodes.

## コマンド / Commands
- `/rtp`: ランダムテレポートを実行します。 / Execute random teleport.
- `/rtp reload`: 設定ファイルを再読み込みします。 / Reload the configuration (Requires `rtpnao.reload`).

## 権限 / Permissions
- `rtpnao.use`: `/rtp` の使用を許可します (デフォルト: 全員)。 / Allows usage of `/rtp` (Default: everyone).
- `rtpnao.bypass.cooldown`: クールタイムを無視します (デフォルト: OP)。 / Bypass cooldown (Default: OP).
- `rtpnao.bypass.warmup`: 待機時間を無視します (デフォルト: OP)。 / Bypass warmup delay (Default: OP).
- `rtpnao.reload`: 設定のリロードを許可します (デフォルト: OP)。 / Allows reloading the config (Default: OP).

## 設定ファイル / Configuration (config.yml)
テレポート範囲、待機時間、クールタイム、許可ワールド、およびすべてのメッセージをカスタマイズできます。 / You can customize the teleport radius, delay, cooldown, allowed worlds, and all messages.
