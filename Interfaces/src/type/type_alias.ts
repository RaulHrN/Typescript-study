type Uuid = number | string | null;

function acessar(uuid: Uuid, nome: string){
    console.log(`ID: ${uuid} - Bem vindo ${nome}`)
}

function logUsuario(uuid: Uuid){
    console.log(`Conta referente ao UUID: ${uuid}`)
}

type Moedas = "BRL" | "EUR" | "USD" | "BTC";

function comprarItem(moeda: Moedas){
    console.log(`Comprando com a moeda ${moeda}`)
}

comprarItem("BRL");