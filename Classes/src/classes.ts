class Loja{
    //Atributos da classe
    nome: string;
    categoria: string;

    constructor(nome: string, categoria: string){
        this.nome = nome;
        this.categoria = categoria;
    }

    criarLoja(){
        console.log(`Loja ${this.nome}, categoria: ${this.categoria}`)
    }
}

const bluePizza = new Loja("Blue Pizza", "Pizzaria");

console.log(bluePizza.nome);