package com.raulhrn.vox.core

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
            title = "2ª voz: terça acima",
            category = "Segunda voz",
            summary = "Cante acima enquanto o app faz a melodia",
            pattern = intArrayOf(0, 2, 4, 5, 4, 2, 0),
            beats = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 2f),
            harmonySteps = 2,
            bpm = 84,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Primeiro o app toca melodia e segunda voz juntas: escute a terça.",
                "Depois ele toca só a melodia e você canta a segunda voz (a linha colorida).",
                "Use fones de ouvido para o microfone ouvir só você.",
                "Se for puxado para a melodia, cante mais baixo e pense na sua nota antes.",
            ),
        ),
        Exercise(
            id = "segunda-abaixo",
            title = "2ª voz: terça abaixo",
            category = "Segunda voz",
            summary = "A harmonia por baixo da melodia",
            pattern = intArrayOf(4, 5, 7, 5, 4, 2, 4),
            beats = floatArrayOf(1f, 1f, 2f, 1f, 1f, 1f, 2f),
            harmonySteps = -2,
            bpm = 84,
            minutes = 4,
            needsHeadphones = true,
            steps = listOf(
                "Escute a melodia com a voz de baixo, depois cante só a de baixo.",
                "A terça abaixo é comum quando a melodia já está aguda.",
                "Use fones de ouvido.",
            ),
        ),
    )

    fun byId(id: String): Exercise? = all.firstOrNull { it.id == id }
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
        "Mandíbula solta: a boca abre por gravidade, não por força.",
        "Cante sorrindo levemente com os olhos: o som fica mais brilhante.",
        "No fim da frase, mantenha o apoio para a afinação não cair.",
        "Constância vence intensidade: 20 minutos por dia valem mais que 2 horas no domingo.",
        "Bocejar abre espaço no fundo da boca e deixa o som mais redondo.",
        "Se a voz quebrar na passagem, treine sirenes em vibração de lábios.",
    )

    fun ofTheDay(epochDay: Long): String = all[Math.floorMod(epochDay, all.size.toLong()).toInt()]
}
