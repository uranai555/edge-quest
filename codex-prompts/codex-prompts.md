# Codex 実装プロンプト集 — 画面端クエスト（Edge Quest）MVP

> 全タスク共通前提:
> - パッケージ名 `com.edgequest.hero`
> - Kotlin + Jetpack Compose
> - minSdk 26, targetSdk 34, compileSdk 34
> - Gradle Kotlin DSL
> - Android Native（Unity/Rust/Flutter不使用）
> - インターネットパーミッション不要
> - アクセシビリティ権限不使用
> - 通知内容・画面内容・位置情報・カメラ・マイク・連絡先・SMS は一切扱わない

---

## Task 1: Android プロジェクト雛形作成

### 背景
スマホ画面端に小さい勇者キャラを常駐表示するAndroidアプリ「画面端クエスト（Edge Quest）」のMVPをゼロから構築する。

### 目的
ビルド可能なAndroidプロジェクトを作成し、MainActivity・設定画面・権限説明画面を実装する。

### 実装対象

1. **Gradle プロジェクト設定**
   - Kotlin 1.9+
   - Jetpack Compose BOM 2024.01.00
   - minSdk 26, targetSdk 34, compileSdk 34
   - Material3
   - DataStore Preferences
   - Foreground Service用権限（POST_NOTIFICATIONS, FOREGROUND_SERVICE_SPECIAL_USE）
   - SYSTEM_ALERT_WINDOW権限宣言

2. **AndroidManifest.xml**
   - SYSTEM_ALERT_WINDOW権限
   - POST_NOTIFICATIONS（Android 13+）
   - FOREGROUND_SERVICE + foregroundServiceType="specialUse"
   - OverlayService宣言
   - MainActivity（exported=true, ランチャー起動）

3. **MainActivity（Jetpack Compose）**
   - 起動時: SYSTEM_ALERT_WINDOW権限チェック
   - 未許可: 権限説明画面を表示 → 許可設定画面へ誘導
   - 許可済み: OverlayServiceを起動 → 設定画面を表示
   - バックボタンは画面を閉じる（サービスは生きたまま）

4. **権限説明画面**
   - このアプリが何をするかの説明:
     - 「スマホ画面端に小さい勇者キャラを表示します」
     - 「通知の中身や画面の内容は読み取りません」
     - 「いつでも表示をOFFにできます」
   - 「他のアプリの上に表示」権限の許可ボタン → Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
   - 権限付与後、自動でOverlayServiceを起動

5. **設定画面**
   - 表示ON/OFF トグル
   - キャラサイズ選択（36dp / 48dp / 64dp）
   - 台詞頻度選択（少ない / 普通 / 多い）
   - バッテリーリアクション ON/OFF
   - 時間帯リアクション ON/OFF
   - 放置復帰リアクション ON/OFF
   - 深夜リアクション ON/OFF
   - 位置リセットボタン
   - データ全消去ボタン（確認ダイアログ付き）
   - 設定はDataStore Preferencesに即時保存

### 受け入れ条件
- `./gradlew assembleDebug` が成功する
- 実機インストール後、権限説明画面が表示される
- 権限許可後、設定画面が表示される
- 設定を変更するとDataStoreに保存される
- アプリ再起動後、設定が保持されている

### 禁止事項
- retrofit/okhttp/ktor/volleyなどのネットワークライブラリ
- Hilt/Dagger/Koin（v0.1では依存性注入なし）
- Firebase / Crashlytics / Analytics
- サードパーティ広告SDK
- アクセシビリティ関連のコード

### 出力してほしいファイル
```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/edgequest/hero/
│   │   ├── MainActivity.kt
│   │   ├── ui/
│   │   │   ├── PermissionScreen.kt
│   │   │   ├── SettingsScreen.kt
│   │   │   └── theme/
│   │   │       ├── Theme.kt
│   │   │       ├── Color.kt
│   │   │       └── Type.kt
│   │   ├── service/
│   │   │   └── OverlayService.kt  (スタブ)
│   │   └── data/
│   │       ├── SettingsDataStore.kt
│   │       └── HeroStateDataStore.kt
│   └── res/
│       ├── values/
│       │   ├── strings.xml
│       │   └── themes.xml
│       ├── mipmap-hdpi/ (ic_launcher)
│       └── drawable/ (ic_notification)
├── build.gradle.kts (ルート)
├── settings.gradle.kts
├── gradle.properties
└── gradle/
    └── libs.versions.toml
```

