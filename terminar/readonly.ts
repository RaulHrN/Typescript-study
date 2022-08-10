
class Pessoa{
  readonly id: string = "123";
  nome: string;
  idade: number;

  constructor(nome: string, idade: number){
    this.nome = nome;
    this.idade = idade;
  }

  mostraId(){
    console.log(`ID DO USUARIO: `, this.id);
  }

}

const ana = new Pessoa("Ana Carol", 29);

console.log("ID DA ANA: ", ana.id)

// console.log(ana);