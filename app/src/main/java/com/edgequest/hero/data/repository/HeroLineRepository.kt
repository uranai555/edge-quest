package com.edgequest.hero.data.repository

import com.edgequest.hero.data.model.HeroLine
import com.edgequest.hero.data.model.LineCategory

/**
 * 勇者ユウの全台詞を保持するリポジトリ。
 * 50個の台詞をハードコードし、カテゴリ・進化段階でフィルタリングする。
 */
object HeroLineRepository {

    private val allLines: List<HeroLine> = listOf(
        // === カテゴリ1：タップ（10個）cooldown=5s ===
        HeroLine(1, "何だ。我を呼んだか？", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(2, "この端っこ、意外と眺めがいいぞ", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(3, "汝も暇人だな。よし、我も暇だ", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(4, "触るな！……まあ、別にいいが", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(5, "我は勇者だぞ。画面端の。", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(6, "何か用か？なかったらまた寝るぞ", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(7, "汝の指、暖かいな", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(8, "魔王討伐の前に、まずはこの通知を片付けろ", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(9, "こうして触られるのも、悪くない", LineCategory.TAP, cooldownSeconds = 5),
        HeroLine(10, "我を呼ぶとは、よほどの用事だな。……で、用事は？", LineCategory.TAP, cooldownSeconds = 5),

        // === カテゴリ2：朝（5個）cooldown=900s ===
        HeroLine(11, "朝だ。今日も一応、世界を救うか", LineCategory.MORNING, cooldownSeconds = 900),
        HeroLine(12, "おはよう。汝、寝癖がついているぞ", LineCategory.MORNING, cooldownSeconds = 900),
        HeroLine(13, "朝日が眩しい……我、画面端に引っ込もう", LineCategory.MORNING, cooldownSeconds = 900),
        HeroLine(14, "早起きだと経験値が入る……はずなんだがな", LineCategory.MORNING, cooldownSeconds = 900),
        HeroLine(15, "今日はどんな一日になるやら。我はここで見守るだけだが", LineCategory.MORNING, cooldownSeconds = 900),

        // === カテゴリ3：昼（4個）cooldown=1800s ===
        HeroLine(16, "昼か。魔王も昼食を取っているはずだ", LineCategory.AFTERNOON, cooldownSeconds = 1800),
        HeroLine(17, "汝、ちゃんと飯を食っているか？我は食わんが", LineCategory.AFTERNOON, cooldownSeconds = 1800),
        HeroLine(18, "日が高いな。画面の輝度も高いな。まあいい", LineCategory.AFTERNOON, cooldownSeconds = 1800),
        HeroLine(19, "昼寝の時間だ。汝も少し休め。……我はいつも休んでいるがな", LineCategory.AFTERNOON, cooldownSeconds = 1800),

        // === カテゴリ4：夜（4個）cooldown=1800s ===
        HeroLine(20, "夜だ。魔王より晩御飯の方が重要らしい", LineCategory.EVENING, cooldownSeconds = 1800),
        HeroLine(21, "一日お疲れ。我は何もしていないがな", LineCategory.EVENING, cooldownSeconds = 1800),
        HeroLine(22, "暗くなってきたな。画面の明るさで我の顔が青白いぞ", LineCategory.EVENING, cooldownSeconds = 1800),
        HeroLine(23, "夜の端っこは落ち着くな。……まあ昼も落ち着いているが", LineCategory.EVENING, cooldownSeconds = 1800),

        // === カテゴリ5：深夜（5個）cooldown=900s ===
        HeroLine(24, "深夜だぞ。魔王ももう寝ている", LineCategory.NIGHT, cooldownSeconds = 900),
        HeroLine(25, "汝、明日というものがあることを忘れていないか？", LineCategory.NIGHT, cooldownSeconds = 900),
        HeroLine(26, "こんな時間まで起きているのは、人間か勇者だけだ", LineCategory.NIGHT, cooldownSeconds = 900),
        HeroLine(27, "画面の光が目に染みる……我は画面の中にいるのに", LineCategory.NIGHT, cooldownSeconds = 900),
        HeroLine(28, "夜更かしは肌にもレベルにも悪いぞ。……まあ、我は関係ないが", LineCategory.NIGHT, cooldownSeconds = 900),

        // === カテゴリ6：低バッテリー（5個）cooldown=3600s ===
        HeroLine(29, "電池が少ないぞ。我の命もあとわずかだ", LineCategory.LOW_BATTERY, cooldownSeconds = 3600),
        HeroLine(30, "充電しろ。我は画面が消えると暗闇に一人ぼっちになるんだ", LineCategory.LOW_BATTERY, cooldownSeconds = 3600),
        HeroLine(31, "残り20%……汝、充電器はどこだ？", LineCategory.LOW_BATTERY, cooldownSeconds = 3600),
        HeroLine(32, "消える前に一つ言っておく。……また明日会おう", LineCategory.LOW_BATTERY, cooldownSeconds = 3600),
        HeroLine(33, "我はバッテリーで動いているわけではない。……多分な", LineCategory.LOW_BATTERY, cooldownSeconds = 3600),

        // === カテゴリ7：放置復帰（5個）cooldown=3600s ===
        HeroLine(34, "留守中に小石を拾ってきたぞ。……役に立つかは知らん", LineCategory.IDLE_RETURN, cooldownSeconds = 3600),
        HeroLine(35, "おお、帰ってきたな。待ちくたびれたぞ", LineCategory.IDLE_RETURN, cooldownSeconds = 3600),
        HeroLine(36, "汝がおらん間、画面端で一人修行していた。……寝ていただけだが", LineCategory.IDLE_RETURN, cooldownSeconds = 3600),
        HeroLine(37, "戻ってきたか。暇だったぞ。画面が暗いと本当に何もできない", LineCategory.IDLE_RETURN, cooldownSeconds = 3600),
        HeroLine(38, "汝がおらん間に、魔王の気配を感じた。……たぶん充電のランプだった", LineCategory.IDLE_RETURN, cooldownSeconds = 3600),

        // === カテゴリ8：長時間使用（4個）cooldown=3600s ===
        HeroLine(39, "そろそろ休憩しないか。我も足がしびれてきた", LineCategory.LONG_USAGE, cooldownSeconds = 3600),
        HeroLine(40, "汝、何時間ここにいるつもりだ。我は画面端に縛り付けられているんだぞ", LineCategory.LONG_USAGE, cooldownSeconds = 3600),
        HeroLine(41, "休憩も立派な作戦だ。……さっき本で読んだ", LineCategory.LONG_USAGE, cooldownSeconds = 3600),
        HeroLine(42, "真剣な顔でスクロールする汝を見ていると、勇者より魔王の方が勝ちそうだと思う", LineCategory.LONG_USAGE, cooldownSeconds = 3600),

        // === カテゴリ9：レベルアップ（4個）isOneShot ===
        HeroLine(43, "レベルが上がった！これでまた一歩勇者に近づいた。…………近づいているのか？", LineCategory.LEVEL_UP, cooldownSeconds = 0, isOneShot = true),
        HeroLine(44, "成長を感じる。我の画面端生活にも、少しずつ意味が生まれている気がする", LineCategory.LEVEL_UP, cooldownSeconds = 0, isOneShot = true),
        HeroLine(45, "レベルアップだ！これで魔王にも一太刀浴びせられる……気がする。気がするだけだが", LineCategory.LEVEL_UP, cooldownSeconds = 0, isOneShot = true),
        HeroLine(46, "汝のおかげで強くなった。……とはいえ画面端からは出られないがな", LineCategory.LEVEL_UP, cooldownSeconds = 0, isOneShot = true),

        // === カテゴリ10：ランダム小ネタ（4個）cooldown=7200s ===
        HeroLine(47, "今日、画面をスクロールしていたら魔王のSNSを見つけた。フォロワー多いなあいつ", LineCategory.RANDOM, minEvolutionStage = 2, cooldownSeconds = 7200),
        HeroLine(48, "勇者にも休暇が必要だ。今、この端っこでバカンス中だ", LineCategory.RANDOM, minEvolutionStage = 1, cooldownSeconds = 7200),
        HeroLine(49, "汝のスマホ、ちょっと熱いぞ。我はサウナ状態だ", LineCategory.RANDOM, minEvolutionStage = 1, cooldownSeconds = 7200),
        HeroLine(50, "画面端の住人としてのライフハックを教えよう。……特にない", LineCategory.RANDOM, minEvolutionStage = 2, cooldownSeconds = 7200)
    )

    /**
     * 指定されたカテゴリと進化段階に合う台詞をランダムに返す。
     */
    fun getRandomLine(category: LineCategory, evolutionStage: Int): HeroLine? {
        val candidates = allLines.filter {
            it.category == category && evolutionStage >= it.minEvolutionStage
        }
        return candidates.randomOrNull()
    }

    /**
     * 指定されたIDの台詞を返す。
     */
    fun getLineById(id: Int): HeroLine? = allLines.find { it.id == id }

    /**
     * 特定のカテゴリに属する全台詞を進化段階フィルタ込みで返す。
     */
    fun getLinesByCategory(category: LineCategory, evolutionStage: Int): List<HeroLine> =
        allLines.filter { it.category == category && evolutionStage >= it.minEvolutionStage }
}