---

## Task 2: オーバーレイ表示実装

### 背景
Task 1で作成したプロジェクトの上に、実際のオーバーレイ表示機能を実装する。

### 目的
SYSTEM_ALERT_WINDOW権限取得後、Foreground Serviceを起動し、WindowManagerを使って他アプリ上に勇者キャラViewを表示する。

### 実装対象

1. **OverlayService（Foreground Service）**
   - 常駐用通知チャンネルを作成（channel_id: "hero_overlay"）
   - 通知内容: 「画面端クエスト - 勇者が待機中」
   - 通知タップ→MainActivityへ遷移（PendingIntent）
   - onStartCommand: START_STICKY
   - onDestroy: WindowManagerからViewを削除

2. **HeroOverlayView（Composeではなく、WindowManager向けFrameLayout）**
   - 48dp × 48dp のView
   - 初期位置: 画面右下（LayoutParams.gravity = Gravity.TOP or Gravity.START + x/y）
   - 背景色指定（プレースホルダーとして**: 緑色の円形View**、後でキャラ画像と差し替え）
   - Viewにタップリスナー（後で吹き出し表示のトリガーになる）
   - Viewにタッチリスナー（ドラッグ移動用）

3. **WindowManager設定**
   - `TYPE_APPLICATION_OVERLAY` を使用
   - flags: FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN | FLAG_WATCH_OUTSIDE_TOUCH
   - LayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
   - LayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

4. **ドラッグ移動**
   - ACTION_DOWN: タッチ位置を記録
   - ACTION_MOVE: View位置をタッチ差分だけ移動
   - ACTION_UP: 画面端吸着処理を呼び出す

5. **画面端吸着（Edge Snap）**
   - 指を離した時、ViewのX座標が画面中央より左なら左端、右なら右端に吸着
   - 吸着時はスムーズに移動（ValueAnimator 200ms）
   - Y座標はそのまま保持（上端/下端に吸着しない）

6. **最小化**
   - Viewに最小化ボタンを追加（右上の小さな「−」ボタン）
   - 最小化時: 24dp × 24dp の小さな円形Viewに差し替え
   - 最小化状態でタップ → 48dpに戻る
   - 最小化状態でもドラッグ移動可能

7. **非表示**
   - Viewに閉じるボタンを追加（左上または右上の「✕」ボタン）
   - 非表示時: WindowManagerからViewをremoveView
   - 設定画面からOverlayServiceに再表示Intentを送って復帰

8. **設定連携**
   - DataStoreの設定値（サイズ、表示ON/OFF）をService起動時と設定変更時に反映
   - サイズ変更: ViewのLayoutParamsを更新

### 受け入れ条件
- 権限許可後、他アプリ上に緑色の円形Viewが表示される
- Viewをドラッグして移動できる
- 画面端で吸着する
- 最小化ボタンで小さくなる
- 閉じるボタンで完全に消える
- 設定画面から再表示できる
- Task 1とTask 2がマージされていれば、設定保存・復元も動作する

### 禁止事項
- ViewにEditText/入力フォームを設置しない
- FLAG_SECURE/FLAG_SECURE_WINDOWを設定しない
- Activityコンテキストを保持し続けない（リーク防止）

### 出力してほしいファイル（追加・変更）
```
app/src/main/java/com/edgequest/hero/
├── service/
│   └── OverlayService.kt           (Task 1 からの書き換え)
└── overlay/
    ├── HeroOverlayManager.kt        (WindowManager管理のラッパー)
    ├── HeroOverlayView.kt           (サブクラス化したFrameLayout)
    └── EdgeSnapHelper.kt            (画面端吸着ロジック)
```

---

## Task 3: 吹き出し・台詞システム

### 背景
Task 2で画面端に勇者Viewを表示できるようになった。次はタップ時に吹き出し台詞を表示する。

### 目的
勇者Viewをタップすると吹き出しが表示され、3.5秒後に自動消去される。台詞はカテゴリ別に管理され、ランダム選択される。

### 実装対象

