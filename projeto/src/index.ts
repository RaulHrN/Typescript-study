const input1 = document.getElementById('num1') as HTMLInputElement;
const input2 =  document.getElementById('num2') as HTMLInputElement;

const buttonSoma = document.getElementById("botaoSomar") as HTMLElement;
const buttonSubtrair = document.getElementById("botaoSubtrair") as HTMLElement;

type Operacoes = "SOMAR" | "SUBTRAIR";

interface Valores {
    op: Operacoes;
    a: number;
    b: number;
}

function operacao({op, a, b}: Valores): number {
    if(op === "SOMAR"){
        return a + b;
    } else {
        return a - b;
    }
}

buttonSoma.addEventListener('click', function () {
    const resultado = operacao({op:"SOMAR", a: Number(input1.value), b: Number(input2.value)});
    console.log(resultado);
})

buttonSubtrair.addEventListener('click', function(){
    const resultado = operacao({op:"SUBTRAIR", a: Number(input1.value), b: Number(input2.value)});
    console.log(resultado);
})