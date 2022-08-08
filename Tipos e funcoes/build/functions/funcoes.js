"use strict";
function login(username) {
    console.log("Bem vindo " + username);
}
login("Raul");
let valor1 = 15;
let valor2 = 6;
function soma(n1, n2) {
    let valorT = n1 + n2;
    if (valorT > 19) {
        return "Maior que 19";
    }
    else {
        return "Menor que 19";
    }
}
console.log(soma(valor1, valor2));
