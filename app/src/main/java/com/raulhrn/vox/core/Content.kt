package com.raulhrn.vox.core

/** "Ode à Alegria" (Beethoven), first phrase. */
private val ODE = intArrayOf(4, 4, 5, 7, 7, 5, 4, 2, 0, 0, 2, 4, 4, 2, 2)
private val ODE_BEATS = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.5f, 0.5f, 2f)

/** "Brilha, brilha estrelinha", first two phrases. */
private val BRILHA = intArrayOf(0, 0, 7, 7, 9, 9, 7, 5, 5, 4, 4, 2, 2, 0)
private val BRILHA_BEATS = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f, 1f, 1f, 2f)

object Exercises {
    val all: List<Exercise> = listOf(
        Exercise(
            id = "respiracao",
            title = "Respiração 4-4-8",
            category = "Aquecimento",
            summary = "Controle do ar antes de cantar",
            kind = ExerciseKind.BREATH,
            minutes = 2,
            steps = listOf(
                "Fique em pé, ombros soltos, uma mão na barriga.",
                "Inspire pelo nariz em 4 tempos, expandindo barriga e costelas (os ombros não sobem).",
                "Segure o ar por 4 tempos, sem travar a garganta.",
                "Solte o ar em 8 tempos fazendo um \"sss\" contínuo e uniforme.",
                "A barra de volume mostra se o \"sss\" está constante.",
            ),
        ),
        Exercise(
            id = "labios",
            title = "Vibração de lábios",
            category = "Aquecimento",
            summary = "Escala de 5 notas em \"brrr\"",
            pattern = intArrayOf(0, 2, 4, 5, 7, 5, 4, 2, 0),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f),
            bpm = 120,
            minutes = 3,
            steps = listOf(
                "Feche os lábios soltos e sopre fazendo \"brrr\", como um motorzinho.",
                "Ouça a escala e repita mantendo a vibração sem parar.",
                "Se os lábios travarem, apoie as bochechas com os dedos.",
                "O exercício sobe meio tom a cada repetição e depois desce.",
            ),
        ),
        Exercise(
            id = "humming",
            title = "Humming",
            category = "Aquecimento",
            summary = "Boca fechada, som na máscara",
            pattern = intArrayOf(0, 4, 7, 4, 0),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 2f),
            bpm = 84,
            minutes = 3,
            steps = listOf(
                "Cante \"mmm\" com os lábios fechados e os dentes separados.",
                "Sinta vibrar no nariz e nos lábios, não na garganta.",
                "Mantenha a língua relaxada no fundo da boca.",
            ),
        ),
        Exercise(
            id = "sirene",
            title = "Sirene",
            category = "Aquecimento",
            summary = "Deslize do grave ao agudo",
            pattern = intArrayOf(0, 7, 12, 7, 0),
            beats = floatArrayOf(1f, 1f, 2f, 1f, 2f),
            bpm = 66,
            rootStep = 2,
            minutes = 2,
            steps = listOf(
                "Em \"u\" ou em vibração de lábios, deslize suavemente entre as notas, sem degraus.",
                "Passe pela mudança de registro sem empurrar: deixe a voz ficar mais leve no agudo.",
                "Volume confortável, nunca grite.",
            ),
        ),
        Exercise(
            id = "nota-longa",
            title = "Nota longa",
            category = "Afinação",
            summary = "Sustente 5 segundos afinado",
            kind = ExerciseKind.SUSTAIN,
            rootStep = 2,
            minutes = 3,
            steps = listOf(
                "Ouça a nota e cante em \"a\" ou \"u\" por 5 segundos.",
                "Mantenha a linha no centro do alvo: sem subir nem cair no fim.",
                "Respire antes de começar e solte o ar de forma uniforme.",
            ),
        ),
        Exercise(
            id = "escala-maior",
            title = "Escala maior",
            category = "Afinação",
            summary = "Dó-ré-mi completo, subindo e descendo",
            pattern = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12, 11, 9, 7, 5, 4, 2, 0),
            bpm = 108,
            minutes = 4,
            steps = listOf(
                "Cante cada nota com o nome (dó, ré, mi...) ou em \"lá\".",
                "Atenção aos meios tons: mi-fá e si-dó ficam bem próximos.",
                "Na descida, não deixe a afinação cair.",
            ),
        ),
        Exercise(
            id = "arpejo",
            title = "Arpejo maior",
            category = "Afinação",
            summary = "1-3-5-8: os saltos do acorde",
            pattern = intArrayOf(0, 4, 7, 12, 7, 4, 0),
            beats = floatArrayOf(1f, 1f, 1f, 2f, 1f, 1f, 2f),
            bpm = 92,
            minutes = 3,
            steps = listOf(
                "Cante em \"ma-me-mi-mo-mu\" ou só em \"a\".",
                "Pense na nota antes de cantá-la: o salto de oitava precisa ser preparado.",
                "Abra mais a boca no agudo, sem levantar o queixo.",
            ),
        ),
        Exercise(
            id = "escala-menor",
            title = "Escala menor",
            category = "Afinação",
            summary = "O som mais melancólico",
            pattern = intArrayOf(0, 2, 3, 5, 7, 8, 10, 12, 10, 8, 7, 5, 3, 2, 0),
            bpm = 100,
            minutes = 4,
            steps = listOf(
                "Compare com a escala maior: a 3ª, a 6ª e a 7ª notas ficam meio tom abaixo.",
                "Cante devagar e escute bem antes de responder.",
            ),
        ),
        Exercise(
            id = "staccato",
            title = "Staccato \"ha\"",
            category = "Técnica",
            summary = "Notas curtas com apoio do abdômen",
            pattern = intArrayOf(0, 4, 7, 4, 0),
            bpm = 100,
            legato = 0.45f,
            minutes = 2,
            steps = listOf(
                "Cante \"ha\" curto em cada nota, como uma risada leve.",
                "Cada nota nasce de um pequeno impulso da barriga, não da garganta.",
                "Entre as notas, o ar entra sozinho.",
            ),
        ),
        Exercise(
            id = "oitavas",
            title = "Saltos de oitava",
            category = "Técnica",
            summary = "Une grave e agudo sem quebrar",
            pattern = intArrayOf(0, 12, 0),
            beats = floatArrayOf(1f, 2f, 2f),
            bpm = 76,
            minutes = 3,
            steps = listOf(
                "Cante \"u\" na nota grave e salte para a oitava.",
                "Mantenha o mesmo espaço na boca nas duas notas.",
                "Se a voz quebrar, faça primeiro em vibração de lábios.",
            ),
        ),
        Exercise(
            id = "agilidade",
            title = "Agilidade",
            category = "Técnica",
            summary = "Arpejo rápido, voz leve",
            pattern = intArrayOf(0, 4, 7, 12, 7, 4, 0, 4, 7, 4, 0),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f),
            bpm = 150,
            minutes = 3,
            steps = listOf(
                "Use \"i\" ou \"a\", leve e sem peso.",
                "Pense nas notas como bolinhas pulando, não como degraus pesados.",
                "Se ficar difícil, diminua o volume em vez de forçar.",
            ),
        ),
        Exercise(
            id = "repita",
            title = "Repita a nota",
            category = "Ouvido",
            summary = "Ouça uma nota e cante igual",
            kind = ExerciseKind.ECHO,
            minutes = 3,
            steps = listOf(
                "O app toca uma nota sorteada dentro da sua extensão.",
                "Cante a mesma nota. Se a linha cair abaixo do alvo, suba devagar até encaixar.",
                "Notas uma oitava acima ou abaixo também valem.",
            ),
        ),
        Exercise(
            id = "tercas",
            title = "Escala em terças",
            category = "Ouvido",
            summary = "O intervalo da segunda voz",
            pattern = intArrayOf(0, 4, 2, 5, 4, 7, 5, 9, 7, 11, 12),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f),
            bpm = 96,
            minutes = 3,
            steps = listOf(
                "Dó-mi, ré-fá, mi-sol... cada par é uma terça.",
                "Memorize o som da terça: é a distância mais usada em segundas vozes.",
            ),
        ),
        Exercise(
            id = "segunda-acima",
            title = "3ª acima",
            category = "Segunda voz",
            summary = "A segunda voz mais comum, por cima da melodia",
            pattern = intArrayOf(0, 2, 4, 5, 4, 2, 0),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 2f),
            harmony = HarmonyMode.THIRD_ABOVE,
            bpm = 84,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Primeiro o app toca melodia e segunda voz juntas: escute a terça.",
                "Depois ele toca só a melodia e você canta a segunda voz (a barra colorida).",
                "Use fones de ouvido para o microfone ouvir só você.",
                "Se for puxado para a melodia, cante mais baixo e pense na sua nota antes.",
            ),
        ),
        Exercise(
            id = "segunda-abaixo",
            title = "3ª abaixo",
            category = "Segunda voz",
            summary = "A harmonia por baixo da melodia",
            pattern = intArrayOf(4, 5, 7, 5, 4, 2, 4),
            beats = floatArrayOf(1f, 1f, 2f, 1f, 1f, 1f, 2f),
            harmony = HarmonyMode.THIRD_BELOW,
            bpm = 84,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Escute a melodia com a voz de baixo, depois cante só a de baixo.",
                "A terça abaixo é comum quando a melodia já está aguda.",
                "Use fones de ouvido.",
            ),
        ),
        Exercise(
            id = "segunda-sexta",
            title = "6ª abaixo",
            category = "Segunda voz",
            summary = "A terça acima, uma oitava mais grave",
            pattern = ODE,
            beats = ODE_BEATS,
            harmony = HarmonyMode.SIXTH_BELOW,
            bpm = 96,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Melodia: \"Ode à Alegria\". Você canta as mesmas notas da terça acima, só que uma oitava abaixo.",
                "A sexta soa mais aberta que a terça e é ótima para vozes graves sob uma melodia aguda.",
                "Use fones de ouvido.",
            ),
        ),
        Exercise(
            id = "segunda-sexta-acima",
            title = "6ª acima",
            category = "Segunda voz",
            summary = "A terça abaixo, uma oitava mais aguda",
            pattern = BRILHA,
            beats = BRILHA_BEATS,
            harmony = HarmonyMode.SIXTH_ABOVE,
            bpm = 96,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Melodia: \"Brilha, brilha estrelinha\". A sua linha fica bem acima da melodia.",
                "Use voz leve: notas agudas aparecem mais que a melodia se você empurrar.",
                "Use fones de ouvido.",
            ),
        ),
        Exercise(
            id = "segunda-conduzida",
            title = "Conduzida (3ª e 6ª)",
            category = "Segunda voz",
            summary = "Troca de terça para sexta para andar pouco",
            pattern = ODE,
            beats = ODE_BEATS,
            harmony = HarmonyMode.GUIDED,
            bpm = 92,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Em vez de copiar cada salto da melodia, a segunda voz escolhe entre terça e sexta para se mover o mínimo.",
                "Repare como a sua linha fica mais \"parada\" que a melodia: é isso que deixa o arranjo suave.",
                "O app escreve a linha dentro da sua extensão vocal.",
            ),
        ),
        Exercise(
            id = "segunda-contraria",
            title = "Movimento contrário",
            category = "Segunda voz",
            summary = "A melodia sobe, você desce",
            pattern = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f),
            harmony = HarmonyMode.CONTRARY,
            bpm = 84,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Enquanto a melodia sobe a escala, a segunda voz desce, sempre em intervalos consonantes.",
                "É o tipo mais independente: treine devagar e ouça bem o modelo antes.",
                "Use fones de ouvido.",
            ),
        ),
        Exercise(
            id = "segunda-pedal",
            title = "Nota pedal",
            category = "Segunda voz",
            summary = "Segure uma nota enquanto a melodia anda",
            pattern = BRILHA,
            beats = BRILHA_BEATS,
            harmony = HarmonyMode.PEDAL,
            bpm = 96,
            minutes = 3,
            needsHeadphones = true,
            steps = listOf(
                "A sua voz fica parada numa nota (geralmente a tônica ou a quinta) e só sai dela quando choca com a melodia.",
                "Mantenha a nota longa bem afinada e sem tremer: ela é o chão da harmonia.",
                "Use fones de ouvido.",
            ),
        ),
        Exercise(
            id = "backing-equilibrio",
            title = "Equilíbrio de volume",
            category = "Backing vocal",
            summary = "Cante mais suave que o regente, na medida",
            kind = ExerciseKind.BALANCE,
            minutes = 3,
            steps = listOf(
                "Primeiro cante \"aah\" no volume de voz principal: essa é a referência.",
                "Depois cante a mesma nota no volume de backing, de pad e de resposta.",
                "O medidor mostra quantos dB você está abaixo da referência. Fique dentro da faixa.",
                "Mantenha a mesma distância do celular o tempo todo.",
            ),
        ),
        Exercise(
            id = "backing-harmonia",
            title = "Harmonia com o regente",
            category = "Backing vocal",
            summary = "Cante a letra em terças, abaixo do volume dele",
            pattern = ODE,
            beats = ODE_BEATS,
            backing = BackingStyle.HARMONY,
            bpm = 92,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "O app toca o regente e o backing; depois você canta o backing com o regente.",
                "Fique abaixo do volume dele: a melodia sempre na frente.",
                "Mesmas vogais e mesmos cortes de frase.",
            ),
        ),
        Exercise(
            id = "backing-pad",
            title = "Pad em \"Uh\"",
            category = "Backing vocal",
            summary = "Notas longas do acorde por baixo da melodia",
            pattern = ODE,
            beats = ODE_BEATS,
            backing = BackingStyle.PAD,
            bpm = 96,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "O app toca a melodia e o pad; depois você canta o pad em \"Uh\" sob a melodia.",
                "Entre e saia suave, sem ataque, e segure a nota inteira com o mesmo volume.",
                "O pad deve ficar bem mais baixo que a melodia (cerca de 10 dB).",
            ),
        ),
        Exercise(
            id = "backing-resposta",
            title = "Resposta (eco)",
            category = "Backing vocal",
            summary = "Repita o fim das frases do regente",
            pattern = intArrayOf(0, 2, 4, 0, Exercise.REST, 4, 5, 7, Exercise.REST, 7, 9, 7, 5, 4, 0, Exercise.REST),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 2f, 1f, 1f, 2f, 2f, 0.5f, 0.5f, 0.5f, 0.5f, 1f, 1f, 2f),
            backing = BackingStyle.ECHO,
            bpm = 100,
            minutes = 3,
            needsHeadphones = true,
            steps = listOf(
                "O regente canta uma frase e o backing responde nas pausas, como um eco.",
                "Entre logo depois que ele termina, nunca por cima.",
                "A resposta pode ser quase tão forte quanto a frase original.",
            ),
        ),
        Exercise(
            id = "backing-dobra",
            title = "Dobra em uníssono",
            category = "Backing vocal",
            summary = "Duas vozes que soam como uma",
            pattern = BRILHA,
            beats = BRILHA_BEATS,
            backing = BackingStyle.DOUBLE,
            bpm = 96,
            minutes = 3,
            needsHeadphones = true,
            steps = listOf(
                "Cante exatamente a mesma melodia, na mesma oitava ou uma oitava acima/abaixo.",
                "Copie as vogais, as respirações e o tempo de cada nota. Tire o vibrato.",
                "Na dobra, quanto menos a sua voz aparecer, melhor.",
            ),
        ),
        Exercise(
            id = "backing-trio",
            title = "Coro a 3 vozes",
            category = "Backing vocal",
            summary = "Você na voz grave de um trio",
            pattern = ODE,
            beats = ODE_BEATS,
            backing = BackingStyle.HARMONY,
            backingVoices = 2,
            backingPart = 1,
            bpm = 88,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "O app faz a melodia e o backing agudo; você canta o backing grave.",
                "As três vozes formam acordes: escute como a sua nota completa o som.",
                "Cante cerca de 8 dB abaixo do regente e acompanhe as vogais dele.",
            ),
        ),
    )

    fun byId(id: String): Exercise? = all.firstOrNull { it.id == id }
}