1. **台詞データモデル**（`com.edgequest.hero.data.model`）
   ```kotlin
   data class HeroLine(
       val id: Int,
       val text: String,
       val category: LineCategory,
       val minEvolutionStage: Int = 1,
       val cooldownSeconds: Long,
       val isOneShot: Boolean = false
   )
   
   enum class LineCategory {
       TAP, MORNING, AFTERNOON, EVENING, NIGHT,
       LOW_BATTERY, IDLE_RETURN, LONG_USAGE,
       LEVEL_UP, RANDOM
   }
   ```

2. **台詞データソース**（`HeroLineRepository`）
   - 50個の台詞をKotlin object / companion objectでハードコード
   - `getRandomLine(category: LineCategory, stage: Int): HeroLine`
   - 進化段階によるフィルタリング（`minEvolutionStage`）
   - one-shot台詞は表示済みIDをDataStoreに保存しておき、二度目はスキップ

3. **SpeechBubbleView**
   - Jetpack Composeではなく、WindowManagerに追加可能なView
   - 吹き出しのUI: 白背景 + 角丸 + 下向き三角
   - フォント: Noto Sans JP or Roboto
   - 最大30文字（1行）
   - 表示時間: 3.5秒後フェードアウト（AlphaAnimation 300ms）
   - 吹き出しの位置: HeroOverlayViewの真上、画面端に応じて左右オフセット調整

4. **台詞管理システム**（`SpeechManager`）
   - タップ時に吹き出しを表示する
   - 各カテゴリのクールダウン管理（DataStoreに最終表示時刻を保存）
   - クールダウン中の場合は台詞を表示しない（無視するだけでエラーにしない）
   - 設定の台詞頻度（QUIET/NORMAL/TALKATIVE）を反映
   - 1時間あたりの最大表示回数を制限

5. **OverlayService連携**
   - OverlayServiceがSpeechManagerを保持
   - HeroOverlayViewのタップイベント → SpeechManager.onTap()
   - SpeechManager表示時 → WindowManagerにSpeechBubbleViewを追加
   - SpeechManager非表示時 → WindowManagerからSpeechBubbleViewを削除

### 受け入れ条件
- 勇者Viewをタップすると吹き出しが表示される
- 吹き出しは3.5秒後に自動で消える
- 連続タップでも5秒に1回しか台詞が出ない
- カテゴリ別のクールダウンが機能する
- 設定「少ない」では時間帯台詞のみ、「多い」ではランダム台詞も出る
- 吹き出しの位置が勇者の真上に表示される

### 禁止事項
- 台詞内にユーザー名など動的置換を含めない（v0.1では固定台詞のみ）
- 吹き出しにEditTextや入力を含めない
- 台詞表示のアニメーションを過剰にしない

### 出力してほしいファイル
```
app/src/main/java/com/edgequest/hero/
├── data/
│   └── model/
│       ├── HeroLine.kt
│       └── LineCategory.kt
├── data/
│   └── repository/
│       └── HeroLineRepository.kt
├── overlay/
│   ├── SpeechBubbleView.kt
│   └── SpeechManager.kt
└── service/
    └── OverlayService.kt            (更新)
```

---

## Task 4: リアクションエンジン

### 背景
Task 3でタップ台詞が出るようになった。次は時間帯・バッテリー残量・放置復帰などの条件で自動的に台詞を表示する。

### 目的
時間帯・バッテリー・放置時間などの状態を監視し、条件に合致したら適切なカテゴリの台詞を自動表示する。

### 実装対象

1. **ReactionEngine**（`com.edgequest.hero.reaction`）
   - 各トリガーの状態を監視し、条件成立時にSpeechManager経由で台詞表示
   - 優先度順の判定（LEVEL_UP > TAP > LOW_BATTERY > TIME > IDLE > RANDOM）
   - 同時発火時は高優先度のみ表示
   - 各トリガーに独立したクールダウン

2. **TimeReaction**
   - 現在時刻を5分ごとにチェック
   - 朝（5:00〜10:59）、昼（11:00〜16:59）、夜（17:00〜20:59）、深夜（21:00〜4:59）
   - 深夜は設定OFFの場合はスキップ
   - 各カテゴリのクールダウンを尊重

3. **BatteryReaction**
   - BroadcastReceiver: `Intent.ACTION_BATTERY_CHANGED` を登録
   - バッテリー残量≤20%で発火
   - 1時間に1回まで（クールダウン管理）
   - 設定OFFの場合はスキップ

4. **IdleReturnReaction**
   - OverlayServiceの表示状態を監視（表示→非表示→表示の遷移を検出）
   - 非表示時間≥30分の場合、再表示時に発火
   - 1時間に1回まで（クールダウン管理）
   - 設定OFFの場合はスキップ

