

// Decorator Método, ele só é chamado quando nosso método for chamado em algum local.



function verificaPessoa(idade: number){
  return (target: any, key: string, descriptor: PropertyDescriptor) => {
    // console.log("Target: ", target);
    // console.log("Key: ", key);
    // console.log("Descriptor: ", descriptor);

    const metodoOriginal = descriptor.value // Salvar nosso metodo original

    // Reescrever nosso método.
    descriptor.value = function () {
      if(idade < 18){
        console.log("Estamos cadastrando como Pessoa menor de idade")

        // Agora queremos retornar o metodo original para ele ser executado
        return metodoOriginal.apply(this);
      }else{
        console.log("Adulto cadastrado no sistema")

        // Agora queremos retornar o metodo original para ele ser executado
        return metodoOriginal.apply(this)
      }
    }

  }
}


class Pessoa {
  nome: string;

  constructor(nome: string){
    this.nome = nome;
  }

  @verificaPessoa(16)
  cadastrarPessoa(){
    console.log(`Bem vindo ${this.nome} ao sistema.`)
  }

}

const pessoa1 = new Pessoa("Matheus F");


pessoa1.cadastrarPessoa();