/** How to learn each kind of second voice. */
class Lesson(val mode: HarmonyMode, val whenToUse: String, val howTo: List<String>, val exerciseId: String)

object SecondVoiceLessons {
    /** Example melody ("Ode à Alegria") used to demonstrate every type, as offsets from the root. */
    val example: IntArray get() = ODE
    val exampleBeats: FloatArray get() = ODE_BEATS

    val all: List<Lesson> = listOf(
        Lesson(
            HarmonyMode.THIRD_ABOVE,
            "A mais usada em duplas (sertanejo, MPB, gospel). Funciona quando a melodia está na região média ou grave da sua voz.",
            listOf(
                "A partir da nota da melodia, suba duas notas da escala: Dó vira Mi, Ré vira Fá.",
                "Ande junto com a melodia: se ela sobe, você sobe; se desce, você desce.",
                "A distância às vezes é de 3 semitons, às vezes de 4. Quem decide é a escala do tom.",
            ),
            "segunda-acima",
        ),
        Lesson(
            HarmonyMode.THIRD_BELOW,
            "Quando a melodia já está aguda ou quando a sua voz é mais grave que a do regente.",
            listOf(
                "Desça duas notas da escala a partir da melodia: Mi vira Dó, Sol vira Mi.",
                "Pense na sua voz como o chão da melodia: firme e um pouco mais suave.",
            ),
            "segunda-abaixo",
        ),
        Lesson(
            HarmonyMode.SIXTH_BELOW,
            "Para uma voz grave sob uma melodia aguda. É a terça acima levada uma oitava para baixo, e soa mais aberta.",
            listOf(
                "Cante as notas da 3ª acima, uma oitava abaixo. Melodia Dó4 → 3ª acima Mi4 → 6ª abaixo Mi3.",
                "Muito usada quando um homem harmoniza a melodia de uma mulher.",
            ),
            "segunda-sexta",
        ),
        Lesson(
            HarmonyMode.SIXTH_ABOVE,
            "Para uma voz aguda sobre uma melodia grave. É a terça abaixo levada uma oitava para cima.",
            listOf(
                "Cante as notas da 3ª abaixo, uma oitava acima. Melodia Mi3 → 3ª abaixo Dó3 → 6ª acima Dó4.",
                "Use voz leve: notas agudas chamam atenção.",
            ),
            "segunda-sexta-acima",
        ),
        Lesson(
            HarmonyMode.GUIDED,
            "Na prática, uma segunda voz raramente fica só em terças. Ela alterna terça e sexta para dar saltos menores e caber na voz.",
            listOf(
                "Antes de cada nota, pergunte: a terça ou a sexta está mais perto da nota em que estou?",
                "Não pule junto com a melodia nos saltos grandes: fique perto e deixe ela se mover.",
                "O resultado é uma linha lisa e fácil de cantar, como a dos arranjadores.",
            ),
            "segunda-conduzida",
        ),
        Lesson(
            HarmonyMode.CONTRARY,
            "Para dar movimento e independência, em escalas e finais de frase. Muito usado em corais.",
            listOf(
                "Quando a melodia sobe, você desce, e vice-versa.",
                "Use só intervalos consonantes: 3ª, 6ª, 5ª e oitava.",
                "Evite quintas e oitavas paralelas: soam ocas.",
            ),
            "segunda-contraria",
        ),
        Lesson(
            HarmonyMode.PEDAL,
            "Para criar tensão e sustentação em introduções, refrões lentos e louvor. Simples de cantar, exigente na afinação.",
            listOf(
                "Escolha a tônica ou a quinta do tom e segure.",
                "Saia da nota só quando ela chocar com a melodia, e volte logo em seguida.",
                "Afinação firme e sem tremer: a nota pedal é a base de todo o resto.",
            ),
            "segunda-pedal",
        ),
        Lesson(
            HarmonyMode.UNISON,
            "Para dar força a um trecho, como um refrão, ou quando a harmonia ainda está insegura. É a base do backing vocal.",
            listOf(
                "Cante a mesma melodia, junto ou uma oitava acima/abaixo.",
                "Copie vogais, respirações e cortes. Tire o vibrato.",
            ),
            "backing-dobra",
        ),
    )

