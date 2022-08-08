interface SomaProps{
    (n1: number, n2: number): number;
}

let Soma: SomaProps = (n1:number, n2: number): number => {
    console.log( "Resultado: ", n1 + n2);

    return n1+n2;
}

const resultado = Soma(15, 10);

console.log("Resultado da variável: ", resultado);