5. **LongUsageReaction**
   - 連続使用時間をトラッキング
   - OverlayService起動からの経過時間を監視（5分ごと）
   - ≥60分で発火
   - 1時間に1回まで

6. **RandomReaction**
   - 設定がNORMALまたはTALKATIVEの場合のみアクティブ
   - 2時間に1回、ランダムタイミングで発火
   - 他のトリガーと重ならないタイミングを選ぶ（優先度最低）

7. **設定連携**
   - 各リアクションのON/OFFと頻度設定をDataStoreから読み取り
   - 設定変更時に即座に反映

### 受け入れ条件
- 朝5:00〜10:59にアプリを開くと朝の台詞が出る
- バッテリー20%以下で低バッテリー台詞が出る
- 30分以上放置後に復帰すると放置復帰台詞が出る
- 各トリガーのクールダウンが正しく機能する
- 設定で各リアクションを個別にOFFにできる
- 同時に複数トリガーが発火しても1つだけ表示される

### 禁止事項
- 位置情報を使ったトリガー
- 通知内容を読むトリガー
- 画面内容を解析するトリガー
- ユーザーの操作ログを外部保存する処理

### 出力してほしいファイル
```
app/src/main/java/com/edgequest/hero/
├── reaction/
│   ├── ReactionEngine.kt
│   ├── TimeReaction.kt
│   ├── BatteryReaction.kt
│   ├── IdleReturnReaction.kt
│   ├── LongUsageReaction.kt
│   └── RandomReaction.kt
├── overlay/
│   └── SpeechManager.kt            (更新 - 外部トリガー用インターフェース追加)
└── service/
    └── OverlayService.kt            (更新 - ReactionEngine起動)
```

---

## Task 5: 育成データ永続化

### 背景
リアクションエンジンまで実装できた。次は勇者の育成データ（レベル・経験値・親密度・進化段階）と設定データを永続化する。

### 目的
DataStore Preferencesを使用して勇者の育成データと各種設定値を保存・読み取りする。アプリ再起動後もデータが保持される。

### 実装対象

1. **HeroStateDataStore**
   - DataStore Preferencesで以下を保存
     ```kotlin
     data class HeroState(
         val level: Int = 1,            // 1-10
         val exp: Int = 0,              // 累積経験値
         val intimacy: Int = 0,         // 親密度 0-100
         val evolutionStage: Int = 1,   // 1-3
         val lastInteractionAt: Long = 0L,  // 最終操作時刻 (epoch millis)
         val lastPositionX: Int = 0,    // 表示位置X
         val lastPositionY: Int = 0,    // 表示位置Y
         val isMinimized: Boolean = false,
         val createdTimestamp: Long = System.currentTimeMillis()
     )
     ```
   - `saveHeroState(HeroState)` / `heroState: Flow<HeroState>` を提供
   - 初回起動時はデフォルト値を返す

2. **SettingsDataStore**
   - 以下の設定値を保存
     ```kotlin
     data class UserSettings(
         val overlayEnabled: Boolean = true,
         val speechFrequency: SpeechFrequency = SpeechFrequency.QUIET,
         val heroSizeDp: Int = 48,
         val batteryReactionEnabled: Boolean = true,
         val timeReactionEnabled: Boolean = true,
         val idleReactionEnabled: Boolean = true,
         val nightReactionEnabled: Boolean = false
     )
     enum class SpeechFrequency { QUIET, NORMAL, TALKATIVE }
     ```
   - `saveSettings(UserSettings)` / `userSettings: Flow<UserSettings>`

3. **GrowthManager**
   - 経験値の獲得処理
     - タップ: +5 exp
     - イベント（時間帯/バッテリーなど）: +10 exp
     - 放置復帰: +15 exp
     - レベルアップ条件: 次のレベルに必要なexp = `currentLevel * 50`
   - レベルアップ時の処理
     - HeroState.level += 1
     - 経験値の余剰は繰り越し
     - レベル3で進化段階2、レベル7で進化段階3
     - レベルアップイベントをReactionEngineに通知
   - 親密度の更新
     - タップ: +1（1日最大3回まで）
     - イベント: +2（1日最大5回まで）
     - 最大100