    fun of(mode: HarmonyMode): Lesson = all.first { it.mode == mode }
}

class Article(val title: String, val summary: String, val body: String)

/** Study material. Body format: "# " heading, "- " bullet, blank line = paragraph break. */
object Studies {
    val all: List<Article> = listOf(
        Article(
            "Respiração", "A base de tudo: o ar que sustenta a voz",
            """
            Cantar bem começa no ar. A respiração usada no canto é a costo-diafragmática: o ar entra baixo, expandindo a barriga, as costelas laterais e as costas, sem levantar os ombros.

            # Como treinar
            - Deite de barriga para cima com um livro sobre o umbigo. Inspire pelo nariz e veja o livro subir; solte o ar e veja descer.
            - Em pé, mãos na cintura com os polegares nas costas: ao inspirar, sinta as costelas abrirem para os lados.
            - Solte o ar em "sss" contando até 10, 15, 20. O som deve ser uniforme do começo ao fim.

            # Erros comuns
            - Subir os ombros e encher só o peito.
            - Puxar o ar fazendo barulho pela boca.
            - Gastar todo o ar na primeira palavra da frase.

            Use o exercício "Respiração 4-4-8" todos os dias, antes de cantar.
            """.trimIndent(),
        ),
        Article(
            "Postura", "Corpo alinhado, voz livre",
            """
            A voz é produzida pelo corpo inteiro. Tensão no pescoço, nos ombros ou na mandíbula aparece no som.

            # Checklist
            - Pés na largura do quadril, peso distribuído.
            - Joelhos destravados, nunca esticados ao máximo.
            - Coluna alongada, como se um fio puxasse o topo da cabeça.
            - Ombros soltos e para baixo.
            - Queixo paralelo ao chão: não levante a cabeça para o agudo nem abaixe para o grave.
            - Mandíbula solta: a boca abre por gravidade, não por força.

            Antes de cantar, gire os ombros, alongue o pescoço devagar e massageie a articulação da mandíbula (logo à frente da orelha).
            """.trimIndent(),
        ),
        Article(
            "Apoio", "Controle da saída do ar",
            """
            Apoio é a capacidade de dosar o ar enquanto canta. Depois de inspirar, você tenta manter as costelas abertas e deixa a musculatura do abdômen controlar a saída do ar aos poucos.

            # Exercícios
            - "sss" longo: o som não pode oscilar.
            - "fff" em pulsos curtos: f-f-f-f, sentindo a barriga dar pequenos impulsos.
            - Staccato em "ha": cada nota nasce do abdômen, não da garganta.

            # Sinais de falta de apoio
            - A afinação cai no fim das frases.
            - A voz fica soprosa ou falha nas notas longas.
            - Você sente a garganta apertar no agudo.
            """.trimIndent(),
        ),
        Article(
            "Ressonância", "Projetar sem gritar",
            """
            O som nasce nas pregas vocais, mas ganha corpo nos espaços de ressonância: garganta, boca e cavidades do rosto. Usar bem esses espaços dá volume sem esforço.

            # Como sentir
            - Faça "mmm" (humming) e sinta vibrar nos lábios e no nariz: isso é ressonância na "máscara".
            - Boceje de leve: a sensação de espaço no fundo da boca (palato mole elevado) deixa o som mais redondo.
            - Alterne "mmm" com "mmmaaa", tentando manter a vibração na frente do rosto.

            Volume vem de ressonância e apoio, não de empurrar a garganta. Se doer ou arranhar, pare.
            """.trimIndent(),
        ),
        Article(
            "Registros vocais", "Peito, cabeça e voz mista",
            """
            A voz muda de "cor" conforme sobe. Esses modos de funcionamento são os registros.

            # Os principais
            - Voz de peito: a voz da fala, cheia e forte nos graves.
            - Voz de cabeça: mais leve, usada nos agudos, vibra mais no rosto e na cabeça.
            - Voz mista: a mistura das duas, que permite subir sem quebrar e sem gritar.
            - Falsete: som leve e soproso, muito usado como efeito.

            # A passagem
            Entre os registros existe uma região de passagem onde a voz pode "quebrar". Para suavizar:
            - Faça sirenes e vibração de lábios atravessando a passagem.
            - Diminua o volume ao subir em vez de empurrar.
            - Mantenha a mesma vogal e o mesmo espaço na boca.
            """.trimIndent(),
        ),
        Article(
            "Afinação e ouvido", "Ouvir antes de cantar",
            """
            Cantar desafinado quase sempre é um problema de percepção ou de controle, e os dois melhoram com treino.

            # Rotina de ouvido
            - Ouça a nota, imagine-a mentalmente e só então cante.
            - Use a tela "Ouvir": cante uma nota longa e observe se a linha fica estável.
            - Grave-se cantando e ouça depois. O que ouvimos por dentro é diferente do que sai.
            - Pratique "Repita a nota" e "Escala em terças" diariamente.

            # Dicas
            - Comece devagar: velocidade vem depois da precisão.
            - Se estiver sempre abaixo (calante), verifique o apoio e pense a nota "por cima".
            - Se estiver sempre acima, relaxe e diminua a força.
            """.trimIndent(),
        ),
        Article(
            "Intervalos", "As distâncias entre as notas",
            """
            Intervalo é a distância entre duas notas, contada em semitons (meios tons). Reconhecer intervalos é essencial para cantar afinado e para fazer segunda voz.

            # Intervalos e referências
            - 2ª menor (1 semitom): tema do filme "Tubarão".
            - 2ª maior (2): "Pa-ra-béns" no início de "Parabéns pra você".
            - 3ª menor (3): "Smoke on the Water"; descendente em "Hey Jude".
            - 3ª maior (4): "Oh When the Saints".
            - 4ª justa (5): "Marcha nupcial" (Here Comes the Bride).
            - 5ª justa (7): "Bri-lha, bri-lha" em "Brilha, brilha estrelinha".
            - 6ª maior (9): "My Bonnie".
            - 8ª justa (12): "Some-where" em "Over the Rainbow".

            As terças e sextas são os intervalos mais usados na segunda voz: soam doces e combinam com quase toda melodia.
            """.trimIndent(),
        ),
        Article(
            "Escalas e campo harmônico", "O mapa das notas de uma música",
            """
            Toda música tem um tom: um conjunto de notas que soam "em casa". Conhecer esse conjunto é o que permite criar uma segunda voz.

            # Escala maior
            Segue o padrão tom-tom-semitom-tom-tom-tom-semitom. Em Dó: Dó Ré Mi Fá Sol Lá Si Dó.

            # Escala menor natural
            Padrão tom-semitom-tom-tom-semitom-tom-tom. Em Lá: Lá Si Dó Ré Mi Fá Sol Lá.

            # Campo harmônico maior
            Sobre cada nota da escala nasce um acorde: I (maior), ii (menor), iii (menor), IV (maior), V (maior), vi (menor) e vii° (diminuto). Em Dó: C, Dm, Em, F, G, Am, Bº.

            # Por que importa
            A segunda voz em terças anda pela escala do tom. Por isso a distância às vezes é de 3 semitons (terça menor) e às vezes de 4 (terça maior): é o que mantém as duas vozes dentro do mesmo campo harmônico. O app calcula isso para você a partir do tom detectado.
            """.trimIndent(),
        ),
        Article(
            "Como fazer segunda voz", "Do zero até cantar em dupla",
            """
            Segunda voz é uma melodia que acompanha a principal, quase sempre em terças ou sextas, cantando o mesmo texto e o mesmo ritmo.

            # O jeito mais comum
            - Terça acima: a segunda voz canta 2 notas da escala acima da melodia (Dó → Mi, Ré → Fá). É a base de muitas duplas sertanejas e do gospel.
            - Terça abaixo: 2 notas da escala abaixo. Boa quando a melodia já é aguda.
            - Sexta abaixo: a terça acima, só que uma oitava mais grave. Útil para vozes graves.

            # Passo a passo no app
            - Adicione a música em "Músicas" (arquivo de áudio, cantando a melodia ou digitando as notas).
            - Confira se o tom detectado está certo; ajuste se precisar.
            - Escolha o intervalo (3ª acima é o melhor para começar).
            - Ouça só a 2ª voz com o guia, várias vezes, até cantarolar de memória.
            - Cante junto com o guia da 2ª voz, usando fones.
            - Ligue a música e o guia; depois desligue o guia e cante só com a música.
            - Estude frase por frase com o modo repetir e a velocidade 0,75×.

            # Dicas de dupla
            - Aprenda primeiro a melodia principal. Sem ela, você não tem referência.
            - A segunda voz costuma ser um pouco mais suave que a principal.
            - Respire, pronuncie e corte as notas junto com a voz principal.
            - Nem toda nota precisa ser terça: em finais de frase, muitos arranjos usam uníssono ou quinta. Confie no seu ouvido.
            - Treine com um fone em só um ouvido para se acostumar a ouvir a melodia sem ser "puxado" por ela.
            """.trimIndent(),
        ),
        Article(
            "Tipos de segunda voz", "Paralela, conduzida, contrária, pedal e dobra",
            """
            Nem toda segunda voz é uma terça paralela. Conhecer os tipos ajuda a escolher o que soa melhor em cada trecho. Em "Treinar", toque em "Aprenda os tipos de segunda voz" para ouvir e praticar cada um.

            # Paralelas
            - 3ª acima: anda junto com a melodia, duas notas da escala acima. A mais comum.
            - 3ª abaixo: duas notas abaixo. Boa quando a melodia é aguda.
            - 6ª abaixo e 6ª acima: as terças levadas uma oitava. Soam mais abertas e resolvem diferenças de região entre as vozes.

            # Conduzida
            Alterna terças e sextas para a linha andar pouco e caber na voz. É assim que os arranjadores escrevem na prática.

            # Movimento contrário
            A segunda voz anda no sentido oposto ao da melodia. Dá independência e movimento, mas exige mais ouvido.

            # Nota pedal
            Uma nota fixa (tônica ou quinta) sustentada enquanto a melodia se move. Cria tensão e dá chão ao arranjo.

            # Dobra
            Uníssono ou oitava. Reforça a melodia e é a base do backing vocal.

            # Como escolher
            - Comece pela 3ª acima ou abaixo, conforme a sua voz.
            - Se a linha ficar pulando demais, use a conduzida.
            - Em finais e escalas, experimente o contrário.
            - Em introduções e trechos lentos, a nota pedal.
            """.trimIndent(),
        ),
        Article(
            "Backing vocal", "Vozes de apoio que valorizam o regente",
            """
            Backing vocal são as vozes de apoio que acompanham o cantor principal, o regente da música. A função é valorizar a melodia, nunca competir com ela.

            # Estilos
            - Harmonia: canta a letra junto, em terças, ou em trio formando acordes.
            - Pad: notas longas do acorde em "Uh" ou "Ah", como um teclado de vozes.
            - Resposta: repete ou comenta o fim das frases, nas pausas do regente.
            - Dobra: a mesma melodia, para engrossar um refrão.

            # Regras de ouro
            - A melodia sempre na frente.
            - Mesmas vogais, mesmas respirações, mesmos cortes.
            - Menos vibrato que o regente.
            - Siga a dinâmica: se ele cresce, você cresce, mas continua atrás.

            Em "Músicas", abra uma música, toque em "Backing", escolha o tipo de voz do regente e o estilo. O Cantorial escreve a sua parte na sua extensão e mostra o volume ideal.
            """.trimIndent(),
        ),
        Article(
            "Equilíbrio entre regente e backing", "Quanto mais baixo cantar, e por quê",
            """
            Equilíbrio é o volume de cada voz em relação à principal. Um bom backing é mais sentido do que ouvido.

            # Volumes de referência, abaixo do regente
            - Harmonia com uma voz: cerca de 6 dB.
            - Harmonia com duas vozes: cerca de 8 dB cada.
            - Pad: cerca de 10 dB.
            - Resposta: cerca de 3 dB, porque entra quando o regente para.
            - Dobra: cerca de 9 dB.

            Cerca de 10 dB a menos soa como metade do volume.

            # Conforme a região da voz
            - Backing mais agudo que o regente: agudos aparecem mais. Cante cerca de 2 dB mais suave, com voz leve.
            - Backing mais grave: pode ter um pouco mais de corpo, sem cobrir a melodia.
            - Mesma região: cuidado para não ir para a melodia dele sem perceber.
            - Vozes masculina e feminina: a harmonia soa uma oitava deslocada e fica aberta. Mantenha o volume baixo.

            # Na prática
            - No palco, o backing fica um pouco mais longe do microfone que o regente.
            - Na mesa de som, os canais de backing ficam abaixo do principal.
            - Treine com o exercício "Equilíbrio de volume" e com o medidor de equilíbrio da prática de músicas.
            """.trimIndent(),
        ),
        Article(
            "Combinações de vozes", "Quem canta acima e quem canta abaixo",
            """
            Quem faz a segunda voz ou o backing, e em que posição, depende do tipo de voz do regente.

            # Regente masculino
            - Tenor com barítono ou baixo no backing: o backing canta abaixo, em 3ª ou 6ª.
            - Barítono ou baixo com tenor no backing: o tenor canta acima, em 3ª.
            - Voz masculina com voz feminina no backing: ela canta na região dela, em geral a 3ª acima soando uma oitava acima. Bem suave.

            # Regente feminina
            - Soprano com mezzo ou contralto no backing: 3ª ou 6ª abaixo.
            - Contralto ou mezzo com soprano no backing: 3ª acima, com voz leve.
            - Voz feminina com voz masculina no backing: ele canta abaixo, na região dele, ou faz um pad grave.

            # Mesma voz
            Dois tenores ou duas sopranos: um faz a 3ª acima e troca para a 3ª abaixo quando a melodia fica aguda demais.

            O Cantorial faz essa escolha quando você informa o tipo de voz do regente e mede a sua extensão no teste de extensão.
            """.trimIndent(),
        ),
        Article(
            "Classificação vocal", "Qual é a sua voz?",
            """
            As vozes são classificadas pela extensão (notas que alcança) e, principalmente, pela tessitura (região onde canta com conforto e boa qualidade).

            # Extensões típicas
            - Baixo: Mi2 a Mi4.
            - Barítono: Lá2 a Lá4.
            - Tenor: Dó3 a Dó5.
            - Contralto: Fá3 a Fá5.
            - Mezzo-soprano: Lá3 a Lá5.
            - Soprano: Dó4 a Dó6.

            Use o "Teste de extensão" em "Treinar" para medir a sua. A classificação é só um ponto de partida: timbre, peso da voz e conforto também contam, e a extensão cresce com treino. Não se rotule cedo demais.
            """.trimIndent(),
        ),
        Article(
            "Dicção", "Palavras claras sem perder o som",
            """
            Cantar bem é também ser entendido. As vogais carregam o som; as consoantes dão o sentido.

            # Exercícios
            - Leia a letra em voz alta, exagerando a articulação.
            - Trava-línguas devagar e depois rápido: "O rato roeu a roupa do rei de Roma".
            - Com uma caneta atravessada entre os dentes, leia um texto por 1 minuto; depois leia sem ela.

            # Dicas
            - Mantenha as vogais abertas e puras nas notas longas.
            - Nas notas longas, sustente a vogal e deixe a consoante para o final.
            """.trimIndent(),
        ),
        Article(
            "Vibrato", "A ondulação natural da voz",
            """
            O vibrato é uma leve oscilação da afinação, de 5 a 7 vezes por segundo. Ele aparece naturalmente quando a voz está livre e bem apoiada.

            # Como estimular
            - Na tela "Ouvir", sustente uma nota e observe a linha: um vibrato saudável aparece como uma ondinha regular.
            - Alterne lentamente entre duas notas a meio tom de distância, acelerando aos poucos, sem tensão.
            - Primeiro aprenda a cantar a nota reta e estável; o vibrato vem como consequência.

            Evite produzir vibrato tremendo o queixo ou a barriga.
            """.trimIndent(),
        ),
        Article(
            "Saúde vocal", "Cuidar do instrumento",
            """
            Sua voz é um instrumento que você não pode trocar.

            # Hábitos
            - Beba água ao longo do dia; a hidratação das pregas vocais vem de dentro.
            - Durma bem: cansaço aparece na voz.
            - Sempre aqueça antes e desaqueça depois de cantar (humming suave descendo).
            - Evite gritar, falar alto em lugares barulhentos e pigarrear. Prefira engolir ou tossir de leve.
            - Álcool e excesso de café ressecam. Fumar agride diretamente as pregas vocais.

            # Sinais de alerta
            Rouquidão por mais de 15 dias, dor ao cantar ou perda de notas que você alcançava são motivo para procurar um otorrinolaringologista ou fonoaudiólogo.
            """.trimIndent(),
        ),
        Article(
            "Rotina de estudo", "20 minutos por dia",
            """
            Constância vale mais do que treinos longos de vez em quando.

            # Plano diário
            - 3 min: Respiração 4-4-8.
            - 4 min: Vibração de lábios e Humming.
            - 5 min: Escala maior, Arpejo ou Nota longa.
            - 3 min: Repita a nota ou Escala em terças.
            - 5 min: repertório e segunda voz em "Músicas".

            # Semanal
            - Grave uma música no começo da semana e outra no fim, e compare.
            - Refaça o teste de extensão a cada mês.
            - Descanse a voz pelo menos um dia por semana.
            """.trimIndent(),
        ),
    )
}

