"use strict";
const BurguerK = {
    nome: "BurguerK",
    endereco: "Rua Avenida Alameda",
    status: false
};
console.log(BurguerK);
function novaLoja({ nome, endereco, status }) {
    console.log(`Loja ${nome} criada com sucesso!`);
    console.log(`Endereço ${endereco} criado com sucesso!`);
    console.log(`Status: ${status}`);
}
novaLoja({ nome: "Helena Gym", endereco: "Avenida sem saida", status: true });
