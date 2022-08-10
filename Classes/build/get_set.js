"use strict";
// Caso de uso de um JOGO
class Jogo {
    constructor(servidor) {
        this.id = "1234";
        this.servidor = servidor;
    }
    //Metodo GET = Para pegar o valor ao inves de acessar diretamente a propriedade
    get getServidorIP() {
        console.log("== METODO GET ==");
        return this.servidor;
    }
    //Metodo SET = Passar / alterar algum atributo.
    set setServidorIP(novoIp) {
        if (this.servidor === novoIp) {
            throw new Error("O novo IP deve ser diferente do IP antigo.");
        }
        this.servidor = novoIp;
    }
}
const GTA5 = new Jogo("192.168.5.10");
try {
    GTA5.setServidorIP = "192.250.155.10";
}
catch (error) {
    console.log("ERROR: ", error.message);
}
console.log(GTA5.getServidorIP);
