

/*  >> DECORATORS <<
  Class
  Propriedade
  Métodos
  Parametros ( BEM POUCO USADO )
  GETTERS / SETTERS ( BEM POUCO USADO )
 */


  // target = receber o construtor da nossa classe.
  // function logInfo(target: any){
  //   console.log("Sistema está online!");
  // }


  // >>> Padrao factory (Fabrica) |  Função que vai retornar a criação do decorator.
  // function logInfo(mensagem: string){
  //   return (target: any) => {
  //     console.log(`${mensagem}, ${target}`)
  //   }
  // }

  // @logInfo("Servidor está rodando!")
  // class Sistema{

  // }


  /// ============ PRATICANDO ============


  function setIpServidor(novoIp: string){
    return (target: any) => { // target é o construtor da nossa classe.
      return class extends target{
        ip = novoIp;
      }
    }
  }

  @setIpServidor("192.168.50.900")
  class Servidor{

  }


  const servidor1 = new Servidor();

  console.log(servidor1);