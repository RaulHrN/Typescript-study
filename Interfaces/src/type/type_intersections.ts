type info = {
    id: number;
    nome: string;
    descricao?: string;
}

const produtoInfo: info = {
    id: 123,
    nome: "Placa de vídeo"
}

type Categoria = {
    slug: string;
    qtdProduto: number
}

const categoria1: Categoria = {
    slug: "hardware",
    qtdProduto: 5
}

type ProdutoInfo = info & Categoria;

const novoProduto: ProdutoInfo = {
    id: 54321,
    nome: "Memoria RAM",
    descricao: "Memoria RAM 8GB",
    slug: "hardware",
    qtdProduto: 18
}