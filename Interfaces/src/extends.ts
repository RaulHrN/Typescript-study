interface JogoProps{
    readonly id: string;
    nome: string;
    descricao: string;
    plataforma: string[];
}

const kingdomHearts: JogoProps = {
    id: "123",
    nome: "Kingdom Hearts 4",
    descricao: "Aventura, Ação, RPG",
    plataforma: ["PS5", "Xbox X|S", "PC"]
}

/* interface DLC extends JogoProps {
    novoConteudo: string[];
}

const kingdomHeartsDLC: DLC = {
    id: "90",
    nome: "Kingdom Hearts 4: Dead Man's Chest",
    descricao: "Capitão Jack Sparrow e seus companheiros se juntam a Sora, Donald e Goof",
    plataforma: ["PS5", "PC"],
    novoConteudo: ["Novo mundo ''Caribbean''", "novos personagens", "novas habilidades", "novos itens", "e muito mais."]
} */

interface DLC extends JogoProps {
    novoConteudo: string[];
    jogoOriginal: JogoProps;
}

const kingdomHeartsDLC: DLC = {
    id: "90",
    nome: "Kingdom Hearts 4: Dead Man's Chest",
    descricao: "Capitão Jack Sparrow e seus companheiros se juntam a Sora, Donald e Goof",
    plataforma: ["PS5", "PC"],
    novoConteudo: ["Novo mundo ''Caribbean''", "novos personagens", "novas habilidades", "novos itens", "e muito mais."],
    jogoOriginal: kingdomHearts
}