4. **LevelUpNotification**
   - レベルアップ時: 特別な吹き出し/エフェクト（Viewの拡大など簡易演出）
   - LEVEL_UPカテゴリの台詞を1回だけ表示（isOneShot = true）

5. **設定リセット**
   - 設定画面の「データ全消去」で全てのDataStoreデータを削除
   - アプリを初期状態に戻す（確認ダイアログ表示）

### 受け入れ条件
- 勇者のレベルがタップやイベントで上昇する
- レベル3で進化段階2、レベル7で進化段階3に変化する
- アプリを終了→再起動してもレベル・経験値が保持される
- 設定値を変更→再起動しても設定値が保持される
- 「データ全消去」で全てのデータがリセットされる
- レベルアップ時に台詞が1回だけ表示される

### 禁止事項
- Room/Databaseの導入（v0.1ではDataStoreで十分）
- クラウド同期
- 外部ストレージへの書き出し
- SharedPreferences（DataStoreに統一）

### 出力してほしいファイル
```
app/src/main/java/com/edgequest/hero/
├── data/
│   ├── HeroStateDataStore.kt
│   ├── SettingsDataStore.kt
│   └── CooldownDataStore.kt        (各リアクションの最終表示時刻管理)
└── growth/
    ├── GrowthManager.kt
    └── LevelUpHandler.kt
```

---

## Task 6: 簡易アニメーション＋テストAPK

### 背景
MVPの最終仕上げ。勇者キャラの見た目をプレースホルダーから簡易アニメーションに差し替え、Debug APKをビルドする。

### 目的

#### A. 簡易アニメーション
- プレースホルダーの緑丸を、Spriteフレーム切り替えまたはLottieアニメーションに置き換える
- 以下のモーションを実装:
  - **待機**: 微かに上下に揺れる（呼吸のようなアニメーション）
  - **タップ反応**: 一瞬跳ねる→通常に戻る
  - **喜ぶ**: 上下に小さくジャンプ
  - **焦る**: 左右に震える
  - **寝る（深夜アイドル時）**: Zzzマークが浮かぶ、ゆっくり上下

#### B. Sprite実装（推奨）
- リソース制約を考慮し、最初はコード上のCanvas描画で簡易表示する
- 以下の要素をCanvasに描画:
  - 緑色の楕円/丸（勇者のボディ）
  - 小さな点（目）
  - 状態に応じて口の形を変える（笑顔・驚き・焦りなど）
- Canvas描画をViewのonDrawで実装し、フレームごとに描画変更
- 進化段階に応じて色やサイズを変化:
  - 段階1: 薄緑、小さい
  - 段階2: 緑、少し大きい（48dp基準）
  - 段階3: 金緑、マントの追加（Canvasに三角形を追加）

#### C. 状態遷移アニメーション
- FrameAnimation または ValueAnimatorで実装
- 各モーションのduration:
  - 待機: ループ 2秒間隔
  - タップ反応: 300ms
  - 喜ぶ: 600ms
  - 焦る: 800ms
  - 寝る: ループ 3秒間隔（Zzz含む）

#### D. Debug APK作成
- `./gradlew assembleDebug` でAPK生成
- 以下を含むREADMEをプロジェクトルートに作成:
  - ビルド手順
  - 実機インストール手順（adb install -t）
  - 必要な権限
  - 対応Androidバージョン（8.0〜15）
  - 既知の制約事項
  - Androidバージョン別の注意点
- APK配置場所: `app/build/outputs/apk/debug/app-debug.apk`

### 受け入れ条件
- 勇者が待機中に微かに上下に動く
- タップ時に跳ねるアニメーションが再生される
- 進化段階によってキャラの見た目が変わる
- Debug APKが正常にビルドされる
- Android 8.0〜15の実機でインストール・動作する

### 禁止事項
- OpenGL / Vulkan / 3Dレンダリング
- 外部アニメーションファイル（.gif/.webp）の埋め込み
- Lottieライブラリの導入（v0.1ではCanvas描画のみ）

### 出力してほしいファイル
```
app/src/main/java/com/edgequest/hero/
└── overlay/
    ├── HeroRenderer.kt              (Canvas描画の勇者キャラ)
    ├── HeroAnimationState.kt        (アニメーション状態管理)
    └── HeroOverlayView.kt           (更新 - Canvas描画に差し替え)

README.md                           (プロジェクトルート - ビルド・インストール手順)
```
