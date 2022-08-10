
// Existem quatro modificadores de acesso que podemos atribuir aos atributos como:
// public, protected, private, readonly (apenas nos atributos)

/*
  >> Protected: Atributos e métodos protegidos podem ser acessados ou alterados por meio da 
  classe em que foram criados e por meio das classes que foram filhas (que extende a classe pai).

  >> Private: Podem ser acessados ou alterados apenas por meio da classe em que foram criados, 
  Ou seja ele nao pode ser acessado ou modificado fora da classe em que foi criado.

  >> E o famoso readonly: (apenas para os atributos) Podemos apenas ler e ver esse atributo porem
  nao podemos alterar ele.
*/

class Usuario{
  protected id: number; // Podemos chamar na classe usuario ou classes que extende a classe usuario
  nome: string;
  email: string;

  constructor(id: number, nome: string, email: string){
    this.id = id;
    this.nome = nome;
    this.email = email;
  }

}


class Admin extends Usuario{
  cargo: string;
  nivel: number;

  constructor(id: number, nome: string, email: string, cargo: string, nivel: number){
    // Chamando o cosntrutor da classe pai.
    super(id, nome, email);
    
    this.cargo = cargo;
    this.nivel = nivel;
  }

  protected mudarCargo(cargo: string): void {
    console.log("ALTERANDO CARGO PARA: ", cargo);
    console.log("ID DO USUARIO: ", this.id)
  }


  acessarAdmin(){
    this.mudarCargo("Designer");
  }

}

const usuario1 = new Admin(123, "Matheus", "matheus@teste.com", "Programador", 2);

usuario1.acessarAdmin();