object Tips {
    val all: List<String> = listOf(
        "Aqueça a voz por pelo menos 5 minutos antes de cantar.",
        "Beba água ao longo do dia; a hidratação das pregas vocais vem de dentro.",
        "Respire pelo nariz quando der tempo, pela boca quando a frase for rápida.",
        "Ouça a nota na cabeça antes de cantá-la.",
        "Se o agudo apertar, diminua o volume em vez de empurrar.",
        "Grave-se. O que ouvimos por dentro é diferente do que sai.",
        "Ombros soltos: tensão no pescoço vira tensão na voz.",
        "Nas notas longas, mantenha a vogal aberta e deixe a consoante para o final.",
        "Aprenda a melodia principal antes de tentar a segunda voz.",
        "Terça acima é o intervalo mais comum para começar a fazer segunda voz.",
        "Estude trechos difíceis devagar; a velocidade vem depois.",
        "Desaqueça depois de cantar: humming suave, descendo.",
        "Dor ou rouquidão persistente é sinal para parar e procurar ajuda.",
        "Use fones de ouvido nos exercícios de segunda voz.",
        "No backing vocal, a melodia sempre vem na frente: cante de 6 a 10 dB abaixo do regente.",
        "Backing bom é sentido, não ouvido: mesmas vogais e mesmos cortes do regente.",
        "Mandíbula solta: a boca abre por gravidade, não por força.",
        "Cante sorrindo levemente com os olhos: o som fica mais brilhante.",
        "No fim da frase, mantenha o apoio para a afinação não cair.",
        "Constância vence intensidade: 20 minutos por dia valem mais que 2 horas no domingo.",
        "Bocejar abre espaço no fundo da boca e deixa o som mais redondo.",
        "Se a voz quebrar na passagem, treine sirenes em vibração de lábios.",
    )

    fun ofTheDay(epochDay: Long): String = all[Math.floorMod(epochDay, all.size.toLong()).toInt()]
}
