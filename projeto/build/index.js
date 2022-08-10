"use strict";
const input1 = document.getElementById('num1');
const input2 = document.getElementById('num2');
const buttonSoma = document.getElementById("botaoSomar");
const buttonSubtrair = document.getElementById("botaoSubtrair");
function operacao({ op, a, b }) {
    if (op === "SOMAR") {
        return a + b;
    }
    else {
        return a - b;
    }
}
buttonSoma.addEventListener('click', function () {
    const resultado = operacao({ op: "SOMAR", a: Number(input1.value), b: Number(input2.value) });
    console.log(resultado);
});
buttonSubtrair.addEventListener('click', function () {
    const resultado = operacao({ op: "SUBTRAIR", a: Number(input1.value), b: Number(input2.value) });
    console.log(resultado);
});
